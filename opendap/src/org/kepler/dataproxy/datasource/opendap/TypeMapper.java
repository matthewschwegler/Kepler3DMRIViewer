/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009 OPeNDAP, Inc.
// All rights reserved.
// Permission is hereby granted, without written agreement and without
// license or royalty fees, to use, copy, modify, and distribute this
// software and its documentation for any purpose, provided that the above
// copyright notice and the following two paragraphs appear in all copies
// of this software.
//
// IN NO EVENT SHALL OPeNDAP BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF
// THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF OPeNDAP HAS BEEN ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.
//
// OPeNDAP SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
// BASIS, AND OPeNDAP HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
//
// Author: Nathan David Potter  <ndp@opendap.org>
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
//
/////////////////////////////////////////////////////////////////////////////

package org.kepler.dataproxy.datasource.opendap;

import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.BaseType;
import opendap.dap.DArray;
import opendap.dap.DByte;
import opendap.dap.DConstructor;
import opendap.dap.DDS;
import opendap.dap.DFloat32;
import opendap.dap.DFloat64;
import opendap.dap.DGrid;
import opendap.dap.DInt16;
import opendap.dap.DInt32;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DStructure;
import opendap.dap.DUInt16;
import opendap.dap.DUInt32;
import opendap.dap.DURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.data.type.ArrayType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

/**
 * Creates a mapping between DAP2 data types and teh kepler data model for use
 * in configuring output ports on an Actor.
 * 
 * @author Nathan Potter
 * @version $Id: TypeMapper.java 24000 2010-04-28 00:12:36Z berkley $
 * @since Kepler 1.0RC1 User: ndp Date: Aug 8, 2007 Time: 9:12:03 AM
 */
public class TypeMapper {

	private static Log log;
	static {
		log = LogFactory
				.getLog("org.kepler.dataproxy.datasource.opendap.TypeMapperWithMetadata");
	}

	public static String metadataName = TokenMapper.metadataName;
	public static String valueName = TokenMapper.valueName;

	/**
	 * Name prefix for The primary data array in a grid.
	 */
	public static final String gridArrayPrefix = "GridArray_";

	/**
	 * Name refix for a map vector in a grid.
	 */
	public static final String gridMapPrefix = "GridMap_";

	/**
	 * Maps a DAP2 DDS object to a list of kepler data types.
	 * 
	 * @param dds
	 *            The DDS to map to kepler.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return A collection (<code>Vector</code>) of kepler data types that
	 *         represent the top level variables in the passed DDS.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	public static Vector mapDDSToTypeList(DDS dds, boolean addMetadataRecord)
			throws IllegalActionException {

		Vector<Type> types = new Vector<Type>();

		Enumeration e = dds.getVariables();
		// Map each top level variable in the DDS to a kepler data type.
		while (e.hasMoreElements()) {
			types.add(mapDapObjectToType(
					(opendap.dap.BaseType) e.nextElement(), addMetadataRecord));
		}

		return types;
	}

	/**
	 * Maps a DAP2 variable (which is represented as an
	 * <code>opendap.dap.BaseType
     * </code> to a kepler data Type.
	 * 
	 * @param dapBT
	 *            The DAP2 variable to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return The corresponding Kepler Type
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	public static Type mapDapObjectToType(opendap.dap.BaseType dapBT,
			boolean addMetadataRecord) throws IllegalActionException {

		Type valueType = null;
		Type type;
		Type attType;

		if (dapBT instanceof DConstructor) {
			type = typemapDConstructorToRecord((DConstructor) dapBT,
					addMetadataRecord);

		} else {
			if (dapBT instanceof DByte) {
				valueType = ptolemy.data.type.BaseType.UNSIGNED_BYTE;

			} else if (dapBT instanceof DUInt16 || dapBT instanceof DInt16) {
				valueType = ptolemy.data.type.BaseType.INT;

			} else if (dapBT instanceof DUInt32 || dapBT instanceof DInt32) {
				valueType = ptolemy.data.type.BaseType.LONG;

			} else if (dapBT instanceof DFloat32 || dapBT instanceof DFloat64) {
				valueType = ptolemy.data.type.BaseType.DOUBLE;

			} else if (dapBT instanceof DURL || dapBT instanceof DString) {
				valueType = ptolemy.data.type.BaseType.STRING;

			}
			// Arrays are special...
			else if (dapBT instanceof DArray) {

				DArray a = (DArray) dapBT;

				int dimensions = a.numDimensions();

				switch (dimensions) {

				case 1: // Map 1D array to 1xN matrix
					valueType = typemapDArrayToMatrix((DArray) dapBT);
					break;

				case 2: // Map 2D array to MxN matrix
					valueType = typemapDArrayToMatrix((DArray) dapBT);
					break;

				default: // punt. Just map it to a Kepler Array (very
							// inefficient)
					valueType = typemapDArrayToArray((DArray) dapBT);

				}

			}
			if (valueType == null)
				throw new IllegalActionException("Unrecognized DAP2 type. "
						+ "TypeName=" + dapBT.getTypeName() + "  VariableName="
						+ dapBT.getName());

			if (addMetadataRecord) {
				attType = AttTypeMapper.convertAttributeToType(dapBT
						.getAttribute());
				type = new RecordType(new String[] { metadataName, valueName },
						new Type[] { attType, valueType });
			} else
				type = valueType;
		}

		if (type == null)
			throw new IllegalActionException("Unrecognized DAP2 type. "
					+ "TypeName=" + dapBT.getTypeName() + "  VariableName="
					+ dapBT.getName());

		return type;

	}

	/**
	 * Maps a DAP2 DConstructor to a Kepler Record Token. All the DAP2 types
	 * that inherit from DConstructor are composite types. I.e. they are
	 * composed of a collection of other types. Specifically:
	 * <ul>
	 * <li>DStructure</li>
	 * <li>DSequence</li>
	 * <li>DGrid</li>
	 * </ul>
	 * All contain one or more DAP2 types.
	 * 
	 * @param dc
	 *            The DConstructor to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return The Kepler Record Token.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static Type typemapDConstructorToRecord(DConstructor dc,
			boolean addMetadataRecord) throws IllegalActionException {

		// Make a handy Vector of the members of the collection.
		Vector<BaseType> v = new Vector<BaseType>();
		Enumeration e = dc.getVariables();
		while (e.hasMoreElements())
			v.add((BaseType) e.nextElement());

		Type[] types = new Type[v.size()];
		String[] labels = new String[v.size()];
		Type type, attType, valueType;

		int i = 0;

		// Map the collection.
		for (BaseType bt : v) {

			// Map this DAP2 Type.
			types[i] = mapDapObjectToType(bt, addMetadataRecord);
			labels[i] = replacePeriods(bt.getName());
			i++;
		}

		// Sequences are special - essential arrays of structures, so they map
		// to Arrays of Records.
		if (dc instanceof DSequence)
			valueType = new ArrayType(new RecordType(labels, types));
		else
			valueType = new RecordType(labels, types);

		if (addMetadataRecord) {
			attType = AttTypeMapper.convertAttributeToType(dc.getAttribute());
			type = new RecordType(new String[] { metadataName, valueName },
					new Type[] { attType, valueType });
		} else
			type = valueType;

		return type;
	}

	/**
	 * Attempts to map a 1 or2 dimensional DAP2 Array to MatrixType. Not all
	 * types can be mapped. Those that can't get mapped to an ArrayType.
	 * 
	 * @param a
	 *            The array to map.
	 * @return The Matrix type to which it's mapped.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static Type typemapDArrayToMatrix(opendap.dap.DArray a)
			throws IllegalActionException {

		if (a.numDimensions() != 2 && a.numDimensions() != 1) {
			throw new IllegalActionException("Cannot map a "
					+ a.numDimensions() + " array to a matrix.");
		}

		opendap.dap.BaseType dapBT = a.getPrimitiveVector().getTemplate();
		if (dapBT instanceof DByte || dapBT instanceof DUInt16
				|| dapBT instanceof DInt16) {

			return ptolemy.data.type.BaseType.INT_MATRIX;

		} else if (dapBT instanceof DUInt32 || dapBT instanceof DInt32) {
			return ptolemy.data.type.BaseType.LONG_MATRIX;

		} else if (dapBT instanceof DFloat32 || dapBT instanceof DFloat64) {
			return ptolemy.data.type.BaseType.DOUBLE_MATRIX;

		} else if (dapBT instanceof DURL || dapBT instanceof DString) {
			return new ArrayType(ptolemy.data.type.BaseType.STRING);

		} else if (dapBT instanceof DStructure || dapBT instanceof DGrid
				|| dapBT instanceof DSequence) {

			return new ArrayType(typemapDConstructorToRecord(
					(DConstructor) dapBT, false));

		}

		return ptolemy.data.type.BaseType.UNKNOWN;
	}

	/**
	 * Maps a DAP2 Array to Kepler ArrayType.
	 * 
	 * @param a
	 *            The array to map.
	 * @return The Matrix type to which it's mapped.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static ArrayType typemapDArrayToArray(opendap.dap.DArray a)
			throws IllegalActionException {

		Type primitiveType = ptolemy.data.type.BaseType.UNKNOWN;

		opendap.dap.BaseType dapBT = a.getPrimitiveVector().getTemplate();
		if (dapBT instanceof DByte) {
			primitiveType = ptolemy.data.type.BaseType.UNSIGNED_BYTE;

		} else if (dapBT instanceof DUInt16 || dapBT instanceof DInt16) {
			primitiveType = ptolemy.data.type.BaseType.INT;

		} else if (dapBT instanceof DUInt32 || dapBT instanceof DInt32) {
			primitiveType = ptolemy.data.type.BaseType.LONG;

		} else if (dapBT instanceof DFloat32 || dapBT instanceof DFloat64) {
			primitiveType = ptolemy.data.type.BaseType.DOUBLE;

		} else if (dapBT instanceof DURL || dapBT instanceof DString) {
			primitiveType = ptolemy.data.type.BaseType.STRING;

		} else if (dapBT instanceof DStructure || dapBT instanceof DGrid
				|| dapBT instanceof DSequence) {

			primitiveType = typemapDConstructorToRecord((DConstructor) dapBT,
					false);

		}

		ArrayType at = new ArrayType(primitiveType);
		for (int i = 1; i < a.numDimensions(); i++) {
			at = new ArrayType(at);

		}

		return at;
	}

	public static String replacePeriods(String str) {
		return str.replaceAll("\\.", "_");
	}
}
