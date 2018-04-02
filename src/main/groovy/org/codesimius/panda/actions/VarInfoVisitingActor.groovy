package org.codesimius.panda.actions

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr
import org.codesimius.panda.datalog.expr.VariableExpr

class VarInfoVisitingActor extends DefaultVisitor<IVisitable> {

	// List instead of set so we can count occurrences (for validation)
	Map<IVisitable, List<VariableExpr>> vars = [:].withDefault { [] }
	private Map<IVisitable, List<VariableExpr>> elementToVars = [:].withDefault { [] }
	// Vars bound (positively) by relations in a rule body
	Map<Rule, Set<VariableExpr>> boundVars = [:].withDefault { [] as Set }
	private Map<IVisitable, List<VariableExpr>> elementToBoundVars = [:].withDefault { [] }

	IVisitable exit(BlockLvl2 n) { n }

	void enter(Rule n) {
		elementToVars.clear()
		elementToBoundVars.clear()
	}

	IVisitable exit(Rule n) {
		vars[n.head] = elementToVars[n.head]
		vars[n.body] = elementToVars[n.body]
		boundVars[n] = elementToBoundVars[n.body] as Set
		null
	}

	IVisitable exit(AggregationElement n) {
		elementToVars[n] = elementToVars[n.var] + elementToVars[n.relation] + elementToVars[n.body]
		null
	}

	IVisitable exit(ComparisonElement n) {
		elementToVars[n] = elementToVars[n.expr]
		null
	}

	IVisitable exit(ConstructionElement n) {
		elementToVars[n] = elementToVars[n.constructor] + elementToVars[n.type]
		null
	}

	IVisitable exit(GroupElement n) {
		elementToVars[n] = elementToVars[n.element]
		elementToBoundVars[n] = elementToBoundVars[n.element]
		null
	}

	IVisitable exit(LogicalElement n) {
		elementToVars[n] = handleVars(n, elementToVars)
		elementToBoundVars[n] = handleVars(n, elementToBoundVars)
		null
	}

	static List<VariableExpr> handleVars(LogicalElement n, Map<IVisitable, List<VariableExpr>> vars) {
		def res = []
		if (n.type == LogicalElement.LogicType.OR) {
			res = vars[n.elements.first()]
			n.elements.drop(1).each {
				def vs = vars[it]
				if (vs) res = res.intersect(vs)
			}
		} else {
			n.elements.each {
				def vs = vars[it]
				if (vs) res += vs
			}
		}
		return res
	}

	IVisitable exit(NegationElement n) {
		elementToVars.remove(n.element)
		elementToBoundVars.remove(n.element)
		null
	}

	IVisitable exit(Constructor n) { exit(n as Relation) }

	IVisitable exit(Relation n) {
		elementToVars[n] = n.exprs.collect { elementToVars[it] }.flatten()
		elementToBoundVars[n] = n.exprs.collect { elementToVars[it] }.flatten()
		null
	}

	IVisitable exit(BinaryExpr n) {
		elementToVars[n] = elementToVars[n.left] + elementToVars[n.right]
		null
	}

	IVisitable exit(GroupExpr n) {
		elementToVars[n] = elementToVars[n.expr]
		null
	}

	void enter(VariableExpr n) { if (inRuleHead || inRuleBody) elementToVars[n] = [n] }
}
