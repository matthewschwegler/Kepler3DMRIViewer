/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

/* Lidar jobs monitoring database processing. 
 */

package org.geon;

//////////////////////////////////////////////////////////////////////////
//// ExecutionThread
/**
 * Thread for executing the Lidar processing.
 * 
 * @author Efrat Jaeger
 */
public class LidarJobConfig {

	public LidarJobConfig() {
	}

	public LidarJobConfig(String jobId) {
		setJobId(jobId);
	}

	private String jobId;
	private String userId;
	private String submissionDate;
	private String srid;
	private String numRows;
	private String xmin;
	private String xmax;
	private String ymin;
	private String ymax;
	private String res;
	private String dmin;
	private String tension;
	private String smooth;
	private String queryTime;
	private String processTime;
	private String completionDate;
	private String[] classifications;
	private String[] processings;
	private String title;
	private String description;
	private String status;
	private String datasetURI;
	private String datasetURL;

	public void setJobConfig(String jobId, String userId,
			String submissionDate, String srid, String xmin, String xmax,
			String ymin, String ymax, String res, String dmin, String tension,
			String smooth, String queryTime, String processTime,
			String completeDate, String[] classifications,
			String[] processings, String jobStatus) {
		this.jobId = jobId;
		setJobConfig(userId, submissionDate, srid, xmin, xmax, ymin, ymax, res,
				dmin, tension, smooth, queryTime, processTime, completeDate,
				classifications, processings, jobStatus);
	}

	public void setJobConfig(String userId, String submissionDate, String srid,
			String xmin, String xmax, String ymin, String ymax, String res,
			String dmin, String tension, String smooth, String queryTime,
			String processTime, String completeDate, String[] classifications,
			String[] processings, String jobStatus) {
		setUserId(userId);
		setSubmissionDate(submissionDate);
		setSrid(srid);
		setSpatial(xmin, xmax, ymin, ymax);
		setAlgAtts(res, dmin, tension, smooth);
		setTimings(queryTime, processTime, completeDate);
		setClassifications(classifications);
		setProcessings(processings);
		setJobStatus(jobStatus);
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobId() {
		return jobId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setSubmissionDate(String submissionDate) {
		this.submissionDate = submissionDate;
	}

	public String getSubmissionDate() {
		return submissionDate;
	}

	public void setSrid(String srid) {
		this.srid = srid;
	}

	public String getSrid() {
		return srid;
	}

	public void setNumRows(String numRows) {
		this.numRows = numRows;
	}

	public String getNumRows() {
		return numRows;
	}

	public void setSpatial(String xmin, String xmax, String ymin, String ymax) {
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
	}

	public String getXmin() {
		return xmin;
	}

	public String getXmax() {
		return xmax;
	}

	public String getYmin() {
		return ymin;
	}

	public String getYmax() {
		return ymax;
	}

	public void setAlgAtts(String res, String dmin, String tension,
			String smooth) {
		this.res = res;
		this.dmin = dmin;
		this.tension = tension;
		this.smooth = smooth;
	}

	public String getRes() {
		return res;
	}

	public String getDmin() {
		return dmin;
	}

	public String getTension() {
		return tension;
	}

	public String getSmooth() {
		return smooth;
	}

	public void setTimings(String queryTime, String processTime,
			String completeDate) {
		this.queryTime = queryTime;
		this.processTime = processTime;
		this.completionDate = completeDate;
	}

	public String getQueryTime() {
		return queryTime;
	}

	public String getProcessTime() {
		return processTime;
	}

	public String getCompletionDate() {
		return completionDate;
	}

	public void setJobDescription(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public String getJobTitle() {
		return title;
	}

	public String getJobDescription() {
		return description;
	}

	public void setJobStatus(String jobStatus) {
		this.status = jobStatus;
	}

	public String getJobStatus() {
		return status;
	}

	public void setJobDatasetPath(String datasetURI, String datasetURL) {
		this.datasetURI = datasetURI;
		this.datasetURL = datasetURL;
	}

	public String getJobURI() {
		return datasetURI;
	}

	public String getJobURL() {
		return datasetURL;
	}

	public void setClassifications(String[] classifications) {
		this.classifications = new String[classifications.length];
		for (int i = 0; i < classifications.length; i++) {
			this.classifications[i] = classifications[i];
		}
	}

	public String[] getClassifications() {
		return classifications;
	}

	public void setProcessings(String[] processings) {
		this.processings = new String[processings.length];
		for (int i = 0; i < processings.length; i++) {
			this.processings[i] = processings[i];
		}
	}

	public String[] getProcessings() {
		return processings;
	}
}
