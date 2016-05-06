/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * 
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

package org.kepler.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support class for SLURM job manager support Class Job uses the methods of a
 * supporter class to submit jobs and check status
 */
public class JobSupportSLURM implements JobSupport {

	private static final Log log = LogFactory.getLog(JobSupportSLURM.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public JobSupportSLURM() {
	}

	public void init(String slurmBinPath) {
		if (slurmBinPath != null && !slurmBinPath.trim().equals("")) {
			String binPath = new String(slurmBinPath);
			if (!slurmBinPath.endsWith("/"))
				binPath += "/";
			_slurmSubmitCmd = binPath + _slurmSubmitCmd;
			_slurmStatusCmd = binPath + _slurmStatusCmd;
			_slurmDeleteCmd = binPath + _slurmDeleteCmd;
		}
	}

	/**
	 * Create a submission file for the specific job manager, based on the
	 * information available in Job: - executable name - input files - output
	 * files - arguments for the job
	 */
	public boolean createSubmitFile(String filename, Job job) {

		return false;
	}

	/**
	 * Submit command for SLURM return: the command for submission
	 */
    public String getSubmitCmd(String submitFile, String options, Job job) throws JobException {

		StringBuffer _commandStr = new StringBuffer(_slurmSubmitCmd);
		
		// see if there are any dependent jobs
		Job[] dependentJobs = job.getDependentJobs();
		if(dependentJobs != null) {
		    _commandStr.append("-W depend=afterok");
		    for(Job dependentJob : dependentJobs) {
		        _commandStr.append(":" + dependentJob.status.jobID);
		    }
		}
		
		if (options != null) {
			_commandStr.append(" " + options);
		}
		
		_commandStr.append(" " + submitFile);

		return _commandStr.toString();
	}

	/**
	 * Parse output of submission and get information: jobID return String jobID
	 * on success throws JobException at failure (will contain the error stream
	 * or output stream)
	 */
	public String parseSubmitOutput(String output, String error)
			throws JobException {

	
		
		// System.out.println("====SLURM parse: picking the jobid from output...");
		/*
		 * SLURM sbatch output: on success, it is: Submitted batch job 8125
		 * Your job 102368 ("sge.cmd") has been submitted.
		 * on error, messages are printed on stderr, stdout is
		 * empty
		 */
		
		String jobID = null;
		int idx = output.indexOf("\n");
	
		

		if (idx > -1) {
			String firstrow = output.substring(0, idx);
			if (firstrow.matches("Submitted batch job [0-9]*.*")) {
				String s = firstrow.substring(20, idx);
				//int toIdx = idx;
				jobID = s;
				//test
				//System.out.println("JOBID rectify --s print " + s);
				//System.out.println("JOBID rectify --idx print " + idx);
				//System.out.println("JOBID rectify --jobID print " + jobID);
				//System.out.println("JOBID rectify --firstrow print " + firstrow);
		        //test
			}
			if (isDebugging)
				log.debug("SLURM parse: jobID = " + jobID + " firstrow = "
						+ firstrow);
		}

		if (jobID == null) {
			if (error != null && error.length() > 0)
				throw new JobException("Error at submission of SLURM job: "
						+ error);
			else
				throw new JobException("Error at submission of SLURM job: "
						+ output);
		}
		
		//test
				//System.out.println("JOBID rectify --jobID print " + jobID);

	    //test
				
		return jobID;
	} // end-of-submit

	/**
	 * Get the command to ask the status of the job return: the String of
	 * command
	 */
	public String getStatusCmd(String jobID) {
		String _commandStr = _slurmStatusCmd + jobID;
		return _commandStr;
	}

	/**
	 * Parse output of status check command and get status info return: a
	 * JobStatusInfo object, or throws an JobException with the error output
	 */
	public JobStatusInfo parseStatusOutput(String jobID, int exitCode,
			String output, String error) throws JobException {

		// SLURM status does not use exitCode. It can show error, but in real it
		// can mean only that
		// job is not in the queue anymore, which is good...

		// System.out.println("+++++ status: picking the status from output" );
		JobStatusInfo stat = new JobStatusInfo();
		stat.statusCode = JobStatusCode.NotInQueue;

		boolean foundStatus = false;

		String sa[] = output.split("\n");
		
		int idx;
		for (int i = 0; i < sa.length; i++) {
			// System.out.println("Shweta SLURM status string " + i + " = "+ sa[i]);
			String vals[] = sa[i].trim().split("( )+", 9);
			if (jobID.startsWith(vals[0].trim())) { // jobID may be longer than
													// the first field which is
													// limited in length
				if (vals.length >= 5) {
					stat.jobID = jobID;
					String jobName = vals[2].trim();
					stat.owner = vals[3].trim();
					stat.runTime = vals[5].trim();
					String sts = vals[4].trim();
					switch (sts.charAt(0)) {
					case 'C':
						stat.statusCode = JobStatusCode.NotInQueue;
						break;
					case 'F':
					case 'R':
						stat.statusCode = JobStatusCode.Running;
						break;
					case 'S':
					case 'H':
					case 'T':
					case 'W':
					case 'P':
						stat.statusCode = JobStatusCode.Wait;
						break;
					default:
						stat.statusCode = JobStatusCode.Wait;
					}
					foundStatus = true;
					if (isDebugging)
						log.debug("SLURM status Values: jobid=" + stat.jobID
								+ " owner=" + stat.owner + " runTime="
								+ stat.runTime + " status=[" + sts + "]");
				}
			}
		}
		// System.out.println("SLURM status = " + stat.statusCode);

		if (!foundStatus) {
			if (error != null && error.length() > 0) {
				// it can be the message: qstat: Unknown Job Id ...
				if (error.startsWith("SLURM: Unknown Job Id")) {
					stat.jobID = jobID;
					stat.statusCode = JobStatusCode.NotInQueue;
				} else {
					log.warn("Error string = [" + error + "] len="
							+ error.length());
					stat.jobID = jobID;
					stat.statusCode = JobStatusCode.Error;
				}
			} else { // not an error, just job is not in the job queue now
				stat.jobID = jobID;
				stat.statusCode = JobStatusCode.NotInQueue;
			}
		}

		return stat;
	}

	/**
	 * Get the command to remove a job from queue (either running or waiting
	 * jobs). return: the String of command
	 */
	public String getDeleteCmd(String jobID) {
		String _commandStr = _slurmDeleteCmd + jobID;
		return _commandStr;
	}

	/**
	 * Parse output of delete command. return: true or false indicating that the
	 * command was successful or not
	 */
	public boolean parseDeleteOutput(String jobID, int exitCode, String output,
			String error) throws JobException {
		if (exitCode == 0)
			return true;
		else
			return false;
	}

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	// The combined command to execute.
	private String _slurmSubmitCmd = "sbatch ";
	private String _slurmStatusCmd = "squeue --job=";
	private String _slurmDeleteCmd = "scancel ";

	public String getTaskStatusCmd(String jobID) throws NotSupportedException {
		//return job status command as SLURM doesn't support task status command
		return getStatusCmd(jobID);
	}

	public TaskParallelJobStatusInfo parseTaskStatusOutput(String jobID,
			int numTasks, int exitCode, String output, String error)
			throws JobException, NotSupportedException {
		JobStatusInfo jobstatus = parseStatusOutput(jobID, exitCode, output, error);
		TaskParallelJobStatusInfo taskStatus = new TaskParallelJobStatusInfo(jobstatus);
		for(int i=0;i<numTasks;i++){
			taskStatus.taskStatusCodes.put(Integer.toString(i), jobstatus.statusCode);
		}
		return taskStatus;
	}

} // end-of-class-JobSupportSLURM
