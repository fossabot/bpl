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
	public Object[][] provideData() throws IOException {
		return new Object[][]{
			//fmt:off
			{"print int8",        wrapMain("print(0);"),                                        "0"},
			{"print int8",        wrapMain("print(1);"),                                        "1"},
			{"print int8",        wrapMain("print(255);"),                                     "ff"},
			{"print int16",       wrapMain("print(256);"),                                    "100"},
			{"print int32",       wrapMain("print(1234567890);"),                        "499602d2"},
			{"int add",           wrapMain("print(1+42+5+6);"),                                "36"},
			{"int sub",           wrapMain("print(3-2);"),                                      "1"},
			{"int mul",           wrapMain("print(2*3);"),                                      "6"},
			{"int div",           wrapMain("print(6/2);"),                                      "3"},
			{"int div",           wrapMain("print(7/2);"),                                      "3"},
			{"int precedence -+", wrapMain("print(8-2+5);"),                                    "b"},
			{"int precedence /*", wrapMain("print(8/2*4);"),                                   "10"},
			{"int precedence /*", wrapMain("print(8/3*4);"),                                    "8"},
			{"int precedence +*", wrapMain("print(2+3*3);"),                                    "b"},
			{"int precedence -*", wrapMain("print(9-2*3);"),                                    "3"},
			{"calc max int64",    wrapMain("print(18446744073709551568+42+5);"), "ffffffffffffffff"},
			{"overflow int8",     wrapMain("print(255+1);"),                                  "100"},

			{"ilt  true",         wrapMain("print(1 < 2);"),                                    "1"},
			{"ilt  false",        wrapMain("print(2 < 2);"),                                    "0"},
			{"igt  true",         wrapMain("print(3 > 2);"),                                    "1"},
			{"igt  false",        wrapMain("print(2 > 2);"),                                    "0"},
			{"ilte true",         wrapMain("print(2 <= 2);"),                                   "1"},
			{"ilte false",        wrapMain("print(3 <= 1);"),                                   "0"},
			{"igte true",         wrapMain("print(2 >= 2);"),                                   "1"},
			{"igte false",        wrapMain("print(1 >= 2);"),                                   "0"},
			{"ieq true",          wrapMain("print(2 == 2);"),                                   "1"},
			{"ieq false",         wrapMain("print(1 == 2);"),                                   "0"},
			{"ineq true",         wrapMain("print(1 != 2);"),                                   "1"},
			{"ineq false",        wrapMain("print(2 != 2);"),                                   "0"},
			//fmt:on

			loadTestFile("var/simple"),
			loadTestFile("var/simple_calc"),
			loadTestFile("var/multi"),
			loadTestFile("var/complex"),

			loadTestFile("func/call_simple"),
			loadTestFile("func/call_simple_params"),
			loadTestFile("func/call_before_and_after_def"),
			loadTestFile("func/call_before_def"),

			loadTestFile("branch/if_0"),
			loadTestFile("branch/if_1"),

			loadTestFile("recursion/factorial"),
			loadTestFile("recursion/factorial_v2"),
		};
	}

	//endregion

	private String[] loadTestFile(String name) throws IOException {
		try (InputStream in = CompilerTest.class.getResourceAsStream("/compiler/" + name + ".test")) {
			if (in == null)
				throw new IllegalArgumentException(String.format("test '%s' not found", name));
			String[] res = IO.readAll(in).split("::exp");

			return new String[]{name, res[0].trim(), res[1].trim()};
		}
	}

	//region targets

	@Test(dataProvider = "provideData")
	public void testTargetBC(String desc, String bpl, String exp) {
		byte[] bc = compileBC(bpl);
		VMExecRes res = runBC(bc);

		Assert.assertEquals(res.exit, 0, "BPLVM exit status (" + desc + ")");
		Assert.assertEquals(res.out, exp, "BPLVM out stream (" + desc + ")");
		Assert.assertEquals(res.err, "", "BPLVM err stream (" + desc + ")");
		Assert.assertEquals(res.dbg, "", "BPLVM dbg stream (" + desc + ")");
	}

	@Test(dataProvider = "provideData")
	public void testTargetC99(String desc, String bpl, String exp) throws IOException {
		ExecRes gcc = compileC99(bpl);
		if (!gcc.isEmpty())
			System.err.println(gcc);
		Assert.assertEquals(gcc.exit, 0, "gcc exit status (" + desc + ")");
		Assert.assertEquals(gcc.out, "", "gcc out stream (" + desc + ")");
		Assert.assertEquals(gcc.err, "", "gcc err stream (" + desc + ")");

		ExecRes run = runC99();
		Assert.assertEquals(run.exit, 0, "run exit status (" + desc + ")");
		Assert.assertEquals(run.out, exp, "run out stream (" + desc + ")");
		Assert.assertEquals(run.err, "", "run err stream (" + desc + ")");
	}

	//endregion

}
