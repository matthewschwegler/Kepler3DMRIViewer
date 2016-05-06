/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-11-15 12:03:09 -0800 (Thu, 15 Nov 2012) $' 
 * '$Revision: 31093 $'
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

package org.kepler.objectmanager.lsid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.AuthNamespace;

/**
 * A kepler class to store lsids of the form
 * urn:lsid:&ltauthority&gt;:&lt;namespace&gt;:&lt;object&gt;:&lt;revision&gt;#&lt;anchor&gt; where
 * &lt;authority&gt; and &lt;namespace&gt; are strings that do not contain : characters and
 * where &lt;object&gt; and &lt;revision&gt; are Long, 64 bit integers, and &lt;anchor&gt; is any
 * string that does not contain a # or : character.
 * 
 *@created June 20, 2005
 */

public class KeplerLSID implements Serializable {
	private static final long serialVersionUID = 6124781256944559026L;
	private static final Log log = LogFactory
			.getLog(KeplerLSID.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private String _lsidStr = null;

	// the parts of the lsid
	private String _authority;
	private String _namespace;
	private Long _object;
	private Long _revision;
	private String _anchor;

	public static final char separatorChar = ':';
	public static final String separator = ":";
	public static final char anchorSeparatorChar = '#';
	public static final String anchorSeparator = "#";

	/*
	 * The following are the different types of Kepler objects. These are used
	 * to identify entries in KAR files. These are going away soon.
	 */
	public static final String ACTOR_METADATA = "actorMetadata";
	public static final String JAR = "jar";
	public static final String JAVA_CLASS = "class";
	public static final String WORKFLOW = "workflow";
	public static final String NATIVE_LIBRARY = "nativeLibrary";
	public static final String XML_METADATA = "xmlMetadata";
	public static final String DATA = "data";
	public static final String RESOURCE_FILE = "file";

	/**
	 * Construct an lsid.
	 * @throws Exception
	 */
	public KeplerLSID(String lsidString) throws Exception {
		if (isDebugging)
			log.debug("KeplerLSID("+lsidString+")");
		// Note - if any changes needs to be made to this constructor,
		// the changes should be made in initializeLSID so the
		// deserialization readObject() method can benefit from it as well.
		_lsidStr = lsidString;
		initializeLSID();
	}

	/**
	 * construct an lsid from components
	 * 
	 * @param authority
	 *            the authority of the new lsid
	 * @param namespace
	 *            the namespace of the new lsid
	 * @param the
	 *            object number of the new lsid
	 * @param revision
	 *            the revision of the new lsid
	 */
	public KeplerLSID(String authority, String namespace, Long object,
			Long revision) throws Exception {
		if (isDebugging)
			log.debug("KeplerLSID("+authority+","+namespace+","+object+","+revision+")");

		_lsidStr = "urn:lsid:" + authority + ":" + namespace + ":" + object
				+ ":" + revision;
		initializeLSID();
	}

	/**
	 * creates an lsid from a metacat docid of the form
	 * <document>.<object>.<rev>
	 * 
	 * @param metacatDocid
	 *            the docid to translate
	 * @param authority
	 *            the authority to use in the lsid
	 */
	public KeplerLSID(String metacatDocid, String authority) throws Exception {
		if (isDebugging) log.debug("KeplerLSID("+metacatDocid+","+authority+")");
		String doc;
		String obj;
		String rev = "1";
		doc = metacatDocid.substring(0, metacatDocid.indexOf("."));
		// check if there is a revision
		if (metacatDocid.lastIndexOf(".") != metacatDocid.indexOf(".")) {
			// there is a revision
			obj = metacatDocid.substring(metacatDocid.indexOf(".") + 1,
					metacatDocid.lastIndexOf("."));
			rev = metacatDocid.substring(metacatDocid.lastIndexOf(".") + 1,
					metacatDocid.length());
		} else {
			// no revision
			obj = metacatDocid.substring(metacatDocid.indexOf(".") + 1,
					metacatDocid.lastIndexOf("."));
		}
		_authority = authority;
		_namespace = doc;
		_object = new Long(obj);
		_revision = new Long(rev);
		_lsidStr = "urn:lsid:" + authority + ":" + _namespace + ":" + _object
				+ ":" + _revision;
		if (isDebugging)
			log.debug("string: " + _lsidStr);
	}

	/**
	 * returns the namespace component of this lsid
	 */
	public String getNamespace() {
		return _namespace;
	}

	/**
	 * returns the authority component of this lsid
	 */
	public String getAuthority() {
		return _authority;
	}

	/**
	 * return the revision componenent of this lsid
	 */
	public Long getRevision() {
		return _revision;
	}

	/**
	 * returns the object component of this lsid
	 */
	public Long getObject() {
		return _object;
	}

	public String getAnchor() {
		return _anchor;
	}

	public void setAnchor(String anchor) throws Exception {
		if (anchor.indexOf( separatorChar ) >= 0) {
			throw new Exception("Anchor may not contain the : character.");
		}
		if (anchor.indexOf( anchorSeparatorChar ) >= 0) {
			throw new Exception("Anchor may not contain the "
					+ anchorSeparator + " character.");
		}
		if (hasAnchor()) {
			_lsidStr = "urn" + separator + "lsid" + separator + getAuthority() 
					+ separator + getNamespace()
					+ separator + getObject() + separator + getRevision() 
					+ anchorSeparator + anchor;
		} else {
			_lsidStr += anchorSeparator + _anchor;
		}
		_anchor = anchor;
	}

	/**
	 * @return true if this KeplerLSID has an anchor
	 */
	public boolean hasAnchor() {
		if (_anchor == null || _anchor.equals("")) {
			return false;
		}
		return true;
	}

	/**
	 * increment the revision number by 1.
	 */
	public void incrementRevision() {
		long rev = Long.valueOf(_revision);
		rev++;
		_revision = Long.valueOf(rev);
		_lsidStr = toString();
	}
	
	/**
	 * Specifically set the revision on this LSID.  You should never be calling this!
	 * Instead, pass your LSID to LSIDGenerator.updateLsidRevision(KeplerLSID).
	 * 
	 * @param newRevision
	 */
	public void setRevision(Long newRevision) {
		_revision = newRevision;
		_lsidStr = toString();
	}

	/**
	 * If this LSID was generated by this Kepler Instance return true else
	 * return false
	 * 
	 * @return true if this KeplerLSID was generated by this Kepler Instance.
	 */
	public boolean isLocalToInstance() {
		AuthNamespace an = AuthNamespace.getInstance();
		if (an.getAuthority().equals(getAuthority())
				&& an.getNamespace().equals(getNamespace())) {
			return true;
		}
		return false;
	}

	/**
	 * this will create a valid filename out of the lsid (without an extension).
	 * It will replace any invalid characters in the lsid with '.' This function
	 * is irreversible.
	 */
	public String createFilename() {
		String fname = "urn.lsid." + getAuthority() + "." + getNamespace()
				+ "." + getObject() + "." + getRevision();
		// if (hasAnchor()) {
		// fname += "." + getAnchor();
		// }
		fname = fname.replace('/', '.');
		fname = fname.replace('\\', '_');
		return fname;
	}

	/**
	 * return a string representation of the lsid
	 */
	public String toString() {
		String lsidStr = "urn" + separator + "lsid" + separator + getAuthority() 
				+ separator + getNamespace()
				+ separator + getObject() + separator + getRevision();
		if (hasAnchor()) {
			lsidStr += anchorSeparator + getAnchor();
		}
		return lsidStr;
	}

	/**
	 * Return a string that contains only the first five elements of the LSID.
	 * i.e. urn:lsid:&lt;authority&gt;:&lt;namespace&gt;:&lt;object&gt;
	 * 
	 * @return String
	 */
	public String toStringWithoutRevision() {
		String lsidStr = "urn" + separator + "lsid"
				+ separator + getAuthority() + separator + getNamespace()
				+ separator + getObject();
		return lsidStr;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {

		int hash = 7;
		hash = 31 * hash + (null == _authority ? 0 : _authority.hashCode());
		hash = 31 * hash + (null == _namespace ? 0 : _namespace.hashCode());
		hash = 31 * hash
				+ (int) (_object.longValue() ^ (_object.longValue() >>> 32));
		hash = 31
				* hash
				+ (int) (_revision.longValue() ^ (_revision.longValue() >>> 32));
		return hash;
	}

	/**
	 * return true if this lsid equals the passed in lsid regardless of anchor
	 */
	public boolean equals(Object lsidObj) {
		if (lsidObj instanceof KeplerLSID) {
			KeplerLSID lsid = (KeplerLSID) lsidObj;
			Long rev = lsid.getRevision();
			if ( equalsWithoutRevision(lsid)
					&& rev.equals(_revision)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * return true if this lsid equals the passed in lsid regardless of 
	 * revision or anchor
	 */
	public boolean equalsWithoutRevision(KeplerLSID lsid) {
		String auth = lsid.getAuthority();
		String ns = lsid.getNamespace();
		Long obj = lsid.getObject();
		if (auth.trim().equals(_authority.trim())
				&& ns.trim().equals(_namespace.trim()) && obj.equals(_object)) {
			return true;
		}
		return false;
	}

	/**
	 * return true if this lsid equals the passed in lsid and the anchors are
	 * the same.
	 * 
	 * @param lsid
	 * 	 */
	public boolean equalsWithAnchor(KeplerLSID lsid) {
		if (equals(lsid) && getAnchor().equals(lsid.getAnchor())) {
			return true;
		}
		return false;
	}

	/**
	 * parses the lsid into its parts
	 * urn:lsid:&lt;authority&gt;:&lt;namespace&gt;:&lt;object&gt;:&lt;revision&gt;
	 */
	private void initializeLSID() throws Exception {
		if (_lsidStr == null) {
			// to avoid NullPointerException
			_lsidStr = "";
		}

		StringTokenizer st = new StringTokenizer(_lsidStr, separator);
		if (st.countTokens() == 6) {
			String u = st.nextToken();
			String l = st.nextToken();
			if (u.equals("urn") && l.equals("lsid")) {
				_authority = st.nextToken();
				_namespace = st.nextToken();
				try {
					_object = new Long(st.nextToken());
				} catch (NumberFormatException nfe) {
					throw new Exception("Object must be a number");
				}
				try {
					String rev = st.nextToken();
					int anchorInd = rev.indexOf(anchorSeparatorChar);
					if (anchorInd > 0) {
						_revision = new Long(rev.substring(0, anchorInd));
						_anchor = rev.substring(anchorInd + 1);
						if (_anchor.indexOf(anchorSeparatorChar) >= 0)
							throw new Exception(
									"Anchor '"+ _anchor + "' must not contain "
									+ anchorSeparator + " character");
					} else {
						_revision = new Long(rev);
						_anchor = "";
					}
					_lsidStr = toString();
				} catch (NumberFormatException nfe) {
					throw new Exception("Object must be a number");
				}
			} else {
				throw new Exception("KeplerLSID must begin with urn:lsid");
			}
		} else if (st.countTokens() == 4) {
			handleBackwardsCompatibilityKludgeFormat1(st);
		} else {
			throw new Exception(
					"KeplerLSID format contains six elements urn:lsid:<authority>:<namespace>:<object>:<revision>");
		}
		if (isDebugging)
			log.debug("toString() -> " + toString());
	}

	public static boolean isKeplerLSIDFormat(String lsidStr) {
		if (isDebugging) log.debug("isKeplerLSIDFormat("+lsidStr+")");
		try {
			new KeplerLSID(lsidStr);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Define custom metadata representation consisting of a single String named
	 * "lsidString"
	 */
	private static final ObjectStreamField[] serialPersistentFields = { new ObjectStreamField(
			"lsidString", String.class) };

	/**
	 * Custom deserialization method. The serial representation of the
	 * KeplerLSID object is simply the toString() representation. This string
	 * representation is passed into the initializeFromString method.
	 * 
	 * @param ois
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream ois) throws IOException,
			ClassNotFoundException {
		ObjectInputStream.GetField fields = ois.readFields();
		String lsidString = (String) fields.get("lsidString", null);
		try {
			_lsidStr = lsidString;
			initializeLSID();
		} catch (Exception e) {
			throw new IOException("Malformed LSID in serialized representation");
		}
	}

	/**
	 * Custom serialization method. The serial representation of the KeplerLSID
	 * object is simply the toString() representation.
	 * 
	 * @param oos
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		ObjectOutputStream.PutField fields = oos.putFields();
		fields.put("lsidString", toString());
		oos.writeFields();
	}

	/**
	 * BackwardsCompatibilityKludgeFormat1 is an lsid in the following format
	 * urn:lsid:ecoinformatics.org:bob.2.4 where 2 is the object id and 4 is the
	 * revision a dot is used as the separator instead of a colon See
	 * http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4066
	 * 
	 * @param st
	 * @throws Exception
	 */
	private void handleBackwardsCompatibilityKludgeFormat1(StringTokenizer st)
			throws Exception {
		String u = st.nextToken();
		String l = st.nextToken();
		if (u.equals("urn") && l.equals("lsid")) {
			_authority = st.nextToken();
			String possibleNamespacePlusObjectPlusRevision = st.nextToken();
			StringTokenizer stn = new StringTokenizer(
					possibleNamespacePlusObjectPlusRevision, ".");
			if (stn.countTokens() >= 3) {
				String ns = "";
				for (int i = 0; i < (stn.countTokens() - 2); i++) {
					ns += stn.nextToken() + ".";
				}
				_namespace = ns.substring(0, ns.length() - 1);

			} else {
				throw new Exception("");
			}
			try {
				_object = new Long(stn.nextToken());
			} catch (NumberFormatException nfe) {
				throw new Exception("Object must be a number");
			}
			try {
				String rev = stn.nextToken();
				int anchorInd = rev.indexOf(anchorSeparatorChar);
				if (anchorInd > 0) {
					_revision = new Long(rev.substring(0, anchorInd));
					_anchor = rev.substring(anchorInd + 1);
					if (_anchor.indexOf(anchorSeparatorChar) >= 0)
						throw new Exception(
								"Anchor must not contain "
								+ anchorSeparator + " character");
				} else {
					_revision = new Long(rev);
					_anchor = "";
				}
			} catch (NumberFormatException nfe) {
				throw new Exception("Object must be a number");
			}
		}
	}
}