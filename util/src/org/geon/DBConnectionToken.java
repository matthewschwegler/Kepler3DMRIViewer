/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

package org.geon;

import java.io.Serializable;
import java.sql.Connection;

import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

//////////////////////////////////////////////////////////////////////////
//// DBConnectionToken
/**
 * A token that contains a reference to a database connection. Note that when
 * this token constructed, the object passed to the constructor is not cloned.
 * Thus, care must be exercised to ensure that actors do not modify that object
 * in a nondeterministic way, unless such nondeterminism is acceptable.
 * 
 * @author jaeger
 * @version $Id: DBConnectionToken.java 24000 2010-04-28 00:12:36Z berkley $
 */
public class DBConnectionToken extends Token {

	/**
	 * Construct an empty token.
	 */
	public DBConnectionToken() {
		super();
	}

	/**
	 * Construct a token with a reference to the specified connection.
	 * 
	 * @exception IllegalActionException
	 *                If the argument is not of the appropriate type (may be
	 *                thrown by derived classes, but is not thrown here).
	 */
	public DBConnectionToken(Connection value) throws IllegalActionException {
		_value = value;
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Convert the specified token into an instance of DBConnectionToken. This
	 * method does lossless conversion. If the argument is already an instance
	 * of DBConnectionToken, it is returned without any change. Otherwise, if
	 * the argument is below DBConnectionToken in the type hierarchy, it is
	 * converted to an instance of DBConnectionToken and returned. If none of
	 * the above condition is met, an exception is thrown.
	 * 
	 * @param token
	 *            The token to be converted to a DBConnectionToken.
	 * @return A DBConnectionToken.
	 * @exception IllegalActionException
	 *                If the conversion cannot be carried out.
	 */
	public static DBConnectionToken convert(Token token)
			throws IllegalActionException {
		if (token instanceof DBConnectionToken) {
			return (DBConnectionToken) token;
		}

		throw new IllegalActionException(notSupportedConversionMessage(token,
				"dbconnection"));
	}

	/**
	 * Return true if the argument is an instance of DBConnectionToken and its
	 * contained object is equal to the object contained in this token, as
	 * tested by the equals() method of the contained object.
	 * 
	 * @param object
	 *            An instance of Object.
	 * @return True if the argument is an instance of DBConnectionToken and its
	 *         contained object is equal to the object contained in this token.
	 */
	public boolean equals(Object object) {
		// This test rules out subclasses.
		if (object.getClass() != DBConnectionToken.class) {
			return false;
		}

		if (((DBConnectionToken) object).getValue().equals(_value)) {
			return true;
		}
		return false;
	}

	/**
	 * Return the type of this token.
	 * 
	 * @return {@link #DBCONNECTION}, the least upper bound of all the database
	 *         connections
	 */
	public Type getType() {
		return DBCONNECTION;
	}

	/**
	 * Return the java.sql.connection.
	 * 
	 * @return The connection in this token.
	 */
	public Connection getValue() {
		return _value;
	}

	/**
	 * Return a hash code value for this token. This method returns the hash
	 * code of the contained object.
	 * 
	 * @return A hash code value for this token.
	 */
	public int hashCode() {
		return _value.hashCode();
	}

	/**
	 * Return the value of this token as a string that can be parsed by the
	 * expression language to recover a token with the same value. The returned
	 * syntax looks like a function call to a one argument method named
	 * "dbconnection". The argument is the string representation of the
	 * contained object, or the string "null" if the object is null. Notice that
	 * this syntax is not currently parseable by the expression language.
	 * 
	 * @return A String representing the object.
	 */
	public String toString() {
		if (_value != null) {
			return "dbconnection(" + _value.toString() + ")";
		} else {
			return "dbconnection(null)";
		}
	}

	/**
	 * The database connection type.
	 */
	public static class DBConnectionType implements Type, Serializable {

		// /////////////////////////////////////////////////////////////////
		// // constructors ////
		// The constructor is private to make a type safe enumeration.
		// We could extend BaseType, yet the BaseType(Class, String)
		// Constructor is private.
		private DBConnectionType() {
			super();
		}

		// /////////////////////////////////////////////////////////////////
		// // public methods ////

		/**
		 * Return a new type which represents the type that results from adding
		 * a token of this type and a token of the given argument type.
		 * 
		 * @param rightArgumentType
		 *            The type to add to this type.
		 * @return A new type, or BaseType.GENERAL, if the operation does not
		 *         make sense for the given types.
		 */
		public Type add(Type rightArgumentType) {
			return this;
		}

		/**
		 * Return this, that is, return the reference to this object.
		 * 
		 * @return A DBConnectionType
		 */
		public Object clone() {
			// FIXME: Note that we do not call super.clone() here. Is
			// that right?
			return this;
		}

		/**
		 * Convert the specified token to a token having the type represented by
		 * this object.
		 * 
		 * @param token
		 *            A token.
		 * @return A token.
		 * @exception IllegalActionException
		 *                If lossless conversion cannot be done.
		 */
		public Token convert(Token token) throws IllegalActionException {
			if (token instanceof DBConnectionToken) {
				return token;
			} else {
				throw new IllegalActionException("Attempt to convert token "
						+ token + " into a DBConnection token, "
						+ "which is not possible.");
			}
		}

		/**
		 * Return a new type which represents the type that results from
		 * dividing a token of this type and a token of the given argument type.
		 * 
		 * @param rightArgumentType
		 *            The type to add to this type.
		 * @return A new type, or BaseType.GENERAL, if the operation does not
		 *         make sense for the given types.
		 */
		public Type divide(Type rightArgumentType) {
			return this;
		}

		/**
		 * Return the class for tokens that this basetype represents. The
		 * DBConectionToken class.
		 */
		public Class getTokenClass() {
			return DBConnectionToken.class;
		}

		/**
		 * Return true if this type does not correspond to a single token class.
		 * This occurs if the type is not instantiable, or it represents either
		 * an abstract base class or an interface.
		 * 
		 * @return Always return false, this token is instantiable.
		 */
		public boolean isAbstract() {
			return false;
		}

		/**
		 * Test if the argument type is compatible with this type. The method
		 * returns true if this type is UNKNOWN, since any type is a
		 * substitution instance of it. If this type is not UNKNOWN, this method
		 * returns true if the argument type is less than or equal to this type
		 * in the type lattice, and false otherwise.
		 * 
		 * @param type
		 *            An instance of Type.
		 * @return True if the argument type is compatible with this type.
		 */
		public boolean isCompatible(Type type) {
			return type == this;
		}

		/**
		 * Test if this Type is UNKNOWN.
		 * 
		 * @return True if this Type is not UNKNOWN; false otherwise.
		 */
		public boolean isConstant() {
			return true;
		}

		/**
		 * Return this type's node index in the (constant) type lattice.
		 * 
		 * @return this type's node index in the (constant) type lattice.
		 */
		public int getTypeHash() {
			return Type.HASH_INVALID;
		}

		/**
		 * Determine if this type corresponds to an instantiable token classes.
		 * A BaseType is instantiable if it does not correspond to an abstract
		 * token class, or an interface, or UNKNOWN.
		 * 
		 * @return True if this type is instantiable.
		 */
		public boolean isInstantiable() {
			return true;
		}

		/**
		 * Return true if the argument is a substitution instance of this type.
		 * 
		 * @param type
		 *            A Type.
		 * @return True if this type is UNKNOWN; false otherwise.
		 */
		public boolean isSubstitutionInstance(Type type) {
			return this == type;
		}

		/**
		 * Return a new type which represents the type that results from
		 * moduloing a token of this type and a token of the given argument
		 * type.
		 * 
		 * @param rightArgumentType
		 *            The type to add to this type.
		 * @return A new type, or BaseType.GENERAL, if the operation does not
		 *         make sense for the given types.
		 */
		public Type modulo(Type rightArgumentType) {
			return this;
		}

		/**
		 * Return a new type which represents the type that results from
		 * multiplying a token of this type and a token of the given argument
		 * type.
		 * 
		 * @param rightArgumentType
		 *            The type to add to this type.
		 * @return A new type, or BaseType.GENERAL, if the operation does not
		 *         make sense for the given types.
		 */
		public Type multiply(Type rightArgumentType) {
			return this;
		}

		/**
		 * Return the type of the multiplicative identity for elements of this
		 * type.
		 * 
		 * @return A new type, or BaseType.GENERAL, if the operation does not
		 *         make sense for the given types.
		 */
		public Type one() {
			return this;
		}

		/**
		 * Return a new type which represents the type that results from
		 * subtracting a token of this type and a token of the given argument
		 * type.
		 * 
		 * @param rightArgumentType
		 *            The type to add to this type.
		 * @return BaseType.GENERAL because the operation does not make sense
		 *         for the given types.
		 */
		public Type subtract(Type rightArgumentType) {
			return BaseType.GENERAL;
		}

		/**
		 * Return the string representation of this type.
		 * 
		 * @return A String.
		 */
		public String toString() {
			return "dbconnection";
		}

		/**
		 * Return the type of the additive identity for elements of this type.
		 * 
		 * @return Return BaseType.GENERAL, because the operation does not make
		 *         sense for the given types.
		 */
		public Type zero() {
			return BaseType.GENERAL;
		}
	}

	/**
	 * The DBConnection type: the least upper bound of all the cryptographic key
	 * types.
	 */
	public static final Type DBCONNECTION = new DBConnectionType();

	// /////////////////////////////////////////////////////////////////
	// // protected variables ////

	/* The java.sql.Connection */
	protected Connection _value = null;
}