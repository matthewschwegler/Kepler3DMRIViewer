/* A interface for modules to start and stop HSQL servers.
 * 
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2011-08-05 10:43:01 -0700 (Fri, 05 Aug 2011) $' 
 * '$Revision: 28216 $'
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

/** An interface for modules to start and stop HSQL servers. 
 * 
 *  If -hsql [start|stop] is specified on the command line,
 *  org.kepler.Kepler uses this interface to control HSQL
 *  servers managed by each module in modules.txt.
 *  <b>NOTE: The name of the class implementing this interface
 *  must be called org.kepler.module.NNN.HSQLManager, where NNN
 *  is the name of the module.</b>
 *
 *  @author Daniel Crawl
 *  @version $Id: ModuleHSQLManager.java 28216 2011-08-05 17:43:01Z crawl $
 */

public interface ModuleHSQLManager
{
    /** Start HSQL servers in a separate process. */
    public void start();
    
    /** Stop HSQL servers in a separate process. */
    public void stop();
}
