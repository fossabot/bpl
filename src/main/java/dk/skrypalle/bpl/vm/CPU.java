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
import dk.skrypalle.bpl.vm.err.*;

import static dk.skrypalle.bpl.vm.Bytecode.*;

class CPU {

	static int MAX_STACK_SIZE = 0xffff;

	private final VM vm;

	private int  ip;
	private int  sp;
	private byte op;

	byte[] code;
	byte[] stack;

	CPU(VM vm, byte[] code, int ip) {
		this.vm = vm;
		this.ip = ip;
		this.sp = -1;
		this.code = code;
		this.stack = new byte[1];
	}

	void step() {
		op = fetch();

		if (vm.trace)
			disassemble();

		int len;
		switch (op) {
		case NOP:
			break;
		case PUSH8:
			cpyCS(1);
			break;
		case PUSH16:
			cpyCS(2);
			break;
		case PUSH32:
			cpyCS(4);
			break;
		case PUSH64:
			cpyCS(8);
			break;
		case PRINT:
			len = Marshal.s32LE(code, ip);
			ip += 4;
			sp -= len;
			if (sp < -1)
				throw new BPLVMStackUnderflowError();
			vm.out(Hex.num(stack, sp + 1, len));
			break;
		case DUMP:
			len = Marshal.s32LE(code, ip);
			ip += 4;
			sp -= len;
			if (sp < -1)
				throw new BPLVMStackUnderflowError();
			vm.out(Hex.dump(stack, sp + 1, len));
			break;
		case HALT:
			break;
		default:
			throw new BPLVMIllegalStateError(op);
		}

		if (vm.trace)
			traceStack();
	}

	boolean hasInstructions() {
		return op != HALT && ip < code.length;
	}

	//region mem code

	private byte fetch() {
		return code[ip++];
	}

	//endregion

	//region mem stack

	private void push(byte v) {
		sp++;
		if (sp == stack.length)
			growStack();
		stack[sp] = v;
	}

	private byte pop() {
		if (sp < 0)
			throw new BPLVMStackUnderflowError();
		return stack[sp--];
	}

	private byte[] pop(int len) {
		byte[] res = new byte[len];
		System.arraycopy(stack, sp - len + 1, res, 0, len);
		sp -= len;
		return res;
	}

	private void cpyCS(int n) {
		while (sp + n + 1 > stack.length)
			growStack();

		System.arraycopy(code, ip, stack, sp + 1, n);
		ip += n;
		sp += n;
	}

	private void growStack() {
		int newLen = stack.length*2;
		if (newLen > MAX_STACK_SIZE)
			throw new BPLVMStackOverflowError(newLen, MAX_STACK_SIZE);

		byte[] newStack = new byte[newLen];
		System.arraycopy(stack, 0, newStack, 0, stack.length);
		stack = newStack;
	}

	//endregion

	//region trace

	private void disassemble() {
		Op inst = Bytecode.opcodes.get(op);
		if (inst == null)
			throw new IllegalStateException(String.format("Illegal op code 0x%02x", op));

		StringBuilder argBuf = new StringBuilder();
		argBuf.append('[');
		for (int i = 0; i < inst.nArgs; i++) {
			argBuf.append(String.format("0x%02x", code[ip + i]));
			if (i < inst.nArgs - 1)
				argBuf.append(", ");
		}
		argBuf.append(']');

		String trace = String.format("%08x  (0x%02x) %-6s %s", ip - 1, op, inst.name, argBuf.toString());

		vm.trace(String.format("%-80s", trace));
	}

	private void traceStack() {
		StringBuilder stackBuf = new StringBuilder();
		stackBuf.append('[');
		for (int i = 0; i < sp + 1; i++) {
			stackBuf.append(String.format("0x%02x", stack[i]));
			if (i < sp)
				stackBuf.append(", ");
		}
		stackBuf.append(']');

		vm.trace(String.format("stack=%s", stackBuf));
	}

	//endregion

}
