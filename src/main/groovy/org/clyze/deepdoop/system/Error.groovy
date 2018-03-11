package org.clyze.deepdoop.system

import org.apache.commons.logging.LogFactory

import java.text.MessageFormat

enum Error {
	ANNOTATION_NON_EMPTY,
	ANNOTATION_MISSING_ARG,
	ANNOTATION_INVALID_ARG,
	ANNOTATION_MISTYPED_ARG,
	ANNOTATION_MULTIPLE,
	ANNOTATION_INVALID,

	COMP_UNKNOWN,
	COMP_ID_IN_USE,
	COMP_DUPLICATE_PARAMS,
	COMP_SUPER_PARAM_MISMATCH,
	COMP_INST_ARITY,
	COMP_UNKNOWN_PARAM,
	INST_ID_IN_USE,

	DECL_MALFORMED,
	DECL_MULTIPLE,
	CONSTR_NON_FUNC,
	FUNC_NON_CONSTR,
	REL_ARITY,
	REL_EXT_INVALID,
	REL_EXT_NO_DECL,
	CONSTR_RULE_CYCLE,
	CONSTR_UNKNOWN,
	CONSTR_TYPE_INCOMP,
	VAR_MULTIPLE_CONSTR,
	VAR_UNKNOWN,
	VAR_UNUSED,
	VAR_ASGN_CYCLE,
	VAR_ASGN_COMPLEX,

	TYPE_INFERENCE_FAIL,
	TYPE_INFERENCE_FIXED,
	TYPE_INCOMP_EXPR,
	TYPE_INCOMP,
	TYPE_UNSUPP,
	TYPE_UNKNOWN,
	TYPE_RULE,


//	MULTIPLE_ENT_DECLS,
//	DEP_CYCLE,
//	DEP_GLOBAL,
//	CMD_DIRECTIVE,
//	CMD_EVAL,
//	CMD_RULE,
//	CMD_NO_DECL,
//	CMD_NO_IMPORT,

	static Map<Error, String> msgMap
	static {
		msgMap = new EnumMap<>(Error.class)
		msgMap[ANNOTATION_NON_EMPTY] = "Annotation `{0}` takes no arguments"
		msgMap[ANNOTATION_MISSING_ARG] = "Missing mandatory argument `{0}` for annotation `{1}`"
		msgMap[ANNOTATION_INVALID_ARG] = "Invalid argument `{0}` for annotation `{1}`"
		msgMap[ANNOTATION_MISTYPED_ARG] = "Type mismatch ({0} instead of {1}) for argument `{2}` of annotation `{3}`"
		msgMap[ANNOTATION_MULTIPLE] = "Annotation `{0}` appears multiple times in clause"
		msgMap[ANNOTATION_INVALID] = "Invalid annotation `{0}` for `{1}`"

		msgMap[COMP_UNKNOWN] = "Unknown component name `{0}`"
		msgMap[COMP_ID_IN_USE] = "Component name `{0}` already in use"
		msgMap[COMP_DUPLICATE_PARAMS] = "Duplicate parameters ({0}) in component `{1}`"
		msgMap[COMP_SUPER_PARAM_MISMATCH] = "Super component parameters ({0}) not matching parameters ({1}) of component `{2}`"
		msgMap[COMP_INST_ARITY] = "Wrong arity for instantiation parameters ({0}) of component `{1}` as `{2}`"
		msgMap[COMP_UNKNOWN_PARAM] = "Unknown `@` parameter `{0}` used in component"
		msgMap[INST_ID_IN_USE] = "Instantiation name `{0}` already in use"

		msgMap[DECL_MALFORMED] = "Number of variables and types in declaration not matching"
		msgMap[DECL_MULTIPLE] = "Multiple declarations for relation or type `{0}`"
		msgMap[CONSTR_NON_FUNC] = "Constructor `{0}` must use functional syntax"
		msgMap[FUNC_NON_CONSTR] = "Functional syntax available only for constructors (`{0}`)"
		msgMap[REL_ARITY] = "Inconsistent arity for relation `{0}`"
		msgMap[REL_EXT_INVALID] = "Relation with a `@` parameter only allowed in a rule body, inside a component"
		msgMap[REL_EXT_NO_DECL] = "Unknown relation `{0}` used with a `@` parameter"
		msgMap[CONSTR_RULE_CYCLE] = "Constructor `{0}` appears in a cycle in rule head"
		msgMap[CONSTR_UNKNOWN] = "Unknown constructor `{0}`"
		msgMap[CONSTR_TYPE_INCOMP] = "Constructor `{0}` used with incompatible type `{1}`"
		msgMap[VAR_MULTIPLE_CONSTR] = "Var `{0}` constructed by multiple constructors in rule head"
		msgMap[VAR_UNKNOWN] = "Unknown var `{0}`"
		msgMap[VAR_UNUSED] = "Unused var `{0}`"
		msgMap[VAR_ASGN_CYCLE] = "Assignment on var `{0}` is part of an assignment cycle"
		msgMap[VAR_ASGN_COMPLEX] = "Assignment on var `{0}` is part of a complicated logical structure"

		msgMap[TYPE_INFERENCE_FAIL] = "Type inference was inconclusive: cannot reach fixpoint"
		msgMap[TYPE_INFERENCE_FIXED] = "Type inference in conflict with declared type `{0}` (at index {1}) for relation `{2}`"
		msgMap[TYPE_INCOMP_EXPR] = "Incompatible types for numeric expression"
		msgMap[TYPE_INCOMP] = "Incompatible types for relation `{0}` (at index {1})"
		msgMap[TYPE_UNSUPP] = "Currently unsupported type `{0}`"
		msgMap[TYPE_UNKNOWN] = "Unknown type `{0}`"
		msgMap[TYPE_RULE] = "Type `{0}` used as a normal relation in rule head"


//		msgMap[MULTIPLE_ENT_DECLS] = "Multiple declarations for Type `{0}` in previous components"
//		msgMap[DEP_CYCLE] = "Cycle detected in the dependency graph of components"
//		msgMap[DEP_GLOBAL] = "Reintroducing relation `{0}` to global space"
//		msgMap[CMD_DIRECTIVE] = "Invalid directive in command block `{0}`"
//		msgMap[CMD_EVAL] = "EVAL property already specified in command block `{0}`"
//		msgMap[CMD_RULE] = "Normal rules are not supported in a command block"
//		msgMap[CMD_NO_DECL] = "Relation `{0}` is imported but has no declaration"
//		msgMap[CMD_NO_IMPORT] = "Relation `{0}` is declared but not imported"
	}

	static void warn(SourceLocation loc = null, Error errorId, Object... values) {
		def msg = "[DD] WARNING: ${MessageFormat.format(msgMap.get(errorId), values)}"
		if (loc) msg = "$msg\n$loc"
		LogFactory.getLog(Error.class).warn(msg)
	}

	static void error(SourceLocation loc = null, Error errorId, Object... values) {
		def msg = "[DD] ERROR: ${MessageFormat.format(msgMap.get(errorId), values)}"
		if (loc) msg = "$msg\n$loc"
		throw new DeepDoopException(msg, errorId)
	}
}
