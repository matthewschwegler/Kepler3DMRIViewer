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
import java.util.StringTokenizer;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.MatrixType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.StructuredType;
import ptolemy.data.type.Type;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * A refinement port is a virtual port that denotes a particular nested
 * structure within a port's value. A refinement port (and virtual ports in
 * general) can be annotated using semantic types. For example, given an actor
 * with a port p1 having type {lat=int, lon=int}, i.e., the port type is a
 * record having a lat and lon component, a port refinement 'p1/lat' can be
 * constructed that denotes the lat values (integers); and a semantic type such
 * as "Latitude" can be assigned to 'p1/lat'.
 * <p>
 * A port refinement may <b>only</b> be contained within an entity (e.g., an
 * actor). All port refinements are required to have a well-formed port name,
 * representing the actual "pointer" into the port's data structure. The pointer
 * 'elem' is used to denote traversal into an array (list) or matrix, and the
 * component name (e.g., 'lat') is used to denote traversal into a record.
 * 
 * @author Shawn Bowers
 * @created June 16, 2005
 */

public class KeplerRefinementIOPort extends KeplerVirtualIOPort {

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
	public KeplerRefinementIOPort(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		if (name == null)
			throw new IllegalActionException(this,
					"Refinement port pointer cannot be null");
		if (!_wellFormedPointer(name))
			throw new IllegalActionException(this,
					"Badly formed refinement port pointer '" + name + "'");
		if (_pointerType(name) == null)
			throw new IllegalActionException(this,
					"Invalid refinement port pointer '" + name + "'");
	}

	/**
	 * Calls setRefinedPortPointer to set the pointer.
	 */
	public void setName(String name) throws IllegalActionException,
			NameDuplicationException {
		if (name == null)
			throw new IllegalActionException(this,
					"Refined port pointer cannot be null");
		else if (!_wellFormedPointer(name))
			throw new IllegalActionException(this,
					"Badly formed refinement port pointer '" + name + "'");
		else if (_pointerType(name) == null)
			throw new IllegalActionException(this,
					"Invalid refinement port pointer '" + name + "'");
		super.setName(name);
	}

	/**
	 * Assign the given pointer (name) to the port. The pointer cannot be null
	 * and must be well-formed. A well-formed pointer takes the form: p/s_1/s_2/
	 * ... /s_n for n > 0, where p is a port name, and s_1 to s_n are strings.
	 */
	public void setPointer(String pointer) throws IllegalActionException,
			NameDuplicationException {
		setName(pointer);
	}

	/**
	 * @return Retrieve the ptolemy port refined by this virtual port. Note that
	 *         the port must reside within the same actor as this refinement.
	 */
	public TypedIOPort getRefinedPort() {
		return _getPort(getPointer());
	}

	/**
     * 
     */
	public TypedIOPort _getPort(String pointer) {
		// get the pointer "steps"
		String[] steps = pointer.split("/");
		// should be greater than zero
		if (!(steps.length > 0))
			return null;
		// grab port name
		String portName = steps[0];

		// iterate to first entity container
		NamedObj container = getContainer();
		while (container != null && !(container instanceof Entity))
			container = container.getContainer();

		// couldn't find a container
		if (container == null)
			return null;

		// find the corresponding port
		Entity actor = (Entity) container;
		for (Iterator ports = actor.portList().iterator(); ports.hasNext();) {
			Object obj = (Object) ports.next();
			if (!(obj instanceof TypedIOPort))
				continue;
			TypedIOPort port = (TypedIOPort) obj;
			if (portName == null && port.getName() == null)
				return port;
			else if (portName.equals(port.getName()))
				return port;
		}
		return null;
	}

	/**
	 * @return Returns the pointer (i.e., name) of the port.
	 */
	public String getPointer() {
		return getName();
	}

	/**
	 * @return True if the refined port is an output port.
	 */
	public boolean isOutput() {
		return getRefinedPort().isOutput();
	}

	/**
	 * @return True if the refined port is an input port.
	 */
	public boolean isInput() {
		return getRefinedPort().isInput();
	}

	/**
	 * ...
	 * 
	 * @return The type associated with the pointer.
	 */
	public Type getType() {
		return _pointerType(getPointer());
	}

	/**
     *
     */
	private Type _pointerType(String pointer) {
		TypedIOPort port = _getPort(pointer);

		// can't find the port
		if (port == null)
			return BaseType.UNKNOWN;

		// get the type
		Type type = port.getType();

		// we don't know the type or base type
		if (type.equals(BaseType.UNKNOWN))
			return BaseType.UNKNOWN;

		// exclude "atomic" base types and other funky types
		if (!(type instanceof StructuredType))
			return null;

		// get the steps
		String[] steps = pointer.split("/");
		if (steps.length <= 0)
			return null;

		// start it here
		for (int i = 1; i < steps.length; i++) {
			type = _applyStep(steps[i], type);
			if (type == null)
				return null;
		}

		return type;
	}

	private Type _applyStep(String step, Type type) {
		if (step.equals("elem")) {
			if (type instanceof ArrayType)
				return ((ArrayType) type).getElementType();
			if (type instanceof MatrixType.BooleanMatrixType)
				return BaseType.BOOLEAN;
			if (type instanceof MatrixType.DoubleMatrixType)
				return BaseType.DOUBLE;
			if (type instanceof MatrixType.FixMatrixType)
				return BaseType.FIX;
			if (type instanceof MatrixType.IntMatrixType)
				return BaseType.INT;
			if (type instanceof MatrixType.LongMatrixType)
				return BaseType.LONG;
		}
		if (type instanceof RecordType)
			return ((RecordType) type).get(step);

		return null;
	}

	/**
	 * A valid pointer takes the form: p/s_1/s_2/ ... /s_n for n > 0, where p is
	 * a port name, and s_1 to s_n are strings.
	 */
	private boolean _wellFormedPointer(String pointer) {
		if (pointer == null)
			return false;
		StringTokenizer st = new StringTokenizer(pointer, "/", true);
		if (st.countTokens() < 3)
			return false;
		boolean delimNeeded = false;

		while (st.hasMoreTokens()) {
			String str = st.nextToken();
			if (!delimNeeded && str.equals("/"))
				return false;
			if (delimNeeded && !str.equals("/"))
				return false;
			delimNeeded = true ? !delimNeeded : false;
		}

		return true;
	}

	public String getTargetName() {
		String[] steps = getName().split("/");
		// should be greater than zero
		if (!(steps.length > 0))
			return "";
		// grab port name
		return steps[steps.length - 1];
	}

	/**
     *
     */
	public String toString() {
		return getName();
	}

	/** PRIVATE MEMBERS */
	private String _pointer;

} // KeplerRefinementIOPort