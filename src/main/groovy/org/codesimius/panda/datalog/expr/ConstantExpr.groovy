package org.codesimius.panda.datalog.expr

import groovy.transform.Canonical

@Canonical
class ConstantExpr implements IExpr {

	enum Kind {
		INTEGER, REAL, BOOLEAN, STRING, SMART_LIT
	}

	Kind kind
	Object value

	ConstantExpr(Long l) {
		kind = Kind.INTEGER
		value = l
	}

	ConstantExpr(Double r) {
		kind = Kind.REAL
		value = r
	}

	ConstantExpr(Boolean b) {
		kind = Kind.BOOLEAN
		value = b
	}

	ConstantExpr(String s, boolean isSmartLiteral = false) {
		kind = isSmartLiteral ? Kind.SMART_LIT : Kind.STRING
		value = s.startsWith("'") ? s.replaceAll(/^'|'$/, '"') : s
	}

	boolean asBoolean() { value }

	String toString() { value }

	private ConstantExpr() { value = "nil" }

	static final NIL = new ConstantExpr()
}
