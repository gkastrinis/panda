package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.GroupElement
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.element.relation.Primitive
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager

class InitVisitingActor extends PostOrderVisitor<IVisitable> implements IActor<IVisitable>, TDummyActor<IVisitable> {

	// Info collection actor for original program
	InfoCollectionVisitingActor infoActor
	// Program before initialization
	Program origP
	// Program after initialization (only a global component)
	Program initP
	// ToId x Set<FromId>
	Map<String, Set<String>> reverseProps = [:].withDefault { [] as Set }
	// Current name used for initialization
	String currInitName
	// Current component being initialized
	Component currComp

	InitVisitingActor() { actor = this }

	// Initialize components (add transformed contents in a single component)
	// A component might be visited multiple times (depending on inits)
	// Components with no inits are dropped
	IVisitable visit(Program n) {
		infoActor = new InfoCollectionVisitingActor()
		origP = n
		initP = new Program(new Component())

		origP.accept(infoActor)
		origP.props.each { reverseProps[it.toId] << it.fromId }

		// Initializations
		origP.globalComp.accept(this)
		origP.inits.each { initName, compName ->
			currInitName = initName
			currComp = origP.comps[compName]
			if (!currComp) ErrorManager.error(ErrorId.UNKNOWN_COMP, compName)
			currComp.accept(this)
		}

		// Propagation rules
		origP.props.each { prop ->
			if (!origP.inits[prop.fromId])
				ErrorManager.error(ErrorId.UNKNOWN_COMP, prop.fromId)
			if (!origP.inits[prop.toId] && prop.toId)
				ErrorManager.error(ErrorId.UNKNOWN_COMP, prop.toId)

			// fromId is the component name after initialization
			// fromCompTemplate is the component before initialization
			def fromCompTemplate = origP.comps[origP.inits[prop.fromId]]

			def declaredRelations = infoActor.declaringAtoms[fromCompTemplate]

			// If preds is null then "*" was used
			def toPropagate = (prop.preds ?
					prop.preds.collect { p ->
						def relation = declaredRelations.find { it.name == p }
						if (!relation)
							ErrorManager.error(ErrorId.UNKNOWN_PRED, p)
						return relation
					} :
					declaredRelations) as Set

			toPropagate.each { rel ->
				// Propagate to global scope and relation already declared there
				if (!prop.toId && rel in infoActor.declaringAtoms[origP.globalComp])
					ErrorManager.error(ErrorId.DEP_GLOBAL, rel.name)

				// TODO should not accept on rel (wrong variables)
				currInitName = prop.toId
				def head = new LogicalElement(rel.accept(this) as Relation)
				currInitName = prop.fromId
				def body = new LogicalElement(rel.accept(this) as Relation)
				initP.globalComp.rules << new Rule(head, body)
			}
		}

		return initP
	}

	Component exit(Component n, Map<IVisitable, IVisitable> m) {
		n.declarations.each { initP.globalComp.declarations << (m[it] as Declaration) }
		n.rules.each { initP.globalComp.rules << (m[it] as Rule) }
		null
	}

	Declaration exit(Declaration n, Map<IVisitable, IVisitable> m) {
		new Declaration(m[n.atom] as Relation, n.types.collect { m[it] as Relation }, n.annotations)
	}

	Rule exit(Rule n, Map<IVisitable, IVisitable> m) {
		new Rule(m[n.head] as LogicalElement, m[n.body] as LogicalElement, n.annotations)
	}

	AggregationElement exit(AggregationElement n, Map<IVisitable, IVisitable> m) {
		new AggregationElement(m[n.var] as VariableExpr, m[n.predicate] as Predicate, m[n.body] as IElement)
	}

	ComparisonElement exit(ComparisonElement n, Map<IVisitable, IVisitable> m) {
		new ComparisonElement(m[n.expr] as BinaryExpr)
	}

	GroupElement exit(GroupElement n, Map<IVisitable, IVisitable> m) {
		new GroupElement(m[n.element] as IElement)
	}

	LogicalElement exit(LogicalElement n, Map<IVisitable, IVisitable> m) {
		new LogicalElement(n.type, n.elements.collect { m[it] as IElement })
	}

	NegationElement exit(NegationElement n, Map<IVisitable, IVisitable> m) {
		new NegationElement(m[n.element] as IElement)
	}

	Constructor exit(Constructor n, Map<IVisitable, IVisitable> m) { n }

	Type exit(Type n, Map<IVisitable, IVisitable> m) { n }

	Functional exit(Functional n, Map<IVisitable, IVisitable> m) {
		def (String newName, String newStage) = rename(n)
		def change = (n.name != newName || n.stage != newStage)
		change ? new Functional(newName, newStage, n.keyExprs, n.valueExpr) : n
	}

	Predicate exit(Predicate n, Map<IVisitable, IVisitable> m) {
		def (String newName, String newStage) = rename(n)
		def change = (n.name != newName || n.stage != newStage)
		change ? new Predicate(newName, newStage, n.exprs) : n
	}

	Primitive exit(Primitive n, Map<IVisitable, IVisitable> m) { n }

	BinaryExpr exit(BinaryExpr n, Map<IVisitable, IVisitable> m) { n }

	ConstantExpr exit(ConstantExpr n, Map<IVisitable, IVisitable> m) { n }

	GroupExpr exit(GroupExpr n, Map<IVisitable, IVisitable> m) { n }

	VariableExpr exit(VariableExpr n, Map<IVisitable, IVisitable> m) { n }

	/*Predicate exit(Predicate n, Map<IVisitable, IVisitable> m) {
		if (!inFrameRules && n.stage == "@past" && n.name in declaredAtoms && !autoGenDecls[newName]) {
			def decl = curComp.declarations.find { it.atom.name == n.name }
			if (!decl)
				ErrorManager.error(ErrorId.NO_DECL_REC, n.name)
		}
	}*/

	/*def rename(Relation atom) {
		def name = atom.name

		if (removeName != null && name.startsWith(removeName + ":"))
			name = name.replaceFirst(removeName + ":", "")

		Set<String> reverseSet = null
		if (reverseProps != null)
			reverseSet = reverseProps[atom.name]
		assert (reverseSet == null || reverseSet.size() == 1)

		// NOTE: This if should go before the next one, since the heuristic for
		// discovering predicated declared in a component will assume that a
		// @past predicate in the head of the rule is declared in the
		// component.
		if (atom.stage == "@past") {
			// * we are in the global component, thus in a custom frame rule
			if (initName == null)
				return new Tuple2(name + ":past", "@past")
			// * if @past is used in the head of a rule
			// * if @past is used for an entity
			// then fix name accordingly
			else if (inRuleHead || atom instanceof Type) {
				if (reverseSet == null)
					return new Tuple2(name, null)
				else
					return new Tuple2(reverseSet.first() + ":" + name, null)
			}
			// * else explicitly add the appropriate prefix and suffix
			else return new Tuple2(initName + ":" + name + ":past", "@past")
		}

		// * if the relation is declared in this component, add the appropriate prefix
		if (declaredAtoms != null && name in declaredAtoms)
			return new Tuple2(initName + ":" + name, atom.stage)

		// * if the relation is propagated from another component, explicitly add
		// the appropriate prefix and suffix
		if (reverseSet != null)
			return new Tuple2(initName + ":" + name + ":past", "@past")

		// * otherwise it is an external relation, thus leave the name unaltered
		return new Tuple2(name, atom.stage)
	}*/

	def rename(Relation r) {
		// Global Component
		if (!currInitName) {
			if (r.stage == "@past") return ["${r.name}:__pAsT", null]
			else return [r.name, r.stage]
		}

		def declared = infoActor.declaringAtoms[currComp].collect { it.name }

		if (r.stage == "@past")
			return ["$currInitName:${r.name}:__pAsT", null]
		else if (r.name in declared)
			return ["$currInitName:${r.name}", r.stage]
		else
			return [r.name, r.stage]
	}
}
