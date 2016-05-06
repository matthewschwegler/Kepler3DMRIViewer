/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.RenameUtil;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * 
 * @author Aaron Schultz
 * 
 */
public class RenameComponentEntityAction extends FigureAction {

	private static final Log log = LogFactory
			.getLog(RenameComponentEntityAction.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private final static String LABEL = "Rename";

	private TableauFrame _parent;
	private NamedObj _obj;

	public RenameComponentEntityAction(TableauFrame parent) {
		super(LABEL);

		_parent = parent;

		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"RenameComponentEntityAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}

		this.putValue("tooltip", "Open the Rename Component Entity Dialog.");
	}

	public void setObject(NamedObj no) {
		_obj = no;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		if (isDebugging)
			log.debug("RenameComponentEntityAction.actionPerformed()");

		NamedObj object = null;
		if (_obj == null) {
			object = getTarget();
		} else {
			object = _obj;
		}

		if (isDebugging)
			log.debug(object.getClass().getName());

		if (object instanceof ComponentEntity) {

			// TODO very similar to code in RenameComponentEntityAction,
			// find a way to merge?
				
			Component parentComponent = (Component) _parent;
			String message = "Enter a new name: ";
			String warnMessage = "ERROR name cannot contain the < sign";
			String title = "Rename";
			int messageType = JOptionPane.QUESTION_MESSAGE;
			String initialValue = object.getName();

			String newName = (String) JOptionPane.showInputDialog(
					parentComponent, message, title, messageType, null, null,
					initialValue);
			if (newName == null) {
				// user hit the cancel button
				return;
			}
			
	        int lessThan = newName.indexOf("<");
	        if (lessThan >= 0){
	        	JOptionPane.showMessageDialog(parentComponent, warnMessage, "Error",
	        	        JOptionPane.ERROR_MESSAGE);
	        	return;
	        }
			
			try {

				RenameUtil.renameComponentEntity((ComponentEntity) object,
						newName);
				_parent.setTitle(object.getName());

			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(_parent, "A problem occured.");
			}
		}

	}

}
