/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author:  $'
 * '$Date:  $' 
 * '$Revision:  $'
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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import ptolemy.actor.IOPortEvent;
import ptolemy.actor.IOPortEventListener;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.moml.MoMLChangeRequest;

public class GenericJobReconnectTest extends TestCase {
	private static final String valid_scheduler_bindir = "/usr/bin";
	private static final String valid_fork_bindir = "/home/d3x140/forkdir/";
	GenericJobReconnect jr;
	CompositeEntity compositeEntity = new CompositeEntity();
	boolean success = false;
	String logString = "";
	boolean reconnectVal = true;
	private static final Log log = LogFactory
			.getLog(GenericJobReconnectTest.class.getName());

	private static final String invalid_jobid = "123456";

	/** ************************************************************************** */

	// For PBS
//	public String protocol = "PBS";
//	private static final String target = "d3x140@colony3a.pnl.gov";
//	private static final String scheduler = "PBS";
//	private static final String binPath = "";
//	private static final String executable = "";
//
//	private static String workdir = "/home/d3x140/TMP";
//	private static String workdir_relative = "TMP";
//
//	// Remote files on Linux machine
//
//	private static final String myworkdir = "/home/d3x140/TMP/mydir";
//	private static final String mynewworkdir = "/home/d3x140/TMP/mynewdir";
//
//	private static final String wrong_JM_name = "PB";
//	private static final String empty_JM_name = "";
//	private static final String lower_JM_name = "pbs";

	// END PBS

	/*---------------------------------------------------------------------------*/

	// For LoadLeveler
	// public String protocol = "LoadLeveler";
	// private static final String target = "d3x140@mitp5.pnl.gov";
	// private static final String jobManager = "LoadLeveler";
	// private static final String binPath = "";
	// private static final String executable = "";
	//  
	// private static String workdir = "/home/d3x140/TMP";
	// private static String workdir_relative = "TMP";
	// private static final String wrong_JM_name = "levelerload";
	// private static final String empty_JM_name = "";
	// private static final String lower_JM_name = "loadleveler";
	// END LoadLeveler
	/*---------------------------------------------------------------------------*/

	// For Fork
	
	 private static final String target = "d3x140@colony3a.pnl.gov";
	 private static final String scheduler = "Fork";
	 private static final String binPath = "/home/d3x140/bin";
	 //private static final String executable =
	 //"/home/chandrika/files/jmgr-fork.sh";
	 private static final String executable =
	 "C:\\Chandrika\\SDM\\GJL_Test\\jmgr-fork.sh";
	  
	 private static String workdir = "/home/d3x140/TMP";
	 private static String workdir_relative = "TMP";
	  
	 private static final String myworkdir = "/home/d3x140/TMP/mydir";
	 private static final String mynewworkdir = "/home/d3x140/TMP/mynewdir";
	 private static final String wrong_JM_name = "For";
	 private static final String empty_JM_name = "";
	 private static final String lower_JM_name = "fork";
	// END Fork
	/**
	 * Constructor - JobLauncherTest
	 * 
	 * @param name
	 * @throws Exception
	 */
	public GenericJobReconnectTest(String name) throws Exception {
		super(name);
		System.setProperty("KEPLER", "C:\\Chandrika\\kepler_rc2_apr22\\");
		jr = new GenericJobReconnect(compositeEntity, "launch job");

		String filePath = System.getProperty("KEPLER");
		filePath = filePath + "actors-2.0\\tests\\src\\log4j.properties";
		PropertyConfigurator.configure(filePath);

		log.info("Testing for protocol " + scheduler);

		jr.target.setToken(new StringToken(target));
		jr.scheduler.setToken(new StringToken(scheduler));
		jr.workdir.setToken(new StringToken(workdir));
		jr.binPath.setToken(new StringToken(binPath));

		if (this.scheduler.equals("Fork")) {
			setExpertMode();
		}

		IOPortEventListener successlistener = new IOPortEventListener() {
			public void portEvent(IOPortEvent event) {
				success = ((BooleanToken) event.getToken()).booleanValue();
			}
		};
		jr.success.addIOPortEventListener(successlistener);

		IOPortEventListener logPortlistener = new IOPortEventListener() {
			public void portEvent(IOPortEvent event) {
				logString = ((StringToken) event.getToken()).stringValue();
			}
		};
		jr.logPort.addIOPortEventListener(logPortlistener);
		
		IOPortEventListener reconnectPortlistener = new IOPortEventListener() {
			public void portEvent(IOPortEvent event) {
				reconnectVal = ((BooleanToken) event.getToken()).booleanValue();
			}
		};
		jr.reconnect.addIOPortEventListener(reconnectPortlistener);
	}

	public final void testEmptyWorkdir() {
		try {

			// set empty workdir
			
			jr.workdir.setToken(new StringToken(""));
			jr.realJobId.setToken(new StringToken(invalid_jobid));

			jr.fire();
			fail("Should have thrown Exception");
			
		} catch (IllegalActionException e) {
			log.info("### testEmptyWorkdir: Exception: " + e);
		}
	}

	public final void testEmptyJobId() {
		try {
			jr.realJobId.setToken(new StringToken(""));
			jr.workdir.setToken(new StringToken(workdir));
			jr.fire();
			log.info("### testEmptyJobId: success= " + success + " log= "
					+ logString);

			fail("Should have thrown Exception");
		} catch (IllegalActionException e) {
			log.info("### testEmptyJobId:  Exception: " + e);
		}
	}
	

	public final void testInvalidJobid() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));
			jr.fire();
			log.info("### testInvalidJobid: success= " + success + " log= "
					+ logString);
			assertEquals(true, success);
			assertEquals(false, reconnectVal);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	// will cause error in Job Manager processing
	// Job Manager support class not found in properties file
	public final void testWronglyNamedJobManager() {
		try {

			jr.realJobId.setToken(new StringToken(invalid_jobid));
			jr.scheduler.setToken(new StringToken(wrong_JM_name));

			jr.fire();
			log.info("### testWronglyNamedJobManager: success= " + success
					+ " log= " + logString);
			fail("Should have thrown IllegalActionException");
		} catch (IllegalActionException e) {
			// success
			log.info("### testWronglyNamedJobManager: Exception: " + e);
		}
	}

	// will cause error in Job Manager processing
	// Job Manager support class not found in properties file
	public final void testEmptyJobManager() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));
			jr.scheduler.setToken(new StringToken(empty_JM_name));

			jr.fire();
			log.info("### testEmptyJobManager: success= " + success + " log= "
					+ logString);
			fail("Should have thrown IllegalActionException");
		} catch (IllegalActionException e) {
			// success
			log.info("### testEmptyJobManager: Exception: " + e);
		}
	}

	public final void testLowerCaseJobManager() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));
			jr.scheduler.setToken(new StringToken(lower_JM_name));

			jr.fire();
			log.info("### testLowerCaseJobManager: success= " + success
					+ " log= " + logString);
			assertEquals(true, success);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	public final void testWorkDirRelative() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));
			jr.workdir.setToken(new StringToken(workdir_relative));
			jr.fire();

			log.info("### testWorkDirRelative: success= " + success + " log= "
					+ logString);
			assertEquals(true, success);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}
	
	// if target is empty gets defaulted to localhost -
	// should run test when OS is not Windows
	public final void testEmptyTarget() {
		String OS = System.getProperty("os.name").toLowerCase();
		if (!OS.contains("windows")) {
			try {
				jr.realJobId.setToken(new StringToken(invalid_jobid));
				jr.workdir.setToken("");
				jr.target.setToken(new StringToken(""));
				jr.fire();

				log.info("### testEmptyTarget: success= " + success + " log= "
						+ logString);
				assertEquals(true, success);
			} catch (IllegalActionException e) {
				fail("Exception " + e);
			}
		}
	}

	public final void testLocalExecLocal() {
		try {
			String OS = System.getProperty("os.name").toLowerCase();
			if (!OS.contains("windows")) {
				jr.realJobId.setToken(new StringToken(invalid_jobid));
				jr.target.setToken(new StringToken("local"));
				jr.fire();

				log.info("### testLocalExecLocal: success= " + success
						+ " log= " + logString);
				assertEquals(true, success);
			}
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	public final void testLocalExecQuotes() {
		try {
			String OS = System.getProperty("os.name").toLowerCase();
			if (!OS.contains("windows")) {
				jr.realJobId.setToken(new StringToken(invalid_jobid));
				jr.target.setToken(new StringToken(""));
				jr.fire();

				log.info("### testLocalExecQuotes: success= " + success
						+ " log= " + logString);
				assertEquals(true, success);
			}
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	public final void testLocalExecNull() {
		try {
			String OS = System.getProperty("os.name").toLowerCase();
			if (!OS.contains("windows")) {
				String strNull = null;
				jr.realJobId.setToken(new StringToken(invalid_jobid));
				jr.target.setToken(new StringToken(strNull));

				jr.fire();
				log.info("### testLocalExecNull: success= " + success
						+ " log= " + logString);
				assertEquals(true, success);
			}
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	// comma separated list of wait_until status. Containing one invalid status
	public final void testWaitUntilMultipleInvalid() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));

			jr.waitUntil.setToken("Wait,Run");

			jr.fire();
			fail("Must have thrown IllegalActionException");
		} catch (IllegalActionException e) {
			log.info("### testWaitUntilMultipleInvalid: Exception= " + e);
		}
	}

	
	
	
	
	
	
    //
    //
	// 
	// Rest of the test cases should be tested individually with valid job id
	// 
	//
	//
/*
	public final void testWaitUntilANY() {
		try {
			jr.realJobId.setToken(new StringToken("14390"));
			jr.waitUntil.setToken("ANY");

			jr.fire();
			log.info("### testWaitUntilANY: success= " + success + " log= "
					+ logString);
			assertEquals(true, success);
			assertEquals(true, reconnectVal);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	// With Fork, this is a good way to test if the actor is getting out without
	// being stuck in an infinite loop. This will eventually exit even if the
	// job
	// never goes into the wait state. It will exit when status is Error or
	// NotInQueue
	public final void testWaitUntilWAIT() {
		try {
			jr.realJobId.setToken(new StringToken("15608"));
			jr.waitUntil.setToken("Wait");

			jr.fire();
			log.info("### testWaitUntilWAIT: success= " + success + " log= "
					+ logString);
			assertEquals(true, success);
			assertEquals(true, reconnectVal);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	public final void testWaitUntilRUNNING() {
		try {
			jr.realJobId.setToken(new StringToken("14705"));
			jr.waitUntil.setToken("Running");

			jr.fire();
			log.info("### testWaitUntilRUNNING: success= " + success + " log= "
					+ logString);
			assertEquals(true, success);
			assertEquals(true, reconnectVal);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	public final void testWaitUntilNOTINQUEUE() {
		try {
			jr.realJobId.setToken(new StringToken("14705"));
			jr.waitUntil.setToken("NotInQueue");

			jr.fire();
			log.info("### testWaitUntilNOTINQUEUE: success= " + success
					+ " log= " + logString);
			assertEquals(true, success);
			assertEquals(true, reconnectVal);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	// Test this case alone, else job manager from a previous test case would be
	// used(factory class returns cached obj) and hence bin path will be taken
	// based on previous test cases
	// With expert mode set to false, values set in bin path should be ignored
	public final void testExpertOff() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));

			removeExpertMode();
			if (!scheduler.equalsIgnoreCase("Fork")) { // fork script doesn't
				// run without bin path
				jr.binPath.setToken("/ignored");
				jr.fire();
				log.info("### testExpertOff: success= " + success + " log= "
						+ logString);
				assertEquals(true, success);
			}
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}
	
	// Test this case alone, else job manager from a previous test case would
	// be used(factory class //returns cached obj) and hence bin path will be
	// taken based on previous test cases
	public final void testExpertOnBinPathRight() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));

			setExpertMode();
			if (scheduler.equalsIgnoreCase("Fork")) {
				jr.binPath.setToken(new StringToken(valid_fork_bindir));
			} else {
				jr.binPath.setToken(new StringToken(valid_scheduler_bindir));
			}
			jr.fire();

			log.info("### testExpertOnBinPathRight: success= " + success
					+ " log= " + logString);
			assertEquals(true, success);
			assertEquals(false, reconnectVal);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	// Test this case alone, else job manager from a previous test case would
	// be used(factory class //returns cached obj) and hence bin path will be
	// taken based on previous test cases
	public final void testExpertOnBinPathWrong() {
		try {
			jr.realJobId.setToken(new StringToken(invalid_jobid));

			setExpertMode();
			jr.binPath.setToken(new StringToken("/wrongbin"));
			jr.fire();

			log.info("### testExpertOnBinPathWrong: success= " + success
					+ " log= " + logString);
			assertEquals(true, success);
			assertTrue(logString.endsWith("Error"));
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}

	// comma separated list of wait_until status. Containing one status as ANY
	// Rest should be ignored
	public final void testWaitUntilMultipleANY() {
		try {
			jr.realJobId.setToken(new StringToken("16479"));

			jr.waitUntil.setToken("Error,Invalid,ANY");

			jr.fire();
			log.info("### testWaitUntilMultipleANY: success= " + success
					+ " log= " + logString);
			assertEquals(true, success);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}
	
	// comma separated list of wait_until status. All valid
	public final void testWaitUntilMultipleValid() {
		try {
			jr.realJobId.setToken(new StringToken("16479"));
			jr.waitUntil.setToken("Error,NotInQueue");

			jr.fire();
			log.info("### testWaitUntilMultipleValid: success= " + success
					+ " log= " + logString);
			assertEquals(true, success);
		} catch (IllegalActionException e) {
			fail("Exception " + e);
		}
	}
*/
	/**
	 * Set expert mode to true
	 */
	private void setExpertMode() {
		StringBuffer moml = new StringBuffer();
		if (jr.getAttribute("_expertMode") == null) {
			moml
					.append("<property name=\"_expertMode\" "
							+ "class=\"ptolemy.kernel.util.SingletonAttribute\"></property>");
			MoMLChangeRequest request = new MoMLChangeRequest(this, // originator
					jr, // context
					moml.toString(), // MoML code
					null);
			jr.requestChange(request);
		}

	}

	private void removeExpertMode() {
		StringBuffer moml = new StringBuffer();
		if (jr.getAttribute("_expertMode") != null) {
			moml.append("<deleteProperty name=\"_expertMode\"/>");
			MoMLChangeRequest request = new MoMLChangeRequest(this, // originator
					jr, // context
					moml.toString(), // MoML code
					null);
			jr.requestChange(request);
		}
	}
}
