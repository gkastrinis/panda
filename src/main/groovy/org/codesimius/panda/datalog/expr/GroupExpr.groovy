package org.codesimius.panda.datalog.expr

import groovy.transform.Canonical

@Canonical
class GroupExpr implements IExpr {

	@Delegate
	IExpr expr

	String toString() { "($expr)" }
}
