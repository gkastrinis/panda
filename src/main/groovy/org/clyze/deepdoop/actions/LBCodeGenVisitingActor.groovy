package org.clyze.deepdoop.actions

import groovy.transform.InheritConstructors
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE

@InheritConstructors
class LBCodeGenVisitingActor extends DefaultCodeGenVisitingActor {

	//Set<String> globalAtoms
	//Component unhandledGlobal
	//Set<String> handledAtoms = [] as Set
	//File latestFile

	String visit(Program p) {
		currentFile = createUniqueFile("out_", ".logic")
		results << new Result(Result.Kind.LOGIC, currentFile)

		// Transform program before visiting nodes
		def n = p.accept(new NormalizeVisitingActor())
				.accept(new InitVisitingActor())
				.accept(infoActor)
				.accept(new ValidationVisitingActor(infoActor))
				.accept(inferenceActor)

		return super.visit(n as Program)
	}

	/*String exit(Program n, Map<IVisitable, String> m) {
		n.accept(new PostOrderVisitor<IVisitable>(infoActor))
		globalAtoms = infoActor.declaringAtoms[n.globalComp].collect { it.name }

		// Check that all used predicates have a declaration/definition
		def allDeclAtoms = globalAtoms
		Map<String, Relation> allUsedAtoms = [:]
		n.comps.values().each {
			allDeclAtoms += infoActor.declaringAtoms[it].collect { it.name }
			allUsedAtoms << infoActor.getUsedAtoms(it)
		}
		allUsedAtoms.each { usedAtomName, usedAtom ->
			if (usedAtom.stage == "@past") return

			if (!(usedAtomName in allDeclAtoms))
				ErrorManager.warn(ErrorId.NO_DECL, usedAtomName)
		}

		// Compute dependency graph for components (and global predicates)
		def graph = new DependencyGraph(n)

		unhandledGlobal = new Component(n.globalComp)
		def currSet = [] as Set
		graph.getLayers().each { layer ->
			if (layer.any { it instanceof DependencyGraph.CmdNode }) {
				emit(n, m, currSet)
				emitCmd(n, m, layer)
				currSet = [] as Set
			} else
				currSet << layer
		}
		emit(n, m, currSet)

		return null
	}*/

	void enter(Component n) {
		inferenceActor.inferredTypes.each { predName, types ->
			def arity = infoActor.functionalRelations[predName]
			def body = types.withIndex().collect { type, i -> "${mapTypes(type)}(x$i)" }.join(", ")
			if (predName in infoActor.refmodeRelations) {
				emit "${types.last()}(x), $predName(x:y) -> ${types.first()}(y)."
			} else if (arity) {
				def head = (0..<(arity - 1)).collect { "x$it" }.join(", ")
				emit "$predName[$head] = x${arity - 1} -> $body."
			} else {
				def head = (0..<(types.size())).collect { "x$it" }.join(", ")
				emit "$predName($head) -> $body."
			}
		}
		emit ""
	}

	//String exit(Constraint n, Map<IVisitable, String> m) {
	//	"${m[n.head]} -> ${m[n.body]}."
	//}

	String exit(Declaration n, Map<IVisitable, String> m) {
		if (TYPE in n.annotations)
			emit "lang:entity(`${n.atom.name})."
		if (CONSTRUCTOR in n.annotations)
			emit "lang:constructor(`${n.atom.name})."
		null
	}

	String exit(Rule n, Map<IVisitable, String> m) {
		emit(n.body ? "${m[n.head]} <- ${m[n.body]}." : "${m[n.head]}.")
		null
	}

	String exit(AggregationElement n, Map<IVisitable, String> m) {
		def pred = n.predicate.name
		if (pred == "count" || pred == "min" || pred == "max")
			"agg<<${m[n.var]} = ${m[n.predicate]}>> ${m[n.body]}"
		else null
	}

	String exit(Constructor n, Map<IVisitable, String> m) {
		def constructor = exit(n as Functional, m)
		"$constructor, ${n.type.name}(${m[n.valueExpr]})"
	}

	String exit(Functional n, Map<IVisitable, String> m) {
		if (n.name in infoActor.refmodeRelations) {
			return "${n.name}(${m[n.valueExpr]}:${m[n.keyExprs.first()]})"
		} else {
			//def stage = ((n.stage == null || n.stage == "@past") ? "" : n.stage)
			def keyParams = n.keyExprs.collect { m[it] }.join(", ")
			return "${n.name}[$keyParams] = ${m[n.valueExpr]}"
		}
	}

	String exit(Predicate n, Map<IVisitable, String> m) {
		//def stage = ((n.stage == null || n.stage == "@past") ? "" : n.stage)
		def params = n.exprs.collect { m[it] }.join(", ")
		return "${n.name}($params)"
	}

	/*void emit(Program n, Map<IVisitable, String> m, Set<DependencyGraph.Node> nodes) {
		latestFile = createUniqueFile("out_", ".logic")
		results << new Result(Result.Kind.LOGIC, latestFile)

		def currSet = [] as Set
		nodes.each { node ->
			if (node instanceof DependencyGraph.CompNode) {
				def c = n.comps[node.name]
				List<String> l = []
				c.declarations.each { l << m[it] }
				c.constraints.each { l << m[it] }
				c.rules.each { l << m[it] }
				write(latestFile, l)
			} else if (node instanceof DependencyGraph.CmdNode)
				assert false
			else /* if (node instanceof PredNode)* / {
				handledAtoms << node.name
				currSet << node.name
			}
		}
		println handledAtoms

		handle(m, unhandledGlobal.declarations, latestFile)
		handle(m, unhandledGlobal.constraints, latestFile)
		handle(m, unhandledGlobal.rules, latestFile)
	}*/

	/*void emitCmd(Program n, Map<IVisitable, String> m, Set<DependencyGraph.Node> nodes) {
		assert nodes.size() == 1
		nodes.each { node ->
			assert node instanceof DependencyGraph.CmdNode
			assert latestFile != null
			def c = n.comps[node.name] as CmdComponent

			// Write frame rules from previous components
			c.rules.each { write(latestFile, m[it]) }

			latestFile = createUniqueFile("out_", "-export.logic")
			results << new Result(Result.Kind.EXPORT, latestFile)

			c.rules.each {
				assert it.head.elements.size() == 1
				def atom = it.head.elements.first() as Relation
				emitFilePredicate(atom, null, latestFile)
			}

			//for (Stub export : c.exports)
			//	write(_bashFile, "bloxbatch -db DB -keepDerivedPreds -exportCsv "+export.name+" -exportDataDir . -exportDelimiter '\\t'");

			results << new Result(c.eval)

			latestFile = createUniqueFile("out_", "-import.logic")
			results << new Result(Result.Kind.IMPORT, latestFile)

			c.declarations.each {
				def atom = infoActor.getDeclaringAtoms(it).values().first() as Relation
				emitFilePredicate(atom, it, latestFile)
			}
		}
	}*/

	/*void emitFilePredicate(Relation atom, Declaration d, File file) {
		def atomName = atom.name
		def vars = VariableExpr.genTempVars(atom.arity)

		def head = atomName + "(" + vars.collect { it.name }.join(', ') + ")"
		def body = (0..atom.arity - 1).collect { i ->
			(d != null ? d.types[i].name : "string") + "(" + vars[i].name + ")"
		}.join(', ')
		def decl = "_$head -> $body."
		def rule = (d != null) ? "+$head <- _$head." : "+_$head <- $head."

		write(file, [
				"lang:physical:storageModel[`_$atomName] = \"DelimitedFile\".",
				"lang:physical:filePath[`_$atomName] = \"${atomName}.facts\".",
				"lang:physical:delimiter[`$atomName] = \"\\t\".",
				"lang:physical:hasColumnNames[`_$atomName] = false.",
				decl,
				rule
		])
	}*/

	/*def <T extends IVisitable> void handle(Map<IVisitable, String> m, Set<T> set, File file) {
		Set<T> toRemove = []
		set.each {
			if (allHandledFor(it)) {
				write(file, m[it])
				toRemove << it
			}
		}
		toRemove.each { set.remove(it) }
	}

	boolean allHandledFor(IVisitable n) {
		Set<String> atoms = []
		infoActor.declaringAtoms[n].each { atoms << it.name }
		infoActor.usedAtoms[n].each { atoms << it.name }
		atoms.retainAll(globalAtoms)

		return atoms.every { handledAtoms.contains(it) }
	}*/

	static def mapTypes(def name) {
		// TODO clean this
		if (!name) throw new RuntimeException("********")
		if (name == "int") return "int[64]"
		return name
	}
}
