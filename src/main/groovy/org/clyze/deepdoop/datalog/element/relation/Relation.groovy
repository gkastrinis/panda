package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.expr.IExpr

@Canonical
@ToString(includePackage = false)
@TupleConstructor
class Relation implements IElement {

	String name

	List<IExpr> exprs = []

	int getArity() { exprs.size() }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
