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
	private int  fp;
	private byte op;

	byte[] code;
	private long[] stack;

	CPU(VM vm, byte[] code, int ip) {
		this.vm = vm;
		this.ip = ip;
		this.sp = -1;
		this.fp = 0;
		this.code = code;
		this.stack = new long[1];
	}

	void step() {
		op = fetch();

		if (vm.trace)
			disassemble();

		int addr, off, nArgs;
		long lhs, rhs, res;
		long rval;
		switch (op) {
		case NOP:
			break;
		case POP:
			pop();
			break;
		case IPUSH:
			push(fetchS64());
			break;
		case IADD:
			rhs = pop();
			lhs = pop();
			res = lhs + rhs;
			push(res);
			break;
		case ISUB:
			rhs = pop();
			lhs = pop();
			res = lhs - rhs;
			push(res);
			break;
		case IMUL:
			rhs = pop();
			lhs = pop();
			res = lhs*rhs;
			push(res);
			break;
		case IDIV:
			rhs = pop();
			lhs = pop();
			res = lhs/rhs;
			push(res);
			break;
		case ISTORE:
			off = fetchS32();
			sput(fp + off + 1, pop());
			break;
		case ILOAD:
			off = fetchS32();
			push(sget(fp + off + 1));
			break;
		case CALL:
			addr = fetchS32();
			nArgs = fetchS32();
			push(nArgs);
			push(fp);
			push(ip);
			fp = sp;
			ip = addr;
			break;
		case RET:
			rval = pop();
			sp = fp;
			ip = (int) pop();
			fp = (int) pop();
			nArgs = (int) pop();
			sp -= nArgs;
			push(rval);
			break;
		case LOCALS:
//			sp += fetchS32();
//			while (sp >= stack.length)
//				growStack();
			int l = fetchS32();
			for (int i = 0; i < l; i++) {
				push((long) (Math.random()*23452345.0));
			}
			break;
		case PRINT:
			vm.out(String.format("%x", pop()));
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

	int exitCode() {
		return (int) stack[sp];
	}

	//region mem code

	private byte fetch() {
		return code[ip++];
	}

	private int fetchS32() {
		int val = Marshal.s32BE(code, ip);
		ip += 4;
		return val;
	}

	private long fetchS64() {
		long val = Marshal.s64BE(code, ip);
		ip += 8;
		return val;
	}

	//endregion

	//region mem stack

	private void push(long v) {
		sp++;
		if (sp == stack.length)
			growStack();
		stack[sp] = v;
	}

	private long pop() {
		if (sp < 0)
			throw new BPLVMStackUnderflowError();
		return stack[sp--];
	}

	private long sget(int addr) {
		if (addr < 0)
			throw new BPLVMStackUnderflowError();
		return stack[addr];
	}

	private void sput(int addr, long val) {
		if (addr < 0)
			throw new BPLVMStackUnderflowError();
		stack[addr] = val;
	}

	private void growStack() {
		int newLen = stack.length*2;
		if (newLen > MAX_STACK_SIZE)
			throw new BPLVMStackOverflowError(newLen, MAX_STACK_SIZE);

		long[] newStack = new long[newLen];
		System.arraycopy(stack, 0, newStack, 0, stack.length);
		stack = newStack;
	}

	//endregion

	//region trace

	private void disassemble() {
		Op inst = Bytecode.opCodes.get(op);
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
			stackBuf.append(String.format("0x%x", stack[i]));
			if (i < sp)
				stackBuf.append(", ");
		}
		stackBuf.append(']');

		vm.trace(String.format("stack=%s", stackBuf));
	}

	//endregion

}
