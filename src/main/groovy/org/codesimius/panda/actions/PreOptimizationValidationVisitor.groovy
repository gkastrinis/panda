package org.codesimius.panda.actions

import groovy.transform.Canonical
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

@Canonical
class PreOptimizationValidationVisitor extends DefaultVisitor<IVisitable> {

	TypeInfoVisitor typeInfo
	private Set<String> tmpDeclaredRelations = [] as Set

	IVisitable exit(BlockLvl2 n) { n }

	void enter(RelDeclaration n) {
		if (n.relation.name in tmpDeclaredRelations)
			error(recall(n), Error.DECL_MULTIPLE, n.relation.name)
		tmpDeclaredRelations << n.relation.name

		checkAnnotations(n.annotations, [CONSTRUCTOR, FUNCTIONAL, INPUT, OUTPUT], "Declarations")
	}

	void enter(TypeDeclaration n) {
		checkAnnotations(n.annotations, [INPUT, OUTPUT, TYPE, TYPEVALUES], "Type")
	}

	void enter(Rule n) {
		checkAnnotations(n.annotations, [PLAN], "Rule")
	}

	void enter(ConstructionElement n) {
		def baseType = typeInfo.constructorBaseType[n.constructor.name]
		if (!baseType)
			error(recall(n), Error.CONSTR_UNKNOWN, n.constructor.name)
		if (n.type != baseType && !(baseType in typeInfo.superTypesOrdered[n.type]))
			error(recall(n), Error.CONSTR_TYPE_INCOMP, n.constructor.name, n.type.name)
	}

	IVisitable exit(Relation n) {
		// Type is used in rule head
		if (inRuleHead && (new Type(n.name) in typeInfo.allTypes))
			error(recall(n), Error.TYPE_RULE, n.name)
		return n
	}

	static def checkAnnotations(Set<Annotation> annotations, List<Annotation> allowedAnnotations, String kind) {
		annotations
				.findAll { !(it in allowedAnnotations) }
				.each { error(recall(it), Error.ANNOTATION_INVALID, it, kind) }
		annotations.each { it.validate() }
	}
}
