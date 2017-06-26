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
		PLAN,
		UNDEF
	}

	Kind kind
	Map<String, ConstantExpr> values

	Annotation(String name, Map<String, ConstantExpr> values = [:]) {
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
			case "plan": return Kind.PLAN
			default: return Kind.UNDEF
		}
	}
}
