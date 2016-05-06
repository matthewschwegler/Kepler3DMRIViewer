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

package org.kepler.actor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.io.MappedLog;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// MappedLogger

/**
 * Log a string (single-line) into a file but also keep all text in a HashSet so
 * that the strings can quickly looked up. This actor is useful to create simple
 * checkpoint mechanism.
 * 
 * At first call, MappedLogger looks for the specified file and reads it into
 * memory if exists.
 * 
 * At each call, the MappedLogger checks if a the input line already is in the
 * set. If not, it writes the line into the set and the file. It returns the
 * boolean flag indicating whether the line was already found (true) or not
 * (false). The check and write is an atomic operation, so two actors cannot mix
 * up this behaviour.
 * 
 * All MappedLogger actors can write into the same file, if their parameter
 * points to the same file. This allows checking if others already did (and
 * logged) something.
 * 
 * Query only (not writing out a line, but only checking its existence) can be
 * achieved by setting the boolean flag 'checkOnly'.
 * 
 * If the line is empty (or only white spaces), nothing will be written and
 * false will be returned.
 * 
 * @author Norbert Podhorszki
 * @version $Id: MappedLogger.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class MappedLogger extends TypedAtomicActor {
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
	public MappedLogger(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Logger file parameter
		logfile = new FileParameter(this, "logfile");
		logfile.setExpression("$CWD" + File.separator + "kepler.ckpt");

		// line
		line = new TypedIOPort(this, "line", true, false);
		line.setTypeEquals(BaseType.STRING);
		new Parameter(line, "_showName", BooleanToken.FALSE);

		// flag: check only the line in already existing text
		checkOnly = new Parameter(this, "checkOnly", new BooleanToken(false));
		checkOnly.setTypeEquals(BaseType.BOOLEAN);

		// return value: true if the text is found in the map
		found = new TypedIOPort(this, "found", false, true);
		found.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(found, "_showName", BooleanToken.FALSE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * The log file. It can be changed between firings. The file will be created
	 * if does not exists. If exists its content will be read before the first
	 * execution.
	 */
	public FileParameter logfile;

	/**
	 * The text to be printed into the log. It must be single-line (no newline)
	 * This port expects strings.
	 */
	public TypedIOPort line;

	/**
	 * Boolean flag. If true, input will only be checked against already
	 * existing texts but will not be printed out.
	 */
	public Parameter checkOnly;

	/**
	 * The boolean return value. True if the line is found in the already
	 * existing text.
	 */
	public TypedIOPort found;

	/***********************************************************
	 * public methods
	 */

	/**
	 * Nothing to do currently.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_mappedLog = new MappedLog();
	}

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 *                Not thrown.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		String logText = ((StringToken) line.get(0)).stringValue();
		boolean bCheckOnly = ((BooleanToken) checkOnly.getToken())
				.booleanValue();

		// do the work
		boolean bFound;
		if (bCheckOnly) {
			bFound = _mappedLog.check(logfile.asFile(), logText);
		} else {
			bFound = _mappedLog.add(logfile.asFile(), logText);
		}

		if (isDebugging)
			log.debug("Return " + bFound);
		found.send(0, new BooleanToken(bFound));
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
			_mappedLog.closeAll();
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
			_mappedLog.closeAll();
		} catch (IOException ex) {
			log.error("Error at stop: " + ex);
		}
		if (isDebugging)
			log.debug("stop end");
	}

	private MappedLog _mappedLog;

	// apache commons log for the source code logging.
	private static final Log log = LogFactory.getLog(MappedLogger.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}