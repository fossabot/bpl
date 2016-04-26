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
import dk.skrypalle.bpl.antlr.BPLParser.*;
import org.antlr.v4.runtime.tree.*;

import java.math.*;

import static dk.skrypalle.bpl.util.Parse.*;
import static dk.skrypalle.bpl.vm.Bytecode.*;

public class BCVisitor extends BPLBaseVisitor<byte[]> {

	@Override
	public byte[] visitCompilationUnit(CompilationUnitContext ctx) {
		BigInteger i = new BigInteger(ttos(ctx.INT()));
		int size = i.bitLength();
		byte[] val = i.toByteArray();
		int len = val.length;
		byte[] res;
		if (size <= 8) {
			res = new byte[]{
				PUSH8,
				val[len - 1],
			};
		} else if (size <= 16) {
			res = new byte[]{
				PUSH16,
				val[len - 1],
				val[len - 2],
			};
		} else if (size <= 32) {
			res = new byte[]{
				PUSH32,
				val[len - 1],
				val[len - 2],
				val[len - 3],
				val[len - 4],
			};
		} else if (size <= 64) {
			res = new byte[]{
				PUSH64,
				val[len - 1],
				val[len - 2],
				val[len - 3],
				val[len - 4],
				val[len - 5],
				val[len - 6],
				val[len - 7],
				val[len - 8],
			};
		} else {
			throw new IllegalStateException(); // TODO
		}

		return append(
			res,
			PRINT, res.length - 1, 0, 0, 0,
			HALT
		);
	}

	//region aggregate, default, visit

	@Override
	protected byte[] aggregateResult(byte[] agg, byte[] nxt) {
		if (agg == null) return nxt;
		if (nxt == null) return agg;
		return concat(agg, nxt);
	}

	@Override
	protected byte[] defaultResult() {
		return null;
	}

	@Override
	public byte[] visit(ParseTree t) {
		return t == null ? defaultResult() : t.accept(this);
	}

	//endregion

}
