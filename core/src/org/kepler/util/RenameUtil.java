/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-01-04 23:59:36 -0800 (Tue, 04 Jan 2011) $' 
 * '$Revision: 26633 $'
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

package org.kepler.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.lsid.LSIDGenerator;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Effigy;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.StringUtilities;

/**
 * A utility class for renaming things.
 * 
 * @author Aaron Schultz
 */
public class RenameUtil {

    /** Regular expression pattern of Unnamed effigy ids. */
    public static Pattern unnamedIdPattern = Pattern.compile("^Unnamed\\d+$");
	
	/**
	 * A method for renaming ComponentEntities. This algorithm is loosely based
	 * on ptolemy.actor.gui.RenameConfigurer.apply() method.
	 * 
	 * @param no
	 * @param newName
	 */
	public static void renameComponentEntity(ComponentEntity ce, String newName)
			throws Exception {

		KeplerLSID origLSID = NamedObjId.getIdFor(ce);
		String oldName = ce.getName();
		String displayName = StringUtilities.escapeForXML(newName);
		NamedObj parent = ce.getContainer();
		
		if (parent == null) {
			if (!ce.getName().equals(newName)) {
				
	            Matcher matcher = unnamedIdPattern.matcher(ce.getName());
				
				// Since MoMLChangeRequest requires a parent container
				// we'll just set it directly if it doesn't have one
				ce.setName(newName);
				ce.setDisplayName(displayName);

				// assign new LSID to workflow except if it's unnamed.
	            if (!matcher.matches()) {
	            	KeplerLSID newLSID = LSIDGenerator.getInstance().getNewLSID();
	            	NamedObjId.assignIdTo(ce, newLSID, true);
	            	//now we must explicitly requestNamedObjIdChange
	            	NamedObjId.getIdAttributeFor(ce).requestNamedObjIdChange();
	            }
	            else{
	            	// if unnamed, just updateRevision.
	            	NamedObjId.getIdAttributeFor(ce).updateRevision();
	            }
			}
			KeplerLSID currentLSID = NamedObjId.getIdFor(ce);
			notifyWorkflowRenameListeners(ce, oldName, newName, origLSID, currentLSID);
			
			// fixes bug 5101
			Effigy eff = Configuration.findEffigy(ce);
			if (eff != null) {
				StringAttribute sa = eff.identifier;
				if (sa != null) {
					sa.setExpression(newName);
				}
			}
			
			return;
		}
		
		
		String oldDisplayName = StringUtilities.escapeForXML(ce
				.getDisplayName());

		StringBuffer moml = new StringBuffer("<");
		String elementName = ce.getElementName();
		moml.append(elementName);
		moml.append(" name=\"");
		moml.append(oldName);
		moml.append("\">");
		if (!oldName.equals(newName)) {
			moml.append("<rename name=\"");
			moml.append(newName);
			moml.append("\"/>");
		}
		if (!oldDisplayName.equals(displayName)) {
			moml.append("<display name=\"");
			moml.append(displayName);
			moml.append("\"/>");
		}

		moml.append("</");
		moml.append(elementName);
		moml.append(">");

		MoMLChangeRequest request = new MoMLChangeRequest(null, // originator
				parent, // context
				moml.toString(), // MoML code
				null); // base

		RenameListener rl = new RenameListener();

		request.addChangeListener(rl);
		request.setUndoable(true);
		parent.requestChange(request);
		
				
		KeplerLSID currentLSID = NamedObjId.getIdFor(ce);
		notifyWorkflowRenameListeners(ce, oldName, newName, origLSID, currentLSID);
		
		// fixes bug 5101
		Effigy eff = Configuration.findEffigy(ce);
		if (eff != null) {
			StringAttribute sa = eff.identifier;
			if (sa != null) {
				sa.setExpression(newName);
			}
		}
		
	}

	
	public static void notifyWorkflowRenameListeners(ComponentEntity ce, String oldName, 
			String newName, KeplerLSID oldLSID, KeplerLSID currentLSID){
		
		if (!oldLSID.equals(currentLSID) && oldName != null && !oldName.equals(newName)){		
			// notify any rename listeners
			WorkflowRenameManager.getInstance().renamedWorkflow(ce, 
					oldLSID, currentLSID, oldName, newName);
		}
	}
	
	
}
