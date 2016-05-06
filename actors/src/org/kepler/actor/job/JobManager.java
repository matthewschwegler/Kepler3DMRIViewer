/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-09-14 15:48:44 -0700 (Fri, 14 Sep 2012) $' 
 * '$Revision: 30678 $'
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

package org.kepler.actor.job;

//import org.kepler.job.JobManager; // same name...
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationNamespace;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.job.JobException;
import org.kepler.job.JobManagerFactory;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// JobManager

/**
 * <p>
 * Define a jobmanager on the local/remote machine
 * </p>
 * 
 * <p>
 * This actor uses the org.kepler.job.JobManagerFactory and org.kepler.job
 * JobManager classes to define a job manager
 * </p>
 * 
 * <p>
 * The input should be:
 * </p>
 * <ul>
 * <li>the supporter class' name, e.g. Condor, PBS, LoadLeveler or whatever for
 * that a class org.kepler.job.JobSupporter&lt;name&gt;.class exists</li>
 * <li>the target machine, either null, "" or "local" to denote a local
 * jobmanager to be used by local execution command, OR "[user@]host" to denote
 * a remote jobmanager to be used by an ssh connection.</li>
 * </ul>
 * 
 * <p>
 * The output is the created jobmanager of type ObjectToken, which should be
 * given as parameter to a JobSubmitter.
 * </p>
 * 
 * <p>
 * On error, an empty string is returned.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: JobManager.java 30678 2012-09-14 22:48:44Z jianwu $
 * @since Ptolemy II 5.0.1
 */
public class JobManager extends TypedAtomicActor {
	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public JobManager(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());

		// jobManager denotes the name of the actual job manager
		jobManager = new PortParameter(this, "jobManager", new StringToken(
				"SGE"));
		// jobManager.setStringMode(true); // string mode (no "s, but no
		// variables as well!
		jobManager.setStringMode(true);
		cp = ConfigurationManager.getInstance()
				.getProperty(ConfigurationManager.getModule("actors"),
						new ConfigurationNamespace("JobLauncher"));
		properties = cp.getProperties("value", true);
		for (ConfigurationProperty property : properties) {
			jobManager.addChoice(property.getValue());
		}
		new Parameter(jobManager.getPort(), "_showName", BooleanToken.TRUE);

		// target selects the machine where the jobmanager is running
		target = new PortParameter(this, "target", new StringToken(
				"[local | [user]@host]"));
		new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);
		target.setStringMode(true);

		// binPath is the full path to the jobmanager commands on the target
		// machine
		binPath = new PortParameter(this, "binPath", new StringToken(
				"/path/to/[remote]jobmanager/bin"));
		new Parameter(binPath.getPort(), "_showName", BooleanToken.TRUE);
		binPath.setStringMode(true);

		jmgr = new TypedIOPort(this, "jmgr", false, true);
		jmgr.setTypeEquals(BaseType.OBJECT);
		new Parameter(jmgr, "_showName", BooleanToken.FALSE);
	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * The name of the jobmanager to be used It should be a name, for which a
	 * supporter class exist as <i>org.kepler.job.JobSupport<jobManager>.class
	 * 
	 * This parameter is read each time in fire().
	 */
	public PortParameter jobManager;

	/**
	 * The machine to be used at job submission. It should be null, "" or
	 * "local" for the local machine or [user@]host to denote a remote machine
	 * accessible with ssh.
	 * 
	 * This parameter is read each time in fire().
	 */
	public PortParameter target;

	/**
	 * The path to the job manager commands on the target machines. Commands are
	 * constructed as <i>binPath/command</i> and they should be executable this
	 * way. This parameter is read each time in fire().
	 */
	public PortParameter binPath;

	/**
	 * The created org.kepler.job.JobManager object as ObjectToken. This jmgr
	 * should be used in JobSubmitter to submit a job. This port is an output
	 * port of type ObjectToken.
	 */
	public TypedIOPort jmgr;

	/***********************************************************
	 * public methods
	 */

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 *                If the subprocess cannot be started, if the input of the
	 *                subprocess cannot be written, if the subprocess gets
	 *                interrupted, or if the return value of the process is
	 *                non-zero.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// update PortParameters
		jobManager.update();
		target.update();
		binPath.update();

		String strJobManager = ((StringToken) jobManager.getToken())
				.stringValue();
		String strTarget = ((StringToken) target.getToken()).stringValue();
		String strBinPath = ((StringToken) binPath.getToken()).stringValue();
		
		//back compatibility, remove the double quotes at the very beginning and at the very last.
		strJobManager = strJobManager.replaceAll("^\"|\"$", "");
		strTarget = strTarget.replaceAll("^\"|\"$", "");
		strBinPath = strBinPath.replaceAll("^\"|\"$", "");

		// Create a JobManager object or get it if it was already created
		org.kepler.job.JobManager myJmgr = null;
		try {
			if (isDebugging)
				log.debug("Create/get JobManager object. Name = "
						+ strJobManager + "; target = " + strTarget
						+ "; binPath = " + strBinPath);
			JobManagerFactory factory = JobManagerFactory.instance;
			myJmgr = factory.get(strJobManager, strTarget, strBinPath);
			// Note that myJmgr.getID can give back a String reference to the
			// object
			// that can be used with JobManagerFactory.get
		} catch (JobException ex) {
			log.error("Job manager object could not be created. " + ex);
			myJmgr = null;
			ex.printStackTrace();
			throw new IllegalActionException("JobManager Error: " + ex.toString());
		}

		jmgr.send(0, new ObjectToken(myJmgr));
	}

	private static final Log log = LogFactory
			.getLog(JobManager.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	private List<ConfigurationProperty> properties;
	private ConfigurationProperty cp;

}