package org.codesimius.panda.datalog.element.relation

import groovy.transform.Canonical
import org.codesimius.panda.datalog.element.IElement

@Canonical
class Type implements IElement {

	String name

	String toString() { "T##$name" }

	String getDefaultConName() { "$name:byStr" }

	boolean isPrimitive() { isPrimitive name }

	static isPrimitive(String t) { t in ["int", "real", "boolean", "string"] }

	static final TYPE_INT = new Type("int")
	static final TYPE_REAL = new Type("real")
	static final TYPE_BOOLEAN = new Type("boolean")
	static final TYPE_STRING = new Type("string")
}
