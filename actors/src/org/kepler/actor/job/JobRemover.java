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

package org.kepler.actor.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.job.Job;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// JobRemover

/**
 * <p>
 * Delete Job from queue whether it is running or waiting in queue.
 * </p>
 * 
 * <p>
 * This actor uses the Job class to delete a submitted job.
 * </p>
 * 
 * <p>
 * The input should be a previously submitted job.
 * </p>
 * i.e. the output from a JobSubmitter.
 * 
 * <p>
 * The ouput is boolean indicating whether the removal was successful or not. If
 * not such job exists, the result will be true.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: JobRemover.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class JobRemover extends TypedAtomicActor {
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
	public JobRemover(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());
		job = new TypedIOPort(this, "job", true, false);
		job.setTypeEquals(BaseType.OBJECT);
		new Parameter(job, "_showName", BooleanToken.FALSE);

		// Output: status code
		succ = new TypedIOPort(this, "succ", false, true);
		succ.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(succ, "_showName", BooleanToken.TRUE);
	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * A submitted job This port is an output port of type Object.
	 */
	public TypedIOPort job;

	/**
	 * Output: true if removal succeeded, false otherwise This port is an output
	 * port of type Integer;
	 */
	public TypedIOPort succ;

	/***********************************************************
	 * public methods
	 */

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 *                Not thrown.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		ObjectToken jobToken = (ObjectToken) job.get(0);
		Job _job = (Job) jobToken.getValue();
		boolean _succ = false;

		try {
			if (_job == null)
				throw new Exception("JobStatus: Job is null");

			_succ = _job.deleteFromQueue(); // successful query or exception

		} catch (Exception ex) {
			log.error(ex);
			ex.printStackTrace();
			throw new IllegalActionException("JobRemover Error: " + ex.toString());
		}

		succ.send(0, new BooleanToken(_succ));
	}

	private static final Log log = LogFactory
			.getLog(JobRemover.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}