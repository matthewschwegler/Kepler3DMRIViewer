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

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.SwingConstants;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.icon.DynamicEditorIcon;
import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.LabelFigure;

/**
 * The actual icon that displays a monitoring figure. This figure is set with
 * {@link #setFigure(Figure)}.
 * 
 * @author Carlos Rueda
 * @version $Id: MonitorIcon.java 30396 2012-08-09 23:50:32Z barseghian $
 */
public class MonitorIcon extends DynamicEditorIcon {
	public MonitorIcon(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		_text = null;
		setPersistent(false);
	}

	/**
	 * Sets the foreground figure for this icon.
	 * 
	 * @param figure
	 */
	public void setFigure(Figure figure) {
		_figure = figure;
	}

	public Figure getFigure() {
		return _figure;
	}

	/**
	 * Create a new default background figure.
	 * 
	 * @return A figure representing the specified shape.
	 */
	public Figure createBackgroundFigure() {
		double x = 0;
		double y = 0;
		if (_text != null) {
			_createLabelFigure();
		} else if (_figure != null) {
			Rectangle2D bounds = _figure.getBounds();
			x = bounds.getMinX();
			y = bounds.getMinY();
			_width = bounds.getWidth();
			_heigth = bounds.getHeight();
		}
		BasicFigure fig = new BasicFigure(new RoundRectangle2D.Double(x, y,
				_width, _heigth, 8, 8));
		fig.setFillPaint(_bgColor);
		fig.setStrokePaint(_bgColor);
		return fig;
	}

	public Figure createFigure() {
		CompositeFigure result = (CompositeFigure) super.createFigure();

		if (_text != null) {
			LabelFigure label = _createLabelFigure();
			Rectangle2D backBounds = result.getBackgroundFigure().getBounds();
			label.translateTo(backBounds.getMinX() + 11, backBounds.getMinY()
					+ _heigth / 4);
			result.add(label);
		}

		if (_figure != null) {
			_addLiveFigure(_figure);
			result.add(_figure);
		}

		return result;
	}

	
	public void setBackgroundColor(Color color) {
		_bgColor = color;
	}

	/** Set the color of the text for this icon. */
	public void setForegroundColor(Color color) {
	    _labelColor = color;
	}

	/** Sets the text for this icon */
	public void setText(String _text) {
		this._text = _text;
	}

	/** Gets the text of this icon */
	public String getText() {
		return _text;
	}

	/** Sets the text font for this icon */
	public void setFont(Font font) {
		this._font = font;
	}
	
	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private String _displayString() {
		if (_text != null) {
			LabelFigure label = new LabelFigure(_text, _font, 1.0,
					SwingConstants.LEFT);
			Rectangle2D stringBounds = label.getBounds();
			_width = stringBounds.getWidth() + 20;
			_heigth = stringBounds.getHeight() + 12;
		}
		return _text;
	}

	private LabelFigure _createLabelFigure() {
		LabelFigure label = new LabelFigure(_displayString(), _font);

		// By default, the origin should be the upper left.
		label.setAnchor(SwingConstants.NORTH_WEST);
		label.setFillPaint(_labelColor);

		return label;
	}

	private double _width = 1;

	private double _heigth = 1;

	/** The foreground figure for this icon */
	private Figure _figure;

	private Font _font = new Font("SansSerif", Font.PLAIN, 12);

	private Color _bgColor = Color.LIGHT_GRAY;

	private Color _labelColor = Color.WHITE;// BLACK;

	private String _text = null;

	private static final long serialVersionUID = 1L;
}
