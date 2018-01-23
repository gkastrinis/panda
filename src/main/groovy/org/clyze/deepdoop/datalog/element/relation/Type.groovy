package org.clyze.deepdoop.datalog.element.relation

import org.clyze.deepdoop.datalog.expr.IExpr

class Type extends Relation {

	Type(String name, IExpr expr) { super(name, [expr]) }

	Type(String name) { super(name, []) }

	boolean isPrimitive() { name in ["int", "float", "boolean", "string"] }

	String toString() { "T##$name" }
}
