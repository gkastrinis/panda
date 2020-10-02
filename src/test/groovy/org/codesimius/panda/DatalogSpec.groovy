package org.codesimius.panda

import org.antlr.v4.runtime.ANTLRInputStream
import org.apache.commons.io.FileUtils
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
		testAndCompare(file)

		then:
		notThrown(PandaException)

		where:
		file                | _
		"t0-basics"         | _
		"t1-primitives"     | _
		"t2-aggregations"   | _
		"t3-types"          | _
		"t4-typeValues"     | _
		"t5-inference"      | _
		"t6-inputs"         | _
		"t7-constr1"        | _
		"t8-constr2"        | _
		"t9-constr3"        | _
		"t10-smartLiterals" | _
		"t11-typesOpt"      | _
		"t12-freeText1"     | _
		"t13-templates1"    | _
		"t14-templates2"    | _
		"t15-templates3"    | _
		"t16-templates4"    | _
		"t17-templates5"    | _
		"t18-includes"      | _
		"t19-templates6"    | _
		"t20-freeText2"     | _
		"t21-constants"     | _
		"t22-inlines"       | _
	}

	@Unroll
	def "Failing tests"() {
		when:
		testAndCompare("failing/$file")

		then:
		def e = thrown(PandaException)
		e.error == expectedErrorId

		where:
		file     | expectedErrorId
		"fail0"  | Error.TEMPL_UNKNOWN_PARAM
		"fail1"  | Error.ID_IN_USE
		"fail2"  | Error.ID_IN_USE
		"fail3"  | Error.TEMPL_UNKNOWN
		"fail4"  | Error.ID_IN_USE
		"fail5"  | Error.REL_QUAL_UNKNOWN
		"fail6"  | Error.CONSTR_TYPE_INCOMPAT
		"fail7"  | Error.FUNC_NON_CONSTR
		"fail8"  | Error.TYPE_UNKNOWN
		"fail9"  | Error.CONSTR_UNKNOWN
		"fail10" | Error.CONSTR_NON_FUNC
		"fail11" | Error.TYPE_INF_INCOMPAT
		"fail12" | Error.REL_ARITY
		"fail13" | Error.TYPE_INF_INCOMPAT_USE
		"fail14" | Error.TYPE_RULE
		"fail15" | Error.DECL_MULTIPLE
		"fail16" | Error.ANNOTATION_NON_EMPTY
		"fail17" | Error.ANNOTATION_MISSING_ARG
		"fail18" | Error.ANNOTATION_INVALID_ARG
		"fail19" | Error.ANNOTATION_INVALID
		"fail20" | Error.REL_QUAL_HEAD
		"fail21" | Error.DECL_MALFORMED
		"fail22" | Error.ANNOTATION_MISTYPED_ARG
		"fail23" | Error.CONSTR_RULE_CYCLE
		"fail24" | Error.VAR_MULTIPLE_CONSTR
		"fail25" | Error.TYPE_INF_INCOMPAT
		"fail26" | Error.TEMPL_DUPLICATE_PARAMS
		"fail27" | Error.TEMPL_SUPER_PARAM_MISMATCH
		"fail28" | Error.TEMPL_INST_ARITY
		"fail29" | Error.REL_QUAL_HEAD
		"fail30" | Error.INST_UNKNOWN
		"fail31" | Error.VAR_UNBOUND
		"fail32" | Error.TYPE_INF_FAIL
		"fail33" | Error.REL_ARITY
		"fail34" | Error.REL_NO_DECL
		"fail35" | Error.DECL_SAME_VAR
		"fail36" | Error.TEXT_VAR_MATCHES_LIT
		"fail37" | Error.ANNOTATION_UNKNOWN
		"fail38" | Error.REL_QUAL_UNKNOWN
		"fail39" | Error.TEMPL_UNKNOWN_PARAM
		"fail40" | Error.TYPE_QUAL_DECL
		"fail41" | Error.REL_QUAL_DECL
		"fail42" | Error.REL_QUAL_DECL
		"fail43" | Error.INST_CYCLE
		"fail44" | Error.REL_TEMPL_CYCLE
		"fail45" | Error.AGGR_UNSUPPORTED_REL
		"fail46" | Error.REL_NEGATION_CYCLE
		"fail47" | Error.VAR_DONTCARE_HEAD
		"fail48" | Error.VAR_CONSTR_BODY
		"fail49" | Error.REL_NAME_DEFCONSTR
		"fail50" | Error.TYPE_OPT_ROOT_NONOPT
		"fail51" | Error.TYPE_OPT_CONSTR
		"fail52" | Error.TYPE_UNKNOWN
		"fail53" | Error.CONSTR_AS_REL
		"fail54" | Error.SMART_LIT_NON_PRIMITIVE
		"fail55" | Error.SMART_LIT_NO_DIRECT_REL
		"fail56" | Error.SMART_LIT_NO_DIRECT_REL
		"fail57" | Error.TYPE_INF_INCOMPAT
		"fail58" | Error.TYPE_INF_INCOMPAT
		"fail59" | Error.REL_ARITY
		"fail60" | Error.TYPE_INF_INCOMPAT
		"fail61" | Error.DECL_MULTIPLE
		"fail62" | Error.TEXT_HEAD
		"fail63" | Error.TEXT_BODY
		"fail64" | Error.TEXT_BODY
		"fail65" | Error.TEXT_HEAD_NON_VAR
		"fail66" | Error.TEXT_BODY_NON_VAR
		"fail67" | Error.TEXT_MULTIPLE_RELS
		"fail68" | Error.TEXT_UNKNOWN
		"fail69" | Error.TEXT_LIT_N_VAR
		"fail70" | Error.TYPE_UNKNOWN
		"fail71" | Error.TYPE_UNKNOWN
		"fail72" | Error.REL_QUAL_DECL
		"fail73" | Error.REL_QUAL_DECL
		"fail74" | Error.REL_NO_DECL
		"fail75" | Error.REL_NO_DECL
		"fail76" | Error.CONSTANT_HEAD
		"fail77" | Error.CONSTANT_ARITY
		"fail78" | Error.CONSTANT_NON_PRIMITIVE
		"fail79" | Error.CONSTANT_BODY
		"fail80" | Error.CONSTANT_AS_REL
		"fail81" | Error.INLINE_HEAD
		"fail82" | Error.INLINE_HEAD_NONVARS
		"fail83" | Error.INLINE_HEAD_NONVARS
		"fail84" | Error.INLINE_HEAD_DUPVARS
		"fail85" | Error.INLINE_INVALID_ANN
		"fail86" | Error.INLINE_RECURSION
		"fail87" | Error.INLINE_NOTIN_BODY
		"fail88" | Error.INLINE_NOTIN_BODY
		"fail89" | Error.INLINE_HEAD
		"fail90" | Error.INLINE_AS_CONSTR
		"fail91" | Error.INLINE_AS_TYPE
		"fail92" | Error.CONSTANT_INVALID_ANN
		"fail93" | Error.DECL_MULTIPLE
	}

	@Unroll
	def "Souffle failing tests"() {
		when:
		test("failing/$file", SouffleCodeGenerator)

		then:
		def e = thrown(PandaException)
		e.error == expectedErrorId

		where:
		file      | expectedErrorId
		"fail-S0" | Error.VAR_ASGN_CYCLE
		"fail-S1" | Error.VAR_ASGN_COMPLEX
	}

	def testAndCompare(String file) {
		def compiler = test(file, SouffleCodeGenerator)
		// Compare contents
		def generatedFile = compiler.codeGenerator.artifacts.find { it instanceof Compiler.LogicFile }
		def expectedFileURL = this.class.getResource("/expected/exp-${file}.dl")
		def expectedFile = expectedFileURL ? new File(expectedFileURL.toURI()) : null

		if (!expectedFile)
			compiler.error(Error.EXP_CONTENTS_MISSING, file)
		if (!FileUtils.contentEqualsIgnoreEOL(generatedFile, expectedFile, null))
			compiler.error(Error.EXP_CONTENTS_MISMATCH)
	}

	def test(String file, Class codeGen) {
		def resourcePath = "/${file}.pnd"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def input = new File(this.class.getResource(resourcePath).file)

		new Compiler(codeGen, input, new File("build/out_test"), null,
				new File("build/logs/panda-test.log"))
				.run(inputStream)
	}
}
