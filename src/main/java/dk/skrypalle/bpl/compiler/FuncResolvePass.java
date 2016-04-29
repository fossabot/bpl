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

import java.util.*;

import static dk.skrypalle.bpl.antlr.BPLParser.*;
import static dk.skrypalle.bpl.util.Parse.*;

public class FuncResolvePass extends BPLBaseVisitor<Map<String, Func>> {

	private static final Map<String, Func> EMPTY = new HashMap<>();

	private final Map<String, Func> funcTbl;

	private Func curF;

	public FuncResolvePass() {
		funcTbl = new HashMap<>();
	}

	@Override
	public Map<String, Func> visitCompilationUnit(CompilationUnitContext ctx) {
		visitChildren(ctx);

		if (!funcTbl.containsKey("main"))
			throw new IllegalStateException("no main function found"); // TODO

		return funcTbl;
	}

	@Override
	public Map<String, Func> visitFuncDecl(FuncDeclContext ctx) {
		String id = ttos(ctx.id);
		if (funcTbl.containsKey(id))
			throw new BPLCErrFuncRedeclared(ctx.id);

		curF = new Func();
		curF.id = id;
		curF.entry = -1;

		funcTbl.put(id, curF);

		visit(ctx.params);

		return funcTbl;
	}

	@Override
	public Map<String, Func> visitParamList(ParamListContext ctx) {
		curF.nParams = ctx.param().size();
		curF.paramCnt = ctx.param().size();
		return funcTbl;
	}

	//region aggregate, default, visit

	@Override
	protected Map<String, Func> aggregateResult(Map<String, Func> agg, Map<String, Func> nxt) {
		if (agg == null) return nxt;
		if (nxt == null) return agg;

		Map<String, Func> res = new HashMap<>(agg);
		res.putAll(nxt);
		return res;
	}

	@Override
	protected Map<String, Func> defaultResult() {
		return EMPTY;
	}

	@Override
	public Map<String, Func> visit(ParseTree t) {
		return t == null ? defaultResult() : t.accept(this);
	}

	//endregion

}
