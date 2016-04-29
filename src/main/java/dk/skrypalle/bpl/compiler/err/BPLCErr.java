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

import static java.lang.String.*;

public abstract class BPLCErr extends Error implements TokenAdapter {

	private static final long serialVersionUID = -3854799174188582313L;

	private final TokenAdapter t;

	BPLCErr(TokenAdapter t) { this.t = t; }

	BPLCErr(Token t) { this(TokenAdapter.from(t)); }

	abstract String msg();

	@Override
	public final String getMessage() {
		return format(
			"%d:%d: error: %s",
			t.row(), t.col(), format(msg(), t.text())
		);
	}

	@Override
	public final String getLocalizedMessage() { return getMessage(); }

	//region TokenAdapter delegate

	@Override
	public int row() { return t.row(); }

	@Override
	public int col() { return t.col(); }

	@Override
	public String text() { return t.text(); }

	//endregion

}
