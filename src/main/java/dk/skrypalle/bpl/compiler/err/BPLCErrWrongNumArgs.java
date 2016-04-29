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

import org.antlr.v4.runtime.*;

public class BPLCErrWrongNumArgs extends BPLCErr {

	private static final long serialVersionUID = -2363166926432452563L;

	private final int act;
	private final int exp;

	public BPLCErrWrongNumArgs(TokenAdapter t, int act, int exp) {
		super(t);
		this.act = act;
		this.exp = exp;
	}

	public BPLCErrWrongNumArgs(Token t, int act, int exp) {
		this(TokenAdapter.from(t), act, exp);
	}

	@Override
	String msg() {
		String qnt = act > exp ? "many" : "few";
		return "too " + qnt + " arguments fo function '%s' - have " + act + " want " + exp;
	}

}
