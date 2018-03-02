package org.clyze.deepdoop.actions.tranform.souffle

import org.clyze.deepdoop.actions.tranform.DummyTransformer
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr

class CleanupTransformer extends DummyTransformer {

	private boolean changed = false
	// Dummy expression used to replace assignment expressions
	private ComparisonElement dummyComparison

	CleanupTransformer(ComparisonElement dummyComparison) { this.dummyComparison = dummyComparison }

	IVisitable visit(Rule n) {
		if (!n.body) return super.visit(n)
		actor.enter(n)
		changed = true
		def body = n.body
		while (changed && body) {
			changed = false
			body = visit body
		}
		m[n.head] = n.head
		m[n.body] = body
		actor.exit(n, m)
	}

	IVisitable exit(LogicalElement n, Map m) {
		if (n.elements.find { it == dummyComparison }) {
			def newElements = n.elements.findAll { it != dummyComparison }
			if (newElements) {
				changed = true
				return new LogicalElement(n.type, newElements)
			} else
				return null
		}
		super.exit(n, m)
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
	IVisitable exit(RecordExpr n, Map m) {
		new RecordExpr(n.exprs.collect { m[it] as IExpr })
	}
}
