package org.codesimius.panda.actions.validation

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.BOOLEAN
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.REAL

@Canonical
class PreliminaryValidator extends DefaultVisitor<IVisitable> {

	@Delegate
	Compiler compiler
	private BlockLvl1 currTemplate
	private List<String> ids

	void enter(BlockLvl2 n) {
		program = n
		ids = n.templates*.name + n.instantiations*.id
		ids.findAll { ids.count(it) > 1 }.each { id ->
			def candidates = (n.templates.findAll { it.name == id } + n.instantiations.findAll { it.id == id })
			error(candidates.collect { loc(it) }.first(), Error.ID_IN_USE, id)
		}
		n.templates = n.templates.toSet()
		n.instantiations = n.instantiations.toSet()

		n.instantiations.each { inst ->
			def loc = loc(inst)
			def currTemplate = n.templates.find { it.name == inst.template }
			if (!currTemplate)
				error(loc, Error.TEMPL_UNKNOWN, inst.template)
			if (currTemplate.parameters.size() != inst.parameters.size())
				error(loc, Error.TEMPL_INST_ARITY, inst.parameters, inst.template, inst.id)
			inst.parameters.findAll { param -> param != "_" && !n.instantiations.any { it.id == param } }.each {
				error(loc, Error.TEMPL_UNKNOWN_PARAM, it)
			}
		}
	}

	IVisitable exit(BlockLvl2 n) { n }

	void enter(BlockLvl1 n) {
		def loc = loc(n)
		if (n.parameters.size() != n.parameters.toSet().size())
			error(loc, Error.TEMPL_DUPLICATE_PARAMS, n.parameters, n.name)
		if (n.superParameters.any { it !in n.parameters })
			error(loc, Error.TEMPL_SUPER_PARAM_MISMATCH, n.superParameters, n.parameters, n.superTemplate)

		currTemplate = n
	}

	IVisitable exit(BlockLvl1 n) { currTemplate = null }

	void enter(BlockLvl0 n) {
		n.typeDeclarations.findAll { it.type.name.contains(".") }.each { error(Error.TYPE_QUAL_DECL, it.type.name) }

		n.relDeclarations.findAll {
			// Declarations in global scope that only have an @output annotation are allowed
			if (!currTemplate && (OUTPUT in it.annotations) && it.annotations.every { it == OUTPUT || it == METADATA }) return false
			// Otherwise, in general, declarations of qualified relations are not allowed
			return it.relation.name.contains(".")
		}.each { error(Error.REL_QUAL_DECL, it.relation.name) }
	}

	void enter(RelDeclaration n) {
		if (Type.isPrimitive(n.relation.name))
			error(loc(n), Error.PRIMITIVE_DECL_ASREL, n.relation.name)
		if (n.relation.exprs.size() != n.types.size())
			error(loc(n), Error.DECL_MALFORMED, null)
		n.relation.exprs.findAll { n.relation.exprs.count(it) > 1 }.each {
			error(loc(n), Error.DECL_SAME_VAR, it)
		}
		if (CONSTRUCTOR in n.annotations && n.relation !instanceof Constructor)
			error(loc(n), Error.CONSTR_NON_FUNC, n.relation.name)
		if (n.relation instanceof Constructor && CONSTRUCTOR !in n.annotations)
			error(loc(n), Error.FUNC_NON_CONSTR, n.relation.name)
	}

	void enter(TypeDeclaration n) {
		if (n.type.primitive)
			error(loc(n), Error.PRIMITIVE_DECL, n.type.name)
		if (n.supertype?.primitive)
			error(loc(n), Error.PRIMITIVE_AS_SUPER, n.supertype.name, n.type.name)
	}

	void enter(Rule n) {
		def conVars = currDatalog.getConstructedVars(n)
		def duplicateVar = conVars.find { conVars.count(it) > 1 }
		if (duplicateVar) error(loc(n), Error.VAR_MULTIPLE_CONSTR, duplicateVar)
	}

	void enter(AggregationElement n) {
		if (n.relation.name !in AggregationElement.SUPPORTED_PREDICATES)
			error(findParentLoc(), Error.AGGR_UNSUPPORTED_REL, n.relation.name)
	}

	void enter(Relation n) {
		if (n.name.contains(".")) {
			def parameter = n.name.split("\\.").first() as String
			if (inRuleHead)
				error(findParentLoc(), Error.REL_QUAL_HEAD, n.name)
			if (!currTemplate && !program.instantiations.any { it.id == parameter })
				error(findParentLoc(), Error.INST_UNKNOWN, parameter)
			if (currTemplate && !currTemplate.parameters.any { it == parameter })
				error(findParentLoc(), Error.TEMPL_UNKNOWN_PARAM, parameter)
		}
	}

	void enter(ConstantExpr n) {
		if (n.kind == REAL || n.kind == BOOLEAN)
			error(Error.TYPE_UNSUPP, n.kind as String)
	}
}
