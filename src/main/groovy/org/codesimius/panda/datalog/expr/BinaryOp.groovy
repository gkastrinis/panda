package org.codesimius.panda.datalog.expr

enum BinaryOp {
	// Comparisons
	EQ("="), LT("<"), LEQ("<="),
	GT(">"), GEQ(">="), NEQ("!="),
	// Arithmetic
	PLUS("+"), MINUS("-"), MULT("*"), DIV("/"),
	// String
	CAT("CAT")

	private String op

	BinaryOp(String op) { this.op = op }

	String toString() { op }
}
