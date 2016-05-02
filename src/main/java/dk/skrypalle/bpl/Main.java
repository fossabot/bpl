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
import dk.skrypalle.bpl.compiler.type.*;
import dk.skrypalle.bpl.util.*;
import dk.skrypalle.bpl.vm.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.commons.lang3.*;

import java.io.*;
import java.nio.file.*;

public final class Main {

	private static long fib(int n) {
		if (n <= 1)
			return n;
		else
			return fib(n - 1) + fib(n - 2);
	}

	private static boolean x(int i) {
		System.out.print(i);
		return i != 0;
	}

	private static int main() {
		int i = 1;
		System.out.println(i);
		{
			int j = 2;
			System.out.println(i);
			System.out.println(j);
			{
				int k = 3;
				System.out.println(i);
				System.out.println(j);
				System.out.println(k);
			}
			int k = 4;
			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
		}
		return 0;
	}

	public static void main(String[] args) throws IOException {
		Exec.trace = true;
//		main();
//		System.exit(1);
		String bpl = String.join("\n",
			"func a() int {",
			"   var i int;",
			"   i = 5;",
			"   while(1) {",
			"       var j int;",
			"       j=i*2;",
			"       i=i-1;",
			"       print(\"i=\");",
			"       print(i);",
			"       print(\"\\nj=\");",
			"       print(j);",
			"       print(\"\\n\");",
			"       if(j){}else{return 1;}",
			"   }",
			"   return 0;",
			"}"
		);
		bpl += String.join("\n",
			"func b() int {",
			"   var i int;",
			"   i=1;",
			"   print(i);",
			"   {",
			"       var j int;",
			"       j=2;",
			"       print(i);",
			"       print(j);",
			"       {",
			"           var k int;",
			"           k=3;",
			"           print(i);",
			"           print(j);",
			"           print(k);",
			"       }",
			"       var k int;",
			"       k=4;",
			"       print(i);",
			"       print(j);",
			"       print(k);",
			"   }",
			"   return 0;",
			"}"
		);
		bpl += String.join("\n",
			"func add(a int, b int, c int) int {",
			"   return a+b;",
			"}",
			"func main() int {",
			"   var i int;",
			"   print(\"a():\\n\");",
			"   i=a();",
			"   print(\"\\nexit:\");",
			"   print(i);",
			"   print(\"\\n\\nb():\\n\");",
			"   i=b();",
			"   print(\"\\nexit:\");",
			"   print(i);",
			"   print(\"\\n\\nadd(3,4,6):\\n\");",
			"   i=add(3,4,6);",
			"   print(\"\\nexit:\");",
			"   print(i);",
			"   return 0;",
			"}"
		);
		bpl = loadTestFile("loop/fibonacci")[1];
		bpl = String.join("\n",
			"func main() int { print(\"Hello, 世界\"); return 0; }");

		byte[] bplbc = null;
		try {
			bplbc = compileBC(bpl);
			VM vm = new VM(bplbc, Exec.trace);
			int vmExit = vm.run();
			if (!Exec.trace)
				System.out.println("\n");
			System.out.printf("BPLVM finished with exit code %d\n", vmExit);
			System.out.println();
		} catch (Throwable t) {
			System.err.printf("Target BPLBC failed: %s: %s\n", t.getClass().getSimpleName(), t.getMessage());
			if (bplbc != null)
				System.err.printf("Code memory:\n%s\n", Hex.dump(bplbc));
			t.printStackTrace();
		}

//		System.exit(1);

		Path tmpDir = null;
		String c99 = null;
		try {
			tmpDir = IO.makeTmpDir("_bplc_");
			c99 = compileC99(bpl);
			Path c99out = tmpDir.resolve("out.c");
			IO.writeAll(c99out, c99);
			ExecRes gcc = Exec.gcc(c99out);
			if (!gcc.isEmpty())
				throw new Error(String.format(
					"GCC compile error (exit status %d)\n%s%s",
					gcc.exit, gcc.out, gcc.err
				));
			ExecRes run = Exec.exec(tmpDir.resolve("out" + OS.exeEXT()));
			if (Exec.trace)
				System.out.println(c99);
			System.out.print(run.out);
			System.out.println("\n");
			System.out.println("Native finished with exit code " + run.exit);
			IO.delRec(tmpDir);
		} catch (Throwable t) {
			System.err.printf("Target C99 failed: %s: %s\n", t.getClass().getSimpleName(), t.getMessage());
			if (c99 != null)
				System.err.printf("Code memory:\n%s\n", c99);
			t.printStackTrace();

			if (tmpDir != null)
				IO.delRec(tmpDir);
		}
	}

	private static String[] loadTestFile(String name) throws IOException {
		Path p = Paths.get("./src/test/resources/compiler/" + name + ".test");

		String[] res = new String(Files.readAllBytes(p), IO.UTF8).split("::exp\n");
		String act = res[0];
		String exp = res[1].trim();
		exp = StringEscapeUtils.unescapeJava(exp);
		exp = exp.replaceAll("\n", System.lineSeparator());

		return new String[]{name, act, exp};
	}

	public static byte[] compileBC(String bpl) {
		ParseTree t = parse(bpl);
		FuncTbl funcTbl = new FuncResolvePass().visit(t);
		return new BCVisitor(funcTbl).visit(t);
	}

	public static String compileC99(String bpl) {
		ParseTree t = parse(bpl);
		FuncTbl funcTbl = new FuncResolvePass().visit(t);
		return new C99Visitor(funcTbl).visit(t);
	}

	private static ParseTree parse(String bpl) {
		ANTLRInputStream ais = new ANTLRInputStream(bpl);
		BPLLexer lex = new BPLLexer(ais);
		BPLParser prs = new BPLParser(new CommonTokenStream(lex));
		return prs.compilationUnit();
	}

	private Main() { /**/ }

}
