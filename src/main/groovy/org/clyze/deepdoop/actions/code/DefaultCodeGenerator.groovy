package org.clyze.deepdoop.actions.code

import org.clyze.deepdoop.actions.*
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.GroupElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr
import org.clyze.deepdoop.system.Result

import java.nio.file.Files
import java.nio.file.Paths

import static org.clyze.deepdoop.datalog.element.LogicalElement.LogicType.AND

class DefaultCodeGenerator extends PostOrderVisitor<String> implements TDummyActor<String> {

	File outDir
	File currentFile

	TypeInfoVisitingActor typeInfoActor = new TypeInfoVisitingActor()
	RelationInfoVisitingActor relInfoActor = new RelationInfoVisitingActor()
	TypeInferenceVisitingActor typeInferenceActor = new TypeInferenceVisitingActor(typeInfoActor, relInfoActor)
	ConstructionInfoVisitingActor constructionInfoActor = new ConstructionInfoVisitingActor()

	List<Result> results = []

	DefaultCodeGenerator(File outDir) {
		actor = this
		this.outDir = outDir
	}

	DefaultCodeGenerator(String outDir) { this(new File(outDir)) }

	//String exit(Program n, Map m) { null }

	//String exit(CmdComponent n, Map m) { null }

	//String exit(Component n, Map m) { null }

	//String exit(RelDeclaration n, Map m) { null }

	//String exit(TypeDeclaration n, Map m) { null }

	//String exit(Rule n, Map m) { null }

	//String exit(AggregationElement n, Map m) { null }

	String exit(ComparisonElement n, Map m) { m[n.expr] }

	//String exit(ConstructorElement n, Map m) { null }

	String exit(GroupElement n, Map m) { "(${m[n.element]})" }

	String exit(LogicalElement n, Map m) {
		n.elements.findAll { m[it] }.collect { m[it] }.join(n.type == AND ? ", " : "; ")
	}

	String exit(NegationElement n, Map m) { "!${m[n.element]}" }

	//String exit(Relation n, Map m) { null }

	//String exit(Constructor n, Map m) { null }

	//String exit(Type n, Map m) { null }

	String exit(BinaryExpr n, Map m) { "${m[n.left]} ${n.op} ${m[n.right]}" }

	String exit(ConstantExpr n, Map m) { n.value as String }

	String exit(GroupExpr n, Map m) { "(${m[n.expr]})" }

	String exit(VariableExpr n, Map m) { n.name }

	protected def createUniqueFile(String prefix, String suffix) {
		Files.createTempFile(Paths.get(outDir.name), prefix, suffix).toFile()
	}

	protected void emit(String data) { write currentFile, data }

	protected static void write(File file, String data) { file << data << "\n" }
}
