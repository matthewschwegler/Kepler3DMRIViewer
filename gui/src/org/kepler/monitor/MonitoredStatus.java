/*
 *    $RCSfile$
 *
 *     $Author: barseghian $
 *       $Date: 2012-08-09 16:50:32 -0700 (Thu, 09 Aug 2012) $
 *   $Revision: 30396 $
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A set of properties subject to be monitored.
 * 
 * <p>
 * Any object interesting in being notified about changes in these properties
 * should register itself as a {@link PropertyChangeListener}.
 * 
 * @author Carlos Rueda
 * @version $Id: MonitoredStatus.java 30396 2012-08-09 23:50:32Z barseghian $
 */
public class MonitoredStatus {

	/** Convenience attributes for a "state" property */
	public static class State {

		/** The name of the state property. */
		public static final String STATE = "state";

		/** Value for an "idle" state. */
		public static final Object IDLE = "IDLE";

		/** Value for a "waiting" state. */
		public static final Object WAITING = "WAITING";

		/** Value for a "running" state. */
		public static final Object RUNNING = "RUNNING";

		/** Value for an "error" state. */
		public static final Object ERROR = "ERROR";

		/** Names for quality properties*/
		public static final String QUALITY_SCORE = "qualityscore";
		
		public static final String HIGH_QUALITY = "highquality";

		public static final String LOW_QUALITY = "lowquality";
		
		public static final Object NO_QUALITY_SCORE = "NO_QUALITY_SCORE"; 
	}

	/**
	 * Gets the names of the properties of this monitored object.
	 * 
	 * @return the names
	 */
	public Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(props.keySet());
	}

	/**
	 * Sets the value of a monitored property. Registered listeners will be
	 * notified of this event.
	 * 
	 * @param propName
	 *            property name
	 * @param propValue
	 *            the value
	 */
	public void setProperty(String propName, Object propValue) {
		_lastUpdate = System.currentTimeMillis();
		Object oldValue = props.put(propName, propValue);
		
		_firePropertyChange(propName, oldValue, propValue);
		
		
	}
	
	/**
	 * Gets the current value of a monitored property.
	 * 
	 * @param propName
	 * @return the value
	 */
	public Object getProperty(String propName) {
		Object propValue = props.get(propName);
		return propValue;
	}

	/**
	 * Adds a property change listener to this monitored status.
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		_propertyChangeSupport.addPropertyChangeListener(listener);
	}

	/**
	 * Removes a property change listener from this monitored status.
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		_propertyChangeSupport.removePropertyChangeListener(listener);
	}

	/**
	 * Gets the property timer.
	 */
	public PropertyTimer getPropertyTimer() {
		if (_propertyTimer == null) {
			_propertyTimer = new PropertyTimer();
		}
		return _propertyTimer;
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * Calls
	 * <code>_propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue)</code>
	 * 
	 * @see java.beans.PropertyChangeSupport#firePropertyChange(String, Object,
	 *      Object)
	 */
	private final void _firePropertyChange(String propertyName,
			Object oldValue, Object newValue) {
		_propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}

	/** the property change support */
	private PropertyChangeSupport _propertyChangeSupport = new PropertyChangeSupport(
			this);

	/** Map propName-&gt;propvalue */
	private Map<String, Object> props = new HashMap<String, Object>();

	/** The last time {@link #setProperty(String, Object)} was called. */
	private long _lastUpdate = 0;

	/**
	 * For automatic update of a property in this status object. In handles an
	 * internal timer that calls setProperty(propName, propValue) if, whenever
	 * the timer is triggered, the current time minus the time when the
	 * setProperty method was last called is greater than the given delay.
	 */
	public class PropertyTimer {
		private int delay = 1000;
		private String propName = State.STATE;
		private Object propValue = State.WAITING;

		private final Timer _timer = new Timer(delay, new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				long current = System.currentTimeMillis();
				long curr_delay = current - _lastUpdate;
				if (curr_delay > delay) {
					setProperty(propName, propValue);
				}
			}
		});

		/** Sets the delay for the timer. By default, 1000ms */
		public void setDelay(int delay) {
			this.delay = delay;
			_timer.setDelay(delay);
			_timer.restart();
			if (isDebugging) {
				log.debug(this);
			}
		}

		/** Sets the property name for the timer. By default, {@link State#STATE} */
		public void setPropertyName(String propName) {
			this.propName = propName;
		}

		/**
		 * Sets the property value for the timer. By default,
		 * {@link State#WAITING}
		 */
		public void setPropertyValue(Object propValue) {
			this.propValue = propValue;
		}

		/** Starts the timer. */
		public void start() {
			_timer.start();
			if (isDebugging) {
				log.debug(this);
			}
		}

		/** Stops the timer. */
		public void stop() {
			_timer.stop();
			if (isDebugging) {
				log.debug(this);
			}
		}

		public String toString() {
			return "delay=" + delay + ", propName=" + propName + ", propValue="
					+ propValue;
		}
	}

	private PropertyTimer _propertyTimer;
	
	private static final Log log = LogFactory.getLog(MonitoredStatus.class);
	private static final boolean isDebugging = log.isDebugEnabled();

}
