package org.clyze.deepdoop.actions

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager

import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.*

class TypeInferenceVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	// Relation name x Type (types are final)
	Map<String, List<String>> inferredTypes = [:].withDefault { [] }
	// Relation name x Type (might have open types)
	Map<String, List<IType>> relationTypes = [:].withDefault { [] }
	// Element x Type (for current clause)
	Map<IVisitable, IType> tmpTypes = [:].withDefault { new OpenType() }
	// Predicate Name x Expression x Index (for current clause)
	Map<String, Map<IExpr, Integer>> exprIndices = [:].withDefault { [:] }

	// Implementing fix-point computation
	Set<Rule> deltaRules

	InfoCollectionVisitingActor infoActor

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

	void enter(Declaration n) {
		tmpTypes.clear()
		exprIndices.clear()
	}

	IVisitable exit(Declaration n, Map m) {
		if (!(TYPE in n.annotations))
			relationTypes[n.atom.name] = n.types.collect { new ClosedType(it.name) }
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
					def prevTypes = relationTypes[relation][i]
					def newTypes = handleTypes(relation, i, prevTypes, types)
					if (prevTypes != newTypes) {
						relationTypes[relation][i] = newTypes
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
			ErrorManager.error(ErrorId.TYPE_FIXED, prev.value, i, relation)
		if (curr instanceof ClosedType)
			ErrorManager.error(ErrorId.TYPE_FIXED, curr.value, i, relation)
	}

	IVisitable exit(AggregationElement n, Map m) {
		tmpTypes[n.var] = ClosedType.INT_T
		null
	}

	IVisitable exit(Constructor n, Map m) {
		tmpTypes[n.valueExpr] = new ClosedType(n.type.name)
		null
	}

	IVisitable exit(Type n, Map m) {
		relationTypes[n.name] = [new ClosedType(n.name)]
		null
	}

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
		exprs.eachWithIndex { expr, i ->
			exprIndices[name][expr] = i
			if (types) tmpTypes[expr] = tmpTypes[expr].join(types[i])
		}
	}

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

	private void coalesce() {
		relationTypes.each { relation, types ->
			types.eachWithIndex { type, i ->
				if (type instanceof ClosedType) {
					inferredTypes[relation][i] = type.value
				} else if (type instanceof OpenType) {
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
						while (resultTypes) {
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
								ErrorManager.error(ErrorId.TYPE_INCOMP, relation, i)
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


interface IType {
	IType join(IType t)
}

// Type with fixed value
@Canonical
@ToString(includePackage = false)
class ClosedType implements IType {

	static final ClosedType INT_T = new ClosedType("int")
	static final ClosedType REAL_T = new ClosedType("float")
	static final ClosedType BOOL_T = new ClosedType("bool")
	static final ClosedType STR_T = new ClosedType("string")

	@Delegate
	String value

	IType join(IType t) {
		return new OpenType().merge(this).merge(t)
	}
}

// Type with candidate values
@Canonical
@ToString(includePackage = false)
class OpenType implements IType {

	@Delegate
	Set<String> values = [] as Set

	IType join(IType t) {
		return new OpenType().merge(this).merge(t)
	}

	def merge(def t) {
		if (t && t instanceof ClosedType) values << t.value
		else if (t && t instanceof OpenType) values.addAll(t.values)
		return this
	}
}
