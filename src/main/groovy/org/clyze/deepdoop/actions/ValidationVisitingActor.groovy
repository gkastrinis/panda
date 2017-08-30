package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.Annotation.Kind.*
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.BOOLEAN
import static org.clyze.deepdoop.datalog.expr.ConstantExpr.Type.REAL

class ValidationVisitingActor extends PostOrderVisitor<IVisitable> implements IActor<IVisitable>, TDummyActor<IVisitable> {

	InfoCollectionVisitingActor infoActor

	Set<String> declaredRelations = [] as Set
	Map<String, Integer> relationArities = [:]

	ValidationVisitingActor(InfoCollectionVisitingActor infoActor) {
		actor = this
		this.infoActor = infoActor
	}

	IVisitable exit(Program n, Map m) { n }

	IVisitable exit(Declaration n, Map m) {
		n.annotations?.each {
			if (TYPE in n.annotations) {
				if (!(it.key in [TYPE, OUTPUT]))
					ErrorManager.error(recall(it), ErrorId.ANNOTATION_INVALID, it.key, "type")
			} else if (!(it.key in [CONSTRUCTOR, INPUT, OUTPUT]))
				ErrorManager.error(recall(it), ErrorId.ANNOTATION_INVALID, it.key, "declaration")

			it.value.validate()
		}

		if (n.atom.name in declaredRelations)
			ErrorManager.error(recall(n), ErrorId.DECL_MULTIPLE, n.atom.name)
		declaredRelations << n.atom.name

		n.types.findAll { !(it instanceof Primitive) }
				.findAll { !(it.name in infoActor.allTypes) }
				.each { ErrorManager.error(recall(it), ErrorId.TYPE_UNKNOWN, it.name) }

		if (n.atom.name in infoActor.refmodeRelations) {
			if (n.atom.arity != 2)
				ErrorManager.error(ErrorId.REFMODE_ARITY, n.atom.name)
			if (!Primitive.isPrimitive(n.types.first().name))
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

		def varsInHead = infoActor.vars[n.head]
		def varsInBody = infoActor.vars[n.body]
		varsInBody.findAll { it.name != "_" }
				.findAll { !varsInHead.contains(it) }
				.findAll { Collections.frequency(varsInBody, it) == 1 }
				.each { ErrorManager.warn(recall(n), ErrorId.VAR_UNUSED, it.name) }

		n.head.elements.findAll { it instanceof Functional && !(it instanceof Constructor) }
				.collect { (it as Functional).name }
				.findAll { it in infoActor.allConstructors }
				.each { ErrorManager.error(recall(n), ErrorId.CONSTR_RULE, it) }

		n.head.elements.findAll { it instanceof Relation }
				.collect { it as Relation }
				.findAll { it.name in infoActor.allTypes }
				.each { ErrorManager.error(recall(n), ErrorId.TYPE_RULE, it.name) }
		null
	}

	IVisitable exit(Constructor n, Map m) {
		if (!(n.type.name in infoActor.allTypes))
			ErrorManager.error(recall(n), ErrorId.TYPE_UNKNOWN, n.type.name)

		def baseType = infoActor.constructorBaseType[n.name]
		// TODO should be more general check for predicates
		if (!baseType)
			ErrorManager.error(recall(n), ErrorId.CONSTR_UNKNOWN, n.name)
		if (n.type.name != baseType && !(baseType in infoActor.superTypesOrdered[n.type.name]))
			ErrorManager.error(recall(n), ErrorId.CONSTR_INCOMP, n.name, n.type.name)

		null
	}

	IVisitable exit(Functional n, Map m) {
		if (relationArities[n.name] && relationArities[n.name] != n.arity)
			ErrorManager.error(recall(n), ErrorId.REL_ARITY, n.name)
		relationArities[n.name] = n.arity

		if (n.name.endsWith("__pArTiAl"))
			ErrorManager.error(recall(n), ErrorId.SUFFIX_RESERVED)
		null
	}

	IVisitable exit(Predicate n, Map m) {
		if (relationArities[n.name] && relationArities[n.name] != n.arity)
			ErrorManager.error(recall(n), ErrorId.REL_ARITY, n.name)
		relationArities[n.name] = n.arity

		if (n.name.endsWith("__pArTiAl"))
			ErrorManager.error(recall(n), ErrorId.SUFFIX_RESERVED)
		null
	}

	IVisitable exit(ConstantExpr n, Map m) {
		if (n.type == REAL || n.type == BOOLEAN)
			ErrorManager.error(ErrorId.TYPE_UNSUPP, n.type as String)
		null
	}

	private static def recall(Object o) { SourceManager.instance.recall(o) }
}
