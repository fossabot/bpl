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

import dk.skrypalle.bpl.util.*;
import org.testng.*;
import org.testng.annotations.*;

import java.io.*;

public class CompilerTest extends CompilerTestBase {

	//region data provider

	@DataProvider
	public Object[][] provideData() {
		return new Object[][]{
			{"print(0);", "0"},
			{"print(1);", "1"},
			{"print(255);", "ff"},
			{"print(256);", "100"},
			{"print(1234567890);", "499602d2"},
			{"print(1+42+5+6);", "36"},
			{"print(18446744073709551568+42+5);", "ffffffffffffffff"},
			{"print(255+1);print(65535);", "100ffff"},
			{"print(3-2);", "1"},
			{"print(2*3);", "6"},
			{"print(6/2);", "3"},
			{"print(7/2);", "3"},
			{"print(8-2+5);", "b"},
			{"print(8/2*4);", "10"},
			{"print(8/3*4);", "8"},
			{"print(2+3*3);", "b"},
			{"print(9-2*3);", "3"},

			{"var foo int; foo=42; print(foo);", "2a"},
			{"var foo int; foo=42; print(foo+2);", "2c"},
			{"var a int; var b int; a=2; b=5; print(a+b);", "7"},
			{"var a int; var b int; var c int; a=2; b=5; c=9; print(c*a+b/a);", "14"},
		};
	}

	//endregion

	//region targets

	@Test(dataProvider = "provideData")
	public void testTargetBC(String bpl, String exp) {
		byte[] bc = compileBC(bpl);
		VMExecRes res = runBC(bc);

		Assert.assertEquals(res.out, exp, "BPLVM out stream");
		Assert.assertEquals(res.err, "", "BPLVM err stream");
		Assert.assertEquals(res.dbg, "", "BPLVM dbg stream");
	}

	@Test(dataProvider = "provideData")
	public void testTargetC99(String bpl, String exp) throws IOException {
		ExecRes gcc = compileC99(bpl);
		if (!gcc.isEmpty())
			System.err.println(gcc);
		Assert.assertEquals(gcc.exit, 0, "gcc exit status");
		Assert.assertEquals(gcc.out, "", "gcc out stream");
		Assert.assertEquals(gcc.err, "", "gcc err stream");

		ExecRes run = runC99();
		Assert.assertEquals(run.exit, 0, "run exit status");
		Assert.assertEquals(run.out, exp, "run out stream");
		Assert.assertEquals(run.err, "", "run err stream");
	}

	//endregion

}
