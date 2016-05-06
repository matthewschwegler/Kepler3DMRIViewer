/*
 *    $RCSfile$
 *
 *     $Author: crawl $
 *       $Date: 2012-11-28 09:22:15 -0800 (Wed, 28 Nov 2012) $
 *   $Revision: 31147 $
 *
 *  For Details: http://kepler-project.org
 *
 * Copyright (c) 2007 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package org.kepler.monitor;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.monitor.MonitoredStatus.State;
import org.kepler.monitor.figure.TrafficLightFigure;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.ExecutionListener;
import ptolemy.actor.IOPort;
import ptolemy.actor.IOPortEvent;
import ptolemy.actor.IOPortEventListener;
import ptolemy.actor.Initializable;
import ptolemy.actor.Manager;
import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFactory;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.SingletonAttribute;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.vergil.actor.ActorController;
import ptolemy.vergil.actor.ActorGraphFrame;
import ptolemy.vergil.icon.EditorIcon;
import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.graph.GraphController;
import diva.graph.GraphPane;
import diva.graph.GraphViewEvent;
import diva.graph.GraphViewListener;
import diva.graph.JGraph;

/**
 * The monitor manager.
 * <p>
 * An instance of this attribute can be inserted in a workflow to dynamically
 * associate activity icons to the existing entities (
 * {@link ptolemy.kernel.ComponentEntity}).
 * 
 * <p>
 * Demo workflows are under $KEPLER/workflows/test/monitor/
 * 
 * <p>
 * Note: Configuration is very limited in this preliminary version.
 * 
 * @author Carlos Rueda
 * @version $Id: MonitorManager.java 31147 2012-11-28 17:22:15Z crawl $
 */
public class MonitorManager extends SingletonAttribute implements Initializable {

	/**
	 * The icon type for all created monitor attributes
	 */
	public StringParameter iconType;

	/** Delay for the timer */
	public StringParameter timerDelay;

	/**
	 * Add monitor attributes for input ports?
	 */
	public Parameter addInputPortCounters;

	/**
	 * Add monitor attributes for output ports?
	 */
	public Parameter addOutputPortCounters;

	/**
	 * Creates a monitor manager.
	 * 
	 * @see ptolemy.kernel.util.SingletonAttribute#SingletonAttribute(NamedObj,
	 *      String)
	 */
	public MonitorManager(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		// set up my own icon and figure:
		_managerIcon = new MonitorIcon(this, "_icon");
		_managerIcon.setText("MonitorManager");
		_managerFigure = new TrafficLightFigure(1, new Rectangle2D.Double(3, 8,
				7, 7));
		_managerIcon.setFigure(_managerFigure);

		// Parameters:
		iconType = new StringParameter(this, "iconType");
		String none = ICON_TYPE_NONE;
		String[] iconTypes = MonitorAttribute.iconTypes();
		iconType.setExpression(none);
		iconType.addChoice(none);
		for (int i = 0; i < iconTypes.length; i++) {
			// don't show the COUNTER style:
			if (!iconTypes[i].equals(MonitorAttribute.COUNTER)) {
				iconType.addChoice(iconTypes[i]);
			}
		}
		iconType.setDisplayName("Monitor icon type for entities");

		timerDelay = new StringParameter(this, "timerDelay");
		timerDelay.setExpression("1000");
		timerDelay.setDisplayName("Timer delay for created indicators (ms)");

		addInputPortCounters = new Parameter(this, "addInputPortCounters");
		addInputPortCounters.setTypeEquals(BaseType.BOOLEAN);
		addInputPortCounters.setExpression("false");
		addInputPortCounters.setDisplayName("Add counters for input ports?");

		addOutputPortCounters = new Parameter(this, "addOutputPortCounters");
		addOutputPortCounters.setTypeEquals(BaseType.BOOLEAN);
		addOutputPortCounters.setExpression("false");
		addOutputPortCounters.setDisplayName("Add counters for output ports?");

		if (isDebugging) {
			log.debug("<" + getFullName() + "> container=" + container
					+ "  name=" + name);
		}
		
        // Hide the name.
        SingletonParameter hideName = new SingletonParameter(this, "_hideName");
        hideName.setToken(BooleanToken.TRUE);
        hideName.setVisibility(Settable.EXPERT);
	}

	/**
	 * Sets the icon type and timer delay according to the changed attribute.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == iconType) {
			String iconTypeValue = iconType.stringValue();
			if (iconTypeValue.equals(ICON_TYPE_NONE)
					|| Arrays.asList(MonitorAttribute.iconTypes()).contains(
							iconTypeValue)) {
				_iconType = iconTypeValue;
			} else {
				throw new IllegalActionException("Unexpected iconType: "
						+ iconTypeValue);
			}
		} else if (attribute == timerDelay) {
			try {
				_timerDelay = Integer.parseInt(timerDelay.stringValue());
			} catch (NumberFormatException e) {
				throw new IllegalActionException("NumberFormatException: " + e);
			}
		} else if (attribute == addInputPortCounters) {
			Token token = addInputPortCounters.getToken();
			boolean newValue = ((BooleanToken) token).booleanValue();
			if (newValue != _addInputPortCounters) {
				_addInputPortCounters = newValue;
			}
		} else if (attribute == addOutputPortCounters) {
			Token token = addOutputPortCounters.getToken();
			boolean newValue = ((BooleanToken) token).booleanValue();
			if (newValue != _addOutputPortCounters) {
				_addOutputPortCounters = newValue;
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Finishes the monitors.
	 */
	public void wrapup() throws IllegalActionException {
		if (isDebugging) {
			log.debug("<" + getFullName() + "> #wrapup");
		}
		Manager manager = ((CompositeActor) getContainer()).getManager();
		if (manager != null) {
			manager.removeExecutionListener(_executionListener);
		}
		_executionFinished();
	}

	/**
	 * If container is null, it finishes monitoring
	 */
	public void setContainer(NamedObj container) throws IllegalActionException,
			NameDuplicationException {

		if (isDebugging) {
			log.debug("<" + getFullName() + "> new container=" + container);
		}

		NamedObj previousContainer = getContainer();
		super.setContainer(container);

		if (previousContainer != null) {
			if (previousContainer instanceof Initializable) {
				((Initializable) previousContainer).removeInitializable(this);
			}
		}

		if (container != null) {
			// Try to add this as an initialible object to the toplevel
			// container:

			NamedObj toplevel = toplevel();

			if (toplevel instanceof Initializable) {
				// make sure we don't add ourselves multiple times:
				((Initializable) toplevel).removeInitializable(this);
				((Initializable) toplevel).addInitializable(this);
				if (isDebugging) {
					log.debug("<" + getFullName()
							+ "> added initializable to toplevel=" + toplevel);
				}
			}
		} else {
			_executionFinished();
			_removeMIcons();
		}
	}

	/**
	 * Creates the MI attributes for the existing component entities in the
	 * container and activates the MI icons.
	 */
	public void initialize() throws IllegalActionException {
		if (isDebugging) {
			log.debug("<" + getFullName() + "> #initialize()");
		}

		_getGraphController();

		_initialize();

		Manager manager = ((CompositeActor) getContainer()).getManager();
		if (manager != null) {
			manager.addExecutionListener(_executionListener);
		}

		for (MonitoredEntity item : _monitoredEntities.values()) {
			item.startFigureUpdater();
		}

		_managerFigure.setState(State.RUNNING);

		if (isDebugging && _graphController != null) {
			for (MonitoredEntity item : _monitoredEntities.values()) {
				item._showPortLocations(_graphController);
			}
		}

		_registerGraphViewListener(true);

	}

	/** Does nothing. */
	public void addInitializable(Initializable initializable) {
	}

	/** Does nothing. */
	public void preinitialize() throws IllegalActionException {
	}

	/** Does nothing. */
	public void removeInitializable(Initializable initializable) {
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * Calls _relocate(item) when a monitored entity changes its location, so
	 * the corresponding MI attribute is relocated accordingly.
	 * 
	 * <p>
	 * Note: it seems nodeMoved(e) is never called, so the action is done in
	 * nodeDrawn(e).
	 */
	private class MyGraphViewListener implements GraphViewListener {
		public void edgeDrawn(GraphViewEvent e) {
			// Ignore.
		}

		public void edgeRouted(GraphViewEvent e) {
			// Ignore.
		}

		public void nodeDrawn(GraphViewEvent e) {
			Object source = e.getSource();
			Object target = e.getTarget();
			if (source instanceof ActorController && target instanceof Location) {
				Location loc = (Location) target;
				NamedObj container = loc.getContainer();

				if (container instanceof ComponentEntity) {
					ComponentEntity entity = (ComponentEntity) container;
					MonitoredEntity item = _monitoredEntities.get(entity
							.getName());
					if (item != null) {
						for (MonitorAttribute attr : item.attrs) {
							_relocate(item, attr);
						}
					}
				}
			}
		}

		public void nodeMoved(GraphViewEvent e) {
			// NOTE haven't seen this method being called -- why?
			log.debug("nodeMoved e=" + e);
		}
	}

	private void _getGraphController() {
		Effigy effigy = Configuration.findEffigy(this.toplevel());
		if (effigy == null) {
			if (isDebugging) {
				log.debug("<" + getFullName() + "> No top effigy");
			}
			return;
		}
		effigy = effigy.topEffigy();
		TableauFactory tf = effigy.getTableauFactory();
		if (isDebugging) {
			log.debug("<" + getFullName() + "> top effigy class = "
					+ effigy.getClass() + "\n" + "tableau factory = " + tf);
		}
		try {
			Tableau tableau = tf.createTableau(effigy);
			JFrame frame = tableau.getFrame();

			if (frame instanceof ActorGraphFrame) {
				ActorGraphFrame agf = (ActorGraphFrame) frame;
				JGraph jgraph = agf.getJGraph();
				GraphPane graphPane = jgraph.getGraphPane();
				_graphController = (GraphController) graphPane
						.getGraphController();
			}
		} catch (Exception e) {
			log.debug("error in _getGraphController", e);
		}
	}

	/**
	 * Registers/Unregisters our GraphViewListener so we can react to changes in
	 * location of entities.
	 * 
	 * @param register
	 *            true to call controller.addGraphViewListener(gvl), false to
	 *            call controller.removeGraphViewListener(gvl), where controller
	 *            is the GraphController obtained through the top effigy
	 *            associated with this.toplevel().
	 * 
	 * @return true iff the {add|remove}GraphViewListener call was done.
	 */
	private boolean _registerGraphViewListener(boolean register) {
		Effigy effigy = Configuration.findEffigy(this.toplevel());
		if (effigy == null) {
			if (isDebugging) {
				log.debug("<" + getFullName() + "> No top effigy");
			}
			return false;
		}
		effigy = effigy.topEffigy();
		TableauFactory tf = effigy.getTableauFactory();
		if (isDebugging) {
			log.debug("<" + getFullName() + "> top effigy class = "
					+ effigy.getClass() + "\n" + "tableau factory = " + tf);
		}
		try {
			Tableau tableau = tf.createTableau(effigy);
			JFrame frame = tableau.getFrame();

			if (frame instanceof ActorGraphFrame) {
				ActorGraphFrame agf = (ActorGraphFrame) frame;
				JGraph jgraph = agf.getJGraph();
				GraphPane graphPane = jgraph.getGraphPane();
				_graphController = (GraphController) graphPane
						.getGraphController();

				if (register) {
					_graphController.addGraphViewListener(gvl);
					log
							.debug("<" + getFullName()
									+ "> GraphViewListener added");
				} else {
					_graphController.removeGraphViewListener(gvl);
					log.debug("<" + getFullName()
							+ "> GraphViewListener removed");
				}
				return true;
			}
		} catch (Exception e) {
			log.debug("<" + getFullName()
					+ "> error in _registerGraphViewListener", e);
		}
		return false;
	}

	/**
	 * Creates and initialize the monitors to the entities in my container.
	 * 
	 * @throws IllegalActionException
	 */
	private/* synchronized */void _initialize() throws IllegalActionException {
		if (getContainer() == null) {
			log.debug("<" + getFullName() + "> NO container!");
			return;
		}

		boolean wasDeferringChangeRequests = isDeferringChangeRequests();

		if (isDebugging) {
			log.debug("<" + getFullName() + "> isDeferringChangeRequests = "
					+ wasDeferringChangeRequests);
		}

		if (wasDeferringChangeRequests) {
			setDeferringChangeRequests(false);
		}

		List entityList = ((CompositeEntity) getContainer())
				.entityList(ComponentEntity.class);
		for (Iterator it = entityList.iterator(); it.hasNext();) {
			ComponentEntity entity = (ComponentEntity) it.next();
			_monitor(entity);
		}

		setDeferringChangeRequests(wasDeferringChangeRequests);

		for (MonitoredEntity item : _monitoredEntities.values()) {
			item.update();
		}
	}

	/**
	 * Creates the monitors for a given entity.
	 * 
	 * @param entity
	 */
	private void _monitor(final ComponentEntity entity) {
		// We need the icon to get the location of the entity
		Attribute attr = entity.getAttribute("_icon");
		if (attr == null) {
			log.error(" Unexpected: No attribute by name '_icon' in entity "
					+ entity);
			return;
		}
		if (!(attr instanceof EditorIcon)) {
			log
					.error(" Unexpected: '_icon' attribute is not an EditorIcon in entity "
							+ entity);
			return;
		}

		log.debug("<" + getFullName() + "> _icon attribute class = "
				+ attr.getClass().getName());

		// If the entity already uses a MonitorIcon, then do nothing:
		if (attr instanceof MonitorIcon) {
			log.debug("<" + getFullName()
					+ "> Entity already uses a MonitorIcon.");
			return;
		}

		// first, create a MonitoredEntity for the entity.
		// This will be initialized with only the associated MonitoredStatus
		// object:
		MonitoredEntity item = new MonitoredEntity(entity);
		_monitoredEntities.put(entity.getName(), item);

		// now, create and associate a visual attribute for the entity as a
		// whole:
		if (!ICON_TYPE_NONE.equalsIgnoreCase(_iconType)) {
			_addMonitorAttributeForEntity(item);
		}

		// now, create and associate visual attributes for the ports:
		List ports = item.entity.portList();
		for (Object obj : ports) {
			Port port = (Port) obj;
			if (port instanceof IOPort) {
				IOPort ioport = (IOPort) port;
				if ((ioport.isInput() && _addInputPortCounters)
						|| (ioport.isOutput() && _addOutputPortCounters)) {
					_addMonitorAttributeForPort(item, ioport);
				}
			}
		}
	}

	/**
	 * Adds a new MI attribute with the given name.
	 */
	private void _addMonitorAttributeForEntity(final MonitoredEntity item) {
		final String name = _getMonitorAttributeNameForEntity(item.entity);

		double[] loc = _getEntityAttributeLocation(item, null);
		double x = loc[0], y = loc[1];

		String moml = _getMonitorAttributeMoml(name, x, y, _iconType,
				_timerDelay);

		ChangeRequest request = new MoMLChangeRequest(this, getContainer(),
				moml) {
			protected void _execute() throws Exception {
				try {
					super._execute();

					// ok, we now have our MonitorAttribute:
					MonitorAttribute attr = (MonitorAttribute) getContainer()
							.getAttribute(name);

					// associate the MonitorAttribute:
					item.addMonitorAttribute(attr);

					attr.setMonitoredStatus(item.status);

					FigureUpdater figUpdater = new FigureUpdater();
					figUpdater.setFigure(attr.getIcon().getFigure());

					attr.setFigureUpdater(figUpdater);

					// and start the figure updater:
					figUpdater.start();

					item.status.addPropertyChangeListener(figUpdater);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		if (isDebugging) {
			log.debug("<" + getFullName()
					+ "> Request to add monitor with name " + name);
		}
		request.setPersistent(false);
		requestChange(request);
	}

	/**
	 * Adds a new MI attribute with the given name.
	 */
	private void _addMonitorAttributeForPort(final MonitoredEntity item,
			final IOPort ioport) {
		final String name = _getMonitorAttributeNameForPort(item.entity, ioport);

		double[] loc = _getPortAttributeLocation(item, ioport, null);

		if (loc == null) {
			// but we need the location; so, cannot add monitor:
			return;
		}

		double x = loc[0], y = loc[1];

		String moml = _getMonitorAttributeMoml(name, x, y,
				MonitorAttribute.COUNTER, _timerDelay);

		ChangeRequest request = new MoMLChangeRequest(this, getContainer(),
				moml) {
			protected void _execute() throws Exception {
				try {
					super._execute();

					// ok, we now have our MonitorAttribute:
					MonitorAttribute attr = (MonitorAttribute) getContainer()
							.getAttribute(name);

					// associate the ioport with the attribute:
					attr.setPort(ioport);

					// associate the MonitorAttribute:
					item.addMonitorAttribute(attr);

					attr.setMonitoredStatus(item.status);

					FigureUpdater figUpdater = new FigureUpdater();
					figUpdater.setFigure(attr.getIcon().getFigure());

					attr.setFigureUpdater(figUpdater);

					// set property for the updates
					String portName = ioport.getName();
					String propName = "portEvent:" + portName;
					figUpdater.setPropertyNameForLabel(propName);

					// and start the figure updater:
					figUpdater.start();

					item.status.addPropertyChangeListener(figUpdater);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		if (isDebugging) {
			log.debug("<" + getFullName()
					+ "> Request to add monitor with name " + name);
		}
		request.setPersistent(false);
		requestChange(request);
	}

	/**
	 * Gets the location for an entity's monitor attribute.
	 */
	private double[] _getEntityAttributeLocation(final MonitoredEntity item,
			double[] loc) {
		if (loc == null) {
			loc = new double[2];
		}

		Location entityLocation = (Location) item.entity
				.getAttribute("_location");
		loc[0] = entityLocation.getLocation()[0];
		loc[1] = entityLocation.getLocation()[1];

		loc[0] += 12;
		loc[1] += 6 + item.entityBounds.getY() + item.entityBounds.getHeight();

		return loc;
	}

	/**
	 * Gets the location for a port's monitor attribute.
	 */
	private double[] _getPortAttributeLocation(final MonitoredEntity item,
			final IOPort ioport, double[] loc) {
		boolean ok = false;
		double x, y;
		Location portLocation = (Location) ioport.getAttribute("_location");
		if (portLocation == null) {
			Location entityLocation = (Location) item.entity
					.getAttribute("_location");
			x = entityLocation.getLocation()[0];
			y = entityLocation.getLocation()[1];
			Figure portFigure = _graphController.getFigure(ioport);
			if (portFigure == null) {

			} else {
				Rectangle2D portBounds = portFigure.getBounds();
				if (portBounds == null) {

				} else {
					if (isDebugging) {
						log.debug("<" + getFullName() + "> " + ioport.getName()
								+ " no _location.  portBounds=" + portBounds);
					}
					x += portBounds.getX();
					y += portBounds.getY();
					ok = true;
				}
			}
		} else {
			x = portLocation.getLocation()[0];
			y = portLocation.getLocation()[1];
			ok = true;
			if (isDebugging) {
				log.debug("<" + getFullName() + "> " + ioport.getName()
						+ " port location: " + portLocation);
			}
		}

		if (ok) {
			x += (ioport.isInput() ? -8 : +12);
			y -= 6;

			if (loc == null) {
				loc = new double[2];
			}
			loc[0] = x;
			loc[1] = y;
			return loc;
		} else {
			return null;
		}
	}

	/**
	 * Relocates the MI attribute according to the location of its monitored
	 * object.
	 */
	private void _relocate(final MonitoredEntity item,
			final MonitorAttribute attr) {
		Location attrLocation = (Location) attr.getAttribute("_location");
		double[] attrLoc = attrLocation.getLocation();

		double[] newAttrLoc;
		IOPort ioport = attr.getPort();
		if (ioport == null) {
			newAttrLoc = _getEntityAttributeLocation(item, null);
		} else {
			newAttrLoc = _getPortAttributeLocation(item, ioport, null);
		}

		if (newAttrLoc == null) {
			return;
		}

		if (attrLoc[0] == newAttrLoc[0] && attrLoc[1] == newAttrLoc[1]) {
			return;
		}

		final double[] dLoc = newAttrLoc;
		ChangeRequest request = new ChangeRequest(this, "update location") {
			protected void _execute() throws Exception {
				Location newLoc;
				try {
					newLoc = new Location(attr, "_location");
					newLoc.setLocation(dLoc);
				} catch (IllegalActionException e) {
					log.debug("<" + getFullName() + "> error setting location",
							e);
				} catch (NameDuplicationException e) {
					log.debug("<" + getFullName() + "> error setting location",
							e);
				}
			}
		};
		if (isDebugging) {
			log.debug("<" + getFullName() + "> MI relocation for entity "
					+ item.entity);
		}
		request.setPersistent(false);
		requestChange(request);
	}

	/** Unregisters the GraphViewListener and removes all MI attributes */
	private/* synchronized */void _executionFinished() {
		if (isDebugging) {
			log.debug("<" + getFullName() + "> #_executionFinished()");
		}

		try {
			_registerGraphViewListener(false);

			for (MonitoredEntity item : _monitoredEntities.values()) {
				item.stopFigureUpdater();
			}
			_removeMIcons();
		} finally {
			_managerFigure.setState(State.IDLE);
		}
	}

	/**
	 * Removes all MI attributes from the container.
	 */
	private/* synchronized */void _removeMIcons() {
		final ChangeRequest request = new ChangeRequest(this,
				"remove all monitor icons") {
			protected void _execute() throws Exception {
				for (MonitoredEntity item : _monitoredEntities.values()) {
					item.status.getPropertyTimer().stop();
					item.releaseIOPortEventListeners();
					for (MonitorAttribute attr : item.attrs) {
						_removeProperty(attr.getName());
					}
				}
				_monitoredEntities.clear();

			}
		};
		if (isDebugging) {
			log.debug("<" + getFullName() + "> Calling requestChange");
		}
		request.setPersistent(false);
		requestChange(request);
	}

	/**
	 * Gets the name for the MI attribute associated with an entity.
	 */
	private static String _getMonitorAttributeNameForEntity(
			ComponentEntity entity) {
		String entityName = entity.getName();
		return entityName + "__monIcon";
	}

	/**
	 * Gets the name for the MI attribute associated with an entity's port.
	 */
	private static String _getMonitorAttributeNameForPort(
			ComponentEntity entity, IOPort ioport) {
		String entityName = entity.getName();
		return entityName + "_PORT_" + ioport.getName() + "__monIcon";
	}

	/** Removes a property with the given name */
	private void _removeProperty(final String attrName) {
		if (isDebugging) {
			log.debug("<" + getFullName() + "> removing property with name "
					+ attrName + " ...");
		}
		String moml = "<deleteProperty name=\"" + attrName + "\"/>";
		ChangeRequest request = new MoMLChangeRequest(this, getContainer(),
				moml) {
			protected void _execute() throws Exception {
				try {
					super._execute();
				} catch (Exception e) {
					log.debug("<" + getFullName()
							+ "> exception executing MoMLChangeRequest", e);
				}
			}
		};
		request.setPersistent(false);
		requestChange(request);
	}

	/**
	 * Monitored entity. Association (ComponentEntity, MonitoredStatus,
	 * MonitorAttribute*).
	 */
	private static class MonitoredEntity {
		ComponentEntity entity;
		Rectangle2D entityBounds;
		MonitoredStatus status;

		/** monitor attributes associated to the entity. */
		List<MonitorAttribute> attrs = new ArrayList<MonitorAttribute>();

		// just to be able to remove them from the ports:
		Map<String, IOPortEventListener> portEventListeners = new HashMap<String, IOPortEventListener>();

		/**
		 * Creates the MonitorStatus member for the given entity. Internal
		 * listeners are registered to update the properties in the status
		 * object according to activity in the entity.
		 */
		MonitoredEntity(ComponentEntity entity) {
			this.entity = entity;
			entityBounds = _getEntityBounds(entity);

			status = new MonitoredStatus();
			//entity.getAttribute(name)
			// the entity as a whole has a state
			status.setProperty(State.STATE, State.IDLE);

			status.getPropertyTimer().start();

			List ports = entity.portList();
			for (Object obj : ports) {
				Port port = (Port) obj;
				if (port instanceof IOPort) {
					IOPort ioport = (IOPort) port;
					String portName = ioport.getName();
					final String propName = "portEvent:" + portName;
					status.setProperty(propName, "0");

					IOPortEventListener portEventListener = new IOPortEventListener() {
						long counter = 0;
						
						public void portEvent(IOPortEvent event) {
							status.setProperty(State.STATE, State.RUNNING);
							
							/** Determine whether user specified a quality field*/
							String quality_field_value = null; 
							Parameter quality_field = (Parameter)event.getPort().getContainer().getAttribute("quality_field");
							
							/** If user specified a quality_field, get the value of the field to obtain which port to take
							 *  the quality score from */
							if (quality_field != null) {
								Token token = null;
                                try {
                                    token = quality_field.getToken();
                                } catch (IllegalActionException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                
                                if(token != null) {                                    
    								 if(token instanceof StringToken) {
    								     quality_field_value = ((StringToken)token).stringValue();
    								 } else {
    								     quality_field_value = token.toString();
    								 }
                                }
							}
							
							/** Initialize parameter attributes to determine
							 *  whether user specified quality fields for quality thresholds*/
							Parameter quality_high_param = (Parameter)event.getPort().getContainer().getAttribute("high_quality");
							Parameter quality_low_param = (Parameter)event.getPort().getContainer().getAttribute("low_quality");
							
							/** Initialize quality scores and thresholds*/
							Token quality_high_value;
							Token quality_low_value; 
							
							/** Check whether user specified a port in the quality field and verify
							 *  if the port the event is coming from is the port the user specified 
							 *  in the quality field*/ 
							if (quality_field_value != null &&
									event.getPort().getDisplayName().equals(quality_field_value)) {
								
								/** Check whether user specified fields for quality thresholds*/
								if (quality_high_param != null && quality_low_param != null) { 
									try {
										
										/** Retrieve quality threshold values */
										quality_high_value = quality_high_param.getToken();
										quality_low_value = quality_low_param.getToken();
										
										/** Store values in quality states*/
										status.setProperty(State.HIGH_QUALITY, quality_high_value);
										status.setProperty(State.LOW_QUALITY, quality_low_value);

									} catch (IllegalActionException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
									
								if(event.getToken() != null) {
									status.setProperty(State.QUALITY_SCORE, event.getToken());
								} else {
								    Token[] tokens = event.getTokenArray();
								    if(tokens != null) {
								        for(Token token : tokens) {
								            status.setProperty(State.QUALITY_SCORE, token);
								        }
								    }
								}
							}
							else {
								//status.setProperty(State.STATE, event.getToken());
								status.setProperty(State.QUALITY_SCORE, State.NO_QUALITY_SCORE);
							}
							
							int eventType = event.getEventType();
							if (eventType == IOPortEvent.GET_END
									|| eventType == IOPortEvent.SEND_END) {
								++counter;
								status.setProperty(propName, String
										.valueOf(counter));
							}
						}
					};
					portEventListeners.put(portName, portEventListener);

					ioport.addIOPortEventListener(portEventListener);
				}
			}
		}

		private void addMonitorAttribute(MonitorAttribute attr) {
			attrs.add(attr);
		}

		private void startFigureUpdater() {
			for (MonitorAttribute attr : attrs) {
				FigureUpdater figUpdater = attr.getFigureUpdater();
				if (figUpdater != null) {
					figUpdater.start();
				}
			}
		}

		private void stopFigureUpdater() {
			for (MonitorAttribute attr : attrs) {
				FigureUpdater figUpdater = attr.getFigureUpdater();
				if (figUpdater != null) {
					figUpdater.stop();
				}
			}
		}

		private void update() {
			for (MonitorAttribute attr : attrs) {
				FigureUpdater fu = attr.getFigureUpdater();
				if (fu != null) {
					fu.update();
				}
			}
		}

		/** Removes the registered port event listeners */
		void releaseIOPortEventListeners() {
			List ports = entity.portList();
			for (Object obj : ports) {
				Port port = (Port) obj;
				if (port instanceof IOPort) {
					IOPort ioport = (IOPort) port;
					String portName = ioport.getName();
					ioport.removeIOPortEventListener(portEventListeners
							.get(portName));
				}
			}
		}

		/** debug utility */
		void _showPortLocations(GraphController gc) {
			log.debug("ENTITY: " + entity);
			List ports = entity.portList();
			for (Object obj : ports) {
				Port port = (Port) obj;
				if (port instanceof IOPort) {
					IOPort ioport = (IOPort) port;
					String portName = ioport.getName();

					Location loc = (Location) ioport.getAttribute("_location");
					if (loc == null) {
						log.debug(portName + " no _location.  Attr list: "
								+ ioport.attributeList());

						// see ActorController._placePortFigures()
						Figure portFigure = gc.getFigure(port);
						if (portFigure == null) {
							log.debug("portFigure is null");
						} else {
							Shape shape = portFigure.getShape();
							if (shape == null) {
								log.debug("shape is null");
							} else {
								Rectangle2D portBounds = shape.getBounds2D();
								log.debug("portBounds=" + portBounds);
							}
						}
					} else {
						log.debug(portName + " port location: " + loc);
					}
				}
			}
		}
	}

	/** For now, intended to detect exceptions and then finish the handling */
	private ExecutionListener _executionListener = new ExecutionListener() {
		public void executionError(Manager manager, Throwable throwable) {
			_executionFinished();
		}

		public void executionFinished(Manager manager) {
			_executionFinished();
		}

		public void managerStateChanged(Manager manager) {
		}
	};

	/** @return the entity bounds. */
	private static Rectangle2D _getEntityBounds(ComponentEntity entity) {
		EditorIcon entityIcon = (EditorIcon) entity.getAttribute("_icon");
		CompositeFigure entityFigure = (CompositeFigure) entityIcon
				.createFigure();
		return entityFigure.getBounds();
	}

	private static String _getMonitorAttributeMoml(String name, double x,
			double y, String iconType, int timerDelay) {
		return "<property name=\"" + name + "\" " + "class=\""
				+ MonitorAttribute.class.getName() + "\">\n"
				+ "<property name=\"_location\" "
				+ "class=\"ptolemy.kernel.util.Location\" " + "value=\"[" + x
				+ ", " + y + "]\">\n" + "</property>\n"
				+ "<property name=\"iconType\" "
				+ "class=\"ptolemy.data.expr.StringParameter\" " + "value=\""
				+ iconType + "\">\n" + "</property>\n"
				+ "<property name=\"timerDelay\" "
				+ "class=\"ptolemy.data.expr.StringParameter\" " + "value=\""
				+ timerDelay + "\">\n" + "</property>\n"
				+ "<property name=\"_hideName\" "
				+ "class=\"ptolemy.data.expr.SingletonParameter\" "
				+ "value=\"true\">\n" + "</property>" + "\n" + "</property>";
	}

	/** My own icon */
	private MonitorIcon _managerIcon;

	/** My own figure */
	private TrafficLightFigure _managerFigure;

	private String _iconType;

	private int _timerDelay;

	/** Cache of the value of addInputPortCounters. */
	boolean _addInputPortCounters = false;

	/** Cache of the value of addOutputPortCounters. */
	boolean _addOutputPortCounters = false;

	/** Name -&gt; MEntity map of current monitored entities. */
	private Map<String, MonitoredEntity> _monitoredEntities = new HashMap<String, MonitoredEntity>();

	private GraphController _graphController;

	/** The GraphViewListener instance for this MI manager. */
	private final GraphViewListener gvl = new MyGraphViewListener();

	private static final String ICON_TYPE_NONE = "None";

	private static final Log log = LogFactory.getLog(MonitorManager.class);

	private static final boolean isDebugging = log.isDebugEnabled();

	static {
		// this helps in debugging the destruction of the created timers
		Log logTimers = LogFactory.getLog(MonitorManager.class.getPackage()
				.getName()
				+ ".logTimers");
		if (logTimers.isDebugEnabled()) {
			logTimers.debug("setting Timer.setLogTimers(true)");
			Timer.setLogTimers(true);
		}
	}

	private static final long serialVersionUID = 1L;
}
