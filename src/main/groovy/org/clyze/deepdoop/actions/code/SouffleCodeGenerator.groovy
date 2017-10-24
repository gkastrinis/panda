package org.clyze.deepdoop.actions.code

import groovy.transform.InheritConstructors
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.ValidationVisitingActor
import org.clyze.deepdoop.actions.tranform.InitializingTransformer
import org.clyze.deepdoop.actions.tranform.NormalizingTransformer
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

import static org.clyze.deepdoop.datalog.Annotation.Kind.*
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	String visit(Program p) {
		currentFile = createUniqueFile("out_", ".dl")
		results << new Result(Result.Kind.LOGIC, currentFile)

		// Transform program before visiting nodes
		def n = p.accept(new NormalizingTransformer())
				.accept(new InitializingTransformer())
				.accept(infoActor)
				.accept(new ValidationVisitingActor(infoActor))
				.accept(typeInferenceActor)
				.accept(new ConstructorTransformer(infoActor, typeInferenceActor))
				.accept(new AssignTransformer(infoActor))

		return super.visit(n as Program)
	}

	String exit(Declaration n, Map<IVisitable, String> m) {
		def name = n.atom.name
		def params = n.types.withIndex().collect { t, int i -> "${var1(i)}:${map(t.name)}" }.join(", ")
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

	String exit(Rule n, Map<IVisitable, String> m) {
		emit(n.body ? "${m[n.head]} :- ${m[n.body]}." : "${m[n.head]}.")
		if (PLAN in n.annotations) emit ".plan ${n.annotations[PLAN].args["plan"]}"
		null
	}

	String exit(AggregationElement n, Map<IVisitable, String> m) {
		def pred = n.relation.name
		def soufflePred = n.relation.exprs ? "$pred(${m[n.relation.exprs.first()]})" : pred
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"${m[n.body]}, ${m[n.var]} = $soufflePred : { ${m[n.body]} }"
		else null
	}

	String exit(ConstructionElement n, Map<IVisitable, String> m) {
		"${m[n.constructor]}, ${m[n.type]}"
	}

	String exit(Constructor n, Map<IVisitable, String> m) { exit(n as Relation, m) }

	String exit(Relation n, Map<IVisitable, String> m) {
		"${mini(n.name)}(${n.exprs.collect { m[it] }.join(", ")})"
	}

	String exit(Type n, Map<IVisitable, String> m) { exit(n as Relation, m) }

	// Must override since the default implementation throws an exception
	String visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	String exit(RecordExpr n, Map<IVisitable, String> m) {
		"[${n.exprs.collect { m[it] }.join(", ")}]"
	}

	static def mini(def name) { name.replace ":", "_" }

	static def map(def name) {
		if (name == "string") return "symbol"
		else if (name == "int") return "number"
		else return "__T_$name"
	}
}
