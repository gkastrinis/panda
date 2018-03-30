package org.clyze.deepdoop

import org.antlr.v4.runtime.ANTLRInputStream
import org.clyze.deepdoop.actions.code.LBCodeGenerator
import org.clyze.deepdoop.actions.code.SouffleCodeGenerator
import org.clyze.deepdoop.system.Compiler
import org.clyze.deepdoop.system.DeepDoopException
import org.clyze.deepdoop.system.Error
import spock.lang.Specification
import spock.lang.Unroll

class DatalogSpec extends Specification {

	@Unroll
	def "Passing tests"() {
		when:
		test(file)

		then:
		notThrown(DeepDoopException)

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
		//"t100" | _
	}

	@Unroll
	def "Failing tests"() {
		when:
		test(file)

		then:
		def e = thrown(DeepDoopException)
		e.error == expectedErrorId

		where:
		file     | expectedErrorId
		"fail0"  | Error.COMP_UNKNOWN_PARAM
		"fail1"  | Error.INST_ID_IN_USE
		"fail2"  | Error.COMP_ID_IN_USE
		"fail3"  | Error.COMP_UNKNOWN
		"fail4"  | Error.INST_ID_IN_USE
		"fail5"  | Error.REL_EXT_NO_DECL
		"fail6"  | Error.CONSTR_TYPE_INCOMP
		"fail7"  | Error.FUNC_NON_CONSTR
		"fail8"  | Error.TYPE_UNKNOWN
		"fail9"  | Error.CONSTR_UNKNOWN
		"fail10" | Error.CONSTR_NON_FUNC
		"fail11" | Error.TYPE_INCOMP
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
		"fail25" | Error.TYPE_INCOMP_EXPR
		"fail26" | Error.COMP_DUPLICATE_PARAMS
		"fail27" | Error.COMP_SUPER_PARAM_MISMATCH
		"fail28" | Error.COMP_INST_ARITY
		"fail29" | Error.REL_EXT_INVALID
		"fail30" | Error.COMP_UNKNOWN
		"fail31" | Error.VAR_UNKNOWN
		"fail32" | Error.TYPE_INFERENCE_FAIL
		"fail33" | Error.REL_ARITY
		"fail34" | Error.REL_NO_DECL
		"fail35" | Error.DECL_SAME_VAR
//		"fail100" | Error.DEP_CYCLE
//		"fail101" | Error.CMD_RULE
//		"fail103" | Error.CMD_DIRECTIVE
//		"fail104" | Error.CMD_NO_DECL
//		"fail105" | Error.CMD_NO_IMPORT
//		"fail106" | Error.CMD_EVAL
//		"fail108" | Error.MULTIPLE_ENT_DECLS
	}

	@Unroll
	def "Souffle failing tests"() {
		when:
		souffleTest(file)

		then:
		def e = thrown(DeepDoopException)
		e.error == expectedErrorId

		where:
		file      | expectedErrorId
		"fail-S0" | Error.VAR_ASGN_CYCLE
		"fail-S1" | Error.VAR_ASGN_COMPLEX
	}

	def lbTest(String file) {
		def resourcePath = "/${file}.logic"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def resource = this.class.getResource(resourcePath).file
		Compiler.compile0(inputStream, resource, new LBCodeGenerator(new File("build")))
	}

	def souffleTest(String file) {
		def resourcePath = "/${file}.logic"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def resource = this.class.getResource(resourcePath).file
		Compiler.compile0(inputStream, resource, new SouffleCodeGenerator(new File("build")))
	}

	def test(String file) {
		DeepDoopException e1 = null, e2 = null

		try {
			lbTest(file)
		}
		catch (DeepDoopException e) {
			e1 = e
		}

		try {
			souffleTest(file)
		}
		catch (DeepDoopException e) {
			e2 = e
		}

		assert e1?.error == e2?.error
		if (e1) throw e1
	}
}
