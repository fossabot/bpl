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

public final class Hex {

	public static String dump(byte[] b) {
		return dump(b, 0, b.length);
	}

	public static String dump(byte[] b, int pos) {
		return dump(b, pos, b.length - pos);
	}

	public static String dump(byte[] b, int pos, int len) {
		if (pos + len > b.length)
			throw new ArrayIndexOutOfBoundsException();

		StringBuilder buf = new StringBuilder();
		for (int row = 0; row < len; row += 16) {
			buf.append(String.format("%08x  ", row));
			for (int col = 0; col < 16; col++) {
				if (row + col < len)
					buf.append(String.format("%02x ", b[pos + row + col]));
				else
					buf.append("   ");

				if (col == 7)
					buf.append(" ");
			}
			buf.append(" |");
			for (int col = 0; col < 16; col++) {
				if (row + col < len) {
					byte v = b[pos + row + col];
					if (v >= 32 && v < 127) // isPrint
						buf.append((char) v);
					else
						buf.append('.');
				}
			}
			buf.append("|");

			if (row < len - 16)
				buf.append("\n");
		}

		return buf.toString();
	}

	public static String num(byte[] b) {
		return num(b, 0, b.length);
	}

	public static String num(byte[] b, int pos) {
		return num(b, pos, b.length - pos);
	}

	public static String num(byte[] b, int pos, int len) {
		if (pos + len > b.length)
			throw new ArrayIndexOutOfBoundsException();

		StringBuilder buf = new StringBuilder();
		for (int i = pos; i < pos + len; i++)
			buf.append(String.format("%02x", b[i]));
		return buf.toString();
	}

	private Hex() { /**/ }

}
