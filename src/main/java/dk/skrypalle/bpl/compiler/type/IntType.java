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

import java.math.*;

public enum IntType {
	U8(8, false), U16(16, false), U32(32, false), U64(64, false),
	S8(8, true), S16(16, true), S32(32, true), S64(64, true);

	public final int     width;
	public final boolean isSigned;

	IntType(int width, boolean isSigned) {
		this.width = width;
		this.isSigned = isSigned;
	}

	public IntType next() {
		for (IntType t : values()) {
			if (t.isSigned != isSigned)
				continue;
			if (t.width == width*2)
				return t;
		}
		throw new IllegalStateException("cannot widen " + this); // TODO
	}

	public static IntType parse(BigInteger val) {
		int size = val.bitLength();
		if (size <= 8) {
			return val.signum() == -1 ? S8 : U8;
		} else if (size <= 16) {
			return val.signum() == -1 ? S16 : U16;
		} else if (size <= 32) {
			return val.signum() == -1 ? S32 : U32;
		} else if (size <= 64) {
			return val.signum() == -1 ? S64 : U64;
		} else {
			throw new IllegalStateException("unreachable"); // TODO
		}
	}

	public static IntType parse(String val) {
		return parse(new BigInteger(val));
	}
}
