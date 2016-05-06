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
import org.kepler.job.JobFactory;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// JobCreator

/**
 * <p>
 * Create a job. It does not execute it!
 * </p>
 * <p>
 * This actor uses the Job and JobFactory classes to create a job that can be
 * submitted with JobSubmitter.
 * </p>
 * 
 * <p>
 * The cmdFile should be the path to the job submission file that you have
 * created before. This is jobmanager specific! It can be empty, if you want the
 * JobManager to create the submission file for the (later) selected JobManager.
 * Note: this capability of the job managers classes is not implemented yet, so
 * you have to feed here the job submission file (prepared in advance). The file
 * can either local or remote. Indicate in cmdFileLocal which case holds.
 * </p>
 * 
 * <p>
 * Executable is the path to the executable if it is located on this local
 * machine. It will be staged to the remote site (into the job's working
 * directory) before job submission. If the executable is located on the remote
 * site, do not give it here, and use its absolute path in your submission file.
 * </p>
 * 
 * <p>
 * The working directory is the path to the remote working directory in which
 * you want the job's files to be created. It will be expanded with the job's
 * unique id assigned by the JobFactory.
 * </p>
 * 
 * <p>
 * The array of inputfiles is the list of (local) inputfiles to be staged to the
 * remote site before job submission.
 * </p>
 * 
 * <p>
 * The array of remotefiles is the list of remote input files that are to be
 * copied into the job's working directory before job submission.
 * </p>
 * 
 * <p>
 * The output is the created job (of type Object) that can be used later to
 * submit the job in JobSubmitter and query its status in JobStatus actor. It is
 * null if job creation failed.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: JobCreator.java 30678 2012-09-14 22:48:44Z jianwu $
 * @since Ptolemy II 5.0.1
 */
public class JobCreator extends TypedAtomicActor {
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
	public JobCreator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());

		// submission file parameter & port
		cmdFile = new PortParameter(this, "cmdFile", new StringToken(
				"/path/to/job.submit"));
		new Parameter(cmdFile.getPort(), "_showName", BooleanToken.TRUE);
		cmdFile.setStringMode(true);

		// local/remote submission file flag parameter
		cmdFileLocal = new Parameter(this, "cmdFileLocal", new BooleanToken(
				true));
		cmdFileLocal.setTypeEquals(BaseType.BOOLEAN);

		// executable file's name parameter & port
		executable = new PortParameter(this, "executable", new StringToken(""));
		new Parameter(executable.getPort(), "_showName", BooleanToken.TRUE);
		executable.setStringMode(true);

		// working dir name parameter & port
		workdir = new PortParameter(this, "workdir", new StringToken(
				".kepler-hpcc"));
		workdir.setStringMode(true);

		new Parameter(workdir.getPort(), "_showName", BooleanToken.TRUE);

		// list of input files' names parameter & port
		inputfiles = new PortParameter(this, "inputfiles", new ArrayToken(
				BaseType.STRING));

		new Parameter(inputfiles.getPort(), "_showName", BooleanToken.TRUE);

		// list of remote input files' names parameter & port
		remotefiles = new PortParameter(this, "remotefiles", new ArrayToken(
				BaseType.STRING));

		new Parameter(remotefiles.getPort(), "_showName", BooleanToken.TRUE);

		// Output: job object reference to the submitted job
		job = new TypedIOPort(this, "job", false, true);
		job.setTypeEquals(BaseType.OBJECT);
		new Parameter(job, "_showName", BooleanToken.FALSE);
	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * The submit file to be used at job submission. Absolute (or relative to
	 * current dir of Java) file path should be provided. Currently you have to
	 * prepare your submit file.
	 * 
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public PortParameter cmdFile;

	/**
	 * Specifying whether the cmdFile is locally stored or on the remote target.
	 * 
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public Parameter cmdFileLocal;

	/**
	 * The executable file to be used at job submission. Absolute path names, or
	 * relative to current dir of the running java virtual machine, should be
	 * provided. If it is "" then it is considered to be already at the remote
	 * site, otherwise the actor will look for it locally and stage it to the
	 * <i>workdir</i> before job submission.
	 * 
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public PortParameter executable;

	/**
	 * The working directory in which the actual job submission command will be
	 * executed (on the remote machine if the job manager is a remote
	 * jobmanager).
	 * 
	 * <p>
	 * It should be an absolute path, or a relative one. In the latter case on
	 * remote machine, the directory path will be relative to the user's home
	 * directory (coming from the use of ssh)
	 * </p>
	 * 
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public PortParameter workdir;

	/**
	 * The string array of inputfiles. Absolute path names, or relative to
	 * current dir of the running java virtual machine, should be provided.
	 * 
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public PortParameter inputfiles;

	/**
	 * The string array of remote input files. Absolute path names, or relative
	 * to the user home dir on the remote host should be provided.
	 * 
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public PortParameter remotefiles;

	/**
	 * The job object. It will be null if the job submission fails.
	 */
	public TypedIOPort job;

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

		cmdFile.update();
		executable.update();
		workdir.update();
		inputfiles.update();
		remotefiles.update();

		String strCmdFile = ((StringToken) cmdFile.getToken()).stringValue();
		boolean bCmdFileLocal = ((BooleanToken) cmdFileLocal.getToken())
				.booleanValue();

		String strExecutable = ((StringToken) executable.getToken())
				.stringValue();
		String strWorkdir = ((StringToken) workdir.getToken()).stringValue();
		
		//back compatibility, remove the double quotes at the very beginning and at the very last.
		strWorkdir  = strWorkdir.replaceAll("^\"|\"$", "");
		strExecutable  = strExecutable.replaceAll("^\"|\"$", "");
		strCmdFile  = strCmdFile.replaceAll("^\"|\"$", "");

		// Process the inputfiles parameter.
		ArrayToken inputTokens = (ArrayToken) inputfiles.getToken();
		String[] inputArray = null;
		if (inputTokens.length() >= 1) {
			inputArray = new String[inputTokens.length()];
			int i;
			for (i = 0; i < inputTokens.length(); i++) {
				inputArray[i] = (((StringToken) inputTokens.getElement(i))
						.stringValue());
			}
			// process empty array
			if (i == 0 || inputArray[0] == "")
				inputArray = null;
		}

		// Process the remotefiles parameter.
		ArrayToken remoteTokens = (ArrayToken) remotefiles.getToken();
		String[] remoteArray = null;
		if (remoteTokens.length() >= 1) {
			remoteArray = new String[remoteTokens.length()];
			int i;
			for (i = 0; i < remoteTokens.length(); i++) {
				remoteArray[i] = (((StringToken) remoteTokens.getElement(i))
						.stringValue());
			}
			// process empty array
			if (i == 0 || remoteArray[0] == "")
				remoteArray = null;
		}

		// create job
		String strJobID = JobFactory.create();
		Job _job = JobFactory.get(strJobID);

		try {
			// set _job's executable, working dir and input files
			if (strExecutable != null && strExecutable.trim().length() > 0)
				_job.setExecutable(strExecutable, true, "");

			if (strWorkdir != null && strWorkdir.trim().length() > 0)
				_job.setWorkdir(strWorkdir);

			if (strCmdFile != null && strCmdFile.trim().length() > 0)
				_job.setSubmitFile(strCmdFile, bCmdFileLocal);

			if (inputArray != null) {
				for (int i = 0; i < inputArray.length; i++) {
					if (inputArray[i] != null
							&& inputArray[i].trim().length() > 0)
						_job.setInputFile(inputArray[i], true);
				}
			}

			if (remoteArray != null) {
				for (int i = 0; i < remoteArray.length; i++) {
					if (remoteArray[i] != null
							&& remoteArray[i].trim().length() > 0)
						_job.setInputFile(remoteArray[i], false);
				}
			}

		} catch (JobException ex) {
			log.error(ex);
			JobFactory.remove(strJobID);
			strJobID = "";
			_job = null;
			ex.printStackTrace();
			throw new IllegalActionException("JobCreator Error: " + ex.toString());
		}

		job.send(0, new ObjectToken(_job));
	}

	private static final Log log = LogFactory
			.getLog(JobCreator.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}