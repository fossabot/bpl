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

package dk.skrypalle.bpl.compiler.type;

public enum DataType {

	INT(0x01, Long.class, "int64_t"),
	STRING(0x02, String.class, "char*");

	public final int    vm_type;
	public final Class  jclass;
	public final String c_type;

	DataType(int vm_type, Class jclass, String c_type) {
		this.vm_type = vm_type;
		this.jclass = jclass;
		this.c_type = c_type;
	}

	public static DataType parse(String s) {
		//fmt:off
		switch (s) {
		case "int"   : return INT;
		case "string": return STRING;
		default      : throw new IllegalStateException("unreachable");
		}
		//fmt:on
	}

	public static DataType parse(int vm_type) {
		for (DataType t : values()) {
			if (t.vm_type == vm_type)
				return t;
		}
		throw new IllegalStateException("unreachable");
	}

}
