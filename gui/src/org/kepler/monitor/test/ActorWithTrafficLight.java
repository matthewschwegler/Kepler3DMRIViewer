/*
 *    $RCSfile$
 *
 *     $Author: welker $
 *       $Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $
 *   $Revision: 24234 $
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
package org.kepler.monitor.test;

import java.awt.geom.Rectangle2D;

import org.kepler.monitor.FigureUpdater;
import org.kepler.monitor.MonitorIcon;
import org.kepler.monitor.MonitoredStatus;
import org.kepler.monitor.MonitoredStatus.State;
import org.kepler.monitor.figure.TrafficLightFigure;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This actor demonstrates the {@link TrafficLightFigure} by simulating some
 * task that depends on the arrival of tokens.
 * 
 * In this demo, the figure is only displayed during execution.
 * 
 * @see org.kepler.monitor.TrafficLightFigure
 * 
 * @author Carlos Rueda
 * @version $Id: ActorWithTrafficLight.java 24234 2010-05-06 05:21:26Z welker $
 */
public class ActorWithTrafficLight extends Transformer {

	/** Delay for the timer */
	public StringParameter timerDelay;

	public ActorWithTrafficLight(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		timerDelay = new StringParameter(this, "timerDelay");
		timerDelay.setExpression("" + _delay);
		timerDelay.setDisplayName("Delay to set WAITING state (ms)");

		_icon = new MonitorIcon(this, "_icon");
		_icon.setText("ActorWithTrafficLight");

		_status = new MonitoredStatus();
		_figure = new TrafficLightFigure(3, new Rectangle2D.Double(5, 3, 6,
				3 * 6));
		_status.getPropertyTimer().setDelay(_delay);
		_figUpdater = new FigureUpdater();
		_figUpdater.setFigure(_figure);
	}

	/**
	 * Starts the figure.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_icon.setFigure(_figure);
		_status.addPropertyChangeListener(_figUpdater);
		_status.getPropertyTimer().start();
	}

	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == timerDelay) {
			try {
				_delay = Integer.parseInt(timerDelay.stringValue());
				_status.getPropertyTimer().setDelay(_delay);
			} catch (NumberFormatException e) {
				throw new IllegalActionException("NumberFormatException: " + e);
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Broadcasts the input token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (input.hasToken(0)) {
			_status.setProperty(State.STATE, State.RUNNING);
			output.broadcast(input.get(0));
		}
	}

	/** Stops the figure. */
	public void wrapup() throws IllegalActionException {
		super.wrapup();
		_figure.setState(State.IDLE);
		_status.getPropertyTimer().stop();
		_status.removePropertyChangeListener(_figUpdater);
		_icon.setFigure(null);
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private MonitoredStatus _status;
	private TrafficLightFigure _figure;
	private FigureUpdater _figUpdater;
	private MonitorIcon _icon;
	private int _delay = 1000;

	private static final long serialVersionUID = 1L;
}
