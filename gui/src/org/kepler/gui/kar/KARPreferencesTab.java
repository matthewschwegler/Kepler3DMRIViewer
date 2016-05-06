/*
 *  Copyright (c) 2003-2010 The Regents of the University of California.
 *  All rights reserved.
 *  Permission is hereby granted, without written agreement and without
 *  license or royalty fees, to use, copy, modify, and distribute this
 *  software and its documentation for any purpose, provided that the above
 *  copyright notice and the following two paragraphs appear in all copies
 *  of this software.
 *  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 *  FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 *  THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 *  PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 *  PT_COPYRIGHT_VERSION_2
 *  COPYRIGHTENDKEY
 */
package org.kepler.gui.kar;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationManagerException;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.gui.PreferencesTab;
import org.kepler.gui.PreferencesTabFactory;
import org.kepler.gui.TabManager;
import org.kepler.kar.KARCacheManager;
import org.kepler.kar.KARFile;
import org.kepler.objectmanager.library.LibIndex;
import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;



public class KARPreferencesTab extends JPanel implements PreferencesTab, ActionListener {

	private JPanel _KARPrefPanel = new JPanel();
	private String _tabName = "KAR Preferences";
	
	ConfigurationManager cman = ConfigurationManager.getInstance();
	ConfigurationProperty coreProperty = cman.getProperty(KARFile.KARFILE_CONFIG_PROP_MODULE);
	ConfigurationProperty KARComplianceProp = coreProperty.getProperty(KARFile.KAR_COMPLIANCE_PROPERTY_NAME);
	private String lastUserComplianceSetting = null;
	

	public String getTabName() {
		return _tabName;
	}

	public void setTabName(String tabName) {
		_tabName = tabName;
	}

	public void initializeTab() throws Exception {
		
		_KARPrefPanel.setLayout(new BorderLayout());
		
		//user has a core configuration.xml from before KARComplianceProp existed, need to add it.
		// if KARPreferencesTab.areAllModuleDependenciesSatisfied hasn't already done so.
		if (KARComplianceProp == null){
			KARComplianceProp = new ConfigurationProperty(KARFile.KARFILE_CONFIG_PROP_MODULE, KARFile.KAR_COMPLIANCE_PROPERTY_NAME, KARFile.KAR_COMPLIANCE_DEFAULT);
			coreProperty.addProperty(KARComplianceProp);
		}
		lastUserComplianceSetting = KARComplianceProp.getValue();
		
		setupButtonPanel();
		setupDescription();
		
		this.add(_KARPrefPanel, BorderLayout.WEST);
	}

	private void setupButtonPanel(){
				
	    JRadioButton strictButton = new JRadioButton(KARFile.KAR_COMPLIANCE_STRICT);
	    strictButton.setMnemonic(KeyEvent.VK_S);
	    strictButton.setActionCommand(KARFile.KAR_COMPLIANCE_STRICT);
	    strictButton.addActionListener(this);
	    JRadioButton relaxedButton = new JRadioButton(KARFile.KAR_COMPLIANCE_RELAXED);
	    relaxedButton.setMnemonic(KeyEvent.VK_R);
	    relaxedButton.setActionCommand(KARFile.KAR_COMPLIANCE_RELAXED);
	    relaxedButton.addActionListener(this);
	    String KARCompliance = KARComplianceProp.getValue();
	    
		if (KARCompliance.equals(KARFile.KAR_COMPLIANCE_STRICT)){
			strictButton.setSelected(true);
		}
		else{
			relaxedButton.setSelected(true);
		}
	    
	    ButtonGroup group = new ButtonGroup();
	    group.add(strictButton);
	    group.add(relaxedButton);
	    
        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(strictButton);
        radioPanel.add(relaxedButton);
        
        _KARPrefPanel.add(radioPanel, BorderLayout.NORTH);		
	}
	
	private void setupDescription() {
		
		String desciption = "Select your KAR opening compliance mode.\n\n" +
				"* Strict * In order to open a KAR in Strict mode, you must be running Kepler with the " +
				"exact same modules*, in the same order, that the KAR was created with. You will be prompted " +
				"if you need to change or install modules. This may mean restarting with an older version of " +
				"a module you're currently using. Strict mode enables " +
				"maximum compatibility.\n\n" +
				"* Relaxed * In order to open a KAR in Relaxed mode, you must simply be running version(s) newer than or equal to" +
				", in some order, of the modules used to create it*. However maximum compatibility is not guaranteed."+
				"\n\n*A caveat: OS specific modules are currently ignored in dependency checks." + 
				"\n\nChanging this setting rebuilds your library, please be patient...";

		JTextArea descriptionTextArea = new JTextArea(desciption);
		descriptionTextArea.setEditable(false);
		descriptionTextArea.setLineWrap(true);
		descriptionTextArea.setWrapStyleWord(true);
		descriptionTextArea.setPreferredSize(new Dimension(400, 400));
		descriptionTextArea.setBackground(TabManager.BGCOLOR);
		JPanel descriptionPanel = new JPanel(new BorderLayout());
		descriptionPanel.setBackground(TabManager.BGCOLOR);
		descriptionPanel.add(descriptionTextArea, BorderLayout.CENTER);

		_KARPrefPanel.add(descriptionPanel, BorderLayout.SOUTH);
	}
	
	/**
	 * If compliance level changes, rebuild library.
	 * Very similar to ComponentLibraryPreferencesTab.rebuildLibrary(), but no need
	 * to setCheckpoint.
	 */
	public void rebuildLibrary(){
		
		KARCacheManager kcm = KARCacheManager.getInstance();
		kcm.clearKARCache();
		
		LibraryManager lm = LibraryManager.getInstance();
		LibIndex index = lm.getIndex();
		index.clear();
		lm.buildLibrary();
		try {
			lm.refreshJTrees();
		} catch (IllegalActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void onCancel() {
		//do nothing
	}

	/**
	 * If any KARCompliance change by the user is made and it's different 
	 * from serialized KARCompliance, serialize it and rebuild library so that
	 * new KARCompliance setting will take effect (so KARs may display warning icons,
	 * KARS_CACHED, and KARS_ERRORS tables are rebuilt, etc)
	 */
	public void onClose() {
		
		try {
			
			String KARCompliance = KARComplianceProp.getValue();
			
			if (!lastUserComplianceSetting.equals(KARCompliance)){
				//serialize their preference
				KARComplianceProp.setValue(lastUserComplianceSetting);
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				rebuildLibrary();
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		} catch (ConfigurationManagerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	public void setParent(TableauFrame frame) {		
	}

	
	/**
	 * A factory that creates the ServicesListModification panel for the
	 * PreferencesFrame.
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


		/*
		 * (non-Javadoc)
		 * @see org.kepler.gui.PreferencesTabFactory#createPreferencesTab()
		 */
		public KARPreferencesTab createPreferencesTab() {
		
			KARPreferencesTab kpt = new KARPreferencesTab();
			kpt.setTabName(this.getName());
			return kpt;
		}
	}

	
	public void actionPerformed(ActionEvent e) {
		//use this onClose()
		lastUserComplianceSetting = e.getActionCommand();
	}
	
	
}