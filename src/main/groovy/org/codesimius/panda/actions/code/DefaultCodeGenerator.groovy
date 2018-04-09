package org.codesimius.panda.actions.code

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.actions.RelationInfoVisitor
import org.codesimius.panda.actions.VarInfoVisitor
import org.codesimius.panda.actions.tranform.TypeInferenceTransformer
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.datalog.expr.GroupExpr
import org.codesimius.panda.datalog.expr.VariableExpr
import org.codesimius.panda.system.Result

import java.nio.file.Files
import java.nio.file.Paths

import static org.codesimius.panda.datalog.element.ComparisonElement.TRIVIALLY_TRUE

class DefaultCodeGenerator extends DefaultVisitor<String> {

	RelationInfoVisitor relationInfo = new RelationInfoVisitor()
	VarInfoVisitor varInfo = new VarInfoVisitor()
	TypeInferenceTransformer typeInferenceTransformer = new TypeInferenceTransformer(relationInfo)
	List<Result> results = []

	File outDir
	private File currentFile
	private FileWriter fw
	// Keep track of active logical and negation elements in order to group them correctly
	private List<Integer> complexElements = []

	DefaultCodeGenerator(File outDir) {
		this.outDir = outDir
		outDir.mkdirs()
	}

	DefaultCodeGenerator(String outDir) { this(new File(outDir)) }

	String exit(BlockLvl2 n) { fw.close(); null }

	String exit(ComparisonElement n) { n == TRIVIALLY_TRUE ? "true" : m[n.expr] }

	String exit(ConstructionElement n) { "${m[n.constructor]}, ${m[n.type]}(${m[n.constructor.valueExpr]})" }

	String exit(GroupElement n) { "(${m[n.element]})" }

	void enter(LogicalElement n) { complexElements << map(n) }

	String exit(LogicalElement n) {
		complexElements = complexElements.dropRight(1)
		def str = n.elements.findAll { m[it] }.collect { m[it] }.join(n.kind == LogicalElement.Kind.AND ? ", " : "; ")
		(complexElements && complexElements.last() != map(n)) ? "($str)" : str
	}

	void enter(NegationElement n) { complexElements << map(n) }

	String exit(NegationElement n) {
		complexElements = complexElements.dropRight(1)
		"!${m[n.element]}"
	}

	String exit(BinaryExpr n) { "${m[n.left]} ${n.op} ${m[n.right]}" }

	String exit(ConstantExpr n) { n.kind == ConstantExpr.Kind.STRING ? "\"${n.value}\"" : "${n.value}" }

	String exit(GroupExpr n) { "(${m[n.expr]})" }

	String exit(VariableExpr n) { n.name }

	void createUniqueFile(String prefix, String suffix) {
		currentFile = Files.createTempFile(Paths.get(outDir.path), prefix, suffix).toFile()
		if (fw) fw.close()
		fw = new FileWriter(currentFile)
	}

	File getCurrentFile() { currentFile }

	void emit(String data) { fw.write "$data\n" }

	private static int map(LogicalElement n) { n.kind == LogicalElement.Kind.AND ? 0 : 1 }

	private static int map(NegationElement n) { 2 }
}
