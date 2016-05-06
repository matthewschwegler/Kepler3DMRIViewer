/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-12-24 02:08:56 -0800 (Sat, 24 Dec 2011) $' 
 * '$Revision: 29100 $'
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;

/**
 * This frame allows a common place for modules to set preferences. By adding an
 * entry to the preferencesTabFactory in gui/resources/configurations/configuration.xml 
 * modules can include their own preferences.
 * 
 * @author Aaron Schultz
 * 
 */
public class PreferencesFrame extends JFrame implements ActionListener {

	private TableauFrame _frame;

	private JTabbedPane _preferenceTabs;
	private JPanel _controls;

	private JButton _okButton;
	private JButton _cancelButton;

	private int _width = 700;
	private int _height = 600;
	
	/**
	 * Constructor accepts a title for the frame and the parent of the frame.
	 * 
	 * @param title
	 *            the title to appear at the top of the preferences frame
	 * @param frame
	 *            the parent TableauFrame
	 */
	public PreferencesFrame(String title, TableauFrame frame) {
		this(title, frame, null);
	}

	/**
	 * Constructor accepts a title for the frame, the parent of the frame, and
	 * the tab to display after opening the preferencesFrame
	 * 
	 * @param title
	 *            the title to appear at the top of the preferences frame
	 * @param frame
	 *            the parent TableauFrame
	 * @param openTabName
	 *            the name of the tab to show by default
	 */
	public PreferencesFrame(String title, TableauFrame frame, String openTabName) {
		super(title);
		_frame = frame;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(new Dimension(_width, _height));

		JPanel layoutPanel = new JPanel();
		layoutPanel.setLayout(new BorderLayout());

		_preferenceTabs = new JTabbedPane();
		initTabs();
		layoutPanel.add(_preferenceTabs, BorderLayout.CENTER);

		_controls = new JPanel();
		initControls();
		layoutPanel.add(_controls, BorderLayout.SOUTH);

		getContentPane().add(layoutPanel);

		if (openTabName != null && openTabName.length() > 0) {
			setSelectedTab(openTabName);
		}
	}

	/**
	 * Initialize the preference tab extensions from configuration.
	 */
	protected void initTabs() {
    try
    {
      PreferencesTabFactory PTfactory = (PreferencesTabFactory)_frame.getConfiguration().getAttribute("PreferencesTabFactor");
      if(PTfactory == null)
      {
        PTfactory = new PreferencesTabFactory(_frame.getConfiguration(), "PreferencesTabFactor");
      }
      if (PTfactory != null) {
        boolean success = PTfactory.createPreferencesTabs(_preferenceTabs,
            _frame);
        if (!success) {
          System.out
              .println("error: preferenceTab is null.  "
                  + "This "
                  + "problem can be fixed by adding a librarySearchGUIPane "
                  + "property in the configuration.xml file.");
        }
      } else {
        System.out.println("error: PreferencesTabFactory is "
            + "null.  This "
            + "problem can be fixed by adding a LibraryPaneTabFactory "
            + "property in the configuration.xml file.");
      }
    }
    catch(Exception e)
    {
      System.out.println("Could not create tab preferences factory: " + e.getMessage());
      e.printStackTrace();
    }

	}

	/**
	 * Initialize the control buttons.
	 */
	protected void initControls() {

		_okButton = new JButton(
				StaticResources.getDisplayString("general.OK", "Ok"));
		_okButton.addActionListener(this);
		_controls.add(_okButton);

		_cancelButton = new JButton(
				StaticResources.getDisplayString("general.CANCEL", "Cancel"));
		_cancelButton.addActionListener(this);
		_controls.add(_cancelButton);
	}

	/**
	 * Set the selected tab using the index of the tab.
	 * 
	 * @param tabIndex
	 */
	public void setSelectedTab(int tabIndex) {
		_preferenceTabs.setSelectedIndex(tabIndex);
	}

	/**
	 * Set the selected tab using the name of the tab.
	 * 
	 * @param tabName
	 */
	public void setSelectedTab(String tabName) {
		boolean tabFound = false;
		for (int i = 0; i < _preferenceTabs.getTabCount(); i++) {
			Component c = _preferenceTabs.getComponentAt(i);
			if (c instanceof PreferencesTab) {
				if (((PreferencesTab) c).getTabName().equals(tabName)) {
					_preferenceTabs.setSelectedIndex(i);
					tabFound = true;
					break;
				}
			}
		}
		if (!tabFound) {
			_preferenceTabs.setSelectedIndex(0);
		}
	}

	/**
	 * As the PreferenceFrame is disposed the onClose method of each preference
	 * tab is called.
	 */
	public void dispose() {
		for (int i = 0; i < _preferenceTabs.getTabCount(); i++) {
			Component c = _preferenceTabs.getComponentAt(i);
			if (c instanceof PreferencesTab) {
				PreferencesTab pt = (PreferencesTab) c;
				pt.onClose();
				pt.setParent(null);
			}
		}
		PreferencesFrameTracker pft = PreferencesFrameTracker.getInstance();
		pft.setClosed();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			if (e.getSource() == _okButton) {

				dispose();

			} else if (e.getSource() == _cancelButton) {

				for (int i = 0; i < _preferenceTabs.getTabCount(); i++) {
					Component c = _preferenceTabs.getComponentAt(i);
					if (c instanceof PreferencesTab) {
						((PreferencesTab) c).onCancel();
					}
				}
				PreferencesFrameTracker pft = PreferencesFrameTracker.getInstance();
				pft.setClosed();
				super.dispose();

			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

	}

}