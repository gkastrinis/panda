package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.LogicalElement.LogicType
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Error

import static org.clyze.deepdoop.datalog.Annotation.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.TYPE
import static org.clyze.deepdoop.system.Error.error
import static org.clyze.deepdoop.system.SourceManager.recallStatic as recall

class SymbolTableVisitingActor extends DefaultVisitor<IVisitable> {

	Map<Type, List<Type>> superTypesOrdered = [:]
	Map<Type, Set<Type>> subTypes = [:].withDefault { [] as Set }
	Map<Type, Type> typeToRootType = [:]

	Set<Type> getAllTypes() { typeToRootType.keySet() }

	Set<Type> getRootTypes() { typeToRootType.values() as Set }


	Set<String> declaredRelations
	Map<BlockLvl0, Set<String>> declaredRelationsPerBlock = [:]
	Map<String, Set<Rule>> relUsedInRules = [:].withDefault { [] as Set }
	private Rule currRule
	// List instead of set so we can count occurrences (for validation)
	Map<IVisitable, List<VariableExpr>> vars = [:].withDefault { [] }
	Map<Rule, Set<VariableExpr>> boundVars = [:].withDefault { [] as Set }


	Map<String, Type> constructorBaseType = [:]
	Map<Type, Set<RelDeclaration>> constructorsPerType = [:].withDefault { [] as Set }
	Map<Rule, Set<VariableExpr>> constructedVars = [:]
	// Order constructors appearing in a rule head based on their dependencies
	// If C2 needs a variable constructed by C1, it will be after C1
	Map<Rule, List<ConstructionElement>> constructionsOrderedPerRule = [:]
	// Count how many times a variable is constructed in a rule head
	// It should be at max once
	private List<VariableExpr> tmpConVars
	private List<ConstructionElement> tmpConstructionsOrdered

	// Types with no constructor in the hierarchy can be treated as symbols (strings)
	Set<Type> typesToOptimize = [] as Set
	private Set<Type> typesWithDefaultCon = [] as Set

	Map<IElement, Set<VariableExpr>> elementToVars = [:]
	private Set<VariableExpr> tmpVars = [] as Set


	IVisitable exit(BlockLvl2 n) { n }

	void enter(BlockLvl0 n) { declaredRelations = [] as Set }

	IVisitable exit(BlockLvl0 n) {
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
		declaredRelations += ["count", "min", "max", "sum"]
		declaredRelationsPerBlock[n] = declaredRelations

		rootTypes.each { root ->
			def types = [root] + subTypes[root]
			def constructors = types.collect { constructorsPerType[it] }.flatten() as Set<RelDeclaration>
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
		if (n.annotations.find { it == TYPE }.args["defaultConstructor"])
			typesWithDefaultCon << n.type
	}

	void enter(Rule n) {
		currRule = n
		elementToVars = [:]
		tmpConVars = []
		tmpConstructionsOrdered = []
	}

	IVisitable exit(Rule n) {
		constructedVars[n] = tmpConVars as Set
		constructionsOrderedPerRule[n] = tmpConstructionsOrdered
		boundVars[n] = elementToVars[n.body]
		elementToVars = [:]
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

	IVisitable exit(LogicalElement n) {
		if (n.type == LogicType.OR) {
			def intersection = elementToVars[n.elements.first()]
			n.elements.drop(1).each {
				def vs = elementToVars[it]
				if (vs) intersection = intersection.intersect(vs)
			}
			if (intersection) elementToVars[n] = intersection
		} else {
			def union = [] as Set
			n.elements.each { union += elementToVars[it] }
			elementToVars[n] = union
		}
		null
	}

	IVisitable exit(NegationElement n) {
		elementToVars.remove(n.element)
		null
	}

	IVisitable exit(Constructor n) {
		if (inRuleBody) relUsedInRules[n.name] << currRule
		exit(n as Relation)
	}

	IVisitable exit(Relation n) {
		// Relations used in the head are implicitly declared by the rule
		if (inRuleHead)
			declaredRelations << n.name
		else if (inRuleBody) {
			relUsedInRules[n.name] << currRule

			elementToVars[n] = tmpVars
			tmpVars = [] as Set
		}
		null
	}

	void enter(Type n) { declaredRelations << n.name }

	void enter(VariableExpr n) {
		if (inRuleHead)
			vars[currRule.head] << n
		else if (inRuleBody) {
			vars[currRule.body] << n
			tmpVars << n
		}
	}
}
