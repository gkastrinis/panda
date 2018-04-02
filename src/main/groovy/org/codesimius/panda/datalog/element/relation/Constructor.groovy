package org.codesimius.panda.datalog.element.relation

import org.codesimius.panda.datalog.expr.IExpr

class Constructor extends Relation {

	Constructor(String name, List<IExpr> exprs) { super(name, exprs) }

	def getKeyExprs() { exprs.dropRight(1) }

	def getValueExpr() { exprs.last() }

	String toString() { "C##$name$exprs" }
}
