package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.CmdComponent
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr

class FlatteningTransformer extends DummyTransformer {

	List<String> actualParameters = []
	List<Integer> parameterIndexes = []
	List<String> formalParameters = []

	FlatteningTransformer() { actor = this }

	// Merge a component with all its super components
	IVisitable exit(Program n, Map m) {
		def newComps = n.comps.values().collectEntries {
			def currComp = m[it] as Component
			if (currComp.superComp) {
				def flatComp = new Component(
						currComp.name,
						null,
						currComp.parameters, [],
						currComp.declarations,
						currComp.rules + [])
				actualParameters = currComp.parameters
				parameterIndexes = (0..<actualParameters.size())
				while (currComp.superComp) {
					// A list of indexes of super parameters in the original parameter list
					// e.g. in `component A <X,Y,Z> : B <Z, X>`, we get [2, 0]
					parameterIndexes = currComp.superParameters.collect { superP ->
						parameterIndexes[currComp.parameters.findIndexOf { it == superP }]
					}
					currComp = m[n.comps[currComp.superComp]] as Component
					formalParameters = currComp.parameters
					currComp.accept(this)
					flatComp.declarations += currComp.declarations
					flatComp.rules += currComp.rules.collect { m[it] as Rule }
				}
				[(flatComp.name): flatComp]
			} else
				[(currComp.name): currComp]
		}
		new Program(m[n.globalComp] as Component, newComps, n.inits)
	}

	IVisitable exit(Relation n, Map m) {
		if (actualParameters && n.name.contains("@")) {
			def (name, parameter) = n.name.split("@")
			def index = formalParameters.findIndexOf { it == parameter }
			def newParameter = actualParameters[parameterIndexes[index]]
			return new Relation("$name@$newParameter", n.exprs)
		} else
			return n
	}

	// Flatten LogicalElement "trees"
	IVisitable exit(LogicalElement n, Map m) {
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

	// Overrides to avoid unneeded allocations

	IVisitable exit(CmdComponent n, Map m) { n }

	IVisitable exit(Declaration n, Map m) { n }

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(ConstructionElement n, Map m) { n }

	IVisitable exit(Constructor n, Map m) { n }

	IVisitable exit(Type n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
