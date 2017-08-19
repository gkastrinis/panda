package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

class SouffleTransformer extends DummyTransformer {

	boolean inRule

	boolean inRuleHead

	Map<String, IExpr> varGetsExpr

	ComparisonElement dummyComparison

	SouffleTransformer() {
		actor = this

		def one = new ConstantExpr(1)
		dummyComparison = new ComparisonElement(one, BinOperator.EQ, one)
	}


	IVisitable exit(Program n, Map<IVisitable, IVisitable> m) {
		new Program(m[n.globalComp] as Component, n.comps, n.inits, n.props)
	}

	IVisitable exit(Component n, Map<IVisitable, IVisitable> m) {
		new Component(n.name, n.superComp, n.declarations, n.constraints, n.rules.collect { m[it] as Rule } as Set)
	}

	// First visit body and keep track when in head
	IVisitable visit(Rule n) {
		actor.enter(n)
		if (n.body) m[n.body] = n.body.accept(this)
		inRuleHead = true
		m[n.head] = n.head.accept(this)
		inRuleHead = false
		return actor.exit(n, m)
	}

	void enter(Rule n) {
		inRule = true
		varGetsExpr = [:]
	}

	IVisitable exit(Rule n, Map<IVisitable, IVisitable> m) {
		inRule = false
		def head = (n.head != m[n.head]) ? new LogicalElement(n.head.type, n.head.elements.collect {
			m[it] as IElement
		}) : n.head
		def body = (n.body != m[n.body]) ? new LogicalElement(n.body.type, n.body.elements.collect {
			m[it] as IElement
		}) : n.body
		if (head != n.head || body != n.body)
			new Rule(head, body, n.annotations)
		else return n
	}

	IVisitable exit(AggregationElement n, Map<IVisitable, IVisitable> m) {
		n
	}

	IVisitable exit(ComparisonElement n, Map<IVisitable, IVisitable> m) {
		if (n.expr instanceof BinaryExpr && n.expr.op == BinOperator.EQ && n.expr.left instanceof VariableExpr) {
			varGetsExpr[(n.expr.left as VariableExpr).name] = n.expr.right
			return dummyComparison
		} else return n
	}

	IVisitable exit(GroupElement n, Map<IVisitable, IVisitable> m) {
		def t = m[n.element] as IElement
		n.element != t ? new GroupElement(t) : n
	}

	IVisitable exit(LogicalElement n, Map<IVisitable, IVisitable> m) {
		if (n.elements.find { it != m[it] }) {
			def r = new LogicalElement(n.type, n.elements.collect { m[it] as IElement })
			return r
		} else return n
	}

	IVisitable exit(NegationElement n, Map<IVisitable, IVisitable> m) {
		def t = m[n.element] as IElement
		n.element != t ? new NegationElement(t) : n
	}

	IVisitable exit(Constructor n, Map<IVisitable, IVisitable> m) {
		n
	}

	IVisitable exit(Functional n, Map<IVisitable, IVisitable> m) {
		inRuleHead ? new Functional(n.name, n.stage, n.keyExprs.collect { map(it) }, map(n.valueExpr)) : n
	}

	IVisitable exit(Predicate n, Map<IVisitable, IVisitable> m) {
		inRuleHead ? new Predicate(n.name, n.stage, n.exprs.collect { map(it) }) : n
	}

	def map(def e) { (e instanceof VariableExpr) ? (varGetsExpr[e.name] ?: e) : e }
}
