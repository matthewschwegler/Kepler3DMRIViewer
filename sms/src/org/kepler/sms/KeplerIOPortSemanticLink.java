/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

package org.kepler.sms;

import java.util.Iterator;
import java.util.Vector;

import ptolemy.actor.IOPort;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;

/**
 * FIXME: Documentation ... A port generalization is a virtual port that
 * encapsulates a set of underlying ports (both ptolemy ports and other virtual
 * ports). A port generalization (and virtual ports in general) can be annotated
 * using semantic types. For example, given an actor with a 'lat' and 'lon'
 * port, a port generalization 'pg' can be constructed that encapsulates both
 * 'lat' and 'lon'; and a semantic type such as "Location" can be assigned to
 * 'pg', stating that 'lat' and 'lon' values combined form a location value.
 * <p>
 * A port generalization may <b>only</b> be contained within an entity (e.g., an
 * actor). A virtual port must have a name.
 * 
 * @author Shawn Bowers
 * @created June 20, 2005
 */

public class KeplerIOPortSemanticLink extends Attribute {

	/**
	 * Constructor
	 * 
	 * @param container
	 *            Description of the Parameter
	 * @param name
	 *            The value of the property
	 * @exception IllegalActionException
	 *                Description of the Exception
	 * @exception NameDuplicationException
	 *                Description of the Exception
	 */
	public KeplerIOPortSemanticLink(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * FIXME: This should be "external" to the port ... Set the container of the
	 * link to the given container. This method fails, in addition to the normal
	 * ptolemy constraints for setting containers, if the given container is not
	 * a org.kepler.sms.KeplerVirtualIOPort or ptolemy IOPort object.
	 * 
	 * @param container
	 *            The container for this virtual port.
	 */
	public void setContainer(NamedObj container) throws IllegalActionException,
			NameDuplicationException {
		if (!(container instanceof IOPort || container instanceof KeplerVirtualIOPort)) {
			String msg = "Container not an IOPort or Kepler virtual IO port '"
					+ getName() + "'";
			throw new IllegalActionException(this, msg);
		}
		super.setContainer(container);
	}

	/**
	 * @return The domain of the link, which is an IOPort or
	 *         KeplerVirtualIOPort.
	 */
	public Object getDomain() {
		return getContainer();
	}

	/**
	 * Assigns the domain of this link. As a side effect, makes the domain the
	 * container for this object.
	 */
	public void setDomain(IOPort domain) throws IllegalActionException,
			NameDuplicationException {
		setContainer(domain);
	}

	/**
	 * Assigns the domain of this link. As a side effect, makes the domain the
	 * container for this object.
	 */
	public void setDomain(KeplerVirtualIOPort domain)
			throws IllegalActionException, NameDuplicationException {
		if (!_validateDomain(domain)) {
			String msg = "Domain port '" + domain.getName()
					+ "' does not match range" + " direction for link '"
					+ getName() + "'";
			throw new IllegalActionException(this, msg);
		}
		setContainer(domain);
	}

	/**
	 * @return The range of the link, which is an IOPort or KeplerVirtualIOPort
	 */
	public Object getRange() {
		KeplerIOPortReference ref = _getRangeRef();
		if (ref != null)
			return ref.getPort();
		return null;
	}

	/**
	 * @return The range as an IOPort reference. Null if there is no range.
	 */
	private KeplerIOPortReference _getRangeRef() {
		for (Iterator iter = attributeList().iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof KeplerIOPortReference)
				return (KeplerIOPortReference) obj;
		}
		return null;
	}

	/**
	 * Assigns the IOPort as the range of the link. Removes the current range if
	 * one exists.
	 */
	public void setRange(IOPort range) throws IllegalActionException,
			NameDuplicationException {
		if (range == null)
			return;
		if (!_validateRange(range)) {
			String msg = "Range port '" + range.getName()
					+ "' does not match domain" + " direction for link '"
					+ getName() + "'";
			throw new IllegalActionException(this, msg);
		}
		KeplerIOPortReference ref = _getRangeRef();
		if (ref != null) {
			MoMLChangeRequest request = new MoMLChangeRequest(this,
					getContainer(), _swapRange(range));
			requestChange(request);
			System.out.println("HERE ... " + ref);
			System.out.println(exportMoML());
		} else {
			ref = new KeplerIOPortReference(this, "_range");
			((KeplerIOPortReference) ref).setPort(range);
		}
	}

	/**
	 * Assigns the Virtual IOPort as the range of the link. Removes the current
	 * range if one exists.
	 */
	public void setRange(KeplerVirtualIOPort range)
			throws IllegalActionException, NameDuplicationException {
		if (range == null)
			return;
		if (!_validateRange(range)) {
			String msg = "Range port '" + range.getName()
					+ "' does not match domain" + " direction for link '"
					+ getName() + "'";
			throw new IllegalActionException(this, msg);
		}
		KeplerIOPortReference ref = _getRangeRef();
		if (ref != null) {
			MoMLChangeRequest request = new MoMLChangeRequest(this,
					getContainer(), _swapRange(range));
			requestChange(request);
			System.out.println("HERE ... " + ref);
			System.out.println(exportMoML());
		} else {
			ref = new KeplerVirtualIOPortReference(this, "_range");
			((KeplerVirtualIOPortReference) ref).setPort(range);
		}
	}

	/**
	 * Helper function to remove an attribute from an a link.
	 */
	private String _swapRange(KeplerVirtualIOPort range) {
		String linkStr = "";
		if (getName() != null)
			linkStr += "<property name=\"" + getName() + "\" ";
		else
			linkStr += "<property ";
		linkStr += "class=\"" + this.getClass().getName() + "\">\n";
		linkStr += "   <property name=\"_range\" ";
		linkStr += "class=\"" + KeplerVirtualIOPortReference.class.getName()
				+ "\" ";
		linkStr += "value=\"" + range.getName() + "\"";
		linkStr += "/>\n";

		for (Iterator<SemanticProperty> iter = getSemanticProperties().iterator(); iter.hasNext();) {
			SemanticProperty p = iter.next();
			linkStr += "   " + p.exportMoML() + "\n";
		}
		return linkStr + "</property>";
	}

	/**
	 * Helper function to remove an attribute from an a link.
	 */
	private String _swapRange(IOPort range) {
		String linkStr = "";
		if (getName() != null)
			linkStr += "<property name=\"" + getName() + "\" ";
		else
			linkStr += "<property ";
		linkStr += "class=\"" + this.getClass().getName() + "\">\n";
		linkStr += "   <property name=\"_range\" ";
		linkStr += "class=\"" + KeplerIOPortReference.class.getName() + "\" ";
		linkStr += "value=\"" + range.getName() + "\"";
		linkStr += "/>\n";

		for (Iterator<SemanticProperty> iter = getSemanticProperties().iterator(); iter.hasNext();) {
			SemanticProperty p = iter.next();
			linkStr += "   " + p.exportMoML() + "\n";
		}
		return linkStr + "</property>";
	}

	/**
     * 
     */
	public Vector<SemanticProperty> getSemanticProperties() {
		Vector<SemanticProperty> result = new Vector<SemanticProperty>();
		for (Iterator<SemanticProperty> iter = attributeList(SemanticProperty.class).iterator(); iter
				.hasNext();)
			result.add(iter.next());
		return result;
	}

	/**
     * 
     */
	public void addSemanticProperty(SemanticProperty property)
			throws IllegalActionException, NameDuplicationException {
		property.setContainer(this);
	}

	/**
	 * @return True if the given domain direction matches existing range
	 *         direction.
	 */
	private boolean _validateDomain(IOPort domain) {
		if (getRange() != null) {
			Object obj = getRange();
			if (obj instanceof IOPort)
				return ((IOPort) obj).isOutput() == domain.isOutput();
			else if (obj instanceof KeplerVirtualIOPort)
				return ((KeplerVirtualIOPort) obj).isOutput() == domain
						.isOutput();
		}
		return true;
	}

	/**
	 * @return True if the given domain direction matches existing range
	 *         direction.
	 */
	private boolean _validateDomain(KeplerVirtualIOPort domain) {
		if (getRange() != null) {
			Object obj = getRange();
			if (obj instanceof IOPort)
				return ((IOPort) obj).isOutput() == domain.isOutput();
			else if (obj instanceof KeplerVirtualIOPort)
				return ((KeplerVirtualIOPort) obj).isOutput() == domain
						.isOutput();
		}
		return true;
	}

	/**
	 * @return True if the given range direction matches existing domain
	 *         direction.
	 */
	private boolean _validateRange(IOPort range) {
		if (getDomain() != null) {
			Object obj = getDomain();
			if (obj instanceof IOPort)
				return ((IOPort) obj).isOutput() == range.isOutput();
			else if (obj instanceof KeplerVirtualIOPort)
				return ((KeplerVirtualIOPort) obj).isOutput() == range
						.isOutput();
		}
		return true;
	}

	/**
	 * @return True if the given range direction matches existing domain
	 *         direction.
	 */
	private boolean _validateRange(KeplerVirtualIOPort range) {
		if (getDomain() != null) {
			Object obj = getDomain();
			if (obj instanceof IOPort)
				return ((IOPort) obj).isOutput() == range.isOutput();
			else if (obj instanceof KeplerVirtualIOPort)
				return ((KeplerVirtualIOPort) obj).isOutput() == range
						.isOutput();
		}
		return true;
	}

} // KeplerIOPortSemanticLink