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
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.Kind.*
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

@InheritConstructors
class SouffleCodeGenerator extends DefaultCodeGenerator {

	ConstructorTransformer constructorTransformer

	// Relations that have an explicit declaration
	Set<String> explicitDeclarations = [] as Set

	String visit(Program p) {
		currentFile = createUniqueFile("out_", ".dl")
		results << new Result(Result.Kind.LOGIC, currentFile)

		constructorTransformer = new ConstructorTransformer(infoActor, typeInferenceActor)

		// Transform program before visiting nodes
		def n = p.accept(new NormalizingTransformer())
				.accept(new InitializingTransformer())
				.accept(infoActor)
				.accept(new ValidationVisitingActor(infoActor))
				.accept(typeInferenceActor)
				.accept(new AssignTransformer())
				.accept(constructorTransformer)

		return super.visit(n as Program)
	}

	String visit(Component n) {
		actor.enter(n)
		n.declarations.each { m[it] = it.accept(this) }

		// Handle explicit declarations
		typeInferenceActor.inferredTypes
				.findAll { rel, types -> !(rel in explicitDeclarations) }
				.each { rel, typeNames ->
			def types = typeNames.withIndex().collect { String t, int i -> new Predicate(t, null, [var1(i)]) }
			def d = new Declaration(new Predicate(rel, null, varN(types.size())), types)
			m[d] = d.accept(this)
		}

		n.rules.each { m[it] = it.accept(this) }
		actor.exit(n, m)
	}

	String exit(Declaration n, Map<IVisitable, String> m) {
		def name = n.atom.name
		explicitDeclarations << name

		def params = n.types.withIndex().collect { t, int i -> "${var1(i)}:${map(t.name)}" }.join(", ")
		if (n in constructorTransformer.recordDeclarations)
			emit ".type ${mini(name)} = [$params]"
		else
			emit ".decl ${mini(name)}($params)"

		if (INPUT in n.annotations)
			emit ".input ${mini(name)}"
		if (OUTPUT in n.annotations)
			emit ".output ${mini(name)}"
		null
	}

	String exit(Rule n, Map<IVisitable, String> m) {
		emit(m[n.body] ? "${m[n.head]} :- ${m[n.body]}." : "${m[n.head]}.")
		if (PLAN in n.annotations) emit ".plan ${n.annotations[PLAN].args["plan"]}"
		null
	}

	String exit(AggregationElement n, Map<IVisitable, String> m) {
		def pred = n.predicate.name
		def soufflePred = n.predicate.exprs ? "$pred(${m[n.predicate.exprs.first()]})" : pred
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"${m[n.body]}, ${m[n.var]} = $soufflePred : { ${m[n.body]} }"
		else null
	}

	String exit(Constructor n, Map<IVisitable, String> m) {
		exitRelation(n, m, n.keyExprs + [n.valueExpr])
	}

	String exit(Functional n, Map<IVisitable, String> m) {
		exitRelation(n, m, n.keyExprs + [n.valueExpr])
	}

	String exit(Predicate n, Map<IVisitable, String> m) {
		exitRelation(n, m, n.exprs)
	}

	static String exitRelation(Relation n, Map<IVisitable, String> m, List<IExpr> exprs) {
		"${mini(n.name)}(${exprs.collect { m[it] }.join(", ")})"
	}

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
		//name == "string" ? "symbol" : "number"
		if (name == "string") return "symbol"
		else if (name == "int") return "number"
		else return name
	}
}
