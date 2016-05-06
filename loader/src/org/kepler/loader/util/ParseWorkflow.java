/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-04-09 11:02:59 -0700 (Wed, 09 Apr 2014) $' 
 * '$Revision: 32651 $'
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
package org.kepler.loader.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Map;

import org.kepler.Kepler;
import org.kepler.build.util.Version;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KARFile;
import org.kepler.kar.handlers.ActorMetadataKAREntryHandler;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObjectInterface;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.sql.HSQL;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLParser;
import ptolemy.moml.filter.BackwardCompatibility;
import ptolemy.util.MessageHandler;

/** A utility class to load workflows. This is used by 
 *  org.kepler.build.RunTestWorkflows to test loading demo workflows.
 * 
 *  @author Daniel Crawl
 *  @version $Id: ParseWorkflow.java 32651 2014-04-09 18:02:59Z crawl $
 */
public class ParseWorkflow {

    /** Load workflows and exit.
     *  @param args A list of workflow files to load.
     */
    public static void main(String[] args) {
        
    	MoMLParser.setMoMLFilters(BackwardCompatibility.allFilters());
        MessageHandler.setMessageHandler(new MessageHandler());
    	try {
    		// initialize Kepler to load the kepler-specific backward-compatible
    		// moml filters
        	Kepler.initialize();
    		Kepler.setJavaPropertiesAndCopyModuleDirectories();
		} catch (Exception e) {
			MessageHandler.error("Error initializing Kepler.", e);
		}

        try {
            ParseWorkflow parse = new ParseWorkflow();
            parse.parseWorkflows(args);
        } catch(Throwable t) {
            System.err.println("Error: " + t.getMessage());
            t.printStackTrace();
        }
        
        Kepler.shutdown();
        HSQL.shutdownServers();
    }

    /** Parse an actor's XML definition. */
    public static NamedObj parseActor(File file) throws Exception {
    	
    	if(!file.exists()) {
    		throw new Exception("Actor file does not exist: " + file.getAbsolutePath());
    	}
    	
    	FileInputStream stream = null;
    	try {
    		stream = new FileInputStream(file);
    		ActorMetadata metadata = new ActorMetadata(stream);
    		NamedObj metadataNamedObj = metadata.getActorAsNamedObj(null);
    		String metadataString = metadataNamedObj.exportMoMLPlain();
    		
    		MoMLParser parser = new MoMLParser();
    		parser.resetAll();
    		CompositeEntity container = new CompositeEntity();
    		parser.setContext(container);
    		parser.parse(metadataString);
    		return container.getEntity(metadata.getName());
    		
    	} finally {
    		if(stream != null) {
    			stream.close();
    		}
    	}
    	
    }
    
    /** Load workflows. */
    public void parseWorkflows(String[] args) throws Exception {
        for(int i = 0; i < args.length; i++) {
        	boolean isActor = false;
        	if(args[i].equals("-a")) {
        		isActor = true;
        		i++;
        	}
        	String path = args[i];
            File file = new File(path);
            if(isActor) {
            	parseActor(file);
            } else {
            	parseWorkflow(file);
            }
        }
    }
    
    /** Parse a workflow MoML XML or KAR file.
     *  @return the constructed NamedObj 
     */
    public static NamedObj parseWorkflow(File file) throws Exception {
    	
        if(!file.exists()) {
            throw new Exception("File does not exist " + file.getAbsolutePath());
        }

        if(file.getName().endsWith(".xml")) {
            return _parseXML(file);
        } else if(file.getName().endsWith(".kar")) {
            return _parseKAR(file);
        } else {
            throw new Exception("Unknown type of workflow: " + file.getAbsolutePath());
        }
    }
    
    /** Parse a workflow KAR file.
     *  @return the constructed NamedObj 
     */
    private static NamedObj _parseKAR(File file) throws Exception {
        
        // FIXME most of this method is duplicated from the
        // KeplerConfigurationApplication constructor.
        
        KARFile karFile = new KARFile(file);
        
        if(!karFile.isOpenable()) {
            
            // check dependencies.
            Map<String, Version> missingDeps = karFile.getMissingDependencies();
            if(!missingDeps.isEmpty()) {
                
                // print out the missing dependencies
                System.out.println("ERROR: Missing module dependencies:");
                for(Map.Entry<String, Version> entry : missingDeps.entrySet()) {
                    System.out.println("   " + entry.getKey());
                }
            }
        } else {
         
            karFile.cacheKARContents();
            // For each Actor in the KAR open the MOML
            for (KAREntry entry : karFile.karEntries()) {
                KeplerLSID lsid = entry.getLSID();

                if (!ActorMetadataKAREntryHandler.handlesClass(entry.getType())) {
                    //WARNING - using null TableauFrame here
                    karFile.open(entry, null);
                    continue;
                }
                // get the object from the cache (it is ActorMetadata
                // even though it is a workflow)
                CacheObjectInterface co = CacheManager.getInstance()
                        .getObject(lsid);
                
                // make sure we were able to load it
                if(co == null) {
                    throw new Exception("Could not find LSID for workflow in the Kepler cache.\n" +
                            "Make sure there are no other Kepler instances running " +
                            "and remove " + DotKeplerManager.getDotKeplerPath());
                }
                
                ActorMetadata am = (ActorMetadata) co.getObject();

                // get the workflow from the metadata
                NamedObj entity = am.getActorAsNamedObj(null);

                // extract MOML to temp file
                File tmpFile = File.createTempFile("moml", ".xml");
                tmpFile.deleteOnExit();
                FileWriter writer = null;
                try {
                    writer = new FileWriter(tmpFile);
                    entity.exportMoML(writer);
                    writer.flush();
                } finally {
                    if(writer != null) {
                        writer.close();
                    }
                }
                
                return _parseXML(tmpFile);
            }       
        }
        return null;
    }
    
    
    /** Parse a workflow MoML file. */
    private static NamedObj _parseXML(File file) throws Exception {
        _parser.reset();
        return _parser.parse(null, file.toURI().toURL());
    }
    
    private final static MoMLParser _parser = new MoMLParser();

}
