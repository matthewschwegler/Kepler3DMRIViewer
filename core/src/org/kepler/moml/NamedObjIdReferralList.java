/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-08-09 15:47:01 -0700 (Thu, 09 Aug 2012) $' 
 * '$Revision: 30395 $'
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

package org.kepler.moml;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;

/**
 * This StringAttribute holds a string of KeplerLSIDs separated by colons.
 * Do not use the setExpression method, instead use the addReferral method.
 * 
 * @author Aaron Schultz
 * 
 */
public class NamedObjIdReferralList extends StringAttribute {
	
	public static final String NAME = "derivedFrom";

	public NamedObjIdReferralList() {
		super();
        setVisibility(Settable.NONE);
	}

	public NamedObjIdReferralList(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
        setVisibility(Settable.NONE);
	}

    /** Add an LSID to the referral list. */
	public void addReferral(KeplerLSID lsid) throws IllegalActionException {
		String value = getExpression();
		if (value.equals("")) {
			value += lsid.toString();
		} else {
			value += ":" + lsid.toString();
		}
		setExpression(value);
	}

	/**
	 * This method returns a list of KeplerLSID objects. The first element in
	 * the list is the most recent KeplerLSID.
	 * 
	 * 	 * @throws Exception
	 */
	public List<KeplerLSID> getReferrals() throws Exception {
		String value = getExpression();
		StringTokenizer st = new StringTokenizer(value, KeplerLSID.separator);
		int cnt = st.countTokens();
		if (cnt == 0) {
			return (List) new Vector(0);
		}
		if (cnt % 6 != 0) {
			throw new Exception(
					NAME
							+ " list must contain properly formatted KeplerLSID strings.");
		}
		int lsidCnt = cnt / 6;
		Vector<KeplerLSID> referrals = new Vector<KeplerLSID>(lsidCnt);
		for (int i = 0; i < lsidCnt; i++) {
			String lsidStr = st.nextToken() + KeplerLSID.separatorChar 
					+ st.nextToken() + KeplerLSID.separatorChar
					+ st.nextToken() + KeplerLSID.separatorChar
					+ st.nextToken() + KeplerLSID.separatorChar
					+ st.nextToken() + KeplerLSID.separatorChar
					+ st.nextToken() + KeplerLSID.separatorChar;
			KeplerLSID lsid = new KeplerLSID(lsidStr);
			referrals.add(0, lsid); // add to beginning of Vector
		}
		return (List<KeplerLSID>) referrals;

	}
	
	public boolean hasReferral(KeplerLSID lsid) throws Exception {
		List<KeplerLSID> lsids = getReferrals();
		for (KeplerLSID thisLsid : lsids) {
			if (thisLsid.equals(lsid)) {
				return true;
			}
		}
		return false;
	}

    /** Remove an LSID from the referral list. */
    public void removeReferral(KeplerLSID removeLSID) throws Exception {

        // get the list of current referrals
        List<KeplerLSID> currentLSIDs = getReferrals();

        // remove from the current list
        List<KeplerLSID> keepLSIDs = new LinkedList<KeplerLSID>();
        for(KeplerLSID checkLSID : currentLSIDs)
        {
            if(!checkLSID.equals(removeLSID))
            {
                keepLSIDs.add(checkLSID);
            }
        }

        // clear the list and add all the ones we want to keep
        setExpression("");
        for(KeplerLSID addLSID : keepLSIDs)
        {
            addReferral(addLSID);
        }
    }
}
