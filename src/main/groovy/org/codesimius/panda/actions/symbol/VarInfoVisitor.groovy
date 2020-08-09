package org.codesimius.panda.actions.symbol

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.LogicalElement
import org.codesimius.panda.datalog.element.NegationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.RecordExpr
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Error

import static org.codesimius.panda.system.Log.error

class VarInfoVisitor extends DefaultVisitor<IVisitable> {

	Set<VariableExpr> constructedVars
	// Lists instead of sets to count occurrences (for validation)
	List<VariableExpr> headVars
	List<VariableExpr> bodyVars
	// Vars bound (positively) by relations in a rule body
	List<VariableExpr> boundBodyVars
	Rule currRule
	int negationLevels
	boolean inRelation

	IVisitable visit(Rule n) {
		parentAnnotations = n.annotations
		constructedVars = [] as Set
		currRule = n

		headVars = []
		inRuleHead = true
		visit n.head
		inRuleHead = false

		bodyVars = []
		boundBodyVars = []
		inRuleBody = true
		if (n.body) visit n.body
		inRuleBody = false

		null
	}

	void enter(ConstructionElement n) {
		if (n.constructor.valueExpr !instanceof VariableExpr) return

		def conVar = n.constructor.valueExpr as VariableExpr
		if (conVar in constructedVars)
			error(findParentLoc(), Error.VAR_MULTIPLE_CONSTR, conVar)
		constructedVars << conVar
	}

	IVisitable visit(LogicalElement n) {
		if (n.kind == LogicalElement.Kind.AND)
			n.elements.each { visit it }
		else {
			def oldBoundVars = boundBodyVars

			boundBodyVars = []
			visit n.elements.first()
			def intersection = boundBodyVars

			n.elements.drop(1).each {
				boundBodyVars = []
				visit it
				intersection = intersection.intersect(boundBodyVars as Iterable<VariableExpr>)
			}

			boundBodyVars = oldBoundVars + intersection
		}
		null
	}

	void enter(NegationElement n) {
		negationLevels++
	}

	IVisitable exit(NegationElement n) {
		negationLevels--
		null
	}

	void enter(Constructor n) {
		inRelation = true
	}

	IVisitable exit(Constructor n) {
		inRelation = false
		null
	}

	void enter(Relation n) {
		inRelation = true
	}

	IVisitable exit(Relation n) {
		inRelation = false
		null
	}

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) { n }

	void enter(VariableExpr n) {
		if (inRuleHead) headVars << n
		else if (inRuleBody) bodyVars << n

		if (inRuleBody && inRelation && negationLevels == 0) boundBodyVars << n
	}
}
