package org.codesimius.panda

import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.DatalogBaseListener
import org.codesimius.panda.datalog.DatalogLexer
import org.codesimius.panda.datalog.DatalogParser
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
import org.codesimius.panda.datalog.element.relation.RelationText
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.*
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.DatalogParser.*
import static org.codesimius.panda.datalog.element.LogicalElement.combineElements

class DatalogParserImpl extends DatalogBaseListener {

	Compiler compiler

	BlockLvl2 program = new BlockLvl2()
	BlockLvl0 currDatalog = program.datalog
	// Relation Name x Annotations
	Map<String, Set<Annotation>> globalPendingAnnotations = [:].withDefault { [] as Set }
	Map<String, Set<Annotation>> currPendingAnnotations = globalPendingAnnotations
	// Generic map to pass computed values around
	def values = [:]

	DatalogParserImpl(Compiler compiler) { this.compiler = compiler }

	void exitProgram(ProgramContext ctx) {
		// Do so only at the end of all code parsing, because
		// exitProgram will also be called at the end of each included file.
		if (compiler.activeFiles.size() == 1)
			currPendingAnnotations.each { addAnnotationsToRelDecl(it.key, it.value) }
	}

	void enterInclude(IncludeContext ctx) {
		def toFile = new File(compiler.activeFiles.last().parentFile, ctx.STRING().text[1..-2])
		def parser = new DatalogParser(new CommonTokenStream(new DatalogLexer(new ANTLRFileStream(toFile.absolutePath))))
		compiler.enterInclude(toFile, ctx.start.line)
		ParseTreeWalker.DEFAULT.walk(this, parser.program())
	}

	void exitInclude(IncludeContext ctx) { compiler.exitInclude() }

	void enterTemplate(TemplateContext ctx) {
		currDatalog = new BlockLvl0()
		currPendingAnnotations = [:].withDefault { [] as Set }
	}

	void exitTemplate(TemplateContext ctx) {
		def name = ctx.IDENTIFIER().text
		def superName = ctx.superTemplate()?.IDENTIFIER()?.text
		def parameters = values[ctx.parameterList()] as List ?: []
		def superParameters = ctx.superTemplate()?.parameterList() ? values[ctx.superTemplate().parameterList()] as List : []
		program.templates << rec(new BlockLvl1(name, superName, parameters, superParameters, currDatalog), ctx)

		currPendingAnnotations.each { addAnnotationsToRelDecl(it.key, it.value) }
		currPendingAnnotations = globalPendingAnnotations
		currDatalog = program.datalog
	}

	void exitInstantiation(InstantiationContext ctx) {
		def parameters = values[ctx.parameterList()] as List ?: []
		values[ctx.identifierList()].each { String id ->
			program.instantiations << rec(new Instantiation(ctx.IDENTIFIER().text, id, parameters), ctx.identifierList())
		}
	}

	void exitDeclaration(DeclarationContext ctx) {
		def annotations = mergeAnnotations(gatherAnnotations(ctx.annotationList()), annotateLocation(ctx))

		// Type declaration
		if (ctx.extIdentifier(0) && TYPE in annotations) {
			def type = new Type(values[ctx.extIdentifier(0)] as String)
			def supertype = ctx.extIdentifier(1) ? new Type(values[ctx.extIdentifier(1)] as String) : null
			// Initial values are of the form `key(value)`. E.g., PUBLIC('public')
			// Keys are used to generate singleton relations. E.g., Modifier:PUBLIC(x)
			if (ctx.initValueList())
				annotations << TYPEVALUES.template(values[ctx.initValueList()] as Map)
			currDatalog.typeDeclarations << new TypeDeclaration(type, supertype, annotations)
		}
		// Incomplete declaration of the form: `@output Foo`
		// i.e. binds annotations with a relation name without providing any details
		else if (ctx.extIdentifier(0)) {
			mergeAnnotations(currPendingAnnotations[values[ctx.extIdentifier(0)] as String], annotations)
		}
		// Full declaration of a relation
		else {
			def rel = ctx.relation() ? values[ctx.relation()] as Relation : values[ctx.constructor()] as Constructor
			def types = values[ctx.extIdentifierList()].collect { new Type(it as String) }
			currDatalog.relDeclarations << new RelDeclaration(rel, types, annotations)
		}
	}

	void exitRule_(Rule_Context ctx) {
		def locAnnotation = annotateLocation(ctx)
		if (ctx.headList()) {
			def annotations = gatherAnnotations(ctx.annotationList()) << locAnnotation
			currDatalog.rules << new Rule(values[ctx.headList()] as IElement, values[ctx.bodyList()] as IElement, annotations)
		}
		// Aggregation
		else {
			def annotations = [locAnnotation] as Set
			currDatalog.rules << new Rule(values[ctx.relation()] as Relation, values[ctx.aggregation()] as AggregationElement, annotations)
		}
	}

	void exitRelationText(RelationTextContext ctx) {
		values[ctx] = new RelationText(ctx.children.collect { (it instanceof TerminalNode) ? it.text : values[it] })
	}

	void exitRelation(RelationContext ctx) {
		def name = values[ctx.extIdentifier()] as String
		def exprs = ctx.exprList() ? values[ctx.exprList()] as List : []
		values[ctx] = new Relation(name, exprs)
	}

	void exitConstructor(ConstructorContext ctx) {
		def name = ctx.IDENTIFIER().text
		def exprs = (ctx.exprList() ? values[ctx.exprList()] as List : []) << (values[ctx.expr()] as IExpr)
		values[ctx] = new Constructor(name, exprs)
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
	}

	void exitExtIdentifier(ExtIdentifierContext ctx) {
		values[ctx] = "${ctx.IDENTIFIER(0).text}${ctx.IDENTIFIER(1) ? "@${ctx.IDENTIFIER(1).text}" : ""}"
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
		else if (ctx.STRING()) values[ctx] = new ConstantExpr(ctx.STRING().text[1..-2], hasToken(ctx, "@"))
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
		if (ctx.relationText())
			values[ctx] = values[ctx.relationText()]
		else if (ctx.relation())
			values[ctx] = values[ctx.relation()]
		else if (ctx.construction())
			values[ctx] = values[ctx.construction()]
		else {
			def list = (0..1).collect { values[ctx.headList(it)] as IElement }
			values[ctx] = combineElements(list)
		}
	}

	void exitBodyList(BodyListContext ctx) {
		if (ctx.relationText())
			values[ctx] = values[ctx.relationText()]
		else if (ctx.relation())
			values[ctx] = values[ctx.relation()]
		else if (ctx.constructor())
			values[ctx] = values[ctx.constructor()]
		else if (ctx.comparison())
			values[ctx] = values[ctx.comparison()]
		else if (hasToken(ctx, "!"))
			values[ctx] = new NegationElement(values[ctx.bodyList(0)] as IElement)
		// Remove group elements and replace them with their contents
		else if (hasToken(ctx, "("))
			values[ctx] = values[ctx.bodyList(0)]
		else {
			def list = (0..1).collect { values[ctx.bodyList(it)] as IElement }
			def kind = hasToken(ctx, ",") ? LogicalElement.Kind.AND : LogicalElement.Kind.OR
			values[ctx] = combineElements(kind, list)
		}
	}

	// Special handling (instead of using "exit")
	Set<Annotation> gatherAnnotations(AnnotationListContext ctx) {
		if (!ctx) return [] as Set
		def valueMap = gatherValues(ctx.annotation().valueList())
		def annotation = rec(new Annotation(ctx.annotation().IDENTIFIER().text, valueMap), ctx) as Annotation
		annotation.validate(compiler)
		mergeAnnotations(gatherAnnotations(ctx.annotationList()), annotation)
	}

	void exitInitValueList(InitValueListContext ctx) {
		values[ctx] = ((values[ctx.initValueList()] ?: [:]) as Map) << (values[ctx.initValue()] as Map)
	}

	void exitIdentifierList(IdentifierListContext ctx) {
		values[ctx] = ((values[ctx.identifierList()] ?: []) as List) << ctx.IDENTIFIER().text
	}

	void exitExtIdentifierList(ExtIdentifierListContext ctx) {
		values[ctx] = ((values[ctx.extIdentifierList()] ?: []) as List) << values[ctx.extIdentifier()]
	}

	// Special handling (instead of using "exit")
	Map<String, ConstantExpr> gatherValues(ValueListContext ctx) {
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

	void visitErrorNode(ErrorNode node) { throw new RuntimeException("Parsing error") }


	def addAnnotationsToRelDecl(String relName, Set<Annotation> annotations) {
		def rd = currDatalog.relDeclarations.find { it.relation.name == relName } as RelDeclaration
		if (rd) mergeAnnotations(rd.annotations, annotations)
		else currDatalog.relDeclarations << new RelDeclaration(new Relation(relName), [], annotations)
	}

	def rec(def o, ParserRuleContext ctx) {
		compiler.rec(o, ctx.start.line)
		return o
	}

	def annotateLocation(ParserRuleContext ctx) {
		def annotation = METADATA.template([loc: null])
		def locExpr = new ConstantExpr(compiler.rec(annotation, ctx.start.line))
		annotation.args["loc"] = locExpr
		return annotation
	}

	Set<Annotation> mergeAnnotations(Set<Annotation> set, Annotation annotation) {
		if (annotation in set && !annotation.isInternal)
			compiler.warn(compiler.loc(annotation), Error.ANNOTATION_MULTIPLE, annotation)
		else
			set << annotation
		return set
	}

	Set<Annotation> mergeAnnotations(Set<Annotation> set1, Set<Annotation> set2) {
		set2.each { mergeAnnotations(set1, it) }
		return set1
	}

	static def hasToken(ParserRuleContext ctx, String token) {
		ctx.children.any { it instanceof TerminalNode && it.text == token }
	}
}
