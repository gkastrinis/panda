package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical
import org.clyze.deepdoop.datalog.BinOperator

@Canonical
class BinaryExpr implements IExpr {

	IExpr left
	BinOperator op
	IExpr right

	String toString() { "$left $op $right" }
}
