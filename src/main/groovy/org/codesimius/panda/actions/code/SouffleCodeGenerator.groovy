package org.codesimius.panda.actions.code

import groovy.transform.InheritConstructors
import org.codesimius.panda.actions.ValidationVisitor
import org.codesimius.panda.actions.graph.DependencyGraphVisitor
import org.codesimius.panda.actions.tranform.ComponentInstantiationTransformer
import org.codesimius.panda.actions.tranform.InputFactsTransformer
import org.codesimius.panda.actions.tranform.SyntaxFlatteningTransformer
import org.codesimius.panda.actions.tranform.TypesTransformer
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
import org.codesimius.panda.datalog.expr.RecordExpr
import org.codesimius.panda.system.DOTGenerator
import org.codesimius.panda.system.Result

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	String visit(BlockLvl2 p) {
		createUniqueFile("out_", ".dl")
		results << new Result(Result.Kind.LOGIC, currentFile)

		def dependencyGraphVisitor = new DependencyGraphVisitor()

		// Transform program before visiting nodes
		def n = p.accept(dependencyGraphVisitor)
				.accept(new SyntaxFlatteningTransformer())
				.accept(new ComponentInstantiationTransformer())
				.accept(relationInfo)
				.accept(new TypesTransformer(relationInfo))
				.accept(new InputFactsTransformer(relationInfo))
				.accept(relationInfo)
				.accept(varInfo)
				.accept(new ValidationVisitor(relationInfo, varInfo))
				.accept(typeInferenceTransformer)
				.accept(new ConstructorTransformer(relationInfo, typeInferenceTransformer))
				.accept(new AssignTransformer(varInfo))

		new DOTGenerator(outDir, dependencyGraphVisitor).gen()

		super.visit(n)
	}

	String visit(RelDeclaration n) {
		def relName = fix(n.relation.name)
		def params = n.types.withIndex().collect { t, int i -> "${var1(i)}:${tr(fix(t.name))}" }
		emit ".decl $relName(${params.join(", ")})"

		if (INPUT in n.annotations) {
			def args = n.annotations.find { it == INPUT }.args
			def filename = args["filename"] ?: "${n.relation.name}.facts"
			def delimeter = args["delimeter"] ?: "\\t"
			emit """.input $relName(filename="$filename", delimeter="$delimeter")"""
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

	String exit(Rule n) {
		emit "${m[n.head]} :- ${m[n.body] ?: "true"}."

		if (PLAN in n.annotations)
			emit ".plan ${n.annotations.find { it == PLAN }.args["plan"].value}"
		null
	}

	String exit(AggregationElement n) {
		def pred = n.relation.name
		def soufflePred = n.relation.exprs ? "$pred(${m[n.relation.exprs.first()]})" : pred
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"${m[n.body]}, ${m[n.var]} = $soufflePred : { ${m[n.body]} }"
		else null
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
