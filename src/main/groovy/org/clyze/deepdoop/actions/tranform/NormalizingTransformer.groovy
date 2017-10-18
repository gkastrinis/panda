package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.CmdComponent
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr

class NormalizingTransformer extends DummyTransformer {

	NormalizingTransformer() { actor = this }

	IVisitable exit(Program n, Map m) {
		// Flatten components that extend other components
		def newComps = n.comps.values().collectEntries {
			Component currComp = m[it] as Component
			Component flatComp
			if (currComp.superComp) {
				flatComp = currComp.clone()
				while (currComp.superComp) {
					currComp = m[n.comps[currComp.superComp]] as Component
					flatComp.add(currComp)
				}
			} else
				flatComp = currComp

			[(flatComp.name): flatComp]
		}
		new Program(m[n.globalComp] as Component, newComps, n.inits, n.props)
	}

	IVisitable exit(Component n, Map m) {
		def newComp = new Component(n.name, n.superComp)
		n.declarations.each { newComp.declarations << (m[it] as Declaration) }
		n.rules.each { newComp.rules << (m[it] as Rule) }
		return newComp
	}

	// Flatten LogicalElement "trees"
	IVisitable exit(LogicalElement n, Map m) {
		def newElements = []
		n.elements.each {
			def flatE = m[it] as IElement
			if (flatE instanceof LogicalElement && flatE.type == n.type)
				newElements += (flatE as LogicalElement).elements
			else
				newElements << flatE
		}
		new LogicalElement(n.type, newElements)
	}

	// Overrides to avoid unneeded allocations

	IVisitable exit(CmdComponent n, Map m) { n }

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(ConstructionElement n, Map m) { n }

	IVisitable exit(Constructor n, Map m) { n }

	IVisitable exit(Relation n, Map m) { n }

	IVisitable exit(Type n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
