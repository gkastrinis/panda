package org.codesimius.panda.actions.tranform

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr

class SyntaxFlatteningTransformer extends DefaultTransformer {

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
			if (flatE instanceof LogicalElement && flatE.kind == n.kind)
				newElements += flatE.elements
			else
				newElements << flatE
		}
		new LogicalElement(n.kind, newElements)
	}

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) { n }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }
}
