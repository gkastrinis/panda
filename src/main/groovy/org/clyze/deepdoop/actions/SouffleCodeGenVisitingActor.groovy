package org.clyze.deepdoop.actions

import groovy.transform.InheritConstructors
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.Kind.*

@InheritConstructors
class SouffleCodeGenVisitingActor extends DefaultCodeGenVisitingActor {

	static class Extra {
		// Unbound variable in a rule's head that a constructor will bound eventually
		Map<IExpr, Constructor> unboundVar = [:]
		// Relation to full relation representation
		Map<Relation, String> relationToFull = [:]
		// Full relation to partial relation mapping
		Map<String, String> fullToPartial = [:]
		// Relation depends on unbound vars to take values
		Map<Relation, List<IExpr>> unboundVarsForRelation = [:]
		// Relation to list of variables
		Map<Relation, List<IExpr>> varsForRelation = [:]
	}
	Extra extra

	// Types for partial relations
	Set<String> partialDeclarations = [] as Set

	String visit(Program p) {
		currentFile = createUniqueFile("out_", ".dl")
		results << new Result(Result.Kind.LOGIC, currentFile)

		// Transform program before visiting nodes
		def n = p.accept(new NormalizeVisitingActor())
				.accept(new InitVisitingActor())
				.accept(infoActor)
				.accept(new ValidationVisitingActor(infoActor))
				.accept(inferenceActor)

		return super.visit(n as Program)
	}

	void enter(Program n) {
		emit "/// Declarations of normal relations"
		inferenceActor.inferredTypes.each { predName, types ->
			def params = types.withIndex().collect { type, i -> "x$i:${mapType(type)}" }.join(", ")
			emit ".decl ${mini(predName)}($params) // ${types.join(" x ")}"
		}
		emit "/// Declarations of type relations"
		infoActor.allTypes.each { emit ".decl ${mini(it)}(x:symbol)" }
		emit "/// Special rules to propagate data to supertypes"
		infoActor.directSuperType.each { emit "${mini(it.value)}(x) :- ${mini(it.key)}(x)." }
		emit "/// Rules"
	}

	String exit(Program n, Map<IVisitable, String> m) {
		partialDeclarations.each { emit ".decl $it" }
		null
	}

	void enter(Declaration n) {
		extra = new Extra()
	}

	String exit(Declaration n, Map<IVisitable, String> m) {
		if (INPUT in n.annotations)
			emit ".input ${mini(n.atom.name)}"
		if (OUTPUT in n.annotations)
			emit ".output ${mini(n.atom.name)}"
		extra = null
		null
	}

	void enter(Rule n) {
		extra = new Extra()
		extra.unboundVar = n.head.elements
				.findAll { it instanceof Constructor }
				.collect { it as Constructor }
				.collectEntries { [(it.valueExpr): it] }
	}

	String exit(Rule n, Map<IVisitable, String> m) {
		// Potentially a rule for partial predicates
		emit(n.body ? "${m[n.head]} :- ${m[n.body]}." : "${m[n.head]}.")
		// TODO TEMP HACK
		if (PLAN in n.annotations) emit ".plan ${n.annotations[PLAN].values["val"]}"

		// Rules for populating full predicates from partial ones
		extra.unboundVarsForRelation.each { atom, vars ->
			def atomVars = extra.varsForRelation[atom].collect { (it as VariableExpr).name }
			def fullPred = extra.relationToFull[atom]
			def partialPred = extra.fullToPartial[fullPred]
			def body = ((partialPred ? [partialPred] : []) +
					vars.collect {
						def con = extra.unboundVar[it]
						def params = (con.keyExprs + [con.valueExpr])
								.collect { (it as VariableExpr).name }
								.collect { it in atomVars ? it : "_" }
								.join(", ")
						return "${mini(con.name)}($params)"
					}).join(", ")
			emit "$fullPred :- $body."
		}
		extra.unboundVar.values().each { con ->
			def fullPred = extra.relationToFull[con]
			def partialPred = extra.fullToPartial[fullPred]
			emit "$fullPred :- $partialPred."
		}
		extra = null
	}

	String exit(AggregationElement n, Map<IVisitable, String> m) {
		def pred = n.predicate.name
		def soufflePred = n.predicate.exprs ? "$pred(${m[n.predicate.exprs.first()]})" : pred
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"${m[n.body]}, ${m[n.var]} = $soufflePred : { ${m[n.body]} }"
		else null
	}

	String exit(Constructor n, Map<IVisitable, String> m) {
		if (!extra) return null

		def allParams = (n.keyExprs.collect { m[it] } + ['$']).join(", ")
		def fullPred = "${mini(n.name)}($allParams)"
		def boundParams = n.keyExprs.collect { m[it] }.join(", ")
		def partialPred = "${mini(n.name)}__pArTiAl($boundParams)"
		extra.relationToFull[n] = fullPred
		extra.fullToPartial[fullPred] = partialPred
		def types = inferenceActor.inferredTypes[n.name]
		def partialTypes = n.keyExprs.withIndex()
				.collect { expr, int i -> "var$i:${mapType(types[i])}" }
				.join(", ")
		partialDeclarations << "${mini(n.name)}__pArTiAl($partialTypes)"

		return partialPred
	}

	String exit(Functional n, Map<IVisitable, String> m) {
		return exitRelation(n, m, n.keyExprs + [n.valueExpr])
	}

	String exit(Predicate n, Map<IVisitable, String> m) {
		return exitRelation(n, m, n.exprs)
	}

	private String exitRelation(Relation n, Map<IVisitable, String> m, List<IExpr> exprs) {
		def allParams = exprs.collect { m[it] }.join(", ")
		def fullPred = "${mini(n.name)}($allParams)"
		extra.relationToFull[n] = fullPred
		extra.varsForRelation[n] = exprs

		def unboundVars = exprs.findAll { extra.unboundVar[it] }

		if (unboundVars) {
			extra.unboundVarsForRelation[n] = unboundVars
			def boundParams = exprs.findAll { !(it in unboundVars) }.collect { m[it] }.join(", ")
			def partialPred = boundParams ? "${mini(n.name)}__pArTiAl($boundParams)" : null
			extra.fullToPartial[fullPred] = partialPred
			def types = inferenceActor.inferredTypes[n.name]
			def partialTypes = exprs.withIndex()
					.findAll { !((it[0] as IExpr) in unboundVars) }
					.collect { expr, int i -> "var$i:${mapType(types[i])}" }
					.join(", ")
			if (partialTypes)
				partialDeclarations << "${mini(n.name)}__pArTiAl($partialTypes)"
			return partialPred
		} else
			return fullPred
	}

	static def mini(def name) { name.replace ":", "_" }

	static def mapType(def name) {
		name == "string" ? "symbol" : "number"
	}
}
