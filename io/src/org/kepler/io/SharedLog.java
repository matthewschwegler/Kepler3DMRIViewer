/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-05-01 09:34:18 -0700 (Wed, 01 May 2013) $' 
 * '$Revision: 31976 $'
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//////////////////////////////////////////////////////////////////////////
//// Log

/**
 * Log information directed into a file. All actors can write into the same
 * file, if they point to the same file. Format as it is, or XML. text format:
 * date: header: text XML format:
 * 
 * 
 * If the text is empty (or only white spaces), nothing will be written. So you
 * do not need to filter out e.g. empty stderr messages before connecting to
 * Logger actors.
 * 
 * @author Norbert Podhorszki
 * @version $Id: SharedLog.java 31976 2013-05-01 16:34:18Z jianwu $
 * @since Ptolemy II 5.0.1
 */
public class SharedLog {

	public SharedLog(boolean xmlFormat) {
		_xmlFormat = xmlFormat;
	}
	
	public void print(File logfile, String header, String logText) {
		print(logfile, header, logText, false);
	}

	public void print(File logfile, String header, String logText, boolean append) {

		// empty string will not do anything
		if (logText == null || logText.trim().length() == 0)
			return;

		try {
			LogFile lf = getLogFile(logfile, append);
			Date date = Calendar.getInstance().getTime();
			PrintWriter writer = lf.getPrintWriter();
			synchronized (writer) {
				if (lf.isXMLFormat()) {
					writer.println("  <item>");
					writer.println("    <date value=\""
							+ dateformat.format(date) + "\"/>");
					writer.println("    <header value=\"" + header + "\"/>");
					writer.println("    <text>" + logText + "</text>");
					writer.println("  </item>");
				} else {
					writer.println(dateformat.format(date) + ": " + header
							+ ": " + logText);
				}
				writer.flush();
			}
		} catch (Exception ex) {
			log.error(ex);
		}

	}

	/**
	 * Close all opened log file in a synchronized way. This method prints the
	 * final line The hashtable elements cannot be removed during the iterator,
	 * because that is fail-fast. So we clear the hashtable at the end.
	 */
	public static void closeAll() throws IOException {

		synchronized (logFiles) {
			Iterator writers = logFiles.keySet().iterator();
			while (writers.hasNext()) {
				String path = (String) writers.next();
				LogFile lf = (LogFile) logFiles.get(path);
				PrintWriter writer = lf.getPrintWriter();

				if (lf.isXMLFormat()) {
					writer.println("</log>");
				} else {
					writer
							.println("----------------------------------------------");
				}
				writer.close();
				if (isDebugging)
					log.debug("Closed log " + path);
			}
			logFiles.clear(); // remove all elements
		}
	}

	private static SimpleDateFormat dateformat = new SimpleDateFormat(
			"MMM dd yyyy HH:mm:ss.SSS");
	private boolean _xmlFormat = false;

	// apache commons log for the source code logging.
	private static final Log log = LogFactory.getLog(SharedLog.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/*
	 * The log files already created (key = their absolute path as String),
	 * value=the LogFile
	 */
	private static Hashtable logFiles = new Hashtable();

	private LogFile getLogFile(File file, boolean append) throws IOException {

		String path = file.getAbsolutePath();
		LogFile lf = null;

		synchronized (logFiles) {
			lf = (LogFile) logFiles.get(path);
			if (lf == null) {
				PrintWriter writer = new PrintWriter(
						new FileWriter(file, append));
				lf = new LogFile(writer, _xmlFormat);
				logFiles.put(path, lf);
				Date date = Calendar.getInstance().getTime();
				if (_xmlFormat) {
					writer.println("<?xml version=\"1.0\" standalone=\"no\"?>");
					writer
							.println("<!DOCTYPE entity PUBLIC \"-//UC Davis//DTD Kepler Log 1//EN\" "
									+ "\"http://kepler-project.org/xml/dtd/KeplerLog_1.dtd\">");
					writer.println("<log>");
					writer.println("  <date format=\"" + dateformat.toPattern()
							+ "\"/>");
					writer.println("  <create_date value=\""
							+ dateformat.format(date) + "\"/>");
				} else {
					writer.println("Log file created on "
							+ dateformat.format(date));
					writer
							.println("Log date format: "
									+ dateformat.toPattern());
					writer
							.println("----------------------------------------------");
				}
				writer.flush();
			}
		}
		return lf;
	}

	/**
	 * Record of { PrintWriter writer, boolean isXmlFormat } to store what we
	 * need about a log file in the hash table.
	 */
	private class LogFile {
		private PrintWriter writer;
		private boolean isXmlFormat;

		public LogFile(PrintWriter w, boolean xmlFormat) {
			writer = w;
			isXmlFormat = xmlFormat;
		}

		public PrintWriter getPrintWriter() {
			return writer;
		}

		public boolean isXMLFormat() {
			return isXmlFormat;
		}

	}

}