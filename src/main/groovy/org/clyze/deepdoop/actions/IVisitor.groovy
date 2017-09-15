package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.CmdComponent
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.*

interface IVisitor<T> {
	T visit(Program n)

	T visit(CmdComponent n)

	T visit(Component n)

	T visit(Declaration n)

	T visit(Rule n)

	T visit(AggregationElement n)

	T visit(ComparisonElement n)

	T visit(GroupElement n)

	T visit(LogicalElement n)

	T visit(NegationElement n)

	T visit(Relation n)

	T visit(Constructor n)

	T visit(Type n)

	T visit(Functional n)

	T visit(Predicate n)

	T visit(Primitive n)

	T visit(BinaryExpr n)

	T visit(ConstantExpr n)

	T visit(GroupExpr n)

	T visit(RecordExpr n)

	T visit(VariableExpr n)
}
