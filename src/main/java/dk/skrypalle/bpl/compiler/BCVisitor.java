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
import dk.skrypalle.bpl.util.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

import static dk.skrypalle.bpl.util.Array.*;
import static dk.skrypalle.bpl.util.Parse.*;
import static dk.skrypalle.bpl.vm.Bytecode.*;

public class BCVisitor extends BPLBaseVisitor<byte[]> {

	private static final byte[] EMPTY = {};

	private final Map<String, Integer> varTbl;

	public BCVisitor() {
		varTbl = new HashMap<>();
	}

	@Override
	public byte[] visitCompilationUnit(CompilationUnitContext ctx) {
		byte[] cld = visitChildren(ctx);

		// Temporary "calling convention"
		// Push "garbage" values
		int nVars = varTbl.size();
		byte[] locals = new byte[nVars*9];
		Random r = new Random(System.nanoTime());
		for (int i = 0; i < locals.length; i++)
			locals[i] = (byte) r.nextInt(0xff);
		for (int i = 0; i < nVars; i++)
			locals[i*9] = IPUSH;
		byte[] pops = new byte[nVars];
		for (int i = 0; i < pops.length; i++)
			pops[i] = POP;
		return concat(locals, cld, pops, HALT);
	}

	@Override
	public byte[] visitStmt(StmtContext ctx) {
		return visitChildren(ctx);
	}

	//region expr

	@Override
	public byte[] visitBinOpExpr(BinOpExprContext ctx) {
		byte[] lhs = visit(ctx.lhs);
		byte[] rhs = visit(ctx.rhs);
		String op_str = ttos(ctx.op);
		byte op;
		//fmt:off
		switch (op_str) {
		case "+": op = IADD; break;
		case "-": op = ISUB; break;
		case "*": op = IMUL; break;
		case "/": op = IDIV; break;
		default : throw new IllegalStateException("unreachable");
		}
		//fmt:on
		return concat(lhs, rhs, op);
	}

	@Override
	public byte[] visitAssignExpr(AssignExprContext ctx) {
		return visit(ctx.varAssign());
	}

	@Override
	public byte[] visitIntExpr(IntExprContext ctx) {
		String val = Parse.ttos(ctx.val);
		byte[] res = Marshal.bytesS64BE(Long.parseUnsignedLong(val, 10));
		return concat(IPUSH, res);
	}

	@Override
	public byte[] visitIdExpr(IdExprContext ctx) {
		String id = ttos(ctx.val);
		if (!varTbl.containsKey(id))
			throw new IllegalStateException(); // TODO

		int idx = varTbl.get(id);
		return concat(ILOAD, Marshal.bytesS32BE(idx));
	}

	//endregion

	@Override
	public byte[] visitVarDecl(VarDeclContext ctx) {
		String id = ttos(ctx.id);
		if (varTbl.containsKey(id))
			throw new IllegalStateException(); // TODO

		varTbl.put(id, varTbl.size());
		return EMPTY;
	}

	@Override
	public byte[] visitVarAssign(VarAssignContext ctx) {
		String id = ttos(ctx.lhs);
		if (!varTbl.containsKey(id))
			throw new IllegalStateException(); // TODO

		int idx = varTbl.get(id);
		return concat(visit(ctx.rhs), ISTORE, Marshal.bytesS32BE(idx));
	}

	@Override
	public byte[] visitPrint(PrintContext ctx) {
		byte[] cld = visitChildren(ctx);
		return concat(cld, PRINT);
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
