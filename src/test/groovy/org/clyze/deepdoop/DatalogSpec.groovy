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
	def "DeepDoop passing tests"() {
		when:
		test(file)

		then:
		notThrown(DeepDoopException)

		where:
		file        | _
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
	def "DeepDoop failing tests"() {
		when:
		test(file)

		then:
		def e = thrown(DeepDoopException)
		e.errorId == expectedErrorId

		where:
		file           | expectedErrorId
		"fail1.logic"  | ErrorId.DEP_GLOBAL
		"fail2.logic"  | ErrorId.ID_IN_USE
		"fail3.logic"  | ErrorId.COMP_UNKNOWN
		"fail4.logic"  | ErrorId.COMP_UNKNOWN
		"fail5.logic"  | ErrorId.REL_UNKNOWN
		"fail6.logic"  | ErrorId.REL_NO_DECL_REC
		"fail7.logic"  | ErrorId.CONSTR_INCOMP
		"fail8.logic"  | ErrorId.FUNC_NON_CONSTR
		"fail9.logic"  | ErrorId.TYPE_UNKNOWN
		"fail10.logic" | ErrorId.CONSTR_UNKNOWN
		"fail11.logic" | ErrorId.CONSTR_NON_FUNC
		"fail12.logic" | ErrorId.TYPE_INCOMP
		"fail13.logic" | ErrorId.REL_ARITY
		"fail14.logic" | ErrorId.TYPE_FIXED
		"fail15.logic" | ErrorId.TYPE_RULE
		"fail16.logic" | ErrorId.DECL_MULTIPLE
		"fail17.logic" | ErrorId.ANNOTATION_NON_EMPTY
		"fail18.logic" | ErrorId.ANNOTATION_MISSING_ARG
		"fail19.logic" | ErrorId.ANNOTATION_INVALID_ARG
		"fail20.logic" | ErrorId.ANNOTATION_INVALID
		"fail21.logic" | ErrorId.REFMODE_ARITY
		"fail22.logic" | ErrorId.REFMODE_KEY
		"fail23.logic" | ErrorId.REL_EXT_HEAD
		"fail24.logic" | ErrorId.DECL_MALFORMED
		"fail25.logic" | ErrorId.DECL_MALFORMED
		"fail26.logic" | ErrorId.DECL_MALFORMED
		"fail27.logic" | ErrorId.ANNOTATION_MISTYPED_ARG
		"fail28.logic" | ErrorId.CONSTR_RULE_CYCLE
		"fail29.logic" | ErrorId.VAR_MULTIPLE_CONSTR
		/*
		"fail100.logic"  | ErrorId.DEP_CYCLE
		"fail101.logic"  | ErrorId.CMD_RULE
		"fail103.logic"  | ErrorId.CMD_DIRECTIVE
		"fail104.logic"  | ErrorId.CMD_NO_DECL
		"fail105.logic"  | ErrorId.CMD_NO_IMPORT
		"fail106.logic"  | ErrorId.CMD_EVAL
		"fail107.logic" | ErrorId.VAR_UNKNOWN
		"fail108.logic" | ErrorId.MULTIPLE_ENT_DECLS
		*/
	}

	def test(String file) {
		def resourcePath = "/$file"
		def resource = this.class.getResource(resourcePath).file

		DeepDoopException e1 = null
		try {
			def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
			Compiler.compile0(inputStream, resource, new LBCodeGenerator("build"))
		} catch (DeepDoopException e) {
			e1 = e
		}

		DeepDoopException e2 = null
		try {
			def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
			Compiler.compile0(inputStream, resource, new SouffleCodeGenerator("build"))
		} catch (DeepDoopException e) {
			e2 = e
		}

		assert e1?.errorId == e2?.errorId
		if (e1) throw e1
	}
}
