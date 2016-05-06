/*
 *    $RCSfile$
 *
 *     $Author: crawl $
 *       $Date: 2010-06-10 10:12:37 -0700 (Thu, 10 Jun 2010) $
 *   $Revision: 24796 $
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
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

import javax.swing.SwingUtilities;

import org.kepler.monitor.MonitoredStatus.State;

import ptolemy.data.ScalarToken;
import ptolemy.kernel.util.IllegalActionException;
import diva.canvas.toolbox.BasicFigure;

/** A quality figure.
 * 
 * @author Aisa Na'Im
 * @version $Id: QualityFigure.java 24796 2010-06-10 17:12:37Z crawl $
 *
 */
public class QualityFigure extends BaseFigure {

	public QualityFigure(RectangularShape shape) {
		super(shape);
		
		this._shape = (RectangularShape) shape.clone();
		_orientation = Orientation.HORIZONTAL;
		figs = new BasicFigure[1];
		_setRectangle(figs, _orientation, _shape);
		
		for (BasicFigure fig : figs) {
			this.add(fig);
		}
	}
	

	/**
     * Updates the quality score value 
     */
    public void update2(final Object value) {
        _lastUpdate = System.currentTimeMillis();
        Runnable doSet = new Runnable() {
            public void run() {
                _update2(value);
            }
        };
        SwingUtilities.invokeLater(doSet);
    }

	/** Set the rectangle figure */
	private static void _setRectangle(BasicFigure[] figs, Orientation orientation,
			RectangularShape shape) {
	
		double x = shape.getX();
		double y = shape.getY();
		double h = shape.getHeight();
		double w = shape.getWidth();

		figs[0] = new BasicFigure(new Rectangle2D.Double(x, y, h, w));
	}
	

	@Override
	protected void _update() {
	}
	

	/** Update color of rectangle based on quality thresholds and score */
	protected void _update2(Object value) {
		
		if (!(value.equals(null) || value.equals(State.NO_QUALITY_SCORE))) {
			//figs[0].setFillPaint(Color.BLACK);
		//}
		//else {
			setQualityValue(value);
			try {
				if (quality_value.equals(high_quality) ||
						quality_value.isGreaterThan(high_quality).booleanValue()) {
					figs[0].setFillPaint(Color.GREEN);
				}
				else if (quality_value.equals(low_quality) || quality_value.isLessThan(low_quality).booleanValue()) {
					figs[0].setFillPaint(Color.RED);
				}
				else if (quality_value.isLessThan(high_quality).booleanValue() &&
						quality_value.isGreaterThan(low_quality).booleanValue())
				{
					figs[0].setFillPaint(Color.YELLOW);
				}
					
			} catch (IllegalActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** Set the high quality threshold for entity being monitored */
	public void setHighQualityThreshold(Object high_quality_token){
		high_quality = (ScalarToken) high_quality_token; 
	}
	
	/** Set the low quality threshold for entity being monitored */
	public void setLowQualityThreshold(Object low_quality_token) {
		low_quality = (ScalarToken) low_quality_token; 

	}
	
	/** Set the quality score for entity being monitored */
	public void setQualityValue(Object quality_score_token) {
			quality_value = (ScalarToken) quality_score_token; 
			//System.out.println("(QF) quality_score is: " + quality_score_token.toString());
	}
	
	/** The rectangle within the rectangle to be filled */
	private BasicFigure[] figs;

	/** Quality score values and thresholds */ 
	private ScalarToken quality_value; 	
	private ScalarToken high_quality; 
	private ScalarToken low_quality; 
	 
}
