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
//// JobFileFetcher

/**
 * <p>
 * Get the job file info of a submitted Job
 * </p>
 * 
 * <p>
 * This actor uses the Job class to ask for the the job file info (file path + file name) of a submitted job.
 * </p>
 * 
 * <p>
 * The input should be a previously submitted job. i.e. the output from a
 * JobSubmitter.
 * </p>
 * 
 * <p>
 * The output is the the job file info (file path + file name) of the job:
 * </p>
 * <p>
 * If not such job exists, the result will be also the Error status.
 * </p>
 * 
 * 
 * @author Jianwu Wang
 * @version $Id: JobFileFetcher.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 7
 */
public class JobFileFetcher extends TypedAtomicActor {
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
	public JobFileFetcher(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());
		jobIn = new TypedIOPort(this, "jobIn", true, false);
		jobIn.setTypeEquals(BaseType.OBJECT);
		new Parameter(jobIn, "_showName", BooleanToken.FALSE);

		// Output: job file name of the submitted job
		jobFileName = new TypedIOPort(this, "jobFileName", false, true);
		jobFileName.setTypeEquals(BaseType.STRING);
		new Parameter(jobFileName, "_showName", BooleanToken.TRUE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * A submitted job This port is an output port of type Object.
	 */
	public TypedIOPort jobIn;

	/**
	 */
	public TypedIOPort jobFileName;


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
		ObjectToken jobToken = (ObjectToken) jobIn.get(0);
		Job job = (Job) jobToken.getValue();
		String fileName = job.getSubmitFile();
		if (fileName != null)
			jobFileName.send(0, new StringToken (fileName));
		else
			{
				throw new IllegalActionException("Can't get file Job info, probably the submitted job for input is not correct.");
			}
	}
}