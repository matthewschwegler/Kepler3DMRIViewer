/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

//implements WmsdActor, which allows Kepler to control the wmsd system (in wmsd.jar at time of writing), which submits Encyclopedia of Life (EOL) tasks via APST system.
//EOL uses the Grid to predict 3D protein structures on entire genomes from homologous experimentally solved structures in support of rational drug design, ultimately in support of the pharmaceutical industry
//This extremely CPU-intensive process is considered "embarrassingly parallel" and thus an excellent application for the computational grid.
//Modeled protein structures are stored in an on-line, Internet accessible database.
//See more about the Encyclopedia of Life project at SDSC at http://eol.sdsc.edu

package org.eol;

import java.sql.Connection;
import java.util.ArrayList;

import org.geon.DBConnectionToken;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.eol.EolLog;
import edu.sdsc.eol.Wmsd;

/**
 * Description of the Class
 */
public class WmsdActor extends TypedAtomicActor {
	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 * @since
	 */

	// inputs
	public TypedIOPort dbcon = new TypedIOPort(this, "dbcon", true, false); // wmsd

	// bookkeeping
	// database

	public TypedIOPort categoryDatabaseName = new TypedIOPort(this,
			"categoryDatabaseName", true, false);

	public TypedIOPort sourceLabel = new TypedIOPort(this, "sourceLabel", true,
			false);

	// outputs
	public TypedIOPort trigger = new TypedIOPort(this, "trigger", false, true);

	public TypedIOPort output_log = new TypedIOPort(this, "output_log", false,
			true);

	public Parameter createDB = new Parameter(this, "CREATE_DB",
			new BooleanToken(false));

	public Parameter init_apstd = new Parameter(this, "INIT_APSTD",
			new BooleanToken(false));

	public Parameter dbPrefix = new StringParameter(this, "DB_PREFIX");

	public Parameter apstdHostColonPort = new StringParameter(this,
			"APSTD_HOST_COLON_PORT");

	public FileParameter tmp_dir = new FileParameter(this, "TMP_DIR");

	public Parameter scp_user = new StringParameter(this, "SCP_USER");

	public Parameter scp_server = new StringParameter(this, "SCP_SERVER"); // Null

	// OK

	public Parameter scp_invoke_string = new StringParameter(this,
			"SCP_INVOKE_STRING");

	public FileParameter resource_xml_file_name = new FileParameter(this,
			"RESOURCE_XML_FILE_NAME");

	public FileParameter application_xml_file_name = new FileParameter(this,
			"APPLICATION_XML_FILE_NAME");

	public Parameter enough_tasks = new Parameter(this, "ENOUGH_TASKS");

	public Parameter tasks_per_update = new Parameter(this, "TASKS_PER_UPDATE");

	// Until the DBconnect actor is fixed, when we won't need these anymore.
	public Parameter db_jdbc_connection_string = new StringParameter(this,
			"DB_JDBC_CONNECTION_STRING");

	public Parameter db_username = new StringParameter(this, "DB_USERNAME");

	public Parameter db_password = new StringParameter(this, "DB_PASSWORD");

	private String DB_PREFIX;

	private String APSTD_HOST_COLON_PORT, TMP_DIR, SCP_USER, SCP_SERVER,
			SCP_INVOKE_STRING;

	private String RESOURCE_XML_FILE_NAME, APPLICATION_XML_FILE_NAME,
			TASKS_PER_UPDATE, ENOUGH_TASKS;

	// Only needed if DBconnection input isn't provided, which happened during
	// testing because DBconnection was broken, or at least configuration
	// was broken.

	private String DB_JDBC_CONNECTION_STRING, DB_USERNAME, DB_PASSWORD;

	public WmsdActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		// make these booleans radio buttons
		createDB.setTypeEquals(BaseType.BOOLEAN);
		attributeChanged(createDB);

		init_apstd.setTypeEquals(BaseType.BOOLEAN);
		attributeChanged(init_apstd);

		enough_tasks.setTypeEquals(BaseType.INT);
		attributeChanged(enough_tasks);

		tasks_per_update.setTypeEquals(BaseType.INT);
		attributeChanged(tasks_per_update);

		dbcon.setTypeEquals(DBConnectionToken.DBCONNECTION);

		categoryDatabaseName.setTypeEquals(BaseType.STRING);
		sourceLabel.setTypeEquals(BaseType.STRING);

		trigger.setTypeEquals(BaseType.STRING);
		output_log.setTypeEquals(BaseType.STRING);

	}

	/**
	 * initialize
	 * 
	 * @throws IllegalActionException
	 */
	public void initialize() throws IllegalActionException {
		// WGK: this is the way others do it, but one can imagine
		// an object that acts as an array encapsulating Parameter,
		// private String, and string descriptor, and a loop here.
		// More elegant, less verbose, less prone to bugs.

		DB_JDBC_CONNECTION_STRING = dequoteAndLog(db_jdbc_connection_string,
				"DB_JDBC_CONNECTION_STRING");
		DB_USERNAME = dequoteAndLog(db_username, "DB_USERNAME");
		DB_PASSWORD = dequoteAndLog(db_password, "DB_PASSWORD");
		DB_PREFIX = dequoteAndLog(dbPrefix, "DB_PREFIX");
		APSTD_HOST_COLON_PORT = dequoteAndLog(apstdHostColonPort,
				"APSTD_HOST_COLON_PORT");
		SCP_USER = dequoteAndLog(scp_user, "SCP_USER");
		SCP_SERVER = dequoteAndLog(scp_server, "SCP_SERVER");
		SCP_INVOKE_STRING = dequoteAndLog(scp_invoke_string,
				"SCP_INVOKE_STRING");

		if (resource_xml_file_name.getToken() != null) {
			RESOURCE_XML_FILE_NAME = resource_xml_file_name.asFile().toString();
			writeLogLn("RESOURCE_XML_FILE_NAME: " + RESOURCE_XML_FILE_NAME);
		}

		if (application_xml_file_name.getToken() != null) {
			APPLICATION_XML_FILE_NAME = application_xml_file_name.asFile()
					.toString();
			writeLogLn("APPLICATION_XML_FILE_NAME: "
					+ APPLICATION_XML_FILE_NAME);
		}

		if (tmp_dir.getToken() != null) {
			TMP_DIR = tmp_dir.asFile().toString();
			writeLogLn("TMP_DIR: " + TMP_DIR);
		}

		if (enough_tasks.getToken() != null) {
			// Integer type.
			ENOUGH_TASKS = enough_tasks.getToken().toString();
			writeLogLn("ENOUGH_TASKS: " + ENOUGH_TASKS);
		}

		if (tasks_per_update.getToken() != null) {
			// Integer type.
			TASKS_PER_UPDATE = tasks_per_update.getToken().toString();
			writeLogLn("TASKS_PER_UPDATE: " + TASKS_PER_UPDATE);
		}

		if (DB_PREFIX == null || TMP_DIR == null || SCP_USER == null
				|| RESOURCE_XML_FILE_NAME == null
				|| APPLICATION_XML_FILE_NAME == null
				|| SCP_INVOKE_STRING == null || APSTD_HOST_COLON_PORT == null) {
			throw new IllegalActionException(
					"The parameters CREATE_DB, DB_PREFIX, APSTD_HOST_COLON_PORT "
							+ "TMP_DIR, SCP_USER, SCP_INVOKE_STRING, INIT_APSTD must have valid values.  CREATE_DB specifies whether to create bookkeeping database tables must be TRUE or FALSE."
							+ "DB_PREFIX gives the wmsd bookkeeping database table prefixes."
							+ "APST_HOST_COLON_PORT is gives the host and port (separated by a colon)"
							+ "where an apstd is listening. (Typically localhost:6660). SCP_INVOKE_STRING provides the local path and command arguments (e.g. identity file) that will be used to start scp. SCP_USER gives the username to be used to scp/ssh login on the apstd host. SCP_SERVER, optional, gives the SCP server when it is different from the apstd connection (due to, say, ssh port forwarding as a result of a firewall.) INIT_APSTD must be TRUE or FALSE, and specifies whether to clear the Apstd and load in the resource file. TMP_DIR gives the name of a temporary directory on the local machine (required)."
							+ "RESOURCE_XML_FILE_NAME must be set to the local location of the XML computation resources file in APSTD format. APPLICATION_XML_FILE_NAME species in the XML file (in WMSD pseudo-Apstd task format) that gives the per-protein and per-genome tasks to be submitted, with variable substitution. ENOUGH_TASKS (optional) specifies the maximum number of tasks apst is allowed to run at any given time --- set larger than cpus in the cluster. TASKS_PER_UPDATE (optional) specifies the max submitted to apstd at any given time. Please provide these values and re-execute.");
		}

	}

	/**
	 * @return Description of the Returned Value
	 * @exception IllegalActionException
	 *                Description of Exception
	 * @since
	 */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
	 * Send a random number with a uniform distribution to the output. This
	 * number is only changed in the prefire() method, so it will remain
	 * constant throughout an iteration.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 * @since
	 */
	public void fire() throws IllegalActionException {
		writeLogLn("firing Wmsd Actor");
		super.fire();

		// convert inputs to string types

		String categoryDatabaseNameStr = ((StringToken) categoryDatabaseName
				.get(0)).stringValue();
		String sourceLabelStr = ((StringToken) sourceLabel.get(0))
				.stringValue();

		ArrayList atemp = new ArrayList();

		// String createDbOption = "--create_db";

		String[] argsTemp = { "--resources", RESOURCE_XML_FILE_NAME,
				"--applications", APPLICATION_XML_FILE_NAME, "--apstcontact",
				APSTD_HOST_COLON_PORT, "--category", categoryDatabaseNameStr,
				"--source", sourceLabelStr, "--dbprefix", DB_PREFIX,
				"--tmpdir", TMP_DIR, "--scp_user", SCP_USER,
				"--scp_invoke_string", SCP_INVOKE_STRING, };
		// convert to ArrayList because Java doesn't support dynamic array sizes
		for (int i = 0; i < argsTemp.length; i++) {
			atemp.add(argsTemp[i]);
		}

		if (((BooleanToken) createDB.getToken()).booleanValue() == true) {
			// we've already tested that this is either true or false.
			// if it is true, the default value above is used.
			atemp.add("--create_db");
		}

		if (((BooleanToken) init_apstd.getToken()).booleanValue() == true) {
			// we've already tested that this is either true or false.
			// if it is true, the default value above is used.
			atemp.add("--init_apstd");
		}

		if (SCP_SERVER != null && !SCP_SERVER.equals("")) {
			atemp.add("--scp_server");
			atemp.add(SCP_SERVER);
		}

		if (ENOUGH_TASKS != null) {
			ENOUGH_TASKS = (new Integer(ENOUGH_TASKS)).toString(); // make sure
			// integer
			// value.
			atemp.add("--enough_tasks");
			atemp.add(ENOUGH_TASKS);
		}

		if (TASKS_PER_UPDATE != null) {
			TASKS_PER_UPDATE = (new Integer(TASKS_PER_UPDATE)).toString(); // make
			// sure
			// integer
			// value.
			atemp.add("--tasks_per_update");
			atemp.add(TASKS_PER_UPDATE);
		}

		try {
			Wmsd wm = null;
			if (dbcon.numberOfSources() > 0 && dbcon.hasToken(0)) {
				String[] args = (String[]) atemp.toArray(new String[atemp
						.size()]);

				DBConnectionToken _dbcon = (DBConnectionToken) dbcon.get(0);
				Connection _con;
				try {
					_con = _dbcon.getValue();

				} catch (Exception e) {
					throw new IllegalActionException(this, e,
							"CONNECTION FAILURE");
				}

				wm = new Wmsd(args, _con);

			} else {
				// DB connect broken, user putting in db parameters manually.
				// :-(
				// this stuff will be eliminated once DBconnect actor is again
				// working reliably.

				if (DB_JDBC_CONNECTION_STRING == null || DB_USERNAME == null
						|| DB_PASSWORD == null) {
					throw new IllegalActionException(
							"In the absence of an DBconnect input, DB_JDBC_CONNECTION_STRING, DB_USERNAME, DB_PASSWORD must not be null but provide the db connection parameters for the Wmsd bookkeeping database.");
				}
				atemp.add("--dbhost");
				atemp.add(DB_JDBC_CONNECTION_STRING);
				atemp.add("--dbuser");
				atemp.add(DB_USERNAME);
				atemp.add("--dbpassword");
				atemp.add(DB_PASSWORD);

				String[] args = (String[]) atemp.toArray(new String[atemp
						.size()]);
				wm = new Wmsd(args);
			}
			wm.run();

		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalActionException("Unexpected error: "
					+ e.getMessage());
		}

		copyToLog(EolLog.getAndClearOutData());
		broadcastToLog();

		trigger.broadcast(new StringToken(categoryDatabaseNameStr));
		writeLogLn("Wmsd Actor done");

	}

	private void writeLogLn(String msg) {
		System.out.println(msg);
		copyToLog(msg + "\n");
	}

	private String logString = null;

	private void copyToLog(String msg) {
		if (logString == null) {
			logString = new String();
		}
		logString += msg;
	}

	private void broadcastToLog() {
		if (logString != null) {
			try {
				output_log.broadcast(new StringToken(logString));
			} catch (IllegalActionException e) {
				e.printStackTrace();
			}
		}
	}

	private String dequote(String mystring) {
		return (mystring.substring(1, mystring.length() - 1));
	}

	private String dequoteAndLog(String mystring, String name) {
		mystring = dequote(mystring);
		writeLogLn(name + ": " + mystring);
		return (mystring);
	}

	private String dequoteAndLog(Parameter myparm, String name)
			throws IllegalActionException {
		if (myparm.getToken() != null) {
			// only fix and print if not equal to null.
			String mystring = ((StringToken) myparm.getToken()).toString();
			return (dequoteAndLog(mystring, name));
		}
		return (null);
	}

}