package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr

@ToString(includeSuper = true, includePackage = false)
class Type extends Relation {

	// Initial values are of the form `key(value)`. E.g., PUBLIC('public')
	// Keys are used to generate singleton relations. E.g., Modifier:PUBLIC(x)
	Map<String, String> initValues

	Type(String name, Map<String, String> initValues = [:]) {
		super(name, [])
		this.initValues = initValues
	}

	Type(String name, IExpr expr) { super(name, [expr]) }

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
