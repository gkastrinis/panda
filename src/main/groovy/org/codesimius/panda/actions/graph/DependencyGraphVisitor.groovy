package org.codesimius.panda.actions.graph

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.NegationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.CONSTRUCTOR
import static org.codesimius.panda.system.Error.error

class DependencyGraphVisitor extends DefaultVisitor<IVisitable> {

	// Each subgraph represents a different component
	Map<String, Graph> graphs = [:].withDefault { new Graph(it) }

	private Graph globalGraph
	private Graph currGraph
	private boolean inNegation
	private Set<RelInfo> headRelations
	private Set<RelInfo> bodyRelations
	private BlockLvl2 origP
	private BlockLvl1 currComp

	void enter(BlockLvl2 n) {
		globalGraph = graphs["_"]
		graphs["_"] = globalGraph
		origP = n
		currGraph = globalGraph
	}

	IVisitable exit(BlockLvl2 n) {
		n.components.each { comp ->
			if (comp.superComponent) {
				def superComp = n.components.find { it.name == comp.superComponent }
				def baseGraph = graphs[comp.name]
				def superGraph = graphs[comp.superComponent]
				def label = comp.superParameters.withIndex().collect { superParam, int i ->
					"$superParam/${superComp.parameters[i]}"
				}.join(", ")
				baseGraph.headNode.connectTo(superGraph.headNode, Edge.Kind.INHERITANCE, label)
			}
		}
		n.instantiations.each { inst ->
			def instanceNode = globalGraph.touch(inst.id, Node.Kind.INSTANCE)
			instanceNode.connectTo(graphs[inst.component].headNode, Edge.Kind.INSTANCE)

			def comp = n.components.find { it.name == inst.component }
			Map<String, List<String>> actualToFormals = [:].withDefault { [] }
			inst.parameters.withIndex().each { String param, int i -> actualToFormals[param] << comp.parameters[i] }
			actualToFormals.each { actual, formals ->
				def node = globalGraph.touch(actual, Node.Kind.INSTANCE)
				instanceNode.connectTo(node, Edge.Kind.PARAMETER, formals.join(", "))
			}
		}

		topologicalSort()

		return n
	}

	void enter(BlockLvl1 n) {
		currGraph = graphs[n.name]
		currComp = n
	}

	IVisitable exit(BlockLvl1 n) {
		currGraph = globalGraph
		currComp = null
	}

	void enter(RelDeclaration n) {
		currGraph.touch(n.relation.name, CONSTRUCTOR in n.annotations ? Node.Kind.CONSTRUCTOR : Node.Kind.RELATION)
	}

	void enter(Rule n) {
		headRelations = [] as Set
		bodyRelations = [] as Set
	}

	IVisitable exit(Rule n) {
		headRelations.each { headRel ->
			def headRelNode = currGraph.touch(headRel.name, headRel.isConstructor ? Node.Kind.CONSTRUCTOR : Node.Kind.RELATION)
			bodyRelations.each { bodyRel ->
				def relNode = currGraph.touch(bodyRel.name, bodyRel.isConstructor ? Node.Kind.CONSTRUCTOR : Node.Kind.RELATION)
				headRelNode.connectTo(relNode, bodyRel.isNegated ? Edge.Kind.NEGATED : Edge.Kind.RELATION)
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

	void enter(Constructor n) {
		if (inRuleHead) headRelations << new RelInfo(n.name, true, false)
		else if (inRuleBody) bodyRelations << new RelInfo(n.name, true, inNegation)
	}

	void enter(Relation n) {
		if (inDecl) return

		if (n.name.contains("@") && currGraph) {
			def parameter = n.name.split("@").last()
			def relNode = currGraph.touch(n.name, Node.Kind.PARAMETER)
			relNode.connectTo(currGraph.headNode, Edge.Kind.PARAMETER, parameter)
		}

		if (inRuleHead) headRelations << new RelInfo(n.name, false, false)
		else if (inRuleBody) bodyRelations << new RelInfo(n.name, false, inNegation)
	}

	// Kahn's algorithm
	void topologicalSort() {
		List<Set<Node>> layers = []

		// Just sort instantiations nodes (top level graph)
		Map<Node, Integer> inDegrees = graphs["_"].nodes.values()
				.findAll { it.kind == Node.Kind.INSTANCE }
				.collectEntries { [(it): it.inEdges.size()] }

		Set<Node> zeroInNodes = inDegrees.findAll { !it.value }.collect { it.key as Node }

		while (!zeroInNodes.isEmpty()) {
			zeroInNodes.each { n ->
				inDegrees.remove(n)
				// Remove incoming edge for nodes in the top level graph
				n.outEdges.findAll { graphs["_"].nodes[it.node.id] }.collect { it.node }.each { inDegrees[it]-- }
			}
			layers << zeroInNodes
			zeroInNodes = inDegrees.findAll { !it.value }.collect { it.key as Node }
		}
		if (!inDegrees.isEmpty())
			error(Error.INST_CYCLE, inDegrees.collect { it.key.title })
	}

	@Canonical
	static class RelInfo {
		String name
		boolean isConstructor
		boolean isNegated
	}
}
