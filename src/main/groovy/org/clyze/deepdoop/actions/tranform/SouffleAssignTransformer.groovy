package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

class SouffleAssignTransformer extends DummyTransformer {

	// Keep track of all assignments in current rule
	Map<VariableExpr, IExpr> assignments
	// Dummy expression to replace assignment expressions
	BinaryExpr dummyExpr = new BinaryExpr(new ConstantExpr(1), BinOperator.EQ, new ConstantExpr(1))
	// For transitive closure computation
	boolean changed

	// Overwrite to compute transitive closure of changes
	IVisitable visit(Rule n) {
		actor.enter(n)

		changed = true
		assignments = [:]
		def head = n.head
		def body = n.body
		while (changed) {
			changed = false
			head = head.accept(this)
			body = body?.accept(this)
		}
		m[n.head] = head
		m[n.body] = body
		assignments = null

		actor.exit(n, m)
	}

	IVisitable exit(BinaryExpr n, Map<IVisitable, IVisitable> m) {
		if (n.op == BinOperator.ASGN) {
			changed = true
			assignments[n.left as VariableExpr] = n.right
			return dummyExpr
		}
		super.exit(n, m)
	}

	IVisitable exit(VariableExpr n, Map<IVisitable, IVisitable> m) {
		if (!assignments) return n
		if (assignments[n]) {
			changed = true
			return assignments[n]
		}
		return n
	}
}
