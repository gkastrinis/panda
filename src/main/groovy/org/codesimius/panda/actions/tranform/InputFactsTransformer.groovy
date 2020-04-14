package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.datalog.AnnotationSet
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.ConstantExpr

import static org.codesimius.panda.datalog.Annotation.INPUT
import static org.codesimius.panda.datalog.Annotation.TYPE
import static org.codesimius.panda.datalog.element.LogicalElement.combineElements
import static org.codesimius.panda.datalog.element.relation.Type.TYPE_STRING
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1
import static org.codesimius.panda.datalog.expr.VariableExpr.genN as varN

@Canonical
class InputFactsTransformer extends DefaultTransformer {

	BlockLvl0 datalog

	IVisitable visit(BlockLvl0 n) {
		datalog = n
		(n.relDeclarations + n.typeDeclarations).each { m[it] = visit it }
		def newRelDecls = (n.relDeclarations.collect { m[it] as RelDeclaration } + extraRelDecls).grep() as Set
		new BlockLvl0(newRelDecls, n.typeDeclarations + extraTypeDecls, n.rules + extraRules)
	}

	IVisitable exit(RelDeclaration n) {
		if (INPUT in n.annotations) genInput(n.relation.name, n.types, n.annotations)
		return n
	}

	IVisitable exit(TypeDeclaration n) {
		if (INPUT in n.annotations) genInput(n.type.name, [n.type], n.annotations)
		return n
	}

	def genInput(String name, List<Type> types, AnnotationSet annotations) {
		def N = types.size()
		def headElements = []
		def vars = []
		def inputTypes = []

		types.eachWithIndex { Type t, int i ->
			if (t.primitive) {
				vars << var1(i)
				inputTypes << t
			} else {
				def rootT = datalog.typeToRootType[t]
				headElements << new ConstructionElement(new Constructor(rootT.defaultConName, [var1(i), var1(N + i)]), t)
				vars << var1(N + i)
				inputTypes << TYPE_STRING
			}
		}

		def origAn = annotations[INPUT]
		def newAn = INPUT.template([
				filename : origAn["filename"] ?: new ConstantExpr("${name.replace ":", "_"}.facts"),
				delimeter: origAn["delimeter"] ?: new ConstantExpr("\\t")])
		annotations -= INPUT

		if (headElements) {
			if (!annotations[TYPE]) headElements << new Relation(name, vars)
			def inputRel = new Relation("__SYS_IN_$name", varN(N))
			extraRules << new Rule(combineElements(headElements), inputRel)
			extraRelDecls << new RelDeclaration(inputRel, inputTypes, new AnnotationSet(newAn))
		} else if (!types.any { !it.isPrimitive() })
			annotations << newAn
		else
			extraRelDecls << new RelDeclaration(new Relation(name, varN(N)), inputTypes, new AnnotationSet(newAn))
	}
}
