/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:24:25 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24235 $'
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

import java.util.HashMap;

/**
 * Status info struct for the job class
 * 
 */
public class TaskParallelJobStatusInfo extends JobStatusInfo {
	public TaskParallelJobStatusInfo(JobStatusInfo jobstatus) {
		this.statusCode = jobstatus.statusCode;
		this.jobID = jobstatus.jobID;
		this.owner = jobstatus.owner;
		this.runTime = jobstatus.runTime;
		this.submissionTime = jobstatus.submissionTime;
	}
	
	public TaskParallelJobStatusInfo() {
	 //default constructor
	}
	public HashMap<String,JobStatusCode> taskStatusCodes = new HashMap<String,JobStatusCode>();
}
