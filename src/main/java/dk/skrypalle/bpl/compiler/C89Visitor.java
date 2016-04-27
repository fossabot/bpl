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
import dk.skrypalle.bpl.compiler.type.*;
import dk.skrypalle.bpl.util.*;
import org.antlr.v4.runtime.tree.*;

import java.math.*;
import java.util.*;

import static dk.skrypalle.bpl.antlr.BPLParser.*;

public class C89Visitor extends BPLBaseVisitor<String> {

	private final Deque<IntType> tStack;

	public C89Visitor() {
		this.tStack = new ArrayDeque<>();
	}

	@Override
	public String visitCompilationUnit(CompilationUnitContext ctx) {
		String cld = visitChildren(ctx);
		IntType t = tStack.pop();
		String cType;
		String cExt = "";
		switch (t) {
		case U8:
			cType = "uint8_t";
			cExt = "U";
			break;
		case U16:
			cType = "uint16_t";
			cExt = "U";
			break;
		case U32:
			cType = "uint32_t";
			cExt = "UL";
			break;
		case U64:
			cType = "uint64_t";
			cExt = "ULL";
			break;
		case S8:
			cType = "int8_t";
			break;
		case S16:
			cType = "int16_t";
			break;
		case S32:
			cType = "int32_t";
			break;
		case S64:
			cType = "int64_t";
			break;
		default:
			throw new Error();
		}
		return String.join("\n",
			"#include <stdio.h>",
			"#include <stdint.h>",
			"#include <limits.h>",
			"",
			"#define TO_HEX(i) ((i) <= 9 ? '0' + (i) : 'a' - 10 + (i))",
			"static void _printHex(void* p, int len);",
			"",
			"int main(void)",
			"{",
			"	" + cType + " a = " + cld + cExt + ";",
			"	_printHex(&a, sizeof(a));",
			"	return 0;",
			"}",
			"",
			"void _printHex(void* p, int len)",
			"{",
			"	int i;",
			"",
			"	for(i=len-1; i>=0; --i) {",
			"		uint8_t* cur = (uint8_t*) p+i;",
			"		uint8_t  val = (*cur)&0xff;",
			"		putchar(TO_HEX(val>>4 & 0xf));",
			"		putchar(TO_HEX(val    & 0xf));",
			"	}",
			"}"
		);
	}

	@Override
	public String visitAddExpr(AddExprContext ctx) {
		String lhs = visit(ctx.lhs);
		IntType lhs_t = tStack.pop();
		String rhs = visit(ctx.rhs);
		IntType rhs_t = tStack.pop();
		IntType res_t = lhs_t;

		// Check matching types
		if (lhs_t != rhs_t) {
			if (lhs_t.isSigned != rhs_t.isSigned)
				throw new IllegalStateException("sign error: " + lhs_t + " != " + rhs_t); // TODO

			// Auto widen int literals
			if (lhs_t.width < rhs_t.width)
				res_t = rhs_t;
		}

		// Auto widen result
		BigInteger res = new BigInteger(lhs).add(new BigInteger(rhs));
		if (res.bitLength() > res_t.width)
			res_t = res_t.next();

		tStack.push(res_t);

		return res.toString();
	}

	@Override
	public String visitIntExpr(IntExprContext ctx) {
		String val = Parse.ttos(ctx.val);
		BigInteger res = new BigInteger(val, 10);
		tStack.push(IntType.parse(res));
		return res.toString();
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
