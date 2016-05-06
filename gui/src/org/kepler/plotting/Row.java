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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.TypedIOPort;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 7, 2010
 * Time: 12:42:55 PM
 */

public class Row {
	public Row(String name, String x, String y, PointType pointType, Color color, PlotEditor plotEditor) {
		this.setAttribute("Name", name);
		this.setAttribute("X", x);
		this.setAttribute("Y", y);
		this.setAttribute("Point Type", pointType);
		this.setAttribute("Color", color);
		setPlotEditor(plotEditor);
	}

	private void setAttribute(String name, Object object) {
		attributes.put(name, object);
	}

	public void setValueByIndex(int index, Object value) {
//		System.out.println("Putting '" + value + "' as column: " + COLUMN_NAMES[index]);
		attributes.put(COLUMN_NAMES[index], value);
	}

	public void setCandidateValue(int columnIndex, Object aValue) {
		this.candidateIndex = columnIndex;
		this.candidateValue = aValue;
	}
	
	public void rollback() {
		this.candidateIndex = -1;
	}

	public void commit() {
		if (candidateIndex >= 0) {
//			System.out.println("Committing! Index = " + candidateIndex + ", value = " + candidateValue);
			this.setValueByIndex(candidateIndex, candidateValue);
		}
	}

	public int getCandidateIndex() {
		return candidateIndex;
	}

	public Object getCandidateValue() {
		return candidateValue;
	}

	public void setPlotEditor(PlotEditor plotEditor) {
		this.plotEditor = plotEditor;
	}

	public PlotEditor getPlotEditor() {
		return plotEditor;
	}

	public Color getColor() {
		return (Color) attributes.get("Color");
	}

	public String getSensorName() {
		return (String) attributes.get("Name");
	}

	public PointType getPointType() {
		return (PointType) attributes.get("Point Type");
	}

	public void addDeletionListener(DeletionListener listener, int priority) {
		listeners.put(listener, priority);
		if (!listenersByPriority.containsKey(priority)) {
			listenersByPriority.put(priority, new ArrayList<DeletionListener>());
		}
		listenersByPriority.get(priority).add(listener);
	}
	
	public void addDeletionListener(DeletionListener listener) {
		addDeletionListener(listener, DEFAULT_PRIORITY);
	}
	
	public void removeDeletionListener(DeletionListener listener) {
		Integer priority = listeners.get(listener);
		listenersByPriority.get(priority).remove(listener);
		listeners.remove(listener);
		
	}

	public void notifyDeletionListeners() {
		// Evaluating in priority order
		Set<Integer> prioritiesSet = listenersByPriority.keySet();
		List<Integer> priorities = new ArrayList<Integer>(prioritiesSet);
		Collections.sort(priorities);
		for (Integer priority : priorities) {
			log.debug("Evaluating priority: " + priority);
			for (DeletionListener listener : listenersByPriority.get(priority)) {
				listener.delete(this.getSensorName());
			}
		}		
	}

	public boolean containsListener(DeletionListener listener) {
		return listeners.containsKey(listener);
	}

	public void setDataPort(TypedIOPort dataPort) {
		this.dataPort = dataPort;
	}

	public TypedIOPort getDataPort() {
		return dataPort;
	}

	public static boolean isEditableColumns(int i) {
		String columnName = COLUMN_NAMES[i];
		return !"Y".equals(columnName) && !"X".equals(columnName);
	}

	private Map<String, Object> attributes = new HashMap<String, Object>();
	
	public Object getValueByIndex(int index) {
		return attributes.get(COLUMN_NAMES[index]);
	}
		
	public static int getColumnCount() {
		return COLUMN_NAMES.length;
	}
	
	public static String getColumnName(int i) {
		try {
			return COLUMN_NAMES[i];
		}
		catch(ArrayIndexOutOfBoundsException ex) {
			throw new IllegalArgumentException("This column does not exist");
		}
	}
	
	public static Class getColumnClass(int i) {
		try {
			return COLUMN_CLASSES[i];
		}
		catch(ArrayIndexOutOfBoundsException ex) {
			throw new IllegalArgumentException("This column does not exist");
		}
	}
	
	private static final Log log = LogFactory.getLog(Row.class.getName());
	
	private static String[] COLUMN_NAMES = new String[] {"Name", "X", "Y", "Point Type", "Color"};
	private static Class[] COLUMN_CLASSES = new Class[] {String.class, String.class, String.class, PointType.class, Color.class};
	public static int[] COLUMN_WIDTHS = new int[] {120, 10, 120, 10, 10};

	private int candidateIndex;
	private Object candidateValue;
	private PlotEditor plotEditor;
	private Map<DeletionListener, Integer> listeners = new HashMap<DeletionListener, Integer>();
	private Map<Integer, List<DeletionListener>> listenersByPriority = new HashMap<Integer, List<DeletionListener>>();
	private static final int DEFAULT_PRIORITY = 0;
	private boolean adHocMode = false;
	private TypedIOPort dataPort;
}
