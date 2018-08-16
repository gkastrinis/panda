package org.codesimius.panda.actions.symbol

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation

class RelationInfoVisitor extends DefaultVisitor<IVisitable> {

	Set<String> implicitlyDeclared = [] as Set
	Map<String, Set<Rule>> relationDefinedInRules = [:].withDefault { [] as Set }
	Map<String, Set<Rule>> relationUsedInRules = [:].withDefault { [] as Set }
	Rule currRule

	void enter(Constructor n) { enter(n as Relation) }

	void enter(Relation n) {
		// Relations used in the head are implicitly declared by the rule
		if (inRuleHead) {
			implicitlyDeclared << n.name
			relationDefinedInRules[n.name] << currRule
		} else if (inRuleBody)
			relationUsedInRules[n.name] << currRule
	}
}
