package org.clyze.deepdoop.datalog.element.relation

class Type extends Relation {

	Type(String name) { super(name, []) }

	boolean isPrimitive() { name in ["int", "float", "boolean", "string"] }

	String toString() { "T##$name" }

	static final TYPE_STR = new Type("string")
	static final TYPE_INT = new Type("int")
}
