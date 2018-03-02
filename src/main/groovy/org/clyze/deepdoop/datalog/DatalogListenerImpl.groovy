package org.clyze.deepdoop.datalog

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.block.Instantiation
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
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

import static org.clyze.deepdoop.datalog.Annotation.TYPE
import static org.clyze.deepdoop.datalog.DatalogParser.*

class DatalogListenerImpl extends DatalogBaseListener {

	BlockLvl2 program
	private BlockLvl0 currDatalog
	// Relation Name x Annotations
	private Map<String, Set<Annotation>> globalPendingAnnotations
	private Map<String, Set<Annotation>> currPendingAnnotations
	// Extra annotations from an annotation block
	private Set<Annotation> extraAnnotations = [] as Set
	private def inDecl = false
	private def values = [:]

	DatalogListenerImpl(String filename) {
		SourceManager.instance.outputFile = new File(filename).absolutePath
	}

	void enterProgram(ProgramContext ctx) {
		program = new BlockLvl2()
		currDatalog = program.datalog
		currPendingAnnotations = globalPendingAnnotations = [:].withDefault { [] as Set }
	}

	void exitProgram(ProgramContext ctx) {
		currPendingAnnotations.each {
			currDatalog.relDeclarations << new RelDeclaration(new Relation(it.key), [], it.value)
		}
	}

	void enterComponent(ComponentContext ctx) {
		currDatalog = new BlockLvl0()
		currPendingAnnotations = [:].withDefault { [] as Set }
	}

	void exitComponent(ComponentContext ctx) {
		def name = ctx.IDENTIFIER().text
		def superName = ctx.superComponent()?.IDENTIFIER()?.text
		if (program.components.any { it.name == name })
			ErrorManager.error(ErrorId.COMP_ID_IN_USE, name)

		def parameters = values[ctx.parameterList()] as List ?: []
		if (parameters.size() != parameters.toSet().size())
			ErrorManager.error(ErrorId.COMP_DUPLICATE_PARAMS, parameters, name)

		def superParameters = ctx.superComponent()?.parameterList() ? values[ctx.superComponent().parameterList()] as List : []
		if (superParameters.any { !(it in parameters) })
			ErrorManager.error(ErrorId.COMP_SUPER_PARAM_MISMATCH, superParameters, superName)

		def component = new BlockLvl1(name, superName, parameters, superParameters, currDatalog)
		program.components << component

		values[ctx.identifierList()].each { String id ->
			if (program.instantiations.any { it.id == id })
				ErrorManager.error(ErrorId.ID_IN_USE, id)
			program.instantiations << new Instantiation(name, id, [])
		}

		currPendingAnnotations.each {
			currDatalog.relDeclarations << new RelDeclaration(new Relation(it.key), [], it.value)
		}
		currPendingAnnotations = globalPendingAnnotations
		currDatalog = program.datalog
	}

//	void enterCmd(CmdContext ctx) {
//		currComp = new CmdComponent(ctx.IDENTIFIER().text)
//	}
//
//	void exitCmd(CmdContext ctx) {
//		values[ctx.identifierList()].each { String id ->
//			program.add id, currComp.name
//		}
//		program.add currComp
//		currComp = program.globalComp
//	}

	void exitInstantiate(InstantiateContext ctx) {
		def parameters = values[ctx.parameterList()] as List ?: []
		values[ctx.identifierList()].each { String id ->
			if (program.instantiations.any { it.id == id })
				ErrorManager.error(ErrorId.ID_IN_USE, id)
			program.instantiations << new Instantiation(ctx.IDENTIFIER().text, id, parameters)
		}
	}

	void enterAnnotationBlock(AnnotationBlockContext ctx) {
		extraAnnotations = gatherAnnotations(ctx.annotationList())
	}

	void exitAnnotationBlock(AnnotationBlockContext ctx) {
		extraAnnotations = [] as Set
	}

	void enterDeclaration(DeclarationContext ctx) {
		inDecl = true
	}

	void exitDeclaration(DeclarationContext ctx) {
		inDecl = false

		def loc = rec(null, ctx)
		def annotations = gatherAnnotations(ctx.annotationList())
//		annotations.keySet().each {
//			if (it in extraAnnotations) ErrorManager.warn(loc, ErrorId.ANNOTATION_MULTIPLE, it)
//		}
		annotations += extraAnnotations

		// Type declaration
		if (ctx.IDENTIFIER(0) && TYPE in annotations) {
			def type = new Type(ctx.IDENTIFIER(0).text)
			def supertype = ctx.IDENTIFIER(1) ? new Type(ctx.IDENTIFIER(1).text) : null
			// Initial values are of the form `key(value)`. E.g., PUBLIC('public')
			// Keys are used to generate singleton relations. E.g., Modifier:PUBLIC(x)
			if (ctx.initValueList())
				annotations << new Annotation("TYPEVALUES", values[ctx.initValueList()] as Map)
			def d = new TypeDeclaration(type, supertype, annotations)
			currDatalog.typeDeclarations << d
			rec(d, ctx)
		}
		// Incomplete declaration of the form: `@output Foo`
		// i.e. binds annotations with a relation name without
		// providing any details
		else if (ctx.IDENTIFIER(0)) {
			currPendingAnnotations[ctx.IDENTIFIER(0).text] += annotations
		}
		// Full declaration of a relation
		else {
			def rel = ctx.relation() ? values[ctx.relation()] as Relation : values[ctx.constructor()] as Constructor
			def types = values[ctx.identifierList()].collect { new Type(it as String) }
			if (rel.exprs.size() != types.size())
				ErrorManager.error(loc, ErrorId.DECL_MALFORMED)
			def d = new RelDeclaration(rel, types, annotations)
			currDatalog.relDeclarations << d
			rec(d, ctx)
		}
	}

	void exitRule_(Rule_Context ctx) {
		Rule r
		if (ctx.headList()) {
			def annotations = gatherAnnotations(ctx.annotationList())
			def head = new LogicalElement(values[ctx.headList()] as List)
			def body = values[ctx.bodyList()] as LogicalElement
			//def body = ctx.bodyList() ? values[ctx.bodyList()] as LogicalElement : null
			r = new Rule(head, body, annotations)
		}
		// Aggregation
		else {
			def head = new LogicalElement(values[ctx.relation()] as Relation)
			def body = new LogicalElement(values[ctx.aggregation()] as AggregationElement)
			r = new Rule(head, body)
		}
		currDatalog.rules << r
		rec(r, ctx)
	}

	void exitRelation(RelationContext ctx) {
		def name = ctx.IDENTIFIER(0).text
		if (inDecl && ctx.IDENTIFIER(1))
			ErrorManager.error(ErrorId.REL_EXT_INVALID)
		def at = ctx.IDENTIFIER(1) ? "@${ctx.IDENTIFIER(1).text}" : ""
		def exprs = ctx.exprList() ? values[ctx.exprList()] as List : []
		values[ctx] = new Relation("$name$at" as String, exprs)
		rec(values[ctx], ctx)
	}

	void exitConstructor(ConstructorContext ctx) {
		def name = ctx.IDENTIFIER().text
		def exprs = (ctx.exprList() ? values[ctx.exprList()] as List : []) << values[ctx.expr()]
		values[ctx] = new Constructor(name, exprs)
		rec(values[ctx], ctx)
	}

	void exitConstruction(ConstructionContext ctx) {
		def cons = values[ctx.constructor()] as Constructor
		values[ctx] = new ConstructionElement(cons, new Type(ctx.IDENTIFIER().text))
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

	void exitConstant(ConstantContext ctx) {
		if (ctx.INTEGER()) {
			def str = ctx.INTEGER().text
			def sign = 1L
			if (str.startsWith("-")) {
				sign = -1L
				str = str[1..-1]
			}
			if (str.startsWith("0x") || str.startsWith("0X"))
				values[ctx] = new ConstantExpr(sign * Long.parseLong(str[2..-1], 16))
			else if (str.startsWith("0") && str.length() > 1)
				values[ctx] = new ConstantExpr(sign * Long.parseLong(str[1..-1], 8))
			else if (str.startsWith("2^"))
				values[ctx] = new ConstantExpr(sign * (1L << Integer.parseInt(str[2..-1])))
			else
				values[ctx] = new ConstantExpr(sign * Long.parseLong(str, 10))
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
			BinaryOp op
			if (hasToken(ctx, "+")) op = BinaryOp.PLUS
			else if (hasToken(ctx, "-")) op = BinaryOp.MINUS
			else if (hasToken(ctx, "*")) op = BinaryOp.MULT
			else op = BinaryOp.DIV

			values[ctx] = new BinaryExpr(e0, op, e1)
		}
	}

	void exitInitValue(InitValueContext ctx) {
		values[ctx] = [(ctx.IDENTIFIER().text): values[ctx.constant()]]
	}

	void exitComparison(ComparisonContext ctx) {
		def e0 = values[ctx.expr(0)] as IExpr
		def e1 = values[ctx.expr(1)] as IExpr
		BinaryOp op
		if (hasToken(ctx, "=")) op = BinaryOp.EQ
		else if (hasToken(ctx, "<")) op = BinaryOp.LT
		else if (hasToken(ctx, "<=")) op = BinaryOp.LEQ
		else if (hasToken(ctx, ">")) op = BinaryOp.GT
		else if (hasToken(ctx, ">=")) op = BinaryOp.GEQ
		else op = BinaryOp.NEQ

		values[ctx] = new ComparisonElement(e0, op, e1)
	}

	void enterLineMarker(LineMarkerContext ctx) {
		// Line number of the original file (emitted by C-Preprocessor)
		def markerLine = Integer.parseInt(ctx.INTEGER(0).text)
		// Actual line in the output file for this line marker
		def markerActualLine = ctx.start.getLine()
		// Name of the original file (emitted by C-Preprocessor)
		def sourceFile = ctx.STRING().text
		// Remove quotes from file values
		sourceFile = sourceFile[1..-2]

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

	// Special handling (instead of using "exit")
	private Set<Annotation> gatherAnnotations(AnnotationListContext ctx) {
		if (!ctx) return [] as Set
		def valueMap = gatherValues(ctx.annotation().valueList())
		def annotation = new Annotation(ctx.annotation().IDENTIFIER().text, valueMap)
		def set = gatherAnnotations(ctx.annotationList())
		//def loc = rec(annotation, ctx)
		//if (annotation.kind in set) ErrorManager.warn(loc, ErrorId.ANNOTATION_MULTIPLE, annotation.kind)
		return set << annotation
	}

	void exitInitValueList(InitValueListContext ctx) {
		values[ctx] = ((values[ctx.initValueList()] ?: [:]) as Map) << (values[ctx.initValue()] as Map)
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

	void exitParameterList(ParameterListContext ctx) {
		values[ctx] = values[ctx.identifierList()]
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
