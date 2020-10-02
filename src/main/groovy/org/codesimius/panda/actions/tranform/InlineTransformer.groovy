package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.INLINE
import static org.codesimius.panda.datalog.Annotation.METADATA

@Canonical
class InlineTransformer extends DefaultTransformer {

	@Delegate
	Compiler compiler

	Rule currRule
	// Inline declarations
	Map<String, Rule> inlines = [:]
	// Stack due to nested inlines
	Stack<Map<String, VariableExpr>> mappings = []
	// Stack of inline relations actively being replaced
	Stack<String> activeInlines = []

	void enter(BlockLvl0 n) {
		def inlineDecls = n.rules.findAll { INLINE in it.annotations }
		inlineDecls.each {
			if (it.head !instanceof Relation)
				error(loc(it), Error.INLINE_HEAD)
			def rel = it.head as Relation
			it.annotations.findAll { it != INLINE && it != METADATA }.each { an ->
				error(loc(it), Error.INLINE_INVALID_ANN, an, rel.name)
			}
			rel.exprs.findAll { it !instanceof VariableExpr || it.name == "_" }.each {expr ->
				error(loc(it), Error.INLINE_HEAD_NONVARS, rel.name, expr)
			}
			rel.exprs.findAll { rel.exprs.count(it) > 1 }.each {expr ->
				error(loc(it), Error.INLINE_HEAD_DUPVARS, rel.name, expr)
			}

			inlines[rel.name] = it
		}
		n.rules -= inlineDecls
	}

	void enter(Rule n) { currRule = n }

	IVisitable exit(Rule n) {
		currRule = null
		return super.exit(n)
	}

	void enter(Constructor n) {
		if (inlines[n.name])
			error(loc(n), Error.INLINE_AS_CONSTR, n.name)
	}

	void enter(Relation n) {
		def inlineRule = inlines[n.name]
		if (!inlineRule) return

		if (inDecl || inRuleHead)
			error(loc(n), Error.INLINE_NOTIN_BODY, n.name)
		if (n.name in activeInlines)
			error(loc(n), Error.INLINE_RECURSION, n.name)
		def inlineRel = inlineRule.head as Relation

		def inlineVarsToChange = currDatalog.getBodyVars(inlineRule).findAll { it.name != "_" } as Set
		def illegalVars = currDatalog.getAllVars(currRule).findAll { it.name != "_" } as Set
		def mapping = [:]
		// Generate new, unique vars for those in the inline rule that are shadowed by vars in the current rule
		inlineVarsToChange.findAll { n.exprs }.each {
			// Make a new unique var name. Use incremental numbers instead of random ones for ease of testing
			VariableExpr newVar
			def counter = 0
			while (true) {
				newVar = new VariableExpr("?v${counter++}")
				if (newVar !in illegalVars) break
			}
			mapping[it.name] = newVar
			illegalVars << newVar
		}
		inlineRel.exprs.eachWithIndex { it, int i -> mapping[it.name] = n.exprs[i] }
		mappings.push mapping
		activeInlines << inlineRel.name
	}

	IVisitable exit(Relation n) {
		def inlineRule = inlines[n.name]
		if (!inlineRule) return super.exit(n)

		inRuleBody = true
		def newElement = visit inlineRule.body
		inRuleBody = false
		mappings.pop()
		activeInlines.pop()
		return newElement
	}

	void enter(Type n) {
		if (inlines[n.name])
			error(loc(n), Error.INLINE_AS_TYPE, n.name)
	}

	IVisitable exit(VariableExpr n) {
		if (!inRuleBody) return n
		// Check the stack of active mappings, from top to bottom, for a valid mapping
		def currVar = n
		def currIndex = mappings.size() - 1
		while (currIndex >= 0) {
			def mapped = mappings[currIndex][currVar.name]
			// Update the current mapping, to reflect the semantics of nesting in inlining
			if (mapped) currVar = mapped
			currIndex--
		}
		return currVar
	}
}
