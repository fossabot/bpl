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

import java.io.*;
import java.nio.file.*;

public final class Exec {

	public static boolean trace = false;

	public static ExecRes exec(String[] cmd, Path dir) throws IOException {
		if (!Files.isDirectory(dir))
			throw new IllegalArgumentException("'dir' must point to a directory");

		if (trace)
			System.out.println(String.join(" ", cmd));

		Process p = Runtime.getRuntime().exec(cmd, null, dir.toFile());
		int exit;
		String out;
		String err;

		try {
			exit = p.waitFor();
			out = IO.readAll(p.getInputStream());
			err = IO.readAll(p.getErrorStream());
			p.destroy();
			return new ExecRes(exit, out, err);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static ExecRes gcc(Path file) throws IOException {
		if (Files.isDirectory(file))
			throw new IllegalArgumentException("'file' must point to a file");

		String fName = file.getFileName().toString();
		if (!fName.endsWith(".c"))
			throw new IllegalArgumentException("'file' must have '.c' extension");

		int idx = fName.lastIndexOf(".c");
		fName = fName.substring(0, idx);
		String[] cmd = {
			"gcc",
			"-std=c99",
			"-Wall",
			"-Wextra",
			"-Wpedantic",
			"-Wno-format",
			"-Wno-unused-parameter",
			"-Wno-unused-function",
			"--save-temps",
			"-o" + fName + "." + OS.exeEXT(),
			fName + ".c"
		};
		return exec(cmd, file.getParent());
	}

	public static ExecRes exec(Path exe) throws IOException {
		if (Files.isDirectory(exe))
			throw new IllegalArgumentException("'exe' must point to a file");
		if (!Files.isExecutable(exe))
			throw new IllegalArgumentException("'exe' must be executable");

		return exec(new String[]{exe.toAbsolutePath().toString()}, exe.getParent());
	}

	private Exec() { /**/ }

}
