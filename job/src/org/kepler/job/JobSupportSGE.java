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
 * Support class for Sun Grid Engine job manager support Class Job uses the
 * methods of a supporter class to submit jobs and check status
 */
public class JobSupportSGE implements JobSupport {

	private static final Log log = LogFactory.getLog(JobSupportSGE.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public JobSupportSGE() {
	}

	public void init(String nccsBinPath) {
		if (nccsBinPath != null && !nccsBinPath.trim().equals("")) {
			String binPath = new String(nccsBinPath);
			if (!nccsBinPath.endsWith("/"))
				binPath += "/";
			_sgeSubmitCmd = binPath + _sgeSubmitCmd;
			_sgeStatusCmd = binPath + _sgeStatusCmd;
			_sgeDeleteCmd = binPath + _sgeDeleteCmd;
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
	 * Submit command for SGE return: the command for submission
	 */
	public String getSubmitCmd(String submitFile, String options, Job job) throws JobException {

		StringBuilder _commandStr = new StringBuilder(_sgeSubmitCmd);
		
		// see if there are any dependent jobs
        Job[] dependentJobs = job.getDependentJobs();
        if(dependentJobs != null) {
            _commandStr.append("-hold_jid ");
            for(Job dependentJob : dependentJobs) {
                _commandStr.append(dependentJob.status.jobID + ",");
            }
            // remove the trailing comma
            _commandStr.deleteCharAt(_commandStr.length()-1);
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

		// System.out.println("====SGE parse: picking the jobid from output...");
		/*
		 * SGE qsub output: on success, it is: Your job 102368 ("sge.cmd") has
		 * been submitted. on error, messages are printed on stderr, stdout is
		 * empty
		 */
		String jobID = null;
		int idx = output.indexOf("\n");

		if (idx > -1) {
			String firstrow = output.substring(0, idx);
			if (firstrow.matches("Your job [0-9]*.*")) {
				String s = firstrow.substring(9);
				int toIdx = s.indexOf(' ');
				jobID = s.substring(0, toIdx);
			}
			if (isDebugging)
				log.debug("SGE parse: jobID = " + jobID + " firstrow = "
						+ firstrow);
		}

		if (jobID == null) {
			if (error != null && error.length() > 0)
				throw new JobException("Error at submission of SGE job: "
						+ error);
			else
				throw new JobException("Error at submission of SGE job: "
						+ output);
		}
		return jobID;
	} // end-of-submit

	/**
	 * Get the command to ask the status of the job return: the String of
	 * command
	 */
	public String getStatusCmd(String jobID) {
		// simple 'qstat' which gives back list of all jobs!
		// parseStatusOutput has to look for the given job
		String _commandStr = _sgeStatusCmd;
		return _commandStr;
	}

	/**
	 * Parse output of status check command and get status info return: a
	 * JobStatusInfo object, or throws an JobException with the error output
	 */
	public JobStatusInfo parseStatusOutput(String jobID, int exitCode,
			String output, String error) throws JobException {

		// SGE's qsub gives back all jobs, one per line
		// we have to look for the given jobID in the beginning of a line
		// line format: jobid priority name user status date queue slots

		// System.out.println("+++++ status: picking the status from output" );
		JobStatusInfo stat = new JobStatusInfo();
		stat.statusCode = JobStatusCode.NotInQueue;

		boolean foundStatus = false;

		String sa[] = output.split("\n");
		int idx;
		for (int i = 0; i < sa.length; i++) {
			// System.out.println("SGE status string " + i + " = "+ sa[i]);
			if (sa[i].trim().startsWith(jobID)) {
				String vals[] = sa[i].trim().split("( )+", 9);
				if (vals.length >= 7) {
					stat.jobID = vals[0].trim();
					String jobName = vals[2].trim();
					stat.owner = vals[3].trim();
					stat.submissionTime = vals[5].trim() + " " + vals[6].trim();
					String sts = vals[4].trim();
					switch (sts.charAt(0)) {
					case 'r': // running
					case 'R': // restarted
					case 't': // transferred
					case 'd': // deletion (under removal)
						stat.statusCode = JobStatusCode.Running;
						break;
					case 's': // suspended
					case 'S': // suspended
					case 'w': // wait
					case 'h': // hold
					case 'T': // Threshold
						stat.statusCode = JobStatusCode.Wait;
						break;
					default:
						stat.statusCode = JobStatusCode.Wait;
					}
					foundStatus = true;
					if (isDebugging)
						log
								.debug("SGE status Values: jobid=" + stat.jobID
										+ " owner=" + stat.owner
										+ " submit/startTime="
										+ stat.submissionTime + " status=["
										+ sts + "]");
				}
			}
		}
		// System.out.println("SGE status = " + stat.statusCode);

		if (!foundStatus) {
			if (error != null && error.length() > 0) {
				log
						.warn("Error string = [" + error + "] len="
								+ error.length());
				stat.jobID = jobID;
				stat.statusCode = JobStatusCode.Error;
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
		String _commandStr = _sgeDeleteCmd + jobID;
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

	// The combined command to execute.
	private String _sgeSubmitCmd = "qsub ";
	private String _sgeStatusCmd = "qstat ";
	private String _sgeDeleteCmd = "qdel ";

	public String getTaskStatusCmd(String jobID) throws NotSupportedException {
		throw new NotSupportedException("Task parallel jobs are not supported");
	}

	public TaskParallelJobStatusInfo parseTaskStatusOutput(String jobID,
			int numTasks, int exitCode, String output, String error)
			throws JobException, NotSupportedException {
		throw new NotSupportedException("Task parallel jobs are not supported");
	}

} // end-of-class-JobSupportSGE
