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

import java.nio.file.*;

public class ErrorTest extends CompilerTestBase {

	//region enum Target, data provider

	private enum Target {
		BC(CompilerTestBase::compileBC),
		C99(CompilerTestBase::compileC99);
		private final Consumer func;

		Target(Consumer func) {
			this.func = func;
		}

		private void compile(CompilerTestBase b, String s, Path p) { func.accept(b, s, p); }
	}

	private interface Consumer {
		void accept(CompilerTestBase t, String s, Path p);
	}

	@DataProvider
	public Object[][] provideCompileSwitch() {
		return new Object[][]{{Target.BC}, {Target.C99}};
	}

	//endregion

	//region func

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrFuncRedeclared.class,
		expectedExceptionsMessageRegExp = "1:33: error: function 'x' redeclared")
	public void testErrFuncRedeclared(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x() int { return 0; } func x() int { return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrFuncUndeclared.class,
		expectedExceptionsMessageRegExp = "2:1: error: function 'x\\(\\)' undeclared")
	public void testErrFuncUndeclared(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("x();"), tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrReturnMissing.class,
		expectedExceptionsMessageRegExp = "1:19: error: missing return statement before '\\}'")
	public void testErrReturnMissing(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:58: error: too few arguments to function 'x' - have \\[\\] want \\[\\[INT\\]\\]")
	public void testErrTooFewArgsOnCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x(i int) int { return i; } func main() int { return x(); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:53: error: too many arguments to function 'x' - have \\[INT\\] want \\[\\[\\]\\]")
	public void testErrTooManyArgsOnCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x() int { return 0; } func main() int { return x(1); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:85: error: too many arguments to function 'x' - have \\[INT, INT\\] want \\[\\[\\], \\[INT\\]\\]")
	public void testErrTooManyArgsOnOverloadCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x() int { return 0; } func x(a int) int { return a; } func main() int { return x(1,2); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:99: error: too few arguments to function 'x' - have \\[\\] want \\[\\[INT\\], \\[INT, INT\\]\\]")
	public void testErrTooFewArgsOnOverloadCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x(a int) int { return a; } func x(a int, b int) int { return a+b; } func main() int { return x(); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:94: error: wrong number of arguments to function 'x' - have \\[INT\\] want \\[\\[\\], \\[INT, INT\\]\\]")
	public void testErrWrongNumArgsOnOverloadCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x() int { return 0; } func x(a int, b int) int { return a+b; } func main() int { return x(1); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrStatementUnreachable.class,
		expectedExceptionsMessageRegExp = "1:58: error: unreachable statement 'return'")
	public void testErrStatementUnreachableInBranch(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { if(1) { return 1; } else { return 2; } return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrReturnMissing.class,
		expectedExceptionsMessageRegExp = "1:48: error: missing return statement before '\\}'")
	public void testErrReturnMissingInBranch(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { if(1) { return 1; } else { } }", tmpDir));
	} // test eval via thrown exception

	//endregion

	//region var

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSymUndeclared.class,
		expectedExceptionsMessageRegExp = "2:7: error: symbol 'x' undeclared")
	public void testErrVarUndeclaredOnRead(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("print(x);"), tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSymUndeclared.class,
		expectedExceptionsMessageRegExp = "2:1: error: symbol 'x' undeclared")
	public void testErrVarUndeclaredOnWrite(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("x=12;"), tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSymRedeclared.class,
		expectedExceptionsMessageRegExp = "2:10: error: symbol 'x' redeclared")
	public void testErrVarRedeclared(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("x : int; x : int;"), tmpDir));
	} // test eval via thrown exception

	//endregion

	//region types

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:19: error: type mismatch at 'return\"mismatch\";' - have STRING want INT")
	public void testErrTypeMismatchOnReturn(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { return \"mismatch\"; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:31: error: type mismatch at 's=0;' - have INT want STRING")
	public void testErrTypeMismatchOnAssign(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; s = 0; return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:43: error: type mismatch at 's' - have STRING want INT")
	public void testErrTypeMismatchBranchCondition(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; s = \"1\"; if(s) { return 0; } else { return 1; } }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:46: error: type mismatch at 's' - have STRING want INT")
	public void testErrTypeMismatchLoopCondition(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; s = \"1\"; while(s) { return 0; } }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:58: error: type mismatch at 's\\+i' - have \\[INT, STRING\\] want \\[INT, INT\\]")
	public void testErrTypeMismatchBinOp(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; i : int; s = \"1\"; i = 1; s=s+i; return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:58: error: type mismatch at 's&&i' - have \\[INT, STRING\\] want \\[INT, INT\\]")
	public void testErrTypeMismatchBoolOp(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; i : int; s = \"1\"; i = 1; i=s&&i; return 0; }", tmpDir));
	} // test eval via thrown exception

	// endregion

}
