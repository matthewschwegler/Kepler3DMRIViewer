/* Perform cleanup for the core module.
 * 
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 12:14:09 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31108 $'
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
package org.kepler.module.core;

import org.kepler.module.ModuleShutdownable;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.LSIDGenerator;
import org.kepler.util.sql.HSQL;

/** A class to perform cleanup for the core module.
 * 
 * @author Daniel Crawl
 * @version $Id: Shutdown.java 31108 2012-11-26 20:14:09Z crawl $
 */
public class Shutdown implements ModuleShutdownable
{

    /** Perform any module-specific cleanup. */
    public void shutdownModule()
    {
        // close the lsid database;
        LSIDGenerator.closeDatabase();
        
        // shutdown the cache manager
        CacheManager.shutdown();
        
        // shutdown down hsql servers so that database files are
        // properly closed.
        HSQL.shutdownServers();
    }
}
