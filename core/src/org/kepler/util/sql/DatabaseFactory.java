/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-05-29 09:28:49 -0700 (Wed, 29 May 2013) $' 
 * '$Revision: 32095 $'
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

package org.kepler.util.sql;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.util.Version;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.configuration.ConfigurationUtilities;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.util.DotKeplerManager;

import ptolemy.util.MessageHandler;

/** 
 *
 * A factory to instantiate and enumerate database types.
 *
 * @author Daniel Crawl
 * @version $Id: DatabaseFactory.java 32095 2013-05-29 16:28:49Z crawl $
 *
 */
    
public class DatabaseFactory
{    
    /** Convenience method to get connection to default kepler DB,
     * which is currently HSQL.
     * 
     * FIXME this method should be deleted 
     * 
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static Connection getDBConnection() 
        throws SQLException, ClassNotFoundException
    {

        ConfigurationProperty commonProperty = ConfigurationManager
                .getInstance().getProperty(
                        ConfigurationManager.getModule("common"));
        List<ConfigurationProperty> hsqlList = commonProperty.findProperties(
                "sqlEngineName", "hsql", true);
        ConfigurationProperty hsqlProp = (ConfigurationProperty) hsqlList
                .get(0);

        String username = hsqlProp.getProperty("userName").getValue();
        String password = hsqlProp.getProperty("password").getValue();
        String url = hsqlProp.getProperty("url").getValue();
        String dbPort = hsqlProp.getProperty("port").getValue();
        
        String dbAlias = hsqlProp.getProperty("dbName").getValue();
        
        DatabaseType dbType = getType("HSQL");
        
        String dbName = dbAlias;
        
        if(dbType.needAbsolutePathForName() &&
            !dbAlias.startsWith("/"))
        {
        	dbName = DotKeplerManager.getInstance().getCacheDirString()
                + "cachedata" + File.separator + dbAlias;
        }
        
        String fullUrl = null;
        
        if (url != null && url.endsWith("/")) 
        {
            fullUrl = url.substring(0, url.length()-1);
        }
        else
        {
            fullUrl = url;
        }

        String randomPort;
        try
        {
            // NOTE: the third argument should be the hostname
            // we use the url since there is not a separate entry
            // for host name in the configuration file.
            // the url may specify to directly access the file, but
            // _getRandomPort will chose a random port. this isn't
            // too bad since the port will not actually be used
            // (since it's configured to directly access the file).
            randomPort = dbType._getPort(dbPort, dbName, url);
        }
        catch (IOException e)
        {
            throw new SQLException("Error chosing random port.", e);
        }
        
        if (randomPort != null && fullUrl != null)
        {
            fullUrl = fullUrl.concat(":" + randomPort);
        }
        
        if(fullUrl != null)
        {
            fullUrl = fullUrl.concat("/" + dbAlias + ";filepath=hsqldb:file:" + dbName);
        }
        
        // if the url is null, then set the url to directly access the file
        // instead of starting the database server.
        if(fullUrl == null)
        {
            fullUrl = "jdbc:hsqldb:file:" + dbName;
        }
        
        // must load the driver
        try {
            Class.forName(dbType._getDriverName()).newInstance();
        } catch (InstantiationException e) {
            throw new SQLException("Error loading HSQL driver: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SQLException("Error loading HSQL driver: " + e.getMessage());
        }
        
        //System.out.println("fullUrl = " + fullUrl);
        
        Connection conn = dbType._getConnection(fullUrl, randomPort, 
                dbName, username, password);
        
        // set the schema name;
        final String schemaName = CacheManager.getDatabaseSchemaName();
        Statement statement = null;
        try {
            statement = conn.createStatement();
            // try to set the schema
            try {
                statement.execute("SET SCHEMA " + schemaName);
            } catch(SQLException e) {
                // see if the exception looks like this schema does not exist
                if(e.getMessage() != null && e.getMessage().startsWith("invalid schema name:")) {
                    // create the schema and set it
                    statement.execute("CREATE SCHEMA " + schemaName + " AUTHORIZATION DBA");
                    statement.execute("SET SCHEMA " + schemaName);                    
                } else {
                    throw e;
                }
            }
        } finally {
            if(statement != null) {
                statement.close();
            }
        }

        return conn;
    }
    
    /** Connect to a database from a ConfigurationProperty. */
    public static DatabaseType getConnectedDatabaseType(
        ConfigurationProperty dbProperty) throws SQLException
    {
        // get a map of the parameters
        Map<String,String> map = ConfigurationUtilities.getPairsMap(dbProperty);   
        return getConnectedDatabaseType(map, dbProperty.getModule().getStemName());
    }
    
    /** Get a connected database from connection parameters. */
    public static DatabaseType getConnectedDatabaseType(
        Map<String,String> parameters, String moduleName) throws SQLException
    {
        // get the type
        String typeStr = parameters.get(Parameter.TYPE.getName());
        if(typeStr == null)
        {
            new Exception().printStackTrace(System.out);
            throw new SQLException("Could not find the " +
                Parameter.TYPE.getName() + " property.");
        }
        
        DatabaseType dbType = getType(typeStr);
        if(dbType == null)
        {
            throw new SQLException("Unsupported type of database: " +
                    typeStr);
        }
        
        Map<String,String> connectParameters = parameters;

        // see if a jdbc url was specified
        String jdbcURLStr = parameters.get(Parameter.JDBC_URL.getName());        
        if(jdbcURLStr == null || jdbcURLStr.length() == 0)
        {        	
            // check path name
            final String dbName = parameters.get(Parameter.NAME.getName());
    
            if(dbType.needAbsolutePathForName())
            {
                File dbFileName = new File(dbName);
                // see if it's a relative path
                if(!dbFileName.isAbsolute())
                {
                	String dbPathStr;
                	
                    // prepend the absolute path to the persistent module
                    // directory for owning module

                    ModuleTree modules = ModuleTree.instance();
                    Module module = modules.getModuleByStemName(moduleName);
                    
                    // make sure we found the module.
                    if(module == null)
                    {
                        throw new SQLException("Could not find module: " +
                            moduleName + " Is it in modules.txt?");
                    }

                    // see if we are running a released module
                    if(Version.isVersioned(module.getName()))
                    {
                        Version version = Version.fromVersionString(module.getName());
                        System.out.println("version = " + version.getMajor() + "." + version.getMinor());
                    
                        dbPathStr = DotKeplerManager.getInstance().
                        	getPersistentModuleDirectory(moduleName) +
                        	File.separator + "db-" +
                        	version.getMajor() + "." + version.getMinor() +
                        	File.separator + dbName;
                        
                        // see if it exists
                    	String dbFullName = dbPathStr + "." + dbType.getPrimaryFileExtension();
                        dbFileName = new File(dbFullName);
                        if(!dbFileName.exists())
                        {
                        	System.out.println("missing db for " + dbPathStr);
                        	
                        	// find previous version, if any
                        	String previousNameStr = _findPreviousVersion(module, 
                        		dbName + "." + dbType.getPrimaryFileExtension());
                        	
                        	// see if there was a previous version
                        	if(previousNameStr != null)
                        	{
                        		// upgrade
                        		System.out.println("going to copy previous version in " + previousNameStr);
                        		File oldDir = new File(DotKeplerManager.getInstance().
                        			getPersistentModuleDirectory(moduleName) +
                        			File.separator + previousNameStr);
                        		
                        		// determine the files to copy
                        		String[] filesToCopy = oldDir.list(new FilenameFilter() {
									public boolean accept(File dir, String name) {
										return name.startsWith(dbName);
									}     			
                        		});
                        		
                        		File dbDestDir = new File(dbPathStr).getParentFile();
                        		// make the destination directory if it does not exist.
                        		if(!dbDestDir.exists())
                        		{
                        			dbDestDir.mkdir();
                        		}
                        		
                        		// copy the files
                        		for(String fileStr : filesToCopy)
                        		{
                        			File srcFile = new File(oldDir, fileStr);
                        			File destFile = new File(dbDestDir, fileStr);
                        			System.out.println("going to copy " + srcFile + " to " + destFile);
                        			Copy copy = new Copy();
                        			copy.setFile(srcFile);
                        			copy.setTofile(destFile);
                        			copy.execute();
                        		}
                        	}                        	
                        }
                    }
                    else
                    {
                        // trunk
                        //System.out.println("on trunk.");
                        
                        dbPathStr = DotKeplerManager.getInstance().
                        	getPersistentModuleDirectory(moduleName)
                        	+ File.separator + dbName;
                    }
                                        
                    // make a copy and replace the name.
                    connectParameters = new HashMap<String,String>(parameters);
                    connectParameters.put(Parameter.NAME.getName(), dbPathStr);
                }
            }
        }

        // make the connection
        dbType.connect(connectParameters);            

        return dbType;
    }

    /** Get the DatabaseType from a name. */
    public static DatabaseType getType(String name)
    {
        DatabaseType retval = null;

        if(name.equals("MySQL"))
        {
            retval =  new MySQL();
        }
        else if(name.equals("HSQL"))
        {
            retval =  new HSQL();
        }
        else if(name.equals("PostgreSQL"))
        {
            retval =  new PostgreSQL();
        }
        else if(name.equals("Oracle"))
        {
            retval =  new Oracle();
        }
        return retval;
    }
    
    /** Get all the type names. */
    public static List<String> getNames()
    {
        List<String> names = new LinkedList<String>();
        names.add("MySQL");
        names.add("HSQL");
        names.add("PostgreSQL");
        names.add("Oracle");
        return names;
    }

    /** Shut down the server for the kepler cache database. */
    public static void shutdownCacheServer() {
        Connection connection = null;
        try {
            connection = getDBConnection();
        } catch (Exception e) {
            MessageHandler.error("Error stopping .kepler cache database.", e);
            return;
        }
        
        try {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute("SHUTDOWN");
            } finally {
                if(statement != null) {
                    statement.close();
                }
            }
        } catch (SQLException e) {
            MessageHandler.error("Error stopping .kepler cache database.", e);
        }
        
        // read the database name from the configuration
        
        ConfigurationProperty commonProperty = ConfigurationManager
                .getInstance().getProperty(
                        ConfigurationManager.getModule("common"));
        List<ConfigurationProperty> hsqlList = commonProperty.findProperties(
                "sqlEngineName", "hsql", true);
        ConfigurationProperty hsqlProp = (ConfigurationProperty) hsqlList
                .get(0);
        
        String dbName = hsqlProp.getProperty("dbName").getValue();
        
        DatabaseType dbType = getType("HSQL");
        
        // get the database path
        
        if(dbType.needAbsolutePathForName() &&
            !dbName.startsWith("/"))
        {
            dbName = DotKeplerManager.getInstance().getCacheDirString()
                + "cachedata" + File.separator + dbName;
        }
        
        // remove the port file
        HSQL._deletePortFile(dbName);
        
    }

    public enum Parameter
    {
        /** User name of database connection. */
        USER("DB User Name"),
          
        /** Password for database connection. */
        PASSWD("Password"),
          
        /** Database host. */
        HOST("DB Host"),
          
        /** Name of database (i.e., schema or sid). */
        NAME("DB Name"),
          
        /** Type of database. */
        TYPE("DB Type"),
    
        /** Port of database. */
        PORT("DB Port"),

        /** Name of table prefix. */
        TABLEPREFIX("DB Table Prefix"),
        
        /** JDBC URL. This is optional; use to override other parameters. */
        JDBC_URL("JDBC URL"),
        
        /** If false, do not create any indexes in schema.
         *  NOTE: this has only been tested for MySQL.
         */
        CREATE_INDEXES("Create Indexes");

          
        Parameter(String name)
        {
            _name = name;
        }
          
        /** Get the name of the parameter. */
        public String getName()
        {
            return _name;
        }
          
        /** Find the Parameter from a name. */
        public static Parameter getType(String name)
        {
            for(Parameter parameter : values())
            {
                if(parameter.getName().equals(name))
                {
                    return parameter;
                }
            }
            return null;
        }
          
        private String _name;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                      private methods                    //////

    /** Returns the directory containing the least oldest version of the
     *  database. If no such directory exists, returns null. 
     */
    private static String _findPreviousVersion(Module module,
        final String dbName)
    {
    	File moduleDir = DotKeplerManager.getInstance().getPersistentModuleDirectory(module.getStemName());
    	
    	// get a list of possible directories
    	String[] choices = moduleDir.list(new FilenameFilter() {
    		public boolean accept(File dir, String name) {
    			//System.out.println("checking " + name);
    			// see if the name matches the pattern
    			if(_DB_PATTERN.matcher(name).matches()) {
    				File dbFile = new File(dir.getAbsolutePath() + 
    					File.separator + name + File.separator + dbName);
    				// make sure the primary database file exists.
    				return dbFile.exists();
    			}
    			return false;
    		}
    	});
    	Arrays.sort(choices);
    	
    	// find the closest version
    	String closestDir = null;
    	String moduleVersion = "db-" + 
    	    Version.fromVersionString(module.getName()).getMajor() + "." +
    	    Version.fromVersionString(module.getName()).getMinor();
    	
    	for(String choice : choices)
    	{
    		if(moduleVersion.compareTo(choice) > 0)
    		{
    			//System.out.println(choice + " is before " + moduleVersion);
    			closestDir = _compareTwoVersions(moduleVersion, closestDir, choice);
    		}
    	}
    	
    	//System.out.println("closest dir is " + closestDir);
    	
    	return closestDir;
    }
    
    /** Check if a version is closer to another version.
     * @param dbName The string against which to test.
     * @param closest The currently nearest version to dbName.
     * @param choice A version string to test.
     * @return If closest has the nearest version to dbName, returns closest, otherwise
     * returns choice.
     */
    private static String _compareTwoVersions(String dbName, String closest,
        String choice)
    {
    	String retval = null;
    	
    	// if there is no currently nearest value, select choice
    	if(closest == null)
    	{
    		retval = choice;
    	}
    	else
    	{
    		// compute the version sum of each
    		
    		Matcher dbMatcher = _DB_PATTERN.matcher(dbName);
    		dbMatcher.matches();
    		int dbSum = _computeVersionSum(Integer.valueOf(dbMatcher.group(1)),
    			Integer.valueOf(dbMatcher.group(2)));
    		
    		Matcher closestMatcher = _DB_PATTERN.matcher(closest);
    		closestMatcher.matches();
    		int closestSum = _computeVersionSum(Integer.valueOf(closestMatcher.group(1)),
    			Integer.valueOf(closestMatcher.group(2)));

    		Matcher choiceMatcher = _DB_PATTERN.matcher(choice);
    		choiceMatcher.matches();
    		int choiceSum = _computeVersionSum(Integer.valueOf(choiceMatcher.group(1)),
    			Integer.valueOf(choiceMatcher.group(2)));
    		
    		// see which sum is smaller
    		if(dbSum - choiceSum < dbSum - closestSum)
    		{
    			retval = choice;
    		}
    		else
    		{
    			retval = closest;
    		}

    	}
    	
    	return retval;
    }
    
    /** Compute a version sum from major and minor numbers. */
    private static int _computeVersionSum(int major, int minor)
    {
    	return (major * 10000) + minor;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                       private variables                   ////
    
    /** Pattern of directory name containing database. */
    private final static Pattern _DB_PATTERN = Pattern.compile("db\\-(\\d+)\\.(\\d+)");
}
