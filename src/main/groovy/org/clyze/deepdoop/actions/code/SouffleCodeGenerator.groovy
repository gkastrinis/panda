package org.clyze.deepdoop.actions.code

import groovy.transform.InheritConstructors
import org.clyze.deepdoop.actions.tranform.AddonsTransformer
import org.clyze.deepdoop.actions.tranform.ComponentInstantiationTransformer
import org.clyze.deepdoop.actions.tranform.SyntaxFlatteningTransformer
import org.clyze.deepdoop.actions.tranform.souffle.ConstructorTransformer
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.RecordType
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.*
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	String visit(BlockLvl2 p) {
		currentFile = createUniqueFile("out_", ".dl")
		results << new Result(Result.Kind.LOGIC, currentFile)

		// Transform program before visiting nodes
		def n = p.accept(new SyntaxFlatteningTransformer())
				.accept(new ComponentInstantiationTransformer())
				.accept(typeInfoActor)
				.accept(relInfoActor)
				.accept(constructionInfoActor)
				.accept(new AddonsTransformer(typeInfoActor))
				.accept(typeInferenceTransformer)
//				//n=n.accept(new ValidationVisitingActor(constructionInfoActor))
				.accept(new ConstructorTransformer(typeInfoActor, typeInferenceTransformer, constructionInfoActor))
//				//.accept(new AssignTransformer(constructionInfoActor))

		super.visit(n)
	}

	String exit(RelDeclaration n, Map m) {
		def params = n.types.withIndex().collect { t, int i -> "${var1(i)}:${map(mini(t.name))}" }
		emit ".decl ${mini(n.relation.name)}(${params.join(", ")})"

		if (INPUT in n.annotations) {
			def args = n.annotations.find { it == INPUT }.args
			def filename = args["filename"] ?: "${n.relation.name}.facts"
			def delimeter = args["delimeter"] ?: "\\t"
			emit """.input ${mini(n.relation.name)}(filename="$filename", delimeter="$delimeter")"""
		}
		if (OUTPUT in n.annotations)
			emit ".output ${mini(n.relation.name)}"
		null
	}

	String exit(TypeDeclaration n, Map m) {
		def params = (n.supertype as RecordType).innerTypes.withIndex().collect { t, int i -> "${var1(i)}:${map(mini(t.name))}" }
		emit ".type ${map(mini(n.type.name))} = [${params.join(", ")}]"
		null
	}

	String exit(Rule n, Map m) {
		emit "${m[n.head]} :- ${m[n.body] ?: "1 = 1"}."

		if (PLAN in n.annotations) {
			def args = n.annotations.find { it == PLAN }.args
			// Remove quotes from start/end
			emit ".plan ${(args["plan"].value as String)[1..-2]}"
		}
		null
	}

	String exit(AggregationElement n, Map m) {
		def pred = n.relation.name
		def soufflePred = n.relation.exprs ? "$pred(${m[n.relation.exprs.first()]})" : pred
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"${m[n.body]}, ${m[n.var]} = $soufflePred : { ${m[n.body]} }"
		else null
	}

	String exit(ConstructionElement n, Map m) {
		"${m[n.constructor]}, ${m[n.type]}(${n.constructor.valueExpr})"
	}

	String exit(Constructor n, Map m) { exit(n as Relation, m) }

	String exit(Relation n, Map m) { "${mini(n.name)}(${n.exprs.collect { m[it] }.join(", ")})" }

	String exit(Type n, Map m) { "${mini(n.name)}" }

	// Must override since the default implementation throws an exception
	String visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	// Must override since the default implementation throws an exception
	String exit(RecordExpr n, Map m) { "[${n.exprs.collect { m[it] }.join(", ")}]" }

	static def mini(def name) { name.replace ":", "_" }

	static def map(def name) {
		if (name == "string") return "symbol"
		else if (name == "int") return "number"
		else return "__SYS_TYPE_$name"
	}
}
