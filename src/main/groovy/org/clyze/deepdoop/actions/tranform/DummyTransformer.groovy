package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.PostOrderVisitor
import org.clyze.deepdoop.actions.TDummyActor
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.*

class DummyTransformer extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	DummyTransformer() { actor = this }

	IVisitable exit(Program n, Map m) {
		def newComps = n.comps.collectEntries { [(it.key): m[it.value]] } as Map
		new Program(m[n.globalComp] as Component, newComps, n.inits)
	}

	IVisitable exit(Component n, Map m) {
		def ds = n.declarations.collect { m[it] as Declaration } as Set
		def rs = n.rules.collect { m[it] as Rule } as Set
		new Component(n.name, n.superComp, n.parameters, n.superParameters, ds, rs)
	}

	IVisitable exit(Declaration n, Map m) {
		new Declaration(m[n.relation] as Relation, n.types.collect { m[it] as Type }, n.annotations)
	}

	IVisitable exit(Rule n, Map m) {
		new Rule(m[n.head] as LogicalElement, m[n.body] as LogicalElement, n.annotations)
	}

	IVisitable exit(AggregationElement n, Map m) {
		new AggregationElement(m[n.var] as VariableExpr, m[n.relation] as Relation, m[n.body] as LogicalElement)
	}

	IVisitable exit(ComparisonElement n, Map m) {
		new ComparisonElement(m[n.expr] as BinaryExpr)
	}

	IVisitable exit(ConstructionElement n, Map m) {
		new ConstructionElement(m[n.constructor] as Constructor, m[n.type] as Type)
	}

	IVisitable exit(GroupElement n, Map m) {
		new GroupElement(m[n.element] as IElement)
	}

	IVisitable exit(LogicalElement n, Map m) {
		new LogicalElement(n.type, n.elements.collect { m[it] as IElement })
	}

	IVisitable exit(NegationElement n, Map m) {
		new NegationElement(m[n.element] as IElement)
	}

	IVisitable exit(Constructor n, Map m) {
		new Constructor(n.name, n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(Relation n, Map m) {
		new Relation(n.name, n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(Type n, Map m) {
		n.exprs ? new Type(n.name, m[n.exprs.first()] as IExpr) : n
	}

	IVisitable exit(BinaryExpr n, Map m) {
		new BinaryExpr(m[n.left] as IExpr, n.op, m[n.right] as IExpr)
	}

	IVisitable exit(ConstantExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) {
		new GroupExpr(m[n.expr] as IExpr)
	}

	IVisitable exit(VariableExpr n, Map m) { n }
}
