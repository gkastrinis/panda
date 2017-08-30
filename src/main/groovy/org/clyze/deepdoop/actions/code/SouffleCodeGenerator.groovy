package org.clyze.deepdoop.actions.code

import groovy.transform.InheritConstructors
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.ValidationVisitingActor
import org.clyze.deepdoop.actions.tranform.InitializingTransformer
import org.clyze.deepdoop.actions.tranform.NormalizingTransformer
import org.clyze.deepdoop.actions.tranform.SouffleAssignTransformer
import org.clyze.deepdoop.actions.tranform.SouffleConstructorTransformer
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
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.Kind.*

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
				.accept(inferenceActor)
				.accept(new SouffleAssignTransformer())
				.accept(new SouffleConstructorTransformer(infoActor, inferenceActor))

		return super.visit(n as Program)
	}

	String visit(Component n) {
		emit "/// Declarations"
		inferenceActor.inferredTypes.each { predName, types -> decl(predName, types) }
		n.declarations.each { m[it] = it.accept(this) }

		//emit "/// Constraints"
		//n.constraints.each { m[it] = it.accept(this) }

		emit "/// Special rules to propagate data to supertypes"
		infoActor.directSuperType.each { emit "${mini(it.value)}(x) :- ${mini(it.key)}(x)." }
		emit "/// Rules"
		n.rules.each { m[it] = it.accept(this) }
	}

	def decl(def predName, def types) {
		def params = types.withIndex().collect { type, i -> "var$i:${map(type)}" }.join(", ")
		emit ".decl ${mini(predName)}($params) // ${types.join(" x ")}"
	}

	String exit(Declaration n, Map<IVisitable, String> m) {
		if (!inferenceActor.inferredTypes[n.atom.name])
			decl(n.atom.name, n.types.collect { it.name })
		if (INPUT in n.annotations)
			emit ".input ${mini(n.atom.name)}"
		if (OUTPUT in n.annotations)
			emit ".output ${mini(n.atom.name)}"
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

	static def mini(def name) { name.replace ":", "_" }

	static def map(def name) { name == "string" ? "symbol" : "number" }
}
