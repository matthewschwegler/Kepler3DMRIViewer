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
package org.kepler.monitor.figure;

import java.awt.Color;
import java.awt.geom.RectangularShape;
import java.util.HashMap;
import java.util.Map;

/**
 * A figure with a current state.
 * 
 * @author Carlos Rueda
 * @version $Id: StateFigure.java 24234 2010-05-06 05:21:26Z welker $
 */
public abstract class StateFigure extends BaseFigure {

	/**
	 * Sets the color scheme for a state.
	 * 
	 * @param state
	 *            the state
	 * @param colors
	 *            the color scheme
	 */
	public void addColorScheme(Object state, Color[] colors) {
		_stateColors.put(state, colors);
	}

	/**
	 * Returns the color scheme associated with a state.
	 * 
	 * @param state
	 *            the state
	 * @return color scheme
	 */
	public Color[] getColorScheme(Object state) {
		return _stateColors.get(state);
	}

	/**
	 * Sets the current state of this figure. Does nothing if the state is equal
	 * to the current state.
	 * 
	 * @param state
	 *            the new state.
	 * 
	 * @throw IllegalArgumentException If the state is not recognized.
	 */
	public void setState(Object state) {
		if (!_stateColors.keySet().contains(state)) {
			throw new IllegalArgumentException("State '" + state
					+ "' has not been associated");
		}
		if (_currentState == state) {
			return;
		}

		_currentState = state;

		update();
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	protected StateFigure(RectangularShape shape) {
		super(shape);
	}

	/** Current state associated with this monitor. null by default */
	protected Object _currentState;

	protected Map<Object, Color[]> _stateColors = new HashMap<Object, Color[]>();

}
