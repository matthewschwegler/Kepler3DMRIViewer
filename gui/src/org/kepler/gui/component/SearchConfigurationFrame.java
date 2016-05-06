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

/**
 * 
 */
package org.kepler.gui.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.kepler.objectmanager.library.LibSearch;
import org.kepler.objectmanager.library.LibSearchConfiguration;
import org.kepler.util.StaticResources;

/** 
 * @author Aaron Schultz
 *
 */
public class SearchConfigurationFrame extends JFrame implements ActionListener {
	
	private JPanel _cbPane;
	private JCheckBox cbName;
	private JCheckBox cbClassName;
	private JCheckBox cbOntName;
	private JCheckBox cbOntClassName;
	private JCheckBox cbFolderName;
	private JCheckBox cbKarName;
	private JCheckBox cbLocalRepo;

	private JPanel _controls;
	private JButton _okButton;
	private JButton _cancelButton;
	
	private LibSearchConfiguration _LibSearchConfig;

	/**
	 * Constructor accepts a title for the frame.
	 * 
	 * @param title
	 *            the title to appear at the top of the frame
	 */
	public SearchConfigurationFrame(String title) {
		super(title);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(new Dimension(350, 350));
		
		_LibSearchConfig = new LibSearchConfiguration();

		JPanel layoutPanel = new JPanel();
		layoutPanel.setLayout(new BorderLayout());
		
		_cbPane = new JPanel(new GridLayout(8,1));
		initCheckBoxes();
		layoutPanel.add(_cbPane, BorderLayout.CENTER);

		_controls = new JPanel();
		initControls();
		layoutPanel.add(_controls, BorderLayout.SOUTH);
		
		getContentPane().add(layoutPanel);
	}
	
	protected void initCheckBoxes() {
		
		// Name check box
		cbName = new JCheckBox(
				StaticResources.getDisplayString(
						"components.search.configuration.options.componentName", 
						"Component Name"));
		if (_LibSearchConfig.contains(LibSearch.TYPE_NAME)){
			cbName.setSelected(true);
		}
		_cbPane.add(cbName);
		
		// Ontology Class Name Checkbox
		cbOntClassName = new JCheckBox(
				StaticResources.getDisplayString(
						"components.search.configuration.options.ontologyClassName",
						"Ontology Class Name"));
		if (_LibSearchConfig.contains(LibSearch.TYPE_ONTCLASSNAME)){
			cbOntClassName.setSelected(true);
		}
		_cbPane.add(cbOntClassName);
		
		// Ontology Name Checkbox
		cbOntName = new JCheckBox(
				StaticResources.getDisplayString(
						"components.search.configuration.options.ontologyName",
						"Ontology Name"));
		if (_LibSearchConfig.contains(LibSearch.TYPE_ONTOLOGY)){
			cbOntName.setSelected(true);
		}
		_cbPane.add(cbOntName);
		
		/* TODO add folder names to LibSearch
		// Folder Name check box
		cbFolderName = new JCheckBox("Folder Name");
		if (_LibSearchConfig.contains(LibSearch.TYPE_FOLDERNAME)){
			cbFolderName.setSelected(true);
		}
		_cbPane.add(cbFolderName);
		*/
		
		// Kar Name check box
		cbKarName = new JCheckBox(
				StaticResources.getDisplayString(
						"components.search.configuration.options.karName",
						"KAR Name"));
		if (_LibSearchConfig.contains(LibSearch.TYPE_KARNAME)){
			cbKarName.setSelected(true);
		}
		_cbPane.add(cbKarName);
		
		// Local Repo check box
		cbLocalRepo = new JCheckBox(
				StaticResources.getDisplayString(
						"components.search.configuration.options.localRepositoryName",
						"Local Repository Name"));
		if (_LibSearchConfig.contains(LibSearch.TYPE_LOCALREPO)){
			cbLocalRepo.setSelected(true);
		}
		_cbPane.add(cbLocalRepo);
		
		// TODO: get this working again
		/* Class Name Checkbox
		cbClassName = new JCheckBox("Component Java Class Name");
		if (_LibSearchConfig.contains(LibSearch.TYPE_CLASSNAME)){
			cbClassName.setSelected(true);
		}
		_cbPane.add(cbClassName);
		*/
		
	}

	/**
	 * Initialize the control buttons.
	 */
	protected void initControls() {

		_okButton = new JButton(
				StaticResources.getDisplayString(
						"general.OK",
						"Ok"));
		_okButton.addActionListener(this);
		_controls.add(_okButton);

		_cancelButton = new JButton(
				StaticResources.getDisplayString(
						"general.CANCEL",
						"Cancel"));
		_cancelButton.addActionListener(this);
		_controls.add(_cancelButton);
	}

	public void dispose() {
		
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == _okButton) {

			if (cbName.isSelected()) {
				if (!_LibSearchConfig.contains(LibSearch.TYPE_NAME)){
					_LibSearchConfig.addSearchType(LibSearch.TYPE_NAME);
				}
			} else {
				_LibSearchConfig.removeSearchType(LibSearch.TYPE_NAME);
			}

			/* TODO get this working again
			if (cbClassName.isSelected()) {
				if (!_LibSearchConfig.contains(LibSearch.TYPE_CLASSNAME)){
					_LibSearchConfig.addSearchType(LibSearch.TYPE_CLASSNAME);
				}
			} else {
				_LibSearchConfig.removeSearchType(LibSearch.TYPE_CLASSNAME);
			}
			*/

			if (cbOntClassName.isSelected()) {
				if (!_LibSearchConfig.contains(LibSearch.TYPE_ONTCLASSNAME)){
					_LibSearchConfig.addSearchType(LibSearch.TYPE_ONTCLASSNAME);
				}
			} else {
				_LibSearchConfig.removeSearchType(LibSearch.TYPE_ONTCLASSNAME);
			}

			if (cbOntName.isSelected()) {
				if (!_LibSearchConfig.contains(LibSearch.TYPE_ONTOLOGY)){
					_LibSearchConfig.addSearchType(LibSearch.TYPE_ONTOLOGY);
				}
			} else {
				_LibSearchConfig.removeSearchType(LibSearch.TYPE_ONTOLOGY);
			}

			/* TODO add folder search to LibSearch
			if (cbFolderName.isSelected()) {
				if (!_LibSearchConfig.contains(LibSearch.TYPE_FOLDERNAME)){
					_LibSearchConfig.addSearchType(LibSearch.TYPE_FOLDERNAME);
				}
			} else {
				_LibSearchConfig.removeSearchType(LibSearch.TYPE_FOLDERNAME);
			}
			*/

			if (cbKarName.isSelected()) {
				if (!_LibSearchConfig.contains(LibSearch.TYPE_KARNAME)){
					_LibSearchConfig.addSearchType(LibSearch.TYPE_KARNAME);
				}
			} else {
				_LibSearchConfig.removeSearchType(LibSearch.TYPE_KARNAME);
			}

			if (cbLocalRepo.isSelected()) {
				if (!_LibSearchConfig.contains(LibSearch.TYPE_LOCALREPO)){
					_LibSearchConfig.addSearchType(LibSearch.TYPE_LOCALREPO);
				}
			} else {
				_LibSearchConfig.removeSearchType(LibSearch.TYPE_LOCALREPO);
			}
			
			_LibSearchConfig.serializeToDisk();
			
			dispose();

		} else if (e.getSource() == _cancelButton) {

			dispose();

		}

	}
}
