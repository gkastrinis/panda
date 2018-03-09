package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.*

class DefaultVisitor<T> {

	protected IActor<T> actor
	protected Map<IVisitable, T> m = [:]
	protected inDecl = false
	protected inRuleHead = false
	protected inRuleBody = false

	T visit(BlockLvl2 n) {
		actor.enter(n)
		m[n.datalog] = visit n.datalog
		n.components.each { m[it] = visit it }
		actor.exit(n, m)
	}

	T visit(BlockLvl1 n) {
		actor.enter(n)
		m[n.datalog] = visit n.datalog
		actor.exit(n, m)
	}

	T visit(BlockLvl0 n) {
		actor.enter(n)
		(n.relDeclarations + n.typeDeclarations + n.rules).each { m[it] = visit it }
		actor.exit(n, m)
	}

	T visit(RelDeclaration n) {
		actor.enter(n)
		inDecl = true
		m[n.relation] = visit n.relation
		n.types.each { m[it] = visit it }
		inDecl = false
		actor.exit(n, m)
	}

	T visit(TypeDeclaration n) {
		actor.enter(n)
		inDecl = true
		m[n.type] = visit n.type
		if (n.supertype) m[n.supertype] = visit n.supertype
		inDecl = false
		actor.exit(n, m)
	}

	T visit(Rule n) {
		actor.enter(n)
		inRuleHead = true
		m[n.head] = visit n.head
		inRuleHead = false
		inRuleBody = true
		if (n.body) m[n.body] = visit n.body
		inRuleBody = false
		actor.exit(n, m)
	}

	T visit(IElement n) { null }

	T visit(AggregationElement n) {
		actor.enter(n)
		m[n.var] = visit n.var
		m[n.relation] = visit n.relation
		m[n.body] = visit n.body
		actor.exit(n, m)
	}

	T visit(ComparisonElement n) {
		actor.enter(n)
		m[n.expr] = visit n.expr
		actor.exit(n, m)
	}

	T visit(ConstructionElement n) {
		actor.enter(n)
		m[n.constructor] = visit n.constructor
		m[n.type] = visit n.type
		actor.exit(n, m)
	}

	T visit(GroupElement n) {
		actor.enter(n)
		m[n.element] = visit n.element
		actor.exit(n, m)
	}

	T visit(LogicalElement n) {
		actor.enter(n)
		n.elements.each { m[it] = visit it }
		actor.exit(n, m)
	}

	T visit(NegationElement n) {
		actor.enter(n)
		m[n.element] = visit n.element
		actor.exit(n, m)
	}

	T visit(Constructor n) {
		actor.enter(n)
		n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	T visit(Relation n) {
		actor.enter(n)
		n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	T visit(Type n) {
		actor.enter(n)
		//n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	T visit(IExpr n) { null }

	T visit(BinaryExpr n) {
		actor.enter(n)
		m[n.left] = visit n.left
		m[n.right] = visit n.right
		actor.exit(n, m)
	}

	T visit(ConstantExpr n) {
		actor.enter(n)
		actor.exit(n, m)
	}

	T visit(GroupExpr n) {
		actor.enter(n)
		m[n.expr] = visit n.expr
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
