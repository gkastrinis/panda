package org.codesimius.panda.system

import groovy.transform.Canonical
import org.apache.commons.logging.LogFactory
import org.codesimius.panda.actions.graph.DependencyGraphVisitor
import org.codesimius.panda.actions.graph.Edge
import org.codesimius.panda.actions.graph.Node

import java.nio.file.Files
import java.nio.file.Paths

@Canonical
class DOTGenerator {

	File outDir
	DependencyGraphVisitor dependencyGraph
	private FileWriter fw

	void gen() {
		def f = Files.createTempFile(Paths.get(outDir.path), "graph_", ".dot").toFile()
		fw = new FileWriter(f)

		def log = LogFactory.getLog(DOTGenerator.class)
		log.info("[DD] GRAPH: ${f.canonicalPath}")

		emit "digraph G {"
		emit "\trankdir=LR;"
		emit "\toutputorder=edgesfirst;"
		emit """\tnode [style=filled,fillcolor="#cce5ff",fontname="Arial"];"""
		emit """\tedge [fontname="Arial"];"""
		dependencyGraph.graphs.each { graphName, graph ->
			if (graphName != "_") {
				emit "\tsubgraph cluster_$graphName {"
				emit """\t\tlabel="$graphName";style=rounded;bgcolor="#c4c4c4";peripheries=0;fontname="Arial";"""
			}
			graph.nodes.each { nodeId, node ->
				if (nodeId == node.name)
					emit """\t\t"$nodeId" [label="",shape=circle,fillcolor=black,width=0.15];"""
				else if (node.kind == Node.Kind.PARAMETER)
					emit """\t\t"$nodeId" [label="${node.name}",shape=diamond,fillcolor="gold"];"""
				else if (node.kind == Node.Kind.INSTANCE)
					emit """\t\t"$nodeId" [label="${node.name}",shape=octagon,fillcolor="orange"];"""
				else
					emit """\t\t"$nodeId" [label="${node.name}",shape=circle];"""

				node.outEdges.each {
					if (it.kind == Edge.Kind.INHERITANCE)
						emit """\t\t"$nodeId" -> "${it.node.id}" [label="${
							it.label
						}",fontcolor="#666666",style=dashed,color="#666666"];"""
					else if (it.kind == Edge.Kind.PARAMETER && node.kind == Node.Kind.PARAMETER)
						emit """\t\t"$nodeId" -> "${it.node.id}" [label="${
							it.label
						}",fontcolor="#666666",color="#666666:gold",penwidth=2,arrowhead=none];"""
					else if (it.kind == Edge.Kind.PARAMETER)
						emit """\t\t"$nodeId" -> "${it.node.id}" [label="${
							it.label
						}",fontcolor="#666666",color="#666666:orange",penwidth=2];"""
					else if (it.kind == Edge.Kind.INSTANCE)
						emit """\t\t"$nodeId" -> "${it.node.id}" [color="#666666",arrowhead=none];"""
					else if (it.kind == Edge.Kind.NEGATED)
						emit """\t\t"$nodeId" -> "${it.node.id}" [color="red"];"""
					else
						emit """\t\t"$nodeId" -> "${it.node.id}" [color="#333333"];"""
				}
			}
			if (graphName != "_") emit "\t};"
		}
		emit "}"
		fw.close()
	}

	void emit(String data) { fw.write "$data\n" }
}
