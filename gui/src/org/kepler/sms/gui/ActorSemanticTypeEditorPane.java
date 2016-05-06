/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.kepler.sms.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.kepler.moml.NamedObjId;
import org.kepler.sms.NamedOntClass;
import org.kepler.sms.SMSServices;
import org.kepler.sms.SemanticType;

import ptolemy.kernel.util.NamedObj;

/**
 * SemanticTypeEditor is a dialog for adding and removing semantic types to
 * particular workflow components.
 * 
 * @author Shawn Bowers
 */
public class ActorSemanticTypeEditorPane extends JPanel {

	private Frame _owner;
	private NamedObj _namedObj;
	private OntoClassSelectionJPanel _classSelector;
	private Vector<NamedOntClass> _initialClasses;
	
	/**
	 * Constructor.
	 */
	public ActorSemanticTypeEditorPane(Frame owner, NamedObj namedObj) {
		super();
		_owner = owner;
		_namedObj = namedObj;
		_initialClasses = new Vector<NamedOntClass>();
		
		System.out.println(System.getProperty("java.class.path"));

		// library only class selection panel
		_classSelector = new OntoClassSelectionJPanel(true, 500, 425);
		// load the library
		loadClassSelector();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		add(Box.createRigidArea(new Dimension(0, 40)));
		add(_createHeader());
		add(Box.createRigidArea(new Dimension(0, 40)));
		add(_classSelector);
	}

	/**
	 * Load the existing semtypes for the entity
	 */
	private void loadClassSelector() {
		Iterator iter = SMSServices.getActorSemanticTypes(_namedObj).iterator();
		while (iter.hasNext()) {
			SemanticType semtype = (SemanticType) iter.next();
			// create a NamedOntClass out of the type
			NamedOntClass ontClass = SMSServices.getNamedOntClassFor(semtype);
			_classSelector.addNamedOntClass(ontClass);
			_initialClasses.add(ontClass);
		}
	}

	/**
	 * Performs the commit button operation.
	 */
	public void doCommit() {
		SMSServices.setActorSemanticTypes(_namedObj, _classSelector
				.getNamedOntClasses());
	}

	/**
	 * Performs the cancel button operation.
	 */
	public void doClose() {
	}

	/**
	 * @return True if any semantic type annotations have been modified.
	 */
	public boolean hasModifiedSemTypes() {
		Vector<NamedOntClass> currentClasses = _classSelector.getNamedOntClasses();
		// check if different sizes
		if (currentClasses.size() != _initialClasses.size())
			return true;
		Iterator<NamedOntClass> iter = currentClasses.iterator();
		// same size, check if not contained
		while (iter.hasNext()) {
			NamedOntClass noc = iter.next();
			if (!_initialClasses.contains(noc)){
				return true;
			}
		}
		// same list
		return false;
	}

	/**
	 * TODO: Check for valid semantic types
	 * 
	 * @return string describing error or null if okay
	 */
	public String wellFormedSemTypes() {
		return null;
	}

	/**
	 * @return The header information for the object
	 */
	private JPanel _createHeader() {
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
		panel1.add(new JLabel("   Name: "));
		panel1.add(new JLabel("   Type: "));
		panel1.add(new JLabel("   ID: "));

		String name = _namedObj.getName();
		if (name.length() > 12)
			name = name.substring(0, 11) + "...";

		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
		panel2.add(new JLabel(name));
		panel2.add(new JLabel(_namedObj.getClass().getName()));
		panel2.add(new JLabel(_getNamedObjId()));

		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));
		panel3.add(Box.createRigidArea(new Dimension(10, 0)));
		panel3.add(panel1);
		panel3.add(Box.createRigidArea(new Dimension(10, 0)));
		panel3.add(panel2);
		panel3.add(Box.createHorizontalGlue());
		return panel3;
	}

	/**
	 * @return The object id for the actor.
	 */
	private String _getNamedObjId() {
		if (_namedObj == null)
			return "<none>";
		for (Iterator iter = _namedObj.attributeList().iterator(); iter
				.hasNext();) {
			Object att = iter.next();
			if (att instanceof NamedObjId) {
				NamedObjId id = (NamedObjId) att;
				return id.getExpression();
			}
		}
		return "<none>";
	}

} // ActorSemanticTypeEditorPane