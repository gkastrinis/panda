package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IActor
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.PostOrderVisitor
import org.clyze.deepdoop.actions.TDummyActor
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.*

class DummyTransformer extends PostOrderVisitor<IVisitable> implements IActor<IVisitable>, TDummyActor<IVisitable> {

	DummyTransformer() { actor = this }

	IVisitable exit(Program n, Map<IVisitable, IVisitable> m) {
		def g = m[n.globalComp] as Component
		(n.globalComp != g) ? rec(new Program(g, [:], [:], [] as Set)) : n
	}

	IVisitable exit(Component n, Map<IVisitable, IVisitable> m) {
		(n.rules.find { it != m[it] }) ?
				rec(new Component(n.name, n.superComp, n.declarations, n.constraints, n.rules.collect {
					m[it] as Rule
				} as Set)) : n
	}

	IVisitable exit(Declaration n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(Rule n, Map<IVisitable, IVisitable> m) {
		def head = m[n.head] as LogicalElement
		def body = m[n.body] as LogicalElement
		(n.head != head || n.body != body) ? rec(new Rule(head, body, n.annotations)) : n
	}

	IVisitable exit(AggregationElement n, Map<IVisitable, IVisitable> m) {
		def v = m[n.var] as VariableExpr
		def p = m[n.predicate] as Predicate
		def body = m[n.body] as IElement
		(n.var != v || n.predicate != p || n.body != body) ?
				rec(new AggregationElement(v, p, body)) : n
	}

	IVisitable exit(ComparisonElement n, Map<IVisitable, IVisitable> m) {
		def e = m[n.expr] as BinaryExpr
		(n.expr != e) ? rec(new ComparisonElement(e)) : n
	}

	IVisitable exit(GroupElement n, Map<IVisitable, IVisitable> m) {
		def e = m[n.element] as IElement
		(n.element != e) ? rec(new GroupElement(e)) : n
	}

	IVisitable exit(LogicalElement n, Map<IVisitable, IVisitable> m) {
		(n.elements.find { it != m[it] }) ?
				rec(new LogicalElement(n.type, n.elements.collect { m[it] as IElement })) : n
	}

	IVisitable exit(NegationElement n, Map<IVisitable, IVisitable> m) {
		def e = m[n.element] as IElement
		(n.element != e) ? rec(new NegationElement(e)) : n
	}

	IVisitable exit(Relation n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(Constructor n, Map<IVisitable, IVisitable> m) {
		def f = n as Functional
		if ((n.keyExprs + n.valueExpr).find { it != m[it] })
			f = new Functional(n.name, n.stage, n.keyExprs.collect { m[it] as IExpr }, m[n.valueExpr] as IExpr)
		(n != f) ? rec(new Constructor(f, n.type)) : n
	}

	IVisitable exit(Type n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(Functional n, Map<IVisitable, IVisitable> m) {
		((n.keyExprs + n.valueExpr).find { it != m[it] }) ?
				rec(new Functional(n.name, n.stage, n.keyExprs.collect { m[it] as IExpr }, m[n.valueExpr] as IExpr)) : n
	}

	IVisitable exit(Predicate n, Map<IVisitable, IVisitable> m) {
		(n.exprs.find { it != m[it] }) ?
				rec(new Predicate(n.name, n.stage, n.exprs.collect { m[it] as IExpr })) : n
	}

	IVisitable exit(Primitive n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(BinaryExpr n, Map<IVisitable, IVisitable> m) {
		def l = m[n.left] as IExpr
		def r = m[n.right] as IExpr
		(n.left != l || n.right != r) ? rec(new BinaryExpr(l, n.op, r)) : n
	}

	IVisitable exit(ConstantExpr n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(GroupExpr n, Map<IVisitable, IVisitable> m) {
		def e = m[n.expr] as IExpr
		(n.expr != e) ? rec(new GroupExpr(e)) : n
	}

	IVisitable exit(VariableExpr n, Map<IVisitable, IVisitable> m) { n }

	// Generic method to keep track of changed elements
	// Should be overwritten when needed
	def rec(IVisitable e) { e }
}
