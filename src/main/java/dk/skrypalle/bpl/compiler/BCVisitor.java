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
import dk.skrypalle.bpl.compiler.err.*;
import dk.skrypalle.bpl.util.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

import static dk.skrypalle.bpl.util.Array.*;
import static dk.skrypalle.bpl.util.Parse.*;
import static dk.skrypalle.bpl.vm.Bytecode.*;

public class BCVisitor extends BPLBaseVisitor<byte[]> {

	private static final byte[] EMPTY = {};

	private final Map<String, Integer> funcTbl;

	private Map<String, Integer> symTbl;
	private boolean              returns;
	private int                  fOff;

	public BCVisitor() {
		funcTbl = new HashMap<>();
		symTbl = new HashMap<>();
		fOff = 10; // CALL(8), HALT
	}

	@Override
	public byte[] visitCompilationUnit(CompilationUnitContext ctx) {
		byte[] cld = visitChildren(ctx);
		if (!funcTbl.containsKey("main"))
			throw new IllegalStateException(); // TODO

		return concat(CALL, Marshal.bytesS32BE(funcTbl.get("main")), Marshal.bytesS32BE(0), HALT, cld);
	}

	@Override
	public byte[] visitStmt(StmtContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitRet(RetContext ctx) {
		if (returns)
			throw new IllegalStateException(); // TODO
		returns = true;

		return concat(visitChildren(ctx), RET);
	}

	@Override
	public byte[] visitPrint(PrintContext ctx) {
		byte[] cld = visitChildren(ctx);
		return concat(cld, PRINT);
	}

	//region var

	@Override
	public byte[] visitVarDecl(VarDeclContext ctx) {
		String id = ttos(ctx.id);
		if (symTbl.containsKey(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		symTbl.put(id, symTbl.size());
		return EMPTY;
	}

	@Override
	public byte[] visitVarAssign(VarAssignContext ctx) {
		String id = ttos(ctx.lhs);
		if (!symTbl.containsKey(id))
			throw new BPLCErrSymUndeclared(ctx.lhs);

		int idx = symTbl.get(id);
		return concat(visit(ctx.rhs), ISTORE, Marshal.bytesS32BE(idx));
	}

	//endregion

	//region func

	@Override
	public byte[] visitFuncDecl(FuncDeclContext ctx) {
		String id = ttos(ctx.id);
		if (funcTbl.containsKey(id))
			throw new IllegalStateException(); // TODO
		funcTbl.put(id, fOff);

		Map<String, Integer> oldSymTbl = symTbl;
		symTbl = new HashMap<>(symTbl);
		returns = false;
		int nOldSyms = symTbl.size();

		byte[] res = visitChildren(ctx);

		if (!returns)
			throw new IllegalStateException(); // TODO

		int nSyms = symTbl.size() - nOldSyms;
		symTbl = oldSymTbl;

		if (nSyms > 0)
			res = concat(LOCALS, Marshal.bytesS32BE(nSyms), res);

		fOff += res.length;

		return res;
	}

	@Override
	public byte[] visitFuncCall(FuncCallContext ctx) {
		String id = ttos(ctx.id);
		if (!funcTbl.containsKey(id))
			throw new IllegalStateException(); // TODO

		int addr = funcTbl.get(id);
		return concat(CALL, Marshal.bytesS32BE(addr), Marshal.bytesS32BE(0));
	}

	//endregion

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
	public byte[] visitFuncCallExpr(FuncCallExprContext ctx) {
		return visit(ctx.funcCall());
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
		if (!symTbl.containsKey(id))
			throw new BPLCErrSymUndeclared(ctx.val);

		int idx = symTbl.get(id);
		return concat(ILOAD, Marshal.bytesS32BE(idx));
	}

	//endregion

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
