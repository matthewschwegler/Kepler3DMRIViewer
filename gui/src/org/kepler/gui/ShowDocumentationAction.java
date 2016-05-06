/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:25 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31122 $'
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Vector;

import javax.swing.tree.TreePath;

import org.kepler.kar.KARCacheContent;
import org.kepler.kar.KARCacheManager;
import org.kepler.kar.KarDoclet;
import org.kepler.kar.SuperClassPathFinderDoclet;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.PtolemyFrame;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.actor.DocEffigy;
import ptolemy.vergil.basic.GetDocumentationAction;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;
import ptolemy.vergil.toolbox.FigureAction;


/**
 * This action displays Kepler documentation. Created to display when actor in
 * library tree is right clicked.
 * 
 *@author Chad Berkley
 *@since 6/5/2007
 */
public class ShowDocumentationAction extends FigureAction {
	private final static String LABEL = "View Documentation";
	private TableauFrame parent = null;
	private PtolemyFrame pFrame = null;
	private KeplerLSID _lsidToView = null;
	private TreePath _path;

	/**
	 * Constructor
	 * 
	 *@param path
	 *            the TreePath where the actor is being removed.
	 */
	public ShowDocumentationAction(TreePath path, Component parent) {
		super(LABEL);
		_path = path;
	}
    
    public ShowDocumentationAction(TableauFrame parent) {
        super("");
        if (parent == null) {
            IllegalArgumentException iae = new IllegalArgumentException(
                    "ShowDocumentationAction constructor received NULL argument for TableauFrame");
            iae.fillInStackTrace();
            throw iae;
        }
        this.parent = parent;    
    }
	
	public void setLsidToView(KeplerLSID lsid) {
		_lsidToView = lsid;
	}
	public KeplerLSID getLsidToView() {
		return _lsidToView;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		NamedObj target = null;
		KeplerLSID lsidToView = getLsidToView();
		
		try {
			
			if (lsidToView != null) {
				target = ObjectManager.getInstance().getObjectRevision(lsidToView);
			} else {
				target = getTarget();
			}	
			
			if(target == null) {
			    Object object = _path.getLastPathComponent();
			    if(object instanceof NamedObj) {
			        target = (NamedObj) object;
			    }
			}
			
			
			NamedObj container = null;
			if (pFrame != null){
				container = pFrame.getModel();
			}
			
			if (container == null) {
				container = target.getContainer();
			}
			
			Effigy ee = Configuration.findEffigy(container);
			Configuration c = null;
			
			if (ee != null){
				c = (Configuration) ee.toplevel();
			}
			else{
				if (parent != null){
					c = parent.getConfiguration();
				}
			}	
			GetDocumentationAction gda = new GetDocumentationAction(1);
			gda.setConfiguration(c);
			DocEffigy de = null;
			if (ee != null){
				try {
					de = new DocEffigy(ee, ee.uniqueName("DocEffigy"));
					gda.setEffigy(de);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
			if (target != null) {

				boolean missingDocs = false;
				
		        final KeplerDocumentationAttribute keplerDocumentationAttribute = (KeplerDocumentationAttribute) target
		                .getAttribute("KeplerDocumentation");
		        
		        // see if the docs are missing or empty
		        if(keplerDocumentationAttribute == null) {
		        	System.out.println("No KeplerDocumentation attribute.");
		        	missingDocs = true;
		        } else {
		        	ConfigurableAttribute userLevelDoc = 
		        			(ConfigurableAttribute) keplerDocumentationAttribute.getAttribute("userLevelDocumentation");
			        if(userLevelDoc == null) {
			        	System.out.println("No userLevelDocumentation attribute.");
			        	missingDocs = true;
			        } else {
			        	String uldStr = userLevelDoc.getConfigureText();
			        	if(uldStr == null || uldStr.trim().isEmpty() || uldStr.trim().equals("null")) {
			        		System.out.println("UserLevelDocumentation appears to be empty.");
			        		missingDocs = true;
			        	}
			        }
		        }
		        
		        if(missingDocs) {
		        	
		        	System.out.println("Missing documentation for " + target.getFullName());
		        	System.out.println("Will try to generate from javadocs.");
		        	
		        	// see if we have javadoc api
		        	
		        	Method javadocMethod = null;
		        	try {
		        		Class<?> clazz = Class.forName("com.sun.tools.javadoc.Main");
		        		javadocMethod = clazz.getMethod("execute", String[].class);
		        	} catch(Exception e1) {
		        		System.err.println("ERROR: javadoc API not in class path.");
		        	}
		        	
		        	if(javadocMethod != null) {
			        	
				        final String fileName = SuperClassPathFinderDoclet.getFileNameForClassName(target.getClassName());
				        if(fileName == null) {
				        	System.err.println("ERROR: unable to get file name for class " + target.getClassName());
				        } else {
				        	
						    // find parent files for this class
				        	javadocMethod.invoke(null, new Object[]{ new String[] {
						            "-quiet",
						            "-doclet",
						            "org.kepler.kar.SuperClassPathFinderDoclet", fileName }});
					    
						    final String className = SuperClassPathFinderDoclet.getClassName();
						    final Set<String> classFiles = SuperClassPathFinderDoclet.getClassFiles();
					    
						    if(classFiles.isEmpty()) {
						    	System.out.println("ERROR: could not find files of super classes of " + fileName);
						    } else {
		
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
							    
							    javadocMethod.invoke(null, new Object[]{args});
								
								// replace the kepler documentation attribute with the generated one
				                final KeplerDocumentationAttribute docletDocAttribute = KarDoclet.getDoc(className);
				                target.removeAttribute(keplerDocumentationAttribute);
				                ((KeplerDocumentationAttribute) docletDocAttribute.clone(target.workspace())).setContainer(target);
				                missingDocs = false;
						    }	
					    }
		        	}
		        }
		        
		        // see if docs are still missing
		        if(missingDocs) {
		        	
		        	System.out.println("Will try to load from Kepler cache.");
		        	
		        	// try to load from cache
		            final KARCacheManager kcm = KARCacheManager.getInstance();
		            final Vector<KARCacheContent> list = kcm.getKARCacheContents();

		            final String targetClassName = target.getClassName();
			        for(KARCacheContent content : list) {
			            String cacheClassName = content.getCacheContent().getClassName();
			            
			            // make sure class name is not null.
			            // it's null for, e.g., workflow runs and report layouts
			            if(cacheClassName != null) {

			            	if(cacheClassName.equals(targetClassName)) {

					        	KeplerLSID lsid = content.getLsid(); 
					        	if(lsid != null) {
									CacheObject cacheObject = CacheManager.getInstance().getHighestCacheObjectRevision(lsid);
									if(cacheObject != null && (cacheObject instanceof ActorCacheObject)) {
										ActorMetadata metadata = ((ActorCacheObject)cacheObject).getMetadata();
										if(metadata != null) {
											final KeplerDocumentationAttribute cacheDocAttribute = metadata.getDocumentationAttribute();
											if(cacheDocAttribute != null) {
												target.removeAttribute(keplerDocumentationAttribute);
												((KeplerDocumentationAttribute) cacheDocAttribute.clone(target.workspace())).setContainer(target);
												missingDocs = false;
											}
										}
									}
					        	}
					        	break;
			            	}
			            }
			        }
		        }
				
		        if(!missingDocs) {
		            gda.showDocumentation(target);
		        } else {
		            MessageHandler.error("Could not find documentation for " + target.getName() +
		                ". To generate documentation, download Kepler from svn and run 'ant update-actor-doc'.");
		        }
			}
		} catch (Exception ee) {
			ee.printStackTrace();
		}
	}

	/**
	 * allows you to set the ptolemyFrame that should be used as the parent of
	 * this action.
	 */
	public void setPtolemyFrame(PtolemyFrame pFrame) {
		this.pFrame = pFrame;
	}
}