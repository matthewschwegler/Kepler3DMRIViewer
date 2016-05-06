/*
 * Copyright (c) 2004-2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-04-09 10:51:01 -0700 (Tue, 09 Apr 2013) $' 
 * '$Revision: 31877 $'
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.KeplerGraphFrame;
import org.kepler.kar.KARFile;
import org.kepler.kar.KARManager;
import org.kepler.kar.SaveKAR;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.repository.Repository;
import org.kepler.objectmanager.repository.RepositoryManager;
import org.kepler.sms.SMSServices;
import org.kepler.sms.gui.SemanticTypeEditor;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.FileUtil;
import org.kepler.util.RenameUtil;

import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.ExtensionFilenameFilter;
import ptolemy.gui.JFileChooserBugFix;
import ptolemy.gui.PtFileChooser;
import ptolemy.gui.PtGUIUtilities;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * This action exports a workflow as a kar file. Subclasses of this class should
 * be used for saving any type of KAR file. To make a subclass you will want to
 * override the initialize method and the handle action method. See
 * ExportActorArchiveAction for an example subclass.
 * 
 * @author Aaron Schultz, Christopher Brooks
 */
public class ExportArchiveAction extends FigureAction {

	private static final Log log = LogFactory.getLog(ExportArchiveAction.class);
	private static final boolean isDebugging = log.isDebugEnabled();
	
	// "A" should probably be used for a too-be-implemeted Edit Select All.
	//private static KeyStroke ACCELERATOR_KEYSTROKE = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, 
	//		Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

	protected TableauFrame _parent;

	protected boolean _saveSucceeded = false;

	/**
	 * The SaveKAR object is a GUI free helper class for saving KARs.
	 */
	protected SaveKAR _savekar = null;

	/**
	 * A string that represents to the user the type of KAR that is being
	 * exported. This string may appear in dialog messages to allow different
	 * callers to better identify to the user what the contents of this KAR file
	 * are. This should be the first thing set in the handleType method of any
	 * subclasses of this class but is not required.
	 */
	protected String _karType = "";

	protected boolean refreshFrameAfterSave = true;

	protected boolean _mapKARToCurrentFrame = true;

	/**
	 * The purpose of KARs are for grouping things together. Sometimes though we
	 * have only one thing in a KAR which changes some things. This is a bit
	 * confusing here because this refers to a single item being saved from the
	 * user perspective. Just because we're saving a single item at this stage
	 * does not mean there will only be one file in the KAR. There may be other
	 * files associated with the single item that we're saving from this class.
	 * For example we may be saving a single workflow that has many files
	 * associated with it.
	 */
	protected boolean _singleItemKAR = false;

	protected String _defaultFileName = null;

	protected String _overrideModuleDependencies = null;
	
	protected boolean _upload = true;
	
	protected File _saveFile = null;
	
	protected boolean _nonInteractiveSave = false;

    /** The workflow associated with the parent frame. */
	private ComponentEntity _workflow = null;
	
	/**
	 * Call this before actionPerformed to do a 
	 * non-interactive Save, not a Save As...
	 * and don't attempt upload.
	 * @param saveFile
	 */
	public void setSaveFile(File saveFile){
		_saveFile = saveFile;
		_nonInteractiveSave = true;
		setUpload(false);
	}

	public boolean isSingleItemKAR() {
		return _singleItemKAR;
	}

	public void setSingleItemKAR(boolean singleItemKAR) {
		_singleItemKAR = singleItemKAR;
	}
	
	/**
	 * @return boolean controlling if upload should
	 * occur
	 */
	public boolean doUpload(){
		return _upload;
	}
	
	/**
	 * Set false if you don't want user prompted to upload
	 * after save if they have a remote repository selected.
	 * @param upload
	 */
	public void setUpload(boolean upload){
		_upload = upload;
	}

	public SaveKAR getSaveKAR() {
		return _savekar;
	}

	/**
	 * Set default KAR save filename.
	 * 
	 * @param name
	 */
	public void setDefaultFileName(String name) {
		_defaultFileName = name;
	}

	/**
	 * Get default KAR save filename.
	 * 
	 * @return _defaultFileName
	 */
	public String getDefaultFileName() {
		return _defaultFileName;
	}

	/**
	 * @return the refreshFrameAfterSave
	 */
	public boolean isRefreshFrameAfterSave() {
		return refreshFrameAfterSave;
	}

	/**
	 * Allow for toggling the close/open of the main frame after saving. This
	 * makes sense for workflows but not for other types of saves like actors.
	 * 
	 * @param refreshFrameAfterSave
	 *            the refreshFrameAfterSave to set
	 */
	public void setRefreshFrameAfterSave(boolean refreshFrameAfterSave) {
		this.refreshFrameAfterSave = refreshFrameAfterSave;
	}

	/**
	 * Set true when this KAR should not be mapped to this JFrame. 
	 */
	public void setMapKARToCurrentFrame(boolean mapKARToCurrentFrame){
		_mapKARToCurrentFrame = mapKARToCurrentFrame;
	}
	public boolean mapKARToCurrentFrame(){
		return _mapKARToCurrentFrame;
	}

	/**
	 * Convenience reference to the LocalRepositoryManager.
	 */
	protected LocalRepositoryManager _localRepositoryManager;

	/**
	 * prevent the file chooser from displaying and just use a temp file
	 */
	public boolean useTempFile = false;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public ExportArchiveAction(TableauFrame parent) {
		super("");

		//putValue(ACCELERATOR_KEY, ACCELERATOR_KEYSTROKE);
		
		_parent = parent;

		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"ExportArchiveAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}

		_localRepositoryManager = LocalRepositoryManager.getInstance();

		_savekar = new SaveKAR();

		initialize();
	}

	/**
	 * The initialize method is called at the end of the public constructor.
	 * This makes it easier to change the behavior of constructing a subclass.
	 */
	protected void initialize() {

		_karType = "workflow";

		this.setSingleItemKAR(true);

		this.putValue("tooltip", "Save a KAR file archive.");

	}

	/**
	 * Invoked when an action occurs.
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		_saveSucceeded = false;

		// grab the new KAR lsid after the file is saved
		// in case we need it for later
		KeplerLSID theNewKARLSID = null;

		
		// save the window size, position, and zoom
        if (_parent instanceof BasicGraphFrame) {
            try {
                ((BasicGraphFrame) _parent).updateWindowAttributes();
            } catch (Exception exception) {
                MessageHandler
                        .error("Failed to save window size, position and zoom while writing KAR.",
                                exception);
            }
        }		
		
		// ////////////////////////////////////////////////
		// Only this part should be different depending on
		// where the KAR is being exported from
		boolean continueExport = handleAction(e);
		if (!continueExport) {
			return;
		}
		// ////////////////////////////////////////////////

		// Force single item KAR if there is only one
		// item in the KAR
		// This may or may not be good/necessary
		if (_savekar.getSaveInitiatorList().size() == 1) {
			setSingleItemKAR(true);
		} else {
			setSingleItemKAR(false);
		}

		File saveFile = null;

		if (_nonInteractiveSave) {
			saveFile = _saveFile;
		} else {
			if (useTempFile) {

				// Use a temporary file for saving to in order to simulate
				// the old upload to repository function
				// don't really want to be doing this...
				ComponentEntity ce = _savekar.getSaveInitiatorList().get(0);
				String tempFileName = ce.getName() + ".kar";
				File tempDir = DotKeplerManager.getInstance()
						.getTransientModuleDirectory("core");
				saveFile = new File(tempDir, tempFileName);

			} else {
                // Create a file filter that accepts .kar files.
                ExtensionFilenameFilter filter = new ExtensionFilenameFilter("kar");
    			// Avoid white boxes in file chooser, see
    			// http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3801
    			JFileChooserBugFix jFileChooserBugFix = new JFileChooserBugFix();
    			Color background = null;
    			PtFileChooser chooser = null;

    			try {
    				background = jFileChooserBugFix.saveBackground();
    				chooser = new PtFileChooser(_parent, "Save",
    						JFileChooser.SAVE_DIALOG);
    				if(_parent instanceof BasicGraphFrame) {
    					chooser.setCurrentDirectory(((BasicGraphFrame)_parent).getLastDirectory());
    				}
    				chooser.addChoosableFileFilter(filter);
    				
    				if(_defaultFileName != null) {
    					chooser.setSelectedFile(new File(_defaultFileName));
    				}

    				int returnVal = chooser.showDialog(_parent, "Save");
    				if (returnVal == JFileChooser.APPROVE_OPTION) {
    					// process the given file
    					saveFile = chooser.getSelectedFile();
    					
    					if(saveFile.exists() && !PtGUIUtilities.useFileDialog()) {
                            if (!MessageHandler.yesNoQuestion("Overwrite \""
                                    + saveFile.getName() + "\"?")) {
                            	saveFile = null;
                            }
    					}
    				}    				
    			} finally {
    				jFileChooserBugFix.restoreBackground(background);
    			}

			}
		}

		if (saveFile == null)
			return;

		_savekar.setFile(saveFile);

		// see if the name has changed.
		
		// make sure there's a reference to the workflow; it could be null when,
		// e.g., exporting a run in the WRM.
		if(_workflow != null) {
    		String newName = FileUtil.getFileNameWithoutExtension(saveFile.getName());
    		if(!newName.equals(_workflow.getName())) {
    			try {
    				// rename the frame title and workflow xml file in the kar
    				RenameUtil.renameComponentEntity(_workflow, newName);
    			} catch(Exception exception) {
    				MessageHandler.error("Error renaming workflow.", exception);
    			}
    		}
		}
		
		// See if this file already exists
		if (_savekar.getFile().exists()) {
			try {
				// Get the LSID of the existing kar
				KARFile existingKARFile = new KARFile(_savekar.getFile());
				KeplerLSID existingFileLSID = existingKARFile.getLSID();
				// Delete the old kar from the library
				LibraryManager lm = LibraryManager.getInstance();
				lm.deleteKAR(_savekar.getFile());
				// increment the revision and set it
				existingFileLSID.incrementRevision();
				_savekar.specifyLSID(existingFileLSID);
				// save the new kar file
				theNewKARLSID = _savekar.saveToDisk(_parent, _overrideModuleDependencies);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} else {
			theNewKARLSID = _savekar.saveToDisk(_parent, _overrideModuleDependencies);
		}

		if (theNewKARLSID != null) {
			_saveSucceeded = true;
			
			// Add JFrame=>KARFile mapping to KARManager, unless ifRefreshFrameAfterSave, 
			// in which case the mapping is added during ActorMetadataKAREntry.open, or if
			// mapKARToCurrentFrame was explicitly set false.
			if (!isRefreshFrameAfterSave() && mapKARToCurrentFrame()){
				try {
					KARFile karFile = new KARFile(_savekar.getFile());
					KARManager.getInstance().add(_parent, karFile);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			if(_parent instanceof KeplerGraphFrame) {
				try {
					((KeplerGraphFrame) _parent).updateHistory(_savekar.getFile().getAbsolutePath());
				} catch (IOException e1) {
					MessageHandler.error("Unable to update history menu.", e1);
				}
			}
			if(_parent instanceof BasicGraphFrame) {
				((BasicGraphFrame)_parent).setLastDirectory(saveFile.getParentFile());
			}
		}

		// After the KAR has been saved to disk we add it to the cache
		// then add it to the library and refresh the JTrees
		if (!useTempFile) {

			// If it is saved in a local repository update the library
			LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
			if (lrm.isInLocalRepository(_savekar.getFile())) {

				_savekar.saveToCache();

				LibraryManager lm = LibraryManager.getInstance();
				try {
					lm.addKAR(_savekar.getFile());
				} catch (Exception e2) {
					JOptionPane.showMessageDialog(_parent,
							"Error adding kar to library: " + e2.getMessage());
				}
				try {
					lm.refreshJTrees();

				} catch (IllegalActionException e2) {
					e2.printStackTrace();
				}
			} else {
				// JOptionPane.showMessageDialog(_parent,
				// "KAR file successfully saved to a folder that is not designated as a Local Repository.  In order for the contents of this KAR to show up in the component library, you can move the KAR file to a Local Repository and restart Kepler, or you can add the folder as a local repository by using the Component 'Sources' button.");
			}
		}

		// Now we
		// 1. check to see if there is a remote repository selected by the user
		// for saving
		// 2. double check with the user that they want to send the KAR
		// to the remote repository,
		// 3. then upload it
		try {
			if (_upload) {
				RepositoryManager rm = RepositoryManager.getInstance();
				Repository remoteRepo = rm.getSaveRepository();
				if (remoteRepo != null) {
					boolean continueWithUpload = true;
					int choice = JOptionPane.showConfirmDialog(_parent,
							"Would you like to upload this KAR to the remote repository?\n"
									+ "     " + remoteRepo.getName() + " at "
									+ remoteRepo.getRepository() + "\n",
							"Upload To Repository", JOptionPane.YES_NO_OPTION);
					if (choice != JOptionPane.YES_OPTION) {
						continueWithUpload = false;
					}

					if (continueWithUpload) {

						try {
							ComponentUploader uploader = new ComponentUploader(
									_parent);
							KARFile kf2upload = new KARFile(getSaveKAR()
									.getFile());
							uploader.upload(kf2upload);
						} catch (Exception ee) {
							ee.printStackTrace();
						}

					}

				}
			}

			if (isRefreshFrameAfterSave()) {
				// Open a new frame and close the old one
				KARFile karf;
				try {
					
					//move old frame close before new frame open. 
					//It will fix the bug http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5200 without having memory leak.
					karf = new KARFile(_savekar.getFile());
					karf.openKARContents(_parent, false);
					karf.close();
					
					// dispose the old window after opening a new one
					_parent.dispose();
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * @return whether or not the file was saved to disk
	 */
	public boolean saveSucceeded() {
		return _saveSucceeded;
	}

	/**
	 * This method will set up the SaveKAR object in the case of saving a
	 * workflow. To save other types of KARs a subclass that overrides this
	 * method can be used to add multiple workflows to the SaveKAR object and to
	 * identify other objects that should be added to the KAR through the
	 * appropriate KAREntryHandlers.
	 * 
	 * @param e
	 * @return boolean true if the export should continue
	 */
	protected boolean handleAction(ActionEvent e) {

		// get the workflow entity from parent
		Tableau tableau = _parent.getTableau();
		Effigy effigy = (Effigy) tableau.getContainer();
		Entity entity = null;
		if (effigy instanceof PtolemyEffigy) {
			entity = (Entity) ((PtolemyEffigy) effigy).getModel();
		}

		if (entity == null){
			return false;
		}

		if (!checkSingleObject(entity, false)) {
			return false;
		}

		_workflow = (ComponentEntity) entity;
		
		// When it's ready to go add it and continue
		_savekar.addSaveInitiator((ComponentEntity) entity);
		return true;
	}

	protected void overrideModuleDependencies(String moduleDependencies) {
		_overrideModuleDependencies = moduleDependencies;
	}

	/**
	 * Check a single NamedObj for LSID, name, and SemanticType.
	 * 
	 * @param checkIfSemenaticallyAnnotated If is true, and entity has no
	 * semantic annotations, user is warned, but not required, to add
	 * annotations.
	 * 
	 * @return Returns true if the export should continue
	 */
	protected boolean checkSingleObject(NamedObj entity,
			boolean checkIfSemenaticallyAnnotated) {

		// Make sure it has an LSID
		_savekar.checkNamedObjLSID(entity);

		if (entity instanceof ComponentEntity) {
			// Make sure it has a Name
			// Query the user for a name if needed
			try {
				_savekar.checkNamedObjName(entity);

				// FIXME: shouldn't really need this
				// LSID should be the unique identifier for the library
				/*
				 * LibraryManager lm = LibraryManager.getInstance(); if (lm ==
				 * null || lm.componentNameInUse(entity.getName())) { throw new
				 * NameDuplicationException(null, ""); }
				 */

			} catch (Exception e1) {

				// / TODO
				// / it might be good to just go through the usual saveAs route
				// here, but
				// / it's challenging since the frame closes and a new is opened
				// without
				// / returning a ref. Also the KAR system reopens everything
				// once done
				// / (which would result in two of the same workflow being
				// open).
				// /kgf = (KeplerGraphFrame) this._parent;
				// /kgf._saveAs(".xml");

				// TODO very similar to code in RenameComponentEntityAction,
				// find a way to merge?
				String message = "Please enter a name";
				if (!_karType.equals("")) {
					message += " for this " + _karType;
				}
				message += ": ";

				String warnMessage = "ERROR name cannot contain the < sign";

				String newName = JOptionPane.showInputDialog(message,
						entity.getName());
				if (newName == null) {
					// user hit the cancel button
					return false;
				}

				int lessThan = newName.indexOf("<");
				if (lessThan >= 0) {
					JOptionPane.showMessageDialog(_parent, warnMessage,
							"Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}

				try {

					RenameUtil.renameComponentEntity((ComponentEntity) entity,
							newName);
					_parent.setTitle(entity.getName());
					setDefaultFileName(newName + ".kar");

				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane
							.showMessageDialog(_parent, "A problem occured.");
					return false;
				}

				try {
					_savekar.checkNamedObjName(entity);
				} catch (Exception e2) {
					if (e2.getMessage().equals("Unnamed")) {
						// continue using the unnamed name
					} else {
						JOptionPane.showMessageDialog(_parent,
								"KAR contents must have a name.");
						// stop the export
						return false;
					}
				}
			}
		}

		// Check to see if it has at least one SemanticType
		// Ask the user if they want to add one if it doesn't
		if (checkIfSemenaticallyAnnotated
				&& SMSServices.getActorSemanticTypes(entity).size() == 0) {

			String message = "In order for KAR item: " + entity.getName()
					+ " to show up in an Ontology"
					+ " it must contain at least one Semantic Type.\n"
					+ " Would you like to add a Semantic Type? ";
			String title = "Add Semantic Types?";
			int choice = JOptionPane.showConfirmDialog(_parent, message, title,
					JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION) {

				SemanticTypeEditor editor = new SemanticTypeEditor(_parent,
						entity);
				editor.setModal(true);
				editor.setVisible(true);
			}

		}

		return true;

	}
}
