grammar Datalog;

@header {
package org.codesimius.panda.datalog;
}

program
	: (include | template | instantiation | datalog)* EOF ;

include
	: 'include' STRING ;

template
	: 'template' IDENTIFIER parameterList? (':' superTemplate)? '{' datalog* '}' ;

superTemplate
	: IDENTIFIER parameterList? ;

instantiation
	: IDENTIFIER parameterList? 'as' identifierList ;

///////////////////////////////////////////////////////////////

datalog
	: declaration | rule_ ;

declaration
	: annotationList qualifiedId (':' qualifiedId)? ('with' initValueList)?
	| annotationList? (relation | constructor) ':' qualifiedIdList
	;

rule_
	: annotationList? headList ('<-' bodyList)?
	| relation '<-' aggregation
	;

///////////////////////////////////////////////////////////////

relation
	: qualifiedId '(' exprList? ')' ;

relationText
	: (IDENTIFIER | constant)+
	| '{' (IDENTIFIER | constant)+ '}' // For disambiguation
	;

constructor
	: IDENTIFIER '[' exprList? ']' '=' expr ;

construction
	: constructor 'new' IDENTIFIER ;

aggregation
	: IDENTIFIER '=' relation '{' bodyList '}' ;

///////////////////////////////////////////////////////////////

qualifiedId
	: (IDENTIFIER '.')? IDENTIFIER ;

value
	: IDENTIFIER '=' constant ;

initValue
	: IDENTIFIER '(' constant ')' ;

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
	| '#' expr
	| expr ('+' | '-' | '*' | '/') expr
	| '(' expr ')'
	;

comparison
	: expr ('=' | '<' | '<=' | '>' | '>=' | '!=') expr ;

///////////////////////////////////////////////////////////////

headList
	: relationText
	| relation
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

qualifiedIdList
	: qualifiedId
	| qualifiedIdList ',' qualifiedId
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
