package org.codesimius.panda.actions.code

import groovy.transform.InheritConstructors
import org.codesimius.panda.actions.tranform.souffle.AssignTransformer
import org.codesimius.panda.actions.tranform.souffle.ConstructorTransformer
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.RecordType
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.RecordExpr
import org.codesimius.panda.system.Artifact

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	BlockLvl0 datalog
	Rule rule

	String visit(BlockLvl2 p) {
		createUniqueFile("out_", ".dl")
		artifacts << new Artifact(Artifact.Kind.LOGIC, currentFile)

		def steps = transformations + [
				new ConstructorTransformer(compiler, typeInferenceTransformer),
				new AssignTransformer(compiler)
		]

		def n = steps.inject(p) { prog, step -> prog.accept(step) }
		super.visit(n)
	}

	void enter(BlockLvl0 n) { datalog = n }

	String visit(RelDeclaration n) {
		def relName = fix(n.relation.name)
		def params = n.types.withIndex().collect { t, int i -> "${var1(i)}:${tr(fix(t.name))}" }
		def meta = n.annotations[METADATA]?.args?.get("types")
		emit ".decl $relName(${params.join(", ")})${meta ? " // $meta" : ""}"

		if (INPUT in n.annotations) {
			def an = n.annotations[INPUT]
			def filename = an["filename"] ?: "${n.relation.name}.facts"
			def delimeter = an["delimeter"] ?: "\\t"
			emit """.input $relName(filσename="$filename", delimeter="$delimeter")"""
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

	void enter(Rule n) { rule = n }

	String exit(Rule n) {
		def body = m[n.body]
		emit "${m[n.head]}${body ? " :- $body" : ""}."
		if (PLAN in n.annotations) emit ".plan ${n.annotations[PLAN]["plan"].value}"
		null
	}

	String exit(AggregationElement n) {
		def pred = n.relation.name
		def soufflePred = n.relation.exprs ? "$pred(${m[n.relation.exprs.first()]})" : pred
		def headVars = datalog.getHeadVars(rule)
		def extraBody = datalog.getBoundBodyVars(rule).any { it in headVars } ? "${m[n.body]}, " : ""
		"${extraBody}${m[n.var]} = $soufflePred : { ${m[n.body]} }"
	}

	String exit(Constructor n) { exit(n as Relation) }

	String exit(Relation n) { "${fix(n.name)}(${n.exprs.collect { m[it] }.join(", ")})" }

	String exit(Type n) { fix n.name }

	// Must override since the default implementation throws an exception
	String visit(RecordExpr n) {
		n.exprs.each { m[it] = visit it }
		"[${n.exprs.collect { visit(it) }.join(", ")}]"
	}

	static def fix(def s) { s.replace ":", "_" }

	static def tr(def name) {
		if (name == "string") return "symbol"
		else if (name == "int") return "number"
		else return "__SYS_TYPE_$name"
	}
}
