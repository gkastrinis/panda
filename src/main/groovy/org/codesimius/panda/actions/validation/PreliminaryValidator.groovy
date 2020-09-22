package org.codesimius.panda.actions.validation

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.CONSTRUCTOR
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.BOOLEAN
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.REAL

@Canonical
class PreliminaryValidator extends DefaultVisitor<IVisitable> {

	@Delegate
	Compiler compiler
	private BlockLvl2 prog
	private BlockLvl1 currComp
	private BlockLvl0 datalog
	private List<String> ids

	void enter(BlockLvl2 n) {
		ids = n.templates*.name + n.instantiations*.id
		ids.findAll { ids.count(it) > 1 }.each { id ->
			def candidates = (n.templates.findAll { it.name == id } + n.instantiations.findAll { it.id == id })
			error(candidates.collect { loc(it) }.first(), Error.ID_IN_USE, id)
		}
		n.templates = n.templates.toSet()
		n.instantiations = n.instantiations.toSet()

		n.instantiations.each { inst ->
			def loc = loc(inst)
			if (inst.id.contains(":"))
				error(loc, Error.COMP_NAME_LIMITS, inst.id)

			def currComp = n.templates.find { it.name == inst.template }
			if (!currComp)
				error(loc, Error.COMP_UNKNOWN, inst.template)
			if (currComp.parameters.size() != inst.parameters.size())
				error(loc, Error.COMP_INST_ARITY, inst.parameters, inst.template, inst.id)
			inst.parameters.findAll { param -> param != "_" && !n.instantiations.any { it.id == param } }.each {
				error(loc, Error.COMP_UNKNOWN_PARAM, it)
			}
		}

		prog = n
	}

	IVisitable exit(BlockLvl2 n) { n }

	void enter(BlockLvl1 n) {
		def loc = loc(n)
		if (n.parameters.size() != n.parameters.toSet().size())
			error(loc, Error.COMP_DUPLICATE_PARAMS, n.parameters, n.name)
		if (n.superParameters.any { it !in n.parameters })
			error(loc, Error.COMP_SUPER_PARAM_MISMATCH, n.superParameters, n.parameters, n.superTemplate)
		if (n.name.contains(":"))
			error(loc, Error.COMP_NAME_LIMITS, n.name)

		currComp = n
	}

	IVisitable exit(BlockLvl1 n) { currComp = null }

	void enter(BlockLvl0 n) {
		datalog = n
		n.typeDeclarations
				.findAll { it.supertype }
				.findAll { decl -> !n.typeDeclarations.any { it.type == decl.supertype } }
				.each { error(loc(it), Error.TYPE_UNKNOWN, it.supertype.name) }
	}

	void enter(RelDeclaration n) {
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

	void enter(Rule n) {
		def conVars = datalog.getConstructedVars(n)
		def duplicateVar = conVars.find { conVars.count(it) > 1 }
		if (duplicateVar) error(loc(n), Error.VAR_MULTIPLE_CONSTR, duplicateVar)
	}

	void enter(AggregationElement n) {
		if (n.relation.name !in AggregationElement.SUPPORTED_PREDICATES)
			error(findParentLoc(), Error.AGGR_UNSUPPORTED_REL, n.relation.name)
	}

	void enter(Relation n) {
		ids.findAll { n.name.startsWith("$it:") }.each {
			error(findParentLoc(), Error.REL_NAME_COMP, "$it:", n.name)
		}
		if (n.name.contains("@")) {
			def parameter = n.name.split("@").last() as String
			if (inDecl || inRuleHead)
				error(findParentLoc(), Error.REL_EXT_INVALID)
			if (!currComp && !prog.instantiations.any { it.id == parameter })
				error(findParentLoc(), Error.INST_UNKNOWN, parameter)
			if (currComp && !currComp.parameters.any { it == parameter })
				error(findParentLoc(), Error.COMP_UNKNOWN_PARAM, parameter)
		}
	}

	void enter(ConstantExpr n) {
		if (n.kind == REAL || n.kind == BOOLEAN)
			error(Error.TYPE_UNSUPP, n.kind as String)
	}
}
