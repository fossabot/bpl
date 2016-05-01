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

import java.util.*;

import static dk.skrypalle.bpl.compiler.type.DataType.INT;
import static dk.skrypalle.bpl.compiler.type.DataType.*;
import static dk.skrypalle.bpl.util.Array.*;
import static dk.skrypalle.bpl.util.Parse.*;
import static dk.skrypalle.bpl.vm.Bytecode.*;

public class BCVisitor extends BPLBaseVisitor<byte[]> {

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

	private static final byte[] EMPTY       = {};
	private static final int    PREABLE_LEN = 0x0e; // data_seg_len(4) + data_seg(?) + CALL(8), HALT(1) +1

	private final FuncTbl         funcTbl;
	private final Deque<DataType> tStack;

	private Map<String, Symbol> symTbl;
	private boolean             returns;
	private int                 fOff;

	private Map<String, StaticStoreEntry> staticStore;
	private int staticLen = 0;

	public BCVisitor(FuncTbl funcTbl) {
		this.funcTbl = funcTbl;
		symTbl = new HashMap<>();
		tStack = new ArrayDeque<>();
		staticStore = new HashMap<>();
		fOff = PREABLE_LEN;
	}

	private int nUnresolved() {
		int res = 0;
		for (Func f : funcTbl.flatten()) {
			if (f.entry == Func.ENTRY_UNRESOLVED)
				res++;
		}
		return res;
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
		}

		fOff = PREABLE_LEN + staticLen;
		byte[] cld = visitChildren(ctx);

		byte[] static_b = {};
		TreeMap<Integer, byte[]> tmp = new TreeMap<>();
		for (StaticStoreEntry e : staticStore.values()) {
//			System.out.println(e.toString());
			tmp.put(e.off, e.val);
		}
		for (Map.Entry<Integer, byte[]> e : tmp.entrySet()) {
//			System.out.println("CONCAT::");
//			System.out.println(Hex.dump(e.getValue()));
			static_b = concat(static_b, e.getValue());
		}

		Func main = funcTbl.getFirst("main");

//		System.out.println(Hex.dump(static_b));
//		System.exit(1);

		return concat(
			Marshal.bytesS32BE(staticLen), // data segment len
			static_b,                      // data segment
			CALL, Marshal.bytesS32BE(main.entry), Marshal.bytesS32BE(0), // call main void
			HALT,
			cld
		);
	}

	@Override
	public byte[] visitStmt(StmtContext ctx) {
		if (returns)
			throw new BPLCErrStatementUnreachable(ctx.start);
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitRet(RetContext ctx) {
		returns = true;
		byte[] cld = visitChildren(ctx);
		DataType have = popt();
		DataType want = curF.type;
		if (have != want)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);

		return concat(cld, RET);
	}

	@Override
	public byte[] visitPrint(PrintContext ctx) {
		byte[] cld = visitChildren(ctx);
		DataType type = popt(); // TODO type-check ?
		return concat(cld, PRINT);
	}

	@Override
	public byte[] visitBranch(BranchContext ctx) {
		boolean trueRet;
		boolean falseRet;

		byte[] cond = visit(ctx.cond);
		DataType cond_t = popt();
		if (cond_t != INT)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.cond), cond_t, INT);

		returns = false;
		byte[] onTrue = visit(ctx.onTrue);
		trueRet = returns;

		returns = false;
		byte[] onFalse = visit(ctx.onFalse);
		falseRet = returns;

		returns = trueRet && falseRet;

		return concat(
			cond,
			BRNE, Marshal.bytesS32BE(onFalse.length + 5),
			onFalse,
			JMP, Marshal.bytesS32BE(onTrue.length),
			onTrue
		);
	}

	@Override
	public byte[] visitBlock(BlockContext ctx) {
		Map<String, Symbol> oldSymTbl = symTbl;
		symTbl = new HashMap<>(symTbl);
		byte[] cld = visitChildren(ctx);
		symTbl = oldSymTbl;
		return cld;
	}

	//region var

	@Override
	public byte[] visitVarDecl(VarDeclContext ctx) {
		String id = ttos(ctx.id);
		String type_str = ttos(ctx.typ);
		DataType type = DataType.parse(type_str);
		if (symTbl.containsKey(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		int off = 0;
		for (Symbol sym : symTbl.values()) {
			if (sym.off >= 0)
				off++;
		}

		symTbl.put(id, new Symbol(id, type, off));
		return EMPTY;
	}

	@Override
	public byte[] visitVarAssign(VarAssignContext ctx) {
		String id = ttos(ctx.lhs);
		if (!symTbl.containsKey(id))
			throw new BPLCErrSymUndeclared(ctx.lhs);

		Symbol sym = symTbl.get(id);
		byte[] rhs = visit(ctx.rhs);
		DataType have = popt();
		DataType want = sym.type;
		if (have != want)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx.getParent()), have, want);

		return concat(rhs, ISTORE, Marshal.bytesS32BE(sym.off));
	}

	//endregion

	//region func

	private Func           curF;
	private int            nArgs;
	private int            nParams;
	private List<DataType> params;

	@Override
	public byte[] visitFuncDecl(FuncDeclContext ctx) {
		String id = ttos(ctx.id);
		nParams = ctx.params == null ? 0 : ctx.params.param().size();
//		id = id + ":" + paramstos(nParams) + ">I";
//		curF = funcTbl.get(id);
//		curF.entry = fOff;

		Map<String, Symbol> oldSymTbl = symTbl;
		symTbl = new HashMap<>(symTbl);
		returns = false;
		params = new ArrayList<>();

		byte[] params_b = visit(ctx.params);

		curF = funcTbl.get(id, params);
		curF.entry = fOff;
		params = null;

		byte[] stmts_b = visit(ctx.stmts);

		if (!returns)
			throw new BPLCErrReturnMissing(ctx.stop);

		Map<String, Symbol> curSymTbl = new HashMap<>();
		for (Map.Entry<String, Symbol> e : symTbl.entrySet()) {
			if (oldSymTbl.containsKey(e.getKey()))
				continue;
			curSymTbl.put(e.getKey(), e.getValue());
		}

		int nLocals = 0;
		for (Symbol sym : curSymTbl.values()) {
			if (sym.off >= 0)
				nLocals++;
		}

		symTbl = oldSymTbl;

		byte[] res = concat(params_b, stmts_b);
		if (nLocals > 0)
			res = concat(LOCALS, Marshal.bytesS32BE(nLocals), res);

		fOff += res.length;

		return res;
	}

	@Override
	public byte[] visitFuncCall(FuncCallContext ctx) {
		String id = ttos(ctx.id);
//		int nParams = ctx.args == null ? 0 : ctx.args.arg().size();
//		id = id + ":" + paramstos(nParams) + ">I";

		nArgs = 0;
		byte[] args = visit(ctx.args);
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

//		if (!funcTbl.isDecl(id, arg_types))

		Func f = funcTbl.get(id, arg_types);
		if (f == null) {
			System.out.println(id);
			System.out.println(arg_types);
			System.out.println(funcTbl);
		}

//		if (nArgs != f.params.size())
//			throw new BPLCErrWrongNumArgs(ctx.id, nArgs, f.params.size());

		// Don't push the return type if the call was a stand-alone statement
		if (!(ctx.getParent() instanceof StmtContext))
			pusht(f.type);

		return concat(args, CALL, Marshal.bytesS32BE(f.entry), Marshal.bytesS32BE(nArgs));
	}

	@Override
	public byte[] visitParamList(ParamListContext ctx) {
//		curF.nParams = ctx.param().size();
//		curF.paramCnt = curF.nParams;
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitParam(ParamContext ctx) {
		String id = ttos(ctx.id);
		String type_str = ttos(ctx.typ);
		DataType type = DataType.parse(type_str);
		if (symTbl.containsKey(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		params.add(type);
		int off = -3 - (nParams--);
		symTbl.put(id, new Symbol(id, type, off));
		return EMPTY;
	}

	@Override
	public byte[] visitArgList(ArgListContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public byte[] visitArg(ArgContext ctx) {
		nArgs++;
		return visit(ctx.expr());
	}

	//endregion

	//region expr

	@Override
	public byte[] visitBinOpExpr(BinOpExprContext ctx) {
		byte[] lhs = visit(ctx.lhs);
		byte[] rhs = visit(ctx.rhs);
		String op_str = ttos(ctx.op);
		DataType rhs_t = popt();
		DataType lhs_t = popt();
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(INT, INT));
		pusht(INT);
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
		DataType rhs_t = popt();
		DataType lhs_t = popt();
		if (rhs_t != lhs_t)
			throw new BPLCErrTypeMismatch(TokenAdapter.from(ctx), Arrays.asList(rhs_t, lhs_t), Arrays.asList(INT, INT));
		pusht(INT);
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
	public byte[] visitAssignExpr(AssignExprContext ctx) {
		return visit(ctx.varAssign());
	}

	@Override
	public byte[] visitFuncCallExpr(FuncCallExprContext ctx) {
		return visit(ctx.funcCall());
	}

	@Override
	public byte[] visitStrExpr(StrExprContext ctx) {
		pusht(STRING);
		String val = Parse.ttos(ctx.val);
		val = val.substring(1, val.length() - 1);
		StaticStoreEntry entry = staticStore.get(val);

		if (entry == null) {
			entry = new StaticStoreEntry();
			byte[] data = val.getBytes(IO.UTF8);
			entry.val = concat(Marshal.bytesS32BE(data.length), data);
			for (StaticStoreEntry e : staticStore.values()) {
				entry.off += e.val.length;
			}
			staticStore.put(val, entry);
//			System.out.println("NEW ENTRY:: " + entry);
//			System.out.println("==============");
//			System.out.println("val_bytes \n"+Hex.dump(val.getBytes(IO.UTF8)));
//			System.out.println("data_length\n"+Hex.dump(Marshal.bytesS32BE(data.length)));
//			System.out.println("data \n"+Hex.dump(data));
//			System.out.println("entry_val\n"+Hex.dump(entry.val));
		} else {
//			System.out.println("USING ENTRY:: " + entry);
		}

		byte[] off = Marshal.bytesS32BE(4 + entry.off);
//		byte[] len = Marshal.bytesS32BE(entry.val.length);
		return concat(SPUSH, Marshal.bytesS32BE(STRING.vm_type), off);
	}

	@Override
	public byte[] visitIntExpr(IntExprContext ctx) {
		pusht(INT);
		String val = Parse.ttos(ctx.val);
		byte[] res = Marshal.bytesS64BE(Long.parseUnsignedLong(val, 10));
		return concat(IPUSH, res);
	}

	@Override
	public byte[] visitIdExpr(IdExprContext ctx) {
		String id = ttos(ctx.val);
		if (!symTbl.containsKey(id))
			throw new BPLCErrSymUndeclared(ctx.val);

		Symbol sym = symTbl.get(id);
		pusht(sym.type);
		//fmt:off
		switch (sym.type) {
		case INT   : return concat(ILOAD, Marshal.bytesS32BE(sym.off));
		case STRING: return concat(SLOAD, Marshal.bytesS32BE(sym.off));
		default    : throw new IllegalStateException("unreachable");
		}
		//fmt:on
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

	private byte[] visit(List<? extends ParseTree> l) {
		byte[] res = {};
		for (ParseTree t : l)
			res = aggregateResult(res, visit(t));
		return res;
	}

	//endregion

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
