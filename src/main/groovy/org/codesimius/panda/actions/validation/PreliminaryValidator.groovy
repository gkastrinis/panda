package org.codesimius.panda.actions.validation

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.CONSTRUCTOR
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.BOOLEAN
import static org.codesimius.panda.datalog.expr.ConstantExpr.Kind.REAL
import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

@Canonical
class PreliminaryValidator extends DefaultVisitor<IVisitable> {

	private BlockLvl2 prog
	private BlockLvl1 currComp
	private List<String> ids = []

	void enter(BlockLvl2 n) {
		ids = n.components.collect { it.name } + n.instantiations.collect { it.id }
		ids.findAll { ids.count(it) > 1 }.each {
			error(Error.ID_IN_USE, it)
		}
		n.components = n.components.toSet()
		n.instantiations = n.instantiations.toSet()

		n.instantiations.each { inst ->
			if (inst.id.contains(":"))
				error(Error.COMP_NAME_LIMITS, inst.id)

			def currComp = n.components.find { it.name == inst.component }
			if (!currComp)
				error(Error.COMP_UNKNOWN, inst.component)
			if (currComp.parameters.size() != inst.parameters.size())
				error(Error.COMP_INST_ARITY, inst.parameters, inst.component, inst.id)
			inst.parameters.findAll { param -> param != "_" && !n.instantiations.any { it.id == param } }.each {
				error(recall(inst), Error.COMP_UNKNOWN_PARAM, it)
			}
		}

		prog = n
	}

	IVisitable exit(BlockLvl2 n) { n }

	void enter(BlockLvl1 n) {
		if (n.parameters.size() != n.parameters.toSet().size())
			error(Error.COMP_DUPLICATE_PARAMS, n.parameters, n.name)
		if (n.superParameters.any { !(it in n.parameters) })
			error(Error.COMP_SUPER_PARAM_MISMATCH, n.superParameters, n.parameters, n.superComponent)
		if (n.name.contains(":"))
			error(Error.COMP_NAME_LIMITS, n.name)

		currComp = n
	}

	IVisitable exit(BlockLvl1 n) { currComp = null }

	void enter(RelDeclaration n) {
		if (n.relation.exprs.size() != n.types.size())
			error(Error.DECL_MALFORMED, null)
		n.relation.exprs.findAll { n.relation.exprs.count(it) > 1 }.each {
			error(Error.DECL_SAME_VAR, it)
		}
		if (CONSTRUCTOR in n.annotations && !(n.relation instanceof Constructor))
			error(recall(n), Error.CONSTR_NON_FUNC, n.relation.name)
		if (n.relation instanceof Constructor && !(CONSTRUCTOR in n.annotations))
			error(recall(n), Error.FUNC_NON_CONSTR, n.relation.name)
	}

	void enter(AggregationElement n) {
		if (!(n.relation.name in AggregationElement.SUPPORTED_PREDICATES))
			error(Error.AGGR_UNSUPPORTED_REL, n.relation.name)
	}

	void enter(Relation n) {
		ids.findAll { n.name.startsWith("$it:") }.each {
			error(recall(n), Error.REL_NAME_COMP, "$it:", n.name)
		}
		if (n.name.contains("@")) {
			def parameter = n.name.split("@").last() as String
			if (inDecl || inRuleHead)
				error(recall(n), Error.REL_EXT_INVALID)
			if (!currComp && !prog.instantiations.any { it.id == parameter })
				error(recall(n), Error.INST_UNKNOWN, parameter)
			if (currComp && !currComp.parameters.any { it == parameter })
				error(recall(n), Error.COMP_UNKNOWN_PARAM, parameter)
		}
	}

	void enter(ConstantExpr n) {
		if (n.kind == REAL || n.kind == BOOLEAN)
			error(Error.TYPE_UNSUPP, n.kind as String)
	}
}
