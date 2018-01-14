package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical

// Represents a record in Souffle
// NOTE: Should only be used internally during transformations
@Canonical
class RecordExpr implements IExpr {

	List<IExpr> exprs

	String toString() { "[${exprs.join(", ")}]" }
}
