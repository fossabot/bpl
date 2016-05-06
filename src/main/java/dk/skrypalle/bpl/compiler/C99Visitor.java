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
import static dk.skrypalle.bpl.util.Parse.*;

public class C99Visitor extends BPLBaseVisitor<String> {

	private final FuncTbl              funcTbl;
	private final Deque<Type>          tStack;
	private final Deque<Deque<String>> defers;

	private boolean isDeferred;
	private String  deferredArgs;
	private int     deferCnt;

	private Func curF;
	private Type curT;

	public C99Visitor(FuncTbl funcTbl) {
		this.funcTbl = funcTbl;
		this.tStack = new ArrayDeque<>();
		this.defers = new ArrayDeque<>();
		isDeferred = false;
		deferredArgs = "";
		deferCnt = 0;
	}

	@Override
	public String visitCompilationUnit(CompilationUnitContext ctx) {
		String cld = visitChildren(ctx);

		StringBuilder protos = new StringBuilder();
		for (Func f : funcTbl.flatten()) {
			if ("main".equals(f.id))
				continue;

			protos.append("static ").append(f.type.c_type).append(" ").append(mangle(f)).append("(");
			List<Type> paramTypes = f.symTbl.getParamTypes();
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

	//region stmt

	@Override
	public String visitSingularStmt(SingularStmtContext ctx) {
		if (curF.returns)
			throw new BPLCErrStatementUnreachable(ctx.start);

		String cld = visitChildren(ctx);
		if (cld.trim().endsWith(";") || cld.trim().endsWith("}"))
			return cld;
		return cld + ";\n";
	}

	@Override
	public String visitDeferrableStmt(DeferrableStmtContext ctx) {
		if (curF.returns)
			throw new BPLCErrStatementUnreachable(ctx.start);

		String cld = visitChildren(ctx);
		if (cld.trim().endsWith(";") || cld.trim().endsWith("}"))
			return cld;
		return cld + ";\n";
	}

	@Override
	public String visitStmt(StmtContext ctx) {
		if (curF.returns)
			throw new BPLCErrStatementUnreachable(ctx.start);

		String cld = visitChildren(ctx);
		if (cld.trim().endsWith(";") || cld.trim().endsWith("}"))
			return cld;
		return cld + ";\n";
	}

	@Override
	public String visitDefer(DeferContext ctx) {
		isDeferred = true;

		StringBuilder decl_buf = new StringBuilder();
		StringBuilder args_buf = new StringBuilder();

		List<ArgContext> argContexts = null;
		Deque<Type> types = new ArrayDeque<>();
		int i = 0;

		if (ctx.rhs.funcCall() != null && ctx.rhs.funcCall().args != null)
			argContexts = ctx.rhs.funcCall().args.arg();
		else if (ctx.rhs.print() != null && ctx.rhs.print().args != null)
			argContexts = ctx.rhs.print().args.arg();

		if (argContexts != null) {
			for (ArgContext actx : argContexts) {
				isDeferred = false;
				String rhs = visit(actx);
				isDeferred = true;
				Type rhs_t = popt();
				types.add(rhs_t);

				String lhs_id = "__$deferred_param_" + deferCnt;
				decl_buf.append(rhs_t.c_type).append(" ").append(lhs_id).append("=").append(rhs).append(";");
				args_buf.append(lhs_id);

				if (i++ < argContexts.size() - 1)
					args_buf.append(",");
				deferCnt++;
			}
			while (!types.isEmpty())
				pusht(types.pop());
			deferredArgs = args_buf.toString();
		}

		String s = visitChildren(ctx);
		isDeferred = false;
		defers.peek().push(s);
		return decl_buf.toString() + " /* " + ttos(ctx) + " */\n";
	}

	@Override
	public String visitRet(RetContext ctx) {
		curF.returns = true;
		String val = visitChildren(ctx);
		Type have = popt();
		Type want = curF.type;
		if (have != want)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);

		StringBuilder buf = new StringBuilder();
		for (Deque<String> scope : defers) {
			for (String defer : scope)
				buf.append(defer);
		}

		return buf + "return " + val;
	}

	@Override
	public String visitPrint(PrintContext ctx) {
		String args_str = isDeferred ? deferredArgs : visit(ctx.args);

		Deque<Type> types = new ArrayDeque<>();
		if (ctx.args != null) {
			for (int i = 0; i < ctx.args.arg().size(); i++)
				types.push(popt());
		}

		StringBuilder fmt_buf = new StringBuilder();
		while (!types.isEmpty()) {
			Type t = types.pop();
			if (t instanceof PtrType) {
				fmt_buf.append("%p");
			} else {
				//fmt:off
				switch (t.name) { // TODO
				case "int"   : fmt_buf.append("%llx"); break;
				case "string": fmt_buf.append("%s");   break;
				default      : throw new IllegalStateException("unreachable");
				}
				//fmt:on
			}
		}
		return "printf(\"" + fmt_buf + "\", " + args_str + ")";
	}

	@Override
	public String visitBranch(BranchContext ctx) {
		boolean trueRet;
		boolean falseRet = false;

		String cond_str = visit(ctx.cond);
		Type cond_t = popt();
		if (cond_t != Types.lookup("int"))
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.cond), cond_t, Types.lookup("int"));

		curF.returns = false;
		String onTrue = visit(ctx.onTrue).trim();
		trueRet = curF.returns;

		String onFalse = "";
		if (ctx.onFalse != null) {
			curF.returns = false;
			onFalse = " else " + visit(ctx.onFalse).trim();
			falseRet = curF.returns;
		}

		curF.returns = trueRet && falseRet;

		return "if (" + cond_str + ") " + onTrue + onFalse;
	}

	@Override
	public String visitLoop(LoopContext ctx) {
		String cond_str = visit(ctx.cond);
		Type cond_t = popt();
		if (cond_t != Types.lookup("int"))
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.cond), cond_t, Types.lookup("int"));

		return "while (" + cond_str + ") " + visit(ctx.body);
	}

	@Override
	public String visitBlock(BlockContext ctx) {
		defers.push(new ArrayDeque<>());
		curF.symTbl.pushScope();

		StringBuilder buf = new StringBuilder();
		String cld = visitChildren(ctx);

		if (!curF.returns) {
			for (String defer : defers.peek())
				buf.append(defer);
		}

		curF.symTbl.popScope();
		defers.pop();

		return "{\n" + buf + cld + "}";
	}

	//endregion

	//region var

	@Override
	public String visitVarDecl(VarDeclContext ctx) {
		String id = ttos(ctx.id);
		visit(ctx.typ);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		curF.symTbl.declLocal(id, curT);
		return curT.c_type + " " + id;
	}

	@Override
	public String visitVarAssign(VarAssignContext ctx) {
		if (!(ctx.lhs instanceof IdExprContext) && !(ctx.lhs instanceof DerefExprContext))
			throw new BPLCErrUnassignable(TokenAdapter.from(ctx.lhs));

		String lhs = visit(ctx.lhs);
		Type lhs_t = popt();
		String rhs = visit(ctx.rhs);
		Type rhs_t = popt();
		if (lhs_t != rhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), rhs_t, lhs_t);

		return lhs + "=" + rhs;
	}

	@Override
	public String visitVarDeclAssign(VarDeclAssignContext ctx) {
		String id = ttos(ctx.lhs);
		visit(ctx.typ);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.lhs);

		Symbol sym = curF.symTbl.declLocal(id, curT);
		String rhs = visit(ctx.rhs);
		Type have = popt();
		Type want = sym.type;
		if (have != want)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);

		return curT.c_type + " " + id + " = " + rhs;
	}

	@Override
	public String visitVarDeclAssignTI(VarDeclAssignTIContext ctx) {
		String id = ttos(ctx.lhs);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.lhs);

		String rhs = visit(ctx.rhs);
		Type type = popt();
		curF.symTbl.declLocal(id, type);

		return type.c_type + " " + id + " = " + rhs;
	}

	//endregion

	//region func

	@Override
	public String visitFuncDecl(FuncDeclContext ctx) {
		deferCnt = 0;
		if (!tStack.isEmpty())
			throw new IllegalStateException("typeStack not empty on func decl start");

		String id = ttos(ctx.id);
		List<Type> params = new ArrayList<>();
		if (ctx.params != null) {
			for (ParamContext pctx : ctx.params.param()) {
				visit(pctx.typ);
				params.add(curT);
			}
		}

		curF = funcTbl.get(id, params);
		if (curF == null)
			throw new NullPointerException(id + " :: " + params);
		curF.entry = -1; // unused by C99 target
		curF.returns = false;

		String params_s = visit(ctx.params);
		String body_s = visit(ctx.body);

		if (!curF.returns)
			throw new BPLCErrReturnMissing(ctx.stop);

		String ret_t = curF.type.c_type; // FIXME temporary to please GCC
		if ("main".equals(id))
			ret_t = "int";
		String res = ret_t + " " + mangle(curF) + "(" + params_s + ")" + body_s;

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

		String args_str = isDeferred ? deferredArgs : visit(ctx.args);
		List<Type> arg_types = new ArrayList<>();
		for (int i = 0; i < nArgs; i++) {
			Type t = popt();
			arg_types.add(t);
		}
		Collections.reverse(arg_types); // reverse stack-order

		// No function with name 'id' declared
		if (!funcTbl.isDecl(id))
			throw new BPLCErrFuncUndeclared(ctx.id, arg_types);

		// Check possible params
		List<List<Type>> possible = funcTbl.getPossibleParams(id);
		boolean isSubset = false;
		for (List<Type> poss : possible) {
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
		possible.sort((l, r) -> {
			if (l.size() < r.size()) // sort [int] before [int, int]
				return -1;
			if (l.size() > r.size()) // sort [int, int] after [int]
				return 1;

			int len = l.size();
			for (int i = 0; i < len; i++) {
				int cmp = l.get(i).compareTo(r.get(i));
				if (cmp != 0)
					return cmp; // sort [int, string] before [string, int]
			}
			return 0; // means that we have 2 overloads with the same signature, may not happen
		});

		int actArgs = ctx.args == null ? 0 : ctx.args.arg().size();
		int[] expArgs = funcTbl.getOverloadedParams(id);
		boolean numArgsOK = Arrays.binarySearch(expArgs, actArgs) >= 0;
		if (isSubset) {
			if (!numArgsOK)
				throw new BPLCErrWrongNumArgs(ctx.id, arg_types, possible);
		} else {
			if (!numArgsOK)
				throw new BPLCErrFuncUndeclared(ctx.id, arg_types);
			throw new BPLCErrWrongArgTypes(ctx.id, arg_types, possible);
		}

		Func f = funcTbl.get(id, arg_types);

		// Don't push the return type if the call was a stand-alone statement
		if (!(ctx.getParent() instanceof StmtContext) && !(ctx.getParent() instanceof DeferrableStmtContext))
			pusht(f.type);

		return mangle(f) + "(" + args_str + ")";
	}

	@Override
	public String visitParamList(ParamListContext ctx) {
		return visit(ctx.param(), ", ");
	}

	@Override
	public String visitParam(ParamContext ctx) {
		String id = ttos(ctx.id);
		visit(ctx.typ);
		return curT.c_type + " " + id;
	}

	@Override
	public String visitArgList(ArgListContext ctx) {
		return visit(ctx.arg(), ", ");
	}

	@Override
	public String visitArg(ArgContext ctx) {
		return visit(ctx.expr());
	}

	//endregion

	//region expr

	@Override
	public String visitRefExpr(RefExprContext ctx) {
		if (!(ctx.rhs instanceof IdExprContext))
			throw new BPLCErrUnaddressable(TokenAdapter.from(ctx.rhs));
		String cld = visit(ctx.rhs);
		Type type = popt();
		type = Types.ref(type);
		pusht(type);
		return "(&" + cld + ")";
	}

	@Override
	public String visitDerefExpr(DerefExprContext ctx) {
		String cld = visit(ctx.rhs);
		Type type = popt();
		if (!(ctx.rhs instanceof IdExprContext)
			&& !(ctx.rhs instanceof FuncCallExprContext)
			&& !(ctx.rhs instanceof DerefExprContext)
			&& !(ctx.rhs instanceof RefExprContext))
			throw new BPLCErrInvalidDereference(TokenAdapter.from(ctx.rhs), type);

		if (!(type instanceof PtrType))
			throw new BPLCErrInvalidDereference(TokenAdapter.from(ctx.rhs), type);
		type = Types.deref((PtrType) type);
		pusht(type);
		return "(*" + cld + ")";
	}

	@Override
	public String visitBinOpExpr(BinOpExprContext ctx) {
		String lhs = visit(ctx.lhs);
		String rhs = visit(ctx.rhs);
		String op_str = ttos(ctx.op);
		Type rhs_t = popt();
		Type lhs_t = popt();
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(Types.lookup("int"), Types.lookup("int")));
		pusht(Types.lookup("int"));
		return lhs + op_str + rhs;
	}

	@Override
	public String visitBoolOpExpr(BoolOpExprContext ctx) {
		String lhs = visit(ctx.lhs);
		String rhs = visit(ctx.rhs);
		String op_str = ttos(ctx.op);
		Type rhs_t = popt();
		Type lhs_t = popt();
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(Types.lookup("int"), Types.lookup("int")));
		pusht(Types.lookup("int"));
		return "(" + lhs + op_str + rhs + ")";
	}

	@Override
	public String visitFuncCallExpr(FuncCallExprContext ctx) {
		return visit(ctx.funcCall());
	}

	@Override
	public String visitStrExpr(StrExprContext ctx) {
		String val = ttos(ctx.val);
		pusht(Types.lookup("string"));
		return val;
	}

	@Override
	public String visitIntExpr(IntExprContext ctx) {
		BigInteger i = new BigInteger(ttos(ctx.val));
		String val = i.toString();

		pusht(Types.lookup("int"));
		return val + "LL";
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

	//region type

	@Override
	public String visitIdType(IdTypeContext ctx) {
		String type_str = ttos(ctx.id);
		curT = Types.lookup(type_str);
		if (curT == null)
			throw new BPLCErrTypeUndeclared(ctx.id);
		return "";
	}

	@Override
	public String visitPtrType(PtrTypeContext ctx) {
		visitChildren(ctx);
		curT = Types.ref(curT);
		return "";
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
		for (Type t : f.symTbl.getParamTypes())
			buf.append("_").append(t.mangleString());

		return "__$bplc_" + f.id + buf.toString() + "_" + f.type.mangleString();
	}

	private void pusht(Type t) {
		tStack.push(t);
	}

	private Type popt() {
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		if (tStack.isEmpty()) {
			throw new IllegalStateException(String.format(
				"%30s :: POP EMPTY STACK\n", ste.getMethodName()
			));
		}
		return tStack.pop();
	}

}
