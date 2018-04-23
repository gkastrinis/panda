package org.codesimius.panda.actions.code

import groovy.transform.InheritConstructors
import org.codesimius.panda.actions.graph.DependencyGraphVisitor
import org.codesimius.panda.actions.tranform.*
import org.codesimius.panda.actions.validation.MainValidator
import org.codesimius.panda.actions.validation.PreliminaryValidator
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.AggregationElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.system.Result

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1
import static org.codesimius.panda.datalog.expr.VariableExpr.genN as varN

@InheritConstructors
class LBCodeGenerator extends DefaultCodeGenerator {

	Set<String> functionalRelations

	String visit(BlockLvl2 p) {
		createUniqueFile("out_", ".logic")
		results << new Result(Result.Kind.LOGIC, currentFile)

		// Transform program before visiting nodes
		def n = p.accept(new PreliminaryValidator())
				.accept(new SyntaxFlatteningTransformer())
				.accept(new ComponentInstantiationTransformer())
				.accept(new DependencyGraphVisitor(outDir))
				.accept(new ComponentFlatteningTransformer())
				.accept(symbolTable.typeInfo)
				.accept(new TypesTransformer(symbolTable))
				.accept(new InputFactsTransformer(symbolTable))
				.accept(symbolTable.relationInfo)
				.accept(symbolTable.varInfo)
				.accept(typeInferenceTransformer)
				.accept(new MainValidator(symbolTable))
				.accept(new TypesOptimizer(symbolTable))

		functionalRelations = n.datalog.relDeclarations
				.findAll { FUNCTIONAL in it.annotations }
				.collect { it.relation.name } as Set

		super.visit(n)
	}

	String visit(RelDeclaration n) {
		def name = n.relation.name
		def types = n.types.withIndex().collect { t, int i -> "${t.name == "int" ? "int[64]" : t.name}(${var1(i)})" }
		n.relation.exprs = varN(n.types.size())
		emit "${handleRelation(n.relation)} -> ${types.join(", ")}."

		if (CONSTRUCTOR in n.annotations) emit "lang:constructor(`$name)."
		null
	}

	String visit(TypeDeclaration n) {
		def name = n.type.name
		emit "lang:entity(`$name)."
		emit """lang:physical:storageModel[`$name] = "ScalableSparse"."""
		def cap = n.annotations.find { it == TYPE }.args["capacity"]
		if (cap) emit "lang:physical:capacity[`$name] = $cap."
		if (n.supertype) emit "$name(${var1()}) -> ${n.supertype.name}(${var1()})."
		null
	}

	String exit(Rule n) { emit "${m[n.head]}${n.body ? " <- ${m[n.body]}" : ""}." }

	String exit(AggregationElement n) {
		def pred = n.relation.name
		def params = n.relation.exprs ? "${m[n.relation.exprs.first()]}" : ""
		def lbPred = "${pred.replaceFirst("sum", "total")}($params)"
		"agg<<${m[n.var]} = $lbPred>> ${m[n.body]}"
	}

	String exit(Constructor n) { handleRelation n }

	String exit(Relation n) { handleRelation n }

	def handleRelation(Relation n) {
		if (n instanceof Constructor || n.name in functionalRelations)
			"${n.name}[${n.exprs.dropRight(1).collect { m[it] }.join(", ")}] = ${m[n.exprs.last()]}"
		else
			"${n.name}(${n.exprs.collect { m[it] }.join(", ")})"
	}

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
				def relation = it.head.elements.first() as Relation
				emitFilePredicate(relation, null, latestFile)
			}

			//for (Stub export : c.exports)
			//	write(_bashFile, "bloxbatch -db DB -keepDerivedPreds -exportCsv "+export.name+" -exportDataDir . -exportDelimiter '\\t'");

			results << new Result(c.eval)

			latestFile = createUniqueFile("out_", "-import.logic")
			results << new Result(Result.Kind.IMPORT, latestFile)

			c.declarations.each {
				def relation = constructionInfoActor.getDeclaringAtoms(it).args().first() as Relation
				emitFilePredicate(relation, it, latestFile)
			}
		}
	}*/

	/*void emitFilePredicate(Relation relation, Declaration d, File file) {
		def atomName = relation.name
		def vars = VariableExpr.genTempVars(relation.arity)

		def head = atomName + "(" + vars.collect { it.name }.join(', ') + ")"
		def body = (0..relation.arity - 1).collect { i ->
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
		constructionInfoActor.declaredRelations[n].each { atoms << it.name }
		constructionInfoActor.usedRelations[n].each { atoms << it.name }
		atoms.retainAll(globalAtoms)

		return atoms.every { handledAtoms.contains(it) }
	}*/
}
