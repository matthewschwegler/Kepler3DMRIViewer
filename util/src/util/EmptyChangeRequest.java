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

package util;

import ptolemy.kernel.util.ChangeRequest;

/**
 * <p>
 * This is a ChangeRequest that does not actually change anything; it is used to
 * fire an update of the workflow on the canvas (for example, to get the icons
 * to update after changes have been made). The benefit is that getBounds() is
 * called on he icon Figures, thus moving the actor ports to their correct
 * locations as part of the update
 * </p>
 * <p>
 * This actually seems like a bit of a hack. TODO - see if there's a better way
 * </p>
 * <p>
 * How to use:
 * </p>
 * <p>
 * // in the icon code - eg <code>ptolemy.vergil.icon</code>, get the<br/>
 * // <code>ptolemy.kernel.util.NamedObj</code> that is the top-level<br/>
 * // container:<br/>
 * NamedObj container = toplevel();<br/>
 * <br/>
 * //now create and issue the empty change request:<br/>
 * ChangeRequest request = new EmptyChangeRequest(this, "update request");<br/>
 * container.requestChange(request);<br/>
 * </p>
 */

public class EmptyChangeRequest extends ChangeRequest {

	public EmptyChangeRequest(Object o, String s) {
		super(o, s);
		setPersistent(false);
	}

	public void _execute() {
		// do nothing
	}
}