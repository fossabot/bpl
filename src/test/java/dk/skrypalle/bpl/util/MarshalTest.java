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

import org.testng.*;
import org.testng.annotations.*;

public class MarshalTest {

	@DataProvider
	public Object[][] provideS32() {
		return new Object[][]{
			{0x00, 0x00, 0x00, 0x00, 0x00000000},
			{0x00, 0x00, 0x00, 0x01, 0x00000001},
			{0xff, 0xff, 0xff, 0xff, 0xffffffff},
			{0x80, 0x00, 0x00, 0x00, 0x80000000},
		};
	}

	@Test(dataProvider = "provideS32")
	public void testS32BE(int b0, int b1, int b2, int b3, int exp) {
		byte[] data = new byte[]{(byte) b0, (byte) b1, (byte) b2, (byte) b3};
		int act = Marshal.s32BE(data, 0);
		Assert.assertEquals(act, exp);
	}

	@Test(dataProvider = "provideS32")
	public void testS32LE(int b0, int b1, int b2, int b3, int exp) {
		byte[] data = new byte[]{(byte) b3, (byte) b2, (byte) b1, (byte) b0};
		int act = Marshal.s32LE(data, 0);
		Assert.assertEquals(act, exp);
	}

}
