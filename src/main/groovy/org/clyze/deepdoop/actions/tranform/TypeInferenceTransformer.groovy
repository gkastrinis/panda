package org.clyze.deepdoop.actions.tranform

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.RelationInfoVisitingActor
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.*
import org.clyze.deepdoop.system.Error

import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.*
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN
import static org.clyze.deepdoop.system.Error.error

@Canonical
class TypeInferenceTransformer extends DefaultTransformer {

	RelationInfoVisitingActor relationInfo

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
		n.relDeclarations.each { visit it }

		Set<Rule> oldDeltaRules = n.rules
		while (!oldDeltaRules.isEmpty()) {
			deltaRules = [] as Set
			oldDeltaRules.each { visit it }
			if (oldDeltaRules == deltaRules)
				error(Error.TYPE_INFERENCE_FAIL, null)
			oldDeltaRules = deltaRules
		}

		coalesce()

		// Fill partial declarations and add implicit ones
		def relDs = inferredTypes.findAll { rel, types -> !(new Type(rel) in relationInfo.typesToOptimize) }.collect { rel, types ->
			RelDeclaration d = relToDecl[rel]
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
						def superTs = relationInfo.superTypesOrdered[declaredTypes[i]]
						if (currTypeSet.any { it in superTs })
							error(Error.TYPE_INFERENCE_FIXED, declaredTypes[i], i, relName)
					}
					def prevTypeSet = tmpRelationTypes[relName][i] ?: [] as Set
					def newTypeSet = (prevTypeSet + currTypeSet) as Set
					if (prevTypeSet != newTypeSet) {
						tmpRelationTypes[relName][i] = newTypeSet
						deltaRules += relationInfo.relUsedInRules[relName]
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
				error(Error.TYPE_INCOMP_EXPR, null)
			}
		tmpExprTypes[n] = tmpExprTypes[n.left] = tmpExprTypes[n.right] = union
		return n
	}

	IVisitable exit(ConstantExpr n) {
		if (n.type == REAL || n.type == BOOLEAN) error(Error.TYPE_UNSUPP, n.type as String)

		if (n.type == INTEGER) tmpExprTypes[n] << Type.TYPE_INT
		else if (n.type == REAL) tmpExprTypes[n] << Type.TYPE_FLOAT
		else if (n.type == BOOLEAN) tmpExprTypes[n] << Type.TYPE_BOOLEAN
		else if (n.type == STRING) tmpExprTypes[n] << Type.TYPE_STRING
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
						def superTs = relationInfo.superTypesOrdered[t]
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

							def superTypesOfT1 = relationInfo.superTypesOrdered[t1]
							def superTypesOfT2 = relationInfo.superTypesOrdered[t2]
							// Move upwards in the hierarchy until a common type is found
							def superT = t1 = superTypesOfT1.find { it in superTypesOfT2 }
							if (!superT) error(Error.TYPE_INCOMP, relation, i)
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
