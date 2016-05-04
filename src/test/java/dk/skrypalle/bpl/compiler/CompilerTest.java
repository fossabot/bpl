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
import org.apache.commons.lang3.*;
import org.testng.*;
import org.testng.annotations.*;

import java.io.*;

public class CompilerTest extends CompilerTestBase {

	//region data provider

	@DataProvider(parallel = true)
	public Object[][] provideData() throws IOException {
		return new Object[][]{
			//fmt:off
			{"print multi",       wrapMain("print(1,2,3,4,5);"),                      "12345"},
			{"print int8",        wrapMain("print(0);"),                                  "0"},
			{"print int8",        wrapMain("print(1);"),                                  "1"},
			{"print int8",        wrapMain("print(255);"),                               "ff"},
			{"print int16",       wrapMain("print(256);"),                              "100"},
			{"print int32",       wrapMain("print(1234567890);"),                  "499602d2"},
			{"print int64",       wrapMain("print(9223372036854775807);"), "7fffffffffffffff"},
			{"int add",           wrapMain("print(1+42+5+6);"),                          "36"},
			{"int sub",           wrapMain("print(3-2);"),                                "1"},
			{"int mul",           wrapMain("print(2*3);"),                                "6"},
			{"int div",           wrapMain("print(6/2);"),                                "3"},
			{"int div",           wrapMain("print(7/2);"),                                "3"},
			{"int precedence -+", wrapMain("print(8-2+5);"),                              "b"},
			{"int precedence /*", wrapMain("print(8/2*4);"),                             "10"},
			{"int precedence /*", wrapMain("print(8/3*4);"),                              "8"},
			{"int precedence +*", wrapMain("print(2+3*3);"),                              "b"},
			{"int precedence -*", wrapMain("print(9-2*3);"),                              "3"},
			{"overflow int8",     wrapMain("print(255+1);"),                            "100"},

			{"ilt true",          wrapMain("print(1 < 2);"),                              "1"},
			{"ilt false",         wrapMain("print(2 < 2);"),                              "0"},
			{"igt true",          wrapMain("print(3 > 2);"),                              "1"},
			{"igt false",         wrapMain("print(2 > 2);"),                              "0"},
			{"ilte true",         wrapMain("print(2 <= 2);"),                             "1"},
			{"ilte false",        wrapMain("print(3 <= 1);"),                             "0"},
			{"igte true",         wrapMain("print(2 >= 2);"),                             "1"},
			{"igte false",        wrapMain("print(1 >= 2);"),                             "0"},
			{"ieq true",          wrapMain("print(2 == 2);"),                             "1"},
			{"ieq false",         wrapMain("print(1 == 2);"),                             "0"},
			{"ineq true",         wrapMain("print(1 != 2);"),                             "1"},
			{"ineq false",        wrapMain("print(2 != 2);"),                             "0"},

			{"and true",          wrapMain("print(1 && 1);"),                             "1"},
			{"and left false",    wrapMain("print(0 && 1);"),                             "0"},
			{"and right false",   wrapMain("print(1 && 0);"),                             "0"},
			{"or false",          wrapMain("print(0 || 0);"),                             "0"},
			{"or left true",      wrapMain("print(0 || 1);"),                             "1"},
			{"or right true",     wrapMain("print(1 || 0);"),                             "1"},

			{"string",            wrapMain("print(\"Hello, World!\");"),      "Hello, World!"},
			//fmt:on

			loadTestFile("var/simple"),
			loadTestFile("var/simple_calc"),
			loadTestFile("var/simple_decl_assign"),
			loadTestFile("var/simple_inferred"),
			loadTestFile("var/multi"),
			loadTestFile("var/multi_decl_assign"),
			loadTestFile("var/multi_inferred"),
			loadTestFile("var/complex"),
			loadTestFile("var/complex_decl_assign"),
			loadTestFile("var/complex_inferred"),
			loadTestFile("var/strings"),
			loadTestFile("var/escape_sequence"),
			loadTestFile("var/scopes"),
			loadTestFile("var/scopes_decl_assign"),
			loadTestFile("var/scopes_inferred"),

			loadTestFile("func/call_simple"),
			loadTestFile("func/call_simple_params"),
			loadTestFile("func/call_before_and_after_def"),
			loadTestFile("func/call_before_def"),
			loadTestFile("func/overload"),

			loadTestFile("branch/if_0"),
			loadTestFile("branch/if_1"),
			loadTestFile("branch/if_0_no_braces"),
			loadTestFile("branch/if_1_no_braces"),
			loadTestFile("branch/if_0_one_armed"),
			loadTestFile("branch/if_1_one_armed"),

			loadTestFile("recursion/factorial"),
			loadTestFile("recursion/factorial_v2"),
			loadTestFile("recursion/factorial_v3"),
			loadTestFile("recursion/fibonacci"),

			loadTestFile("operators/and_skip_right"),
			loadTestFile("operators/or_skip_right"),
			loadTestFile("operators/and_or_precedence"),

			loadTestFile("loop/while"),
			loadTestFile("loop/while_no_braces"),
			loadTestFile("loop/factorial"),
			loadTestFile("loop/fibonacci"),
			loadTestFile("loop/fibonacci_v2"),

			loadTestFile("defer/simple"),
			loadTestFile("defer/recursion"),
		};
	}

	//endregion

	private String[] loadTestFile(String name) throws IOException {
		try (InputStream in = CompilerTest.class.getResourceAsStream("/compiler/" + name + ".test")) {
			if (in == null)
				throw new IllegalArgumentException(String.format("test /compiler/%s.test not found", name));
			String f = IO.readAll(in);
			String[] res = f.split("::exp");
			String act = res[0];
			String exp = res[1].trim();
			exp = StringEscapeUtils.unescapeJava(exp);
			exp = exp.replaceAll("\n", System.lineSeparator());

			return new String[]{name, act, exp};
		}
	}

	//region targets

	@Test(dataProvider = "provideData")
	public void testTargetBC(String desc, String bpl, String exp) {
		byte[] bc = compileBC(bpl, null);
		VMExecRes res = runBC(bc);

		Assert.assertEquals(res.exit, 0, "BPLVM exit status (" + desc + ")");
		Assert.assertEquals(res.out, exp, "BPLVM out stream (" + desc + ")");
		Assert.assertEquals(res.err, "", "BPLVM err stream (" + desc + ")");
		Assert.assertEquals(res.dbg, "", "BPLVM dbg stream (" + desc + ")");
	}

	@Test(dataProvider = "provideData")
	public void testTargetC99(String desc, String bpl, String exp) throws Throwable {
		execWithTmpDir(tmpDir -> {
			ExecRes gcc = compileC99(bpl, tmpDir);
			if (!gcc.isEmpty())
				System.err.println(gcc);
			Assert.assertEquals(gcc.exit, 0, "gcc exit status (" + desc + ")");
			Assert.assertEquals(gcc.out, "", "gcc out stream (" + desc + ")");
			Assert.assertEquals(gcc.err, "", "gcc err stream (" + desc + ")");

			ExecRes run = runC99(tmpDir);
			Assert.assertEquals(run.exit, 0, "run exit status (" + desc + ")");
			Assert.assertEquals(run.out, exp, "run out stream (" + desc + ")");
			Assert.assertEquals(run.err, "", "run err stream (" + desc + ")");
		});
	}

	//endregion

}
