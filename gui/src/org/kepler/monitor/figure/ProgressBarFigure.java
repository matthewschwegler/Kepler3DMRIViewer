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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RectangularShape;

import javax.swing.Timer;

import diva.canvas.toolbox.BasicFigure;

/**
 * A "progress bar" monitor figure.
 * 
 * This figure has some similar functionality as that of
 * {@link javax.swing.JProgressBar}.
 * 
 * <p>
 * By default, the progress bar will be indeterminate. In this case, an
 * animation is set up to show a bouncing box within the given displayable area.
 * 
 * @see org.kepler.monitor.test.ActorWithProgressBar
 * 
 * @author Carlos Rueda
 * @version $Id: ProgressBarFigure.java 24234 2010-05-06 05:21:26Z welker $
 */
public class ProgressBarFigure extends BaseFigure {

	/**
	 * Creates an horizontal and indeterminate progress bar figure.
	 * 
	 * @param shape
	 *            Rectangular shape for the progress bar.
	 */
	public ProgressBarFigure(RectangularShape shape) {
		super(shape);

		_orientation = Orientation.HORIZONTAL;
		_indeterminate = true;

		// _figs[0]: background; _figs[1]: foreground

		_figs = new BasicFigure[2];
		_figs[0] = new BasicFigure(shape);
		_figs[0].setFillPaint(_backgroundColor);
		this.add(_figs[0]);

		_figs[1] = new BasicFigure(shape);
		_figs[1].setFillPaint(_foregroundColor);
		this.add(_figs[1]);
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
			update();
		}
	}

	/**
	 * Set the indeterminate flag of this progress bar figure.
	 * 
	 * @param indeterminate
	 *            true for indeterminate, false for determinate.
	 */
	public void setIndeterminate(boolean indeterminate) {
		boolean old = this._indeterminate;
		this._indeterminate = indeterminate;
		if (old != indeterminate) {
			if (_started) {
				if (_indeterminate) {
					startTimer();
				} else {
					stopTimer();
				}
			}
			update();
		}
	}

	/**
	 * Sets the maximum value, which is meaningful only for a determinate
	 * progress bar.
	 * 
	 * @param n
	 *            the maximum value.
	 */
	public void setMaximum(int n) {
		_range.max = n;
		if (!_indeterminate) {
			update();
		}
	}

	/**
	 * Sets the minimum value, which is meaningful only for a determinate
	 * progress bar.
	 * 
	 * @param n
	 *            the minimum value.
	 */
	public void setMinimum(int n) {
		_range.min = n;
		if (!_indeterminate) {
			update();
		}
	}

	/**
	 * Sets the current value, which is meaningful only for a determinate
	 * progress bar.
	 * 
	 * @param n
	 *            the current value.
	 */
	public void setValue(int n) {
		int old = _range.cur;
		_range.cur = n;
		if (old != n && !_indeterminate) {
			update();
		}
	}

	/**
	 * Tells if this progress bar is indeterminate.
	 * 
	 * @return true iff this progress bar is indeterminate.
	 */
	public boolean isIndeterminate() {
		return _indeterminate;
	}

	/**
	 * Gets the current value associated for a determinate progress bar.
	 * 
	 * @return current value.
	 */
	public int getCurrentValue() {
		return _range.cur;
	}

	/**
	 * Gets the maximum value in the range associated for a determinate progress
	 * bar.
	 * 
	 * @return maximum value in range.
	 */
	public int getMaximum() {
		return _range.max;
	}

	/**
	 * Gets the minimum value in the range associated for a determinate progress
	 * bar.
	 * 
	 * @return minimum value in range.
	 */
	public int getMinimum() {
		return _range.min;
	}

	/**
	 * @return the backgroundColor
	 */
	public Color getBackgroundColor() {
		return _backgroundColor;
	}

	/**
	 * @param backgroundColor
	 *            the backgroundColor to set
	 */
	public void setBackgroundColor(Color backgroundColor) {
		this._backgroundColor = backgroundColor;
		_figs[0].setFillPaint(backgroundColor);
		update();
	}

	/**
	 * @return the foregroundColor
	 */
	public Color getForegroundColor() {
		return _foregroundColor;
	}

	/**
	 * @param foregroundColor
	 *            the foregroundColor to set
	 */
	public void setForegroundColor(Color foregroundColor) {
		this._foregroundColor = foregroundColor;
		_figs[1].setFillPaint(foregroundColor);
		update();
	}

	/**
	 * Sets the rectangular shape for this figure.
	 * 
	 * @param shape
	 */
	public void setShape(RectangularShape shape) {
		this._shape = (RectangularShape) shape.clone();
		_figs[0].setShape(this._shape);
		update();
	}

	public void setStarted(boolean started) {
		log.debug(started);
		_started = started;
		if (_started) {
			_figs[1].setFillPaint(_foregroundColor);
			if (_indeterminate) {
				startTimer();
			}
		} else {
			stopTimer();
			setValue(getMinimum() - 1);
			update();
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	/**
	 * Updates the appearance of this progress bar.
	 */
	protected void _update() {
		double x = _shape.getX();
		double y = _shape.getY();
		double h = _shape.getHeight();
		double w = _shape.getWidth();

		RectangularShape rect1 = (RectangularShape) _figs[1].getShape();

		if (!_started) {
			// a trick to "only" show the background
			_figs[1].setFillPaint(_backgroundColor);
			rect1.setFrame(x, y, w, h);
			_figs[1].setShape(rect1);
			return;
		}

		if (_orientation == Orientation.HORIZONTAL) {
			if (_indeterminate) {
				double xx = x + _indet.shift * w;
				double ww = _indet.bbSize * w;
				if (xx < x) {
					ww -= x - xx;
					xx = x;
				} else if (xx + ww > x + w) {
					ww = x + w - xx;
				}
				rect1.setFrame(xx, y, ww, h);
			} else {
				// always, left to right:
				double ww = _range.fraction() * w;
				rect1.setFrame(x, y, ww, h);
			}
		} else {
			if (_indeterminate) {
				double yy = y + _indet.shift * h;
				double hh = _indet.bbSize * h;
				if (yy < y) {
					hh -= y - yy;
					yy = y;
				} else if (yy + hh > y + h) {
					hh = y + h - yy;
				}
				rect1.setFrame(x, yy, w, hh);
			} else {
				// always, bottom to top:
				double hh = _range.fraction() * h;
				rect1.setFrame(x, y + h - hh, w, hh);
			}
		}
		_figs[0].setShape(_shape);
		_figs[1].setShape(rect1);
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * Starts a timer for animating this indeterminate progress bar. Does
	 * nothing if <code>!this.isIndeterminate()</code>.
	 */
	private void startTimer() {
		if (_timer == null && _indeterminate) {
			_timer = new Timer(_indet.delay, new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					_indet.update();
					// we can call _update() directly because this runs on the
					// event thread:
					_lastUpdate = System.currentTimeMillis();
					_update();
				}
			});
			_timer.start();
			if (isDebugging) {
				log.debug("Started timer");
			}
		}
	}

	/**
	 * Stops the timer for animating this indeterminate progress bar. Does
	 * nothing if no such timer has been created.
	 */
	private void stopTimer() {
		if (_timer != null) {
			_timer.stop();
			_timer = null;
			if (isDebugging) {
				log.debug("Stopped timer");
			}
		}
	}

	/** to update an indeterminate progress bar */
	private Timer _timer;

	private boolean _started;

	private Color _backgroundColor = new Color(185, 185, 255);
	private Color _foregroundColor = Color.CYAN;
	private BasicFigure[] _figs;

	private boolean _indeterminate;

	/** Range in case of a determinate progress bar */
	private Range _range = new Range(0, 100);

	/** Info in case of an indeterminate progress bar */
	private Indet _indet = new Indet();

	/** Range info for a determinate progress bar. */
	private final static class Range {
		Range(int min, int max) {
			this.min = min;
			this.max = max;
			this.cur = min - 1;
		}

		double fraction() {
			return cur < min ? 0.0 : min >= max ? 1.0 : (double) (cur - min)
					/ (max - min);
		}

		int min;
		int max;
		int cur;
	}

	/** Info for an indeterminate progress bar. */
	private final static class Indet {
		/** To repaint */
		final int delay = 55;

		/** bouncing box size */
		final double bbSize = 0.3;

		/** increment to update shift; should be &lt; bbSize */
		double delta = 0.1;

		/** we allow the bouncing box to "hide" by at most this fraction */
		final double hideSize = delta;

		/** current position of bouncing box */
		double shift = Math.random(); // -hideSize;

		/** updates position of bouncing box */
		void update() {
			shift += delta;
			if (shift < -hideSize) {
				shift = -hideSize;
				delta *= -1;
			} else if (shift > 1.0 - (bbSize - hideSize)) {
				shift = 1.0 - (bbSize - hideSize);
				delta *= -1;
			}
		}
	}

}
