package org.codesimius.panda.datalog

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.*
import static org.codesimius.panda.system.Log.error
import static org.codesimius.panda.system.SourceManager.loc

@EqualsAndHashCode(includes = "kind")
@ToString(includePackage = false)
class Annotation {

	String kind
	Map<String, ConstantExpr> args
	boolean isInternal

	Annotation(String kind, Map<String, ConstantExpr> args = [:], boolean isInternal = false) {
		this.kind = kind.toUpperCase()
		this.args = args
		this.isInternal = isInternal
	}

	Annotation template(Map<String, ConstantExpr> args = [:]) { new Annotation(kind, args, isInternal) }

	ConstantExpr getAt(String key) { args[key] }

	void validate() {
		if (!VALIDATORS.containsKey(kind))
			error(loc(this), Error.ANNOTATION_UNKNOWN, kind)
		VALIDATORS[kind]?.call(this)
	}

	String toString() { "@$kind${args ? "$args " : ""}" }

	static def NO_ARGS_VALIDATOR = { Annotation a ->
		if (!a.args.isEmpty()) error(loc(a), Error.ANNOTATION_NON_EMPTY, a.kind)
	}

	static def MANDATORY_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Kind> mandatory ->
		mandatory.findAll { argName, type -> !a.args[argName] }.each {
			error(loc(a), Error.ANNOTATION_MISSING_ARG, it, a.kind)
		}
		mandatory.findAll { argName, type -> a.args[argName].kind != type }.each { argName, type ->
			error(loc(a), Error.ANNOTATION_MISTYPED_ARG, a.args[argName].kind, type, argName, a.kind)
		}
	}

	static def OPTIONAL_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Kind> optional ->
		a.args.findAll { argName, value -> !optional[argName] }.each {
			error(loc(a), Error.ANNOTATION_INVALID_ARG, it.key, a.kind)
		}
		a.args.findAll { argName, value -> optional[argName] != value.kind }.each { argName, value ->
			error(loc(a), Error.ANNOTATION_MISTYPED_ARG, a.args[argName].kind, optional[argName], argName, a.kind)
		}
	}

	static Map<String, Closure> VALIDATORS = [
			"CONSTRUCTOR": NO_ARGS_VALIDATOR,
			"FUNCTIONAL" : NO_ARGS_VALIDATOR,
			"INPUT"      : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, [filename: STRING, delimeter: STRING])
			},
			"NAMESPACE"  : { Annotation a ->
				MANDATORY_VALIDATOR.call(a, [v: STRING])
			},
			"OUTPUT"     : NO_ARGS_VALIDATOR,
			"PLAN"       : { Annotation a ->
				MANDATORY_VALIDATOR.call(a, [plan: STRING])
				OPTIONAL_VALIDATOR.call(a, [plan: STRING])
			},
			"TEXT"       : NO_ARGS_VALIDATOR,
			"TYPE"       : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, [capacity: INTEGER, opt: BOOLEAN])
			},
			"TYPEVALUES" : {},
			"METADATA"   : {}
	]

	static final CONSTRUCTOR = new Annotation("CONSTRUCTOR")
	static final FUNCTIONAL = new Annotation("FUNCTIONAL")
	static final INPUT = new Annotation("INPUT")
	static final NAMESPACE = new Annotation("NAMESPACE")
	static final OUTPUT = new Annotation("OUTPUT")
	static final PLAN = new Annotation("PLAN")
	static final TEXT = new Annotation("TEXT")
	static final TYPE = new Annotation("TYPE")
	static final TYPEVALUES = new Annotation("TYPEVALUES")
	static final METADATA = new Annotation("METADATA", [:], true)
}
