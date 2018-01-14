package org.clyze.deepdoop.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.IExpr

@Canonical
@ToString(includePackage = false)
class ComparisonElement implements IElement {

	BinaryExpr expr

	ComparisonElement(IExpr left, BinOperator op, IExpr right) {
		this.expr = new BinaryExpr(left, op, right)
	}

	ComparisonElement(BinaryExpr expr) {
		this.expr = expr
	}
}
