package org.codesimius.panda.actions.tranform

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr

import static org.codesimius.panda.datalog.element.LogicalElement.combineElements

class SyntaxFlatteningTransformer extends DefaultTransformer {

	IVisitable visit(BlockLvl0 n) {
		def rs = n.rules.collect { visit it } as Set<Rule>
		new BlockLvl0(n.relDeclarations, n.typeDeclarations, rs)
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(ConstructionElement n) { n }

	// Remove group elements and replace them with their contents
	IVisitable exit(GroupElement n) { m[n.element] }

	IVisitable exit(LogicalElement n) { combineElements(n.kind, n.elements.collect { m[it] as IElement }) }

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) { n }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }
}
