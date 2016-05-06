/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-27 11:43:22 -0700 (Wed, 27 Mar 2013) $' 
 * '$Revision: 31767 $'
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
package org.kepler.gui.kar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KARFile;
import org.kepler.kar.handlers.ActorMetadataKAREntryHandler;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObjectInterface;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLParser;
import ptolemy.util.MessageHandler;
import ptolemy.util.StringUtilities;

/** An Effigy for KAR files.
 * 
 *  @author Daniel Crawl
 *  @version $Id: KAREffigy.java 31767 2013-03-27 18:43:22Z crawl $
 */
public class KAREffigy extends PtolemyEffigy {

    public KAREffigy(Workspace workspace) {
        super(workspace);
    }

    public KAREffigy(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }
    
    /** Get the KAR file. */
    public KARFile getKARFile() {
    	return _karFile;
    }
    
    /** Open the non-actor entries in the KAR file, e.g., report layout.
     *  @param frame the frame in which to open the entries
     *  @param openActorEntries if true, open actor entries.
     */
    public void openKAREntries(TableauFrame frame, boolean openActorEntries) {
    	if(_karFile != null) {
			for (KAREntry entry : _karFile.karEntries()) {
				if (openActorEntries || !ActorMetadataKAREntryHandler.handlesClass(entry.getType())) {
					_karFile.open(entry, frame);
				}
			}
    	}
    }
    
    /** Set the KAR file. */
    public void setKARFile(KARFile karFile) {
    	_karFile = karFile;
    }
    
    /** Write the model associated with this effigy. Currently only saving
     *  the MoML is supported. 
     */
    public void writeFile(File file) throws IOException {
        String path = file.getAbsolutePath();
        // see if we're saving the MoML
        // this can happen with File->Export->XML
        if(path.toLowerCase().endsWith(".xml")) {
            super.writeFile(file);
        } else if(path.toLowerCase().endsWith(".kar")) {
            throw new IOException("writeFile() does not support writing KAR files.");
        } else {
            throw new IOException("unknown type of file: " + path);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         inner classes                     ////

    /** A factory for creating new KAR effigies. */
    public static class Factory extends PtolemyEffigy.FactoryWithoutNew {
        /** Create a factory with the given name and container.
         *  @param container The container.
         *  @param name The name.
         *  @exception IllegalActionException If the container is incompatible
         *   with this entity.
         *  @exception NameDuplicationException If the name coincides with
         *   an entity already in the container.
         */
        public Factory(CompositeEntity container, String name)
                throws IllegalActionException, NameDuplicationException {
            super(container, name);
        }
        
        /** Create a new effigy in the given container by reading the
         *  <i>input</i> URL. If the URL is a KAR file, attempt to open
         *  it and extract the model. If there are missing module dependencies,
         *  the user is asked if she wants to ignore the dependencies and
         *  try to open the KAR, download the dependencies and restart Kepler,
         *  or do nothing. In the last case this method returns null.
         *  @param container The container for the effigy.
         *  @param base The base for relative file references, or null if
         *   there are no relative file references.
         *  @param input The input URL.
         *  @return A new instance of PtolemyEffigy, or null if the URL
         *   does not specify a Ptolemy II model.
         *  @exception Exception If the URL cannot be read, or if the data
         *   is malformed in some way.
         */
        public Effigy createEffigy(CompositeEntity container, URL base,
                URL input) throws Exception {
            if (input == null) {
                return super.createEffigy(container, base, input);
            } else {
                String extension = getExtension(input).toLowerCase();
                if(extension.equals("kar")) {

	                KAREffigy effigy = new KAREffigy(container, container.uniqueName("effigy"));
	                
	                File file = new File(input.toURI());
					KARFile karf = new KARFile(file);
					
					// see if it's openable
					if(!karf.isOpenable()) {
						// see if dependencies are missing
						if(!karf.areAllModuleDependenciesSatisfied()) {
							ImportModuleDependenciesAction action = new ImportModuleDependenciesAction(new TableauFrame());
							action.setArchiveFile(file);
							ImportModuleDependenciesAction.ImportChoice choice = action.checkDependencies();
							if(choice == ImportModuleDependenciesAction.ImportChoice.DO_NOTHING) {
								return null;
							} else if(choice == ImportModuleDependenciesAction.ImportChoice.DOWNLOADING_AND_RESTARTING) {
							    action.waitForDownloadAndRestart();
							}
						} else {
							MessageHandler.error("This KAR cannot be opened.");
							return null;
						}
					}
					
					effigy.setKARFile(karf);
					karf.cacheKARContents();
	                
					// For each Actor in the KAR open the MOML
					for (KAREntry entry : karf.karEntries()) {
						KeplerLSID lsid = entry.getLSID();
						//System.out.println("Processing entry, LSID=" + lsid + ", type=" + entry.getType());
						if(isDebugging) {
						    log.debug("Processing entry, LSID=" + lsid + ", type=" + entry.getType());
						}
						
						if (ActorMetadataKAREntryHandler.handlesClass(entry.getType())) {
							// get the object from the cache (it is ActorMetadata
							// even though it is a workflow)
							CacheObjectInterface co = CacheManager.getInstance()
									.getObject(lsid);
							
							NamedObj toplevel;
							
							// see if the kar is in the cache
							// KARFile.cacheKARContents() was called above, but this may
							// not put the KAR in the cache.
							if(co == null) {
							    
							    MoMLParser parser = new MoMLParser();
							    
							    parser.reset();
							    
							    InputStream stream = null;
							    try {
							        stream = karf.getInputStream(entry);
							        toplevel = parser.parse(base, file.getCanonicalPath(), stream);
							    } finally {
							        if(stream != null) {
							            stream.close();
							        }
							    }
							    
							} else {

							    ActorMetadata am = (ActorMetadata) co.getObject();
	
    							// get the workflow from the metadata
    							final NamedObj namedObj = am.getActorAsNamedObj(null);
    							
    			                 if (namedObj instanceof CompositeEntity) {

    			                        // get the xml representation - needs parsing to be
    			                        // correct!
    			                        String moml = namedObj.exportMoML();
    			                        MoMLParser parser = new MoMLParser();
    			                        toplevel = parser.parse(moml);
    			                    } else {
    			                        MessageHandler.error("The contents of this KAR are not openable.");
    			                        return null;
    			                    }

    							
    		                    // The cloning process results an object that defers change
    		                    // requests.  By default, we do not want to defer change
    		                    // requests, but more importantly, we need to execute
    		                    // any change requests that may have been queued
    		                    // during cloning. The following call does that.
    		                    toplevel.setDeferringChangeRequests(false);
							}
							
							// make sure we were able to parse or load the NamedObj
							if(toplevel == null) {
							    return null;
							}
							
							effigy.setModel(toplevel);
							
	                        URI inputURI = null;
	
	                        try {
	                            inputURI = new URI(input.toExternalForm());
	                        } catch (java.net.URISyntaxException ex) {
	                            // This is annoying, if the input has a space
	                            // in it, then we cannot create a URI,
	                            // but we could create a URL.
	                            // If, under Windows, we call
	                            // File.createTempFile(), then we are likely
	                            // to get a pathname that has space.
	                            // FIXME: Note that jar urls will barf if there
	                            // is a %20 instead of a space.  This could
	                            // cause problems in Web Start
	                            String inputExternalFormFixed = StringUtilities
	                                    .substitute(input.toExternalForm(),
	                                            " ", "%20");
	
	                            try {
	                                inputURI = new URI(inputExternalFormFixed);
	                            } catch (Exception ex2) {
	                                throw new Exception("Failed to generate "
	                                        + "a URI from '"
	                                        + input.toExternalForm()
	                                        + "' and from '"
	                                        + inputExternalFormFixed + "'", ex);
	                            }
	                        }
	
	                        // This is used by TableauFrame in its
	                        //_save() method.
	                        effigy.uri.setURI(inputURI);
						}
					}
	                
	                return effigy;
                }
            }
            return null;
        }
    }
        
	private static final Log log = LogFactory.getLog(KAREffigy.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/** The KAR file. */
	private KARFile _karFile;	
}
