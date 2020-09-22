package org.codesimius.panda.actions.symbol

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation

class RelationInfoVisitor extends DefaultVisitor<IVisitable> {

	Map<String, RelDeclaration> explicitlyDeclared = [:]
	Set<String> implicitlyDeclared = [] as Set
	Map<String, Set<Rule>> relationDefinedInRules = [:].withDefault { [] as Set }
	Map<String, Set<Rule>> relationUsedInRules = [:].withDefault { [] as Set }

	private Rule currRule

	void enter(RelDeclaration n) {
		explicitlyDeclared[n.relation.name] = n
	}

	void enter(TypeDeclaration n) {
		// Types can be used as unary relations (with the same type and name)
		implicitlyDeclared << n.type.name
	}

	void enter(Rule n) { currRule = n }

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
