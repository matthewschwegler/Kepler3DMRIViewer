/*
 * Copyright (c) 2004-2012 The Regents of the University of California.
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
 * Support class for Load Sharing Facility (or simply LSF) job manager support Class Job uses the
 * methods of a supporter class to submit jobs and check status
 * @author Jianwu Wang
 * @version $Id: JobSupportlsf.java 30523 2012-08-24 23:13:55Z jianwu $
 */
public class JobSupportLSF implements JobSupport {

	private static final Log log = LogFactory.getLog(JobSupportLSF.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public JobSupportLSF() {
	}

	public void init(String nccsBinPath) {
		if (nccsBinPath != null && !nccsBinPath.trim().equals("")) {
			String binPath = new String(nccsBinPath);
			if (!nccsBinPath.endsWith("/"))
				binPath += "/";
			_lsfSubmitCmd = binPath + _lsfSubmitCmd;
			_lsfStatusCmd = binPath + _lsfStatusCmd;
			_lsfDeleteCmd = binPath + _lsfDeleteCmd;
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
	 * Submit command for lsf return: the command for submission
	 */
	public String getSubmitCmd(String submitFile, String options, Job job) throws JobException {

		StringBuilder _commandStr = new StringBuilder(_lsfSubmitCmd);
		
        Job[] dependentJobs = job.getDependentJobs();
        if(job.getDependentJobs() != null) {
            _commandStr.append("-w 'ended(");
            for(Job dependentJob : dependentJobs) {
                _commandStr.append(dependentJob.status.jobID + ")&&ended(");
            }
            // deal with the last letters.
            _commandStr.delete(_commandStr.length()-8, _commandStr.length());
            _commandStr.append("' ");
            log.info("command string after adding dependency jobs:" + _commandStr);
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

		// System.out.println("====lsf parse: picking the jobid from output...");
		/*
		 * lsf bsub output: on success, it is: Your job 102368 ("lsf.cmd") has
		 * been submitted. on error, messages are printed on stderr, stdout is
		 * empty
		 */
		String jobID = null;
		int idx = output.indexOf("\n");

		if (idx > -1) {
			String firstrow = output.substring(0, idx);
			if (firstrow.matches("Job <[0-9]*.*")) {
				String s = firstrow.substring(5);
				int toIdx = s.indexOf('>');
				jobID = s.substring(0, toIdx);
			}
			if (isDebugging)
				log.debug("lsf parse: jobID = " + jobID + " firstrow = "
						+ firstrow);
		}

		if (jobID == null) {
			if (error != null && error.length() > 0)
				throw new JobException("Error at submission of lsf job: "
						+ error);
			else
				throw new JobException("Error at submission of lsf job: "
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
		String _commandStr = _lsfStatusCmd + " " + jobID;
		return _commandStr;
	}

	/**
	 * Parse output of status check command and get status info return: a
	 * JobStatusInfo object, or throws an JobException with the error output
	 */
	public JobStatusInfo parseStatusOutput(String jobID, int exitCode,
			String output, String error) throws JobException {

		// System.out.println("+++++ status: picking the status from output" );
		JobStatusInfo stat = new JobStatusInfo();
		stat.statusCode = JobStatusCode.NotInQueue;

		boolean foundStatus = false;

		String sa[] = output.split("\n");
		int idx;
		for (int i = 0; i < sa.length; i++) {
			// System.out.println("lsf status string " + i + " = "+ sa[i]);
			if (sa[i].trim().startsWith(jobID)) {
				String vals[] = sa[i].trim().split("( )+", 9);
				if (vals.length >= 7) {
					stat.jobID = vals[0].trim();
					String jobName = vals[6].trim();
					stat.owner = vals[1].trim();
					stat.submissionTime = vals[7].trim() + " " + vals[8].trim();
					String sts = vals[2].trim();
					//"DONE" means correctly finished, "EXIT" means exit with non-zero result.
					if (sts.equals("RUN")) {
						stat.statusCode = JobStatusCode.Running;
					}  else if (sts.equals("PEND")) {
						stat.statusCode = JobStatusCode.Wait;
					}  else if (sts.equals("EXIT")) {
						stat.statusCode = JobStatusCode.Error;
					}
					foundStatus = true;
					if (isDebugging)
						log
								.debug("lsf status Values: jobid=" + stat.jobID
										+ " owner=" + stat.owner
										+ " submit/startTime="
										+ stat.submissionTime + " status=["
										+ sts + "]");
				}
			}
		}
		// System.out.println("lsf status = " + stat.statusCode);

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
		String _commandStr = _lsfDeleteCmd + jobID;
		return _commandStr;
	}

	/**
	 * Parse output of delete command. return: true if stdout is empty and
	 * exitCode is 0, otherwise return false
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
	private String _lsfSubmitCmd = "bsub ";
	private String _lsfStatusCmd = "bjobs ";
	private String _lsfDeleteCmd = "bkill ";

	public String getTaskStatusCmd(String jobID) throws NotSupportedException {
		throw new NotSupportedException("Task parallel jobs are not supported");
	}

	public TaskParallelJobStatusInfo parseTaskStatusOutput(String jobID,
			int numTasks, int exitCode, String output, String error)
			throws JobException, NotSupportedException {
		throw new NotSupportedException("Task parallel jobs are not supported");
	}

} // end-of-class-JobSupportLSF
