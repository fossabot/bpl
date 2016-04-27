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

package dk.skrypalle.bpl.util;

import dk.skrypalle.bpl.compiler.type.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.math.*;

public final class Parse {

	public static String ttos(Token t) {
		if (t == null)
			throw new NullPointerException("'t' is null");
		return t.getText();
	}

	public static String ttos(ParseTree t) {
		if (t == null)
			throw new NullPointerException("'t' is null");
		return t.getText();
	}

	public static byte[] concat(byte[] a, byte[] b) {
		byte[] res = new byte[a.length + b.length];
		System.arraycopy(a, 0, res, 0, a.length);
		System.arraycopy(b, 0, res, a.length, b.length);
		return res;
	}

	public static byte[] append(byte[] a, int... bytes) {
		byte[] res = new byte[a.length + bytes.length];
		System.arraycopy(a, 0, res, 0, a.length);
		for (int i = 0; i < bytes.length; i++)
			res[a.length + i] = (byte) bytes[i];
		return res;
	}

	public static byte[] prepend(byte b, byte[] a) {
		byte[] res = new byte[a.length + 1];
		res[0] = b;
		System.arraycopy(a, 0, res, 1, a.length);
		return res;
	}

	public static byte[] toBA(String val, int radix, IntType t) {
		return toBA(new BigInteger(val, radix), t);
	}

	public static byte[] toBA(BigInteger val, IntType t) {
		int len = t.width/8;
		byte[] res = new byte[len];
		byte[] bi = val.toByteArray();

		int i = len;
		int j = bi.length;

		while (i > 0 && j > 0)
			res[--i] = bi[--j];
		return res;
	}

	private Parse() { /**/ }

}
