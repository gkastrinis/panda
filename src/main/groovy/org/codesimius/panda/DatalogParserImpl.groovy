package org.codesimius.panda

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.DatalogBaseListener
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.block.Instantiation
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.*
import org.codesimius.panda.system.Error
import org.codesimius.panda.system.SourceManager

import static org.codesimius.panda.datalog.DatalogParser.*
import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.Error.warn

class DatalogParserImpl extends DatalogBaseListener {

	BlockLvl2 program
	BlockLvl0 currDatalog
	// Relation Name x Annotations
	Map<String, Set<Annotation>> globalPendingAnnotations
	Map<String, Set<Annotation>> currPendingAnnotations
	// Extra annotations from annotation blocks
	Stack<Set<Annotation>> extraAnnotationsStack = []
	Stack<String> activeNamespaces = []
	def values = [:]

	DatalogParserImpl(String filename) {
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
		def parameters = values[ctx.parameterList()] as List ?: []
		def superParameters = ctx.superComponent()?.parameterList() ? values[ctx.superComponent().parameterList()] as List : []

		if (componentIDs.any { it == name })
			error(Error.COMP_ID_IN_USE, name)

		if (parameters.size() != parameters.toSet().size())
			error(Error.COMP_DUPLICATE_PARAMS, parameters, name)

		if (superParameters.any { !(it in parameters) })
			error(Error.COMP_SUPER_PARAM_MISMATCH, superParameters, parameters, superName)

		program.components << new BlockLvl1(name, superName, parameters, superParameters, currDatalog)

		values[ctx.identifierList()].each { String id ->
			if (componentIDs.any { it == id }) error(Error.INST_ID_IN_USE, id)
			program.instantiations << new Instantiation(name, id, [])
		}

		currPendingAnnotations.each {
			currDatalog.relDeclarations << new RelDeclaration(new Relation(it.key), [], it.value)
		}
		currPendingAnnotations = globalPendingAnnotations
		currDatalog = program.datalog
	}

	void exitInstantiation(InstantiationContext ctx) {
		def parameters = values[ctx.parameterList()] as List ?: []
		values[ctx.identifierList()].each { String id ->
			if (componentIDs.any { it == id }) error(Error.INST_ID_IN_USE, id)
			program.instantiations << new Instantiation(ctx.IDENTIFIER().text, id, parameters)
		}
	}

	void enterAnnotationBlock(AnnotationBlockContext ctx) {
		def annotations = gatherAnnotations(ctx.annotationList())
		if (Annotation.NAMESPACE in annotations) {
			activeNamespaces.push(annotations.find { it == Annotation.NAMESPACE }.args["v"] as String)
			annotations.remove Annotation.NAMESPACE
		}
		extraAnnotationsStack.push annotations
	}

	void exitAnnotationBlock(AnnotationBlockContext ctx) {
		if (Annotation.NAMESPACE in extraAnnotationsStack.peek()) activeNamespaces.pop()
		extraAnnotationsStack.pop()
	}

	void exitDeclaration(DeclarationContext ctx) {
		def loc = rec(null, ctx)
		def annotations = gatherAnnotations(ctx.annotationList())
		if (Annotation.NAMESPACE in annotations) error(loc, Error.ANNOTATION_BLOCK_ONLY, Annotation.NAMESPACE)
		def extraAnnotations = extraAnnotationsStack.flatten() as Set<Annotation>
		annotations.findAll { it in extraAnnotations }.each { warn(loc, Error.ANNOTATION_MULTIPLE, it) }
		annotations += extraAnnotations

		// Type declaration
		if (ctx.IDENTIFIER(0) && Annotation.TYPE in annotations) {
			def type = new Type(suffix(ctx.IDENTIFIER(0).text))
			def supertype = ctx.IDENTIFIER(1) ? new Type(suffix(ctx.IDENTIFIER(1).text)) : null
			// Initial values are of the form `key(value)`. E.g., PUBLIC('public')
			// Keys are used to generate singleton relations. E.g., Modifier:PUBLIC(x)
			if (ctx.initValueList())
				annotations << new Annotation("TYPEVALUES", values[ctx.initValueList()] as Map)
			def d = new TypeDeclaration(type, supertype, annotations)
			currDatalog.typeDeclarations << d
			rec(d, ctx)
		}
		// Incomplete declaration of the form: `@output Foo`
		// i.e. binds annotations with a relation name without \providing any details
		else if (ctx.IDENTIFIER(0)) {
			currPendingAnnotations[ctx.IDENTIFIER(0).text] += annotations
		}
		// Full declaration of a relation
		else {
			def rel = ctx.relation() ? values[ctx.relation()] as Relation : values[ctx.constructor()] as Constructor
			def types = values[ctx.identifierList()].collect { new Type(suffix(it as String)) }
			if (rel.exprs.any { rel.exprs.count(it) > 1 }) error(loc, Error.DECL_SAME_VAR)
			if (rel.exprs.size() != types.size()) error(loc, Error.DECL_MALFORMED)
			def d = new RelDeclaration(rel, types, annotations)
			currDatalog.relDeclarations << d
			rec(d, ctx)
		}
	}

	void exitRule_(Rule_Context ctx) {
		Rule r
		if (ctx.headList()) {
			def annotations = gatherAnnotations(ctx.annotationList())
			if (Annotation.NAMESPACE in annotations) error(rec(null, ctx), Error.ANNOTATION_BLOCK_ONLY, Annotation.NAMESPACE)
			r = new Rule(values[ctx.headList()] as IElement, values[ctx.bodyList()] as IElement, annotations)
		}
		// Aggregation
		else
			r = new Rule(values[ctx.relation()] as Relation, values[ctx.aggregation()] as AggregationElement)
		currDatalog.rules << r
		rec(r, ctx)
	}

	void exitRelation(RelationContext ctx) {
		def name = suffix(ctx.IDENTIFIER(0).text)
		def at = ctx.IDENTIFIER(1) ? "@${ctx.IDENTIFIER(1).text}" : ""
		def exprs = ctx.exprList() ? values[ctx.exprList()] as List : []
		values[ctx] = new Relation("$name$at", exprs)
		rec(values[ctx], ctx)
	}

	void exitConstructor(ConstructorContext ctx) {
		def name = suffix(ctx.IDENTIFIER().text)
		def exprs = (ctx.exprList() ? values[ctx.exprList()] as List : []) << (values[ctx.expr()] as IExpr)
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
				values[ctx.bodyList()] as IElement)
		rec(values[ctx], ctx)
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
		else if (ctx.STRING()) values[ctx] = new ConstantExpr(ctx.STRING().text[1..-2])
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

	void exitInitValue(InitValueContext ctx) { values[ctx] = [(ctx.IDENTIFIER().text): values[ctx.constant()]] }

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

	void exitHeadList(HeadListContext ctx) {
		def t = ctx.relation() ? values[ctx.relation()] : values[ctx.construction()]
		values[ctx] = ctx.headList() ? new LogicalElement([values[ctx.headList()], t] as List<IElement>) : t
	}

	void exitBodyList(BodyListContext ctx) {
		if (ctx.relation())
			values[ctx] = values[ctx.relation()]
		else if (ctx.constructor())
			values[ctx] = values[ctx.constructor()]
		else if (ctx.comparison())
			values[ctx] = values[ctx.comparison()]
		else if (hasToken(ctx, "!"))
			values[ctx] = new NegationElement(values[ctx.bodyList(0)] as IElement)
		else if (hasToken(ctx, "("))
			values[ctx] = new GroupElement(values[ctx.bodyList(0)] as IElement)
		else {
			def list = (0..1).collect { values[ctx.bodyList(it)] as IElement }
			def type = hasToken(ctx, ",") ? LogicalElement.Kind.AND : LogicalElement.Kind.OR
			values[ctx] = new LogicalElement(type, list)
		}
	}

	// Special handling (instead of using "exit")
	private Set<Annotation> gatherAnnotations(AnnotationListContext ctx) {
		if (!ctx) return [] as Set
		def valueMap = gatherValues(ctx.annotation().valueList())
		def annotation = new Annotation(ctx.annotation().IDENTIFIER().text, valueMap)
		def set = gatherAnnotations(ctx.annotationList())
		if (annotation in set) warn(rec(annotation, ctx), Error.ANNOTATION_MULTIPLE, annotation.kind)
		return set << annotation
	}

	void exitInitValueList(InitValueListContext ctx) {
		values[ctx] = ((values[ctx.initValueList()] ?: [:]) as Map) << (values[ctx.initValue()] as Map)
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

	void exitParameterList(ParameterListContext ctx) { values[ctx] = values[ctx.identifierList()] }

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

	void visitErrorNode(ErrorNode node) { throw new RuntimeException("Parsing error") }

	Set<String> getComponentIDs() { program.components.collect { it.name } + program.instantiations.collect { it.id } }

	def suffix(String name) {
		if (Type.isPrimitive(name)) return name
		activeNamespaces ? "${activeNamespaces.join(":")}:$name" : name
	}

	static def hasToken(ParserRuleContext ctx, String token) {
		ctx.children.any { it instanceof TerminalNode && it.text == token }
	}

	static def rec(def o, ParserRuleContext ctx) { SourceManager.instance.record(o, ctx.start.getLine()) }
}
