package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr

@Canonical
@EqualsAndHashCode(callSuper = true)
@TupleConstructor(callSuper = true, includeSuperProperties = true)
class Functional extends Relation {

	List<IExpr> keyExprs
	IExpr valueExpr

	int getArity() { (keyExprs ? keyExprs.size() : 0) + 1 }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	String toString() { "$name[${keyExprs.join(", ")}] = $valueExpr" }
}
