package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr

class SyntaxFlatteningTransformer extends DefaultTransformer {

	private List<String> actualParameters = []
	private List<Integer> parameterIndexes = []
	private List<String> formalParameters = []

	// Merge a component with all its super components
	IVisitable exit(BlockLvl2 n) {
		def newComponents = n.components.collect {
			def currComp = m[it] as BlockLvl1
			if (currComp.superComponent) {
				def flatComp = new BlockLvl1(currComp.name, null, currComp.parameters, [], currComp.datalog)

				actualParameters = currComp.parameters
				parameterIndexes = (0..<actualParameters.size())
				while (currComp.superComponent) {
					// A list of indexes of super parameters in the original parameter list
					// e.g. in `component A <X,Y,Z> : B <Z, X>`, we get [2, 0]
					parameterIndexes = currComp.superParameters.collect { superP ->
						parameterIndexes[currComp.parameters.findIndexOf { it == superP }]
					}
					def superComp = n.components.find { it.name == currComp.superComponent }
					currComp = m[superComp] as BlockLvl1
					formalParameters = currComp.parameters
					visit currComp
					flatComp.datalog.relDeclarations += currComp.datalog.relDeclarations
					flatComp.datalog.typeDeclarations += currComp.datalog.typeDeclarations
					flatComp.datalog.rules += currComp.datalog.rules.collect { m[it] as Rule }
				}
				return flatComp
			} else
				return currComp
		} as Set
		new BlockLvl2(m[n.datalog] as BlockLvl0, newComponents, n.instantiations)
	}

	IVisitable exit(RelDeclaration n) { n }

	IVisitable exit(TypeDeclaration n) { n }

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(ConstructionElement n) { n }

	IVisitable exit(GroupElement n) { m[n.element] }
	// Flatten LogicalElement "trees"
	IVisitable exit(LogicalElement n) {
		def newElements = []
		n.elements.each {
			def flatE = m[it] as IElement
			if (flatE instanceof LogicalElement && flatE.type == n.type)
				newElements += flatE.elements
			else
				newElements << flatE
		}
		new LogicalElement(n.type, newElements)
	}

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) {
		if (actualParameters && n.name.contains("@")) {
			def (name, parameter) = n.name.split("@")
			def index = formalParameters.findIndexOf { it == parameter }
			def newParameter = actualParameters[parameterIndexes[index]]
			return new Relation("$name@$newParameter", n.exprs)
		} else
			return n
	}

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }
}
