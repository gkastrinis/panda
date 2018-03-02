package org.clyze.deepdoop.system

import java.text.MessageFormat

enum ErrorId {
	ID_IN_USE,
	COMP_ID_IN_USE,
	COMP_UNKNOWN,
	COMP_DUPLICATE_PARAMS,
	COMP_SUPER_PARAM_MISMATCH,
	COMP_INST_ARITY,
	ANNOTATION_INVALID,
	ANNOTATION_INVALID_ARG,
	ANNOTATION_MISSING_ARG,
	ANNOTATION_MISTYPED_ARG,
	ANNOTATION_NON_EMPTY,
	ANNOTATION_MULTIPLE,
	DECL_MULTIPLE,
	DECL_MALFORMED,
	TYPE_UNKNOWN,
	TYPE_UNSUPP,
	TYPE_FIXED,
	TYPE_INCOMP,
	TYPE_INCOMP_EXPR,
	TYPE_INFERENCE_FAIL,
	TYPE_RULE,
	REL_NO_DECL_REC,
	REL_ARITY,
	REL_EXT_UNKNOWN,
	REL_EXT_INVALID,
	VAR_UNKNOWN,
	VAR_UNUSED,
	VAR_MULTIPLE_CONSTR,
	VAR_ASGN_CYCLE,
	VAR_ASGN_COMPLEX,
	CONSTR_UNKNOWN,
	CONSTR_INCOMP,
	CONSTR_NON_FUNC,
	CONSTR_RULE_CYCLE,
	FUNC_NON_CONSTR,

	MULTIPLE_ENT_DECLS,
	DEP_CYCLE,
	DEP_GLOBAL,
	CMD_DIRECTIVE,
	CMD_EVAL,
	CMD_RULE,
	CMD_NO_DECL,
	CMD_NO_IMPORT,

	static Map<ErrorId, String> msgMap
	static {
		msgMap = new EnumMap<>(ErrorId.class)
		msgMap[ID_IN_USE] = "Instantiation name `{0}` already in use"
		msgMap[COMP_ID_IN_USE] = "Component name `{0}` already in use"
		msgMap[COMP_UNKNOWN] = "Unknown component `{0}`"
		msgMap[COMP_DUPLICATE_PARAMS] = "Duplicate parameters ({0}) in component `{1}`"
		msgMap[COMP_SUPER_PARAM_MISMATCH] = "Non-matching parameters ({0}) for super component of `{1}`"
		msgMap[COMP_INST_ARITY] = "Wrong arity for instantiation parameters ({0}) of component `{1}` as `{2}`"
		msgMap[ANNOTATION_INVALID] = "Invalid annotation `{0}` for `{1}`"
		msgMap[ANNOTATION_INVALID_ARG] = "Invalid argument `{0}` for annotation `{1}`"
		msgMap[ANNOTATION_MISSING_ARG] = "Missing mandatory argument `{0}` for annotation `{1}`"
		msgMap[ANNOTATION_MISTYPED_ARG] = "Type mismatch ({0} instead of {1}) for argument `{2}` of annotation `{3}`"
		msgMap[ANNOTATION_NON_EMPTY] = "Annotation `{0}` takes no arguments"
		msgMap[ANNOTATION_MULTIPLE] = "Annotation `{0}` appears more than once in clause"
		msgMap[DECL_MULTIPLE] = "Multiple declarations for relation `{0}`"
		msgMap[DECL_MALFORMED] = "Malformed declaration"
		msgMap[TYPE_UNKNOWN] = "Unknown type `{0}`"
		msgMap[TYPE_UNSUPP] = "Unsupported type `{0}` (currently)"
		msgMap[TYPE_FIXED] = "Declared type `{0}` (at index {1}) for relation `{2}`"
		msgMap[TYPE_INCOMP] = "Incompatible types for relation `{0}` (at index {1})"
		msgMap[TYPE_INCOMP_EXPR] = "Incompatible types for numeric expression"
		msgMap[TYPE_INFERENCE_FAIL] = "Type inference was inconclusive: cannot reach fixpoint"
		msgMap[TYPE_RULE] = "Type `{0}` used as a normal relation in rule head"
		msgMap[REL_NO_DECL_REC] = "Unknown relation `{0}` used with a `@` parameter"
		msgMap[REL_ARITY] = "Inconsistent arity for relation `{0}`"
		msgMap[REL_EXT_UNKNOWN] = "Relation used with an unknown `@` parameter `{0}`"
		msgMap[REL_EXT_INVALID] = "Relation with a `@` parameter only allowed in a rule body, inside a component"
		msgMap[VAR_UNKNOWN] = "Unknown var `{0}`"
		msgMap[VAR_UNUSED] = "Unused var `{0}`"
		msgMap[VAR_MULTIPLE_CONSTR] = "Var `{0}` constructed by multiple constructors"
		msgMap[VAR_ASGN_CYCLE] = "Assignment of var `{0}` is part of a cycle"
		msgMap[VAR_ASGN_COMPLEX] = "Assignment of var `{0}` is part of a complicated logical structure"
		msgMap[CONSTR_UNKNOWN] = "Unknown constructor `{0}`"
		msgMap[CONSTR_INCOMP] = "Constructor `{0}` used with incompatible type `{1}`"
		msgMap[CONSTR_NON_FUNC] = "Constructor `{0}` must use functional syntax"
		msgMap[CONSTR_RULE_CYCLE] = "Constructor `{0}` appears in a cycle in rule head"
		msgMap[FUNC_NON_CONSTR] = "Functional syntax available only for constructors (`{0}`)"

		msgMap[MULTIPLE_ENT_DECLS] = "Multiple declarations for Type `{0}` in previous components"
		msgMap[DEP_CYCLE] = "Cycle detected in the dependency graph of components"
		msgMap[DEP_GLOBAL] = "Reintroducing relation `{0}` to global space"
		msgMap[CMD_DIRECTIVE] = "Invalid directive in command block `{0}`"
		msgMap[CMD_EVAL] = "EVAL property already specified in command block `{0}`"
		msgMap[CMD_RULE] = "Normal rules are not supported in a command block"
		msgMap[CMD_NO_DECL] = "Relation `{0}` is imported but has no declaration"
		msgMap[CMD_NO_IMPORT] = "Relation `{0}` is declared but not imported"
	}

	static String idToMsg(ErrorId errorId, Object[] values) {
		MessageFormat.format(msgMap.get(errorId), values)
	}
}
