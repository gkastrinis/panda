package org.codesimius.panda.actions

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
import static org.codesimius.panda.datalog.Annotation.TYPE
import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

class RelationInfoVisitor extends DefaultVisitor<IVisitable> {

	Map<Type, List<Type>> superTypesOrdered = [:]
	Map<Type, Set<Type>> subTypes = [:].withDefault { [] as Set }
	Map<Type, Type> typeToRootType = [:]

	Set<Type> getAllTypes() { typeToRootType.keySet() }

	Set<Type> getRootTypes() { typeToRootType.values() as Set }

	///////////////////////////////////////////////////

	Set<String> declaredRelations
	Map<BlockLvl0, Set<String>> declaredRelationsPerBlock = [:]
	Map<String, Set<Rule>> relUsedInRules = [:].withDefault { [] as Set }
	private Rule currRule

	///////////////////////////////////////////////////

	Map<String, Type> constructorBaseType = [:]
	Map<Type, Set<RelDeclaration>> constructorsPerType = [:].withDefault { [] as Set }
	// Types with no constructor in the hierarchy can be treated as symbols (strings)
	Set<Type> typesToOptimize = [] as Set
	private Set<Type> typesWithDefaultCon = [] as Set

	Map<Rule, Set<VariableExpr>> constructedVars = [:]
	// Order constructors appearing in a rule head based on their dependencies
	// If C2 needs a variable constructed by C1, it will be after C1
	Map<Rule, List<ConstructionElement>> constructionsOrderedPerRule = [:]
	// List to count how many times a variable is constructed in a rule head
	// It should be at max once
	private List<VariableExpr> tmpConVars
	private List<ConstructionElement> tmpConstructionsOrdered

	///////////////////////////////////////////////////

	IVisitable exit(BlockLvl2 n) { n }

	void enter(BlockLvl0 n) {
		n.typeDeclarations.each { d ->
			superTypesOrdered[d.type] = []
			def currDecl = d
			while (currDecl.supertype) {
				superTypesOrdered[d.type] << currDecl.supertype
				currDecl = n.typeDeclarations.find { it.type == currDecl.supertype }
			}
			superTypesOrdered.each { t, ts ->
				ts.each { subTypes[it] << t }

				typeToRootType[t] = ts ? ts.last() : t
			}
		}

		// Implicitly, add relations supported in aggregation
		declaredRelations = ["count", "min", "max", "sum"] as Set
	}

	IVisitable exit(BlockLvl0 n) {
		declaredRelationsPerBlock[n] = declaredRelations

		rootTypes.each { root ->
			def types = [root] + subTypes[root]
			def constructors = types.collect { constructorsPerType[it] }.flatten()
			if (!constructors && !types.any { it in typesWithDefaultCon }) typesToOptimize += types
		}
		null
	}

	void enter(RelDeclaration n) {
		declaredRelations << n.relation.name

		if (CONSTRUCTOR in n.annotations) {
			def type = n.types.last()
			constructorBaseType[n.relation.name] = type
			constructorsPerType[type] << n
		}
	}

	void enter(TypeDeclaration n) {
		declaredRelations << n.type.name

		if (n.annotations.find { it == TYPE }.args["defaultConstructor"])
			typesWithDefaultCon << n.type
	}

	void enter(Rule n) {
		currRule = n

		tmpConVars = []
		tmpConstructionsOrdered = []
	}

	IVisitable exit(Rule n) {
		constructedVars[n] = tmpConVars as Set
		constructionsOrderedPerRule[n] = tmpConstructionsOrdered
		null
	}

	void enter(ConstructionElement n) {
		def conVar = n.constructor.valueExpr as VariableExpr
		if (conVar in tmpConVars) error(recall(n), Error.VAR_MULTIPLE_CONSTR, conVar)
		tmpConVars << conVar

		// Max index of a constructor that constructs a variable used by `n`
		def maxBefore = n.constructor.keyExprs
				.collect { e -> tmpConstructionsOrdered.findIndexOf { it.constructor.valueExpr == e } }
				.max()
		// Min index of a constructor that uses the variable constructed by `con`
		def minAfter = tmpConstructionsOrdered.findIndexValues {
			n.constructor.valueExpr in it.constructor.keyExprs
		}
		.min()
		// `maxBefore` should be strictly before `minAfter`
		maxBefore = (maxBefore != -1 ? maxBefore : -2)
		minAfter = (minAfter != null ? minAfter : -1)
		if (maxBefore >= minAfter) error(recall(n), Error.CONSTR_RULE_CYCLE, n.constructor.name)

		tmpConstructionsOrdered.add(maxBefore >= 0 ? maxBefore : 0, n)
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
