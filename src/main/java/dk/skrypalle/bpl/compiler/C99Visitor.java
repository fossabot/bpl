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

package dk.skrypalle.bpl.compiler;

import dk.skrypalle.bpl.antlr.*;
import org.antlr.v4.runtime.tree.*;

import java.math.*;

import static dk.skrypalle.bpl.antlr.BPLParser.*;
import static dk.skrypalle.bpl.util.Parse.*;

public class C99Visitor extends BPLBaseVisitor<String> {

	public C99Visitor() { }

	@Override
	public String visitCompilationUnit(CompilationUnitContext ctx) {
		String cld = visitChildren(ctx);

		return String.join("\n",
			"#include <stdio.h>",
			"#include <stdint.h>",
			"",
			"int main(void)",
			"{",
			cld.trim(),
			"return 0;",
			"}",
			""
		);
	}

	@Override
	public String visitStmt(StmtContext ctx) {
		return visitChildren(ctx) + ";\n";
	}

	//region expr

	@Override
	public String visitBinOpExpr(BinOpExprContext ctx) {
		return visit(ctx.lhs) + ttos(ctx.op) + visit(ctx.rhs);
	}

	@Override
	public String visitAssignExpr(AssignExprContext ctx) {
		return visit(ctx.varAssign());
	}

	@Override
	public String visitIntExpr(IntExprContext ctx) {
		BigInteger i = new BigInteger(ttos(ctx.val));
		String val = i.toString();
		if (i.signum() > 0)
			val += "U";

		if (i.bitLength() <= 16)
			return val;

		if (i.bitLength() <= 32)
			val += "L";
		else if (i.bitLength() <= 64)
			val += "LL";

		return val;
	}

	@Override
	public String visitIdExpr(IdExprContext ctx) {
		return ttos(ctx.val);
	}

	//endregion

	@Override
	public String visitVarDecl(VarDeclContext ctx) {
		return "int " + ttos(ctx.id);
	}

	@Override
	public String visitVarAssign(VarAssignContext ctx) {
		return ttos(ctx.lhs) + "=" + visit(ctx.rhs);
	}

	@Override
	public String visitPrint(PrintContext ctx) {
		return "printf(\"%llx\", " + visitChildren(ctx) + ")";
	}

	//region aggregate, default, visit

	@Override
	protected String aggregateResult(String agg, String nxt) {
		if (agg == null) return nxt;
		if (nxt == null) return agg;
		return agg + nxt;
	}

	@Override
	protected String defaultResult() {
		return "";
	}

	@Override
	public String visit(ParseTree t) {
		return t == null ? defaultResult() : t.accept(this);
	}

	//endregion

}
