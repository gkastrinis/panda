package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.InfoCollectionVisitingActor
import org.clyze.deepdoop.actions.TypeInferenceVisitingActor
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

class SouffleConstructorTransformer extends DummyTransformer {

	InfoCollectionVisitingActor infoActor
	TypeInferenceVisitingActor inferenceActor

	// Unbound variables in a rule's head that a constructor will eventually bind
	Map<IExpr, Constructor> unboundVar

	boolean inRuleHead = false

	Set<Declaration> newDeclarations = [] as Set

	Set<Rule> newRules = [] as Set

	SouffleConstructorTransformer(InfoCollectionVisitingActor infoActor, TypeInferenceVisitingActor inferenceActor) {
		this.infoActor = infoActor
		this.inferenceActor = inferenceActor
	}

	IVisitable exit(Component n, Map<IVisitable, IVisitable> m) {
		if (newDeclarations || newRules || n.rules.find { it != m[it] }) {
			def ds = n.declarations + newDeclarations
			def rs = (n.rules.collect { m[it] as Rule } as Set) + newRules
			return rec(new Component(n.name, n.superComp, ds, n.constraints, rs))
		} else return n
	}

	// Overwrite for rule heads with constructors
	IVisitable visit(Rule n) {
		actor.enter(n)

		unboundVar = n.head.elements
				.findAll { it instanceof Constructor }
				.collect { it as Constructor }
				.collectEntries { [(it.valueExpr): it] }
		inRuleHead = true
		m[n.head] = n.head.accept(this)
		inRuleHead = false
		unboundVar = null
		if (n.body) m[n.body] = n.body.accept(this)

		return actor.exit(n, m)
	}

	IVisitable exit(Constructor n, Map<IVisitable, IVisitable> m) {
		exitRelation(n, n.keyExprs + [n.valueExpr])
	}

	IVisitable exit(Functional n, Map<IVisitable, IVisitable> m) {
		exitRelation(n, n.keyExprs + [n.valueExpr])
	}

	IVisitable exit(Predicate n, Map<IVisitable, IVisitable> m) {
		exitRelation(n, n.exprs)
	}

	IVisitable exitRelation(Relation n, List<IExpr> exprs) {
		if (!inRuleHead || !exprs.find { unboundVar[it] }) return n

		def boundParams = exprs.findAll { !(unboundVar[it]) }
		def types = inferenceActor.inferredTypes[n.name]
		def boundTypes = exprs.withIndex()
				.findAll { expr, int i -> expr in boundParams }
				.collect { expr, int i -> new Predicate(types[i], null, [new VariableExpr("var$i")]) }
		def p = new Predicate("${n.name}__pArTiAl", n.stage, newVars(boundParams.size()))
		newDeclarations << new Declaration(p, boundTypes)

		if (n.name in infoActor.allConstructors && !(n.name in infoActor.refmodeRelations)) {
			def vars = newVars(boundParams.size())
			newRules << new Rule(
					new LogicalElement(new Functional(n.name, n.stage, vars, new VariableExpr('$'))),
					new LogicalElement(new Predicate("${n.name}__pArTiAl", n.stage, vars)))
		}

		return new Predicate("${n.name}__pArTiAl", n.stage, boundParams)
	}

	static def newVars(int n) { (0..<n).collect { new VariableExpr("var$it") } }
}
