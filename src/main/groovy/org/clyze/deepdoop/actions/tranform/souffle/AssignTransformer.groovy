package org.clyze.deepdoop.actions.tranform.souffle

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.InfoCollectionVisitingActor
import org.clyze.deepdoop.actions.tranform.DummyTransformer
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.expr.*
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager

class AssignTransformer extends DummyTransformer {

	InfoCollectionVisitingActor infoActor

	// Variables that are assigned some expression, in the body of a rule
	Map<VariableExpr, IExpr> assignments
	// Variables already replaced by an assignment
	Set<VariableExpr> replacedVars
	// Dummy expression to replace assignment expressions
	ComparisonElement dummyComparison = new ComparisonElement(new ConstantExpr(1), BinOperator.EQ, new ConstantExpr(1))
	// For transitive closure computation
	boolean changed

	int complexLogic

	Set<VariableExpr> boundVars

	AssignTransformer(InfoCollectionVisitingActor infoActor) { this.infoActor = infoActor }

	IVisitable visit(Program n) {
		def n1 = super.visit(n) as Program
		new CleanupTransformer(dummyComparison).visit(n1)
	}

	IVisitable visit(Rule n) {
		if (!n.body) return super.visit(n)
		actor.enter(n)
		assignments = [:]
		replacedVars = [] as Set
		boundVars = infoActor.boundVars[n]
		complexLogic = 0
		changed = true
		def head = n.head
		def body = n.body
		while (changed) {
			changed = false
			body = body.accept(this)
			head = head.accept(this)
		}
		m[n.head] = head
		m[n.body] = body
		actor.exit(n, m)
	}

	IVisitable exit(ComparisonElement n, Map m) {
		if (n.expr.op == BinOperator.EQ && n.expr.left instanceof VariableExpr /*&& mode == Mode.ASSIGN*/) {
			def var = n.expr.left as VariableExpr
			if (!(var in boundVars)) {
				if (complexLogic > 1)
					ErrorManager.error(ErrorId.VAR_ASGN_COMPLEX, var)
				changed = true
				assignments[var] = n.expr.right
				return dummyComparison
			}
		}
		super.exit(n, m)
	}

	void enter(LogicalElement n) {
		if (n.type == LogicalElement.LogicType.OR) complexLogic += 2
		else complexLogic++
	}

	IVisitable exit(LogicalElement n, Map m) {
		if (n.type == LogicalElement.LogicType.OR) complexLogic -= 2
		else complexLogic--
		super.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	// Must override since the default implementation throws an exception
	IVisitable exit(RecordExpr n, Map m) {
		new RecordExpr(n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(VariableExpr n, Map m) {
		if (assignments && assignments[n]) {
			changed = true
			if (n in replacedVars)
				ErrorManager.error(ErrorId.VAR_ASGN_CYCLE, n.name)
			replacedVars << n
			return assignments[n]
		}
		return n
	}
}
