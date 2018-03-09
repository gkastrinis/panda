package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.VariableExpr

class RelationInfoVisitingActor extends DefaultVisitor<IVisitable> implements TDummyActor<IVisitable> {

	Map<IVisitable, Set<Relation>> declaredRelations = [:].withDefault { [] as Set }
	Map<IVisitable, Set<Relation>> usedRelations = [:].withDefault { [] as Set }
	Map<String, Set<Rule>> relUsedInRules = [:].withDefault { [] as Set }

	// List instead of set so we can count occurrences (for validation)
	Map<IVisitable, List<VariableExpr>> vars = [:]
	Map<Rule, Set<VariableExpr>> boundVars = [:]

	private Set<Relation> tmpRelations = [] as Set
	private List<VariableExpr> tmpVars = []
	private Set<VariableExpr> tmpBoundVars = [] as Set

	RelationInfoVisitingActor() { actor = this }

	IVisitable exit(BlockLvl2 n, Map m) { n }

	IVisitable exit(BlockLvl0 n, Map m) {
		declaredRelations[n] = (n.relDeclarations + n.rules).collect { declaredRelations[it] }.flatten() as Set<Relation>
		usedRelations[n] = (n.relDeclarations + n.rules).collect { usedRelations[it] }.flatten() as Set<Relation>
		null
	}

	void enter(RelDeclaration n) { declaredRelations[n] = [n.relation] as Set }

	IVisitable visit(Rule n) {
		actor.enter(n)

		tmpRelations = [] as Set
		tmpVars = []
		visit n.head
		// Relations used in the head (excluding constructors) are implicitly declared by the rule
		declaredRelations[n] = tmpRelations.findAll { it instanceof Relation } as Set
		usedRelations[n] = tmpRelations
		vars[n.head] = tmpVars

		if (n.body) {
			tmpRelations = [] as Set
			tmpVars = []
			tmpBoundVars = [] as Set
			visit n.body
			usedRelations[n] += tmpRelations
			tmpRelations.each { relUsedInRules[it.name] << n }
			vars[n.body] = tmpVars
			boundVars[n] = tmpBoundVars
		}

		actor.exit(n, m)
	}

	void enter(Constructor n) { enter(n as Relation) }

	void enter(Relation n) {
		tmpRelations << n
		tmpBoundVars += n.exprs.findAll { it instanceof VariableExpr }
	}

	void enter(VariableExpr n) { tmpVars << n }
}
