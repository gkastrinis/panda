package org.codesimius.panda.actions

import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.*

import static org.codesimius.panda.datalog.Annotation.METADATA

class DefaultVisitor<T> {

	// The annotation set of a declaration / rule for usage in its children
	Set<Annotation> parentAnnotations
	Map<IVisitable, T> m = [:]
	boolean inDecl
	boolean inRuleHead
	boolean inRuleBody
	BlockLvl2 program
	BlockLvl0 currDatalog

	T visit(BlockLvl2 n) {
		enter n
		program = n
		m[n.datalog] = visit n.datalog
		n.templates.each { m[it] = visit it }
		exit n
	}

	T visit(BlockLvl1 n) {
		enter n
		m[n.datalog] = visit n.datalog
		exit n
	}

	T visit(BlockLvl0 n) {
		enter n
		currDatalog = n
		(n.relDeclarations + n.typeDeclarations + n.rules).each { m[it] = visit it }
		currDatalog = null
		exit n
	}

	T visit(RelDeclaration n) {
		parentAnnotations = n.annotations
		enter n
		inDecl = true
		m[n.relation] = visit n.relation
		n.types.each { m[it] = visit it }
		inDecl = false
		exit n
	}

	T visit(TypeDeclaration n) {
		parentAnnotations = n.annotations
		enter n
		inDecl = true
		m[n.type] = visit n.type
		if (n.supertype) m[n.supertype] = visit n.supertype
		inDecl = false
		exit n
	}

	T visit(Rule n) {
		parentAnnotations = n.annotations
		enter n
		inRuleHead = true
		m[n.head] = visit n.head
		inRuleHead = false
		inRuleBody = true
		if (n.body) m[n.body] = visit n.body
		inRuleBody = false
		exit n
	}

	T visit(IElement n) { null }

	T visit(AggregationElement n) {
		enter n
		m[n.var] = visit n.var
		m[n.relation] = visit n.relation
		m[n.body] = visit n.body
		exit n
	}

	T visit(ComparisonElement n) {
		enter n
		m[n.expr] = visit n.expr
		exit n
	}

	T visit(ConstructionElement n) {
		enter n
		m[n.constructor] = visit n.constructor
		m[n.type] = visit n.type
		exit n
	}

	T visit(LogicalElement n) {
		enter n
		n.elements.each { m[it] = visit it }
		exit n
	}

	T visit(NegationElement n) {
		enter n
		m[n.element] = visit n.element
		exit n
	}

	T visit(Constructor n) {
		enter n
		n.exprs.each { m[it] = visit it }
		exit n
	}

	T visit(Relation n) {
		enter n
		n.exprs.each { m[it] = visit it }
		exit n
	}

	T visit(Type n) {
		enter n
		exit n
	}

	T visit(IExpr n) { null }

	T visit(BinaryExpr n) {
		enter n
		m[n.left] = visit n.left
		m[n.right] = visit n.right
		exit n
	}

	T visit(ConstantExpr n) {
		enter n
		exit n
	}

	T visit(GroupExpr n) {
		enter n
		m[n.expr] = visit n.expr
		exit n
	}

	// Handling of RecordExpr is not supported in general since it is reserved for interal use
	// Individual implementations should override this method
	T visit(RecordExpr n) { throw new UnsupportedOperationException() }

	T visit(VariableExpr n) {
		enter n
		exit n
	}


	void enter(BlockLvl2 n) {}

	T exit(BlockLvl2 n) { null }

	void enter(BlockLvl1 n) {}

	T exit(BlockLvl1 n) { null }

	void enter(BlockLvl0 n) {}

	T exit(BlockLvl0 n) { null }

	void enter(RelDeclaration n) {}

	T exit(RelDeclaration n) { null }

	void enter(TypeDeclaration n) {}

	T exit(TypeDeclaration n) { null }

	void enter(Rule n) {}

	T exit(Rule n) { null }

	void enter(AggregationElement n) {}

	T exit(AggregationElement n) { null }

	void enter(ComparisonElement n) {}

	T exit(ComparisonElement n) { null }

	void enter(ConstructionElement n) {}

	T exit(ConstructionElement n) { null }

	void enter(LogicalElement n) {}

	T exit(LogicalElement n) { null }

	void enter(NegationElement n) {}

	T exit(NegationElement n) { null }

	void enter(Relation n) {}

	T exit(Relation n) { null }

	void enter(Constructor n) {}

	T exit(Constructor n) { null }

	void enter(Type n) {}

	T exit(Type n) { null }

	void enter(BinaryExpr n) {}

	T exit(BinaryExpr n) { null }

	void enter(ConstantExpr n) {}

	T exit(ConstantExpr n) { null }

	void enter(GroupExpr n) {}

	T exit(GroupExpr n) { null }

	void enter(VariableExpr n) {}

	T exit(VariableExpr n) { null }

	def findParentLoc() { if(parentAnnotations) parentAnnotations[METADATA]?.args?.loc }
}
