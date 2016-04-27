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
import dk.skrypalle.bpl.compiler.type.*;
import dk.skrypalle.bpl.util.*;
import org.antlr.v4.runtime.tree.*;

import java.math.*;
import java.util.*;

import static dk.skrypalle.bpl.util.Parse.*;
import static dk.skrypalle.bpl.vm.Bytecode.*;

public class BCVisitor extends BPLBaseVisitor<byte[]> {

	private static final byte[] EMPTY = {};

	private final Deque<IntType> tStack;

	private BigInteger arithRes;

	public BCVisitor() {
		this.tStack = new ArrayDeque<>();
	}

	@Override
	public byte[] visitCompilationUnit(CompilationUnitContext ctx) {
		byte[] cld = visitChildren(ctx);
		IntType t = tStack.pop();
		return append(
			cld,
			PRINT, t.width/8, 0, 0, 0,
			HALT
		);
	}

	@Override
	public byte[] visitAddExpr(AddExprContext ctx) {
		byte[] lhs = visit(ctx.lhs);
		IntType lhs_t = tStack.pop();
		BigInteger lval = arithRes;
		byte[] rhs = visit(ctx.rhs);
		IntType rhs_t = tStack.pop();
		IntType res_t = lhs_t;
		BigInteger rval = arithRes;

		if (lhs_t != rhs_t) {
			if (lhs_t.isSigned != rhs_t.isSigned)
				throw new IllegalStateException("sign error: " + lhs_t + " != " + rhs_t); // TODO

			if (rhs_t.width > lhs_t.width) {
				lhs = append(lhs, WIDEN, lhs_t.width, rhs_t.width);
				res_t = rhs_t;
			} else if (rhs_t.width < lhs_t.width) {
				rhs = append(rhs, WIDEN, rhs_t.width, lhs_t.width);
				res_t = lhs_t;
			}
		}

		arithRes = lval.add(rval);
		if (arithRes.bitLength() > res_t.width) {
			res_t = res_t.next();
			lhs = append(lhs, WIDEN, lhs_t.width, res_t.width);
			rhs = append(rhs, WIDEN, rhs_t.width, res_t.width);
		}

		byte op;
		switch (res_t) {
		case U8:
			op = ADDU8;
			break;
		case U16:
			op = ADDU16;
			break;
		case U32:
			op = ADDU32;
			break;
		case U64:
			op = ADDU64;
			break;
		case S8:
			op = ADDS8;
			break;
		case S16:
			op = ADDS16;
			break;
		case S32:
			op = ADDS32;
			break;
		case S64:
			op = ADDS64;
			break;
		default:
			throw new IllegalStateException("unreachable"); // TODO
		}

		tStack.push(res_t);

		return append(concat(lhs, rhs), op);
	}

	@Override
	public byte[] visitIntExpr(IntExprContext ctx) {
		String val = Parse.ttos(ctx.val);
		arithRes = new BigInteger(val);
		IntType t = IntType.parse(val);

		byte op;
		switch (t.width) {
		case 8:
			op = PUSH8;
			break;
		case 16:
			op = PUSH16;
			break;
		case 32:
			op = PUSH32;
			break;
		case 64:
			op = PUSH64;
			break;
		default:
			throw new IllegalStateException("unreachable"); // TODO
		}

		tStack.push(t);
		return prepend(op, toBA(val, 10, t));
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
		return EMPTY;
	}

	@Override
	public byte[] visit(ParseTree t) {
		return t == null ? defaultResult() : t.accept(this);
	}

	//endregion

}
