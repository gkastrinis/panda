package org.codesimius.panda.actions.graph

import groovy.transform.Canonical
import org.codesimius.panda.system.Error

import static org.codesimius.panda.actions.graph.DependencyGraphVisitor.INSTANTIATION
import static org.codesimius.panda.system.Log.error

@Canonical
class CycleDetector {

	Map<String, Graph> graphs

	void checkInstantiations() {
		check(
				graphs[INSTANTIATION].nodes.values() as Collection<Node>,
				{ Edge e -> e.kind == Edge.Kind.ACTUAL_PARAM },
				{ Node node, List<Node> visitedNodes, List<Edge> visitedEdges ->
					def cycleNodes = visitedNodes.drop(visitedNodes.indexOf(node))
					error(Error.INST_CYCLE, cycleNodes.collect { prettify it.id }.join(" - "))
				}
		)
	}

	void checkIndirectEdges() {
		check(
				graphs.findAll { it.key != INSTANTIATION }
						.collect { it.value.nodes.values() }.flatten() as Collection<Node>,
				{ true },
				{ Node node, List<Node> visitedNodes, List<Edge> visitedEdges ->
					def cycleEdges = visitedEdges.drop(visitedNodes.indexOf(node))
					if (cycleEdges.any { it.kind == Edge.Kind.INDIRECT_PARAM })
						error(Error.REL_EXT_CYCLE, cycleEdges.collect { prettify it.node.id }.join(" - "))
				}
		)
	}

	void checkNegation() {
		check(
				graphs.findAll { it.key != INSTANTIATION }
						.collect { it.value.nodes.values() }.flatten() as Collection<Node>,
				{ true },
				{ Node node, List<Node> visitedNodes, List<Edge> visitedEdges ->
					def cycleEdges = visitedEdges.drop(visitedNodes.indexOf(node))
					if (cycleEdges.any { it.kind == Edge.Kind.NEGATION })
						error(Error.REL_NEGATION_CYCLE, cycleEdges.collect { prettify it.node.id }.join(" - "))
				}
		)
	}

	static def check(Collection<Node> nodes, Closure<Boolean> validateEdge, Closure handleCycle) {
		Set<Node> verifiedNodes = [] as Set
		List<Node> visitedNodes
		List<Edge> visitedEdges
		def dfs

		dfs = { Node n ->
			if (n in verifiedNodes)
				return
			if (n in visitedNodes) {
				handleCycle(n, visitedNodes, visitedEdges)
				return
			}

			visitedNodes << n
			n.outEdges.each {
				if (!validateEdge(it)) return
				visitedEdges << it
				dfs it.node
			}
			verifiedNodes << n
		}

		nodes.each { n ->
			visitedNodes = []
			visitedEdges = []
			dfs n
		}
	}

	static def prettify(String str) {
		def (first, second) = str.split(":")
		(first == "null" || first == "_") ? second : "$first:$second"
	}

//	// Kahn's algorithm
//	void topologicalSort() {
//		List<Set<Node>> layers = []
//
//		// Just sort instantiations nodes (top level graph)
//		Map<Node, Integer> inDegrees = graphs[INSTANTIATION].nodes.values()
//				.findAll { it.kind == Node.Kind.INSTANCE }
//				.collectEntries { [(it): it.inEdges.count { it.kind == Edge.Kind.ACTUAL_PARAM }] }
//
//		Set<Node> zeroInNodes = inDegrees.findAll { !it.value }.collect { it.key as Node }
//
//		while (!zeroInNodes.isEmpty()) {
//			zeroInNodes.each { n ->
//				inDegrees.remove(n)
//				// Remove incoming edges
//				n.outEdges.findAll { it.kind == Edge.Kind.ACTUAL_PARAM }.each { inDegrees[it.node]-- }
//			}
//			layers << zeroInNodes
//			zeroInNodes = inDegrees.findAll { !it.value }.collect { it.key as Node }
//		}
//	}
}
