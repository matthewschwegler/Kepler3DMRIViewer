/*
 * Copyright (c) 2010 The Regents of the University of California.
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

/* CPES Actor for processing a stream of strings,
   waiting for a specific termination element Nth occurence
   and emit the termination element only that time.
   Do not use in SDF!
 */
/**
 *    '$RCSfile$'
 *
 *     '$Author: welker $'
 *       '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $'
 *   '$Revision: 24234 $'
 *
 *  For Details: http://www.kepler-project.org
 *
 * Copyright (c) 2004 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.sdm.spa;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.MonotonicFunction;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.graph.Inequality;
import ptolemy.graph.InequalityTerm;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// SyncOnTerminator

/**
 * <p>
 * Pass on a stream of tokens except for a specific element (termination) and
 * emit the termination when it is found in the stream
 * <i>NumberOfOccurences</i>th times.<br/>
 * The input should be a stream of tokens of a given type that matches the type
 * of the terminator parameter of this actor.
 * </p>
 * 
 * <p>
 * Input tokens are passed on as long as the termination element is found as
 * many times as specified. The termination element is not passed on until the
 * <i>NumberOfOccurences</i>th occurence. After the termination element is
 * emitted, incoming tokens will raise an Exception.
 * </p>
 * 
 * <p>
 * This actor can be used in the following stream processing scenario. A stream,
 * which has a termination token, is split and elements are processed in
 * parallel and then merged non-deterministically again. In such a case, if the
 * termination token is routed on only one branch, it can overcome other tokens
 * at the merge. So the termination token should be routed on ALL branches and
 * then after the merge this actor can help to wait for the last termination
 * token and thus ensuring that the termination token is the very last token in
 * the stream.
 * </p>
 * 
 * <p>
 * Note that for record types, you do not need to specify all fields within the
 * terminator. E.g. if you have a stream with {name/string, value=int}, you can
 * define the terminator, for instance, as {name="T"}.
 * </p>
 * 
 * <p>
 * If the flag <i>discardOthers</i> is set, the input tokens are NOT emitted on
 * output. That is, there is only one output, the termination token on its
 * <i>NumberOfOccurences</i>th occurence.
 * </p>
 * 
 * <p>
 * The actor outputs the input tokens.
 * </p>
 * 
 * <p>
 * This actor is not always producing a token for an input (namely for the
 * termination tokens), thus, it cannot be used in SDF. Since you do not have
 * parallel stream processing under SDF, this can hardly be a problem.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: SyncOnTerminator.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 6.0.2
 */
public class SyncOnTerminator extends TypedAtomicActor {
	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public SyncOnTerminator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		/*
		 * Input ports and port parameters
		 */

		// input
		input = new TypedIOPort(this, "input", true, false);
		new Parameter(input, "_showName", BooleanToken.FALSE);

		// The terminator element to wait for
		terminator = new Parameter(this, "terminator");

		// Number of occurences of the terminator element to wait for
		numberOfOccurences = new Parameter(this, "numberOfOccurences",
				new IntToken(1));
		numberOfOccurences.setTypeEquals(BaseType.INT);

		// Flag to indicate to discarding all non-terminator inputs
		discardOthers = new Parameter(this, "discardOthers", BooleanToken.FALSE);
		discardOthers.setTypeEquals(BaseType.BOOLEAN);

		/*
		 * Output ports
		 */

		// file name
		output = new TypedIOPort(this, "output", false, true);
		new Parameter(output, "_showName", BooleanToken.FALSE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * Input token. The type should match the terminator's type.
	 */
	public TypedIOPort input;

	/**
	 * The terminator element to wait for.
	 * 
	 */
	public Parameter terminator;

	/**
	 * The number of occurences of the terminator in the stream. The terminator
	 * will be emitted only at the last occurence.
	 */
	public Parameter numberOfOccurences;

	/**
	 * A flag to indicate whether non-terminator tokens should be passed on or
	 * discarded. If set, only one token will be ever emitted, name the
	 * terminator at the time of its last occurence.
	 */
	public Parameter discardOthers;

	/**
	 * The output token, which is always the input token.
	 */
	public TypedIOPort output;

	/***********************************************************
	 * public methods
	 */

	/**
	 * initialize() runs once before first exec
	 * 
	 * @exception IllegalActionException
	 *                If the parent class throws it.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_occured = 0;
	}

	/**
	 * Override the base class to determine which function is being specified.
	 * 
	 * @param attribute
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the function is not recognized.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == discardOthers) {
			_discard = ((BooleanToken) discardOthers.getToken()).booleanValue();
			if (isDebugging)
				log.debug("Changed attribute discardOthers to: " + _discard);
		} else if (attribute == numberOfOccurences) {
			_occurence = ((IntToken) numberOfOccurences.getToken()).intValue();
			if (isDebugging)
				log.debug("Changed attribute numberOfOccurences to: "
						+ _occurence);
		} else if (attribute == terminator) {
			_terminator = (Token) terminator.getToken();
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		Token _input = input.get(0);

		// error check: what if already terminated?
		if (_occured >= _occurence) {
			throw new IllegalActionException(
					this.getName()
							+ " : A token has arrived on input after the last occurence of the terminator. Max occurence = "
							+ _occurence + "; occured = " + _occured);

		}

		if (_isTerminator(_input)) {
			_occured++;
			if (_occured >= _occurence)
				output.send(0, _input); // emit the terminator finally

		} else if (!_discard) {
			output.send(0, _input); // pass-on the non-terminator
		}

	}

	/**
	 * Return the type constraints of this actor. The type constraints are (a)
	 * the input port type is at most the type of the terminator parameter's
	 * type; (b) in case of record types, the type of the output port equals to
	 * the type of the input port; (c) for all other types, the output port type
	 * is at least the terminator parameter's type;
	 * 
	 * This allows a terminator specification of {name="T"} and still passing
	 * {name=string, date=long,...} records through the actor and the type of
	 * the output port will be the longer record.
	 * 
	 * @return a list of Inequality.
	 */

	public List typeConstraintList() {
		List constraints = new LinkedList();

		_type = terminator.getType();

		// I. non-record types
		if (!(_type instanceof RecordType)) {
			input.setTypeAtMost(_type);
			output.setTypeEquals(_type);
			if (isDebugging)
				log.debug("typeConstraintList(). Type = " + _type
						+ " input type = " + input.getType()
						+ " output type = " + output.getType());
			return constraints;
		}

		// II. record types

		// ensure that input has the labels of the parameter record
		// i.e. can be a subtype, with more labels than the parameter
		input.setTypeAtMost(_type);

		// Declare that output has all the fields of the input port
		Inequality inequality = new Inequality(new FunctionTerm(), output
				.getTypeTerm());
		constraints.add(inequality);

		if (isDebugging)
			log.debug("typeConstraintList(). Type = " + _type
					+ " input type = " + input.getType() + " output type = "
					+ output.getType());
		return constraints;
	}

	/**
	 * Check if the incoming token is the terminator. For basic types, the
	 * tokens should be equal by the Token.equals() method. For record types,
	 * the terminator's all labels should exist in input, and their values
	 * should be equal (i.e. input record fully contains the terminator).
	 */
	private boolean _isTerminator(Token token) {
		// all types except records
		if (!(_type instanceof RecordType))
			return _terminator.equals(token);

		// check records
		Set terminatorLabelSet = ((RecordToken) _terminator).labelSet();
		Set tokenLabelSet = ((RecordToken) token).labelSet();
		Iterator iterator = terminatorLabelSet.iterator();

		while (iterator.hasNext()) {
			String label = (String) iterator.next();

			// check that the label is in input
			if (!tokenLabelSet.contains(label))
				return false;

			// check the values
			Token token1 = ((RecordToken) _terminator).get(label);
			Token token2 = ((RecordToken) token).get(label);

			if (!token1.equals(token2))
				return false;
		}
		return true; // _terminator record is fully contained in input
	}

	private int _occurence; // the max # of occurence of terminator
	private int _occured = 0; // the # of occurence of terminator so far
	private boolean _discard; // discard all non-terminator?
	private Token _terminator; // the terminator
	private Type _type; // the type of the terminator token

	private static final Log log = LogFactory.getLog(SyncOnTerminator.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////
	// This class implements a monotonic function of the input port
	// types. The value of the function is the record type of the
	// input record. To ensure that this function is monotonic, the
	// value of the function is bottom if the type of the port with
	// name "input" is bottom. Otherwise (it must be a record), the
	// value of the function is that record type.

	private class FunctionTerm extends MonotonicFunction {
		// /////////////////////////////////////////////////////////////
		// // public inner methods ////

		/**
		 * Return the function result.
		 * 
		 * @return A Type.
		 */
		public Object getValue() {
			Type inputType = input.getType();

			if (isDebugging)
				log.debug("FunctionTerm.getValue called. input type="
						+ inputType);

			if (!(inputType instanceof RecordType)) {
				return BaseType.UNKNOWN;
			}

			return (RecordType) inputType;

		}

		/**
		 * Return all the InequalityTerms for all input ports in an array.
		 * 
		 * @return An array of InequalityTerm.
		 */
		public InequalityTerm[] getVariables() {
			InequalityTerm[] variables = new InequalityTerm[1];
			variables[0] = input.getTypeTerm();
			return variables;
		}
	}

}
