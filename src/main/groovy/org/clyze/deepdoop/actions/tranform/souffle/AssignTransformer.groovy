package org.clyze.deepdoop.actions.tranform.souffle

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.RelationInfoVisitingActor
import org.clyze.deepdoop.actions.tranform.DummyTransformer
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryOp
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Error

import static org.clyze.deepdoop.datalog.element.ComparisonElement.TRIVIALLY_TRUE
import static org.clyze.deepdoop.system.Error.error as error

@Canonical
class AssignTransformer extends DummyTransformer {

	RelationInfoVisitingActor relInfoActor

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
		if (!n.body) return super.visit(n)

		actor.enter(n)
		assignments = [:]
		replacedVars = [] as Set
		boundVars = relInfoActor.boundVars[n]
		complexLogic = 0
		def head = n.head
		def body = n.body
		// Simulating a do-while in groovy
		changes = true
		while (changes) {
			changes = false
			body = visit(body) as LogicalElement
			// Update expressions for assignment as well
			assignments.each { it.value = visit(it.value) as IExpr }
			head = visit(head) as LogicalElement
			replacedVars += assignments.keySet()
			assignments = [:]
		}

		// Clean-up
		if (body.elements.find { it == TRIVIALLY_TRUE }) {
			def newElements = body.elements.findAll { it != TRIVIALLY_TRUE }
			body = newElements ? new LogicalElement(body.type, newElements) : null
		}

		replacedVars = [] as Set
		m[n.head] = head
		m[n.body] = body
		actor.exit(n, m)
	}

	IVisitable exit(ComparisonElement n, Map m) {
		if (n.expr.op == BinaryOp.EQ && n.expr.left instanceof VariableExpr) {
			def var = n.expr.left as VariableExpr
			if (!(var in boundVars)) {
				if (complexLogic > 1) error(Error.VAR_ASGN_COMPLEX, var)
				changes = true
				assignments[var] = n.expr.right
				return TRIVIALLY_TRUE
			}
		}
		super.exit(n, m)
	}

	IVisitable visit(LogicalElement n) {
		actor.enter(n)
		complexLogic += (n.type == LogicalElement.LogicType.OR) ? 2 : 1
		n.elements.each { m[it] = visit it }
		complexLogic -= (n.type == LogicalElement.LogicType.OR) ? 2 : 1
		actor.exit(n, m)
	}

	IVisitable visit(NegationElement n) {
		actor.enter(n)
		complexLogic++
		m[n.element] = visit n.element
		complexLogic--
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	// Must override since the default implementation throws an exception
	IVisitable exit(RecordExpr n, Map m) { new RecordExpr(n.exprs.collect { m[it] as IExpr }) }

	IVisitable exit(VariableExpr n, Map m) {
		if (n in replacedVars) error(Error.VAR_ASGN_CYCLE, n.name)
		if (assignments[n]) {
			changes = true
			return assignments[n]
		}
		return n
	}

	// Overrides to avoid unneeded allocations

	IVisitable exit(RelDeclaration n, Map m) { n }

	IVisitable exit(TypeDeclaration n, Map m) { n }

	IVisitable exit(Type n, Map m) { n }
}
