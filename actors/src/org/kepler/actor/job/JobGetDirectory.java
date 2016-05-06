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
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// JobGetDirectory

/**
 * <p>
 * Get the working directory of a submitted Job
 * </p>
 * 
 * <p>
 * This actor uses the Job class to get the working directory of a submitted
 * job. Since the working directory is created by the JobSubmitter, this is the
 * way to get to know the actual directory name. If you want to use the output
 * files of a job before deleting it, you need to get the working directory
 * first.
 * </p>
 * 
 * <p>
 * The input should be a previously submitted job which has not been removed
 * yet, i.e. the output from a JobSubmitter, never given to a JobRemover.
 * </p>
 * 
 * <p>
 * The ouput is the working directory of type String. If the job is null object
 * this actor throws an IllegalActionException
 * </p>
 * .
 * 
 * <p>
 * Note that if the working directory is not set by the workflow, it gets a
 * default value at job submission. Before submission, this actor returns an
 * empty string. It is advisable to set a working directory at job creation
 * (with JobCreator) or use this actor only after submission.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: JobGetDirectory.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class JobGetDirectory extends TypedAtomicActor {
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
	public JobGetDirectory(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());
		job = new TypedIOPort(this, "job", true, false);
		job.setTypeEquals(BaseType.OBJECT);
		new Parameter(job, "_showName", BooleanToken.FALSE);

		// Output: real JobID as string
		dir = new TypedIOPort(this, "dir", false, true);
		dir.setTypeEquals(BaseType.STRING);
		new Parameter(dir, "_showName", BooleanToken.TRUE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * A submitted job This port is an output port of type Object.
	 */
	public TypedIOPort job;

	/**
	 * Working directory of the job that was assigned and created by the
	 * JobSubmitter when the job was submitted. This port is an output port of
	 * type String.
	 */
	public TypedIOPort dir;

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
		String _dir = "";

		try {
			if (_job == null)
				throw new IllegalActionException("JobGetDirectory: Job is null");

			_dir = _job.getWorkdirPath();
			if (_dir == null) {
				log
						.warn("Working directory of the job "
								+ _job.getJobID()
								+ " is unknown. "
								+ "Probably, you have not set a working directory for it and it has not been "
								+ "submitted yet.");
			}
			if (isDebugging)
				log.debug("Working directory of job " + _job.getJobID() + ": "
						+ _dir);

		} catch (Exception ex) {
			log.error(ex);
			ex.printStackTrace();
			throw new IllegalActionException("JobGetDirectory Error: " + ex.toString());
		}

		dir.send(0, new StringToken(_dir));

	}

	private static final Log log = LogFactory.getLog(JobGetDirectory.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}