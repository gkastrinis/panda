package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.VariableExpr

class RelInfoCollector {

	Set<String> declaredRelations
	Map<BlockLvl0, Set<String>> declaredRelationsPerBlock = [:]
	Map<String, Set<Rule>> relUsedInRules = [:].withDefault { [] as Set }
	private Rule currRule

	// List instead of set so we can count occurrences (for validation)
	Map<IVisitable, List<VariableExpr>> vars = [:].withDefault { [] }
	Map<Rule, Set<VariableExpr>> boundVars = [:].withDefault { [] as Set }
	private Map<IElement, Set<VariableExpr>> elementToVars
	private Set<VariableExpr> tmpVars

	void enter(BlockLvl0 n) {
		// Implicitly, add relations supported in aggregation
		declaredRelations = ["count", "min", "max", "sum"] as Set
		declaredRelationsPerBlock[n] = declaredRelations
	}

	void enter(RelDeclaration n) { declaredRelations << n.relation.name }

	void enter(Rule n) {
		currRule = n
		elementToVars = [:]
		tmpVars = [] as Set
	}

	void exit(Rule n) {
		boundVars[n] = elementToVars[n.body]
	}

	void exit(LogicalElement n) {
		if (n.type == LogicalElement.LogicType.OR) {
			def intersection = elementToVars[n.elements.first()]
			n.elements.drop(1).each {
				def vs = elementToVars[it]
				if (vs) intersection = intersection.intersect(vs)
			}
			if (intersection) elementToVars[n] = intersection
		} else {
			def union = [] as Set
			n.elements.each { union += elementToVars[it] }
			elementToVars[n] = union
		}
	}

	void exit(NegationElement n) { elementToVars.remove(n.element) }

	void enter(Relation n, boolean inRuleHead, boolean inRuleBody) {
		// Relations used in the head are implicitly declared by the rule
		if (inRuleHead)
			declaredRelations << n.name
		else if (inRuleBody) {
			relUsedInRules[n.name] << currRule
			elementToVars[n] = tmpVars
		}
	}

	void enter(Type n) { declaredRelations << n.name }

	void enter(VariableExpr n, boolean inRuleHead, boolean inRuleBody) {
		if (inRuleHead)
			vars[currRule.head] << n
		else if (inRuleBody) {
			vars[currRule.body] << n
			tmpVars << n
		}
	}
}
