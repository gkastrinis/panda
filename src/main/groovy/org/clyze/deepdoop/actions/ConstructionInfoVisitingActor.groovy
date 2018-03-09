package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Error
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.Annotation.CONSTRUCTOR
import static org.clyze.deepdoop.system.Error.error as error

class ConstructionInfoVisitingActor extends DefaultVisitor<IVisitable> implements TDummyActor<IVisitable> {

	Map<String, Type> constructorBaseType = [:]
	Map<Type, Set<RelDeclaration>> constructorsPerType = [:].withDefault { [] as Set }
	Map<Rule, Set<VariableExpr>> constructedVars = [:]
	// Order constructors appearing in a rule head based on their dependencies
	// If C2 needs a variable constructed by C1, it will be after C1
	Map<Rule, List<ConstructionElement>> constructionsOrderedPerRule = [:]

	// Count how many times a variable is constructed in a rule head
	// It should be at max once
	private Map<VariableExpr, Integer> tmpConVarCounter = [:].withDefault { 0 }
	private Set<VariableExpr> tmpConVars
	private List<ConstructionElement> tmpConstructionsOrdered

	ConstructionInfoVisitingActor() { actor = this }

	IVisitable exit(BlockLvl2 n, Map m) { n }

	IVisitable visit(BlockLvl1 n) { throw new UnsupportedOperationException() }

	void enter(RelDeclaration n) {
		if (CONSTRUCTOR in n.annotations) {
			def type = n.types.last()
			constructorBaseType[n.relation.name] = type
			constructorsPerType[type] << n
		}
	}

	IVisitable visit(Rule n) {
		actor.enter(n)

		tmpConVarCounter.clear()
		tmpConVars = [] as Set
		tmpConstructionsOrdered = []
		visit n.head
		constructedVars[n] = tmpConVars
		constructionsOrderedPerRule[n] = tmpConstructionsOrdered

		actor.exit(n, m)
	}

	void enter(ConstructionElement n) {
		def loc = SourceManager.instance.recall(n)
		def conVar = n.constructor.valueExpr as VariableExpr
		tmpConVars << conVar
		tmpConVarCounter[conVar]++
		if (tmpConVarCounter[conVar] > 1) error(loc, Error.VAR_MULTIPLE_CONSTR, conVar)

		// Max index of a constructor that constructs a variable used by `n`
		def maxBefore = n.constructor.keyExprs
				.collect { e -> tmpConstructionsOrdered.findIndexOf { it.constructor.valueExpr == e } }
				.max()
		// Min index of a constructor that uses the variable constructed by `con`
		def minAfter = tmpConstructionsOrdered.findIndexValues { n.constructor.valueExpr in it.constructor.keyExprs }
				.min()
		// `maxBefore` should be strictly before `minAfter`
		maxBefore = (maxBefore != -1 ? maxBefore : -2)
		minAfter = (minAfter != null ? minAfter : -1)
		if (maxBefore >= minAfter) error(loc, Error.CONSTR_RULE_CYCLE, n.constructor.name)

		tmpConstructionsOrdered.add(maxBefore >= 0 ? maxBefore : 0, n)
	}
}
