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
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public final class IO {

	public static final Charset UTF8 = StandardCharsets.UTF_8;

	public static void writeAll(Path file, String s) throws IOException {
		if (Files.isDirectory(file))
			throw new IllegalArgumentException("'file' must point to a file");
		Files.write(file, s.getBytes(UTF8));
	}

	public static String readAll(Path file) throws IOException {
		if (Files.isDirectory(file))
			throw new IllegalArgumentException("'file' must point to a file");
		byte[] bytes = Files.readAllBytes(file);
		return new String(bytes, UTF8);
	}

	public static String readAll(InputStream in) {
		Scanner sc = new Scanner(in, "UTF8").useDelimiter("\\A");
		return sc.hasNext() ? sc.next() : "";
	}

	public static Path makeTmpDir(String prefix) throws IOException {
		return Files.createTempDirectory(prefix);
	}

	public static void delRec(Path p) throws IOException {
		Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private IO() { /**/ }

}
