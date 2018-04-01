package org.clyze.deepdoop.actions.tranform.souffle

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.SymbolTableVisitingActor
import org.clyze.deepdoop.actions.tranform.DefaultTransformer
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.expr.BinaryOp
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Error

import static org.clyze.deepdoop.datalog.element.ComparisonElement.TRIVIALLY_TRUE
import static org.clyze.deepdoop.system.Error.error

@Canonical
class AssignTransformer extends DefaultTransformer {

	SymbolTableVisitingActor symbolTable

	// Variables that are assigned some expression in a rule body
	private Map<VariableExpr, IExpr> assignments = [:]
	// Variables already replaced by an assignment
	private Set<VariableExpr> replacedVars = [] as Set
	// Variables bound by relations in a rule body
	private Set<VariableExpr> boundVars
	// Count levels of "complex" logic in rule bodies
	private int complexLogic
	// For transitive closure
	private boolean changes

	IVisitable visit(Rule n) {
		if (!n.body) return n

		assignments = [:]
		replacedVars = [] as Set
		boundVars = symbolTable.boundVars[n]
		complexLogic = 0
		def head = n.head
		def body = n.body
		// Simulating a do-while in groovy
		changes = true
		while (changes) {
			changes = false
			body = visit(body) as IElement
			// Update expressions for assignment as well
			assignments.each { it.value = visit(it.value) as IExpr }
			head = visit(head) as IElement
			replacedVars += assignments.keySet()
			assignments = [:]
		}

		// Clean-up
		def elements = asElements(body)
		if (elements.any { it == TRIVIALLY_TRUE }) {
			def newElements = elements.findAll { it != TRIVIALLY_TRUE }
			if (newElements.size() > 1) body = new LogicalElement((body as LogicalElement).type, newElements)
			else if (newElements.size() == 1) body = newElements.first() as IElement
			else body = null
		}
		replacedVars = [] as Set
		m[n.head] = head
		m[n.body] = body

		super.exit n
	}

	IVisitable exit(RelDeclaration n) { n }

	IVisitable exit(TypeDeclaration n) { n }

	IVisitable exit(ComparisonElement n) {
		if (n.expr.op == BinaryOp.EQ && n.expr.left instanceof VariableExpr) {
			def var = n.expr.left as VariableExpr
			if (!(var in boundVars)) {
				if (complexLogic > 1) error(Error.VAR_ASGN_COMPLEX, var)
				changes = true
				assignments[var] = n.expr.right
				return TRIVIALLY_TRUE
			}
		}
		super.exit n
	}

	void enter(LogicalElement n) { complexLogic += (n.type == LogicalElement.LogicType.OR) ? 2 : 1 }

	IVisitable exit(LogicalElement n) {
		complexLogic -= (n.type == LogicalElement.LogicType.OR) ? 2 : 1
		super.exit n
	}

	void enter(NegationElement n) { complexLogic++ }

	IVisitable exit(NegationElement n) {
		complexLogic--
		super.exit n
	}

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) {
		n.exprs.each { m[it] = visit it }
		new RecordExpr(n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(VariableExpr n) {
		if (n in replacedVars) error(Error.VAR_ASGN_CYCLE, n.name)
		if (assignments[n]) {
			changes = true
			return assignments[n]
		}
		return n
	}
}
