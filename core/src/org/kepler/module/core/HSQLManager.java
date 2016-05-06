/* Manage HSQL servers for the core module.
 * 
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-04-15 15:23:41 -0700 (Mon, 15 Apr 2013) $' 
 * '$Revision: 31920 $'
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
package org.kepler.module.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.module.ModuleHSQLManager;
import org.kepler.util.sql.DatabaseFactory;
import org.kepler.util.sql.DatabaseType;

import ptolemy.util.MessageHandler;

/** A class to start and stop the two HSQL databases managed by the core
 *  module:
 *  
 *  KeplerData/modules/core/coreDB
 *  .kepler/cache-2.1/cachedata/hsqldb
 * 
 *  The HSQL servers are run in a separate process.
 *  
 *  @author Daniel Crawl
 *  @version $Id: HSQLManager.java 31920 2013-04-15 22:23:41Z crawl $
 *
 */
public class HSQLManager implements ModuleHSQLManager {

    /** Start HSQL servers in a separate process. */
    public void start() {
        
        // start server for coreDB
        
        try {
            _getCoreDBConnection();
        } catch (Exception e) {
            MessageHandler.error("Error starting coreDB.", e);
        }
        
        // start server for .kepler cache db
        
        try {
            DatabaseFactory.getDBConnection();
        } catch (Exception e) {
            MessageHandler.error("Error starting .kepler cache database.", e);
        }
    }

    /** Stop HSQL servers in a separate process. */
    public void stop() {

        // stop server for coreDB
        
        DatabaseType dbType;
        try {
            dbType = _getCoreDBConnection();
        } catch (Exception e) {
            MessageHandler.error("Error getting connection to coreDB.", e);
            return;
        }
        
        try {
            dbType.disconnect();
        } catch (SQLException e) {
            MessageHandler.error("Error shutting down coreDB.", e);
        }
        
        // stop server for .kepler cache db
        DatabaseFactory.shutdownCacheServer();        
    }

    /** Get a connection to the coreDB database. */
    private DatabaseType _getCoreDBConnection() throws Exception
    {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        ConfigurationProperty coreProperty = configManager
                .getProperty(ConfigurationManager.getModule("core"));
        ConfigurationProperty coreDBProperty = coreProperty
                .getProperty("coreDB");

        if (coreDBProperty == null) {
            throw new Exception("Could not find " + "coreDB"
                    + " in core module's configuration.xml.");
        }

        return DatabaseFactory.getConnectedDatabaseType(coreDBProperty);
    }

}
