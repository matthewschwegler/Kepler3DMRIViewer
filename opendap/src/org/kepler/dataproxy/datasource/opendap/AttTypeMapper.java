/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2009 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package org.kepler.dataproxy.datasource.opendap;

import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.Attribute;
import opendap.dap.AttributeTable;
import opendap.dap.BaseType;
import opendap.dap.DConstructor;
import opendap.dap.NoSuchAttributeException;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

/**
 * User: ndp Date: Jun 16, 2008 Time: 4:41:03 PM
 */
public class AttTypeMapper {

	/**
	 * 
	 * @param bt
	 *            The BaseType object from which to extract the DAP2 Attributes
	 *            (aka semantic metadata in DAP2 speak.)
	 * @return The ptII Type that contains the metadata structure.
	 * @throws IllegalActionException
	 *             When the bad things happen.
	 */
	public static Type buildMetaDataTypes(BaseType bt)
			throws IllegalActionException {

		Type type;
		String s;
		// String namePrefix = bt.getLongName()+"@";

		if (bt instanceof DConstructor) {

			Vector<Type> typeVector = new Vector<Type>();
			Vector<String> labelVector = new Vector<String>();

			Attribute thisAtt;
			AttributeTable attTable = bt.getAttribute().getContainerN();
			Enumeration e = attTable.getNames();
			while (e.hasMoreElements()) {

				s = (String) e.nextElement();
				thisAtt = attTable.getAttribute(s);
				// attName = thisAtt.getName();
				typeVector.add(convertAttributeToType(thisAtt));
				labelVector.add(TypeMapper.replacePeriods(thisAtt.getName()));
			}

			BaseType thisBT;
			e = ((DConstructor) bt).getVariables();
			while (e.hasMoreElements()) {
				thisBT = (BaseType) e.nextElement();

				typeVector.add(buildMetaDataTypes(thisBT));
				labelVector.add(TypeMapper.replacePeriods(thisBT.getName()));
			}
			Type[] types = new Type[0];
			types = typeVector.toArray(types);

			String[] labels = new String[0];
			labels = labelVector.toArray(labels);

			type = new RecordType(labels, types);

		} else {
			type = convertAttributeToType(bt.getAttribute());
		}

		return type;

	}

	/**
	 * 
	 * @param att
	 *            The Attribute to map to a Kepler Type.
	 * @return The Type.
	 * @throws IllegalActionException
	 *             When the bad things happen.
	 */
	public static Type convertAttributeToType(Attribute att)
			throws IllegalActionException {

		Type type;
		String thisAttName;
		// String name = labelPrefix+att.getName();
		Enumeration values;

		Vector<Type> typeVector = new Vector<Type>();
		Type[] types = new Type[0];

		Vector<String> labelVector = new Vector<String>();
		String[] labels = new String[0];

		int i = 0;

		try {

			if (att.getType() == Attribute.CONTAINER) {

				Attribute thisAtt;
				// Make a handy Vector of the members of the collection.
				AttributeTable attTable = att.getContainerN();
				Enumeration e = attTable.getNames();
				while (e.hasMoreElements()) {

					thisAttName = (String) e.nextElement();
					thisAtt = attTable.getAttribute(thisAttName);
					typeVector.add(convertAttributeToType(thisAtt));
					labelVector.add(TypeMapper
							.replacePeriods(thisAtt.getName()));

				}
				types = typeVector.toArray(types);
				labels = labelVector.toArray(labels);
				type = new RecordType(labels, types);
			} else if (att.getNumVal() > 1) {

				values = att.getValues();
				while (values.hasMoreElements()) {

					values.nextElement();

					typeVector.add(mapSimpleTypeToType(att.getType()));
					labelVector.add("value_" + (i++));
				}
				types = typeVector.toArray(types);
				labels = labelVector.toArray(labels);
				type = new RecordType(labels, types);
			} else {
				type = mapSimpleTypeToType(att.getType());
			}

			return type;
		} catch (NoSuchAttributeException e) {
			throw new IllegalActionException(e.getMessage());
		}

	}

	public static Type mapSimpleTypeToType(int attType)
			throws IllegalActionException {

		Type type;

		switch (attType) {

		case Attribute.BYTE:
			type = ptolemy.data.type.BaseType.UNSIGNED_BYTE;
			break;

		case Attribute.FLOAT32:
		case Attribute.FLOAT64:
			type = ptolemy.data.type.BaseType.DOUBLE;
			break;

		case Attribute.UINT16:
		case Attribute.INT16:
			type = ptolemy.data.type.BaseType.INT;
			break;

		case Attribute.INT32:
		case Attribute.UINT32:
			type = ptolemy.data.type.BaseType.LONG;
			break;

		case Attribute.STRING:
		case Attribute.URL:
			type = ptolemy.data.type.BaseType.STRING;
			break;

		case Attribute.ALIAS:
		case Attribute.UNKNOWN:
		default:
			type = ptolemy.data.type.BaseType.STRING;
			break;

		}
		return type;
	}

	/**
	 * 
	 * @param bt
	 *            The BaseType object from which to extract the DAP2 Attributes
	 *            (aka semantic metadata in DAP2 speak.)
	 * @return The ptII Token that contains the structured metadata values.
	 * @throws IllegalActionException
	 *             When the bad things happen.
	 */
	public static Token buildMetaDataTokens(BaseType bt)
			throws IllegalActionException {

		Token token;
		String s;

		if (bt instanceof DConstructor) {

			Vector<Token> typeVector = new Vector<Token>();
			Vector<String> labelVector = new Vector<String>();

			Attribute thisAtt;
			AttributeTable attTable = bt.getAttribute().getContainerN();
			Enumeration e = attTable.getNames();
			while (e.hasMoreElements()) {

				s = (String) e.nextElement();
				thisAtt = attTable.getAttribute(s);
				typeVector.add(convertAttributeToToken(thisAtt));
				labelVector.add(TypeMapper.replacePeriods(thisAtt.getName()));
			}

			BaseType thisBT;
			e = ((DConstructor) bt).getVariables();
			while (e.hasMoreElements()) {
				thisBT = (BaseType) e.nextElement();

				typeVector.add(buildMetaDataTokens(thisBT));
				labelVector.add(TypeMapper.replacePeriods(thisBT.getName()));
			}
			Token[] types = new Token[0];
			types = typeVector.toArray(types);

			String[] labels = new String[0];
			labels = labelVector.toArray(labels);

			token = new RecordToken(labels, types);

		} else {
			token = convertAttributeToToken(bt.getAttribute());
		}

		return token;

	}

	/**
	 * 
	 * @param att
	 *            The Attribute to map to a Kepler Type.
	 * @return The Type.
	 * @throws IllegalActionException
	 *             When the bad things happen.
	 */
	public static Token convertAttributeToToken(Attribute att)
			throws IllegalActionException {

		Token token;
		String s, thisAttName;
		Enumeration values;

		Vector<Token> tokenVector = new Vector<Token>();
		Token[] types = new Token[0];

		Vector<String> labelVector = new Vector<String>();
		String[] labels = new String[0];

		int i = 0;

		try {

			if (att.getType() == Attribute.CONTAINER) {
				Attribute thisAtt;
				// Make a handy Vector of the members of the collection.
				AttributeTable attTable = att.getContainerN();
				Enumeration e = attTable.getNames();
				while (e.hasMoreElements()) {
					thisAttName = (String) e.nextElement();
					thisAtt = attTable.getAttribute(thisAttName);
					tokenVector.add(convertAttributeToToken(thisAtt));
					labelVector.add(TypeMapper.replacePeriods(thisAttName));
				}
				types = tokenVector.toArray(types);
				labels = labelVector.toArray(labels);
				token = new RecordToken(labels, types);
			} else if (att.getNumVal() > 1) {

				values = att.getValues();
				while (values.hasMoreElements()) {
					s = (String) values.nextElement();
					tokenVector.add(mapSimpleTypeToToken(att.getType(), s));
					labelVector.add("value_" + (i++));
				}
				types = tokenVector.toArray(types);
				labels = labelVector.toArray(labels);
				token = new RecordToken(labels, types);
			} else {
				s = att.getValueAt(0);
				token = mapSimpleTypeToToken(att.getType(), s);
			}
			return token;

		} catch (NoSuchAttributeException e) {
			throw new IllegalActionException(e.getMessage());
		}

	}

	public static Token mapSimpleTypeToToken(int attType, String attValue)
			throws IllegalActionException {

		Token token;

		switch (attType) {

		case Attribute.BYTE:

			token = new UnsignedByteToken(attValue);
			break;

		case Attribute.FLOAT32:
		case Attribute.FLOAT64:

			token = new DoubleToken(attValue);
			break;

		case Attribute.UINT16:
		case Attribute.INT16:
			token = new IntToken(attValue);
			break;

		case Attribute.INT32:
		case Attribute.UINT32:

			token = new LongToken(attValue);
			break;

		case Attribute.STRING:
		case Attribute.URL:

			token = new StringToken(attValue);
			break;

		case Attribute.ALIAS:
		case Attribute.UNKNOWN:
		default:

			token = new StringToken(attValue);
			break;

		}
		return token;
	}

}
