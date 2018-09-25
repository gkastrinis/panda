package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.actions.graph.Trie
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.RelationText
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.TEXT
import static org.codesimius.panda.system.Error.error

@Canonical
class NormalizingTransformer extends DefaultTransformer {

	BlockLvl0 datalog
	Trie trie

	IVisitable visit(BlockLvl0 n) {
		datalog = n
		trie = new Trie()
		def textRules = n.rules.findAll { TEXT in it.annotations }
		textRules.each {
			def loc = it.loc()

			if (!(it.head instanceof Relation)) error(loc, Error.TEXT_MALFORMED_HEAD, null)
			def relation = it.head as Relation
			def relationParams = relation.exprs
			relationParams.findAll { !(it instanceof VariableExpr) }.each { error(loc, Error.TEXT_HEAD_NON_VAR) }

			if (!(it.body instanceof RelationText)) error(loc, Error.TEXT_MALFORMED_BODY, null)
			def tokens = (it.body as RelationText).tokens.collect { token ->
				if (token instanceof ConstantExpr) error(loc, Error.TEXT_BODY_NON_VAR)
				def index = relationParams.findIndexOf { (it as VariableExpr).name == token }
				index >= 0 ? index : token
			}
			trie.insert(tokens, relation, loc)
		}
		n.rules -= textRules

		def rules = n.rules.collect { visit it } as Set<Rule>
		new BlockLvl0(n.relDeclarations, n.typeDeclarations, rules)
	}

	IVisitable visit(RelationText n) { trie.find(n.tokens, findParentLoc()) }
}
