/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-05-15 17:22:28 -0700 (Thu, 15 May 2014) $' 
 * '$Revision: 32720 $'
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.Server;
import org.hsqldb.ServerConstants;

import ptolemy.util.MessageHandler;

/**
 *
 * An implementation of DatabaseType for HSQL.
 *
 * @author Daniel Crawl
 * @version $Id: HSQL.java 32720 2014-05-16 00:22:28Z crawl $
 *
 */
    
public class HSQL extends DatabaseType
{
    
    /** Only this package (DatabaseFactory) can instantiate. */
    protected HSQL()
    {
        super();
    }

    /** Close a JDBC connection. */
    @Override
    public void disconnect() throws SQLException
    {
        if(_connection != null)
        {   
            // NOTE: set the write delay to flush any pending updates to disk.
            // if this is not done, data could be lost. 
            // see:
            // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5410
            // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4325           
            //
            _executeSQL(_SQL_SET_WRITE_DELAY);

            synchronized(_urlMapLock)
            {
                String urlStr = _connectionToURLMap.remove(_connection);
                
                // see if the url was in the map. only connections that access
                // the db files directly are added to the map.
                if(urlStr != null)
                {
                    // it accesses the file directly
                    
                    // see what the ref count is
                    Integer count = _urlToCountMap.remove(urlStr);
                    
                    if(_isDebugging)
                    {
                        _log.debug("ref count is " + count + " for " + urlStr);
                    }
                    
                    if(count == 1)
                    {
                        if(_isDebugging)
                        {
                            _log.debug("shutting down for " + urlStr);
                        }
                        _executeSQL("SHUTDOWN");
                        
                        // remove the port file
                        
                        // make sure the dbName is set
                        if(_dbName != null)
                        {
                            _deletePortFile(_dbName);
                        }                            
                        
                    }
                    else
                    {
                        // decrement ref count
                        count--;
                        _urlToCountMap.put(urlStr, count);
                    }
                }
            }

            if(_isDebugging)
            {
                _log.debug("closed connection " + _connection);
            }
        }
        
        super.disconnect();
    }
    
    /** Get the port for a database name. If dbPort is outside the range of
     *  ports previously used for Kepler (9001-9010), then dbPort is returned.
     *  If the port file exists in the database directory, then the port in
     *  this file is returned. Otherwise, this method chooses a random port,
     *  writes it to the port file, and returns the value. The port file is
     *  removed when the server is shut down either in shutdownServers(), or
     *  disconnect().
     */
    @Override
    protected synchronized String _getPort(String dbPort, String dbName, String hostName) throws IOException
    {
        
        boolean choseRandom = false;

        // make sure the host name is set
        if (hostName != null && !hostName.trim().isEmpty()) 
        {
            // check if db port is not null, and in 9001-9010
            if(dbPort == null || dbPort.trim().isEmpty())
            {
                choseRandom = true;
            }
            else
            {
                final int port = Integer.valueOf(dbPort);
                if(port >= 9001 && port <= 9010)
                {
                    choseRandom = true;
                }
            }
        }
        
        if(choseRandom) {
            
            dbPort = null;
            
            // see if port file exists for this database
            final File portFile = new File(dbName + _PORT_FILE_EXTENSION);
            if(portFile.exists())
            {
                final Properties properties = _readPropertiesFile(portFile);
                dbPort = properties.getProperty(_PORT_FILE_PASSWORD_PROP_NAME);
            }
            
            // see if the port file existed and we're already using this port.
            // NOTE: by keeping a set of ports we've randomly chosen and checking
            // it here, we can handle the (unlikely) case where the same port is
            // in more than one port file.
            if(dbPort != null)
            {
                String existingDbName = _serverPortToName.get(dbPort);
                // see if the port is associated with a database
                if(existingDbName == null)
                {
                    // add the mapping
                    _serverPortToName.put(dbPort, dbName);
                }
                // see if the port is associated with a different database
                else if(!dbName.equals(existingDbName))
                {
                    // set to null so we chose a different port.
                    dbPort = null;
                }
            }
                    
            
            if(dbPort == null)
            {
                int port = -1;
                int tries = 10;
                
                while(tries > 0) {
                    // pick random port between 10,000 and 60,000
                    port = _random.nextInt(50000) + 10000;
                                                            
                    // see the port is in use
                    final Socket socket = new Socket();
                    final InetSocketAddress addr = new InetSocketAddress("localhost", port);
                    try
                    {
                        socket.connect(addr);
                        socket.close();
                        //System.out.println("port " + port + " already in use. tries = " + tries);
                        tries--;
                    }
                    catch(IOException e)
                    {
                        // connection failed
                        //System.out.println("port " + port + " not in use.");
                        break;
                    }
                }
                
                if(tries == 0)
                {
                    throw new IOException("Could not choose random port for HSQL server.");
                }

                dbPort = String.valueOf(port);

                _serverPortToName.put(dbPort, dbName);
                
                // write to port file
                final Properties properties = new Properties();
                properties.put(_PORT_FILE_PASSWORD_PROP_NAME, dbPort);
                _writePropertiesFile(portFile, properties);                
            }
        }
        return dbPort;
        
    }
                
    
    /** Get the string used in SQL statements to modify the
     *  data type of a column.
     */
    @Override
    public String getColumnAlterStr()
    {
        return "ALTER";
    }

    /** Get the name of the type. */
    @Override
    public String getName()
    {
        return "HSQL";
    }

    /** Get the primary file extension of the database. If the database
     *  is not file-based, this returns null.
     */
    @Override
    public String getPrimaryFileExtension()
    {
    	return "data";
    }

    /** Returns true if database name should be an absolute path in the
     *  file system.
     */
    @Override
    public boolean needAbsolutePathForName()
    {
        return true;
    }

    /** Returns true if need host name for connect. */
    @Override
    public boolean needHostForConnect()
    {
        return false;
    }

    /** Returns true if need password for connect. */
    @Override
    public boolean needPasswordForConnect()
    {
        return false;
    }

    /** Returns true if need user name for connect. */
    @Override
    public boolean needUserForConnect()
    {
        return false;
    }
    
    /** Rename a column. */
    @Override
    public void renameColumn(String oldName, Column newColumn, String tableName) throws SQLException
    {
        String newName = getColumnName(newColumn.getName());
        String sqlStr = "ALTER TABLE " + getTableName(tableName) +
            " ALTER COLUMN " + getColumnName(oldName) + " RENAME TO " + newName;
        _executeSQL(sqlStr);
    }
    
    /** Set not null constraint to a column. */
    @Override
    public void setColumnNotNull(Column column, String tableName) throws SQLException
    {
        String sqlStr = "ALTER TABLE " + getTableName(tableName) +
            " ALTER COLUMN " + getColumnName(column.getName()) + " SET NOT NULL";
        _executeSQL(sqlStr);
    }

    /** Set whether HSQL servers are run in a separate process. */
    public static void setForkServers(boolean fork)
    {
        _forkServers = fork;
    }
    
    /** Stop any running servers. */
    public static void shutdownServers()
    {
    	for(Server server : _servers)
        {
            // remove the port file
    	    _deletePortFile(server.getDatabasePath(0, true));
    	    
    		final Connection connection = _getConnectionForServer(server);
    		if(connection != null)
    		{
	    		Statement statement = null;
	            try
	            {
	                try
	                {
	                    statement = connection.createStatement();
	                    statement.execute("SHUTDOWN");
	                }
	                finally
	                {
	                    if(statement != null)
	                    {
	                        statement.close();
	                    }
	                }
	            }
	            catch(SQLException e)
	            {
	            	MessageHandler.error("Error shutting down database.", e);
	            }
	            finally
	            {
	            	try {
						connection.close();
					} catch (SQLException e) {
					    MessageHandler.error("Error closing connection " +
					            "while shutting down databases.", e);
					}
	            }
    		}    		
        }
    	_servers.clear();
    }
    
    ///////////////////////////////////////////////////////////////////
    // protected methods
    
    /** Returns true if foreign keys are automatically indexed. */
    @Override
    protected boolean _areForeignKeysIndexed()
    {
        return false;
    }

    /** Returns true if primary keys are automatically indexed. */
    @Override
    protected boolean _arePrimaryKeysIndexed()
    {
        return false;
    }

    /** Return the alias for a database given the path. */
    private static String _getDatabaseAlias(String path) throws SQLException
    {
        int pathIndex = path.lastIndexOf(File.separator);
        if(pathIndex == -1)
        {
            throw new SQLException("Database name must be an absolute " +
                "path: " + path);
        }
        else
        {
            return path.substring(pathIndex + 1);
        }        
    }
    
    /** Return the database path without the file name given the path. */
    private static String _getDatabasePath(String path) throws SQLException
    {
        int pathIndex = path.lastIndexOf(File.separator);
        if(pathIndex == -1)
        {
            throw new SQLException("Database name must be an absolute " +
                "path: " + path);
        }
        else
        {
            return path.substring(0, pathIndex);
        }
    }    

    /** Get the driver class name. */
    @Override
    protected String _getDriverName()
    {
        return "org.hsqldb.jdbcDriver";
    }

    /** Get a JDBC URL. */
    @Override
    protected String _getJDBCUrl(String hostName, String port,
        String databaseName) throws SQLException
    {
        // see if the host name is set
        if (hostName != null && hostName.length() > 0) 
        {
            String hostAndPort = _combineHostAndPort(hostName, port);
            
            // make sure the database name starts with a /
            if(!databaseName.startsWith("/"))
            {
                databaseName = "/" + databaseName;
            }
            
            String alias = _getDatabaseAlias(databaseName);
            return "jdbc:hsqldb:hsql://" + hostAndPort + "/" + alias +
                ";filepath=hsqldb:file:" + databaseName;
        }
        return "jdbc:hsqldb:" + databaseName;
    }

    /** Get the SQL string of a column type. */
    @Override
    protected String _getTypeString(Column column)
    {
        String retval = null;

        switch(column.getType())
        {
            case Boolean:
                retval = "BOOLEAN";
                break;
            case Blob:
                // NOTE: we use BINARY instead of OBJECT so that we can
                // query the size of the column.
                // see DatabaseType.getColumnSize()
                retval = "BINARY";
                break;
            case Integer:
                retval = "INTEGER";
                break;
            case Timestamp:
                retval = "TIMESTAMP";
                break;
            case TextBlob:
            case Varchar:
                retval = "VARCHAR";
                break;
        }

        if(retval != null && column.isAutoIncrement())
        {
            // set the starting value to be 1 since ResulSet.getInt()
            // returns 0 if the value is NULL.
            retval += " GENERATED BY DEFAULT AS IDENTITY(START WITH 1)";
        }

        return retval;
    }

    /** Returns true if database uses a catalog. */
    @Override
    protected boolean _hasCatalog()
    {
        return false;
    }

    /** Returns true if database supports auto-generated keys in its prepared
     *  statements.
     */
    @Override
    protected boolean _hasGeneratedKeys()
    {
        return false;
    }
    
    /** Returns true if table is cached. */
    @Override
    protected boolean _isTableCached()
    {
        return true;
    }
    
    /** Returns true if column names should be capitalized. */
    @Override
    protected boolean _needCapitalColumnNames()
    {
        return true;
    }

    /** Returns true if table names should be capitalized. */
    @Override
    protected boolean _needCapitalTableNames()
    {
        return true;
    }

    /** Returns true if need to call IDENTITY() after INSERTs with
     *  autoincrement columns.
     */
    @Override
    protected boolean _needIdentityForAutoInc()
    {
        return true;
    }
    
    /** Get a connection to the database. */
    @Override
    protected Connection _getConnection(String jdbcURL) throws SQLException
    {
        try
        {
            Connection connection = DriverManager.getConnection(jdbcURL);
            _updateConnectionReferenceCount(connection, jdbcURL);
            return connection;
        }
        catch(SQLException e)
        {
            String sqlState = e.getSQLState();

            if(_isDebugging)
            {
                _log.debug("failed = " + sqlState);
            }
            
            // see if the server is not running
            if(sqlState.equals(_SERVER_NOT_RUNNING_SQL_STATE))
            {                
                // attempt to find the path, alias, and port in the jdbc url
                Matcher matcher = _jdbcURLPattern.matcher(jdbcURL);
                if(!matcher.matches())
                {
                    throw new SQLException("Could not parse JDBC URL " + 
                        jdbcURL + "\n" +
                        "JDBC URL must be in form of: " +
                        "jdbc:hsqldb:hsql://hostname:port/alias;filepath=hsqldb:path");
                }
                
                String pathStr = matcher.group(3);
                String aliasStr = matcher.group(2);
                String dbPort = matcher.group(1);
                
                // start a server
                int serverState = _launchDBServer(pathStr, aliasStr, dbPort);
                if(serverState != ServerConstants.SERVER_STATE_ONLINE)
                {
                    throw new SQLException("Unable to start HSQL server for " +
                        jdbcURL);
                }

                try
                {
                    Connection retval = DriverManager.getConnection(jdbcURL);
                    
                    //System.out.println("new conn " + URL + " is " + conn);
                    _serverConnectionSet.add(retval);
                    _initializeNewURL(retval);
                    return retval;
                }
                catch(SQLException e2)
                {
                    throw new SQLException("Unable to start HSQL server for " +
                        jdbcURL);
                }
            }
            else
            {
                throw e;
            }
        }
    }
    
    /** Get a connection to the database. */
    @Override
    protected Connection _getConnection(String URL, String dbPort,
        String databaseName, String userName, String passwd)
        throws SQLException
    {
                
        // HSQL defaults to user "sa"
        userName = "sa";

        // read the password from the auth file unless it was specified
        // in the configuration file
        if(passwd == null || passwd.trim().isEmpty())
        {
            try
            {
                passwd = _getAuthFilePassword(databaseName);
            }
            catch (IOException e)
            {
                throw new SQLException("Error reading auth file.", e);
            }
        }
        
        String pathStr = _getDatabasePath(databaseName);
        String aliasStr = _getDatabaseAlias(databaseName);
       
        // record the database path so that in disconnect() we can remove
        // the port file when the database is shut down.
        _dbName = databaseName;
                        
        Connection conn = null;
        try
        {
            if(_isDebugging)
            {
                _log.debug("getting connection for " + URL);
            }
                        
            conn = DriverManager.getConnection(URL, userName, passwd);

            if(_isDebugging)
            {
                _log.debug("success");
            }

            //System.out.println("already running hsql url = " + URL);
        }
        catch(SQLException e)
        {
            String sqlState = e.getSQLState();
            String errorMessage = e.getMessage();

            if(_isDebugging)
            {
                _log.debug("failed = " + sqlState);
            }
            
            // see if the server is not running
            if(sqlState.equals(_SERVER_NOT_RUNNING_SQL_STATE))
            {
                // start a server
                int serverState = _launchDBServer(pathStr, aliasStr, dbPort);
                if(serverState != ServerConstants.SERVER_STATE_ONLINE)
                {
                    throw new SQLException("Unable to start HSQL server for " +
                        URL, e);
                }

                try
                {
                    // create a connection using the existing password, if any
                    String existingPasswd = _getAuthFilePassword(_dbName);
                    conn = DriverManager.getConnection(URL, userName, existingPasswd);
                    
                    _log.info("started HSQL server at " + URL);
                    _serverConnectionSet.add(conn);
                    _initializeNewURL(conn);
                }
                catch(SQLException e2)
                {
                    throw new SQLException("Unable to start HSQL server for " +
                        URL, e2);
                }
                catch(IOException e3)
                {
                    throw new SQLException("Error reading auth file.", e3);
                }
            }
            else if(errorMessage != null && errorMessage.equals("Access is denied"))
            {
                // if the database does not exist, and the password is set,
                // getConnection() fails with Access is denied.
                // new databases must be created with no password.
                conn = DriverManager.getConnection(URL, userName, "");
            }
            else
            {
                throw e;
            }
        }
        
        if (conn != null) 
        {
            if(_isDebugging)
            {
                _log.debug("opened connection " + conn + " url = " + URL);
            }

            _updateConnectionReferenceCount(conn, URL);
            
            try
            {
                _changePasswordFromDefault(conn, databaseName, passwd);
            }
            catch (IOException e)
            {
                throw new SQLException("Error writing auth file.", e);
            }
                        
            return conn;
        }
        else
        {
            throw new SQLException(
                    "Failed to connect to url \""
                            + URL
                            + "\" as user \""
                            + userName
                            + ". Perhaps there was an error launching the db server. "
                            + "In addition, sometimes the database "
                            + "cache can become corrupt, so another "
                            + "solution is to remove the \"" + pathStr
                            + "\" directory.");
        }
    }    

    ///////////////////////////////////////////////////////////////////
    // private methods

    /** Change the default HSQL password. The new password is written to the
     *  auth file.
     *  @param connection the jdbc connection
     *  @param databaseName the database name
     *  @param passwd the new password for the database. if null or empty,
     *  one is randomly picked.
     */
    private static void _changePasswordFromDefault(Connection connection,
        String databaseName, String passwd) throws IOException, SQLException
    {

        String newPasswd;
        
        if(passwd == null || passwd.isEmpty())
        {
            // generate a random password
            newPasswd = UUID.randomUUID().toString();
        }
        else
        {
            newPasswd = passwd;
        }
        
        final Properties properties = new Properties();
        properties.setProperty(_AUTH_FILE_PASSWORD_PROP_NAME, newPasswd);
        
        // write the properties file
        final File authFile = new File(databaseName + _AUTH_FILE_EXTENSION);
        _writePropertiesFile(authFile, properties);
        
        // remove read permissions for everyone but owner
        authFile.setReadable(false, false);
        authFile.setReadable(true, true);
                
        Statement statement = null;
        try
        {
            statement = connection.createStatement();
            statement.execute("ALTER USER SA SET PASSWORD \"" + newPasswd + "\"");
            // checkpoint the database so the password change is made persistent.
            statement.execute("CHECKPOINT");
        }
        finally
        {
            if(statement != null)
            {
                statement.close();
            }
        }
    }
    
    /** Get the password from the auth file.
     *  @return the password from the auth file. if the file does not
     *  exist, returns an empty string.
     */
    private static String _getAuthFilePassword(String databaseName) throws IOException
    {
        String retval = null;
        String authFileStr = databaseName + _AUTH_FILE_EXTENSION;
        
        if(_isDebugging)
        {
            _log.debug("attempting to read password from " + authFileStr);
        }
        
        final File authFile = new File(authFileStr);
        if(authFile.exists())
        {
           final Properties properties = _readPropertiesFile(authFile);
           retval = properties.getProperty(_AUTH_FILE_PASSWORD_PROP_NAME);
        }
        
        if(retval == null)
        {
            retval = "";
        }
        return retval;
    }
    
    /** Read a properties file. */
    private static Properties _readPropertiesFile(File file) throws IOException
    {
        final Properties properties = new Properties();
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream(file);
            properties.load(inputStream);
        }
        finally
        {
            if(inputStream != null)
            {
                inputStream.close();
            }
        }
        return properties;
    }
    
    /** Write properties to a file. */
    private static void _writePropertiesFile(File file, Properties properties) throws IOException
    {
    	// make sure the containing directory exists.
    	File parentDir = file.getParentFile();
    	if(!parentDir.exists() && !parentDir.isDirectory() &&
    			!parentDir.mkdirs()) {
    		throw new IOException("Unable to create directories for " + parentDir);
    	}
    	
        OutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream(file);
            properties.store(outputStream, null);
        }
        finally
        {
            if(outputStream != null)
            {
                outputStream.close();
            }
        }
    }
    
    /** Get a Connection object for a Server. Returns null on error. */
    private static Connection _getConnectionForServer(Server server)
    {
    	final String host = server.getAddress();
    	final int port = server.getPort();
    	final String alias = server.getDatabaseName(0, true);
    	final String path = server.getDatabasePath(0, true);
		try {
		    // read the password from the auth file
		    String passwd = _getAuthFilePassword(path);
		    
		    // XXX this assume user is sa and password is either in auth file or empty
		    // a better solution is to store the username and password when starting
		    // the server.
			return DriverManager.getConnection("jdbc:hsqldb:hsql://" +
					host + ":" + port + "/" + alias, "sa", passwd);
		} catch (Exception e) {
			MessageHandler.error("Could not get Connection object for HSQL server " + server, e);
		}
		return null;
    }
    
    /** Start the HSQL Server */
    private static int _launchDBServer(String dbNamePath, String dbAlias,
        String dbPort)
    {
        
        if(_forkServers)
        {
            System.out.println("spawning HSQL server for " + dbNamePath);
            
            // find the hsql jar
            String classpath = System.getProperty("java.class.path");
            String[] jars = classpath.split(File.pathSeparator);
            String hsqlJar = null;
            for(String jar : jars)
            {
                if(jar.matches(".*hsqldb-[\\d\\.]+\\.jar$"))
                {
                    hsqlJar = jar;
                    break;
                }
            }
            
            if(hsqlJar == null)
            {
                MessageHandler.error("Unable to find HSQL jar in class path.");
                return ServerConstants.SERVER_STATE_SHUTDOWN;
            }
            
            // NOTE: the database argument must include the file name of
            // the database. when using the Server API to start the server
            // (see below), the database argument does NOT include the file
            // name, but uses the alias as the file name.
            
            ProcessBuilder procBuilder = new ProcessBuilder("java",
                    "-cp",
                    hsqlJar,
                    "org.hsqldb.Server",
                    "-address",
                    "localhost",
                    "-port",
                    dbPort,
                    "-dbname.0",
                    dbAlias,
                    "-database.0",
                    dbNamePath + File.separator + dbAlias);
            procBuilder.redirectErrorStream(true);
            
            //for(String str : procBuilder.command())
              //System.out.print(str + " ");
            //System.out.println();
            
            try {
                /*Process proc =*/ procBuilder.start();
                
                // sleep a few seconds so that it has time to start before we
                // try to connect
                // XXX this may not be long enough
                Thread.sleep(3000);
                return ServerConstants.SERVER_STATE_ONLINE;
            } catch (Exception e) {
                MessageHandler.error("Error starting HSQL server.", e);
                return ServerConstants.SERVER_STATE_SHUTDOWN;
            }
        }
        else
        {
            Server server = new Server();
            
            if (!_isDebugging)
            {
                server.setLogWriter(null);
                server.setErrWriter(null);
            }
            else
            {
                _log.debug("starting server for " + dbNamePath);            
            }
      
            // the file name is full path and alias.
            String dbFileName = dbNamePath + File.separator + dbAlias;
      
            server.setDatabasePath(0, dbFileName);
            server.setDatabaseName(0, dbAlias);
            
            if(dbPort != null && dbPort.length() > 0)
            {
                try
                {
                    int port = Integer.parseInt(dbPort);
                    server.setPort(port);
                }
                catch(NumberFormatException e)
                {
                    System.out.print("ERROR: bad port " + dbPort + ": " +
                        e.getMessage());
                }
            }
            
            server.setSilent(true);
            server.setTrace(false);
            server.setNoSystemExit(true);
            server.start();        
                    
            _servers.add(server);
            
            return server.getState();
        }
    }
    
    /** Checkpoint any servers to compact their size. This rewrites
     *  the backing files for the databases.
     */
    public static void checkpointAllServers()
    {
        for(Connection connection : _serverConnectionSet)
        {
            Statement statement = null;
            try
            {
                try
                {
                    //long startTime = System.nanoTime();

                    //System.out.print("checkpointing....");
                    
                    statement = connection.createStatement();
                    statement.execute("CHECKPOINT DEFRAG");
                    
                    //long estimatedTime = System.nanoTime() - startTime;
                    
                    //System.out.println("done; took " +
                        //(double)(estimatedTime / 1000000000) + "s");

                }
                finally
                {
                    if(statement != null)
                    {
                        statement.close();
                    }
                }
            }
            catch(SQLException e)
            {
                //System.out.println("Error checkpointing database: " +
                    //e.getMessage());
            }
        }
    }
    
    /** Increment the reference count for a URL. */
    private void _updateConnectionReferenceCount(Connection connection, String urlStr) throws SQLException
    {
        // the reference count should be incremented for URLs with direct file access
        // or if the URL is for a server and servers are run in a separate process.
        if((!_forkServers && !urlStr.startsWith("jdbc:hsqldb:hsql") && !urlStr.startsWith("jdbc:hsqldb:http")) ||
            (_forkServers && urlStr.startsWith("jdbc:hsqldb:hsql")))
        {
            if(_isDebugging)
            {
                _log.debug(urlStr + " appears to access file directly.");
            }
            
            boolean isNew = false;
            
            // it does not, so we are accessing the database file
            // directly, and must issue shutdown in disconnect().

            synchronized(_urlMapLock)
            {
                Integer count = _urlToCountMap.get(urlStr);
                if(count == null)
                {
                    count = 1;
                    isNew = true;
                }
                else
                {
                    count++;
                }
                
                _urlToCountMap.put(urlStr, count);
                _connectionToURLMap.put(connection, urlStr);
            }
            
            if(isNew)
            {
                _initializeNewURL(connection);
            }
        }
    }
    
    /** Perform initialization for a URL that has not yet been used. */
    private void _initializeNewURL(Connection connection) throws SQLException
    {
        //System.out.println("initializing for new URL. connection = " + connection);
        
        // increase number of rows cached in memory
        _executeSQL(_SET_CACHE_SCALE, connection);
    }
    
    /** Execute an SQL statement for a Connection.
     *  XXX move to parent class
     */
    protected void _executeSQL(String sqlStr, Connection connection) throws SQLException 
    {
        Statement statement = null;
        try
        {
            statement = connection.createStatement();
            statement.execute(sqlStr);
        }
        finally
        {
            if(statement != null)
            {
                statement.close();
            }
        }
    }

    /** Delete the port file for a database. */
    protected static void _deletePortFile(String dbName)
    {
        String portFileName = dbName + _PORT_FILE_EXTENSION;
        File portFile = new File(portFileName);
        if(portFile.exists() && !portFile.delete())
        {
            System.out.println("WARNING: Could not delete port file " + portFileName);
        }
    }
    
    ///////////////////////////////////////////////////////////////////
    // private variables
  
    /** A regex to find the port, alias, and file path in an hsql jdbc url. */
    private static final Pattern _jdbcURLPattern = 
        Pattern.compile("jdbc:hsqldb:hsql://[\\w/]+:(\\d+)/(\\w+)\\;filepath=hsqldb\\:(.*)");

    /** SQL state string returned in SQLException when trying to connect
     *  to a HSQL server that is not running.
     */
    private static final String _SERVER_NOT_RUNNING_SQL_STATE = "08000";

    /** SQL statement to set write delay. */ 
    private static final String _SQL_SET_WRITE_DELAY = "SET WRITE_DELAY TRUE";
        
    /** A collection of HSQL server connections that we start. */
    private static final Set<Connection> _serverConnectionSet =
        new HashSet<Connection>();
    
    /** A collection of Server objects. */
    private static final Set<Server> _servers = Collections.synchronizedSet(new HashSet<Server>());
    
    /** A mapping of URLs to a count. (Only for URLs that directly access the file.) */
    private static final Map<String,Integer> _urlToCountMap = new HashMap<String,Integer>();
    
    /** A mapping of Connections to URL strings. (Only for URLs that directly access the file.) */
    private static final Map<Connection,String> _connectionToURLMap = new HashMap<Connection,String>();
        
    /** A lock to synchronize access for _urlToCountMap and _connectionToURLMap. */
    private static final Object _urlMapLock = new Object();
    
    /** SQL statement to set the maximum number of rows of cached tables that
     *  are held in memory. Setting to 18 improves performance.
     *  see: http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5429#c2 
     */ 
    private static final String _SET_CACHE_SCALE = "SET PROPERTY \"hsqldb.cache_scale\" 18";

    /** If true, HSQL servers are run in a separate process. */
    private static boolean _forkServers = false;
    
    /** Logging */
    private static final Log _log = LogFactory.getLog(HSQL.class.getName());
    private static final boolean _isDebugging = _log.isDebugEnabled();
    
    /** Extension of auth file containing the password. */
    private static final String _AUTH_FILE_EXTENSION = ".auth";
    
    /** Name of password property in auth file. */
    private static final String _AUTH_FILE_PASSWORD_PROP_NAME = "password";

    /** Extension of the port file containing the server port. */
    private static final String _PORT_FILE_EXTENSION = ".port";

    /** Name of port property in port file. */
    private static final String _PORT_FILE_PASSWORD_PROP_NAME = "port";
   
    /** Random number generator used to generate random port numbers. */
    private static final Random _random = new Random();
    
    /** A mapping of ports randomly chosen for the server to database names. */
    private static final Map<String,String> _serverPortToName = new HashMap<String,String>();

    /** The database path and name. */
    private String _dbName;
    
}
