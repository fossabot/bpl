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
import org.antlr.v4.runtime.tree.*;

import java.math.*;
import java.util.*;

import static dk.skrypalle.bpl.antlr.BPLParser.*;
import static dk.skrypalle.bpl.compiler.type.DataType.INT;
import static dk.skrypalle.bpl.compiler.type.DataType.*;
import static dk.skrypalle.bpl.util.Parse.*;

public class C99Visitor extends BPLBaseVisitor<String> {

	private final FuncTbl         funcTbl;
	private final Deque<DataType> tStack;

	//	private Map<String, Symbol> symTbl;
	private boolean returns;

	public C99Visitor(FuncTbl funcTbl) {
		this.funcTbl = funcTbl;
//		symTbl = new HashMap<>();
		tStack = new ArrayDeque<>();
	}

	@Override
	public String visitCompilationUnit(CompilationUnitContext ctx) {
		String cld = visitChildren(ctx);

		StringBuilder protos = new StringBuilder();
		for (Func f : funcTbl.flatten()) {
			if ("main".equals(f.id))
				continue;

			protos.append("static ").append(f.type.c_type).append(" ").append(mangle(f)).append("(");
			List<DataType> paramTypes = f.symTbl.getParamTypes();
			for (int i = 0; i < paramTypes.size(); i++) {
				protos.append(paramTypes.get(i).c_type);
				if (i < paramTypes.size() - 1)
					protos.append(",");
			}
			if (paramTypes.isEmpty())
				protos.append("void");
			protos.append(");\n");
		}

		return String.join("\n",
			"#include <stdio.h>",
			"#include <stdint.h>",
			"",
			protos,
			cld,
			""
		);
	}

	@Override
	public String visitStmt(StmtContext ctx) {
		if (returns)
			throw new BPLCErrStatementUnreachable(ctx.start);
		return visitChildren(ctx) + ";\n";
	}

	@Override
	public String visitRet(RetContext ctx) {
		returns = true;
		String val = visitChildren(ctx);
		DataType have = popt();
		DataType want = curF.type;
		if (have != want)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);

		return "return " + val;
	}

	@Override
	public String visitPrint(PrintContext ctx) {
		String val = visitChildren(ctx);
		DataType type = popt();
		String fmt;
		//fmt:off
		switch (type) {
		case INT   : fmt = "%llx"; break;
		case STRING: fmt = "%s";   break;
		default    : throw new IllegalStateException("unreachable");
		}
		//fmt:on
		return "printf(\"" + fmt + "\", " + val + ")";
	}

	@Override
	public String visitBranch(BranchContext ctx) {
		boolean trueRet;
		boolean falseRet;

		String cond_str = visit(ctx.cond);
		DataType cond_t = popt();
		if (cond_t != INT)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.cond), cond_t, INT);

		returns = false;
		String onTrue = visit(ctx.onTrue).trim();
		trueRet = returns;

		returns = false;
		String onFalse = visit(ctx.onFalse).trim();
		falseRet = returns;

		returns = trueRet && falseRet;

		return "if (" + cond_str + ") " + onTrue + " else " + onFalse;
	}

	@Override
	public String visitLoop(LoopContext ctx) {
		String cond_str = visit(ctx.cond);
		DataType cond_t = popt();
		if (cond_t != INT)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.cond), cond_t, INT);

		return "while (" + cond_str + ") " + visit(ctx.body);
	}

	@Override
	public String visitBlock(BlockContext ctx) {
//		Map<String, Symbol> oldSymTbl = symTbl;
//		symTbl = new HashMap<>(symTbl);
		curF.symTbl.pushScope();
		String cld = visitChildren(ctx);
		curF.symTbl.popScope();
//		symTbl = oldSymTbl;
		return "{" + cld + "}";
	}

	//region var

	@Override
	public String visitVarDecl(VarDeclContext ctx) {
		String id = ttos(ctx.id);
		String type_str = ttos(ctx.typ);
		DataType type = DataType.parse(type_str);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		curF.symTbl.declLocal(id, type);
		return type.c_type + " " + id;
	}

	@Override
	public String visitVarAssign(VarAssignContext ctx) {
		String id = ttos(ctx.lhs);
		if (!curF.symTbl.isDecl(id))
			throw new BPLCErrSymUndeclared(ctx.lhs);

		Symbol sym = curF.symTbl.get(id);
		String rhs = visit(ctx.rhs);
		DataType have = popt();
		DataType want = sym.type;
		if (have != want)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);

		return id + "=" + rhs;
	}

	//endregion

	//region func

	private Func curF;
//	private int  nArgs;
//	private List<DataType> params;

	@Override
	public String visitFuncDecl(FuncDeclContext ctx) {
		if (!tStack.isEmpty())
			throw new IllegalStateException("typeStack not empty on func decl start");

		String id = ttos(ctx.id);
		List<DataType> params = new ArrayList<>();
		if (ctx.params != null) {
			for (ParamContext pctx : ctx.params.param())
				params.add(DataType.parse(ttos(pctx.typ)));
		}
		returns = false;

//		System.out.println(id + " :: params: " + params);
		curF = funcTbl.get(id, params);
//		System.out.println("GREP ME " + curF);

//		Map<String, Symbol> oldSymTbl = symTbl;
//		symTbl = new HashMap<>(symTbl);
//		params = new ArrayList<>();

//		curF = new Func();// FIXME hack:: visitParams needs symTbl, need params to get correct func
//		curF.symTbl.pushScope();// FIXME hack

//		List<DataType> prms = new ArrayList<>();
//		if (ctx.params != null) {
//			for (ParamContext pctx : ctx.params.param()) {
//				prms.add(DataType.parse(ttos(pctx.typ)));
//			}
//		}

//		SymTbl st = curF.symTbl; // FIXME hack
//		curF = funcTbl.get(id, prms);
//		curF.symTbl = st;// FIXME hack
//		params = null;

//		curF.symTbl.pushScope();
//		String param_str = visit(ctx.params);

		String ret_t = curF.type.c_type; // FIXME temporary to please GCC
		if ("main".equals(id))
			ret_t = "int";

		String res = String.join("\n",
			ret_t + " " + mangle(curF) + "(" + visit(ctx.params) + ")",
			visit(ctx.body)
		);

		if (!returns)
			throw new BPLCErrReturnMissing(ctx.stop);
//		symTbl = oldSymTbl;
//		curF.symTbl.popScope();

		if (!tStack.isEmpty())
			throw new IllegalStateException("typeStack not empty on func decl end");

		return res;
	}

	@Override
	public String visitFuncCall(FuncCallContext ctx) {
		String id = ttos(ctx.id);

		int nArgs = 0;
		if (ctx.args != null)
			nArgs = ctx.args.arg().size();

		String args_str = visit(ctx.args);

		List<DataType> arg_types = new ArrayList<>();
		for (int i = 0; i < nArgs; i++) {
			DataType t = popt();
			arg_types.add(t);
		}
		Collections.reverse(arg_types); // reverse stack-order

		// No function with name 'id' declared
		if (!funcTbl.isDecl(id))
			throw new BPLCErrFuncUndeclared(ctx.id, arg_types);

		// Check possible params
		List<List<DataType>> possible = funcTbl.getPossibleParams(id);
		boolean isSubset = false;
		for (List<DataType> poss : possible) {
			boolean isSS = true;
			int len = arg_types.size() > poss.size() ? poss.size() : arg_types.size();
			for (int i = 0; i < len; i++) {
				if (poss.get(i) != arg_types.get(i)) {
					isSS = false;
					break;
				}
			}

			if (isSS) {
				isSubset = true;
				break;
			}
		}

		if (isSubset) {
			int actArgs = ctx.args == null ? 0 : ctx.args.arg().size();
			int[] expArgs = funcTbl.getOverloadedParams(id);
			if (Arrays.binarySearch(expArgs, actArgs) < 0) {
				// # of provided args not found in overloads
				throw new BPLCErrWrongNumArgs(ctx.id, arg_types, possible);
			}
		} else {
			throw new BPLCErrFuncUndeclared(ctx.id, arg_types);
		}

		Func f = funcTbl.get(id, arg_types);

		// Don't push the return type if the call was a stand-alone statement
		if (!(ctx.getParent() instanceof StmtContext) && !(ctx.getParent() instanceof SimpleStmtContext))
			pusht(f.type);

		return mangle(f) + "(" + args_str + ")";
	}

	@Override
	public String visitParamList(ParamListContext ctx) {
//		curF.nParams = ctx.param().size();
//		curF.paramCnt = curF.nParams;
		return visit(ctx.param(), ", ");
	}

	@Override
	public String visitParam(ParamContext ctx) {
		String id = ttos(ctx.id);
		String type_str = ttos(ctx.typ);
		DataType type = DataType.parse(type_str);
//		if (curF.symTbl.isDecl(id))
//			throw new BPLCErrSymRedeclared(ctx.id);

//		params.add(type);
//		curF.symTbl.declParam(id, type);
		return type.c_type + " " + id;
	}

	@Override
	public String visitArgList(ArgListContext ctx) {
		return visit(ctx.arg(), ", ");
	}

	@Override
	public String visitArg(ArgContext ctx) {
//		nArgs++;
		return visit(ctx.expr());
	}

	//endregion

	//region expr

	@Override
	public String visitBinOpExpr(BinOpExprContext ctx) {
		String res = visit(ctx.lhs) + ttos(ctx.op) + visit(ctx.rhs);
		DataType rhs_t = popt();
		DataType lhs_t = popt();
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(INT, INT));
		pusht(INT);
		return res;
	}

	@Override
	public String visitBoolOpExpr(BoolOpExprContext ctx) {
		String res = "(" + visit(ctx.lhs) + ttos(ctx.op) + visit(ctx.rhs) + ")";
		DataType rhs_t = popt();
		DataType lhs_t = popt();
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(INT, INT));
		pusht(INT);
		return res;
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
	public String visitStrExpr(StrExprContext ctx) {
		String res = ttos(ctx.val);
		pusht(STRING);
		return res;
	}

	@Override
	public String visitIntExpr(IntExprContext ctx) {
		BigInteger i = new BigInteger(ttos(ctx.val));
		String val = i.toString();
		pusht(INT);

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
		if (!curF.symTbl.isDecl(id))
			throw new BPLCErrSymUndeclared(ctx.val);
		Symbol sym = curF.symTbl.get(id);
		pusht(sym.type);
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

	private String mangle(Func f) {
		if ("main".equals(f.id))
			return f.id;

		StringBuilder buf = new StringBuilder();
		for (DataType t : f.symTbl.getParamTypes())
			buf.append("_").append(t);

		return "__$bplc_" + f.id + buf.toString() + "_" + f.type;
	}

	private void pusht(DataType t) {
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		tStack.push(t);
//		System.out.printf("%30s :: PUSH %-6s :: %s\n", ste.getMethodName(), t, tStack);
	}

	private DataType popt() {
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		if (tStack.isEmpty()) {
			throw new IllegalStateException(String.format(
				"%30s :: POP EMPTY STACK\n", ste.getMethodName()
			));
		}
		DataType t = tStack.pop();
//		System.out.printf("%30s :: POP  %-6s :: %s\n", ste.getMethodName(), t, tStack);
		return t;
	}

}
