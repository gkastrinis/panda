package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.actions.symbol.SymbolTable
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.ComparisonElement
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.*
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.element.relation.Type.*
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.*
import static org.codesimius.panda.datalog.expr.VariableExpr.genN as varN
import static org.codesimius.panda.system.Error.error

@Canonical
class TypeInferenceTransformer extends DefaultTransformer {

	SymbolTable symbolTable

	// Relation x Types (final)
	Map<String, List<Type>> inferredTypes = [:].withDefault { [] }

	// Relation x Types
	Map<String, List<Type>> candidateTypes = [:].withDefault { [] }
	// Relations found in the current rule head that will have types inferred
	Set<Relation> forInference

	// Relation x Type Set for each column
	// Keep track of all types that a relation was used with
	Map<String, List<Set<Type>>> usedTypes = [:].withDefault { [].withDefault { [] as Set } }
	// Relations found in the current rule (head/body) that will have types validated
	// For those found in the head, this is because there is an explicit declaration
	Set<Relation> forValidation

	// Expression x Type Set (for active rule)
	Map<IExpr, Set<Type>> exprTypes

	// Relation x Declaration
	Map<String, RelDeclaration> relToDeclaration = [:]

	// Implementing fix-point computation
	Set<Rule> deltaRules

	IVisitable visit(BlockLvl0 n) {
		// Gather explicit types
		n.relDeclarations.each { visit it }
		n.typeDeclarations.each { visit it }

		// Gather candidate types for relations, until fix-point
		Set<Rule> oldDeltaRules = n.rules
		while (!oldDeltaRules.isEmpty()) {
			deltaRules = [] as Set
			oldDeltaRules.each { visit it }
			if (oldDeltaRules == deltaRules)
				error(Error.TYPE_INF_FAIL, null)
			oldDeltaRules = deltaRules
		}

		// Copy inferred types in the final map
		candidateTypes.each { relName, type -> inferredTypes[relName] = type }

		// Validate type usage
		usedTypes.each { relName, types ->
			def inferredTs = inferredTypes[relName]
			types.eachWithIndex { typeSet, i ->
				// All types used must be a subtype of the inferred (or declared) type
				def subtypes = [inferredTs[i]] + symbolTable.subTypes[inferredTs[i]]
				if (typeSet.any { !(it in subtypes) })
					error(Error.TYPE_INF_INCOMPAT_USE, relName, i, inferredTs[i].name, typeSet.collect { it.name })
			}
		}

		// Fill partial declarations and add implicit ones
		// Ignore relations that derive from types
		def relDeclarations = inferredTypes
				.findAll { rel, types -> !symbolTable.allTypes.any { it.name == rel } }
				.collect { rel, types ->

			def vars = varN(types.size())
			def d = relToDeclaration[rel]
			// Explicit, partial declaration
			if (d && !d.types) {
				d.relation.exprs = vars
				d.types = types
			}
			// Implicit declaration
			else if (!d) {
				d = new RelDeclaration(new Relation(rel, vars), types)
			}

			return d
		} as Set

		new BlockLvl0(relDeclarations, n.typeDeclarations, n.rules)
	}

	IVisitable exit(RelDeclaration n) {
		relToDeclaration[n.relation.name] = n
		// Not a partial declaration
		if (n.types) inferredTypes[n.relation.name] = n.types
		return n
	}

	IVisitable exit(TypeDeclaration n) {
		// Types can be used as single column relations (with the same type)
		inferredTypes[n.type.name] = [n.type]
		return n
	}

	void enter(Rule n) {
		forInference = [] as Set
		forValidation = [] as Set
		exprTypes = [:].withDefault { [] as Set }
	}

	IVisitable exit(Rule n) {
		// For inferring a type *in* a rule body, approximate by assuming all relations are used conjunctively
		// Therefore, infer the lowest type in the hierarchy that is present
		Map<IExpr, Type> coalescedExprTypes = [:]
		def coalesce = { IExpr expr ->
			if (!coalescedExprTypes[expr]) {
				def types = exprTypes[expr]
				if (types.collect { symbolTable.typeToRootType[it] }.toSet().size() > 1)
					error(Error.TYPE_INF_INCOMPAT_DISJ, types.collect { it.name })
				if (inDiffBranches(types))
					error(Error.TYPE_INF_INCOMPAT_BRANCH, types.collect { it.name })

				coalescedExprTypes[expr] = types.find { t -> !symbolTable.subTypes[t].any { it in types } }
			}
			return coalescedExprTypes[expr]
		}

		forInference.each { relation ->
			relation.exprs.eachWithIndex { expr, int i ->
				def prevCandidate = candidateTypes[relation.name][i]
				def currCandidate = coalesce(expr)

				if (currCandidate) {
					if (prevCandidate && prevCandidate != currCandidate) {
						if (symbolTable.typeToRootType[prevCandidate] != symbolTable.typeToRootType[currCandidate])
							error(Error.TYPE_INF_INCOMPAT_DISJ, [prevCandidate, currCandidate].collect { it.name })
						// For inferring a type *among* different rules of the same relation, treat it as a disjunction
						// Therefore, infer the lowest, *higher* common type in the hierarchy
						candidateTypes[relation.name][i] = lowestHigherType(prevCandidate, currCandidate)
					} else
						candidateTypes[relation.name][i] = currCandidate

					if (prevCandidate != candidateTypes[relation.name][i]) {
						// Ignore current rule (in case of recursive relations)
						deltaRules += symbolTable.relUsedInRules[relation.name] - n
					}
				}
				// Still missing type information
				else
					deltaRules << n
			}
		}

		forValidation.each { relation ->
			relation.exprs.eachWithIndex { expr, int i ->
				def types = exprTypes[expr]
				// Still missing type information
				if (!types) return
				// Ignore relations that have an explicit declaration and are used with the same exact types
				if (isExplicit(relation) && types.size() == 1 && types.first() == inferredTypes[relation.name][i]) return

				usedTypes[relation.name][i] << coalesce(expr)
			}
		}
		return n
	}

	IVisitable exit(AggregationElement n) {
		inferredTypes[n.relation.name] = [TYPE_INT]
		exprTypes[n.var] << TYPE_INT
		return n
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(ConstructionElement n) {
		// The type for constructed vars is fixed
		exprTypes[n.constructor.valueExpr] = [n.type] as Set
		return n
	}

	IVisitable exit(Constructor n) { exit(n as Relation) }

	IVisitable exit(Relation n) {
		if (inRuleHead)
			(isExplicit(n) ? forValidation : forInference) << n
		else if (inRuleBody) {
			forValidation << n
			def types = (isExplicit(n) ? inferredTypes[n.name] : candidateTypes[n.name])
			if (types) n.exprs.eachWithIndex { expr, i -> exprTypes[expr] << types[i] }
		}
		return n
	}

	IVisitable exit(BinaryExpr n) {
		def union = exprTypes[n.left] + exprTypes[n.right]
		// Need to share type in both parts of the expression
		exprTypes[n.left] = exprTypes[n.right] = union
		switch (n.op) {
			case BinaryOp.LT:
			case BinaryOp.LEQ:
			case BinaryOp.GT:
			case BinaryOp.GEQ:
			case BinaryOp.EQ:
			case BinaryOp.NEQ:
				exprTypes[n] << TYPE_BOOLEAN
				break
			case BinaryOp.PLUS:
			case BinaryOp.MINUS:
			case BinaryOp.MULT:
			case BinaryOp.DIV:
				union.findAll { it != TYPE_INT && it != TYPE_FLOAT }.each {
					error(Error.TYPE_INF_INCOMPAT_NUM, n)
				}
				exprTypes[n] += union
				break
		}
		return n
	}

	IVisitable exit(ConstantExpr n) {
		if (n.kind == INTEGER) exprTypes[n] << TYPE_INT
		else if (n.kind == REAL) exprTypes[n] << TYPE_FLOAT
		else if (n.kind == BOOLEAN) exprTypes[n] << TYPE_BOOLEAN
		else if (n.kind == STRING) exprTypes[n] << TYPE_STRING
		return n
	}

	IVisitable exit(GroupExpr n) { n }

	def isExplicit(def rel) { inferredTypes.containsKey(rel.name) }

	Type lowestHigherType(Type t1, Type t2) {
		// Traverse the type hierarhcy from the top and stop at the first branching point
		def superTs1 = [t1] + symbolTable.superTypesOrdered[t1]
		def superTs2 = [t2] + symbolTable.superTypesOrdered[t2]
		def k = superTs1.size() - 1, l = superTs2.size() - 1
		while (k >= 0 && l >= 0 && superTs1[k--] == superTs2[l--])
		superTs1[k + 1]
	}

	boolean inDiffBranches(Set<Type> types) {
		def superTs = types.collect { [it] + symbolTable.superTypesOrdered[it] }
		def indexes = superTs.collect { it.size() - 1 }
		int counter
		while ((counter = indexes.withIndex().collect { int index, int i -> index >= 0 ? superTs[i][index] : null }.toSet().grep().size()) == 1)
			indexes = indexes.collect { it - 1 }
		return counter > 1
	}
}
