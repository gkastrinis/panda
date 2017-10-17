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

trait TDummyActor<T> implements IActor<T> {
	void enter(Program n) {}

	T exit(Program n, Map<IVisitable, T> m) { null }

	void enter(CmdComponent n) {}

	T exit(CmdComponent n, Map<IVisitable, T> m) { null }

	void enter(Component n) {}

	T exit(Component n, Map<IVisitable, T> m) { null }

	void enter(Declaration n) {}

	T exit(Declaration n, Map<IVisitable, T> m) { null }

	void enter(Rule n) {}

	T exit(Rule n, Map<IVisitable, T> m) { null }

	void enter(AggregationElement n) {}

	T exit(AggregationElement n, Map<IVisitable, T> m) { null }

	void enter(ComparisonElement n) {}

	T exit(ComparisonElement n, Map<IVisitable, T> m) { null }

	void enter(ConstructionElement n) {}

	T exit(ConstructionElement n, Map<IVisitable, T> m) { null }

	void enter(GroupElement n) {}

	T exit(GroupElement n, Map<IVisitable, T> m) { null }

	void enter(LogicalElement n) {}

	T exit(LogicalElement n, Map<IVisitable, T> m) { null }

	void enter(NegationElement n) {}

	T exit(NegationElement n, Map<IVisitable, T> m) { null }

	void enter(Relation n) {}

	T exit(Relation n, Map<IVisitable, T> m) { null }

	void enter(Constructor n) {}

	T exit(Constructor n, Map<IVisitable, T> m) { null }

	void enter(Type n) {}

	T exit(Type n, Map<IVisitable, T> m) { null }

	void enter(BinaryExpr n) {}

	T exit(BinaryExpr n, Map<IVisitable, T> m) { null }

	void enter(ConstantExpr n) {}

	T exit(ConstantExpr n, Map<IVisitable, T> m) { null }

	void enter(GroupExpr n) {}

	T exit(GroupExpr n, Map<IVisitable, T> m) { null }

	// Handling of RecordExpr is not supported in general since it is reserved for interal use
	// Individual implementations should override this method
	void enter(RecordExpr n) { throw new UnsupportedOperationException() }

	// Handling of RecordExpr is not supported in general since it is reserved for interal use
	// Individual implementations should override this method
	T exit(RecordExpr n, Map<IVisitable, T> m) { throw new UnsupportedOperationException() }

	void enter(VariableExpr n) {}

	T exit(VariableExpr n, Map<IVisitable, T> m) { null }
}
