package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.BinOperator

@Canonical
@ToString(includePackage = false)
class BinaryExpr implements IExpr {

	IExpr left
	BinOperator op
	IExpr right

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
