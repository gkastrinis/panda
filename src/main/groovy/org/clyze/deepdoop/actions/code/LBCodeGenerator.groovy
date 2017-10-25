package org.clyze.deepdoop.actions.code

import groovy.transform.InheritConstructors
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.ValidationVisitingActor
import org.clyze.deepdoop.actions.tranform.InitializingTransformer
import org.clyze.deepdoop.actions.tranform.NormalizingTransformer
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.AggregationElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

@InheritConstructors
class LBCodeGenerator extends DefaultCodeGenerator {

	String visit(Program p) {
		currentFile = createUniqueFile("out_", ".logic")
		results << new Result(Result.Kind.LOGIC, currentFile)

		// Transform program before visiting nodes
		def n = p.accept(new NormalizingTransformer())
				.accept(new InitializingTransformer())
				.accept(infoActor)
				.accept(new ValidationVisitingActor(infoActor))
				.accept(typeInferenceActor)

		return super.visit(n as Program)
	}

	String exit(Declaration n, Map m) {
		def name = n.atom.name
		if (TYPE in n.annotations) {
			emit "lang:entity(`$name)."
			emit """lang:physical:storageModel[`$name] = "ScalableSparse"."""
			def cap = n.annotations[TYPE].args["capacity"]
			if (cap) emit "lang:physical:capacity[`$name] = $cap."
		} else {
			def headVars = varN(n.types.size()).join(", ")
			def types = n.types.withIndex().collect { Type t, int i -> "${map(t.name)}(${var1(i)})" }.join(", ")
			emit "${n.atom.name}($headVars) -> $types."
		}
		if (CONSTRUCTOR in n.annotations && !(name in infoActor.refmodeRelations)) {
			emit "lang:constructor(`$name)."
		}
		null
	}

	String exit(Rule n, Map m) {
		emit(n.body ? "${m[n.head]} <- ${m[n.body]}." : "${m[n.head]}.")
		null
	}

	String exit(AggregationElement n, Map m) {
		def pred = n.relation.name
		def params = n.relation.exprs ? "${m[n.relation.exprs.first()]}" : ""
		def lbPred = "${pred.replaceFirst("sum", "total")}($params)"
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"agg<<${m[n.var]} = $lbPred>> ${m[n.body]}"
		else null
	}

	String exit(ConstructionElement n, Map m) {
		"${m[n.constructor]}, ${m[n.type]}"
	}

	String exit(Constructor n, Map m) { exit0(n, m) }

	String exit(Relation n, Map m) { exit0(n, m) }

	String exit0(Relation n, Map m) {
		if (n.name in infoActor.functionalRelations) {
			def keyExprs = n.exprs.dropRight(1)
			def valueExpr = n.exprs.last()
			"${n.name}[${keyExprs.collect { m[it] }.join(", ")}] = ${m[valueExpr]}"
		}
		else
			"${n.name}(${n.exprs.collect { m[it] }.join(", ")})"
	}

	String exit(Type n, Map m) { exit0(n, m) }

	/*void emit(Program n, Map m, Set<DependencyGraph.Node> nodes) {
		latestFile = createUniqueFile("out_", ".logic")
		results << new Result(Result.Kind.LOGIC, latestFile)

		def currSet = [] as Set
		nodes.each { node ->
			if (node instanceof DependencyGraph.CompNode) {
				def c = n.comps[node.name]
				List<String> l = []
				c.declarations.each { l << m[it] }
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
				def atom = infoActor.getDeclaringAtoms(it).args().first() as Relation
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
		infoActor.declaredRelations[n].each { atoms << it.name }
		infoActor.usedRelations[n].each { atoms << it.name }
		atoms.retainAll(globalAtoms)

		return atoms.every { handledAtoms.contains(it) }
	}*/

	static def map(def name) { name == "int" ? "int[64]" : name }
}
