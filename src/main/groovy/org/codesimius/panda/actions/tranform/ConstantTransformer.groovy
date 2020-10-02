package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.CONSTANT
import static org.codesimius.panda.datalog.Annotation.METADATA

@Canonical
class ConstantTransformer extends DefaultTransformer {

    @Delegate
    Compiler compiler

    Map<String, ConstantExpr> constants = [:]

    void enter(BlockLvl0 n) {
        def constDecls = n.rules.findAll { CONSTANT in it.annotations }
        constDecls.each {
            if (it.head !instanceof Relation)
                error(loc(it), Error.CONSTANT_HEAD)
            def rel = it.head as Relation
            it.annotations.findAll { it != CONSTANT && it != METADATA }.each { an ->
                error(loc(it), Error.CONSTANT_INVALID_ANN, an, rel.name)
            }
            if (rel.exprs.size() != 1)
                error(loc(it), Error.CONSTANT_ARITY, rel.name)
            if (rel.exprs.first() !instanceof ConstantExpr)
                error(loc(it), Error.CONSTANT_NON_PRIMITIVE, rel.name, rel.exprs.first())
            if (it.body)
                error(loc(it), Error.CONSTANT_BODY, rel.name)

            constants[rel.name] = rel.exprs.first()
        }
        n.rules -= constDecls
    }

    void enter(Relation n) { if (constants[n.name]) error(loc(n), Error.CONSTANT_AS_REL, n.name) }

    IVisitable exit(VariableExpr n) {
        if (inRuleHead || inRuleBody) return constants[n.name] ?: n
        else return n
    }
}
