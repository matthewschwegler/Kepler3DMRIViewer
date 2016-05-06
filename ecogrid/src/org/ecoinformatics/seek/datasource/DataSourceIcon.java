/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 * CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 *
 */

package org.ecoinformatics.seek.datasource;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.icon.EditorIcon;
import ptolemy.vergil.kernel.attributes.EllipseAttribute;
import ptolemy.vergil.kernel.attributes.LineAttribute;
import ptolemy.vergil.kernel.attributes.ResizablePolygonAttribute;
import ptolemy.vergil.kernel.attributes.TextAttribute;

public class DataSourceIcon {

	private static final String TRANSPARENT = "{0.0, 0.0, 0.0, 0.0}";
	private static final String WHITE = "{1.0, 1.0, 1.0, 1.0}";

	private static final String READY_TEXT = "";
	private static final String READY_TEXT_COLOR = TRANSPARENT;
	private static final String READY_BG_COLOR = "{1.0, 0.81, 0.54, 1.0}";
	private static final String READY_OUTLINE_COLOR = READY_BG_COLOR;

	private static final String BUSY_TEXT = "BUSY";
	private static final String BUSY_TEXT_COLOR = "{0.0, 0.28, 0.54, 1.0}";
	private static final String BUSY_BG_COLOR = TRANSPARENT;
	private static final String BUSY_OUTLINE_COLOR = BUSY_TEXT_COLOR;

	private static final String ERROR_TEXT = "ERROR";
	private static final String ERROR_TEXT_COLOR = "{0.85, 0.11, 0.11, 1.0}";
	private static final String ERROR_BG_COLOR = TRANSPARENT;
	private static final String ERROR_OUTLINE_COLOR = ERROR_TEXT_COLOR;

	// size in pixels of blank margin or buffer-zone to be drawn around the
	// icon:
	private static final int MARGIN_WIDTH = 5;

	private static final int CYL_WIDTH = 40;
	private static final int CYL_DIA = 10;
	private static final int CYL_HEIGHT = 35;

	/** Icon indicating the communication region. */
	private EllipseAttribute _topEllipse, _bottomEllipse;

	private LineAttribute _leftLine, _rightLine;

	ResizablePolygonAttribute _mainFill;

	private TextAttribute _text;

	private EditorIcon node_icon;

	/**
	 * Create the DataSource Icon.
	 * 
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 * @param parent
	 *            NamedObj
	 */
	public DataSourceIcon(NamedObj parent) throws NameDuplicationException,
			IllegalActionException {

		node_icon = new EditorIcon(parent, "_icon");

		_leftLine = new LineAttribute(node_icon, "left");
		_leftLine.x.setToken("0");
		_leftLine.y.setToken(String.valueOf(CYL_HEIGHT));
		Location _location2 = new Location(_leftLine, "_location");
		_location2.setExpression("-" + ((long) CYL_WIDTH) / 2 + ", 0.0");
		_leftLine.lineColor.setToken(BUSY_OUTLINE_COLOR);

		final String halfCylWidthStr = String.valueOf(((long) CYL_WIDTH) / 2);

		_rightLine = new LineAttribute(node_icon, "right");
		_rightLine.x.setToken("0");
		_rightLine.y.setToken(String.valueOf(CYL_HEIGHT));
		Location _location3 = new Location(_rightLine, "_location");
		_location3.setExpression(halfCylWidthStr + ", 0.0");
		_rightLine.lineColor.setToken(BUSY_OUTLINE_COLOR);

		_bottomEllipse = new EllipseAttribute(node_icon, "bottom");
		_bottomEllipse.width.setToken(String.valueOf(CYL_WIDTH));
		_bottomEllipse.height.setToken(String.valueOf(CYL_DIA));
		_bottomEllipse.centered.setToken("false");
		Location _location4 = new Location(_bottomEllipse, "_location");
		_location4.setExpression("0.0, " + CYL_HEIGHT);
		_bottomEllipse.lineColor.setToken(BUSY_OUTLINE_COLOR);

		_mainFill = new ResizablePolygonAttribute(node_icon, "mainFill");
		_mainFill.vertices.setToken("{-" + halfCylWidthStr + ", 0, "
				+ halfCylWidthStr + ", 0, " + halfCylWidthStr + ", "
				+ CYL_HEIGHT + ", " + "-" + halfCylWidthStr + ", " + CYL_HEIGHT
				+ ", " + "-" + halfCylWidthStr + ", 0 }");
		_mainFill.width.setToken(String.valueOf(CYL_WIDTH));
		_mainFill.height.setToken(String.valueOf(CYL_HEIGHT));
		_mainFill.centered.setToken("false");
		_mainFill.lineColor.setToken(TRANSPARENT);
		_mainFill.fillColor.setToken(TRANSPARENT);

		_topEllipse = new EllipseAttribute(node_icon, "top");
		_topEllipse.width.setToken(String.valueOf(CYL_WIDTH));
		_topEllipse.height.setToken(String.valueOf(CYL_DIA));
		_topEllipse.centered.setToken("false");
		Location _location1 = new Location(_topEllipse, "_location");
		_location1.setExpression("0.0, 0.0");
		_topEllipse.lineColor.setToken(BUSY_OUTLINE_COLOR);

		// outline is a transparent box that acts as a buffer zone or spacer
		// around the entire folder, so the ports don't get too close. The size
		// of this buffer, in pixels, can be set using the "MARGIN_WIDTH"
		//
		// NOTE - bad code - values showing size of folder are hard-coded,
		// because this code will ultimately be replaced by an SVG image file...
		//
		ResizablePolygonAttribute outline = new ResizablePolygonAttribute(
				node_icon, "outline");

		outline.vertices.setToken("{-1, 0, 1, 0, 1, 1, -1, 1, -1, 0 }");

		outline.width.setToken("" + (2 * MARGIN_WIDTH + CYL_WIDTH));
		outline.height.setToken("" + (2 * MARGIN_WIDTH + CYL_HEIGHT));
		outline.centered.setToken("false");
		outline.lineColor.setToken(TRANSPARENT);
		outline.fillColor.setToken(TRANSPARENT);

		_text = new TextAttribute(node_icon, "text");
		_text.textSize.setExpression("11");
		_text.bold.setExpression("true");

		int textVertOffset = (2 * CYL_DIA);
		Location _location5 = new Location(_text, "_location");
		_location5
				.setExpression("-" + halfCylWidthStr + ", -" + textVertOffset);

		// this is the view that shows up as the tree icon:
		setReady();

		node_icon.setPersistent(false);
	}

	/**
	 * Set the icon to Busy state
	 * 
	 */
	public void setBusy() {
		setIconStatus(BUSY_TEXT, BUSY_TEXT_COLOR, BUSY_BG_COLOR,
				BUSY_OUTLINE_COLOR);
	}

	/**
	 * Set the icon to Ready state
	 * 
	 */
	public void setReady() {
		setIconStatus(READY_TEXT, READY_TEXT_COLOR, READY_BG_COLOR,
				READY_OUTLINE_COLOR);
		try {
			_topEllipse.lineColor.setToken(WHITE);
			_topEllipse.lineWidth.setToken("2");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set the icon to Error state
	 * 
	 */
	public void setError() {
		setIconStatus(ERROR_TEXT, ERROR_TEXT_COLOR, ERROR_BG_COLOR,
				ERROR_OUTLINE_COLOR);
	}

	/**
	 * Set the text and color of the icon
	 * 
	 * @param displayText
	 *            String
	 * @param textColor
	 *            String
	 * @param bgColor
	 *            String
	 * @param outlineColor
	 *            String
	 */
	private void setIconStatus(String displayText, String textColor,
			String bgColor, String outlineColor) {
		try {
			_text.textColor.setToken(textColor);
			_text.text.setExpression(displayText);
			_topEllipse.fillColor.setToken(bgColor);
			_topEllipse.lineColor.setToken(outlineColor);
			_topEllipse.lineWidth.setToken("1");
			_leftLine.lineColor.setToken(outlineColor);
			_rightLine.lineColor.setToken(outlineColor);
			_bottomEllipse.fillColor.setToken(bgColor);
			_bottomEllipse.lineColor.setToken(outlineColor);
			_mainFill.fillColor.setToken(bgColor);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}