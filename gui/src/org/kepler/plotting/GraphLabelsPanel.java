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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.kepler.gui.PlotsEditorPanel;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 7, 2010
 * Time: 12:06:47 PM
 */

public class GraphLabelsPanel extends JPanel {
	public GraphLabelsPanel(PlotEditor plotEditor) {
		this.plotEditor = plotEditor;
		this.setLayout(new GridBagLayout());
		
		addRow(GraphProperty.TITLE, 0);
		addRow(GraphProperty.X_LABEL, 1);
		addRow(GraphProperty.Y_LABEL, 2);
	}

	private void addRow(final GraphProperty property, int row) {
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.ipady = 0;
		labelConstraints.insets = new Insets(0,10,0,10);
		JLabel label = new JLabel(property.toString());
		this.add(label, labelConstraints);
		
		GridBagConstraints textFieldConstraints = new GridBagConstraints();
		textFieldConstraints.gridx = 1;
		textFieldConstraints.gridy = row;
		final JTextField textField = new JTextField();
		textField.setText("test");
		Dimension oldSize = textField.getPreferredSize();
		textField.setText("");
		Dimension dimension = new Dimension();
		dimension.setSize(oldSize.getWidth() * 10, oldSize.getHeight());
		
		textField.setMinimumSize(dimension);
		textField.setPreferredSize(dimension);
		
		if (GraphProperty.TITLE == property) {
			if (getPlotEditor().getPlot() != null) {
				String graphName = generateGraphName();
				getPlotEditor().getPlot().setGraphName(graphName);
				textField.setText(graphName);
			}
		}
		
		textField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {}
			public void focusLost(FocusEvent e) {
				getPlotEditor().getPlot().setProperty(property, ((JTextField) e.getSource()).getText());
//				getPlotEditor().getPlot().setGraphName(((JTextField) e.getSource()).getText());
			}
		});
		this.add(textField, textFieldConstraints);
		textFields.put(property, textField);
		labels.put(property, label);
	}

	private String generateGraphName() {
		PlotsEditorPanel editorPanel = plotEditor.getEditorPanel();
		if (editorPanel == null) {
			return "ERROR";
		}
		int nextGraphId = editorPanel.getNextUnusedGraphId();
		return "Graph " + nextGraphId;
	}

	public void setActive(boolean active) {
		for (GraphProperty property : labels.keySet()) {			
			JComponent component = textFields.get(property);
			JLabel label = labels.get(property);
			component.setEnabled(active);
			label.setEnabled(active);
		}
	}
	
	public PlotEditor getPlotEditor() {
		return plotEditor;
	}
	
	private Map<GraphProperty, JLabel> labels = new HashMap<GraphProperty, JLabel>();
	private Map<GraphProperty, JComponent> textFields = new HashMap<GraphProperty, JComponent>();
	final private PlotEditor plotEditor;
}
