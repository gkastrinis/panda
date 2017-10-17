package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.expr.IExpr

@ToString(includePackage = false)
@TupleConstructor
class Relation implements IElement {

	String name

	String stage = null

	List<IExpr> exprs

	int getArity() { exprs ? exprs.size() : 0 }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
