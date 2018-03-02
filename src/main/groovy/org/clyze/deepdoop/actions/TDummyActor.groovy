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

trait TDummyActor<T> implements IActor<T> {
	void enter(BlockLvl2 n) {}

	T exit(BlockLvl2 n, Map m) { null }

	void enter(BlockLvl1 n) {}

	T exit(BlockLvl1 n, Map m) { null }

	void enter(BlockLvl0 n) {}

	T exit(BlockLvl0 n, Map m) { null }

	void enter(RelDeclaration n) {}

	T exit(RelDeclaration n, Map m) { null }

	void enter(TypeDeclaration n) {}

	T exit(TypeDeclaration n, Map m) { null }

	void enter(Rule n) {}

	T exit(Rule n, Map m) { null }

	void enter(AggregationElement n) {}

	T exit(AggregationElement n, Map m) { null }

	void enter(ComparisonElement n) {}

	T exit(ComparisonElement n, Map m) { null }

	void enter(ConstructionElement n) {}

	T exit(ConstructionElement n, Map m) { null }

	void enter(GroupElement n) {}

	T exit(GroupElement n, Map m) { null }

	void enter(LogicalElement n) {}

	T exit(LogicalElement n, Map m) { null }

	void enter(NegationElement n) {}

	T exit(NegationElement n, Map m) { null }

	void enter(Relation n) {}

	T exit(Relation n, Map m) { null }

	void enter(Constructor n) {}

	T exit(Constructor n, Map m) { null }

	void enter(Type n) {}

	T exit(Type n, Map m) { null }

	void enter(BinaryExpr n) {}

	T exit(BinaryExpr n, Map m) { null }

	void enter(ConstantExpr n) {}

	T exit(ConstantExpr n, Map m) { null }

	void enter(GroupExpr n) {}

	T exit(GroupExpr n, Map m) { null }

	// Handling of RecordExpr is not supported in general since it is reserved for interal use
	// Individual implementations should override this method
	void enter(RecordExpr n) { throw new UnsupportedOperationException() }

	// Handling of RecordExpr is not supported in general since it is reserved for interal use
	// Individual implementations should override this method
	T exit(RecordExpr n, Map m) { throw new UnsupportedOperationException() }

	void enter(VariableExpr n) {}

	T exit(VariableExpr n, Map m) { null }
}
