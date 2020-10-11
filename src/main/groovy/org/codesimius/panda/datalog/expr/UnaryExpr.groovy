package org.codesimius.panda.datalog.expr

import groovy.transform.Canonical

@Canonical
class UnaryExpr implements IExpr {

	UnaryOp op
	IExpr expr

	String toString() { "$op $expr" }
}