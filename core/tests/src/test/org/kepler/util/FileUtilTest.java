/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 * CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 *
 */

package test.org.kepler.util;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.util.DotKeplerManager;
import org.kepler.util.FileUtil;

public class FileUtilTest extends TestCase {
	private boolean debug = false;

	private File _tempDir;

	private File[] _files;

	public FileUtilTest(String name) {
		super(name);
		if (debug)
			System.out.println("FileUtilTest(" + name + ")");

		_tempDir = new File(DotKeplerManager.getInstance().getCacheDir(), "temptest");
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() {
		if (debug)
			System.out.println("setUp()");

		if (debug)
			System.out.println("_tempDir: " + _tempDir);
		if (_tempDir.exists()) {
			_tempDir.delete();
		}
		_tempDir.mkdirs();

		_files = new File[4];

		_files[0] = new File(_tempDir, "dir0");
		_files[1] = new File(_files[0], "dir1");
		_files[2] = new File(_tempDir, "dir/dir2");
		_files[3] = new File(_files[2], "dir3");

		printFiles();

		for (int i = 0; i < _files.length; i++) {
			_files[i].mkdirs();
		}

		if (debug)
			System.out.println("setUp() Finished");
	}

	private void printFiles() {

		for (int i = 0; i < _files.length; i++) {
			if (debug)
				System.out.println(i + ": " + _files[i]);
			try {
				if (debug)
					System.out
							.println("   clean: " + FileUtil.clean(_files[i]));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {
		if (debug)
			System.out.println("tearDown()");
		if (_tempDir.exists()) {
			// Free up files so we can delete them
			for (int i = 0; i < _files.length; i++) {
				_files[i] = null;
			}
			// delete the test directory
			if (_tempDir.delete()) {
				if (debug)
					System.out.println("Deleted testDir");
			} else {
				if (debug)
					System.out.println("Failed to delete testDir");
			}
		}
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {

		TestSuite suite = new TestSuite();
		suite.addTest(new FileUtilTest("initialize"));
		suite.addTest(new FileUtilTest("testSubDir"));

		return suite;
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() {
		if (debug)
			System.out.println("initialize()");
		assertTrue(1 == 1);
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void testSubDir() {
		if (debug)
			System.out.println("testSubDir()");
		printFiles();
		try {
			assertTrue(FileUtil.isSubdirectory(_tempDir, _files[2]));
			assertFalse(FileUtil.isSubdirectory(_files[2], _tempDir));

		} catch (IOException e) {
			fail();
			e.printStackTrace();
		}

	}
}
