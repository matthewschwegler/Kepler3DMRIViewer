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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.kepler.gui.PlotsEditorPanel;

import ptolemy.vergil.toolbox.FigureAction;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 6, 2010
 * Time: 5:06:05 PM
 */

public class PlotEditor extends JPanel {
	
	public PlotEditor(PlotsEditorPanel plotsEditorPanel, Plot plot) {
		this.setEditorPanel(plotsEditorPanel);
		this.setPlot(plot);
		initialize();
	}
	
	public PlotEditor() {
		initialize();
	}
	
	private void initialize() {

		Border loweredEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		this.setBorder(loweredEtchedBorder);
		
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		deleteButton = new JButton("Delete");
		deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		final PlotEditor me = this;
		deleteButton.setAction(new FigureAction("delete") {
			@Override
			public void actionPerformed(ActionEvent e) {
				JPanel panel = me.getPlot().getPanel();
				panel.getParent().remove(panel);
				JButton clearButton = me.getPlot().getClearButton();
				clearButton.getParent().remove(clearButton);
				me.getEditorPanel().removePlot(me);
				me.getEditorPanel().fixGraphics();
			}
		});
		JPanel deleteButtonflowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		deleteButtonflowPanel.add(deleteButton);
		this.add(deleteButtonflowPanel);
		JPanel graphLabelsflowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

		graphLabelsPanel = new GraphLabelsPanel(this);
		graphLabelsflowPanel.add(graphLabelsPanel);
		this.add(graphLabelsflowPanel);
		table = new DataTable(this);
		JScrollPane scrollPane = new JScrollPane(table);

		this.add(scrollPane);
		this.add(Box.createRigidArea(new Dimension(0,20)));
		this.setMaximumSize(PlottingConstants.MAX_SIZE);	
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(new PlotsEditorPanel(null, "title"));
//		frame.getContentPane().add(new PlotEditor());
		frame.pack();
		frame.setVisible(true);
	}

	public void setEditorPanel(PlotsEditorPanel panel) {
		this.editorPanel = panel;
	}

	public PlotsEditorPanel getEditorPanel() {
		return editorPanel;
	}

	public void setActive(boolean active) {
		this.active = active;
		_setActive(active);
	}

	private void _setActive(boolean active) {
		if (active) {
			
		}
		else {
//			this.setBackground(Color.BLUE);
			tbp.setActive(false);
			deleteButton.setEnabled(false);
			graphLabelsPanel.setActive(false);
			table.setActive(false);
		}
	}

	public boolean isActive() {
		return active;
	}

	public void setPlot(Plot plot) {
		this.plot = plot;
	}
	
	public Plot getPlot() {
		return plot;
	}

	public DataTable getTable() {
		return table;
	}

	private PlotsEditorPanel editorPanel;
	private boolean active = true;
	
	private JButton deleteButton;
	private GraphLabelsPanel graphLabelsPanel;
	private DataTable table;
	private TableButtonPanel tbp;
	private Plot plot;
}
