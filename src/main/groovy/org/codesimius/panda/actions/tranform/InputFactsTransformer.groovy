package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.actions.symbol.TypeInfoVisitor
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ComparisonElement
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.IElement
import org.codesimius.panda.datalog.element.LogicalElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.datalog.expr.GroupExpr

import static org.codesimius.panda.datalog.Annotation.INPUT
import static org.codesimius.panda.datalog.element.relation.Type.TYPE_STRING
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1
import static org.codesimius.panda.datalog.expr.VariableExpr.genN as varN

@Canonical
class InputFactsTransformer extends DefaultTransformer {

	TypeInfoVisitor typeInfo

	IVisitable exit(RelDeclaration n) {
		if (INPUT in n.annotations) {
			genInput(n.relation.name, n.types, false)
			return null
		}
		return n
	}

	IVisitable exit(TypeDeclaration n) {
		if (INPUT in n.annotations)
			genInput(n.type.name, [n.type], true)
		return n
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(ConstructionElement n) { n }

	IVisitable exit(LogicalElement n) { n }

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) { n }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }

	def genInput(String name, List<Type> types, boolean inTypeDecl) {
		def N = types.size()
		def headElements = []
		def vars = []
		def inputTypes = []

		types.withIndex().each { Type t, int i ->
			if (t.isPrimitive()) {
				vars << var1(i)
				inputTypes << t
			} else {
				def rootT = typeInfo.typeToRootType[t]
				headElements << new ConstructionElement(new Constructor(rootT.defaultConName, [var1(i), var1(N + i)]), t)
				vars << var1(N + i)
				inputTypes << TYPE_STRING
			}
		}

		def an = new Annotation("INPUT", [
				filename : new ConstantExpr("${name.replace ":", "_"}.facts"),
				delimeter: new ConstantExpr("\\t")])

		if (headElements) {
			if (!inTypeDecl) headElements << new Relation(name, vars)
			def inputRel = new Relation("__SYS_IN_$name", varN(N))
			extraRules << new Rule(headElements.size() > 1 ? new LogicalElement(headElements) : headElements.first() as IElement, inputRel)
			extraRelDecls << new RelDeclaration(inputRel, inputTypes, [an] as Set)
		} else
			extraRelDecls << new RelDeclaration(new Relation(name, varN(N)), inputTypes, [an] as Set)
	}
}
