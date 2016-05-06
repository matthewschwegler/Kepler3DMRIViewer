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

/**
 * Abstract interface for jobmanager support classes Class Job uses the methods
 * of a supporter class to submit jobs and check status
 */
interface JobSupport {

	/**
	 * Store the binary path given by the user and use that in the methods below
	 */
	void init(String binPath);

	/**
	 * Create a submission file for the specific job manager, based on the
	 * information available in Job: - executable name - input files - output
	 * files - arguments for the job
	 */
	boolean createSubmitFile(String filename, Job job);

	/**
	 * Submit command for a jobmanager return: the command for submission
	 */
	String getSubmitCmd(String submitFile, String options, Job job) throws JobException;

	/**
	 * Parse output of submission and get information: jobID return String jobID
	 * on success throws JobException at failure (will contain the error stream
	 * or output stream)
	 */
	String parseSubmitOutput(String output, String error) throws JobException;

	/**
	 * Get the command to ask the status of the job return: the String of
	 * command
	 */
	String getStatusCmd(String jobID);

	/**
	 * Parse output of status check command and get status info return: a
	 * JobStatusInfo object, or throws an JobException with the error output
	 */
	JobStatusInfo parseStatusOutput(String jobID, int exitCode, String output,
			String error) throws JobException;

	/**
	 * Get the command to remove a job from queue (either running or waiting
	 * jobs) return: the String of command
	 */
	String getDeleteCmd(String jobID);

	/**
	 * Parse output of delete command return: true or false indicating that the
	 * command was successful or not
	 */
	boolean parseDeleteOutput(String jobID, int exitCode, String output,
			String error) throws JobException;
	
	/**
     * Parse output of task status check command and get status info
     * @return: a JobStatusInfo object or 
     *     		throws an JobException with the error output or
     *     		throws a NotSupportedException if the scheduler doesn't support task status
     * 
     */
    public TaskParallelJobStatusInfo parseTaskStatusOutput (
        String jobID,
        int numTasks,
        int exitCode,
        String output,
        String error ) throws JobException, NotSupportedException;


    /** Get the command to ask the status of both job and tasks
     *  Eg. <job status command>;<task status command>
     *  return: the String of command or
     *   		throws a NotSupportedException if the scheduler doesn't support task status
     */
    public String getTaskStatusCmd (String jobID) throws NotSupportedException;

}