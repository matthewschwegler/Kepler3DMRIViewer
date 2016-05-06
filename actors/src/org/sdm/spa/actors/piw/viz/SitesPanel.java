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

package org.sdm.spa.actors.piw.viz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * @author xiaowen
 * 
 */
public class SitesPanel extends JPanel {

	private static final long serialVersionUID = 3977297702998454328L;

	private final SequenceCollection _sequenceCollection;
	private final SiteCollection _siteCollection;
	private final ActionListener _listenerSiteChange;

	public SitesPanel(SequenceCollection sequenceCollection,
			SiteCollection siteCollection, ActionListener listenerSiteChange) {
		this._sequenceCollection = sequenceCollection;
		this._siteCollection = siteCollection;
		this._listenerSiteChange = listenerSiteChange;

		TableModelFrequency tableModel = new TableModelFrequency();
		JTable table = new JTable(tableModel);
		table.setDefaultRenderer(SiteCollection.Site.class, new SiteRenderer());
		table.setDefaultEditor(SiteCollection.Site.class, new SiteEditor());
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().getColumn(1).setMaxWidth(75);
		table.getColumnModel().getColumn(2).setMaxWidth(75);
		table.setRowHeight(18);

		// Pull down menu containing selection actions.
		JComboBox comboBox = new JComboBox(new String[] { "Select ...",
				"Select All", "Deselect All", "Select Marked",
				"Deselect Marked" });
		comboBox.addActionListener(new ListenerComboBoxSelect(table));

		this.setLayout(new BorderLayout());
		this.add(comboBox, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setWheelScrollingEnabled(true);
		this.add(scrollPane, BorderLayout.CENTER);
		this.setPreferredSize(new Dimension(50, 50));
	}

	private class TableModelFrequency extends AbstractTableModel {
		private static final long serialVersionUID = 3257850999766069561L;
		private String[] columnNames = { "TFBS", "Shown", "Frequency" };

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return _siteCollection.size();
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {
			if (0 == col) {
				return _siteCollection.getSite(row).name;
			} else if (1 == col) {
				return _siteCollection.getSite(row);
			} else if (2 == col) {
				return new Double(_siteCollection.getSite(row).frequency);
			}
			return null;
		}

		public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		public boolean isCellEditable(int row, int col) {
			return 1 == col;
		}

		public void setValueAt(Object value, int row, int col) {
			if (1 == col) {
				_siteCollection.getSite(row).selected = ((Boolean) value)
						.booleanValue();
				_listenerSiteChange
						.actionPerformed(new ActionEvent(this, 0, ""));
			}
		}
	}

	private class SiteRenderer extends JPanel implements TableCellRenderer {

		private static final long serialVersionUID = 3258410646906615096L;

		Border unselectedBorder = null;
		Border selectedBorder = null;
		JCheckBox checkbox = null;

		public SiteRenderer() {
			setOpaque(true);
		}

		public Component red() {
			this.setBackground(Color.RED);
			this.setForeground(Color.RED);
			return this;
		}

		public Component getTableCellRendererComponent(JTable table, Object s,
				boolean isSelected, boolean hasFocus, int row, int column) {

			SiteCollection.Site site = _siteCollection.getSite(row);

			if (null == checkbox) {
				checkbox = new JCheckBox();
				checkbox.setBorder(BorderFactory.createEmptyBorder());
				this.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
				this.add(checkbox);
			}

			checkbox.setSelected(site.selected);

			setBackground(site.color);
			if (isSelected) {
				if (selectedBorder == null) {
					selectedBorder = BorderFactory.createMatteBorder(2, 3, 2,
							3, table.getSelectionBackground());
				}
				setBorder(selectedBorder);
			} else {
				if (unselectedBorder == null) {
					unselectedBorder = BorderFactory.createMatteBorder(2, 3, 2,
							3, table.getBackground());
				}
				setBorder(unselectedBorder);
			}

			return this;
		}
	}

	private class SiteEditor extends AbstractCellEditor implements
			TableCellEditor {

		private static final long serialVersionUID = 3256727290376433720L;
		private SiteRenderer _siteRenderer;

		/**
		 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable,
		 *      java.lang.Object, boolean, int, int)
		 */
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {

			if (null == _siteRenderer) {
				_siteRenderer = new SiteRenderer();
			}

			Component component = _siteRenderer.getTableCellRendererComponent(
					table, value, true, true, row, column);

			_siteRenderer.checkbox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SiteEditor.this.stopCellEditing();
				}
			});

			return component;
		}

		/**
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		public Object getCellEditorValue() {
			return new Boolean(_siteRenderer.checkbox.isSelected());
		}
	}

	private class ListenerComboBoxSelect implements ActionListener {

		private final JTable _table;

		public ListenerComboBoxSelect(JTable table) {
			this._table = table;
		}

		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			int index = cb.getSelectedIndex();

			if (1 == index) {
				// Selected all.
				for (int i = 0; i < _siteCollection.size(); i++) {
					_siteCollection.getSite(i).selected = true;
				}
			} else if (2 == index) {
				// Deselect all.
				for (int i = 0; i < _siteCollection.size(); i++) {
					_siteCollection.getSite(i).selected = false;
				}
			} else if (3 == index) {
				// Select marked.
				int[] selected = _table.getSelectedRows();
				for (int i = 0; i < selected.length; i++) {
					_siteCollection.getSite(selected[i]).selected = true;
				}
			} else if (4 == index) {
				// Deselect marked.
				int[] selected = _table.getSelectedRows();
				for (int i = 0; i < selected.length; i++) {
					_siteCollection.getSite(selected[i]).selected = false;
				}
			}
			cb.setSelectedIndex(0);
			_listenerSiteChange.actionPerformed(new ActionEvent(this, 0, ""));
		}
	}

}