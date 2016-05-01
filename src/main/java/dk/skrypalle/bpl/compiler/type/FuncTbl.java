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

public class FuncTbl {

	private final Map<String, Map<List<DataType>, Func>> map;

	public FuncTbl(FuncTbl other) {
		map = new HashMap<>(other.map);
	}

	public FuncTbl() {
		map = new HashMap<>();
	}

	public void decl(Func f) {
		Map<List<DataType>, Func> overloads = map.get(f.id);
		if (overloads == null) {
			overloads = new HashMap<>();
			overloads.put(f.params, f);
			map.put(f.id, overloads);
		} else {
			overloads.put(f.params, f);
		}
	}

	public boolean isDecl(String id) {
		return map.get(id) != null;
	}

	public boolean isDecl(String id, List<DataType> params) {
		return get(id, params) != null;
	}

	public Func getFirst(String id) {
		Map<List<DataType>, Func> overloads = map.get(id);
		if (overloads == null)
			return null;

		for (Func f : overloads.values())
			return f;
		return null;
	}

	public Func get(String id, List<DataType> params) {
		Map<List<DataType>, Func> overloads = map.get(id);
		if (overloads == null)
			return null;
		return overloads.get(params);
	}

	public int[] getOverloadedParams(String id) {
		Map<List<DataType>, Func> overloads = map.get(id);
		if (overloads == null)
			return null;

		int[] res = new int[overloads.keySet().size()];
		int i = 0;
		for (List<DataType> params : overloads.keySet()) {
			res[i++] = params.size();
		}
		Arrays.sort(res);
		return res;
	}

	public List<Func> flatten() {
		List<Func> res = new ArrayList<>();
		for (Map<List<DataType>, Func> overloads : map.values())
			res.addAll(overloads.values());
		return res;
	}

	public boolean hasOverloads(String id) {
		Map<List<DataType>, Func> overloads = map.get(id);
		return overloads != null && !overloads.isEmpty();
	}

	public void merge(FuncTbl other) {
		this.map.putAll(other.map);
	}

	@Override
	public String toString() {
		return "FuncTbl{" +
			"map=" + map +
			'}';
	}
}
