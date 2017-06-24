package org.clyze.deepdoop.datalog

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.expr.ConstantExpr

@Canonical
@ToString(includePackage = false)
class Annotation {

	static enum Kind {
		CONSTRAINT,
		CONSTRUCTOR,
		ENTITY,
		INPUT,
		OUTPUT,
		UNDEF
	}

	@Canonical
	@ToString(includePackage = false)
	static class Value {
		String name
		ConstantExpr value
	}

	Kind kind
	List<Value> values

	Annotation(String name, List<Value> values = []) {
		this.kind = findKind(name)
		this.values = values
	}

	static def findKind(String name) {
		name = name.toLowerCase()
		switch (name) {
			case "constraint": return Kind.CONSTRAINT
			case "constructor": return Kind.CONSTRUCTOR
			case "entity": return Kind.ENTITY
			case "input": return Kind.INPUT
			case "output": return Kind.OUTPUT
			default: return Kind.UNDEF
		}
	}
}
