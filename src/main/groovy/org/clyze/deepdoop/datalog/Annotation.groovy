package org.clyze.deepdoop.datalog

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceLocation

import static org.clyze.deepdoop.datalog.Annotation.Kind.*

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

	void validate(SourceLocation loc) { VALIDATORS[kind].call(this, loc) }

	private static def findKind(String name) {
		name = name.toLowerCase()
		switch (name) {
			case "constraint": return CONSTRAINT
			case "constructor": return CONSTRUCTOR
			case "entity": return ENTITY
			case "input": return INPUT
			case "output": return OUTPUT
			case "plan": return PLAN
			default: return UNDEF
		}
	}

	private static final def EMPTY_VALIDATOR = { Annotation a, SourceLocation loc ->
		if (!a.values.isEmpty()) ErrorManager.error(loc, ErrorId.NON_EMPTY_ANNOTATION, a.kind)
	}

	private static final def MANDATORY_VALIDATOR = { Annotation a, List<String> mandatory, SourceLocation loc ->
		mandatory.findAll { !(it in a.values) }.each {
			ErrorManager.error(loc, ErrorId.MISSING_ARG_ANNOTATION, it, a.kind)
		}
	}

	private static final def OPTIONAL_VALIDATOR = { Annotation a, List<String> optional, SourceLocation loc ->
		a.values.findAll { !(it.key in optional) }.each {
			ErrorManager.error(loc, ErrorId.INVALID_ARG_ANNOTATION, it.key, a.kind)
		}
	}

	private static final Map<Kind, Closure> VALIDATORS = [
			(CONSTRAINT) : EMPTY_VALIDATOR,
			(CONSTRUCTOR): { Annotation a, SourceLocation loc ->
				OPTIONAL_VALIDATOR.call(a, ["refmode"], loc)
			},
			(ENTITY)     : EMPTY_VALIDATOR,
			(INPUT)      : EMPTY_VALIDATOR,
			(OUTPUT)     : EMPTY_VALIDATOR,
			(PLAN)       : { Annotation a, SourceLocation loc ->
				MANDATORY_VALIDATOR.call(a, ["val"], loc)
				OPTIONAL_VALIDATOR.call(a, ["val"], loc)
			},
	]
}
