package org.clyze.deepdoop.datalog

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.clyze.deepdoop.datalog.Annotation.Kind
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.CmdComponent
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.component.Propagation
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.LogicalElement.LogicType
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.*
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceLocation
import org.clyze.deepdoop.system.SourceManager

import static org.clyze.deepdoop.datalog.DatalogParser.*

class DatalogListenerImpl extends DatalogBaseListener {

	def values = [:]
	def inDecl = false
	Component currComp
	Map<Kind, Annotation> pendingAnnotations = [:]

	def program = new Program()

	DatalogListenerImpl(String filename) {
		currComp = program.globalComp
		SourceManager.instance.outputFile = new File(filename).absolutePath
	}

	void enterComponent(ComponentContext ctx) {
		currComp = new Component(ctx.IDENTIFIER(0).text, ctx.IDENTIFIER(1)?.text)
	}

	void exitComponent(ComponentContext ctx) {
		values[ctx.identifierList()].each { String id ->
			program.add id, currComp.name
		}
		program.add currComp
		currComp = program.globalComp
	}

	void enterCmd(CmdContext ctx) {
		currComp = new CmdComponent(ctx.IDENTIFIER().text)
	}

	void exitCmd(CmdContext ctx) {
		values[ctx.identifierList()].each { String id ->
			program.add id, currComp.name
		}
		program.add currComp
		currComp = program.globalComp
	}

	void exitInitialize(InitializeContext ctx) {
		values[ctx.identifierList()].each { String id ->
			program.add id, ctx.IDENTIFIER().text
		}
	}

	void exitPropagate(PropagateContext ctx) {
		program.add new Propagation(
				ctx.IDENTIFIER(0).text,
				values[ctx.propagationList()] as Set,
				ctx.IDENTIFIER(1)?.text)
	}

	void enterDeclaration(DeclarationContext ctx) {
		inDecl = true
	}

	void exitDeclaration(DeclarationContext ctx) {
		inDecl = false

		def loc = rec(null, ctx)
		def annotations = gatherAnnotations(ctx.annotationList())
		annotations.keySet().each {
			if (it in pendingAnnotations) ErrorManager.warn(loc, ErrorId.ANNOTATION_MULTIPLE, it)
		}
		annotations += pendingAnnotations

		Declaration d
		if (ctx.relationName(0)) {
			def type = new Type(values[ctx.relationName(0)] as String)
			def supertype = ctx.relationName(1) ? [new Type(values[ctx.relationName(1)] as String)] : []
			d = new Declaration(type, supertype, annotations)
		} else {
			def rel = ctx.relation() ? values[ctx.relation()] as Relation : values[ctx.constructor()] as Constructor
			def types = values[ctx.relationNameList()].collect { new Type(it as String) }
			if (rel.exprs.size() != types.size())
				ErrorManager.error(loc, ErrorId.DECL_MALFORMED)
			d = new Declaration(rel, types, annotations)
		}
		currComp.declarations << d
		rec(d, ctx)
	}

	void enterAnnotationBlock(AnnotationBlockContext ctx) {
		pendingAnnotations = gatherAnnotations(ctx.annotationList())
	}

	void exitAnnotationBlock(AnnotationBlockContext ctx) {
		pendingAnnotations = [:]
	}

	void exitRule_(Rule_Context ctx) {
		Rule r
		def annotations = gatherAnnotations(ctx.annotationList())
		if (ctx.headList()) {
			def head = new LogicalElement(values[ctx.headList()] as List)
			def body = ctx.bodyList() ? values[ctx.bodyList()] as LogicalElement : null
			r = new Rule(head, body, annotations)
			currComp.rules << r
		} else {
			def head = new LogicalElement(values[ctx.relation()] as Relation)
			def body = new LogicalElement(values[ctx.aggregation()] as AggregationElement)
			r = new Rule(head, body)
			currComp.rules << r
		}
		rec(r, ctx)
	}

	void exitRelation(RelationContext ctx) {
		values[ctx] = new Relation(
				values[ctx.relationName()] as String,
				ctx.AT_STAGE()?.text,
				ctx.exprList() ? values[ctx.exprList()] as List : [])
		rec(values[ctx], ctx)
	}

	void exitConstructor(ConstructorContext ctx) {
		def name = values[ctx.relationName()] as String
		def exprs = (ctx.exprList() ? values[ctx.exprList()] as List : []) << values[ctx.expr()]
		values[ctx] = new Constructor(name, exprs)
		rec(values[ctx], ctx)
	}

	void exitConstruction(ConstructionContext ctx) {
		values[ctx] = new ConstructionElement(
				values[ctx.constructor()] as Constructor,
				new Type(values[ctx.relationName()] as String))
	}

	void exitAggregation(AggregationContext ctx) {
		values[ctx] = new AggregationElement(
				new VariableExpr(ctx.IDENTIFIER().text),
				values[ctx.relation()] as Relation,
				values[ctx.bodyList()] as LogicalElement)
		rec(values[ctx], ctx)
	}

	void exitBodyList(BodyListContext ctx) {
		if (ctx.relation())
			values[ctx] = new LogicalElement(values[ctx.relation()] as Relation)
		else if (ctx.constructor())
			values[ctx] = new LogicalElement(values[ctx.constructor()] as Relation)
		else if (ctx.comparison())
			values[ctx] = new LogicalElement(values[ctx.comparison()] as ComparisonElement)
		else if (hasToken(ctx, "!"))
			values[ctx] = new LogicalElement(new NegationElement(values[ctx.bodyList(0)] as IElement))
		else if (hasToken(ctx, "("))
			values[ctx] = new LogicalElement(new GroupElement(values[ctx.bodyList(0)] as IElement))
		else {
			def list = (0..1).collect { values[ctx.bodyList(it)] as IElement }
			def type = hasToken(ctx, ",") ? LogicType.AND : LogicType.OR
			values[ctx] = new LogicalElement(type, list)
		}
	}

	void enterLineMarker(LineMarkerContext ctx) {
		// Line number of the original file (emitted by C-Preprocessor)
		def markerLine = Integer.parseInt(ctx.INTEGER(0).text)
		// Actual line in the output file for this line marker
		def markerActualLine = ctx.start.getLine()
		// Name of the original file (emitted by C-Preprocessor)
		def sourceFile = ctx.STRING().text
		// Remove quotes from file values
		sourceFile = sourceFile.substring(1, sourceFile.length() - 1)

		// Ignore first line of output. It reports the values of the C-Preprocessed file
		if (markerActualLine == 1) return
		// Ignore lines for system info (e.g. <built-in> or /usr/include/stdc-predef.h)
		if (sourceFile.startsWith("<") || sourceFile.startsWith("/usr/include")) return

		def t = (ctx.INTEGER(1) != null ? Integer.parseInt(ctx.INTEGER(1).text) : 0)
		// 1 - Start of a new file
		if (t == 0 || t == 1)
			SourceManager.instance.lineMarkerStart(markerLine, markerActualLine, sourceFile)
		// 2 - Returning to previous file
		else if (t == 2)
			SourceManager.instance.lineMarkerEnd()
		// 3 - Following text comes from a system header file (#include <> vs #include "")
		// 4 - Following text should be treated as being wrapped in an implicit extern "C" block.
		else
			println "*** Weird line marker flag: $t ***"
	}

	void exitRelationName(RelationNameContext ctx) {
		def name = ctx.IDENTIFIER().text
		if (ctx.relationName())
			name = values[ctx.relationName()] + ":" + name
		values[ctx] = name
	}

	void exitConstant(ConstantContext ctx) {
		if (ctx.INTEGER()) {
			def str = ctx.INTEGER().text
			Long constant
			if (str.startsWith("0x") || str.startsWith("0X")) {
				str = str.substring(2)
				constant = Long.parseLong(str, 16)
			} else if (str.startsWith("0") && str.length() > 1) {
				str = str.substring(1)
				constant = Long.parseLong(str, 8)
			} else if (str.startsWith("2^")) {
				str = str.substring(2)
				constant = 1L << Integer.parseInt(str)
			} else {
				constant = Long.parseLong(str, 10)
			}
			values[ctx] = new ConstantExpr(constant)
		} else if (ctx.REAL()) values[ctx] = new ConstantExpr(Double.parseDouble(ctx.REAL().text))
		else if (ctx.BOOLEAN()) values[ctx] = new ConstantExpr(Boolean.parseBoolean(ctx.BOOLEAN().text))
		else if (ctx.STRING()) values[ctx] = new ConstantExpr(ctx.STRING().text)
	}

	void exitExpr(ExprContext ctx) {
		if (ctx.IDENTIFIER())
			values[ctx] = new VariableExpr(ctx.IDENTIFIER().text)
		else if (ctx.constant())
			values[ctx] = values[ctx.constant()] as ConstantExpr
		else if (hasToken(ctx, "("))
			values[ctx] = new GroupExpr(values[ctx.expr(0)] as IExpr)
		else {
			def e0 = values[ctx.expr(0)] as IExpr
			def e1 = values[ctx.expr(1)] as IExpr
			BinOperator op
			if (hasToken(ctx, "+")) op = BinOperator.PLUS
			else if (hasToken(ctx, "-")) op = BinOperator.MINUS
			else if (hasToken(ctx, "*")) op = BinOperator.MULT
			else op = BinOperator.DIV

			values[ctx] = new BinaryExpr(e0, op, e1)
		}
	}

	void exitComparison(ComparisonContext ctx) {
		def e0 = values[ctx.expr(0)] as IExpr
		def e1 = values[ctx.expr(1)] as IExpr
		BinOperator op
		if (hasToken(ctx, "=")) op = BinOperator.EQ
		else if (hasToken(ctx, "<")) op = BinOperator.LT
		else if (hasToken(ctx, "<=")) op = BinOperator.LEQ
		else if (hasToken(ctx, ">")) op = BinOperator.GT
		else if (hasToken(ctx, ">=")) op = BinOperator.GEQ
		else op = BinOperator.NEQ

		values[ctx] = new ComparisonElement(e0, op, e1)
	}

	void exitPropagationList(PropagationListContext ctx) {
		if (ctx.ALL()) values[ctx] = null
		else values[ctx] = (values[ctx.propagationList()] ?: []) << (values[ctx.relationName()] as String)
	}

	// Special handling (instead of using "exit")
	private Map<Kind, Annotation> gatherAnnotations(AnnotationListContext ctx) {
		if (!ctx) return [:]
		def valueMap = gatherValues(ctx.annotation().valueList())
		def annotation = new Annotation(ctx.annotation().IDENTIFIER().text, valueMap)
		def map = gatherAnnotations(ctx.annotationList())
		def loc = rec(annotation, ctx)
		if (annotation.kind in map) ErrorManager.warn(loc, ErrorId.ANNOTATION_MULTIPLE, annotation.kind)
		return map << [(annotation.kind): annotation]
	}

	void exitRelationNameList(RelationNameListContext ctx) {
		values[ctx] = ((values[ctx.relationNameList()] ?: []) as List) << values[ctx.relationName()]
	}

	void exitHeadList(HeadListContext ctx) {
		def t = ctx.relation() ? values[ctx.relation()] : values[ctx.construction()]
		values[ctx] = ((values[ctx.headList()] ?: []) as List) << t
	}

	void exitIdentifierList(IdentifierListContext ctx) {
		values[ctx] = ((values[ctx.identifierList()] ?: []) as List) << ctx.IDENTIFIER().text
	}

	// Special handling (instead of using "exit")
	private Map<String, ConstantExpr> gatherValues(ValueListContext ctx) {
		if (!ctx) return [:]
		exitConstant(ctx.value().constant())
		def constant = values[ctx.value().constant()] as ConstantExpr
		def value = [(ctx.value().IDENTIFIER().text): constant]
		return gatherValues(ctx.valueList()) << value
	}

	void exitExprList(ExprListContext ctx) {
		values[ctx] = ((values[ctx.exprList()] ?: []) as List) << values[ctx.expr()]
	}

	void visitErrorNode(ErrorNode node) {
		throw new RuntimeException("Parsing error")
	}

	static boolean hasToken(ParserRuleContext ctx, String token) {
		return ctx.children.find { it instanceof TerminalNode && it.text == token } as boolean
	}

	static SourceLocation rec(Object o, ParserRuleContext ctx) {
		SourceManager.instance.record(o, ctx.start.getLine())
	}
}
