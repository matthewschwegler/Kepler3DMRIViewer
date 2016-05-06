/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

package org.ROADnet;

import java.io.BufferedReader;
import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

import org.ROADnet.qaqc.DataQAQC.Range;
import org.ROADnet.qaqc.DataQAQC.RangeList;
import org.ROADnet.qaqc.DataQAQC.ReadConfig;
import org.ROADnet.qaqc.DataQAQC.RecordDef;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////QAQC
/**
 * Produce an output token on each firing with a value that is equal to a QAQC
 * checked version of the input.
 * 
 * @author Brandon Smith and Efrat Jaeger
 * @version $Id: QAQC.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0.1
 */

public class QAQC extends TypedAtomicActor {

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
	public QAQC(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input = new TypedIOPort(this, "input", true, false);
		new Attribute(input, "_showName");

		originalValue = new TypedIOPort(this, "originalValue", false, true);
		originalValue.setTypeEquals(BaseType.DOUBLE);
		new Attribute(originalValue, "_showName");
		modifiedValue = new TypedIOPort(this, "modifiedValue", false, true);
		modifiedValue.setTypeEquals(BaseType.DOUBLE);
		new Attribute(modifiedValue, "_showName");
		config = new FileParameter(this, "configuration file");
		checks = new StringParameter(this, "attribute");
		checks.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n"
				+ "<polygon points=\"-30,-10 -30,10 -10,10 -10,30 10,30 10,10"
				+ " 30,10 30,-10 10,-10 10,-30 -10,-30 -10,-10\" "
				+ "style=\"fill:red\"/>\n" + "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input port. This base class imposes no type constraints except that
	 * the type of the input cannot be greater than the type of the output.
	 */
	public TypedIOPort input;

	/**
	 * The output port. By default, the type of this output is constrained to be
	 * at least that of the input.
	 */
	public TypedIOPort modifiedValue;

	// TODO: make it configurable..
	/**
	 * The output port. By default, the type of this output is constrained to be
	 * at least that of the input.
	 */
	public TypedIOPort originalValue;

	/**
	 * The config file. This parameter is the location of the config file as a
	 * String
	 */
	public FileParameter config;

	/**
	 * The checks String. This parameter contains the columns to check as a
	 * String
	 */
	public StringParameter checks;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Callback for changes in attribute values Get the attributes from the
	 * config file.
	 * 
	 * @param a
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {
		if (at == config) {
			if (!config.getExpression().equals(_prevConfig)) { // If the value
																// has really
																// changed.

				_prevConfig = config.getExpression();
				checks.removeAllChoices();
				try {
					BufferedReader reader = config.openForReading();
					reader.readLine();
					String line = "";
					while ((line = reader.readLine()) != null) {
						if (!line.equals("")) {
							int space = line.indexOf(" ");
							if (space != -1) {
								checks.addChoice(line.substring(0, space));
							} else
								checks.addChoice(line);
						}
					}
					boolean inChoices = false;
					String[] choices = checks.getChoices();
					for (int i = 0; i < choices.length; i++) {
						if (checks.getExpression().equals(choices[i])) {
							inChoices = true;
							break;
						}
					}
					if (!inChoices)
						checks.setExpression("");
					reader.close();
				} catch (Exception ex) {
					_debug("<EXCEPTION> There was an error while parsing the config file. "
							+ ex + ". </EXCEPTION>");
					GraphicalMessageHandler
							.message(ex.getMessage()
									+ "There was an error while parsing the config file"
									+ config.getExpression() + "in the actor: "
									+ this.getName());
				}

			} else {
				_debug("The config file" + _prevConfig + " hasn't changed.");
			}
		} else {
			super.attributeChanged(at);
		}
	}

	/**
	 * Compute the product of the input and the <i>factor</i>. If there is no
	 * input, then produce no output.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		if (input.hasToken(0)) {

			// TODO: remote files..
			File config_file = config.asFile();
			String line = ((StringToken) input.get(0)).stringValue();
			String current_col = ((StringToken) checks.getToken())
					.stringValue();
			if (current_col.equals("")) {
				throw new IllegalActionException(this,
						"Please select an attribute for actor "
								+ this.getName() + ".");
			}

			// parse the CSV line into Vector data_list
			Vector data_list = new Vector();
			StringTokenizer st = new StringTokenizer(line);
			// don't need if 1 rec type: st.nextToken(","); // skip the first
			// token = record id
			while (st.hasMoreTokens())
				data_list.add(st.nextToken(","));

			// make call to read config info from file
			ReadConfig rc = new ReadConfig(config_file.getAbsolutePath());

			// only one type of record, so set the config as the single
			// RecordDef
			RecordDef config = (RecordDef) rc.getConfig().firstElement();

			// check the value equal to the current_col
			int index = ((Vector) config.getTypes()).indexOf(current_col);

			// get the value to check (as a double)
			double value = Double.parseDouble((String) data_list
					.elementAt(index));
			originalValue.broadcast(new DoubleToken(value));

			// get the range values, can have a set/list of ranges, but only 1
			// now
			RangeList list = (RangeList) ((Vector) config.getRanges())
					.elementAt(index);
			double high = ((Range) list.getRangeAt(0)).getHigh();
			double low = ((Range) list.getRangeAt(0)).getLow();

			// do checks, replace value by min if too low or max if too high
			// System.err.println(low + "<" + value + "<" + high);
			// check if too low
			if (value < low) {
				Double l = new Double(low);
				data_list.setElementAt(l.toString(), index);
			}
			// check if too high
			if (value > high) {
				Double h = new Double(high);
				data_list.setElementAt(h.toString(), index);
			}
			double modValue = Double.parseDouble((String) data_list
					.elementAt(index));

			modifiedValue.broadcast(new DoubleToken(modValue));
		}

	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {
		return super.postfire();
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Previous value of configuration file. */
	String _prevConfig = "";
}