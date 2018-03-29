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
class InputFactsTransformer extends DummyTransformer {

	SymbolTableVisitingActor symbolTable

	IVisitable exit(RelDeclaration n, Map m) {
		if (INPUT in n.annotations) {
			genInput(n.relation.name, n.types, false)
			return null
		}
		return n
	}

	IVisitable exit(TypeDeclaration n, Map m) {
		if (INPUT in n.annotations)
			genInput(n.type.name, [n.type], true)
		return n
	}

	def genInput(String name, List<Type> types, boolean isType) {
		def N = types.size()
		def elements = []
		def vars = []
		def inputTypes = []

		types.withIndex().each { Type t, int i ->
			def rootT = symbolTable.typeToRootType[t]
			if (rootT) {
				elements << new ConstructionElement(new Constructor(rootT.defaultConName, [var1(i), var1(N + i)]), t)
				vars << var1(N + i)
				inputTypes << TYPE_STRING
			} else {
				vars << var1(i)
				inputTypes << t
			}
		}

		Relation inputRel
		if (elements) {
			if (!isType) elements << new Relation(name, vars)
			inputRel = new Relation("__SYS_IN_$name", varN(N))
			extraRules << new Rule(elements.size() > 1 ? new LogicalElement(elements) : elements.first() as IElement, inputRel)
		} else {
			inputRel = new Relation(name, varN(N))
		}
		def an = new Annotation("INPUT", [
				"filename" : new ConstantExpr("${name.replace ":", "_"}.facts"),
				"delimeter": new ConstantExpr("\\t")])
		extraRelDecls << new RelDeclaration(inputRel, inputTypes, [an] as Set)
	}

	// Overrides to avoid unneeded allocations

	IVisitable exit(LogicalElement n, Map m) { n }

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(ConstructionElement n, Map m) { n }

	IVisitable exit(Constructor n, Map m) { n }

	IVisitable exit(Relation n, Map m) { n }

	IVisitable exit(Type n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
