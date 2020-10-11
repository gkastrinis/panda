package org.codesimius.panda.actions.tranform.souffle

import groovy.transform.Canonical
import org.codesimius.panda.actions.tranform.DefaultTransformer
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ComparisonElement
import org.codesimius.panda.datalog.element.IElement
import org.codesimius.panda.datalog.element.LogicalElement
import org.codesimius.panda.datalog.element.NegationElement
import org.codesimius.panda.datalog.expr.BinaryOp
import org.codesimius.panda.datalog.expr.IExpr
import org.codesimius.panda.datalog.expr.RecordExpr
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.element.ComparisonElement.TRIVIALLY_TRUE
import static org.codesimius.panda.datalog.element.LogicalElement.combineElements

@Canonical
class AssignTransformer extends DefaultTransformer {

	@Delegate
	Compiler compiler
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

		parentAnnotations = n.annotations
		assignments = [:]
		replacedVars = [] as Set
		boundVars = currDatalog.getBoundBodyVars(n)
		complexLogic = 0

		def head = n.head
		def body = n.body
		do {
			changes = false
			if (body) body = visit(body) as IElement
			// Update expressions for assignment as well
			assignments.each { it.value = visit(it.value) as IExpr }
			head = visit(head) as IElement
			replacedVars += assignments.keySet()
			assignments = [:]
		} while (changes)

		replacedVars = [] as Set
		m[n.head] = combineElements([head])
		m[n.body] = combineElements([body])

		super.exit n
	}

	// Needed for record types
	IVisitable exit(TypeDeclaration n) { n }

	IVisitable exit(ComparisonElement n) {
		if (n.expr.op == BinaryOp.EQ && n.expr.left instanceof VariableExpr) {
			def var = n.expr.left as VariableExpr
			if (var !in boundVars) {
				if (complexLogic > 1) error(Error.VAR_ASGN_COMPLEX, var)
				changes = true
				assignments[var] = n.expr.right
				return TRIVIALLY_TRUE
			}
		}
		super.exit n
	}

	void enter(LogicalElement n) { complexLogic += (n.kind == LogicalElement.Kind.OR) ? 2 : 1 }

	IVisitable exit(LogicalElement n) {
		complexLogic -= (n.kind == LogicalElement.Kind.OR) ? 2 : 1
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
