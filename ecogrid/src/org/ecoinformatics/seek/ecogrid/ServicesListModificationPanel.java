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

package org.ecoinformatics.seek.ecogrid;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.kepler.gui.PreferencesTab;
import org.kepler.gui.PreferencesTabFactory;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

public class ServicesListModificationPanel extends JPanel implements PreferencesTab {

	private TableauFrame _frame;
	private String _tabName;
	
	public ServicesListModificationPanel() {}
	
	public void initializeTab() throws Exception {
		JButton temp = new JButton("Under Construction");
		this.add(temp);
	}

	public void setParent(TableauFrame frame) {
		this._frame = frame;
	}

	/* (non-Javadoc)
	 * @see org.kepler.gui.PreferencesTab#getTabName()
	 */
	public String getTabName() {
		return _tabName;
	}
	
	public void setTabName(String name) {
		_tabName = name;
	}

	/**
	 * A factory that creates the ServicesListModification panel for the PreferencesFrame.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends PreferencesTabFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a PreferencesTab that displays the selected Ecogrid Services.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public PreferencesTab createPreferencesTab() {
			ServicesListModificationPanel slmp = new ServicesListModificationPanel();
			slmp.setTabName(this.getName());
			return slmp;
		}
	}

	public void onClose() {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see org.kepler.gui.PreferencesTab#onCancel()
	 */
	public void onCancel() {
		// Do nothing
	}
}
