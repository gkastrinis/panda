package org.codesimius.panda.actions.tranform

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr
import org.codesimius.panda.datalog.expr.RecordExpr

import static org.codesimius.panda.datalog.element.ComparisonElement.TRIVIALLY_TRUE

class SyntaxFlatteningTransformer extends DefaultTransformer {

	IVisitable visit(BlockLvl0 n) {
		def rs = n.rules.collect { visit it } as Set<Rule>
		new BlockLvl0(n.relDeclarations, n.typeDeclarations, rs)
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(ConstructionElement n) { n }

	IVisitable exit(GroupElement n) { m[n.element] }

	IVisitable exit(LogicalElement n) {
		// Flatten LogicalElement "trees"
		def newElements = []
		n.elements.each {
			def flatE = m[it] as IElement
			if (flatE instanceof LogicalElement && flatE.kind == n.kind)
				newElements += flatE.elements
			else
				newElements << flatE
		}
		// Remove trivially true elements
		newElements = newElements.findAll { it != TRIVIALLY_TRUE } as List<IElement>
		if (newElements.size() > 1) new LogicalElement(n.kind, newElements)
		else if (newElements.size() == 1) newElements.first() as IElement
		else null
	}

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) { n }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }

	IVisitable visit(RecordExpr n) { n }
}
