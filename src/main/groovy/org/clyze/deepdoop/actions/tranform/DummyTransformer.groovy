package org.clyze.deepdoop.actions.tranform

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

class DummyTransformer extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	DummyTransformer() { actor = this }

	IVisitable exit(Program n, Map<IVisitable, IVisitable> m) {
		new Program(m[n.globalComp] as Component, [:], [:], [] as Set)
	}

	IVisitable exit(Component n, Map<IVisitable, IVisitable> m) {
		def ds = n.declarations.collect { m[it] as Declaration } as Set
		def rs = n.rules.collect { m[it] as Rule } as Set
		new Component(n.name, n.superComp, ds, rs)
	}

	IVisitable exit(Declaration n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(Rule n, Map<IVisitable, IVisitable> m) {
		new Rule(m[n.head] as LogicalElement, m[n.body] as LogicalElement, n.annotations)
	}

	IVisitable exit(AggregationElement n, Map<IVisitable, IVisitable> m) {
		new AggregationElement(
				m[n.var] as VariableExpr,
				m[n.predicate] as Predicate,
				m[n.body] as IElement)
	}

	IVisitable exit(ComparisonElement n, Map<IVisitable, IVisitable> m) {
		new ComparisonElement(m[n.expr] as BinaryExpr)
	}

	IVisitable exit(GroupElement n, Map<IVisitable, IVisitable> m) {
		new GroupElement(m[n.element] as IElement)
	}

	IVisitable exit(LogicalElement n, Map<IVisitable, IVisitable> m) {
		new LogicalElement(n.type, n.elements.collect { m[it] as IElement })
	}

	IVisitable exit(NegationElement n, Map<IVisitable, IVisitable> m) {
		new NegationElement(m[n.element] as IElement)
	}

	IVisitable exit(Relation n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(Constructor n, Map<IVisitable, IVisitable> m) {
		def f = new Functional(n.name, n.stage, n.keyExprs.collect { m[it] as IExpr }, m[n.valueExpr] as IExpr)
		new Constructor(f, n.type)
	}

	IVisitable exit(Type n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(Functional n, Map<IVisitable, IVisitable> m) {
		new Functional(n.name, n.stage, n.keyExprs.collect { m[it] as IExpr }, m[n.valueExpr] as IExpr)
	}

	IVisitable exit(Predicate n, Map<IVisitable, IVisitable> m) {
		new Predicate(n.name, n.stage, n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(Primitive n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(BinaryExpr n, Map<IVisitable, IVisitable> m) {
		new BinaryExpr(m[n.left] as IExpr, n.op, m[n.right] as IExpr)
	}

	IVisitable exit(ConstantExpr n, Map<IVisitable, IVisitable> m) { n }

	IVisitable exit(GroupExpr n, Map<IVisitable, IVisitable> m) {
		new GroupExpr(m[n.expr] as IExpr)
	}

	IVisitable exit(VariableExpr n, Map<IVisitable, IVisitable> m) { n }
}
