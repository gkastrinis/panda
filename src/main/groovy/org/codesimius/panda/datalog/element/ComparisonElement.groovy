package org.codesimius.panda.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.BinaryOp
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.datalog.expr.IExpr

@Canonical
@ToString(includePackage = false)
class ComparisonElement implements IElement {

	BinaryExpr expr

	ComparisonElement(IExpr left, BinaryOp op, IExpr right) {
		this.expr = new BinaryExpr(left, op, right)
	}

	ComparisonElement(BinaryExpr expr) {
		this.expr = expr
	}

	static
	final ComparisonElement TRIVIALLY_TRUE = new ComparisonElement(new ConstantExpr(1), BinaryOp.EQ, new ConstantExpr(1))
}
