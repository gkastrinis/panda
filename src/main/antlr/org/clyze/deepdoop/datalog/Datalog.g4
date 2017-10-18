grammar Datalog;

@header {
package org.clyze.deepdoop.datalog;
}

program
	: (component | cmd | initialize | propagate | datalog)* ;

component
	: COMP IDENTIFIER (':' IDENTIFIER)? '{' datalog* '}' (AS identifierList)? ;

cmd
	: CMD IDENTIFIER '{' datalog* '}' (AS identifierList)? ;

initialize
	: IDENTIFIER AS identifierList ;

propagate
	: IDENTIFIER '{' propagationList '}' '->' (IDENTIFIER | GLOBAL) ;

datalog
	: declaration | annotationBlock | rule_ | lineMarker ;

declaration
	: annotationList? relationName ('<:' relationName)?
	| annotationList? (relation | constructor) ':' relationNameList
	;

annotationBlock
	: annotationList '{' declaration+ '}' ;

rule_
	: annotationList? headList ('<-' bodyList)? '.'
	| relation '<-' aggregation '.'
	;

relation
	: relationName AT_STAGE? '(' exprList? ')' ;

constructor
	: relationName '[' exprList? ']' '=' expr ;

construction
	: constructor NEW relationName ;

aggregation
	: AGG '<<' IDENTIFIER '=' relation '>>' bodyList ;

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

propagationList
    : ALL
	| relationName
	| propagationList ',' relationName
	;

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



// Lexer

AGG
	: 'agg' ;

NEW
	: 'new' ;

ALL
	: '*' ;

AS
	: [aA][sS] ;

AT_STAGE
	: '@init'
	| '@initial'
	| '@prev'
	| '@previous'
	| '@ext'
	;

CAPACITY
	: '[' ('32' | '64' | '128') ']' ;

CMD
	: 'command' ;

COMP
	: 'component' ;

GLOBAL
	: '.' ;

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
