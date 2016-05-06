/*
 *    $RCSfile$
 *
 *     $Author: crawl $
 *       $Date: 2012-11-26 14:22:25 -0800 (Mon, 26 Nov 2012) $
 *   $Revision: 31122 $
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

import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;

import org.kepler.monitor.MonitorIcon;
import org.kepler.monitor.figure.BaseFigure.Orientation;
import org.kepler.monitor.figure.ProgressBarFigure;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This actor demonstrates the ProgressBarFigure by simulating some task that
 * depends on the arrival of tokens.
 * 
 * In this demo, the figure is always displayed. A parameter allows to set the
 * maximum value for the range of the progress bar. Upon each firing, the
 * current value of the progress bar is incremented. If it reaches the maximum,
 * then it's set to the minimum again. If the maximum parameter is set to 0, the
 * progress bar takes the indeterminate style. By default, the progress bar
 * takes a horizontal layout.
 * 
 * @see org.kepler.monitor.figure.ProgressBarFigure
 * 
 * @author Carlos Rueda
 * @version $Id: ActorWithProgressBar.java 31122 2012-11-26 22:22:25Z crawl $
 */
public class ActorWithProgressBar extends Transformer {
	public StringParameter maxParam;
	public StringParameter layoutParam;

	public ActorWithProgressBar(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		maxParam = new StringParameter(this, "maxParam");
		maxParam.setExpression("" + _max);
		maxParam.setDisplayName("Maximum (0 for indeterminate)");

		layoutParam = new StringParameter(this, "layoutParam");
		layoutParam.setExpression("horizontal");
		layoutParam.addChoice("horizontal");
		layoutParam.addChoice("vertical");
		layoutParam.setDisplayName("Layout");

		_icon = new MonitorIcon(this, "_icon");
		_icon.setText("ActorWithProgressBar");

		_figure = _createFigure();
		_icon.setFigure(_figure);
	}

	/**
	 * Starts the progress bar.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_figure.setStarted(true);
	}

	/**
	 * Updates the progress bar according to the new parameters.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == maxParam) {
			try {
				int max = Integer.parseInt(maxParam.stringValue());
				if (max <= 0) {
					_figure.setIndeterminate(true);
				} else {
					_figure.setIndeterminate(false);
					_figure.setMaximum(max);
					_figure.setValue(_figure.getMinimum() - 1);
				}
			} catch (NumberFormatException e) {
				throw new IllegalActionException("NumberFormatException: " + e);
			}
		} else if (attribute == layoutParam) {
			if ("horizontal".equalsIgnoreCase(layoutParam.stringValue())) {
				_figure.setShape(_horizontalBounds);
				_figure.setOrientation(Orientation.HORIZONTAL);
			} else {
				_figure.setShape(_verticalBounds);
				_figure.setOrientation(Orientation.VERTICAL);
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Broadcasts the input token. If the progress bar is determinate, it
	 * increments the current value, re-starting from the minimum if the current
	 * value has already reached the maximum.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (input.hasToken(0)) {

			if (!_figure.isIndeterminate()) {
				int next = 1 + _figure.getCurrentValue();
				if (next > _figure.getMaximum()) {
					// re-start progress:
					next = _figure.getMinimum() - 1;
				}
				_figure.setValue(next);
			}

			output.broadcast(input.get(0));
		}
	}

	/** Stops the progress bar. */
	public void wrapup() throws IllegalActionException {
		super.wrapup();
		_figure.setStarted(false);
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Creates a horizontal, determinate progress bar */
	private static ProgressBarFigure _createFigure() {
		ProgressBarFigure progFig = new ProgressBarFigure(_horizontalBounds);
		progFig.setIndeterminate(false);
		progFig.setMaximum(_max);
		progFig.setValue(progFig.getMinimum() - 1);
		return progFig;
	}

	private static int _max = 50;
	private static RectangularShape _horizontalBounds = new RoundRectangle2D.Double(
			8, 20, 60, 6, 6, 6);
	private static RectangularShape _verticalBounds = new RoundRectangle2D.Double(
			4, 3, 6, 18, 6, 6);
	private ProgressBarFigure _figure;
	private MonitorIcon _icon;

	private static final long serialVersionUID = 1L;
}
