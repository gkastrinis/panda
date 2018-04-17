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

	private Graph currGraph
	private boolean inNegation
	private Set<RelInfo> headRelations
	private Set<RelInfo> bodyRelations
	private BlockLvl2 origP
	private BlockLvl1 currComp

	static final String GLOBAL = "_"
	static final String INSTANTIATION = null

	void enter(BlockLvl2 n) {
		currGraph = graphs[GLOBAL]
		origP = n
	}

	IVisitable exit(BlockLvl2 n) {
		n.components.findAll { comp -> !n.instantiations.any { it.id == comp.name } }.each { comp ->
			def baseNode = graphs[INSTANTIATION].touch(comp.name, Node.Kind.TEMPLATE)
			def superNode = graphs[INSTANTIATION].touch(comp.superComponent, Node.Kind.TEMPLATE)
			baseNode.connectTo(superNode, Edge.Kind.INHERITANCE)
		}
		n.instantiations.each { inst ->
			def templateNode = graphs[INSTANTIATION].touch(inst.component, Node.Kind.TEMPLATE)
			def instanceNode = graphs[INSTANTIATION].touch(inst.id, Node.Kind.INSTANCE)
			instanceNode.connectTo(templateNode, Edge.Kind.INSTANCE)

			inst.parameters.each {
				def paramNode = graphs[INSTANTIATION].touch(it, Node.Kind.INSTANCE)
				instanceNode.connectTo(paramNode, Edge.Kind.ACTUAL_PARAM)
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
		currGraph = graphs[GLOBAL]
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
				headRelNode.connectTo(relNode, bodyRel.isNegated ? Edge.Kind.NEGATION : Edge.Kind.RELATION)
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

		if (n.name.contains("@")) {
			def (String name, String parameter) = n.name.split("@")
			def relNode = currGraph.touch(n.name, Node.Kind.PARAMETER)
			def paramNode = graphs[parameter == "_" ? GLOBAL : parameter].touch(name, Node.Kind.RELATION)
			relNode.connectTo(paramNode, Edge.Kind.INDIRECT_PARAM)
		}

		if (inRuleHead) headRelations << new RelInfo(n.name, false, false)
		else if (inRuleBody) bodyRelations << new RelInfo(n.name, false, inNegation)
	}

	// Kahn's algorithm
	void topologicalSort() {
		List<Set<Node>> layers = []

		// Just sort instantiations nodes (top level graph)
		Map<Node, Integer> inDegrees = graphs[INSTANTIATION].nodes.values()
				.findAll { it.kind == Node.Kind.INSTANCE }
				.collectEntries { [(it): it.inEdges.count { it.kind == Edge.Kind.ACTUAL_PARAM }] }

		Set<Node> zeroInNodes = inDegrees.findAll { !it.value }.collect { it.key as Node }

		while (!zeroInNodes.isEmpty()) {
			zeroInNodes.each { n ->
				inDegrees.remove(n)
				// Remove incoming edges
				n.outEdges.findAll { it.kind == Edge.Kind.ACTUAL_PARAM }.each { inDegrees[it.node]-- }
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
