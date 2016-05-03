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

	// Bytecode preamble length
	// data_seg_len(4) + data_seg(?) + CALL(8), HALT(1) +1
	public static final int PREABLE_LEN = 0x0e;

	// Parameter offset from fp after call
	public static final int PARAM_START = -4;

	public static final byte NOP   = (byte) 0x00;
	public static final byte POP   = (byte) 0x01;
	public static final byte IPUSH = (byte) 0x02;
	public static final byte IADD  = (byte) 0x03;
	public static final byte ISUB  = (byte) 0x04;
	public static final byte IMUL  = (byte) 0x05;
	public static final byte IDIV  = (byte) 0x06;
	public static final byte ILT   = (byte) 0x07;
	public static final byte IGT   = (byte) 0x08;
	public static final byte ILTE  = (byte) 0x09;
	public static final byte IGTE  = (byte) 0x10;
	public static final byte IEQ   = (byte) 0x11;
	public static final byte INEQ  = (byte) 0x12;

	public static final byte ILOAD  = (byte) 0x30;
	public static final byte ISTORE = (byte) 0x31;
	public static final byte SPUSH  = (byte) 0x32;
	public static final byte SLOAD  = (byte) 0x33;
	public static final byte CALL   = (byte) 0x34;
	public static final byte RET    = (byte) 0x35;
	public static final byte LOCALS = (byte) 0x36;

	public static final byte JMP  = (byte) 0x40;
	public static final byte BREQ = (byte) 0x41;
	public static final byte BRNE = (byte) 0x42;

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
		opCodes.put(NOP,    new Op("nop",    0));
		opCodes.put(POP,    new Op("pop",    0));
		opCodes.put(IPUSH,  new Op("ipush",  8));
		opCodes.put(IADD,   new Op("iadd",   0));
		opCodes.put(ISUB,   new Op("isub",   0));
		opCodes.put(IMUL,   new Op("imul",   0));
		opCodes.put(IDIV,   new Op("idiv",   0));
		opCodes.put(ILT,    new Op("igt",    0));
		opCodes.put(IGT,    new Op("ilt",    0));
		opCodes.put(ILTE,   new Op("igte",   0));
		opCodes.put(IGTE,   new Op("ilte",   0));
		opCodes.put(IEQ,    new Op("ieq",    0));
		opCodes.put(INEQ,   new Op("ineq",   0));

		opCodes.put(ILOAD,  new Op("iload",  4));
		opCodes.put(ISTORE, new Op("istore", 4));
		opCodes.put(SPUSH,  new Op("spush",  8));
		opCodes.put(SLOAD,  new Op("sload",  4));
		opCodes.put(CALL,   new Op("call",   8));
		opCodes.put(RET,    new Op("ret",    0));
		opCodes.put(LOCALS, new Op("locals", 4));

		opCodes.put(JMP,    new Op("jmp",    4));
		opCodes.put(BREQ,   new Op("breq",   4));
		opCodes.put(BRNE,   new Op("brne",   4));

		opCodes.put(PRINT,  new Op("print",  4));
		opCodes.put(HALT,   new Op("halt",   0));
		//fmt:on
	}

	private Bytecode() { /**/ }

}
