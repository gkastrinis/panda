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
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.system.Result

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE

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
				.accept(inferenceActor)

		return super.visit(n as Program)
	}

	void enter(Program n) {
		emit "/// Declarations of normal relations"
		inferenceActor.inferredTypes.each { predName, types ->
			def functionalArity = infoActor.functionalRelations[predName]
			def body = types.withIndex().collect { type, i -> "${mapTypes(type)}(x$i)" }.join(", ")
			if (predName in infoActor.refmodeRelations) {
				emit "${types.last()}(x), $predName(x:y) -> ${types.first()}(y)."
			} else if (functionalArity) {
				def head = (0..<(functionalArity - 1)).collect { "x$it" }.join(", ")
				emit "$predName[$head] = x${functionalArity - 1} -> $body."
			} else if (!(predName in infoActor.allTypes)) {
				def head = (0..<(types.size())).collect { "x$it" }.join(", ")
				emit "$predName($head) -> $body."
			}
		}
		emit "/// Declarations of types"
		infoActor.allTypes.findAll { !infoActor.directSuperType[it] }.each { emit "$it(x) -> ." }
		infoActor.directSuperType.each { emit "${it.key}(x) -> ${it.value}(x)." }
		emit "/// Rules"
	}

	//String exit(Constraint n, Map<IVisitable, String> m) {
	//	"${m[n.head]} -> ${m[n.body]}."
	//}

	String exit(Declaration n, Map<IVisitable, String> m) {
		def name = n.atom.name
		if (TYPE in n.annotations) {
			emit "lang:entity(`$name)."
			emit """lang:physical:storageModel[`$name] = "ScalableSparse"."""
			def cap = n.annotations[TYPE].args["capacity"]
			if (cap) emit "lang:physical:capacity[`$name] = $cap."
		}
		if (CONSTRUCTOR in n.annotations && !(name in infoActor.refmodeRelations)) {
			emit "lang:constructor(`$name)."
		}
		null
	}

	String exit(Rule n, Map<IVisitable, String> m) {
		emit(n.body ? "${m[n.head]} <- ${m[n.body]}." : "${m[n.head]}.")
		null
	}

	String exit(AggregationElement n, Map<IVisitable, String> m) {
		def pred = n.predicate.name
		def lbPred = m[n.predicate].replaceFirst("sum", "total")
		if (pred == "count" || pred == "min" || pred == "max" || pred == "sum")
			"agg<<${m[n.var]} = $lbPred>> ${m[n.body]}"
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
			def keyParams = n.keyExprs.collect { m[it] }.join(", ")
			return "${n.name}[$keyParams] = ${m[n.valueExpr]}"
		}
	}

	String exit(Predicate n, Map<IVisitable, String> m) {
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
		infoActor.declaringAtoms[n].each { atoms << it.name }
		infoActor.usedAtoms[n].each { atoms << it.name }
		atoms.retainAll(globalAtoms)

		return atoms.every { handledAtoms.contains(it) }
	}*/

	static def mapTypes(def name) {
		name == "int" ? "int[64]" : name
	}
}
