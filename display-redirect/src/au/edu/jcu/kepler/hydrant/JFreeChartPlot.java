/*
 * Copyright (C) 2007-2008  James Cook University (http://www.jcu.edu.au).
 * 
 * This program was developed as part of the ARCHER project (Australian
 * (Research Enabling Environment) funded by a Systemic Infrastructure
 * Initiative (SII) grant and supported by the Australian Department of
 * Innovation, Industry, Science and Research.

 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.

 * IN NO EVENT SHALL THE JAMES COOK UNIVERSITY BE LIABLE TO ANY PARTY FOR 
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING
 *  OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE 
 * JAMES COOK UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * THE JAMES COOK UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE JAMES COOK UNIVERSITY 
 * HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 */

package au.edu.jcu.kepler.hydrant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.KeypointPNGEncoderAdapter;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ptolemy.actor.lib.gui.PlotterBase;
import ptolemy.plot.Plot;

///////////////////////////////////////////////////////////////////
////JFreeChartPlot

/**
<p>
JFreeChartPlot is to generate plot figures using JFree library.
It will be used to redirect images of ptolemy.actor.lib.gui.Plotter actor into files.
</p>

@author Jianwu Wang
@version $Id: JFreeChartPlot.java 62778 2012-01-12 04:21:43Z cxh $
@since Kepler 2.3
*/

public class JFreeChartPlot extends Plot {

    protected JFreeChart _chart;
    protected XYSeriesCollection _dataset;
    protected List<String> _legend;
    PlotterBase _plotterBase;
	
	
	public JFreeChartPlot(PlotterBase plotterBase) {
		_plotterBase = plotterBase;
		_dataset = new XYSeriesCollection(); 
    	_chart = ChartFactory.createXYLineChart(getName(), "", "", _dataset, PlotOrientation.VERTICAL, true, false, false);    	
    	String value = plotterBase.legend.getExpression();
    	_legend = new ArrayList<String>();
        if ((value != null) && !value.trim().equals("")) {
            StringTokenizer tokenizer = new StringTokenizer(value, ",");
            while (tokenizer.hasMoreTokens()) {
                _legend.add(tokenizer.nextToken().trim());
            }
        }
	}


	public synchronized void addPoint(final int series_id, final double xvalue, final double yvalue,  final boolean connected) {
		XYSeries series = null;
    	try {
    		series = _dataset.getSeries(series_id);
    	} catch (IllegalArgumentException e) {
    		String key;
    		try {
    			key = _legend.get(series_id);
    		} catch (IndexOutOfBoundsException e1) {
    			key = Integer.toString(series_id);
    		}
    		series = new XYSeries(key);
    		_dataset.addSeries(series);
    	}
    	series.add(xvalue, yvalue);	
	}
	
    public synchronized void fillPlot() {
		ReplacementManager man = ReplacementUtils.getReplacementManager(_plotterBase);
		HashMap data_map = new HashMap();
		String fullName = _plotterBase.getFullName();
		data_map.put("name", fullName);
		data_map.put("type", "IMAGE");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		KeypointPNGEncoderAdapter encoder = new KeypointPNGEncoderAdapter();
		try {
			encoder.encode(_chart.createBufferedImage(480, 300), baos);

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		data_map.put("plotOutput", baos);
		data_map.put("format", "png");
		//data_map.put("output", file.getAbsolutePath());
		man.writeData(data_map);
    }

}
