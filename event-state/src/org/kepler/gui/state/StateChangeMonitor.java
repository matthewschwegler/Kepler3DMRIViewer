/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:37:59 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26433 $'
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

package org.kepler.gui.state;

import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Maintain a registry of objects that are interested in changes in application
 * state. When the application state changes (through posting a notification to
 * this class), distribute the StateChangeEvent to all of the registered
 * listeners. This class follows the singleton pattern because we never need or
 * want more than a single instance to manage all of the change events.
 * 
 * @author Matt Jones
 */
public class StateChangeMonitor {
	private static StateChangeMonitor monitor;
	private static final Object classLock = StateChangeMonitor.class;
	private Hashtable listeners = null;

	private static Log log = LogFactory.getLog(StateChangeMonitor.class);

	/**
	 * Creates a new instance of the StateChangeMonitor, and is private because
	 * this is a singleton.
	 */
	private StateChangeMonitor() {
		// Create the registry of StateChangeListeners
		listeners = new Hashtable();
	}

	/**
	 * Get the single instance of the StateChangeMonitor, creating it if needed.
	 * 
	 * @return the single instance of the StateChangeMonitor
	 */
	public static StateChangeMonitor getInstance() {
		synchronized (classLock) {
			if (monitor == null) {
				monitor = new StateChangeMonitor();
			}
			return monitor;
		}
	}

	/**
	 * This method is called by objects to register a listener for changes in
	 * the application state. Any change in the state will trigger notification.
	 * 
	 * @param stateChange
	 *            the name of the state change for which notifications should be
	 *            sent
	 * @param listener
	 *            a reference to the object to be notified of changes
	 */
	public void addStateChangeListener(String stateChange,
			StateChangeListener listener) {
		Vector currentStateListeners = null;
		if (!listeners.containsKey(stateChange)) {
			log.debug("Adding state vector: " + stateChange);
			currentStateListeners = new Vector();
			listeners.put(stateChange, currentStateListeners);
		} else {
			currentStateListeners = (Vector) listeners.get(stateChange);
		}

		if (!currentStateListeners.contains(listener)) {
			log.debug("Adding listener: " + listener.toString());
			currentStateListeners.addElement(listener);
		}
	}

	/**
	 * This method is called by objects to remove a listener for changes in the
	 * application state. Any change in the state will trigger notification.
	 * 
	 * @param stateChange
	 *            the name of the state change for which the listener should be
	 *            removed
	 * @param listener
	 *            a reference to the object to be removed
	 */
	public void removeStateChangeListener(String stateChange,
			StateChangeListener listener) {
		Vector currentStateListeners = null;
		if (listeners.containsKey(stateChange)) {
			currentStateListeners = (Vector) listeners.get(stateChange);
			log.debug("Removing listener: " + listener.toString());
			currentStateListeners.removeElement(listener);
			if (currentStateListeners.size() == 0) {
				listeners.remove(stateChange);
			}
		}
	}

	/**
	 * Notify the monitor of an application state change so that it in turn can
	 * notify all of the registered listeners of that state change.
	 * 
	 * @param event
	 *            the StateChangeEvent that has occurred
	 */
	public void notifyStateChange(StateChangeEvent event) {
		String stateChange = event.getChangedState();
		Vector currentStateListeners = null;
		if (listeners.containsKey(stateChange)) {
			currentStateListeners = (Vector) listeners.get(stateChange);
			for (int i = 0; i < currentStateListeners.size(); i++) {
				StateChangeListener listener = (StateChangeListener) currentStateListeners
						.elementAt(i);
				if (listener != null) {
					listener.handleStateChange(event);
				}
			}
		}
	}
}
