package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Error

import static org.clyze.deepdoop.datalog.Annotation.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.TYPE
import static org.clyze.deepdoop.system.Error.error
import static org.clyze.deepdoop.system.SourceManager.recallStatic as recall

class ConInfoCollector {

	Map<String, Type> constructorBaseType = [:]
	Map<Type, Set<RelDeclaration>> constructorsPerType = [:].withDefault { [] as Set }
	Map<Rule, Set<VariableExpr>> constructedVars = [:]
	// Order constructors appearing in a rule head based on their dependencies
	// If C2 needs a variable constructed by C1, it will be after C1
	Map<Rule, List<ConstructionElement>> constructionsOrderedPerRule = [:]
	// Count how many times a variable is constructed in a rule head
	// It should be at max once
	private List<VariableExpr> tmpConVars
	private List<ConstructionElement> tmpConstructionsOrdered

	// Types with no constructor in the hierarchy can be treated as symbols (strings)
	Set<Type> typesToOptimize = [] as Set
	private Set<Type> typesWithDefaultCon = [] as Set

	void exit(BlockLvl0 n, Set<Type> rootTypes, Map<Type, Set<Type>> subTypes) {
		rootTypes.each { root ->
			def types = [root] + subTypes[root]
			def constructors = types.collect { constructorsPerType[it] }.flatten() as Set<RelDeclaration>
			if (!constructors && !types.any { it in typesWithDefaultCon }) typesToOptimize += types
		}
	}

	void enter(RelDeclaration n) {
		if (CONSTRUCTOR in n.annotations) {
			def type = n.types.last()
			constructorBaseType[n.relation.name] = type
			constructorsPerType[type] << n
		}
	}

	void enter(TypeDeclaration n) {
		if (n.annotations.find { it == TYPE }.args["defaultConstructor"])
			typesWithDefaultCon << n.type
	}

	void enter(Rule n) {
		tmpConVars = []
		tmpConstructionsOrdered = []
	}

	void exit(Rule n) {
		constructedVars[n] = tmpConVars as Set
		constructionsOrderedPerRule[n] = tmpConstructionsOrdered
	}

	void enter(ConstructionElement n) {
		def conVar = n.constructor.valueExpr as VariableExpr
		if (conVar in tmpConVars) error(recall(n), Error.VAR_MULTIPLE_CONSTR, conVar)
		tmpConVars << conVar

		// Max index of a constructor that constructs a variable used by `n`
		def maxBefore = n.constructor.keyExprs
				.collect { e -> tmpConstructionsOrdered.findIndexOf { it.constructor.valueExpr == e } }
				.max()
		// Min index of a constructor that uses the variable constructed by `con`
		def minAfter = tmpConstructionsOrdered.findIndexValues {
			n.constructor.valueExpr in it.constructor.keyExprs
		}
		.min()
		// `maxBefore` should be strictly before `minAfter`
		maxBefore = (maxBefore != -1 ? maxBefore : -2)
		minAfter = (minAfter != null ? minAfter : -1)
		if (maxBefore >= minAfter) error(recall(n), Error.CONSTR_RULE_CYCLE, n.constructor.name)

		tmpConstructionsOrdered.add(maxBefore >= 0 ? maxBefore : 0, n)
	}
}
