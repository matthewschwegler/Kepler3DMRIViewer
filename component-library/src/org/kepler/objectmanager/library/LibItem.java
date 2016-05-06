/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 15:16:13 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31483 $'
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

package org.kepler.objectmanager.library;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * The LibItem class represents one node of the Component Library Tree.
 * The data in this class corresponds directly to the columns of the
 * Library_Index table as well as the data stored in auxiliary tables
 * through foreign key references.
 * 
 * @author Aaron Schultz
 * 
 */
public class LibItem {

	private static final Log log = LogFactory.getLog(LibItem.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * the name of the table in the database used to store attributes
	 */
	public static final String LIBRARY_ATTRIBUTES_TABLE_NAME = "LIBRARY_ATTRIBUTES";
	
	/**
	 * Library Index ID
	 */
	private int _liid;

	/**
	 * The parent liid
	 */
	private Integer _parent;

	/**
	 * preorder tree traversal left integer
	 */
	private int _left;

	/**
	 * preorder tree traversal right integer
	 */
	private int _right;

	/**
	 * preorder tree traversal level
	 */
	private int _level;

	/**
	 * The default LSID associated with this item.
	 */
	private KeplerLSID _lsid;

	/**
	 * The type of item
	 */
	private int _type;

	/**
	 * The name of the item
	 */
	private String _name;

	/**
	 * Attributes that are associated with this item.
	 */
	private Hashtable<String, String> _attributes;

	/**
	 * KeplerLSIDs that are associated with this item.
	 */
	private Vector<KeplerLSID> _lsids;
	
	/**
	 * public constructor
	 */
	public LibItem() {
		_attributes = new Hashtable<String, String>();
		_lsids = new Vector<KeplerLSID>();
	}

	/**
	 * Update the database row that matches this LibItem liid. This method only
	 * updates LSID,TYPE,NAME columns.
	 * 
	 * @throws SQLException
	 */
	/*
	public void update(Statement stmt) throws SQLException {
		if (getLiid() <= 0) {
			log.warn("LIID in LibItem was not set before update(stmt) call");
			return;
		}
		if (getType() <= 0) {
			log.warn("LIID in LibItem was not set before update(stmt) call");
			return;
		}
		if (getName() == null || getName().trim() == "") {
			log.warn("NAME in LibItem was not set before update(stmt) call");
			return;
		}
		String update = "UPDATE " + LibIndex.LIBRARY_INDEX_TABLE_NAME + " set ";
		if (getLsid() != null) {
			update += " LSID = '" + getLsid() + "',";
		}
		update += " TYPE = " + getType();
		update += ", NAME = '" + getName() + "'";
		update += " WHERE LIID = " + getLiid();

		if (isDebugging)
			log.debug(update);
		stmt.executeUpdate(update);
		stmt.getConnection().commit();
	}
	*/

	/**
	 * Delete this populated LibItem from the Library_Index table. This will
	 * also delete all the children and update the LFT and RGT indexes of all
	 * the other items in the table.
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	public void delete(Statement stmt) throws SQLException {

		// double check the LFT and RGT values
		String query = "SELECT LFT,RGT FROM "
				+ LibIndex.LIBRARY_INDEX_TABLE_NAME + " WHERE LIID = "
				+ getLiid();
		if (isDebugging)
			log.debug(query);
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(query);
			if (rs == null)
				throw new SQLException("Query Failed: " + query);
			if (rs.next()) {
				int l = rs.getInt(1);
				int r = rs.getInt(2);
				if (l != getLeft())
					log.warn("Left value wasn't set before delete " + getLiid());
				if (r != getRight())
					log.warn("Right value wasn't set before delete " + getLiid());
				setLeft(l);
				setRight(r);
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}

		// The number of rows to be deleted
		int delCount = getRight() - getLeft();

		String delete = "delete from " + LibIndex.LIBRARY_INDEX_TABLE_NAME
				+ " WHERE LFT >= " + getLeft() + " AND RGT <= " + getRight();

		String updateLeft = "UPDATE " + LibIndex.LIBRARY_INDEX_TABLE_NAME
				+ " SET LFT = LFT - " + (delCount + 1) + " WHERE LFT >= "
				+ getLeft();
		String updateRight = "UPDATE " + LibIndex.LIBRARY_INDEX_TABLE_NAME
				+ " SET RGT = RGT - " + (delCount + 1) + " WHERE RGT >= "
				+ getLeft();

		if (isDebugging) {
			log.debug("\n" + delete + "\n" + updateLeft + "\n" + updateRight);
		}

		stmt.executeUpdate(updateLeft);
		stmt.executeUpdate(updateRight);
		stmt.executeUpdate(delete);
		stmt.getConnection().commit();

	}

	/**
	 * @return the _liid
	 */
	public int getLiid() {
		return _liid;
	}

	/**
	 * @param liid
	 *            the _liid to set
	 */
	public void setLiid(int liid) {
		_liid = liid;
	}

	/**
	 * @return the _parent
	 */
	public Integer getParent() {
		return _parent;
	}

	/**
	 * @param parent
	 *            the _parent to set
	 */
	public void setParent(Integer parent) {
		_parent = parent;
	}

	/**
	 * @return the _left
	 */
	public int getLeft() {
		return _left;
	}

	/**
	 * @param left
	 *            the _left to set
	 */
	public void setLeft(int left) {
		_left = left;
	}

	/**
	 * @return the _right
	 */
	public int getRight() {
		return _right;
	}

	/**
	 * @param right
	 *            the _right to set
	 */
	public void setRight(int right) {
		_right = right;
	}

	/**
	 * @return the _level
	 */
	public int getLevel() {
		return _level;
	}

	/**
	 * @param level
	 *            the _level to set
	 */
	public void setLevel(int level) {
		_level = level;
	}

	/**
	 * @return the _lsid
	 */
	public KeplerLSID getLsid() {
		return _lsid;
	}

	/**
	 * @param lsid
	 *            the _lsid to set
	 */
	public void setLsid(KeplerLSID lsid) {
		_lsid = lsid;
	}

	/**
	 * @return the _type
	 */
	public int getType() {
		return _type;
	}

	/**
	 * @param type
	 *            the _type to set
	 */
	public void setType(int type) {
		_type = type;
	}

	/**
	 * @return the _name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @param name
	 *            the _name to set
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * @param name
	 * @param value
	 */
	public void addAttribute(String name, String value) {
		if (value == null || name == null) return;
		if (name.trim().equals("")) return;
		if (_attributes.containsKey(name)) {
			_attributes.remove(name);
		}
		_attributes.put(name, value);
	}

	/**
	 * @param attributeName
	 * @return
	 */
	public String getAttributeValue(String attributeName) {
		return _attributes.get(attributeName);
	}

	public Hashtable<String, String> getAttributes() {
		return _attributes;
	}

	public Vector<KeplerLSID> getLsids() {
		return _lsids;
	}

	public void addLsid(KeplerLSID lsid) {
		_lsids.add(lsid);
	}

	public void removeLsid(KeplerLSID lsid) {
		_lsids.remove(lsid);
	}

	public String toString() {
		return getName();
	}

	public String debugString() {
		String s = new String();
		s += "LibItem: \n";
		s += getLiid() + ", ";
		s += getParent() + ", ";
		s += getLeft() + ", ";
		s += getRight() + ", ";
		s += getLevel() + ", ";
		s += getLsid() + ", ";
		s += getType() + ", ";
		s += getName() + "\n";
		s += "  Attributes -> \n";
		for (String attName : getAttributes().keySet()) {
			s += "      " + attName + ": " + getAttributes().get(attName)
					+ "\n";
		}
		s += "  Lsids -> \n";
		for (KeplerLSID lsid : getLsids()) {
			s += "      " + lsid.toString() + "\n";
		}
		return s;
	}
}
