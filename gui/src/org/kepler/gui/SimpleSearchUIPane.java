/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-03-18 19:24:12 -0700 (Fri, 18 Mar 2011) $' 
 * '$Revision: 27324 $'
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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;

import org.kepler.gui.component.SearchConfigurationFrame;
import org.kepler.util.StaticResources;

/**
 * Class to build the search gui pane that allows the user to enter a single
 * search term to search by.
 * 
 *@author berkley
 *@since February 17, 2005
 */
public class SimpleSearchUIPane extends LibrarySearchPane {

	private SearchUIJPanel _searchUIJPanel;

	/**
	 * constructor
	 * 
	 *@param searchButtonHandler
	 *            Description of the Parameter
	 */
	public SimpleSearchUIPane(ActionListener searchButtonHandler) {
		super(searchButtonHandler);
		init();
	}

	/**
	 * Clear the search term.
	 */
	public void clearSearch() {
		_searchUIJPanel.setSearchTerm("");
	}

	/**
	 *@return the search term
	 */
	public String getSearchTerm() {
		return _searchUIJPanel.getSearchTerm();
	}

	/*
	 * Initialize the search panel
	 */
	private void init() {

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		_searchUIJPanel = new SearchUIJPanel();
		_searchUIJPanel.setBorderTitle(
				StaticResources.getDisplayString("components.search", "Search Components"));
		_searchUIJPanel.setSearchAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				searchButtonHandler.actionPerformed(e);
				_searchUIJPanel.setCancelButtonEnabled(true);
			}
		});
		/*
		 * _searchUIJPanel.setResetAction(new AbstractAction() { public void
		 * actionPerformed(ActionEvent e) { _searchUIJPanel.setSearchTerm("");
		 * searchButtonHandler.actionPerformed(e);
		 * _searchUIJPanel.setCancelButtonEnabled(false); } });
		 */
		_searchUIJPanel.setCancelAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				_searchUIJPanel.setSearchTerm("");
				searchButtonHandler.actionPerformed(e);
				_searchUIJPanel.setCancelButtonEnabled(false);
			}
		});
		// TODO: look up the initial tab from configuration (i18n)
		_searchUIJPanel.setSourceAction(new PreferencesAction("Components"));

		_searchUIJPanel.setAdvancedAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Object o = e.getSource();
				if (o instanceof Component) {
					Window w = GUIUtil.getParentWindow((Component) o);
					SearchConfigurationFrame scf = new SearchConfigurationFrame(
							StaticResources.getDisplayString(
									"components.search.configuration", 
									"Component Search Configuration"));
					scf.setLocation(w.getLocation());
					scf.setVisible(true);
				}
			}
		});

		_searchUIJPanel.init();

		this.add(_searchUIJPanel);
	}

	/**
	 * get the preferred/minimum width of this panel - calculated to allow
	 * enough space for all buttons and spacers etc
	 * 
	 * @return the minimum allowable width of this panel
	 */
	public final int getMinimumWidth() {
		return _searchUIJPanel.getMinimumWidth();
	}
	
	public void closing() {
		//System.out.println("SimpleSearchUIPane.closing()");
		_searchUIJPanel.closing();
	}
}