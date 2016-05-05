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

package dk.skrypalle.bpl.compiler.type;

import dk.skrypalle.bpl.vm.*;

import java.util.*;

public class SymTbl {

	private final Deque<Map<String, Symbol>> stack;
	private final Map<String, Symbol>        params;
	private final ArrayDeque<Symbol>         paramStack;

	private int localIdx;

	SymTbl() {
		stack = new ArrayDeque<>();
		params = new HashMap<>();
		paramStack = new ArrayDeque<>();
		localIdx = 0;
	}

	public void pushScope() {
		stack.push(new HashMap<>());
	}

	public void popScope() {
		stack.pop();
	}

	public void declParam(String id, Type t) {
		if (params.containsKey(id))
			throw new IllegalArgumentException("param sym " + id + "[" + t + "]" + " already declared in this scope");

		// Shift down all previous values (stack)
		for (Symbol sym : params.values()) {
			if (sym.off < 0)
				sym.off--;
		}
		Symbol sym = new Symbol(id, t, Bytecode.PARAM_START);
		params.put(id, sym);
		paramStack.push(sym);
	}

	public Symbol declLocal(String id, Type t) {
		if (cur().containsKey(id))
			throw new IllegalArgumentException("local sym " + id + "[" + t + "]" + " already declared in this scope");

		Symbol sym = new Symbol(id, t, localIdx++);
		cur().put(id, sym);
		return sym;
	}

	public boolean isDecl(String id) {
		for (Map<String, Symbol> m : stack)
			if (m.containsKey(id))
				return true;

		return params.containsKey(id);
	}

	public int nLocals() {
		return localIdx;
	}

	public List<Type> getParamTypes() {
		List<Type> res = new ArrayList<>();
		for (Symbol sym : paramStack)
			res.add(sym.type);
		Collections.reverse(res);
		return res;
	}

	public Symbol get(String id) {
		for (Map<String, Symbol> m : stack) {
			if (m.containsKey(id))
				return m.get(id);
		}
		return params.get(id);
	}

	public void clearLocals() {
		stack.clear();
		localIdx = 0;
	}

	private Map<String, Symbol> cur() {
		return stack.peek();
	}

	//region Object class overrides

	@Override
	public String toString() {
		return "SymTbl{" +
			"params=" + params +
			", stack=" + stack +
			", paramStack=" + paramStack +
			", localIdx=" + localIdx +
			'}';
	}

	//endregion

}
