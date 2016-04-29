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

public final class Array {

	public static byte[] concat(Object... bytes) {
		int len = 0;
		for (Object o : bytes) {
			if (o instanceof byte[])
				len += ((byte[]) o).length;
			else if (o instanceof Byte)
				len++;
			else
				throw new IllegalArgumentException("Type of var_args must be byte or byte[]");
		}

		int idx = 0;
		byte[] res = new byte[len];
		for (Object o : bytes) {
			if (o instanceof byte[]) {
				for (byte b : ((byte[]) o))
					res[idx++] = b;
			} else {
				res[idx++] = (byte) o;
			}
		}

		return res;
	}

	public static byte[] prepend(byte b, byte[] a) {
		byte[] res = new byte[a.length + 1];
		res[0] = b;
		System.arraycopy(a, 0, res, 1, a.length);
		return res;
	}

	public static int[] toIntArray(Integer[] l) {
		int[] res = new int[l.length];
		for (int i = 0; i < l.length; i++)
			res[i] = l[i];
		return res;
	}

	private Array() { /**/ }

}
