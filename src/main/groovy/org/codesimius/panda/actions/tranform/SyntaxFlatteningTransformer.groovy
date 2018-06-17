package org.codesimius.panda.actions.tranform

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr

import static org.codesimius.panda.datalog.element.ComparisonElement.TRIVIALLY_TRUE

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
}
