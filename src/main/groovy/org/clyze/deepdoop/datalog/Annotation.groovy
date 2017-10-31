package org.clyze.deepdoop.datalog

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.Annotation.Kind.*
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.*

@Canonical
@ToString(includePackage = false)
class Annotation {

	static enum Kind {
		CONSTRUCTOR,
		FUNCTIONAL,
		INPUT,
		OUTPUT,
		PLAN,
		TYPE,
		UNDEF
	}

	Kind kind
	Map<String, ConstantExpr> args

	Annotation(String name, Map<String, ConstantExpr> values = [:]) {
		this.kind = findKind(name)
		this.args = values
		this.args.values().each {
			if (it.type == STRING)
				it.value = (it.value as String)[1..-2]
		}
	}

	void validate() { VALIDATORS[kind].call(this) }

	private static def findKind(String name) {
		name = name.toLowerCase()
		switch (name) {
			case "constructor": return CONSTRUCTOR
			case "functional": return FUNCTIONAL
			case "input": return INPUT
			case "output": return OUTPUT
			case "plan": return PLAN
			case "type": return TYPE
			default: return UNDEF
		}
	}

	private static final def EMPTY_VALIDATOR = { Annotation a ->
		def loc = SourceManager.instance.recall(a)
		if (!a.args.isEmpty()) ErrorManager.error(loc, ErrorId.ANNOTATION_NON_EMPTY, a.kind)
	}

	private static final def MANDATORY_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> mandatory ->
		def loc = SourceManager.instance.recall(a)
		mandatory.findAll { argName, type -> !a.args[argName] }.each {
			ErrorManager.error(loc, ErrorId.ANNOTATION_MISSING_ARG, it, a.kind)
		}
		mandatory.findAll { argName, type -> a.args[argName].type != type }.each { argName, type ->
			ErrorManager.error(loc, ErrorId.ANNOTATION_MISTYPED_ARG, a.args[argName].type, type, argName, a.kind)
		}
	}

	private static final def OPTIONAL_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> optional ->
		def loc = SourceManager.instance.recall(a)
		a.args.findAll { argName, value -> !optional[argName] }.each {
			ErrorManager.error(loc, ErrorId.ANNOTATION_INVALID_ARG, it.key, a.kind)
		}
		a.args.findAll { argName, value -> optional[argName] != value.type }.each { argName, type ->
			ErrorManager.error(loc, ErrorId.ANNOTATION_MISTYPED_ARG, a.args[argName].type, type, argName, a.kind)
		}
	}

	private static final Map<Kind, Closure> VALIDATORS = [
			(CONSTRUCTOR): EMPTY_VALIDATOR,
			(FUNCTIONAL) : EMPTY_VALIDATOR,
			(INPUT)      : EMPTY_VALIDATOR,
			(OUTPUT)     : EMPTY_VALIDATOR,
			(PLAN)       : { Annotation a ->
				MANDATORY_VALIDATOR.call(a, ["plan": STRING])
				OPTIONAL_VALIDATOR.call(a, ["plan": STRING])
			},
			(TYPE)       : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, ["capacity": INTEGER])
			},
	]
}
