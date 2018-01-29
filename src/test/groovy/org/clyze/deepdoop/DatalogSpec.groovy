package org.clyze.deepdoop

import org.antlr.v4.runtime.ANTLRInputStream
import org.clyze.deepdoop.actions.code.LBCodeGenerator
import org.clyze.deepdoop.actions.code.SouffleCodeGenerator
import org.clyze.deepdoop.system.Compiler
import org.clyze.deepdoop.system.DeepDoopException
import org.clyze.deepdoop.system.ErrorId
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
		file       | _
		"t0.logic" | _
		"t1.logic" | _
		"t2.logic" | _
		"t3.logic" | _
		"t4.logic" | _
		"t5.logic" | _
		"t6.logic" | _
		"t7.logic" | _
		"t8.logic" | _
		"t9.logic" | _
		//"t100.logic" | _
	}

	@Unroll
	def "Failing tests"() {
		when:
		test(file)

		then:
		def e = thrown(DeepDoopException)
		e.errorId == expectedErrorId

		where:
		file           | expectedErrorId
		//"fail0.logic"  | ErrorId.DEP_GLOBAL
		"fail1.logic"  | ErrorId.ID_IN_USE
		///"fail2.logic"  | ErrorId.COMP_UNKNOWN
		"fail3.logic"  | ErrorId.COMP_UNKNOWN
		///"fail4.logic"  | ErrorId.REL_UNKNOWN
		"fail5.logic"  | ErrorId.REL_NO_DECL_REC
		"fail6.logic"  | ErrorId.CONSTR_INCOMP
		"fail7.logic"  | ErrorId.FUNC_NON_CONSTR
		"fail8.logic"  | ErrorId.TYPE_UNKNOWN
		"fail9.logic"  | ErrorId.CONSTR_UNKNOWN
		"fail10.logic" | ErrorId.CONSTR_NON_FUNC
		"fail11.logic" | ErrorId.TYPE_INCOMP
		"fail12.logic" | ErrorId.REL_ARITY
		"fail13.logic" | ErrorId.TYPE_FIXED
		"fail14.logic" | ErrorId.TYPE_RULE
		"fail15.logic" | ErrorId.DECL_MULTIPLE
		"fail16.logic" | ErrorId.ANNOTATION_NON_EMPTY
		"fail17.logic" | ErrorId.ANNOTATION_MISSING_ARG
		"fail18.logic" | ErrorId.ANNOTATION_INVALID_ARG
		//"fail19.logic" | ErrorId.ANNOTATION_INVALID
		"fail20.logic" | ErrorId.REL_EXT_INVALID
		"fail21.logic" | ErrorId.DECL_MALFORMED
		"fail22.logic" | ErrorId.ANNOTATION_MISTYPED_ARG
		"fail23.logic" | ErrorId.CONSTR_RULE_CYCLE
		"fail24.logic" | ErrorId.VAR_MULTIPLE_CONSTR
		"fail25.logic" | ErrorId.TYPE_INCOMP_EXPR
		"fail26.logic" | ErrorId.COMP_DUPLICATE_PARAMS
		"fail27.logic" | ErrorId.COMP_SUPER_PARAM_MISMATCH
		"fail28.logic" | ErrorId.COMP_INIT_ARITY
		"fail29.logic" | ErrorId.REL_EXT_INVALID
		"fail30.logic" | ErrorId.COMP_UNKNOWN
//		"fail100.logic" | ErrorId.DEP_CYCLE
//		"fail101.logic" | ErrorId.CMD_RULE
//		"fail103.logic" | ErrorId.CMD_DIRECTIVE
//		"fail104.logic" | ErrorId.CMD_NO_DECL
//		"fail105.logic" | ErrorId.CMD_NO_IMPORT
//		"fail106.logic" | ErrorId.CMD_EVAL
//		"fail107.logic" | ErrorId.VAR_UNKNOWN
//		"fail108.logic" | ErrorId.MULTIPLE_ENT_DECLS
	}

	@Unroll
	def "DeepDoop Souffle-failing tests"() {
		when:
		souffleTest(file)

		then:
		def e = thrown(DeepDoopException)
		e.errorId == expectedErrorId

		where:
		file            | expectedErrorId
		"fail-S0.logic" | ErrorId.VAR_ASGN_CYCLE
		"fail-S1.logic" | ErrorId.VAR_ASGN_COMPLEX
	}

	def lbTest(String file) {
		def resourcePath = "/$file"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def resource = this.class.getResource(resourcePath).file
		Compiler.compile0(inputStream, resource, new LBCodeGenerator("build"))
	}

	def souffleTest(String file) {
		def resourcePath = "/$file"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def resource = this.class.getResource(resourcePath).file
		Compiler.compile0(inputStream, resource, new SouffleCodeGenerator("build"))
	}

	def test(String file) {
		DeepDoopException e1 = null, e2 = null

		try { lbTest(file) }
		catch (DeepDoopException e) { e1 = e }

		try { souffleTest(file) }
		catch (DeepDoopException e) { e2 = e }

		assert e1?.errorId == e2?.errorId
		if (e1) throw e1
	}
}
