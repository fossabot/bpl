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

package dk.skrypalle.bpl;

import dk.skrypalle.bpl.compiler.*;
import dk.skrypalle.bpl.util.*;
import dk.skrypalle.bpl.vm.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.*;
import java.nio.file.*;

public final class Main {

	public static void main(String[] args) throws IOException {
		String bpl = String.join("\n",
			"func add(int a, int b) int {",
			"return a-b;",
			"}",
			"func main() int {",
			"print(add(3,42));",
			"return 0;",
			"}"
		);

		byte[] bplbc = compileBC(bpl);
		System.out.println(Hex.dump(bplbc));
		VM vm = new VM(bplbc, 0, true);
		int vmExit = vm.run();
		System.out.printf("VM finished with exit code 0x%08x\n", vmExit);
		System.out.println();

		Path tmpDir = IO.makeTmpDir("_bplc_");
		try {
			String c99 = compileC99(bpl);
			Path c99out = tmpDir.resolve("out.c");
			IO.writeAll(c99out, c99);
			ExecRes gcc = Exec.gcc(c99out);
			if (!gcc.isEmpty())
				throw new Error(gcc.toString());
			ExecRes run = Exec.exec(tmpDir.resolve("out." + OS.exeEXT()));
			System.out.println(c99);
			System.out.println(run);
		} finally {
			IO.delRec(tmpDir);
		}
	}

	public static byte[] compileBC(String bpl) {
		ParseTree t = parse(bpl);
		return new BCVisitor().visit(t);
	}

	public static String compileC99(String bpl) {
		ParseTree t = parse(bpl);
		return new C99Visitor().visit(t);
	}

	private static ParseTree parse(String bpl) {
		ANTLRInputStream ais = new ANTLRInputStream(bpl);
		BPLLexer lex = new BPLLexer(ais);
		BPLParser prs = new BPLParser(new CommonTokenStream(lex));
		return prs.compilationUnit();
	}

	private Main() { /**/ }

}
