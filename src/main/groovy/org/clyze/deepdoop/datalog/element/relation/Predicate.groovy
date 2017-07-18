package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr

@Canonical
@EqualsAndHashCode(callSuper = true)
@TupleConstructor(callSuper = true, includeSuperProperties = true)
class Predicate extends Relation {

	List<IExpr> exprs

	int getArity() { exprs ? exprs.size() : 0 }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	String toString() { "$name(${exprs.join(", ")})" }
}
