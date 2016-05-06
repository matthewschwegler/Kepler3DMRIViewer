/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2011-03-31 12:49:31 -0700 (Thu, 31 Mar 2011) $' 
 * '$Revision: 27405 $'
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

import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.authentication.AuthenticationException;
import org.kepler.authentication.AuthenticationManager;
import org.kepler.authentication.ProxyEntity;
import org.kepler.kar.KARFile;
import org.kepler.kar.UploadToRepository;
import org.kepler.kar.karxml.KarXmlGenerator;
import org.kepler.objectmanager.repository.Repository;
import org.kepler.objectmanager.repository.RepositoryManager;

import ptolemy.actor.gui.TableauFrame;

public class ComponentUploader {
	private static final Log log = LogFactory.getLog(ComponentUploader.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	private TableauFrame _frame;

	private UploadToRepository _uploader;

	private RepositoryManager _manager;

	public ComponentUploader(TableauFrame parent) {
		_frame = parent;

		try {
			_manager = RepositoryManager.getInstance();
		} catch (Exception e) {
			System.out.println("Could not get repository manager: "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Upload a kar file and generated kaxml to the repository.
	 * @param karFile
	 * @return boolean true if the upload succeeded
	 */
	public boolean upload(KARFile karFile) {
	  boolean success = false;
	  boolean fileUploadSuccess = uploadDataFile(karFile);
		if (fileUploadSuccess) {
			boolean metadataUploadSuccess = false;
			try {
				KarXmlGenerator kxg = new KarXmlGenerator(karFile);
				String metadataObj = kxg.getKarXml();
				metadataUploadSuccess = _uploader.uploadMetadata(metadataObj);
				if (!metadataUploadSuccess) {
					JOptionPane.showMessageDialog(_frame,
							"There was a problem uploading the metadata",
							"Error", JOptionPane.ERROR_MESSAGE);
				}
				else {
	        JOptionPane.showMessageDialog(_frame,
	            "Component successfully uploaded.", "Success",
	            JOptionPane.INFORMATION_MESSAGE);
	        success = true;
	      }
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(_frame,
						"There was a problem uploading the metadata: "
								+ e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}

			
		}
		return success;
	}
	
	/**
	 * Upload a kar file and associated metadata to a repository
	 * @param karFile the Kar file will be uploaded
	 * @param metadataObj the associated the metadata will be uploaded
	 * @return true if the uploading was successful
	 */
	public boolean upload(KARFile karFile, String metadataObj)
	{
	  boolean success = false;
	  success = uploadDataFile(karFile);
	  if(success)
	  {
	    success = uploadMetadataObj(metadataObj);
	  }
	  if(success)
	  {
	    JOptionPane.showMessageDialog(_frame,
          "Component successfully uploaded.", "Success",
          JOptionPane.INFORMATION_MESSAGE);
	  }
	  return success;
	}
	
	/*
	 * Upload the kar file to remote repository
	 */
	private boolean uploadDataFile(KARFile karFile) {
	  if (isDebugging)
      log.debug("upload(" + karFile.toString());

    Repository saveRepository = _manager.getSaveRepository();
    if (saveRepository == null) {
      JOptionPane.showMessageDialog(_frame,
          "To upload to a remote repository, select one from the "
              + "component search preferences", "Cannot Save",
          JOptionPane.ERROR_MESSAGE);
      return false;
    }
    _uploader = new UploadToRepository(karFile.getFileLocation());
    _uploader.setRepository(saveRepository);

    if (!authenticateUser()) {
      return false;
    }

    queryIfPublic();
    boolean fileUploadSuccess = false;
    try {
      fileUploadSuccess = _uploader.uploadFile();
      if (!fileUploadSuccess) {
        JOptionPane.showMessageDialog(_frame,
            "There was a problem uploading the data", "Error",
            JOptionPane.ERROR_MESSAGE);
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane
          .showMessageDialog(_frame,
              "There was a problem uploading the data: "
                  + e.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
      return false;
    }
    return true;
	}
	
	/*
	 * Upload a metacat associated with kar file to the repository.
	 * This method only can be called after calling uploadDataFile
	 */
	private boolean uploadMetadataObj(String metadataObj)
	{
	  return _uploader.uploadMetadata(metadataObj);
	}
	

	/**
	 * open a login gui to authenticate the user
	 * 
	 * @return boolean true if the user was authenticated
	 */
	private boolean authenticateUser() {
		if (isDebugging)
			log.debug("authenticateUser()");

		try {
			AuthenticationManager authMan = AuthenticationManager.getManager();

			// get the auth domain to authenticate against
			ProxyEntity pentity = authMan.getProxy(_uploader.getRepository()
					.getAuthDomain());
			_uploader.setSessionId(pentity.getCredential());
			if (_uploader.getSessionId() == null) {
				return false;
			}

		} catch (AuthenticationException ae) {
			if (ae.getMessage() != null && ae.getMessage().indexOf("<unauth_login>") != -1) {
				// bad password... try again
				JOptionPane.showMessageDialog(_frame,
						"Incorrect password or username.",
						"Failed Authentication",
						JOptionPane.INFORMATION_MESSAGE);

				return false;
			} else if (ae.getType() == AuthenticationException.USER_CANCEL) {
				return false;
			} else {
				ae.printStackTrace();
				JOptionPane.showMessageDialog(_frame, "Error authenticating: "
						+ ae.getMessage(), "Failed Authentication",
						JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		}
		return true;
	}

	private void queryIfPublic() {
		if (isDebugging)
			log.debug("queryIfPublic()");
		int ispublic = JOptionPane.showConfirmDialog(_frame,
				"Would you like your component "
						+ "to be publicly accessible in the library?",
				"Access Rights", JOptionPane.YES_NO_OPTION);
		if (JOptionPane.YES_OPTION == ispublic) {
			_uploader.setPublicFile(true);
		}
	}
}
