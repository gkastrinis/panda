package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE

class InfoCollectionVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	// Symbol table information
	Map<IVisitable, Set<Relation>> declaringAtoms = [:].withDefault { [] as Set }
	Map<IVisitable, Set<Relation>> usedAtoms = [:].withDefault { [] as Set }
	Map<IVisitable, List<VariableExpr>> vars = [:].withDefault { [] }

	// Type information
	Set<String> allTypes = [] as Set
	Map<String, String> directSuperType = [:]
	Map<String, List<String>> superTypesOrdered = [:].withDefault { [] }
	Map<String, Set<String>> subTypes = [:].withDefault { [] as Set }

	// Constructor information
	Set<String> allConstructors = [] as Set
	Map<String, String> constructorBaseType = [:]
	Map<String, Set<String>> constructorsPerType = [:].withDefault { [] as Set }
	Map<Rule, List<Constructor>> constructorsOrderedPerRule = [:]

	// Predicate Name x Set of Rules
	Map<String, Set<Rule>> affectedRules = [:].withDefault { [] as Set }

	List<String> refmodeRelations = []
	// Map functional relations to their arity
	Map<String, Integer> functionalRelations = [:]

	InfoCollectionVisitingActor() { actor = this }

	IVisitable exit(Program n, Map m) {
		declaringAtoms[n] = declaringAtoms[n.globalComp] +
				(n.comps.values().collect { declaringAtoms[it] }.flatten() as Set<Relation>)
		usedAtoms[n] = usedAtoms[n.globalComp] +
				(n.comps.collect { usedAtoms[it.value] }.flatten() as Set<Relation>)

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
		declaringAtoms[n] = declMap

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
		usedAtoms[n] = usedMap
		null
	}
	*/

	IVisitable exit(Component n, Map m) {
		declaringAtoms[n] = (n.declarations + n.rules).collect { declaringAtoms[it] }.flatten() as Set<Relation>
		usedAtoms[n] = (n.declarations + n.rules).collect { usedAtoms[it] }.flatten() as Set<Relation>
		null
	}

	IVisitable exit(Declaration n, Map m) {
		declaringAtoms[n] = [n.atom] as Set
		usedAtoms[n] = n.types as Set

		def predName = n.atom.name

		if (TYPE in n.annotations) {
			allTypes << predName
			if (!n.types.isEmpty()) directSuperType[predName] = n.types.first().name
		}

		if (CONSTRUCTOR in n.annotations) {
			def type = n.types.last().name
			constructorBaseType[predName] = type
			constructorsPerType[type] << predName

			def refmode = n.annotations[CONSTRUCTOR].args["refmode"]
			if (refmode && refmode.type == ConstantExpr.Type.BOOLEAN && refmode.value)
				refmodeRelations << n.atom.name
		}

		null
	}

	IVisitable exit(Rule n, Map m) {
		// Atoms used in the head, are declared in the rule
		declaringAtoms[n] = usedAtoms[n.head]
		usedAtoms[n] = usedAtoms[n.body]

		n.body?.elements
				?.findAll { it instanceof Relation }
				?.each { affectedRules[(it as Relation).name] << n }

		// Count how many times a variable is constructed in the head
		// It should be at max once
		def varConstructionCounter = [:].withDefault { 0 }
		// Order constructors appearing in the head based on their dependencies
		// If C2 needs a variable constructed by C1 it will be after C1
		List<Constructor> constructorsOrdered = []
		n.head?.elements
				?.findAll { it instanceof Constructor }
				?.collect { it as Constructor }
				?.each { con ->
			def loc = SourceManager.instance.recall(con)

			varConstructionCounter[con.valueExpr]++
			if (varConstructionCounter[con.valueExpr] > 1)
				ErrorManager.error(loc, ErrorId.VAR_MULTIPLE_CONSTR, con.valueExpr)

			// Max index of a constructor that constructs a variable used by `con`
			def maxBefore = con.keyExprs
					.collect { e -> constructorsOrdered.findIndexOf { it.valueExpr == e } }
					.max()
			// Min index of a constructor that uses the variable constructed by `con`
			def minAfter = constructorsOrdered.findIndexValues { con.valueExpr in it.keyExprs }
					.min()
			// `maxBefore` should be strictly before `minAfter`
			maxBefore = (maxBefore != -1 ? maxBefore : -2)
			minAfter = (minAfter != null ? minAfter : -1)
			if (maxBefore >= minAfter)
				ErrorManager.error(loc, ErrorId.CONSTR_RULE_CYCLE, con.name)

			constructorsOrdered.add(maxBefore >= 0 ? maxBefore : 0, con)
		}
		constructorsOrderedPerRule[n] = constructorsOrdered

		null
	}

	IVisitable exit(AggregationElement n, Map m) {
		usedAtoms[n] = usedAtoms[n.body]

		vars[n] = vars[n.body] + vars[n.predicate]
		null
	}

	IVisitable exit(ComparisonElement n, Map m) {
		vars[n] = vars[n.expr]
		null
	}

	IVisitable exit(GroupElement n, Map m) {
		usedAtoms[n] = usedAtoms[n.element]

		vars[n] = vars[n.element]
		null
	}

	IVisitable exit(LogicalElement n, Map m) {
		usedAtoms[n] = n.elements.collect { usedAtoms[it] }.flatten() as Set<Relation>

		vars[n] = n.elements.collect { vars[it] }.flatten() as List<VariableExpr>
		null
	}

	IVisitable exit(NegationElement n, Map m) {
		usedAtoms[n] = usedAtoms[n.element]

		vars[n] = vars[n.element]
		null
	}

	IVisitable exit(Constructor n, Map m) {
		exit(n as Functional, m)
		allConstructors << n.name
		null
	}

	IVisitable exit(Type n, Map m) {
		exit(n as Predicate, m)
	}

	IVisitable exit(Functional n, Map m) {
		usedAtoms[n] = [n] as Set

		vars[n] = (n.keyExprs + [n.valueExpr]).collect { vars[it] }.flatten() as List<VariableExpr>

		functionalRelations[n.name] = n.arity
		null
	}

	IVisitable exit(Predicate n, Map m) {
		usedAtoms[n] = [n] as Set

		vars[n] = n.exprs.collect { vars[it] }.flatten() as List<VariableExpr>
		null
	}

	IVisitable exit(Primitive n, Map m) {
		vars[n] = [n.var]
		null
	}

	IVisitable exit(BinaryExpr n, Map m) {
		vars[n] = vars[n.left] + vars[n.right]
		null
	}

	IVisitable exit(GroupExpr n, Map m) {
		vars[n] = vars[n.expr]
		null
	}

	IVisitable exit(VariableExpr n, Map m) {
		vars[n] = [n]
		null
	}
}
