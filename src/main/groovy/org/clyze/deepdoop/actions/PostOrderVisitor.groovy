package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.CmdComponent
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.*

class PostOrderVisitor<T> implements IVisitor<T> {

	protected IActor<T> actor
	protected Map<IVisitable, T> m = [:]
	protected inRuleHead = false
	protected inRuleBody = false

	protected def getInRule() { inRuleHead || inRuleBody }

	PostOrderVisitor(IActor<T> actor = null) { this.actor = actor }

	T visit(Program n) {
		actor.enter(n)
		m[n.globalComp] = n.globalComp.accept(this) as T
		n.comps.values().each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(CmdComponent n) {
		actor.enter(n)
		n.exports.each { m[it] = it.accept(this) as T }
		n.imports.each { m[it] = it.accept(this) as T }
		n.declarations.each { m[it] = it.accept(this) as T }
		n.rules.each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(Component n) {
		actor.enter(n)
		n.declarations.each { m[it] = it.accept(this) as T }
		n.rules.each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(Declaration n) {
		actor.enter(n)
		m[n.atom] = n.atom.accept(this) as T
		n.types.each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(Rule n) {
		actor.enter(n)
		inRuleHead = true
		m[n.head] = n.head.accept(this) as T
		inRuleHead = false
		inRuleBody = true
		m[n.body] = n.body?.accept(this) as T
		inRuleBody = false
		actor.exit(n, m)
	}

	T visit(AggregationElement n) {
		actor.enter(n)
		m[n.var] = n.var.accept(this) as T
		m[n.relation] = n.relation.accept(this) as T
		m[n.body] = n.body.accept(this) as T
		actor.exit(n, m)
	}

	T visit(ComparisonElement n) {
		actor.enter(n)
		m[n.expr] = n.expr.accept(this) as T
		actor.exit(n, m)
	}

	T visit(ConstructionElement n) {
		actor.enter(n)
		m[n.constructor] = n.constructor.accept(this) as T
		m[n.type] = n.type.accept(this) as T
		actor.exit(n, m)
	}

	T visit(GroupElement n) {
		actor.enter(n)
		m[n.element] = n.element.accept(this) as T
		actor.exit(n, m)
	}

	T visit(LogicalElement n) {
		actor.enter(n)
		n.elements.each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(NegationElement n) {
		actor.enter(n)
		m[n.element] = n.element.accept(this) as T
		actor.exit(n, m)
	}

	T visit(Constructor n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(Relation n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(Type n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) as T }
		actor.exit(n, m)
	}

	T visit(BinaryExpr n) {
		actor.enter(n)
		m[n.left] = n.left.accept(this) as T
		m[n.right] = n.right.accept(this) as T
		actor.exit(n, m)
	}

	T visit(ConstantExpr n) {
		actor.enter(n)
		actor.exit(n, m)
	}

	T visit(GroupExpr n) {
		actor.enter(n)
		m[n.expr] = n.expr.accept(this) as T
		actor.exit(n, m)
	}

	// Handling of RecordExpr is not supported in general since it is reserved for interal use
	// Individual implementations should override this method
	T visit(RecordExpr n) { throw new UnsupportedOperationException() }

	T visit(VariableExpr n) {
		actor.enter(n)
		actor.exit(n, m)
	}
}
