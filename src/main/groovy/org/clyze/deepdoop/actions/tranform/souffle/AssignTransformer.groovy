package org.clyze.deepdoop.actions.tranform.souffle

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.tranform.DummyTransformer
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.*

class AssignTransformer extends DummyTransformer {

	enum Mode {
		GATHER, ASSIGN, CLEANUP
	}

	// Current mode of operation
	Mode mode
	// Variables that are bound by relations, in the body of a rule
	Set<VariableExpr> boundVars
	// Variables that are assigned some expression, in the body of a rule
	Map<VariableExpr, IExpr> assignments
	// Dummy expression to replace assignment expressions
	ComparisonElement dummyComparison = new ComparisonElement(new ConstantExpr(1), BinOperator.EQ, new ConstantExpr(1))
	// For transitive closure computation
	boolean changed
	boolean inRule = false

	IVisitable visit(Rule n) {
		if (!n.body) return super.visit(n)

		actor.enter(n)
		inRule = true
		def head = n.head
		def body = n.body

		mode = Mode.GATHER
		boundVars = [] as Set
		body.accept(this)

		mode = Mode.ASSIGN
		assignments = [:]
		changed = true
		while (changed) {
			changed = false
			body = body.accept(this)
			head = head.accept(this)
		}

		mode = Mode.CLEANUP
		changed = true
		while (changed && body) {
			changed = false
			body = body.accept(this)
		}

		m[n.head] = head
		m[n.body] = body

		inRule = false
		actor.exit(n, m)
	}

	IVisitable exit(ComparisonElement n, Map m) {
		if (n.expr.op == BinOperator.EQ && n.expr.left instanceof VariableExpr && mode == Mode.ASSIGN) {
			def var = n.expr.left as VariableExpr
			if (!(var in boundVars)) {
				changed = true
				assignments[var] = n.expr.right
				return dummyComparison
			}
		}
		super.exit(n, m)
	}

	IVisitable visit(LogicalElement n) {
		if (mode == Mode.CLEANUP && n.elements.find { it == dummyComparison }) {
			def newElements = n.elements.findAll { it != dummyComparison }
			if (newElements) {
				changed = true
				return new LogicalElement(n.type, newElements)
			} else
				return null
		}
		super.visit(n)
	}

	IVisitable exit(Constructor n, Map m) {
		if (inRule && mode == Mode.GATHER) {
			boundVars += n.exprs.findAll { it instanceof VariableExpr }
			return n
		}
		super.exit(n, m)
	}

	IVisitable exit(Relation n, Map m) {
		if (inRule && mode == Mode.GATHER) {
			boundVars += n.exprs.findAll { it instanceof VariableExpr }
			return n
		}
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
		if (mode == Mode.ASSIGN && assignments[n]) {
			changed = true
			return assignments[n]
		}
		return n
	}
}
