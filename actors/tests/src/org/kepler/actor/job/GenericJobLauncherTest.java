/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-09-30 17:04:47 -0700 (Fri, 30 Sep 2011) $' 
 * '$Revision: 28695 $'
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
import ptolemy.moml.MoMLChangeRequest;

public class GenericJobLauncherTest extends TestCase {
  GenericJobLauncher jl;
  CompositeEntity compositeEntity = new CompositeEntity();
  boolean success = false;
  String logString = "";
  
  private static final Log log = LogFactory.getLog(GenericJobLauncher.class.getName());
  
  private static final StringToken [] empty_file_quotes = {new StringToken ("")};
  
  private static final String LOG_PROPERTIES = "actors/tests/src/log4j.properties";
  private static final String KEPLER_HOME = "/Users/d3x140/Work/SDM/kepler_Dec2010/";
  private static final String valid_scheduler_bindir = "/usr/bin";
  private static final String valid_fork_bindir = "/home/d3x140/forkdir/";
 
  
//////// Local files on Windows machine ////////////////
//  private static final StringToken [] localinput = 
//    {new StringToken ("C:\\chandrika\\SDM\\GJL_Test\\test.sh"),
//     new StringToken ("C:\\chandrika\\SDM\\GJL_Test\\forkjob.sh")};
//  
//  private static final StringToken [] wrong_local_input = 
//    {new StringToken ("C:\\chandrika\\SDM\\GJL_Test\\why.sh"), // this is named wrong
//     new StringToken ("C:\\chandrika\\SDM\\GJL_Test\\forkjob.sh")};
//  
//  private static final StringToken [] local_regex = 
//    {new StringToken ("C:\\chandrika\\SDM\\GJL_Test\\*.sh")};
//  
//  private static final StringToken [] local_no_regex =
//    {new StringToken ("C:\\chandrika\\SDM\\GJL_Test\\*.nofiles")};
//  
//  // relative to %HOMEPATH%
//  private static final StringToken [] local_relative_input =
//    {new StringToken ("files\\test.sh"),
//     new StringToken ("files\\forkjob.sh")};
//  
//  private static final StringToken [] local_relative_regex_input =
//    {new StringToken ("files\\*.sh")};
//  
//  private static final StringToken [] local_relative_no_regex_input =
//  {new StringToken ("files\\*.nofiles")};
  
/////////Local files on Linux machine //////////////////
  private static final String local_fork_bindir = "/Users/d3x140/bin";
  private static final StringToken [] localinput = 
    {new StringToken ("/Users/d3x140/files/whytest.sh"),
     new StringToken ("/Users/d3x140/files/forkjob.sh")};
  
  private static final StringToken [] wrong_local_input = 
    {new StringToken ("/Users/d3x140/files/why.sh"), // this is named wrong
     new StringToken ("/Users/d3x140/files/forkjob.sh")};
  
  private static final StringToken [] local_regex = 
    {new StringToken ("/Users/d3x140/files/*.sh")};
  
  private static final StringToken [] local_no_regex =
    {new StringToken ("/Users/d3x140/files/*.nofiles")};
  
  // relative to %HOMEPATH%
  private static final StringToken [] local_relative_input =
    {new StringToken ("files/whytest.sh"),
     new StringToken ("files/forkjob.sh")};
  
  private static final StringToken [] local_relative_regex_input =
    {new StringToken ("files/*.sh")};
  
  private static final StringToken [] local_relative_no_regex_input =
  {new StringToken ("files/*.nofiles")};
  
  // Remote files on Linux machine
  private static final StringToken [] remoteinput = 
    {new StringToken ("/home/d3x140/TEST/colony-forkjob.sh"),
     new StringToken ("/home/d3x140/TEST/colony-test.sh"),
     new StringToken ("/home/d3x140/TEST/colony-forking.sh")};
  
  private static final StringToken [] wrong_remote_input = 
    {new StringToken ("/home/d3x140/TEST/colony-forkjob.sh"),
     new StringToken ("/home/d3x140/TEST/colony-test"), // this is named wrong
     new StringToken ("/home/d3x140/TEST/colony-forking.sh")};
  
  private static final StringToken [] remote_regex =
    {new StringToken ("/home/d3x140/TEST/*.sh")};
  
  private static final StringToken [] remote_no_regex =
    {new StringToken ("/home/d3x140/TEST/*.nofiles")};
  
  // relative to $HOME
  private static final StringToken [] remote_relative_input = 
    {new StringToken ("TEST/colony-forkjob.sh"),
     new StringToken ("TEST/colony-test.sh"),
     new StringToken ("TEST/colony-forking.sh")};
  
  private static final StringToken [] remote_relative_regex_input = 
    {new StringToken ("TEST/*.sh")};
  
  private static final StringToken [] remote_relative_no_regex_input = 
    {new StringToken ("TEST/*.nofiles")};
  
/*****************************************************************************/
  
  // For PBS
//  public String protocol = "PBS";
//  private static final String target = "d3x140@colony3a.pnl.gov";
//  private static final String scheduler = "PBS";
//  private static final String binPath = "";
//  private static final String executable = "";
//  
//  private static String workdir = "/home/d3x140/TMP";
//  private static String workdir_relative = "TMP";
//  
//  // Local files on Windows machine
//  private static final String local_cmdFile = "C:\\Chandrika\\SDM\\GJL_Test\\pbsscript.sh";
//  private static final String local_relative_cmdFile = "files\\pbsscript.sh";
//  private static final String wrong_local_cmd = "/home/chandrika/files/pbs.sh"; // this is named wrong
//  
//  // Remote files on Linux machine
//  private static final String remote_cmdFile = "/home/d3x140/TEST/colony-pbsscript.sh";
//  private static final String remote_relative_cmdFile = "TEST/colony-pbsscript.sh";
//  private static final String wrong_remote_cmd = "/home/d3x140/TEST/colony-pbs.sh"; // this is named wrong
//  private static final String myworkdir = "/home/d3x140/TMP/mydir";
//  private static final String mynewworkdir = "/home/d3x140/TMP/mynewdir";
//  
//  private static final String wrong_JM_name = "PB"; 
//  private static final String empty_JM_name = "";
//  private static final String lower_JM_name = "pbs";
  // END PBS
  
/*---------------------------------------------------------------------------*/
  
  // For LoadLeveler
//  public String protocol = "LoadLeveler";
//  private static final String target = "d3x140@mitp5.pnl.gov";
//  private static final String jobManager = "LoadLeveler";
//  private static final String binPath = "";
//  private static final String executable = "";
//  
//  private static String workdir = "/home/d3x140/TMP";
//  private static String workdir_relative = "TMP";
//  
//  // Local files on Windows machine
//  private static final String local_cmdFile = "/home/chandrika/files/llscript.sh";
//  private static final String local_relative_cmdFile = "files/llscript.sh";
//  private static final String wrong_local_cmd = "/home/chandrika/files/lscriptl.sh"; // this is named wrong
//  
//  // Remote files on Linux machine
//  private static final String remote_cmdFile = "/home/d3x140/TEST/mitp5-llscript.sh";
//  private static final String remote_relative_cmdFile = "TEST/mitp5-llscript.sh";
//  private static final String wrong_remote_cmd = "/home/d3x140/TEST/llscript.sh"; // this is named wrong
//  
//  private static final String wrong_JM_name = "levelerload";  
//  private static final String empty_JM_name = "";
//  private static final String lower_JM_name = "loadleveler";
  // END LoadLeveler
  
/*---------------------------------------------------------------------------*/
  
  // For Fork
  private static final String target = "d3x140@colony3a.pnl.gov";
  private static final String scheduler = "Fork";
  private static final String binPath = "/home/d3x140/bin";
  private static final String executable = "/Users/d3x140/files/jmgr-fork.sh";  // always a local file
  //private static final String executable = "C:\\Chandrika\\SDM\\GJL_Test\\jmgr-fork.sh";
  
  private static String workdir = "/home/d3x140/TMP";
  private static String workdir_relative = "TMP";
  
  
//  //Local files on Windows machine
//  private static final String local_cmdFile = "C:\\Chandrika\\SDM\\GJL_Test\\cmd.sh";
//  private static final String local_relative_cmdFile = "files\\cmd.sh";
//  private static final String wrong_local_cmd = "/home/chandrika/files/cdm.sh"; // this is named wrong
  
  //Local files on Linux machine
  private static final String local_cmdFile = "/Users/d3x140/files/cmd.sh";
  private static final String local_relative_cmdFile = "files/cmd.sh";
  private static final String wrong_local_cmd = "/Users/d3x140/files/cdm.sh"; // this is named wrong
  private static String local_workdir = "/Users/d3x140/TMP";
  private static String local_workdir_relative = "TMP";
  private static final String local_binPath = "/Users/d3x140/bin";
  

// // Remote files on Linux machine
  private static final String remote_cmdFile = "/home/d3x140/TEST/colony-cmd.sh"; 
  private static final String remote_relative_cmdFile = "TEST/colony-cmd.sh";
  private static final String wrong_remote_cmd = "/home/d3x140/TEST/colony-cdm.sh"; // this is named wrong
  private static final String myworkdir = "/home/d3x140/TMP/mydir";
  private static final String mynewworkdir = "/home/d3x140/TMP/mynewdir";
  private static final String wrong_JM_name = "For";  
  private static final String empty_JM_name = "";
  private static final String lower_JM_name = "fork";
  // END Fork
  
  /** Constructor - JobLauncherTest
   * 
   * @param name
   * @throws Exception
   */
  public GenericJobLauncherTest (String name) throws Exception {
    super (name);
    System.setProperty("KEPLER", KEPLER_HOME);
    jl = new GenericJobLauncher (compositeEntity, "launch job");

    String filePath = System.getProperty("KEPLER");
    filePath = filePath + LOG_PROPERTIES;
    System.out.println("Log4j properties:"+filePath);
    PropertyConfigurator.configure(filePath);   
    
    log.info("Testing for protocol " + scheduler);

    jl.target.setToken(new StringToken(target));
    jl.scheduler.setToken(new StringToken(scheduler));
    jl.workdir.setToken(new StringToken(workdir));
    jl.binPath.setToken(new StringToken(binPath));
    jl.executable.setToken(new StringToken(executable));
    
    if (this.scheduler.equals("Fork")){
      setExpertMode();
    }
    
    IOPortEventListener successlistener = new IOPortEventListener() {
      public void portEvent(IOPortEvent event) {
    	  success = ((BooleanToken) event.getToken()).booleanValue();
      }
    };
    jl.success.addIOPortEventListener(successlistener);
    
    IOPortEventListener logPortlistener = new IOPortEventListener() {
      public void portEvent(IOPortEvent event) {
        logString = ((StringToken) event.getToken()).stringValue();
      }
    };
    jl.logPort.addIOPortEventListener(logPortlistener);
  }
 
  /*
  public final void testLocalCMD () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (local_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.remotefiles.setToken (files);
      
      jl.fire ();
      log.info("### testLocalCMD: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  public final void testRemoteCMD () {
    try {
      ArrayToken files = new ArrayToken (remoteinput);
      
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      log.info("### testRemoteCMD: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

  // will cause error in Job Submission
  // local files get copied first
  // local cmd file not found in remote system
  public final void testLocalCMDSetLocalFalse () {
    try {
      ArrayToken files = new ArrayToken (remoteinput);
      
      jl.cmdFile.setToken(new StringToken (local_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false)); // think it's remote
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      log.info("### testLocalCMDSetLocalFalse: success= " + success + " log= " + logString);
        assertNotSame(true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  
  // will cause error in create Job
  // error caused while attempting to copy local files
  // remote cmd file not found in local system
  public final void testRemoteCMDSetLocalTrue () {
    try {
      ArrayToken files = new ArrayToken (localinput);
      
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true)); // think itis local
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      log.info("### testRemoteCMDSetLocalTrue: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException saying the cmd file could be found" ); 
    }
    catch (IllegalActionException e) {
      //success
      log.info("### testRemoteCMDSetLocalTrue: Exce[tion " + e);
    }
  }

  public final void testLocalInputWithLocalCMD () {
    try {
      ArrayToken files = new ArrayToken (localinput);
      
      jl.cmdFile.setToken(new StringToken (local_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.inputfiles.setToken(files);
      
      jl.fire ();
      log.info("### testLocalInputWithLocalCMD: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  public final void testLocalInputWithRemoteCMD () {
    try {
      ArrayToken files = new ArrayToken (localinput);
      
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.inputfiles.setToken(files);
      
      jl.fire ();
      log.info("### testLocalInputWithRemoteCMD: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  } 

  public final void testEmptyWorkdir () {
    try {
      ArrayToken files = new ArrayToken (remoteinput);
      
      // set empty workdir
      workdir = "";
      jl.workdir.setToken(new StringToken (workdir));
      
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      log.info("### testEmptyWorkdir: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

  public final void testEmptyFiles () {
    try {
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
            
      jl.fire ();
      log.info("### testEmptyFiles: success= " + success + " log= " + logString);
      
      assertEquals (true, success);     
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  */
  
  /*
  
  // will cause error Job Submission
  // error caused while attempting to copy remote files
  // remote file not found
  public final void testWronglyNamedRemoteFiles () {
    try {
      ArrayToken files = new ArrayToken (wrong_remote_input);

      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      log.info("### testWronglyNamedRemoteFiles: success= " + success + " log= " + logString);
      assertNotSame (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  // Will cause an error when trying to find files based on pattern
  // submitted in inputfiles
  public final void testWronglyNamedLocalFiles () {
    try {
      ArrayToken files = new ArrayToken (wrong_local_input);
      
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.inputfiles.setToken(files);
      
      jl.fire ();
      log.info("### testWronglyNamedLocalFiles: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException for wrong local files");
      //assertNotSame (true, success);
    }
    catch (IllegalActionException e) {
      //success
      log.info("### testWronglyNamedLocalFiles: Exception : " + e);
    }
  }

  // will cause error in create job
  // error caused while attempting to copy local files
  // local cmd file not found
  public final void testWronglyNamedLocalCMD () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);

      jl.cmdFile.setToken(new StringToken (wrong_local_cmd));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.remotefiles.setToken (files);

      jl.fire ();
      log.info("### testWronglyNamedLocalCMD: success= " + success + " log= " + logString);
      //assertNotSame (true, success);
      fail("Should have thrown IllegalActionException for wrong cmd file");
    }
    catch (IllegalActionException e) {
    	//success
    	log.info("### testWronglyNamedLocalCMD: Exception : "+ e);
    }
  }
  
  // will cause error in Job Submission
  // local files copied first
  // error caused while attempting to copy remote files
  // remote cmd file not found
  public final void testWronglyNamedRemoteCMD () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);

      jl.cmdFile.setToken(new StringToken (wrong_remote_cmd));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken (files);

      jl.fire ();
      log.info("### testWronglyNamedRemoteCMD: success= " + success + " log= " + logString);
      assertNotSame (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  

  // will cause error in Job Manager processing
  // Job Manager support class not found in properties file
  public final void testWronglyNamedJobManager () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (local_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.remotefiles.setToken (files);
      
      jl.scheduler.setToken(new StringToken (wrong_JM_name));
      
      jl.fire ();
      log.info("### testWronglyNamedJobManager: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException");
    }
    catch (IllegalActionException e) {
      //success
      log.info("### testWronglyNamedJobManager: Exception: " +e);
    }
  }
  
  // will cause error in Job Manager processing
  // Job Manager support class not found in properties file
  public final void testEmptyJobManager () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (local_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.remotefiles.setToken (files);
      
      jl.scheduler.setToken(new StringToken (empty_JM_name));
      
      jl.fire ();
      log.info("### testEmptyJobManager: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException");
    }
    catch (IllegalActionException e) {
      //success
      log.info("### testEmptyJobManager: Exception: " +e);
    }
  }
  
  // Will cause an error when trying to find files based on pattern
  // submitted in inputfiles
  public final void testInputtingRemoteIntoLocal () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (local_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.inputfiles.setToken (files);
      
      jl.fire ();
      log.info("### testInputtingRemoteIntoLocal: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException");

    }
    catch (IllegalActionException e) {
      //success
      log.info("### testInputtingRemoteIntoLocal: "+e);
    }
  }

  // will cause error in job submission
  // local cmd file gets copied to remote system first
  // attempting to copy remote files with local filenames
  public final void testInputtingLocalIntoRemote () {
    try {     
      ArrayToken files = new ArrayToken (localinput);
            
      jl.cmdFile.setToken(new StringToken (local_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.remotefiles.setToken (files);
      
      jl.fire ();
      log.info("### testInputtingLocalIntoRemote: success= " + success + " log= " + logString);
      assertNotSame (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
   
  public final void testLowerCaseJobManager () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken (files);
      
      jl.scheduler.setToken(new StringToken (lower_JM_name));
      
      jl.fire ();
      log.info("### testLowerCaseJobManager: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  */
  
 /*
  public final void testWaitUntilANY () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken (files);
    
      jl.waitUntil.setToken("ANY");
      
      jl.fire ();
      log.info("### testWaitUntilANY: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  //This is a good way to test if the actor is getting out without
  //being stuck in an infinite loop. This will eventually exit even if the job
  //never goes into the wait state. It will exit when status is Error or NotInQueue
  public final void testWaitUntilWAIT () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken (files);
    
      jl.waitUntil.setToken("Wait");
      
      jl.fire ();
      log.info("### testWaitUntilWAIT: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  public final void testWaitUntilRUNNING () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken (files);
    
      jl.waitUntil.setToken("Running");
      
      jl.fire ();
      log.info("### testWaitUntilRUNNING: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

  public final void testWaitUntilNOTINQUEUE () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken (files);
    
      jl.waitUntil.setToken("NotInQueue");
      
      jl.fire ();
      log.info("### testWaitUntilNOTINQUEUE: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  } 
  
  public final void testLocalRegex () {
    try {     
      ArrayToken files = new ArrayToken (local_regex);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      jl.inputfiles.setToken(files);
        
      jl.fire ();
      log.info("### testLocalRegex: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  // will cause an error when trying to find local files
  public final void testLocalNoRegex () {
    try {     
      ArrayToken files = new ArrayToken (local_no_regex);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      jl.inputfiles.setToken(files);
        
      jl.fire ();
      log.info("### testLocalNoRegex: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException");
    }
    catch (IllegalActionException e) {
     //success
     log.info("### testLocalNoRegex: " + e);
    }
  }
  
  public final void testRemoteRegex () {
    try {     
      ArrayToken files = new ArrayToken (remote_regex);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      jl.remotefiles.setToken(files);
        
      jl.fire ();
      log.info("### testRemoteRegex: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  // Will cause an error when trying to find remote files
  public final void testRemoteNoRegex () {
    try {     
      ArrayToken files = new ArrayToken (remote_no_regex);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      jl.remotefiles.setToken(files);
        
      jl.fire ();
      log.info("### testRemoteNoRegex: success= " + success + " log= " + logString);
      assertNotSame (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  public final void testBothRegex () {
    try {           
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      ArrayToken files = new ArrayToken (local_regex);
      jl.inputfiles.setToken(files);
      
      files = new ArrayToken (remote_regex);
      jl.remotefiles.setToken(files);
        
      jl.fire ();
      log.info("### testBothRegex: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  // will cause an error when trying to find local files,
  // will not reach the point of finding files for remote machine
  public final void testBothNoRegex () {
    try {
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      ArrayToken files = new ArrayToken (local_no_regex);
      jl.inputfiles.setToken(files);
      
      files = new ArrayToken (remote_no_regex);
      jl.remotefiles.setToken(files);
        
      jl.fire ();
      log.info("### testBothNoRegex: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException");
    }
    catch (IllegalActionException e) {
      //success
      log.info("### testBothNoRegex: "+ e );
    }
  }
 
  
  public final void testLocalEmptyFileQuotes () {
    try {     
      ArrayToken files = new ArrayToken (empty_file_quotes);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      jl.inputfiles.setToken(files);
            
      jl.fire ();
      log.info("### testLocalEmptyFileQuotes: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  public final void testRemoteEmptyFileQuotes () {
    try {     
      ArrayToken files = new ArrayToken (empty_file_quotes);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
        
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      log.info("### testRemoteEmptyFileQuotes: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  public final void testBothEmptyFileQuotes () {
    try {     
      ArrayToken files = new ArrayToken (empty_file_quotes);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      
      jl.inputfiles.setToken(files);
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      log.info("### testBothEmptyFileQuotes: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

 
  // Will give an error, requires a valid cmdFile
  public final void testEmptyCMD () {
    try {     
      ArrayToken files = new ArrayToken (remoteinput);
      jl.remotefiles.setToken(files);
      
      jl.cmdFile.setToken(new StringToken(""));     
      jl.fire ();
      
      log.info("### testEmptyCMD: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException");
    }
    catch (IllegalActionException e) {
      //success
      log.info("### testEmptyCMD: "+ e);
    }
  }
 */
  
  /*
  //if target is empty gets defaulted to localhost - 
  //should run test when OS is not Windows
  public final void testEmptyTarget () {
    String OS = System.getProperty("os.name").toLowerCase();
	if (!OS.contains("windows")){
      try {     
      
    	jl.binPath.setToken(new StringToken(local_binPath));
    	jl.executable.setToken(new StringToken(executable));
        //ArrayToken files = new ArrayToken (remoteinput);
        ArrayToken files = new ArrayToken (localinput);    
        //jl.cmdFile.setToken(new StringToken (remote_cmdFile));
        jl.cmdFile.setToken(new StringToken (local_cmdFile));
        jl.cmdFileLocal.setToken(new BooleanToken (false));
        //jl.remotefiles.setToken(files);
        jl.workdir.setToken(new StringToken(local_workdir));
        jl.target.setToken(new StringToken(""));      
        jl.fire ();
      
        log.info("### testEmptyTarget: success= " + success + " log= " + logString);
        assertEquals (true, success);
      }
      catch (IllegalActionException e) {
    	log.info("### testEmptyTarget: Exception "+e); 
        fail("Exception " + e);
      }
	}
  }
  
  //if target is empty gets defaulted to localhost - 
  //should run test when OS is not Windows
  public final void testEmptyTargetDefaultWorkdir () {
	    String OS = System.getProperty("os.name").toLowerCase();
		if (!OS.contains("windows")){
	      try {     
	        //ArrayToken files = new ArrayToken (remoteinput);
	        ArrayToken files = new ArrayToken (localinput);    
	        //jl.cmdFile.setToken(new StringToken (remote_cmdFile));
	        jl.cmdFile.setToken(new StringToken (local_cmdFile));
	        jl.cmdFileLocal.setToken(new BooleanToken (false));
	        //jl.remotefiles.setToken(files);
	        jl.binPath.setToken(new StringToken(local_binPath));
    		jl.executable.setToken(new StringToken(executable));
	        jl.workdir.setToken(new StringToken(""));
	        jl.target.setToken(new StringToken(""));      
	        jl.fire ();
	      
	        log.info("### testEmptyTargetDefaultWorkdir: success= " + success + " log= " + logString);
	        assertEquals (true, success);
	      }
	      catch (IllegalActionException e) {
	    	log.info("### testEmptyTargetDefaultWorkdir: Exception "+e); 
	        fail("Exception " + e);
	      }
		}
  }
  
  public final void testEmptyTargetRelativeWorkdir () {
	    String OS = System.getProperty("os.name").toLowerCase();
		if (!OS.contains("windows")){
	      try {     
	        //ArrayToken files = new ArrayToken (remoteinput);
	        ArrayToken files = new ArrayToken (localinput);    
	        //jl.cmdFile.setToken(new StringToken (remote_cmdFile));
	        jl.cmdFile.setToken(new StringToken (local_cmdFile));
	        jl.cmdFileLocal.setToken(new BooleanToken (false));
	        //jl.remotefiles.setToken(files);
	        jl.workdir.setToken(new StringToken(local_workdir_relative));
	        jl.target.setToken(new StringToken(""));    
	        jl.binPath.setToken(new StringToken(local_binPath));
    		jl.executable.setToken(new StringToken(executable));  
	        jl.fire ();
	      
	        log.info("### testEmptyTargetRelativeWorkdir: success= " + success + " log= " + logString);
	        assertEquals (true, success);
	      }
	      catch (IllegalActionException e) {
	    	log.info("### testEmptyTargetRelativeWorkdir: Exception "+e); 
	        fail("Exception " + e);
	      }
		}
	  }
  
  public final void testLocalExecLocal () {
	    try {     
	      String OS = System.getProperty("os.name").toLowerCase();
	      if (!OS.contains("windows")){
	        ArrayToken files = new ArrayToken (localinput);
	        jl.workdir.setToken(new StringToken(local_workdir_relative));
	        jl.binPath.setToken(new StringToken(local_binPath));
	        jl.cmdFile.setToken(new StringToken (local_cmdFile));
	        jl.cmdFileLocal.setToken(new BooleanToken (true));
	        jl.inputfiles.setToken(files);
	        
	        jl.target.setToken(new StringToken("local"));       
	        jl.fire ();

	        log.info("### testLocalExecLocal: success= " + success + " log= " + logString);
	        assertEquals (true, success);
	      }  
	    }
	    catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	  }
	
	  public final void testLocalExecQuotes () {
	    try {     
	      String OS = System.getProperty("os.name").toLowerCase();
	      if (!OS.contains("windows")){
	        ArrayToken files = new ArrayToken (localinput);
	        jl.workdir.setToken(new StringToken(""));
	        jl.binPath.setToken(new StringToken(local_binPath));
	        jl.cmdFile.setToken(new StringToken (local_cmdFile));
	        jl.cmdFileLocal.setToken(new BooleanToken (true));
	        jl.inputfiles.setToken(files);
	        
	        jl.target.setToken(new StringToken(""));        
	        jl.fire ();

	        log.info("### testLocalExecQuotes: success= " + success + " log= " + logString);
	        assertEquals (true, success);
	      }
	    }
	    catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	  }
	  
	  public final void testLocalExecNull () {
	    try {     
	      String OS = System.getProperty("os.name").toLowerCase();
	      if (!OS.contains("windows")){
	        String strNull = null;
	        ArrayToken files = new ArrayToken (localinput);

	        jl.workdir.setToken(new StringToken(""));
	        jl.binPath.setToken(new StringToken(local_binPath));
	        jl.cmdFile.setToken(new StringToken (local_cmdFile));
	        jl.cmdFileLocal.setToken(new BooleanToken (true));        
	        jl.inputfiles.setToken(files);
	        
	        jl.target.setToken(new StringToken (strNull));
	        
	        jl.fire ();
	        log.info("### testLocalExecNull: success= " + success + " log= " + logString);
	      	assertEquals (true, success); 
	      }
	    }
	    catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	  }
  */
/*
  
  //Will be relative to JVM start point - for kepler it would be the actor module dir
  public final void testCMDRelativeLocal () {
    try {
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (local_relative_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (true));
      jl.remotefiles.setToken(files);
      jl.workdir.setToken(new StringToken(workdir));
      jl.fire ();
      
      log.info("### testCMDRelativeLocal: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  } 
 
   //Relative to $HOME
  public final void testCMDRelativeRemote () {
    try {
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (remote_relative_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));       
      jl.remotefiles.setToken(files);
      
      jl.fire ();
      
      log.info("### testCMDRelativeRemote: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
  
  public final void testWorkDirRelative () {
    try {
      ArrayToken files = new ArrayToken (remoteinput);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
      jl.remotefiles.setToken(files);
      
      jl.workdir.setToken(new StringToken(workdir_relative));     
      jl.fire ();
      
      log.info("### testWorkDirRelative: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

 
  public final void testInputFilesRelative () {
    try {
      ArrayToken files = new ArrayToken (local_relative_input);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
        
      jl.inputfiles.setToken(files);
      jl.fire ();
      
      log.info("### testInputFilesRelative: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

  public final void testInputFilesRelativeRegex () {
    try {
      ArrayToken files = new ArrayToken (local_relative_regex_input);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
        
      jl.inputfiles.setToken(files);
      jl.fire ();
      
      log.info("### testInputFilesRelativeRegex: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

  // Will cause an error when trying to find local files
  public final void testInputFilesRelativeNoRegex () {
    try {
      ArrayToken files = new ArrayToken (local_relative_no_regex_input);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
        
      jl.inputfiles.setToken(files);
      jl.fire ();
      
      log.info("### testInputFilesRelativeNoRegex: success= " + success + " log= " + logString);
      fail("Should have thrown IllegalActionException");
    }
    catch (IllegalActionException e) {
    	//success
    	log.info("### testInputFilesRelativeNoRegex: Exception: "+ e.toString());
    }
  }
  
  
  public final void testRemoteFilesRelative () {
    try {
      ArrayToken files = new ArrayToken (remote_relative_input);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
        
      jl.remotefiles.setToken(files);
      jl.fire ();
      
      log.info("### testRemoteFilesRelative: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

  public final void testRemoteFilesRelativeRegex () {
    try {
      ArrayToken files = new ArrayToken (remote_relative_regex_input);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
        
      jl.remotefiles.setToken(files);
      jl.fire ();
      
      log.info("### testRemoteFilesRelativeRegex: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }
 
  // Will cause an error when trying to find remote files
  public final void testRemoteFilesRelativeNoRegex () {
    try {
      ArrayToken files = new ArrayToken (remote_relative_no_regex_input);
            
      jl.cmdFile.setToken(new StringToken (remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken (false));
        
      jl.remotefiles.setToken(files);
      jl.fire ();
      
      log.info("### testRemoteFilesRelativeNoRegex: success= " + success + " log= " + logString);
      assertNotSame (true, success);
    }
    catch (IllegalActionException e) {
      fail("Exception " + e);
    }
  }

*/
  
  /*

  //TODO: Update for Moab
  public final void testExpertOnJobSubmitOptionGood(){
    try {
      ArrayToken files = new ArrayToken (remoteinput);
    
      jl.cmdFile.setToken(new StringToken(remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken(false));
      jl.remotefiles.setToken(files);
      
      if(jl.scheduler.equals("PBS")){
        setExpertMode();
        // -p sets priority of the job to 0 which is default
        jl.jobSubmitOptions.setToken(new StringToken("-p 0"));
        jl.fire();
      }
      else{ // no good submit options for loadleveler
        success = true;
      }
      log.info("### testExpertOnJobSubmitOptionGood: success= " + success + " log= " + logString);
      assertEquals (true, success);
    }
    catch(IllegalActionException e){
      fail("Exception " + e);
    }
  } 

  // will cause an error
  public final void testExpertOnJobSubmitOptionBad(){
    try {
      ArrayToken files = new ArrayToken (remoteinput);
    
      jl.cmdFile.setToken(new StringToken(remote_cmdFile));
      jl.cmdFileLocal.setToken(new BooleanToken(false));
      jl.remotefiles.setToken(files);
      
      setExpertMode();
      jl.jobSubmitOptions.setToken(new StringToken("-abcdefghijklmnopqrstuvwxyz"));
      jl.fire();
      
      log.info("### testExpertOnJobSubmitOptionBad: success= " + success + " log= " + logString);
      assertNotSame (true, success);
    }
    catch(IllegalActionException e){
      fail("Exception " + e);
    }
  } 


  
  // New test cases added on 03 Jun 2010
  
  //User provided working directory - existing dir
  //Existing dir should not get deleted
  public final void testUserGivenWorkDir() {
	    try {
	      ArrayToken files = new ArrayToken (remoteinput);
	    
	      jl.cmdFile.setToken(new StringToken(remote_cmdFile));
	      jl.cmdFileLocal.setToken(new BooleanToken(false));
	      jl.remotefiles.setToken(files);
	      
	      setExpertMode();
	      jl.usegivendir.setToken(new BooleanToken(true));
	      jl.workdir.setToken(new StringToken(myworkdir));
	      jl.fire();
	      
	      log.info("### testUserGivenWorkDir: success= " + success + " log= " + logString);
	      assertEquals (true, success);
	    }
	    catch(IllegalActionException e){
	      fail("Exception " + e);
	    }
  }
 
  //User provided working directory - new dir
  public final void testUserGivenNewWorkDir() {
	    try {
	      ArrayToken files = new ArrayToken (remoteinput);
	    
	      jl.cmdFile.setToken(new StringToken(remote_cmdFile));
	      jl.cmdFileLocal.setToken(new BooleanToken(false));
	      jl.remotefiles.setToken(files);
	      
	      setExpertMode();
	      jl.usegivendir.setToken(new BooleanToken(true));
	      jl.workdir.setToken(new StringToken(mynewworkdir));
	      jl.fire();
	      
	      log.info("### testUserGivenNewWorkDir: success= " + success + " log= " + logString);
	      assertEquals (true, success);
	    }
	    catch(IllegalActionException e){
	      fail("Exception " + e);
	    }
  }
  
  //useGivenWorkDir set but no valid working directory provide 
  //should throw error
  public final void testUserGivenWorkDirEmptyDir() {
	    try {
	      ArrayToken files = new ArrayToken (remoteinput);
	    
	      jl.cmdFile.setToken(new StringToken(remote_cmdFile));
	      jl.cmdFileLocal.setToken(new BooleanToken(false));
	      jl.remotefiles.setToken(files);
	      
	      setExpertMode();
	      jl.usegivendir.setToken(new BooleanToken(true));
	      jl.workdir.setToken(new StringToken(""));
	      jl.fire();
	      fail("Should have thrown IllegalActionException" );
	    }
	    catch(IllegalActionException e){
	    	log.info("### testUserGivenWorkDirEmptyDir: Exception= " + e);
	    }
  }
  
  //comma separated list of wait_until status. All valid 
  
  public final void testWaitUntilMultipleValid () {
	try {     
	  ArrayToken files = new ArrayToken (remoteinput);
	        
	  jl.cmdFile.setToken(new StringToken (remote_cmdFile));
	  jl.cmdFileLocal.setToken(new BooleanToken (false));
	  jl.remotefiles.setToken (files);
	
	  jl.waitUntil.setToken("Error,NotInQueue");
	  
	  jl.fire ();
	  log.info("### testWaitUntilMultipleValid: success= " + success + " log= " + logString);
	  assertEquals (true, success);
	}
	catch (IllegalActionException e) {
	  fail("Exception " + e);
	}
  }

  //comma separated list of wait_until status. Containing one invalid status
  public final void testWaitUntilMultipleInvalid () {
	try {     
	  ArrayToken files = new ArrayToken (remoteinput);
	        
	  jl.cmdFile.setToken(new StringToken (remote_cmdFile));
	  jl.cmdFileLocal.setToken(new BooleanToken (false));
	  jl.remotefiles.setToken (files);
	
	  jl.waitUntil.setToken("Wait,Run");
	  
	  jl.fire ();
	  fail("Must have thrown IllegalActionException");
	}
	catch (IllegalActionException e) {
		log.info("### testWaitUntilMultipleInvalid: Exception= " + e);
	}
  }
  
  //comma separated list of wait_until status. Containing one status as ANY
  //Rest should be ignored
  public final void testWaitUntilMultipleANY () {
	try {     
	  ArrayToken files = new ArrayToken (remoteinput);
	        
	  jl.cmdFile.setToken(new StringToken (remote_cmdFile));
	  jl.cmdFileLocal.setToken(new BooleanToken (false));
	  jl.remotefiles.setToken (files);
	
	  jl.waitUntil.setToken("Error,Invalid,ANY");
	  
	  jl.fire ();
	  log.info("### testWaitUntilMultipleANY: success= " + success + " log= " + logString);
	  assertEquals (true, success);
	}
	catch (IllegalActionException e) {
	  fail("Exception " + e);
	}
  } 

  //Default fork script flag set + bin path not set
  public final void testDefaultForkWithoutBinpath() {
		try {     
		  ArrayToken files = new ArrayToken (remoteinput);
		        
		  jl.cmdFile.setToken(new StringToken (remote_cmdFile));
		  jl.cmdFileLocal.setToken(new BooleanToken (false));
		  jl.remotefiles.setToken (files);
		  setExpertMode();
		  jl.defaultForkScript.setToken(new BooleanToken(true));
		  jl.binPath.setToken(new StringToken(""));
		  jl.fire ();
		  log.info("### testDefaultForkWithoutBinpath: success= " + success + " log= " + logString);
		  assertEquals (true, success);
		}
		catch (IllegalActionException e) {
		  fail("Exception " + e);
		}
  }
  */
  
  /* 
   * 
   * 
   * Below test cases should be tested one by one *
   * 
   * 
   */
//
//  //Test alone - 
//  //Move jmgr-fork.sh from job/resources folder before testing this case
//  //jmgr-fork.sh is not present
//  //Default fork script flag is set 
//  //should fail if job manager = Fork, else should be successful
//  //jl.executable.setToken(new StringToken(""));
//  public final void testDefaultForkMissingFile() {
//		try {     
//		  ArrayToken files = new ArrayToken (remoteinput);
//		        
//		  jl.cmdFile.setToken(new StringToken (remote_cmdFile));
//		  jl.cmdFileLocal.setToken(new BooleanToken (false));
//		  jl.remotefiles.setToken (files);
//		  setExpertMode();
//		  jl.defaultForkScript.setToken(new BooleanToken(true));
//		  jl.binPath.setToken(new StringToken(""));
//		  jl.fire ();
//		  if(scheduler.equalsIgnoreCase("Fork")){
//			  fail("Should throw Exception ");
//		  }else{
//			  //default fork flag should be ignored
//			  log.info("### testDefaultForkMissingFile: success= " + success + " log= " + logString);
//			  assertEquals (true, success);
//		  }
//		  
//		}
//		catch (IllegalActionException e) {
//		  if(scheduler.equalsIgnoreCase("Fork")){
//			  //success
//			  log.info("### testDefaultForkMissingFile: Exception= " +e);
//		  }else {
//			  fail("Exception " + e);
//		  }
//		}
// }
  
  

//
////Test this alone - this involves setting of bin path and
////for that we have to make sure a new JobManager is created.
////When run with other test cases - a cached JobManager might get used
////Test case -- Default fork script flag set + bin path set
////jl.executable.setToken(new StringToken(""));
//public final void testDefaultForkWithBinpath() {
//	
//	try {     
//	  ArrayToken files = new ArrayToken (remoteinput);
//	        
//	  jl.cmdFile.setToken(new StringToken (remote_cmdFile));
//	  jl.cmdFileLocal.setToken(new BooleanToken (false));
//	  jl.remotefiles.setToken (files);
//	  setExpertMode();
//	  jl.defaultForkScript.setToken(new BooleanToken(true));
//	  if(scheduler.equalsIgnoreCase("Fork")){
//		  jl.binPath.setToken(new StringToken("/home/d3x140/forkdir/default"));
//	  }else {
//		  jl.binPath.setToken(new StringToken(valid_scheduler_bindir));
//	  }
//	  jl.fire ();
//	  log.info("### testDefaultForkWithBinpath: success= " + success + " log= " + logString);
//	  assertEquals (true, success);
//	}
//	catch (IllegalActionException e) {
//	  fail("Exception " + e);
//	}
//}
  

//
////Test this case alone, else job manager from a previous test case would be used(factory class 
////returns cached obj) and hence bin path will be taken based on previous test cases
//// Expert mode. For example values set in bin path should be ignored if the mode is not 'expert'
//public final void testExpertOff (){
//	try{
//    ArrayToken files = new ArrayToken (remoteinput);
//    jl.cmdFile.setToken(new StringToken(remote_cmdFile));
//    jl.cmdFileLocal.setToken(new BooleanToken(false));
//    jl.remotefiles.setToken(files);
//    
//    removeExpertMode();
//    jl.executable.setToken("/ignored");
//    jl.jobSubmitOptions.setToken("/ignored");
//    
//    if(scheduler.equalsIgnoreCase("Fork")){
//  	  //Stage fork script as another remote inputfile and set bin path
//  	  StringToken [] temp = {new StringToken("/home/d3x140/forkdir/jmgr-fork.sh")};
//        ArrayToken temparray = new ArrayToken(temp);
//  	  files = files.append(temparray);
//  	  jl.binPath.setToken("/home/d3x140/forkdir/jmgr-fork.sh");
//  	  jl.fire();
//  	  assertNotSame(true, success);
//    }
//    else{
//    	  jl.binPath.setToken("/ignored");
// 		  jl.fire();
// 		  log.info("### testExpertOff: success= " + success + " log= " + logString);
// 		  assertEquals (true, success);
//    }
//	}catch(IllegalActionException e){
//		  fail("Exception " + e);
//	}
//}

//
////Test this case alone, else job manager from a previous test case would be used(factory class 
////returns cached obj) and hence bin path will be taken based on previous test cases
//public final void testExpertOnBinPathRight() {
//  try {
//    ArrayToken files = new ArrayToken (remoteinput);
//  
//    jl.cmdFile.setToken(new StringToken(remote_cmdFile));
//    jl.cmdFileLocal.setToken(new BooleanToken(false));
//    jl.remotefiles.setToken(files);
//    
//    setExpertMode();
//    if(scheduler.equalsIgnoreCase("Fork")){
//  	  jl.binPath.setToken(new StringToken(valid_fork_bindir));
//    }else{
//  	  jl.binPath.setToken(new StringToken(valid_scheduler_bindir));
//    }
//    jl.fire();
//    
//    log.info("### testExpertOnBinPathRight: success= " + success + " log= " + logString);
//    assertEquals (true, success);
//  }
//  catch(IllegalActionException e){
//    fail("Exception " + e);
//  }
//}


////Test this case alone, else job manager from a previous test case would be used(factory class 
////returns cached obj) and hence bin path will be taken based on previous test cases
//public final void testExpertOnBinPathWrong() {
//  try {
//    ArrayToken files = new ArrayToken (remoteinput);
//  
//    jl.cmdFile.setToken(new StringToken(remote_cmdFile));
//    jl.cmdFileLocal.setToken(new BooleanToken(false));
//    jl.remotefiles.setToken(files);
//    
//    setExpertMode();
//    jl.binPath.setToken(new StringToken("/wrongbin"));
//    jl.fire();
//    
//    log.info("### testExpertOnBinPathWrong: success= " + success + " log= " + logString);
//    assertNotSame (true, success);
//  }
//  catch(IllegalActionException e){
//    fail("Exception " + e);
//  }
//}
  
    /**
     * Set expert mode to true
     */
    private void setExpertMode() {
      StringBuffer moml = new StringBuffer();
      if (jl.getAttribute("_expertMode") == null) {
        moml.append("<property name=\"_expertMode\" "
            + "class=\"ptolemy.kernel.util.SingletonAttribute\"></property>");
        MoMLChangeRequest request = new MoMLChangeRequest(this, // originator
            jl, // context
            moml.toString(), // MoML code
            null);
        jl.requestChange(request);
      }

    }

    private void removeExpertMode(){
      StringBuffer moml = new StringBuffer();
      if (jl.getAttribute("_expertMode") != null) {
            moml.append("<deleteProperty name=\"_expertMode\"/>");
            MoMLChangeRequest request = new MoMLChangeRequest(this, // originator
              jl, // context
              moml.toString(), // MoML code
              null);
            jl.requestChange(request);
        }
    }
}
