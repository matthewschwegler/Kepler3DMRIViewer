/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-11-21 17:13:06 -0800 (Mon, 21 Nov 2011) $' 
 * '$Revision: 28984 $'
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

package org.kepler.modulemanager;

import java.io.File;
import java.net.URL;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.layout.GroupLayout;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.configuration.ConfigurationManager;

import ptolemy.actor.gui.BrowserLauncher;


/**
 * Created by Debi Staggs
 * to facilitate the viewing of the
 * module documentation
 * 
 * This will list only those active modules for which documentation has been installed.
 * IMPORTANT:  In order for this to work correctly, the documentation must be named
 * the same as the module, but without the version number.  Also it must be in .pdf format.
 * 
 * Date: 06/10/2010
 * Time: 2:36:12 PM
 */
public class ActiveModulesDocumentationPanel extends JPanel {
	
	private JLabel activeModulesListLabel = new JLabel("Module Documentation available:");
	private JList activeModulesList = new JList();
	JScrollPane activeModulesListScrollPane = new JScrollPane(activeModulesList);
	private static final String WIKI_DOCLIST_URL = "https://kepler-project.org/users/documentation";
	private String extension = ".pdf";
	private String modulename;
	private File tmpfl;
	private String docFilePath;
	private String basePath;
    
   //JButton btnViewDocs = new JButton("View Documentation");
   ConfigurationManager cman = ConfigurationManager.getInstance();

	public ActiveModulesDocumentationPanel() {
		super();
		initActiveModulesList();
		initComponents();
		layoutComponents();
	}

	/**
	*  This initializes the ActiveModulesList from the ModuleTree instance, iterates through the
	*  tree, getting each module name + stripping out the version number, it then uses that to
	*  get the local path to the documentation file, and checks to see if it exists.
	*  If it is there, it adds the filename to the list and 
	*/
	private void initActiveModulesList()
    {
		
		DefaultListModel listModel = new DefaultListModel();
        ModuleTree.init();
        
        // rollcursor = new Cursor(Cursor.HAND_CURSOR);

        for(Module module : ModuleTree.instance()) {
        	modulename = module.getStemName().toString();
    		docFilePath = getDocFilePath(modulename);
    			
        	try {
        		File tmpfl = new File(docFilePath);
        		if (tmpfl.exists() && tmpfl.canRead()){
        			listModel.addElement(modulename);
        		} else {
        			// the file does not exist
        		}
        	} catch (Exception e) {
        		// exception
        	}
        }
        
        activeModulesList.setModel(listModel);
    }
	

	private void initComponents() {

		activeModulesList.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {

				if (activeModulesList.getSelectedIndex() == -1) {
					// No selection, disable fire button.
					// fireButton.setEnabled(false);

				} else {
					getDocFile();
				}

				activeModulesList.clearSelection();

			}
		});
	}
	
	private String getDocFilePath(String modulename) {
		
		// find the right directory & build the path
		tmpfl= ConfigurationManager.getModule(modulename).getDir();
		basePath = tmpfl.getAbsolutePath();
		docFilePath = (basePath + "/docs/" + modulename + extension); 
		return docFilePath;
		
	}

	private void getDocFile() {
		
		Object selectedmodule = activeModulesList.getSelectedValue();
			
		try {
			
			docFilePath = getDocFilePath(selectedmodule.toString());
			System.out.println("docFilePath:"+docFilePath);
			File file = new File(docFilePath);
			URL url = file.toURI().toURL();
			BrowserLauncher.openURL(url.toString());
			
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.getParent(), "The Documentation for the module you have selelected \n" 
					+ "is not available, please try locating it at: \n" + WIKI_DOCLIST_URL, null, JOptionPane.WARNING_MESSAGE);
		 }
		
	}

	private void layoutComponents() {
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutocreateContainerGaps(true);
		layout.setAutocreateGaps(true);

		layout.setHorizontalGroup(layout.createParallelGroup(
				GroupLayout.TRAILING).add(activeModulesListLabel).add(
				activeModulesListScrollPane).add(
				layout.createSequentialGroup())//.add(btnViewDocs))

		);

		layout.setVerticalGroup(layout.createSequentialGroup().add(
				activeModulesListLabel).add(activeModulesListScrollPane).add(
				layout.createParallelGroup())//.add(btnViewDocs))

		);
	}

	
}
