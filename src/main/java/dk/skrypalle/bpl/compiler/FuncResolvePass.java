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

import static dk.skrypalle.bpl.antlr.BPLParser.*;
import static dk.skrypalle.bpl.util.Parse.*;

public class FuncResolvePass extends BPLBaseVisitor<FuncTbl> {

	private static final FuncTbl EMPTY = new FuncTbl();

	private final FuncTbl funcTbl;

	private Func curF;
	private Type curT;

	public FuncResolvePass() {
		funcTbl = new FuncTbl();
	}

	@Override
	public FuncTbl visitCompilationUnit(CompilationUnitContext ctx) {
		visitChildren(ctx);

		if (!funcTbl.isDecl("main"))
			throw new IllegalStateException("no main function found"); // TODO
		if (!funcTbl.hasOverloads("main"))
			throw new IllegalStateException("main function cannot be overloaded"); // TODO

		return funcTbl;
	}

	@Override
	public FuncTbl visitFuncDecl(FuncDeclContext ctx) {
		String id = ttos(ctx.id);

		curF = new Func();
		curF.id = id;
		visit(ctx.typ);
		curF.type = curT;
		visit(ctx.params);

		if (funcTbl.isDecl(id, curF.symTbl.getParamTypes()))
			throw new BPLCErrFuncRedeclared(ctx.id);

		funcTbl.decl(curF);

		return funcTbl;
	}

	@Override
	public FuncTbl visitParam(ParamContext ctx) {
		String id = ttos(ctx.id);
		visit(ctx.typ);
		if (curF.symTbl.isDecl(id))
			throw new BPLCErrSymRedeclared(ctx.id);

		curF.symTbl.declParam(id, curT);
		return funcTbl;
	}

	@Override
	public FuncTbl visitIdType(IdTypeContext ctx) {
		String type_str = ttos(ctx.id);
		curT = Types.lookup(type_str);
		if (curT == null)
			throw new BPLCErrTypeUndeclared(ctx.id);

		return funcTbl;
	}

	//region aggregate, default, visit

	@Override
	protected FuncTbl aggregateResult(FuncTbl agg, FuncTbl nxt) {
		if (agg == null) return nxt;
		if (nxt == null) return agg;

		FuncTbl res = new FuncTbl(agg);
		res.merge(nxt);
		return res;
	}

	@Override
	protected FuncTbl defaultResult() {
		return EMPTY;
	}

	@Override
	public FuncTbl visit(ParseTree t) {
		return t == null ? defaultResult() : t.accept(this);
	}

	//endregion

}
