/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-27 11:35:29 -0700 (Fri, 27 Jul 2012) $' 
 * '$Revision: 30295 $'
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
 * Support class for the fork job manager support. Uses the jobmgr-fork.sh
 * script to fork processes, which should be installed in path on the target
 * machine. Class Job uses the methods of a supporter class to submit jobs and
 * check status
 */
public class JobSupportFork implements JobSupport {

	private static final Log log = LogFactory.getLog(JobSupportFork.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public JobSupportFork() {
	}

	public void init(String nccsBinPath) {
		if (nccsBinPath != null && !nccsBinPath.trim().equals("")) {
			String binPath = new String(nccsBinPath);
			if (!nccsBinPath.endsWith("/"))
				binPath += "/";
			_forkMgrCmd = binPath + _forkMgrCmd;
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
	 * Submit command for fork return: the command for submission
	 */
    public String getSubmitCmd(String submitFile, String options, Job job) throws JobException {

        if(job.getDependentJobs() != null) {
            throw new JobException("Support for job dependencies with Fork has not been implemented.");
        }

		String _commandStr;
		if (options != null)
			_commandStr = _forkMgrCmd + " " + options + " -s " + submitFile;
		else
			_commandStr = _forkMgrCmd + " -s " + submitFile;

		return _commandStr;
	}

	/**
	 * Parse output of submission and get information: jobID return String jobID
	 * on success throws JobException at failure (will contain the error stream
	 * or output stream)
	 */
	public String parseSubmitOutput(String output, String error)
			throws JobException {

		/*
		 * jmgr-fork.sh submission output: on success, it is like: Submitted job
		 * 102368 on error, messages are printed on stderr, stdout is empty
		 */
		String jobID = null;
		int idx = output.indexOf("\n");

		if (idx > -1) {
			String firstrow = output.substring(0, idx);
			if (firstrow.matches("Submitted job [0-9]*.*")) {
				jobID = firstrow.substring(14);
			}
			if (isDebugging)
				log.debug("Fork parse: jobID = " + jobID + " firstrow = "
						+ firstrow);
		}

		if (jobID == null) {
			if (error != null && error.length() > 0)
				throw new JobException("Error at submission of fork job: "
						+ error);
			else
				throw new JobException("Error at submission of fork job: "
						+ output);
		}
		return jobID;
	} // end-of-submit

	/**
	 * Get the command to ask the status of the job return: the String of
	 * command
	 */
	public String getStatusCmd(String jobID) {
		// jmgr-fork.sh -t <jobID>
		// parseStatusOutput has to look for the given job
		String _commandStr = _forkMgrCmd + " -t " + jobID;
		return _commandStr;
	}

	/**
	 * Parse output of status check command and get status info return: a
	 * JobStatusInfo object, or throws an JobException with the error output
	 */
	public JobStatusInfo parseStatusOutput(String jobID, int exitCode,
			String output, String error) throws JobException {

		// Fork status gives back the unix ps command string on the specific
		// process id
		// with the format: PID USER STAT COMMAND START
		// e.g. 1625 pnorbert Ss -bash 15:09
		// 5087 root Ss /share/apps/merc Jun 14
		// note that start-up time can be two fields
		// If no such job found, the exit code is 1
		// (and error log is: No job with id 24135 found)

		JobStatusInfo stat = new JobStatusInfo();
		stat.statusCode = JobStatusCode.NotInQueue;

		if (exitCode == 1) {
			// no such job, which is not an error for us
			if (isDebugging)
				log.debug("No such process found with process ID: " + jobID);
			return stat;
		}

		boolean foundStatus = false;

		if (output.trim().startsWith(jobID)) {

			String vals[] = output.trim().split("( )+", 6);
			if (vals.length >= 5) {
				stat.jobID = vals[0].trim();
				stat.owner = vals[1].trim();
				String jobName = vals[3].trim();
				stat.submissionTime = vals[4].trim();
				if (vals.length > 5)
					stat.submissionTime += " " + vals[5].trim();
				stat.statusCode = JobStatusCode.Running;

				foundStatus = true;
				if (isDebugging)
					log.debug("Fork status Values: jobid=" + stat.jobID
							+ " owner=" + stat.owner + " jobname=" + jobName
							+ " submit/startTime=" + stat.submissionTime
							+ " status=[" + vals[2].trim() + "]");
			}
		}

		if (!foundStatus) {
			if (error != null && error.length() > 0) {
				log
						.warn("Error string = [" + error + "] len="
								+ error.length());
				stat.jobID = jobID;
				stat.statusCode = JobStatusCode.Error;
			} else { // have no idea what happened, not even an error string?
				log.warn("Unexpected case: No process found with ID " + jobID
						+ " but the command did not return with error code 1");
				stat.jobID = jobID;
				stat.statusCode = JobStatusCode.Error;
			}
		}

		return stat;
	}

	/**
	 * Get the command to remove a job (kill the process) return: the String of
	 * command
	 */
	public String getDeleteCmd(String jobID) {
		String _commandStr = _forkMgrCmd + " -r " + jobID;
		return _commandStr;
	}

	/**
	 * Parse output of delete command. return: true if stdout is empty and
	 * exitCode is 0, otherwise return false
	 */
	public boolean parseDeleteOutput(String jobID, int exitCode, String output,
			String error) throws JobException {
		if (exitCode == 0 && output.length() == 0)
			return true;
		else
			return false;
	}

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	// The jobmanager command to execute.
	private String _forkMgrCmd = "jmgr-fork.sh";

	public String getTaskStatusCmd(String jobID) throws NotSupportedException {
		throw new NotSupportedException("Task parallel jobs are not supported");
	}

	public TaskParallelJobStatusInfo parseTaskStatusOutput(String jobID,
			int numTasks, int exitCode, String output, String error)
			throws JobException, NotSupportedException {
		throw new NotSupportedException("Task parallel jobs are not supported");
	}

} // end-of-class-JobSupportFork
