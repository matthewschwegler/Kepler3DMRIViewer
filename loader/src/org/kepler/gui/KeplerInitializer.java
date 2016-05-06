/*
 * Copyright (c) 2006-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-11 14:58:30 -0700 (Wed, 11 Jul 2012) $' 
 * '$Revision: 30174 $'
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

package org.kepler.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.seek.ecogrid.SearchRegistryAction;
import org.geon.DBConnectionToken;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.util.AuthNamespace;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.sql.DatabaseFactory;

import ptolemy.actor.gui.Configuration;
import ptolemy.data.expr.Constants;
import ptolemy.gui.Top;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.MoMLParser;
import ptolemy.util.StringUtilities;

//////////////////////////////////////////////////////////////////////////
//// KeplerInitializer

/**
 * Initialize Kepler.
 * 
 * <p>
 * How this works is that configs/ptolemy/configs/kepler/configuration.xml sets
 * a StringParameter called "_applicationInitializer" that names this class.
 * KeplerApplication reads the parameter and instantiates this class.
 * 
 * @author Kevin Ruland
 * @version $Id: KeplerInitializer.java 30174 2012-07-11 21:58:30Z crawl $
 * @since Ptolemy II 6.0
 * @Pt.ProposedRating Red (cxh)
 * @Pt.AcceptedRating Red (cxh)
 */
public class KeplerInitializer {

	private static Log log = LogFactory
			.getLog("org.kepler.gui.KeplerInitialization");
	private static boolean hasBeenInitialized = false;

	// redirects standard out and err streams
	private static boolean log_file = false;

	private static boolean autoUpdate;

	private static int autoUpdateDelay;

	/** Perform any Kepler specific initialization. */
	public KeplerInitializer() throws Exception {
		initializeSystem();
	}

	public static void initializeSystem() throws Exception {
		System.out.println("Kepler Initializing...");
		// Only run initialization once.
		if (hasBeenInitialized) {
			return;
		}

		hasBeenInitialized = true;

		ConfigurationProperty commonProperty = ConfigurationManager
				.getInstance().getProperty(
						ConfigurationManager.getModule("common"));

		autoUpdate = Boolean.parseBoolean(commonProperty.getProperty(
				"autoDataSourcesUpdate.value").getValue());
		autoUpdateDelay = Integer.parseInt(commonProperty.getProperty(
				"autoDataSourcesUpdate.delay").getValue());

		// Add the dbconnection type.
		Constants.add("dbconnection", new DBConnectionToken());

		DotKeplerManager dkm = DotKeplerManager.getInstance();

		// String log_file_setting = c.getValue("//log_file");
		String log_file_setting = commonProperty.getProperty("log_file")
				.getValue();

		if (log_file_setting != null) {
			if (log_file_setting.equalsIgnoreCase("true")) {
				log_file = true;
			} else {
				log_file = false;
			}
		}
		if (log_file) {
			try {
				FileOutputStream err = new FileOutputStream("kepler_stderr.log");
				PrintStream errPrintStream = new PrintStream(err);
				System.setErr(errPrintStream);
				System.setOut(errPrintStream);
			} catch (FileNotFoundException fnfe) {
				System.out
						.println("Warning: Failure to redirect log to a file.");
			}
		}

		// First get the entries named mkdir. We will make
		// directories for each of these entries under the UserDir.

		List mkdirList = commonProperty.getProperties("startup");
		List mkdirs = ConfigurationProperty.getValueList(mkdirList, "mkdir",
				true);

		for (Iterator i = mkdirs.iterator(); i.hasNext();) {
			String dir = (String) i.next();
			if (dir == null || dir.length() == 0) {
				continue;
			}

			dir = dkm.getCacheDir(dir);

			File f = new File(dir);
			if (!f.exists()) {
				boolean created = f.mkdirs();
				if (created) {
					log.debug("Making directory " + dir);
				}
			}
		}

		Connection conn = DatabaseFactory.getDBConnection();
		
		//
		// Get the tabletestsql entry. This is the sql used to test if a
		// table already exists in the database.
		//
		String tabletestsql = commonProperty
				.getProperty("startup.tabletestsql").getValue();
		PreparedStatement tabletest = null;
		if (tabletestsql != null) {
			tabletest = conn.prepareStatement(tabletestsql);
		}

		// We use pattern matching to extract the tablename from the
		// ddl statement. This is a Java 1.4 regex regular expression
		// which extracts the word after the string "table". The
		// match is case insensitive so "table" will also match
		// "TABLE". The "(" ")" characters denote a grouping. This
		// will be returned as group 1.

		Pattern extractTable = Pattern.compile("table\\s+(\\w*)",
				Pattern.CASE_INSENSITIVE);

		// Get the list of ddl statements defining the tables to create.
		List createTableList = commonProperty.getProperties("startup");
		List createtables = ConfigurationProperty.getValueList(createTableList,
				"createtable", true);

		final String schemaName = CacheManager.getDatabaseSchemaName();
		
		for (Iterator i = createtables.iterator(); i.hasNext();) {
			String ddl = (String) i.next();
			// Create our Matcher object for the ddl string.
			Matcher m = extractTable.matcher(ddl);
			// Matcher.find() looks for any match of the pattern in the string.
			m.find();

			String tablename = m.group(1);
			if (tabletest == null) {
				log.error("unable to test for table: " + tablename);
				continue;
			}

			tabletest.setString(1, tablename);
			tabletest.setString(2, schemaName);
			ResultSet rs = tabletest.executeQuery();
			if (rs.next()) {
				log.debug("Table " + tablename + " already exists");
				continue;
			}
			Statement statement = conn.createStatement();
			statement.execute(ddl);
			statement.close();
			log.debug("Table " + tablename + " created");
		}
		tabletest.close();

		// Get the Authorized Namespace after the tables are set up.
		AuthNamespace an = AuthNamespace.getInstance();
		an.initialize();

		// Hook to execute arbitrary sql statements on the internal database.
		// List sqls = c.getList("//startup/sql");
		List sqlList = commonProperty.getProperties("startup");
		List sqls = ConfigurationProperty.getValueList(sqlList, "sql", true);
		Statement statement = null;
		for (Iterator i = sqls.iterator(); i.hasNext();) {
			String statementStr = (String) i.next();
			log.info("Executing sql command " + statementStr);
			statement = conn.createStatement();
			statement.execute(statementStr);			
		}
		if (statement != null)
			statement.close();
		conn.close();

		// Set the icon loader to load Kepler files
		MoMLParser.setIconLoader(new KeplerIconLoader());

		// set the initial directory for file dialogs
		setDefaultFileDialogDir();

		// refresh the datasources from the registry
		updateDataSources();
	}

	private static void updateDataSources() {
		if (autoUpdate) {
			Executors.newSingleThreadScheduledExecutor().schedule(
					new Runnable() {
						public void run() {
							SearchRegistryAction.queryRegistryRewriteConfig();
						}
					}, autoUpdateDelay, TimeUnit.SECONDS);
		}
	}

	/** Set the default directory used for open and save dialogs. Use
	 *  _alternateDefaultOpenDirectory is set. Otherwise, use the
	 *  default local save repository (MyWorkflows).
	 * 
	 */
	private static void setDefaultFileDialogDir() {
		boolean wasSet = false;

		String currentWorkingDirectory;
		List configsList = Configuration.configurations();
		Configuration config = null;
		Object object = null;
		for (Iterator it = configsList.iterator(); it.hasNext();) {
			config = (Configuration) it.next();
			if (config != null) {
				break;
			}
		}

		// see if _alternateDefaultOpenDirectory is set

		if (config != null) {
			StringAttribute alternateDefaultOpenDirAttribute = (StringAttribute) config
					.getAttribute("_alternateDefaultOpenDirectory");
			if (alternateDefaultOpenDirAttribute != null) {
				final String altDirStr = alternateDefaultOpenDirAttribute
						.getExpression();
				final File altDirFile = new File(altDirStr);

				// see if the alternate directory is absolute and exists
				if(altDirFile.isAbsolute() && altDirFile.isDirectory()) {
					Top.setDirectory(altDirFile);
					wasSet = true;
				} else if(!altDirFile.isAbsolute()) {
				
					// it was not absolute, so put prepend the Kepler directory
					currentWorkingDirectory = StringUtilities
						.getProperty("KEPLER");
					if (currentWorkingDirectory != null) {
						File dir = new File(currentWorkingDirectory,
								altDirStr);
						if (dir != null && dir.isDirectory()) {
							Top.setDirectory(dir);
							wasSet = true;
						}
					}
				}
			}
		}

		// if we did set it, use the default save repository
		if (!wasSet) {
			File saveRepo = LocalRepositoryManager.getInstance().getSaveRepository();
			if (saveRepo != null && saveRepo.isDirectory()){
				Top.setDirectory(saveRepo);
			}
		}
	}
}