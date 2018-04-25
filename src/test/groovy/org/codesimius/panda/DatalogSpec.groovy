package org.codesimius.panda

import org.antlr.v4.runtime.ANTLRInputStream
import org.codesimius.panda.actions.code.LBCodeGenerator
import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error
import org.codesimius.panda.system.PandaException
import spock.lang.Specification
import spock.lang.Unroll

class DatalogSpec extends Specification {

	@Unroll
	def "Passing tests"() {
		when:
		test(file)

		then:
		notThrown(PandaException)

		where:
		file  | _
		"t0"  | _
		"t1"  | _
		"t2"  | _
		"t3"  | _
		"t4"  | _
		"t5"  | _
		"t6"  | _
		"t7"  | _
		"t8"  | _
		"t9"  | _
		"t10" | _
		"t11" | _
		"t12" | _
		"t13" | _
	}

	@Unroll
	def "Failing tests"() {
		when:
		test(file)

		then:
		def e = thrown(PandaException)
		e.error == expectedErrorId

		where:
		file     | expectedErrorId
		"fail0"  | Error.COMP_UNKNOWN_PARAM
		"fail1"  | Error.ID_IN_USE
		"fail2"  | Error.ID_IN_USE
		"fail3"  | Error.COMP_UNKNOWN
		"fail4"  | Error.ID_IN_USE
		"fail5"  | Error.REL_EXT_NO_DECL
		"fail6"  | Error.CONSTR_TYPE_INCOMPAT
		"fail7"  | Error.FUNC_NON_CONSTR
		"fail8"  | Error.TYPE_UNKNOWN
		"fail9"  | Error.CONSTR_UNKNOWN
		"fail10" | Error.CONSTR_NON_FUNC
		"fail11" | Error.TYPE_INCOMPAT
		"fail12" | Error.REL_ARITY
		"fail13" | Error.TYPE_INFERENCE_FIXED
		"fail14" | Error.TYPE_RULE
		"fail15" | Error.DECL_MULTIPLE
		"fail16" | Error.ANNOTATION_NON_EMPTY
		"fail17" | Error.ANNOTATION_MISSING_ARG
		"fail18" | Error.ANNOTATION_INVALID_ARG
		"fail19" | Error.ANNOTATION_INVALID
		"fail20" | Error.REL_EXT_INVALID
		"fail21" | Error.DECL_MALFORMED
		"fail22" | Error.ANNOTATION_MISTYPED_ARG
		"fail23" | Error.CONSTR_RULE_CYCLE
		"fail24" | Error.VAR_MULTIPLE_CONSTR
		"fail25" | Error.TYPE_INCOMPAT_EXPR
		"fail26" | Error.COMP_DUPLICATE_PARAMS
		"fail27" | Error.COMP_SUPER_PARAM_MISMATCH
		"fail28" | Error.COMP_INST_ARITY
		"fail29" | Error.REL_EXT_INVALID
		"fail30" | Error.INST_UNKNOWN
		"fail31" | Error.VAR_UNKNOWN
		"fail32" | Error.TYPE_INFERENCE_FAIL
		"fail33" | Error.REL_ARITY
		"fail34" | Error.REL_NO_DECL
		"fail35" | Error.DECL_SAME_VAR
		"fail36" | Error.COMP_NAME_LIMITS
		"fail37" | Error.ANNOTATION_UNKNOWN
		"fail38" | Error.REL_EXT_NO_DECL
		"fail39" | Error.COMP_UNKNOWN_PARAM
		"fail40" | Error.COMP_NAME_LIMITS
		"fail41" | Error.REL_NAME_COMP
		"fail42" | Error.REL_NAME_COMP
		"fail43" | Error.INST_CYCLE
		"fail44" | Error.REL_EXT_CYCLE
		"fail45" | Error.AGGR_UNSUPPORTED_REL
		"fail46" | Error.REL_NEGATION_CYCLE
		"fail47" | Error.VAR_UNBOUND_HEAD
		"fail48" | Error.VAR_CONSTR_BODY
		"fail49" | Error.REL_NAME_DEFCONSTR
		"fail50" | Error.TYPE_OPT_ROOT_NONOPT
		"fail51" | Error.TYPE_OPT_CONSTR
		"fail52" | Error.TYPE_UNKNOWN
	}

	@Unroll
	def "Souffle failing tests"() {
		when:
		test0(file, SouffleCodeGenerator)

		then:
		def e = thrown(PandaException)
		e.error == expectedErrorId

		where:
		file      | expectedErrorId
		"fail-S0" | Error.VAR_ASGN_CYCLE
		"fail-S1" | Error.VAR_ASGN_COMPLEX
	}

	def test0(String file, Class codeGen) {
		def resourcePath = "/${file}.pnd"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def resource = this.class.getResource(resourcePath).file
		Compiler.compile0(inputStream, resource, codeGen.newInstance("build/out"))
	}

	def test(String file) {
		PandaException e1 = null, e2 = null

		try {
			test0(file, LBCodeGenerator)
		} catch (PandaException e) {
			e1 = e
		}

		try {
			test0(file, SouffleCodeGenerator)
		}
		catch (PandaException e) {
			e2 = e
		}

		assert e1?.error == e2?.error
		if (e1) throw e1
	}
}
