/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.sms.actors;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.Vector;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypeEvent;
import ptolemy.actor.TypeListener;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.graph.InequalityTerm;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.vergil.actor.ActorInstanceController;
import ptolemy.vergil.basic.NamedObjController;
import ptolemy.vergil.basic.NodeControllerFactory;
import ptolemy.vergil.toolbox.FigureAction;
import ptolemy.vergil.toolbox.MenuActionFactory;
import diva.graph.GraphController;

/*

 OPERATIONS: 


 Vector       getActors()                            // the actors connected to merge actor
 Iterator     getTargetPorts(IOPort inputPort)       // target (output) i/o ports for inputPort
 Iterator     getInputPortMappings(IOPort port)      // get mappings for input port
 void         removeOutputPort(IOPort port)          // removes the given output port
 Vector       getActorPorts(NamedObj actor)          // output ports of source actor
 Iterator     getMappings()                          // all merge actor mappings
 void         setProductionRate()

 void         _computeTargetTypes()                  // sets target (output) types
 Vector       _getActorPortChannels(NamedObj actor)  // return the ports for the connected actor
 void         _applyMerge()                          // applies the merge mapping
 PortChannel  _getPortChannel(IOPort inputPort)      // input ports as port channels
 Iterator     _getInputPorts()                       // input i/o ports
 void         _initInputPorts()                      // reset input ports (and listeners)
 void         _removeMappings() 


 */

/**
 * This is a first-cut, dumbed-down, and simple version of a generic merge
 * function based on semantic annotations.
 */
public class MergeActor extends TypedAtomicActor implements TypeListener {

	public MergeInputPort mergeInputPort; // single multiport for inputs to be
											// merged

	/**
	 * Constructor
	 */
	public MergeActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		init();
	}

	/**
	 * Constructor
	 */
	public MergeActor(Workspace workspace) throws NameDuplicationException,
			IllegalActionException {
		super(workspace);
		init();
	}

	/**
	 * Initialize the actor
	 */
	private void init() throws IllegalActionException, NameDuplicationException {
		// add the input port
		mergeInputPort = new MergeInputPort(this, "input");

		// black, blue, cyan, darkgray, gray, green, lightgray, magenta,
		// orange, pink, red, white, yellow
		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"0\" y=\"0\" rx=\"10\" ry=\"10\" "
				+ "width=\"80\" height=\"40\" " + "style=\"fill:orange\"/>\n"
				+ "<text x=\"12.5\" y=\"25\" "
				+ "style=\"font-size:20; fill:darkgray; "
				+ "font-family:SansSerif\">" + "merge</text>\n" + "</svg>\n");

		// inPort.addTypeListener?

		// Create a node controller to control the context menu
		_nodeController = new MergeActorControllerFactory(this,
				"_controllerFactory");
	}

	public void typeChanged(TypeEvent event) {
		_computeTargetTypes();
	}

	public void preinitialize() throws IllegalActionException {
		super.preinitialize();
		_initInputPorts();
		_computeTargetTypes();
	}

	public void fire() throws IllegalActionException {
		super.fire();

		Iterator actors = getActors().iterator();
		while (actors.hasNext()) {
			NamedObj actor = (NamedObj) actors.next();
			_applyMerge(actor);
		}
	}

	public void computeMerge() throws IllegalActionException,
			NameDuplicationException {
		_removeMappings();
		_initInputPorts();

		SimpleComputeMergeAlgorithm alg = new SimpleComputeMergeAlgorithm(this);
		alg.computeMerge();

		// assign types to target outputs
		_computeTargetTypes();
		setProductionRate();

		// refresh the canvas
		ChangeRequest r = new EmptyChangeRequest(this, "update request");
		toplevel().requestChange(r);
	}

	public void computeMergeOld() throws IllegalActionException,
			NameDuplicationException {
		// check if any mappings already if so, warn that they will be
		// removed then remove them
		_removeMappings();
		_initInputPorts();

		// first step: add one output port per input port
		Iterator iter = _getInputPorts();
		for (int i = 0; iter.hasNext(); i++) {
			IOPort p = (IOPort) iter.next();
			String sourceActor = p.getContainer().getName();
			String sourcePort = p.getName();
			String targetPort = sourceActor + "_" + sourcePort;
			TypedIOPort output = new TypedIOPort(this, targetPort);
			output.setOutput(true);
			output.setInput(false);
			output.setMultiport(false);
			// create a mapping
			String mergeName = "merge_" + i;
			SimpleMergeMapping m = new SimpleMergeMapping(this, mergeName,
					sourceActor, sourcePort, targetPort);
		}// end for

		// assign types to target outputs
		_computeTargetTypes();
		setProductionRate();

		// refresh the canvas
		ChangeRequest r = new EmptyChangeRequest(this, "update request");
		toplevel().requestChange(r);
	}

	public void editMerge() throws IllegalActionException,
			NameDuplicationException {
		// open the dialog
		MergeEditorDialog editor = new MergeEditorDialog(null, this);
		editor.setVisible(true);
	}

	/**
     * 
     */
	public Iterator<SimpleMergeMapping> getInputPortMappings(IOPort port) {
		Vector<SimpleMergeMapping> results = new Vector<SimpleMergeMapping>();
		if (port == null)
			return results.iterator();
		Iterator<SimpleMergeMapping> iter = attributeList(SimpleMergeMapping.class).iterator();
		while (iter.hasNext()) {
			try {
				SimpleMergeMapping m = iter.next();
				String actorName = port.getContainer().getName();
				String portName = port.getName();
				if (actorName.equals(m.getSourceActor())) {
					if (portName.equals(m.getSourceActorPort()))
						results.add(m);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return results.iterator();
	}

	/**
     *
     */
	public Vector getMappings() {
		return new Vector(attributeList(SimpleMergeMapping.class));
	}

	/**
	 * removes the given output port
	 */
	public void removeOutputPort(IOPort port) {
		try {
			port.setContainer(null);
			// refresh the canvas
			ChangeRequest r = new EmptyChangeRequest(this, "update request");
			toplevel().requestChange(r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Determine the output types based on the types of input port.
	 */
	private void _computeTargetTypes() {
		Iterator iter = _getInputPorts();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			if (p instanceof TypedIOPort) {
				Iterator<IOPort> targets = getTargetPorts(p);
				while (targets.hasNext()) {
					TypedIOPort target = (TypedIOPort) targets.next();
					Type targetType = target.getType();
					Type inputType = ((TypedIOPort) p).getType();
					if (targetType.equals(BaseType.UNKNOWN)
							|| inputType.isCompatible(targetType))
						target.setTypeEquals(inputType);
				}
			}
		}// end while
	}

	/**
	 * A helper funtion that computes the port channel (port, channel) of the
	 * given port.
	 */
	private PortChannel _getPortChannel(IOPort inputPort) {
		Iterator channels = _getInputPorts();
		for (int channel = 0; channels.hasNext(); channel++) {
			IOPort p = (IOPort) channels.next();
			if (p == inputPort)
				return new PortChannel(inputPort, channel);
		}// end while
		return null;
	}

	/**
	 * Calculates the set of actors that are connected to this merge actor via
	 * one or more input ports.
	 * 
	 * @return An iterator containing actors connected to this merge actor.
	 */
	public Vector getActors() {
		Vector results = new Vector();
		Iterator iter = _getInputPorts();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			NamedObj o = p.getContainer();
			if (!results.contains(o))
				results.add(o);
		}// end while
		return results;
	}

	/**
	 * For a connected actor, applies the merge rules producing data tokens.
	 */
	private void _applyMerge(NamedObj actor) {
		Vector portChannels = _getActorPortChannels(actor);
		while (_actorHasTokens(portChannels)) {
			_passMergeTokens(portChannels);
			_passNonMergeTokens(portChannels);
		}
	}

	private void _passNonMergeTokens(Vector portChannels) {
		Vector inputActorPorts = new Vector();
		// get all the input ports from portChannels (i.e., ports for
		// current actor)
		Iterator iter = portChannels.iterator();
		while (iter.hasNext()) {
			PortChannel p = (PortChannel) iter.next();
			inputActorPorts.add(p.getPort());
		}
		// get all the target output ports for ports in inputActorPorts
		Vector<IOPort> targetOutputPorts = new Vector<IOPort>();
		iter = inputActorPorts.iterator();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			Iterator<IOPort> targets = getTargetPorts(p);
			while (targets.hasNext()) {
				IOPort target = targets.next();
				if (!targetOutputPorts.contains(target))
					targetOutputPorts.add(target);
			}
		}
		// get all the output ports not in targetOutputPorts
		Vector nonMergeOutputPorts = new Vector();
		iter = outputPortList().iterator();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			if (!targetOutputPorts.contains(p))
				nonMergeOutputPorts.add(p);
		}

		// send a "null" value to each of the nonMergeOutputPorts
		iter = nonMergeOutputPorts.iterator();
		while (iter.hasNext()) {
			TypedIOPort p = (TypedIOPort) iter.next();
			// get the type of the output port of p (FIXME:
			// _computeType should set output port to "largest" of
			// inputs that map to it)
			Token t = null;
			Type type = p.getType();
			Class cls = type.getTokenClass();
			try {
				// FIXME: SHOULD BE TO t.null(), BUT null() UNIMPLEMENTED
				t = (Token) cls.newInstance();
				p.send(0, t.zero());
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("SENT " + p.getName() + " TOKEN " + t);
		}
	}

	private void _passMergeTokens(Vector portChannels) {
		Iterator iter = portChannels.iterator();
		while (iter.hasNext()) {
			try {
				PortChannel p = (PortChannel) iter.next();
				IOPort in = p.getPort();
				int channel = p.getChannel();
				System.out.println("PASS MERGE TOKENS \n ... PORT: '"
						+ in.getName() + "' CHANNEL: '" + channel + "'");
				Token t = mergeInputPort.get(channel);
				Iterator<IOPort> ptargets = getTargetPorts(in);
				while (ptargets.hasNext()) {
					IOPort ptarget = ptargets.next();
					ptarget.send(0, t);
					System.out.println("SENT " + ptarget.getName() + " TOKEN "
							+ t + " FROM " + in.getName());
				}// end while
			} catch (IllegalActionException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Determines if there is a token on the port channels
	 */
	private boolean _actorHasTokens(Vector portChannels) {
		Iterator iter = portChannels.iterator();
		while (iter.hasNext()) {
			try {
				PortChannel p = (PortChannel) iter.next();
				if (!mergeInputPort.hasToken(p.getChannel()))
					return false;
			} catch (IllegalActionException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * Given the mappings, determines which ports of the given actor are to be
	 * used as well as their channel.
	 * 
	 * @return The given source actor's ports and channels as PortChannels
	 */
	private Vector _getActorPortChannels(NamedObj actor) {
		Vector results = new Vector();
		Iterator iter = _getInputPorts();
		for (int channel = 0; iter.hasNext(); channel++) {
			IOPort p = (IOPort) iter.next();
			if (actor.equals(p.getContainer()))
				results.add(new PortChannel(p, channel));
		}
		return results;
	}

	/**
	 * @return The output ports of actor connected to the merge actor
	 */
	public Vector<IOPort> getActorPorts(NamedObj actor) {
		Vector<IOPort> results = new Vector<IOPort>();
		Iterator iter = _getInputPorts();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			if (actor.equals(p.getContainer()))
				results.add(p);
		}
		return results;
	}

	public Iterator<IOPort> getTargetPorts(IOPort inPort) {
		Vector<IOPort> results = new Vector<IOPort>();
		String actorSource = inPort.getContainer().getName();
		String portName = inPort.getName();
		Iterator maps = attributeList(SimpleMergeMapping.class).iterator();
		while (maps.hasNext()) {
			SimpleMergeMapping m = (SimpleMergeMapping) maps.next();
			if (actorSource.equals(m.getSourceActor())
					&& portName.equals(m.getSourceActorPort())) {
				String outName = m.getTargetPort();
				Iterator ports = outputPortList().iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (outName.equals(p.getName()))
						results.add(p);
				}
			}
		}
		return results.iterator();
	}

	private void _initInputPorts() {
		// remove old listeners
		Iterator iter = _getInputPorts();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			if (p instanceof TypedIOPort)
				((TypedIOPort) p).removeTypeListener(this);
		}// end while

		// create new set of input ports
		_inputPorts = new Vector();
		iter = mergeInputPort.connectedPortList().iterator();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			_inputPorts.add(p);
			if (p instanceof TypedIOPort)
				((TypedIOPort) p).addTypeListener(this);
		}// end while
	}

	private Iterator _getInputPorts() {
		return mergeInputPort.connectedPortList().iterator();
	}

	private void _removeMappings() throws IllegalActionException,
			NameDuplicationException {
		// remove the mapping attributes
		Iterator iter = attributeList(SimpleMergeMapping.class).iterator();
		;
		while (iter.hasNext()) {
			SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
			m.setContainer(null);
		}
		// remove the output ports
		iter = outputPortList().iterator();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			p.setContainer(null);
		}
	}

	public void setProductionRate() {
		// the number of production tokens is num(actors)
		// production rate set on each output port
		Vector actors = getActors();
		int rate = actors.size();
		Iterator iter = outputPortList().iterator();
		while (iter.hasNext()) {
			try {
				IOPort p = (IOPort) iter.next();
				Parameter tpr = (Parameter) p
						.getAttribute("tokenProductionRate");
				if (tpr == null) {
					tpr = new Parameter(this, "tokenProductionRate",
							new IntToken(rate));
					tpr.setContainer(p);
				} else {
					tpr.setToken(new IntToken(rate));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void update() {
		// refresh the canvas
		ChangeRequest r = new EmptyChangeRequest(this, "update request");
		toplevel().requestChange(r);
	}

	//
	// Inner classes
	// 

	/**
	 * Specific input port
	 */
	class MergeInputPort extends TypedIOPort {

		/**
		 * constructor
		 */
		public MergeInputPort(ComponentEntity container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
			setInput(true);
			setOutput(false);
			setMultiport(true);
		}

		// not sure what needs to be overriden now
		public void link(Relation relation) throws IllegalActionException {
			super.link(relation);
		}

		/**
		 * Prevent types from being modified
		 */
		public Type getType() {
			return BaseType.GENERAL;
		}

		/**
		 * Sabotage type resolution ...
		 */
		public InequalityTerm getTypeTerm() {
			return new MergePortTypeTerm();
		}

		private class MergePortTypeTerm implements InequalityTerm {
			public Object getAssociatedObject() {
				return MergeInputPort.this;
			}

			public Object getValue() {
				return getType();
			}

			public InequalityTerm[] getVariables() {
				return (new InequalityTerm[0]);
			}

			public void initialize(Object type) throws IllegalActionException {
			}

			public boolean isSettable() {
				return false;
			}

			public boolean isValueAcceptable() {
				return true;
			}

			public void setValue(Object type) throws IllegalActionException {
			}
		}
	};

	/**
     * 
     */
	private class MergeActorControllerFactory extends NodeControllerFactory {
		private MergeActor _mergeActor;

		public MergeActorControllerFactory(MergeActor mergeActor, String name)
				throws NameDuplicationException, IllegalActionException {
			super(mergeActor, name);
			_mergeActor = mergeActor;
		}

		public NamedObjController create(GraphController controller) {
			return new MergeActorInstanceController(controller, _mergeActor);
		}
	};

	/**
	 * ...
	 */
	public class MergeActorInstanceController extends ActorInstanceController {
		public MergeActorInstanceController(GraphController controller,
				MergeActor mergeActor) {
			this(controller, FULL, mergeActor);
		}

		public MergeActorInstanceController(GraphController controller,
				Access access, MergeActor mergeActor) {
			super(controller, access);
			MenuActionFactory f1 = new MenuActionFactory(new MergeActorAction1(
					mergeActor));
			_menuFactory.addMenuItemFactory(f1);
			MenuActionFactory f2 = new MenuActionFactory(new MergeActorAction2(
					mergeActor));
			_menuFactory.addMenuItemFactory(f2);

		}
	}

	/**
	 * ...
	 */
	protected class MergeActorAction1 extends FigureAction {
		private MergeActor _mergeActor;

		public MergeActorAction1(MergeActor mergeActor) {
			super("Compute Merge");
			_mergeActor = mergeActor;
		}

		public void actionPerformed(ActionEvent ev) {
			try {
				_mergeActor.computeMerge();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * ...
	 */
	protected class MergeActorAction2 extends FigureAction {
		private MergeActor _mergeActor;

		public MergeActorAction2(MergeActor mergeActor) {
			super("Edit Merge Mappings");
			_mergeActor = mergeActor;
		}

		public void actionPerformed(ActionEvent ev) {
			try {
				_mergeActor.editMerge();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private class EmptyChangeRequest extends ChangeRequest {
		public EmptyChangeRequest(Object o, String s) {
			super(o, s);
		}

		public void _execute() {
			// do nothing
		}
	};

	/**
	 * A helper class that encapsulates a port and its channel
	 */
	private class PortChannel {
		public PortChannel(IOPort port, int channel) {
			_port = port;
			_channel = channel;
		}

		public IOPort getPort() {
			return _port;
		}

		public int getChannel() {
			return _channel;
		}

		private IOPort _port;
		private int _channel;
	};

	/**
	 * Overriden from NamedObj ...
	 */
	public String uniqueName(String prefix) {
		if (prefix == null)
			prefix = "null";
		prefix = _stripNumericSuffix(prefix);
		String candidate = prefix + "1";
		int uniqueNameIndex = 2;
		while (_hasName(candidate))
			candidate = prefix + uniqueNameIndex++;
		return candidate;
	}

	private boolean _hasName(String name) {
		Iterator iter = containedObjectsIterator();
		while (iter.hasNext()) {
			NamedObj obj = (NamedObj) iter.next();
			if (obj.getName().equals(name))
				return true;
		}
		return false;
	}

	// this needs to be a subclass of typed io port
	private Vector _inputPorts = new Vector(); // the set of ports connected to
												// the mergeInputPort
	private MergeActorControllerFactory _nodeController = null; // for updating
																// canvas
	private Vector _indexPorts = new Vector();

}