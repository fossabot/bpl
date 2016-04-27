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

package dk.skrypalle.bpl.util;

import dk.skrypalle.bpl.*;
import dk.skrypalle.bpl.vm.*;
import org.testng.*;
import org.testng.annotations.*;

import java.io.*;
import java.nio.file.*;

public class CompilerTest {

	private Path tmpDir;

	@BeforeTest
	public void setup() throws IOException {
		tmpDir = IO.makeTmpDir("_bplc_");
	}

	@AfterTest
	public void teardown() throws IOException {
		IO.delRec(tmpDir);
	}

	@DataProvider
	public Object[][] provideData() {
		return new Object[][]{
			{"1", "01"},
			{"255", "ff"},
			{"256", "0001"},
			{"1234567890", "d2029649"},
		};
	}

	@Test(dataProvider = "provideData")
	public void testTargetBC(String bpl, String exp) {
		byte[] bc = Main.compileBC(bpl);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		ByteArrayOutputStream dbg = new ByteArrayOutputStream();
		VM vm = new VM(bc, 0, false,
			new PrintStream(out), new PrintStream(err), new PrintStream(dbg));
		vm.run();

		Assert.assertEquals(out.toString(), exp);
		Assert.assertEquals(err.toString(), "");
		Assert.assertEquals(dbg.toString(), "");
	}

	@Test(dataProvider = "provideData")
	public void testTargetC89(String bpl, String exp) throws IOException {
		String c89 = Main.compileC89(bpl);
		Path c89out = tmpDir.resolve("out.c");
		IO.writeAll(c89out, c89);
		ExecRes gcc = Exec.gcc(c89out);
		Assert.assertEquals(gcc.exit, 0, "gcc exit status");
		Assert.assertEquals(gcc.out, "", "gcc out stream");
		Assert.assertEquals(gcc.err, "", "gcc err stream");

		ExecRes run = Exec.exec(tmpDir.resolve("out." + OS.exeEXT()));
		Assert.assertEquals(run.exit, 0, "run exit status");
		Assert.assertEquals(run.out, exp, "run out stream");
		Assert.assertEquals(run.err, "", "run err stream");
	}

}
