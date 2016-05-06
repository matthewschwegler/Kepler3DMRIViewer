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

package org.geon;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

//////////////////////////////////////////////////////////////////////////
//// LidarUtitilities
/**
 * Thread for executing the Lidar processing.
 * 
 * @author Efrat Jaeger
 */
public class LidarUtilities {

	private static final String METADATATABLE = "NSAF.META";
	public final long PROCESSLIMIT = 1600000;
	public final long QUERYLIMIT = 20000000;
	public final long PROCESSLIMITNOACC = 1000000;
	public final long QUERYLIMITNOACC = 5000000;
	private final static String COMMENT_CHAR = "#";

	public LidarUtilities(StringBuffer threadResp, String header,
			String footer, String srid) {
		this.threadResp = threadResp;
		this.header = header;
		this.footer = footer;
		this.srid = srid;
	}

	public StringBuffer threadResp;
	private String header;
	private String footer;
	private String srid;
	private Map propsMap = new HashMap();
	private String dbclassname;
	private String dburl;
	private String username;
	private String password;

	/**
	 * Sets or resets the property file parameters.
	 */
	public boolean setProperties(String propsFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(propsFile));
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
			return true;
		} catch (Exception ex) {
			System.out.println("unable to set up config properties");
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp
					.append("<tr><td>Unable to connect to the lidar database");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			return false;
		}
	}

	public long calculateNumRows(String MinX, String MinY, String MaxX,
			String MaxY, String[] classification, String download) {
		Connection con = null;
		try {
			con = connect();

		} catch (Exception ex) {
			System.out.println("unable to connect to lidar database");
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp
					.append("<tr><td>Unable to connect to the lidar database");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			disconnect(con);
			return -1;
		}
		// Get all tables within the bounding box.
		tableNames = new Vector();
		tableNames = getResidingTableNames(con, MinX, MaxX, MinY, MaxY);
		if (tableNames.size() == 0) {
			disconnect(con);
			return -1;
		}
		// create the query constraint.
		StringBuffer constraint = createConstraint(classification, MinX, MinY,
				MaxX, MaxY);

		// sum the number of rows by each table
		long count = countAcrossTables(con, tableNames, constraint, download);

		disconnect(con);
		return count;
	}

	public Vector getTableNames(String MinX, String MinY, String MaxX,
			String MaxY) {
		Vector tNames = new Vector();
		Connection con = null;
		try {
			con = connect();

		} catch (Exception ex) {
			System.out.println("unable to connect to lidar database");
			ex.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp
					.append("<tr><td>Unable to connect to the lidar database");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			disconnect(con);
			return tNames;
		}
		// Get all tables within the bounding box.
		tNames = getResidingTableNames(con, MinX, MaxX, MinY, MaxY);
		disconnect(con);
		return tNames;
	}

	private long countAcrossTables(Connection con, Vector tableNames,
			StringBuffer constraint, String download) {
		long count = 0;
		for (int i = 0; i < tableNames.size(); i++) {
			long tmpCount = countQuery(con, constraint.toString(),
					(String) tableNames.get(i));
			if (tmpCount == -1) {
				return -1;
			}
			count += tmpCount;
			// If the user selected some processing and the count is more than
			// the processing limit.
			if (count > PROCESSLIMIT && download.equals("1"))
				return count;
			// If the user selected to just download raw data and the query
			// limit has been reached.
			if (count > QUERYLIMIT)
				return count;
		}
		return count;
	}

	public void DBsetupvars() {
		dbclassname = (String) propsMap.get("dbc.classname");
		dburl = (String) propsMap.get("dbc.url");
		username = (String) propsMap.get("dbc.username");
		password = (String) propsMap.get("dbc.password");
	}

	private Connection connect() throws Exception {
		Connection con = null;
		Class.forName(dbclassname).newInstance();
		con = DriverManager.getConnection(dburl, username, password);
		return con;
	}

	private void disconnect(Connection con) {
		try {
			con.close();
		} catch (Exception ex) {
			con = null;
		}
	}

	private Vector getResidingTableNames(Connection con, String X1, String X2,
			String Y1, String Y2) {
		String query = "select table_name from " + METADATATABLE;
		query += "\nwhere ";
		query += "(((" + X1 + " <= X_MIN) and (X_MIN <= " + X2 + ")) "
				+ "or ((" + X1 + " <= X_MAX) and (X_MAX <= " + X2 + ")) "
				+ "or ((X_MIN <= " + X1 + ") and (" + X2 + " <= X_MAX))) \n"
				+ "and (((" + Y1 + " <= Y_MIN) and (Y_MIN <= " + Y2 + ")) "
				+ "or ((" + Y1 + " <= Y_MAX) and (Y_MAX <= " + Y2 + ")) "
				+ "or ((Y_MIN <= " + Y1 + ") and (" + Y2 + " <= Y_MAX))) ";
		Vector tablesVec = new Vector();
		try {
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				String val = rs.getString(1);
				if (val != null && !val.equals("")) {
					System.out.println("tableName  = " + val);
					tablesVec.add(val);
				}
			}
			rs.close();
			if (tablesVec.size() > 0) {
				return tablesVec;
			}
		} catch (Exception e1) {
			System.out.println("Unable to query lidar database table "
					+ METADATATABLE);
			e1.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to query the lidar database");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			return tablesVec;
		}
		// tablesVec size is empty.
		threadResp.append(header);
		threadResp.append("<tr><td><h2>Empty Query Response!<h2></td></tr>");
		threadResp.append("<tr><td>Querying for ");
		threadResp.append("bounding box selection: MinX = " + X1 + ", MaxX = "
				+ X2 + ", ");
		threadResp.append("MinY = " + Y1 + ", MaxY = " + Y2
				+ " returned no result!</td></tr>");
		threadResp.append(footer);
		return tablesVec;
	}

	public StringBuffer createConstraint(String[] classification, String MinX,
			String MinY, String MaxX, String MaxY) {
		StringBuffer constraint = new StringBuffer();
		constraint.append("WHERE ");
		if (classification != null) {
			if (classification.length > 0 && classification.length < 4) {// something
																			// was
																			// selected
																			// but
																			// not
																			// all
				constraint.append("(CLASSIFICATION = '" + classification[0]
						+ "'");
				for (int i = 1; i < classification.length; i++) {
					constraint.append(" OR CLASSIFICATION = '"
							+ classification[i] + "'");
				}
				constraint.append(") ");
				constraint.append("AND ");
			}
		}
		constraint.append("db2gse.EnvelopesIntersect ( geometry, ");
		constraint.append(MinX + ", " + MinY + ", " + MaxX + ", " + MaxY + ", "
				+ srid + " ) = 1");
		System.out.println("constraint in LidarUtil: " + constraint.toString());
		return constraint;
	}

	private long countQuery(Connection con, String constraint, String tableName) {

		String query = "select count(*) from " + tableName + "\n" + constraint;
		System.out.println("count query ==> " + query);
		try {
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				String val = rs.getString(1);
				if (val == null || val.equals("")) {
					throw new Exception("query value is null");
				} else {
					long count = Long.parseLong(val);
					System.out.println("number of rows per table " + tableName
							+ " ==> " + count);
					return count;
				}
			}
			rs.close();

		} catch (Exception e1) {
			System.out.println("unable to query lidar database table "
					+ tableName);
			e1.printStackTrace();
			threadResp.append(header);
			threadResp.append("<tr><td><h2>Error!<h2></td></tr>");
			threadResp.append("<tr><td>Unable to query the lidar database");
			threadResp.append("</td></tr>");
			threadResp.append(footer);
			return -1;
		}
		return -1;
	}

	public boolean processAccessRequest(HttpServletRequest request,
			String configFile) {

		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		String org = request.getParameter("institution");
		String email = request.getParameter("reqEmail");
		String user = request.getParameter("user");
		String interest = request.getParameter("interest");

		// Add user to Lidar Pending List table.
		LidarJobDB lidarJobDB = new LidarJobDB(configFile);
		boolean added = lidarJobDB.addUser("PENDINGACCESSLIST", user,
				firstName, lastName, org, email);

		String fromAddress = "GLW Support <efrat@geon01.sdsc.edu>";
		String[] toAddress = { "ramon.arrowsmith@asu.edu" };
		String[] ccAddress = { "chris.crosby@asu.edu" };
		String[] bccAddress = { "efrat@sdsc.edu" };
		String subject = "Request to Run LiDAR Jobs";
		String body = "username:     " + user;
		body += "\nfirst name:   " + firstName;
		body += "\nlast name:    " + lastName;
		body += "\norganization: " + org;
		body += "\nemail:        " + email;
		body += "\ninterest:\n" + interest;
		System.out.println(body);
		return sendEmail(fromAddress, toAddress, ccAddress, bccAddress,
				subject, body);
	}

	public boolean approveAccesssRequest(HttpServletRequest request,
			String configFile) {

		String newUser = request.getParameter("newUser");
		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		String org = request.getParameter("institution");
		String email = request.getParameter("reqEmail");

		// Add user to Lidar Access List table.
		LidarJobDB lidarJobDB = new LidarJobDB(configFile);
		boolean added = lidarJobDB.addUser("LIDARACCESSLIST", newUser,
				firstName, lastName, org, email);
		if (!added) {
			threadResp
					.append("<tr><td>Unable to connect to the LiDAR DB to add user "
							+ newUser + " . Please try again later</td></tr>");
			return false;
		}
		threadResp
				.append("<tr><td>User "
						+ newUser
						+ " was successfully added to the LiDAR access list.</td></tr>");
		boolean removed = lidarJobDB.removePendingUser(newUser);
		if (!removed) {
			threadResp
					.append("<tr><td>Unable to remove user from pending access list. Please remove manually.</td></tr>");
		}
		// Email notification to user.
		String fromAddress = "GLW Support <efrat@geon01.sdsc.edu>";
		String[] toAddress = { newUser };
		String[] ccAddress = { "ramon.arrowsmith@asu.edu",
				"chris.crosby@asu.edu" };
		String[] bccAddress = { "efrat@sdsc.edu" };
		String subject = "Request to Run LiDAR Jobs";
		String body = "Dear " + firstName.trim() + ",\n\n";
		body += "Your request to submit LiDAR jobs through the GEON LiDAR Workflow has been approved. \n\n";
		body += "Thank you,\n";
		body += "The LiDAR team\n";
		body += "http://www.geongrid.org";
		System.out.println(body);
		boolean emailSent = sendEmail(fromAddress, toAddress, ccAddress,
				bccAddress, subject, body);
		if (!emailSent) {
			threadResp
					.append("<tr><td>Reason: Unable to send user a nofication email.</td></tr>");
			return false;
		}
		return true;
	}

	protected boolean sendEmail(String fromAddress, String[] toAddress,
			String[] ccAddress, String[] bccAddress, String subject, String body) {
		String host = "localhost";
		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.debug", "false");
		Session session = Session.getInstance(props);
		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(fromAddress));
			InternetAddress[] address;
			if (toAddress != null) {
				address = new InternetAddress[toAddress.length];
				for (int i = 0; i < toAddress.length; i++) {
					address[i] = new InternetAddress(toAddress[i]);
				}
				msg.setRecipients(Message.RecipientType.TO, address);
			}
			if (ccAddress != null) {
				address = new InternetAddress[ccAddress.length];
				for (int i = 0; i < ccAddress.length; i++) {
					address[i] = new InternetAddress(ccAddress[i]);
				}
				msg.setRecipients(Message.RecipientType.CC, address);
			}
			if (bccAddress != null) {
				address = new InternetAddress[bccAddress.length];
				for (int i = 0; i < bccAddress.length; i++) {
					address[i] = new InternetAddress(bccAddress[i]);
				}
				msg.setRecipients(Message.RecipientType.BCC, address);
			}
			msg.setSubject(subject);
			msg.setSentDate(new java.util.Date());
			msg.setText(body);
			Transport.send(msg);
			return true;
		} catch (MessagingException mex) {
			mex.printStackTrace();
			threadResp
					.append("<tr><td>We're sorry, we're unable to process your request at this point. ");
			threadResp.append("Please try again later.</td></tr>");
			return false;
		}
	}

	static public void main(String args[]) {
		String[] classification = { "G" };
		StringBuffer threadResp = new StringBuffer();
		LidarUtilities et = new LidarUtilities(threadResp, "", "", "1005");
		long count = et.calculateNumRows("6101117", "1971306", "6207459",
				"1991991", classification, "1");
		for (int i = 0; i < et.tableNames.size(); i++) {
			System.out.println((String) et.tableNames.get(i));
		}
		System.out.println("number of rows ==> " + count);
	}

	public long estimateTime(long nPoints) {
		double q = queryTime(nPoints);
		double p = processTime(nPoints);
		System.out.println(nPoints + " points, query time " + q
				+ ", process time " + p);
		return Math.round(p + q);
		// return Math.round(queryTime(nPoints) + processTime(nPoints));
	}

	public double processTime(long nPoints) {
		return Math.pow(10, -9) * Math.pow(nPoints, 2) + 0.0003 * nPoints
				+ 50.492;
	}

	public double queryTime(long nPoints) {
		return -8 * Math.pow(10, -12) * Math.pow(nPoints, 2) + 5
				* Math.pow(10, -5) * nPoints + 15.399;
	}

	public Vector tableNames;
}
