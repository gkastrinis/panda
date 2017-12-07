package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr

@ToString(includeSuper = true, includePackage = false)
class Type extends Relation {

	Type(String name, IExpr expr) { super(name, [expr]) }

	Type(String name) { super(name, []) }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	boolean isPrimitive() {
		switch (name) {
			case "int":
			case "float":
			case "boolean":
			case "string":
				return true
			default:
				return false
		}
	}
}
