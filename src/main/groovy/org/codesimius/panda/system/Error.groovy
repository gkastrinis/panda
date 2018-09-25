package org.codesimius.panda.system

import groovy.util.logging.Log4j
import org.apache.log4j.*

import java.text.MessageFormat

@Log4j
enum Error {
	ANNOTATION_UNKNOWN,
	ANNOTATION_NON_EMPTY,
	ANNOTATION_MISSING_ARG,
	ANNOTATION_INVALID_ARG,
	ANNOTATION_MISTYPED_ARG,
	ANNOTATION_MULTIPLE,
	ANNOTATION_INVALID,

	COMP_UNKNOWN,
	COMP_DUPLICATE_PARAMS,
	COMP_SUPER_PARAM_MISMATCH,
	COMP_INST_ARITY,
	COMP_UNKNOWN_PARAM,
	COMP_NAME_LIMITS,
	INST_CYCLE,
	INST_UNKNOWN,
	ID_IN_USE,

	DECL_SAME_VAR,
	DECL_MALFORMED,
	DECL_MULTIPLE,
	AGGR_UNSUPPORTED_REL,
	CONSTR_NON_FUNC,
	FUNC_NON_CONSTR,
	REL_ARITY,
	REL_EXT_CYCLE,
	REL_EXT_INVALID,
	REL_EXT_NO_DECL,
	REL_NO_DECL,
	REL_NAME_COMP,
	REL_NAME_DEFCONSTR,
	REL_NEGATION_CYCLE,
	CONSTR_RULE_CYCLE,
	CONSTR_UNKNOWN,
	CONSTR_TYPE_INCOMPAT,
	CONSTR_AS_REL,
	VAR_MULTIPLE_CONSTR,
	VAR_UNKNOWN,
	VAR_UNUSED,
	VAR_UNBOUND_HEAD,
	VAR_CONSTR_BODY,
	VAR_ASGN_CYCLE,
	VAR_ASGN_COMPLEX,
	SMART_LIT_NON_PRIMITIVE,
	SMART_LIT_NO_DIRECT_REL,

	TYPE_INF_FAIL,
	TYPE_INF_INCOMPAT,
	TYPE_INF_INCOMPAT_USE,
	TYPE_UNSUPP,
	TYPE_UNKNOWN,
	TYPE_RULE,
	TYPE_OPT_ROOT_NONOPT,
	TYPE_OPT_CONSTR,

	TEXT_MALFORMED_HEAD,
	TEXT_HEAD_NON_VAR,
	TEXT_MALFORMED_BODY,
	TEXT_BODY_NON_VAR,
	TEXT_LIT_N_VAR,
	TEXT_MULTIPLE_RELS,
	TEXT_UNKNOWN,
	TEXT_VAR_MATCHES_LIT,

	EXP_CONTENTS_MISMATCH,

	static Map<Error, String> msgMap = [
			(ANNOTATION_UNKNOWN)       : "Unknown annotation `{0}`",
			(ANNOTATION_NON_EMPTY)     : "Annotation `{0}` takes no arguments",
			(ANNOTATION_MISSING_ARG)   : "Missing mandatory argument `{0}` for annotation `{1}`",
			(ANNOTATION_INVALID_ARG)   : "Invalid argument `{0}` for annotation `{1}`",
			(ANNOTATION_MISTYPED_ARG)  : "Type mismatch ({0} instead of {1}) for argument `{2}` of annotation `{3}`",
			(ANNOTATION_MULTIPLE)      : "Annotation `{0}` appears multiple times in clause",
			(ANNOTATION_INVALID)       : "Invalid annotation `{0}` for `{1}`",

			(COMP_UNKNOWN)             : "Unknown component name `{0}`",
			(COMP_DUPLICATE_PARAMS)    : "Duplicate parameters ({0}) in component `{1}`",
			(COMP_SUPER_PARAM_MISMATCH): "Super component parameters ({0}) not matching parameters ({1}) of component `{2}`",
			(COMP_INST_ARITY)          : "Wrong arity for instantiation parameters ({0}) of component `{1}` as `{2}`",
			(COMP_UNKNOWN_PARAM)       : "Unknown parameter `{0}` used in component",
			(COMP_NAME_LIMITS)         : "Non-valid character `:` in component/instantiation name (`{0}`)",
			(INST_CYCLE)               : "Cycle detected in instantiations ({0})",
			(INST_UNKNOWN)             : "Unknown instantiation `{0}`",
			(ID_IN_USE)                : "Component or instantiation name `{0}` already in use",

			(DECL_SAME_VAR)            : "Same variable `{0}` used multiple times in declaration",
			(DECL_MALFORMED)           : "Number of variables and types in declaration not matching",
			(DECL_MULTIPLE)            : "Multiple declarations for relation or type `{0}`",
			(AGGR_UNSUPPORTED_REL)     : "Unsupported predicate `{0}` used in aggregation",
			(CONSTR_NON_FUNC)          : "Constructor `{0}` must use functional syntax",
			(FUNC_NON_CONSTR)          : "Functional syntax available only for constructors (`{0}`)",
			(REL_ARITY)                : "Inconsistent arity for relation `{0}`",
			(REL_EXT_CYCLE)            : "Cycle detected involving a global relation used with a `@` paramater ({0})",
			(REL_EXT_INVALID)          : "Relation with a parameter only allowed in a rule body, inside a component",
			(REL_EXT_NO_DECL)          : "Unknown relation `{0}` used with a parameter",
			(REL_NO_DECL)              : "Unknown relation `{0}`",
			(REL_NAME_COMP)            : "Invalid prefix `{0}` in relation name `{1}` (component/instantiation name)",
			(REL_NAME_DEFCONSTR)       : "Default constructor name used in user defined relation `{0}`",
			(REL_NEGATION_CYCLE)       : "Cycle detected involving negation ({0})",
			(CONSTR_RULE_CYCLE)        : "Constructor `{0}` appears in a cycle in rule head",
			(CONSTR_UNKNOWN)           : "Unknown constructor `{0}`",
			(CONSTR_TYPE_INCOMPAT)     : "Constructor `{0}` used with incompatible type `{1}`",
			(CONSTR_AS_REL)            : "Constructor `{0}` used with relation syntax",
			(VAR_MULTIPLE_CONSTR)      : "Var `{0}` constructed by multiple constructors in rule head",
			(VAR_UNKNOWN)              : "Unknown var `{0}`",
			(VAR_UNUSED)               : "Unused var `{0}`",
			(VAR_UNBOUND_HEAD)         : "Unbound var `_` used in rule head",
			(VAR_CONSTR_BODY)          : "Constructed bar `{0}` cannot appear in rule body",
			(VAR_ASGN_CYCLE)           : "Assignment on var `{0}` is part of an assignment cycle",
			(VAR_ASGN_COMPLEX)         : "Assignment on var `{0}` is part of a complicated logical structure",
			(SMART_LIT_NON_PRIMITIVE)  : "Smart literals (`{0}`) can only be use for user defined types (type `{1}`)",
			(SMART_LIT_NO_DIRECT_REL)  : "Smart literals (`{0}`) can only be direct parameters of a relation/constructor",

			(TYPE_INF_FAIL)            : "Type inference was inconclusive: cannot reach fixpoint",
			(TYPE_INF_INCOMPAT)        : "Incompatible types during type inference ({0})",
			(TYPE_INF_INCOMPAT_USE)    : "Incompatible types used for relation `{0}` at index {1} (expected `{2}` & used {3})",
			(TYPE_UNSUPP)              : "Currently unsupported type `{0}`",
			(TYPE_UNKNOWN)             : "Unknown type `{0}`",
			(TYPE_RULE)                : "Type `{0}` used as a normal relation in rule head",
			(TYPE_OPT_ROOT_NONOPT)     : "Root types of a hierarchy must be explicitly marked for optimization (`{0}`) when optimizing types",
			(TYPE_OPT_CONSTR)          : "Type hierarchies marked for optimization cannot have user defined constructors (`{0}`)",

			(TEXT_MALFORMED_HEAD)      : "Declaration for text representation of relation is malformed (head must be a single relation)",
			(TEXT_HEAD_NON_VAR)        : "Declaration for text representation of relation is malformed (head relation must contain only variables)",
			(TEXT_MALFORMED_BODY)      : "Declaration for text representation of relation is malformed (body must be a single text representation)",
			(TEXT_BODY_NON_VAR)        : "Declaration for text representation of relation is malformed (body must contain only text and variables)",
			(TEXT_LIT_N_VAR)           : "Conflict in text representations between literals ({0}) and parameter `{1}`",
			(TEXT_MULTIPLE_RELS)       : "Same text representation cannot be used for multiple relations ({0})",
			(TEXT_UNKNOWN)             : "Unknown text representation ({0})",
			(TEXT_VAR_MATCHES_LIT)     : "Parameter `{0}` also matches text literals in `{1}`",

			(EXP_CONTENTS_MISMATCH)    : "Generated and expected file differ in contents"
	]

	static void warn(def loc = null, Error errorId, Object... values) {
		def msg = "${MessageFormat.format(msgMap.get(errorId), values)} -- [$errorId]"
		if (loc) msg = "$msg\n$loc"
		log.warn(tag(msg, "WARNING"))
	}

	static void error(def loc = null, Error errorId, Object... values) {
		def msg = "${MessageFormat.format(msgMap.get(errorId), values)} -- [$errorId]"
		if (loc) msg = "$msg\n$loc"
		throw new PandaException(msg, errorId)
	}

	static String tag(String msg, String tag) { "[paNda] $tag: $msg" }

	static boolean loggingInitialized

	static initializeLogging() {
		if (!loggingInitialized) {
			def logDir = new File("build/logs")
			if (!logDir) logDir.mkdir()

			def root = Logger.rootLogger
			root.setLevel(Level.toLevel("INFO", Level.WARN))
			root.addAppender(new DailyRollingFileAppender(new PatternLayout("%d [%t] %-5p %c - %m%n"), "$logDir/panda.log", "'.'yyyy-MM-dd"))
			root.addAppender(new ConsoleAppender(new PatternLayout("%m%n")))

			loggingInitialized = true
		}
	}
}
