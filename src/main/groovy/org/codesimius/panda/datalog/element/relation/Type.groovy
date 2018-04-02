package org.codesimius.panda.datalog.element.relation

import groovy.transform.Canonical
import org.codesimius.panda.datalog.element.IElement

@Canonical
class Type implements IElement {

	String name

	boolean isPrimitive() { name in ["int", "float", "boolean", "string"] }

	String toString() { "T##$name" }

	String getDefaultConName() { "$name:byStr" }

	static final TYPE_INT = new Type("int")
	static final TYPE_FLOAT = new Type("float")
	static final TYPE_BOOLEAN = new Type("boolean")
	static final TYPE_STRING = new Type("string")
}
