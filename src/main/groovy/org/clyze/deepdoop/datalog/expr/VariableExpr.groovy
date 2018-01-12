package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitor

@Canonical
class VariableExpr implements IExpr {

	String name

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	String toString() { name }

	static VariableExpr gen1(int i = 0) { new VariableExpr("v$i") }

	static List<VariableExpr> genN(int n) { (0..<n).collect { gen1(it) } }
}
