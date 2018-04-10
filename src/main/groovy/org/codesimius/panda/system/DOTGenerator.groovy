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

		emit "digraph {"
		emit "rankdir=LR;"
		emit "outputorder=edgesfirst;"
		emit """node [style=filled,fillcolor="$DEF_NODE_COLOR",fontname="$FONT"];"""
		emit """edge [fontname="$FONT"];"""
		dependencyGraph.graphs.each { graphName, graph ->
			if (graphName != "_") {
				emit "subgraph cluster_$graphName {"
				emit """label="$graphName";style=rounded;bgcolor="$COMP_COLOR";peripheries=0;fontname="$FONT";"""
			}
			graph.nodes.each { nodeId, node ->
				if (nodeId == node.name)
					emit """"$nodeId" [label="",shape=circle,fillcolor=black,width=0.15];"""
				else if (node.kind == Node.Kind.PARAMETER) {
					def name = node.name.split("@").first()
					emit """"$nodeId" [label="$name",shape=diamond,fillcolor="$PARAM_NODE_COLOR"];"""
				} else if (node.kind == Node.Kind.INSTANCE)
					emit """"$nodeId" [label="${node.name}",shape=octagon,fillcolor="$INST_NODE_COLOR"];"""
				else if (node.kind == Node.Kind.CONSTRUCTOR)
					emit """"$nodeId" [label="${node.name}",shape=doublecircle,fillcolor="$CONSTR_NODE_COLOR"];"""
				else
					emit """"$nodeId" [label="${node.name}",shape=circle];"""

				node.outEdges.each {
					def toId = it.node.id
					def l = it.label

					if (it.kind == Edge.Kind.INHERITANCE)
						emit """"$nodeId" -> "$toId" [label="$l",fontcolor="$INHERIT_FONT_COLOR",color="$INHERIT_FONT_COLOR",style=dashed];"""
					else if (it.kind == Edge.Kind.PARAMETER && node.kind == Node.Kind.PARAMETER)
						emit """"$nodeId" -> "$toId" [label="$l",fontcolor="$PARAM_FONT_COLOR",color="$PARAM_FONT_COLOR:$PARAM_NODE_COLOR",penwidth=2,arrowhead=none];"""
					else if (it.kind == Edge.Kind.PARAMETER)
						emit """"$nodeId" -> "$toId" [label="$l",fontcolor="$PARAM_FONT_COLOR",color="$PARAM_FONT_COLOR:$INST_NODE_COLOR",penwidth=2];"""
					else if (it.kind == Edge.Kind.INSTANCE)
						emit """"$nodeId" -> "$toId" [color="$INST_FONT_COLOR",arrowhead=none];"""
					else if (it.kind == Edge.Kind.NEGATED)
						emit """"$nodeId" -> "$toId" [color="$NEG_FONT_COLOR"];"""
					else
						emit """"$nodeId" -> "$toId" [color="$DEF_FONT_COLOR"];"""
				}
			}
			if (graphName != "_") emit "}"
		}
		emit "}"
		fw.close()
	}
}
