/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-02-27 16:07:47 -0800 (Wed, 27 Feb 2013) $' 
 * '$Revision: 31500 $'
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

package org.kepler.job;

import java.util.Hashtable;

/**
 * JobManagerFactory singleton class to create JobManager objects. This class
 * provides a factory to store JobManager objects. The reference to a jobmanager
 * is with "<name>-user@host", where <name> is one of the supported jobmanagers,
 * e.g. Condor, PBS or LoadLeveler. This class should be used instead of the
 * JobManager class directly
 * 
 * <p>
 * 
 * @author Norbert Podhorszki
 */

public class JobManagerFactory {

	/* Singleton object */
	public final static JobManagerFactory instance = new JobManagerFactory();

	/* Private variables */
	private static Hashtable jmgrTable = new Hashtable();

	private JobManagerFactory() {
		// jmgrTable = new Hashtable();
	}

	/**
	 * return an existing job manager
	 * 
	 * @return the existing jobmanager or null
	 */
	public synchronized static JobManager get(String jmgrID) {
		return (JobManager) jmgrTable.get(jmgrID);
	}

	/**
	 * return an existing job manager
	 * 
	 * @return the existing jobmanager or null
	 */
	public synchronized static JobManager get(String supportname, String target) {
		return get(createKey(supportname, target));
	}

	/** return an existing job manager OR create one now */
	public synchronized static JobManager get(String supportname,
			String target, String binPath) throws JobException {
		JobManager jmgr;
		// System.out.println("org.kepler.job.JobManagerFactory.getJobManager(): supportclass = "
		// + supportname +
		// " target = " + target + " binPath = " + binPath );
		jmgr = (JobManager) jmgrTable.get(createKey(supportname, target));
		if (jmgr == null) {
			// System.out.println("Job Manager will now be created");
			jmgr = new JobManager();
			jmgr.selectJobManager(supportname, target, binPath);
			jmgrTable.put(createKey(supportname, target), jmgr);
		}
		return jmgr;
	}

	protected static String createKey(String supportname, String target) {
		return (supportname + "-" + target);
	}
	
	public synchronized static void removeJmgrFromTable(String supportname, String target) {
		jmgrTable.remove(createKey(supportname,target));
	}

}