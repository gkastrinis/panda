package org.clyze.deepdoop.actions

import org.apache.commons.logging.LogFactory
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager

import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.*

class TypeInferenceVisitingActor extends PostOrderVisitor<IVisitable> implements IActor<IVisitable>, TDummyActor<IVisitable> {

	Map<String, List<String>> inferredTypes = [:].withDefault { [] }

	Map<String, ComplexType> relationTypes = [:].withDefault { new ComplexType() }

	Map<IVisitable, IType> tmpTypes = [:].withDefault { new OpenType() }
	// Predicate Name x Variable x Index (for current clause)
	Map<String, Map<VariableExpr, Integer>> varIndices = [:].withDefault { [:] }


	//Map<String, List<String>> inferredTypes = [:]
	// Predicate Name x Parameter Index x Possible Types
	//Map<String, Map<Integer, Set<String>>> possibleTypes = [:].withDefault { [:].withDefault { [] as Set } }
	/// Variable x Possible Types (for current clause)
	///Map<VariableExpr, Set<String>> varTypes = [:].withDefault { [] as Set }
	/// Misc Elements (e.g. constants) x Type (for current clause)
	///Map<IVisitable, String> values = [:]

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
		//possibleTypes.each { pred, map ->
		//	inferredTypes[pred] = map.collect { i, types -> coalesce(types, pred, i) }
		//}
		//relationTypes.each { relation, complexType -> coalesce2(relation, complexType)}

		coalesce()

		actor.exit(n, null)
		null
	}

	IVisitable exit(Program n, Map m) { n }

	//IVisitable exit(CmdComponent n, Map m) { null }

	//IVisitable exit(Component n, Map m) { null }

	//IVisitable exit(Constraint n, Map m) { null }

	void enter(Declaration n) {
		//varTypes.clear()
		//values.clear()
		tmpTypes.clear()
		varIndices.clear()
	}

	IVisitable exit(Declaration n, Map m) {
		if (!n.annotations.any { it.kind == Annotation.Kind.ENTITY }) {
			//def t = new ComplexType()
			//n.types.eachWithIndex { type, i ->
			//	possibleTypes[n.atom.name][i] << type.name
				//t.components << new ClosedType(type.name)
			//}
			relationTypes[n.atom.name] = new ComplexType(n.types.collect { new ClosedType(it.name) })
		}
		null
	}

	//IVisitable exit(RefModeDeclaration n, Map m) { null }

	void enter(Rule n) {
		//varTypes.clear()
		//values.clear()
		tmpTypes.clear()
		varIndices.clear()
	}

	IVisitable exit(Rule n, Map m) {
		n.head.elements.each {
			def relation = (it as Relation).name
			varIndices[relation].each { var, i ->
				def types = tmpTypes[var]
				if (types) {
					def prevTypes = relationTypes[relation].components[i]
					def newTypes = types.join(prevTypes)
					if (prevTypes != newTypes) {
						relationTypes[relation].components[i] = newTypes
						deltaRules += infoActor.affectedRules[relation]
					}
				}
				//def types = varTypes[var]
				//// Var might not have possible types yet
				//if (!types.isEmpty()) {
				//	// Don't add to deltas if no new type is inferred
				//	def currentTypes = possibleTypes[relation][i]
				//	if (types.any { !(it in currentTypes) }) {
				//		possibleTypes[relation][i] += types
				//		// Add to deltas every rule that uses the current predicate in it's body
				//		deltaRules += infoActor.affectedRules[relation]
				//	}
				//} else
				//	deltaRules << n
			}
		}
		null
	}

	//IVisitable exit(AggregationElement n, Map m) { null }

	IVisitable exit(ComparisonElement n, Map m) {
		//VariableExpr var
		//if (n.expr.left instanceof VariableExpr) var = n.expr.left as VariableExpr
		//else if (n.expr.right instanceof VariableExpr) var = n.expr.right as VariableExpr

		//ConstantExpr value
		//if (n.expr.left instanceof ConstantExpr) value = n.expr.left as ConstantExpr
		//else if (n.expr.right instanceof ConstantExpr) value = n.expr.right as ConstantExpr

		//if (var && value) varTypes[var] << values[value]

		tmpTypes[n] = tmpTypes[n.expr]
		null
	}

	//IVisitable exit(GroupElement n, Map m) { null }

	//IVisitable exit(LogicalElement n, Map m) { null }

	//IVisitable exit(NegationElement n, Map m) { null }

	IVisitable exit(Constructor n, Map m) {
		//exit(n as Functional, m)
		//varTypes[n.valueExpr as VariableExpr] << n.entity.name
		null
	}

	//IVisitable exit(Entity n, Map m) { null }

	IVisitable exit(Functional n, Map m) {
		(n.keyExprs + n.valueExpr).eachWithIndex { expr, i -> varWithIndex(n.name, expr, i) }
		null
	}

	IVisitable exit(Predicate n, Map m) {
		n.exprs.eachWithIndex { expr, i -> varWithIndex(n.name, expr, i) }

		def types = relationTypes[n.name]
		if (types) {
			n.exprs.withIndex()
					.findAll { expr, i -> expr instanceof VariableExpr }
					.each { expr, i -> tmpTypes[expr as IExpr].merge(types.components[i as int]) }
		}
		null
	}

	//IVisitable exit(Primitive n, Map m) { null }

	//IVisitable exit(RefMode n, Map m) { null }

	IVisitable exit(BinaryExpr n, Map m) {
		if (n.op == BinOperator.EQ || n.op == BinOperator.NEQ) {
			def commonType = tmpTypes[n.left].join(tmpTypes[n.right])
			// TODO add example where n.left needs to be updated
			tmpTypes[n] = tmpTypes[n.left] = tmpTypes[n.right] = commonType
		} else {
			// TODO numeric checks
			tmpTypes[n].merge(tmpTypes[n.left])
			tmpTypes[n].merge(tmpTypes[n.right])
		}
		null
	}

	IVisitable exit(ConstantExpr n, Map m) {
		//if (n.type == INTEGER) values[n] = "int"
		//else if (n.type == REAL) values[n] = "real"
		//else if (n.type == STRING) values[n] = "string"
		if (n.type == INTEGER) tmpTypes[n] = ClosedType.INT_T
		else if (n.type == REAL) tmpTypes[n] = ClosedType.REAL_T
		else if (n.type == BOOLEAN) tmpTypes[n] = ClosedType.BOOL_T
		else if (n.type == STRING) tmpTypes[n] = ClosedType.STR_T

		null
	}

	//IVisitable exit(GroupExpr n, Map m) { null }

	//IVisitable exit(VariableExpr n, Map m) { null }

	private void varWithIndex(String name, IExpr expr, int i) {
		if (expr instanceof VariableExpr) {
			def var = expr as VariableExpr
			varIndices[name][var] = i
			//if (name in possibleTypes && !var.isDontCare())
			//	varTypes[var] += possibleTypes[name][i]
		}
	}

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
