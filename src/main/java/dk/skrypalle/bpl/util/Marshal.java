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

public final class Marshal {

	public static long s64BE(byte[] data, int pos) {
		return ((long) (data[pos] & 0xff)) << 56L
			| ((long) (data[pos + 1] & 0xff)) << 48L
			| ((long) (data[pos + 2] & 0xff)) << 40L
			| ((long) (data[pos + 3] & 0xff)) << 32L
			| ((long) (data[pos + 4] & 0xff)) << 24L
			| ((long) (data[pos + 5] & 0xff)) << 16L
			| ((long) (data[pos + 6] & 0xff)) << 8L
			| ((long) (data[pos + 7] & 0xff));
	}

	public static byte[] bytesS64BE(long l) {
		byte[] res = new byte[8];
		for (int i = 0; i < 8; i++)
			res[7 - i] = (byte) (l >> (i*8));
		return res;
	}

	private Marshal() { /**/ }

}
