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

package org.kepler.dataproxy.datasource;

import java.net.URL;

/**
 * <p>
 * 
 * Title:PlugInQueryTransferInterface
 * </p>
 * <p>
 * 
 * Description: This is a plugin interface and it defines a all methods any
 * metadata type should implement into order to search ecogrid service and parse
 * the results
 * </p>
 * <p>
 * 
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * 
 * Company:
 * </p>
 * 
 *@author not attributable
 *@created February 17, 2005
 *@version 1.0
 */

public interface DataSourceInterface {
	// Constant

	public static final String ENDPOINT = "endpoint";
	public static final String RECORDID = "recordid";
	public static final String NAMESPACE = "namespace";

	public static final String YELLOW = "{1.0, 1.0, 0.0, 1.0}";
	public static final String RED = "{1.0, 0.0, 0.0, 1.0}";
	public static final String BLACK = "{0.0, 0.0, 0.0, 1.0}";
	public static final String MAGENTA = "{1.0, 0.0, 1.0, 1.0}";
	public static final String TITLE_BINARY = "0101";
	public static final String TITLE_BUSY = "BUSY";
	public static final String TITLE_ERROR = "ERROR";

	/**
	 * Get a URL pointer to the documentation for this data source.
	 * 
	 * @return URL the URL of the HTML file containing the documentation
	 */
	public abstract URL getDocumentation();

}