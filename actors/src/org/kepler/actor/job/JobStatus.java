/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: chandrika $'
 * '$Date: 2011-01-03 19:01:07 -0800 (Mon, 03 Jan 2011) $' 
 * '$Revision: 26613 $'
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.Module;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.job.Job;
import org.kepler.job.JobStatusCode;
import org.kepler.job.JobStatusInfo;
import org.kepler.job.TaskParallelJobStatusInfo;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

//////////////////////////////////////////////////////////////////////////
//// JobStatus

/**
 * <p>
 * Check the status of a Job
 * </p>
 * 
 * <p>
 * This actor uses the Job class to ask for the status of a submitted job.
 * </p>
 * 
 * <p>
 * The input should be a previously submitted job. i.e. the output from a
 * JobSubmitter. When the job contains more than one task also provide the
 * number of tasks using the numTasks parameter
 * </p>
 * <p> 
 * Optional inputs -
 * 1. you could specify the actor to wait for a specific status - for example Running
 * 2. or wait till one of many status - for example Error,NotInQueue
 * 3. Or you could ask the actor to all status changes as and when it is detected.
 * </p>
 * 
 * <p>
 * The output is the status code of the job:
 * </p>
 * <ul>
 * <li>0: Error: some error occured during the execution of the actor</li>
 * <li>1: NotInQueue: no such job in the queue, i.e. never was or already gone</li>
 * <li>2: Wait: the job is in the queue and it is not running yet</li>
 * <li>3: Running: the job is running</li>
 * </ul>
 * <p>
 * If not such job exists, the result will be also the Error status.
 * </p>
 * 
 * <p>
 * For convenience, the job is also passed on output port <i>jobOut</i> if the
 * status is NOT Error and NOT NotInQueue. This token can be used (delaying it
 * with a Sleep actor) to ask its Status again and again until the job is
 * finished or aborted.
 * </p>
 * 
 * <p>
 * When numTasks input is greater than zero, in addition to job's overall
 * status code the actor also outputs  task id and task status of individual tasks
 * </p>
 * 
 * @author Norbert Podhorszki, Chandrika Sivaramakrishnan, Jared Chase
 * @version $Id: JobStatus.java 26613 2011-01-04 03:01:07Z chandrika $
 * @since Ptolemy II 5.0.1
 */
public class JobStatus extends TypedAtomicActor {
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
	public JobStatus(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());
		jobIn = new TypedIOPort(this, "jobIn", true, false);
		jobIn.setTypeEquals(BaseType.OBJECT);
		new Parameter(jobIn, "_showName", BooleanToken.FALSE);

		//Input Parameters
		waitUntil = new Parameter(this, "Wait Until Status", new StringToken(
				"ANY"));
		waitUntil.setStringMode(true);
		waitUntil.addChoice("ANY");
		waitUntil.addChoice("NEXT");
		for (JobStatusCode code : JobStatusCode.values()) {
			waitUntil.addChoice(code.toString());
		}

		sleepWhileWaiting = new Parameter(this, "Wait Until Sleep",
				new LongToken(_sleepWhileWaitingVal));
		sleepWhileWaiting.setTypeEquals(BaseType.LONG);
		
		sendAllChanges = new Parameter(this, "Send all status changes",
				new BooleanToken(false));
		sendAllChanges.setTypeEquals(BaseType.BOOLEAN);
		
		// Output: jobID of the submitted job
		jobOut = new TypedIOPort(this, "jobOut", false, true);
		jobOut.setTypeEquals(BaseType.OBJECT);
		new Parameter(jobOut, "_showName", BooleanToken.TRUE);

		// Output: task id
		taskId = new TypedIOPort(this, "taskId", false, true);
		taskId.setTypeEquals(BaseType.INT);

		// Output: task status code
		taskStatusCode = new TypedIOPort(this, "taskStatusCode", false, true);
		taskStatusCode.setTypeEquals(BaseType.INT);
		new Parameter(taskStatusCode, "_showName", BooleanToken.TRUE);

		// Output: status code
		statusCode = new TypedIOPort(this, "statusCode", false, true);
		statusCode.setTypeEquals(BaseType.INT);
		new Parameter(statusCode, "_showName", BooleanToken.TRUE);

		// Output: log
		logport = new TypedIOPort(this, "log", false, true);
		logport.setTypeEquals(BaseType.STRING);
		new Parameter(logport, "_showName", BooleanToken.TRUE);

		
		statuscode_tokenProdRate = new Parameter(statusCode,
        "tokenProductionRate");
		statuscode_tokenProdRate.setExpression("4");
		statuscode_tokenProdRate.setVisibility(Settable.NOT_EDITABLE);
		statuscode_tokenProdRate.setTypeEquals(BaseType.INT);
		statuscode_tokenProdRate.setPersistent(false);
		
		log_tokenProdRate = new Parameter(logport,
        "tokenProductionRate");
		log_tokenProdRate.setExpression("4");
		log_tokenProdRate.setVisibility(Settable.NOT_EDITABLE);
		log_tokenProdRate.setTypeEquals(BaseType.INT);
		log_tokenProdRate.setPersistent(false);
		
		job_tokenProdRate = new Parameter(jobOut,
        "tokenProductionRate");
		job_tokenProdRate.setExpression("4");
		job_tokenProdRate.setVisibility(Settable.NOT_EDITABLE);
		job_tokenProdRate.setTypeEquals(BaseType.INT);
		job_tokenProdRate.setPersistent(false);

		numTasks = new Parameter(this,"numTasks");
		numTasks.setExpression("0");
	}

	/***********************************************************
	 * ports and parameters
	 */
	
	/**
	 * A submitted job This port is an output port of type Object.
	 */
	public TypedIOPort jobIn;

	/**
	 * The job is passed on in this actor. This token can be used (delaying it
	 * with a Sleep actor) to ask its Status again and again until the job is
	 * finished or aborted. This port is an output port of type Object.
	 */
	public TypedIOPort jobOut;

	/**
	 * Status code of the job 0 : for some error during execution or if jobID is
	 * invalid 1 : not in queue: i.e. already finished if it had ever been there
	 * (this is good news!) 2 : job is waiting in the queue 3 : job is running
	 * This port is an output port of type Integer;
	 */
	public TypedIOPort statusCode;

	/**
	 * Task status code : for some error during execution or if jobID is
	 * invalid 1 : not in queue: i.e. already finished if it had ever been there
	 * (this is good news!) 2 : job is waiting in the queue 3 : job is running
	 * This port is an output port of type Integer;
	 */
	public TypedIOPort taskStatusCode;

	/**
	 * Task ID : this is the task id for the status code
	 */
	public TypedIOPort taskId;

	/**
	 * Logging information of job status query. Useful to inform user about
	 * problems at unsuccessful status query but it also prints out job status
	 * and job id on successful query. This port is an output port of type
	 * String. The name of port on canvas is 'log'
	 */
	public TypedIOPort logport;

	/** Wait until the job has a specific status. */
	public Parameter waitUntil;

	/**
	 * Amount of time (in milliseconds) to sleep between checking job status.
	 */
	public Parameter sleepWhileWaiting;
	
	/**
	 * Parameter to set if you want job status to ignore waitUntil parameter
	 * and send out every status change
	 */
	public Parameter sendAllChanges;

	/**
	 * Parameter to set if the workflow is task parallel.  This allows for the
	 * tasks to be created and set to the submitted state
	 */
	public Parameter numTasks;
	
	 /** The rate parameter for the output port.
     */
    public Parameter statuscode_tokenProdRate;
    public Parameter log_tokenProdRate;
    public Parameter job_tokenProdRate;


	/***********************************************************
	 * public methods
	 */

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
			} else if(!_waitUntilCodes.contains("NEXT")){
				for(int i=0;i<_waitUntilCodes.size();i++){
					JobStatusCode waitUntilCode = JobStatusCode.getFromString(_waitUntilCodes.get(i));
					if (waitUntilCode == null) {
						throw new IllegalActionException(this,
								"Invalid job status type: " + _waitUntilCodes.get(i));
					}
				}
			}
		} else if (attribute == sleepWhileWaiting) {
			_sleepWhileWaitingVal = ((LongToken) sleepWhileWaiting.getToken())
					.longValue();
			if (_sleepWhileWaitingVal < 0) {
				throw new IllegalActionException(this,
						"Sleep While Waiting value cannot be negative.");
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	@Override
	public void initialize(){
		log.info("Initializing lastStatusCode to null");
		//reset last recorded job and status 
		lastJobID = null;
		lastStatusCode = null;
		lastTaskStatusCodes.clear();
	}
	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 *                Not thrown.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		
		Module module = ConfigurationManager.getModule("actors");
		//System.out.println("KEPLER HOME IS " + System.getProperty("KEPLER"));
		//System.out.println("Resource dir is " + module.getResourcesDir());
		boolean bSendAll = ((BooleanToken) sendAllChanges.getToken()).booleanValue();
		ObjectToken jobToken = (ObjectToken) jobIn.get(0);
		Job job = (Job) jobToken.getValue();
		log.info("****** In job status actor for job: "+ job.getJobID());
		int numTasksVal = ((IntToken)numTasks.getToken()).intValue();
		job.setNumTasks(numTasksVal);
		
		if(_waitUntilCodes.contains("NEXT")|| bSendAll){
			//if it is a new job and waitUntil is NEXT or if all status changes have to be sent
			//set lastJobID to current job and
			//set lastStatusCode  to null(we will start tracking status) 
			String realJobID = job.status.jobID;
			if(lastJobID==null || !realJobID.equalsIgnoreCase(lastJobID)){
				log.debug("lastJobId was " + lastJobID + " current job id is " + realJobID + "  " + job.getJobID() +" Reseting lastjobid and status");
				lastJobID = realJobID;
				lastStatusCode = null;
				lastTaskStatusCodes.clear();
			}
		}
		
		JobStatusCode jobStatusCode;
		JobStatusInfo jobStatusInfo; 
		try {
			if(bSendAll){
				do{
					log.debug("In send all loop of " + job.getJobID());
					jobStatusInfo = getNextStatus(job,numTasksVal);
					jobStatusCode = jobStatusInfo.statusCode;
					sendResult(jobToken, job, jobStatusCode);
					if(numTasksVal > 0){
						sendTaskResults(jobStatusInfo);
					}
				}while(jobStatusCode.ordinal()>1);
				
			} else if(_waitUntilCodes.contains("NEXT")){
				if(lastStatusCode!=null && lastStatusCode.ordinal()<2) {
					//If last status=error or notinqueue there is no NEXT status
					//Return last status(ERROR or NotInQueue)
					jobStatusInfo = lastStatusInfo;
					jobStatusCode = lastStatusCode;
				}else{
					jobStatusInfo = getNextStatus(job,numTasksVal);
					jobStatusCode = jobStatusInfo.statusCode;
				}
			}else {
				//Wait for a specific status
				jobStatusInfo = _checkStatus(job);
				jobStatusCode = jobStatusInfo.statusCode;
				//Modified to support multiple _waitUntil codes - Chandrika Sivaramakrishnan
				//while (_waitUntilCode != null && _waitUntilCode != jobStatusCode) {
				
				//Loop if there is no match and job status is NOT Error or NotInQueue.
				//Second check is necessary to avoid infinite loop in case where job
				//never gets to the user requested state or if the state goes undetected
				//(say during sleep between poll). 
				while(!matchStatus(jobStatusCode)&& jobStatusCode.ordinal()>1){
					log.debug("cur status(" + jobStatusCode +
					  ") is not equal to any of the waitUntil codes (" + _waitUntilCodes.toString() + ")");
					Thread.sleep(_sleepWhileWaitingVal);
					jobStatusInfo = _checkStatus(job);
					jobStatusCode = jobStatusInfo.statusCode;
				}
			}
		} catch (Exception ex) {
			log.error(ex);
			jobStatusCode = JobStatusCode.Error;
			jobStatusInfo= new JobStatusInfo();
			ex.printStackTrace();
			throw new IllegalActionException("JobStatus Error: " + ex.toString());
		}

		if(!bSendAll){ //already sent in the while loop
			sendResult(jobToken, job, jobStatusCode);
			if(numTasksVal > 0){
				//sendTaskResults(job,(TaskParallelJobStatusInfo)lastStatusInfo);
				sendTaskResults(jobStatusInfo);
			}
		}

	}

	private JobStatusInfo getNextStatus(
		Job job, int numTasksVal) throws Exception, InterruptedException {
		log.info("IN getNextStatus for job "+ job.getJobID());
		JobStatusInfo jobStatusInfo = _checkStatus(job);
		JobStatusCode jobStatusCode = jobStatusInfo.statusCode;
		HashMap<String, JobStatusCode> changedTasks = new HashMap<String, JobStatusCode>();
		if(numTasksVal>0){
			changedTasks = getTaskStatusChanges(job,numTasksVal);
		}
		log.debug("Before while loop for job " + job.getJobID()+ "  laststatus code = " +lastStatusCode);
		// while the job status code hasn't changed AND
		// either there are 0 tasks OR all the tasks have remained at the same status
		while(	( lastStatusCode != null && jobStatusCode.ordinal() > 1 && jobStatusCode == lastStatusCode ) && 
			( numTasksVal == 0 || changedTasks.size() == 0 ) ) {
			log.debug("job " + job.getJobID() + " cur status(" + jobStatusCode +
				  ") is equal to lastStatusInfo(" + lastStatusCode + ")");
			Thread.sleep(_sleepWhileWaitingVal);
			jobStatusInfo = _checkStatus(job);
			jobStatusCode = jobStatusInfo.statusCode;

			if(numTasksVal>0){
				changedTasks = getTaskStatusChanges(job,numTasksVal);
			}

		}
		if(numTasksVal>0){
			//record last job and task status
			lastTaskStatusCodes.clear();
			lastTaskStatusCodes.putAll(
				((TaskParallelJobStatusInfo)jobStatusInfo).taskStatusCodes); 
			lastStatusCode = jobStatusCode;
			//return only the changed task statuses
			((TaskParallelJobStatusInfo)jobStatusInfo).taskStatusCodes = changedTasks;
		} else {
			//record only job statuscode
			lastStatusCode = jobStatusCode;
			log.debug("Found Next state for job " + job.getJobID()+ " setting laststatus code = " +lastStatusCode);
		}
		return jobStatusInfo;
	}
	
	

	private HashMap<String,JobStatusCode> getTaskStatusChanges(
		Job job,int numTasksVal) throws IllegalActionException {

		HashMap<String,JobStatusCode> result = new HashMap<String,JobStatusCode>();
	    JobStatusCode jobcode = job.status.statusCode;
	    
		for( int idx = 0; idx < numTasksVal; idx++ ) {
			String taskId = "" + idx;

			JobStatusCode code = 
				((TaskParallelJobStatusInfo)job.status).taskStatusCodes.get(taskId);
			JobStatusCode oldCode = lastTaskStatusCodes.get(taskId);

			if(oldCode != code) {
				result.put(taskId,code); 
			}
		}

		return result;
	}

	private void sendResult(ObjectToken jobToken, Job job,
			JobStatusCode jobStatusCode) throws IllegalActionException {
		String strLog;
		strLog = new String("*******JobStatus: Status of job - " + job.getJobID() + ":" + job.getJobID() + ": "
				+ jobStatusCode.toString());
		log.info(this.getName() + ":Sending job status "+jobStatusCode.ordinal() + " for  " + job.getJobID() );

		if (strLog != null)
			logport.send(0, new StringToken(strLog));
		statusCode.send(0, new IntToken(jobStatusCode.ordinal()));
		jobOut.send(0, jobToken);
	}

	private void sendTaskResults(JobStatusInfo jobStatusInfo) 
		throws IllegalActionException {

		HashMap<String,JobStatusCode> taskStatuses = 
							((TaskParallelJobStatusInfo)jobStatusInfo).taskStatusCodes;
		for(String taskIdStr : taskStatuses.keySet()) {
			taskId.send(0, new IntToken(Integer.parseInt(taskIdStr)));
			taskStatusCode.send(0,new IntToken(taskStatuses.get(taskIdStr).ordinal()));

			// set the old task status
			lastTaskStatusCodes.put(taskIdStr,taskStatuses.get(taskIdStr));
		}
	}

	private boolean matchStatus(JobStatusCode jobStatusCode) {
		String str = jobStatusCode.toString();
		
		if(_waitUntilCodes.size() == 0 || _waitUntilCodes.contains(str)){
			return true;
		}
		return false;
	}

	/***********************************************************
	 * private methods
	 */

	private JobStatusInfo _checkStatus(Job job) throws Exception {
		JobStatusCode jobStatusCode = JobStatusCode.Error;
		if (job == null)
			throw new Exception("JobStatus: Job is null");

		job.status(); // successful query or exception
		log.info("Status of job " + job.getJobID() + ": "
				+ job.status.statusCode.toString());

		return job.status;
	}

	private static final Log log = LogFactory.getLog(JobStatus.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	private ArrayList<String> _waitUntilCodes = new ArrayList<String>();
	//private JobStatusCode _waitUntilCode = null;
	private TaskParallelJobStatusInfo lastStatusInfo = new TaskParallelJobStatusInfo();
	private HashMap<String, JobStatusCode> lastTaskStatusCodes = new HashMap<String, JobStatusCode>();
	private JobStatusCode lastStatusCode = null;
	private long _sleepWhileWaitingVal = 5000;
	private String lastJobID =null;
}
