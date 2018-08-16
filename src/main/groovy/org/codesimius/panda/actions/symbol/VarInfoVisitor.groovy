package org.codesimius.panda.actions.symbol

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Error

import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

class VarInfoVisitor extends DefaultVisitor<IVisitable> {

	Set<VariableExpr> constructedVars
	// Lists instead of sets to count occurrences (for validation)
	List<VariableExpr> headVars
	List<VariableExpr> bodyVars
	Rule currRule

	IVisitable visit(Rule n) {
		constructedVars = [] as Set
		currRule = n

		headVars = []
		inRuleHead = true
		visit n.head
		inRuleHead = false

		bodyVars = []
		inRuleBody = true
		if (n.body) visit n.body
		inRuleBody = false

		null
	}

	void enter(ConstructionElement n) {
		def conVar = n.constructor.valueExpr as VariableExpr
		if (conVar in constructedVars)
			error(recall(n), Error.VAR_MULTIPLE_CONSTR, conVar)
		constructedVars << conVar
	}

	// Vars bound (positively) by relations in a rule body
	Map<Rule, Set<VariableExpr>> boundVars = [:].withDefault { [] as Set }
	private Map<IVisitable, List<VariableExpr>> elementToBoundVars = [:].withDefault { [] }

//	IVisitable exit(Rule n) {
//		boundVars[n] = elementToBoundVars[n.body] as Set
//		null
//	}
//
//	IVisitable exit(GroupElement n) {
//		elementToBoundVars[n] = elementToBoundVars[n.element]
//		null
//	}
//
//	IVisitable exit(LogicalElement n) {
//		elementToVars[n] = handleVars(n, elementToVars)
//		elementToBoundVars[n] = handleVars(n, elementToBoundVars)
//		null
//	}
//
//	static List<VariableExpr> handleVars(LogicalElement n, Map<IVisitable, List<VariableExpr>> vars) {
//		def res = []
//		if (n.kind == LogicalElement.Kind.OR) {
//			res = vars[n.elements.first()]
//			n.elements.drop(1).each {
//				def vs = vars[it]
//				if (vs) res = res.intersect(vs)
//			}
//		} else {
//			n.elements.each {
//				def vs = vars[it]
//				if (vs) res += vs
//			}
//		}
//		return res
//	}

//	IVisitable exit(NegationElement n) {
//		elementToVars.remove(n.element)
//		elementToBoundVars.remove(n.element)
//		null
//	}
//
//	IVisitable exit(Constructor n) { exit(n as Relation) }

//	IVisitable exit(Relation n) {
//		elementToBoundVars[n] = n.exprs.collect { elementToVars[it] }.flatten()
//		null
//	}

	void enter(VariableExpr n) {
		if (inRuleHead) headVars << n
		else bodyVars << n
	}
}
