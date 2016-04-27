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

	public static final byte NOP    = (byte) 0x00;
	public static final byte PUSH8  = (byte) 0x01;
	public static final byte PUSH16 = (byte) 0x02;
	public static final byte PUSH32 = (byte) 0x03;
	public static final byte PUSH64 = (byte) 0x04;
	public static final byte ADDU8  = (byte) 0x05;
	public static final byte ADDU16 = (byte) 0x06;
	public static final byte ADDU32 = (byte) 0x07;
	public static final byte ADDU64 = (byte) 0x08;
	public static final byte ADDS8  = (byte) 0x09;
	public static final byte ADDS16 = (byte) 0x0a;
	public static final byte ADDS32 = (byte) 0x0b;
	public static final byte ADDS64 = (byte) 0x0c;
	public static final byte WIDEN  = (byte) 0x0d;
	public static final byte PRINT  = (byte) 0xfc;
	public static final byte DUMP   = (byte) 0xfe;
	public static final byte HALT   = (byte) 0xff;

	static class Op {
		final String name;
		final int    nArgs;

		Op(String name, int nArgs) {
			this.name = name;
			this.nArgs = nArgs;
		}
	}

	static final Map<Byte, Op> opcodes;

	static {
		opcodes = new HashMap<>();
		opcodes.put(NOP, new Op("nop", 0));
		opcodes.put(PUSH8, new Op("push8", 1));
		opcodes.put(PUSH16, new Op("push16", 2));
		opcodes.put(PUSH32, new Op("push32", 4));
		opcodes.put(PUSH64, new Op("push64", 8));
		opcodes.put(ADDU8, new Op("addu8", 0));
		opcodes.put(ADDU16, new Op("addu16", 0));
		opcodes.put(ADDU32, new Op("addu32", 0));
		opcodes.put(ADDU64, new Op("addu64", 0));
		opcodes.put(ADDS8, new Op("adds8", 0));
		opcodes.put(ADDS16, new Op("adds16", 0));
		opcodes.put(ADDS32, new Op("adds32", 0));
		opcodes.put(ADDS64, new Op("adds64", 0));
		opcodes.put(WIDEN, new Op("widen", 2)); // widen 8,64 : widens TOS 8bit to 64bit
		opcodes.put(PRINT, new Op("print", 4));
		opcodes.put(DUMP, new Op("dump", 4));
		opcodes.put(HALT, new Op("halt", 0));
	}

	private Bytecode() { /**/ }

}
