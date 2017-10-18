package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.InfoCollectionVisitingActor
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

class InitializingTransformer extends DummyTransformer {

	// Info collection actor for original program
	InfoCollectionVisitingActor infoActor
	// Program before initialization
	Program origP
	// Program after initialization (only a global component)
	Program initP
	// Current name used for initialization
	String currInitName
	// Current component being initialized
	Component currComp
	// Relations in a component that have a declaration
	Set<String> haveDeclaration
	// Relations in a component that need a declaration (their stage is @ext)
	Set<String> needDeclaration

	boolean inRuleHead

	InitializingTransformer() { actor = this }

	// Initialize components (add transformed contents in a single component)
	// A component might be visited multiple times (depending on inits)
	// Components with no inits are dropped
	IVisitable visit(Program n) {
		infoActor = new InfoCollectionVisitingActor()
		origP = n
		initP = new Program(new Component())

		origP.accept(infoActor)

		// Initializations
		origP.globalComp.accept(this)
		origP.inits.each { initName, compName ->
			currInitName = initName
			currComp = origP.comps[compName]
			if (!currComp) ErrorManager.error(ErrorId.COMP_UNKNOWN, compName)
			currComp.accept(this)
		}

		// Propagation rules
		origP.props.each { prop ->
			if (!origP.inits[prop.fromId])
				ErrorManager.error(ErrorId.COMP_UNKNOWN, prop.fromId)
			if (!origP.inits[prop.toId] && prop.toId)
				ErrorManager.error(ErrorId.COMP_UNKNOWN, prop.toId)

			// fromId is the component name after initialization
			// fromCompTemplate is the component before initialization
			def fromCompTemplate = origP.comps[origP.inits[prop.fromId]]

			def declaredRelations = infoActor.declaringAtoms[fromCompTemplate]

			// If preds is null then "*" was used
			def toPropagate = (prop.preds ?
					prop.preds.collect { p ->
						def relation = declaredRelations.find { it.name == p }
						if (!relation)
							ErrorManager.error(ErrorId.REL_UNKNOWN, p)
						return relation
					} :
					declaredRelations) as Set

			toPropagate.each { rel ->
				// Propagate to global scope and relation already declared there
				if (!prop.toId && rel in infoActor.declaringAtoms[origP.globalComp])
					ErrorManager.error(ErrorId.DEP_GLOBAL, rel.name)

				def head = new LogicalElement(rename(rel, prop.toId, prop.toId != null))
				def body = new LogicalElement(rename(rel, prop.fromId, false))
				initP.globalComp.rules << new Rule(head, body)
			}
		}

		return initP
	}

	void enter(Component n) {
		haveDeclaration = [] as Set
		needDeclaration = [] as Set
	}

	IVisitable exit(Component n, Map m) {
		n.declarations.each { initP.globalComp.declarations << (m[it] as Declaration) }
		n.rules.each { initP.globalComp.rules << (m[it] as Rule) }

		needDeclaration?.findAll { !(it in haveDeclaration) }?.each {
			ErrorManager.error(ErrorId.REL_NO_DECL_REC, it)
		}
		null
	}

	IVisitable exit(Declaration n, Map m) {
		// Ignore declarations in global scope
		if (currComp) haveDeclaration << n.atom.name
		super.exit(n, m)
	}

	// Override to keep track of when in rule's head
	IVisitable visit(Rule n) {
		actor.enter(n)
		inRuleHead = true
		m[n.head] = n.head.accept(this)
		inRuleHead = false
		if (n.body) m[n.body] = n.body.accept(this)
		return actor.exit(n, m)
	}

	IVisitable exit(Constructor n, Map m) {
		def (String newName, _) = rename(n)
		new Constructor(newName, n.exprs)
	}

	IVisitable exit(Relation n, Map m) {
		def loc = SourceManager.instance.recall(n)
		// @ext is allowed in the rule's head only in global space
		if (inRuleHead && currInitName && n.stage == "@ext")
			ErrorManager.error(loc, ErrorId.REL_EXT_HEAD, n.name)

		// Ignore relations in global scope or without an "@ext" stage
		if (currComp && n.stage == "@ext") needDeclaration << n.name

		def (String newName, String newStage) = rename(n)
		new Relation(newName, newStage, n.exprs)
	}

	IVisitable exit(Type n, Map m) {
		def (String newName, _) = rename(n)
		new Type(newName)
	}

	// Overrides to avoid unneeded allocations

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }

	def rename(Relation r) {
		// Global Component
		if (!currInitName)
			return [rename(r, currInitName, r.stage == "@ext").name, null]

		def declared = infoActor.declaringAtoms[currComp].collect { it.name }

		if (r.stage == "@ext")
			return [rename(r, currInitName, true).name, null]
		else if (r.name in declared)
			return [rename(r, currInitName, false).name, null]
		else
			return [r.name, null]
	}

	static def rename(Relation n, String initName, boolean withExt) {
		def prefix = initName ? "$initName:" : ""
		def suffix = withExt ? ":__eXt" : ""
		def name = "$prefix${n.name}$suffix"
		new Relation(name, null, n.exprs)
	}
}
