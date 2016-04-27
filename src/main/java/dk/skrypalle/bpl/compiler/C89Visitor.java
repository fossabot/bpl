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

public class C89Visitor extends BPLBaseVisitor<String> {

	@Override
	public String visitCompilationUnit(CompilationUnitContext ctx) {
		String val = ttos(ctx.INT());
		BigInteger i = new BigInteger(val);
		int size = i.bitLength();
		String res;
		if (size <= 8) {
			res = "uint8_t a = " + val + "U;";
		} else if (size <= 16) {
			res = "uint16_t a = " + val + "U;";
		} else if (size <= 32) {
			res = "uint32_t a = " + val + "UL;";
		} else if (size <= 64) {
			res = "uint64_t a = " + val + "ULL;";
		} else {
			throw new IllegalStateException(); // TODO
		}

		return String.join("\n",
			"#include <stdio.h>",
			"#include <stdint.h>",
			"",
			"#define TO_HEX(i) ((i) <= 9 ? '0' + (i) : 'a' - 10 + (i))",
			"static void _printHex(void* p, int len);",
			"",
			"int main(void)",
			"{",
			"	" + res,
			"	_printHex(&a, sizeof(a));",
			"	return 0;",
			"}",
			"",
			"void _printHex(void* p, int len)",
			"{",
			"	int i;",
			"	for(i=0; i<len; ++i) {",
			"		uint8_t* cur = (uint8_t*) p+i;",
			"		uint8_t  val = (*cur)&0xff;",
			"		putchar(TO_HEX(val>>4 & 0xf));",
			"		putchar(TO_HEX(val    & 0xf));",
			"	}",
			"}"
		);
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
