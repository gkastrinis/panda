package org.clyze.deepdoop.actions.code

import groovy.transform.InheritConstructors
import org.clyze.deepdoop.actions.TypeHierarchyVisitingActor
import org.clyze.deepdoop.actions.ValidationVisitingActor
import org.clyze.deepdoop.actions.tranform.ComponentInitializingTransformer
import org.clyze.deepdoop.actions.tranform.SyntaxFlatteningTransformer
import org.clyze.deepdoop.actions.tranform.TypeValuesTransformer
import org.clyze.deepdoop.actions.tranform.souffle.AssignTransformer
import org.clyze.deepdoop.actions.tranform.souffle.ConstructorTransformer
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.*
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	String visit(Program p) {
		currentFile = createUniqueFile("out_", ".dl")
		results << new Result(Result.Kind.LOGIC, currentFile)

		def tmpTypeHierarchyVA = new TypeHierarchyVisitingActor()

		// Transform program before visiting nodes
		def n = p.accept(new SyntaxFlatteningTransformer())
				.accept(tmpTypeHierarchyVA)
				.accept(new TypeValuesTransformer(tmpTypeHierarchyVA))
				.accept(new ComponentInitializingTransformer())
				.accept(infoActor)
				.accept(new ValidationVisitingActor(infoActor))
				.accept(typeInferenceActor)
				.accept(new ConstructorTransformer(infoActor, typeInferenceActor))
				.accept(new AssignTransformer(infoActor))

		return super.visit(n)
	}

	String exit(Declaration n, Map m) {
		def name = n.relation.name
		def params = n.types.withIndex().collect { t, int i -> "${var1(i)}:${map(mini(t.name))}" }.join(", ")
		if (TYPE in n.annotations)
			emit ".type ${map(mini(name))} = [$params]"
		else
			emit ".decl ${mini(name)}($params)"

		if (INPUT in n.annotations)
			emit ".input ${mini(name)}"
		if (OUTPUT in n.annotations)
			emit ".output ${mini(name)}"
		null
	}

	String exit(Rule n, Map m) {
		def head = m[n.head]
		if (n.head.elements.size() > 1 && !n.body) emit "$head :- 1 = 1."
		else if (!n.body) emit "$head."
		else emit "$head :- ${m[n.body]}."

		if (PLAN in n.annotations) emit ".plan ${n.annotations.find { it == PLAN }.args["plan"]}"
		null
	}

	String exit(AggregationElement n, Map m) {
		def pred = n.relation.name
		def soufflePred = n.relation.exprs ? "$pred(${m[n.relation.exprs.first()]})" : pred
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"${m[n.body]}, ${m[n.var]} = $soufflePred : { ${m[n.body]} }"
		else null
	}

	void enter(ConstructionElement n) {
		n.type.exprs = [n.constructor.valueExpr]
	}

	String exit(ConstructionElement n, Map m) {
		"${m[n.constructor]}, ${m[n.type]}"
	}

	String exit(Constructor n, Map m) { exit(n as Relation, m) }

	String exit(Relation n, Map m) {
		"${mini(n.name)}(${n.exprs.collect { m[it] }.join(", ")})"
	}

	String exit(Type n, Map m) { exit(n as Relation, m) }

	// Must override since the default implementation throws an exception
	String visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	// Must override since the default implementation throws an exception
	String exit(RecordExpr n, Map m) {
		"[${n.exprs.collect { m[it] }.join(", ")}]"
	}

	static def mini(def name) { name.replace ":", "_" }

	static def map(def name) {
		if (name == "string") return "symbol"
		else if (name == "int") return "number"
		else return "_T_$name"
	}
}
