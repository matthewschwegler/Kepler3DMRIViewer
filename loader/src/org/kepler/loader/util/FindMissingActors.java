/**
 *  '$RCSfile$'
 *  '$Author: crawl $'
 *  '$Date: 2012-07-25 10:41:30 -0700 (Wed, 25 Jul 2012) $'
 *  '$Revision: 30276 $'
 *
 *  For Details:
 *  http://www.kepler-project.org
 *
 *  Copyright (c) 2012 The Regents of the
 *  University of California. All rights reserved. Permission is hereby granted,
 *  without written agreement and without license or royalty fees, to use, copy,
 *  modify, and distribute this software and its documentation for any purpose,
 *  provided that the above copyright notice and the following two paragraphs
 *  appear in all copies of this software. IN NO EVENT SHALL THE UNIVERSITY OF
 *  CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 *  OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 *  DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY
 *  DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
 *  SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 */
package org.kepler.loader.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.kepler.Kepler;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.kar.KarDoclet;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.util.sql.DatabaseFactory;
import org.kepler.util.sql.HSQL;

import ptolemy.actor.Actor;
import ptolemy.actor.Director;
import ptolemy.actor.gui.ConfigurationApplication;
import ptolemy.data.expr.Parameter;
import ptolemy.util.MessageHandler;

/** A class to that finds source files whose classes are not in the actor tree.
 * 
 *  TODO:
 *    separate deprecated classes
 *    separate super classes
 *    include some subclasses of Attribute
 *    warn if xml has missing or invalid semantic types
 *    warn if xml has no semantic types in onto:2
 *    
 *  @author Daniel Crawl
 *  @version $Id: FindMissingActors.java 30276 2012-07-25 17:41:30Z crawl $
 */
public class FindMissingActors {

	/** Find files whose classes are not in the actor tree.
	 * 
	 *  @param args a list of module names. if empty, all modules in
	 *  current suite are searched.
	 */
	public static void main(String[] args) {
		
        // call the module initializers
        try {
            Kepler.initialize();
        } catch (Exception e) {
            MessageHandler.error("ERROR initializing modules.", e);
            System.exit(1);
        }

        // start the kepler cache
        
        Kepler.setOntologyIndexFile();

        _initializeCache();
        
        final ModuleTree tree = ModuleTree.instance();
        List<Module> modules;
        if(args.length == 0 || (args.length == 1 && args[0].equals("undefined"))) {
            modules = tree.getModuleList();
        } else {
            modules = new LinkedList<Module>();
            for(String name : args) {
                final Module module = tree.getModule(name);
                if(module == null) {
                    System.out.println("Module " + name + " is not in current suite.");
                } else {
                    modules.add(module);
                }
            }
        }
        
        for(Module module : modules) {
            
            final String moduleName = module.getName();
            System.out.println("Find missing entries for module " + moduleName);

            // print which files do not have entries
            try {                                
                _printFilesMissingEntries(module);
                
            } catch(Exception e) {
                MessageHandler.error("Error finding missing entries.", e);
            }
            
        }
        
        // shutdown the cache and exit
       _shutdownCache(true);

	}
	
	
	/** Get a list of source files in a module that are not in the actor cache. */
	private static void _printFilesMissingEntries(Module module) throws Exception {
	    
	    
	    List<String> fileNames = module.getSourceFileNames();
	    File srcDir = module.getSrc();
	    
	    if(_actorCacheClasses.isEmpty()) {
	        
	        Connection conn = null;
	        Statement st = null;
	        ResultSet result = null;
	        try {
	            conn = DatabaseFactory.getDBConnection();
	            st = conn.createStatement();
                result = st.executeQuery("select classname from cachecontenttable ");
                
                while(result.next()) {
                    _actorCacheClasses.add(result.getString(1));
                }
	        } finally {
	            try {
	                if(result != null) {
	                    result.close();
	                }
	                if(st != null) {
	                    st.close();
	                }
	                if(conn != null) {
	                    conn.close();
	                }
	            } catch(SQLException e) {
	                MessageHandler.error("ERROR closing cache database.", e);
	            }
	        }
	    }
	    
	    List<String> missing = new LinkedList<String>();
	    List<String> deprecated = new LinkedList<String>();
	    
	    for(String relativeName : fileNames) {
	        
	        String className = relativeName.replace(File.separatorChar, '.');
	        className = className.substring(0, className.indexOf(".java"));
	        //System.out.println("class is " + className);
	        
	        String fileName = new File(srcDir, relativeName).getAbsolutePath();          
	        
	        if(!_actorCacheClasses.contains(className)) {
	            
	            // make sure class is an actor, director or parameter
	            //System.out.println("checking " + className);
	            Class<?> clazz = null;
	            
	            try {
	                clazz = Class.forName(className);
	            } catch(Throwable t) {
	                // ignore
	                continue;
	            }
	            
	            // see if the class is an Actor, Director, or Parameter
	            // is not abstract, or an interface
	            if((Actor.class.isAssignableFrom(clazz) ||
	                    Director.class.isAssignableFrom(clazz) ||
	                    Parameter.class.isAssignableFrom(clazz)) &&
	                    !clazz.isInterface() && 
	                    !Modifier.isAbstract(clazz.getModifiers())) {
	     
	                // see if it's deprecated
	                // XXX this always returns false since KarDoclet was not
	                // run for this class.
	                if(KarDoclet.isClassDeprecated(className)) {
	                    deprecated.add(fileName);
	                } else {
	                    missing.add(fileName);	                
	                }
	            }
	        }
	    }
	    
	    if(!missing.isEmpty()) {
    	    System.out.println("Source files not in library:");
    	    for(String name : missing) {
    	        System.out.println(name);
    	    }
	    }
	    
	    if(!deprecated.isEmpty()) {
    	    System.out.println("Deprecated source files not in library:");
            for(String name : deprecated) {
                System.out.println(name);
            }
	    }
	}

	private static void _initializeCache() {
				
		// load the configuration since it is required to load the
		// KAR entries handlers, which in turn is required to build
		// the actor library
		
	    // NOTE: we must use a configuration with the GUI, otherwise
	    // MoML filters are used to remove GUI actors.
		String spec = "ptolemy/configs/kepler/ConfigGUIAndCache.xml";
		URL url = null;
		try {
			url = ConfigurationApplication.specToURL(spec);
		} catch (IOException e) {
			MessageHandler.error("ERROR configuration URL.", e);
			System.exit(1);
		}
		
		try {
			ConfigurationApplication.readConfiguration(url);
		} catch (Exception e) {
			MessageHandler.error("ERROR reading configuration.", e);
			System.exit(1);
		}		
		
		// build the actor library in case it does not exist
		LibraryManager.getInstance().buildLibrary();
	}
	
	
	private static void _shutdownCache(boolean exit) {
		// call the module deinitializers
		Kepler.shutdown();
		HSQL.shutdownServers();
		
		if(exit) {
		    // we have to call System.exit() because ???
		    System.exit(0);
		}
	}

	private static Set<String> _actorCacheClasses = new HashSet<String>();
}
