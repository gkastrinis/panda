grammar Datalog;

@header {
package org.codesimius.panda.datalog;
}

program
	: (component | cmd | instantiation | datalog)* ;

component
	: 'component' IDENTIFIER parameterList? (':' superComponent)? '{' datalog* '}' ('as' identifierList)? ;

superComponent
	: IDENTIFIER parameterList? ;

cmd
	: 'cmd' IDENTIFIER '{' datalog* '}' ('as' identifierList)? ;

instantiation
	: IDENTIFIER parameterList? 'as' identifierList ;

datalog
	: annotationBlock | declaration | rule_ | lineMarker ;

annotationBlock
	: annotationList '{' datalog* '}' ;

declaration
	: annotationList? IDENTIFIER (':' IDENTIFIER)? ('with' initValueList)?
	| annotationList? (relation | constructor) ':' identifierList
	;

rule_
	: annotationList? headList ('<-' bodyList)?
	| relation '<-' aggregation
	;

relation
	: IDENTIFIER ('@' IDENTIFIER)? '(' exprList? ')' ;

relationText
	: (IDENTIFIER | constant)+ ;

constructor
	: IDENTIFIER '[' exprList? ']' '=' expr ;

construction
	: constructor 'new' IDENTIFIER ;

aggregation
	: 'agg' '<<' IDENTIFIER '=' relation '>>' bodyList ;

value
	: IDENTIFIER '=' constant ;

annotation
	: '@' IDENTIFIER ('(' valueList ')')? ;

constant
	: INTEGER
	| REAL
	| BOOLEAN
	| '@'? STRING
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

headList
	: relation
	| construction
	| headList ',' headList
	;

bodyList
	: relationText
	| relation
	| constructor
	| comparison
	| '!' bodyList
	| '(' bodyList ')'
	| bodyList (',' | ';') bodyList
	;

annotationList
	: annotation
	| annotationList annotation
	;

initValueList
	: initValue
	| initValueList ',' initValue
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

lineMarker
	: '#' INTEGER STRING INTEGER* ;


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
