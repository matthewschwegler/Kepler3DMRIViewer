/**
 *  '$RCSfile$'
 *  '$Author: crawl $'
 *  '$Date: 2013-08-27 17:00:07 -0700 (Tue, 27 Aug 2013) $'
 *  '$Revision: 32478 $'
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kepler.Kepler;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.kar.KARCacheContent;
import org.kepler.kar.KARCacheManager;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KARManifest;
import org.kepler.kar.KarDoclet;
import org.kepler.kar.SuperClassPathFinderDoclet;
import org.kepler.kar.handlers.ActorMetadataKAREntryHandler;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.sms.SemanticType;
import org.kepler.util.sql.DatabaseFactory;
import org.kepler.util.sql.HSQL;

import ptolemy.actor.AtomicActor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.gui.ConfigurationApplication;
import ptolemy.actor.lib.RandomSource;
import ptolemy.actor.lib.TimedSource;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLParser;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;
import ptolemy.vergil.basic.export.web.WebContent;
import util.StringUtil;

import com.sun.tools.javadoc.Main;

/** A class to update the KAR XMLs for actors, directors, etc.
 * 
 *  @author Daniel Crawl
 *  @version $Id: UpdateActorTreeFiles.java 32478 2013-08-28 00:00:07Z crawl $
 */
public class UpdateActorTreeFiles {

	/** Update the KAR XMLs for modules.
	 * 
	 *  @param args a list of module names
	 */
	public static void main(String[] args) {
		
		// call the module initializers
		try {
			Kepler.initialize();
		} catch (Exception e) {
			MessageHandler.error("ERROR initializing modules.", e);
			System.exit(1);
		}

        Kepler.setOntologyIndexFile();

		_initializeCache();
		
        final ModuleTree tree = ModuleTree.instance();
        List<String> moduleNames;
        if(args.length == 0 || (args.length == 1 && args[0].equals("undefined"))) {
            List<Module>modules = tree.getModuleList();
            moduleNames = new LinkedList<String>();
            for(Module module : modules) {
                moduleNames.add(module.getName());
            }
        } else {
            moduleNames = Arrays.asList(args);
        }
		
		for(String name : moduleNames) {
			try {
				updateKarXMLDocsForModule(name);
			} catch(Exception e) {
				MessageHandler.error("ERROR updating module " + name, e);
			}
		}
				
		_shutdownCache(true);		
	}
	
	/** */
	public static void updateKarXMLDocsForFile(String[] args) throws Exception {
		
	    boolean overwrite = false;
	    boolean allowMultiClasses = false;
	    List<String> names = new LinkedList<String>();
	    for(String arg : args) {
	        if(arg.equals("-overwrite")) {
	            overwrite = true;
	        } else if(arg.equals("-allowMultiClasses")) {
	            allowMultiClasses = true;
	        } else {
	            names.add(arg);
	        }
	    }
	    
		_initializeCache();
		
		final ModuleTree moduleTree = ModuleTree.instance();
		Connection conn = null;
		Statement st = null;
		ResultSet result = null;
		try {
			conn = DatabaseFactory.getDBConnection();
			st = conn.createStatement();
			for(String name : names) {
			    System.out.println("Updating docs for " + name);
	
			    if(!name.endsWith(".xml")) {
			        System.out.println("  ERROR: do not know how to update " + name + " (does not end in .xml)");
			    } else {
			    	File xmlFile = new File(name);
			    	String xmlName = xmlFile.getName();
					result = st.executeQuery("select kars_cached.file, kar_contents.name, reponame, classname, kar_contents.lsid " +
						       "from kars_cached, kar_contents, cachecontenttable " +
						       "where kar_contents.lsid = cachecontenttable.lsid and " +
						       "kar_contents.file = kars_cached.file and kar_contents.name = '" + xmlName + "'");			    	
			    }
				if(!result.next()) {
					System.out.println("  ERROR: could not find " + name + " in cache.");
					continue;
				}
				
				String karPath = result.getString(1);
				final String xmlName = result.getString(2);
				final String moduleName = result.getString(3).toLowerCase();
				final String className = result.getString(4);
				final String lsidStr = result.getString(5);
								
				Module module = moduleTree.getModuleByStemName(moduleName);
				if(module == null) {
					throw new IllegalActionException("ERROR: module " + moduleName + " not in modules.txt");
				}
				
				// the path returned is the query is something like:
				// .../KeplerData/modules/actors/kar/CoreActors.kar
				// the name of the kar is the directory name where to write the new xml file:
				// .../kepler.modules/actors/resources/kar/CoreActors
				
				final File karRepoFile = new File(karPath.replaceAll("\\.kar$", ""));
				final String karDirName = karRepoFile.getName();
				final String fullXMLPath = module.getKarResourcesDir() + File.separator + karDirName;
				
                updateDocs(className, fullXMLPath + File.separator + xmlName,
                        true, lsidStr, !overwrite, allowMultiClasses);
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

		_shutdownCache(true);
	}

	/** Update the KAR XML documentation for a module. */ 
	public static void updateKarXMLDocsForModule(String moduleName) throws Exception {
		
		System.out.println("Updating docs for module " + moduleName);
		
		// get the classes for the xmls
		
		ModuleTree moduleTree = ModuleTree.instance();
		Module module = moduleTree.getModuleByStemName(moduleName);
		if(module == null) {
			throw new IllegalActionException("ERROR: module " + moduleName + " not in modules.txt");
		}
		
		Connection conn = null;
		Statement st = null;
		ResultSet result = null;
		try {
			conn = DatabaseFactory.getDBConnection();
			st = conn.createStatement();
			
			// first character of repository name is upper case
			final char[] stringArray = moduleName.toCharArray();
			stringArray[0] = Character.toUpperCase(stringArray[0]);
			final String repoName = new String(stringArray);
			
			result = st.executeQuery("select kars_cached.file, kar_contents.name, classname, kar_contents.lsid " +
				       "from kars_cached, kar_contents, cachecontenttable " +
				       "where kar_contents.lsid = cachecontenttable.lsid and " +
				       "kar_contents.file = kars_cached.file and reponame = '" + repoName + "'");
			
			while(result.next()) {
				String karPath = result.getString(1);
				final String xmlName = result.getString(2);
				final String className = result.getString(3);
                final String lsidStr = result.getString(4);

				// the path returned is the query is something like:
				// .../KeplerData/modules/actors/kar/CoreActors.kar
				// the name of the kar is the directory name where to write the new xml file:
				// .../kepler.modules/actors/resources/kar/CoreActors
				
				final File karRepoFile = new File(karPath.replaceAll("\\.kar$", ""));
				final String karDirName = karRepoFile.getName();
				final String fullXMLPath = module.getKarResourcesDir() + File.separator + karDirName;
				
				final String outputFileName = fullXMLPath + File.separator + xmlName;
				System.out.println("Updating docs for " + outputFileName);
				updateDocs(className, outputFileName, false, lsidStr, true, false);
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
	
    /** Create XML files for actor KARs.
     *  @param names a java source file names or class name.
     *  @param outputFileName the name of the output file.
     *  @param printWhenChangePropName if true, print when the class field names
     *  in the docs are different from the name returned by NamedObj.getName(). 
     *  @param lsidStr the lsid
     *  @param onlyCreateForMissing if true, documentation is only created if not present.
     *  @param allowMultiClasses if true, create documentation even if multiple actors
     *  use the same class, e.g., customized versions of the R actor.
     */
    public static void updateDocs(String name, String outputFileName, 
            boolean printWhenChangePropName, String lsidStr,
            boolean onlyCreateForMissing, boolean allowMultiClasses) throws Exception {

        //System.out.println("    output file name = " + outputFileName);
        
        // read the existing file
        _parser.reset();
        NamedObj oldKarXML = _parser.parseFile(outputFileName);
        final KeplerDocumentationAttribute oldDoc = (KeplerDocumentationAttribute) oldKarXML.getAttribute("KeplerDocumentation");
        final KeplerDocumentationAttribute tmpDoc = new KeplerDocumentationAttribute(_workspace);
        tmpDoc.createInstanceFromExisting(oldDoc);
        if(tmpDoc != null) {
            String userLevelDoc = tmpDoc.getUserLevelDocumentation();
            if(onlyCreateForMissing && userLevelDoc != null && userLevelDoc.trim().length() > 10) {
                System.out.println("WARNING: not updating docs for " +
                    outputFileName + " since docs already exist.");
                return;
            }
        }
        
        String fileName = _getFileName(name);
        if(fileName == null) {
            return;
        }
                    
        final KeplerDocumentationAttribute newDoc = _generateDocsFromDoclet(fileName);
        if(newDoc == null) {
            return;
        }

        final String className = _getClassName(fileName);
        
        if(!allowMultiClasses && _classHasMultipleDocs(className)) {
            System.out.println("WARNING: not updating docs for " + outputFileName + " since multiple doc files found for " + className);
            return;
        }

        //System.out.println("    class name = " + className);
        
        String shortName = className
                .substring(className.lastIndexOf(".") + 1);

        Class<?> cls = Class.forName(className);
        NamedObj no;
        
        try {
            no = ActorMetadata.createInstance(cls,
                new Object[] { new CompositeEntity(), shortName });
        } catch(Exception e) {
            System.err.println("ERROR: could not instantiate " + className);
            return;
        }
        
        // convert names of parameters and ports from object field name
        // to the name returned by NamedObj.getName()
        _fixNames(no, newDoc, printWhenChangePropName);
        
        if(oldDoc != null) {     
            
            /*
            // see if we're only creating docs when they are missing
            if(onlyCreateForMissing) {
                // update the existing docs from the new class docs
                oldDoc.updateFromExisting(newDoc, true);
            } else {
            */ 
            
            // use old entries if the javadoc ones are empty
            newDoc.updateFromExisting(oldDoc, true);
            
            // remove the old doc attribute, and move the new doc attribute 
            // to the same position as the old one
            int index = oldDoc.moveToLast();
            oldDoc.setContainer(null);
            newDoc.setContainer(oldKarXML);
            newDoc.moveToIndex(index);
            
            //}
        } else {
            newDoc.setContainer(oldKarXML);
        }

        String xmlStr = oldKarXML.exportMoML();
        String prettyXMLStr = StringUtil.prettifyXML(xmlStr, 2);
        
        final File outputFile = new File(outputFileName);
        final FileWriter writer = new FileWriter(outputFile);
        writer.write(prettyXMLStr);
        writer.close();
    }
    
    /** Create XML files for a list of source files. */
    public static void buildXMLs(String args[]) {

        _initializeCache();

        List<String> names = new LinkedList<String>();
        
        boolean dup = true;
        boolean requireSemanticTypes = false;
        
        for (String arg : args) {            
            if(arg.equals("-nodup")) {
                System.out.println("Will not create files for actors already in cache.");
                dup = false;
            } else if(arg.equals("-requireSem")) {
                System.out.println("Will not create files for actors without semantic types.");
                requireSemanticTypes = true;
            } else {
                names.add(arg);
            }
        }
        
        // do not generate XMLs for deprecated classes
        KarDoclet.setGenerateForDeprecated(false);
        
        for(String name : names) {
            System.out.println("Building actor XML for " + name);
            buildKARXML(name, null, null, true, true, dup, requireSemanticTypes);
        }
        
        _shutdownCache(true);
    }

    /** Create XML files for actor KARs.
	 *  @param names a java source file names or class name.
	 *  @param outputDir the output directory to write the file to. if null,
	 *  then try to guess module containing the source or class name and write
	 *  to module/resources/kar. if cannot guess module, write to user.dir.
	 *  @param outputName the name of the output file. if null, then use the short class name. 
	 *  @param printWhenNameIsDifferent if true, print when the class name and name in the cache are different.
	 *  @param printWhenChangePropName if true, print when the class field names in the docs are different
	 *  from the name returned by NamedObj.getName(). 
	 *  @param duplicateExistingActor if true, create XML for actor already in cache. otherwise,
	 *  do nothing.
	 *  @param requireSemanticTypes if true, do not create XML for actors unless semantic types are
	 *  known or can be guessed.
	 */
	public static void buildKARXML(String name, String outputDir, String outputName, 
			boolean printWhenNameIsDifferent, boolean printWhenChangePropName,
			boolean duplicateExistingActor, boolean requireSemanticTypes) {
		try {
		    
            final KARCacheManager karCacheManager = KARCacheManager.getInstance();
            final Vector<KARCacheContent> karCacheContents = karCacheManager.getKARCacheContents();
		    
		    String fileName = _getFileName(name);
		    if(fileName == null) {
		    	return;
		    }
		    			            
            final String className = _getClassName(fileName);

            //System.out.println("class name is = " + className);
            
			String shortName = className
					.substring(className.lastIndexOf(".") + 1);

			Class<?> cls = Class.forName(className);
			NamedObj no;
			
			try {
			    no = ActorMetadata.createInstance(cls,
					new Object[] { new CompositeEntity(), shortName });
			} catch(Exception e) {
			    System.err.println("ERROR: could not instantiate " + className);
			    return;
			}
			
			no = (NamedObj) no.clone(_workspace);
			
			String LSIDStr = null;
			Vector<KeplerLSID> semanticLSIDs = null;
			
            final String parentClassName = cls.getSuperclass().getName();
            Vector<KeplerLSID> parentSemanticLSIDs = null;

			//System.out.println("  Searching for " + className + " in Kepler actor cache.");
            
            boolean foundActorInCache = false;
            
	        for(KARCacheContent content : karCacheContents) {
	            String cacheClassName = content.getCacheContent().getClassName();
	            
	            // make sure class name is not null.
	            // it's null for, e.g., workflow runs and report layouts
	            if(cacheClassName != null) {

	                if(cacheClassName.equals(className)) {
	                    //System.out.println("  Found in cache.");
		                
		                // get the lsid
	            		KeplerLSID lsid = content.getLsid();
		                LSIDStr = lsid.toString();
		                foundActorInCache = true;
		                if(!duplicateExistingActor) {
		                    System.out.println("  WARNING: will not duplicate actor in cache: " + name);
		                    return;
		                }

		                // get the semantic types
		                semanticLSIDs = content.getSemanticTypes(); 
		                
		                String cachedName = content.getCacheContent().getName();
		                if(!shortName.equals(cachedName)) {
		                	if(printWhenNameIsDifferent) {
		                		System.out.println("  using name found in cache: " + cachedName);
		                	}
		                	no.setName(cachedName);
		                }		                
		                break;
		                
		            } else if(cacheClassName.equals(parentClassName)) {
		                parentSemanticLSIDs = content.getSemanticTypes();
		            }
	            }
	        }

	        // generate and set the documentation
            final KeplerDocumentationAttribute doc = _generateDocsFromDoclet(fileName);
            if(doc == null) {
                return;
            }
            
            _fixNames(no, doc, printWhenChangePropName);
            doc.setContainer(no);
            
            //_removeDefaultValues(no);

            Set<String> semanticTypes = new HashSet<String>();
            
            // see if we found semantic types in the cache
            if(semanticLSIDs == null || semanticLSIDs.isEmpty()) {
	            
	            // try guessing
                semanticTypes = _guessSemanticTypes(no);
                for(String type : semanticTypes) {
                    System.out.println("  guessed " + type);
                }

                // see if we found semantic types of the parent class in the cache.
	            if(parentSemanticLSIDs != null) {
	                System.out.println("  WARNING: semantic types not found; using types from parent " + parentClassName);
	                semanticLSIDs = parentSemanticLSIDs;
	            }
			}
						
			// if we found semantic types in the cache for the class or a parent class,
            // convert to strings.
			if(semanticLSIDs != null) {
                // convert from LSID to string
                for(KeplerLSID semanticTypeLSID : semanticLSIDs) {
                    semanticTypes.add(semanticTypeLSID.toString());
                }
			}

    		// see if we still have to semantic types
		    if(semanticTypes.isEmpty()) {		     
		        
		        if(requireSemanticTypes) {
		            System.out.println("  WARNING: will not create XML since no semantic types for " + name);
		            return;
		        }
		        
		        // add a default semantic type
		        semanticTypes.add("urn:lsid:localhost:onto:2:1#FIXME");    
                System.out.println("  WARNING: semantic types need to be set for " + name);
			}
			
			// add the semantic types
            int count = 0;
            for(String semanticTypeStr : semanticTypes) {
                SemanticType semanticType = new SemanticType(no, "semanticType0" + count);
                semanticType.moveToLast();
                semanticType.setExpression(semanticTypeStr);
                count++;
                //System.out.println("  added semantic type " + semanticType.getName() + " = " + semanticType.getExpression());
            }
            
            NamedObjId noId = new NamedObjId(no, NamedObjId.NAME);
            noId.moveToFirst();
            
            if(LSIDStr != null) {
                noId.setExpression(LSIDStr);
                //System.out.println("LSID is " + LSIDStr);
            } else {
                
                LSIDStr = _getNextActorLSIDFromReadme();
                
                if(LSIDStr != null) {
                    System.out.println("  Using new LSID: " + LSIDStr);
                    noId.setExpression(LSIDStr);
                } else {
                    System.out.println("  WARNING: the LSID needs to be set.");
                    noId.setExpression("urn:lsid:kepler-project.org:actor:999:1");
                }
            }

			if(outputDir == null) {
			    
			    // try to find the module containing the source.
			    // NOTE: if the module is found, outputDir is set to
			    // module/resources/kar. However, the actor kar
			    // entries are always in a sub-directory of this
			    // directory. Usually there is only one sub-directory
			    // in module/resources/kar, and below we try to find it.
			    
			    Module module = findModuleSrcDirForNameObj(no);
			    if(module != null) {
			        File karResourcesDir = module.getKarResourcesDir();
			        if(!karResourcesDir.exists() && !karResourcesDir.mkdirs()) {
		                System.out.println("  WARNING: unable to mkdirs: " + karResourcesDir);
		            } else {
		                // see if there is only one directory in the kar resources dir
		                String potentialDir = null;
		                int potentialDirs = 0;
		                final File[] filesInKarResourcesDir = karResourcesDir.listFiles();
		                for(File fileInKarResourcesDir : filesInKarResourcesDir) {
		                    if(fileInKarResourcesDir.isDirectory() && !fileInKarResourcesDir.getName().startsWith(".")) {
		                        potentialDirs++;
		                        potentialDir = fileInKarResourcesDir.getAbsolutePath();
		                    }
		                }
		                // if there's only one sub-directory, use it
		                if(potentialDirs == 1) {
		                    outputDir = potentialDir;
		                } else {
		                    // otherwise use module/resources/kar and files must be moved by hand
		                    outputDir = karResourcesDir.getAbsolutePath();
		                }
		            }
			    }
			}
			
			if(outputDir == null) {
			    outputDir = System.getProperty("user.dir");
			}
			
			// use ActorMetadata instead of NamedObj.exportMoML() to generate
			// the XML since the XML generated by exportMoML is not complete:
			// missing <?xml version="1.0"?> (necessary?)
			// <entity> class attribute is actor class, not ptolemy.kernel.ComponentEntity
			
			String xmlOutputFileName = outputDir + File.separator;
			if(outputName == null) {
				xmlOutputFileName += shortName + ".xml";
			} else {
				xmlOutputFileName += outputName;
			}
			
			System.out.println("  output file is " + xmlOutputFileName);
			
			final File xmlOutputFile = new File(xmlOutputFileName);
			
			// if we're not duplicating existing actors, make sure the output
			// file does not exist.
			if(!duplicateExistingActor && xmlOutputFile.exists()) {
			    System.out.println("  not writing output file since it already exists.");
			    return;
			}
			
			FileWriter writer = new FileWriter(xmlOutputFile);
			ActorMetadata actorMetadata = null;					
			
			if(actorMetadata == null) {
				actorMetadata = new ActorMetadata(no);					
			}
			
			writer.write(actorMetadata.toString(false, false, false));
			writer.close();
			
			if(!foundActorInCache) {
    			InputStream inputStream = null;
    			OutputStream outputStream = null;
    			try {
        			// create a MANIFEST.MF if it does not exist
        			KARManifest manifest;
                    File manifestFile = new File(outputDir, "MANIFEST.MF");
        			if(!manifestFile.exists()) {
        			    manifest = new KARManifest();
        			} else {
        			    inputStream = new FileInputStream(manifestFile);
        			    manifest = new KARManifest(inputStream);
        			}
        			
        			// add the new entries
        			// e.g.:
        			// Name: DecimalFormatConverter.xml
        			// type: ptolemy.kernel.ComponentEntity
        			// lsid: urn:lsid:kepler-project.org:actor:570:1
        			// handler: org.kepler.kar.handlers.ActorMetadataKAREntryHandler
    
        			String typeStr;
        			if(no instanceof AtomicActor) {
        			    typeStr = "ptolemy.kernel.ComponentEntity";
        			} else if(no instanceof CompositeActor) {
        			    typeStr = "org.kepler.moml.CompositeClassEntity";
        			} else {
        			    typeStr = "org.kepler.moml.PropertyEntity";
        			}
        			
        			String xmlOutputFileBaseName = xmlOutputFile.getName();
        			manifest.addEntryAttribute(xmlOutputFileBaseName, KAREntry.TYPE.toString(), typeStr);
                    manifest.addEntryAttribute(xmlOutputFileBaseName, KAREntry.LSID.toString(), LSIDStr);
                    manifest.addEntryAttribute(xmlOutputFileBaseName, KAREntry.HANDLER.toString(),
                            ActorMetadataKAREntryHandler.class.getName());
        			
        			
                    if(inputStream != null) {
                        inputStream.close();
                        inputStream = null;
                    }
        			
        			// write the updated manifest
        			outputStream = new FileOutputStream(manifestFile);    			
        			manifest.write(outputStream);
        			
    			} finally {
    			    if(inputStream != null) {
    			        inputStream.close();
    			    }
    			    if(outputStream != null) {
    			        outputStream.close();
    			    }
    			}
			}
		} catch (Exception e) {
		    MessageHandler.error("Error creating XML.", e);
		}
	}
	
	/** Find the module whose source directory contains a NamedObj.
	 *  Returns null if not found.
	 */
	public static Module findModuleSrcDirForNameObj(NamedObj namedObj) throws ClassNotFoundException {

        ModuleTree tree = ModuleTree.instance();

	    String className = namedObj.getClass().getName();
	    
	    // see if it's a ptolemy class
	    if(className.startsWith("ptolemy")) {
	        if(namedObj instanceof Director) {
	            return tree.getModuleByStemName("directors");
	        } else {
	            return tree.getModuleByStemName("actors");
	        }
	    }
	    
	    String fileName = _getFileName(className);
	    if(fileName != null) {
    	    for(Module module : tree.getModuleList()) {
    	        if(fileName.startsWith(module.getSrc().getAbsolutePath())) {
    	            return module;
    	        }
    	    }
	    }
	    return null;
	}
		
	/** Try to guess the semantic types for a NamedObj. */
	private static Set<String> _guessSemanticTypes(NamedObj namedObj) {
	    
	    Set<String> types = new HashSet<String>();
	    
	    Matcher matcher;
	    
	    final String namedObjName = namedObj.getName();
	    final String namedObjClassName = namedObj.getClass().getName();
	    final KeplerDocumentationAttribute doc = 
	            (KeplerDocumentationAttribute) namedObj.getAttribute("KeplerDocumentation");
	    final String userLevelDocLowerCase = doc.getUserLevelDocumentation().toLowerCase();
	    
	    if(namedObjName.contains("Array")) {
	        types.add("urn:lsid:localhost:onto:2:1#DataArrayOperation");
	    }
	    
	    matcher = _CONVERT_FROM_TO_PATTERN.matcher(namedObjName);
	    if(namedObjName.contains("Assembler") ||
	        namedObjName.contains("Disassembler") ||
	        namedObjName.contains("Updater") ||
	        (matcher.find() && !(namedObjName.contains("String")))) {
	        types.add("urn:lsid:localhost:onto:2:1#DataStructureOperation");
	    }
	    
	    if(namedObjName.contains("String")) {
            types.add("urn:lsid:localhost:onto:2:1#DataStringOperation");	        
	    }
	    
	    if(namedObjName.endsWith(("Average")) ||
	        namedObjName.endsWith("Maximum") ||
	        namedObjName.endsWith(("Minimum"))) {
	        types.add("urn:lsid:localhost:onto:2:1#StatisticalOperation");
	    }
	    
	    if(namedObjName.contains("Matrix")) {
	        types.add("urn:lsid:localhost:onto:2:1#MatrixOperation");
	    }
	    
	    if(namedObj instanceof RandomSource) {
	        types.add("urn:lsid:localhost:onto:2:1#RandomNumberOperation");
	    }
	    
	    if(namedObjClassName.startsWith("ptolemy.actor.lib.comm")) {
	        types.add("urn:lsid:localhost:onto:2:1#Communications");
	    }
	    
	    if(namedObjClassName.startsWith("ptolemy.actor.lib.logic")) {
	        types.add("urn:lsid:localhost:onto:2:1#BooleanControl");
	    }
	    
	    if(userLevelDocLowerCase.contains("filter") &&
            (userLevelDocLowerCase.contains("adaptive") ||
            userLevelDocLowerCase.contains("recursive") ||
            userLevelDocLowerCase.contains("lattice") ||
            userLevelDocLowerCase.contains("impulse response"))) {
            types.add("urn:lsid:localhost:onto:2:1#Filtering");
        }
	    
	    if(namedObjName.endsWith("Select") ||
	        namedObjName.endsWith("Switch")) {
	        types.add("urn:lsid:localhost:onto:2:1#WorkflowControl");
	        
	        if(namedObjName.contains("Boolean")) {
	            types.add("urn:lsid:localhost:onto:2:1#BooleanControl");
	        }
	    }
	    
	    if(namedObjName.contains("Time") ||
	        (namedObj instanceof TimedSource)) {
	        types.add("urn:lsid:localhost:onto:2:1#Time");
	    }
	    
	    if(namedObj instanceof TypedCompositeActor) {
	        types.add("urn:lsid:localhost:onto:2:1#Workflow");
	    }
	    
	    if((namedObj instanceof WebContent) ||
	        namedObjClassName.startsWith("ptolemy.vergil.basic.export.web")) {
	        types.add("urn:lsid:localhost:onto:2:1#WorkflowWebExport");
	    }
	        
	    if(namedObjName.contains("XML") ||
	        namedObjName.contains("XSLT")) {
	        types.add("urn:lsid:localhost:onto:2:1#XMLProcessor");
	    }
	    	    
	    return types;
	}
	    
	/** Returns true if the specified class appears more than once
	 *  in the cache content table.
	 */
	private static boolean _classHasMultipleDocs(String className) throws Exception {
	    
	    if(_numClassDocFilesMap.isEmpty()) {
	        Connection conn = null;
	        Statement st = null;
	        ResultSet result = null;
	        try {
	            conn = DatabaseFactory.getDBConnection();
	            st = conn.createStatement();
	            	            
	            result = st.executeQuery("select classname, count(classname) from cachecontenttable group by classname");
	            
	            while(result.next()) {
	                _numClassDocFilesMap.put(result.getString(1), result.getInt(2));
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
	    
	    Integer count = _numClassDocFilesMap.get(className);
	    if(count == null) {
	        System.out.println("Class " + className + " not found in cache.");
	        return true;
	    }
	    return count > 1;
	}
	
	/**
	 * Change property (parameter) and port names in a KeplerDocumentationAttribute from
	 * the class field name to the name return by getName().
	 */
	private static void _fixNames(NamedObj namedObj,
			KeplerDocumentationAttribute doc, boolean printWhenChangeName) {
		Class<?> clazz = namedObj.getClass();

		// get the property names
		Map<String, String> docNameTable = new HashMap<String, String>(
				doc.getPropertyHash());
		docNameTable.putAll(doc.getPortHash());
		for (String docName : docNameTable.keySet()) {

			try {
				// get the field for this property name
				Field field = clazz.getField(docName);
				if (field == null) {
					System.out.println("ERROR: unable to find field for "
							+ docName);
				} else {
					// get the NamedObj for this field
					NamedObj fieldNamedObj = (NamedObj) field.get(namedObj);
					if (fieldNamedObj != null) {
					    
					    // if the property is nested within another property, 
					    // do not create docs for it
					    if(fieldNamedObj.getContainer() != namedObj) {
					        doc.removeProperty(docName);
					        continue;
					    }
					    
						// get the real name
						String fullName = fieldNamedObj.getName();

						if(!fullName.equals(docName)) {
    						
						    // remove the old name from the doc attribute and
						    // add the new one
						    String value;
						    if(fieldNamedObj instanceof Port) {
                                value = doc.removePort(docName);
                                doc.addPort(fullName, value);
						    } else {
                                value = doc.removeProperty(docName);
                                doc.addProperty(fullName, value);
						    }
    
    						if(printWhenChangeName) {
    							System.out.println("changed name " + docName
    								+ " --> " + fullName);
    						}
    					}
					}
				}
			} catch (Exception e) {
				System.out.println(e.getClass() + " ERROR: " + e.getMessage());
			}
		}
	}
		
	/** Get the next actor LSID from the README file. This method also
	 *  updates the README file.
	 */
	private static String _getNextActorLSIDFromReadme() {
	    
	    try {
	        
            ModuleTree modules = ModuleTree.instance();
            Module module = modules.getModuleByStemName("actors");
            String readmePath = module.getKarResourcesDir() + File.separator + "README";
	        
	        BufferedReader reader = new BufferedReader(new FileReader(readmePath));
	        StringBuilder buf = new StringBuilder();
	        
	        Matcher matcher = null;
	        String line = reader.readLine();
	        while(line != null) {
	            
	            // see if line matches
	            matcher = _LAST_ACTOR_ID_PATTERN.matcher(line);
	            if(matcher.matches()) {
	                break;
	            } 
	            
	            buf.append(line);
	            line = reader.readLine();
	            if(line != null) {
	                buf.append(System.getProperty("line.separator"));
	            }
	        }
	        	        
	        if(matcher == null) {
	            System.out.println("WARNING: could not read LSID README file.");
	            reader.close();
	            return null;
	        }
	        
	        // parse last id from line and increment
	        String idStr = matcher.group(1);
            int id = Integer.parseInt(idStr);
            id++;
            System.out.println("  incrementing README id to " + id);
	        	        
	        // write id line to file
            buf.append(_LAST_ACTOR_ID_PREFIX + id + System.getProperty("line.separator"));
	        
	        // write remaining file
	        line = reader.readLine();
	        while(line != null) {
	            buf.append(line);
	            line = reader.readLine();
	            if(line != null) {
	                buf.append(System.getProperty("line.separator"));
	            }
	        }
	        reader.close();
	        
	        BufferedWriter writer = new BufferedWriter(new FileWriter(readmePath));
	        writer.write(buf.toString());
	        writer.close();
	        
	        
	        return "urn:lsid:kepler-project.org:actor:" + id + ":1";
	        
	    } catch (IOException e) {
	        System.out.println("Error updating LSID README file: " + e.getMessage());
	        return null;
	    }
	}
	
	/** Get a file name from a class name or file name. */
	private static String _getFileName(String name) throws ClassNotFoundException {
	    // see if it's a file name
	    if(name.endsWith(".java")) {
	        return name;
	    } else {
	        // looks like a class name. see if we can figure out the
	        // file name.
	        String fileName = SuperClassPathFinderDoclet.getFileNameForClassName(name);
	        if(fileName == null) {
	            //System.out.println("ERROR: skipping " + name +
	                //" since could not determine source file name.");
	            return null;
	        }
	        return fileName;
	    }
	}
	
    /** Get a class name from a file name or class name. */
    private static String _getClassName(String name) {

        // if it looks like a file name, assume it's a class name
        if (!name.endsWith(".java")) {
            return name;
        }

        String className = _filenameToClassMap.get(name);
        if (className == null) {
            
            Main.execute(new String[] { "-quiet", "-doclet",
                    "org.kepler.kar.SuperClassPathFinderDoclet", name });
            className = SuperClassPathFinderDoclet.getClassName();
                        
            _filenameToClassMap.put(name, className);
            //System.out.println("file name " + name + " class name " + className);
            
        }
        return className;

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
	
	private static KeplerDocumentationAttribute _generateDocsFromDoclet(String fileName) {

	    // find parent files for this class
        Main.execute(new String[] {
                "-quiet",
                "-doclet",
                "org.kepler.kar.SuperClassPathFinderDoclet", fileName });
        
        final Set<String> classFiles = SuperClassPathFinderDoclet.getClassFiles();
        final String className = SuperClassPathFinderDoclet.getClassName();
        _filenameToClassMap.put(fileName, className);
        
        if(classFiles.isEmpty()) {
            System.out.println("ERROR: could not find files of super classes of " + fileName);
            return null;
        }

        // construct documentation for the class using its source file, and
        // those of its super classes
        final String[] args = new String[3 + classFiles.size()];
        args[0] = "-quiet";
        args[1] = "-doclet";
        args[2] = "org.kepler.kar.KarDoclet";
        int j = 3;
        for(String filename : classFiles) {
            args[j] = filename;
            j++;
        }
        
        Main.execute(args);

        return KarDoclet.getDoc(className);
	}
	
	/** Remove parameter and port entries. TODO: remove ports */
	/*
	private static void _removeDefaultValues(NamedObj namedObj) {
	    
	    List<?> attributes = new LinkedList<Object>(namedObj.attributeList());
	    for(Object object : attributes) {
	        String name = ((Attribute)object).getName();
	        if(!name.equals("KeplerDocumentation") && 
	            !name.startsWith("semanticType")) {
	            
	            System.out.println("Removing attribute " + name);
	            
	            try {
                    ((Attribute)object).setContainer(null);
                } catch (Exception e) {
                    System.out.println("ERROR removing attribute " + name + ": " + e.getMessage());
                }
	        }
	    }
	    
	    // XXX remove ports
	    
	}
	*/
		
	private static void _shutdownCache(boolean exit) {
		// call the module deinitializers
		Kepler.shutdown();
		HSQL.shutdownServers();
		
		if(exit) {
		    // we have to call System.exit() because ???
		    System.exit(0);
		}
	}

	/** The prefix string for the last known LSID. */
	private final static String _LAST_ACTOR_ID_PREFIX = "The last known id for an actor is actor:";
	
	/** A pattern to match the last know LSID string. */
	private final static Pattern _LAST_ACTOR_ID_PATTERN = Pattern.compile(_LAST_ACTOR_ID_PREFIX + "\\s*(\\d+)");
	
	private static Map<String,Integer>_numClassDocFilesMap = new HashMap<String,Integer>();
	
	private static Map<String,String> _filenameToClassMap = new HashMap<String,String>();
	
	private static Workspace _workspace = new Workspace();
	private static MoMLParser _parser = new MoMLParser(_workspace);
	
	static {
	    KarDoclet.setWorkspace(_workspace);
	}

	/** Regex to match actor names that perform conversions. */
	private final static Pattern _CONVERT_FROM_TO_PATTERN = Pattern.compile(".+To[A-Z].+");
}
