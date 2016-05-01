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
import dk.skrypalle.bpl.util.*;
import org.antlr.v4.runtime.*;

import java.util.*;

public class BPLCErrTypeMismatch extends BPLCErr {

	private static final long serialVersionUID = 474385486925413626L;

	private final List<DataType> have;
	private final List<DataType> want;

	public BPLCErrTypeMismatch(TokenAdapter t, DataType have, DataType want) {
		this(t, Collections.singletonList(have), Collections.singletonList(want));
	}

	public BPLCErrTypeMismatch(Token t, DataType have, DataType want) {
		this(t, Collections.singletonList(have), Collections.singletonList(want));
	}

	public BPLCErrTypeMismatch(TokenAdapter t, List<DataType> have, List<DataType> want) {
		super(t);
		this.have = have;
		this.want = want;
	}

	public BPLCErrTypeMismatch(Token t, List<DataType> have, List<DataType> want) {
		super(t);
		this.have = have;
		this.want = want;
	}

	@Override
	String msg() {
		String h, w;
		h = have.size() == 1 ? have.get(0).toString() : "[" + Parse.join(", ", have) + "]";
		w = want.size() == 1 ? want.get(0).toString() : "[" + Parse.join(", ", want) + "]";

		return String.format("type mismatch at '%s' - have %s want %s", "%s", h, w);
	}

}
