package org.clyze.deepdoop.actions.code

import org.clyze.deepdoop.actions.*
import org.clyze.deepdoop.datalog.BinOperator
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.GroupElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.element.relation.Primitive
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Result

import java.nio.file.Files
import java.nio.file.Paths

import static org.clyze.deepdoop.datalog.element.LogicalElement.LogicType.AND

class DefaultCodeGenerator extends PostOrderVisitor<String> implements IActor<String>, TDummyActor<String> {

	File outDir
	File currentFile

	InfoCollectionVisitingActor infoActor = new InfoCollectionVisitingActor()
	TypeInferenceVisitingActor typeInferenceActor = new TypeInferenceVisitingActor(infoActor)
	List<Result> results = []

	DefaultCodeGenerator(File outDir) {
		actor = this
		this.outDir = outDir
	}

	DefaultCodeGenerator(String outDir) { this(new File(outDir)) }

	//String exit(Program n, Map<IVisitable, String> m) { null }

	//String exit(CmdComponent n, Map<IVisitable, String> m) { null }

	//String exit(Component n, Map<IVisitable, String> m) { null }

	//String exit(Declaration n, Map<IVisitable, String> m) { null }

	//String exit(Rule n, Map<IVisitable, String> m) { null }

	//String exit(AggregationElement n, Map<IVisitable, String> m) { null }

	String exit(ComparisonElement n, Map<IVisitable, String> m) { m[n.expr] }

	String exit(GroupElement n, Map<IVisitable, String> m) { "(${m[n.element]})" }

	String exit(LogicalElement n, Map<IVisitable, String> m) {
		n.elements.findAll { m[it] }.collect { m[it] }.join(n.type == AND ? ", " : "; ")
	}

	String exit(NegationElement n, Map<IVisitable, String> m) { "!${m[n.element]}" }

	//String exit(Relation n, Map<IVisitable, String> m) { null }

	//String exit(Constructor n, Map<IVisitable, String> m) { null }

	//String exit(Type n, Map<IVisitable, String> m) { null }

	//String exit(Functional n, Map<IVisitable, String> m) { null }

	//String exit(Predicate n, Map<IVisitable, String> m) { null }

	String exit(Primitive n, Map<IVisitable, String> m) { "${n.name}(${m[n.var]})" }

	String exit(BinaryExpr n, Map<IVisitable, String> m) { "${m[n.left]} ${mapOp(n.op)} ${m[n.right]}" }

	String exit(ConstantExpr n, Map<IVisitable, String> m) { n.value as String }

	String exit(GroupExpr n, Map<IVisitable, String> m) { "(${m[n.expr]})" }

	String exit(VariableExpr n, Map<IVisitable, String> m) { n.name }

	protected def createUniqueFile(String prefix, String suffix) {
		Files.createTempFile(Paths.get(outDir.name), prefix, suffix).toFile()
	}

	protected void emit(String data) { write currentFile, data }

	protected static void write(File file, String data) { file << data << "\n" }

	static def mapOp(def op) { op == BinOperator.ASGN ? '=' : op as String }
}
