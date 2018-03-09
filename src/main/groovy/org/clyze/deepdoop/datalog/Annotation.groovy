package org.clyze.deepdoop.datalog

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.system.Error
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.INTEGER
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.STRING
import static org.clyze.deepdoop.system.Error.error as error

@EqualsAndHashCode(includes = "kind")
@ToString(includePackage = false)
class Annotation {

	String kind
	Map<String, ConstantExpr> args

	Annotation(String name, Map<String, ConstantExpr> args = [:]) {
		name = name.toUpperCase()
		this.kind = VALIDATORS.containsKey(name) ? name : null
		this.args = args
	}

	String toString() { "@$kind${ args ? "$args " : "" }" }

	void validate() { VALIDATORS[kind]?.call(this) }

	static def NO_ARGS_VALIDATOR = { Annotation a ->
		if (!a.args.isEmpty()) error(SourceManager.instance.recall(a), Error.ANNOTATION_NON_EMPTY, a.kind)
	}

	static def MANDATORY_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> mandatory ->
		def loc = SourceManager.instance.recall(a)
		mandatory.findAll { argName, type -> !a.args[argName] }.each {
			error(loc, Error.ANNOTATION_MISSING_ARG, it, a.kind)
		}
		mandatory.findAll { argName, type -> a.args[argName].type != type }.each { argName, type ->
			error(loc, Error.ANNOTATION_MISTYPED_ARG, a.args[argName].type, type, argName, a.kind)
		}
	}

	static def OPTIONAL_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> optional ->
		def loc = SourceManager.instance.recall(a)
		a.args.findAll { argName, value -> !optional[argName] }.each {
			error(loc, Error.ANNOTATION_INVALID_ARG, it.key, a.kind)
		}
		a.args.findAll { argName, value -> optional[argName] != value.type }.each { argName, type ->
			error(loc, Error.ANNOTATION_MISTYPED_ARG, a.args[argName].type, type, argName, a.kind)
		}
	}

	static Map<String, Closure> VALIDATORS = [
			"CONSTRUCTOR": NO_ARGS_VALIDATOR,
			"FUNCTIONAL" : NO_ARGS_VALIDATOR,
			"INPUT"      : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, ["filename": STRING, "delimeter": STRING])
			},
			"OUTPUT"     : NO_ARGS_VALIDATOR,
			"PLAN"       : { Annotation a ->
				MANDATORY_VALIDATOR.call(a, ["plan": STRING])
				OPTIONAL_VALIDATOR.call(a, ["plan": STRING])
			},
			"TYPE"       : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, ["capacity": INTEGER])
			},
			"TYPEVALUES" : {},
	]

	static final CONSTRUCTOR = new Annotation("CONSTRUCTOR")
	static final FUNCTIONAL = new Annotation("FUNCTIONAL")
	static final INPUT = new Annotation("INPUT")
	static final OUTPUT = new Annotation("OUTPUT")
	static final PLAN = new Annotation("PLAN")
	static final TYPE = new Annotation("TYPE")
	static final TYPEVALUES = new Annotation("TYPEVALUES")
}
