package org.clyze.deepdoop.actions

import org.apache.commons.logging.LogFactory
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager

import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.*

class TypeInferenceVisitingActor extends PostOrderVisitor<IVisitable> implements IActor<IVisitable>, TDummyActor<IVisitable> {

	// Relation name x Type (types are final)
	Map<String, List<String>> inferredTypes = [:].withDefault { [] }
	// Relation name x Type (might have open types)
	Map<String, ComplexType> relationTypes = [:].withDefault { new ComplexType() }
	// Element x Type (for current clause)
	Map<IVisitable, IType> tmpTypes = [:].withDefault { new OpenType() }
	// Predicate Name x Expression x Index (for current clause)
	Map<String, Map<IExpr, Integer>> exprIndices = [:].withDefault { [:] }

	// Implementing fix-point computation
	Set<Rule> deltaRules

	InfoCollectionVisitingActor infoActor

	TypeInferenceVisitingActor(InfoCollectionVisitingActor infoActor) {
		// Implemented this way, because Java doesn't allow usage of "this"
		// keyword before all implicit/explicit calls to super/this have
		// returned
		super(null)
		actor = this
		this.infoActor = infoActor
	}

	IVisitable visit(Component n) {
		actor.enter(n)
		n.declarations.each { it.accept(this) }
		n.constraints.each { it.accept(this) }

		Set<Rule> oldDeltaRules = n.rules
		while (!oldDeltaRules.isEmpty()) {
			deltaRules = [] as Set
			oldDeltaRules.each { it.accept(this) }
			if (oldDeltaRules == deltaRules) {
				LogFactory.getLog(this.class).warn("TODO fix")
				break
			}
			oldDeltaRules = deltaRules
		}
		coalesce()

		actor.exit(n, null)
		null
	}

	IVisitable exit(Program n, Map m) { n }

	//IVisitable exit(CmdComponent n, Map m) { null }

	//IVisitable exit(Component n, Map m) { null }

	//IVisitable exit(Constraint n, Map m) { null }

	void enter(Declaration n) {
		tmpTypes.clear()
		exprIndices.clear()
	}

	IVisitable exit(Declaration n, Map m) {
		if (!n.annotations.any { it.kind == Annotation.Kind.ENTITY }) {
			relationTypes[n.atom.name] = new ComplexType(n.types.collect { new ClosedType(it.name) })
		}
		null
	}

	void enter(Rule n) {
		tmpTypes.clear()
		exprIndices.clear()
	}

	IVisitable exit(Rule n, Map m) {
		n.head.elements.each {
			def relation = (it as Relation).name
			exprIndices[relation].each { expr, i ->
				// Var might not have possible types yet
				def types = tmpTypes[expr]
				if (types) {
					def prevTypes = relationTypes[relation].components[i]
					def newTypes = handleTypes(relation, i, prevTypes, types)
					if (prevTypes != newTypes) {
						relationTypes[relation].components[i] = newTypes
						deltaRules += infoActor.affectedRules[relation]
					}
				} else
					deltaRules << n
			}
		}
		null
	}

	static def handleTypes(def relation, def i, def prev, def curr) {
		if (!prev) return curr
		if (!curr) return prev
		// Closed & Closed with same value => return Closed
		// Closed & Open with single same value => return Open
		// Open with single value & Closed with same value => return Open
		// Open & Open => return join
		// Otherwise ERROR
		if (prev instanceof ClosedType && curr instanceof ClosedType) {
			if (prev.value == curr.value) return prev
		} else if (prev instanceof ClosedType && curr instanceof OpenType && curr.values.size() == 1) {
			if (prev.value == curr.values.first()) return curr
		} else if (prev instanceof OpenType && prev.values.size() == 1 && curr instanceof ClosedType) {
			if (prev.values.first() == curr.value) return prev
		} else if (prev instanceof OpenType && curr instanceof OpenType) {
			return prev.join(curr)
		}

		if (prev instanceof ClosedType)
			ErrorManager.error(ErrorId.FIXED_TYPE, prev.value, i, relation)
		if (curr instanceof ClosedType)
			ErrorManager.error(ErrorId.FIXED_TYPE, curr.value, i, relation)
	}

	IVisitable exit(AggregationElement n, Map m) {
		tmpTypes[n.var] = ClosedType.INT_T
		null
	}

	//IVisitable exit(ComparisonElement n, Map m) { null }

	//IVisitable exit(GroupElement n, Map m) { null }

	//IVisitable exit(LogicalElement n, Map m) { null }

	//IVisitable exit(NegationElement n, Map m) { null }

	IVisitable exit(Constructor n, Map m) {
		tmpTypes[n.valueExpr] = new ClosedType(n.entity.name)
		null
	}

	//IVisitable exit(Entity n, Map m) { null }

	IVisitable exit(Functional n, Map m) {
		exitRelation(n.name, n.keyExprs + [n.valueExpr])
		null
	}

	IVisitable exit(Predicate n, Map m) {
		exitRelation(n.name, n.exprs)
		null
	}

	private void exitRelation(String name, List<IExpr> exprs) {
		def types = relationTypes[name]
		exprs.eachWithIndex{ expr, i ->
			exprIndices[name][expr] = i
			if (types) tmpTypes[expr] = tmpTypes[expr].join(types.components[i])
		}
	}

	//IVisitable exit(Primitive n, Map m) { null }

	IVisitable exit(BinaryExpr n, Map m) {
		def commonType = tmpTypes[n.left].join(tmpTypes[n.right])
		if (n.op != BinOperator.EQ && n.op != BinOperator.NEQ) {
			// TODO numeric checks
		}
		// Both parts must have the same type
		tmpTypes[n] = tmpTypes[n.left] = tmpTypes[n.right] = commonType
		null
	}

	IVisitable exit(ConstantExpr n, Map m) {
		if (n.type == INTEGER) tmpTypes[n] = ClosedType.INT_T
		else if (n.type == REAL) tmpTypes[n] = ClosedType.REAL_T
		else if (n.type == BOOLEAN) tmpTypes[n] = ClosedType.BOOL_T
		else if (n.type == STRING) tmpTypes[n] = ClosedType.STR_T
		null
	}

	//IVisitable exit(GroupExpr n, Map m) { null }

	//IVisitable exit(VariableExpr n, Map m) { null }

	private void coalesce() {
		relationTypes.each { relation, complexType ->
			complexType.components.eachWithIndex{ type, i ->
				if (type instanceof ClosedType) {
					inferredTypes[relation][i] = type.value
				} else if (type instanceof  OpenType) {
					def resultTypes = []
					String coalescedType

					// Phase 1: Include types that don't have a better representative already in the set
					type.values.each { t ->
						def superTypes = infoActor.superTypesOrdered[t]
						if (!superTypes.any { it in type.values }) resultTypes << t
					}

					// Phase 2: Find first common supertype for types in the same hierarchy
					if (resultTypes.size() != 1) {
						String t1 = resultTypes[0]
						resultTypes.removeAt(0)
						while(resultTypes) {
							// Iterate types in pairs
							String t2 = resultTypes[0]
							resultTypes.removeAt(0)

							def superTypesOfT1 = infoActor.superTypesOrdered[t1]
							def superTypesOfT2 = infoActor.superTypesOrdered[t2]
							// Move upwards in the hierarchy until a common type is found
							String superT
							superTypesOfT1.each {
								if (!superT && it in superTypesOfT2) superT = it
							}
							t1 = superT

							if (!superT)
								ErrorManager.error(ErrorId.INCOMPATIBLE_TYPES, relation, i)
						}
						coalescedType = t1
					} else
						coalescedType = resultTypes.first()

					inferredTypes[relation][i] = coalescedType
				}
			}
		}
	}
}
