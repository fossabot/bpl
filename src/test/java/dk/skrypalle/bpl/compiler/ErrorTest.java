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

	//region syntax

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSyntax.class,
		expectedExceptionsMessageRegExp = "1:25: error: no viable alternative at input 'retur0'")
	public void testErrNoViableAlt(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { retur 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrSyntax.class,
		expectedExceptionsMessageRegExp = "1:5: error: token recognition error at: '~'")
	public void testErrTokenRecognition(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func~ main() int { return 0; }", tmpDir));
	} // test eval via thrown exception

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
		expectedExceptionsMessageRegExp = "1:58: error: too few arguments to function 'x' - have \\[\\] want \\[\\[int\\]\\]")
	public void testErrTooFewArgsOnCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x(i int) int { return i; } func main() int { return x(); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:53: error: too many arguments to function 'x' - have \\[int\\] want \\[\\[\\]\\]")
	public void testErrTooManyArgsOnCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x() int { return 0; } func main() int { return x(1); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:85: error: too many arguments to function 'x' - have \\[int, int\\] want \\[\\[\\], \\[int\\]\\]")
	public void testErrTooManyArgsOnOverloadCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x() int { return 0; } func x(a int) int { return a; } func main() int { return x(1,2); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:99: error: too few arguments to function 'x' - have \\[\\] want \\[\\[int\\], \\[int, int\\]\\]")
	public void testErrTooFewArgsOnOverloadCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x(a int) int { return a; } func x(a int, b int) int { return a+b; } func main() int { return x(); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongNumArgs.class,
		expectedExceptionsMessageRegExp = "1:94: error: wrong number of arguments to function 'x' - have \\[int\\] want \\[\\[\\], \\[int, int\\]\\]")
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

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrInvalidDereference.class,
		expectedExceptionsMessageRegExp = "2:11: error: invalid dereference of 'x' - type int")
	public void testErrInvalidDereferenceOfSymbol(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("x:=1; y:=*x;"), tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrInvalidDereference.class,
		expectedExceptionsMessageRegExp = "2:5: error: invalid dereference of '1' - type int")
	public void testErrInvalidDereferenceOfLiteral(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("x:=*1;"), tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrUnaddressable.class,
		expectedExceptionsMessageRegExp = "1:55: error: cannot take the address of 'a\\(1\\)'")
	public void testErrUnaddressableSymbol(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func a(i int) int { return i; } func main() int { i:=&a(1); return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrUnaddressable.class,
		expectedExceptionsMessageRegExp = "2:5: error: cannot take the address of '1'")
	public void testErrUnaddressableLiteral(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("x:=&1;"), tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrUnassignable.class,
		expectedExceptionsMessageRegExp = "2:1: error: cannot assign to '1'")
	public void testErrUnassignableLiteral(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, wrapMain("1=1;"), tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrUnassignable.class,
		expectedExceptionsMessageRegExp = "1:51: error: cannot assign to 'a\\(1\\)'")
	public void testErrUnassignableSymbol(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func a(i int) int { return i; } func main() int { a(1)=2; return 0; }", tmpDir));
	} // test eval via thrown exception

	//endregion

	//region types

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:19: error: type mismatch at 'return\"mismatch\";' - have string want int")
	public void testErrTypeMismatchOnReturn(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { return \"mismatch\"; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:31: error: type mismatch at 's=0;' - have int want string")
	public void testErrTypeMismatchOnAssign(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; s = 0; return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:19: error: type mismatch at 's:string=0;' - have int want string")
	public void testErrTypeMismatchOnDeclAssign(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string = 0; return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:43: error: type mismatch at 's' - have string want int")
	public void testErrTypeMismatchBranchCondition(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; s = \"1\"; if(s) { return 0; } else { return 1; } }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:46: error: type mismatch at 's' - have string want int")
	public void testErrTypeMismatchLoopCondition(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; s = \"1\"; while(s) { return 0; } }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:58: error: type mismatch at 's\\+i' - have \\[int, string\\] want \\[int, int\\]")
	public void testErrTypeMismatchBinOp(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; i : int; s = \"1\"; i = 1; s=s+i; return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeMismatch.class,
		expectedExceptionsMessageRegExp = "1:58: error: type mismatch at 's&&i' - have \\[int, string\\] want \\[int, int\\]")
	public void testErrTypeMismatchBoolOp(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() int { s : string; i : int; s = \"1\"; i = 1; i=s&&i; return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrTypeUndeclared.class,
		expectedExceptionsMessageRegExp = "1:13: error: type 'undef' undeclared")
	public void testErrTypeUndeclared(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func main() undef { return 0; }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongArgTypes.class,
		expectedExceptionsMessageRegExp = "1:58: error: wrong types of arguments to function 'x' - have \\[string\\] want \\[\\[int\\]\\]")
	public void testErrWrongTypesOfArgumentsOnCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func x(a int) int { return 0; } func main() int { return x(\"1\"); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrWrongArgTypes.class,
		expectedExceptionsMessageRegExp = "3:26: error: wrong types of arguments to function 'x' - have \\[string, int\\] want \\[\\[int, int\\], \\[int, string\\]\\]")
	public void testErrWrongTypesOfArgumentsOnOverloadedCall(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, String.join("\n",
			"func x(a int, b int) int    { return 0; }",
			"func x(a int, b string) int { return 0; }",
			"func main() int { return x(\"1\", 2); }"
		), tmpDir));
	} // test eval via thrown exception

	// endregion

	//region void

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrVoidAsValue.class,
		expectedExceptionsMessageRegExp = "1:37: error: 'a\\(\\)' has no type but is used as value")
	public void testErrVoidAsValueInVarDeclAssign(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func a() {} func main() int { x:int=a(); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrVoidAsValue.class,
		expectedExceptionsMessageRegExp = "1:40: error: 'a\\(\\)' has no type but is used as value")
	public void testErrVoidAsValueInVarAssign(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func a() {} func main() int { x:int; x=a(); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrVoidAsValue.class,
		expectedExceptionsMessageRegExp = "1:34: error: 'a\\(\\)' has no type but is used as value")
	public void testErrVoidAsValueInVarDeclAssignTI(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func a() {} func main() int { x:=a(); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrVoidAsValue.class,
		expectedExceptionsMessageRegExp = "1:50: error: 'a\\(\\)' has no type but is used as value")
	public void testErrVoidAsValueInFuncParam(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func a() {} func b(a int) {} func main() int { b(a()); }", tmpDir));
	} // test eval via thrown exception

	@Test(dataProvider = "provideCompileSwitch",
		expectedExceptions = BPLCErrVoidAsValue.class,
		expectedExceptionsMessageRegExp = "1:35: error: 'a\\(\\)' has no type but is used as value")
	public void testErrVoidAsValueInReturn(Target t) throws Throwable {
		execWithTmpDir(tmpDir -> t.compile(this, "func a() {} func b() int { return a(); } func main() int { b(); }", tmpDir));
	} // test eval via thrown exception

	//endregion

}
