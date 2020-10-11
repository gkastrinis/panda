package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.*
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.element.relation.Type.*
import static org.codesimius.panda.datalog.expr.BinaryOp.*
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.*
import static org.codesimius.panda.datalog.expr.UnaryOp.TO_STR
import static org.codesimius.panda.datalog.expr.VariableExpr.genN as varN

@Canonical
class TypeInferenceTransformer extends DefaultTransformer {

	@Delegate
	Compiler compiler

	Map<String, List<Type>> inferredTypes = [:].withDefault { [] }

	// Relations with apriori known types
	private Set<String> explicitRelations
	// Expression x Type (for current rule)
	private Map<IExpr, Type> exprType
	// Expressions in a rule that are missing a type
	private Set<IExpr> untypedExprs

	// Relations found in the current rule head that will have types inferred
	private Set<Relation> forHeadInference
	// Relations found in the current rule (head/body) that will have types validated
	// For those found in the head, this is because there is an explicit declaration
	private Set<Relation> forValidation

	// Variables bound by relations in a rule body
	private Set<VariableExpr> boundVars

	// Implementing fix-point computation
	private Set<Rule> deltaRules

	IVisitable visit(BlockLvl0 n) {
		currDatalog = n

		// Gather explicit and known relation types
		n.relDeclarations.each { visit it }
		n.typeDeclarations.each { visit it }
		explicitRelations = (inferredTypes.keySet() + []) as Set

		// Gather candidate types for relations, until fix-point
		Set<Rule> oldDeltaRules = n.rules
		while (!oldDeltaRules.empty) {
			deltaRules = [] as Set
			oldDeltaRules.each { m[it] = visit it }
			if (oldDeltaRules == deltaRules)
				error(Error.TYPE_INF_FAIL, null)
			oldDeltaRules = deltaRules
		}

		// Incomplete relations with no usage or declaration (e.g. @output P)
		n.relDeclarations
				.findAll { it.relation.name !in inferredTypes }
				.each { error(loc(it.relation), Error.REL_NO_DECL, it.relation.name) }

		// Fill partial declarations and add implicit ones
		// Ignore relations that derive from types
		def interestingRelationsWithTypes = inferredTypes.findAll { rel, types ->
			!n.allTypes.any { it.name == rel } && (rel !in AggregationElement.SUPPORTED_PREDICATES)
		}
		def relDeclarations = interestingRelationsWithTypes.collect { rel, types ->
			def vars = varN(types.size())
			def decl = currDatalog.relationToDeclaration[rel]
			// Explicit, partial declaration
			if (decl && !decl.types) {
				decl.relation.exprs = vars
				decl.types = types
			}
			// Implicit declaration
			else if (!decl)
				decl = new RelDeclaration(new Relation(rel, vars), types)

			return decl
		} as Set

		new BlockLvl0(relDeclarations, n.typeDeclarations, n.rules.collect { m[it] as Rule } as Set)
	}

	void enter(RelDeclaration n) {
		// Not a partial declaration
		if (n.types) inferredTypes[n.relation.name] = n.types
	}

	void enter(TypeDeclaration n) {
		// Types can be used as unary relations (with the same type and name)
		inferredTypes[n.type.name] = [n.type]
	}

	IVisitable visit(Rule n) {
		parentAnnotations = n.annotations
		forHeadInference = [] as Set
		forValidation = [] as Set
		exprType = [:]
		boundVars = currDatalog.getBoundBodyVars(n) as Set

		do {
			untypedExprs = [] as Set
			inRuleHead = true
			m[n.head] = visit n.head
			inRuleHead = false
			inRuleBody = true
			if (n.body) m[n.body] = visit n.body
			inRuleBody = false
			// An expr type was needed and missing, but then it was inferred
		} while (untypedExprs.any { exprType[it] })

		forHeadInference.each { Relation relation ->
			relation.exprs.eachWithIndex { expr, int i ->
				def prevType = inferredTypes[relation.name][i]
				def currType = join(prevType, exprType[expr])

				// Relation has an explicit declaration. Just validate
				// currType must be a subtype of the explicit type
				if (relation.name in explicitRelations) {
					if (currType !in currDatalog.getExtendedSubTypesOf(prevType))
						error(loc(n), Error.TYPE_INF_INCOMPAT_USE, relation.name, i, prevType, currType)
				}
				// Still missing type information
				else if (!currType) {
					deltaRules << n
				}
				// Type information has changed
				else if (prevType != currType) {
					inferredTypes[relation.name][i] = currType
					// Affected rules must be revisited
					// Ignore current rule (in case of recursive relations)
					deltaRules += currDatalog.relationUsedInRules[relation.name] - n
				}
			}
		}

		forValidation.each { Relation relation ->
			relation.exprs.eachWithIndex { expr, int i ->
				def prevType = inferredTypes[relation.name][i]
				def currType = exprType[expr]
				// Type information is present and is new
				if (prevType && currType && (currType !in currDatalog.getExtendedSubTypesOf(prevType)))
					error(loc(n), Error.TYPE_INF_INCOMPAT_USE, relation.name, i, prevType, currType)
			}
		}

		super.exit(n)
	}

	void enter(AggregationElement n) {
		def name = n.relation.name
		inferredTypes[name] = AggregationElement.PREDICATE_TYPES[name]
		exprType[n.var] = meet(exprType[n.var], AggregationElement.PREDICATE_RET_TYPE[name])
	}

	void enter(ConstructionElement n) {
		// The type for constructed vars is fixed
		exprType[n.constructor.valueExpr] = n.type
	}

	void enter(Constructor n) { enter(n as Relation) }

	void enter(Relation n) {
		if (inRuleHead)
			(n.name in explicitRelations ? forValidation : forHeadInference) << n
		else if (inRuleBody) {
			forValidation << n
			def types = inferredTypes[n.name]
			if (types) n.exprs.eachWithIndex { expr, i ->
				if (expr instanceof VariableExpr && expr.name == '_') return
				exprType[expr] = meet(exprType[expr], types[i])
			}
		}
	}

	IVisitable exit(BinaryExpr n) {
		def numericalTypes = [TYPE_INT, TYPE_REAL]
		def ordinalTypes = [TYPE_INT, TYPE_REAL, TYPE_STRING]
		def leftType = exprType[n.left], rightType = exprType[n.right]
		def types = [leftType, rightType]

		def handleAssignment = { IExpr e, IExpr other ->
			// Assignment instead of equality check
			if (e instanceof VariableExpr && (e as VariableExpr) !in boundVars) {
				exprType[e] = join(leftType, rightType)
				return true
			}
		}
		if (n.op == EQ && handleAssignment(n.left, n.right)) return super.exit(n)
		if (n.op == EQ && handleAssignment(n.right, n.left)) return super.exit(n)

		if (!leftType || !rightType) {
			if (!leftType) untypedExprs << n.left
			if (!rightType) untypedExprs << n.right
			return n
		}

		switch (n.op) {
			case LT:
			case LEQ:
			case GT:
			case GEQ:
				if (types.any { it !in ordinalTypes })
					error(findParentLoc(), Error.TYPE_INF_INCOMPAT, types)
			case EQ:
			case NEQ:
				// Just join types to test for compatibility
				join(leftType, rightType)
				break
			case PLUS:
				if (TYPE_BOOLEAN in types)
					error(findParentLoc(), Error.TYPE_INF_INCOMPAT, types)

				if (TYPE_STRING in types) {
					def newLeft = m[n.left] as IExpr, newRight = m[n.right] as IExpr
					newLeft = leftType in numericalTypes ? new UnaryExpr(TO_STR, newLeft) : newLeft
					newRight = rightType in numericalTypes ? new UnaryExpr(TO_STR, newRight) : newRight
					exprType[n] = TYPE_STRING
					return new BinaryExpr(newLeft, CAT, newRight)
				}
			case MINUS:
			case MULT:
			case DIV:
				if (types.any { it !in numericalTypes })
					error(findParentLoc(), Error.TYPE_INF_INCOMPAT, types)
				exprType[n] = join(leftType, rightType)
				break
		}

		super.exit(n)
	}

	void enter(ConstantExpr n) {
		if (n.kind == INTEGER) exprType[n] = TYPE_INT
		else if (n.kind == REAL) exprType[n] = TYPE_REAL
		else if (n.kind == BOOLEAN) exprType[n] = TYPE_BOOLEAN
		else if (n.kind == STRING) exprType[n] = TYPE_STRING
	}

	// For inferring a type *in* a rule body, approximate by assuming all relations are used conjunctively
	// Therefore, infer the lowest type in the hierarchy (type-lattice) that is present (meet operation)
	// Since there is no multiple subtyping, either currType or t must be the lowest type
	Type meet(Type currType, Type t) {
		if (currType == t)
			return currType
		else if (!currType || t in currDatalog.getExtendedSubTypesOf(currType))
			return t
		else if (currType !in currDatalog.subTypes[t])
			error(findParentLoc(), Error.TYPE_INF_INCOMPAT, [currType.name, t.name])

		return currType
	}

	// For inferring a type *among* different rules of the same relation, treat it as a disjunction
	// Therefore, infer the lowest, *higher* common type in the hierarchy (type-lattice / join operation)
	// Traverse the type hierarchy from the top and stop at the first branching point
	Type join(Type t1, Type t2) {
		if (!t1) return t2
		if (!t2) return t1

		if (currDatalog.typeToRootType[t1] != currDatalog.typeToRootType[t2])
			error(findParentLoc(), Error.TYPE_INF_INCOMPAT, [t1.name, t2.name])

		def superTs1 = [t1] + currDatalog.superTypesOrdered[t1]
		def superTs2 = [t2] + currDatalog.superTypesOrdered[t2]
		def k = superTs1.size() - 1, l = superTs2.size() - 1
		while (k >= 0 && l >= 0 && superTs1[k--] == superTs2[l--]) assert true
		superTs1[k + 1]
	}
}
