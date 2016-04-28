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

package dk.skrypalle.bpl.compiler;

import dk.skrypalle.bpl.*;
import dk.skrypalle.bpl.util.*;
import dk.skrypalle.bpl.vm.*;
import org.testng.annotations.*;

import java.io.*;
import java.nio.file.*;

public class CompilerTestBase {

	private Path tmpDir;

	@BeforeTest
	public void setup() throws IOException {
		tmpDir = IO.makeTmpDir("_bplc_");
	}

	@AfterTest
	public void teardown() throws IOException {
		IO.delRec(tmpDir);
	}

	protected String wrapMain(String stmts) {
		return String.join("\n",
			"func main() int {",
			stmts,
			"return 0;",
			"}"
		);
	}

	protected byte[] compileBC(String bpl) {
		return Main.compileBC(bpl);
	}

	protected VMExecRes runBC(byte[] bc) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		ByteArrayOutputStream dbg = new ByteArrayOutputStream();
		VM vm = new VM(bc, 0, false,
			new PrintStream(out), new PrintStream(err), new PrintStream(dbg));
		int exit = vm.run();

		return new VMExecRes(exit, out.toString(), err.toString(), dbg.toString());
	}

	protected ExecRes compileC99(String bpl) {
		try {
			String c99 = Main.compileC99(bpl);
			Path c99out = tmpDir.resolve("out.c");
			IO.writeAll(c99out, c99);
			return Exec.gcc(c99out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected ExecRes runC99() throws IOException {
		return Exec.exec(tmpDir.resolve("out." + OS.exeEXT()));
	}

	//region inner class VMExecRes

	static class VMExecRes {
		final int    exit;
		final String out;
		final String err;
		final String dbg;

		public VMExecRes(int exit, String out, String err, String dbg) {
			this.exit = exit;
			this.out = out;
			this.err = err;
			this.dbg = dbg;
		}
	}

	//endregion

}
