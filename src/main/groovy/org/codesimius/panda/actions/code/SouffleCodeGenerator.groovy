package org.codesimius.panda.actions.code

import groovy.transform.InheritConstructors
import org.codesimius.panda.actions.tranform.souffle.AssignTransformer
import org.codesimius.panda.actions.tranform.souffle.ConstructorTransformer
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.RecordType
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.*
import org.codesimius.panda.system.Compiler

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	private Rule currRule

	String visit(BlockLvl2 p) {
		def steps = transformations + [
				new ConstructorTransformer(compiler, typeInferenceTransformer),
				new AssignTransformer(compiler)
		]

		def n = steps.inject(p) { prog, step -> prog.accept(step) }
		super.visit(n)
	}

	String visit(RelDeclaration n) {
		def relName = fix(n.relation.name)
		def params = n.types.withIndex().collect { t, int i -> "${var1(i)}:${tr(fix(t.name))}" }
		def meta = n.annotations[METADATA]?.args?.get("types")
		emit ".decl $relName(${params.join(", ")})${meta ? " // $meta" : ""}"

		if (INPUT in n.annotations) {
			def an = n.annotations[INPUT]
			def filename = an["filename"] ?: "${n.relation.name}.facts"
			def delimiter = an["delimiter"] ?: "\\t"
			emit """.input $relName(filename="$filename", delimiter="$delimiter")"""
		}
		if (OUTPUT in n.annotations)
			emit ".output $relName"
		null
	}

	String visit(TypeDeclaration n) {
		def params = (n.supertype as RecordType).innerTypes.withIndex().collect { t, int i -> "${var1(i)}:${tr(fix(t.name))}" }
		emit ".type ${tr(fix(n.type.name))} = [${params.join(", ")}]"
		null
	}

	void enter(Rule n) { currRule = n }

	String exit(Rule n) {
		def head = m[n.head]
		def body = m[n.body]
		if (!body)
			emit "$head :- 1=1."
		else
			emit "$head :- $body."

		if (PLAN in n.annotations) emit ".plan ${n.annotations[PLAN]["plan"].value}"
		null
	}

	String exit(AggregationElement n) {
		def pred = n.relation.name
		def soufflePred = n.relation.exprs ? "$pred(${m[n.relation.exprs.first()]})" : pred
		def headVars = currDatalog.getHeadVars(currRule)
		def extraBody = currDatalog.getBoundBodyVars(currRule).any { it in headVars } ? "${m[n.body]}, " : ""
		"${extraBody}${m[n.var]} = $soufflePred : { ${m[n.body]} }"
	}

	String exit(Constructor n) { exit(n as Relation) }

	String exit(Relation n) { "${fix(n.name)}(${n.exprs.collect { m[it] }.join(", ")})" }

	String exit(Type n) { fix n.name }

	String exit(BinaryExpr n) {
		switch (n.op) {
			case BinaryOp.CAT:
				return "cat(${m[n.left]}, ${m[n.right]})"
			default:
				return super.exit(n)
		}
	}

	String exit(UnaryExpr n) {
		switch (n.op) {
			case UnaryOp.TO_STR:
				return "to_string(${m[n.expr]})"
			case UnaryOp.TO_ORD:
				return "ord(${m[n.expr]})"
			default:
				return null
		}
	}

	// Must override since the default implementation throws an exception
	String visit(RecordExpr n) {
		n.exprs.each { m[it] = visit it }
		"[${n.exprs.collect { visit(it) }.join(", ")}]"
	}

	Compiler.Artifact getOutputArtifact(File output) {
		output ? new Compiler.LogicFile(output.path) : Compiler.LogicFile.createUniqueFile("out_", ".dl", outDir)
	}

	static def fix(def s) { s.replaceAll "[:.]", "_" }

	static def tr(def name) {
		switch (name) {
			case "string": return "symbol"
			case "int": return "number"
			default: return "__SYS_TYPE_$name"
		}
	}
}
