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
import java.awt.geom.Ellipse2D;
import java.awt.geom.RectangularShape;

import org.kepler.monitor.MonitoredStatus.State;

import diva.canvas.toolbox.BasicFigure;

public class TrafficLightFigure extends StateFigure {

	public TrafficLightFigure(int numberOfLights, RectangularShape shape) {
		super(shape);
		if (numberOfLights <= 0) {
			throw new IllegalArgumentException(
					"numberOfLights must be positive");
		}

		this._shape = (RectangularShape) shape.clone();

		_orientation = Orientation.VERTICAL;

		figs = new BasicFigure[numberOfLights];
		_setLights(figs, _orientation, _shape);
		for (BasicFigure fig : figs) {
			this.add(fig);
		}

		// set the color schemes for my possible states:
		Color[] idleColors = new Color[numberOfLights];
		Color[] waitingColors = new Color[numberOfLights];
		Color[] runningColors = new Color[numberOfLights];
		Color[] errorColors = new Color[numberOfLights];
		for (int i = 0; i < numberOfLights; i++) {
			idleColors[i] = Color.GRAY;
			waitingColors[i] = Color.GRAY;
			runningColors[i] = Color.GRAY;
			errorColors[i] = Color.RED;
		}
		runningColors[numberOfLights - 1] = Color.GREEN;

		int wait_from = 1;
		int wait_lim = numberOfLights - 1;
		if (numberOfLights == 2) {
			wait_from = 0;
		} else if (numberOfLights == 1) {
			wait_from = 0;
			wait_lim = numberOfLights;
		}
		for (int i = wait_from; i < wait_lim; i++) {
			waitingColors[i] = Color.YELLOW;
		}

		// Add my possible states:
		addColorScheme(State.IDLE, idleColors);
		addColorScheme(State.WAITING, waitingColors);
		addColorScheme(State.RUNNING, runningColors);
		addColorScheme(State.ERROR, errorColors);

		// set current state, and the state for the timer:
		setState(State.IDLE);
	}

	/**
	 * Sets the orientation and calls {@link #update()} if the orientation
	 * changes.
	 * 
	 * @param orientation
	 *            the orientation.
	 */
	public void setOrientation(Orientation orientation) {
		Orientation old = this._orientation;
		this._orientation = orientation;
		if (old != orientation) {
			_setLights(figs, _orientation, _shape);
			update();
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	/**
	 * Sets the color for each light according to current state.
	 */
	protected void _update() {
		Color[] colors = getColorScheme(_currentState);
		for (int i = 0; i < figs.length; i++) {
			figs[i].setFillPaint(colors[i]);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * Creates or sets the shape for the lights.
	 */
	private static void _setLights(BasicFigure[] figs, Orientation orientation,
			RectangularShape shape) {
		int numberOfLights = figs.length;
		double x = shape.getX();
		double y = shape.getY();
		double h = shape.getHeight();
		double w = shape.getWidth();

		double figW = w;
		double figH = h / numberOfLights;
		double incX = 0;
		double incY = figH;

		if (orientation == Orientation.VERTICAL) {
			figW = w;
			figH = h / numberOfLights;
			incX = 0;
			incY = figH;
		} else {
			figW = w / numberOfLights;
			figH = h;
			incX = figW;
			incY = 0;
		}

		// create or set the lights:
		for (int i = 0; i < figs.length; i++) {
			if (figs[i] == null) {
				figs[i] = new BasicFigure(new Ellipse2D.Double(x + i * incX, y
						+ i * incY, figW, figH));
			} else {
				figs[i].setShape(new Ellipse2D.Double(x + i * incX, y + i
						* incY, figW, figH));
			}
		}
	}

	/** The lights */
	private BasicFigure[] figs;

}
