/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Peter Skrypalle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

grammar BPL;

compilationUnit
	: funcDecl*
	;

stmt
	: varDecl   ';'
	| varAssign ';'
	| funcCall  ';'
	| print     ';'
	| ret       ';'
	| branch
	| loop
	| block
	;

loop      : 'while' '(' cond=expr ')' body=block                       ;
branch    : 'if' '(' cond=expr ')' onTrue=block 'else' onFalse=block   ;
block     : '{' stmts+=stmt* '}'                                       ;
funcDecl  : 'func' id=ID '(' params=paramList? ')' typ=type body=block ;
funcCall  : id=ID '(' args=argList? ')'                                ;
varDecl   : 'var' id=ID typ=type                                       ;
varAssign : lhs=ID '=' rhs=expr                                        ;
print     : 'print' '(' arg ')'                                        ;
ret       : 'return' expr                                              ;
param     : id=ID typ=type                                             ;
paramList : param (',' param)*                                         ;
arg       : expr                                                       ;
argList   : arg (',' arg)*                                             ;

expr
	: lhs=expr op=('/'|'*')                     rhs=expr #BinOpExpr
	| lhs=expr op=('+'|'-')                     rhs=expr #BinOpExpr
	| lhs=expr op=('<'|'>'|'<='|'>='|'=='|'!=') rhs=expr #BinOpExpr
	| lhs=expr op= '&&'                         rhs=expr #BoolOpExpr
	| lhs=expr op= '||'                         rhs=expr #BoolOpExpr
	| varAssign                                          #AssignExpr
	| funcCall                                           #FuncCallExpr
	| val=STR                                            #StrExpr
	| val=INT                                            #IntExpr
	| val=ID                                             #IdExpr
	;

type
	: 'int'
	| 'string'
	;

INT : [0-9]+            ;
ID  : [a-zA-Z0-9_]+     ;
STR : '"' .*? '"'       ;
WS  : [ \t\r\n] -> skip ;
