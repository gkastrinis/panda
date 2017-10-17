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

class NormalizingTransformer extends DummyTransformer {

	NormalizingTransformer() { actor = this }

	Program exit(Program n, Map<IVisitable, IVisitable> m) {
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
		return new Program(m[n.globalComp] as Component, newComps, n.inits, n.props)
	}

	Component exit(Component n, Map<IVisitable, IVisitable> m) {
		def newComp = new Component(n.name, n.superComp)
		n.declarations.each { newComp.declarations << (m[it] as Declaration) }
		n.rules.each { newComp.rules << (m[it] as Rule) }
		return newComp
	}

	Rule exit(Rule n, Map<IVisitable, IVisitable> m) {
		new Rule(m[n.head] as LogicalElement, m[n.body] as LogicalElement, n.annotations)
	}

	AggregationElement exit(AggregationElement n, Map<IVisitable, IVisitable> m) {
		new AggregationElement(n.var, n.relation, m[n.body] as LogicalElement)
	}

	GroupElement exit(GroupElement n, Map<IVisitable, IVisitable> m) {
		new GroupElement(m[n.element] as IElement)
	}

	// Flatten LogicalElement "trees"
	LogicalElement exit(LogicalElement n, Map<IVisitable, IVisitable> m) {
		def newElements = []
		n.elements.each {
			def flatE = m[it] as IElement
			if (flatE instanceof LogicalElement && flatE.type == n.type)
				newElements += (flatE as LogicalElement).elements
			else
				newElements << flatE
		}
		return new LogicalElement(n.type, newElements)
	}

	NegationElement exit(NegationElement n, Map<IVisitable, IVisitable> m) {
		new NegationElement(m[n.element] as IElement)
	}

	CmdComponent exit(CmdComponent n, Map<IVisitable, IVisitable> m) { n }

	ComparisonElement exit(ComparisonElement n, Map<IVisitable, IVisitable> m) { n }

	Constructor exit(Constructor n, Map<IVisitable, IVisitable> m) { n }

	Relation exit(Relation n, Map<IVisitable, IVisitable> m) { n }
}
