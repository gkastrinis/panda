package org.codesimius.panda

import org.antlr.v4.runtime.ANTLRInputStream
import org.apache.commons.io.FileUtils
import org.codesimius.panda.actions.code.DefaultCodeGenerator
import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.system.Artifact
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error
import org.codesimius.panda.system.PandaException
import spock.lang.Specification
import spock.lang.Unroll

import static org.codesimius.panda.system.Log.error

class DatalogSpec extends Specification {

	@Unroll
	def "Passing tests"() {
		when:
		test(file)

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
		"t12-freeText"      | _
		"t13-templates1"    | _
		"t14-templates2"    | _
		"t15-templates3"    | _
		"t16-templates4"    | _
		"t17-templates5"    | _
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
		"fail11" | Error.TYPE_INF_INCOMPAT
		"fail12" | Error.REL_ARITY
		"fail13" | Error.TYPE_INF_INCOMPAT_USE
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
		"fail25" | Error.TYPE_INF_INCOMPAT
		"fail26" | Error.COMP_DUPLICATE_PARAMS
		"fail27" | Error.COMP_SUPER_PARAM_MISMATCH
		"fail28" | Error.COMP_INST_ARITY
		"fail29" | Error.REL_EXT_INVALID
		"fail30" | Error.INST_UNKNOWN
		"fail31" | Error.VAR_UNKNOWN
		"fail32" | Error.TYPE_INF_FAIL
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
		"fail53" | Error.CONSTR_AS_REL
		"fail54" | Error.SMART_LIT_NON_PRIMITIVE
		"fail55" | Error.SMART_LIT_NO_DIRECT_REL
		"fail56" | Error.SMART_LIT_NO_DIRECT_REL
		"fail57" | Error.TYPE_INF_INCOMPAT
		"fail58" | Error.TYPE_INF_INCOMPAT
		"fail59" | Error.REL_ARITY
		"fail60" | Error.TYPE_INF_INCOMPAT
		"fail61" | Error.DECL_MULTIPLE
		"fail62" | Error.TEXT_MALFORMED_HEAD
		"fail63" | Error.TEXT_MALFORMED_BODY
		"fail64" | Error.TEXT_MALFORMED_BODY
		"fail65" | Error.TEXT_HEAD_NON_VAR
		"fail66" | Error.TEXT_BODY_NON_VAR
		"fail67" | Error.TEXT_MULTIPLE_RELS
		"fail68" | Error.TEXT_UNKNOWN
		"fail69" | Error.TEXT_LIT_N_VAR
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

	def test(String file) {
		def artifacts = test0(file, SouffleCodeGenerator)
		// Validate Contents
		def generatedFile = artifacts.find { it.kind == Artifact.Kind.LOGIC }.file
		def expectedFileURL = this.class.getResource("/expected/exp-${file}.dl")
		def expectedFile = expectedFileURL ? new File(expectedFileURL.toURI()) : null

		if (expectedFile?.exists() && !FileUtils.contentEquals(generatedFile, expectedFile))
			error(Error.EXP_CONTENTS_MISMATCH, null)
	}

	def test0(String file, Class codeGen) {
		def resourcePath = "/${file}.pnd"
		def inputStream = new ANTLRInputStream(this.class.getResourceAsStream(resourcePath))
		def resource = this.class.getResource(resourcePath).file
		def codeGenerator = codeGen.newInstance("build/out") as DefaultCodeGenerator
		Compiler.run(inputStream, resource, codeGenerator)
		return codeGenerator.artifacts
	}
}
