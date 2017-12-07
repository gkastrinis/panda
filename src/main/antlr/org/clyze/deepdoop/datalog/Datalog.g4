grammar Datalog;

@header {
package org.clyze.deepdoop.datalog;
}

program
	: (component | cmd | initialize | datalog)* ;

component
	: 'component' IDENTIFIER parameterList? (':' superComponent)? '{' datalog* '}' (AS identifierList)? ;

superComponent
	: IDENTIFIER parameterList? ;

cmd
	: 'cmd' IDENTIFIER '{' datalog* '}' (AS identifierList)? ;

initialize
	: IDENTIFIER parameterList? AS identifierList ;

datalog
	: declaration | annotationBlock | rule_ | lineMarker ;

declaration
	: annotationList? relationName ('<:' relationName)?
	| annotationList? (relation | constructor) ':' relationNameList
	;

annotationBlock
	: annotationList '{' declaration+ '}' ;

rule_
	: annotationList? headList ('<-' bodyList)?
	| relation '<-' aggregation
	;

relation
	: relationName ('@' IDENTIFIER)? '(' exprList? ')' ;

constructor
	: relationName '[' exprList? ']' '=' expr ;

construction
	: constructor 'new' relationName ;

aggregation
	: 'agg' '<<' IDENTIFIER '=' relation '>>' bodyList ;

bodyList
	: relation
	| constructor
	| comparison
	| '!' bodyList
	| '(' bodyList ')'
	| bodyList (',' | ';') bodyList
	;

value
	: IDENTIFIER '=' constant ;

annotation
	: '@' IDENTIFIER ('(' valueList ')')? ;

lineMarker
	: '#' INTEGER STRING INTEGER* ;

relationName
	: '$'? IDENTIFIER
	| relationName ':' IDENTIFIER
	;

constant
	: INTEGER
	| REAL
	| BOOLEAN
	| STRING
	;

expr
	: IDENTIFIER
	| constant
	| expr ('+' | '-' | '*' | '/') expr
	| '(' expr ')'
	;

comparison
	: expr ('=' | '<' | '<=' | '>' | '>=' | '!=') expr ;



// Various lists

annotationList
	: annotation
	| annotationList annotation
	;

relationNameList
	: relationName
	| relationNameList ',' relationName
	;

headList
	: (relation | construction)
	| headList ',' (relation | construction)
	;

identifierList
	: IDENTIFIER
	| identifierList ',' IDENTIFIER
	;

valueList
    : value
    | valueList ',' value
    ;

exprList
	: expr
	| exprList ',' expr
	;

parameterList
	: '<' identifierList '>' ;



// Lexer

AS
	: [aA][sS] ;

INTEGER
	: [0-9]+
	| '0'[0-7]+
	| '0'[xX][0-9a-fA-F]+
	| '2^'[0-9]+
	;

fragment
EXPONENT
	: [eE][-+]?INTEGER ;

REAL
	: INTEGER EXPONENT
	| INTEGER EXPONENT? [fF]
	| (INTEGER)? '.' INTEGER EXPONENT? [fF]?
	;

BOOLEAN
	: 'true' | 'false' ;

STRING
	: '"' ~["]* '"' ;

IDENTIFIER
	: [?]?[a-zA-Z_][a-zA-Z_0-9]* ;

LINE_COMMENT
	: '//' ~[\r\n]* -> skip ;

BLOCK_COMMENT
	: '/*' .*? '*/' -> skip ;

WHITE_SPACE
	: [ \t\r\n]+ -> skip ;
