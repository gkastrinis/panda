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
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

class DependencyGraphVisitor extends DefaultVisitor<IVisitable> {

	Map<String, Graph> graphs = [:]

	private Graph globalGraph
	private Graph currGraph
	private boolean inNegation
	private Set<RelInfo> headRelations
	private Set<RelInfo> bodyRelations
	private BlockLvl2 origP
	private BlockLvl1 currComp

	void enter(BlockLvl2 n) {
		globalGraph = mkGraph("_")
		graphs["_"] = globalGraph
		origP = n
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
			currComp = n.components.find { it.name == inst.component }
			if (!currComp)
				error(Error.COMP_UNKNOWN, inst.component)
			if (currComp.parameters.size() != inst.parameters.size())
				error(Error.COMP_INST_ARITY, inst.parameters, inst.component, inst.id)

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
		currComp = n
	}

	IVisitable exit(BlockLvl1 n) {
		currGraph = globalGraph
		currComp = null
	}

	void enter(RelDeclaration n) {
		mkNode(currGraph, n.relation.name, CONSTRUCTOR in n.annotations ? Node.Kind.CONSTRUCTOR : Node.Kind.RELATION)
	}

	void enter(Rule n) {
		headRelations = [] as Set
		bodyRelations = [] as Set
	}

	IVisitable exit(Rule n) {
		headRelations.each { headRel ->
			def headRelNode = mkNode(currGraph, headRel.name, headRel.isConstructor ? Node.Kind.CONSTRUCTOR : Node.Kind.RELATION)
			bodyRelations.each { bodyRel ->
				def relNode = mkNode(currGraph, bodyRel.name, bodyRel.isConstructor ? Node.Kind.CONSTRUCTOR : Node.Kind.RELATION)
				mkEdge(headRelNode, relNode, bodyRel.isNegated ? Edge.Kind.NEGATED : Edge.Kind.RELATION)
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
		if (n.name.contains("@") && (inDecl || inRuleHead))
			error(recall(n), Error.REL_EXT_INVALID)

		if (inDecl) return

		if (n.name.contains("@") && currGraph) {
			def parameter = n.name.split("@").last()
			// null when in global space
			if (!currComp && !origP.instantiations.any { it.id == parameter })
				error(recall(n), Error.INST_UNKNOWN, parameter as String)
			if (currComp && !currComp.parameters.any { it == parameter })
				error(recall(n), Error.COMP_UNKNOWN_PARAM, parameter as String)

			def relNode = mkNode(currGraph, n.name, Node.Kind.PARAMETER)
			mkEdge(relNode, currGraph.headNode, Edge.Kind.PARAMETER, parameter)
		}

		if (inRuleHead) headRelations << new RelInfo(n.name, false, false)
		else if (inRuleBody) bodyRelations << new RelInfo(n.name, false, inNegation)
	}

	Graph mkGraph(String name) {
		if (!graphs[name]) graphs[name] = new Graph(name)
		graphs[name]
	}

	static Node mkNode(Graph g, String name, Node.Kind kind) {
		def id = "${g.name}:$name" as String
		if (!g.nodes[id]) g.nodes[id] = new Node(id, name, kind)
		g.nodes[id]
	}

	static void mkEdge(Node from, Node to, Edge.Kind kind, String label = "") {
		from.outEdges << new Edge(to, kind, label)
	}

	@Canonical
	static class RelInfo {
		String name
		boolean isConstructor
		boolean isNegated
	}
}
