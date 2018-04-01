package org.clyze.deepdoop.actions.code

import org.clyze.deepdoop.actions.DefaultVisitor
import org.clyze.deepdoop.actions.SymbolTableVisitingActor
import org.clyze.deepdoop.actions.tranform.TypeInferenceTransformer
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Result

import java.nio.file.Files
import java.nio.file.Paths

import static org.clyze.deepdoop.datalog.element.ComparisonElement.TRIVIALLY_TRUE
import static org.clyze.deepdoop.datalog.element.LogicalElement.LogicType.AND

class DefaultCodeGenerator extends DefaultVisitor<String> {

	SymbolTableVisitingActor symbolTable = new SymbolTableVisitingActor()
	TypeInferenceTransformer typeInferenceTransformer = new TypeInferenceTransformer(symbolTable)
	List<Result> results = []

	private File outDir
	private File currentFile
	private FileWriter fw
	private List<LogicalElement.LogicType> logicTypes = []

	DefaultCodeGenerator(File outDir) { this.outDir = outDir }

	String exit(BlockLvl2 n) { fw.close(); null }

	String exit(ComparisonElement n) { n == TRIVIALLY_TRUE ? "true" : m[n.expr] }

	String exit(ConstructionElement n) { "${m[n.constructor]}, ${m[n.type]}(${m[n.constructor.valueExpr]})" }

	String exit(GroupElement n) { "(${m[n.element]})" }

	void enter(LogicalElement n) { logicTypes << n.type }

	String exit(LogicalElement n) {
		logicTypes = logicTypes.dropRight(1)
		def str = n.elements.findAll { m[it] }.collect { m[it] }.join(n.type == AND ? ", " : "; ")
		(logicTypes && logicTypes.last() != n.type) ? "($str)" : str
	}

	String exit(NegationElement n) { "!${m[n.element]}" }

	String exit(BinaryExpr n) { "${m[n.left]} ${n.op} ${m[n.right]}" }

	String exit(ConstantExpr n) { n.type == ConstantExpr.Type.STRING ? "\"${n.value}\"" : "${n.value}" }

	String exit(GroupExpr n) { "(${m[n.expr]})" }

	String exit(VariableExpr n) { n.name }

	void createUniqueFile(String prefix, String suffix) {
		currentFile = Files.createTempFile(Paths.get(outDir.name), prefix, suffix).toFile()
		if (fw) fw.close()
		fw = new FileWriter(currentFile)
	}

	File getCurrentFile() { currentFile }

	void emit(String data) { fw.write "$data\n" }
}
