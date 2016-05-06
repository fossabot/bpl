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

import dk.skrypalle.bpl.compiler.type.*;
import dk.skrypalle.bpl.util.*;
import dk.skrypalle.bpl.vm.err.*;

import java.util.*;

import static dk.skrypalle.bpl.vm.Bytecode.*;

class CPU {

	static int MAX_STACK_SIZE = 0xffff;

	private final VM vm;

	private int  ip;
	private int  sp;
	private int  fp;
	private byte op;

	byte[] code;
	int    ds_len;
	private StackEntry[] stack;

	CPU(VM vm, byte[] code) {
		this.ds_len = Marshal.s32BE(code, 0);
		this.vm = vm;
		this.ip = VM.HEADER + ds_len;
		this.sp = -1;
		this.fp = 0;
		this.code = code;
		this.stack = new StackEntry[0];
	}

	void step() {
		op = fetch();

		if (vm.trace)
			disassemble();

		int addr, off, nArgs;
		StackEntry lhs, rhs, cmp, arg;
		long res;
		long val;
		switch (op) {
		case NOP:
			break;
		case POP:
			pop();
			break;
		case IPUSH:
			val = fetchS64();
			push(val, Types.lookup("int"));
			break;
		case IADD:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("IADD:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val + rhs.val;
			push(res, Types.lookup("int"));
			break;
		case ISUB:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("ISUB:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val - rhs.val;
			push(res, Types.lookup("int"));
			break;
		case IMUL:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("IMUL:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val*rhs.val;
			push(res, Types.lookup("int"));
			break;
		case IDIV:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("IDIV:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val/rhs.val;
			push(res, Types.lookup("int"));
			break;
		case ILT:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("ILT:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val < rhs.val ? 1 : 0;
			push(res, Types.lookup("int"));
			break;
		case IGT:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("IGT:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val > rhs.val ? 1 : 0;
			push(res, Types.lookup("int"));
			break;
		case ILTE:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("ILTE:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val <= rhs.val ? 1 : 0;
			push(res, Types.lookup("int"));
			break;
		case IGTE:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("IGTE:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val >= rhs.val ? 1 : 0;
			push(res, Types.lookup("int"));
			break;
		case IEQ:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("IEQ:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val == rhs.val ? 1 : 0;
			push(res, Types.lookup("int"));
			break;
		case INEQ:
			rhs = pop();
			lhs = pop();
			if (lhs.type != rhs.type)
				throw new IllegalArgumentException(String.format("INEQ:: want [INT,INT], have [%s,%s]", lhs.type, rhs.type));
			res = lhs.val != rhs.val ? 1 : 0;
			push(res, Types.lookup("int"));
			break;
		case ISTORE:
			rhs = pop();
			lhs = pop();
			addr = fp + (int) lhs.val + 1;
			sput(addr, rhs.val, rhs.type);
			break;
		case ILOAD:
			lhs = pop();
			addr = fp + (int) lhs.val + 1;
			rhs = sget(addr);
			push(rhs.val, rhs.type);
			break;
		case ADDR_OF:
			lhs = pop();
			addr = fp + (int) lhs.val + 1;
			push(addr, Types.ref(lhs.type));
			break;
		case VAL_OF:
			lhs = pop();
			addr = (int) lhs.val;
			rhs = sget(addr);
			push(rhs.val, rhs.type);
			break;
		case RESOLVE:
			lhs = pop();
			push(lhs.val - fp - 1, Types.lookup("int"));
			break;
		case SPUSH:
			int t = fetchS32();
			addr = fetchS32();
			push(addr, Types.lookup(t));
			break;
		case SLOAD:
			lhs = pop();
			addr = fp + (int) lhs.val + 1;
			rhs = sget(addr);
			push(rhs.val, rhs.type);
			break;
		case CALL:
			addr = fetchS32();
			nArgs = fetchS32();
			push(nArgs, Types.lookup("int"));
			push(fp, Types.lookup("int"));
			push(ip, Types.lookup("int"));
			fp = sp;
			ip = addr;
			if (ip < 0 || ip >= code.length)
				throw new ArrayIndexOutOfBoundsException(String.format("call to invalid addr 0x%08x\n", ip));
			break;
		case RET:
			rhs = pop();
			sp = fp;
			StackEntry _ip = pop();
			StackEntry _fp = pop();
			StackEntry _nArgs = pop();
			if (_ip.type != Types.lookup("int"))
				throw new IllegalArgumentException(String.format("RET:: want ip[INT], have ip[%s]", _ip.type));
			if (_fp.type != Types.lookup("int"))
				throw new IllegalArgumentException(String.format("RET:: want fp[INT], have fp[%s]", _fp.type));
			if (_nArgs.type != Types.lookup("int"))
				throw new IllegalArgumentException(String.format("RET:: want nArgs[INT], have nArgs[%s]", _nArgs.type));
			ip = (int) _ip.val;
			fp = (int) _fp.val;
			nArgs = (int) _nArgs.val;
			sp -= nArgs;
			push(rhs.val, rhs.type);
			break;
		case LOCALS:
//			sp += fetchS32();
//			while (sp >= stack.length)
//				growStack();
			int l = fetchS32();
			// simulate garbage in local storage
			for (int i = 0; i < l; i++)
				push((long) (Math.random()*23452345.0), Types.lookup("int"));
			break;
		case JMP:
			off = fetchS32();
			ip += off;
			if (ip < 0 || ip >= code.length)
				throw new ArrayIndexOutOfBoundsException(String.format("jmp to invalid addr 0x%08x --> 0x%08x\n", ip - off, ip));
			break;
		case BRNE:
			off = fetchS32();
			cmp = pop();
			if (cmp.type != Types.lookup("int"))
				throw new IllegalArgumentException(String.format("BRNE:: want [INT], have [%s]", cmp.type));
			if (cmp.val != 0)
				ip += off;
			break;
		case BREQ:
			off = fetchS32();
			cmp = pop();
			if (cmp.type != Types.lookup("int"))
				throw new IllegalArgumentException(String.format("BREQ:: want [INT], have [%s]", cmp.type));
			if (cmp.val == 0)
				ip += off;
			break;
		case PRINT:
			off = fetchS32();
			Deque<StackEntry> args = new ArrayDeque<>();
			for (int i = 0; i < off; i++) {
				args.push(pop());
			}
			for (int i = 0; i < off; i++) {
				arg = args.pop();
				switch (arg.type.name) { // TODO
				case "int":
					vm.out(String.format("%x", arg.val));
					break;
				case "string":
					addr = (int) arg.val;
					int len = Marshal.s32BE(code, addr);
					addr += 4;
					String s = new String(code, addr, len, IO.UTF8);
					vm.out(s);
					break;
				default:
					vm.out(String.format("Don't know, how to print [%s] addr=0x%08x", arg.type, arg.val));
					break;
				}
			}
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
		return (int) stack[sp].val;
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

	private void push(long val, Type type) {
		sp++;
		if (sp == stack.length)
			growStack();
		stack[sp].val = val;
		stack[sp].type = type;
	}

	private StackEntry pop() {
		if (sp < 0)
			throw new BPLVMStackUnderflowError();
		return stack[sp--];
	}

	private StackEntry sget(int addr) {
		if (addr < 0)
			throw new BPLVMStackUnderflowError();
		return stack[addr];
	}

	private void sput(int addr, long val, Type type) {
		if (addr < 0)
			throw new BPLVMStackUnderflowError();
		stack[addr].val = val;
		stack[addr].type = type;
	}

	private void growStack() {
		int newLen = stack.length*2;
		if (newLen == 0)
			newLen = 1;
		if (newLen > MAX_STACK_SIZE)
			throw new BPLVMStackOverflowError(newLen, MAX_STACK_SIZE);

		StackEntry[] newStack = new StackEntry[newLen];
		for (int i = 0; i < newLen; i++)
			newStack[i] = new StackEntry();
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

		String trace = String.format("%08x  (0x%02x) %-8s %s", ip - 1, op, inst.name, argBuf.toString());

		vm.trace(String.format("%-80s", trace));
	}

	private void traceStack() {
		StringBuilder stackBuf = new StringBuilder();
		stackBuf.append('[');
		for (int i = 0; i < sp + 1; i++) {
			stackBuf.append(String.format("0x%x(%s)", stack[i].val, stack[i].type));
			if (i < sp)
				stackBuf.append(", ");
		}
		stackBuf.append(']');

		vm.trace(String.format("stack=%s", stackBuf));
	}

	//endregion

	private static class StackEntry {
		private long val;
		private Type type;

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof StackEntry)) return false;
			StackEntry other = (StackEntry) obj;

			return this.val == other.val
				&& this.type == other.type;
		}

		@Override
		public String toString() {
			return String.format("StackEntry{val=0x%016x, type=%s}", val, type);
		}
	}

}
