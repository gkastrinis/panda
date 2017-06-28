package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Constraint
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.CmdComponent
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

class PostOrderVisitor<T> implements IVisitor<T> {

	protected IActor<T> actor
	protected Map<IVisitable, T> m = [:]

	PostOrderVisitor(IActor<T> actor = null) { this.actor = actor }

	T visit(Program n) {
		actor.enter(n)
		m[n.globalComp] = n.globalComp.accept(this)
		n.comps.values().each { m[it] = it.accept(this) }
		return actor.exit(n, m)
	}

	T visit(CmdComponent n) {
		actor.enter(n)
		n.exports.each { m[it] = it.accept(this) }
		n.imports.each { m[it] = it.accept(this) }
		n.declarations.each { m[it] = it.accept(this) }
		n.rules.each { m[it] = it.accept(this) }
		return actor.exit(n, m)
	}

	T visit(Component n) {
		actor.enter(n)
		n.declarations.each { m[it] = it.accept(this) }
		n.constraints.each { m[it] = it.accept(this) }
		n.rules.each { m[it] = it.accept(this) }
		return actor.exit(n, m)
	}

	T visit(Constraint n) {
		actor.enter(n)
		m[n.head] = n.head.accept(this)
		m[n.body] = n.body.accept(this)
		return actor.exit(n, m)
	}

	T visit(Declaration n) {
		actor.enter(n)
		m[n.atom] = n.atom.accept(this)
		n.types.each { m[it] = it.accept(this) }
		return actor.exit(n, m)
	}

	T visit(Rule n) {
		actor.enter(n)
		m[n.head] = n.head.accept(this)
		if (n.body) m[n.body] = n.body.accept(this)
		return actor.exit(n, m)
	}

	T visit(AggregationElement n) {
		actor.enter(n)
		m[n.var] = n.var.accept(this)
		m[n.predicate] = n.predicate.accept(this)
		m[n.body] = n.body.accept(this)
		return actor.exit(n, m)
	}

	T visit(ComparisonElement n) {
		actor.enter(n)
		m[n.expr] = n.expr.accept(this)
		return actor.exit(n, m)
	}

	T visit(GroupElement n) {
		actor.enter(n)
		m[n.element] = n.element.accept(this)
		return actor.exit(n, m)
	}

	T visit(LogicalElement n) {
		actor.enter(n)
		n.elements.each { m[it] = it.accept(this) }
		return actor.exit(n, m)
	}

	T visit(NegationElement n) {
		actor.enter(n)
		m[n.element] = n.element.accept(this)
		return actor.exit(n, m)
	}

	T visit(Relation n) {
		actor.enter(n)
		return actor.exit(n, m)
	}

	T visit(Constructor n) {
		actor.enter(n)
		n.keyExprs.each { m[it] = it.accept(this) }
		if (n.valueExpr) m[n.valueExpr] = n.valueExpr.accept(this)
		m[n.entity] = n.entity.accept(this)
		return actor.exit(n, m)
	}

	T visit(Entity n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) }
		return actor.exit(n, m)
	}

	T visit(Functional n) {
		actor.enter(n)
		n.keyExprs.each { m[it] = it.accept(this) }
		if (n.valueExpr) m[n.valueExpr] = n.valueExpr.accept(this)
		return actor.exit(n, m)
	}

	T visit(Predicate n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) }
		return actor.exit(n, m)
	}

	T visit(Primitive n) {
		actor.enter(n)
		m[n.var] = n.var.accept(this)
		return actor.exit(n, m)
	}

	T visit(BinaryExpr n) {
		actor.enter(n)
		m[n.left] = n.left.accept(this)
		m[n.right] = n.right.accept(this)
		return actor.exit(n, m)
	}

	T visit(ConstantExpr n) {
		actor.enter(n)
		return actor.exit(n, m)
	}

	T visit(GroupExpr n) {
		actor.enter(n)
		m[n.expr] = n.expr.accept(this)
		return actor.exit(n, m)
	}

	T visit(VariableExpr n) {
		actor.enter(n)
		return actor.exit(n, m)
	}
}
