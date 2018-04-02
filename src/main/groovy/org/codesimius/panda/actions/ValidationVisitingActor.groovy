package org.codesimius.panda.actions

import groovy.transform.Canonical
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.Error.warn
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

@Canonical
class ValidationVisitingActor extends DefaultVisitor<IVisitable> {

	RelationInfoVisitingActor relationInfo
	VarInfoVisitingActor varInfo

	Set<String> tmpDeclaredRelations = [] as Set
	Set<String> tmpDeclaredTypes = [] as Set
	Map<String, Integer> arities = [:]

	IVisitable exit(BlockLvl2 n) { n }

	void enter(RelDeclaration n) {
		if (n.relation.name in tmpDeclaredRelations) error(recall(n), Error.DECL_MULTIPLE, n.relation.name)
		tmpDeclaredRelations << n.relation.name

		checkAnnotations(n.annotations, [CONSTRUCTOR, FUNCTIONAL, INPUT, OUTPUT], "Declarations")
		checkArity(n.relation.name, n.types.size(), n)

		if (CONSTRUCTOR in n.annotations && !(n.relation instanceof Constructor))
			error(recall(n), Error.CONSTR_NON_FUNC, n.relation.name)
		if (n.relation instanceof Constructor && !(CONSTRUCTOR in n.annotations))
			error(recall(n), Error.FUNC_NON_CONSTR, n.relation.name)

		n.types.findAll { !it.isPrimitive() }
				.findAll { !(it in relationInfo.allTypes) }
				.each { error(recall(it), Error.TYPE_UNKNOWN, it.name) }
	}

	void enter(TypeDeclaration n) {
		if (n.type.name in tmpDeclaredTypes) error(recall(n), Error.DECL_MULTIPLE, n.type.name)
		tmpDeclaredTypes << n.type.name

		checkAnnotations(n.annotations, [INPUT, OUTPUT, TYPE, TYPEVALUES], "Type")
	}

	void enter(Rule n) {
		checkAnnotations(n.annotations, [PLAN], "Rule")

		def varsInHead = varInfo.vars[n.head]
		def varsInBody = varInfo.vars[n.body]
		def conVars = relationInfo.constructedVars[n]
		varsInHead.findAll { !(it in varsInBody) && !(it in conVars) }
				.each { error(recall(n), Error.VAR_UNKNOWN, it.name) }

		varsInBody.findAll { it.name != "_" }
				.findAll { !(it in varsInHead) }
				.findAll { varsInBody.count(it) == 1 }
				.each { warn(recall(n), Error.VAR_UNUSED, it.name) }
	}

	void enter(ConstructionElement n) {
		if (!(n.type in relationInfo.allTypes)) error(recall(n), Error.TYPE_UNKNOWN, n.type.name)

		def baseType = relationInfo.constructorBaseType[n.constructor.name]
		if (!baseType)
			error(recall(n), Error.CONSTR_UNKNOWN, n.constructor.name)
		if (n.type != baseType && !(baseType in relationInfo.superTypesOrdered[n.type]))
			error(recall(n), Error.CONSTR_TYPE_INCOMP, n.constructor.name, n.type.name)
	}

	void enter(Constructor n) { if (!inDecl) checkRelation(n) }

	void enter(Relation n) { if (!inDecl) checkRelation(n) }

	def checkRelation(Relation n) {
		// Type is used in rule head (and not marked for optimization)
		def t = new Type(n.name)
		if (inRuleHead && (t in relationInfo.allTypes) && !(t in relationInfo.typesToOptimize))
			error(recall(n), Error.TYPE_RULE, n.name)

		if (inRuleBody && !(n.name in relationInfo.declaredRelations))
			error(recall(n), Error.REL_NO_DECL, n.name)

		checkArity(n.name, n.arity, n)
	}

	def checkArity(String name, int arity, IVisitable n) {
		if (inRuleBody && relationInfo.allTypes.find { it.name == name } && arity != 1)
			error(recall(n), Error.REL_ARITY, name)

		def prevArity = arities[name]
		if (prevArity && prevArity != arity) error(recall(n), Error.REL_ARITY, name)
		if (!prevArity) arities[name] = arity
	}

	static def checkAnnotations(Set<Annotation> annotations, List<Annotation> allowedAnnotations, String kind) {
		annotations
				.findAll { !(it in allowedAnnotations) }
				.each { error(recall(it), Error.ANNOTATION_INVALID, it, kind) }
		annotations.each { it.validate() }
	}
}
