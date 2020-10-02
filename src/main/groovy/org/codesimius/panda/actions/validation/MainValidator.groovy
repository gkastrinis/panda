package org.codesimius.panda.actions.validation

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.actions.symbol.ConstructionInfoVisitor
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.*

@Canonical
class MainValidator extends DefaultVisitor<IVisitable> {

	@Delegate
	Compiler compiler
	private Set<String> tmpDeclaredRelations = [] as Set
	private Set<String> tmpDeclaredTypes = [] as Set
	private Map<String, Integer> arities = [:]

	IVisitable exit(BlockLvl2 n) { n }

	void enter(RelDeclaration n) {
		def alternativeName = n.relation.name.replace ":", "_"
		if (n.relation.name in tmpDeclaredRelations || alternativeName in tmpDeclaredRelations ||
				n.relation.name in tmpDeclaredTypes || alternativeName in tmpDeclaredTypes)
			error(loc(n), Error.DECL_MULTIPLE, n.relation.name)

		tmpDeclaredRelations << n.relation.name

		checkAnnotations(n.annotations, [CONSTRUCTOR, FUNCTIONAL, INPUT, OUTPUT, METADATA], "Declarations")

		if (!n.types) return
		checkArity(n.relation.name, n.types.size())

		n.types.findAll { !it.primitive }
				.findAll { it !in currDatalog.allTypes }
				.each { error(loc(n), Error.TYPE_UNKNOWN, it.name) }

		if (CONSTRUCTOR in n.annotations) {
			def rootT = currDatalog.typeToRootType[n.types.last()]
			def optimized = currDatalog.typeDeclarations.find { it.type == rootT }.annotations[TYPE]["opt"]
			if (optimized && rootT.defaultConName != n.relation.name)
				error(loc(n), Error.TYPE_OPT_CONSTR, n.relation.name)
		}
	}

	void enter(TypeDeclaration n) {
		def alternativeName = n.type.name.replace ":", "_"
		if (n.type.name in tmpDeclaredRelations || alternativeName in tmpDeclaredRelations ||
				n.type.name in tmpDeclaredTypes || alternativeName in tmpDeclaredTypes)
			error(loc(n), Error.DECL_MULTIPLE, n.type.name)

		tmpDeclaredTypes << n.type.name

		checkAnnotations(n.annotations, [INPUT, OUTPUT, TYPE, TYPEVALUES], "Type")

		if (n.annotations[TYPE]["opt"]) {
			def rootT = currDatalog.typeToRootType[n.type]
			if (!currDatalog.typeDeclarations.find { it.type == rootT }.annotations[TYPE]["opt"])
				error(loc(n), Error.TYPE_OPT_ROOT_NONOPT, n.type.name)
		}
	}

	void enter(Rule n) {
		checkAnnotations(n.annotations, [PLAN], "Rule")

		def varsInHead = currDatalog.getHeadVars(n)
		def varsInBody = currDatalog.getBodyVars(n)
		def conVars = currDatalog.getConstructedVars(n)
		varsInHead.findAll { it.name == "_" }
				.each { error(loc(n), Error.VAR_DONTCARE_HEAD, null) }
		varsInHead.findAll { (it !in varsInBody) && (it !in conVars) }
				.each { error(loc(n), Error.VAR_UNBOUND, it.name) }
		varsInBody.findAll { it in conVars }
				.each { error(loc(n), Error.VAR_CONSTR_BODY, it.name) }
		varsInBody.findAll { it.name != "_" && (it !in varsInHead) && (varsInBody.count(it) == 1) }
				.each { warn(loc(n), Error.VAR_UNUSED, it.name) }

		// Visit the rule for error checking reasons
		new ConstructionInfoVisitor(compiler).visit n
	}

	void enter(AggregationElement n) {
		arities[n.relation.name] = AggregationElement.PREDICATE_TYPES[n.relation.name].size()
	}

	void enter(ConstructionElement n) {
		def baseType = currDatalog.constructorToBaseType[n.constructor.name]
		if (!baseType)
			error(findParentLoc(), Error.CONSTR_UNKNOWN, n.constructor.name)
		if (n.type != baseType && (baseType !in currDatalog.superTypesOrdered[n.type]))
			error(findParentLoc(), Error.CONSTR_TYPE_INCOMPAT, n.constructor.name, n.type.name)

		if (n.type !in currDatalog.allTypes) error(findParentLoc(), Error.TYPE_UNKNOWN, n.type.name)
	}

	void enter(Constructor n) { if (!inDecl) checkRelation(n) }

	void enter(Relation n) {
		if (!inDecl) checkRelation(n)

		if (n.name in currDatalog.allConstructors)
			error(findParentLoc(), Error.CONSTR_AS_REL, n.name)
	}

	def checkRelation(Relation n) {
		// Type is used in rule head
		if (inRuleHead && (new Type(n.name) in currDatalog.allTypes))
			error(findParentLoc(), Error.TYPE_RULE, n.name)

		if (inRuleBody && (n.name !in currDatalog.declaredRelations))
			error(findParentLoc(), Error.REL_NO_DECL, n.name)

		checkArity(n.name, n.arity)
	}

	def checkArity(String name, int arity) {
		if (currDatalog.allTypes.find { it.name == name } && arity != 1)
			error(findParentLoc(), Error.REL_ARITY, name)

		def prevArity = arities[name]
		// Explicit check with null cause 0 is a valid value
		if (prevArity != null && prevArity != arity)
			error(findParentLoc(), Error.REL_ARITY, name)
		if (!prevArity) arities[name] = arity
	}

	def checkAnnotations(Set<Annotation> annotations, List<Annotation> allowedAnnotations, String kind) {
		annotations
				.findAll { (it !in allowedAnnotations) && !it.isInternal }
				.each { error(loc(it), Error.ANNOTATION_INVALID, it, kind) }
		annotations.each { it.validate(compiler) }
	}
}
