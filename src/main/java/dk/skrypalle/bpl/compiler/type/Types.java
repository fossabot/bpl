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

import java.util.*;

public final class Types {

	private static final Map<String, Type>  fwd;
	private static final Map<Integer, Type> rev;

	static {
		fwd = new HashMap<>();
		rev = new HashMap<>();
		newPrimitive("int", "int64_t");
		newPrimitive("string", "char*");
	}

	public static Type lookup(String name) {
		return fwd.get(name);
	}

	public static Type lookup(int vm_type) {
		return rev.get(vm_type);
	}

	private static Type newPrimitive(String name, String c_name) {
		Type t = new Type(name, fwd.size(), c_name);
		fwd.put(name, t);
		rev.put(t.vm_type, t);
		return t;
	}

	private Types() { /**/ }

}
