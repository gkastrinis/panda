package org.codesimius.panda.datalog.expr

enum UnaryOp {
	// String
	TO_STR("TO_STR"), TO_ORD("#")

	private String op

	UnaryOp(String op) { this.op = op }

	String toString() { op }
}
