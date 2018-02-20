package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.ConstructionInfoVisitingActor
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.RelationInfoVisitingActor
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

class ComponentInitializingTransformer extends DummyTransformer {

	private RelationInfoVisitingActor relInfoActor
	// Info collection actor for original program
	private ConstructionInfoVisitingActor constructionInfoActor
	// Original program before initialization
	private Program origP
	// Program after initialization (only a global component)
	private Program initP
	// Current name used for initialization
	private String currInitName
	// Current component being initialized
	private Component currComp

	ComponentInitializingTransformer() { actor = this }

	// Initialize components (add transformed contents in a single component)
	// A component might be visited multiple times (depending on inits)
	// Components with no inits are dropped
	IVisitable visit(Program n) {
		origP = n
		initP = new Program(new Component())

		relInfoActor = new RelationInfoVisitingActor()
		relInfoActor.visit origP
		constructionInfoActor = new ConstructionInfoVisitingActor()
		constructionInfoActor.visit origP

		visit origP.globalComp
		origP.inits.each {
			currInitName = it.id
			currComp = n.comps[it.compName]
			if (!currComp)
				ErrorManager.error(ErrorId.COMP_UNKNOWN, it.compName)
			if (currComp.parameters.size() != it.parameters.size())
				ErrorManager.error(ErrorId.COMP_INIT_ARITY, it.parameters, it.compName, it.id)
			visit currComp
		}
		return initP
	}

	IVisitable exit(Component n, Map m) {
		n.declarations.each { initP.globalComp.declarations << (m[it] as Declaration) }
		n.rules.each { initP.globalComp.rules << (m[it] as Rule) }
		null
	}

	IVisitable exit(Constructor n, Map m) { new Constructor(rename(n.name), n.exprs) }

	IVisitable exit(Relation n, Map m) {
		def loc = SourceManager.instance.recall(n)
		if (inRuleHead && n.name.contains("@"))
			ErrorManager.error(loc, ErrorId.REL_EXT_INVALID)

		def origName = n.name
		if (!origName.contains("@"))
			return new Relation(rename(origName), n.exprs)

		def (simpleName, parameter) = origName.split("@")
		// Global space
		if (!currComp) {
			if (!origP.inits.any { it.id == parameter })
				ErrorManager.error(loc, ErrorId.COMP_UNKNOWN, parameter as String)
			return new Relation("$parameter:$simpleName", n.exprs)
		} else {
			def paramIndex = currComp.parameters.findIndexOf { it == parameter }
			if (paramIndex == -1)
				ErrorManager.error(loc, ErrorId.REL_EXT_UNKNOWN, parameter as String)

			def initParameter = origP.inits.find { it.id == currInitName }.parameters[paramIndex]
			def externalName = initParameter == "_" ? simpleName : "$initParameter:$simpleName"

			def externalTemplateComp = initParameter == "_" ?
					origP.globalComp :
					origP.comps[origP.inits.find { it.id == initParameter }.compName]
			if (!relInfoActor.declaredRelations[externalTemplateComp].any { it.name == simpleName })
				ErrorManager.error(loc, ErrorId.REL_NO_DECL_REC, simpleName as String)

			return new Relation(externalName, n.exprs)
		}
	}

	IVisitable exit(Type n, Map m) { n.isPrimitive() ? n : new Type(rename(n.name)) }

	String rename(String name) { currInitName ? "$currInitName:$name" : name }

	// Overrides to avoid unneeded allocations

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
