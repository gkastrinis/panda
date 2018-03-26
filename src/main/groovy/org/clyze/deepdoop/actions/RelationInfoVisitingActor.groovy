package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.VariableExpr

class RelationInfoVisitingActor extends DefaultVisitor<IVisitable> implements TDummyActor<IVisitable> {

	Set<String> declaredRelations
	Map<BlockLvl0, Set<String>> declaredRelationsPerBlock = [:]
	Map<String, Set<Rule>> relUsedInRules = [:].withDefault { [] as Set }
	// List instead of set so we can count occurrences (for validation)
	Map<IVisitable, List<VariableExpr>> vars = [:].withDefault { [] }
	Map<Rule, Set<VariableExpr>> boundVars = [:].withDefault { [] as Set }

	private Rule currRule

	RelationInfoVisitingActor() { actor = this }

	IVisitable exit(BlockLvl2 n, Map m) { n }

	void enter(BlockLvl0 n) { declaredRelations = [] as Set }

	IVisitable exit(BlockLvl0 n, Map m) {
		declaredRelationsPerBlock[n] = declaredRelations
		null
	}

	void enter(RelDeclaration n) { declaredRelations << n.relation.name }

	void enter(Rule n) { currRule = n }

	void enter(Constructor n) { if (inRuleBody) relUsedInRules[n.name] << currRule }

	void enter(Relation n) {
		// Relations used in the head are implicitly declared by the rule
		if (inRuleHead) declaredRelations << n.name
		else if (inRuleBody) relUsedInRules[n.name] << currRule
	}

	void enter(Type n) { declaredRelations << n.name }

	void enter(VariableExpr n) {
		if (inRuleHead)
			vars[currRule.head] << n
		else if (inRuleBody) {
			vars[currRule.body] << n
			boundVars[currRule] << n
		}
	}
}
