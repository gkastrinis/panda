package org.codesimius.panda.system

import groovy.transform.Canonical
import org.apache.commons.logging.LogFactory
import org.codesimius.panda.actions.graph.DependencyGraphVisitor
import org.codesimius.panda.actions.graph.Edge
import org.codesimius.panda.actions.graph.Node

import java.nio.file.Files
import java.nio.file.Paths

import static org.codesimius.panda.actions.graph.DependencyGraphVisitor.INSTANTIATION as INSTANTIATION_GRAPH

@Canonical
class DOTGenerator {

	File outDir
	DependencyGraphVisitor dependencyGraph

	void gen() {
		def FONT = "Arial"
		def COMP_COLOR = "#c4c4c4"
		def PARAM_NODE_COLOR = "gold"
		def INST_NODE_COLOR = "orange"
		def CONSTR_NODE_COLOR = "#9abde2"
		def DEF_NODE_COLOR = "#cce5ff"
		def INHERIT_FONT_COLOR = "#666666"
		def PARAM_FONT_COLOR = INHERIT_FONT_COLOR
		def INST_FONT_COLOR = INHERIT_FONT_COLOR
		def NEG_FONT_COLOR = "red"
		def DEF_FONT_COLOR = "#333333"

		def f = Files.createTempFile(Paths.get(outDir.path), "graph_", ".dot").toFile()
		def fw = new FileWriter(f)
		def emit = { fw.write "$it\n" }

		def log = LogFactory.getLog(DOTGenerator.class)
		log.info("[DD] GRAPH: ${f.canonicalPath}")

		def pendingEdges = []
		def getEdge = { String nodeId, Node node, Edge edge ->
			def toId = edge.node.id
			def l = edge.label
			if (edge.kind == Edge.Kind.INHERITANCE)
				""""$nodeId" -> "$toId" [label="$l",fontcolor="$INHERIT_FONT_COLOR",color="$INHERIT_FONT_COLOR",style=dashed];"""
			else if (edge.kind == Edge.Kind.PARAM_REL)
				""""$nodeId" -> "$toId" [label="$l",fontcolor="$PARAM_FONT_COLOR",color="$PARAM_FONT_COLOR:$PARAM_NODE_COLOR",penwidth=2,arrowhead=none];"""
			else if (edge.kind == Edge.Kind.PARAM_COMP)
				""""$nodeId" -> "$toId" [label="$l",fontcolor="$PARAM_FONT_COLOR",color="$PARAM_FONT_COLOR:$INST_NODE_COLOR",penwidth=2];"""
			else if (edge.kind == Edge.Kind.INSTANCE)
				""""$nodeId" -> "$toId" [color="$INST_FONT_COLOR",arrowhead=none];"""
			else if (edge.kind == Edge.Kind.NEGATED)
				""""$nodeId" -> "$toId" [color="$NEG_FONT_COLOR"];"""
			else
				""""$nodeId" -> "$toId" [color="$DEF_FONT_COLOR"];"""
		}

		emit "digraph {"
		emit "rankdir=LR;"
		emit "outputorder=edgesfirst;"
		emit """node [style=filled,fillcolor="$DEF_NODE_COLOR",fontname="$FONT"];"""
		emit """edge [fontname="$FONT"];"""
		dependencyGraph.graphs.each { graphName, graph ->
			if (graphName != INSTANTIATION_GRAPH) {
				emit "subgraph cluster_$graphName {"
				emit """label="$graphName";style=rounded;bgcolor="$COMP_COLOR";peripheries=0;fontname="$FONT";"""
			}
			graph.nodes.each { nodeId, node ->
				if (nodeId == node.title)
					emit """"$nodeId" [label="",shape=circle,fillcolor=black,width=0.15];"""
				else if (node.kind == Node.Kind.PARAMETER) {
					def name = node.title.split("@").first()
					emit """"$nodeId" [label="$name",shape=diamond,fillcolor="$PARAM_NODE_COLOR"];"""
				} else if (node.kind == Node.Kind.INSTANCE)
					emit """"$nodeId" [label="${node.title}",shape=octagon,fillcolor="$INST_NODE_COLOR"];"""
				else if (node.kind == Node.Kind.CONSTRUCTOR)
					emit """"$nodeId" [label="${node.title}",shape=doublecircle,fillcolor="$CONSTR_NODE_COLOR"];"""
				else
					emit """"$nodeId" [label="${node.title}",shape=circle];"""

				node.outEdges.each {
					def str = getEdge(nodeId, node, it)
					// Parameter node connecting to "global" instantiation nodes
					// Edge must be declared outside the current subgraph
					if (graphName != INSTANTIATION_GRAPH && it.kind == Edge.Kind.PARAM_REL)
						pendingEdges << str
					else
						emit str
				}
			}
			if (graphName != INSTANTIATION_GRAPH) emit "}"
		}
		pendingEdges.each { emit it }
		emit "}"
		fw.close()
	}
}
