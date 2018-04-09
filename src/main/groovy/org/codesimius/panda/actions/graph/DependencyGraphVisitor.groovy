package org.codesimius.panda.actions.graph

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.NegationElement
import org.codesimius.panda.datalog.element.relation.Relation

class DependencyGraphVisitor extends DefaultVisitor<IVisitable> {

	Map<String, Graph> graphs = [:]

	private Graph globalGraph
	private Graph currGraph
	private boolean inNegation
	private Set<String> headRelations
	// Relation x isNegated
	private Map<String, Boolean> bodyRelations

	void enter(BlockLvl2 n) {
		globalGraph = mkGraph("_")
		graphs["_"] = globalGraph
		currGraph = globalGraph
	}

	IVisitable exit(BlockLvl2 n) {
		n.components.each { comp ->
			if (comp.superComponent) {
				def superComp = n.components.find { it.name == comp.superComponent }
				def baseNode = mkGraph(comp.name)
				def superNode = mkGraph(comp.superComponent)
				def label = comp.superParameters.withIndex().collect { superParam, int i ->
					"$superParam/${superComp.parameters[i]}"
				}.join(", ")
				mkEdge(baseNode.headNode, superNode.headNode, Edge.Kind.INHERITANCE, label)
			}
		}
		n.instantiations.each { inst ->
			def instanceNode = mkNode(globalGraph, inst.id, Node.Kind.INSTANCE)
			mkEdge(instanceNode, graphs[inst.component].headNode, Edge.Kind.INSTANCE)

			def comp = n.components.find { it.name == inst.component }
			Map<String, List<String>> actualToFormals = [:].withDefault { [] }
			inst.parameters.withIndex().each { String param, int i -> actualToFormals[param] << comp.parameters[i] }
			actualToFormals.each { actual, formals ->
				mkEdge(instanceNode, mkNode(globalGraph, actual, Node.Kind.INSTANCE), Edge.Kind.PARAMETER, formals.join(", "))
			}
		}
		return n
	}

	void enter(BlockLvl1 n) {
		currGraph = mkGraph(n.name)
	}

	IVisitable exit(BlockLvl1 n) {
		currGraph = globalGraph
		null
	}

	void enter(RelDeclaration n) { mkNode(currGraph, currGraph.name, n.relation.name, Node.Kind.RELATION) }

	void enter(Rule n) {
		headRelations = [] as Set
		bodyRelations = [:]
	}

	IVisitable exit(Rule n) {
		headRelations.each { headRel ->
			def headRelNode = mkNode(currGraph, currGraph.name, headRel, Node.Kind.RELATION)
			bodyRelations.each { rel, inNeg ->
				def relNode = mkNode(currGraph, currGraph.name, rel, Node.Kind.RELATION)
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
		if (n.name.contains("@") && currGraph) {
			def (String simpleName, String parameter) = n.name.split("@")
			def relNode = mkNode(currGraph, currGraph.name, simpleName, Node.Kind.PARAMETER)
			mkEdge(relNode, currGraph.headNode, Edge.Kind.PARAMETER, parameter)
			name = simpleName
		} else
			name = n.name

		if (inRuleHead) headRelations << name
		else bodyRelations[name] = inNegation
	}

	Graph mkGraph(String name) {
		if (!graphs[name]) graphs[name] = new Graph(name)
		graphs[name]
	}

	static Node mkNode(Graph g, String tag = "", String name, Node.Kind kind) {
		def id = "$tag:$name" as String
		if (!g.nodes[id]) g.nodes[id] = new Node(id, name, kind)
		g.nodes[id]
	}

	static void mkEdge(Node from, Node to, Edge.Kind kind, String label = "") {
		from.outEdges << new Edge(to, kind, label)
	}
}
