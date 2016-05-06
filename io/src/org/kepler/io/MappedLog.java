/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.kepler.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//////////////////////////////////////////////////////////////////////////
//// MappedLog

/**
 * Log a string (single-line) into a file but also keep all text in a HashSet so
 * that the strings can quickly looked up. This class is useful to create simple
 * checkpoint mechanism.
 * 
 * At first call, MappedLog looks for the specified file and reads it into
 * memory if exists.
 * 
 * At each call, the MappedLog checks if a the input line already is in the set.
 * If not, it writes the line into the set and the file. It returns the boolean
 * flag indicating whether the line was already found (true) or not (false). The
 * check and write is an atomic operation, so two actors cannot mix up this
 * behaviour.
 * 
 * All actors can write into the same file, if their parameter points to the
 * same file. This allows checking if others already did (and logged) something.
 * 
 * Query only (not writing out a line, but only checking its existence) can be
 * achieved by setting the boolean flag 'checkOnly'.
 * 
 * If the line is empty (or only white spaces), nothing will be written and
 * false will be returned.
 * 
 * @author Norbert Podhorszki
 * @version $Id: MappedLog.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class MappedLog {

	public MappedLog() {
	}

	public boolean check(File logfile, String logText) {
		return checkOrAdd(logfile, logText, true);
	}

	public boolean add(File logfile, String logText) {
		return checkOrAdd(logfile, logText, false);
	}

	/**
	 * Close all opened log file in a synchronized way. The hashtable elements
	 * cannot be removed during the iterator, because that is fail-fast. So we
	 * clear the hashtable at the end.
	 */
	public static void closeAll() throws IOException {

		synchronized (logFiles) {
			Iterator logs = logFiles.keySet().iterator();
			while (logs.hasNext()) {
				String path = (String) logs.next();
				MappedLogFile lf = (MappedLogFile) logFiles.get(path);
				lf.close();
				if (isDebugging)
					log.debug("Closed log " + path);
			}
			logFiles.clear(); // remove all elements
		}
	}

	private boolean checkOrAdd(File logfile, String logText, boolean checkOnly) {
		// if empty string, we do not do anything
		if (logText.trim().length() == 0) {
			if (isDebugging)
				log.debug("Empty input. do nothing. return false.");
			return false;
		}

		try {
			MappedLogFile lf = getMappedLogFile(logfile); // get the log file or
															// create it
			if (checkOnly)
				return lf.contains(logText);
			else
				return lf.print(logText);

		} catch (Exception ex) {
			log.error(ex);
			return false;
		}
	}

	private static SimpleDateFormat dateformat = new SimpleDateFormat(
			"MMM dd yyyy HH:mm:ss.SSS");
	private boolean _xmlFormat = false;
	private String _header;

	// apache commons log for the source code logging.
	private static final Log log = LogFactory.getLog(MappedLog.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/*
	 * The log files already created (key = their absolute path as String),
	 * value=the MappedLog
	 */
	private static Hashtable logFiles = new Hashtable();

	/**
	 * Get the mapped log. If it was not yet requested, open create it.
	 */
	private MappedLogFile getMappedLogFile(File file) throws IOException {

		String path = file.getAbsolutePath();
		MappedLogFile lf = null;

		synchronized (logFiles) {
			lf = (MappedLogFile) logFiles.get(path);
			if (lf == null) {
				lf = new MappedLogFile(file);
				logFiles.put(path, lf);
			}
		}
		return lf;
	}

	/**
	 * Record of PrintWriter and HashSet to store what we need about a mapped
	 * log in the hash table.
	 */
	private class MappedLogFile {
		private HashSet set;
		private PrintWriter writer;

		public MappedLogFile(File f) throws IOException {
			set = new HashSet();
			int nLines = 0;
			if (f.exists()) {
				// read file contents into hash set
				try {
					BufferedReader in = new BufferedReader(new FileReader(f));
					if (!in.ready())
						throw new IOException();
					String line;
					while ((line = in.readLine()) != null) {
						set.add(line);
						nLines++;
					}
					in.close();
				} catch (IOException e) {
					log.error("Error at reading the file " + f + ": " + e);
					System.out.println("Error at reading the file " + f + ": "
							+ e);
				}
			}
			writer = new PrintWriter(new FileWriter(f, true)); // append mode
			if (isDebugging)
				log.info("New MappedLogFile created for file " + f + " with "
						+ nLines + " lines of existing text");
		}

		/**
		 * Print line if it is not already in the set. Returns true if line was
		 * already in the set, false otherwise. Synchronized on the PrintWriter
		 * object.
		 */
		public boolean print(String line) {
			synchronized (this) {
				if (isDebugging)
					log.debug("start print() on " + line);
				if (!set.contains(line)) {
					set.add(line);
					writer.println(line);
					writer.flush();
					return false;
				} else
					return true;
			}
		}

		/**
		 * Check if the line is in the set. Synchronized on the PrintWriter
		 * object.
		 */
		public boolean contains(String line) {
			if (isDebugging)
				log.debug("start contains() on " + line);
			boolean found = false;
			synchronized (this) {
				found = set.contains(line);
			}
			return found;
		}

		/**
		 * Close log file and destroy the memory map.
		 */
		public void close() {
			if (writer != null) {
				writer.close();
				writer = null;
			}
			if (set != null) {
				set.clear();
				set = null;
			}
		}
	}

}