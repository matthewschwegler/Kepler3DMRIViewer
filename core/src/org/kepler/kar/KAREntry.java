/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:21:34 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31119 $'
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

package org.kepler.kar;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.karxml.KarXml;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * Represents one entry that is contained inside of a KAR file.
 */
public class KAREntry extends JarEntry {
	private static final Log log = LogFactory.getLog(KAREntry.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	Attributes _attributes = new Attributes();

	/**
	 * <code>Name</code> object for <code>lsid</code> manifest attribute used
	 * for globally identifying KAREntries as unique.
	 */
	public static final Name LSID = new Name("lsid");

	/**
	 * <code>Name</code> object for <code>type</code> manifest attribute used
	 * for specifying the type of object contained by the KAREntry.
	 */
	public static final Name TYPE = new Name("type");

	/**
	 * <code>Name</code> object for <code>handler</code> manifest
	 * attribute used for identifying which KAREntry handler was used
	 * to write this KAREntry.
	 */
	public static final Name HANDLER = new Name("handler");

	/**
	 * <code>Name</code> object for <code>lsid_dependencies</code> manifest
	 * attribute used for identifying other KAREntries that this entry depends
	 * on.
	 */
	public static final Name LSID_DEPENDENCIES = new Name("dependsOn");

	/**
	 * create a karentry from a jarentry
	 */
	public KAREntry(JarEntry je) {
		super(je);
		try {
			Attributes atts = je.getAttributes();
			if (atts == null) {
				_attributes = new Attributes();
			} else {
				_attributes = atts;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (isDebugging)
			log.debug(this.debugString());
	}

	/**
	 * create a karentry from a name
	 */
	public KAREntry(String name) {
		super(name);
	}

	/**
	 * create a karentry from a zipentry
	 */
	public KAREntry(ZipEntry ze) {
		super(ze);
	}

	/**
	 * @return
	 */
	public boolean isValid() {
		if (isDebugging) log.debug("isValid()");
		if (getLSID() == null) {
			return false;
		}
		if (getType() == null) {
			return false;
		}
		if (getType().trim().equals("")) {
			return false;
		}
		if (getHandler() == null) {
			return false;
		}
		if (getHandler().trim().equals("")) {
			return false;
		}
		return true;
	}

	/**
	 * @return KeplerLSID or null
	 */
	public KeplerLSID getLSID() {
		try {
			String lsidStr = _attributes.getValue(LSID);
			if (lsidStr == null) {
				log.warn("no LSID for KAREntry " + getName());
				return null;
			}
			return new KeplerLSID(lsidStr);
		} catch (Exception e) {
			log.warn("KAREntry lsid error: " + e.getMessage());
			return null;
		}
	}

	/**
	 * @param lsid
	 * @throws IOException
	 */
	public void setLSID(KeplerLSID lsid) throws IOException {
		if (_attributes.containsKey(LSID)) {
			_attributes.remove(LSID);
		}
		_attributes.put(LSID, lsid.toString());
	}

	/**
	 * @return
	 */
	public String getType() {
		String type = _attributes.getValue(TYPE);
		if (type == null) {
			log.warn("no type for KAREntry");
			return null;
		}
		return type;
	}
	
	/**
	 * Determine if the kar entry is report layout
	 * @return true if the type is ReportLayout
	 */
	public boolean isReportLayout(){
	  String type = _attributes.getValue(TYPE);
	  if(type != null && type.contains(KarXml.REPORTLAYOUT)){
	    return true;
	  }
	  else{
	    return false;
	  }
	}

	/**
	 * @param type
	 * @throws IOException
	 */
	public void setType(String type) throws IOException {
		if (_attributes.containsKey(TYPE)) {
			_attributes.remove(TYPE);
		}
		_attributes.put(TYPE, type);
	}

	/**
	 * @return
	 */
	public String getHandler() {
		String handler = _attributes.getValue(HANDLER);
		if (handler == null) {
			log.warn("no handler for KAREntry: " + getName() + " " + getLSID());
			return null;
		}
		return handler;
	}

	/**
	 * @param handler
	 * @throws IOException
	 */
	public void setHandler(String handler) throws IOException {
		if (_attributes.containsKey(HANDLER)) {
			_attributes.remove(HANDLER);
		}
		_attributes.put(HANDLER, handler);
	}

	/**
	 * @return
	 */
	public List<KeplerLSID> getLsidDependencies() {
		Vector<KeplerLSID> depList = parseLsidDependencies(getAttributes());
		return depList;

	}


	/**
	 * @param atts
	 * @return
	 */
	public static Vector<KeplerLSID> parseLsidDependencies(Attributes atts) {

		Vector<KeplerLSID> depList = new Vector<KeplerLSID>(2);
		if (atts == null)
			return depList;

		String ld = atts.getValue(KAREntry.LSID_DEPENDENCIES);
		if (ld == null) {
			return depList;
		}

		StringTokenizer st = new StringTokenizer(ld, ":");
		while (st.hasMoreTokens()) {
			try {
				String lsidStr = st.nextToken() + ":" + st.nextToken() + ":"
						+ st.nextToken() + ":" + st.nextToken() + ":"
						+ st.nextToken() + ":" + st.nextToken();
				KeplerLSID lsid = new KeplerLSID(lsidStr);
				depList.add(lsid);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return depList;
	}

	/**
	 * @param lsid
	 */
	public void addLsidDependency(KeplerLSID lsid) {
		if (isDebugging) log.debug("addLsidDependency("+lsid+")");
		if (this.dependsOn(lsid)) {
			// it is already in the list
			// do nothing
		} else {
			String ld = _attributes.getValue(LSID_DEPENDENCIES);
			if (ld == null || ld.trim().equals("")) {
				ld = "";
				ld += lsid.toString();
			} else {
				ld += ":" + lsid.toString();
			}
			_attributes.remove(LSID_DEPENDENCIES);
			_attributes.put(LSID_DEPENDENCIES, ld);
		}
		if (isDebugging) {
			log.debug(_attributes.getValue(LSID_DEPENDENCIES));
		}
	}

	/**
	 * @param lsid
	 */
	public void removeLsidDependency(KeplerLSID lsid) {
		if (isDebugging) log.debug("removeLsidDependency("+lsid+")");
		if (isDebugging) {
			log.debug(_attributes.getValue(LSID_DEPENDENCIES));
		}
		if (this.dependsOn(lsid)) {
			List<KeplerLSID> deps = getLsidDependencies();
			deps.remove(lsid);
			String newList = "";
			int i = 0;
			for (KeplerLSID dep : deps) {
				if (i == 0) {
					newList += dep.toString();
				} else {
					newList += ":" + dep.toString();
				}
				i++;
			}
			_attributes.remove(LSID_DEPENDENCIES);
			_attributes.put(LSID_DEPENDENCIES, newList);
		} else {
			// do nothing
			// it is not in the list
		}
		if (isDebugging) {
			log.debug(_attributes.getValue(LSID_DEPENDENCIES));
		}
	}

	/**
	 * @param lsid
	 * @return
	 */
	public boolean dependsOn(KeplerLSID lsid) {
		for (KeplerLSID l : getLsidDependencies()) {
			if (l.equals(lsid)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * get the attributes object for this entry
	 */
	public Attributes getAttributes() {
		return _attributes;
	}

	/**
	 * @param name
	 * @param value
	 */
	public void addAttribute(String name, String value) {
		_attributes.put(new Attributes.Name(name), value);
	}

	/**
	 * @return
	 */
	private String debugString() {
		String s = "KAREntry: " + "\n  name=" + getName() + "\n  lsid="
				+ getLSID() + "\n  type=" + getType() + "\n  handler="
				+ getHandler() + "\n lsidDependencies="
				+ getLsidDependencies();
		return s;
	}

}