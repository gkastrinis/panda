package org.codesimius.panda.actions.code

import groovy.transform.InheritConstructors
import org.codesimius.panda.actions.graph.DependencyGraphVisitor
import org.codesimius.panda.actions.tranform.*
import org.codesimius.panda.actions.tranform.souffle.AssignTransformer
import org.codesimius.panda.actions.tranform.souffle.ConstructorTransformer
import org.codesimius.panda.actions.validation.MainValidator
import org.codesimius.panda.actions.validation.PreliminaryValidator
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
import org.codesimius.panda.system.Result

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	String visit(BlockLvl2 p) {
		createUniqueFile("out_", ".dl")
		results << new Result(Result.Kind.LOGIC, currentFile)

		// Transform program before visiting nodes
		def n = p.accept(new PreliminaryValidator())
				.accept(new SyntaxFlatteningTransformer())
				.accept(new ComponentInstantiationTransformer())
				.accept(new DependencyGraphVisitor(outDir))
				.accept(new ComponentFlatteningTransformer())
				.accept(symbolTable.typeInfo)
				.accept(new TypesTransformer(symbolTable))
				.accept(new InputFactsTransformer(symbolTable))
				.accept(symbolTable.relationInfo)
				.accept(symbolTable.varInfo)
				.accept(new MainValidator(symbolTable))
				.accept(typeInferenceTransformer)
				.accept(new TypesOptimizer(symbolTable))
				.accept(new ConstructorTransformer(symbolTable, typeInferenceTransformer))
				.accept(new AssignTransformer(symbolTable))

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
		"${m[n.body]}, ${m[n.var]} = $soufflePred : { ${m[n.body]} }"
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
