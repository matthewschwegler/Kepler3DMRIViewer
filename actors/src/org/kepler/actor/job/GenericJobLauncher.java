/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2014-06-13 11:48:24 -0700 (Fri, 13 Jun 2014) $' 
 * '$Revision: 32764 $'
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationNamespace;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.job.Job;
import org.kepler.job.JobException;
import org.kepler.job.JobFactory;
import org.kepler.job.JobManagerFactory;
import org.kepler.job.JobStatusCode;
import org.kepler.job.TaskParallelJobStatusInfo;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.style.TextStyle;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

/**
 * A generic job launcher actor that can launch a job using PBS, NCCS, Condor,
 * Loadleveler, SGE, Moab or LSF, and wait till a user specified status.
 * <p>
 * JobLauncher actor is based on code from Norbert Podhorszki's JobCreator, JobManager,
 * JobStatus, and JobSubmitter actors. It uses JobLauncher.properties to find
 * the list of supported job schedulers and the corresponding support class.
 * 
 * Additionally it can support multi task jobs
 * @author Frankie Kwok, Chandrika Sivaramakrishnan, Jared Chase
 * @version $Id: GenericJobLauncher.java 32764 2014-06-13 18:48:24Z jianwu $
 */
@SuppressWarnings("serial")
public class GenericJobLauncher extends TypedAtomicActor {
	public GenericJobLauncher(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		/** Job creator parameter and port */
		// target selects the machine where the jobmanager is running
		target = new PortParameter(this, "target", new StringToken(
				"[local | [user]@host]"));
		new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);
		target.setStringMode(true);

		// submission file parameter & port
		cmdFile = new PortParameter(this, "cmdFile", new StringToken(
				"/path/to/job.submit"));
		cmdFile.setTypeEquals(BaseType.STRING);
		cmdFile.getPort().setTypeEquals(BaseType.STRING);
		new Parameter(cmdFile.getPort(), "_showName", BooleanToken.TRUE);
		cmdFile.setStringMode(true);

		// local/remote submission file flag parameter
		cmdFileLocal = new Parameter(this, "cmdFileLocal", BooleanToken.TRUE);
		cmdFileLocal.setTypeEquals(BaseType.BOOLEAN);

		// executable file's name parameter & port
		executable = new StringParameter(this, "executable file");
		executable.setVisibility(Settable.EXPERT);

		// working dir name parameter & port
		workdir = new PortParameter(this, "workdir", new StringToken(
				".kepler-hpcc"));
		new Parameter(workdir.getPort(), "_showName", BooleanToken.TRUE);
		workdir.setStringMode(true);
		
		//if true - actor doesn't create a unique sub directory, assumes that the
		//user given workdir is unique and to be used as such.
		usegivendir = new Parameter(this, "use given workdir",
				new BooleanToken(false));
		usegivendir.setTypeEquals(BaseType.BOOLEAN);
		usegivendir.setVisibility(Settable.EXPERT);

		// list of input files' names parameter & port
		inputfiles = new PortParameter(this, "inputfiles", new ArrayToken(
				BaseType.STRING));
		new Parameter(inputfiles.getPort(), "_showName", BooleanToken.TRUE);

		// list of remote input files' names parameter & port
		remotefiles = new PortParameter(this, "remotefiles", new ArrayToken(
				BaseType.STRING));
		new Parameter(remotefiles.getPort(), "_showName", BooleanToken.TRUE);

		/** Job Manager parameter and port */
		// jobManager denotes the name of the actual job manager
		scheduler = new PortParameter(this, "scheduler", new StringToken(
				"SGE"));
		scheduler.setStringMode(true);
		cp = ConfigurationManager.getInstance()
				.getProperty(ConfigurationManager.getModule("actors"),
						new ConfigurationNamespace("JobLauncher"));
		properties = cp.getProperties("value", true);
		for (ConfigurationProperty property : properties) {
			scheduler.addChoice(property.getValue());
		}
		
		
		// jobManager.setStringMode(true); // string mode (no "s, but no
		// variables as well!
		new Parameter(scheduler.getPort(), "_showName", BooleanToken.TRUE);

		// flag to set if you want the actor to stage the default fork script
		defaultForkScript = new Parameter(this, "Use default fork script",
				new BooleanToken(false));
		defaultForkScript.setTypeEquals(BaseType.BOOLEAN);
		defaultForkScript.setVisibility(Settable.EXPERT);

		// binPath is the full path to the jobmanager commands on the target
		// machine
		binPath = new StringParameter(this, "binary path");
		binPath.setVisibility(Settable.EXPERT);

		// jobSubmitOptions are optional parameters to pass to
		// submitting a job
		jobSubmitOptions = new StringParameter(this, "job submit options");
		jobSubmitOptions.setVisibility(Settable.EXPERT);

		// numTasks is the number of tasks for this job
		numTasks = new Parameter(this, "numTasks");
		numTasks.setExpression("0");

		/** Job Status parameter and port */
		
		waitUntil = new Parameter(this, "Wait Until Status", new StringToken(
		"ANY"));
		waitUntil.setStringMode(true);
		waitUntil.addChoice("ANY");
		for (JobStatusCode code : JobStatusCode.values()) {
			waitUntil.addChoice(code.toString());
		}

		sleepWhileWaiting = new Parameter(this, "Wait Until Sleep (seconds)",
		new IntToken(_sleepWhileWaitingVal));
		sleepWhileWaiting.setTypeEquals(BaseType.INT);
		
		// Output: jobID of the submitted job
		jobOut = new TypedIOPort(this, "jobOut", false, true);
		jobOut.setTypeEquals(BaseType.OBJECT);
		new Parameter(jobOut, "_showName", BooleanToken.TRUE);

		// Output: log
		logPort = new TypedIOPort(this, "logPort", false, true);
		logPort.setTypeEquals(BaseType.STRING);
		new Parameter(logPort, "_showName", BooleanToken.TRUE);

		//Output: success
		success = new TypedIOPort(this, "success", false, true);
		success.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(success, "_showName", BooleanToken.TRUE);
		
		cmdText = new PortParameter(this, "cmdText");
		cmdText.setTypeEquals(BaseType.STRING);
		cmdText.setStringMode(true);
		new TextStyle(cmdText, "_style");
		cmdText.getPort().setTypeEquals(BaseType.STRING);
		new Parameter(cmdText.getPort(), "_showName");
		
		dependentJob = new TypedIOPort(this, "dependentJob", true, false);
		dependentJob.setTypeEquals(BaseType.OBJECT);
		dependentJob.setMultiport(true);
		new Parameter(dependentJob, "_showName");
		
		jobID = new TypedIOPort(this, "jobID", false, true);
		jobID.setTypeEquals(BaseType.STRING);
		new Parameter(jobID, "_showName", BooleanToken.TRUE);
		
	}

	/***************************************************************************
	 * ports and parameters
	 */

	/**
	 * The submit file to be used at job submission. Absolute (or relative to
	 * current dir of Java) file path should be provided. The job file must
	 * be provided here, or the contents can be specified in cmdText.
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
	public Parameter executable;

	/**
	 * The working directory in which the actual job submission command will be
	 * executed (on the remote machine if the job manager is a remote
	 * jobmanager).
	 * 
	 * <p>
	 * It should be an absolute path, or a relative one. In the latter case on
	 * remote machine, the directory path will be relative to the user's home
	 * directory (coming from the use of ssh).
	 * </p>
	 * By default, a new unique sub directory is created within this workdir
	 * based on the job id created by kepler. Job is run from this sub
	 * directory. This can be overwritten by setting the parameter "use given
	 * workdir"
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public PortParameter workdir;

	/**
	 * By default, Kepler creates a unique sub directory within workdir based on
	 * the the job id it creates for the job. Job is run from this sub
	 * directory. Set this flag to true if you want job to be run directly from
	 * workdir instead of a subdir
	 * 
	 * <p>
	 * This parameter is read each time in fire().
	 * </p>
	 */
	public Parameter usegivendir;

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
	 * The name of the jobmanager to be used It should be a name, for which a
	 * supporter class exist as <i>org.kepler.job.JobSupport<jobManager>.class
	 * 
	 * This parameter is read each time in fire().
	 */
	public PortParameter scheduler;

	/**
	 * Boolean flag to indicate if the default fork script should be staged. If
	 * bin path is provided the default script is uploaded to bin path, else it
	 * is uploaded to the working directory
	 */
	public Parameter defaultForkScript;

	/**
	 * The machine to be used at job submission. It should be null, "" or
	 * "local" for the local machine or [user@]host to denote a remote machine
	 * accessible with ssh.
	 * 
	 * This parameter is read each time in fire().
	 */
	public PortParameter target;

	/**
	 * The path to the job manager commands on the target machines. Commands are
	 * constructed as <i>binPath/command</i> and they should be executable this
	 * way. This parameter is read each time in fire().
	 */
	public Parameter binPath;

	/**
	 * The number of tasks for the job - used in a task parallel job
	 */
	public Parameter numTasks;

	 /** 
	 * The Options of the job submission. Such as "-o /u/joboutput/ -j y -l
	 * h_rt=24:00:00" for SGE job scheduler. Its default value is empty.
	 */
	public Parameter jobSubmitOptions;

	/**
	 * The job is passed on in this actor. This token can be used (delaying it
	 * with a Sleep actor) to ask its Status again and again until the job is
	 * finished or aborted. This port is an output port of type Object.
	 */
	public TypedIOPort jobOut;
	
	/**
	 * The real job ID generated from the job scheduler.
	 */
	public TypedIOPort jobID;

	/**
	 * Logging information of job status query. Useful to inform user about
	 * problems at unsuccessful status query but it also prints out job status
	 * and job id on successful query. This port is an output port of type
	 * String. The name of port on canvas is 'log'
	 */
	public TypedIOPort logPort;

	/**
	 * Wait until the job has a reached specific status. The available status'
	 * that can be reached are: any, wait, running, not in queue, and error.
	 */
	public Parameter waitUntil;

	/**
	 * Amount of time (in seconds) to sleep between checking job status.
	 */
	public Parameter sleepWhileWaiting;

	/**
	 * The exit code of the command. If the exit code is 0, the command was
	 * performed successfully. If the exit code is anything other than a 0, an
	 * error occured.
	 */
	// public TypedIOPort exitcode;
	
	/**
	 * boolean flag to indicate if job launch was successful
	 */
	public TypedIOPort success;
	
	
	/** The text of the job specification. The job specification must either
	 *  be provided in this parameter or the file name in cmdFile. 
	 */
	public PortParameter cmdText;
	
	/** One or more jobs that must successfully complete before this job can run. */
	public TypedIOPort dependentJob;

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 *                Not thrown.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		/* Job creation by processing port parameters */
		System.out.println("KEPLER HOME IS " + System.getProperty("KEPLER"));
		System.out.println("USER DIR IS "+ System.getProperty("user.dir"));
		cmdFile.update();
		cmdText.update();
		workdir.update();
		inputfiles.update();
		remotefiles.update();
		
		String strLog = null;
		String strExecutable = null;
		String strJobOptions = null;
		String strBinPath = null;
		boolean bUseGivenDir = false;
		boolean bDefaultFork = false;
		
		// read any dependent jobs
		Job[] dependentJobArray = null;
		if(dependentJob.numberOfSources() > 0) {
		    dependentJobArray = new Job[dependentJob.numberOfSources()];
		    for(int i = 0; i < dependentJob.numberOfSources(); i++) {
		        dependentJobArray[i] = (Job) ((ObjectToken)dependentJob.get(i)).getValue();
		    }
		}

		if (this.getAttribute("_expertMode") != null) {
			Token temp = null;
			temp = (executable != null) ? executable.getToken() : null;
			strExecutable = (temp != null) ? ((StringToken) temp).stringValue()
					.trim() : null;
			//back compatibility, remove the double quotes at the very beginning and at the very last.
			strExecutable = strExecutable.replaceAll("^\"|\"$", "");

			temp = (binPath != null) ? binPath.getToken() : null;
			strBinPath = (temp != null) ? ((StringToken) temp).stringValue()
					.trim() : null;
			//back compatibility, remove the double quotes at the very beginning and at the very last.
			strBinPath = strBinPath.replaceAll("^\"|\"$", "");

			temp = (jobSubmitOptions != null) ? jobSubmitOptions.getToken()
					: null;
			strJobOptions = (temp != null) ? ((StringToken) temp).stringValue()
					.trim() : null;
			//back compatibility, remove the double quotes at the very beginning and at the very last.
			strJobOptions = strJobOptions.replaceAll("^\"|\"$", "");			

			bUseGivenDir = ((BooleanToken) usegivendir.getToken())
					.booleanValue();
			bDefaultFork = ((BooleanToken) defaultForkScript.getToken())
					.booleanValue();
		}

		scheduler.update();
		target.update();

	    String strCmdFile = null;
	    String strCmdText = null;
		StringToken token = (StringToken) cmdFile.getToken();
		if(token != null) {
		    strCmdFile = token.stringValue().trim();
			//back compatibility, remove the double quotes at the very beginning and at the very last.
		    strCmdFile = strCmdFile.replaceAll("^\"|\"$", "");
		    if(strCmdFile.isEmpty()) {
		        strCmdFile = null;
		    }
		}

		token = (StringToken) cmdText.getToken();
		if(token != null) {
		    strCmdText = token.stringValue().trim();
		    if(strCmdText.isEmpty()) {
		        strCmdText = null;
		    }
		}
		
		boolean bCmdFileLocal = ((BooleanToken) cmdFileLocal.getToken())
				.booleanValue();

		StringToken temp = ((StringToken) workdir.getToken());
		String strWorkdir = temp==null? null :temp.stringValue().trim();
		//back compatibility, remove the double quotes at the very beginning and at the very last.
		strWorkdir = strWorkdir.replaceAll("^\"|\"$", "");

		temp = ((StringToken) scheduler.getToken());
		strScheduler =  temp==null? null :temp.stringValue().trim();
		//back compatibility, remove the double quotes at the very beginning and at the very last.
		strScheduler = strScheduler.replaceAll("^\"|\"$", "");
		
		temp = ((StringToken) target.getToken());
		strTarget = temp==null? null :temp.stringValue().trim();
		//back compatibility, remove the double quotes at the very beginning and at the very last.
		strTarget = strTarget.replaceAll("^\"|\"$", "");
		
		// Process the inputfiles parameter.
		ArrayToken inputTokens = (ArrayToken) inputfiles.getToken();
		String[] inputArray = null;

		try {
			if (inputTokens.length() >= 1) {
				int i;

				ArrayList<String> iFiles = new ArrayList<String>();

				for (i = 0; i < inputTokens.length(); i++) {
					boolean fileFound = false;
					File pattern = new File(((StringToken) inputTokens
							.getElement(i)).stringValue().trim());
					String[] contents = (pattern.getParent() != null) ? new File(
							pattern.getParent()).list()
							: null;
					String fileName = pattern.getName();

					if (!fileName.equals("")) {
						fileName = fileName.replaceAll("[*]", ".*").replaceAll(
								"[?]", ".?").replaceAll("[+]", ".+");

						Pattern p = Pattern.compile(fileName);
						if (contents != null) {
							for (int index = 0; index < contents.length; index++) {
								Matcher m = p.matcher(contents[index].trim());
								if (m.matches()) {
									iFiles
											.add(pattern.getParent()
													+ System
															.getProperty("file.separator")
													+ contents[index]);
									fileFound = true;
								}
							}
						}
						if (!fileFound) {
							throw new JobException(
									"No matching file found for "
											+ pattern.toString());
						}
					}
				}

				if (iFiles.size() != 0) {
					inputArray = new String[iFiles.size()];
					iFiles.toArray(inputArray);
				}
			}
		} catch (JobException ex) {
			log.error(ex);
			// ex.printStackTrace();
			throw new IllegalActionException(ex.toString());
		}

		// Process the remotefiles parameter.
		ArrayToken remoteTokens = (ArrayToken) remotefiles.getToken();
		String[] remoteArray = null;
		if (remoteTokens.length() >= 1) {
			remoteArray = new String[remoteTokens.length()];
			int i;
			for (i = 0; i < remoteTokens.length(); i++) {
				remoteArray[i] = (((StringToken) remoteTokens.getElement(i))
						.stringValue().trim());
			}
			// process empty array
			if (i == 0 || remoteArray[0] == "") {
				remoteArray = null;
			}
		}

		// create job
		String strJobID = JobFactory.create();
		Job _job = JobFactory.get(strJobID);

		// set the dependencies, if any
		if(dependentJobArray != null) {
		    _job.setDependentJobs(dependentJobArray);
		}
		
		try {
			// set _job's executable, working dir and input files
			if (strExecutable != null && strExecutable.trim().length() > 0) {
				_job.setExecutable(strExecutable, true, strJobOptions);
			}

			if (strWorkdir != null && strWorkdir.trim().length() > 0) {
				_job.setWorkdir(strWorkdir, !bUseGivenDir);
			} else {
				if (bUseGivenDir) {
					throw new JobException(
							"The flag 'use given workdir' is set to true. " +
							"Please provide a valid working directory. \n " + 
							"Or you could uncheck the flag and let the actor create a " +
							"unique working directory for your job");
				}
				if (strTarget == null || strTarget.trim().equals("")
						|| strTarget.equals("local")|| strTarget.equals("localhost")) {
					//If submitting to localhost, find home dir using java
					strWorkdir = System.getProperty("user.home");
					if ( System.getProperty("os.name").toLowerCase().indexOf("win") >= 0 ) {
						strWorkdir = System.getenv().get("HOMEPATH");
					}
				}else{
					strWorkdir = "$HOME";
				}
				_job.setWorkdir(strWorkdir);
			}

			// make sure both cmdFile and cmdText were not used
			if(strCmdText != null && strCmdFile != null) {
			    throw new IllegalActionException(this, "Do not specify both cmdText and cmdFile.");
			}
			
			// make sure at least one of cmdFile and cmdText were used
			if(strCmdText == null && strCmdFile == null) {
			    throw new IllegalActionException(this, "Must specify either cmdText or cmdFile.");
			}
			
			// if the commands were specified as text, write to a temporary file
			if (strCmdText != null) {
			    File file;
                try {
                    file = File.createTempFile("job", null);
                } catch (IOException e) {
                    throw new IllegalActionException(this, e,
                            "Error creating temporary file for cmd text.");
                }
			    Writer writer = null;
			    try {
			        writer = new FileWriter(file);
			        writer.write(strCmdText);
			    } catch(IOException e) {
			        throw new IllegalActionException(this, e, "Error write cmd text to file.");
			    } finally {
			        if(writer != null) {
			            try {
                            writer.close();
                        } catch (IOException e) {
                            throw new IllegalActionException(this, e, "Error closing cmd text file.");
                        }
			        }
			    }
			    strCmdFile = file.getAbsolutePath();
			    bCmdFileLocal = true; //if commands are text, bCmdFileLocal should be true so the file will be transfered to the target cluster.
			}
			
			_job.setSubmitFile(strCmdFile, bCmdFileLocal);
			
			if(bDefaultFork && "Fork".equalsIgnoreCase(strScheduler)){
				File resourcesDir = ConfigurationManager.getModule("job").getResourcesDir();
				File binFile = new File(resourcesDir,"jmgr-fork.sh");
				if(!binFile.exists()){
					throw new JobException("Unable to locate default fork script - "
							+ binFile.getAbsolutePath() + ". Please copy fork script manually.");
				}
				_job.setBinFile(binFile.getAbsolutePath(), true);
				//Set the bin path explicitly if it is not already set
				//This is required because jmgr-fork.sh fails with command not found. 
				//It works only if there is an absolute or relative path prefix to "jmgr-fork.sh"
				if(strBinPath == null || strBinPath.trim().equals("")){
					strBinPath = _job.getWorkdirPath(); //setWorkdir was already called 
													//so, this method should return the right path
				}
			}

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
			throw new IllegalActionException(this, ex, "Error creating job.");
		}

		/* Job Manager processing */
		
		org.kepler.job.JobManager myJmgr = null;

		try {
			if (strScheduler==null || strScheduler.equals("")) {
				throw new JobException(
						"Please provide a valid input for the port/parameter scheduler.");
			}
			// Read properties file.
			// String filePath = System.getProperty("KEPLER");
			// filePath = filePath + "/common/configs/ptolemy/configs"
			// + "/kepler/JobLauncher.properties";

			// File myFile = new File(filePath);
			// Properties properties = new Properties();
			// properties.load(new FileInputStream(filePath));
			// String jobsSupported = properties.getProperty(strScheduler
			// .toLowerCase());

//			ConfigurationProperty cp = ConfigurationManager.getInstance()
//					.getProperty(ConfigurationManager.getModule("actors"),
//							new ConfigurationNamespace("JobLauncher"));
			properties = cp.findProperties("name",
					strScheduler.toLowerCase(), true);
			String jobsSupported = null;
			if(properties.size() != 0){
				ConfigurationProperty prop = properties.get(0);
				jobsSupported = prop.getProperty("value").getValue();
			}
			if (jobsSupported != null) {
				strScheduler = jobsSupported;
			} else {
				throw new JobException("Job Scheduler " + strScheduler
						+ " is not supported.");
			}

			// Create a JobManager object or get it if it was already created
			if (isDebugging)
				log.debug("Create/get JobManager object. Name = "
						+ strScheduler + "; target = " + strTarget
						+ "; binPath = " + strBinPath);
			myJmgr = JobManagerFactory.get(strScheduler, strTarget, strBinPath);

			
			// Note that myJmgr.getID can give back a String reference to the
			// object that can be used with JobManagerFactory.get
		} catch (JobException ex) {
			log.error("Job manager object could not be created. " + ex);
			myJmgr = null;
			JobFactory.remove(strJobID);
			strJobID = "";
			throw new IllegalActionException("JobManager Error: "
					+ ex.toString());
		}

		/* Job Submission */
		boolean bSucc = false;
		try {
			if (_job == null) {
				throw new JobException("JobSubmitter: incoming Job is null");
			}

			if (isDebugging) {
				log.debug("JobSubmit: submit job " + _job.getJobID() + "...");
			}
			String realJobID;

                        int numTasksVal = ((IntToken)numTasks.getToken()).intValue();
			if(numTasksVal > 0) {
				_job.status = new TaskParallelJobStatusInfo();
				_job.setNumTasks(numTasksVal);
			}
			if(bUseGivenDir){
				//do not overwrite existing folder. create if not present
				realJobID = _job.submit(myJmgr, false, strJobOptions);
			}else {
				realJobID = _job.submit(myJmgr, true, strJobOptions);
			}
			strLog = new String("JobSubmitter: Job " + _job.getJobID()
					+ " is submitted, it's real jobID is: " + realJobID);
			log.info(strLog);
			jobID.send(0, new StringToken(realJobID));
			bSucc = true;
		} catch (JobException ex) {
			log.error(ex);
			strLog = "JobSubmitter Error: " + ex.toString();
			success.send(0, new BooleanToken(bSucc));
			logPort.send(0, new StringToken(strLog));
			return;
		} catch (Exception ex) {
			log.error(ex);
			strLog = "JobSubmitter Error: " + ex.toString();
			success.send(0, new BooleanToken(bSucc));
			logPort.send(0, new StringToken(strLog));
			return;
		}

		/* Job Status Checking */
		JobStatusCode jobStatusCode;

		try {

	      do {
	        //System.out.println("BEFORE CHECK JOB STATUS");
	        //jobStatusCode = _checkStatus(_job);
	        //System.out.println("AFTER CHECK JOB STATUS " + jobStatusCode);
	        // while (_waitUntilCode != null && _waitUntilCode != jobStatusCode)
	        // {
	        //Loop if there is no match and job status is NOT Error or NotInQueue.
	        //Second check is necessary to avoid infinite loop in case where job
	        //never gets to the user requested state or if the state goes undetected
	        //(say during sleep between poll).
	        Long time = 1000L * _sleepWhileWaitingVal;
	        Thread.sleep(time);
	        jobStatusCode = _checkStatus(_job);
	      } while (!matchStatus(jobStatusCode) && jobStatusCode.ordinal()>1);

		} catch (Exception ex) {
			log.error(ex);
			jobStatusCode = JobStatusCode.Error;
			strLog = "JobStatus Error: " + ex.toString();
			bSucc = false;
			success.send(0, new BooleanToken(bSucc));
			logPort.send(0, new StringToken(strLog));
			return;
		}

		if (_job != null) {
			strLog = new String("JobStatus: Status of job " + _job.getJobID()
					+ ": " + jobStatusCode.toString());
			jobOut.send(0, new ObjectToken(_job));
		}
		success.send(0, new BooleanToken(bSucc));
		logPort.send(0, new StringToken(strLog));
	}

	private JobStatusCode _checkStatus(Job job) throws Exception {
		JobStatusCode jobStatusCode = JobStatusCode.Error;
		if (job == null) {
			throw new Exception("JobStatus: Job is null");
		}

		job.status(); // successful query or exception

		jobStatusCode = job.status.statusCode;
		log.info("Status of job " + job.getJobID() + ": "
				+ jobStatusCode.toString());
		return jobStatusCode;
	}

	private boolean matchStatus(JobStatusCode jobStatusCode) {
		String str = jobStatusCode.toString();

      if (_waitUntilCodes.size() == 0 || _waitUntilCodes.contains(str)) {
			return true;
		}
		return false;
	}

	/** React to a change in an attribute. */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == waitUntil) {
			String waitUntilStr = waitUntil.getExpression();
			waitUntilStr = waitUntilStr.trim();
			String[] split = waitUntilStr.split("\\s*,\\s*");
			_waitUntilCodes = new ArrayList<String>(Arrays.asList(split));
			// check validity
			if (_waitUntilCodes.contains("ANY")) {
				_waitUntilCodes.clear();
			} else {
				for (int i = 0; i < _waitUntilCodes.size(); i++) {
					JobStatusCode waitUntilCode = JobStatusCode
							.getFromString(_waitUntilCodes.get(i));
					if (waitUntilCode == null) {
						throw new IllegalActionException(this,
								"Invalid job status type: "
										+ _waitUntilCodes.get(i));
					}
				}
			}
		} else if (attribute == sleepWhileWaiting) {
			if ((IntToken) sleepWhileWaiting.getToken() != null) {
				_sleepWhileWaitingVal = ((IntToken) sleepWhileWaiting
						.getToken()).intValue();
				if (_sleepWhileWaitingVal < 0) {
					throw new IllegalActionException(this,
							"Sleep While Waiting value cannot be negative.");
				}
			}
		} else if (attribute == binPath) {
			//if binPath is changed, we should remove JobManager from the table to force it update.
			if (strScheduler != null && !strScheduler.isEmpty() && strTarget != null && !strTarget.isEmpty())
				JobManagerFactory.instance.removeJmgrFromTable(strScheduler, strTarget);
		} else {
			super.attributeChanged(attribute);
		}
	}

	private static final Log log = LogFactory.getLog(GenericJobLauncher.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	// private JobStatusCode _waitUntilCode = null;
	private ArrayList<String> _waitUntilCodes = new ArrayList<String>();
	private int _sleepWhileWaitingVal = 5;
	private List<ConfigurationProperty> properties;
	private ConfigurationProperty cp;
	private String strScheduler, strTarget;
}
