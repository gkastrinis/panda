package org.codesimius.panda.actions.validation

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.actions.symbol.ConstructionInfoVisitor
import org.codesimius.panda.actions.symbol.SymbolTable
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
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
class MainValidator extends DefaultVisitor<IVisitable> {

	SymbolTable symbolTable

	private Set<String> tmpDeclaredRelations = [] as Set
	private Set<String> tmpDeclaredTypes = [] as Set
	private Map<String, Integer> arities = [:]
	private BlockLvl0 datalog
	private Set<String> allConstructors

	IVisitable exit(BlockLvl2 n) { n }

	void enter(BlockLvl0 n) {
		datalog = n
		allConstructors = symbolTable.constructorBaseType.keySet()
	}

	void enter(RelDeclaration n) {
		if (n.relation.name in tmpDeclaredRelations)
			error(recall(n), Error.DECL_MULTIPLE, n.relation.name)
		tmpDeclaredRelations << n.relation.name

		checkAnnotations(n.annotations, [CONSTRUCTOR, FUNCTIONAL, INPUT, OUTPUT], "Declarations")
		checkArity(n.relation.name, n.types.size(), n)

		n.types.findAll { !it.isPrimitive() }
				.findAll { !(it in datalog.allTypes) }
				.each { error(recall(it), Error.TYPE_UNKNOWN, it.name) }

		if (CONSTRUCTOR in n.annotations) {
			def rootT = datalog.typeToRootType[n.types.last()]
			def optimized = datalog.typeDeclarations
					.find { it.type == rootT }
					.annotations.find { it == TYPE }.args["opt"]
			if (optimized && rootT.defaultConName != n.relation.name)
				error(recall(n), Error.TYPE_OPT_CONSTR, n.relation.name)
		}
	}

	void enter(TypeDeclaration n) {
		checkAnnotations(n.annotations, [INPUT, OUTPUT, TYPE, TYPEVALUES], "Type")

		if (n.type.name in tmpDeclaredTypes)
			error(recall(n), Error.DECL_MULTIPLE, n.type.name)
		tmpDeclaredTypes << n.type.name

		if (n.annotations.find { it == TYPE }.args["opt"]) {
			def rootT = datalog.typeToRootType[n.type]
			if (!datalog.typeDeclarations.find { it.type == rootT }.annotations.find { it == TYPE }.args["opt"])
				error(recall(n), Error.TYPE_OPT_ROOT_NONOPT, n.type.name)
		}
	}

	void enter(Rule n) {
		checkAnnotations(n.annotations, [PLAN], "Rule")

		def varsInHead = symbolTable.vars[n.head]
		def varsInBody = symbolTable.vars[n.body]
		def conVars = symbolTable.constructedVars[n]
		varsInHead.findAll { it.name == "_" }
				.each { error(recall(n), Error.VAR_UNBOUND_HEAD, null) }
		varsInHead.findAll { !(it in varsInBody) && !(it in conVars) }
				.each { error(recall(n), Error.VAR_UNKNOWN, it.name) }
		varsInBody.findAll { it in conVars }
				.each { error(recall(n), Error.VAR_CONSTR_BODY, it.name) }
		varsInBody.findAll { it.name != "_" && !(it in varsInHead) && (varsInBody.count(it) == 1) }
				.each { warn(recall(n), Error.VAR_UNUSED, it.name) }

		// Visit the rule for error checking reasons
		new ConstructionInfoVisitor().visit n
	}

	void enter(ConstructionElement n) {
		def baseType = symbolTable.constructorBaseType[n.constructor.name]
		if (!baseType)
			error(recall(n), Error.CONSTR_UNKNOWN, n.constructor.name)
		if (n.type != baseType && !(baseType in datalog.superTypesOrdered[n.type]))
			error(recall(n), Error.CONSTR_TYPE_INCOMPAT, n.constructor.name, n.type.name)

		if (!(n.type in datalog.allTypes)) error(recall(n), Error.TYPE_UNKNOWN, n.type.name)
	}

	void enter(Constructor n) { if (!inDecl) checkRelation(n) }

	void enter(Relation n) {
		if (!inDecl) checkRelation(n)

		if (n.name in allConstructors)
			error(recall(n), Error.CONSTR_AS_REL, n.name)
	}

	def checkRelation(Relation n) {
		// Type is used in rule head
		if (inRuleHead && (new Type(n.name) in datalog.allTypes))
			error(recall(n), Error.TYPE_RULE, n.name)

		if (inRuleBody && !(n.name in symbolTable.declaredRelations))
			error(recall(n), Error.REL_NO_DECL, n.name)

		checkArity(n.name, n.arity, n)
	}

	def checkArity(String name, int arity, IVisitable n) {
		if (inRuleBody && datalog.allTypes.find { it.name == name } && arity != 1)
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
