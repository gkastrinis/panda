package org.clyze.deepdoop.actions

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

interface IVisitor<T> {
	T visit(BlockLvl2 n)

	T visit(BlockLvl1 n)

	T visit(BlockLvl0 n)

	T visit(RelDeclaration n)

	T visit(TypeDeclaration n)

	T visit(Rule n)

	T visit(IElement n)

	T visit(AggregationElement n)

	T visit(ComparisonElement n)

	T visit(ConstructionElement n)

	T visit(GroupElement n)

	T visit(LogicalElement n)

	T visit(NegationElement n)

	T visit(Constructor n)

	T visit(Relation n)

	T visit(Type n)

	T visit(IExpr n)

	T visit(BinaryExpr n)

	T visit(ConstantExpr n)

	T visit(GroupExpr n)

	T visit(RecordExpr n)

	T visit(VariableExpr n)
}
