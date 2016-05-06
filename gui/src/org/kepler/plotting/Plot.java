/*
 * Copyright (c) 2010-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-09 11:05:40 -0700 (Wed, 09 May 2012) $' 
 * '$Revision: 29823 $'
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

package org.kepler.plotting;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 6, 2010
 * Time: 12:17:07 PM
 */

public class Plot extends JPanel {
	public static List<Plot> getAllPlots() {
		return _plots;
	}
	
	public Plot(TableauFrame frame) {
		GridBagLayout layout = new GridBagLayout();
		this.setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		JPanel testGraph = getGraph();
		this.add(testGraph, c);
		
		_plots.add(this);
		
		// When the window is closed the plot needs to be
		// removed from the static list so it can be garbage
		// collected
		WindowClosedAdapter adapter = new WindowClosedAdapter();
		frame.addWindowListener(adapter);
	}

	private JPanel getGraph() {
		// series = new XYSeries("Line!");
		demoSeries = new TimePeriodValues("Demo Series");
		TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();
		dataset.setDomainIsPointsInTime(false);
		this.dataset = dataset;
		// addDemoSeries();
		// dataset = new XYSeriesCollection(series);
		
		//initializeDemoSeries();
		createChart();

		// don't forget this initializes the chart
		setScrolling(false);

		resetToggleMenu();
		
		return panel;
	}
	
	public ChartPanel getChartPanel() {
		return chartPanel;
	}
	
	public void setScrolling(boolean scrolling) {
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 1;

		if (panel == null) {
			// initialize panel
			panel = new JPanel(new GridBagLayout());
		}
		else {
			panel.removeAll();
		}
		// Now we have an empty panel
		
		Component graphComponent;
		chartPanel = new ChartPanel(chart);
		// System.out.println("Chart panel has been set: " + System.identityHashCode(chartPanel));
		if (scrolling) {
			JScrollPane scrollPane = new JScrollPane(chartPanel);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			graphComponent = scrollPane;			
		}
		else {
			graphComponent = chartPanel;
		}
		
		panel.add(graphComponent, constraints);
	}

	private void initializeToggleMenu() {
		if (toggleMenu == null) {
			toggleMenu = new JMenu("Toggle line");
		}
		if (!added) {
			chartPanel.getPopupMenu().add(toggleMenu);
			added = true;
		}
	}

	public void setClearButton(JButton clearButton) {
		this.clearButton = clearButton;
	}
	
	public JButton getClearButton() {
		return clearButton;
	}

	private boolean added = false;
	
	private void clearToggleMenu() {
		toggleMenu.removeAll();
	}
	
	private void populateToggleMenu() {
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			TimePeriodValues series = dataset.getSeries(i);
			addEntry(series);
		}
		for (TimePeriodValues series : frozenSeries) {
			addEntry(series);
		}
	}
	
	private JMenu toggleMenu = null;

	private void addEntry(final TimePeriodValues series) {
		String menuEntryName = "Toggle line '" + series.getKey() + "'";
		final Plot me = this;
		toggleMenu.add(new JMenuItem(new FigureAction(menuEntryName) {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Get series number
				int seriesIndex = -1;
				for (int i = 0; i < dataset.getSeriesCount(); i++) {
					TimePeriodValues currentSeries = dataset.getSeries(i);
					if (series == currentSeries) {
						seriesIndex = i;
						break;
					}
				}
				if (seriesIndex == -1) {
					return;
				}

				XYItemRenderer renderer = me.getXYPlot().getRendererForDataset(dataset);
				Boolean currentVisibility = renderer.getSeriesVisible(seriesIndex);
				if (currentVisibility == null) {
					currentVisibility = true;
				}
				renderer.setSeriesVisible(seriesIndex, !currentVisibility);
			}
		}));		
	}

	public void resetToggleMenu() {
		initializeToggleMenu();
		clearToggleMenu();
		populateToggleMenu();
	}

	private final Set<TimePeriodValues> frozenSeries = new HashSet<TimePeriodValues>();

	public void removeDemoSeries() {
		dataset.removeSeries(demoSeries);
	}
	
	public void addDemoSeries() {
		dataset.addSeries(demoSeries);
	}

	private void createChart() {
		boolean includeLegend = false;
		boolean tooltips = true;
		boolean urls = false;
		
		// chart = ChartFactory.createXYLineChart("Chart Title", "Domain label", "Value label", dataset, PlotOrientation.VERTICAL, includeLegend, tooltips, urls);
		plot = new XYPlot(dataset, new DateAxis("X Axis"), new NumberAxis("Y Axis"), new DefaultXYItemRenderer());
		chart = new JFreeChart("Chart Title", plot);
	}

	private void initializeDemoSeries() {
		DateFormat df = new SimpleDateFormat("M/d/yyyy HH:mm:ss.SSS");
		Date d0 = null, d1 = null, d2 = null, d3 = null, d4 = null;
		Date d5 = null, d6 = null, d7 = null, d8 = null, d9 = null;
		Date d10 = null, d11 = null, d12 = null, d13 = null, d14 = null;
		Date d15 = null;
		try {
			d0 = df.parse("11/5/2003 0:00:00.000");
			d1 = df.parse("11/5/2003 0:01:00.000");
			d2 = df.parse("11/5/2003 0:02:00.000");
			d3 = df.parse("11/5/2003 0:03:00.000");
			d4 = df.parse("11/5/2003 0:04:00.000");
			d5 = df.parse("11/5/2003 0:05:00.000");
			d6 = df.parse("11/5/2003 0:06:00.000");
			d7 = df.parse("11/5/2003 0:07:00.000");
			d8 = df.parse("11/5/2003 0:08:00.000");
			d9 = df.parse("11/5/2003 0:09:00.000");
			d10 = df.parse("11/5/2003 0:10:00.000");
			d11 = df.parse("11/5/2003 0:11:00.000");
			d12 = df.parse("11/5/2003 0:12:00.000");
			d13 = df.parse("11/5/2003 0:13:00.000");
			d14 = df.parse("11/5/2003 0:14:00.000");
			d15 = df.parse("11/5/2003 0:15:00.000");
		}
		catch(ParseException ex) {
			ex.printStackTrace();
		}
		
		demoSeries.add(new SimpleTimePeriod(d0, d0), 0);
		demoSeries.add(new SimpleTimePeriod(d1, d1), 40.674);
		demoSeries.add(new SimpleTimePeriod(d2, d2), 74.314);
		demoSeries.add(new SimpleTimePeriod(d3, d3), 95.106);
		demoSeries.add(new SimpleTimePeriod(d4, d4), 99.452);
		demoSeries.add(new SimpleTimePeriod(d5, d5), 86.603);
		demoSeries.add(new SimpleTimePeriod(d6, d6), 58.779);
		demoSeries.add(new SimpleTimePeriod(d7, d7), 20.791);
		demoSeries.add(new SimpleTimePeriod(d8, d8), -20.791);
		demoSeries.add(new SimpleTimePeriod(d9, d9), -58.779);
		demoSeries.add(new SimpleTimePeriod(d10, d10), -86.603);
		demoSeries.add(new SimpleTimePeriod(d11, d11), -99.452);
		demoSeries.add(new SimpleTimePeriod(d12, d12), -95.106);
		demoSeries.add(new SimpleTimePeriod(d13, d13), -74.314);
		demoSeries.add(new SimpleTimePeriod(d14, d14), -40.674);
		demoSeries.add(new SimpleTimePeriod(d15, d15), 0);
	}
	
	public TimePeriodValuesCollection getDataset() {
		return dataset;
	}

	public PlotEditor getPlotEditor() {
		return plotEditor;
	}
	
	public void setPlotEditor(PlotEditor plotEditor) {
		this.plotEditor = plotEditor;
		// Initialize a connection between the dataset and the table model, so that the table model can be accurately reflected
		this.getPlotEditor().getTable().setDataset(dataset);
	}

	public JPanel getPanel() {
		return panel;
	}

	public void setGraphName(String graphName) {
		chart.setTitle(graphName);
	}
	
	public JFreeChart getChart() {
		return chart;
	}
	
	public XYPlot getXYPlot() {
		return plot;
	}

	public void setProperty(GraphProperty label, String value) {
		if (label == GraphProperty.TITLE) {
			chart.setTitle(value);
		}
		else if (label == GraphProperty.X_LABEL) {
			chart.getXYPlot().getDomainAxis().setLabel(value);
		}
		else if (label == GraphProperty.Y_LABEL) {
			chart.getXYPlot().getRangeAxis().setLabel(value);
		}
	}
	
	public String getTitle() {
		try {
			return chart.getTitle().getText();
		}
		catch(NullPointerException ex) {
			return null;
		}
	}
	
    /** Listener for windowClosing action. */
    class WindowClosedAdapter extends WindowAdapter {
        public void windowClosed(WindowEvent e) {
        	//Window window = e.getWindow();
            //System.out.println("org.kepler.plotting.Plot$WindowClosedAdapter.windowClosed() : " + window.getName());
            
            _plots.remove(Plot.this);
        }
    }

	private static List<Plot> _plots = new ArrayList<Plot>();
	private JFreeChart chart;
	private XYPlot plot;
	private TimePeriodValues demoSeries;
	private TimePeriodValuesCollection dataset;
	private JPanel panel;
	private ChartPanel chartPanel;
	private PlotEditor plotEditor;
	private JButton clearButton;
}
