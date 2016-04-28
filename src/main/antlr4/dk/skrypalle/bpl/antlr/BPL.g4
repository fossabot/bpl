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
	;

funcDecl  : 'func' id=ID '('')' 'int' '{' stmts=stmt* '}' ;
funcCall  : id=ID '('')'                                  ;
varDecl   : 'var' id=ID 'int'                             ;
varAssign : lhs=ID '=' rhs=expr                           ;
print     : 'print' '(' arg=expr ')'                      ;
ret       : 'return' expr                                 ;

expr
	: lhs=expr op=('/'|'*') rhs=expr #BinOpExpr
	| lhs=expr op=('+'|'-') rhs=expr #BinOpExpr
	| varAssign                      #AssignExpr
	| funcCall                       #FuncCallExpr
	| val=INT                        #IntExpr
	| val=ID                         #IdExpr
	;

INT : [0-9]+            ;
ID  : [a-zA-Z0-9_]+     ;
WS  : [ \t\r\n] -> skip ;
