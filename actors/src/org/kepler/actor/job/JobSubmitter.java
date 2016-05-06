/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-09-14 15:48:44 -0700 (Fri, 14 Sep 2012) $' 
 * '$Revision: 30678 $'
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
import org.kepler.job.JobException;
import org.kepler.job.JobManager;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// JobSubmitter

/**
 * <p>
 * Submit a job into a jobmanager on the local/remote machine, using external
 * execute command/ssh
 * </p>
 * 
 * <p>
 * This actor uses the Job and JobManager classes that use<br/>
 * - either java.lang.Runtime.exec() to invoke a subprocess - or org.kepler.ssh
 * classes to use ssh/scp to submit a job and ask for the status of a submitted
 * job.
 * </p>
 * 
 * <p>
 * Inputs: a created Job, a JobManager and Submit options. Job can be created by JobCreator.
 * JobManager can be created by JobManager. Submit options are different for different job scheduler. 
 * </p>
 * 
 * <p>
 * Outputs: - <i>jobOut</i> (Object): the job of the submitted job is passed on
 * so that it can be used later to query its status in JobStatus, or destroyed
 * (by ???) - <i>succ</i> (boolean): true means successful submission, false
 * means error - <i>log</i> (String): for log/error message
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: JobSubmitter.java 30678 2012-09-14 22:48:44Z jianwu $
 * @since Ptolemy II 5.0.1
 */
public class JobSubmitter extends TypedAtomicActor {
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
	public JobSubmitter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());

		// job as input
		jobIn = new TypedIOPort(this, "jobIn", true, false);
		jobIn.setTypeEquals(BaseType.OBJECT);
		new Parameter(jobIn, "_showName", BooleanToken.TRUE);

		// jobManager
		jobManager = new TypedIOPort(this, "jobManager", true, false);
		jobManager.setTypeEquals(BaseType.OBJECT);
		new Parameter(jobManager, "_showName", BooleanToken.TRUE);

		// Output: the submitted job (actually the same as jobIn)
		jobOut = new TypedIOPort(this, "jobOut", false, true);
		jobOut.setTypeEquals(BaseType.OBJECT);
		new Parameter(jobOut, "_showName", BooleanToken.TRUE);

		// Output: boolean succ
		succ = new TypedIOPort(this, "succ", false, true);
		succ.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(succ, "_showName", BooleanToken.TRUE);

		// Output: log
		logport = new TypedIOPort(this, "log", false, true);
		logport.setTypeEquals(BaseType.STRING);
		new Parameter(logport, "_showName", BooleanToken.TRUE);
		
		
		// submit options, which are different for different job scheduler. 
		jobSubmitOptions = new PortParameter(this, "jobSubmitOptions", new StringToken(""));
		jobSubmitOptions.setStringMode(true);

		new Parameter(jobSubmitOptions.getPort(), "_showName", BooleanToken.TRUE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * The job to be submitted Such a job can be created by the JobCreator. This
	 * port is an input port of type Object.
	 */
	public TypedIOPort jobIn;

	/**
	 * The selected jobmanager (result of JobManager) This port is an input port
	 * of type Object;
	 */
	public TypedIOPort jobManager;

	/**
	 * The job, which is passed on in this actor for later use (status, destroy,
	 * etc) This port is an output port of type Object.
	 */
	public TypedIOPort jobOut;

	/**
	 * The status of the job submission. True if submission succeeded, false
	 * otherwise. This port is an output port of type Boolean.
	 */
	public TypedIOPort succ;

	/**
	 * Logging information of job submission. (to inform user about problems at
	 * unsuccessful submission). This port is an output port of type String.
	 * It's name on canvas is 'log'
	 */
	public TypedIOPort logport;	
	
	/**
	 * The Options of the job submission. Such as "-o /u/joboutput/ -j y -l h_rt=24:00:00"
	 * for SGE job scheduler. Its default value is empty.
	 */
	public PortParameter jobSubmitOptions;

	/**
	 * The rate parameter for the log port.
	 */
	public Parameter log_tokenProductionRate;

	/***********************************************************
	 * public methods
	 */

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 *                Not thrown
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		jobSubmitOptions.update();

		ObjectToken jobToken = (ObjectToken) jobIn.get(0);
		Job job = (Job) jobToken.getValue();
		JobManager jmgr = (JobManager) ((ObjectToken) jobManager.get(0))
				.getValue();
		boolean bSucc = false;
		String strLog = null;
		String strJobOptions = ((StringToken) jobSubmitOptions.getToken()).stringValue();
		//back compatibility, remove the double quotes at the very beginning and at the very last.
		strJobOptions  = strJobOptions.replaceAll("^\"|\"$", "");

		try {
			if (job == null)
				throw new Exception("JobSubmitter: incoming Job is null");

			if (jmgr == null)
				throw new Exception("JobSubmitter: JobManager is null");

			if (isDebugging)
				log.debug("JobSubmit: submit job " + job.getJobID() + "...");

			String realJobID = job.submit(jmgr, true, strJobOptions);

			strLog = new String("JobSubmitter: Job " + job.getJobID()
					+ " is submitted, it's real jobID is: " + realJobID);
			log.info(strLog);

			bSucc = true;

		} catch (JobException ex) {
			log.error(ex);
			strLog = "JobSubmitter Error: " + ex.toString();
			ex.printStackTrace();
			throw new IllegalActionException("JobSubmitter Error: " + ex.toString());
		} catch (Exception ex) {
			log.error(ex);
			strLog = "JobSubmitter Error: " + ex.toString();
			ex.printStackTrace();
			throw new IllegalActionException("JobSubmitter Error: " + ex.toString());
		}

		succ.send(0, new BooleanToken(bSucc));
		logport.send(0, new StringToken(strLog));
		jobOut.send(0, jobToken);
	}

	private static final Log log = LogFactory.getLog(JobSubmitter.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}