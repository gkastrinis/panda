package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.Annotation.Kind.*
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.BOOLEAN
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.REAL

class ValidationVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	InfoCollectionVisitingActor infoActor

	Set<String> declaredRelations = [] as Set
	Map<String, Integer> arities = [:]

	ValidationVisitingActor(InfoCollectionVisitingActor infoActor) {
		actor = this
		this.infoActor = infoActor
	}

	IVisitable exit(Program n, Map m) { n }

	IVisitable exit(Declaration n, Map m) {
		if (n.atom.name in declaredRelations)
			ErrorManager.error(recall(n), ErrorId.DECL_MULTIPLE, n.atom.name)
		declaredRelations << n.atom.name

		if (n.annotations[TYPE]) {
			def a = n.annotations.find { !(it.key in [TYPE, OUTPUT]) }
			if (a) ErrorManager.error(recall(a), ErrorId.ANNOTATION_INVALID, a.key, "type")
		} else {
			def a = n.annotations.find { !(it.key in [CONSTRUCTOR, FUNCTIONAL, INPUT, OUTPUT]) }
			if (a) ErrorManager.error(recall(a), ErrorId.ANNOTATION_INVALID, a.key, "declaration")

			if (n.annotations[CONSTRUCTOR] && !(n.atom instanceof Constructor))
				ErrorManager.error(recall(n), ErrorId.CONSTR_NON_FUNC, n.atom.name)
			if (n.atom instanceof Constructor && !n.annotations[CONSTRUCTOR])
				ErrorManager.error(recall(n), ErrorId.FUNC_NON_CONSTR, n.atom.name)

			arities[n.atom.name] = n.types.size()
		}
		n.annotations?.each { it.value.validate() }

		n.types.findAll { !it.isPrimitive() }
				.findAll { !(it.name in infoActor.allTypes) }
				.each { ErrorManager.error(recall(it), ErrorId.TYPE_UNKNOWN, it.name) }

		if (n.atom.name in infoActor.refmodeRelations) {
			if (n.atom.arity != 2)
				ErrorManager.error(ErrorId.REFMODE_ARITY, n.atom.name)
			if (!(n.types.first() as Type).isPrimitive())
				ErrorManager.error(ErrorId.REFMODE_KEY, n.atom.name)
		}
		null
	}

	IVisitable exit(Rule n, Map m) {
		n.annotations?.each {
			if (!(it.key in [PLAN]))
				ErrorManager.error(recall(it), ErrorId.ANNOTATION_INVALID, it.key, "rule")

			it.value.validate()
		}

		def loc = recall(n)

		def conVars = infoActor.constructedVars[n]
		def varsInHead = infoActor.vars[n.head]
		def varsInBody = infoActor.vars[n.body]
		varsInHead.findAll { !(it in varsInBody) && !(it in conVars) }
				.each { ErrorManager.error(loc, ErrorId.VAR_UNKNOWN, it.name) }

		varsInBody.findAll { it.name != "_" }
				.findAll { !(it in varsInHead) }
				.findAll { Collections.frequency(varsInBody, it) == 1 }
				.each { ErrorManager.warn(loc, ErrorId.VAR_UNUSED, it.name) }

		n.head.elements.findAll { it instanceof Relation }
				.collect { it as Relation }
				.findAll { it.name in infoActor.allTypes }
				.each { ErrorManager.error(loc, ErrorId.TYPE_RULE, it.name) }
		null
	}

	IVisitable exit(ConstructionElement n, Map m) {
		if (!(n.type.name in infoActor.allTypes))
			ErrorManager.error(recall(n), ErrorId.TYPE_UNKNOWN, n.type.name)

		def baseType = infoActor.constructorBaseType[n.constructor.name]
		if (!baseType)
			ErrorManager.error(recall(n), ErrorId.CONSTR_UNKNOWN, n.constructor.name)
		if (n.type.name != baseType && !(baseType in infoActor.superTypesOrdered[n.type.name]))
			ErrorManager.error(recall(n), ErrorId.CONSTR_INCOMP, n.constructor.name, n.type.name)
		null
	}

	IVisitable exit(Constructor n, Map m) {
		if (arities[n.name] && arities[n.name] != n.arity)
			ErrorManager.error(recall(n), ErrorId.REL_ARITY, n.name)
		null
	}

	IVisitable exit(Relation n, Map m) {
		if (arities[n.name] && arities[n.name] != n.arity)
			ErrorManager.error(recall(n), ErrorId.REL_ARITY, n.name)
		if (!arities[n.name]) arities[n.name] = n.arity
		null
	}

	IVisitable exit(ConstantExpr n, Map m) {
		if (n.type == REAL || n.type == BOOLEAN)
			ErrorManager.error(ErrorId.TYPE_UNSUPP, n.type as String)
		null
	}

	static def recall(Object o) { SourceManager.instance.recall(o) }
}
