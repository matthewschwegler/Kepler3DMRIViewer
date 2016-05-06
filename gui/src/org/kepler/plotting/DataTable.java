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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jfree.data.general.Dataset;
import org.kepler.gui.PlotsEditorPanel;
import org.kepler.plotting.table.ColorTableCellEditor;
import org.kepler.plotting.table.ColorTableCellRenderer;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.vergil.toolbox.PtolemyTransferable;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 7, 2010
 * Time: 12:31:50 PM
 */

public class DataTable extends JTable implements ActionListener {
	
	private DataTable() {
		DataTable.Model model = new DataTable.Model();
		model.setTable(this);
		this.setModel(model);
		this.setTransferHandler(new MyTransferHandler());
		// insertDemoData();
		
		this.addMouseListener(new java.awt.event.MouseAdapter() {
			// os X
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jTableShowContextMenu(evt);
			}
			// Windows
			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jTableShowContextMenu(evt);
			}
		});
		popupMenu.add(removeRowMenuItem);
		removeRowMenuItem.addActionListener(this);
		removeRowMenuItem.setActionCommand(REMOVE_ROW);
		
		// adjust column widths
		int numCols = getColumnCount();
		for (int i=0; i<numCols; i++){
			getColumnModel().getColumn(i).setPreferredWidth(Row.COLUMN_WIDTHS[i]);
		}
	}
	
	private void jTableShowContextMenu(java.awt.event.MouseEvent mouseEvent) {
		DataTable dataTable = getPlotEditor().getTable();
		int[] selectedRows = dataTable.getSelectedRows();
		int clickedRow = dataTable.rowAtPoint(mouseEvent.getPoint());
		
		boolean clickWasOnASelectedRow = false;
		for (int i = 0; i < selectedRows.length; i++) {
			if (clickedRow == selectedRows[i]) {
				clickWasOnASelectedRow = true;
				break;
			}
		}
		
		if (selectedRows.length > 0 && clickWasOnASelectedRow && 
				(mouseEvent.isPopupTrigger() || mouseEvent.isControlDown())){
			popupMenu.show(mouseEvent.getComponent(), mouseEvent
					.getX(), mouseEvent.getY());
		}
	}
	
	public DataTable(PlotEditor plotEditor) {
		this();
		setPlotEditor(plotEditor);
	}
	
	public void add(Row row) {
		((Model) this.getModel()).addRow(row);
	}
	
	public void removeRow(Row row) {
		((Model) this.getModel()).removeRow(row);
	}
		
	public Row getRow(int index) {
		return ((Model) this.getModel()).getRow(index);
	}
	
	public void update() {
		((Model) this.getModel()).fireTableDataChanged();
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		Container parent = getParent();
		if (parent != null) {
			return parent.getHeight() > getPreferredSize().height;
		}
		else {
			return false;
		}
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		if (this.getColumnClass(column) == Color.class) {
			return new ColorTableCellRenderer();
		}
		else {
			return super.getCellRenderer(row, column);
		}
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (this.getColumnClass(column) == Color.class) {
			return new ColorTableCellEditor();
		}
		else if (this.getColumnClass(column) == PointType.class) {
			if (pointTypeChoices == null) {
				pointTypeChoices = new JComboBox();
				for (PointType pointType : PointType.values()) {
					pointTypeChoices.addItem(pointType);
				}
			}
			return new DefaultCellEditor(pointTypeChoices);
		}
		else {
			return super.getCellEditor(row, column);
		}
	}
	
	private JComboBox pointTypeChoices = null;

	private void insertDemoData() {
		Model model = (Model) this.getModel();
		model.addRow(new Row("sensor0", "time", "sensor0.data", PointType.CIRCLE, Color.BLACK, this.getPlotEditor()));
		model.addRow(new Row("sensor1", "time", "sensor1.data", PointType.SQUARE, Color.BLUE, this.getPlotEditor()));
		model.addRow(new Row("sensor2", "time", "sensor2.data", PointType.DIAMOND, Color.GREEN, this.getPlotEditor()));
	}

	public void setActive(boolean active) {
		if (!active) {
			// insertDemoData();
		}
	}

	public PlotEditor getPlotEditor() {
		return plotEditor;
	}

	private void setPlotEditor(PlotEditor plotEditor) {
		this.plotEditor = plotEditor;
	}

	public void setDataset(Dataset dataset) {
		((Model) this.getModel()).setDataset(dataset);
	}

	public PlottingControllerFactory getPlottingControllerFactory() {
		return plottingControllerFactory;
	}

	public PlottingController getPlottingController() {
		return plottingController;
	}

	public void actionPerformed(ActionEvent ae) {

		DataTable table = getPlotEditor().getTable();
		int[] selectedRows = table.getSelectedRows();
		List<Row> rows = new ArrayList<Row>();
		for (int selectedRow : selectedRows) {
			rows.add(table.getRow(selectedRow));
		}

		for (Row row : rows) {
			row.notifyDeletionListeners();
			table.removeRow(row);
		}
		table.update();
	}

	private PlotEditor plotEditor = null;
	private static PlottingControllerFactory plottingControllerFactory;

	private class Model extends AbstractTableModel {

		public void addRow(Row row) {
			rows.add(row);
			initializeController();
			getPlottingController().initialize(row, getPlotEditor().getPlot());
			this.getTable().setSize(new Dimension(2, 2));
		}
		
//		public void removeIndividualRow(Row row) {
//			removeRow(row);
//			SwingUtilities.invokeLater(new Runnable() {
//				public void run() {
//					fireTableDataChanged();
//				}
//			});
//		}
		
		private void removeRow(Row row) {
			rows.remove(row);
			this.getTable().setSize(new Dimension(2, 2));
		}
		
		public Row getRow(int index) {
			return rows.get(index);
		}
		
		public int getRowCount() {
			return rows.size();
		}

		public int getColumnCount() {
			return Row.getColumnCount();
		}

		public String getColumnName(int columnIndex) {
			return Row.getColumnName(columnIndex);
		}

		public Class<?> getColumnClass(int columnIndex) {
			return Row.getColumnClass(columnIndex);
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return Row.isEditableColumns(columnIndex);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			return rows.get(rowIndex).getValueByIndex(columnIndex);
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			
			rows.get(rowIndex).setCandidateValue(columnIndex, aValue);
			// rows.get(rowIndex).setValueByIndex(columnIndex, aValue);
			
			UpdateType updateType = null;
			if (columnIndex == 0) {
				updateType = UpdateType.SENSOR_NAME;
			}
			else if (columnIndex == 1) {				
				updateType = UpdateType.X_AXIS;
			}
			else if (columnIndex == 2) {
				updateType = UpdateType.Y_AXIS;
			}
			else if (columnIndex == 3) {
				updateType = UpdateType.POINT_TYPE;
			}
			else if (columnIndex == 4) {
				updateType = UpdateType.COLOR;
			}
			
			updateSensor(rows.get(rowIndex), updateType);
		}

		private void updateSensor(Row row, UpdateType updateType) {
			initializeController();
			getPlottingController().register(row, updateType, getPlotEditor().getPlot());
		}

		public void addTableModelListener(TableModelListener l) {
			listeners.add(l);
		}

		public void removeTableModelListener(TableModelListener l) {
			listeners.remove(l);
		}

		public void setDataset(Dataset dataset) {
			this.dataset = dataset;
		}

		public void setTable(DataTable table) {
			this.table = table;
		}
		
		public DataTable getTable() {
			return table;
		}

		private List<Row> rows = new ArrayList<Row>();		
		private List<TableModelListener> listeners = new ArrayList<TableModelListener>();
		private Dataset dataset = null;
		private DataTable table;
	}

	private void initializeController() {
		if (plottingController == null) {
			plottingController = PlotsEditorPanel.getPlottingControllerFactory().
				getPlottingController(getPlotEditor().getEditorPanel().getParentFrame());
		}
	}
	
	private PlottingController plottingController = null;

	private class MyTransferHandler extends TransferHandler {
		
		public boolean canImport(JComponent component, DataFlavor[] flavors) {
			for (DataFlavor flavor : flavors) {
				if (PtolemyTransferable.namedObjFlavor.equals(flavor)) {
					return true;
				}
			}
			return false;
		}
		
		public boolean importData(JComponent component, Transferable tr) {

			if (canImport(component, tr.getTransferDataFlavors())) {
				try {
					List droppedObjects = (List) tr.getTransferData(PtolemyTransferable.namedObjFlavor);
					for (Object droppedObject : droppedObjects) {
						TypedIOPort sensorDataPort = null;
						if (droppedObject instanceof TypedIOPort) {
							TypedIOPort port = (TypedIOPort) droppedObject;
							if (isPlottablePort(port)) {
								sensorDataPort = port;
							}
						}
						
						if (sensorDataPort != null) {
							// This is a plottable item
							String sensorName = sensorDataPort.getFullName();
							Row row = new Row(sensorName, "time", sensorName, 
									PlottingConstants.DEFAULT_PLOT_ITEM_SHAPE, 
									PlottingConstants.DEFAULT_PLOT_ITEM_COLOR, getPlotEditor());
							row.setDataPort(sensorDataPort);
							add(row);
							getPlottingController().updatePlotManually(row);
						}
					}
					return true;
				}
				catch(UnsupportedFlavorException e) {
					e.printStackTrace();
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
		
		public int getSourceActions(JComponent component) {
			return COPY;
		}
		
		protected void exportDone(JComponent component, Transferable tr, int action) {
			// System.out.println("Export done!: " + component);
		}		
	}
		
	private static boolean isPlottablePort(TypedIOPort port) {
		Type type = port.getType();
		return type.isCompatible(PLOTTABLE_TYPE);
	}
	
	private static final Type PLOTTABLE_TYPE = new ArrayType(BaseType.INT, 2);
	
	private JPopupMenu popupMenu = new JPopupMenu();
	private static final String REMOVE_ROW = "Delete";
	private JMenuItem removeRowMenuItem = new JMenuItem(REMOVE_ROW);
	
	
}
