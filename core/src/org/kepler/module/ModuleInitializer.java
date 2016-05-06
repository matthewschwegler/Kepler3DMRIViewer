/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.kepler.module;

/** An interface for module initialization. When Kepler starts,
 *  org.kepler.Kepler uses this interface to initialize each module
 *  in modules.txt.
 *  <b>NOTE: The name of the class implementing this interface
 *  must be called org.kepler.module.NNN.Initialize, where NNN
 *  is the name of the module.</b>
 *
 *
 * @author Daniel Crawl
 * @version $Id: ModuleInitializer.java 24234 2010-05-06 05:21:26Z welker $
 */

public interface ModuleInitializer
{
    /** Perform any module-specific initializations. */
    public void initializeModule();
}