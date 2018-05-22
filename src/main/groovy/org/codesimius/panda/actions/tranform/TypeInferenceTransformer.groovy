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
	// Relation x Type Set (for each index)
	Map<String, List<Set<Type>>> candidateTypes = [:].withDefault { [].withDefault { [] as Set } }
	// Expression x Type Set (for active rule)
	Map<IExpr, Set<Type>> exprTypes
	// Relations (found in hear and body) of the active rule
	Set<Relation> relations
	// Implementing fix-point computation
	Set<Rule> deltaRules
	// Relation x Declaration
	Map<String, RelDeclaration> relDeclarations = [:]

	IVisitable visit(BlockLvl0 n) {
		// Gather candidate types for relations, until fix-point
		n.relDeclarations.each { visit it }

		Set<Rule> oldDeltaRules = n.rules
		while (!oldDeltaRules.isEmpty()) {
			deltaRules = [] as Set
			oldDeltaRules.each { visit it }
			if (oldDeltaRules == deltaRules)
				error(Error.TYPE_INFERENCE_FAIL, null)
			oldDeltaRules = deltaRules
		}
		// Type inference
		coalesce()

		// Fill partial declarations and add implicit ones
		// Ignore relations that derive from types
		def relDs = inferredTypes
				.findAll { rel, types -> !symbolTable.allTypes.any { it.name == rel } }
				.collect { rel, types ->

			def d = relDeclarations[rel]
			// Explicit, non-partial declaration
			if (d?.types) return d

			def vars = varN(types.size())
			// Explicit, partial declaration
			if (d) {
				d.relation.exprs = vars
				d.types = types
				return d
			}

			// Implicit declaration
			return new RelDeclaration(new Relation(rel, vars), types)
		} as Set

		new BlockLvl0(relDs, n.typeDeclarations, n.rules)
	}

	IVisitable exit(RelDeclaration n) {
		relDeclarations[n.relation.name] = n
		if (n.types) {
			inferredTypes[n.relation.name] = n.types
			candidateTypes[n.relation.name] = n.types.collect { [it] as Set }
		}
		return n
	}

	IVisitable exit(TypeDeclaration n) { n }

	void enter(Rule n) {
		exprTypes = [:].withDefault { [] as Set }
		relations = [] as Set
	}

	IVisitable exit(Rule n) {
		relations.each { rel ->
			rel.exprs.eachWithIndex { expr, int i ->
				def currTypeSet = exprTypes[expr]
				if (currTypeSet) {
					def prevTypeSet = candidateTypes[rel.name][i]
					def newTypeSet = prevTypeSet + currTypeSet
					if (prevTypeSet != newTypeSet) {
						candidateTypes[rel.name][i] = newTypeSet
						// Ignore current rule (in case of recursive relations)
						deltaRules += symbolTable.relUsedInRules[rel.name] - n
					}
				} else
					deltaRules << n
			}
		}
		return n
	}

	IVisitable exit(AggregationElement n) {
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
		if (inDecl) return n

		if (inRuleBody) {
			def types = candidateTypes[n.name]
			n.exprs.eachWithIndex { expr, i -> if (types) exprTypes[expr] += types[i] }
		}
		relations << n
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
					error(Error.TYPE_INCOMPAT_EXPR, n)
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

	private void coalesce() {
		candidateTypes.each { relation, types ->
			def declaredTypes = inferredTypes[relation]
			types.eachWithIndex { typeSet, i ->
				// Non-empty for relations with explicit declarations
				if (declaredTypes) {
					// There is an explicit declaration and the possible types
					// for some expressions are more generic that the declared ones
					def superTs = symbolTable.superTypesOrdered[declaredTypes[i]]
					if (typeSet.any { it in superTs })
						error(Error.TYPE_INFERENCE_FIXED, declaredTypes[i], i, relation)
				}

				if (typeSet.size() == 1)
					inferredTypes[relation][i] = typeSet.first()
				else {
					def workingSet = [] as List<Type>
					Type coalescedType

					// Phase 1: Include types that don't have a better representative already in the set
					typeSet.each { t ->
						def superTs = symbolTable.superTypesOrdered[t]
						if (!superTs.any { it in typeSet }) workingSet << t
					}

					// Phase 2: Find first common supertype for types in the same hierarchy
					if (workingSet.size() != 1) {
						def t1 = workingSet.first()
						workingSet.removeAt(0)
						while (workingSet) {
							// Iterate types in pairs
							def t2 = workingSet.first()
							workingSet.removeAt(0)

							def superTypesOfT1 = symbolTable.superTypesOrdered[t1]
							def superTypesOfT2 = symbolTable.superTypesOrdered[t2]
							// Move upwards in the hierarchy until a common kind is found
							t1 = superTypesOfT1.find { it in superTypesOfT2 }
							if (!t1)
								error(Error.TYPE_INCOMPAT, relation, i, typeSet.collect { it.name }.join(", "))
						}
						coalescedType = t1
					} else
						coalescedType = workingSet.first()

					inferredTypes[relation][i] = coalescedType
				}
			}
		}
	}
}
