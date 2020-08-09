package org.codesimius.panda.actions.graph

import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.datalog.expr.IExpr
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Error

import static org.codesimius.panda.system.Log.error
import static org.codesimius.panda.system.Log.warn

class Trie {

	static class Node {
		// Either represents a literal, or a relation name (for leaf nodes)
		String value

		Set<Node> children = [] as Set

		// For leaf nodes, represents the order of parameters in the text representation
		List<Integer> paramIndexes

		Node(String value) { this.value = value }

		Node(String relation, List<Integer> paramIndexes) {
			this.value = relation
			this.paramIndexes = paramIndexes
		}

		String toString() { "{$value}" }
	}

	Set<Node> roots = []

	void insert(List tokens, Relation relation, def loc) {
		def currRoots = roots
		def paramIndexes = []
		tokens.each { token ->
			def origToken = token
			// Token represents a parameter
			if (token instanceof Integer) {
				paramIndexes << token
				// Treat all parameters the same (a single node with null value)
				token = null
			}

			def node = currRoots.find { it.value == token }
			if (!node) {
				node = new Node(token as String)
				currRoots << node
			}

			if (currRoots.any { !it.value } && currRoots.size() != 1)
				error(loc, Error.TEXT_LIT_N_VAR, currRoots.collect { it.value }.grep().join(", "), origToken)

			currRoots = node.children
		}
		// At leaf level, no other nodes should exist
		if (!currRoots.empty)
			error(loc, Error.TEXT_MULTIPLE_RELS, (currRoots.collect { it.value } + [relation.name]).join(", "))
		// Leaf node
		currRoots << new Node(relation.name, paramIndexes)
	}

	Relation find(List tokens, def loc) {
		def currRoots = roots
		List<IExpr> params = []
		List<String> nonParams = []
		tokens.each { token ->
			// Search for text literals first
			def node = currRoots.find { it.value == token }
			// Fallback to parameter
			if (!node) {
				node = currRoots.find { !it.value }
				params << (token instanceof ConstantExpr ? token : new VariableExpr(token as String))
			} else
				nonParams << token as String

			if (!node) error(loc, Error.TEXT_UNKNOWN, tokens.join(", "))

			currRoots = node.children
		}

		def p = params.find { it instanceof VariableExpr && it.name in nonParams }
		if (p) warn(loc, Error.TEXT_VAR_MATCHES_LIT, p, tokens.join(" "))

		def leaf = currRoots.first()
		new Relation(leaf.value, leaf.paramIndexes.collect { params[it] })
	}
}
