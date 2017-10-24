package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager

import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.*

class TypeInferenceVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	InfoCollectionVisitingActor infoActor

	// Relation name x Type (final)
	Map<String, List<String>> inferredTypes = [:].withDefault { [] }

	// Relation name x Type Set for each index
	Map<String, List<Set<String>>> tmpRelationTypes = [:].withDefault { [] }
	// Expression x Type Set (for current clause)
	Map<IVisitable, Set<String>> tmpExprTypes = [:].withDefault { [] as Set }
	// Relation Name x Expression x Index (for current clause)
	Map<String, Map<IExpr, Integer>> tmpExprIndices = [:].withDefault { [:] }

	// Implementing fix-point computation
	Set<Rule> deltaRules

	TypeInferenceVisitingActor(InfoCollectionVisitingActor infoActor) {
		actor = this
		this.infoActor = infoActor
	}

	IVisitable visit(Component n) {
		actor.enter(n)
		n.declarations.each { it.accept(this) }

		Set<Rule> oldDeltaRules = n.rules
		while (!oldDeltaRules.isEmpty()) {
			deltaRules = [] as Set
			oldDeltaRules.each { it.accept(this) }
			if (oldDeltaRules == deltaRules)
				ErrorManager.error(ErrorId.TYPE_INFERENCE_FAIL)
			oldDeltaRules = deltaRules
		}
		coalesce()

		actor.exit(n, m)
		null
	}

	IVisitable exit(Program n, Map m) { n }

	IVisitable visit(Declaration n) {
		inferredTypes[n.atom.name] =
				(TYPE in n.annotations) ?
						[n.atom.name] :
						n.types.collect { it.name }

		tmpRelationTypes[n.atom.name] =
				(TYPE in n.annotations) ?
						[[n.atom.name] as Set] :
						n.types.collect { [it.name] as Set }
		null
	}

	IVisitable visit(Rule n) {
		tmpExprTypes = [:].withDefault { [] as Set }
		tmpExprIndices = [:].withDefault { [:] }

		m[n.head] = n.head.accept(this) as IVisitable
		inRuleBody = true
		m[n.body] = n.body?.accept(this) as IVisitable
		inRuleBody = false

		n.head.elements.each {
			def relation = (it instanceof ConstructionElement ? it.constructor.name : (it as Relation).name)
			// null for relations without explicit declarations
			def declaredTypes = inferredTypes[relation]
			tmpExprIndices[relation].each { expr, i ->
				// expr might not have possible types yet
				def currTypeSet = tmpExprTypes[expr]
				if (currTypeSet) {
					// There is an explicit declaration and the possible types
					// for some expressions are more generic that the declared ones
					if (declaredTypes) {
						def superTypes = infoActor.superTypesOrdered[declaredTypes[i]]
						if (currTypeSet.find { it in superTypes })
							ErrorManager.error(ErrorId.TYPE_FIXED, declaredTypes[i], i, relation)
					}
					def prevTypeSet = tmpRelationTypes[relation][i] ?: []
					def newTypeSet = (prevTypeSet + currTypeSet) as Set
					if (prevTypeSet != newTypeSet) {
						tmpRelationTypes[relation][i] = newTypeSet
						deltaRules += infoActor.usedInRules[relation]
					}
				} else
					deltaRules << n
			}
		}
		null
	}

	IVisitable exit(AggregationElement n, Map m) {
		tmpExprTypes[n.var] << "int"
		null
	}

	IVisitable exit(ComparisonElement n, Map m) {
		tmpExprTypes[n] = tmpExprTypes[n.expr]
		null
	}

	IVisitable exit(ConstructionElement n, Map m) {
		tmpExprTypes[n.constructor.valueExpr] << n.type.name
		null
	}

	IVisitable exit(Constructor n, Map m) {
		def types = tmpRelationTypes[n.name]
		n.keyExprs.eachWithIndex { expr, i ->
			tmpExprIndices[n.name][expr] = i
			if (types && types[i]) tmpExprTypes[expr] += types[i]
		}
		if (inRuleBody) {
			int i = n.keyExprs.size()
			if (types && types[i]) tmpExprTypes[n.valueExpr] += types[i]
		}
		null
	}

	IVisitable exit(Relation n, Map m) {
		def types = tmpRelationTypes[n.name]
		n.exprs.eachWithIndex { expr, i ->
			tmpExprIndices[n.name][expr] = i
			if (types && types[i]) tmpExprTypes[expr] += types[i]
		}
		null
	}

	IVisitable exit(BinaryExpr n, Map m) {
		def union = tmpExprTypes[n.left] + tmpExprTypes[n.right]
		// Numeric operations
		if (n.op != BinOperator.EQ && n.op != BinOperator.NEQ)
			union.findAll { it != "int" && it != "float" }.each { ErrorManager.error(ErrorId.TYPE_INCOMP_EXPR) }
		tmpExprTypes[n] = tmpExprTypes[n.left] = tmpExprTypes[n.right] = union
		null
	}

	IVisitable exit(ConstantExpr n, Map m) {
		if (n.type == INTEGER) tmpExprTypes[n] << "int"
		else if (n.type == REAL) tmpExprTypes[n] << "float"
		else if (n.type == BOOLEAN) tmpExprTypes[n] << "bool"
		else if (n.type == STRING) tmpExprTypes[n] << "string"
		null
	}

	private void coalesce() {
		tmpRelationTypes.each { relation, types ->
			types.eachWithIndex { typeSet, i ->
				if (typeSet.size() == 1)
					inferredTypes[relation][i] = typeSet.first()
				else {
					def workingSet = []
					String coalescedType

					// Phase 1: Include types that don't have a better representative already in the set
					typeSet.each { t ->
						def superTypes = infoActor.superTypesOrdered[t]
						if (!superTypes.any { it in typeSet }) workingSet << t
					}

					// Phase 2: Find first common supertype for types in the same hierarchy
					if (workingSet.size() != 1) {
						String t1 = workingSet.first()
						workingSet.removeAt(0)
						while (workingSet) {
							// Iterate types in pairs
							String t2 = workingSet.first()
							workingSet.removeAt(0)

							def superTypesOfT1 = infoActor.superTypesOrdered[t1]
							def superTypesOfT2 = infoActor.superTypesOrdered[t2]
							// Move upwards in the hierarchy until a common type is found
							def superT = t1 = superTypesOfT1.find { it in superTypesOfT2 }
							if (!superT)
								ErrorManager.error(ErrorId.TYPE_INCOMP, relation, i)
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
