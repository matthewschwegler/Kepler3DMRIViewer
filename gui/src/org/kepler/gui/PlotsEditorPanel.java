/* A tab pane to display sensor data. 
 * 
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2010-06-03 16:45:10 -0700 (Thu, 03 Jun 2010) $' 
 * '$Revision: 24730 $'
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

package org.kepler.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.plotting.Plot;
import org.kepler.plotting.PlotEditor;
import org.kepler.plotting.PlottingControllerFactory;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.toolbox.FigureAction;

public class PlotsEditorPanel extends JPanel implements TabPane 
{
	private static final Log log = LogFactory.getLog(PlotsEditorPanel.class.getName());
	
    public PlotsEditorPanel(TableauFrame parent, String title)
    {
        super();
        _title = title;
        _frame = parent;
        setBackground(TabManager.BGCOLOR);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		plotsPanel = new JPanel();
		plotsPanel.setLayout(new BoxLayout(plotsPanel, BoxLayout.PAGE_AXIS));
		
		fixGraphics();
		
		JScrollPane scrollPane = new JScrollPane(plotsPanel);
		
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(scrollPane, BorderLayout.CENTER);
		JButton addPlotButton = createAddPlotButton();
		this.add(addPlotButton, BorderLayout.PAGE_END);
		
		// This thread will try to wait until the PlotsPanel is available
		// before adding a plot to it. If a plot is added before the
		// applicable tab pane is available, an NPE will be triggered.
		// The thread will poll for tab pane setup completion every 100 ms,
		// up to a maximum of 10 seconds (100 iterations). If after 10 seconds
		// the tab pane is not available, something has probably gone wrong,
		// and further attempts would do no good.
		new Thread(new Runnable() {
			private void pause(long l) {
				try {
					Thread.sleep(l);
				}
				catch(InterruptedException ignored) {}
			}
			
			public void run() {
				int maxIterations = 100;
				for (int i = 0; i < maxIterations; i++) {
					log.debug("PlotsEditorPanel thread waiting " +
						"for TabPane to become available...");
					pause(100);
					if (canAddPlot()) {
						log.debug("PlotsEditorPanel thread got TabPane, adding Plot.");
						addPlot();
						return;
					}
				}
			}
		}).start();		
    }

	public void fixGraphics() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				plotsPanel.repaint();
				plotsPanel.revalidate();
				plotsPanel.repaint();
			}
		});		
	}

	private JButton createAddPlotButton() {
		JButton button = new JButton();
		button.setText("New Plot");
		final PlotsEditorPanel me = this; 
		FigureAction action = new FigureAction("add plot") {
			@Override
			public void actionPerformed(ActionEvent e) {
				me.addPlot();
			}
		};
		button.setAction(action);
		return button;
	}

	/** Get the container frame. */
    public TableauFrame getParentFrame()
    {
        return _frame;
    }

    /** Get the name of the tab. */
    public String getTabName() {
		return _title;
	}

    /** Initialize the contents of the tab. */
    public void initializeTab() throws Exception {
		
    }
	
	private boolean canAddPlot() {
		TabPane pane = TabManager.getInstance().getTab(this.getParentFrame(), "Plot Viewer");
		return pane != null;
	}
	
	public void addPlot() {
		TabPane pane = TabManager.getInstance().getTab(this.getParentFrame(), "Plot Viewer");
		Plot plot = ((PlotsPanel) pane).addGraph();

		addPlotEditor(true, true, plot);
	}
	
	public void addPlotEditor(boolean enabled, boolean refreshGraphics, Plot plot) {
		PlotEditor editor = new PlotEditor(this, plot);
		
		if (plot != null) {
			plot.setPlotEditor(editor);
		}
		editor.setActive(enabled);
		plotsPanel.add(editor);
		
		if (refreshGraphics) {
			fixGraphics();
		}
	}

	/** Set the frame for this tab. */
    public void setParentFrame(TableauFrame parent)
    {
        _frame = parent;
    }

	public void removePlot(final PlotEditor plotEditor) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				plotsPanel.remove(plotEditor);
			}
		});
	}

	public synchronized int getNextUnusedGraphId() {
		return graphId++;
	}

	public static void setPlottingControllerFactory(PlottingControllerFactory plottingControllerFactory) {
		PlotsEditorPanel.plottingControllerFactory = plottingControllerFactory;
	}

	public static PlottingControllerFactory getPlottingControllerFactory() {
		return plottingControllerFactory;
	}

	public static class Factory extends TabPaneFactory {
		public Factory(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}
		
		public TabPane createTabPane(TableauFrame parent) {
			return new PlotsEditorPanel(parent, "Plot Designer");
		}
	}
	
	private JPanel plotsPanel;
	private static PlottingControllerFactory plottingControllerFactory;
    private TableauFrame _frame;
    private String _title;
	private int graphId = 1;
    
}
