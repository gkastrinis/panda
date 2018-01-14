package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical

@Canonical
class VariableExpr implements IExpr {

	String name

	String toString() { name }

	static VariableExpr gen1(int i = 0) { new VariableExpr("v$i") }

	static List<VariableExpr> genN(int n) { (0..<n).collect { gen1(it) } }
}
