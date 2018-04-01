package org.clyze.deepdoop.actions.tranform

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.SymbolTableVisitingActor
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr

import static org.clyze.deepdoop.datalog.Annotation.INPUT
import static org.clyze.deepdoop.datalog.element.relation.Type.TYPE_STRING
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

@Canonical
class InputFactsTransformer extends DefaultTransformer {

	SymbolTableVisitingActor symbolTable

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
			def rootT = symbolTable.typeToRootType[t]
			if (rootT) {
				if (!(rootT in symbolTable.typesToOptimize)) {
					headElements << new ConstructionElement(new Constructor(rootT.defaultConName, [var1(i), var1(N + i)]), t)
					vars << var1(N + i)
				} else {
					headElements << new Relation(t.name, [var1(i)])
					vars << var1(i)
				}
				inputTypes << TYPE_STRING
			} else {
				vars << var1(i)
				inputTypes << t
			}
		}

		Relation inputRel
		if (headElements) {
			if (!inTypeDecl) headElements << new Relation(name, vars)
			inputRel = new Relation("__SYS_IN_$name", varN(N))
			extraRules << new Rule(headElements.size() > 1 ? new LogicalElement(headElements) : headElements.first() as IElement, inputRel)
		} else {
			inputRel = new Relation(name, varN(N))
		}
		def an = new Annotation("INPUT", [
				filename : new ConstantExpr("${name.replace ":", "_"}.facts"),
				delimeter: new ConstantExpr("\\t")])
		extraRelDecls << new RelDeclaration(inputRel, inputTypes, [an] as Set)
	}
}
