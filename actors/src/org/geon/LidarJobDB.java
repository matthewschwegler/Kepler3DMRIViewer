/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2010-12-09 14:47:22 -0800 (Thu, 09 Dec 2010) $' 
 * '$Revision: 26468 $'
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

//////////////////////////////////////////////////////////////////////////
//// ExecutionThread
/**
 * Thread for executing the Lidar processing.
 * 
 * @author Efrat Jaeger
 */
public class LidarJobDB {

	private static final String LIDARJOBS = "LIDAR.LIDARJOBS";
	private static final String JOBSTATUS = "LIDAR.JOBSTATUS";
	private static final String DATASETS = "LIDAR.DATASETS";
	private static final String JOBCLASSIFICATIONS = "LIDAR.JOBCLASSIFICATIONS";
	private static final String JOBPROCESSINGS = "LIDAR.JOBPROCESSINGS";
	private static final String JOBDESCRIPTION = "LIDAR.JOBDESCRIPTION";
	private static final String LIDARACCESSLIST = "LIDAR.LIDARACCESSLIST";
	private static final String PENDINGACCESSLIST = "LIDAR.PENDINGACCESSLIST";
	private static final String COMMENT_CHAR = "#";
	private static final String algs[] = { "elev", "slope", "aspect", "pcurv" };
	private static final String formats[] = { "view", "arc", "ascii", "tiff" };

	public LidarJobDB(String configFile) {
		setProperties(configFile);
	}

	public LidarJobDB(String configFile, String submissionDate) {
		this.submissionDate = submissionDate;
		setProperties(configFile);
	}

	private String dbclassname;
	private String dburl;
	private String username;
	private String password;
	private Map propsMap = new HashMap();
	private Connection con;
	private String submissionDate;

	public void setProperties(String configFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (!(line.startsWith(COMMENT_CHAR) || line.equals(""))) {
					StringTokenizer st = new StringTokenizer(line, "=");
					propsMap.put(st.nextToken(), st.nextToken());
				}
				line = br.readLine();
			}
			DBsetupvars();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void DBsetupvars() {
		dbclassname = (String) propsMap.get("mdbc.classname");
		dburl = (String) propsMap.get("mdbc.url");
		username = (String) propsMap.get("mdbc.username");
		password = (String) propsMap.get("mdbc.password");
	}

	public void createNewEntry(HttpServletRequest request) {
		String jobId = (String) request.getParameter("id");
		if (jobId == null || jobId.equals("")) {
			System.out.println("ERROR! Job id cannot be null");
			return;
		}
		try {
			connect();
			createNewJobEntry(request);
			createNewJobClassificationsEntry(request);
			createNewJobProcessingsEntry(request);
			createNewJobStatusEntry(jobId);
			disconnect();
		} catch (Exception ex) {
			try {
				disconnect();
				PrintWriter pw = new PrintWriter(new FileWriter(
						"/tmp/dbLog.txt", true));
				ex.printStackTrace(pw);
				pw.close();
			} catch (Exception e) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * create a new job entry in the lidar jobs table.
	 * 
	 * @param request
	 * @throws Exception
	 */
	private void createNewJobEntry(HttpServletRequest request) throws Exception {
		String jobId = (String) request.getParameter("id");

		// use username instead of email.
		String userId = request.getParameter("username"); // TODO: would need to
															// be userID!!!!

		String srid = request.getParameter("srid");
		String numRows = request.getParameter("numRows");

		String MinX = request.getParameter("MinX");
		String MaxX = request.getParameter("MaxX");
		String MinY = request.getParameter("MinY");
		String MaxY = request.getParameter("MaxY");

		String res = request.getParameter("resolution");
		String dmin = request.getParameter("dmin");
		String tension = request.getParameter("spline_tension");
		String smooth = request.getParameter("spline_smoothing");

		if (res == null || res.equals(""))
			res = "6";
		if (dmin == null || dmin.equals(""))
			dmin = "1";
		if (tension == null || tension.equals(""))
			tension = "40";
		if (smooth == null || smooth.equals(""))
			smooth = "0.1";

		Statement stmt = con.createStatement();
		System.out.println("INSERT INTO " + LIDARJOBS + " VALUES('" + jobId
				+ "', '" + userId + "', '" + submissionDate + "', '" + srid
				+ "', '" + MinX + "', '" + MaxX + "', '" + MinY + "', '" + MaxY
				+ "', '" + res + "', '" + dmin + "', '" + tension + "', '"
				+ smooth + "', NULL, NULL, NULL,'" + numRows + "')");
		stmt.execute("INSERT INTO " + LIDARJOBS + " VALUES('" + jobId + "', '"
				+ userId + "', '" + submissionDate + "', '" + srid + "', '"
				+ MinX + "', '" + MaxX + "', '" + MinY + "', '" + MaxY + "', '"
				+ res + "', '" + dmin + "', '" + tension + "', '" + smooth
				+ "', NULL, NULL, NULL,'" + numRows + "')");
		stmt.close();

	}

	/**
	 * Insert all the job's classification attributes.
	 * 
	 * @param request
	 * @throws Exception
	 */
	private void createNewJobClassificationsEntry(HttpServletRequest request)
			throws Exception {
		String jobId = (String) request.getParameter("id");
		String[] classification = request.getParameterValues("c");

		Statement stmt = con.createStatement();

		if (classification != null) {
			for (int i = 0; i < classification.length; i++) {
				System.out.println("INSERT INTO " + JOBCLASSIFICATIONS
						+ " VALUES('" + jobId + "', '" + classification[i]
						+ "')");
				stmt.execute("INSERT INTO " + JOBCLASSIFICATIONS + " VALUES('"
						+ jobId + "', '" + classification[i] + "')");
			}
		}
		stmt.close();
	}

	/**
	 * Insert all of the job's selected processings.
	 * 
	 * @param request
	 * @throws Exception
	 */
	private void createNewJobProcessingsEntry(HttpServletRequest request)
			throws Exception {
		String jobId = (String) request.getParameter("id");

		Statement stmt = con.createStatement();
		for (int i = 0; i < algs.length; i++) {
			for (int j = 0; j < formats.length; j++) {
				String type = algs[i] + formats[j];
				String typeVal = request.getParameter(type);
				System.out.println(type + "=" + typeVal);
				if (typeVal != null && !typeVal.equals("")) {
					System.out.println("INSERT INTO " + JOBPROCESSINGS
							+ " VALUES('" + jobId + "', '" + type + "')");
					stmt.execute("INSERT INTO " + JOBPROCESSINGS + " VALUES('"
							+ jobId + "', '" + type + "')");
				}
			}
		}
		stmt.close();
	}

	/**
	 * Create an initial job status entry
	 * 
	 * @param jobId
	 * @throws Exception
	 */
	public void createNewJobStatusEntry(String jobId) throws Exception {
		Statement stmt = con.createStatement();
		System.out.println("INSERT INTO " + JOBSTATUS + " VALUES('" + jobId
				+ "', 'submitted', '')");
		stmt.execute("INSERT INTO " + JOBSTATUS + " VALUES('" + jobId
				+ "', 'submitted', '')");
		stmt.close();
	}

	public void updateJobEntry(String jobId, Map fieldValues) {

		if (fieldValues.size() > 0) { // Otherwise there is nothing to update.
			String updateQuery = "UPDATE " + LIDARJOBS + " SET";
			Iterator keys = fieldValues.keySet().iterator();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				String value = (String) fieldValues.get(key);
				updateQuery += " " + key + " = '" + value + "',";
			}
			// remove last comma.
			updateQuery = updateQuery.substring(0, updateQuery.length() - 1);
			updateQuery += " WHERE JOBID = '" + jobId + "'";
			System.out.println(updateQuery);
			try {
				connect();
				Statement stmt = con.createStatement();
				stmt.executeUpdate(updateQuery);
				stmt.close();
				disconnect();
			} catch (Exception ex) {
				try {
					disconnect();
					PrintWriter pw = new PrintWriter(new FileWriter(
							"/tmp/dbLog.txt", true));
					ex.printStackTrace(pw);
					pw.close();
				} catch (Exception e) {
					ex.printStackTrace();
				}
			}
		}
	}

	public void setJobStatus(String jobId, String jobStatus, String description) {
		try {
			connect();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + JOBSTATUS
					+ " WHERE JOBID = '" + jobId + "'");
			boolean exists = false;
			while (rs.next()) {
				exists = true;
				break;
			}
			rs.close();
			if (exists) { // update
				System.out.println("UPDATE " + JOBSTATUS + " SET STATUS = '"
						+ jobStatus + "', DESCRIPTION = '" + description
						+ "' WHERE JOBID = '" + jobId + "'");
				stmt.execute("UPDATE " + JOBSTATUS + " SET STATUS = '"
						+ jobStatus + "', DESCRIPTION = '" + description
						+ "' WHERE JOBID = '" + jobId + "'");
			} else { // insert
				System.out.println("INSERT INTO " + JOBSTATUS + " VALUES('"
						+ jobId + "', '" + jobStatus + "', '" + description
						+ "'");
				stmt.execute("INSERT INTO " + JOBSTATUS + " VALUES('" + jobId
						+ "', '" + jobStatus + "', '" + description + "')");
			}
			stmt.close();
			disconnect();
		} catch (Exception ex) {
			disconnect();
			ex.printStackTrace();
		}
	}

	/**
	 * Enter a job description
	 * 
	 */
	public void setJobDescription(String jobId, String title, String description) {
		try {
			connect();
			Statement stmt = con.createStatement();
			System.out.println("INSERT INTO " + JOBDESCRIPTION + " VALUES('"
					+ jobId + "', '" + title + "', '" + description + "')");
			stmt.execute("INSERT INTO " + JOBDESCRIPTION + " VALUES('" + jobId
					+ "', '" + title + "', '" + description + "')");
			stmt.close();
			disconnect();
		} catch (Exception ex) {
			disconnect();
			ex.printStackTrace();
		}
	}

	/**
	 * Get a job description
	 * 
	 */
	public LidarJobConfig getJobDescription(String jobId) {
		LidarJobConfig jobConfig = new LidarJobConfig(jobId);
		String title = "";
		String description = "";
		try {
			connect();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT TITLE, DESCRIPTION FROM "
					+ JOBDESCRIPTION + " WHERE JOBID = '" + jobId + "'");
			while (rs.next()) {
				title = rs.getString(1);
				description = rs.getString(2);
				break;
			}
			rs.close();
			stmt.close();
			jobConfig.setJobDescription(title, description);
			disconnect();
			return jobConfig;
		} catch (Exception ex) {
			try {
				disconnect();
				PrintWriter pw = new PrintWriter(new FileWriter(
						"/tmp/dbLog.txt", true));
				ex.printStackTrace(pw);
				pw.close();
				return jobConfig;
			} catch (Exception e) {
				ex.printStackTrace();
				return jobConfig;
			}
		}
	}

	/**
	 * Get all of a user's job entries
	 * 
	 * @param userId
	 * 	 */
	public LidarJobConfig[] getUserJobs(String userId) {
		LidarJobConfig[] userJobs = null;
		try {
			connect();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT JOBID, SUBMISSIONDATE "
					+ "FROM " + LIDARJOBS + " WHERE USERID = '" + userId + "'");
			Vector jobConfigs = new Vector();
			while (rs.next()) {
				String jobId = rs.getString(1);
				String submissionDate = rs.getString(2);
				String title = "";
				String description = "";
				String status = "";
				Statement stmt1 = con.createStatement();
				ResultSet rsStat = stmt1
						.executeQuery("SELECT TITLE, DESCRIPTION FROM "
								+ JOBDESCRIPTION + " WHERE JOBID = '" + jobId
								+ "'");
				while (rsStat.next()) {
					title = rsStat.getString(1);
					description = rsStat.getString(2);
					break;
				}
				rsStat.close();
				rsStat = stmt1.executeQuery("SELECT STATUS FROM " + JOBSTATUS
						+ " WHERE JOBID = '" + jobId + "'");
				while (rsStat.next()) {
					status = rsStat.getString(1);
					break;
				}
				rsStat.close();
				stmt1.close();
				LidarJobConfig jobConfig = new LidarJobConfig(jobId);
				jobConfig.setUserId(userId);
				jobConfig.setSubmissionDate(submissionDate);
				jobConfig.setJobDescription(title, description);
				jobConfig.setJobStatus(status);
				jobConfigs.add(jobConfig);
			}
			rs.close();
			stmt.close();
			if (jobConfigs.size() > 0) {
				userJobs = new LidarJobConfig[jobConfigs.size()];
				jobConfigs.toArray(userJobs);
			}
			disconnect();
			return userJobs;
		} catch (Exception ex) {
			try {
				disconnect();
				PrintWriter pw = new PrintWriter(new FileWriter(
						"/tmp/dbLog.txt", true));
				ex.printStackTrace(pw);
				pw.close();
				return userJobs;
			} catch (Exception e) {
				ex.printStackTrace();
				return userJobs;
			}
		}
	}

	public LidarJobConfig getJobConfig(String jobId) {
		LidarJobConfig jobConfig = new LidarJobConfig(jobId);
		try {
			connect();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * " + "FROM " + LIDARJOBS
					+ " WHERE JOBID = '" + jobId + "'");
			boolean exists = false;
			String srid = "";
			while (rs.next()) {
				String userId = rs.getString("USERID");
				jobConfig.setUserId(userId);
				String submissionDate = rs.getString("SUBMISSIONDATE");
				jobConfig.setSubmissionDate(submissionDate);
				srid = rs.getString("SRID");
				jobConfig.setSrid(srid);
				String xmin = rs.getString("XMIN");
				String xmax = rs.getString("XMAX");
				String ymin = rs.getString("YMIN");
				String ymax = rs.getString("YMAX");
				jobConfig.setSpatial(xmin, xmax, ymin, ymax);
				String numRows = rs.getString("NUMROWS");
				jobConfig.setNumRows(numRows);
				String res = rs.getString("RES");
				String dmin = rs.getString("DMIN");
				String tension = rs.getString("TENSION");
				String smooth = rs.getString("SMOOTH");
				jobConfig.setAlgAtts(res, dmin, tension, smooth);
				String queryTime = rs.getString("QUERYTIME");
				String processTime = rs.getString("PROCESSTIME");
				String completionDate = rs.getString("COMPLETIONDATE");
				jobConfig.setTimings(queryTime, processTime, completionDate);
				exists = true;
				break;
			}
			rs.close();
			if (!exists) {
				System.out.println("No entry for job id " + jobId
						+ " in the lidar job archival!");
				return null;
			}
			// get job classifications
			rs = stmt.executeQuery("SELECT ATTRIBUTE " + "FROM "
					+ JOBCLASSIFICATIONS + " WHERE JOBID = '" + jobId + "'");
			Vector cVec = new Vector();
			while (rs.next()) {
				String c = rs.getString(1);
				cVec.add(c);
			}
			rs.close();
			String[] classifications = new String[cVec.size()];
			cVec.toArray(classifications);
			jobConfig.setClassifications(classifications);

			// get job processings
			rs = stmt.executeQuery("SELECT ALGORITHM " + "FROM "
					+ JOBPROCESSINGS + " WHERE JOBID = '" + jobId + "'");
			cVec = new Vector();
			while (rs.next()) {
				String c = rs.getString(1);
				cVec.add(c);
			}
			rs.close();
			String[] processings = new String[cVec.size()];
			cVec.toArray(processings);
			jobConfig.setProcessings(processings);
			System.out.println(processings.length);
			// get job status
			rs = stmt.executeQuery("SELECT STATUS " + "FROM " + JOBSTATUS
					+ " WHERE JOBID = '" + jobId + "'");
			while (rs.next()) {
				String status = rs.getString(1);
				jobConfig.setJobStatus(status);
				break;
			}
			rs.close();
			if (srid.equals("") || srid == null) {
				System.out.println("No dataset entry for job id " + jobId
						+ " in the lidar job archival!");
				jobConfig.setJobDatasetPath("", "");
			} else {
				rs = stmt.executeQuery("SELECT URL, PORTLETURI " + "FROM "
						+ DATASETS + " WHERE SRID = '" + srid + "'");
				String portletUri = "";
				String portletUrl = "";
				while (rs.next()) {
					portletUrl = rs.getString(1);
					if (portletUrl == null)
						portletUrl = "";
					portletUri = rs.getString(2);
					if (portletUri == null)
						portletUri = "";
					jobConfig.setJobDatasetPath(portletUri, portletUrl);
					break;
				}
			}
			rs.close();
			stmt.close();
			disconnect();
			return jobConfig;
		} catch (Exception ex) {
			try {
				disconnect();
				PrintWriter pw = new PrintWriter(new FileWriter(
						"/tmp/dbLog.txt", true));
				ex.printStackTrace(pw);
				pw.close();
				return null;
			} catch (Exception e) {
				ex.printStackTrace();
				return null;
			}
		}
	}

	public String getJobStatus(String jobId) {
		try {
			connect();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT STATUS FROM " + JOBSTATUS
					+ " WHERE JOBID = '" + jobId + "'");
			String jobStatus = "";
			while (rs.next()) {
				jobStatus = rs.getString(1);
				break; // Each job should have a single entry.
			}
			stmt.close();
			rs.close();
			disconnect();
			return jobStatus;
		} catch (Exception ex) {
			disconnect();
			ex.printStackTrace();
			return "";
		}
	}

	/**
	 * Add or update user to access/pending access list.
	 * 
	 */
	public boolean addUser(String tableName, String username, String firstName,
			String lastName, String org, String email) {
		boolean hasAccess = false;
		try {
			connect();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM LIDAR." + tableName
					+ " WHERE USERNAME = '" + username.trim().toLowerCase()
					+ "'");
			while (rs.next()) {
				System.out.println("user has access");
				hasAccess = true;
				break;
			}
			rs.close();
			if (hasAccess) {
				// update
				System.out.println("UPDATE LIDAR." + tableName
						+ " SET EMAIL = '" + email + "', FIRSTNAME = '"
						+ firstName + "', LASTNAME = '" + lastName
						+ "', ORGANIZATION = '" + org + "'  WHERE USERNAME = '"
						+ username.trim().toLowerCase() + "'");
				stmt.execute("UPDATE LIDAR." + tableName + " SET EMAIL = '"
						+ email + "', FIRSTNAME = '" + firstName
						+ "', LASTNAME = '" + lastName + "', ORGANIZATION = '"
						+ org + "'  WHERE USERNAME = '"
						+ username.trim().toLowerCase() + "'");
			} else {
				// insert
				System.out.println("INSERT INTO LIDAR." + tableName
						+ " VALUES('" + username.trim().toLowerCase() + "', '"
						+ email + "', '" + firstName + "', '" + lastName
						+ "', '" + org + "')");
				stmt.execute("INSERT INTO LIDAR." + tableName + " VALUES('"
						+ username.trim().toLowerCase() + "', '" + email
						+ "', '" + firstName + "', '" + lastName + "', '" + org
						+ "')");
			}
			stmt.close();
			disconnect();
			return true;
		} catch (Exception ex) {
			disconnect();
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Get existing/pending users list.
	 * 
	 */
	public String getUsers(String tableName) {
		String users = "";
		try {
			connect();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt
					.executeQuery("SELECT * FROM LIDAR." + tableName);
			while (rs.next()) {
				users += rs.getString(1) + " " + rs.getString(2) + " "
						+ rs.getString(3) + " " + rs.getString(4) + " "
						+ rs.getString(5) + "<br>\n";
			}
			stmt.close();
			rs.close();
			disconnect();
			return users;
		} catch (Exception ex) {
			disconnect();
			ex.printStackTrace();
			return "unable to query " + tableName;
		}
	}

	/**
	 * Verify whether a user has access to run lidar jobs.
	 * 
	 */
	public boolean verifyUser(String username) {
		boolean hasAccess = false;
		try {
			connect();
			Statement stmt = con.createStatement();
			System.out.println("SELECT * FROM " + LIDARACCESSLIST
					+ " WHERE USERNAME = '" + username.trim().toLowerCase()
					+ "'");
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + LIDARACCESSLIST
					+ " WHERE USERNAME = '" + username.trim().toLowerCase()
					+ "'");
			while (rs.next()) {
				System.out.println("user has access");
				hasAccess = true;
				break;
			}
			stmt.close();
			rs.close();
			disconnect();
			return hasAccess;
		} catch (Exception ex) {
			disconnect();
			ex.printStackTrace();
			return hasAccess;
		}
	}

	/**
	 * Remove a pending user from the pending access list once approved.
	 * 
	 */
	public boolean removePendingUser(String username) {
		try {
			connect();
			Statement stmt = con.createStatement();
			stmt
					.execute("DELETE FROM " + PENDINGACCESSLIST
							+ " WHERE USERNAME='"
							+ username.trim().toLowerCase() + "'");
			stmt.close();
			disconnect();
			return true;
		} catch (Exception ex) {
			disconnect();
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Delete a job entry
	 * 
	 */
	public void deleteJob(String jobId) {
		try {
			connect();
			Statement stmt = con.createStatement();
			stmt.execute("DELETE FROM " + LIDARJOBS + " WHERE jobid='" + jobId
					+ "'");
			stmt.execute("DELETE FROM " + JOBSTATUS + " WHERE jobid='" + jobId
					+ "'");
			stmt.execute("DELETE FROM " + JOBPROCESSINGS + " WHERE jobid='"
					+ jobId + "'");
			stmt.execute("DELETE FROM " + JOBCLASSIFICATIONS + " WHERE jobid='"
					+ jobId + "'");
			stmt.close();
			disconnect();
		} catch (Exception ex) {
			try {
				PrintWriter pw = new PrintWriter(new FileWriter(
						"/tmp/dbLog.txt", true));
				ex.printStackTrace(pw);
				pw.close();
			} catch (Exception e) {
				ex.printStackTrace();
			}
		}
	}

	private void connect() throws Exception {
		Class.forName(dbclassname).newInstance();
		con = DriverManager.getConnection(dburl, username, password);
	}

	private void disconnect() {
		try {
			con.close();
		} catch (Exception ex) {
			con = null;
		}
	}
}
