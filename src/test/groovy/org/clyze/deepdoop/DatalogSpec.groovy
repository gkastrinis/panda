package org.clyze.deepdoop

import org.antlr.v4.runtime.ANTLRInputStream
import org.clyze.deepdoop.actions.LBCodeGenVisitingActor
import org.clyze.deepdoop.actions.SouffleCodeGenVisitingActor
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
		file       | _
		"t1.logic" | _
		"t2.logic" | _
		"t3.logic" | _
		"t4.logic" | _
		"t5.logic" | _
		"t6.logic" | _
		"t7.logic" | _
		"t8.logic" | _
		//"t9.logic" | _
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
		"fail3.logic"  | ErrorId.UNKNOWN_COMP
		"fail4.logic"  | ErrorId.UNKNOWN_COMP
		"fail5.logic"  | ErrorId.UNKNOWN_PRED
		"fail6.logic"  | ErrorId.NO_DECL_REC
		"fail7.logic"  | ErrorId.CONSTRUCTOR_INCOMPATIBLE
		"fail8.logic"  | ErrorId.CONSTRUCTOR_RULE
		"fail9.logic"  | ErrorId.UNKNOWN_TYPE
		"fail10.logic" | ErrorId.CONSTRUCTOR_UNKNOWN
		"fail11.logic" | ErrorId.RESERVED_SUFFIX
		"fail12.logic" | ErrorId.INCOMPATIBLE_TYPES
		"fail13.logic" | ErrorId.INCONSISTENT_ARITY
		"fail14.logic" | ErrorId.FIXED_TYPE
		"fail15.logic" | ErrorId.TYPE_RULE
		"fail16.logic" | ErrorId.MULTIPLE_DECLS
		"fail17.logic" | ErrorId.NON_EMPTY_ANNOTATION
		"fail18.logic" | ErrorId.MISSING_ARG_ANNOTATION
		"fail19.logic" | ErrorId.INVALID_ARG_ANNOTATION
		"fail20.logic" | ErrorId.INVALID_ANNOTATION
		"fail21.logic" | ErrorId.REFMODE_ARITY
		"fail22.logic" | ErrorId.REFMODE_KEY
		/*
		"fail100.logic"  | ErrorId.DEP_CYCLE
		"fail101.logic"  | ErrorId.CMD_RULE
		"fail102.logic"  | ErrorId.CMD_CONSTRAINT
		"fail103.logic"  | ErrorId.CMD_DIRECTIVE
		"fail104.logic"  | ErrorId.CMD_NO_DECL
		"fail105.logic"  | ErrorId.CMD_NO_IMPORT
		"fail106.logic"  | ErrorId.CMD_EVAL
		"fail107.logic" | ErrorId.UNKNOWN_VAR
		"fail108.logic" | ErrorId.MULTIPLE_ENT_DECLS
		*/
	}

	def test(String file) {
		def resourcePath = "/$file"
		def resource = this.class.getResource(resourcePath).file

		DeepDoopException e1 = null
		try {
			def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
			Compiler.compile0(inputStream, resource, new LBCodeGenVisitingActor("build"))
		} catch (DeepDoopException e) {
			e1 = e
		}

		DeepDoopException e2 = null
		try {
			def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
			Compiler.compile0(inputStream, resource, new SouffleCodeGenVisitingActor("build"))
		} catch (DeepDoopException e) {
			e2 = e
		}

		assert e1?.errorId == e2?.errorId
		if (e1) throw e1
	}
}
