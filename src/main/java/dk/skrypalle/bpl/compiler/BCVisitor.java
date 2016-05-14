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
import dk.skrypalle.bpl.compiler.type.*;
import dk.skrypalle.bpl.util.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.commons.lang3.*;

import java.util.*;

import static dk.skrypalle.bpl.util.Array.*;
import static dk.skrypalle.bpl.util.Parse.*;
import static dk.skrypalle.bpl.vm.Bytecode.*;

public class BCVisitor extends BPLBaseVisitor<byte[]> {

	private static final byte[] EMPTY = {};

	private final FuncTbl              funcTbl;
	private final Deque<Type>          tStack;
	private final Deque<Deque<byte[]>> defers;

	private Map<String, StaticStoreEntry> staticStore;
	private Func                          curF;
	private int                           staticLen;
	private int                           fOff;
	private boolean                       isDeferred;
	private Type                          curT;
	private boolean                       load;

	public BCVisitor(FuncTbl funcTbl) {
		this.funcTbl = funcTbl;
		this.tStack = new ArrayDeque<>();
		this.defers = new ArrayDeque<>();
		this.staticStore = new HashMap<>();
		this.staticLen = 0;
		this.fOff = PREABLE_LEN;
		this.isDeferred = false;
		this.load = true;
	}

	@Override
	public byte[] visitCompilationUnit(CompilationUnitContext ctx) {
		boolean calcStaticLen = false;
		while (nUnresolved() > 0) {
			fOff = PREABLE_LEN + staticLen;
			visitChildren(ctx);
			if (!calcStaticLen) {
				for (StaticStoreEntry e : staticStore.values())
					staticLen += e.val.length;
				calcStaticLen = true;
			}
			for (Func f : funcTbl.flatten())
				f.symTbl.clearLocals();
			defers.clear();
		}

		fOff = PREABLE_LEN + staticLen;
		byte[] cld = visitChildren(ctx);

		byte[] static_b = {};
		// Sort data segment
		TreeMap<Integer, byte[]> tmp = new TreeMap<>();
		for (StaticStoreEntry e : staticStore.values())
			tmp.put(e.off, e.val);
		for (Map.Entry<Integer, byte[]> e : tmp.entrySet())
			static_b = concat(static_b, e.getValue());

		Func main = funcTbl.getFirst("main");

		return concat(
			Marshal.bytesS32BE(staticLen),                               // data segment length
			static_b,                                                    // data segment
			CALL, Marshal.bytesS32BE(main.entry), Marshal.bytesS32BE(0), // call main void
			HALT,
			cld
		);
	}

	//region stmt

	@Override
	public byte[] visitSingularStmt(SingularStmtContext ctx) {
		if (curF.returns)
			throw new BPLCErrStatementUnreachable(ctx.start);
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitDeferrableStmt(DeferrableStmtContext ctx) {
		if (curF.returns)
			throw new BPLCErrStatementUnreachable(ctx.start);
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitStmt(StmtContext ctx) {
		if (curF.returns)
			throw new BPLCErrStatementUnreachable(ctx.start);
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitDefer(DeferContext ctx) {
		isDeferred = true;
		byte[] args = {};

		List<ArgContext> argContexts = null;
		if (ctx.rhs.funcCall() != null && ctx.rhs.funcCall().args != null)
			argContexts = ctx.rhs.funcCall().args.arg();
		else if (ctx.rhs.print() != null && ctx.rhs.print().args != null)
			argContexts = ctx.rhs.print().args.arg();

		if (argContexts != null) {
			for (ArgContext actx : argContexts) {
				isDeferred = false;
				args = concat(args, visit(actx));
				isDeferred = true;
			}
		}

		byte[] s = visitChildren(ctx);
		isDeferred = false;
		defers.peek().push(s);
		return args;
	}

	@Override
	public byte[] visitRet(RetContext ctx) {
		curF.returns = true;
		byte[] cld = visitChildren(ctx);
		Type have;
		Type want = curF.type;

		if (want != Types.VOID) {
			have = popt();
			if (have == Types.VOID)
				throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.expr()));
			if (have != want)
				throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);
		}

		byte[] ll = {};
		for (Deque<byte[]> scope : defers) {
			for (byte[] defer : scope)
				ll = concat(ll, defer);
		}

		return concat(ll, cld, RET);
	}

	@Override
	public byte[] visitPrint(PrintContext ctx) {
		byte[] cld = isDeferred ? new byte[]{} : visitChildren(ctx);

		int n = 0;
		if (ctx.args != null) {
			for (int i = 0; i < ctx.args.arg().size(); i++) {
				popt();
				n++;
			}
		}

		return concat(cld, PRINT, Marshal.bytesS32BE(n));
	}

	@Override
	public byte[] visitBranch(BranchContext ctx) {
		boolean trueRet;
		boolean falseRet;

		byte[] cond = visit(ctx.cond);
		Type cond_t = popt();
		if (cond_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.cond));
		if (cond_t != Types.lookup("int"))
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.cond), cond_t, Types.lookup("int"));

		curF.returns = false;
		byte[] onTrue = visit(ctx.onTrue);
		trueRet = curF.returns;

		curF.returns = false;
		byte[] onFalse = visit(ctx.onFalse);
		falseRet = curF.returns;

		curF.returns = trueRet && falseRet;

		return concat(
			cond,
			BRNE, Marshal.bytesS32BE(onFalse.length + 5),
			onFalse,
			JMP, Marshal.bytesS32BE(onTrue.length),
			onTrue
		);
	}

	@Override
	public byte[] visitLoop(LoopContext ctx) {
		byte[] cond = visit(ctx.cond);
		Type cond_t = popt();
		if (cond_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.cond));
		if (cond_t != Types.lookup("int"))
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.cond), cond_t, Types.lookup("int"));

		byte[] body = visit(ctx.body);
		return concat(
			cond,
			BREQ, Marshal.bytesS32BE(body.length + 5),
			body,
			JMP, Marshal.bytesS32BE(-(body.length + cond.length + 5 + 5))
		);
	}

	@Override
	public byte[] visitBlock(BlockContext ctx) {
		defers.push(new ArrayDeque<>());
		curF.symTbl.pushScope();

		byte[] cld = visitChildren(ctx);

		if (!curF.returns) {
			for (byte[] defer : defers.peek())
				cld = concat(cld, defer);
		}

		curF.symTbl.popScope();
		defers.pop();

		return cld;
	}

	//endregion

	//region var

	@Override
	public byte[] visitVarDecl(VarDeclContext ctx) {
		String id = ttos(ctx.id);
		visit(ctx.typ);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		curF.symTbl.declLocal(id, curT);
		return EMPTY;
	}

	@Override
	public byte[] visitVarAssign(VarAssignContext ctx) {
		if (!(ctx.lhs instanceof IdExprContext) && !(ctx.lhs instanceof DerefExprContext))
			throw new BPLCErrUnassignable(TokenAdapter.from(ctx.lhs));

		load = true;

		byte[] rhs = visit(ctx.rhs);
		Type rhs_t = popt();
		load = false;
		byte[] lhs = visit(ctx.lhs);
		load = true;
		Type lhs_t = popt();
		if (lhs_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.lhs));
		if (rhs_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.rhs));
		if (lhs_t != rhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), rhs_t, lhs_t);

		return concat(lhs, rhs, ISTORE);
	}

	@Override
	public byte[] visitVarDeclAssign(VarDeclAssignContext ctx) {
		String id = ttos(ctx.lhs);
		visit(ctx.typ);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.lhs);

		Symbol sym = curF.symTbl.declLocal(id, curT);
		byte[] rhs = visit(ctx.rhs);
		Type have = popt();
		Type want = sym.type;
		if (have == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.rhs));
		if (have != want)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);

		return concat(IPUSH, Marshal.bytesS64BE(sym.off), rhs, ISTORE);
	}

	@Override
	public byte[] visitVarDeclAssignTI(VarDeclAssignTIContext ctx) {
		String id = ttos(ctx.lhs);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.lhs);

		byte[] rhs = visit(ctx.rhs);
		Type type = popt();
		if (type == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.rhs));

		Symbol sym = curF.symTbl.declLocal(id, type);

		return concat(IPUSH, Marshal.bytesS64BE(sym.off), rhs, ISTORE);
	}

	//endregion

	//region func

	@Override
	public byte[] visitFuncDecl(FuncDeclContext ctx) {
		if (!tStack.isEmpty())
			throw new IllegalStateException("typeStack not empty on func decl start");

		String id = ttos(ctx.id);
		List<Type> params = new ArrayList<>();
		if (ctx.params != null) {
			for (ParamContext pctx : ctx.params.param())
				params.add(Types.lookup((ttos(pctx.typ))));
		}

		curF = funcTbl.get(id, params);
		curF.entry = fOff;
		curF.returns = false;

		byte[] params_b = visit(ctx.params);
		byte[] body_b = visit(ctx.body);

		if (!curF.returns) {
			if (curF.type != Types.VOID)
				throw new BPLCErrReturnMissing(ctx.stop);

			byte[] ll = {};
			for (Deque<byte[]> scope : defers) {
				for (byte[] defer : scope)
					ll = concat(ll, defer);
			}

			// Pushing -1 as return value for VOID, which is non-assignable (void not defined as type)
			// and thus it will get popped off after the call for now
			body_b = concat(body_b, ll, IPUSH, Marshal.bytesS64BE(-1), RET); //FIXME: temp hack
		}

		byte[] res = concat(params_b, body_b);

		int nLocals = curF.symTbl.nLocals();
		if (nLocals > 0)
			res = concat(LOCALS, Marshal.bytesS32BE(nLocals), res);

		fOff += res.length;

		if (!tStack.isEmpty())
			throw new IllegalStateException("typeStack not empty on func decl end: " + tStack);

		return res;
	}

	@Override
	public byte[] visitFuncCall(FuncCallContext ctx) {
		String id = ttos(ctx.id);

		int nArgs = 0;
		if (ctx.args != null)
			nArgs = ctx.args.arg().size();

		byte[] args = isDeferred ? new byte[]{} : visit(ctx.args);

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
		if (ctx.getParent() instanceof DeferrableStmtContext)
			return concat(args, CALL, Marshal.bytesS32BE(f.entry), Marshal.bytesS32BE(nArgs), POP);

		pusht(f.type);
		return concat(args, CALL, Marshal.bytesS32BE(f.entry), Marshal.bytesS32BE(nArgs));
	}

	@Override
	public byte[] visitParamList(ParamListContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitParam(ParamContext ctx) {
		return EMPTY;
	}

	@Override
	public byte[] visitArgList(ArgListContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitArg(ArgContext ctx) {
		byte[] arg = visit(ctx.expr());
		Type type = popt();
		if (type == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.expr()));
		pusht(type);
		return arg;
	}

	//endregion

	//region expr

	@Override
	public byte[] visitRefExpr(RefExprContext ctx) {
		if (!(ctx.rhs instanceof IdExprContext))
			throw new BPLCErrUnaddressable(TokenAdapter.from(ctx.rhs));

		load = false;
		byte[] cld = visitChildren(ctx);
		load = true;
		Type type = popt();
		type = Types.ref(type);
		pusht(type);
		return concat(cld, ADDR_OF);
	}

	@Override
	public byte[] visitDerefExpr(DerefExprContext ctx) {
		byte[] cld = visit(ctx.rhs);
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
		if (!load)
			return concat(cld, ILOAD, RESOLVE);
		return concat(cld, VAL_OF);
	}

	@Override
	public byte[] visitBinOpExpr(BinOpExprContext ctx) {
		byte[] lhs = visit(ctx.lhs);
		byte[] rhs = visit(ctx.rhs);
		String op_str = ttos(ctx.op);
		Type rhs_t = popt();
		Type lhs_t = popt();
		if (lhs_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.lhs));
		if (rhs_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.rhs));
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(Types.lookup("int"), Types.lookup("int")));
		pusht(Types.lookup("int"));
		byte op;
		//fmt:off
		switch (op_str) {
		case "+" : op = IADD; break;
		case "-" : op = ISUB; break;
		case "*" : op = IMUL; break;
		case "/" : op = IDIV; break;
		case "<" : op = ILT;  break;
		case ">" : op = IGT;  break;
		case "<=": op = ILTE; break;
		case ">=": op = IGTE; break;
		case "==": op = IEQ;  break;
		case "!=": op = INEQ; break;
		default  : throw new IllegalStateException("unreachable");
		}
		//fmt:on
		return concat(lhs, rhs, op);
	}

	@Override
	public byte[] visitBoolOpExpr(BoolOpExprContext ctx) {
		byte[] lhs = visit(ctx.lhs);
		byte[] rhs = visit(ctx.rhs);
		String op_str = ttos(ctx.op);
		Type rhs_t = popt();
		Type lhs_t = popt();
		if (lhs_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.lhs));
		if (rhs_t == Types.VOID)
			throw new BPLCErrVoidAsValue(TokenAdapter.from(ctx.rhs));
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(Types.lookup("int"), Types.lookup("int")));
		Type type = Types.lookup("int");
		pusht(type);
		byte op;
		int v0;
		int v1;
		//fmt:off
		switch (op_str) {
		case "&&": op = BREQ; v0 = 1; v1 = 0; break;
		case "||": op = BRNE; v0 = 0; v1 = 1; break;
		default  : throw new IllegalStateException("unreachable");
		}
		//fmt:on

		int off1 = rhs.length + 5 + 9 + 5;
		int off2 = 9 + 5;
		int off3 = 9;

		return concat(
			/* ll=lhs.length;  */
			/* rl=rhs.length;  */
			/* rel. offsets    */
			/*               0 */ lhs,
			/*              ll */ op, Marshal.bytesS32BE(off1),   // op(BRNE/BREQ) :L1
			/*               5 */ rhs,
			/*              rl */ op, Marshal.bytesS32BE(off2),   // op(BRNE/BREQ) :L1
			/*               5 */ IPUSH, Marshal.bytesS64BE(v0),
			/*               9 */ JMP, Marshal.bytesS32BE(off3),  // JMP  :END
			/* L:L1          5 */ IPUSH, Marshal.bytesS64BE(v1)
			/* L:END         9 */
		);
	}

	@Override
	public byte[] visitFuncCallExpr(FuncCallExprContext ctx) {
		return visit(ctx.funcCall());
	}

	@Override
	public byte[] visitStrExpr(StrExprContext ctx) {
		String val = Parse.ttos(ctx.val);
		val = val.substring(1, val.length() - 1);
		val = StringEscapeUtils.unescapeJava(val);
		val = val.replaceAll("\n", System.lineSeparator());
		StaticStoreEntry entry = staticStore.get(val);

		if (entry == null) {
			entry = new StaticStoreEntry();
			byte[] data = val.getBytes(IO.UTF8);
			entry.val = concat(Marshal.bytesS32BE(data.length), data);
			for (StaticStoreEntry e : staticStore.values()) {
				entry.off += e.val.length;
			}
			staticStore.put(val, entry);
		}

		byte[] off = Marshal.bytesS32BE(4 + entry.off);
		pusht(Types.lookup("string"));
		return concat(SPUSH, Marshal.bytesS32BE(Types.lookup("string").vm_type), off);
	}

	@Override
	public byte[] visitIntExpr(IntExprContext ctx) {
		String val = Parse.ttos(ctx.val);
		byte[] res = Marshal.bytesS64BE(Long.parseUnsignedLong(val, 10));
		Type type = Types.lookup("int");
		pusht(type);
		return concat(IPUSH, res);
	}

	@Override
	public byte[] visitIdExpr(IdExprContext ctx) {
		String id = ttos(ctx.val);
		if (!curF.symTbl.isDecl(id))
			throw new BPLCErrSymUndeclared(ctx.val);
		Symbol sym = curF.symTbl.get(id);
		pusht(sym.type);
		byte[] res = concat(IPUSH, Marshal.bytesS64BE(sym.off));
		if (!load)
			return res;

		//fmt:off
		switch (sym.type.name) { // TODO
		case "string": res = concat(res, SLOAD); break;
		default      : res = concat(res, ILOAD); break;
		}
		//fmt:on

		return res;
	}

	//endregion

	//region type

	@Override
	public byte[] visitIdType(IdTypeContext ctx) {
		String type_str = ttos(ctx.id);
		curT = Types.lookup(type_str);
		if (curT == null)
			throw new BPLCErrTypeUndeclared(ctx.id);
		return EMPTY;
	}

	@Override
	public byte[] visitPtrType(PtrTypeContext ctx) {
		visitChildren(ctx);
		curT = Types.ref(curT);
		return EMPTY;
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

	private int nUnresolved() {
		int res = 0;
		for (Func f : funcTbl.flatten()) {
			if (f.entry == Func.ENTRY_UNRESOLVED)
				res++;
		}
		return res;
	}

	//region StaticStoreEntry

	private static class StaticStoreEntry {

		private int    off = 0;
		private byte[] val = null;

		@Override
		public String toString() {
			return "StaticStoreEntry{" +
				"off=" + off +
				", val=" + Arrays.toString(val) +
				", val='" + new String(val, 4, val.length - 4) +
				"'}";
		}

	}

	//endregion

}
