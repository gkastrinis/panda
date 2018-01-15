package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical

@Canonical
class BinaryExpr implements IExpr {

	IExpr left
	BinaryOp op
	IExpr right

	String toString() { "$left $op $right" }
}
