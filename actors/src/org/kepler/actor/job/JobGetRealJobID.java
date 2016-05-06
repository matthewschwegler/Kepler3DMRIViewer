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
//// JobGetRealJobID

/**
 * <p>
 * Get the real (given back the jobManager as a handle) JobID of a submitted Job
 * </p>
 * 
 * <p>
 * This actor uses the Job class get the jobID of a submitted job. It is useful
 * only if you want to do weird things based on the real jobID of your submitted
 * job (e.g edit files or go to remote site and commit something nasty there).
 * </p>
 * 
 * <p>
 * The input should be a previously submitted job. i.e. the output from a
 * JobSubmitter.
 * </p>
 * <p>
 * The ouput is the jobID of type String. If the job is null object or it was
 * not submitted, this actor throws an IllegalActionException.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: JobGetRealJobID.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class JobGetRealJobID extends TypedAtomicActor {
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
	public JobGetRealJobID(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());
		job = new TypedIOPort(this, "job", true, false);
		job.setTypeEquals(BaseType.OBJECT);
		new Parameter(job, "_showName", BooleanToken.FALSE);

		// Output: real JobID as string
		jobID = new TypedIOPort(this, "jobID", false, true);
		jobID.setTypeEquals(BaseType.STRING);
		new Parameter(jobID, "_showName", BooleanToken.TRUE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * A submitted job This port is an output port of type Object.
	 */
	public TypedIOPort job;

	/**
	 * Real jobID of the job that came from the job manager as a handle when the
	 * job was submitted. Useful only if you want to do weird things based on
	 * the real jobID of your submitted job (e.g edit files or go to remote site
	 * and commit something nasty there). This port is an output port of type
	 * String.
	 */
	public TypedIOPort jobID;

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
		String _jobID = "";

		try {
			if (_job == null)
				throw new Exception("JobGetRealJobID: Job is null");

			if (_job.status == null)
				throw new Exception(
						"JobGetRealJobID: This job was not submitted or has been lost somehow in the meantime.");

			_jobID = _job.status.jobID;
			if (isDebugging)
				log.debug("Real ID of job " + _job.getJobID() + ": " + _jobID);

		} catch (Exception ex) {
			log.error(ex);
			ex.printStackTrace();
			throw new IllegalActionException("JobGetRealJobID Error: " + ex.toString());
		}

		jobID.send(0, new StringToken(_jobID));

	}

	private static final Log log = LogFactory.getLog(JobGetRealJobID.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}