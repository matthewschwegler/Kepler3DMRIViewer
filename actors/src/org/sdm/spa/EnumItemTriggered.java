/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.sdm.spa;

import java.util.StringTokenizer;
import java.util.Vector;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// Extract Item
/**
 * This Actor will extract items from an XML document. for now it will only
 * extract the first item.
 * 
 * @param input
 *            : The string that contains the XML content
 * @param xpath
 *            : The xpath for the item to be extracted.
 */

public class EnumItemTriggered extends TypedAtomicActor {

	/**
	 * Construct a constant source with the given container and name. Create the
	 * <i>value</i> parameter, initialize its value to the default value of an
	 * IntToken with value 1.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationedException
	 *                If the container already has an actor with this name.
	 * 
	 */
	public EnumItemTriggered(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		// super(container, name);
		xpath = new Parameter(this, "xpath", new StringToken(""));

		endOfLoop = new TypedIOPort(this, "endOfLoop", false, true);
		endOfLoop.setTypeEquals(BaseType.BOOLEAN);
		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.STRING);
		branchFinished = new TypedIOPort(this, "branchFinished", true, false);
		branchFinished.setTypeEquals(BaseType.BOOLEAN);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The value produced by this constant source. By default, it contains an
	 * IntToken with value 1. If the type of this token is changed during the
	 * execution of a model, then the director will be asked to redo type
	 * resolution.
	 */
	public Parameter xpath;
	public TypedIOPort input;
	public TypedIOPort output;
	public TypedIOPort endOfLoop;
	public TypedIOPort branchFinished;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Clone the actor into the specified workspace. This calls the base class
	 * and then sets the value public variable in the new object to equal the
	 * cloned parameter in that new object.
	 * 
	 * @param workspace
	 *            The workspace for the new object.
	 * @return A new actor.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	/*
	 * public Object clone(Workspace workspace) throws
	 * CloneNotSupportedException { Const newObject = (Const)
	 * super.clone(workspace); // Set the type constraint.
	 * newObject.output.setTypeAtLeast(newObject.value); return newObject; }
	 */

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {
		if (_reachedEND) {
			endOfLoop.broadcast(BooleanToken.TRUE);
			return true;
		}
		// endOfLoop.broadcast(BooleanToken.FALSE);
		return true;
	}

	/**
	 * Send the token in the <i>value</i> parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		p_input = ((StringToken) (input.get(0))).stringValue();
		p_xpath = ((StringToken) (xpath.getToken())).stringValue();
		// Now we need to extract the first item from the
		// XML content;
		_debug(" Broadcast:Input:" + p_input + ":" + p_xpath);
		Vector items;
		XMLUtil xmlutil = new XMLUtil();
		if (p_xpath.startsWith("/")) {
			items = xmlutil.getItems(p_input, p_xpath);
		} else {
			items = getStrItems(p_input, p_xpath);
		}
		String item;

		for (int i = 0; i < items.size(); i++) {
			item = (String) items.elementAt(i);
			if (item.length() > 0) {
				_debug(" Broadcast:Process Item:" + item);
				output.broadcast(new StringToken(item));
				boolean tg = ((BooleanToken) branchFinished.get(0))
						.booleanValue();
				if (tg)
					_debug("EnumItemTriggered:Got branch finished trigger.");
			}
			// Wait for the trigger to come back.

			/*
			 * //now wait for the return trigger //while (true) { boolean tg =
			 * ((BooleanToken) branchFinished.get(0)).booleanValue(); if (tg) {
			 * _debug(" Broadcast:Branching finished in Enum:" + item); //break;
			 * } else { try { Thread.currentThread().wait(100); } catch
			 * (InterruptedException e) { e.printStackTrace(); } }
			 */
			// }
		}
		// endOfLoop.broadcast(new IntToken(items.size()));
		_reachedEND = true;
	}

	private Vector getStrItems(String input, String xpath) {
		Vector result = new Vector();
		StringTokenizer st = new StringTokenizer(input, xpath);
		while (st.hasMoreElements()) {
			String token = st.nextToken();
			if (token.trim().length() > 0)
				result.add(token.trim());
		}

		return result;
	}

	private String p_input;
	private String p_xpath;

	/** Indicator that we have reached the end of file. */
	private boolean _reachedEND = false;

}