package org.codesimius.panda.system

import groovy.transform.Canonical
import org.codesimius.panda.actions.graph.DependencyGraphVisitor
import org.codesimius.panda.actions.graph.Edge
import org.codesimius.panda.actions.graph.Node

import java.nio.file.Files
import java.nio.file.Paths

import static org.codesimius.panda.actions.graph.DependencyGraphVisitor.GLOBAL as GLOBAL_GRAPH
import static org.codesimius.panda.actions.graph.DependencyGraphVisitor.INSTANTIATION as INSTANTIATION_GRAPH

@Canonical
class DependencyGraphDOTGenerator {

	File outDir
	DependencyGraphVisitor dependencyGraph

	void gen() {
		def f = Files.createTempFile(Paths.get(outDir.path), "graph_", ".dot").toFile()
		def fw = new FileWriter(f)
		def emit = { fw.write "$it\n" }

		Logger.log(f.canonicalPath, "GRAPH")

		def edges = []
		def FONT = "Arial"
		emit "digraph {"
		emit "rankdir=LR;"
		emit "outputorder=edgesfirst;"
		emit """node [style=filled,fillcolor="#bdd0ef",fontname="$FONT"];"""
		emit """edge [fontname="$FONT",penwidth=2];"""
		dependencyGraph.graphs.each { graphName, graph ->
			if (graphName == GLOBAL_GRAPH) graphName = ""
			if (graphName != INSTANTIATION_GRAPH) {
				emit "subgraph cluster_$graphName {"
				emit """label="$graphName";style=rounded;bgcolor="#c4c4c4";peripheries=0;fontname="$FONT";"""
			}
			graph.nodes.values().each { node ->
				switch (node.kind) {
					case Node.Kind.TEMPLATE:
						emit """"${node.id}" [label="${node.title}",shape=rectangle,fillcolor="#cc7284"];"""
						break
					case Node.Kind.INSTANCE:
						emit """"${node.id}" [label="${node.title}",shape=octagon,fillcolor="orange"];"""
						break
					case Node.Kind.PARAMETER:
						def name = node.title.split("@").first()
						emit """"${node.id}" [label="$name",shape=diamond,fillcolor="gold"];"""
						break
					case Node.Kind.RELATION:
						emit """"${node.id}" [label="${node.title}",shape=circle];"""
						break
					case Node.Kind.CONSTRUCTOR:
						emit """"${node.id}" [label="${node.title}",shape=doublecircle,fillcolor="#9abde2"];"""
						break
					case Node.Kind.TYPE:
						emit """"${node.id}" [label="${node.title}",shape=Mcircle,fillcolor="#96d67a"];"""
						break
				}

				node.outEdges.each { edge ->
					def toId = edge.node.id
					def l = edge.label
					switch (edge.kind) {
						case Edge.Kind.INHERITANCE:
							edges << """"${
								node.id
							}" -> "$toId" [label="$l",fontcolor="#cc7284",color="#cc7284",style=dashed];"""
							break
						case Edge.Kind.INSTANCE:
							edges << """"${node.id}" -> "$toId" [color="#cc7284",arrowhead=none];"""
							break
						case Edge.Kind.ACTUAL_PARAM:
							edges << """"${
								node.id
							}" -> "$toId" [label="$l",fontcolor="#666666",color="#666666:orange"];"""
							break
						case Edge.Kind.INDIRECT_PARAM:
							edges << """"${node.id}" -> "$toId" [label="$l",color="#666666:gold",style=dashed];"""
							break
						case Edge.Kind.NEGATION:
							edges << """"${node.id}" -> "$toId" [color="red"];"""
							break
						case Edge.Kind.RELATION:
							edges << """"${node.id}" -> "$toId" """
							break
						case Edge.Kind.SUBTYPE:
							edges << """"${node.id}" -> "$toId" [color="#666666:#96d67a",style=dashed];"""
							break
					}
				}
			}
			if (graphName != INSTANTIATION_GRAPH) emit "}"
		}
		edges.each { emit it }
		emit "}"
		fw.close()
	}
}
