/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-06 16:49:03 -0700 (Fri, 06 Jul 2012) $' 
 * '$Revision: 30131 $'
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

package org.kepler.gui.component;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.KeplerGraphFrame;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObjectInterface;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.ModelDirectory;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLParser;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action opens a CompositeEntity.
 */
public class OpenCompositeAction extends FigureAction {

	private static String DISPLAY_NAME = "Open";
	private static String TOOLTIP = "Open";
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	// //////////////////////////////////////////////////////////////////////////////

	private TableauFrame parent;

	private KeplerLSID lsidToOpen;
	
	/** A NamedObj to open. */
	private NamedObj _namedObjToOpen;
	
	/** The path of a MoML file to open. */
	private String _path;
	
	/** MoMLParser to parse a MoML file. */
	private MoMLParser _parser;

	public void setLsidToOpen(KeplerLSID lsid) {
		lsidToOpen = lsid;
	}

	public KeplerLSID getLsidToOpen() {
		return lsidToOpen;
	}
	
	/** Set the path of a MoML file to be opened. */
	public void setFilePath(String path) {
	    _path = path;
	}

	private final static Log log = LogFactory.getLog(OpenCompositeAction.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * Constructor
	 * 
	 *@param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public OpenCompositeAction(TableauFrame parent) {
		super("Open Archive (KAR)");
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"OpenCompositeAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.parent = parent;

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);
	}

	public OpenCompositeAction(String name, TableauFrame parent) {
	    super(name);
	    this.parent = parent;
	}
	
	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		if(_path != null) {
		    if(_parser == null) {
		        _parser = new MoMLParser();
		    }
		    _parser.reset();
		    try {
                _namedObjToOpen = _parser.parseFile(_path);
            } catch (Exception exception) {
                MessageHandler.error("Unable to open " + _path, exception);
            }
		}
		
		if(_namedObjToOpen != null) {
		    Configuration configuration = parent.getConfiguration();
		    if(configuration != null) {
		        try {
		            configuration.openModel(_namedObjToOpen);
                } catch (Exception exception) {
                    MessageHandler.error("Unable to open " + _namedObjToOpen.getName(), exception);
                    return;
                }    		    
            }

		    // if we successfully opened a file, update the history menu and
		    // set the last directory.
		    if(_path != null) {
                // update the history menu
                if(parent instanceof KeplerGraphFrame) {
                    try {
                        ((KeplerGraphFrame)parent).updateHistory(_path);
                    } catch (IOException exception) {
                        MessageHandler.error("Unable to update history menu.", exception);
                    }
                }
                if(parent instanceof BasicGraphFrame) {
                    ((BasicGraphFrame)parent).setLastDirectory(new File(_path).getParentFile());
                }
		    }
		    
		    return;
		}
		
		try {

			KeplerLSID lsid = getLsidToOpen();

			CacheManager cache = CacheManager.getInstance();

			// get the object from the cache (it is GraphicalActorMetadata even
			// though it is a workflow)
			CacheObjectInterface co = cache.getObject(lsid);
			if (co == null) {

				if (isDebugging)
					log.debug(lsid + " was not found in the cache");

			} else {

				Object o = co.getObject();
				if (isDebugging)
					log.debug(o.getClass().getName());

				if (o instanceof ActorMetadata) {
					ActorMetadata am = (ActorMetadata) o;
					try {
						// get the workflow from the metadata
						NamedObj entity = am.getActorAsNamedObj(null);

						if (isDebugging)
							log.debug(entity.getName() + " "
									+ NamedObjId.getIdFor(entity) + " "
									+ entity.getClass().getName());
						if (entity instanceof CompositeEntity) {
							if (isDebugging)
								log.debug("Opening CompositeEntity");

							// get the xml representation - needs parsing to be
							// correct!
							String moml = entity.exportMoML();
							MoMLParser parser = new MoMLParser();
							entity = parser.parse(moml);

							Configuration configuration = parent
									.getConfiguration();

							// TODO check on this
							// ----begin questionable title bar code:
							PtolemyEffigy effigy = new PtolemyEffigy(
									configuration.workspace());
							effigy.setModel(entity);
							ModelDirectory directory = (ModelDirectory) configuration
									.getEntity("directory");
							effigy.setName(entity.getName()); 
							// is this the right name for the effigy?
							effigy.identifier.setExpression(entity.getName());
							if (directory != null) {
								if (directory.getEntity(entity.getName()) != null) {
									// Name is already taken.
									int count = 2;
									String newName = effigy.getName() + " "
											+ count;
									while (directory.getEntity(newName) != null) {
										newName = effigy.getName() + " "
												+ ++count;
									}
									effigy.setName(newName);
								}
							}
							effigy.setContainer(directory);
							// ---end questionable title bar code

							// open a new window for the workflow
							configuration.openModel(entity);
						} else if (entity instanceof ComponentEntity) {
							// TODO handle opening of ComponentEntities??
							JOptionPane.showMessageDialog(null, "This component is not openable.");
						} else {
							if (isDebugging)
								log.debug("Not a CompositeEntity");
							JOptionPane.showMessageDialog(null, "This component is not openable.");
						}

					} catch (Exception ex) {
						ex.printStackTrace();
						log.error("error opening the workflow: "
								+ ex.getMessage());
					}
				} else {
					JOptionPane.showMessageDialog(null, "This component is not openable.");
				}
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

    public void setNamedObjToOpen(NamedObj obj) {
        _namedObjToOpen = obj;
    }
}