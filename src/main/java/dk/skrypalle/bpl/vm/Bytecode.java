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

package dk.skrypalle.bpl.vm;

import java.util.*;

public final class Bytecode {

	public static final byte NOP   = (byte) 0x00;
	public static final byte IPUSH = (byte) 0x01;
	public static final byte IADD  = (byte) 0x02;
	public static final byte ISUB  = (byte) 0x03;
	public static final byte IMUL  = (byte) 0x04;
	public static final byte IDIV  = (byte) 0x05;

	public static final byte PRINT = (byte) 0xfe;
	public static final byte HALT  = (byte) 0xff;

	static class Op {
		final String name;
		final int    nArgs;

		Op(String name, int nArgs) {
			this.name = name;
			this.nArgs = nArgs;
		}
	}

	static final Map<Byte, Op> opCodes;

	static {
		//fmt:off
		opCodes = new HashMap<>();
		opCodes.put(NOP,   new Op("nop",   0));
		opCodes.put(IPUSH, new Op("ipush", 8));
		opCodes.put(IADD,  new Op("iadd",  0));
		opCodes.put(ISUB,  new Op("isub",  0));
		opCodes.put(IMUL,  new Op("imul",  0));
		opCodes.put(IDIV,  new Op("idiv",  0));
		opCodes.put(PRINT, new Op("print", 0));
		opCodes.put(HALT,  new Op("halt",  0));
		//fmt:on
	}

	private Bytecode() { /**/ }

}
