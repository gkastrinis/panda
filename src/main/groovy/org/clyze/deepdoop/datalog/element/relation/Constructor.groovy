package org.clyze.deepdoop.datalog.element.relation

import org.clyze.deepdoop.datalog.expr.IExpr

class Constructor extends Relation {

	Constructor(String name, List<IExpr> exprs) { super(name, exprs) }

	def getKeyExprs() { exprs.dropRight(1) }

	def getValueExpr() { exprs.last() }

	String toString() { "CON##$name$exprs" }
}
