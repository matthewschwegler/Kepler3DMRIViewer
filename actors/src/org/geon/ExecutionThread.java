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

/* A thread for executing the lidar processing. 
 */

package org.geon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

//////////////////////////////////////////////////////////////////////////
//// ExecutionThread
/**
 * Thread for executing the Lidar processing.
 * 
 * @author Efrat Jaeger
 */
public class ExecutionThread extends Thread {

	public ExecutionThread() {
	}

	public ExecutionThread(HttpServletRequest request, StringBuffer threadResp,
			String appPath, String uniqueId, String host, String port) {
		super();
		this.request = request;
		this.appPath = appPath;
		this.uniqueId = uniqueId;
		this.threadResp = threadResp;
		this.host = host;
		this.port = port;
	}

	private HttpServletRequest request;
	private String appPath;
	private String uniqueId;
	private String host;
	private String port;
	public StringBuffer threadResp;
	private String header;
	private String footer;

	public void run() {

		header = "<TABLE>\n";
		header += "<TR>\n";
		header += "<TD><A HREF=\"http://activetectonics.la.asu.edu/GEONatASU/index.htm\" target=\"_new\"><IMG SRC=\"http://agassiz.la.asu.edu/logos/GEONASUWebBanner.jpg\" alt=\"GEON at ASU homepage\"></A></TD>\n";
		header += "<TD><a href=\"http://www.sdsc.edu\" target=\"_new\"><img src=\"http://www.sdsc.edu/logos/SDSClogo-plusname-red.gif\" alt=\"San Diego Supercomputer Center\" height=\"60\" width=\"216\"></a></TD>\n";
		header += "</TR>\n";
		header += "</TABLE>\n";
		header += "<table cellpadding=2>\n";

		footer = "</table>\n";

		String configFile = (String) request.getAttribute("configFile");
		if (configFile == null || configFile.equals("")) {
			System.out
					.println("unable to connect to lidar db - missing configuration file");

			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp
					.append("<tr><td>Unable to connect to the LiDAR database.");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			return;
		}
		LidarJobDB lidarJobDB = new LidarJobDB(configFile, new Date()
				.toString());
		try {
			lidarJobDB.createNewEntry(request);
		} catch (Exception ex) {
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to submit job.");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			return;

		}
		// For logging purposes.
		String log = "";

		// For recording execution times.
		Map timings = new HashMap();

		// get parameters.
		Map inputs = new TreeMap();
		inputs.put("id", uniqueId);
		System.out.println("uniqueId ==> " + uniqueId + "\n");
		log += uniqueId + " ";

		// use username instead of email.
		String email = request.getParameter("email");
		// System.out.println("email ==> " + email);
		inputs.put("email", email);

		String user = request.getParameter("username");

		String ip = request.getRemoteAddr();
		log += ip + " ";

		if (email == null || email.equals("")) {
			email = "noEmail";
		}
		log += email + " ";

		String srid = request.getParameter("srid");
		log += srid + " ";

		String rawdata = request.getParameter("rawdata");
		if (rawdata == null)
			rawdata = "0";
		inputs.put("rawdata", rawdata);

		String download = request.getParameter("download");
		System.out.println("download = " + download);

		// Read processing parameters.
		StringBuffer sb = new StringBuffer();
		StringBuffer xmlParams = new StringBuffer();
		xmlParams
				.append("<command><dataset><filename name=\"/export/downloads/kepler/rawData/");
		xmlParams.append(uniqueId + "\"/></dataset>\n");
		xmlParams.append("<query>\n");

		String algs[] = { "elev", "slope", "aspect", "pcurv" };
		String formats[] = { "view", "arc", "ascii", "tiff" };
		String logAlgs = "";

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				String type = algs[i] + formats[j];
				String typeVal = request.getParameter(type);
				if (j == 0) {
					inputs.put(type, typeVal);
					logAlgs += (i == 0 ? "spline" : algs[i]) + "="
							+ (typeVal.equals("") ? "0" : "1") + " ";
				}
				System.out.println(type + "=" + typeVal);
				if (typeVal != null && !typeVal.equals("")) {
					sb.append(type + "=" + typeVal + "\n");
					xmlParams
							.append("<attribute name=\"" + algs[i]
									+ "\" type=\"" + formats[j]
									+ "\" value=\"1\" />\n");
				}
			}
		}
		String resolution = request.getParameter("resolution");
		if (resolution != null && !resolution.equals("")) {
			sb.append("res=" + resolution + "\n");
			xmlParams.append("<attribute name=\"res\" type=\"\" value=\""
					+ resolution + "\" />\n");
			logAlgs += "res=" + resolution + " ";
		} else
			logAlgs += "res=6 ";

		String dmin = request.getParameter("dmin");
		String tension = request.getParameter("spline_tension");
		String smooth = request.getParameter("spline_smoothing");

		if (dmin != null && !dmin.equals("")) {
			sb.append("dmin=" + dmin + "\n");
			xmlParams.append("<attribute name=\"dmin\" type=\"\" value=\""
					+ dmin + "\" />\n");
			logAlgs += "dmin=" + dmin + " ";
		} else
			logAlgs += "dmin=1 ";
		if (tension != null && !tension.equals("")) {
			sb.append("tension=" + tension + "\n");
			xmlParams.append("<attribute name=\"tension\" type=\"\" value=\""
					+ tension + "\" />\n");
			logAlgs += "tension=" + tension + " ";
		} else
			logAlgs += "tension=40 ";
		if (smooth != null && !smooth.equals("")) {
			sb.append("smooth=" + smooth + "\n");
			xmlParams.append("<attribute name=\"smooth\" type=\"\" value=\""
					+ smooth + "\" />\n");
			logAlgs += "smooth=" + smooth + " ";
		} else
			logAlgs += "smooth=0.1 ";

		String projection = request.getParameter("projection");
		String units = request.getParameter("units");
		sb.append("projection=" + projection + "\n");
		sb.append("units=" + units + "\n");

		xmlParams.append("<attribute name=\"id\" type=\"\" value=\"" + uniqueId
				+ "\" />\n");
		xmlParams
				.append("<attribute name=\"path\" type=\"\" value=\"./\" />\n");
		xmlParams.append("</query></command>");
		System.out.println("xmlParams:\n" + xmlParams.toString());

		String MinX = request.getParameter("MinX");
		String MaxX = request.getParameter("MaxX");
		String MinY = request.getParameter("MinY");
		String MaxY = request.getParameter("MaxY");
		System.out.println("MinX ==> " + MinX);
		System.out.println("MinY ==> " + MinY);
		System.out.println("MaxX ==> " + MaxX);
		System.out.println("MaxY ==> " + MaxY);
		log += MinX + " " + MaxX + " " + MinY + " " + MaxY + " ";

		String[] classification = request.getParameterValues("c");
		log += "{";
		// log += classification.toString() + " ";
		if (classification != null) {
			for (int i = 0; i < classification.length - 1; i++) {
				log += classification[i] + ",";
				// System.out.println("clasification (Execution thread) ==> " +
				// classification[i]);
			}
			log += classification[classification.length - 1];
		}
		log += "} ";

		LidarUtilities lutil = new LidarUtilities(threadResp, header, footer,
				srid);
		boolean propSet = lutil.setProperties(configFile);
		if (!propSet) {
			lidarJobDB.setJobStatus(uniqueId, "query failure",
					"Unable to setup database connection properties.");
			log += "query failure ";
			_logExecution(log + "\n", "errorLog");
			return;
		}

		StringBuffer constraint = new StringBuffer();
		constraint = lutil.createConstraint(classification, MinX, MinY, MaxX,
				MaxY);

		// count the number of matching ROWS - TEMPORARILY DONE HERE!!!
		String numRows = request.getParameter("numRows");
		long queryCount = Long.parseLong(numRows);
		System.out.println("inside exec thread queryCount/rowNum = "
				+ queryCount);
		Vector tableNames = new Vector();
		if (queryCount == -1) {// calculate num rows only if it wasn't
								// calculated yet.
			queryCount = lutil.calculateNumRows(MinX, MinY, MaxX, MaxY,
					classification, download);

			tableNames = lutil.tableNames;
			System.out.println("query returned " + queryCount + "rows.");
		} else {
			tableNames = lutil.getTableNames(MinX, MinY, MaxX, MaxY);
		}
		timings.put("NUMROWS", String.valueOf(queryCount));
		lidarJobDB.updateJobEntry(uniqueId, timings);
		timings.clear();
		log += queryCount + " ";
		if (queryCount == -1) {
			lidarJobDB.setJobStatus(uniqueId, "query failure", "");
			log += "query failure ";
			_logExecution(log + "\n", "errorLog");
			return;
		}
		if (tableNames.size() == 0) {
			lidarJobDB.setJobStatus(uniqueId, "query failure",
					"Empty query response.");
			log += "empty query response ";
			_logExecution(log + "\n", "errorLog");
			return;
		}

		String cStr = "";
		if (classification != null) {
			if (classification.length > 0 && classification.length < 4) {// something
																			// was
																			// selected
																			// but
																			// not
																			// all
				cStr = "classification = " + classification[0];
				for (int i = 1; i < classification.length; i++) {
					// if (classification.length > 1) {
					cStr += "," + classification[i];
				}
				cStr += " and ";
			}
		}
		// System.out.println("cstr = " + cStr);
		if (queryCount == 0) {
			lidarJobDB.setJobStatus(uniqueId, "query failure",
					"Empty query response.");
			threadResp.append(header);
			threadResp
					.append("<tr><td><h2>Empty Query Response!<h2></td></tr>");
			threadResp.append("<tr><td>Querying for " + cStr);
			threadResp.append("bounding box selection: MinX = " + MinX
					+ ", MaxX = " + MaxX + ", ");
			threadResp.append("MinY = " + MinY + ", MaxY = " + MaxY
					+ " returned no result!</td></tr>");
			threadResp.append(footer);
			log += "empty query response ";
			_logExecution(log + "\n", "errorLog");
			return;
		}

		long PROCESSLIMIT = lutil.PROCESSLIMIT;
		long QUERYLIMIT = lutil.QUERYLIMIT;
		boolean hasAccess = lidarJobDB.verifyUser(user);
		if (!hasAccess) {
			PROCESSLIMIT = lutil.PROCESSLIMITNOACC;
			QUERYLIMIT = lutil.QUERYLIMITNOACC;
		}

		if (queryCount > PROCESSLIMIT) {
			if (download.equals("1")) {
				// if exceeding the process limit and a processing algorithm was
				// selected then return a warning.
				// else return the rawdata.
				threadResp.append(header);
				threadResp
						.append("<tr><td><h3>Sorry, unable to process your request.<h3></td></tr>");
				threadResp.append("<tr><td>Querying for " + cStr);
				threadResp.append("bounding box selection: MinX = " + MinX
						+ ", MaxX = " + MaxX + ", ");
				threadResp.append("MinY = " + MinY + ", MaxY = " + MaxY
						+ " returned more than the maximum capacity. ");
				threadResp
						.append("Currently the process is limited to 1,600,000 points, please modify your query or ");
				threadResp.append("try again in the future.</td></tr>");
				threadResp.append(footer);
				lidarJobDB.setJobStatus(uniqueId, "process failure",
						"Processing exceeds maximum points limit.");

				String fromAddress = "GLW Support <efrat@geon01.sdsc.edu>";

				String messageBody = "Thank you for using the GEON LiDAR Workflow running on the GEONgrid.\n\n";
				messageBody += "We were unable to process your request as bounding box selection: ";
				messageBody += "MinX = " + MinX + ", MaxX = " + MaxX + ", "
						+ "MinY = " + MinY + ", MaxY = " + MaxY;
				messageBody += "exceeds maximum processing limits. Please modify your selection and try again.\n\n";
				if (!hasAccess)
					messageBody += "To be able to process larger amounts of data please requst access on the LiDAR main page.\n\n";
				messageBody += "---------------\nThe GEON project";
				String messageSubject = "GEON LiDAR Workflow processing error notification";
				String[] toAddress = { email };
				lutil.sendEmail(fromAddress, toAddress, null, null,
						messageSubject, messageBody);

				log += "query returned more than 1600000 points ";
				_logExecution(log + "\n", "errorLog");
				return;
			}
		}

		if (queryCount > QUERYLIMIT) {
			// if exceeding the query limit (20,000,000 rows).
			threadResp.append(header);
			threadResp
					.append("<tr><td><h3>Sorry, unable to process your request.<h3></td></tr>");
			threadResp.append("<tr><td>Querying for " + cStr);
			threadResp.append("bounding box selection: MinX = " + MinX
					+ ", MaxX = " + MaxX + ", ");
			threadResp.append("MinY = " + MinY + ", MaxY = " + MaxY
					+ " returned more than the maximum capacity. ");
			threadResp
					.append("and is unsupported. Please modify your bounding box selection and/or attributes ");
			threadResp.append("and try again.</td></tr>");
			threadResp.append(footer);
			lidarJobDB.setJobStatus(uniqueId, "query failure",
					"Query exceeds maximum points limits.");

			String fromAddress = "GLW Support <efrat@geon01.sdsc.edu>";

			String messageBody = "Thank you for using the GEON LiDAR Workflow running on the GEONgrid.\n\n";
			messageBody += "We were unable to process your request as bounding box selection: ";
			messageBody += "MinX = " + MinX + ", MaxX = " + MaxX + ", "
					+ "MinY = " + MinY + ", MaxY = " + MaxY;
			messageBody += "exceeds maximum download quota and cannot be processed. Please modify your selection and try again.\n\n";
			if (!hasAccess)
				messageBody += "To be able to download more data please requst access on the LiDAR main page.\n\n";
			messageBody += "---------------\nThe GEON project";

			String messageSubject = "GEON LiDAR Workflow processing error notification";
			String[] toAddress = { email };
			lutil.sendEmail(fromAddress, toAddress, null, null, messageSubject,
					messageBody);

			log += "query returned more than 20000000 points ";
			_logExecution(log + "\n", "errorLog");
			return;
		}

		log += logAlgs;
		// call query template

		int i;
		String tableNamesStr = "{";
		for (i = 0; i < tableNames.size() - 1; i++) {
			tableNamesStr += "\"" + (String) tableNames.get(i) + "\"" + ",";
		}
		tableNamesStr += "\"" + tableNames.get(i) + "\"" + "}";
		System.out.println("tableNamesStr ==> " + tableNamesStr);

		String columnNames = request.getParameter("columnNames");
		System.out.println("columnNames = " + columnNames);
		String columnExpression = request.getParameter("columnExpression");
		System.out.println("columnExpression = " + columnExpression);

		Map queryInputs = new TreeMap();
		queryInputs.put("uniqueId", uniqueId);
		queryInputs.put("appPath", appPath); // NO LONGER NECESSARY??
		queryInputs.put("tableNames", tableNamesStr);
		queryInputs.put("constraint", constraint.toString());
		queryInputs.put("columnExpression", columnExpression);
		queryInputs.put("columnNames", columnNames);

		String qtemplate = appPath + "data/queryAcrossTemplate.xml";
		File qtemplateFile = new File(qtemplate);
		// System.out.println("original query template ==> " + qtemplate);

		// copy the template.
		String queryTemplate = appPath + "data/tmp/queryAcrossTemplate"
				+ uniqueId + ".xml";
		File qwftemplateFile = new File(queryTemplate);
		queryTemplate = "file:///" + queryTemplate;
		// System.out.println("unique id query template ==> " + queryTemplate);

		try {
			InputStream is = new FileInputStream(qtemplateFile);
			OutputStream os = new FileOutputStream(qwftemplateFile);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();

		} catch (Exception ex) {
			lidarJobDB.setJobStatus(uniqueId, "query failure", ex.getMessage());
			System.out.println("unable to create query template for "
					+ uniqueId + ex.getMessage());

			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to create query template for "
					+ uniqueId + ":\n" + ex.getMessage());
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			log += "unable to query the lidar db ";
			_logExecution(log + "\n", "errorLog");
			return;
		}

		// System.out.println("query template url ==> " + queryTemplate);
		System.out.println("BEFORE executing query!!");

		lidarJobDB.setJobStatus(uniqueId, "querying", ""); // set the job status
															// in the monitoring
															// DB.

		LidarWorkflowExecute lwfe = new LidarWorkflowExecute();
		String res = "";
		try {
			long begin = new Date().getTime();
			res = lwfe.executeQuery(queryTemplate, queryInputs);
			long end = new Date().getTime();
			long queryTime = end - begin;
			String queryTimeSec = queryTime / 1000 + "";
			timings.put("QUERYTIME", queryTimeSec);
			log += queryTimeSec + " ";
			System.out.println("query response url ==> " + res
					+ "\n queryTime = " + queryTimeSec);
			if (res.equals(""))
				throw new Exception("res is empty");
		} catch (Exception ex) {
			lidarJobDB.setJobStatus(uniqueId, "query failure",
					"Unable to query LiDAR database.");
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to query the LiDAR database.");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			log += "unable to query the lidar db ";
			_logExecution(log + "\n", "errorLog");
			return;
			// ADD EXCEPTION NOTIFICATION TO USER AND STOP EXECUTION!!!!
		}

		// /TODO! this test is no longer necessary
		if (res.equals("0")) {
			lidarJobDB.setJobStatus(uniqueId, "query failure",
					"No available data for user selection.");
			threadResp.append(header);
			threadResp
					.append("<tr><td><h2>Empty Query Response!<h2></td></tr>");
			threadResp.append("<tr><td>Querying for " + cStr);
			threadResp.append("bounding box selection: MinX = " + MinX
					+ ", MaxX = " + MaxX + ", ");
			threadResp.append("MinY = " + MinY + ", MaxY = " + MaxY
					+ " returned no result!</td></tr>");
			threadResp.append(footer);
			log += "empty query response ";
			_logExecution(log + "\n", "errorLog");
			return;
		}
		inputs.put("rawdataURL", res);

		// remove query file - TODO!

		inputs.put("appPath", appPath);
		inputs.put("host", host);
		inputs.put("port", port);
		inputs.put("download", download);
		// inputs.put("xmlParams",xmlParams.toString());

		// if only download raw data was selected
		if (download.equals("0")) {
			// The user is only interested in raw data.
			lidarJobDB.setJobStatus(uniqueId, "done", "");
			timings.put("COMPLETIONDATE", new Date().toString());
			lidarJobDB.updateJobEntry(uniqueId, timings);
			StringBuffer queryResp = new StringBuffer();
			queryResp.append("Raw data for " + cStr);
			queryResp.append("bounding box selection: MinX = " + MinX
					+ ", MaxX = " + MaxX + ", ");
			queryResp.append("MinY = " + MinY + ", MaxY = " + MaxY
					+ " is available at ");

			threadResp.append(header);
			threadResp.append("<tr><td>");
			threadResp.append(queryResp.toString());
			threadResp.append("<A href=\"" + res + "\">queryResult</A> ("
					+ queryCount + " points).</td></tr>");
			threadResp.append(footer);
			threadResp.append("<br><table><tr><td>Download ");
			threadResp
					.append("<A href=\"http://activetectonics.la.asu.edu/GEONatASU/LViz.html\">");
			threadResp.append("LViz</A>");
			threadResp
					.append(" - A free application for visualization of LiDAR point cloud and interpolated surface ");
			threadResp
					.append("data developed in the Active Tectonics Research Group at Arizona State University.");
			threadResp.append("</td></tr></table>");

			// Email results.
			if (email != null && !email.equals("")) {
				emailQueryResp(queryResp.toString(), res, email, queryCount);
			}
			_logExecution(log + "\n", "rawDataLog");
			return;
		}
		// else..
		inputs.put("queryCount", String.valueOf(queryCount));
		if (rawdata.equals("1")) {
			// send bounding box for printout.
			inputs.put("MinX", MinX);
			inputs.put("MinY", MinY);
			inputs.put("MaxX", MaxX);
			inputs.put("MaxY", MaxY);
			inputs.put("classification", cStr);
		}

		// Create parameters file from string buffer.
		try {
			String filePath = appPath + "data/tmp/params" + uniqueId + ".txt";
			File paramsFile = new File(filePath);
			String paramsFileURL = paramsFile.getAbsolutePath();
			System.out.println("Writing to parameter file " + paramsFileURL);
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					paramsFileURL, false));

			bw.write(sb.toString());
			bw.close();

		} catch (Exception ex) {
			lidarJobDB.setJobStatus(uniqueId, "process failure", ex
					.getMessage());
			System.out.println("unable to create params file " + uniqueId
					+ ": " + ex.getMessage());
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to create params file "
					+ uniqueId + ": " + ex.getMessage());
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			log += "unable to process user selections ";
			_logExecution(log + "\n", "errorLog");
			return;
		}

		// String template = "file:///" + appPath + "data/processTemplate.xml";
		// String template = appPath + "data/processTemplate.xml";
		String template = appPath + "data/processTemplateWS.xml";
		System.out.println("process template ==> " + template);
		File templateFile = new File(template);

		// copy the template.
		// String workflowTemplate = "file:///" + appPath +
		// "data/processTemplate" + uniqueId + ".xml";
		String workflowTemplate = appPath + "data/tmp/processTemplate"
				+ uniqueId + ".xml";
		File wftemplateFile = new File(workflowTemplate);
		workflowTemplate = "file:///" + workflowTemplate;

		try {
			InputStream is = new FileInputStream(templateFile);
			OutputStream os = new FileOutputStream(wftemplateFile);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();

		} catch (Exception ex) {
			lidarJobDB.setJobStatus(uniqueId, "process failure", ex
					.getMessage());
			System.out.println("unable to create process template for "
					+ uniqueId + ex.getMessage());

			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to create process template for "
					+ uniqueId + ":\n" + ex.getMessage());
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			log += "unable to process user selections ";
			_logExecution(log + "\n", "errorLog");
			return;
		}

		System.out.println("workflow url ==> " + workflowTemplate);
		System.out.println("BEFORE!!");
		lidarJobDB.setJobStatus(uniqueId, "processing", ""); // set the job
																// status in the
																// monitoring
																// DB.
		// LidarWorkflowExecute lwfe = new LidarWorkflowExecute();
		try {
			long begin = new Date().getTime();
			res = lwfe.executeProcess(workflowTemplate, inputs);
			long end = new Date().getTime();
			long processTime = end - begin;
			String processTimeSec = processTime / 1000 + "";
			timings.put("PROCESSTIME", processTimeSec);
			log += processTimeSec + " ";
			System.out.println("process result url ==> " + res
					+ "\n processTime = " + processTimeSec);
			// response.sendRedirect(res);
			System.out.println("RES = " + res);
			if (res.equals("0")) {
				lidarJobDB.setJobStatus(uniqueId, "process failure",
						"GRASS processing error");
				threadResp.append(header);
				threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
				threadResp
						.append("<tr><td>GRASS processing error for bounding box selection: ");
				threadResp.append("MinX = " + MinX + ", MaxX = " + MaxX
						+ ", MinY = " + MinY + ", MaxY = " + MaxY);
				threadResp
						.append(". Please modify selection area and try again.");
				threadResp.append("</td></tr>");
				threadResp.append(footer);
				log += "GRASS processing error ";
				_logExecution(log + "\n", "errorLog");

				String fromAddress = "GLW Support <efrat@geon01.sdsc.edu>";

				String messageBody = "Thank you for using the GEON LiDAR Workflow running on the GEONgrid.\n\n";
				messageBody += "We were unable to process your request for bounding box selection: ";
				messageBody += "MinX = " + MinX + ", MaxX = " + MaxX + ", "
						+ "MinY = " + MinY + ", MaxY = " + MaxY;
				messageBody += "due to a GRASS processing error. Please modify your selection and try again.\n\n";
				messageBody += "---------------\nThe GEON project";
				String messageSubject = "GEON LiDAR Workflow processing error notification";
				String[] toAddress = { email };
				lutil.sendEmail(fromAddress, toAddress, null, null,
						messageSubject, messageBody);

				return;

			}
			URL url = new URL(res);
			Reader in = new InputStreamReader(url.openStream());
			BufferedReader br = new BufferedReader(in);
			String line;

			while ((line = br.readLine()) != null) {
				threadResp.append(line + "\n");
			}

		} catch (Exception ex) {
			lidarJobDB.setJobStatus(uniqueId, "process failure", ex
					.getMessage());
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Workflow exception please try again.");
			threadResp.append("</td></tr>");
			threadResp.append("<tr><td>" + ex.getMessage() + ".");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			log += "workflow execution error ";
			_logExecution(log + "\n", "errorLog");
			return;
		}
		String completeDate = new Date().toString();
		log += completeDate;
		lidarJobDB.setJobStatus(uniqueId, "done", "");
		timings.put("COMPLETIONDATE", completeDate);
		lidarJobDB.updateJobEntry(uniqueId, timings);
		_logExecution(log + "\n", "lidarLog");
	}

	public void getProcessResponse(String configFile) {

		LidarJobDB lidarJobDB = new LidarJobDB(configFile);
		LidarJobConfig jobConfig = lidarJobDB.getJobConfig(uniqueId);

		Map inputs = new TreeMap();
		inputs.put("id", uniqueId);
		inputs.put("rawdata", "0");
		inputs.put("download", "1");
		inputs.put("appPath", appPath);
		inputs.put("host", host);
		inputs.put("port", port);

		String[] processings = jobConfig.getProcessings();
		for (int i = 0; i < processings.length; i++) {
			inputs.put(processings[i], "1");
		}

		String template = appPath + "data/gmTemplate.xml";
		System.out.println("global mapper template ==> " + template);
		File templateFile = new File(template);

		// copy the template.
		String workflowTemplate = appPath + "data/tmp/gmTemplate" + uniqueId
				+ ".xml";
		File wftemplateFile = new File(workflowTemplate);
		workflowTemplate = "file:///" + workflowTemplate;

		try {
			InputStream is = new FileInputStream(templateFile);
			OutputStream os = new FileOutputStream(wftemplateFile);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();

			System.out.println("workflow url ==> " + workflowTemplate);
			System.out.println("BEFORE!!");

			LidarWorkflowExecute lwfe = new LidarWorkflowExecute();
			String res = lwfe.executeProcess(workflowTemplate, inputs);

			URL url = new URL(res);
			Reader in = new InputStreamReader(url.openStream());
			BufferedReader br = new BufferedReader(in);
			String line;

			while ((line = br.readLine()) != null) {
				threadResp.append(line + "\n");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to obtain process response:\n"
					+ ex.getMessage());
			threadResp.append("</td></tr>");
			threadResp.append("<tr><td>" + ex.getMessage() + ".");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			return;
		}
	}

	public void getQueryResponse(String configFile) {

		LidarJobDB lidarJobDB = new LidarJobDB(configFile);
		LidarJobConfig jobConfig = lidarJobDB.getJobConfig(uniqueId);

		String MinX = jobConfig.getXmin();
		String MaxX = jobConfig.getXmax();
		String MinY = jobConfig.getYmin();
		String MaxY = jobConfig.getYmax();
		String[] classification = jobConfig.getClassifications();
		String srid = jobConfig.getSrid();
		String queryCount = jobConfig.getNumRows();

		try {
			LidarUtilities lutil = new LidarUtilities(threadResp, header,
					footer, srid);
			boolean propSet = lutil.setProperties(configFile);
			if (!propSet) {
				throw new Exception(
						"unable to setup database connection properties");
			}

			StringBuffer constraint = new StringBuffer();
			constraint = lutil.createConstraint(classification, MinX, MinY,
					MaxX, MaxY);

			Vector tableNames = new Vector();
			tableNames = lutil.getTableNames(MinX, MinY, MaxX, MaxY);

			String cStr = "";
			if (classification != null) {
				if (classification.length > 0 && classification.length < 4) {// something
																				// was
																				// selected
																				// but
																				// not
																				// all
					cStr = "classification = " + classification[0];
					for (int i = 1; i < classification.length; i++) {
						// if (classification.length > 1) {
						cStr += "," + classification[i];
					}
					cStr += " and ";
				}
			}

			int i;
			String tableNamesStr = "{";
			for (i = 0; i < tableNames.size() - 1; i++) {
				tableNamesStr += "\"" + (String) tableNames.get(i) + "\"" + ",";
			}
			tableNamesStr += "\"" + tableNames.get(i) + "\"" + "}";

			Map queryInputs = new TreeMap();
			queryInputs.put("uniqueId", uniqueId); // check that
			queryInputs.put("appPath", appPath); // check that
			queryInputs.put("tableNames", tableNamesStr);
			queryInputs.put("constraint", constraint.toString());

			String queryTemplate = appPath + "data/tmp/queryAcrossTemplate"
					+ uniqueId + ".xml"; // replace jobId with generated id
			File qwftemplateFile = new File(queryTemplate);
			queryTemplate = "file:///" + queryTemplate;

			String qtemplate = appPath + "data/queryAcrossTemplate.xml";
			File qtemplateFile = new File(qtemplate);

			InputStream is = new FileInputStream(qtemplateFile);
			OutputStream os = new FileOutputStream(qwftemplateFile);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				os.write(buf, 0, len);
			}
			is.close();
			os.close();

			LidarWorkflowExecute lwfe = new LidarWorkflowExecute();
			String res = "";
			res = lwfe.executeQuery(queryTemplate, queryInputs);

			StringBuffer queryResp = new StringBuffer();
			queryResp.append("Raw data for " + cStr);
			queryResp.append("bounding box selection: MinX = " + MinX
					+ ", MaxX = " + MaxX + ", ");
			queryResp.append("MinY = " + MinY + ", MaxY = " + MaxY
					+ " is available at ");

			threadResp.append("<table><tr><td>");
			threadResp.append(queryResp.toString());
			threadResp.append("<A href=\"" + res + "\">queryResult</A>");
			if (queryCount != null) {
				if (!queryCount.equals("-1")) {
					threadResp.append(" (" + queryCount + " points).");
				}
			}
			threadResp.append(".</td></tr></table>");
			threadResp.append("<br><table><tr><td>Download ");
			threadResp
					.append("<A href=\"http://activetectonics.la.asu.edu/GEONatASU/LViz.html\">");
			threadResp.append("LViz</A>");
			threadResp
					.append(" - A free application for visualization of LiDAR point cloud and interpolated surface ");
			threadResp
					.append("data developed in the Active Tectonics Research Group at Arizona State University.");
			threadResp.append("</td></tr></table>");

		} catch (Exception ex) {
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to obtain query response:\n"
					+ ex.getMessage());
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			return;
		}
	}

	private void emailQueryResp(String queryResp, String URL, String email,
			long queryCount) {
		String messageBody = "Thank you for using the GEON LiDAR Workflow running on the GEONgrid.\n\n";
		messageBody += queryResp + URL + " (" + queryCount + " points).\n\n";
		messageBody += "Please note that the results will expire after 48 hours.\n\n";
		messageBody += "---------------\nThe GEON project";

		String messageSubject = "GEON LiDAR Workflow processing results";

		String host = "localhost";
		String fromAddress = "GEON LiDAR Workflow Processing Notification <efrat@geon01.sdsc.edu>";

		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.debug", "false");

		Session session = Session.getInstance(props);

		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(fromAddress));
			InternetAddress[] address = { new InternetAddress(email) };
			msg.setRecipients(Message.RecipientType.TO, address);
			msg.setSubject(messageSubject);
			msg.setSentDate(new java.util.Date());
			msg.setText(messageBody);

			Transport.send(msg);
		} catch (MessagingException mex) {
			mex.printStackTrace();
		}
	}

	public void _logExecution(String log, String fileName) {
		String logURL = System.getProperty("user.home") + File.separator
				+ ".lidar" + File.separator + fileName;
		try {
			File logFile = new File(logURL);
			FileWriter fw = new FileWriter(logFile, true);

			RandomAccessFile raf = new RandomAccessFile(logFile, "r");
			if (raf.length() == 0) {// write header
				logFile.createNewFile();
				fw
						.write("id ip email dataset minX maxX minY maxY classification munberOfPoints "
								+ "spline slope aspect pcurv res dmin tension smooth queryTime processTime\n");
			}
			fw.write(log);
			fw.flush();
			fw.close();
		} catch (IOException ioex) {
			System.out.println("Unable to write " + log + " to " + logURL
					+ ".\n" + ioex.getMessage());
		}
	}
}
