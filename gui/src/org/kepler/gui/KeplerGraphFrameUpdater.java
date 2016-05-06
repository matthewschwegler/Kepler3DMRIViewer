/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2010-09-21 12:00:44 -0700 (Tue, 21 Sep 2010) $' 
 * '$Revision: 25786 $'
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

package org.kepler.gui;

/**
 * An interface for updating components of a KeplerGraphFrame.
 *
 * Instances of this interface are used in a PriorityQueue in KeplerGraphFrame;
 * a ClassCastException may be thrown if not a Comparable.
 *
 * @author Daniel Crawl
 * @version $Id: KeplerGraphFrameUpdater.java 25786 2010-09-21 19:00:44Z jianwu $
 */

public interface KeplerGraphFrameUpdater extends Comparable<KeplerGraphFrameUpdater>
{
    /** Update the components. */
    public void updateFrameComponents(KeplerGraphFrame.Components components);
    
    /** clear when a frame is close. */
    public void dispose(KeplerGraphFrame frame);
    
}