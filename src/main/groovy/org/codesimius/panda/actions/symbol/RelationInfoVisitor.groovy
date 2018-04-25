package org.codesimius.panda.actions.symbol

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.CONSTRUCTOR
import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

class RelationInfoVisitor extends DefaultVisitor<IVisitable> {

	Map<String, Type> constructorBaseType = [:]
	Map<Type, Set<RelDeclaration>> constructorsPerType = [:].withDefault { [] as Set }
	Map<Rule, Set<VariableExpr>> constructedVars = [:].withDefault { [] as Set }
	Set<String> declaredRelations = [] as Set
	Map<String, Set<Rule>> relUsedInRules = [:].withDefault { [] as Set }
	private Rule currRule

	IVisitable exit(BlockLvl2 n) { n }

	IVisitable exit(BlockLvl0 n) {
		// Implicitly, add relations supported in aggregation
		declaredRelations += ["count", "min", "max", "sum"]
		null
	}

	void enter(RelDeclaration n) {
		declaredRelations << n.relation.name

		if (CONSTRUCTOR in n.annotations) {
			constructorBaseType[n.relation.name] = n.types.last()
			constructorsPerType[n.types.last()] << n
		}
	}

	void enter(TypeDeclaration n) { declaredRelations << n.type.name }

	void enter(Rule n) { currRule = n }

	void enter(ConstructionElement n) {
		def conVar = n.constructor.valueExpr as VariableExpr
		if (conVar in constructedVars[currRule]) error(recall(n), Error.VAR_MULTIPLE_CONSTR, conVar)
		constructedVars[currRule] << conVar
	}

	IVisitable exit(Constructor n) { exit(n as Relation) }

	IVisitable exit(Relation n) {
		// Relations used in the head are implicitly declared by the rule
		if (inRuleHead)
			declaredRelations << n.name
		else if (inRuleBody)
			relUsedInRules[n.name] << currRule
		null
	}
}
