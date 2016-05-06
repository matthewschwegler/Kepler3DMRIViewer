/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

package org.kepler.objectmanager.cache.browser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.kepler.gui.TabPane;
import org.kepler.gui.TabPaneFactory;
import org.kepler.objectmanager.cache.CacheManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

public class CacheViewerTabPane extends JPanel implements TabPane {
	
	TableauFrame _frame;
	String _tabName;
	
	JButton _refreshButton;
	JTable _cacheList;
	
	public CacheViewerTabPane () {}

	/* (non-Javadoc)
	 * @see org.kepler.gui.TabPane#getParentFrame()
	 */
	public TableauFrame getParentFrame() {
		return _frame;
	}

	/* (non-Javadoc)
	 * @see org.kepler.gui.TabPane#setParentFrame(ptolemy.actor.gui.TableauFrame)
	 */
	public void setParentFrame(TableauFrame parent) {
		_frame = parent;
	}

	/* (non-Javadoc)
	 * @see org.kepler.gui.TabPane#getTabName()
	 */
	public String getTabName() {
		return _tabName;
	}
	
	public void setTabName(String name) {
		_tabName = name;
	}

	/* (non-Javadoc)
	 * @see org.kepler.gui.TabPane#initializeTab()
	 */
	public void initializeTab() throws Exception {
		setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
		_refreshButton = new JButton("Refresh");
		_refreshButton.addActionListener(new RefreshButtonActionListener());
		add( _refreshButton );
		
		initCacheListTable();
	}
	
	private void initCacheListTable() {
		Vector<String> columnNames = new Vector<String>();
		columnNames.addElement("Name");
		columnNames.addElement("LSID");
		columnNames.addElement("Cache File");
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		_cacheList = new JTable( new DefaultTableModel(data,columnNames) );
		JScrollPane cacheListSP = new JScrollPane(_cacheList);
		add( cacheListSP );
	}
	
	private class RefreshButtonActionListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			// System.out.println("Refresh");
			_cacheList.setModel(buildCacheList());
		}
	}
	
	private DefaultTableModel buildCacheList() {
		Vector<String> columnNames = new Vector<String>();
		columnNames.addElement("Name");
		columnNames.addElement("LSID");
		columnNames.addElement("Cache File");
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		try {
			CacheManager cm = CacheManager.getInstance();
			cm.showDB();
			//data = cm.selectAllFiles();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return new DefaultTableModel( data, columnNames );
	}
	
	/**
	 * Query the database for all names, lsids and files.
	 * @return Vector<Vector<String>>
	 * 			A Vector containing three Vectors, names lsids files
	 *
	public Vector<Vector<String>> selectAllFiles() {
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		int row = 0;
		data.addElement( new Vector<String>(3) );
		data.elementAt(row).addElement("testname");
		data.elementAt(row).addElement("testlsid");
		data.elementAt(row).addElement("testfile");
		row++;
		try {
			ResultSet rs = selectAll.executeQuery();
			while (rs.next()) {
				data.addElement( new Vector<String>(3) );
				data.elementAt(row).addElement( rs.getString("name") );
				data.elementAt(row).addElement( rs.getString("lsid") );
				data.elementAt(row).addElement( rs.getString("file") );
				row++;
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return data;
	} */

	/**
	 * A factory that creates the library panel for the editors.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends TabPaneFactory {
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
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a library pane that displays the given library of actors.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public TabPane createTabPane(TableauFrame parent) {
			CacheViewerTabPane cv = new CacheViewerTabPane();
			cv.setTabName( this.getName() );
			return cv;
		}
	}
	
	

}
