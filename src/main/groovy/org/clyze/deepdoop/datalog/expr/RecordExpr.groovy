package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitor

// Represents a record in Souffle
// NOTE: Should only be used internally during transformations
@Canonical
class RecordExpr implements IExpr {

	List<IExpr> exprs

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	String toString() { "[${exprs.join(", ")}]" }
}
