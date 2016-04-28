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

import dk.skrypalle.bpl.compiler.err.*;
import org.testng.annotations.*;

import java.util.function.*;

public class ErrorTest extends CompilerTestBase {

	//region enum Target, data provider

	private enum Target {
		BC(CompilerTestBase::compileBC),
		C99(CompilerTestBase::compileC99);
		private final BiConsumer<CompilerTestBase, String> func;

		Target(BiConsumer<CompilerTestBase, String> func) {
			this.func = func;
		}

		private void compile(CompilerTestBase b, String s) { func.accept(b, s); }
	}

	@DataProvider
	public Object[][] provideCompileSwitch() {
		return new Object[][]{{Target.BC}, {Target.C99}};
	}

	//endregion

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSymUndeclared.class,
		expectedExceptionsMessageRegExp = "2:7: error: symbol 'x' undeclared")
	public void testErrVarUndeclaredOnRead(Target t) throws Exception {
		t.compile(this, wrapMain("print(x);"));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSymUndeclared.class,
		expectedExceptionsMessageRegExp = "2:1: error: symbol 'x' undeclared")
	public void testErrVarUndeclaredOnWrite(Target t) throws Exception {
		t.compile(this, wrapMain("x=12;"));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSymRedeclared.class,
		expectedExceptionsMessageRegExp = "2:16: error: symbol 'x' redeclared")
	public void testErrVarRedeclared(Target t) throws Exception {
		t.compile(this, wrapMain("var x int; var x int;"));
	} // test eval via thrown exception

}
