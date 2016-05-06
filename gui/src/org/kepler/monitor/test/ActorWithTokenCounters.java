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

import java.awt.Font;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingConstants;

import org.kepler.monitor.MonitorIcon;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import diva.canvas.CompositeFigure;
import diva.canvas.toolbox.LabelFigure;

/**
 * This actor demonstrates the association of monitoring icons that show the
 * number of tokens being received/produced in each port.
 * 
 * @see org.kepler.monitor.figure
 * 
 * @author Carlos Rueda
 * @version $Id: ActorWithTokenCounters.java 24234 2010-05-06 05:21:26Z welker $
 */
public class ActorWithTokenCounters extends Transformer {
	public StringParameter maxParam;

	public ActorWithTokenCounters(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		_icon = new MonitorIcon(this, "_icon");
		_icon.setText("ActorWithTokenCounters");

		_inputCounterFigure = _createFigure("in=0");
		_outputCounterFigure = _createFigure("out=0");

		CompositeFigure _figure = new CompositeFigure();
		_figure.add(_inputCounterFigure);
		_figure.add(_outputCounterFigure);

		// TODO: proper location of figures is still unclear

		Rectangle2D inpBounds = _inputCounterFigure.getBounds();
		double h = 25 + inpBounds.getHeight();
		double inX = 20;
		double outX = inX + 5 * inpBounds.getWidth();
		_inputCounterFigure.translate(inX, h);
		_outputCounterFigure.translate(outX, h);

		_icon.setFigure(_figure);
	}

	/**
	 * Starts the counters.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_inputCounterFigure.setString("in=" + (_inputCounter = 0));
		_outputCounterFigure.setString("out=" + (_outputCounter = 0));
	}

	/**
	 * Broadcasts every other input token. Updates the input and output
	 * counters.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (input.hasToken(0)) {
			_inputCounterFigure.setString("in=" + (++_inputCounter));
			if (_inputCounter % 2 == 0) {
				output.broadcast(input.get(0));
				_outputCounterFigure.setString("out=" + (++_outputCounter));
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Creates a figure for a counter */
	private static LabelFigure _createFigure(String str) {
		LabelFigure fig = new LabelFigure(str);
		fig.setAnchor(SwingConstants.LEFT);
		fig.setFont(new Font("monospaced", Font.PLAIN, 8));
		return fig;
	}

	private int _inputCounter = 0;
	private LabelFigure _inputCounterFigure;

	private int _outputCounter = 0;
	private LabelFigure _outputCounterFigure;

	private MonitorIcon _icon;

	private static final long serialVersionUID = 1L;
}
