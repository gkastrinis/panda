package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.actions.symbol.SymbolTable
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.IElement
import org.codesimius.panda.datalog.element.LogicalElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.*
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1
import static org.codesimius.panda.system.Error.error

@Canonical
class SmartLiteralTransformer extends DefaultTransformer {

	SymbolTable symbolTable
	TypeInferenceTransformer typeInference

	Set<VariableExpr> currVars
	boolean parentIsRelation

	void enter(Rule n) { currVars = (symbolTable.vars[n.head] + symbolTable.vars[n.body]).toSet() }

	IVisitable exit(Rule n) { new SyntaxFlatteningTransformer().visit(super.exit(n) as Rule) }

	void enter(Constructor n) { parentIsRelation = true }

	IVisitable exit(Constructor n) {
		parentIsRelation = false
		def (List<IElement> elements, List<IExpr> newExprs) = handleSmartLiterals(n.name, n.keyExprs)
		elements ? new LogicalElement(elements << new Constructor(n.name, newExprs << n.valueExpr)) : n
	}

	void enter(Relation n) { parentIsRelation = true }

	IVisitable exit(Relation n) {
		parentIsRelation = false
		def (List<IElement> elements, List<IExpr> newExprs) = handleSmartLiterals(n.name, n.exprs)
		elements ? new LogicalElement(elements << new Relation(n.name, newExprs)) : n
	}

	void enter(BinaryExpr n) { parentIsRelation = false }

	void enter(ConstantExpr n) {
		if (n.kind == ConstantExpr.Kind.SMART_LIT && !parentIsRelation)
			error(Error.SMART_LIT_NO_DIRECT_REL, n.value)
	}

	void enter(GroupExpr n) { parentIsRelation = false }

	def handleSmartLiterals(String name, List<IExpr> exprs) {
		def elements = []
		def newExprs = []
		exprs.eachWithIndex { expr, int i ->
			if (expr instanceof ConstantExpr && expr.kind == ConstantExpr.Kind.SMART_LIT) {
				def inferredType = typeInference.inferredTypes[name][i]
				if (inferredType.primitive)
					error(Error.SMART_LIT_NON_PRIMITIVE, expr.value, inferredType.name)

				def rootType = symbolTable.typeToRootType[inferredType]
				def var = findUnused(currVars)
				newExprs << var
				currVars << var
				elements << new Constructor(rootType.defaultConName, [new ConstantExpr(expr.value as String), var])
			} else
				newExprs << expr
		}
		[elements, newExprs]
	}

	static def findUnused(def vars) {
		int i = 0
		def v = var1(i)
		while (v in vars) v = var1(i++)
		return v
	}
}
