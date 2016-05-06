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
package org.kepler.gui.lsid;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.GUIUtil;
import org.kepler.moml.NamedObjId;
import org.kepler.moml.NamedObjIdReferralList;
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.lsid.LSIDGenerator;

import ptolemy.kernel.util.NamedObj;

/**
 * A popup dialog box that shows the current LSID and LSID Referral List for the
 * NamedObj that is passed to the initialize(NamedObj) method.
 * 
 * @author Aaron Schultz
 * 
 */
public class LSIDViewer extends JFrame implements ActionListener {
	private static final long serialVersionUID = 3894368355858308419L;
	private static final Log log = LogFactory
			.getLog(LSIDViewer.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private NamedObj _no;
	private JTextField _lsidField;
	private JTextArea _derivedFromField;
	private JScrollPane _derivedFromScrollPane;
	
	private boolean _editingEnabled = false;

	/**
	 * @throws HeadlessException
	 */
	public LSIDViewer() throws HeadlessException {
		this("LSID Viewer");
	}

	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public LSIDViewer(String title) throws HeadlessException {
		super(title);
	}

	public void initialize(NamedObj no) {
		if (isDebugging) {
			log.debug("initialize( " + no.getName() + " " + no.getClass().getName() + " )");
		}
		_no = no;

		JPanel layoutPanel = new JPanel();
		layoutPanel.setLayout(new BorderLayout());

		JPanel lsidPanel = new JPanel();
		lsidPanel.setLayout(new BorderLayout());
		
		JLabel lsidLabel = new JLabel("LSID:");
		lsidPanel.add(lsidLabel,BorderLayout.WEST);
		
		_lsidField = new JTextField("");
		_lsidField.setEditable(false);
		lsidPanel.add(_lsidField, BorderLayout.CENTER);

		JPanel dfPanel = new JPanel();
		dfPanel.setLayout(new BorderLayout());
		
		_derivedFromField = new JTextArea("");
		_derivedFromField.setEditable(false);
		_derivedFromScrollPane = new JScrollPane(_derivedFromField);
		_derivedFromScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		_derivedFromScrollPane.setBorder(
	            BorderFactory.createCompoundBorder(
	                BorderFactory.createCompoundBorder(
	                                BorderFactory.createTitledBorder("Derived From"),
	                                BorderFactory.createEmptyBorder(5,5,5,5)),
	                                _derivedFromScrollPane.getBorder()));
		dfPanel.add(_derivedFromScrollPane, BorderLayout.CENTER);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		
		String infStr = "Name: "+_no.getName();
		JLabel objInfo = new JLabel(infStr);
		if (isEditingEnabled()) {
			bottomPanel.add(objInfo,BorderLayout.NORTH);
		} else {
			bottomPanel.add(objInfo,BorderLayout.CENTER);
		}
		
		JPanel buttonPanel = new JPanel();
		
		if (isEditingEnabled()) {
			JButton newObjButton = new JButton("New Object ID");
			newObjButton.addActionListener(this);
			buttonPanel.add(newObjButton);
			
			JButton revisionButton = new JButton("Roll Revision");
			revisionButton.addActionListener(this);
			buttonPanel.add(revisionButton);
		}
		
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(this);
		buttonPanel.add(refreshButton);
		
		bottomPanel.add(buttonPanel,BorderLayout.EAST);

		layoutPanel.add(lsidPanel, BorderLayout.NORTH);
		layoutPanel.add(dfPanel, BorderLayout.CENTER);
		layoutPanel.add(bottomPanel, BorderLayout.SOUTH);

		getContentPane().add(layoutPanel);

		refreshValues();
	}
	
	public void setEditingEnabled(boolean editingEnabled) {
		_editingEnabled = editingEnabled;
	}
	
	public boolean isEditingEnabled() {
		return _editingEnabled;
	}

	/**
	 * Update the textfield and textarea to reflect the current LSID and LSID
	 * referral list values.
	 */
	public void refreshValues() {

		KeplerLSID lsid = null;
		String derivedLSIDs = new String();
		ObjectManager om = ObjectManager.getInstance();
		try {
			if (_no != null) {
				NamedObjId noi = NamedObjId.getIdAttributeFor(_no);
				if (noi == null) {
					log.error("NamedObjId is null");
					lsid = null;
				} else {
					lsid = noi.getId();
				}
				NamedObjIdReferralList noirl = NamedObjId
						.getIDListAttributeFor(_no);
				if (noirl == null) {
					derivedLSIDs = "null";
				} else {
					List<KeplerLSID> derivedFrom = noirl.getReferrals();
					if (derivedFrom.size() <= 0) {
						derivedLSIDs = "empty";
					} else {
						for (KeplerLSID derlsid : derivedFrom) {
							derivedLSIDs += derlsid.toString() + "\n";
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (isDebugging) {
			log.debug(lsid.toString());
			log.debug(derivedLSIDs);
		}
		if (lsid == null) {
			_lsidField.setText("null");
		} else {
			_lsidField.setText(lsid.toString());
		}
		_derivedFromField.setText(derivedLSIDs);
	}
	
	private void rollRevision() {
		if (isEditingEnabled()) {
			NamedObjId noi = NamedObjId.getIdAttributeFor(_no);
			try {
				noi.updateRevision();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(GUIUtil
						.getParentWindow(_derivedFromScrollPane), e
						.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void assignNewObjectId() {
		if (isEditingEnabled()) {
			LSIDGenerator lg = LSIDGenerator.getInstance();
			try {
				NamedObjId.assignIdTo(_no,lg.getNewLSID());
			} catch (Exception e) {
				JOptionPane.showMessageDialog(GUIUtil
						.getParentWindow(_derivedFromScrollPane), e
						.getMessage());
				e.printStackTrace();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o instanceof JButton) {
			JButton b = (JButton) o;
			if (b.getText().equals("Refresh")) {
				if (isDebugging) 
					log.debug("Refresh");
				refreshValues();
			} else if (b.getText().equals("Roll Revision")) {
				if (isDebugging)
					log.debug("Roll Revision");
				rollRevision();
				refreshValues();
			} else if (b.getText().equals("New Object ID")) {
				if (isDebugging)
					log.debug("New Object ID");
				assignNewObjectId();
				refreshValues();
			}
		}
	}

}
