/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-07-02 16:10:26 -0700 (Tue, 02 Jul 2013) $' 
 * '$Revision: 32183 $'
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

package org.sdm.spa;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.XMLToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
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
import ptolemy.kernel.util.Settable;

//////////////////////////////////////////////////////////////////////////
//// ArrayPermute 
/**
 * Create all permutations of input arrays. If <i>outputAll</i> is true, the
 * output is an array; otherwise this actor produces the next permutation each
 * time it fires. The permutation type is selected via <i>outputType</i>: either
 * a record or an XML document.
 *
 *  <p>Example: </p>
 *      <p> input port a: {1, 2} </p>
 *      <p> input port b: {"foo", "bar"} </p>
 *      <p> output: {{a=1, b="foo"}, {a=1, b="bar"}, {a=2, b="foo"}, {a=2, b="bar"}} </p>
 * 
 *
 * @author Daniel Crawl
 * @version $Id: ArrayPermute.java 32183 2013-07-02 23:10:26Z jianwu $
 */

public class ArrayPermute extends TypedAtomicActor {
	/**
	 * Construct an ArrayPermute with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public ArrayPermute(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// set default output type.
		_curOutputKind = OutputKind.Record;

		outputType = new StringParameter(this, "outputType");
		outputType.setExpression(_curOutputKind.getName());

		for (OutputKind kind : OutputKind.values()) {
			outputType.addChoice(kind.getName());
		}

		output = new TypedIOPort(this, "output", false, true);

		outputAll = new Parameter(this, "outputAll", new BooleanToken(true));
		outputAll.setTypeEquals(BaseType.BOOLEAN);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** The permutation output. */
	public TypedIOPort output = null;

	/** The type of output: an array of records or an array of XML tokens. */
	public StringParameter outputType = null;

	/**
	 * If true, output all permutations in an array. Otherwise output as
	 * individual tokens.
	 */
	public Parameter outputAll = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	public void preinitialize() throws IllegalActionException {
		super.preinitialize();

		_remaining = -1;

		Object[] portArray = inputPortList().toArray();
		for (int i = 0; i < portArray.length; i++) {
			TypedIOPort port = (TypedIOPort) portArray[i];
			Parameter param = (Parameter) port
					.getAttribute("tokenConsumptionRate");

			try {
				if (_outputAllVal && param != null) {
					// remove
					param.setContainer(null);
				} else if (param == null) {
					param = new Parameter(port, "tokenConsumptionRate");
					param.setVisibility(Settable.NOT_EDITABLE);
					param.setTypeEquals(BaseType.INT);
				}
			} catch (NameDuplicationException e) {
				throw new IllegalActionException(this, e.getMessage());
			}
		}
	}

	public void initialize() throws IllegalActionException {
		super.initialize();

		if (!_outputAllVal) {
			_setTokenConsumptionRate(IntToken.ONE);
		}
	}

	/**
	 * React to a change in attributes.
	 * 
	 * @param attribute
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == outputType) {
			String str = outputType.getExpression();

			if (str == null) {
				throw new IllegalActionException(this, "Empty outputType.");
			} else {
				// make sure it's a valid type.
				OutputKind kind = OutputKind.getKind(str);

				if (kind == null) {
					throw new IllegalActionException(this,
							"Unknown output type: " + str);
				}

				_curOutputKind = kind;
			}
		} else if (attribute == outputAll) {
			_outputAllVal = ((BooleanToken) outputAll.getToken())
					.booleanValue();
		} else {
			super.attributeChanged(attribute);
		}
	}

	/** Compute the permutations of the inputs and send to output port. */
	public void fire() throws IllegalActionException {
		super.fire();

		if (_remaining == -1) {
			List list = inputPortList();
			if (list.size() > 0) {
				int count = 1;

				_data = new LinkedList();

				_labels = new String[list.size()];
				_curPerm = new Token[list.size()];

				Object ports[] = list.toArray();
				for (int i = 0; i < ports.length; i++) {
					TypedIOPort p = (TypedIOPort) ports[i];

					// collect the input port names
					_labels[i] = p.getName();

					// collect all the input data
					Token token = p.get(0);
					ArrayToken array;

					if(token instanceof ArrayToken) {
						array = (ArrayToken) token;
					} else {
						array = new ArrayToken(new Token[] {token});
					}

					_data.add(i, array);

					// increment the number of elements in all permutations
					count *= array.length();
				}

				// allocate the permutation set
				_set = _curOutputKind.allocateSet(count);
				_setNext = 0;

				// recursively build the set and then output it.
				_permutate(list.size() - 1);

				if (_outputAllVal) {
					output.broadcast(new ArrayToken(_set));
				} else {
					_remaining = _set.length - 1;
					output.broadcast(_set[_remaining]);
					_remaining--;
					_setTokenConsumptionRate(IntToken.ZERO);
				}
			}
		} else {
			output.broadcast(_set[_remaining]);
			_remaining--;

			if (_remaining == -1) {
				_setTokenConsumptionRate(IntToken.ONE);
			}
		}
	}

    /** Return the type constraints of this actor. The type constraint is
     *  that the type of the output ports is no less than the type of the
     *  fields of the input RecordToken.
     *  @return a list of Inequality.
     */
    protected Set<Inequality> _customTypeConstraints() {

        // Set the constraints between record fields and output ports.
        Set<Inequality> constraints = new HashSet<Inequality>();

        // Since the input port has a clone of the above RecordType, need to
        // get the type from the input port.
        //   RecordType inputTypes = (RecordType)input.getType();
        Iterator<?> outputPorts = outputPortList().iterator();

        while (outputPorts.hasNext()) {
            TypedIOPort outputPort = (TypedIOPort) outputPorts.next();
            //String label = outputPort.getName();
            Inequality inequality = new Inequality(new FunctionTerm(),
                    outputPort.getTypeTerm());
            constraints.add(inequality);
        }

        return constraints;
    }

    /**
     * Do not establish the usual default type constraints.
     * @return null
     */
    @Override
    protected Set<Inequality> _defaultTypeConstraints() {
        // See TypedAtomicActor for details.
        return null;
    }

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Recursively create a set of input permutations. */
	private void _permutate(int level) throws IllegalActionException {
		if (level >= 0) {
			ArrayToken array = (ArrayToken) _data.get(level);
			for (int i = 0; i < array.length(); i++) {
				_curPerm[level] = array.getElement(i);

				// base case
				if (level == 0) {
					// generate the token for the current permutation and
					// add it to the set.

					_set[_setNext] = _makeToken();
					_setNext++;
				} else {
					// recurse
					_permutate(level - 1);
				}
			}
		}
	}

	/** Make a token based on the current permutation. */
	private Token _makeToken() throws IllegalActionException {
		Token retval = null;
		switch (_curOutputKind) {
		case XML:
			String str = _genXML();

			try {
				retval = new XMLToken(str);
			} catch (Exception e) {
				throw new IllegalActionException(this,
						"Exception creating new XMLToken: " + e.getMessage());
			}
			break;

		case Record:
			retval = new RecordToken(_labels, _curPerm);
			break;
		}

		return retval;
	}

	/** Generate an XML string of the given permutation. */
	private String _genXML() {
		StringBuffer retval = new StringBuffer("<");
		retval.append(_XML_ROOT_NAME);
		retval.append(">");

		for (int i = 0; i < _curPerm.length; i++) {
			String tokenStr = null;

			// if the token is a StringToken, do stringValue so we
			// don't get the surrounding quotes.
			if (_curPerm[i] instanceof StringToken) {
				tokenStr = ((StringToken) _curPerm[i]).stringValue();
			} else {
				tokenStr = _curPerm[i].toString();
			}

			retval.append("<" + _labels[i] + ">" + tokenStr + "</" + _labels[i]
					+ ">");
		}
		retval.append("</");
		retval.append(_XML_ROOT_NAME);
		retval.append(">");
		return retval.toString();
	}

	/** Set the token consumption rate for all the input ports. */
	private void _setTokenConsumptionRate(IntToken token)
			throws IllegalActionException {
		Object[] portArray = inputPortList().toArray();
		for (int i = 0; i < portArray.length; i++) {
			TypedIOPort port = (TypedIOPort) portArray[i];
			Parameter param = (Parameter) port
					.getAttribute("tokenConsumptionRate");
			param.setToken(token);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private classes ////

	/** A enumeration for the kinds of outputs */
	private enum OutputKind {
		XML("XML"), Record("Record");

		OutputKind(String name) {
			_name = name;
		}

		public String getName() {
			return _name;
		}

		public static OutputKind getKind(String name) {
			if (name == null) {
				return null;
			} else if (name.equals("XML")) {
				return XML;
			} else if (name.equals("Record")) {
				return Record;
			} else {
				return null;
			}
		}

		/** Allocate a token array of size <i>count</i>. */
		public Token[] allocateSet(int count) {
			Token[] retval = null;
			switch (this) {
			case XML:
				retval = new XMLToken[count];
				break;
			case Record:
				retval = new RecordToken[count];
				break;
			}
			return retval;
		}

		private String _name;
	}

	/** A class to determine the type of the output port. */
	private class FunctionTerm extends MonotonicFunction {
		/*
		 * Get the type. If output kind is XML, return array of xml. Otherwise,
		 * return array of records with a label and value type corresponding to
		 * each input type.
		 */
		public Object getValue() {
			Type retval = null;
			switch (_curOutputKind) {
			case XML:
				retval = new ArrayType(BaseType.XMLTOKEN);
				break;

			case Record:
				Object[] portArray = inputPortList().toArray();
				String labels[] = new String[portArray.length];
				Type types[] = new Type[portArray.length];
				for (int i = 0; i < portArray.length; i++) {
					TypedIOPort port = (TypedIOPort) portArray[i];
					labels[i] = port.getName();
					Type inType = port.getType();

					if (inType instanceof ArrayType) {
						types[i] = ((ArrayType) inType).getElementType();
					} else {
						types[i] = inType;
					}

				}

				RecordType recordType = new RecordType(labels, types);

				if (_outputAllVal) {
					retval = new ArrayType(recordType);
				} else {
					retval = recordType;
				}
				break;
			}

			//System.out.println("returning type = " + retval);
			return retval;
		}

		/**
		 * Return the variables. If output is XML or have no inputs, return
		 * array of size 0. Otherwise, return terms from input ports.
		 */
		public InequalityTerm[] getVariables() {
			InequalityTerm[] retval = null;

			Object[] portArray = inputPortList().toArray();
			if (portArray.length == 0 || _curOutputKind == OutputKind.XML) {
				retval = new InequalityTerm[0];
			} else {
				List<InequalityTerm> terms = new LinkedList<InequalityTerm>();
				for (int i = 0; i < portArray.length; i++) {
					TypedIOPort port = (TypedIOPort) portArray[i];
                    InequalityTerm term = port.getTypeTerm();
                    if(term.isSettable()) {
                        terms.add(term);
                    }
				}
				retval = terms.toArray(new InequalityTerm[0]);
			}

			return retval;
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/** A list of the input arrays. */
	private LinkedList _data = null;

	/** The set of permutations. */
	private Token _set[] = null;

	/** The next index to use in the set. */
	private int _setNext = 0;

	/** The names of the input arrays. */
	private String _labels[] = null;

	/** The current permutation. */
	private Token _curPerm[] = null;

	/** XML root element's name */
	private static final String _XML_ROOT_NAME = "perm";

	/** The currently selected output type. */
	private OutputKind _curOutputKind;

	/**
	 * If true, output array of all permutations, else output next permutation.
	 */
	private boolean _outputAllVal;

	/** Index of next permutation to output. */
	private int _remaining;
}
