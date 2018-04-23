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

import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.*
import static org.codesimius.panda.datalog.expr.VariableExpr.genN as varN
import static org.codesimius.panda.system.Error.error

@Canonical
class TypeInferenceTransformer extends DefaultTransformer {

	SymbolTable symbolTable

	// Relation name x Type (final)
	Map<String, List<Type>> inferredTypes = [:].withDefault { [] }

	// Relation name x Type Set for each index
	private Map<String, List<Set<Type>>> tmpRelationTypes = [:].withDefault { [] }
	// Expression x Type Set (for current clause)
	private Map<IVisitable, Set<Type>> tmpExprTypes
	// Relation Name x Parameter Expression x Index (for current clause)
	private Map<String, Map<IExpr, Integer>> tmpExprIndices

	private Map<String, RelDeclaration> relToDecl = [:]
	// Implementing fix-point computation
	private Set<Rule> deltaRules

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
		def relDs = inferredTypes.collect { rel, types ->
			def d = relToDecl[rel]
			if (d?.types) return d

			def vars = varN(types.size())
			if (d) {
				d.relation.exprs = vars
				d.types = types
				return d
			}

			return new RelDeclaration(new Relation(rel, vars), types)
		} as Set

		new BlockLvl0(relDs, n.typeDeclarations, n.rules)
	}

	void enter(RelDeclaration n) {
		tmpExprTypes = [:].withDefault { [] as Set }
		tmpExprIndices = [:].withDefault { [:] }
	}

	IVisitable exit(RelDeclaration n) {
		relToDecl[n.relation.name] = n
		if (n.types) {
			inferredTypes[n.relation.name] = n.types
			tmpRelationTypes[n.relation.name] = n.types.collect { [it] as Set }
		}
		return n
	}

	IVisitable exit(TypeDeclaration n) { n }

	void enter(Rule n) {
		tmpExprTypes = [:].withDefault { [] as Set }
		tmpExprIndices = [:].withDefault { [:] }
	}

	IVisitable exit(Rule n) {
		asElements(n.head).each {
			def relName = (it instanceof ConstructionElement ? it.constructor.name : (it as Relation).name)
			// null for relations without explicit declarations
			def declaredTypes = inferredTypes[relName]
			tmpExprIndices[relName].each { expr, i ->
				// expr might not have possible types yet
				def currTypeSet = tmpExprTypes[expr]
				if (currTypeSet) {
					// There is an explicit declaration and the possible types
					// for some expressions are more generic that the declared ones
					if (declaredTypes) {
						def superTs = symbolTable.superTypesOrdered[declaredTypes[i]]
						if (currTypeSet.any { it in superTs })
							error(Error.TYPE_INFERENCE_FIXED, declaredTypes[i], i, relName)
					}
					def prevTypeSet = tmpRelationTypes[relName][i] ?: [] as Set
					def newTypeSet = (prevTypeSet + currTypeSet) as Set
					if (prevTypeSet != newTypeSet) {
						tmpRelationTypes[relName][i] = newTypeSet
						deltaRules += symbolTable.relUsedInRules[relName]
					}
				} else
					deltaRules << n
			}
		}
		return n
	}

	IVisitable exit(AggregationElement n) {
		tmpExprTypes[n.var] << Type.TYPE_INT
		return n
	}

	IVisitable exit(ComparisonElement n) {
		tmpExprTypes[n] = tmpExprTypes[n.expr]
		return n
	}

	IVisitable exit(ConstructionElement n) {
		tmpExprTypes[n.constructor.valueExpr] << n.type
		return n
	}

	IVisitable exit(Constructor n) {
		def types = tmpRelationTypes[n.name]
		n.keyExprs.eachWithIndex { expr, i ->
			tmpExprIndices[n.name][expr] = i
			if (types && types[i]) tmpExprTypes[expr] += types[i]
		}
		if (inRuleBody) {
			int i = n.keyExprs.size()
			if (types && types[i]) tmpExprTypes[n.valueExpr] += types[i]
		}
		return n
	}

	IVisitable exit(Relation n) {
		def types = tmpRelationTypes[n.name]
		n.exprs.eachWithIndex { expr, i ->
			tmpExprIndices[n.name][expr] = i
			if (inRuleBody && types && types[i]) tmpExprTypes[expr] += types[i]
		}
		return n
	}

	IVisitable exit(BinaryExpr n) {
		def union = tmpExprTypes[n.left] + tmpExprTypes[n.right]
		// Numeric operations
		if (n.op != BinaryOp.EQ && n.op != BinaryOp.NEQ)
			union.findAll { it != Type.TYPE_INT && it != Type.TYPE_FLOAT }.each {
				error(Error.TYPE_INCOMPAT_EXPR, null)
			}
		tmpExprTypes[n] = tmpExprTypes[n.left] = tmpExprTypes[n.right] = union
		return n
	}

	IVisitable exit(ConstantExpr n) {
		if (n.kind == INTEGER) tmpExprTypes[n] << Type.TYPE_INT
		else if (n.kind == REAL) tmpExprTypes[n] << Type.TYPE_FLOAT
		else if (n.kind == BOOLEAN) tmpExprTypes[n] << Type.TYPE_BOOLEAN
		else if (n.kind == STRING) tmpExprTypes[n] << Type.TYPE_STRING
		return n
	}

	IVisitable exit(GroupExpr n) { n }

	private void coalesce() {
		tmpRelationTypes.each { relation, types ->
			types.eachWithIndex { typeSet, i ->
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
							def superT = t1 = superTypesOfT1.find { it in superTypesOfT2 }
							if (!superT) error(Error.TYPE_INCOMPAT, relation, i)
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
