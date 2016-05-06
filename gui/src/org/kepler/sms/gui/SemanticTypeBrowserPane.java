/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.kepler.sms.NamedOntClass;
import org.kepler.sms.NamedOntModel;
import org.kepler.sms.NamedOntProperty;
import org.kepler.sms.OntologyCatalog;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * This ui is meant as a generic tool to navigate owl ontologies within Kepler.
 * 
 * This one isn't finished yet ... but once we get more complex ontologies, we
 * can add more
 * 
 * Some assumptions: (1) ontologies have already been classifed; and (2) labels
 * are used as the "real" name of the concept, otherwise local name is used
 * 
 * @author Shawn Bowers
 */
public class SemanticTypeBrowserPane extends JPanel {

	/**
	 * create a new semantic type browser panel if classView is true, then
	 * forces only the class tab, and if false, forces only the property tab.
	 */
	public SemanticTypeBrowserPane(java.awt.Container container,
			boolean classView) {
		this(container);
		if (classView) {
			_tabbedPane.setEnabledAt(0, true);
			_tabbedPane.setEnabledAt(1, false);
			_tabbedPane.setSelectedIndex(0);
		} else {
			_tabbedPane.setEnabledAt(0, false);
			_tabbedPane.setEnabledAt(1, true);
			_tabbedPane.setSelectedIndex(1);
		}
	}

	/**
	 * create a new semantic type browser panel
	 */
	public SemanticTypeBrowserPane(java.awt.Container container) {
		super();
		_container = container;

		_classPane = new JPanel(new GridLayout(1, 2));
		_classPane.add(_createClassListPane());
		_classPane.add(_createClassNavPane());
		_propPane = new JPanel(new GridLayout(1, 2));
		_propPane.add(_createPropListPane());
		_propPane.add(_createPropNavPane());

		_tabbedPane.addTab("classes", _classPane);
		_tabbedPane.addTab("properties", _propPane);

		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		this.add(_createOntoSelectPane());
		this.add(Box.createRigidArea(new Dimension(0, 10)));
		this.add(_tabbedPane);
		this.add(_createCommentPane());
		this.add(_createControlPane());

		// this.setPreferredSize(new Dimension(PREFERRED_WIDTH,
		// PREFERRED_HEIGHT));
		init_ontos();
	}

	/**
	 * initializes the ontology selection panel
	 */
	private JPanel _createOntoSelectPane() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
		_ontoselectCmb.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
		pane.add(new JLabel("ontology", SwingConstants.LEFT));
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		pane.add(_ontoselectCmb);
		pane.add(Box.createHorizontalGlue());

		return pane;
	}

	/**
	 * initializes the class basic panel (left-hand side)
	 */
	private JPanel _createClassListPane() {
		JPanel pane = new JPanel();

		// create layout
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// add space between selector and tree; 0 width, 5 pixels high
		// pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// subclass label
		JPanel classPanel = new JPanel();
		classPanel.setLayout(new BoxLayout(classPanel, BoxLayout.X_AXIS));
		classPanel.add(new JLabel("class", SwingConstants.LEFT));
		classPanel.add(Box.createHorizontalGlue());
		pane.add(classPanel);

		// add the class view panel (list of classes)
		JScrollPane classView = new JScrollPane(_classLst);
		_classLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // for
																			// now
																			// ...
																			// later
																			// permit
																			// multiple
		_classLst.addListSelectionListener(new _ClassListSelectionListener());
		classView
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		classView
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		pane.add(classView);

		// add space between tree and search
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// the panel contains the textfield and go button
		JPanel textFieldAndButtonPanel = new JPanel();
		textFieldAndButtonPanel.setLayout(new BoxLayout(
				textFieldAndButtonPanel, BoxLayout.X_AXIS));
		textFieldAndButtonPanel.add(_searchTxt);
		_searchTxt.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
		_searchTxt.addActionListener(new _ClassSearchButtonListener());
		textFieldAndButtonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		textFieldAndButtonPanel.add(_classSearchBtn);
		_classSearchBtn.addActionListener(new _ClassSearchButtonListener());
		pane.add(textFieldAndButtonPanel);

		return pane;
	}

	/**
	 * initializes the prop basic panel (left-hand side)
	 */
	private JPanel _createPropListPane() {
		JPanel pane = new JPanel();

		// create layout
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// subclass label
		JPanel propPanel = new JPanel();
		propPanel.setLayout(new BoxLayout(propPanel, BoxLayout.X_AXIS));
		propPanel.add(new JLabel("property", SwingConstants.LEFT));
		propPanel.add(Box.createHorizontalGlue());
		pane.add(propPanel);

		// add the class view panel (list of classes)
		JScrollPane propView = new JScrollPane(_propLst);
		_propLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // for
																		// now
																		// ...
																		// later
																		// permit
																		// multiple
		_propLst.addListSelectionListener(new _PropListSelectionListener());
		propView
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		propView
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		pane.add(propView);

		// add space between tree and search
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// the panel contains the textfield and go button
		JPanel textFieldAndButtonPanel = new JPanel();
		textFieldAndButtonPanel.setLayout(new BoxLayout(
				textFieldAndButtonPanel, BoxLayout.X_AXIS));
		textFieldAndButtonPanel.add(_propSearchTxt);
		_propSearchTxt.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
		_propSearchTxt.addActionListener(new _PropSearchButtonListener());
		textFieldAndButtonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		textFieldAndButtonPanel.add(_classSearchBtn);
		_classSearchBtn.addActionListener(new _PropSearchButtonListener());
		pane.add(textFieldAndButtonPanel);

		return pane;
	}

	/**
	 * initializes the control panel (the two buttons)
	 */
	private JPanel _createControlPane() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
		pane.add(Box.createHorizontalGlue());
		pane.add(_selectBtn);
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		pane.add(_closeBtn);

		_selectBtn.setMnemonic(KeyEvent.VK_S);

		_selectBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				_fireSemanticTypeBrowserSelection(ev);
			}
		});

		_closeBtn.setMnemonic(KeyEvent.VK_C);
		_closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				_container.setVisible(false);
			}
		});

		return pane;
	}

	/**
	 * initializes the detail class pane (the right-hand side)
	 **/
	private JPanel _createClassNavPane() {
		JPanel pane = new JPanel();
		int y_min = 80;

		// create layout
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// superclass label
		JPanel superclassPanel = new JPanel();
		superclassPanel.setLayout(new BoxLayout(superclassPanel,
				BoxLayout.X_AXIS));
		superclassPanel.add(new JLabel("superclass", SwingConstants.LEFT));
		superclassPanel.add(Box.createHorizontalGlue());
		pane.add(superclassPanel);

		// superclass list
		y_min = 40;
		JScrollPane superclassView = new JScrollPane(_superclassLst);
		_superclassLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_superclassLst.addMouseListener(new _SuperclassMouseAdapter());
		superclassView.setMinimumSize(new Dimension(120, y_min));
		superclassView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		superclassView.setPreferredSize(new Dimension(120, y_min));
		pane.add(superclassView);

		// add space between superclass and property
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// subclass label
		JPanel subclassPanel = new JPanel();
		subclassPanel.setLayout(new BoxLayout(subclassPanel, BoxLayout.X_AXIS));
		subclassPanel.add(new JLabel("subclass", SwingConstants.LEFT));
		subclassPanel.add(Box.createHorizontalGlue());
		pane.add(subclassPanel);

		// subclass list
		y_min = 80;
		JScrollPane subclassView = new JScrollPane(_subclassLst);
		_subclassLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_subclassLst.addMouseListener(new _SubclassMouseAdapter());
		subclassView.setMinimumSize(new Dimension(120, y_min));
		subclassView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		subclassView.setPreferredSize(new Dimension(120, y_min));
		pane.add(subclassView);

		// add space between superclass and property
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// sibling label
		JPanel siblingPanel = new JPanel();
		siblingPanel.setLayout(new BoxLayout(siblingPanel, BoxLayout.X_AXIS));
		siblingPanel.add(new JLabel("sibling", SwingConstants.LEFT));
		siblingPanel.add(Box.createHorizontalGlue());
		pane.add(siblingPanel);

		// sibling list
		y_min = 80;
		JScrollPane siblingView = new JScrollPane(_classSiblingLst);
		_classSiblingLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_classSiblingLst.addMouseListener(new _ClassSiblingMouseAdapter());
		siblingView.setMinimumSize(new Dimension(120, y_min));
		siblingView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		siblingView.setPreferredSize(new Dimension(120, y_min));
		pane.add(siblingView);

		// add space between subclass and superclass
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// property label
		JPanel propPanel = new JPanel();
		propPanel.setLayout(new BoxLayout(propPanel, BoxLayout.X_AXIS));
		propPanel.add(new JLabel("property", SwingConstants.LEFT));
		propPanel.add(Box.createHorizontalGlue());
		pane.add(propPanel);

		// property list
		y_min = 80;
		JScrollPane propView = new JScrollPane(_classPropLst);
		_classPropLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_classPropLst
				.addListSelectionListener(new _ClassPropListSelectionListener());
		propView.setMinimumSize(new Dimension(120, y_min));
		propView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		propView.setPreferredSize(new Dimension(120, y_min));
		pane.add(propView);

		// add space between subclass and superclass
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// property range label
		JPanel propRangePanel = new JPanel();
		propRangePanel
				.setLayout(new BoxLayout(propRangePanel, BoxLayout.X_AXIS));
		propRangePanel.add(new JLabel("property range", SwingConstants.LEFT));
		propRangePanel.add(Box.createHorizontalGlue());
		pane.add(propRangePanel);

		// property range list
		y_min = 40;
		JScrollPane propRangeView = new JScrollPane(_classPropRangeLst);
		_classPropRangeLst
				.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_classPropRangeLst.addMouseListener(new _PropRangeMouseAdapter());
		propRangeView.setMinimumSize(new Dimension(120, y_min));
		propRangeView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		propRangeView.setPreferredSize(new Dimension(120, y_min));
		pane.add(propRangeView);

		return pane;
	}

	/**
	 * initializes the detail prop pane (the right-hand side)
	 **/
	private JPanel _createPropNavPane() {
		JPanel pane = new JPanel();
		int y_min = 80;

		// create layout
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// superprop label
		JPanel superpropPanel = new JPanel();
		superpropPanel
				.setLayout(new BoxLayout(superpropPanel, BoxLayout.X_AXIS));
		superpropPanel.add(new JLabel("superproperty", SwingConstants.LEFT));
		superpropPanel.add(Box.createHorizontalGlue());
		pane.add(superpropPanel);

		// superclass list
		y_min = 40;
		JScrollPane superpropView = new JScrollPane(_superpropLst);
		_superpropLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_superpropLst.addMouseListener(new _SuperpropMouseAdapter());
		superpropView.setMinimumSize(new Dimension(120, y_min));
		superpropView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		superpropView.setPreferredSize(new Dimension(120, y_min));
		pane.add(superpropView);

		// add space between superclass and property
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// subclass label
		JPanel subpropPanel = new JPanel();
		subpropPanel.setLayout(new BoxLayout(subpropPanel, BoxLayout.X_AXIS));
		subpropPanel.add(new JLabel("subproperty", SwingConstants.LEFT));
		subpropPanel.add(Box.createHorizontalGlue());
		pane.add(subpropPanel);

		// subclass list
		y_min = 80;
		JScrollPane subpropView = new JScrollPane(_subpropLst);
		_subpropLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_subpropLst.addMouseListener(new _SubpropMouseAdapter());
		subpropView.setMinimumSize(new Dimension(120, y_min));
		subpropView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		subpropView.setPreferredSize(new Dimension(120, y_min));
		pane.add(subpropView);

		// add space between superclass and property
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// sibling label
		JPanel siblingPanel = new JPanel();
		siblingPanel.setLayout(new BoxLayout(siblingPanel, BoxLayout.X_AXIS));
		siblingPanel.add(new JLabel("sibling", SwingConstants.LEFT));
		siblingPanel.add(Box.createHorizontalGlue());
		pane.add(siblingPanel);

		// sibling list
		y_min = 80;
		JScrollPane siblingView = new JScrollPane(_propSiblingLst);
		_propSiblingLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_propSiblingLst.addMouseListener(new _PropSiblingMouseAdapter());
		siblingView.setMinimumSize(new Dimension(120, y_min));
		siblingView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		siblingView.setPreferredSize(new Dimension(120, y_min));
		pane.add(siblingView);

		// add space between subclass and superclass
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// domain label
		JPanel propPanel = new JPanel();
		propPanel.setLayout(new BoxLayout(propPanel, BoxLayout.X_AXIS));
		propPanel.add(new JLabel("domain", SwingConstants.LEFT));
		propPanel.add(Box.createHorizontalGlue());
		pane.add(propPanel);

		// property list
		y_min = 80;
		JScrollPane propView = new JScrollPane(_propDomainClassLst);
		_propDomainClassLst
				.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_propDomainClassLst
				.addListSelectionListener(new _PropDomainMouseAdapter());
		propView.setMinimumSize(new Dimension(120, y_min));
		propView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		propView.setPreferredSize(new Dimension(120, y_min));
		pane.add(propView);

		// add space between subclass and superclass
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// property range label
		JPanel propRangePanel = new JPanel();
		propRangePanel
				.setLayout(new BoxLayout(propRangePanel, BoxLayout.X_AXIS));
		propRangePanel.add(new JLabel("property range", SwingConstants.LEFT));
		propRangePanel.add(Box.createHorizontalGlue());
		pane.add(propRangePanel);

		// property range list
		y_min = 40;
		JScrollPane propRangeView = new JScrollPane(_propRangeLst);
		_propRangeLst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_propRangeLst.addMouseListener(new _PropRangeMouseAdapter());
		propRangeView.setMinimumSize(new Dimension(120, y_min));
		propRangeView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		propRangeView.setPreferredSize(new Dimension(120, y_min));
		pane.add(propRangeView);

		return pane;
	}

	/**
     * 
     */
	private JPanel _createCommentPane() {
		JPanel pane = new JPanel();
		int y_min = 60;

		// create layout
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		// pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		pane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // top,
																		// left,
																		// bottom,
																		// right

		// comment label
		JPanel commentPanel = new JPanel();
		commentPanel.setLayout(new BoxLayout(commentPanel, BoxLayout.X_AXIS));
		commentPanel.add(new JLabel("comment", SwingConstants.LEFT));
		commentPanel.add(Box.createHorizontalGlue());
		pane.add(commentPanel);

		// comment list
		JScrollPane commentView = new JScrollPane(_commentTxt);
		_commentTxt.setEditable(false);
		_commentTxt.setLineWrap(true);
		_commentTxt.setWrapStyleWord(true);
		commentView.setMinimumSize(new Dimension(120, y_min));
		commentView.setMaximumSize(new Dimension(Short.MAX_VALUE, y_min));
		commentView.setPreferredSize(new Dimension(120, y_min));
		pane.add(commentView);

		return pane;
	}

	/**
	 * adds a listener for selection events
	 */
	public void addSelectionListener(SemanticTypeBrowserSelectionListener l) {
		if (!_selectListeners.contains(l)) {
			_selectListeners.add(l);
		}
	}

	/**
	 * load ontologies into _ontoselectCmb
	 */
	private void init_ontos() {
		_catalog = OntologyCatalog.instance();
		for (Iterator iter = _catalog.getNamedOntModels(); iter.hasNext();)
			_ontoselectCmb.addItem(iter.next());
		_ontoselectCmb.setSelectedIndex(-1);
		_ontoselectCmb.addActionListener(new _OntoSelectListener());
	}

	/**
	 * TODO: change to ontology catalog
	 */
	private void loadOntology(NamedOntModel mdl) {
		// get a sorted list of names
		Iterator iter = mdl.getNamedClasses();
		Vector results = new Vector();
		while (iter.hasNext())
			results.add(iter.next());
		_classLst.setListData(results);
		iter = mdl.getNamedProperties(true);
		results = new Vector();
		while (iter.hasNext())
			results.add(iter.next());
		_propLst.setListData(results);
	}

	//
	// helper method:
	// clears the various details
	//
	private void _clearGUI() {
		_subclassLst.setListData(new Vector());
		_superclassLst.setListData(new Vector());
		_classSiblingLst.setListData(new Vector());
		_classPropLst.setListData(new Vector());
		_classPropRangeLst.setListData(new Vector());
		_subpropLst.setListData(new Vector());
		_superpropLst.setListData(new Vector());
		_propSiblingLst.setListData(new Vector());
		_propDomainClassLst.setListData(new Vector());
		_propRangeLst.setListData(new Vector());
		_commentTxt.setText("");
	}

	protected void _fireSemanticTypeBrowserSelection(ActionEvent ev) {
		NamedOntClass c = (NamedOntClass) _classLst.getSelectedValue();
		if (c == null)
			return;
		SemanticTypeBrowserSelectionEvent event;
		event = new SemanticTypeBrowserSelectionEvent(ev.getSource(), c);
		SemanticTypeBrowserSelectionListener l;

		for (Iterator iter = _selectListeners.iterator(); iter.hasNext();) {
			l = (SemanticTypeBrowserSelectionListener) iter.next();
			l.valueChanged(event);
		}
	}

	/* LISTENERS */

	/**
	 * listener for _ontselectCmb
	 */
	private class _OntoSelectListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getModifiers() == 0)
				return;
			JComboBox cb = (JComboBox) e.getSource();
			NamedOntModel mdl = (NamedOntModel) cb.getSelectedItem();
			if (mdl != null) {
				_clearGUI();
				loadOntology(mdl);
			}
		}
	}

	/**
	 * listener for click events on _subclassLst
	 */
	private class _SubclassMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent ev) {
			if (ev.getClickCount() == 1) {
				int index = _subclassLst.locationToIndex(ev.getPoint());
				NamedOntClass item = (NamedOntClass) _subclassLst.getModel()
						.getElementAt(index);
				if (!_inClassList(item)) {
					JOptionPane.showMessageDialog(_getFrame(), "'" + item
							+ "' is not defined in this ontology.");
					_subclassLst.clearSelection();
					return;
				}
				if (item != null)
					_goToClass(item);
			}
		}
	};

	/**
	 * listener for click events on _subpropLst
	 */
	private class _SubpropMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent ev) {
			if (ev.getClickCount() == 1) {
				int index = _subpropLst.locationToIndex(ev.getPoint());
				NamedOntProperty item = (NamedOntProperty) _subpropLst
						.getModel().getElementAt(index);
				if (!_inPropList(item)) {
					JOptionPane.showMessageDialog(_getFrame(), "'" + item
							+ "' is not defined in this ontology.");
					_subpropLst.clearSelection();
					return;
				}
				if (item != null)
					_goToProp(item);
			}
		}
	};

	/**
	 * listener for click events on _superclassLst
	 */
	private class _SuperclassMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent ev) {
			if (ev.getClickCount() == 1) {
				int index = _superclassLst.locationToIndex(ev.getPoint());
				NamedOntClass item = (NamedOntClass) _superclassLst.getModel()
						.getElementAt(index);
				if (!_inClassList(item)) {
					JOptionPane.showMessageDialog(_getFrame(), "'" + item
							+ "' is not defined in this ontology.");
					_superclassLst.clearSelection();
					return;
				}
				if (item != null)
					_goToClass(item);
			}
		}
	};

	/**
	 * listener for click events on _superpropLst
	 */
	private class _SuperpropMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent ev) {
			if (ev.getClickCount() == 1) {
				int index = _superpropLst.locationToIndex(ev.getPoint());
				NamedOntProperty item = (NamedOntProperty) _superpropLst
						.getModel().getElementAt(index);
				if (!_inPropList(item)) {
					JOptionPane.showMessageDialog(_getFrame(), "'" + item
							+ "' is not defined in this ontology.");
					_superpropLst.clearSelection();
					return;
				}
				if (item != null)
					_goToProp(item);
			}
		}
	};

	/**
	 * listener for double-click events on _classSiblingLst
	 */
	private class _ClassSiblingMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent ev) {
			if (ev.getClickCount() == 1) {
				int index = _classSiblingLst.locationToIndex(ev.getPoint());
				NamedOntClass item = (NamedOntClass) _classSiblingLst
						.getModel().getElementAt(index);
				if (!_inClassList(item)) {
					JOptionPane.showMessageDialog(_getFrame(), "'" + item
							+ "' is not defined in this ontology.");
					_classSiblingLst.clearSelection();
					return;
				}
				if (item != null)
					_goToClass(item);
			}
		}
	};

	/**
	 * listener for double-click events on _propSiblingLst
	 */
	private class _PropSiblingMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent ev) {
			if (ev.getClickCount() == 1) {
				int index = _propSiblingLst.locationToIndex(ev.getPoint());
				NamedOntProperty item = (NamedOntProperty) _propSiblingLst
						.getModel().getElementAt(index);
				if (!_inPropList(item)) {
					JOptionPane.showMessageDialog(_getFrame(), "'" + item
							+ "' is not defined in this ontology.");
					_propSiblingLst.clearSelection();
					return;
				}
				if (item != null)
					_goToProp(item);
			}
		}
	};

	/**
	 * listener for _classLst
	 */
	private class _ClassListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent ev) {
			if (ev.getValueIsAdjusting() == false) {
				NamedOntClass c = (NamedOntClass) _classLst.getSelectedValue();
				if (c == null)
					return;
				// reset dialogs
				_clearGUI();
				// vars
				Vector result;
				Iterator iter;
				// get subclasses
				result = new Vector();
				for (iter = c.getNamedSubClasses(true); iter.hasNext();)
					result.add(iter.next());
				_subclassLst.setListData(result);
				// get superclasses
				result = new Vector();
				for (iter = c.getNamedSuperClasses(true); iter.hasNext();)
					result.add(iter.next());
				_superclassLst.setListData(result);
				// get the properties
				result = new Vector();
				for (iter = c.getNamedProperties(true); iter.hasNext();)
					result.add(iter.next());
				_classPropLst.setListData(result);
				// get the siblings (i.e., superclasses, subclasses)
				result = new Vector();
				for (iter = c.getNamedSuperClasses(false); iter.hasNext();) {
					NamedOntClass cls = (NamedOntClass) iter.next();
					for (Iterator iter2 = cls.getNamedSubClasses(false); iter2
							.hasNext();) {
						NamedOntClass cls2 = (NamedOntClass) iter2.next();
						if (!c.equals(cls2))
							result.add(cls2);
					}
				}
				Collections.sort(result);
				_classSiblingLst.setListData(result);
				// get the comment
				_commentTxt.setText(c.getComment());
			}
		}
	}

	/**
	 * listener for _classLst
	 */
	private class _PropListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent ev) {
			if (ev.getValueIsAdjusting() == false) {
				NamedOntProperty p = (NamedOntProperty) _propLst
						.getSelectedValue();
				if (p == null)
					return;
				// reset dialogs
				_clearGUI();
				// vars
				Vector result = new Vector();
				// get superclasses
				for (Iterator iter = p.getNamedSuperProperties(true); iter
						.hasNext();)
					result.add(iter.next());
				_superpropLst.setListData(result);
				// get subclasses
				result = new Vector();
				for (Iterator iter = p.getNamedSubProperties(true); iter
						.hasNext();)
					result.add(iter.next());
				_subpropLst.setListData(result);
				// get the domain classes
				result = new Vector();
				for (Iterator iter = p.getDomain(true); iter.hasNext();)
					result.add(iter.next());
				_propDomainClassLst.setListData(result);
				// get the ranges
				result = new Vector();
				for (Iterator iter = p.getRange(true); iter.hasNext();)
					result.add(iter.next());
				_propRangeLst.setListData(result);
				// get the siblings (i.e., superclasses, subclasses)
				result = new Vector();
				for (Iterator iter = p.getNamedSuperProperties(false); iter
						.hasNext();) {
					NamedOntProperty prop = (NamedOntProperty) iter.next();
					for (Iterator iter2 = prop.getNamedSubProperties(false); iter2
							.hasNext();) {
						NamedOntProperty prop2 = (NamedOntProperty) iter2
								.next();
						if (!p.equals(prop2))
							result.add(prop2);
					}
				}
				Collections.sort(result);
				_propSiblingLst.setListData(result);
				// // get the comment
				_commentTxt.setText(p.getComment());
			}
		}
	}

	// listener for _classPropLst
	private class _ClassPropListSelectionListener implements
			ListSelectionListener {
		public void valueChanged(ListSelectionEvent ev) {
			if (ev.getValueIsAdjusting() == false) {
				NamedOntProperty p = (NamedOntProperty) _classPropLst
						.getSelectedValue();
				if (p == null)
					return;
				Vector result = new Vector();
				for (Iterator iter = p.getRange(true); iter.hasNext();) {
					NamedOntClass cls = (NamedOntClass) iter.next();
					result.add(cls);
				}
				_classPropRangeLst.setListData(result);
			}
		}
	};

	// listener for _propDomainClassLst
	private class _PropDomainMouseAdapter implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent ev) {
			if (ev.getValueIsAdjusting() == false) {
				_propDomainClassLst.clearSelection();
			}
		}
	};

	// listener for _propRangeLst
	private class _PropRangeListMouseAdapter implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent ev) {
			if (ev.getValueIsAdjusting() == false) {
				_propRangeLst.clearSelection();
			}
		}
	};

	// listener for double-click events on _classPropRangeLst
	private class _PropRangeMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent ev) {
			if (ev.getClickCount() == 1) {
				int index = _classPropRangeLst.locationToIndex(ev.getPoint());
				NamedOntClass item = (NamedOntClass) _classPropRangeLst
						.getModel().getElementAt(index);
				if (!_inClassList(item)) {
					JOptionPane.showMessageDialog(_getFrame(), "'" + item
							+ "' is not defined in this ontology.");
					return;
				}
				if (item != null)
					_goToClass(item);
			}
		}
	};

	// listener for search button
	private class _ClassSearchButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			String searchStr = _searchTxt.getText();
			if (searchStr.trim().equals(""))
				return;
			Frame frame = _getFrame();
			Vector results = _findMatchingClasses(searchStr);
			if (results.size() == 0) {
				// create a dialog-box warning
				String errStr = "Could not find match for: " + searchStr;
				String diaStr = "Search Error";
				JOptionPane.showMessageDialog(frame, errStr, diaStr,
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			NamedOntClass result = OntoClassSearchDialog.showDialog(frame,
					results);
			if (result != null)
				_goToClass(result);
		}
	}

	// listener for search button
	private class _PropSearchButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			String searchStr = _searchTxt.getText();
			if (searchStr.trim().equals(""))
				return;
			Frame frame = _getFrame();
			Vector results = _findMatchingClasses(searchStr);
			if (results.size() == 0) {
				// create a dialog-box warning
				String errStr = "Could not find match for: " + searchStr;
				String diaStr = "Search Error";
				JOptionPane.showMessageDialog(frame, errStr, diaStr,
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			NamedOntProperty result = OntoPropSearchDialog.showDialog(frame,
					results);
			if (result != null)
				_goToProp(result);
		}
	}

	/**
	 * @return The frame for the panel.
	 */
	private Frame _getFrame() {
		java.awt.Container c = getParent();
		while (!(c instanceof Frame))
			c = c.getParent();
		return (Frame) c;
	}

	/**
	 * @return If the class is in the current ontology then true, otherwise
	 *         false.
	 */
	private boolean _inClassList(NamedOntClass cls) {
		ListModel mdl = _classLst.getModel();
		for (int i = 0; i < mdl.getSize(); i++) {
			if (cls.equals(mdl.getElementAt(i)))
				return true;
		}
		return false;
	}

	/**
	 * @return If the class is in the current ontology then true, otherwise
	 *         false.
	 */
	private boolean _inPropList(NamedOntProperty prop) {
		ListModel mdl = _propLst.getModel();
		for (int i = 0; i < mdl.getSize(); i++) {
			if (prop.equals(mdl.getElementAt(i)))
				return true;
		}
		return false;
	}

	/**
	 * Given a search string, returns lists of matching classes.
	 */
	private Vector _findMatchingClasses(String searchStr) {
		searchStr = searchStr.trim().toLowerCase(); // normalize search string
		Vector results = new Vector(); // to hold results
		ListModel m = _classLst.getModel(); // search class list
		for (int i = 0; i < m.getSize(); i++) { // for each class in list
			NamedOntClass cls = (NamedOntClass) m.getElementAt(i);
			String name = cls.getName().trim().toLowerCase();
			if (name.indexOf(searchStr) != -1)
				results.add(cls);
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * selects the class in the class list
	 */
	private void _goToClass(NamedOntClass namedClass) {
		// clear lists
		_clearGUI();
		// clear the current selection
		_classLst.clearSelection();
		_classLst.setSelectedValue(namedClass, true); // should scroll
	}

	/**
	 * selects the property in the property list
	 */
	private void _goToProp(NamedOntProperty namedProp) {
		// clear lists
		_clearGUI();
		// clear the current selection
		_propLst.clearSelection();
		_propLst.setSelectedValue(namedProp, true); // should scroll
	}

	/**
	 * for testing
	 */
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new SemanticTypeBrowserPane(frame));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Semantic Type Browser");
		frame.pack();
		frame.show();
		frame.setResizable(false);
	}

	/* PUBLIC MEMBERS */

	public static final int PREFERRED_WIDTH = 500;
	public static final int PREFERRED_HEIGHT = 680;

	/* PRIVATE MEMBERS */

	private String KEPLER = System.getProperty("KEPLER");
	private String ONTO_PATH = KEPLER
			+ "/configs/ptolemy/configs/kepler/ontologies/";

	private JComboBox _ontoselectCmb = new JComboBox(); // ontologies to choose
														// from
	private JTabbedPane _tabbedPane = new JTabbedPane();
	private JPanel _classPane;
	private JPanel _propPane;

	// class left-hand side components
	private JList _classLst = new JList(); // list of classes in ontology
	private JTextField _searchTxt = new JTextField(15); // text field for search
	private JButton _classSearchBtn = new JButton("search"); // button to search
																// for concepts
	// class right-hand side components
	private JList _superclassLst = new JList(); // text list for superclasses
	private JList _subclassLst = new JList(); // text list for subclasses
	private JList _classSiblingLst = new JList(); // text list for siblings
	private JList _classPropLst = new JList(); // text list for properties
	private JList _classPropRangeLst = new JList(); // text list from property
													// values

	// property left-hand side components
	private JList _propLst = new JList(); // list of classes in ontology
	private JTextField _propSearchTxt = new JTextField(15); // text field for
															// search
	private JButton _propSearchBtn = new JButton("search"); // button to search
															// for concepts
	// property right-hand side components
	private JList _superpropLst = new JList(); // text list for superclasses
	private JList _subpropLst = new JList(); // text list for subclasses
	private JList _propSiblingLst = new JList(); // text list for siblings
	private JList _propDomainClassLst = new JList(); // text list for properties
	private JList _propRangeLst = new JList(); // text list from property values

	private JTextArea _commentTxt = new JTextArea(4, 15); // text field for
															// search

	// bottom components
	private JButton _selectBtn = new JButton("Select"); // button to select
														// ontologies (close
														// window)
	private JButton _closeBtn = new JButton("Close"); // button to cancel
														// navigation (cancel
														// window)

	private java.awt.Container _container;

	// current ontology model
	private OntModel _currentModel; // currently loaded model
	private Vector _selectListeners = new Vector(); // listeners
	private OntologyCatalog _catalog; // ontology catalog

} // SemanticTypeBrowserPane