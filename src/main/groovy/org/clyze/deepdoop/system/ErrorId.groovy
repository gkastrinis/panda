package org.clyze.deepdoop.system

import java.text.MessageFormat

enum ErrorId {
	CMD_CONSTRAINT,
	CMD_DIRECTIVE,
	CMD_EVAL,
	CMD_RULE,
	CMD_NO_DECL,
	CMD_NO_IMPORT,
	DEP_CYCLE,
	DEP_GLOBAL,
	ID_IN_USE,
	NO_DECL,
	NO_DECL_REC,
	UNKNOWN_PRED,
	UNKNOWN_VAR,
	UNUSED_VAR,
	UNKNOWN_COMP,
	MULTIPLE_ENT_DECLS,
	INVALID_ANNOTATION,
	UNSUPPORTED_TYPE,
	UNKNOWN_TYPE,
	CONSTRUCTOR_UNKNOWN,
	CONSTRUCTOR_RULE,
	CONSTRUCTOR_INCOMPATIBLE,
	INCOMPATIBLE_TYPES,
	RESERVED_SUFFIX,
	INCONSISTENT_ARITY,
	ENTITY_RULE,
	FIXED_TYPE,
	MULTIPLE_DECLS,
	DUP_ANNOTATION,
	NON_EMPTY_ANNOTATION,
	MISSING_ARG_ANNOTATION,
	INVALID_ARG_ANNOTATION,

	static Map<ErrorId, String> msgMap
	static {
		msgMap = new EnumMap<>(ErrorId.class)
		msgMap[CMD_CONSTRAINT] = "Constraints are not supported in a command block"
		msgMap[CMD_DIRECTIVE] = "Invalid directive in command block `{0}`"
		msgMap[CMD_EVAL] = "EVAL property already specified in command block `{0}`"
		msgMap[CMD_RULE] = "Normal rules are not supported in a command block"
		msgMap[CMD_NO_DECL] = "Relation `{0}` is imported but has no declaration"
		msgMap[CMD_NO_IMPORT] = "Relation `{0}` is declared but not imported"
		msgMap[DEP_CYCLE] = "Cycle detected in the dependency graph of components"
		msgMap[DEP_GLOBAL] = "Reintroducing relation `{0}` to global space"
		msgMap[ID_IN_USE] = "Id `{0}` already used to initialize a component"
		msgMap[NO_DECL] = "Relation `{0}` used but not declared"
		msgMap[NO_DECL_REC] = "Relation `{0}` used with @past but not declared"
		msgMap[UNKNOWN_PRED] = "Unknown relation `{0}` used in propagation"
		msgMap[UNKNOWN_VAR] = "Unknown var `{0}`"
		msgMap[UNUSED_VAR] = "Unused var `{0}`"
		msgMap[UNKNOWN_COMP] = "Unknown component `{0}`"
		msgMap[MULTIPLE_ENT_DECLS] = "Multiple declarations for Entity `{0}` in previous components"
		msgMap[INVALID_ANNOTATION] = "Invalid annotation `{0}` for `{1}`"
		msgMap[UNSUPPORTED_TYPE] = "Type `{0}` is currently unsupported"
		msgMap[UNKNOWN_TYPE] = "Unknown type `{0}`"
		msgMap[CONSTRUCTOR_UNKNOWN] = "Unknown constructor `{0}`"
		msgMap[CONSTRUCTOR_RULE] = "Constructor `{0}` used as a normal relation in rule head"
		msgMap[CONSTRUCTOR_INCOMPATIBLE] = "Constructor `{0}` used with incompatible type `{1}`"
		msgMap[INCOMPATIBLE_TYPES] = "Incompatible types for relation `{0}` (at index {1})"
		msgMap[RESERVED_SUFFIX] = "Suffix `__pArTiAl` is reserved and cannot appear in relation names"
		msgMap[INCONSISTENT_ARITY] = "Relation `{0}` appears with inconsistent arity"
		msgMap[ENTITY_RULE] = "Entity `{0}` used as a normal relation in rule head"
		msgMap[FIXED_TYPE] = "Type `{0}` (at index {1}) for relation `{2}` is fixed"
		msgMap[MULTIPLE_DECLS] = "Relation `{0}` has multiple declarations"
		msgMap[DUP_ANNOTATION] = "Annotation `{0}` appears more than once in clause"
		msgMap[NON_EMPTY_ANNOTATION] = "Annotation `{0}` takes no arguments"
		msgMap[MISSING_ARG_ANNOTATION] = "Mandatory argument `{0}` for annotation `{1}` is missing"
		msgMap[INVALID_ARG_ANNOTATION] = "Argument `{0}` for annotation `{1}` is invalid"
	}

	static String idToMsg(ErrorId errorId, Object[] values) {
		MessageFormat.format(msgMap.get(errorId), values)
	}
}
