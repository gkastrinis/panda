package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.IElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.*
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.element.LogicalElement.combineElements
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@Canonical
class SmartLiteralTransformer extends DefaultTransformer {

	@Delegate
	Compiler compiler
	TypeInferenceTransformer typeInference

	private List<IElement> extraElementsForBody
	private Set<VariableExpr> currVars
	private boolean parentIsRelation

	void enter(Rule n) {
		extraElementsForBody = []
		currVars = currDatalog.getAllVars(n).toSet()
	}

	IVisitable exit(Rule n) {
		def bodyElements = (m[n.body] ? [m[n.body] as IElement] : []) + extraElementsForBody
		new Rule(m[n.head] as IElement, combineElements(bodyElements), n.annotations)
	}

	void enter(Constructor n) { parentIsRelation = true }

	IVisitable exit(Constructor n) { handleRelation(n, n.keyExprs, new Constructor(n.name, [])) }

	void enter(Relation n) { parentIsRelation = true }

	IVisitable exit(Relation n) { handleRelation(n, n.exprs, new Relation(n.name, [])) }

	void enter(BinaryExpr n) { parentIsRelation = false }

	void enter(UnaryExpr n) { parentIsRelation = false }

	void enter(ConstantExpr n) {
		if (n.kind == ConstantExpr.Kind.SMART_LIT && !parentIsRelation)
			error(findParentLoc(), Error.SMART_LIT_NO_DIRECT_REL, n.value)
	}

	void enter(GroupExpr n) { parentIsRelation = false }

	def handleRelation(Relation n, List<IExpr> exprs, Relation clone) {
		parentIsRelation = false

		def elements = []
		def newExprs = []
		exprs.eachWithIndex { expr, int i ->
			if (expr instanceof ConstantExpr && expr.kind == ConstantExpr.Kind.SMART_LIT) {
				def inferredType = typeInference.inferredTypes[n.name][i]
				if (inferredType.primitive)
					error(findParentLoc(), Error.SMART_LIT_NON_PRIMITIVE, expr.value, inferredType.name)

				def rootType = currDatalog.typeToRootType[inferredType]

				def j = 0
				def var = var1(j)
				while (var in currVars) var = var1(j++)

				newExprs << var
				currVars << var
				elements << new Constructor(rootType.defaultConName, [new ConstantExpr(expr.value as String), var])
			} else
				newExprs << expr
		}
		clone.exprs = newExprs

		if (!elements)
			return n
		else if (inRuleHead) {
			extraElementsForBody += elements
			return clone
		} else
			return combineElements(elements << clone)
	}
}
