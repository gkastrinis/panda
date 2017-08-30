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
	Map<String, IExpr> assignments
	// Dummy expression to replace assignment expressions
	BinaryExpr dummyExpr = new BinaryExpr(new ConstantExpr(1), BinOperator.EQ, new ConstantExpr(1))
	// For transitive closure computation
	boolean changed

	// Overwrite to compute transitive closure of changes
	IVisitable visit(Rule n) {
		actor.enter(n)

		changed = true
		assignments = [:]
		def curHead = n.head
		def curBody = n.body
		while (changed) {
			changed = false
			def newAssignments = [:]
			assignments.each { var, e -> newAssignments[var] = e.accept(this) }
			assignments = newAssignments
			m[curHead] = curHead.accept(this)
			if (curBody) m[curBody] = curBody.accept(this)
			curHead = m[curHead]
			curBody = m[curBody]
		}
		assignments = null

		return actor.exit(n, m)
	}

	IVisitable exit(BinaryExpr n, Map<IVisitable, IVisitable> m) {
		if (n.op == BinOperator.ASGN) {
			changed = true
			assignments[(n.left as VariableExpr).name] = n.right
			//rec(dummyExpr)
			return dummyExpr
		}
		super.exit(n, m)
	}

	IVisitable exit(VariableExpr n, Map<IVisitable, IVisitable> m) {
		if (!assignments) return n
		def e = assignments[n.name]
		e ? rec(e) : n
	}

	@Override
	def rec(IVisitable e) {
		changed = true
		return e
	}
}
