package org.codesimius.panda.datalog.expr

import groovy.transform.Canonical

@Canonical
class ConstantExpr implements IExpr {

	enum Type {
		INTEGER, REAL, BOOLEAN, STRING
	}

	Type type
	Object value

	ConstantExpr(Long l) {
		type = Type.INTEGER
		value = l
	}

	ConstantExpr(Double r) {
		type = Type.REAL
		value = r
	}

	ConstantExpr(Boolean b) {
		type = Type.BOOLEAN
		value = b
	}

	ConstantExpr(String s) {
		type = Type.STRING
		value = s.startsWith("'") ? s.replaceAll(/^'|'$/, '"') : s
	}

	String toString() { value }
}
