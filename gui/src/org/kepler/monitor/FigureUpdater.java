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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.monitor.MonitoredStatus.State;
import org.kepler.monitor.figure.BaseFigure;
import org.kepler.monitor.figure.ProgressBarFigure;
import org.kepler.monitor.figure.QualityFigure;
import org.kepler.monitor.figure.StateFigure;

import diva.canvas.Figure;
import diva.canvas.toolbox.LabelFigure;

/**
 * Updates a figure used for monitoring. The figure is updated as reaction to
 * property changes occuring in some monitored object.
 * 
 * @author Carlos Rueda
 * @version $Id: FigureUpdater.java 30396 2012-08-09 23:50:32Z barseghian $
 */
public class FigureUpdater implements PropertyChangeListener {

	/**
	 * Creates a figure updater. Use {@link #setFigure(Figure)} to associate a
	 * figure.
	 */
	public FigureUpdater() {
	}

	/**
	 * Sets the figure. Updates on the previous figure, if any, are stopped. If
	 * this updater was already started, then it continues updating the new
	 * figure.
	 * 
	 * @param fig
	 */
	public void setFigure(Figure fig) {
		boolean wasStarted = _started;
		stop();
		_figure = fig;
		if (wasStarted) {
			start();
		}
	}

	/**
	 * Gets the associated figure.
	 * 
	 * @return the figure
	 */
	public Figure getFigure() {
		return _figure;
	}

	/**
	 * 
	 * Sets the property name for the case of LabelFigure
	 * 
	 * @param propName
	 *            Name of property
	 */
	public void setPropertyNameForLabel(String propName) {
		_propNameForLabel = propName;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		Object newValue = evt.getNewValue();
		if (isDebugging) {
			log.debug(propName + " -> " + newValue);
		}
		
		if (_figure instanceof StateFigure) {
			StateFigure fig = (StateFigure) _figure;
			if (State.STATE.equals(propName)) {
				Object state = newValue;
				fig.setState(state);
				fig.update();
			}
		}
		else if (_figure instanceof QualityFigure) {
			QualityFigure fig = (QualityFigure) _figure;
			
			/** Check states and assign appropriate quality values*/
			if (State.HIGH_QUALITY.equals(propName)) {
				fig.setHighQualityThreshold(newValue);
			}
			else if (State.LOW_QUALITY.equals(propName)) {
				fig.setLowQualityThreshold(newValue);	
			}
			
			/** If state being passed is quality score, then update quality figure*/
			else if (State.QUALITY_SCORE.equals(propName)) {
				fig.update2(newValue);
			}
		}
		else if (_figure instanceof ProgressBarFigure) {
			ProgressBarFigure fig = (ProgressBarFigure) _figure;

			// TODO: the current value for the bar should be taken from the
			// property?
			// ..

			fig.update();
		} else if (_figure instanceof LabelFigure) {
			if (isDebugging) {
				log.debug("updating LabelFigure: _propNameForLabel="
						+ _propNameForLabel);
			}
			LabelFigure fig = (LabelFigure) _figure;
			if (propName.equals(_propNameForLabel)) {
				fig.setString(String.valueOf(newValue));
			}
		}
	}

	/**
	 * Starts the dynamic behavior of the figure.
	 */
	public void start() {
		_started = true;
		if (_figure instanceof ProgressBarFigure) {
			((ProgressBarFigure) _figure).setStarted(_started);
		}
	}

	/**
	 * Stops the dynamic behavior of the figure.
	 */
	public void stop() {
		_started = false;
		if (_figure instanceof ProgressBarFigure) {
			((ProgressBarFigure) _figure).setStarted(_started);
		}
	}

	/**
	 * Tells if the figure is started.
	 */
	public boolean isStarted() {
		return _started;
	}

	/**
	 * Updates the figure.
	 */
	public void update() {
	    if (_figure instanceof BaseFigure) {
			((BaseFigure) _figure).update();
		}
		
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** The figure under control */
	private Figure _figure;

	/** True iff the updater is started. */
	private volatile boolean _started;

	/** property name for the case of LabelFigure */
	private String _propNameForLabel;

	private static final Log log = LogFactory.getLog(FigureUpdater.class);

	private static final boolean isDebugging = log.isDebugEnabled();
}
