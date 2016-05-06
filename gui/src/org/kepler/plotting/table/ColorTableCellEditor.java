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

package org.kepler.plotting.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 7, 2010
 * Time: 3:12:37 PM
 */

public class ColorTableCellEditor implements TableCellEditor {

	public ColorTableCellEditor() {
		this.color = null;
		this.chooser = null;
		this.frame = null;
		this.listeners = new ArrayList<CellEditorListener>();
		this.valid = false;
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		JPanel mainPanel = new JPanel();
		JPanel subPanel = new JPanel();
		frame = new JFrame();
		frame.addWindowListener(new WindowListener() {
			public void windowClosing(WindowEvent e) {
				notifyListenersOfCancellation();
			}
			public void windowOpened(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowActivated(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
		});
		chooser = new JColorChooser((Color) value);
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.LINE_AXIS));
		
		mainPanel.add(chooser);
		
		JButton confirmButton = new JButton("Set");
		confirmButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Mouse click!
				valid = true;
				frame.setVisible(false);
				color = chooser.getColor();
				notifyListenersOfCompletion();
			}
		});
		subPanel.add(confirmButton);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
				notifyListenersOfCancellation();
			}
		});
		subPanel.add(confirmButton);
		subPanel.add(cancelButton);
		mainPanel.add(subPanel);
		frame.add(mainPanel);
		frame.pack();
		frame.setVisible(true);
		valid = false;
		return new JLabel("(edit)");
	}

	private void notifyListenersOfCompletion() {
		List<CellEditorListener> myListeners = new ArrayList<CellEditorListener>(listeners);
		for (CellEditorListener listener : myListeners) {
			listener.editingStopped(new ChangeEvent(chooser));
		}
	}

	private void notifyListenersOfCancellation() {
		List<CellEditorListener> myListeners = new ArrayList<CellEditorListener>(listeners);
		for (CellEditorListener listener : myListeners) {
			listener.editingCanceled(new ChangeEvent(chooser));
		}		
	}

	public Object getCellEditorValue() {
//		color = new Color(chooser.getColor());
		return color;
	}

	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	public boolean shouldSelectCell(EventObject anEvent) {
		return false;
	}

	public boolean stopCellEditing() {
		// Destroy color picker window
		hideChooser();
		notifyListenersOfCancellation();
		return valid;
	}

	private void hideChooser() {
		frame.setVisible(false);
	}

	public void cancelCellEditing() {
		hideChooser();
	}

	public void addCellEditorListener(CellEditorListener l) {
		synchronized(listeners) {
			listeners.add(l);
		}
	}

	public void removeCellEditorListener(CellEditorListener l) {
		synchronized(listeners) {
			listeners.remove(l);
		}
	}
	
	private JFrame frame;
	private JColorChooser chooser;
	private Color color;
	private final List<CellEditorListener> listeners;
	private boolean valid;
}
