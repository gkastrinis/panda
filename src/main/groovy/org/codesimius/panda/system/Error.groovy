package org.codesimius.panda.system

enum Error {
	ANNOTATION_UNKNOWN("Unknown annotation `{0}`"),
	ANNOTATION_NON_EMPTY("Annotation `{0}` takes no arguments"),
	ANNOTATION_MISSING_ARG("Missing mandatory argument `{0}` for annotation `{1}`"),
	ANNOTATION_INVALID_ARG("Invalid argument `{0}` for annotation `{1}`"),
	ANNOTATION_MISTYPED_ARG("Type mismatch ({0} instead of {1}) for argument `{2}` of annotation `{3}`"),
	ANNOTATION_MULTIPLE("Annotation `{0}` appears multiple times in clause"),
	ANNOTATION_INVALID("Invalid annotation `{0}` for `{1}`"),

	TEMPL_UNKNOWN("Unknown template `{0}`"),
	TEMPL_DUPLICATE_PARAMS("Duplicate parameters ({0}) in template `{1}`"),
	TEMPL_SUPER_PARAM_MISMATCH("Super template parameters ({0}) not matching parameters ({1}) of template `{2}`"),
	TEMPL_INST_ARITY("Wrong arity for instantiation parameters ({0}) of template `{1}` as `{2}`"),
	TEMPL_UNKNOWN_PARAM("Unknown parameter `{0}` used in template"),
	INST_CYCLE("Cycle detected in instantiations ({0})"),
	INST_UNKNOWN("Unknown instantiation `{0}`"),
	ID_IN_USE("Template or instantiation name (`{0}`) already in use"),

	DECL_SAME_VAR("Same variable `{0}` used multiple times in declaration"),
	DECL_MALFORMED("Number of variables and types in declaration not matching"),
	DECL_MULTIPLE("Multiple declarations for relation or type `{0}`"),
	AGGR_UNSUPPORTED_REL("Unsupported predicate `{0}` used in aggregation"),
	CONSTR_NON_FUNC("Constructor `{0}` must use functional syntax"),
	FUNC_NON_CONSTR("Functional syntax used in `{0}` allowed only for constructors"),
	CONSTR_AS_REL("Constructor `{0}` used with relation syntax"),
	CONSTR_UNKNOWN("Unknown constructor `{0}`"),
	CONSTR_TYPE_INCOMPAT("Constructor `{0}` used with incompatible type `{1}`"),
	CONSTR_RULE_CYCLE("Constructor `{0}` appears in a cycle, in rule head"),
	REL_ARITY("Inconsistent arity for relation `{0}`"),
	REL_NO_DECL("Unknown relation `{0}`"),
	REL_NAME_DEFCONSTR("Default constructor name used in user defined relation `{0}`"),
	REL_NEGATION_CYCLE("Cycle detected involving negation ({0})"),

	REL_QUAL_DECL("Declaration of qualified relation `{0}` is not allowed"),
	REL_QUAL_HEAD("Qualified relation `{0}` not allowed in rule head"),
	REL_QUAL_UNKNOWN("Unknown qualified relation `{0}`"),
	REL_TEMPL_CYCLE("Cycle detected involving a global relation used in template ({0})"),

	VAR_MULTIPLE_CONSTR("Var `{0}` constructed by multiple constructors in rule head"),
	VAR_UNBOUND("Unbound var `{0}` in rule head"),
	VAR_UNUSED("Unused var `{0}`"),
	VAR_DONTCARE_HEAD("Invalid use of `_` in rule head"),
	VAR_CONSTR_BODY("Constructed var `{0}` cannot appear in rule body"),
	VAR_ASGN_CYCLE("Assignment on var `{0}` is part of an assignment cycle"),
	VAR_ASGN_COMPLEX("Assignment on var `{0}` is part of a complicated logical structure"),
	SMART_LIT_NON_PRIMITIVE("Smart literal @\"{0}\" can only be used for user defined types (used for `{1}`)"),
	SMART_LIT_NO_DIRECT_REL("Smart literal @\"{0}\" can only be a direct argument of a relation/constructor"),

	TYPE_INF_FAIL("Type inference was inconclusive; cannot reach fixpoint"),
	TYPE_INF_INCOMPAT("Incompatible types during type inference ({0})"),
	TYPE_INF_INCOMPAT_USE("Incompatible types used for relation `{0}` at index {1} (expected `{2}` but `{3}` was used)"),
	TYPE_UNSUPP("Currently unsupported type `{0}`"),
	TYPE_UNKNOWN("Unknown type `{0}`"),
	TYPE_RULE("Type `{0}` used as a normal relation in rule head"),
	TYPE_OPT_ROOT_NONOPT("Root type `{0}` of a hierarchy must be explicitly marked for optimization when optimizing types"),
	TYPE_OPT_CONSTR("Type hierarchies marked for optimization cannot have user defined constructors (`{0}`)"),
	TYPE_QUAL_DECL("Declaration of a qualified type `{0}` is not allowed"),

	TEXT_HEAD("Declaration of text representation for a relation is malformed (head must be a single relation)"),
	TEXT_HEAD_NON_VAR("Declaration of text representation for a relation is malformed (head relation must contain only variables)"),
	TEXT_BODY("Declaration of text representation for a relation is malformed (body must be a single text representation)"),
	TEXT_BODY_NON_VAR("Declaration of text representation for a relation is malformed (body must contain only text and variables)"),
	TEXT_LIT_N_VAR("Conflict in text representations between literal \"{0}\" and parameter `{1}`"),
	TEXT_MULTIPLE_RELS("Same text representation cannot be used for multiple relations ({0})"),
	TEXT_UNKNOWN("Unknown text representation ({0})"),
	TEXT_VAR_MATCHES_LIT("Parameter `{0}` also matches text literals in `{1}`"),

	CONSTANT_HEAD("Declaration of constant is malformed (head must be a single relation)"),
	CONSTANT_ARITY("Declaration of constant `{0}` is malformed (relation arity must be 1)"),
	CONSTANT_NON_PRIMITIVE("Declaration of constant `{0}` is malformed (non primitive value `{1}` given)"),
	CONSTANT_BODY("Declaration of constant `{0}` is malformed (body is not allowed)"),
	CONSTANT_AS_REL("Constant `{0}` cannot be used as a normal relation"),

	INLINE_HEAD("Declaration of inline relation is malformed (head must be a single relation)"),
	INLINE_HEAD_NONVARS("Declaration of inline relation `{0}` is malformed (only named variables are allowed in the head -- `{1}`)"),
	INLINE_HEAD_DUPVARS("Declaration of inline relation `{0}` is malformed (var duplication is not allowed in the head -- `{1}`)"),
	INLINE_INVALID_ANN("Invalid annotation `{0}` in declaration of inline relation `{1}`"),
	INLINE_RECURSION("Inline relation `{0}` appears in a recursive cycle"),
	INLINE_NOTIN_BODY("Inline relation `{0}` can only appear in a rule body"),
	INLINE_AS_CONSTR("Inline relation `{0}` used as a constructor"),
	INLINE_AS_TYPE("Inline relation `{0}` used as a type"),

	EXP_CONTENTS_MISMATCH("Generated and expected file differ in contents")

	public final String label

	private Error(String label) { this.label = label }
}
