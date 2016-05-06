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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.kepler.plotting.Plot;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/** A tab pane to display sensor data.
 * 
 * @author Daniel Crawl
 * @version $Id: PlotsPanel.java 24730 2010-06-03 23:45:10Z crawl $
 */
public class PlotsPanel extends JPanel implements TabPane 
{
    /** Construct a new PlotsPanel in a frame with a specific title. */
    public PlotsPanel(TableauFrame parent, String title)
    {
        super();
        _title = title;
        _frame = parent;
        setBackground(TabManager.BGCOLOR);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.scrollRectToVisible(getVisibleRect());
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		scrollPane = new JScrollPane(panel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}

	/** Get the container frame. */
    public TableauFrame getParentFrame()
    {
        return _frame;
    }

    /** Get the name of the tab. */
    public String getTabName()
    {
        return _title;
    }

    /** Initialize the contents of the tab. */
    public void initializeTab() throws Exception
    {

//		System.out.println("initializeTab() PlotsPanel");
		this.add(scrollPane);
    }
	
	public Plot addGraph() {
		final JComponent me = panel;
		final Plot plot = createPlot();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = GridBagConstraints.RELATIVE;
				JPanel chart = plot.getPanel();
				chart.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
				me.add(chart, gbc);
				DoubleCheckButton clearButton = new DoubleCheckButton("Clear", "Are you sure you want to clear this graph?");
				clearButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						TimePeriodValuesCollection dataset = plot.getDataset();
						for (int i = 0; i < dataset.getSeriesCount(); i++) {
							// Evil black magic
							// This is required to remove accumulated data
							// while still maintaining the data structures
							// so future points can be plotted for these
							// lines.
							TimePeriodValues series = dataset.getSeries(i);
							try {
								Field dataField = series.getClass().getDeclaredField("data");
								dataField.setAccessible(true);
								List dataList = (List) dataField.get(series);
								dataList.clear();
								Method method = series.getClass().getDeclaredMethod("recalculateBounds");
								method.setAccessible(true);
								method.invoke(series);
							}
							catch(InvocationTargetException e1) {
								e1.printStackTrace();
							}
							catch(NoSuchMethodException e1) {
								e1.printStackTrace();
							}
							catch(IllegalAccessException e1) {
								e1.printStackTrace();
							}
							catch(NoSuchFieldException e1) {
								e1.printStackTrace();
							}
							series.fireSeriesChanged(); // Can do this normally at least.
						}
					}
				});
				plot.setClearButton(clearButton);
				me.add(clearButton, gbc);
				me.repaint();
				me.revalidate();
			}
		});
		return plot;
	}

	private Plot createPlot() {
		Plot plot = new Plot(_frame);
		_plots.add(plot);
		return plot;
	}

//	private boolean isEmpty() {
//		return _plots.isEmpty();
//	}
	
	private final List<Plot> _plots = new ArrayList<Plot>();

	/** Set the frame for this tab. */
    public void setParentFrame(TableauFrame parent)
    {
        _frame = parent;
    }
    
    /** A factory to create PlotsPanels. */
    public static class ViewerFactory extends TabPaneFactory
    {
        /**
         * Create a factory with the given name and container.
         * 
         *@param container
         *            The container.
         *@param name
         *            The name of the entity.
         *@exception IllegalActionException
         *                If the container is incompatible with this attribute.
         *@exception NameDuplicationException
         *                If the name coincides with an attribute already in the
         *                container.
         */
        public ViewerFactory(NamedObj container, String name)
                throws IllegalActionException, NameDuplicationException {
            super(container, name);
        }

        /** Create a tab pane in a frame. */
        public TabPane createTabPane(TableauFrame parent) {
            return new PlotsPanel(parent, "Plot Viewer");
        }
    }
    
    private TableauFrame _frame;
    private String _title;
	private JScrollPane scrollPane;
	private JPanel panel;
    
	private class DoubleCheckButton extends JButton {
		public DoubleCheckButton(String initialText, String confirmText) {
			super(initialText);
			this.confirmText = confirmText;
		}


		@Override
		protected void fireActionPerformed(ActionEvent event) {
			int response = JOptionPane.showConfirmDialog(null, confirmText, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.YES_OPTION) {
				super.fireActionPerformed(event);				
			}
		}
		
		private String confirmText = null;
	}
}
