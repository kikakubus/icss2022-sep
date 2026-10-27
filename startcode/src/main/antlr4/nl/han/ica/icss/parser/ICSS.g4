grammar ICSS;

// --- LEXER ---

// IF support
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';

// Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;

// Color value takes precedence over ID identifiers
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

// Specific identifiers for IDs and CSS classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

// General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

// All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';


// --- PARSER ---

stylesheet
    : statement* EOF
    ;

statement
    : variableAssignment
    | stylerule
    | ifStatement
    ;

variableAssignment
    : CAPITAL_IDENT ASSIGNMENT_OPERATOR expression SEMICOLON
    ;

stylerule
    : selector OPEN_BRACE (declaration | ifStatement)* CLOSE_BRACE
    ;

selector
    : LOWER_IDENT
    | CLASS_IDENT
    | ID_IDENT
    ;

declaration
    : LOWER_IDENT COLON expression SEMICOLON
    ;

// IF body can contain declarations and nested IF statements
ifBody
    : (declaration | ifStatement)*
    ;

// ELSE body can contain declarations and nested IF statements
elseBody
    : (declaration | ifStatement)*
    ;

ifStatement
    : IF BOX_BRACKET_OPEN expression BOX_BRACKET_CLOSE
      OPEN_BRACE ifBody CLOSE_BRACE
      (ELSE OPEN_BRACE elseBody CLOSE_BRACE)?
    ;

// Addition and subtraction
expression
    : multiplication ((PLUS | MIN) multiplication)*
    ;

// Multiplication
multiplication
    : atom (MUL atom)*
    ;

// Individual values and variables
atom
    : COLOR
    | PIXELSIZE
    | PERCENTAGE
    | SCALAR
    | TRUE
    | FALSE
    | CAPITAL_IDENT
    ;