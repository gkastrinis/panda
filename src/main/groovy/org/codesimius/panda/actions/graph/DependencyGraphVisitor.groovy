package org.codesimius.panda.actions.graph

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.NegationElement
import org.codesimius.panda.datalog.element.relation.Relation

class DependencyGraphVisitor extends DefaultVisitor<IVisitable> {

	Map<String, Node> nodes = [:]

	private Node globalNode = mkNode("_", Node.Kind.INSTANCE)
	private Node currNode = globalNode
	private boolean inNegation
	private Set<String> headRelations
	// Relation x isNegated
	private Map<String, Boolean> bodyRelations

	IVisitable exit(BlockLvl2 n) {
		n.components.each { comp ->
			if (comp.superComponent) {
				def superComp = n.components.find { it.name == comp.superComponent }
				def baseNode = mkNode(comp.name, Node.Kind.TEMPLATE)
				def superNode = mkNode(comp.superComponent, Node.Kind.TEMPLATE)
				def label = comp.superParameters.withIndex().collect { superParam, int i ->
					"$superParam/${superComp.parameters[i]}"
				}.join(", ")
				mkEdge(baseNode, superNode, Edge.Kind.INHERITANCE, label)
			}
		}
		n.instantiations.each { inst ->
			def templateNode = mkNode(inst.component, Node.Kind.TEMPLATE)
			def instanceNode = mkNode(inst.id, Node.Kind.INSTANCE)
			mkEdge(instanceNode, templateNode, Edge.Kind.INSTANCE)
			inst.parameters.each { mkEdge(instanceNode, mkNode(it, Node.Kind.INSTANCE), Edge.Kind.PARAMETER) }
		}
		return n
	}

	void enter(BlockLvl1 n) {
		currNode = mkNode(n.name, Node.Kind.TEMPLATE)
	}

	IVisitable exit(BlockLvl1 n) {
		currNode = globalNode
		null
	}

	void enter(Rule n) {
		headRelations = [] as Set
		bodyRelations = [:]
	}

	IVisitable exit(Rule n) {
		headRelations.each { headRel ->
			def headRelNode = mkNode(currNode.name, headRel, Node.Kind.RELATION)
			bodyRelations.each { rel, inNeg ->
				def relNode = mkNode(currNode.name, rel, Node.Kind.RELATION)
				mkEdge(headRelNode, relNode, inNeg ? Edge.Kind.NEGATED : Edge.Kind.RELATION)
			}
		}
		null
	}

	void enter(NegationElement n) {
		inNegation = true
	}

	IVisitable exit(NegationElement n) {
		inNegation = false
		null
	}

	void enter(Relation n) {
		if (inDecl) return

		String name
		if (n.name.contains("@") && currNode) {
			def (String simpleName, String parameter) = n.name.split("@")
			def relNode = mkNode(currNode.name, simpleName, Node.Kind.PARAMETER)
			mkEdge(relNode, currNode, Edge.Kind.PARAMETER, parameter)
			name = simpleName
		} else {
			def relNode = mkNode(currNode.name, n.name, Node.Kind.RELATION)
			mkEdge(relNode, currNode, Edge.Kind.RELATION)
			name = n.name
		}
		if (inRuleHead) headRelations << name
		else bodyRelations[name] = inNegation
	}

	Node mkNode(String tag = "", String name, Node.Kind kind) {
		def id = "$tag:$name" as String
		if (!nodes[id]) nodes[id] = new Node(name, kind)
		nodes[id]
	}

	static void mkEdge(Node from, Node to, Edge.Kind kind, String label = "") {
		from.outEdges << new Edge(to, kind, label)
	}
}
