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

import dk.skrypalle.bpl.util.*;

import java.io.*;

public class VM {

	private final CPU           cpu;
	private final StringBuilder outBuf;
	private final StringBuilder traceBuf;

	private final PrintStream out;
	private final PrintStream err;
	private final PrintStream dbg;

	boolean trace;

	public VM(byte[] code, int start, boolean trace) {
		this(code, start, trace, System.out, System.err, System.out);
	}

	public VM(byte[] code, int start, boolean trace,
			  PrintStream out, PrintStream err, PrintStream dbg) {
		this.out = out;
		this.err = err;
		this.dbg = dbg;
		this.cpu = new CPU(this, code, start);
		this.outBuf = new StringBuilder();
		this.traceBuf = new StringBuilder();
		this.trace = trace;
	}

	public void run() {
		while (cpu.hasInstructions()) {
			cpu.step();
			flush();
		}

		if (trace) {
			dbg.printf("\nCode memory: %d bytes\n", cpu.code.length);
			dbg.println(Hex.dump(cpu.code));
		}
	}

	void trace(String s) {
		if (trace)
			traceBuf.append(s);
	}

	void out(String s) {
		outBuf.append(s);
	}

	private void flush() {
		if (trace) {
			dbg.println(traceBuf.toString().trim());
			traceBuf.delete(0, traceBuf.capacity());
		}

		if (outBuf.length() > 0) {
			out.print(outBuf.toString().trim());
			if (trace)
				out.println();
			outBuf.delete(0, outBuf.capacity());
		}
	}

}
