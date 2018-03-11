package org.clyze.deepdoop

import org.antlr.v4.runtime.ANTLRInputStream
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
		file        | _
		"t0.logic"  | _
		"t1.logic"  | _
		"t2.logic"  | _
		"t3.logic"  | _
		"t4.logic"  | _
		"t5.logic"  | _
		"t6.logic"  | _
		"t7.logic"  | _
		"t8.logic"  | _
		"t9.logic"  | _
		"t10.logic" | _
		//"t100.logic" | _
	}

	@Unroll
	def "Failing tests"() {
		when:
		test(file)

		then:
		def e = thrown(DeepDoopException)
		e.error == expectedErrorId

		where:
		file           | expectedErrorId
		"fail0.logic"  | Error.COMP_UNKNOWN_PARAM
		"fail1.logic"  | Error.INST_ID_IN_USE
		"fail2.logic"  | Error.COMP_ID_IN_USE
		"fail3.logic"  | Error.COMP_UNKNOWN
		"fail4.logic"  | Error.INST_ID_IN_USE
		"fail5.logic"  | Error.REL_EXT_NO_DECL
		"fail6.logic"  | Error.CONSTR_TYPE_INCOMP
		"fail7.logic"  | Error.FUNC_NON_CONSTR
		"fail8.logic"  | Error.TYPE_UNKNOWN
		"fail9.logic"  | Error.CONSTR_UNKNOWN
		"fail10.logic" | Error.CONSTR_NON_FUNC
		"fail11.logic" | Error.TYPE_INCOMP
		"fail12.logic" | Error.REL_ARITY
		"fail13.logic" | Error.TYPE_INFERENCE_FIXED
		"fail14.logic" | Error.TYPE_RULE
		"fail15.logic" | Error.DECL_MULTIPLE
		"fail16.logic" | Error.ANNOTATION_NON_EMPTY
		"fail17.logic" | Error.ANNOTATION_MISSING_ARG
		"fail18.logic" | Error.ANNOTATION_INVALID_ARG
		"fail19.logic" | Error.ANNOTATION_INVALID
		"fail20.logic" | Error.REL_EXT_INVALID
		"fail21.logic" | Error.DECL_MALFORMED
		"fail22.logic" | Error.ANNOTATION_MISTYPED_ARG
		"fail23.logic" | Error.CONSTR_RULE_CYCLE
		"fail24.logic" | Error.VAR_MULTIPLE_CONSTR
		"fail25.logic" | Error.TYPE_INCOMP_EXPR
		"fail26.logic" | Error.COMP_DUPLICATE_PARAMS
		"fail27.logic" | Error.COMP_SUPER_PARAM_MISMATCH
		"fail28.logic" | Error.COMP_INST_ARITY
		"fail29.logic" | Error.REL_EXT_INVALID
		"fail30.logic" | Error.COMP_UNKNOWN
		"fail31.logic" | Error.VAR_UNKNOWN
		"fail32.logic" | Error.TYPE_INFERENCE_FAIL
//		"fail100.logic" | Error.DEP_CYCLE
//		"fail101.logic" | Error.CMD_RULE
//		"fail103.logic" | Error.CMD_DIRECTIVE
//		"fail104.logic" | Error.CMD_NO_DECL
//		"fail105.logic" | Error.CMD_NO_IMPORT
//		"fail106.logic" | Error.CMD_EVAL
//		"fail108.logic" | Error.MULTIPLE_ENT_DECLS
	}

	@Unroll
	def "Souffle failing tests"() {
		when:
		souffleTest(file)

		then:
		def e = thrown(DeepDoopException)
		e.error == expectedErrorId

		where:
		file            | expectedErrorId
		"fail-S0.logic" | Error.VAR_ASGN_CYCLE
		"fail-S1.logic" | Error.VAR_ASGN_COMPLEX
	}

//	def lbTest(String file) {
//		def resourcePath = "/$file"
//		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
//		def resource = this.class.getResource(resourcePath).file
//		Compiler.compile0(inputStream, resource, new LBCodeGenerator("build"))
//	}

	def souffleTest(String file) {
		def resourcePath = "/$file"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def resource = this.class.getResource(resourcePath).file
		Compiler.compile0(inputStream, resource, new SouffleCodeGenerator("build"))
	}

	def test(String file) {
		DeepDoopException e1 = null, e2 = null

//		try {
//			lbTest(file)
//		}
//		catch (DeepDoopException e) {
//			e1 = e
//		}

		try {
			souffleTest(file)
		}
		catch (DeepDoopException e) {
			e2 = e
		}

//		assert e1?.error == e2?.error
		if (e2) throw e2
	}
}
