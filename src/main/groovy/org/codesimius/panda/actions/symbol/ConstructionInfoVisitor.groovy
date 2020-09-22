package org.codesimius.panda.actions.symbol

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

@Canonical
class ConstructionInfoVisitor extends DefaultVisitor<IVisitable> {

	@Delegate
	Compiler compiler
	// Order constructors appearing in a rule head based on their dependencies
	// If C2 needs a variable constructed by C1, it will be after C1
	Map<Rule, List<ConstructionElement>> constructionsOrderedPerRule = [:].withDefault { [] }
	private Rule currRule

	IVisitable visit(Rule n) {
		parentAnnotations = n.annotations
		currRule = n
		visit n.head
		null
	}

	void enter(ConstructionElement n) {
		// Max index of a constructor that constructs a variable used by `n`
		def maxBefore = n.constructor.keyExprs
				.collect { e -> constructionsOrderedPerRule[currRule].findIndexOf { it.constructor.valueExpr == e } }
				.max()
		// Min index of a constructor that uses the variable constructed by `con`
		def minAfter = constructionsOrderedPerRule[currRule].findIndexValues {
			n.constructor.valueExpr in it.constructor.keyExprs
		}
		.min()
		// `maxBefore` should be strictly before `minAfter`
		maxBefore = (maxBefore != -1 ? maxBefore : -2)
		minAfter = (minAfter != null ? minAfter : -1)
		if (maxBefore >= minAfter) error(findParentLoc(), Error.CONSTR_RULE_CYCLE, n.constructor.name)

		constructionsOrderedPerRule[currRule].add(maxBefore >= 0 ? maxBefore : 0, n)
	}
}
