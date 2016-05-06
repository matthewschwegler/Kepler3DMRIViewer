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

import java.awt.geom.RectangularShape;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import diva.canvas.CompositeFigure;

/**
 * A base class for some of the offered figures.
 * 
 * @author Carlos Rueda
 * @version $Id: BaseFigure.java 24234 2010-05-06 05:21:26Z welker $
 */
public abstract class BaseFigure extends CompositeFigure {

	/**
	 * Two possible orientations for a monitor figure. Orientation may be
	 * meaningless for certain figures.
	 */
	public enum Orientation {
		HORIZONTAL, VERTICAL
	};

	/**
	 * Updates the GUI according to the current state of this monitor. It calls
	 * _update on the AWT event thread.
	 */
	public void update() {
		_lastUpdate = System.currentTimeMillis();
		Runnable doSet = new Runnable() {
			public void run() {
				_update();
			}
		};
		SwingUtilities.invokeLater(doSet);
	}

	/**
	 * @return the orientation
	 */
	public Orientation getOrientation() {
		return _orientation;
	}

	/**
	 * It only sets the orientation indicator.
	 * 
	 * @param orientation
	 *            the orientation.
	 */
	public void setOrientation(Orientation orientation) {
		this._orientation = orientation;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	protected BaseFigure(RectangularShape shape) {
		this._shape = (RectangularShape) shape.clone();
	}

	/**
	 * A subclass implements this method to do the actual update of the this
	 * figure. This method is called by {@link #update()} on the AWT event
	 * thread.
	 */
	protected abstract void _update();

	/** Arbitrarily initialized to {@link Orientation.HORIZONTAL}. */
	protected Orientation _orientation = Orientation.HORIZONTAL;

	/** The shape associated with the figure */
	protected RectangularShape _shape;

	/** The last time {@link #update()} was called. */
	protected long _lastUpdate = 0;

	protected static final Log log = LogFactory.getLog(BaseFigure.class);
	protected static final boolean isDebugging = log.isDebugEnabled();

}
