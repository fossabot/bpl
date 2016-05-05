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

package dk.skrypalle.bpl.compiler.err;

import dk.skrypalle.bpl.compiler.type.*;
import org.antlr.v4.runtime.*;

import java.util.*;

public class BPLCErrWrongNumArgs extends BPLCErr {

	private static final long serialVersionUID = -2363166926432452563L;

	private final List<Type>       act;
	private final List<List<Type>> exp;

	public BPLCErrWrongNumArgs(TokenAdapter t, List<Type> act, List<List<Type>> exp) {
		super(t);
		this.act = act;
		this.exp = exp;
	}

	public BPLCErrWrongNumArgs(Token t, List<Type> act, List<List<Type>> exp) {
		this(TokenAdapter.from(t), act, exp);
	}

	@Override
	String msg() {
		int nSmaller = 0;
		int nGreater = 0;
		for (int i = 0; i < exp.size(); i++) {
			if (exp.get(i).size() > act.size())
				nSmaller++;
			else
				nGreater++;
		}

		String s;
		if (nSmaller == exp.size()) {
			s = "too few arguments";
		} else if (nGreater == exp.size()) {
			s = "too many arguments";
		} else {
			s = "wrong number of arguments";
		}

		return s + " to function '%s' - have " + act + " want " + exp;
	}

}
