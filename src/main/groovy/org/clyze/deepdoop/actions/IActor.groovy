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

interface IActor<T> {
	void enter(BlockLvl2 n)

	T exit(BlockLvl2 n, Map<IVisitable, T> m)

	void enter(BlockLvl1 n)

	T exit(BlockLvl1 n, Map<IVisitable, T> m)

	void enter(BlockLvl0 n)

	T exit(BlockLvl0 n, Map<IVisitable, T> m)

	void enter(RelDeclaration n)

	T exit(RelDeclaration n, Map<IVisitable, T> m)

	void enter(TypeDeclaration n)

	T exit(TypeDeclaration n, Map<IVisitable, T> m)

	void enter(Rule n)

	T exit(Rule n, Map<IVisitable, T> m)

	void enter(AggregationElement n)

	T exit(AggregationElement n, Map<IVisitable, T> m)

	void enter(ComparisonElement n)

	T exit(ComparisonElement n, Map<IVisitable, T> m)

	void enter(ConstructionElement n)

	T exit(ConstructionElement n, Map<IVisitable, T> m)

	void enter(GroupElement n)

	T exit(GroupElement n, Map<IVisitable, T> m)

	void enter(LogicalElement n)

	T exit(LogicalElement n, Map<IVisitable, T> m)

	void enter(NegationElement n)

	T exit(NegationElement n, Map<IVisitable, T> m)

	void enter(Relation n)

	T exit(Relation n, Map<IVisitable, T> m)

	void enter(Constructor n)

	T exit(Constructor n, Map<IVisitable, T> m)

	void enter(Type n)

	T exit(Type n, Map<IVisitable, T> m)

	void enter(BinaryExpr n)

	T exit(BinaryExpr n, Map<IVisitable, T> m)

	void enter(ConstantExpr n)

	T exit(ConstantExpr n, Map<IVisitable, T> m)

	void enter(GroupExpr n)

	T exit(GroupExpr n, Map<IVisitable, T> m)

	void enter(RecordExpr n)

	T exit(RecordExpr n, Map<IVisitable, T> m)

	void enter(VariableExpr n)

	T exit(VariableExpr n, Map<IVisitable, T> m)
}
