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
import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * A compoiste port is a virtual port that encapsulates a set of underlying
 * ports (both ptolemy ports and other virtual ports). A composite port (and
 * virtual ports in general) can be annotated using semantic types. For example,
 * given an actor with a 'lat' and 'lon' port, a port generalization 'pg' can be
 * constructed that encapsulates both 'lat' and 'lon'; and a semantic type such
 * as "Location" can be assigned to 'pg', stating that 'lat' and 'lon' values
 * combined form a location value.
 * <p>
 * A compoiste port may <b>only</b> be contained within an entity (e.g., an
 * actor). A virtual port must have a name.
 * 
 * @author Shawn Bowers
 * @created June 16, 2005
 */

public class KeplerCompositeIOPort extends KeplerVirtualIOPort {

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
	public KeplerCompositeIOPort(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		if (name == null) {
			String msg = "Port generalization must have a non-null name";
			throw new IllegalActionException(this, msg);
		}
	}

	public void setName(String name) throws IllegalActionException,
			NameDuplicationException {
		if (name == null) {
			String msg = "Port generalization must have a non-null name";
			throw new IllegalActionException(this, msg);
		}
		super.setName(name);
	}

	/**
	 * @return The list of encapsulated port references for this port
	 *         generalization. Each returned reference can be called to obtain
	 *         the actual port.
	 */
	public Vector getEncapsulatedPortReferences() {
		return new Vector(attributeList(KeplerIOPortReference.class));
	}

	/**
	 * Assigns the given ptolemy port reference to the current set of
	 * encapsulated ports. Creates a new reference for the port and adds to the
	 * generalization.
	 * 
	 * @param port
	 *            The ptolemy port to encapsulate.
	 */
	public void addEncapsulatedPort(IOPort port) throws IllegalActionException,
			NameDuplicationException {
		_addPort(port);
	}

	/**
	 * Assigns the given virtual port to the current set of encapsulated ports.
	 * This operation creates a reference to the given port.
	 * 
	 * @param vport
	 *            The virtual port to encapsulate.
	 */
	public void addEncapsulatedPort(KeplerVirtualIOPort port)
			throws IllegalActionException, NameDuplicationException {
		_addPort(port);
	}

	/**
     * 
     */
	private void _addPort(Object obj) throws IllegalActionException,
			NameDuplicationException {
		// adding a ptolemy port
		if (obj instanceof IOPort) {
			IOPort port = (IOPort) obj;
			// make sure the port has the same direction
			if (!_validDirection(port)) {
				String msg = "error adding port '" + port.getName()
						+ "' to port generalization '" + getName()
						+ "': mismatched input/output directions";
				throw new IllegalActionException(this, msg);
			}
			String refname = "_" + port.getName() + "_ref";
			KeplerIOPortReference portRef = new KeplerIOPortReference(this,
					refname);
			portRef.setPort(port);
		}
		// adding a kepler virtual port
		else if (obj instanceof KeplerVirtualIOPort) {
			KeplerVirtualIOPort port = (KeplerVirtualIOPort) obj;
			// make sure the port has the same direction
			if (!_validDirection(port)) {
				String msg = "error adding port '" + port.getName()
						+ "' to port generalization '" + getName()
						+ "': mismatched input/output directions";
				throw new IllegalActionException(this, msg);
			}
			String refname = "_" + port.getName() + "_ref";
			KeplerVirtualIOPortReference portRef = new KeplerVirtualIOPortReference(
					this, refname);
			portRef.setPort(port);
		}

	}

	/**
     * 
     */
	private boolean _validDirection(IOPort port) {
		if (_direction == null) {
			_direction = port.isOutput() ? _OUTPUT : _INPUT;
			return true;
		}

		if (port.isOutput() && _direction.equals(_INPUT))
			return false;
		else if (port.isInput() && _direction.equals(_OUTPUT))
			return false;

		return true;
	}

	/**
     * 
     */
	private boolean _validDirection(KeplerVirtualIOPort port) {
		if (_direction == null) {
			_direction = port.isOutput() ? _OUTPUT : _INPUT;
			return true;
		}

		// get the virtual port direction
		if (port.isOutput() && _direction.equals(_INPUT))
			return false;
		else if (port.isInput() && _direction.equals(_OUTPUT))
			return false;

		return true;
	}

	/**
     * 
     */
	public boolean isOutput() {
		return _OUTPUT.equals(_direction);
	}

	/**
     * 
     */
	public boolean isInput() {
		return _INPUT.equals(_direction);
	}

	/**
	 * Returns a record type of the encapsulated ports, where the labels are the
	 * port names.
	 */
	public Type getType() {
		Vector<String> labels = new Vector<String>();
		Vector<Type> types = new Vector<Type>();
		// get all the ports and add their names and types
		for (Iterator iter = getEncapsulatedPortReferences().iterator(); iter
				.hasNext();) {
			Object ref = iter.next();
			// it should be a port reference
			if (ref instanceof KeplerIOPortReference) {
				Object obj = ((KeplerIOPortReference) ref).getPort();
				// it has a type
				if (obj instanceof TypedIOPort) {
					TypedIOPort port = (TypedIOPort) obj;
					String label = port.getName();
					labels.add(label);
					Type type = port.getType() == null ? BaseType.UNKNOWN
							: port.getType();
					types.add(type);
				}
				// it doesn't have a type, use unknown type
				else if (obj instanceof IOPort) {
					IOPort port = (IOPort) obj;
					String label = port.getName();
					labels.add(label);
					types.add(BaseType.UNKNOWN);
				}
				// it is a virtual port
				else if (obj instanceof KeplerVirtualIOPort) {
					KeplerVirtualIOPort port = (KeplerVirtualIOPort) obj;
					String label = port.getName();
					labels.add(label);
					Type type = port.getType() == null ? BaseType.UNKNOWN
							: port.getType();
					types.add(type);
				}
			}
		}

		String[] recLabels = new String[labels.size()];
		Type[] recTypes = new Type[types.size()];
		int i = 0;
		for (Iterator<String> iter = labels.iterator(); iter.hasNext(); i++)
			recLabels[i] = iter.next();
		i = 0;
		for (Iterator<Type> iter = types.iterator(); iter.hasNext(); i++)
			recTypes[i] = iter.next();
		return new RecordType(recLabels, recTypes);
	}

	/**
     *
     */
	public String toString() {
		return getName();
	}

	/** private members */

	private String _direction = null;
	private String _OUTPUT = "output";
	private String _INPUT = "input";

} // KeplerCompositeIOPort