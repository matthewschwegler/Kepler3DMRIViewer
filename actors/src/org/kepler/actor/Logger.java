/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-04-30 13:53:16 -0700 (Tue, 30 Apr 2013) $' 
 * '$Revision: 31973 $'
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

package org.kepler.actor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.io.SharedLog;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// Logger

/**
 * Log information directed into a file. All Logger actors can write into the
 * same file, if their parameter points to the same file. Format as it is, or
 * XML. text format: date: header: text XML format:
 * 
 * 
 * If the text is empty (or only white spaces), nothing will be written. So you
 * do not need to filter out e.g. empty stderr messages before connecting to
 * this Logger actor.
 * 
 * @author Norbert Podhorszki
 * @version $Id: Logger.java 31973 2013-04-30 20:53:16Z jianwu $
 * @since Ptolemy II 5.0.1
 */
public class Logger extends TypedAtomicActor {
	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public Logger(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Logger file parameter
		logfile = new FileParameter(this, "logfile");
		logfile.setExpression("$CWD" + File.separator + "kepler.log");

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());
		text = new TypedIOPort(this, "text", true, false);
		text.setTypeEquals(BaseType.STRING);
		new Parameter(text, "_showName", BooleanToken.FALSE);

		// header is a fixed string to print out (e.g. an actor name)
		header = new StringParameter(this, "header");
		header.setExpression("");

		// parameter for format: "text" or "xml"
		format = new StringParameter(this, "format");
		format.setExpression("text");
		
		// parameter for append:
		append = new Parameter(this,
				"alwaysAppend");
		append.setTypeEquals(BaseType.BOOLEAN);
		append.setToken(BooleanToken.FALSE);
	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * The log file. It can be changed between firings. The file will be created
	 * if does not exists, otherwise text will be appended to the existing file.
	 */
	public FileParameter logfile;

	/**
	 * The text to be printed into the log. This port is expects strings.
	 */
	public TypedIOPort text;

	/**
	 * The header information to be printed with the log. Useful to provide an
	 * actor name, or some meaningful name. Date is printed independently to the
	 * log.
	 */
	public StringParameter header;

	/**
	 * The format of the logger. Currently "text" or "xml". Other value will
	 * mean "text". If different loggers use different format option for the
	 * same file, the output will be a mixture of different formats.
	 */
	public StringParameter format;
	
	/**
	 * If true, always append the new log info to the log file.
	 */
	public Parameter append;

	/***********************************************************
	 * public methods
	 */

	/**
	 * Set the format for the logging.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		String logFormat = format.stringValue();
		_xmlFormat = logFormat.trim().equalsIgnoreCase("xml");
		_header = header.stringValue();
		_sharedLog = new SharedLog(_xmlFormat);
		_append = ((BooleanToken)append.getToken()).booleanValue();
		if (isDebugging)
			log.debug("format is [" + logFormat + "]. header is [" + _header
					+ "].");

	}

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 *                Not thrown.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		String logText = ((StringToken) text.get(0)).stringValue();

		_sharedLog.print(logfile.asFile(), _header, logText, _append);
	}

	/**
	 * Close all opened log files. This method is invoked exactly once per
	 * execution of an application. None of the other action methods should be
	 * be invoked after it.
	 * 
	 * @exception IllegalActionException
	 *                Not thrown in this base class.
	 */
	public void wrapup() throws IllegalActionException {
		super.wrapup();
		if (isDebugging)
			log.debug("wrapup begin");
		try {
			_sharedLog.closeAll();
		} catch (IOException ex) {
			log.error("Error at wrapup: " + ex);
		}
		if (isDebugging)
			log.debug("wrapup end");
	}

	/**
	 * Close all opened log files. This method is invoked exactly once per
	 * execution of an application. None of the other action methods should be
	 * be invoked after it.
	 * 
	 * @exception IllegalActionException
	 *                Not thrown in this base class.
	 */
	public void stop() {
		if (isDebugging)
			log.debug("stop begin");
		super.stop();
		try {
			_sharedLog.closeAll();
		} catch (IOException ex) {
			log.error("Error at stop: " + ex);
		}
		if (isDebugging)
			log.debug("stop end");
	}

	private boolean _xmlFormat = false;
	private String _header;
	private SharedLog _sharedLog;
	private boolean _append = false;

	// apache commons log for the source code logging.
	private static final Log log = LogFactory.getLog(Logger.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}