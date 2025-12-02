grammar AeroScript;

@header {
package no.uio.aeroscript.antlr;
}

// Whitespace and comments added
WS           : [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT      : '/*' .*? '*/' -> channel(HIDDEN) ;
LINE_COMMENT : '//' ~[\r\n]* -> channel(HIDDEN) ;

LCURL   : '{';
RCURL   : '}';
LSQUARE : '[';
RSQUARE : ']';
LPAREN  : '(';
RPAREN  : ')';

SEMI    : ';';
COMMA   : ',';
ARROW   : '->';

NEG     : '--';
PLUS    : '+';
MINUS   : '-';
TIMES   : '*';
RANDOM  : 'random';
POINT   : 'point';

// Keywords
// I would have used positive integers like was specified, if that's what we were actually dealing with...
NUMBER: '-'? [0-9]+ ('.'[0-9]+)?;

// Entry point
//program : (expression)* EOF;

ID : [a-zA-Z_][a-zA-Z0-9_]*; // maybe? idk
program : (execution)+;
execution : prefix=ARROW? ID LCURL (statement)* RCURL (ARROW ID)?;
statement : action | reaction;
reaction : 'on' event '->' ID;
event : 'obstacle' | 'low battery' | 'message' '[' ID ']';
action : (acDock | acMove | acTurn | acAscend | acDescend) ('for' expression 'seconds' | 'at speed' expression)?;
acDock : 'return to base';
acMove : 'move' ('to' expression | 'by' expression);
acTurn : 'turn' ('right' | 'left')? 'by' expression;
acAscend : 'ascend by' expression;
acDescend : 'descend by' expression | 'descend to ground';

expression : NEG expression | expression (TIMES | PLUS | MINUS) expression | RANDOM (range)? | POINT point | NUMBER | LPAREN expression RPAREN;

point : LPAREN expression COMMA expression RPAREN; // Why only two expressions?
range : LSQUARE expression COMMA expression RSQUARE;