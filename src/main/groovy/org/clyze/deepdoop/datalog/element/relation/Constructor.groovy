package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr

@ToString(includeSuper = true, includePackage = false)
class Constructor extends Relation {

	Constructor(String name, List<IExpr> exprs) { super(name, null, exprs) }

	def getKeyExprs() { exprs.dropRight(1) }

	def getValueExpr() { exprs.last() }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
