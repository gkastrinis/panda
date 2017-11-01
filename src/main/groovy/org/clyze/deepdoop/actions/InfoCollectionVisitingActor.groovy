package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE

class InfoCollectionVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	// Symbol table information
	Map<IVisitable, Set<Relation>> declaredRelations = [:]
	Map<IVisitable, Set<Relation>> usedRelations = [:]
	Map<String, Set<Rule>> usedInRules = [:].withDefault { [] as Set }
	Map<IVisitable, Set<VariableExpr>> vars = [:]
	Map<Rule, Set<VariableExpr>> boundVars = [:]
	// Constructor information
	Map<Rule, Set<VariableExpr>> constructedVars = [:]
	Set<String> allConstructors = [] as Set
	Map<String, String> constructorBaseType = [:]
	Map<String, Set<String>> constructorsPerType = [:].withDefault { [] as Set }
	Map<Rule, List<ConstructionElement>> constructionsOrderedPerRule = [:]

	// Type information
	Set<String> allTypes = [] as Set
	Map<String, String> directSuperType = [:]
	Map<String, List<String>> superTypesOrdered = [:].withDefault { [] }
	Map<String, Set<String>> subTypes = [:].withDefault { [] as Set }


	Set<Relation> tmpRelations = [] as Set
	Set<VariableExpr> tmpVars = [] as Set
	Set<VariableExpr> tmpBoundVars = [] as Set
	Set<VariableExpr> tmpConVars
	// Count how many times a variable is constructed in a rule head
	// It should be at max once
	Map<VariableExpr, Integer> conVarCounter = [:].withDefault { 0 }
	// Order constructors appearing in a rule head based on their dependencies
	// If C2 needs a variable constructed by C1 it will be after C1
	List<ConstructionElement> constructionsOrdered

	InfoCollectionVisitingActor() { actor = this }

	IVisitable exit(Program n, Map m) {
		declaredRelations[n] = declaredRelations[n.globalComp] +
				(n.comps.values().collect { declaredRelations[it] }.flatten() as Set<Relation>)
		usedRelations[n] = usedRelations[n.globalComp] +
				(n.comps.collect { usedRelations[it.value] }.flatten() as Set<Relation>)

		// Base case for supertypes
		allTypes.each {
			def superType = directSuperType[it]
			if (superType) superTypesOrdered[it] << superType
		}
		// Transitive closure on supertypes
		def deltaTypes = superTypesOrdered.keySet() + []
		while (!deltaTypes.isEmpty()) {
			def newDeltaTypes = [] as Set
			deltaTypes.each {
				def lastSuperType = superTypesOrdered[it].last()
				def newTypes = lastSuperType ? superTypesOrdered[lastSuperType] : []
				if (newTypes) {
					superTypesOrdered[it].addAll(newTypes)
					newDeltaTypes << it
				}
			}
			deltaTypes = newDeltaTypes
		}
		superTypesOrdered.each { t, superTypes -> superTypes.each { subTypes[it] << t } }

		return n
	}

	/*
	IVisitable exit(CmdComponent n, Map m) {
		Map<String, Relation> declMap = [:]
		n.declarations.each { declMap << getDeclaringAtoms(it) }
		declaredRelations[n] = declMap

		Set<String> importPreds = [] as Set
		n.imports.each { p ->
			def pName = p.name
			importPreds << pName
			if (declMap[pName] == null)
				ErrorManager.error(ErrorId.CMD_NO_DECL, pName)
		}
		declMap.keySet().each { declName ->
			if (!(declName in importPreds))
				ErrorManager.error(ErrorId.CMD_NO_IMPORT, declName)
		}
		Map<String, Relation> usedMap = [:]
		n.exports.each { usedMap[it.name] = it }
		usedRelations[n] = usedMap
		null
	}
	*/

	IVisitable exit(Component n, Map m) {
		declaredRelations[n] = (n.declarations + n.rules).collect { declaredRelations[it] }.flatten() as Set<Relation>
		usedRelations[n] = (n.declarations + n.rules).collect { usedRelations[it] }.flatten() as Set<Relation>
		null
	}

	IVisitable exit(Declaration n, Map m) {
		declaredRelations[n] = [n.atom] as Set
		usedRelations[n] = n.types as Set

		def predName = n.atom.name
		if (n.annotations[TYPE]) {
			allTypes << predName
			if (n.types) directSuperType[predName] = n.types.first().name
		}
		if (n.annotations[CONSTRUCTOR]) {
			def type = n.types.last().name
			constructorBaseType[predName] = type
			constructorsPerType[type] << predName
		}
		null
	}

	IVisitable visit(Rule n) {
		actor.enter(n)

		tmpRelations = [] as Set
		tmpVars = [] as Set
		tmpConVars = [] as Set
		conVarCounter.clear()
		constructionsOrdered = []
		n.head.accept(this)
		// Relations used in the head (except in constructions)
		// are implicitly declared by the rule
		declaredRelations[n] = tmpRelations.findAll { it instanceof Relation } as Set
		usedRelations[n] = tmpRelations
		vars[n.head] = tmpVars
		constructedVars[n] = tmpConVars
		constructionsOrderedPerRule[n] = constructionsOrdered

		tmpRelations = [] as Set
		tmpVars = [] as Set
		tmpBoundVars = [] as Set
		n.body?.accept(this)
		usedRelations[n] += tmpRelations
		tmpRelations.each { usedInRules[it.name] << n }
		vars[n.body] = tmpVars
		boundVars[n] = tmpBoundVars

		actor.exit(n, m)
	}

	void enter(ConstructionElement n) {
		tmpRelations << n.type
		allConstructors << n.constructor.name

		def loc = SourceManager.instance.recall(n)
		def conVar = n.constructor.valueExpr as VariableExpr
		tmpConVars << conVar
		conVarCounter[conVar]++
		if (conVarCounter[conVar] > 1)
			ErrorManager.error(loc, ErrorId.VAR_MULTIPLE_CONSTR, conVar)

		// Max index of a constructor that constructs a variable used by `n`
		def maxBefore = n.constructor.keyExprs
				.collect { e -> constructionsOrdered.findIndexOf { it.constructor.valueExpr == e } }
				.max()
		// Min index of a constructor that uses the variable constructed by `con`
		def minAfter = constructionsOrdered.findIndexValues { n.constructor.valueExpr in it.constructor.keyExprs }
				.min()
		// `maxBefore` should be strictly before `minAfter`
		maxBefore = (maxBefore != -1 ? maxBefore : -2)
		minAfter = (minAfter != null ? minAfter : -1)
		if (maxBefore >= minAfter)
			ErrorManager.error(loc, ErrorId.CONSTR_RULE_CYCLE, n.constructor.name)

		constructionsOrdered.add(maxBefore >= 0 ? maxBefore : 0, n)
	}

	void enter(Constructor n) { enter(n as Relation) }

	void enter(Relation n) {
		tmpRelations << n
		tmpBoundVars += n.exprs.findAll { it instanceof VariableExpr }
	}

	void enter(VariableExpr n) { tmpVars << n }
}
