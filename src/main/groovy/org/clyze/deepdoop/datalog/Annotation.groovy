package org.clyze.deepdoop.datalog

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.INTEGER
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.STRING

@EqualsAndHashCode(includes = "kind")
@ToString(includePackage = false)
class Annotation {

	public static final CONSTRUCTOR = new Annotation("CONSTRUCTOR")
	public static final FUNCTIONAL = new Annotation("FUNCTIONAL")
	public static final INPUT = new Annotation("INPUT")
	public static final OUTPUT = new Annotation("OUTPUT")
	public static final PLAN = new Annotation("PLAN")
	public static final TYPE = new Annotation("TYPE")

	String kind
	Map<String, ConstantExpr> args

	Annotation(String name, Map<String, ConstantExpr> values = [:]) {
		name = name.toUpperCase()
		switch (name) {
			case "CONSTRUCTOR":
			case "FUNCTIONAL":
			case "INPUT":
			case "OUTPUT":
			case "PLAN":
			case "TYPE":
				this.kind = name
				break
			default:
				this.kind = null
		}
		this.args = values
		this.args.values().each {
			if (it.type == STRING)
				it.value = (it.value as String)[1..-2]
		}
	}

	void validate() { VALIDATORS[kind].call(this) }

	static def EMPTY_VALIDATOR = { Annotation a ->
		def loc = SourceManager.instance.recall(a)
		if (!a.args.isEmpty()) ErrorManager.error(loc, ErrorId.ANNOTATION_NON_EMPTY, a.kind)
	}

	static def MANDATORY_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> mandatory ->
		def loc = SourceManager.instance.recall(a)
		mandatory.findAll { argName, type -> !a.args[argName] }.each {
			ErrorManager.error(loc, ErrorId.ANNOTATION_MISSING_ARG, it, a.kind)
		}
		mandatory.findAll { argName, type -> a.args[argName].type != type }.each { argName, type ->
			ErrorManager.error(loc, ErrorId.ANNOTATION_MISTYPED_ARG, a.args[argName].type, type, argName, a.kind)
		}
	}

	static def OPTIONAL_VALIDATOR = { Annotation a, Map<String, ConstantExpr.Type> optional ->
		def loc = SourceManager.instance.recall(a)
		a.args.findAll { argName, value -> !optional[argName] }.each {
			ErrorManager.error(loc, ErrorId.ANNOTATION_INVALID_ARG, it.key, a.kind)
		}
		a.args.findAll { argName, value -> optional[argName] != value.type }.each { argName, type ->
			ErrorManager.error(loc, ErrorId.ANNOTATION_MISTYPED_ARG, a.args[argName].type, type, argName, a.kind)
		}
	}

	static Map<String, Closure> VALIDATORS = [
			"CONSTRUCTOR": EMPTY_VALIDATOR,
			"FUNCTIONAL" : EMPTY_VALIDATOR,
			"INPUT"      : EMPTY_VALIDATOR,
			"OUTPUT"     : EMPTY_VALIDATOR,
			"PLAN"       : { Annotation a ->
				MANDATORY_VALIDATOR.call(a, ["plan": STRING])
				OPTIONAL_VALIDATOR.call(a, ["plan": STRING])
			},
			"TYPE"       : { Annotation a ->
				OPTIONAL_VALIDATOR.call(a, ["capacity": INTEGER])
			},
	]
}
