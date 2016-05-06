/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

package org.kepler.sms.util;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.kepler.sms.SemanticType;

import ptolemy.kernel.util.NamedObj;

/**
 * A Utility class for accessing common sms functions through static methods.
 * @author Aaron Schultz
 */
public class SMSUtil {

	/**
     * Copied into core module from the sms module.
     * Duplicate method of org.kepler.sms.SMSServices.getActorSemanticTypes(NamedObj)
     */
	public static Vector<SemanticType> getActorSemanticTypes(NamedObj obj) {
		Vector<SemanticType> result = new Vector<SemanticType>();
		List<SemanticType> semAtts = obj.attributeList(SemanticType.class);
		Iterator<SemanticType> iter = semAtts.iterator();
		while (iter.hasNext())
			result.add(iter.next());
		return result;
	}
}
