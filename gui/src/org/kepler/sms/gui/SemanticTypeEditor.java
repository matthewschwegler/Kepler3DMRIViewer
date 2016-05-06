/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:25 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31122 $'
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * SemanticTypeEditor is a dialog for adding and removing semantic types to
 * particular workflow components.
 * 
 * @author Shawn Bowers
 */
public class SemanticTypeEditor extends JDialog {

	/**
	 * This is the default constructor
	 */
	public SemanticTypeEditor(Frame owner, NamedObj namedObj) {
		super(owner);
		// set title and close behavior
		this.setTitle("Semantic Type Annotation");
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		// create the frame's pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Add our private anonymous window listener so we can
		// call _doClose on windowClosing
		this.addWindowListener(_windowListener);
		
		// create the actor and port annotations pane
		_actorPane = new ActorSemanticTypeEditorPane(owner, namedObj);
		int IN = PortSemanticTypeEditorPane.INPUT;
		int OUT = PortSemanticTypeEditorPane.OUTPUT;
		_inputPortPane = new PortSemanticTypeEditorPane(owner, namedObj, IN);
		_outputPortPane = new PortSemanticTypeEditorPane(owner, namedObj, OUT);

		// create the tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Component", _actorPane);
		tabbedPane.addTab("Input", _inputPortPane);
		tabbedPane.addTab("Output", _outputPortPane);

		// add the components
		pane.add(tabbedPane);
		pane.add(Box.createRigidArea(new Dimension(5, 0)));
		pane.add(_createButtons());
		// add the pane
		setContentPane(pane);
		setSize(550, 700);
		// this.setResizable(false);
	}

	/**
	 * Initialize the bottom buttons (close)
	 */
	private JPanel _createButtons() {
		// init buttons
		_helpBtn.setMnemonic(KeyEvent.VK_H);
		_commitBtn.setMnemonic(KeyEvent.VK_O);
		_commitBtn.setActionCommand("commit");
		_commitBtn.setToolTipText("Save changes to annotations");
		_commitBtn.addActionListener(_buttonListener);
		_closeBtn.setMnemonic(KeyEvent.VK_C);
		_closeBtn.setActionCommand("close");
		_closeBtn.setToolTipText("Close editor");
		_closeBtn.addActionListener(_buttonListener);

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(Box.createHorizontalGlue());
		pane.add(_helpBtn);
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		pane.add(_closeBtn);
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		pane.add(_commitBtn);

		return pane;
	}

	/**
	 * anonymous class to handle button events
	 */
	private ActionListener _buttonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("commit"))
				_doCommit();
			else if (e.getActionCommand().equals("close"))
				_doClose();
		}
	};

	/**
	 * Performs the cancel button operation.
	 */
	private void _doClose() {
		if (_actorPane.hasModifiedSemTypes()
				|| _inputPortPane.hasModifiedSemTypes()
				|| _outputPortPane.hasModifiedSemTypes()) {

			// do a JDialog
			Object[] options = { "Yes", "No", "Cancel" };
			String msg = "Would you like to save the changes you made?";
			int n = JOptionPane.showOptionDialog(this, msg, "Message",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
			if (n == 0) {
				_doCommit();
				return;
			} else if (n == 1) {
				_close();
				return;
			}
		}
		_close();
	}

	/**
	 * Performs the commit button operation. Commit, like in Ptolemy, saves the
	 * changes and closes the window.
	 */
	private void _doCommit() {
		// ensure well formed semtypes
		String msg;
		if ((msg = _actorPane.wellFormedSemTypes()) != null) {
			JOptionPane.showMessageDialog(this, msg, "Message",
					JOptionPane.ERROR_MESSAGE);
			return; // abort
		} else if ((msg = _inputPortPane.wellFormedSemTypes()) != null) {
			JOptionPane.showMessageDialog(this, msg, "Message",
					JOptionPane.ERROR_MESSAGE);
			return; // abort
		} else if ((msg = _outputPortPane.wellFormedSemTypes()) != null) {
			JOptionPane.showMessageDialog(this, msg, "Message",
					JOptionPane.ERROR_MESSAGE);
			return; // abort
		}
		_actorPane.doCommit();
		_inputPortPane.doCommit();
		_outputPortPane.doCommit();
		_close();
	}

	/**
	 * Really shut down everything
	 */
	private void _close() {
		_actorPane.doClose();
		_inputPortPane.doClose();
		_outputPortPane.doClose();
		dispose();
	}

	/**
	 * Main method for testing the dialog.
	 * 
	 * @param args
	 *            the arguments to the program
	 */
	public static void main(String[] args) {
		try {
			// a composite "wrapper"
			TypedCompositeActor swf = new TypedCompositeActor();

			// create an actor; set it's name
			TypedAtomicActor a1 = new TypedAtomicActor(swf, "a1");

			// add a semantic type category to it
			// SemanticType s = new SemanticType();

			// input ports
			TypedIOPort i1 = addInputType(a1, BaseType.INT, "in1");
			TypedIOPort i2 = addInputType(a1, new ArrayType(BaseType.DOUBLE),
					"in2");
			TypedIOPort i3 = addInputType(a1, BaseType.STRING, "in3");

			// output port types
			String[] o1_labels = { "a", "b", "c" };
			ptolemy.data.type.Type[] o1_types = { BaseType.DOUBLE, BaseType.INT, BaseType.STRING };
			String[] o2_sublabels = { "c", "d" };
			ptolemy.data.type.Type[] o2_subtypes = { BaseType.STRING, BaseType.INT };
			String[] o2_labels = { "a", "b" };
			ptolemy.data.type.Type[] o2_types = { new ArrayType(BaseType.DOUBLE),
					new RecordType(o2_sublabels, o2_subtypes) };

			// output ports
			TypedIOPort o1 = addOutputType(a1, new RecordType(o1_labels,
					o1_types), "out1");
			TypedIOPort o2 = addOutputType(a1, new RecordType(o2_labels,
					o2_types), "out2");

			// // create a port generalization
			// KeplerIOPortGeneralization g1 = new
			// KeplerIOPortGeneralization(a1, "g1");
			// g1.addEncapsulatedPort(i1);
			// g1.addEncapsulatedPort(i2);

			// // create another one
			// KeplerIOPortGeneralization g2 = new
			// KeplerIOPortGeneralization(a1, "g2");
			// g2.addEncapsulatedPort(g1);
			// g2.addEncapsulatedPort(i3);

			// // create yet another one
			// KeplerIOPortGeneralization g3 = new
			// KeplerIOPortGeneralization(a1, "g3");
			// g3.addEncapsulatedPort(i2);
			// g3.addEncapsulatedPort(i3);

			// add some semantic types
			// SemanticType s1 = new SemanticType(i1, "_semType0");
			// s1.setConceptId("#Lat");
			// SemanticType s2 = new SemanticType(i2, "_semType0");
			// s2.setConceptId("#Lon");
			// SemanticType s3 = new SemanticType(g1, "_semType0");
			// s3.setConceptId("#Location");

			// create port refinements
			// KeplerIOPortRefinement r1 = new KeplerIOPortRefinement(a1,
			// "in2/elem");
			// KeplerIOPortRefinement r2 = new KeplerIOPortRefinement(a1,
			// "out1/b");
			// KeplerIOPortRefinement r3 = new KeplerIOPortRefinement(a1,
			// "out2/a");
			// KeplerIOPortRefinement r4 = new KeplerIOPortRefinement(a1,
			// "out2/a/elem");
			// KeplerIOPortRefinement r5 = new KeplerIOPortRefinement(a1,
			// "out2/b/d");

			// create a port generalization from the refinements
			// KeplerIOPortGeneralization g4 = new
			// KeplerIOPortGeneralization(a1, "g4");
			// g4.addEncapsulatedPort(r2);
			// g4.addEncapsulatedPort(r4);

			// create a semantic link
			// KeplerIOPortSemanticLink l1 = new KeplerIOPortSemanticLink(g1,
			// "_link1");
			// l1.setRange(g2);
			// SemanticProperty p1 = new SemanticProperty(l1, "_semProp0");
			// p1.setPropertyId("#prop1");
			// l1.setRange(r1);

			// windows look and feel
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SemanticTypeEditor editor = new SemanticTypeEditor(null, a1);
			editor.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static TypedIOPort addInputType(TypedAtomicActor actor, ptolemy.data.type.Type type,
			String name) throws NameDuplicationException,
			IllegalActionException {
		TypedIOPort p = (TypedIOPort) actor.newPort(name);
		p.setTypeEquals(type);
		p.setInput(true);
		p.setOutput(false);
		return p;
	}

	public static TypedIOPort addOutputType(TypedAtomicActor actor, ptolemy.data.type.Type type,
			String name) throws NameDuplicationException,
			IllegalActionException {
		TypedIOPort p = (TypedIOPort) actor.newPort(name);
		p.setTypeEquals(type);
		p.setInput(false);
		p.setOutput(true);
		return p;
	}
	
	// Anonymous WindowListener to invoke _doClose on
	// windowClosing.
	private WindowListener _windowListener = new WindowListener() {
		public void windowActivated(WindowEvent e) {				
		}
		public void windowClosed(WindowEvent e) {
		}
		public void windowClosing(WindowEvent e) {			
			_doClose();
		}
		public void windowDeactivated(WindowEvent e) {				
		}
		public void windowDeiconified(WindowEvent e) {				
		}
		public void windowIconified(WindowEvent e) {				
		}
		public void windowOpened(WindowEvent e) {	
		}
	};

	// private members

	private ActorSemanticTypeEditorPane _actorPane;
	private PortSemanticTypeEditorPane _inputPortPane;
	private PortSemanticTypeEditorPane _outputPortPane;

	private JButton _commitBtn = new JButton("OK"); // button for commiting
													// changes
	private JButton _closeBtn = new JButton("Cancel"); // button to close dialog
	private JButton _helpBtn = new JButton("Help"); // button to close dialog
	private JCheckBox _saveBox;
	private Frame _owner; // the owner of this dialog
}