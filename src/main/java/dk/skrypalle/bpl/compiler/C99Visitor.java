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
import dk.skrypalle.bpl.compiler.err.*;
import dk.skrypalle.bpl.compiler.type.*;
import dk.skrypalle.bpl.util.*;
import org.antlr.v4.runtime.tree.*;

import java.math.*;
import java.util.*;

import static dk.skrypalle.bpl.antlr.BPLParser.*;
import static dk.skrypalle.bpl.util.Parse.*;

public class C99Visitor extends BPLBaseVisitor<String> {

	private final Map<String, Func> funcTbl;

	private Map<String, Integer> symTbl;
	private boolean              returns;

	public C99Visitor(Map<String, Func> funcTbl) {
		this.funcTbl = funcTbl;
		symTbl = new HashMap<>();
	}

	@Override
	public String visitCompilationUnit(CompilationUnitContext ctx) {
		String cld = visitChildren(ctx);

		StringBuilder protos = new StringBuilder();
		for (Func f : funcTbl.values()) {
			if ("main:V>I".equals(f.id))
				continue;
			protos.append("static int64_t ").append(mangle(f.id)).append("(");
			for (int i = 0; i < f.nParams; i++) {
				protos.append("int64_t");
				if (i < f.nParams - 1)
					protos.append(",");
			}
			protos.append(");\n");
		}

		return String.join("\n",
			"#include <stdio.h>",
			"#include <stdint.h>",
			"",
			protos,
			cld.trim(),
			""
		);
	}

	@Override
	public String visitStmt(StmtContext ctx) {
		return visitChildren(ctx) + ";\n";
	}

	@Override
	public String visitRet(RetContext ctx) {
//		if (returns)
//			throw new IllegalStateException("function already returned"); // TODO
		returns = true;

		return "return " + visitChildren(ctx);
	}

	@Override
	public String visitPrint(PrintContext ctx) {
		return "printf(\"%llx\", " + visitChildren(ctx) + ")";
	}

	@Override
	public String visitBranch(BranchContext ctx) {
		return String.join("\n",
			"if (" + visit(ctx.cond) + ") {",
			visit(ctx.onTrue).trim(),
			"} else {",
			visit(ctx.onFalse).trim(),
			"}"
		);
	}

	@Override
	public String visitBlock(BlockContext ctx) {
		Map<String, Integer> oldSymTbl = symTbl;
		symTbl = new HashMap<>(symTbl);
		String cld = visitChildren(ctx);
		symTbl = oldSymTbl;
		return cld;
	}

	//region var

	@Override
	public String visitVarDecl(VarDeclContext ctx) {
		String id = ttos(ctx.id);
		if (symTbl.containsKey(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		symTbl.put(id, symTbl.size());
		return "int64_t " + id;
	}

	@Override
	public String visitVarAssign(VarAssignContext ctx) {
		String id = ttos(ctx.lhs);
		if (!symTbl.containsKey(id))
			throw new BPLCErrSymUndeclared(ctx.lhs);

		return id + "=" + visit(ctx.rhs);
	}

	//endregion

	//region func

	private Func curF;
	private int  nArgs;

	@Override
	public String visitFuncDecl(FuncDeclContext ctx) {
		String id = ttos(ctx.id);
		int nParams = ctx.params == null ? 0 : ctx.params.param().size();
		id = id + ":" + paramstos(nParams) + ">I";

		curF = funcTbl.get(id);
		curF.id = id;

		Map<String, Integer> oldSymTbl = symTbl;
		symTbl = new HashMap<>(symTbl);
		returns = false;

		String ret_t = "int64_t";
		if ("main:V>I".equals(id))
			ret_t = "int";

		String res = String.join("\n",
			ret_t + " " + mangle(id) + "(" + visit(ctx.params) + ")",
			"{",
			visit(ctx.stmts, "").trim(),
			"}",
			""
		);

		if (!returns)
			throw new BPLCErrReturnMissing(ctx.stop);
		symTbl = oldSymTbl;
		return res;
	}

	@Override
	public String visitFuncCall(FuncCallContext ctx) {
		String id = ttos(ctx.id);
		int nParams = ctx.args == null ? 0 : ctx.args.arg().size();
		id = id + ":" + paramstos(nParams) + ">I";

		List<Integer> ovlds = new ArrayList<>();
		for (Map.Entry<String, Func> e : funcTbl.entrySet()) {
			String k = e.getKey();
			int iCol = k.indexOf(':');
			String name = k.substring(0, iCol);

			if (ttos(ctx.id).equals(name)) {
				// Found base_name, check params
				int have = nParams;
				int want = e.getValue().nParams;
				if (have != want)
					ovlds.add(want);
			}
		}

		if (!ovlds.isEmpty()) {
			int[] wants = Array.toIntArray(ovlds.toArray(new Integer[ovlds.size()]));
			throw new BPLCErrWrongNumArgs(ctx.id, nParams, wants);
		}

		if (!funcTbl.containsKey(id))
			throw new BPLCErrFuncUndeclared(ctx.id);

		nArgs = 0;
		String args = visit(ctx.args);

		Func f = funcTbl.get(id);
		if (nArgs != f.nParams)
			throw new BPLCErrWrongNumArgs(ctx.id, nArgs, f.nParams);

		return mangle(id) + "(" + args + ")";
	}

	@Override
	public String visitParamList(ParamListContext ctx) {
		curF.nParams = ctx.param().size();
		curF.paramCnt = curF.nParams;
		return visit(ctx.param(), ", ");
	}

	@Override
	public String visitParam(ParamContext ctx) {
		String id = ttos(ctx.id);
		if (symTbl.containsKey(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		symTbl.put(id, symTbl.size());
		return "int64_t " + id;
	}

	@Override
	public String visitArgList(ArgListContext ctx) {
		return visit(ctx.arg(), ", ");
	}

	@Override
	public String visitArg(ArgContext ctx) {
		nArgs++;
		return visit(ctx.expr());
	}

	//endregion

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
	public String visitFuncCallExpr(FuncCallExprContext ctx) {
		return visit(ctx.funcCall());
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
		String id = ttos(ctx.val);
		if (!symTbl.containsKey(id))
			throw new BPLCErrSymUndeclared(ctx.val);
		return id;
	}

	//endregion

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

	private String visit(List<? extends ParseTree> list, String delim) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (ParseTree t : list) {
			sb.append(visit(t));
			if (++i < list.size())
				sb.append(delim);
		}
		return sb.toString();
	}

	//endregion

	private String mangle(String internal) {
		int iCln = internal.indexOf(':');
		int iRsh = internal.indexOf('>');

		String id = internal.substring(0, iCln);
		String args = internal.substring(iCln + 1, iRsh);
		String ret = internal.substring(iRsh + 1);

		if ("main".equals(id))
			return id;

		return "__bplc_" + id + "_" + args + "_" + ret;
	}

}
