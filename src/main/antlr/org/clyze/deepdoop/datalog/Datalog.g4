grammar Datalog;

@header {
package org.clyze.deepdoop.datalog;
}

program
	: (component | cmd | initialize | datalog)* ;

component
	: 'component' IDENTIFIER parameterList? (':' superComponent)? '{' datalog* '}' ('as' identifierList)? ;

superComponent
	: IDENTIFIER parameterList? ;

cmd
	: 'cmd' IDENTIFIER '{' datalog* '}' ('as' identifierList)? ;

initialize
	: IDENTIFIER parameterList? 'as' identifierList ;

datalog
	: declaration | annotationBlock | rule_ | lineMarker ;

declaration
	: annotationList? IDENTIFIER (':' IDENTIFIER)? ('with' initValueList)?
	| annotationList? (relation | constructor) ':' identifierList
	;

annotationBlock
	: annotationList '{' declaration+ '}' ;

rule_
	: annotationList? headList ('<-' bodyList)?
	| relation '<-' aggregation
	;

relation
	: IDENTIFIER ('@' IDENTIFIER)? '(' exprList? ')' ;

constructor
	: IDENTIFIER '[' exprList? ']' '=' expr ;

construction
	: constructor 'new' IDENTIFIER ;

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

initValue
	: IDENTIFIER '(' constant ')' ;

comparison
	: expr ('=' | '<' | '<=' | '>' | '>=' | '!=') expr ;

lineMarker
	: '#' INTEGER STRING INTEGER* ;

// Various lists

annotationList
	: annotation
	| annotationList annotation
	;

initValueList
	: initValue
	| initValueList ',' initValue
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

INTEGER
	: [\-]?POSITIVE ;

fragment
POSITIVE
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
	: '"' ~["]* '"'
	| '\'' ~[']* '\''
	;

IDENTIFIER
	: [?]?[a-zA-Z_][a-zA-Z_0-9:]* ;

LINE_COMMENT
	: '//' ~[\r\n]* -> skip ;

BLOCK_COMMENT
	: '/*' .*? '*/' -> skip ;

WHITE_SPACE
	: [ \t\r\n]+ -> skip ;
