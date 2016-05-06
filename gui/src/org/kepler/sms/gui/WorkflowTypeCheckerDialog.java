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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import org.kepler.gui.GraphicalActorMetadata;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.sms.NamedOntClass;
import org.kepler.sms.OntologyCatalog;
import org.kepler.sms.SMSServices;
import org.kepler.sms.SemanticType;

import ptolemy.actor.IOPort;
import ptolemy.actor.IORelation;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.TypedIORelation;
import ptolemy.actor.gui.Tableau;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.ComponentRelation;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.Port;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.actor.ActorGraphFrame;
import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.kernel.Link;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.PTree;
import ptolemy.vergil.tree.VisibleTreeModel;
import ptolemy.vergil.unit.BasicEdgeHighlighter;
import diva.canvas.Figure;
import diva.canvas.interactor.BasicSelectionRenderer;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.SelectionRenderer;
import diva.graph.GraphController;
import diva.graph.GraphModel;
import diva.graph.GraphPane;
import diva.graph.GraphUtilities;
import diva.graph.JGraph;

/**
 * This dialog provides a simple interface for performing semantic and
 * structural static type checking. The interface includes both "unsafe" and
 * "potentially unsafe" connections. FIXME: Haven't figured out quite why the
 * getType method on composite actors doesn't work ... there is probably
 * something that needs to be called prior to obtaining the types, but couldn't
 * find it.
 * 
 * @author Shawn Bowers
 */
public class WorkflowTypeCheckerDialog extends JDialog {

	/**
	 * Default construct
	 * 
	 * @param owner
	 *            The frame that called the dialog
	 * @param entity
	 *            The entity that is being type checked.
	 */
	public WorkflowTypeCheckerDialog(Frame owner, Entity entity) {
		super(owner);
		_owner = owner;
		_entity = entity;
		_catalog = OntologyCatalog.instance();
		this.setTitle("Structural and Semantic Type Checker");
		_initializeDialog();
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		// FIXME: Figure out why composites getType method doesn't seem to work
		// ...
		// "tighten" the types if possible ...
		try {
			if (entity instanceof TypedCompositeActor)
				TypedCompositeActor.resolveTypes((TypedCompositeActor) entity);
		} catch (Exception e) {
		}

		// initialize the selection utility of diva
		_initializeSelections();
	}

	/**
	 * This method initializes the dialog
	 */
	private void _initializeDialog() {
		_pane = new JPanel();
		_pane.setLayout(new BoxLayout(_pane, BoxLayout.Y_AXIS));
		_pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel channelPane = new JPanel();
		channelPane.setLayout(new BoxLayout(channelPane, BoxLayout.X_AXIS));
		channelPane.setBorder(_createTitledBorder("Channels"));
		channelPane.add(_createUnsafePane());
		channelPane.add(Box.createRigidArea(new Dimension(10, 0)));
		channelPane.add(_createUnknownPane());

		_pane.add(channelPane);
		_pane.add(Box.createRigidArea(new Dimension(0, 10)));
		_pane.add(_createStructTypeDisplay());
		_pane.add(Box.createRigidArea(new Dimension(0, 10)));
		_pane.add(_createSemTypeDisplay());
		_pane.add(Box.createRigidArea(new Dimension(0, 10)));
		_pane.add(_createButtons());

		this.setSize(750, 500);
		this.setResizable(false);
		this.setContentPane(_pane);
	}

	/**
	 * Create the unsafe channel table and pane
	 */
	private JPanel _createUnsafePane() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// text label
		JPanel txtPane = new JPanel();
		txtPane.setLayout(new BoxLayout(txtPane, BoxLayout.LINE_AXIS));
		txtPane.add(new JLabel("Type Errors:"));
		txtPane.add(Box.createHorizontalGlue());

		// the table for unsafe channels
		_ChannelTableModel model = _getUnsafeChannels();
		_unsafeTable = new JTable(model);
		_unsafeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_unsafeTable.setPreferredScrollableViewportSize(new Dimension(325, 80));
		_unsafeTable.setColumnSelectionAllowed(false);

		// add the listener for row selections
		_unsafeTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						ListSelectionModel lsm = (ListSelectionModel) e
								.getSource();
						if (!e.getValueIsAdjusting() && !lsm.isSelectionEmpty())
							_adjustUnsafeHighlightedConnections(lsm);
					}
				});

		// add text label and table
		pane.add(txtPane);
		pane.add(new JScrollPane(_unsafeTable));
		return pane;
	}

	/**
	 * Create the unknown channel table and pane
	 */
	private JPanel _createUnknownPane() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// text label
		JPanel txtPane = new JPanel();
		txtPane.setLayout(new BoxLayout(txtPane, BoxLayout.LINE_AXIS));
		txtPane.add(new JLabel("Type Warnings:"));
		txtPane.add(Box.createHorizontalGlue());

		_ChannelTableModel model = _getUnknownChannels();
		_unknownTable = new JTable(model);
		_unknownTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_unknownTable
				.setPreferredScrollableViewportSize(new Dimension(325, 80));
		_unknownTable.setColumnSelectionAllowed(false);

		// add the listener for row selections
		_unknownTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						ListSelectionModel lsm = (ListSelectionModel) e
								.getSource();
						if (!e.getValueIsAdjusting() && !lsm.isSelectionEmpty())
							_adjustUnknownHighlightedConnections(lsm);
					}
				});

		// add text label and table
		pane.add(txtPane);
		pane.add(new JScrollPane(_unknownTable));

		return pane;
	}

	/**
	 * Create the structural type display
	 */
	private JPanel _createStructTypeDisplay() {
		// the main pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
		pane.setBorder(_createTitledBorder("Structural Types"));

		int w1 = 75; // the text width
		int w2 = 325; // the text width
		int w3 = 325; // the text width

		// the error message view
		JPanel view1 = new JPanel();
		view1.setLayout(new BoxLayout(view1, BoxLayout.Y_AXIS));
		view1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// the error message label
		JPanel v1p1 = new JPanel();
		v1p1.setLayout(new BoxLayout(v1p1, BoxLayout.X_AXIS));
		v1p1.add(new JLabel("channel status", SwingConstants.LEFT));
		v1p1.add(Box.createHorizontalGlue());
		view1.add(v1p1);
		// the error message text box
		JPanel v1p2 = new JPanel();
		v1p2.setLayout(new BoxLayout(v1p2, BoxLayout.X_AXIS));
		_structSafetyTxt = new JTextField();
		_structSafetyTxt.setEditable(false);
		_structSafetyTxt.setMaximumSize(new Dimension(w1, 25));
		_structSafetyTxt.setPreferredSize(new Dimension(w1, 25));
		_structSafetyTxt.setBackground(Color.white);
		v1p2.add(_structSafetyTxt);
		v1p2.add(Box.createHorizontalGlue());
		view1.add(v1p2);
		// add the error message view
		pane.add(view1);

		// the output type view
		JPanel view2 = new JPanel();
		view2.setLayout(new BoxLayout(view2, BoxLayout.Y_AXIS));
		view2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// the output type label
		JPanel v2p1 = new JPanel();
		v2p1.setLayout(new BoxLayout(v2p1, BoxLayout.X_AXIS));
		v2p1.add(new JLabel("output", SwingConstants.LEFT));
		v2p1.add(Box.createHorizontalGlue());
		view2.add(v2p1);
		// the output type text box
		JPanel v2p2 = new JPanel();
		v2p2.setLayout(new BoxLayout(v2p2, BoxLayout.X_AXIS));
		_structOutTypeTxt = new JTextField();
		_structOutTypeTxt.setEditable(false);
		_structOutTypeTxt.setMaximumSize(new Dimension(w2, 25));
		_structOutTypeTxt.setPreferredSize(new Dimension(w2, 25));
		_structOutTypeTxt.setBackground(Color.white);
		v2p2.add(_structOutTypeTxt);
		v2p2.add(Box.createHorizontalGlue());
		view2.add(v2p2);
		// add the output type view
		pane.add(view2);

		// the input type view
		JPanel view3 = new JPanel();
		view3.setLayout(new BoxLayout(view3, BoxLayout.Y_AXIS));
		view3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// the input type label
		JPanel v3p1 = new JPanel();
		v3p1.setLayout(new BoxLayout(v3p1, BoxLayout.X_AXIS));
		v3p1.add(new JLabel("input", SwingConstants.LEFT));
		v3p1.add(Box.createHorizontalGlue());
		view3.add(v3p1);
		// the output type text box
		JPanel v3p2 = new JPanel();
		v3p2.setLayout(new BoxLayout(v3p2, BoxLayout.X_AXIS));
		_structInTypeTxt = new JTextField();
		_structInTypeTxt.setEditable(false);
		_structInTypeTxt.setMaximumSize(new Dimension(w3, 25));
		_structInTypeTxt.setPreferredSize(new Dimension(w3, 25));
		_structInTypeTxt.setBackground(Color.white);
		v3p2.add(_structInTypeTxt);
		v3p2.add(Box.createHorizontalGlue());
		view3.add(v3p2);
		// add the output type view
		pane.add(view3);

		pane.setMaximumSize(new Dimension(w1 + w2 + w3, 100));
		pane.setSize(new Dimension(w1 + w2 + w3, 100));
		return pane;
	}

	/**
	 * Create the semantic type display
	 */
	private JPanel _createSemTypeDisplay() {
		// the main pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
		pane.setBorder(_createTitledBorder("Semantic Types"));

		int w1 = 75; // the status text width
		int w2 = 325; // the output text width
		int w3 = 325; // the input text width
		int h = 125; // the height of input/output

		// the error message view
		JPanel view1 = new JPanel();
		view1.setLayout(new BoxLayout(view1, BoxLayout.Y_AXIS));
		view1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// the error message label
		JPanel v1p1 = new JPanel();
		v1p1.setLayout(new BoxLayout(v1p1, BoxLayout.X_AXIS));
		v1p1.add(new JLabel("channel status", SwingConstants.LEFT));
		v1p1.add(Box.createHorizontalGlue());
		view1.add(v1p1);
		// the error message text box
		JPanel v1p2 = new JPanel();
		v1p2.setLayout(new BoxLayout(v1p2, BoxLayout.X_AXIS));
		_semSafetyTxt = new JTextField();
		_semSafetyTxt.setEditable(false);
		_semSafetyTxt.setMaximumSize(new Dimension(w1, 25));
		_semSafetyTxt.setPreferredSize(new Dimension(w1, 25));
		_semSafetyTxt.setBackground(Color.white);
		v1p2.add(_semSafetyTxt);
		view1.add(v1p2);
		view1.add(Box.createVerticalGlue());
		// add the error message view
		pane.add(view1);

		// the output type view
		JPanel view2 = new JPanel();
		view2.setLayout(new BoxLayout(view2, BoxLayout.Y_AXIS));
		view2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// the output type label
		JPanel v2p1 = new JPanel();
		v2p1.setLayout(new BoxLayout(v2p1, BoxLayout.X_AXIS));
		v2p1.add(new JLabel("output", SwingConstants.LEFT));
		v2p1.add(Box.createHorizontalGlue());
		view2.add(v2p1);
		// the output type text box
		JPanel v2p2 = new JPanel();
		v2p2.setLayout(new BoxLayout(v2p2, BoxLayout.X_AXIS));
		_semOutTypeTbl = new JTable(new _SemTableModel());
		_semOutTypeTbl.setCellSelectionEnabled(false);
		_semOutTypeTbl.setBackground(Color.white);
		v2p2.add(new JScrollPane(_semOutTypeTbl));
		view2.add(v2p2);
		view2.setMaximumSize(new Dimension(w2, h));
		view2.setPreferredSize(new Dimension(w2, h));
		view2.add(Box.createVerticalGlue());
		// add the output type view
		pane.add(view2);

		// the input type view
		JPanel view3 = new JPanel();
		view3.setLayout(new BoxLayout(view3, BoxLayout.Y_AXIS));
		view3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// the input type label
		JPanel v3p1 = new JPanel();
		v3p1.setLayout(new BoxLayout(v3p1, BoxLayout.X_AXIS));
		v3p1.add(new JLabel("input", SwingConstants.LEFT));
		v3p1.add(Box.createHorizontalGlue());
		view3.add(v3p1);
		// the output type text box
		JPanel v3p2 = new JPanel();
		v3p2.setLayout(new BoxLayout(v3p2, BoxLayout.X_AXIS));
		_semInTypeTbl = new JTable(new _SemTableModel());
		_semInTypeTbl.setCellSelectionEnabled(false);
		_semInTypeTbl.setBackground(Color.white);
		v3p2.add(new JScrollPane(_semInTypeTbl));
		view3.add(v3p2);
		view3.setMaximumSize(new Dimension(w3, h));
		view3.setPreferredSize(new Dimension(w3, h));
		view3.add(Box.createVerticalGlue());
		// add the output type view
		pane.add(view3);

		pane.setMaximumSize(new Dimension(w1 + w2 + w3, h));
		pane.setPreferredSize(new Dimension(w1 + w2 + w3, h));

		return pane;
	}

	/**
	 * Create the bottom buttons
	 */
	private JPanel _createButtons() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(Box.createHorizontalGlue());
		pane.add(_adapterBtn);
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		pane.add(_closeBtn);

		// init buttons
		_adapterBtn.setMnemonic(KeyEvent.VK_A);
		_adapterBtn.setActionCommand("adapter");
		_adapterBtn.setToolTipText("Insert adapters for unsafe channels");
		_adapterBtn.addActionListener(_buttonListener);
		_adapterBtn.setEnabled(false);
		_closeBtn.setActionCommand("close");
		_closeBtn.setToolTipText("Close type checker");
		_closeBtn.addActionListener(_buttonListener);

		return pane;
	}

	/**
	 * Create a titled border with a blue font
	 */
	private TitledBorder _createTitledBorder(String title) {
		TitledBorder border = BorderFactory.createTitledBorder(" " + title
				+ " ");
		border.setTitleColor(new Color(0, 10, 230));
		return border;
	}

	/**
	 * Much of this code was taken from the ptolemy.actor.gui.UnitSolverDialog
	 */
	private void _initializeSelections() {
		if (!(_owner instanceof ActorGraphFrame))
			return;

		// get the tableau
		ActorGraphFrame tmpGraphFrame = (ActorGraphFrame) _owner;
		// Cast to a Tableau because KeplerGraphGraphTableau extends Tableau.
		// http://bugzilla.ecoinformatics.org/show_bug.cgi?id=2321
		// See http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4050
		Tableau tmpTableau = (Tableau) tmpGraphFrame.getTableau();
		// get the controller
		BasicGraphFrame tmpParent = (BasicGraphFrame) tmpTableau.getFrame();
		JGraph tmpJGraph = tmpParent.getJGraph();
		GraphPane tmpGraphPane = tmpJGraph.getGraphPane();
		_controller = (GraphController) tmpGraphPane.getGraphController();
		// get the selection model
		_selectionModel = _controller.getSelectionModel();
		// get the interactor
		Interactor tmpInteractor = _controller.getEdgeController(new Object())
				.getEdgeInteractor();
		// get the graph model
		_graphModel = (GraphModel) _controller.getGraphModel();
		// get the selection interactor
		_selectionInteractor = (SelectionInteractor) tmpInteractor;
		// get the default renderer
		_defaultSelectionRenderer = _selectionInteractor.getSelectionRenderer();
		// create the new edge highlighter
		_edgeSelectionRenderer = new BasicSelectionRenderer(
				new BasicEdgeHighlighter());
	}

	/**
	 * anonymous class to handle button events
	 */
	private ActionListener _buttonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("adapter")) {
				_insertAdapters();
			} else if (e.getActionCommand().equals("close")) {
				_unHighlightEdges();
				dispose();
			}
		}
	};

	/**
	 * @return The channnels as rows in the table that are either semantically
	 *         or structurally unsafe
	 */
	private _ChannelTableModel _getUnsafeChannels() {
		_ChannelTableModel model = _getChannels();
		_ChannelTableModel unsafeModel = new _ChannelTableModel();

		for (int i = 0; i < model.getRowCount(); i++) {
			_IOPortWrapper outPort = (_IOPortWrapper) model.getValueAt(i, 0);
			_IOPortWrapper inPort = (_IOPortWrapper) model.getValueAt(i, 1);
			IOPort output = outPort.getIOPort();
			IOPort input = inPort.getIOPort();
			if (_unsafeStructTypes(output, input)
					|| _unsafeSemTypes(output, input))
				unsafeModel.insertRow(outPort, inPort);
		}

		return unsafeModel;
	}

	/**
	 * @return The channels as rows in the table that are either semantically or
	 *         structurally unkown, but not not unsafe
	 */
	private _ChannelTableModel _getUnknownChannels() {
		_ChannelTableModel model = _getChannels();
		_ChannelTableModel unknownModel = new _ChannelTableModel();

		for (int i = 0; i < model.getRowCount(); i++) {
			_IOPortWrapper outPort = (_IOPortWrapper) model.getValueAt(i, 0);
			_IOPortWrapper inPort = (_IOPortWrapper) model.getValueAt(i, 1);
			IOPort output = outPort.getIOPort();
			IOPort input = inPort.getIOPort();
			if (_unknownStructTypes(output, input)
					&& !_unsafeSemTypes(output, input))
				unknownModel.insertRow(outPort, inPort);
			if (!_unsafeStructTypes(output, input)
					&& _unknownSemTypes(output, input))
				unknownModel.insertRow(outPort, inPort);
		}

		return unknownModel;
	}

	/**
     *
     */
	private boolean _unsafeSemTypes(IOPort output, IOPort input) {
		Vector outputTypes = _getSemtypesForPort(output);
		Vector inputTypes = _getSemtypesForPort(input);
		return (SMSServices.compare(outputTypes, inputTypes) == SMSServices.INCOMPATIBLE);
	}

	/**
     *
     */
	private boolean _unknownSemTypes(IOPort output, IOPort input) {
		Vector outputTypes = _getSemtypesForPort(output);
		Vector inputTypes = _getSemtypesForPort(input);
		return (SMSServices.compare(outputTypes, inputTypes) == SMSServices.UNKNOWN);
	}

	/**
     *
     */
	private boolean _unsafeStructTypes(IOPort output, IOPort input) {
		if (!(output instanceof TypedIOPort) || !(input instanceof TypedIOPort))
			return false;
		ptolemy.data.type.Type outType = ((TypedIOPort) output).getType();
		ptolemy.data.type.Type inType = ((TypedIOPort) input).getType();

		if (outType == null || inType == null)
			return false;

		if (outType.equals(BaseType.UNKNOWN) || inType.equals(BaseType.UNKNOWN))
			return false;

		return !(inType.isCompatible(outType));
	}

	/**
     *
     */
	private boolean _unknownStructTypes(IOPort output, IOPort input) {
		if (!(output instanceof TypedIOPort) || !(input instanceof TypedIOPort))
			return true;

		ptolemy.data.type.Type outType = ((TypedIOPort) output).getType();
		ptolemy.data.type.Type inType = ((TypedIOPort) input).getType();

		if (outType == null || inType == null)
			return true;

		if (outType.equals(BaseType.UNKNOWN) || inType.equals(BaseType.UNKNOWN))
			return true;

		return false;
	}

	/**
     *
     */
	private Vector _getSemtypesForPort(IOPort port) {
		Vector results = new Vector();
		for (Iterator iter = port.attributeList().iterator(); iter.hasNext();) {
			Object att = iter.next();
			if (att instanceof SemanticType) {
				SemanticType st = (SemanticType) att;
				results.add(st);
			}
		}
		return results;
	}

	/**
	 * @return An array of out/in port pairs that each define a channel
	 */
	private _ChannelTableModel _getChannels() {
		_ChannelTableModel model = new _ChannelTableModel();

		if (_entity == null || !(_entity instanceof CompositeEntity)) {
			return model;
		}

		CompositeEntity wf = (CompositeEntity) _entity;

		// get all the relations of the workflow and store them in a
		// vector consisting of one (output ports, input ports) pair
		// for each row
		Vector rels = new Vector();
		for (Iterator relations = wf.relationList().iterator(); relations
				.hasNext();) {
			ComponentRelation relation = (ComponentRelation) relations.next();
			Object[] rel = new Object[2];
			Vector inputs = new Vector();
			Vector outputs = new Vector();
			for (Iterator ports = relation.linkedPortList().iterator(); ports
					.hasNext();) {
				Port p = (Port) ports.next();
				if (p instanceof IOPort) {
					IOPort iop = (IOPort) p;
					if (iop.isInput() && !inputs.contains(iop))
						inputs.add(iop);
					if (iop.isOutput() && !outputs.contains(iop))
						outputs.add(iop);
				}
			}
			rel[0] = outputs;
			rel[1] = inputs;
			rels.add(rel);
		}
		// do a cross product for each relation to obtain a list of
		// binary connection channels
		for (Iterator iter = rels.iterator(); iter.hasNext();) {
			Object[] rel = (Object[]) iter.next();
			Vector outputs = (Vector) rel[0];
			Vector inputs = (Vector) rel[1];
			for (Iterator iter2 = outputs.iterator(); iter2.hasNext();) {
				IOPort out = (IOPort) iter2.next();
				for (Iterator iter3 = inputs.iterator(); iter3.hasNext();) {
					IOPort in = (IOPort) iter3.next();
					model.insertRow(new _IOPortWrapper(out),
							new _IOPortWrapper(in));
				}
			}
		}
		return model;
	}

	/**
	 * Highlights the connection associated with a row in the table, when that
	 * row is selected. Un-Highlights rows when deselected.
	 */
	private void _adjustUnsafeHighlightedConnections(ListSelectionModel lsm) {
		_adapterBtn.setEnabled(true);
		_unknownTable.getSelectionModel().clearSelection();
		_ChannelTableModel model = (_ChannelTableModel) _unsafeTable.getModel();

		_clearTypeDisplays();

		int row = lsm.getMinSelectionIndex();
		if (row == -1)
			return;

		_IOPortWrapper outPort = (_IOPortWrapper) model.getValueAt(row, 0);
		_IOPortWrapper inPort = (_IOPortWrapper) model.getValueAt(row, 1);
		IOPort output = outPort.getIOPort();
		IOPort input = inPort.getIOPort();

		_adjustChannelTypeStatus(output, input);

		if (_entity == null || !(_entity instanceof CompositeEntity))
			return;

		CompositeEntity wf = (CompositeEntity) _entity;

		// get the relations having output / input
		Vector relations = new Vector();
		for (Iterator iter = wf.relationList().iterator(); iter.hasNext();) {
			Relation rel = (Relation) iter.next();
			java.util.List ports = rel.linkedPortList();
			if (ports.contains(output) && ports.contains(input))
				relations.add(rel);
		}

		_highlightEdges(relations, output, input);
	}

	/**
	 * Highlights the connection associated with a row in the table, when that
	 * row is selected. Un-Highlights rows when deselected.
	 */
	private void _adjustUnknownHighlightedConnections(ListSelectionModel lsm) {
		_adapterBtn.setEnabled(false);
		_unsafeTable.getSelectionModel().clearSelection();
		_ChannelTableModel model = (_ChannelTableModel) _unknownTable
				.getModel();

		_clearTypeDisplays();

		int row = lsm.getMinSelectionIndex();
		if (row == -1)
			return;

		_IOPortWrapper outPort = (_IOPortWrapper) model.getValueAt(row, 0);
		_IOPortWrapper inPort = (_IOPortWrapper) model.getValueAt(row, 1);
		IOPort output = outPort.getIOPort();
		IOPort input = inPort.getIOPort();

		_adjustChannelTypeStatus(output, input);

		if (_entity == null || !(_entity instanceof CompositeEntity))
			return;

		CompositeEntity wf = (CompositeEntity) _entity;

		// get the relations having output / input
		Vector relations = new Vector();
		for (Iterator iter = wf.relationList().iterator(); iter.hasNext();) {
			Relation rel = (Relation) iter.next();
			java.util.List ports = rel.linkedPortList();
			if (ports.contains(output) && ports.contains(input))
				relations.add(rel);
		}

		_highlightEdges(relations, output, input);
	}

	/**
	 * Clears any highlighted selections, and returns the default selection
	 * renderer
	 */
	private void _unHighlightEdges() {
		if (!(_owner instanceof ActorGraphFrame))
			return;

		// clear all the current selection(s)
		_selectionModel.clearSelection();

		// reset the edge highlighter
		_selectionInteractor.setSelectionRenderer(_defaultSelectionRenderer);

		for (Iterator nodes = _graphModel.nodes(_entity); nodes.hasNext();) {
			Location node = (Location) nodes.next();
			Iterator edges = GraphUtilities.partiallyContainedEdges(node,
					_graphModel);
			while (edges.hasNext()) {
				Object edge = edges.next();
				Figure figure = _controller.getFigure(edge);
				if (_selectionModel.containsSelection(figure))
					_selectionModel.addSelection(figure);
			}
		}
		// clear all the current selection(s)
		_selectionModel.clearSelection();
	}

	/**
	 * Highlights the links between the given output and input port
	 * 
	 * @param output
	 *            The output port
	 * @param input
	 *            The input port
	 * @param relations
	 *            The relations connecting output to input
	 */
	private void _highlightEdges(Vector relations, IOPort output, IOPort input) {
		if (!(_owner instanceof ActorGraphFrame))
			return;

		_unHighlightEdges();

		// set edge highlighter
		_selectionInteractor.setSelectionRenderer(_edgeSelectionRenderer);

		for (Iterator nodes = _graphModel.nodes(_entity); nodes.hasNext();) {
			Location node = (Location) nodes.next();
			Iterator edges = GraphUtilities.partiallyContainedEdges(node,
					_graphModel);
			while (edges.hasNext()) {
				Object edge = edges.next();
				if (edge instanceof Link) {
					Link link = (Link) edge;
					ComponentRelation rel = link.getRelation();
					Object head = link.getHead();
					Object tail = link.getTail();
					if (relations.contains(rel) && output.equals(head)) {
						Figure figure = _controller.getFigure(link);
						_selectionModel.addSelection(figure);
					}
					if (relations.contains(rel) && input.equals(head)) {
						Figure figure = _controller.getFigure(link);
						_selectionModel.addSelection(figure);
					}
					if (link.getHead().equals(output)
							&& link.getTail().equals(input)) {
						Figure figure = _controller.getFigure(link);
						_selectionModel.addSelection(figure);
					}
				}
			}
		}
	}

	/**
     *
     */
	private void _highlightEdges(Vector relations) {
		if (!(_owner instanceof ActorGraphFrame))
			return;

		_unHighlightEdges();

		// set edge highlighter
		_selectionInteractor.setSelectionRenderer(_edgeSelectionRenderer);

		for (Iterator rels = relations.iterator(); rels.hasNext();) {
			Relation rel = (Relation) rels.next();
			for (Iterator nodes = _graphModel.nodes(_entity); nodes.hasNext();) {
				Location node = (Location) nodes.next();
				Iterator edges = GraphUtilities.partiallyContainedEdges(node,
						_graphModel);
				while (edges.hasNext()) {
					Object edge = edges.next();
					System.out.println("EDGE = " + edge);
					Object relation = _graphModel.getSemanticObject(edge);
					if (rel.equals(relation)) {
						Figure figure = _controller.getFigure(edge);
						_selectionModel.addSelection(figure);
					}
				}
			}
		}
	}

	/**
     *
     */
	private void _adjustChannelTypeStatus(IOPort output, IOPort input) {
		Color redBg = new Color(255, 215, 215);
		Color yelBg = new Color(255, 255, 215);
		Color grnBg = new Color(225, 255, 225);

		// display the structural types
		if (output instanceof TypedIOPort)
			_structOutTypeTxt.setText("" + ((TypedIOPort) output).getType());
		if (input instanceof TypedIOPort)
			_structInTypeTxt.setText("" + ((TypedIOPort) input).getType());

		// display the semantic types
		_SemTableModel outmodel = new _SemTableModel();
		for (Iterator iter = _getSemtypesForPort(output).iterator(); iter
				.hasNext();)
			outmodel.insertRow((SemanticType) iter.next());
		_semOutTypeTbl.setModel(outmodel);
		_SemTableModel inmodel = new _SemTableModel();
		for (Iterator iter = _getSemtypesForPort(input).iterator(); iter
				.hasNext();)
			inmodel.insertRow((SemanticType) iter.next());
		_semInTypeTbl.setModel(inmodel);

		if (_unsafeStructTypes(output, input)) {
			_structSafetyTxt.setText(_ERROR);
			_structSafetyTxt.setBackground(redBg);
		} else if (_unknownStructTypes(output, input)) {
			_structSafetyTxt.setText(_UNKNOWN);
			_structSafetyTxt.setBackground(yelBg);
		} else {
			_structSafetyTxt.setText(_SAFE);
			_structSafetyTxt.setBackground(grnBg);
		}

		if (_unsafeSemTypes(output, input)) {
			_semSafetyTxt.setText(_ERROR);
			_semSafetyTxt.setBackground(redBg);
		} else if (_unknownSemTypes(output, input)) {
			_semSafetyTxt.setText(_UNKNOWN);
			_semSafetyTxt.setBackground(yelBg);
		} else {
			_semSafetyTxt.setText(_SAFE);
			_semSafetyTxt.setBackground(grnBg);
		}

	}

	/**
     * 
     */
	private void _clearTypeDisplays() {
		_structSafetyTxt.setText("");
		_structOutTypeTxt.setText("");
		_structInTypeTxt.setText("");
		_semSafetyTxt.setText("");
		_SemTableModel model = new _SemTableModel();
		_semOutTypeTbl.setModel(model);
		_semInTypeTbl.setModel(model);
	}

	/**
	 * insert adapters into workflow ...
	 */
	private void _insertAdapters() {
		_adapterBtn.setEnabled(false);

        // create a search task, add listener and run it
		int row = _unsafeTable.getSelectionModel().getMinSelectionIndex();
    	if (row == -1)
			return;
    	_ChannelTableModel model = (_ChannelTableModel) _getUnsafeChannels();

		_IOPortWrapper outPort = (_IOPortWrapper) model.getValueAt(row, 0);
		_IOPortWrapper inPort = (_IOPortWrapper) model.getValueAt(row, 1);
		IOPort output = outPort.getIOPort();
		IOPort input = inPort.getIOPort();
		
        _SearchTask task = new _SearchTask(new Vector(),SMSServices.getPortSemanticTypes(output),SMSServices.getPortSemanticTypes(input));
        task.addListener(this);
        Thread thread = new Thread(task);
        thread.start();
	}
	
	/**
     * Callback for thread
     */
    private void _searchCompletedAction(_SearchTask task) {
    	_adapterBtn.setEnabled(true);
    	_displayAdapterResults(task.getResults());
    }

	
    /**
     * A private inner class to execute query as a separate thread.
     */
    private class _SearchTask implements Runnable {
        private Vector _actorTypes;
        private Vector _inputTypes;
        private Vector _outputTypes;
        private Vector _results;
        private Vector _listeners = new Vector();

        /** constructor */
        public _SearchTask(Vector actorTypes, Vector inputTypes,
                           Vector outputTypes) {
            _actorTypes = actorTypes;
            _inputTypes = inputTypes;
            _outputTypes = outputTypes;
        }

        /** to notify dialog when results are obtained */
        public void addListener(WorkflowTypeCheckerDialog obj) {
            _listeners.add(obj);
        }

        /** executes the query */
        public void run() {
            _results = _doSearch(_actorTypes, _inputTypes, _outputTypes);
            for (Iterator iter = _listeners.iterator(); iter.hasNext();) {
            	WorkflowTypeCheckerDialog dialog = (WorkflowTypeCheckerDialog) iter
                    .next();
                dialog._searchCompletedAction(this);
            }
        }

        /** retrieve the results */
        public Vector getResults() {
            return _results;
        }

    }; // _SearchTask

    /**
     * 
     */
    private synchronized Vector _doSearch(Vector searchActorTypes,
                                          Vector searchInTypes, Vector searchOutTypes) {
        Vector result = new Vector();

        Vector objects = _getObjectsToSearch();

        // check if there is a match; we know there is at least one
        // semantic type, which is checked in _doSearch() above
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            boolean compatible = true;
            Entity entity = (Entity) iter.next();

            if (searchActorTypes.size() > 0 && compatible) {
                Vector entityActorTypes = SMSServices
                    .getActorSemanticTypes(entity);
                if (SMSServices.compare(entityActorTypes, searchActorTypes) != SMSServices.COMPATIBLE)
                    compatible = false;
            }

            if (searchInTypes.size() > 0 && compatible) {
                // iterate through the semantic types while still compatible
                for (Iterator types = searchInTypes.iterator(); types.hasNext()
                         && compatible;) {
                    boolean found = false;
                    Vector searchInType = new Vector();
                    searchInType.add(types.next());
                    // iterator through ports until we find a match
                    for (Iterator ports = entity.portList().iterator(); ports
                             .hasNext()
                             && !found;) {
                        IOPort port = (IOPort) ports.next();
                        Vector entityInTypes = SMSServices
                            .getPortSemanticTypes(port);
                        if (port.isInput() && SMSServices.compare(searchInType, entityInTypes) == SMSServices.COMPATIBLE)
                            found = true;
                    }
                    // if we didn't find a match for a semtype then we're not
                    // compatible
                    if (!found)
                        compatible = false;
                }
            }

            if (searchOutTypes.size() > 0 && compatible) {
                // iterate through the semantic types while still compatible
                for (Iterator types = searchOutTypes.iterator(); types
                         .hasNext()
                         && compatible;) {
                    boolean found = false;
                    Vector searchOutType = new Vector();
                    searchOutType.add(types.next());
                    // iterator through ports until we find a match
                    for (Iterator ports = entity.portList().iterator(); ports
                             .hasNext()
                             && !found;) {
                        IOPort port = (IOPort) ports.next();
                        Vector entityOutTypes = SMSServices
                            .getPortSemanticTypes(port);
                        if (port.isOutput() && SMSServices.compare(entityOutTypes, searchOutType) == SMSServices.COMPATIBLE)
                            found = true;
                    }
                    // if we didn't find a match for a semtype then we're not
                    // compatible
                    if (!found)
                        compatible = false;
                }
            }

            if (compatible)
                result.add(entity);
        }

        return result;
    }

	/**
	 * 
	 */
	private Vector _getObjectsToSearch() {
		Vector result = new Vector();
		try {
        	CacheManager manager = CacheManager.getInstance();
        	Vector<KeplerLSID> cachedLsids = manager.getCachedLsids();
            for (KeplerLSID lsid : cachedLsids) {
				ActorCacheObject aco = (ActorCacheObject) manager
						.getObject(lsid);
				ActorMetadata am = aco.getMetadata();
        GraphicalActorMetadata gam = new GraphicalActorMetadata(am);
				NamedObj obj = gam.getActorAsNamedObj(null);
				// relax to just named obj here?
				if (obj instanceof Entity)
					result.add(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private void _displayAdapterResults(Vector adapters) {
		// the dialog for viewing results
		final JDialog dialog = new JDialog();
		dialog.setTitle("Search Results");

		// overall panel for the dialog
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// set up the button
		JButton closeBtn = new JButton("Close");
		closeBtn.setActionCommand("close");
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("close"))
					dialog.dispose();
			}
		});

		// label
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.add(new JLabel("Suggeted Components (drag to canvas):"));
		panel1.add(Box.createHorizontalGlue());

		// close button
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
		panel2.add(Box.createHorizontalGlue());
		panel2.add(closeBtn);

		// add everything to the main panel
		panel.add(panel1);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(new JScrollPane(_buildResultTree(adapters)));
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(panel2);

		// add main panel to the dialog and show it
		dialog.add(panel);
		dialog.pack();
		dialog.show();
	}

	/**
	 * Construct the visual tree widget showing a list of results.
	 */
	private PTree _buildResultTree(Vector adapters) {
		EntityLibrary root = new EntityLibrary();
		Workspace workspace = root.workspace();
		EntityTreeModel model = new VisibleTreeModel(root);

		Iterator compIter = adapters.iterator();
		while (compIter.hasNext()) {
			try {
				NamedObj entity = (NamedObj) compIter.next();
				// add to the tree
				NamedObj obj = (NamedObj) entity.clone(workspace);
				if (obj instanceof ComponentEntity)
					((ComponentEntity) obj).setContainer(root);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		PTree resultTree = new PTree(model);
		resultTree.setRootVisible(false);
		resultTree.setMinimumSize(new Dimension(120, 100));
		resultTree.setMaximumSize(new Dimension(120, 100));

		return resultTree;
	}

	/**
	 * inner class for managing channel tables
	 */
	private class _SemTableModel extends AbstractTableModel {
		// private members
		private String[] tableHeader = { "Ontology", "Class" }; // two columns
		private Vector tableData = new Vector(); // vector of arrays

		/** get the column count */
		public int getColumnCount() {
			return 2;
		}

		/** get the row cound */
		public int getRowCount() {
			return tableData.size();
		}

		/** get the column name */
		public String getColumnName(int columnIndex) {
			return tableHeader[columnIndex];
		}

		/** get the value of a cell */
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (!_validIndex(rowIndex, columnIndex))
				return null;
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			return row[columnIndex];
		}

		/** nothing is editable */
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		/** sets the value in the column */
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (!_validIndex(rowIndex, columnIndex))
				return;
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			row[columnIndex] = aValue;
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		/** Insert a new named ontology class into the model */
		public void insertRow(SemanticType semtype) {
			Object[] row = new Object[2];
			OntologyCatalog _catalog = OntologyCatalog.instance();
			NamedOntClass cls = _catalog.getNamedOntClass(semtype);
			if (cls != null) {
				row[0] = cls.getOntologyName();
				row[1] = cls.getName();
			} else {
				String[] clsStr = semtype.getExpression().split("#");
				if (clsStr.length == 1) {
					row[1] = clsStr[0];
				} else if (clsStr.length > 1) {
					row[0] = clsStr[0];
					row[1] = clsStr[1];
				}
			}
			tableData.add(row);
			fireTableChanged(new TableModelEvent(this));
		}

		/** @return True if the given row/column index is valid */
		private boolean _validIndex(int rowIndex, int columnIndex) {
			if (rowIndex < 0 || rowIndex >= getRowCount())
				return false;
			if (columnIndex < 0 || columnIndex >= getColumnCount())
				return false;
			return true;
		}
	}; // _SemTableModel

	/**
	 * Inner class for wrapping a port to view in a table.
	 */
	private class _IOPortWrapper {
		/** create a wrapper for the given io port */
		public _IOPortWrapper(IOPort port) {
			_port = port;
		}

		/** return the encapsulated port as actor.port name */
		public String toString() {
			String portStr = "";
			String actorStr = "";
			if (_port != null)
				portStr = _port.getName();
			if (_port.getContainer() != null)
				actorStr = _port.getContainer().getName();
			if (portStr.split(" ").length > 1)
				portStr = "'" + portStr + "'";
			if (actorStr.split(" ").length > 1)
				actorStr = "'" + actorStr + "'";
			return actorStr + "." + portStr;
		}

		/** @return The encapsulated port */
		public IOPort getIOPort() {
			return _port;
		}

		/** */
		public boolean equals(Object obj) {
			if (!(obj instanceof _IOPortWrapper))
				return false;
			_IOPortWrapper p = (_IOPortWrapper) obj;
			if (this.getIOPort() == null)
				return false;
			return this.getIOPort().equals(p.getIOPort());
		}

		/** PRIVATE MEMBER */
		private IOPort _port;
	}

	/**
	 * inner class for managing channel tables
	 */
	private class _ChannelTableModel extends AbstractTableModel {
		/** get the column count */
		public int getColumnCount() {
			return 2;
		}

		/** get the row cound */
		public int getRowCount() {
			return tableData.size();
		}

		/** get the column name */
		public String getColumnName(int columnIndex) {
			return tableHeader[columnIndex];
		}

		/** get the value of a cell */
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (!_validIndex(rowIndex, columnIndex))
				return null;
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			return row[columnIndex];
		}

		/** nothing is editable */
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		/** sets the value in the column */
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (!_validIndex(rowIndex, columnIndex))
				return;
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			row[columnIndex] = aValue;
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		/** Insert a new named ontology class into the model */
		public void insertRow(_IOPortWrapper p1, _IOPortWrapper p2) {
			if (_contains(p1, p2))
				return;
			Object[] row = new Object[2];
			row[0] = p1;
			row[1] = p2;
			tableData.add(row);
			fireTableChanged(new TableModelEvent(this));
		}

		/** @return True if the channel already exists, false otherwise */
		public boolean _contains(_IOPortWrapper p1, _IOPortWrapper p2) {
			assert (p1 != null && p2 != null);
			for (Iterator rows = tableData.iterator(); rows.hasNext();) {
				Object[] row = (Object[]) rows.next();
				if (p1.equals(row[0]) && p2.equals(row[1]))
					return true;
			}
			return false;
		}

		/** @return True if the given row/column index is valid */
		private boolean _validIndex(int rowIndex, int columnIndex) {
			if (rowIndex < 0 || rowIndex >= getRowCount())
				return false;
			if (columnIndex < 0 || columnIndex >= getColumnCount())
				return false;
			return true;
		}

		/* private members */
		private String[] tableHeader = { "Output Port", "Input Port" }; // two
		// columns
		private Vector tableData = new Vector(); // vector of arrays

	}; // _ChannelTableModel

	/**
	 * Main method for testing the dialog.
	 * 
	 * @param args
	 *            the arguments to the program
	 */
	public static void main(String[] args) {
		try {
			// the workflow
			TypedCompositeActor swf = new TypedCompositeActor();

			// the two actors
			// CompositeActor a1 = new TypedCompositeActor(swf, "Actor1");
			// CompositeActor a2 = new TypedCompositeActor(swf, "Actor2");
			TypedAtomicActor a1 = new TypedAtomicActor(swf, "Actor1");
			TypedAtomicActor a2 = new TypedAtomicActor(swf, "Actor2");

			// a port with <unknown, error> data/semantic type
			IOPort p1_a1 = new TypedIOPort(a1, "p1", false, true);
			IOPort p1_a2 = new TypedIOPort(a2, "p1", true, false);
			IORelation r1 = new TypedIORelation(swf, "r1");
			p1_a1.link(r1);
			p1_a2.link(r1);
			SemanticType s1_p1_a1 = new SemanticType(p1_a1, "semType0");
			s1_p1_a1
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#BiomassMeasurement");
			SemanticType s1_p1_a2 = new SemanticType(p1_a2, "semType0");
			s1_p1_a2
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#Biomass");

			// a port with <error, unknown> data/semantic type
			TypedIOPort p2_a1 = new TypedIOPort(a1, "p2", false, true);
			TypedIOPort p2_a2 = new TypedIOPort(a2, "p2", true, false);
			p2_a1.setTypeEquals(new ArrayType(BaseType.DOUBLE));
			p2_a2.setTypeEquals(BaseType.DOUBLE);
			IORelation r2 = new TypedIORelation(swf, "r2");
			p2_a1.link(r2);
			p2_a2.link(r2);

			// a port with <safe, safe> data/semantic type
			TypedIOPort p3_a1 = new TypedIOPort(a1, "p3", false, true);
			TypedIOPort p3_a2 = new TypedIOPort(a2, "p3", true, false);
			p3_a1.setTypeEquals(new ArrayType(BaseType.DOUBLE));
			p3_a2.setTypeEquals(BaseType.GENERAL);
			IORelation r3 = new TypedIORelation(swf, "r3");
			p3_a1.link(r3);
			p3_a2.link(r3);
			SemanticType s2_a1 = new SemanticType(p3_a1, "semType0");
			s2_a1
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#SpeciesBiomassMeasurement");
			SemanticType s2_a2 = new SemanticType(p3_a2, "semType0");
			s2_a2
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#BiomassMeasurement");

			// a relation with multiple channels (p4 error, error with p5)
			TypedIOPort p4_a1 = new TypedIOPort(a1, "p4", false, true);
			TypedIOPort p5_a1 = new TypedIOPort(a1, "p5", false, true);
			TypedIOPort p4_a2 = new TypedIOPort(a2, "p4", true, false);
			TypedIOPort p5_a2 = new TypedIOPort(a2, "p5", true, false);
			p4_a1.setTypeEquals(new ArrayType(BaseType.DOUBLE));
			p5_a1.setTypeEquals(BaseType.STRING);
			p4_a2.setTypeEquals(new ArrayType(BaseType.DOUBLE));
			p5_a2.setTypeEquals(new ArrayType(BaseType.INT));
			IORelation r4 = new TypedIORelation(swf, "r4");
			p4_a1.link(r4);
			p4_a2.link(r4);
			p5_a1.link(r4);
			p5_a2.link(r4);
			SemanticType s3_a1 = new SemanticType(p4_a1, "semType0");
			s3_a1
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#CoverArea");
			SemanticType s3_a2 = new SemanticType(p4_a2, "semType0");
			s3_a2
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#Biomass");
			SemanticType s4_a1 = new SemanticType(p5_a1, "semType0");
			s4_a1
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#Species");
			SemanticType s4_a2 = new SemanticType(p5_a2, "semType0");
			s4_a2
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#Species");

			// what do we do with multiple semantic types? (There has to be a
			// "cross-wise" match)
			// a port with <safe, safe> data/semantic type
			TypedIOPort p6_a1 = new TypedIOPort(a1, "p6", false, true);
			TypedIOPort p6_a2 = new TypedIOPort(a2, "p6", true, false);
			// p6_a1.setTypeEquals(new ArrayType(BaseType.DOUBLE));
			// p3_a2.setTypeEquals(BaseType.GENERAL);
			IORelation r5 = new TypedIORelation(swf, "r5");
			p6_a1.link(r5);
			p6_a2.link(r5);
			SemanticType s5_a1 = new SemanticType(p6_a1, "semType0");
			s5_a1
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#SpeciesBiomassMeasurement");
			SemanticType s5_a2 = new SemanticType(p6_a2, "semType0");
			s5_a2
					.setExpression("urn:lsid:lsid.ecoinformatics.org:onto:3:1#BiomassMeasurement");

			// windows look and feel
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			WorkflowTypeCheckerDialog checker = new WorkflowTypeCheckerDialog(
					null, swf);
			checker.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* PRIVATE MEMBERS */

	private static String _SAFE = "safe"; // const for safe channel
	private static String _ERROR = "error"; // const for unsafe channel
	private static String _UNKNOWN = "unknown"; // const for unknown channel

	private JPanel _pane = null; // overall pane for the dialog
	private JTable _table = null; // the ontology, class table

	private JTable _unsafeTable; // table for unsafe channels
	private JTable _unknownTable; // table for unkown channels
	private JButton _adapterBtn = // inserting adapters
	new JButton("   Insert Adapters   ");
	private JButton _closeBtn = new JButton("Close"); // button to close dialog
	private JTextField _structSafetyTxt; // displays error, safe, unknown
	private JTextField _semSafetyTxt; // displays error, safe, unknown
	private JTextField _structOutTypeTxt; // displays output type
	private JTextField _structInTypeTxt; // displays input type
	private JTable _semOutTypeTbl; // displays output type
	private JTable _semInTypeTbl; // displays input type

	private Frame _owner; // the owner of this dialog
	private Entity _entity; // the entity calling the dialog
	private OntologyCatalog _catalog; // holds the ontologies

	private SemanticTypeBrowserDialog _browser; // the browser

	private SelectionModel _selectionModel; // diva selection model
	private SelectionInteractor _selectionInteractor; // diva selection
	// interactor
	private GraphModel _graphModel; // diva graph model
	private GraphController _controller; // diva selection controller
	private SelectionRenderer _defaultSelectionRenderer; // diva default
	// selection
	// renderer
	private SelectionRenderer _edgeSelectionRenderer; // for highlighted edges

} // WorkflowTypeCheckerDialog