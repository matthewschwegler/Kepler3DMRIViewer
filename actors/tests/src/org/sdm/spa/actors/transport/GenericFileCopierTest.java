/**
 *
 */
package org.sdm.spa.actors.transport;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.IOPortEvent;
import ptolemy.actor.IOPortEventListener;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.moml.MoMLChangeRequest;

/**
 * Test cases for actor org.sdm.spa.actors.transport.GenericFileCopier.
 * Before testing, modify path variables based on test environment
 * @author Chandrika Sivaramakrishnan
 *
 */
public class GenericFileCopierTest extends TestCase {

  public String protocolDefault = "";
  public String protocolSCP = "scp";
  public String protocolSFTP = "sftp";
  public String protocolBBCP = "bbcp";
  public String protocolSRMLite = "srmlite";
   
  
   public String secondaryProtocol = "scp";
  //public String secondaryProtocol = "sftp";
  //public String secondaryProtocol = "bbcp";

  
  private static final Log log = LogFactory.getLog(GenericFileCopierTest.class.getName());
  
  
  /*  // environment - Windows
  private static final String kepler_dir = "C:/Chandrika/new_kepler_sdm/common";
  private static final String local_file1 = "C:\\Chandrika\\new.txt";
  private static final String local_file2 = "C:\\Chandrika\\new6757.txt";
  private static final String local_dir1 = "C:\\Chandrika\\cpdir";
  private static final String local_dir2 = "C:\\Chandrika\\cpdir2";
  private static final String local_nonexisting = "C:\\abc\\def\\nonexisting";
  private static final String local_dir_noperm = "C:\\Chandrika\\no_perm_dir";
  private static final String local_dir_partialcp = "C:\\Chandrika\\partial_cp";
  private static final String local_file_noperm = "C:\\Chandrika\\no_perm.txt";

  private static final String remote_host1 = "d3x140@foap";
  private static final String remote_file1 = "/home/chandrika/f2.txt";
  private static final String remote_host2 = "d3x140@scout";
  private static final String remote_file2 = "/home/chandrika/f2cp.txt";
  private static final String remote_dir1 = "/home/chandrika/testdir";
  private static final String remote_dir2 = "/home/chandrika/testdir";
  private static final String remote_dir_noperm1 = "/home/chandrika/test_perm";
  private static final String remote_dir_noperm2 = "/home/chandrika/test_perm";
  private static final String remote_dir_partialcp1 = "/home/chandrika/partial_cp";
  private static final String remote_dir_partialcp2 = "/home/chandrika/partial_cp";
  private static final String remote_file_noperm1 = "/home/chandrika/noperm.txt";
  private static final String remote_file_noperm2 = "/home/chandrika/noperm.txt";
  private static final String remote_nonexisting = "/abc/nonexistingdir/nonexisting";
*/

  // environment - Mac
//  private static final String kepler_dir = "/Applications/kepler";
//  private static final String local_file1 = "/Users/d3x140/new.txt";
//  private static final String local_file2 = "/Users/d3x140/new6757.txt";
//  private static final String local_dir1 = "/Users/d3x140/testdir";
//  private static final String local_dir2 = "/Users/d3x140/testdir2";
//  private static final String local_nonexisting = "/abc/def/nonexisting";
//  private static final String local_dir_noperm = "/Users/d3x140/no_perm_dir";
//  private static final String local_dir_partialcp = "/Users/d3x140/partial_cp";
//  private static final String local_file_noperm = "/Users/d3x140/noperm.txt";
//
//  private static final String remote_host1 = "d3x140@foap";
//  private static final String remote_file1 = "/home/chandrika/f2.txt";
//  private static final String remote_host2 = "d3x140@scout";
//  private static final String remote_file2 = "/home/chandrika/f2cp.txt";
//  private static final String remote_dir1 = "/home/chandrika/testdir";
//  private static final String remote_dir2 = "/home/chandrika/testdir";
//  private static final String remote_dir_noperm1 = "/home/chandrika/test_perm";
//  private static final String remote_dir_noperm2 = "/home/chandrika/test_perm";
//  private static final String remote_dir_partialcp1 = "/home/chandrika/partial_cp";
//  private static final String remote_dir_partialcp2 = "/home/chandrika/partial_cp";
//  private static final String remote_file_noperm1 = "/home/chandrika/noperm.txt";
//  private static final String remote_file_noperm2 = "/home/chandrika/noperm.txt";
//  private static final String remote_nonexisting = "/abc/nonexistingdir/nonexisting";

 
//environment - Linux
  /*
   * remote_host1 is:
   * * source host for testing default remote to local and
   *   remote to remote protocol
   * * destination host for scp file transfer
   * * source host for all protocols local to remote and
   *   remote to remote file transfer
   */ 
//   private static final String remote_host1 = "anusua@wolverine2.sci.utah.edu";
   private static final String remote_host1 = "gs3dev@130.20.105.156";
   /*
    * remote_host2 is a destination host for:
    * * testing default protocol
    * * scp file transfer
    * * sftp file transfer
    */
//   private static final String remote_host2 = "anusua@shell.sci.utah.edu";
   private static final String remote_host2 = "gs3dev@130.20.107.80";
   /*
    * remote_host3 is:
    * * a destination host for srmlite file transfer for
    *   remote to remote cases
    * * a source and a destination host for bbcp file transfer for
    *   remote to remote cases
    * * a source and a destination host for other remote to
    *   remote tests such as command line options, timeouts etc.
    */
//   private static final String remote_host3 = "anusua@wolverine3.sci.utah.edu";
   private static final String remote_host3 = "gs3dev@130.20.107.80";
   /*
    * remote_host4 is:
    * * a source host for bbcp file transfer for
    *   remote to local cases
    * * a destination host for bbcp file transfer for
    *   remote to remote cases
    */
//   private static final String remote_host4 = "test@fraser.sci.utah.edu";
   private static final String remote_host4 = "gs3dev@130.20.105.156";
   /*
    * remote_host5 is:
    * * a source host for all protocols file transfer for
    *   remote to local cases
    * * a source host for bbcp file transfer for
    *   remote to remote cases
    */
//   private static final String remote_host5 = "test@wolverine2.sci.utah.edu";
   private static final String remote_host5 = "gs3dev@130.20.105.156";
  
   private static final String local_file_source = "/Users/anand/Documents/temp/f1.txt";
   private static final String remote_file_source = "/home/gs3dev/mydir/asd.txt";
   private static final String local_file_destination = "/Users/anand/Documents/test/";
   
   private static final String local_cp_dir_source = "/Users/anand/Documents/temp/md1";
   private static final String local_cp_dir_destination = "/Users/anand/Documents/test/";
   
   private static final String local_dir_source = "/home/gs3dev/scp_testdir";
   private static final String remote_dir_source = "/home/gs3dev/scp_testdir";
   private static final String local_dir_destination = "C:\\test\\";
   private static final String remote_dir_dest = "/home/gs3dev/scp_testdir";
   private static final String remote_dir_destn = "/home/sci/gs3dev/scp_testdir";
   
   private static final String local_dir_sources = "/home/gs3dev/scp_testdir1,/home/gs3dev/scp_testdir2,/home/gs3dev/scp_testdir3";
   private static final String remote_dir_sources = "/home/gs3dev/scp_testdir/scp_testdir1,/home/gs3dev/scp_testdir/scp_testdir2,/home/gs3dev/scp_testdir/scp_testdir3";
   private static final String local_dir_destinations = "/home/gs3dev/scp_testdir";
   private static final String remote_dir_dests = "/home/gs3dev/scp_testdir";
   
   private static final String local_dir_files_sources = "/home/gs3dev/scp_testdir1,/home/gs3dev/scp_testdir2,/home/gs3dev/tmp/aa,/home/gs3dev/tmp/bb.txt";
   private static final String remote_dir_files_sources = "/home/gs3dev/scp_testdir/scp_testdir1,/home/gs3dev/scp_testdir/scp_testdir2,/home/gs3dev/tmp/aa,/home/gs3dev/tmp/bb.txt";
   private static final String local_dir_files_destinations = "/home/gs3dev/scp_testdir";
   private static final String remote_dir_files_dests = "/home/gs3dev/scp_testdir";
   
   private static final String local_dir_files_regexp_sources = "/home/gs3dev/scp_testdir1,/home/gs3dev/scp_testdir2,/home/gs3dev/tmp/aa,/home/gs3dev/tmp/bb.txt,/home/gs3dev/tmp/*.log,/home/gs3dev/tmp/*.txt";
   private static final String remote_dir_files_regexp_sources = "/home/gs3dev/scp_testdir/scp_testdir1,/home/gs3dev/scp_testdir/scp_testdir2,/home/gs3dev/tmp/aa,/home/gs3dev/tmp/bb.txt,/home/gs3dev/tmp/*.log,/home/gs3dev/tmp/*.txt";
   private static final String local_dir_files_regexp_destinations = "/home/gs3dev/scp_testdir";
   private static final String remote_dir_files_regexp_dests = "/home/gs3dev/scp_testdir";
   
   private static final String local_dir_source1 = "C:\\test\\";
   private static final String remote_dir_source1 = "/home/gs3dev/mydir/md1";
   private static final String local_dir_destination1 = "C:\\test\\";
   private static final String remote_dir_dest1 = "/home/gs3dev/test";
   private static final String remote_dir_destination1 = "/home/gs3dev/test/";
   
   private static final String local_dirs_sources1 = "C:\\temp\\md1, C:\\temp\\md2";
   private static final String remote_dirs_sources1 = "/home/gs3dev/mydir/md1 , /home/gs3dev/mydir/md2";
   private static final String local_dirs_destinations1 = "C:\\test\\";
   private static final String remote_dirs_dests1 = "/home/gs3dev/sftp_testdir";
   private static final String remote_dir_destn1 = "/home/sci/gs3dev/sftp_testdir2";
   private static final String remote_dir_destn2 = "/home/sci/gs3dev/sftp_testdir3";
   private static final String remote_dir_destn3 = "/home/sci/gs3dev/sftp_testdir4";
   private static final String remote_dir_destn4 = "/home/sci/gs3dev/sftp_testdir5";
   private static final String remote_dir_destn5 = "/home/sci/gs3dev/sftp_testdir6";
   private static final String remote_dir_destn6 = "/home/gs3dev/test";
   
   private static final String remote_dir_destion1 = "/home/gs3dev/sftp_testdir2";
   private static final String remote_dir_destion2 = "/home/gs3dev/sftp_testdir3";
   private static final String remote_dir_destion3 = "/home/gs3dev/sftp_testdir4";
   private static final String remote_dir_destion4 = "/Users/test/sftp_testdir5";
   private static final String remote_dir_destion5 = "/Users/test/sftp_testdir6";
   private static final String remote_dir_destion6 = "/Users/test/sftp_testdir7";
   
   private static final String remote_dirs_destinations1 = "/home/gs3dev/test";
   
   private static final String local_dirs_files_sources1 = "C:\\temp\\md1 ,C:\\temp\\md2,C:\\temp\\asd f1.txt,C:\\temp\\f1.log";
   private static final String remote_dirs_files_sources1 = "/home/gs3dev/mydir/md1 , /home/gs3dev/mydir/md2,/home/gs3dev/mydir/asd.txt,/home/gs3dev/mydir/t4";
   private static final String local_dirs_files_destinations1 = "C:\\test";
   private static final String remote_dirs_files_dests1 = "/home/gs3dev/sftp_testdir";
   private static final String remote_dirs_files_destinations1 = "/home/gs3dev/test";
   
   private static final String local_dirs_files_regexp_sources1 = "C:\\temp\\md1 ,C:\\temp\\md2,C:\\temp\\*.txt,C:\\temp\\*.log, C:\\temp\\asd";
   private static final String remote_dirs_files_regexp_sources1 = "/home/gs3dev/mydir/md1 , /home/gs3dev/mydir/md2,/home/gs3dev/mydir/xml_input,/home/gs3dev/mydir/t4, /home/gs3dev/mydir/*.txt,/home/gs3dev/mydir/*.rr";
   private static final String local_dirs_files_regexp_destinations1 = "C:\\test\\";
   private static final String remote_dirs_files_regexp_dests1 = "/home/gs3dev/sftp_testdir";
   private static final String remote_dirs_files_regexp_destinations1 = "/home/gs3dev/test";
   
   
   
   
   private static final String local_dir_source2 = "/home/gs3dev/bbcp_testdir";
   private static final String remote_dir_source2 = "/home/gs3dev/bbcp_testdir";
   private static final String new_remote_dir_source2 = "/Users/test/bbcp_testdir";
   private static final String local_dir_destination2 = "/home/gs3dev/bbcp_testdir";
   private static final String remote_dir_destination2 = "/home/gs3dev/bbcp_testdir";
   
   private static final String local_dir_source3 = "/home/gs3dev/srmlite_testdir";
   private static final String remote_dir_source3 = "/home/gs3dev/srmlite_testdir";
   private static final String local_dir_destination3 = "/home/gs3dev/srmlite_testdir";
   private static final String remote_dir_destination3 = "/home/gs3dev/srmlite_testdir1";
   
   
   private static final String remote_dir_source4 = "/home/gs3dev/srmlite_testdir";
   private static final String local_dir_destination4 = "/home/gs3dev/srmlite_testdir4";
   
   private static final String remote_dir_source5 = "/home/gs3dev/srmlite_testdir";
   private static final String local_dir_destination5 = "/home/gs3dev/srmlite_testdir5";
  
   private static final String remote_dir_source6 = "/home/gs3dev/srmlite_testdir";
   private static final String local_dir_destination6 = "/home/gs3dev/srmlite_testdir6";
   
   private static final String remote_dir_source7 = "/home/gs3dev/srmlite_testdir7";
   private static final String local_dir_destination7 = "/home/gs3dev/srmlite_testdir7";
   
   private static final String local_file_homedir_source = "test.txt";
   private static final String remote_file_homedir_source = "bbb.txt";
   private static final String remote_file_destination = "/home/gs3dev/test/cc2.txt";

   private static final String remote_dir_destination = "/home/gs3dev/test/";
   
   private static final String local_files_source = "/Users/anand/Documents/temp/asd, /Users/anand/Documents/temp/f2.log, /Users/anand/Documents/temp/asd f3.txt";
   private static final String local_files_source_with_spaces = "C:\\temp\\asd f1.txt, C:\\temp\\asd f2.txt, C:\\temp\\asd f3.txt";;
   private static final String remote_files_source = "/home/gs3dev/mydir/t1.txt, /home/gs3dev/mydir/t2.txt,/home/gs3dev/mydir/t4";
   private static final String remote_files_source_with_spaces = "/home/gs3dev/mydir/asd t1.rr, /home/gs3dev/mydir/asd.txt,/home/gs3dev/mydir/asd t2.rr";
   
   private static final String local_log_files_source = "C:\\temp\\*.log";
   private static final String local_text_files_source = "C:\\temp\\*.txt";
   private static final String remote_log_files_source = "/home/gs3dev/mydir/*.txt";
   private static final String default_remote_log_files_source = "*.log";
   private static final String remote_text_files_source = "/home/gs3dev/mydir/*.rr";
   private static final String default_remote_text_files_source = "*.txt";
   
   
   private static final String remotesrc_file_homedir_source = "bbb.txt";
   private static final String remotesrc_file_source = "/home/gs3dev/mydir/t1.txt";
   private static final String new_remotesrc_file_source = "/Users/test/tmp/fccp2.txt";
   private static final String remotesrc_files_source = "/home/gs3dev/mydir/t1.txt, /home/gs3dev/mydir/asd t1.rr, /home/gs3dev/mydir/t4";
   private static final String remotesrc_files_source_with_spaces = "/home/gs3dev/tmp/My File1,My File2.txt, /home/gs3dev/tmp/My File3";
   private static final String remotesrc_log_files_source = "/home/gs3dev/mydir/*.rr";
  // private static final String new_remotesrc_log_files_source = "/Users/test/tmp/*.log";
   private static final String new_remotesrc_log_files_source = "/home/test/tmp/*.log";
   private static final String default_remotesrc_log_files_source = "*.log";
   private static final String remotesrc_text_files_source = "/home/gs3dev/mydir/*.txt";
  // private static final String new_remotesrc_text_files_source = "/Users/test/tmp/*.txt";
   private static final String new_remotesrc_text_files_source = "/home/test/tmp/*.txt";
   private static final String default_remotesrc_text_files_source = "*.txt";
   private static final String remotedest_file_destination = "/home/gs3dev/cc.txt";
   private static final String remotedest_dir_destination = "/home/gs3dev/test";
   
//   private static final String remotedest_file_destination1 = "/home/gs3dev/test/cc.txt";
//   private static final String new_remotedest_file_destination1 = "/home/gs3dev/test/cc1.txt";
//   private static final String remotedest_dir_destination1 = "/home/gs3dev/test";
//   private static final String new_remotedest_dir_destination1 = "/home/gs3dev/temp";
//  
//   private static final String local_dir_sources1 = "/home/gs3dev/temp/md1,/home/gs3dev/temp/md2";
//   private static final String remote_dir_sources1 = "/home/gs3dev/bbcp_testdir/bbcp_testdir1,/home/gs3dev/bbcp_testdir/bbcp_testdir2,/home/gs3dev/bbcp_testdir/bbcp_testdir3";
//   private static final String local_dir_destinations1 = "/home/gs3dev/bbcp_testdir";
//   private static final String remote_dir_destinations1 = "/home/gs3dev/bbcp_testdir";
//   
//   private static final String local_dir_files_sources1 = "/home/gs3dev/bbcp_testdir1,/home/gs3dev/bbcp_testdir2,/home/gs3dev/tmp/bb.txt,/home/gs3dev/latest_tmp/dd.txt";
//   private static final String remote_dir_files_sources1 = "/home/gs3dev/bbcp_testdir/bbcp_testdir1,/home/gs3dev/bbcp_testdir/bbcp_testdir2,/home/gs3dev/tmp/bb.txt,/home/gs3dev/tmp/dd.txt";
//   private static final String local_dir_files_destinations1 = "/home/gs3dev/bbcp_testdir";
//   private static final String remote_dir_files_destinations1 = "/home/gs3dev/bbcp_testdir";
//   
//   
//   private static final String local_dir_files_regexp_sources1 = "/home/gs3dev/bbcp_testdir1,/home/gs3dev/bbcp_testdir2,/home/gs3dev/tmp/bb.txt,/home/gs3dev/latest_tmp/dd.txt,/home/gs3dev/tmp/*.txt,/home/gs3dev/tmp/*.log";
//   private static final String remote_dir_files_regexp_sources1 = "/home/gs3dev/bbcp_testdir/bbcp_testdir1,/home/gs3dev/bbcp_testdir/bbcp_testdir2,/home/gs3dev/tmp/bb.txt,/home/gs3dev/tmp/dd.txt,/home/gs3dev/tmp/*.txt,/home/gs3dev/tmp/*.log";
//   private static final String local_dir_files_regexp_destinations1 = "/home/gs3dev/bbcp_testdir";
//   private static final String remote_dir_files_regexp_destinations1 = "/home/gs3dev/bbcp_testdir";
//   
//   private static final String local_dir1 = "/home/gs3dev/Transfer_Test/testdir1";
//   private static final String local_dir2 = "/home/gs3dev/Tested_Transfer/testdir1";
//   private static final String remote_dir1 = "/home/gs3dev/Transfer_Test/testdir1";
//   private static final String remote_dir2 = "/home/gs3dev/Tested_Transfer/testdir2";
//   private static final String local_file1 = "/home/gs3dev/f1.txt";
//   private static final String remote_file1 = "/home/gs3dev/f2.txt";
//   private static final String remote_file2 ="/home/gs3dev/fccp1.txt";
   
   /* Config for testing with ORNL machines */
  //similar set of variables for two remote hosts(host1 and host2).
 // number 1 or 2 are used to prefix the corresponding files
/*
  private static final String remote_host1 = "gggchin@lens.ccs.ornl.gov";
  private static final String remote_file1 = "/autofs/na1_home/gggchin/newtest/f2.txt";
  private static final String remote_host2 = "gggchin@ewok.lb.ccs.ornl.gov";
  private static final String remote_file2 = "/autofs/na1_home/gggchin/f2cp.txt";
  private static final String remote_dir1 = "/autofs/na1_home/gggchin/newtest/testdir";
  private static final String remote_dir2 = "/autofs/na1_home/gggchin/testdir";
  private static final String remote_dir_noperm1 = "/autofs/na1_home/gggchin/newtest/test_perm";
  private static final String remote_dir_noperm2 = "/autofs/na1_home/gggchin/test_perm";
  private static final String remote_dir_partialcp1 = "/autofs/na1_home/gggchin/newtest/partial_cp";
  private static final String remote_dir_partialcp2 = "/autofs/na1_home/gggchin/partial_cp";
  private static final String remote_file_noperm1 = "/autofs/na1_home/gggchin/newtest/noperm.txt";
  private static final String remote_file_noperm2 = "/autofs/na1_home/gggchin/noperm.txt";
  private static final String remote_nonexisting = "/abc/nonexistingdir/nonexisting";
*/

  GenericFileCopier gfc;
  CompositeEntity compositeEntity = new CompositeEntity();
  int exitCode = 0;
  String error = "";

  
  /**
   * @param name
   */

  
  public GenericFileCopierTest(String name) throws Exception {
	    super(name);

	    gfc = new GenericFileCopier(compositeEntity, "remote file copier");
	    IOPortEventListener exitCodelistener = new IOPortEventListener() {
	        public void portEvent(IOPortEvent event) {
	          exitCode = ((IntToken) event.getToken()).intValue();
	        }

	      };
	      IOPortEventListener errorlistener = new IOPortEventListener() {
	        public void portEvent(IOPortEvent event) {
	          error = ((StringToken) event.getToken()).stringValue();
	        }
	      };
	      gfc.exitcode.addIOPortEventListener(exitCodelistener);
	      gfc.errors.addIOPortEventListener(errorlistener);
  }
  
  
  @Override
  protected void setUp() throws Exception {
    exitCode = -999;
    error = "Initialized value";
    gfc.recursive.setToken(new BooleanToken(false));
    gfc.timeoutSeconds.setToken(new IntToken(0));
    gfc.cmdOptions.setToken(new StringToken(""));
    
     }  
  
//  /**
//   * Local to Local copy
//   * Copy one file to another file on localhost.
//   * Should default to using platform-specific copy functionality.
//   */
//  public final void testLocalCopy() {
//    try {
//      gfc.source.setToken(new StringToken("localhost"));
//      gfc.sourceFile.setToken(new StringToken(local_file_source));
//      gfc.destination.setToken("localhost");
//      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//      gfc.fire();
//      log.info("testLocalCopy: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//  /**
//   * Local to Local recursive copy
//   * Copy the contents of one directory to another directory on localhost.
//   * Should default to using platform-specific copy functionality.
//   */
//  public final void testLocalCopyDir() {
//    try {
//      gfc.source.setToken(new StringToken(""));
//      gfc.sourceFile.setToken(new StringToken(local_cp_dir_source));
//      gfc.destination.setToken("");
//      gfc.destinationFile.setToken(new StringToken(local_cp_dir_destination));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testLocalCopyDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }

//  /**
//   * Local to remote
//   * Use SCP to copy one file to another file from localhost to remote host.
//   * Source file will be relative to the home directory.
//   */
//   public final void testSCPLocalToRemoteSingleFileDefaultPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//	      gfc.source.setToken(new StringToken("localhost"));
//	      gfc.sourceFile.setToken(new StringToken(local_file_homedir_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }

///**
// * Local to remote
// * Use SCP to copy one file to another file from localhost to remote host.
// * Source file will have full path.
// */
//public final void testSCPLocalToRemoteSingleFileFullPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteSingleFileFullPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }

///**
// * Local to remote
// * Use SCP to copy one file to directory from localhost to remote host.
// * Source file will have full path.
// */
//public final void testSCPLocalToRemoteSingleFileToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteSingleFileToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
// * Local to remote
// * Use SCP to copy multiple files to directory from localhost to remote host.
// * Source files can have full path or relative path.
// */
//public final void testSCPLocalToRemoteMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_files_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
////
  
///**
// * Local to remote
// * Use SCP to copy multiple files to directory from localhost to remote host.
// * Source files can have full path or relative path.
// */
//public final void testSCPLocalToRemoteMultFilesWithSpacesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_files_source_with_spaces));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
// * Local to remote
// * Use SCP to copy files using a regular expression to directory from localhost to remote host.
// * Test cases are '/some/path/*.log' and '/another/path/*.txt'.
// */
//public final void testSCPLocalToRemoteRegularExprFullPathToDirectory() {
//
//	try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_log_files_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteRegularExprFullPathToDirectory: pattern=" + local_log_files_source + " exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//
//	    exitCode = -999;
//	    error = "Initialized value";
//
//	    try {
//		      gfc.protocol.setToken(new StringToken(protocolSCP));
//		      gfc.source.setToken(new StringToken(""));
//		      gfc.sourceFile.setToken(new StringToken(local_text_files_source));
//		      gfc.destination.setToken(remote_host1);	      
//		      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//		      gfc.fire();
//	          log.info("testSCPLocalToRemoteRegularExprFullPathToDirectory: pattern=" + local_text_files_source + " exitcode= " + exitCode
//		              + " error= " + error);
//		      assertTrue("Error: " + error, error.equals(""));
//		      assertEquals(0, exitCode);
//		    } catch (IllegalActionException e) {
//		      fail("Exception " + e);
//		    }
//	  }


/**
 * Remote to local
 * Use SCP to copy one file to another file from remote machine to localhost.
 * Source file will be relative to the home directory.
 */
//public final void testSCPRemoteToLocalSingleFileDefaultPath() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remote_file_homedir_source));
//      gfc.destination.setToken("localhost");
//      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//      gfc.fire();
//      log.info("testSCPRemoteToLocalSingleFileDefaultPath: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
// }
	 

///**
// * Remote to local
// * Use SCP to copy one file to another file from remote machine to localhost.
// * Source file will have full path. */
//public final void testSCPRemoteToLocalSingleFileFullPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//	      gfc.fire();
//	      log.info("testSCPRemoteToLocalSingleFileFullPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
// * Remote to local
// * Use SCP to copy one file to another file from remote machine to localhost.
// * Source file will have full path.
// */
//public final void testSCPRemoteToLocalSingleFileToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSCPRemoteToLocalSingleFileToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
// * Remote to local
// * Use SCP to copy one file to another file from remote machine to localhost.
// * Source files can have full path or relative path.
// */
//public final void testSCPRemoteToLocalMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_files_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSCPRemoteToLocalMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
// * Remote to local
// * Use SCP to copy one file to another file from remote machine to localhost.
// * Source files can have full path or relative path.
// */
//public final void testSCPRemoteToLocalMultFilesWithSpacesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_files_source_with_spaces));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSCPRemoteToLocalMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
///**
// * Remote to local
// * Use SCP to copy one file to another file from remote machine to localhost.
// * Test cases are '/some/path/*.log' and '/another/path/*.txt'.
// */
//public final void testSCPRemoteToLocalRegularExprFullPathToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host5));
//	      gfc.sourceFile.setToken(new StringToken(remote_log_files_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSCPRemoteToLocalRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	    
//	    
//	    exitCode = -999;
//	    error = "Initialized value";
//         
//	    try {
//		      gfc.protocol.setToken(new StringToken(protocolSCP));
//	          gfc.source.setToken(new StringToken(remote_host5));
//		      gfc.sourceFile.setToken(new StringToken(remote_text_files_source));
//		      gfc.destination.setToken("localhost");
//		      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//		      gfc.fire();
//		      log.info("testSCPRemoteToLocalRegularExprFullPathToDirectory: exitcode= " + exitCode
//		              + " error= " + error);
//		      assertTrue("Error: " + error, error.equals(""));
//		      assertEquals(0, exitCode);
//		    } catch (IllegalActionException e) {
//		      fail("Exception " + e);
//		    }
//      } 


///**
// * Remote to Remote
// * Connecting to Remote source machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files will have relative path.
// */
//
//public final void testSCPRemoteSrcToRemoteDestSingleFileDefaultPath() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
//      gfc.fire();
//      log.info("testScpRemoteSrcToRemoteDestSingleFileDefaultPath: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
///**
// * Remote to Remote
// * Connecting to Remote source machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files will have full path.
// */
//
//public final void testSCPRemoteSrcToRemoteDestSingleFileFullPath() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
//      gfc.fire();
//      log.info("testScpRemoteSrcToRemoteDestSingleFileFullPath: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
///**
// * Remote to Remote
// * Connecting to Remote source machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files will have full path.
// */
//
//public final void testSCPRemoteSrcToRemoteDestSingleFileToDirectory() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//      gfc.fire();
//      log.info("testScpRemoteSrcToRemoteDestSingleFileToDirectory: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
///**
// * Remote to Remote
// * Connecting to Remote source machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files can have full path or relative path.
// */
//
//public final void testSCPRemoteSrcToRemoteDestMultFilesToDirectory() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//      gfc.fire();
//      log.info("testScpRemoteSrcToRemoteDestMultFilesToDirectory: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
///**
// * Remote to Remote
// * Connecting to Remote source machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files can have full path or relative path.
// */
//
//public final void testSCPRemoteSrcToRemoteDestMultFilesWithSpacesToDirectory() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//      gfc.fire();
//      log.info("testScpRemoteSrcToRemoteDestMultFilesToDirectory: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
///**
// * Remote to Remote
// * Connecting to Remote source machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files can have full path or relative path.
// */
//
//public final void testSCPRemoteSrcToRemoteDestRegularExprFullPathToDirectory() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//      gfc.fire();
//      log.info("testScpRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//    exitCode = -999;
//    error = "Initialized value";
//     
//    try {
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host2);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//	      gfc.fire();
//	      log.info("testScpRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
// }

/**
 * Remote to Remote
 * Connecting to Remote destination machine
 * Use SCP to copy one file to another file from remote source machine to remote destination machine
 * Source files will have relative path.
 */

/*public final void testSCPRemoteDestToRemoteSrcSingleFileDefaultPath() {
	  
	  try {
		  gfc.connectFromDest.setToken(BooleanToken.TRUE);
		  gfc.protocol.setToken(new StringToken(protocolSCP));
		  gfc.source.setToken(new StringToken(remote_host1));
	      gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
	      gfc.destination.setToken(remote_host2);
	      gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
	      gfc.fire();
	      log.info("testScpRemoteDestToRemoteSrcSingleFileDefaultPath: exitcode= " + exitCode
	              + " error= " + error);
	      assertTrue("Error: " + error, error.equals(""));
	      assertEquals(0, exitCode);
	    } catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	    
     }
     */

/**
 * Remote to Remote
 * Connecting to Remote destination machine
 * Use SCP to copy one file to another file from remote source machine to remote destination machine
 * Source files will have full path.
 */

//public final void testSCPRemoteDestToRemoteSrcSingleFileFullPath() {
//    try {
//      gfc.connectFromDest.setToken(BooleanToken.TRUE);
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
//      gfc.fire();
//      log.info("testScpRemoteDestToRemoteSrcSingleFileFullPath: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
///**
// * Remote to Remote
// * Connecting to Remote destination machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files will have full path.
// */
//
//public final void testSCPRemoteDestToRemoteSrcSingleFileToDirectory() {
//    try {
//      gfc.connectFromDest.setToken(BooleanToken.TRUE);
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//      gfc.fire();
//      log.info("testScpRemoteDestToRemoteSrcSingleFileToDirectory: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
///**
// * Remote to Remote
// * Connecting to Remote destination machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files can have full path or relative path.
// */
//
//public final void testSCPRemoteDestToRemoteSrcMultFilesToDirectory() {
//    try {
//      gfc.connectFromDest.setToken(BooleanToken.TRUE);
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//      gfc.fire();
//      log.info("testScpRemoteDestToRemoteSrcMultFilesToDirectory: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
///**
// * Remote to Remote
// * Connecting to Remote destination machine
// * Use SCP to copy one file to another file from remote source machine to remote destination machine
// * Source files can have full path or relative path.
// */
//
//public final void testSCPRemoteDestToRemoteSrcRegularExprFullPathToDirectory() {
//    try {
//      gfc.connectFromDest.setToken(BooleanToken.TRUE);	
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//      gfc.fire();
//      log.info("testScpRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//    exitCode = -999;
//    error = "Initialized value";
//     
//    try {
//    	  gfc.connectFromDest.setToken(BooleanToken.TRUE);
//	      gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host2);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//	      gfc.fire();
//	      log.info("testScpRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
// }

///**
// * Local to remote
// * Use SCP to copy a local directory to a remote directory from localhost to remote host.
// * Source files can have full path or relative path.
// */
//
//public final void testSCPLocalToRemoteDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(""));
//      gfc.sourceFile.setToken(new StringToken(local_dir_source));
//      gfc.destination.setToken(remote_host1);
//      gfc.destinationFile.setToken(new StringToken(remote_dir_dest));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testLocalToRemoteDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
///**
// * Remote to Local
// * Use SCP to copy a remote directory to a local directory from remote host to localhost.
// * Source files can have full path or relative path.
// */
//
// public final void testSCPRemoteToLocalDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//      gfc.destination.setToken("localhost"); 
//      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSCPRemoteToLocalDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
// /**
//  * Remote to Remote
//  * Connection to Remote Source
//  * Use SCP to copy a remote directory to a another remote directory from a remote host to another remote destination.
//  * Source files can have full path or relative path.
//  */
//
//  public final void testSCPRemoteSrcToRemoteDestDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSCP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remote_dir_destn));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSCPRemoteSrcToRemoteDestDir: exitcode= " + exitCode + " error= "
//          + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//  
//  /**
//   * Local to remote
//   * Use SCP to copy local directories to a remote directory from localhost to remote host.
//   * Source files can have full path only.
//   */
//
//  public final void testSCPLocalToRemoteMultDirectory() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(""));
//        gfc.sourceFile.setToken(new StringToken(local_dir_sources));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_dir_dests));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSCPLocalToRemoteMultDirectory: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//  /**
//   * Local to remote
//   * Use SCP to copy local directories to a remote directory from localhost to remote host.
//   * Source files can have full path only.
//   */
//
//  public final void testSCPLocalToRemoteMultDirectoryAndFiles() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(""));
//        gfc.sourceFile.setToken(new StringToken(local_dir_files_sources));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_dir_files_dests));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSCPLocalToRemoteMultDirectoryAndFiles: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//  /**
//   * Local to remote
//   * Use SCP to copy local directories to a remote directory from localhost to remote host.
//   * Source files can have full path only.
//   */
//
//  public final void testSCPLocalToRemoteMultDirectoryAndFilesAndRegExp() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(""));
//        gfc.sourceFile.setToken(new StringToken(local_dir_files_regexp_sources));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_dir_files_regexp_dests));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSCPLocalToRemoteMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//  /**
//   * Remote to Local
//   * Use SCP to copy remote directories to a local directory from remote host to localhost.
//   * Source files can have full path only.
//   */
//
//   public final void testSCPRemoteToLocalMultDirectory() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_dir_sources));
//        gfc.destination.setToken("localhost"); 
//        gfc.destinationFile.setToken(new StringToken(local_dir_destinations));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSCPRemoteToLocalMultDirectory: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//   /**
//    * Remote to Local
//    * Use SCP to copy remote directories to a local directory from remote host to localhost.
//    * Source files can have full path only.
//    */
//
//    public final void testSCPRemoteToLocalMultDirectoryAndFiles() {
//       try {
//         gfc.protocol.setToken(new StringToken(protocolSCP));
//         gfc.source.setToken(new StringToken(remote_host1));
//         gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources));
//         gfc.destination.setToken("localhost"); 
//         gfc.destinationFile.setToken(new StringToken(local_dir_files_destinations));
//         gfc.recursive.setToken(new BooleanToken(true));
//         gfc.fire();
//         log.info("testSCPRemoteToLocalMultDirectoryAndFiles: exitcode= " + exitCode
//                 + " error= " + error);
//         assertTrue("Error: " + error, error.equals(""));
//         assertEquals(0, exitCode);
//       } catch (IllegalActionException e) {
//         fail("Exception " + e);
//       }
//     }
//    
//    /**
//     * Remote to Local
//     * Use SCP to copy remote directories to a local directory from remote host to localhost.
//     * Source files can have full path only.
//     */
//
//     public final void testSCPRemoteToLocalMultDirectoryAndFilesAndRegExp() {
//        try {
//          gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir_files_regexp_sources));
//          gfc.destination.setToken("localhost"); 
//          gfc.destinationFile.setToken(new StringToken(local_dir_files_regexp_destinations));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testSCPRemoteToLocalMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode
//                  + " error= " + error);
//          assertTrue("Error: " + error, error.equals(""));
//          assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//      }
//   
//     /**
//      * Remote to Remote
//      * Connection to Remote Dest
//      * Use SCP to copy multiple remote directories to a another remote directory from a remote host to another remote destination.
//      * Source files can have full path only.
//      */
//
//      public final void testSCPRemoteDestToRemoteSrcMultDirectory() {
//        try {
//          gfc.connectFromDest.setToken(BooleanToken.TRUE);
//          gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir_sources));
//          gfc.destination.setToken(remote_host2);
//          gfc.destinationFile.setToken(new StringToken(remote_dir_destn));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testSCPRemoteDestToRemoteSrcMultDirectory: exitcode= " + exitCode + " error= "
//              + error);
//          assertTrue("Error: " + error, error.equals(""));
//          assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//      }
//
//     
//      /**
//       * Remote to Remote
//       * Connection to Remote Dest
//       * Use SCP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//       * Source files can have full path only.
//       */
//
//       public final void testSCPRemoteDestToRemoteSrcMultDirectoryAndFiles() {
//         try {
//           gfc.connectFromDest.setToken(BooleanToken.TRUE);
//           gfc.protocol.setToken(new StringToken(protocolSCP));
//           gfc.source.setToken(new StringToken(remote_host1));
//           gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources));
//           gfc.destination.setToken(remote_host2);
//           gfc.destinationFile.setToken(new StringToken(remote_dir_destn));
//           gfc.recursive.setToken(new BooleanToken(true));
//           gfc.fire();
//           log.info("testSCPRemoteDestToRemoteSrcMultDirectoryAndFiles: exitcode= " + exitCode + " error= "
//               + error);
//           assertTrue("Error: " + error, error.equals(""));
//           assertEquals(0, exitCode);
//         } catch (IllegalActionException e) {
//           fail("Exception " + e);
//         }
//       }
//       
//       
//       /**
//        * Remote to Remote
//        * Connection to Remote Dest
//        * Use SCP to copy multiple remote directories & files & regular expressions to a another remote directory from a remote host to another remote destination.
//        * Source files can have full path only.
//        */
//
//        public final void testSCPRemoteDestToRemoteSrcMultDirectoryAndFilesAndRegExp() {
//          try {
//        	gfc.connectFromDest.setToken(BooleanToken.TRUE);
//            gfc.protocol.setToken(new StringToken(protocolSCP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir_files_regexp_sources));
//            gfc.destination.setToken(remote_host2);
//            gfc.destinationFile.setToken(new StringToken(remote_dir_destn));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//            log.info("testSCPRemoteDestToRemoteSrcMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode + " error= "
//                + error);
//            assertTrue("Error: " + error, error.equals(""));
//            assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//        }
//     
//       
//       /**
//        * Remote to Remote
//        * Connection to Remote Source
//        * Use SCP to copy multiple remote directories to a another remote directory from a remote host to another remote destination.
//        * Source files can have full path only.
//        */
//
//        public final void testSCPRemoteSrcToRemoteDestMultDirectory() {
//          try {
//            gfc.protocol.setToken(new StringToken(protocolSCP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir_sources));
//            gfc.destination.setToken(remote_host2);
//            gfc.destinationFile.setToken(new StringToken(remote_dir_destn));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//            log.info("testSCPRemoteSrcToRemoteDestMultDirectory: exitcode= " + exitCode + " error= "
//                + error);
//            assertTrue("Error: " + error, error.equals(""));
//            assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//        }
//
//       
//        /**
//         * Remote to Remote
//         * Connection to Remote Source
//         * Use SCP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//         * Source files can have full path only.
//         */
//
//         public final void testSCPRemoteSrcToRemoteDestMultDirectoryAndFiles() {
//           try {
//             gfc.protocol.setToken(new StringToken(protocolSCP));
//             gfc.source.setToken(new StringToken(remote_host1));
//             gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources));
//             gfc.destination.setToken(remote_host2);
//             gfc.destinationFile.setToken(new StringToken(remote_dir_destn));
//             gfc.recursive.setToken(new BooleanToken(true));
//             gfc.fire();
//             log.info("testSCPRemoteSrcToRemoteDestMultDirectoryAndFiles: exitcode= " + exitCode + " error= "
//                 + error);
//             assertTrue("Error: " + error, error.equals(""));
//             assertEquals(0, exitCode);
//           } catch (IllegalActionException e) {
//             fail("Exception " + e);
//           }
//         }
//       /**
//        * Remote to Remote
//        * Connection to Remote Source
//        * Use SCP to copy multiple remote directories & files & regular expressions to a another remote directory from a remote host to another remote destination.
//        * Source files can have full path only.
//        */
//
//        public final void testSCPRemoteSrcToRemoteDestMultDirectoryAndFilesAndRegExp() {
//          try {
//            gfc.protocol.setToken(new StringToken(protocolSCP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir_files_regexp_sources));
//            gfc.destination.setToken(remote_host2);
//            gfc.destinationFile.setToken(new StringToken(remote_dir_destn));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//            log.info("testSCPRemoteSrcToRemoteDestMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode + " error= "
//                + error);
//            assertTrue("Error: " + error, error.equals(""));
//            assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//        }
//     
//     
//     
   
///**
// * Local to remote
// * Use SFTP to copy one file to another file from localhost to remote host.
// * Source file will be relative to the home directory.
// */
//public final void testSFTPLocalToRemoteSingleFileDefaultPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_homedir_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
//	      gfc.fire();
//          log.info("testSFTPLocalToRemoteSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//	  
//
/**
* Local to remote
* Use SFTP to copy one file to another file from localhost to remote host.
* Source file will have full path.
*/
public final void testSFTPLocalToRemoteSingleFileFullPath() {
	    try {
	    	System.setProperty("KEPLER_PWD_INPUT_METHOD", "STDIN");
	      gfc.protocol.setToken(new StringToken(protocolSFTP));
	      gfc.source.setToken(new StringToken(""));
	      gfc.sourceFile.setToken(new StringToken(local_file_source));
	      gfc.destination.setToken(remote_host1);	      
	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
	      gfc.fire();
          log.info("testSFTPLocalToRemoteSingleFileFullPath: exitcode= " + exitCode
	              + " error= " + error);
	      assertTrue("Error: " + error, error.equals(""));
	      assertEquals(0, exitCode);
	    } catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	  }

/**
* Local to remote
* Use SFTP to copy one file to directory from localhost to remote host.
* Source file will have full path.
*/
public final void testSFTPLocalToRemoteSingleFileToDirectory() {
	    try {
	    	System.setProperty("KEPLER_PWD_INPUT_METHOD", "STDIN");
	      gfc.protocol.setToken(new StringToken(protocolSFTP));
	      gfc.source.setToken(new StringToken(""));
	      gfc.sourceFile.setToken(new StringToken(local_file_source));
	      gfc.destination.setToken(remote_host1);	      
	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
	      gfc.fire();
          log.info("testSFTPLocalToRemoteSingleFileToDirectory: exitcode= " + exitCode
	              + " error= " + error);
	      assertTrue("Error: " + error, error.equals(""));
	      assertEquals(0, exitCode);
	    } catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	  }

///**
//* Local to remote
//* Use SFTP to copy multiple files to directory from localhost to remote host.
//* Source files can have full path or relative path.
//*/
//public final void testSFTPLocalToRemoteMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_files_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSFTPLocalToRemoteMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Local to remote
//* Use SFTP to copy files using a regular expression to directory from localhost to remote host.
//* Test cases are '/some/path/*.log' and '/another/path/*.txt'.
//*/
//public final void testSFTPLocalToRemoteRegularExprFullPathToDirectory() {
//
//	try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_log_files_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSFTPLocalToRemoteRegularExprFullPathToDirectory: pattern=" + local_log_files_source + " exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//
//	    exitCode = -999;
//	    error = "Initialized value";
//
//	    try {
//		      gfc.protocol.setToken(new StringToken(protocolSFTP));
//		      gfc.source.setToken(new StringToken(""));
//		      gfc.sourceFile.setToken(new StringToken(local_text_files_source));
//		      gfc.destination.setToken(remote_host1);	      
//		      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//		      gfc.fire();
//	          log.info("testSFTPLocalToRemoteRegularExprFullPathToDirectory: pattern=" + local_text_files_source + " exitcode= " + exitCode
//		              + " error= " + error);
//		      assertTrue("Error: " + error, error.equals(""));
//		      assertEquals(0, exitCode);
//		    } catch (IllegalActionException e) {
//		      fail("Exception " + e);
//		    }
//	  }
//
//
///**
//* Remote to local
//* Use SFTP to copy one file to another file from remote machine to localhost.
//* Source file will be relative to the home directory.
//*/
///*public final void testSFTPRemoteToLocalSingleFileDefaultPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_homedir_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//	      gfc.fire();
//	      log.info("testSFTPRemoteToLocalSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//	  */
//
///**
//* Remote to local
//* Use SFTP to copy one file to another file from remote machine to localhost.
//* Source file will have full path. */
//public final void testSFTPRemoteToLocalSingleFileFullPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//	      gfc.fire();
//	      log.info("testSFTPRemoteToLocalSingleFileFullPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Remote to local
//* Use SFTP to copy one file to another file from remote machine to localhost.
//* Source file will have full path.
//*/
//public final void testSFTPRemoteToLocalSingleFileToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSFTPRemoteToLocalSingleFileToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Remote to local
//* Use SFTP to copy one file to another file from remote machine to localhost.
//* Source files can have full path or relative path.
//*/
//public final void testSFTPRemoteToLocalMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_files_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSFTPRemoteToLocalMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
//
///**
//* Remote to local
//* Use SFTP to copy one file to another file from remote machine to localhost.
//* Test cases are '/some/path/*.log' and '/another/path/*.txt'.
//*/
//public final void testSFTPRemoteToLocalRegularExprFullPathToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host5));
//	      gfc.sourceFile.setToken(new StringToken(remote_log_files_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSFTPRemoteToLocalRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	    
//	    
//	    exitCode = -999;
//	    error = "Initialized value";
//       
//	    try {
//		      gfc.protocol.setToken(new StringToken(protocolSCP));
//	          gfc.source.setToken(new StringToken(remote_host5));
//		      gfc.sourceFile.setToken(new StringToken(remote_text_files_source));
//		      gfc.destination.setToken("localhost");
//		      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//		      gfc.fire();
//		      log.info("testSFTPRemoteToLocalRegularExprFullPathToDirectory: exitcode= " + exitCode
//		              + " error= " + error);
//		      assertTrue("Error: " + error, error.equals(""));
//		      assertEquals(0, exitCode);
//		    } catch (IllegalActionException e) {
//		      fail("Exception " + e);
//		    }
//    } 


///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have relative path.
//*/
//
//public final void testSFTPRemoteSrcToRemoteDestSingleFileDefaultPath() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteSrcToRemoteDestSingleFileDefaultPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSFTPRemoteSrcToRemoteDestSingleFileFullPath() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteSrcToRemoteDestSingleFileFullPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}


///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSFTPRemoteSrcToRemoteDestSingleFileToDirectory() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteSrcToRemoteDestSingleFileToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSFTPRemoteSrcToRemoteDestMultFilesToDirectory() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteSrcToRemoteDestMultFilesToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}


///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSFTPRemoteSrcToRemoteDestRegularExprFullPathToDirectory() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host2);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//	      gfc.fire();
//	      log.info("testSFTPRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}

/**
* Remote to Remote
* Connecting to Remote destination machine
* Use SFTP to copy one file to another file from remote source machine to remote destination machine
* Source files will have relative path.
*/

/*public final void testSFTPRemoteDestToRemoteSrcSingleFileDefaultPath() {
	  
	  try {
		  gfc.connectFromDest.setToken(BooleanToken.TRUE);
		  gfc.protocol.setToken(new StringToken(protocolSFTP));
		  gfc.source.setToken(new StringToken(remote_host1));
	      gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
	      gfc.destination.setToken(remote_host2);
	      gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
	      gfc.fire();
	      log.info("testSFTPRemoteDestToRemoteSrcSingleFileDefaultPath: exitcode= " + exitCode
	              + " error= " + error);
	      assertTrue("Error: " + error, error.equals(""));
	      assertEquals(0, exitCode);
	    } catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	    
   }
   */

///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSFTPRemoteDestToRemoteSrcSingleFileFullPath() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteDestToRemoteSrcSingleFileFullPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSFTPRemoteDestToRemoteSrcSingleFileToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteDestToRemoteSrcSingleFileToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSFTPRemoteDestToRemoteSrcMultFilesToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteDestToRemoteSrcMultFilesToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SFTP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSFTPRemoteDestToRemoteSrcRegularExprFullPathToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSFTP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host2);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//    gfc.fire();
//    log.info("testSFTPRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.connectFromDest.setToken(BooleanToken.TRUE);
//  	      gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host2);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//	      gfc.fire();
//	      log.info("testSFTPRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}


//
///**
// * Local to remote
// * Use SFTP to copy a local directory to a remote directory from localhost to remote host.
// * Source files can have full path or relative path.
// */
//
//public final void testSFTPLocalToRemoteDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSFTP));
//      gfc.source.setToken(new StringToken(""));
//      gfc.sourceFile.setToken(new StringToken(local_dir_source1));
//      gfc.destination.setToken(remote_host1);
//      gfc.destinationFile.setToken(new StringToken(remote_dir_destination1));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSFTPLocalToRemoteDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
///**
// * Remote to Local
// * Use SFTP to copy a remote directory to a local directory from remote host to localhost.
// * Source files can have full path or relative path.
// */
//
// public final void testSFTPRemoteToLocalDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSFTP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remote_dir_source1));
//      gfc.destination.setToken("localhost"); 
//      gfc.destinationFile.setToken(new StringToken(local_dir_destination1));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSFTPRemoteToLocalDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
// /**
//  * Remote to Remote
//  * Connection to Remote Source
//  * Use SFTP to copy a remote directory to a another remote directory from a remote host to another remote destination.
//  * Source files can have full path or relative path.
//  */
//
//  public final void testSFTPRemoteSrcToRemoteDestDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSFTP));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remote_dir_source1));
//      gfc.destination.setToken(remote_host2);
//      gfc.destinationFile.setToken(new StringToken(remote_dir_dest1));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSFTPRemoteSrcToRemoteDestDir: exitcode= " + exitCode + " error= "
//          + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//  /**
//   * Local to remote
//   * Use SFTP to copy local directories to a remote directory from localhost to remote host.
//   * Source files can have full path only.
//   */
//
//  public final void testSFTPLocalToRemoteMultDirectory() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSFTP));
//        gfc.source.setToken(new StringToken(""));
//        gfc.sourceFile.setToken(new StringToken(local_dirs_sources1));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_dirs_destinations1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSFTPLocalToRemoteMultDirectory: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//  /**
//   * Local to remote
//   * Use SFTP to copy local directories to a remote directory from localhost to remote host.
//   * Source files can have full path only.
//   */
//
//  public final void testSFTPLocalToRemoteMultDirectoryAndFiles() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSFTP));
//        gfc.source.setToken(new StringToken(""));
//        gfc.sourceFile.setToken(new StringToken(local_dirs_files_sources1));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_dirs_files_destinations1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSFTPLocalToRemoteMultDirectoryAndFiles: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//  /**
//   * Local to remote
//   * Use SFTP to copy local directories to a remote directory from localhost to remote host.
//   * Source files can have full path only.
//   */
//
//  public final void testSFTPLocalToRemoteMultDirectoryAndFilesAndRegExp() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSFTP));
//        gfc.source.setToken(new StringToken(""));
//        gfc.sourceFile.setToken(new StringToken(local_dirs_files_regexp_sources1));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_dirs_files_regexp_destinations1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSFTPLocalToRemoteMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//  /**
//   * Remote to Local
//   * Use SFTP to copy remote directories to a local directory from remote host to localhost.
//   * Source files can have full path or relative path.
//   */
//
//   public final void testSFTPRemoteToLocalMultDirectory() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolSFTP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_dirs_sources1));
//        gfc.destination.setToken("localhost"); 
//        gfc.destinationFile.setToken(new StringToken(local_dirs_destinations1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSFTPRemoteToLocalMultDirectory: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//  
//   /**
//    * Remote to Local
//    * Use SFTP to copy remote directories to a local directory from remote host to localhost.
//    * Source files can have full path or relative path.
//    */
//
//    public final void testSFTPRemoteToLocalMultDirectoryAndFiles() {
//       try {
//         gfc.protocol.setToken(new StringToken(protocolSFTP));
//         gfc.source.setToken(new StringToken(remote_host1));
//         gfc.sourceFile.setToken(new StringToken(remote_dirs_files_sources1));
//         gfc.destination.setToken("localhost"); 
//         gfc.destinationFile.setToken(new StringToken(local_dirs_files_destinations1));
//         gfc.recursive.setToken(new BooleanToken(true));
//         gfc.fire();
//         log.info("testSFTPRemoteToLocalMultDirectoryAndFiles: exitcode= " + exitCode
//                 + " error= " + error);
//         assertTrue("Error: " + error, error.equals(""));
//         assertEquals(0, exitCode);
//       } catch (IllegalActionException e) {
//         fail("Exception " + e);
//       }
//     }
//  
//    /**
//     * Remote to Local
//     * Use SFTP to copy remote directories to a local directory from remote host to localhost.
//     * Source files can have full path or relative path.
//     */
//
//     public final void testSFTPRemoteToLocalMultDirectoryAndFilesAndRegExp() {
//        try {
//          gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dirs_files_regexp_sources1));
//          gfc.destination.setToken("localhost"); 
//          gfc.destinationFile.setToken(new StringToken(local_dirs_files_regexp_destinations1));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testSFTPRemoteToLocalMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode
//                  + " error= " + error);
//          assertTrue("Error: " + error, error.equals(""));
//          assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//      }
//     
//     /**
//      * Remote to Remote
//      * Connection to Remote Source
//      * Use SFTP to copy multiple remote directories to a another remote directory from a remote host to another remote destination.
//      * Source files can have full path only.
//      */
//
//      public final void testSFTPRemoteSrcToRemoteDestMultDirectory() {
//        try {
//          gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir_sources));
//          gfc.destination.setToken(remote_host2);
//          gfc.destinationFile.setToken(new StringToken(remote_dir_destn1));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testSFTPRemoteSrcToRemoteDestMultDirectory: exitcode= " + exitCode + " error= "
//              + error);
//          assertTrue("Error: " + error, error.equals(""));
//          assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//      }
//     
//      /**
//       * Remote to Remote
//       * Connection to Remote Source
//       * Use SFTP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//       * Source files can have full path only.
//       */
//
//       public final void testSFTPRemoteSrcToRemoteDestMultDirectoryAndFiles() {
//         try {
//           gfc.protocol.setToken(new StringToken(protocolSFTP));
//           gfc.source.setToken(new StringToken(remote_host1));
//           gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources));
//           gfc.destination.setToken(remote_host2);
//           gfc.destinationFile.setToken(new StringToken(remote_dir_destn2));
//           gfc.recursive.setToken(new BooleanToken(true));
//           gfc.fire();
//           log.info("testSFTPRemoteSrcToRemoteDestMultDirectoryAndFiles: exitcode= " + exitCode + " error= "
//               + error);
//           assertTrue("Error: " + error, error.equals(""));
//           assertEquals(0, exitCode);
//         } catch (IllegalActionException e) {
//           fail("Exception " + e);
//         }
//       }
//     
//     
//       /**
//        * Remote to Remote
//        * Connection to Remote Source
//        * Use SFTP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//        * Source files can have full path only.
//        */
//
//        public final void testSFTPRemoteSrcToRemoteDestDirectoryAndFilesAndRegExp() {
//          try {
//            gfc.protocol.setToken(new StringToken(protocolSFTP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir_files_regexp_sources));
//            gfc.destination.setToken(remote_host2);
//            gfc.destinationFile.setToken(new StringToken(remote_dir_destn3));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//            log.info("testSFTPRemoteSrcToRemoteDestDirectoryAndFilesAndRegExp: exitcode= " + exitCode + " error= "
//                + error);
//            assertTrue("Error: " + error, error.equals(""));
//            assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//        }
//     
//        /**
//         * Remote to Remote
//         * Connection to Remote Dest
//         * Use SFTP to copy multiple remote directories to a another remote directory from a remote host to another remote destination.
//         * Source files can have full path only.
//         */
//
//         public final void testSFTPRemoteDestToRemoteSrcMultDirectory() {
//           try {
//        	 gfc.connectFromDest.setToken(BooleanToken.TRUE);
//             gfc.protocol.setToken(new StringToken(protocolSFTP));
//             gfc.source.setToken(new StringToken(remote_host1));
//             gfc.sourceFile.setToken(new StringToken(remote_dir_sources));
//             gfc.destination.setToken(remote_host2);
//             gfc.destinationFile.setToken(new StringToken(remote_dir_destn4));
//             gfc.recursive.setToken(new BooleanToken(true));
//             gfc.fire();
//             log.info("testSFTPRemoteSrcToRemoteDestMultDirectory: exitcode= " + exitCode + " error= "
//                 + error);
//             assertTrue("Error: " + error, error.equals(""));
//             assertEquals(0, exitCode);
//           } catch (IllegalActionException e) {
//             fail("Exception " + e);
//           }
//         }
//        
//         /**
//          * Remote to Remote
//          * Connection to Remote Dest
//          * Use SFTP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//          * Source files can have full path only.
//          */
//
//          public final void testSFTPRemoteDestToRemoteSrcMultDirectoryAndFiles() {
//            try {
//              gfc.connectFromDest.setToken(BooleanToken.TRUE);
//              gfc.protocol.setToken(new StringToken(protocolSFTP));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources));
//              gfc.destination.setToken(remote_host2);
//              gfc.destinationFile.setToken(new StringToken(remote_dir_destn5));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//              log.info("testSFTPRemoteSrcToRemoteDestMultDirectoryAndFiles: exitcode= " + exitCode + " error= "
//                  + error);
//              assertTrue("Error: " + error, error.equals(""));
//              assertEquals(0, exitCode);
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//          }
//        
        
//          /**
//           * Remote to Remote
//           * Connection to Remote Dest
//           * Use SFTP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//           * Source files can have full path only.
//           */
//
//           public final void testSFTPRemoteDestToRemoteSrcDirectoryAndFilesAndRegExp() {
//             try {
//               gfc.connectFromDest.setToken(BooleanToken.TRUE);
//               gfc.protocol.setToken(new StringToken(protocolSFTP));
//               gfc.source.setToken(new StringToken(remote_host1));
//               gfc.sourceFile.setToken(new StringToken(remote_dirs_files_regexp_sources1));
//               gfc.destination.setToken(remote_host2);
//               gfc.destinationFile.setToken(new StringToken(remote_dir_destn6));
//               gfc.recursive.setToken(new BooleanToken(true));
//               gfc.fire();
//               log.info("testSFTPRemoteSrcToRemoteDestDirectoryAndFilesAndRegExp: exitcode= " + exitCode + " error= "
//                   + error);
//               assertTrue("Error: " + error, error.equals(""));
//               assertEquals(0, exitCode);
//             } catch (IllegalActionException e) {
//               fail("Exception " + e);
//             }
//           }
//     
////     
//  
//  /**
//   * Local to Remote
//   * Use Default Protocol to copy a local file to another remote file from localhost to a remote host.
//   * Source files can have full path or relative path.
//   */
//public final void testDefaultProtocolLocalToRemote() {
//	  
//	  try {
//		  gfc.protocol.setToken(new StringToken(protocolDefault));
//		  gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
//	      gfc.fire();
//          log.info("testDefaultProtocolLocalToRemote: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//
//     }
//
///**
// * Remote to Local
// * Use Default Protocol to copy a remote file to a local file from remote host to localhost.
// * Source files can have full path or relative path.
// */
// 
// public final void testDefaultProtocolRemoteToLocal() {
//	  
//	  try {
//		  gfc.protocol.setToken(new StringToken(protocolDefault));
//		  gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testDefaultProtocolRemoteToLocal: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//    }
// 
// /**
//  * Remote to Remote
//  * Use Default Protocol to copy a remote file to another remote file from remote host to another remote host.
//  * Source files can have full path or relative path.
//  */
//  
//  public final void testDefaultProtocolRemoteToRemote() {
//	   
//	  try {
//		    gfc.protocol.setToken(new StringToken(protocolDefault));
//		    gfc.source.setToken(new StringToken(remote_host1));
//		    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//		    gfc.destination.setToken(remote_host2);
//		    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination));
//		    gfc.fire();
//		    log.info("testDefaultProtocolRemoteToRemote: exitcode= " + exitCode
//		            + " error= " + error);
//		    assertTrue("Error: " + error, error.equals(""));
//		    assertEquals(0, exitCode);
//		  } catch (IllegalActionException e) {
//		    fail("Exception " + e);
//		  }
//	  }
// 
//
//  
  /**
   * Local to remote
   * Use SRMLite to copy one file to another file from localhost to remote host.
   * Source file will be relative to the home directory.
   */
  /*public final void testSRMLiteLocalToRemoteSingleFileDefaultPath() {
	    try {
	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
	      gfc.srmProtocol.setToken(new StringToken(secondaryProtocol));
	      if(gfc.getAttribute("_expertMode") == null ){
	          setExpertMode();
	      }
	      gfc.source.setToken(new StringToken(""));
	      gfc.sourceFile.setToken(new StringToken(local_file_homedir_source));
	      gfc.destination.setToken(remote_host1);	      
	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
	      gfc.fire();
          log.info("testSRMLiteLocalToRemoteSingleFileDefaultPath: exitcode= " + exitCode
	              + " error= " + error);
	      assertTrue("Error: " + error, error.equals(""));
	      assertEquals(0, exitCode);
	    } catch (IllegalActionException e) {
	      fail("Exception " + e);
	    }
	  }
	  */
//
///**
// * Local to remote
// * Use SRMLite to copy one file to another file from localhost to remote host.
// * Source file will have full path.
// */
//public final void testSRMLiteLocalToRemoteSingleFileFullPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteSingleFileFullPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
// * Local to remote
// * Use SRMLite to copy one file to directory from localhost to remote host.
// * Source file will have full path.
// */
//public final void testSRMLiteLocalToRemoteSingleFileToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSRMLiteLocalToRemoteSingleFileToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
// * Local to remote
// * Use SRMLite to copy multiple files to directory from localhost to remote host.
// * Source files can have full path or relative path.
// */
//public final void testSRMLiteLocalToRemoteMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_files_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//          log.info("testSCPLocalToRemoteMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
//
///**
// * Remote to Local
// * Use SRMLite to copy one file to another file from localhost to remote host.
// * Source file will be relative to the home directory.
// */
///*public final void testSRMLiteRemoteToLocalSingleFileDefaultPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//	      gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_homedir_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteToLocalSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//	  */
//
///**
//* Remote to Local
//* Use SRMLite to copy one file to another file from localhost to remote host.
//* Source file will have full path.
//*/
//public final void testSRMLiteRemoteToLocalSingleFileFullPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//	      gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteToLocalSingleFileFullPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Remote to Local
//* Use SRMLite to copy one file to directory from localhost to remote host.
//* Source file will have full path.
//*/
//public final void testSRMLiteRemoteToLocalSingleFileToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//	      gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteToLocalSingleFileToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }

///**
//* Remote to Local
//* Use SRMLite to copy multiple files to directory from localhost to remote host.
//* Source files can have full path or relative path.
//*/
//public final void testSRMLiteRemoteToLocalMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//	      gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_files_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteToLocalMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }

//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files will have relative path.
//*/
//
///*public final void testSRMLiteRemoteSrcToRemoteDestSingleFileDefaultPath() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteSrcToRemoteDestSingleFileDefaultPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//*/
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSRMLiteRemoteSrcToRemoteDestSingleFileFullPath() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination));
//    gfc.fire();
//    log.info("testSRMLiteRemoteSrcToRemoteDestSingleFileFullPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}

//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSRMLiteRemoteSrcToRemoteDestSingleFileToDirectory() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteSrcToRemoteDestSingleFileToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSRMLiteRemoteSrcToRemoteDestMultFilesToDirectory() {
//  try {
//	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteSrcToRemoteDestMultFilesToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files will have relative path.
//*/
//
///*public final void testSRMLiteRemoteDestToRemoteSrcSingleFileDefaultPath() {
//	  
//	  try {
//		  gfc.connectFromDest.setToken(BooleanToken.TRUE);
//		  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//		  gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
//	      gfc.destination.setToken(remote_host3);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_file_destination1));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteDestToRemoteSrcSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	    
//   }
//   */
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSRMLiteRemoteDestToRemoteSrcSingleFileFullPath() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteDestToRemoteSrcSingleFileFullPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testSRMLiteRemoteDestToRemoteSrcSingleFileToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteDestToRemoteSrcSingleFileToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSRMLiteRemoteDestToRemoteSrcMultFilesToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteDestToRemoteSrcMultFilesToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
///**
//* Local to Remote
//* Use SRMLite to copy one file to another file from local source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSRMLiteLocalToRemoteRegularExprFullPathToDirectory() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(""));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host1);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteLocalToRemoteRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//          gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host1);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testSRMLiteLocalToRemoteRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
//
///**
//* Remote to Local
//* Use SRMLite to copy one file to another file from remote machine to local machine
//* Source files can have full path.
//*/
//
//public final void testSRMLiteRemoteToLocalRegularExprFullPathToDirectory() {
// try {
//    gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host5));
//    gfc.sourceFile.setToken(new StringToken(new_remotesrc_log_files_source));
//    gfc.destination.setToken("");
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteToLocalRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//          gfc.source.setToken(new StringToken(remote_host5));
//	      gfc.sourceFile.setToken(new StringToken(new_remotesrc_text_files_source));
//	      gfc.destination.setToken("");
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteToLocalRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
//
//
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSRMLiteRemoteSrcToRemoteDestRegularExprFullPathToDirectory() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host3);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use SRMLite to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testSRMLiteRemoteDestToRemoteSrcRegularExprFullPathToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolSRMLite));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.connectFromDest.setToken(BooleanToken.TRUE);
//  	      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host3);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
//
///**
// * Local to remote
// * Use SRMLite to copy a local directory to a remote directory from localhost to remote host.
// * Source files can have full path or relative path.
// */
//
//public final void testSRMLiteLocalToRemoteDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//      gfc.source.setToken(new StringToken(""));
//      gfc.sourceFile.setToken(new StringToken(local_dir_source3));
//      gfc.destination.setToken(remote_host1);
//      gfc.destinationFile.setToken(new StringToken(remote_dir_destination3));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSRMLiteLocalToRemoteDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
///**
// * Remote to Local
// * Use SRMLite to copy a remote directory to a local directory from remote host to localhost.
// * Source files can have full path or relative path.
// */
//
// public final void testSRMLiteRemoteToLocalDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolSRMLite));
//      gfc.source.setToken(new StringToken(remote_host1));
//      gfc.sourceFile.setToken(new StringToken(remote_dir_source4));
//      gfc.destination.setToken("localhost"); 
//      gfc.destinationFile.setToken(new StringToken(local_dir_destination4));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSRMLiteRemoteToLocalDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
//
// /**
//  * Remote to Remote
//  * Use SRMLite to copy a remote directory to a another remote directory from a remote host to a remote destination.
//  * Source files can have full path only.
//  */
//
//  public final void testSRMLiteRemoteSrcToRemoteDestDir() {
//     try {
//       gfc.protocol.setToken(new StringToken(protocolSRMLite));
//       gfc.source.setToken(new StringToken(remote_host1));
//       gfc.sourceFile.setToken(new StringToken(remote_dir_source5));
//       gfc.destination.setToken(remote_host3); 
//       gfc.destinationFile.setToken(new StringToken(local_dir_destination5));
//       gfc.recursive.setToken(new BooleanToken(true));
//       gfc.fire();
//       log.info("testSRMLiteRemoteSrcToRemoteDestDir: exitcode= " + exitCode
//               + " error= " + error);
//       assertTrue("Error: " + error, error.equals(""));
//       assertEquals(0, exitCode);
//     } catch (IllegalActionException e) {
//       fail("Exception " + e);
//     }
//   }
//
//  /**
//   * Remote to Remote
//   * Connection to Dest
//   * Use SRMLite to copy a remote directory to a another remote directory from a remote host to a remote destination.
//   * Source files can have full path only.
//   */
//
//   public final void testSRMLiteRemoteDestToRemoteSrcDir() {
//      try {
//    	gfc.connectFromDest.setToken(BooleanToken.TRUE);
//        gfc.protocol.setToken(new StringToken(protocolSRMLite));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_dir_source6));
//        gfc.destination.setToken(remote_host3); 
//        gfc.destinationFile.setToken(new StringToken(local_dir_destination6));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testSRMLiteRemoteDestToRemoteSrcDir: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//
//
//
///**
// * Local to remote
// * Use BBCP to copy one file to another file from localhost to remote host.
// * Source file will be relative to the home directory.
// */
//public final void testBBCPLocalToRemoteSingleFileDefaultPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_homedir_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
//	      gfc.fire();
//        log.info("testBBCPLocalToRemoteSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//	  
//
///**
//* Local to remote
//* Use BBCP to copy one file to another file from localhost to remote host.
//* Source file will have full path.
//*/
//public final void testBBCPLocalToRemoteSingleFileFullPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_file_destination));
//	      gfc.fire();
//        log.info("testBBCPLocalToRemoteSingleFileFullPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Local to remote
//* Use BBCP to copy one file to directory from localhost to remote host.
//* Source file will have full path.
//*/
//public final void testBBCPLocalToRemoteSingleFileToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_file_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//        log.info("testBBCPLocalToRemoteSingleFileToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Local to remote
//* Use BBCP to copy multiple files to directory from localhost to remote host.
//* Source files can have full path or relative path.
//*/
//public final void testBBCPLocalToRemoteMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//	      gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(local_files_source));
//	      gfc.destination.setToken(remote_host1);	      
//	      gfc.destinationFile.setToken(new StringToken(remote_dir_destination));
//	      gfc.fire();
//        log.info("testBBCPLocalToRemoteMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
//
//
///**
//* Remote to local
//* Use BBCP to copy one file to another file from remote machine to localhost.
//* Source file will be relative to the home directory.
//*/
//public final void testBBCPRemoteToLocalSingleFileDefaultPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_homedir_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//	      gfc.fire();
//	      log.info("testBBCPRemoteToLocalSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//	  
//
///**
//* Remote to local
//* Use BBCP to copy one file to another file from remote machine to localhost.
//* Source file will have full path. */
//public final void testBBCPRemoteToLocalSingleFileFullPath() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_file_destination));
//	      gfc.fire();
//	      log.info("testBBCPRemoteToLocalSingleFileFullPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Remote to local
//* Use BBCP to copy one file to another file from remote machine to localhost.
//* Source file will have full path.
//*/
//public final void testBBCPRemoteToLocalSingleFileToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_file_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testBBCPRemoteToLocalSingleFileToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
///**
//* Remote to local
//* Use BBCP to copy one file to another file from remote machine to localhost.
//* Source files can have full path or relative path.
//*/
//public final void testBBCPRemoteToLocalMultFilesToDirectory() {
//	    try {
//	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remote_files_source));
//	      gfc.destination.setToken("localhost");
//	      gfc.destinationFile.setToken(new StringToken(local_dir_destination));
//	      gfc.fire();
//	      log.info("testBBCPRemoteToLocalMultFilesToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	  }
//
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have relative path.
//*/
//
//public final void testBBCPRemoteSrcToRemoteDestSingleFileDefaultPath() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteSrcToRemoteDestSingleFileDefaultPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testBBCPRemoteSrcToRemoteDestSingleFileFullPath() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteSrcToRemoteDestSingleFileFullPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testBBCPRemoteSrcToRemoteDestSingleFileToDirectory() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteSrcToRemoteDestSingleFileToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testBBCPRemoteSrcToRemoteDestMultFilesToDirectory() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteSrcToRemoteDestMultFilesToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have relative path.
//*/
//
//public final void testBBCPRemoteDestToRemoteSrcSingleFileDefaultPath() {
//	  
//	  try {
//		  gfc.connectFromDest.setToken(BooleanToken.TRUE);
//		  gfc.protocol.setToken(new StringToken(protocolBBCP));
//		  gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_file_homedir_source));
//	      gfc.destination.setToken(remote_host3);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_file_destination1));
//	      gfc.fire();
//	      log.info("testBBCPRemoteDestToRemoteSrcSingleFileDefaultPath: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//	    
//   }
//   
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testBBCPRemoteDestToRemoteSrcSingleFileFullPath() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host3));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host1);
//    gfc.destinationFile.setToken(new StringToken(remotedest_file_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteDestToRemoteSrcSingleFileFullPath: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files will have full path.
//*/
//
//public final void testBBCPRemoteDestToRemoteSrcSingleFileToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host3));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_file_source));
//    gfc.destination.setToken(remote_host1);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteDestToRemoteSrcSingleFileToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testBBCPRemoteDestToRemoteSrcMultFilesToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host3));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_files_source));
//    gfc.destination.setToken(remote_host1);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteDestToRemoteSrcMultFilesToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//}
//
//
///**
//* Local to Remote
//* Use BBCP to copy one file to another file from local source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testBBCPLocalToRemoteRegularExprFullPathToDirectory() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(""));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host1);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testBBCPLocalToRemoteRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(""));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host1);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testBBCPLocalToRemoteRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
//
///**
//* Remote to Local
//* Use BBCP to copy one file to another file from remote machine to local machine
//* Source files can have full path.
//*/
//
//public final void testBBCPRemoteToLocalRegularExprFullPathToDirectory() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host5));
//    gfc.sourceFile.setToken(new StringToken(new_remotesrc_log_files_source));
//    gfc.destination.setToken("");
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testSRMLiteRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host5));
//	      gfc.sourceFile.setToken(new StringToken(new_remotesrc_text_files_source));
//	      gfc.destination.setToken("");
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testSRMLiteRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
//
//
//
///**
//* Remote to Remote
//* Connecting to Remote source machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testBBCPRemoteSrcToRemoteDestRegularExprFullPathToDirectory() {
//  try {
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host5));
//    gfc.sourceFile.setToken(new StringToken(new_remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host5));
//	      gfc.sourceFile.setToken(new StringToken(new_remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host3);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testBBCPRemoteSrcToRemoteDestRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
//
///**
//* Remote to Remote
//* Connecting to Remote destination machine
//* Use BBCP to copy one file to another file from remote source machine to remote destination machine
//* Source files can have full path or relative path.
//*/
//
//public final void testBBCPRemoteDestToRemoteSrcRegularExprFullPathToDirectory() {
//  try {
//    gfc.connectFromDest.setToken(BooleanToken.TRUE);
//    gfc.protocol.setToken(new StringToken(protocolBBCP));
//    gfc.source.setToken(new StringToken(remote_host1));
//    gfc.sourceFile.setToken(new StringToken(remotesrc_log_files_source));
//    gfc.destination.setToken(remote_host3);
//    gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//    gfc.fire();
//    log.info("testBBCPRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//            + " error= " + error);
//    assertTrue("Error: " + error, error.equals(""));
//    assertEquals(0, exitCode);
//  } catch (IllegalActionException e) {
//    fail("Exception " + e);
//  }
//  exitCode = -999;
//  error = "Initialized value";
//   
//  try {
//  	      gfc.connectFromDest.setToken(BooleanToken.TRUE);
//  	      gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//	      gfc.sourceFile.setToken(new StringToken(remotesrc_text_files_source));
//	      gfc.destination.setToken(remote_host3);
//	      gfc.destinationFile.setToken(new StringToken(remotedest_dir_destination1));
//	      gfc.fire();
//	      log.info("testBBCPRemoteDestToRemoteSrcRegularExprFullPathToDirectory: exitcode= " + exitCode
//	              + " error= " + error);
//	      assertTrue("Error: " + error, error.equals(""));
//	      assertEquals(0, exitCode);
//	    } catch (IllegalActionException e) {
//	      fail("Exception " + e);
//	    }
//}
//
///**
// * Local to remote
// * Use BBCP to copy a local directory to a remote directory from localhost to remote host.
// * Source files can have full path or relative path.
// */
//
//public final void testBBCPLocalToRemoteDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolBBCP));
//      gfc.source.setToken(new StringToken(""));
//      gfc.sourceFile.setToken(new StringToken(local_dir_source2));
//      gfc.destination.setToken(remote_host1);
//      gfc.destinationFile.setToken(new StringToken(remote_dir_destination2));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testLocalToRemoteDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
///**
// * Remote to Local
// * Use BBCP to copy a remote directory to a local directory from remote host to localhost.
// * Source files can have full path or relative path.
// */
//
// public final void testBBCPRemoteToLocalDir() {
//    try {
//      gfc.protocol.setToken(new StringToken(protocolBBCP));
//      gfc.source.setToken(new StringToken(remote_host4));
//      gfc.sourceFile.setToken(new StringToken(new_remote_dir_source2));
//      gfc.destination.setToken("localhost"); 
//      gfc.destinationFile.setToken(new StringToken(local_dir_destination2));
//      gfc.recursive.setToken(new BooleanToken(true));
//      gfc.fire();
//      log.info("testSCPRemoteToLocalDir: exitcode= " + exitCode
//              + " error= " + error);
//      assertTrue("Error: " + error, error.equals(""));
//      assertEquals(0, exitCode);
//    } catch (IllegalActionException e) {
//      fail("Exception " + e);
//    }
//  }
//
//
//
//
// /**
//  * Local to remote
//  * Use BBCP to copy  local directories to a remote directory from localhost to remote host.
//  * Source files can have full path only.
//  */
//
// public final void testBBCPLocalToRemoteMultDirectory() {
//     try {
//       gfc.protocol.setToken(new StringToken(protocolBBCP));
//       gfc.source.setToken(new StringToken(""));
//       gfc.sourceFile.setToken(new StringToken(local_dir_sources1));
//       gfc.destination.setToken(remote_host1);
//       gfc.destinationFile.setToken(new StringToken(remote_dir_destinations1));
//       gfc.recursive.setToken(new BooleanToken(true));
//       gfc.fire();
//       log.info("testBBCPLocalToRemoteMultDirectory: exitcode= " + exitCode
//               + " error= " + error);
//       assertTrue("Error: " + error, error.equals(""));
//       assertEquals(0, exitCode);
//     } catch (IllegalActionException e) {
//       fail("Exception " + e);
//     }
//   }
//
// /**
//  * Local to remote
//  * Use BBCP to copy  local directories to a remote directory from localhost to remote host.
//  * Source files can have full path only.
//  */
//
// public final void testBBCPLocalToRemoteMultDirectoryAndFiles() {
//     try {
//       gfc.protocol.setToken(new StringToken(protocolBBCP));
//       gfc.source.setToken(new StringToken(""));
//       gfc.sourceFile.setToken(new StringToken(local_dir_files_sources1));
//       gfc.destination.setToken(remote_host1);
//       gfc.destinationFile.setToken(new StringToken(remote_dir_files_destinations1));
//       gfc.recursive.setToken(new BooleanToken(true));
//       gfc.fire();
//       log.info("testBBCPLocalToRemoteMultDirectoryAndFiles: exitcode= " + exitCode
//               + " error= " + error);
//       assertTrue("Error: " + error, error.equals(""));
//       assertEquals(0, exitCode);
//     } catch (IllegalActionException e) {
//       fail("Exception " + e);
//     }
//   }
//
// /**
//  * Local to remote
//  * Use BBCP to copy  local directories to a remote directory from localhost to remote host.
//  * Source files can have full path only.
//  */
//
// public final void testBBCPLocalToRemoteMultDirectoryAndFilesAndRegExp() {
//     try {
//       gfc.protocol.setToken(new StringToken(protocolBBCP));
//       gfc.source.setToken(new StringToken(""));
//       gfc.sourceFile.setToken(new StringToken(local_dir_files_regexp_sources1));
//       gfc.destination.setToken(remote_host1);
//       gfc.destinationFile.setToken(new StringToken(remote_dir_files_regexp_destinations1));
//       gfc.recursive.setToken(new BooleanToken(true));
//       gfc.fire();
//       log.info("testBBCPLocalToRemoteMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode
//               + " error= " + error);
//       assertTrue("Error: " + error, error.equals(""));
//       assertEquals(0, exitCode);
//     } catch (IllegalActionException e) {
//       fail("Exception " + e);
//     }
//   }
//
// /**
//  * Remote to Local
//  * Use BBCP to copy remote directories to a local directory from remote host to localhost.
//  * Source files can have full path only.
//  */
//
//  public final void testBBCPRemoteToLocalMultDirectory() {
//     try {
//       gfc.protocol.setToken(new StringToken(protocolBBCP));
//       gfc.source.setToken(new StringToken(remote_host1));
//       gfc.sourceFile.setToken(new StringToken(remote_dir_sources1));
//       gfc.destination.setToken("localhost"); 
//       gfc.destinationFile.setToken(new StringToken(local_dir_destinations1));
//       gfc.recursive.setToken(new BooleanToken(true));
//       gfc.fire();
//       log.info("testBBCPRemoteToLocalMultDirectory: exitcode= " + exitCode
//               + " error= " + error);
//       assertTrue("Error: " + error, error.equals(""));
//       assertEquals(0, exitCode);
//     } catch (IllegalActionException e) {
//       fail("Exception " + e);
//     }
//   }
//
//  /**
//   * Remote to Local
//   * Use BBCP to copy remote directories to a local directory from remote host to localhost.
//   * Source files can have full path only.
//   */
//
//   public final void testBBCPRemoteToLocalMultDirectoryAndFiles() {
//      try {
//        gfc.protocol.setToken(new StringToken(protocolBBCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources1));
//        gfc.destination.setToken("localhost"); 
//        gfc.destinationFile.setToken(new StringToken(local_dir_files_destinations1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testBBCPRemoteToLocalMultDirectoryAndFiles: exitcode= " + exitCode
//                + " error= " + error);
//        assertTrue("Error: " + error, error.equals(""));
//        assertEquals(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//    }
//
//
//   /**
//    * Remote to Local
//    * Use BBCP to copy remote directories to a local directory from remote host to localhost.
//    * Source files can have full path only.
//    */
//
//    public final void testBBCPRemoteToLocalMultDirectoryAndFilesAndRegExp() {
//       try {
//         gfc.protocol.setToken(new StringToken(protocolBBCP));
//         gfc.source.setToken(new StringToken(remote_host1));
//         gfc.sourceFile.setToken(new StringToken(remote_dir_files_regexp_sources1));
//         gfc.destination.setToken("localhost"); 
//         gfc.destinationFile.setToken(new StringToken(local_dir_files_regexp_destinations1));
//         gfc.recursive.setToken(new BooleanToken(true));
//         gfc.fire();
//         log.info("testBBCPRemoteToLocalMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode
//                 + " error= " + error);
//         assertTrue("Error: " + error, error.equals(""));
//         assertEquals(0, exitCode);
//       } catch (IllegalActionException e) {
//         fail("Exception " + e);
//       }
//     }
//   
//    /**
//     * Remote to Remote
//     * Connection to Remote Source
//     * Use BBCP to copy multiple remote directories to a another remote directory from a remote host to another remote destination.
//     * Source files can have full path only.
//     */
//
//     public final void testBBCPRemoteSrcToRemoteDestMultDirectory() {
//       try {
//         gfc.protocol.setToken(new StringToken(protocolBBCP));
//         gfc.source.setToken(new StringToken(remote_host1));
//         gfc.sourceFile.setToken(new StringToken(remote_dir_sources));
//         gfc.destination.setToken(remote_host3);
//         gfc.destinationFile.setToken(new StringToken(remote_dir_destion1));
//         gfc.recursive.setToken(new BooleanToken(true));
//         gfc.fire();
//         log.info("testBBCPRemoteSrcToRemoteDestMultDirectory: exitcode= " + exitCode + " error= "
//             + error);
//         assertTrue("Error: " + error, error.equals(""));
//         assertEquals(0, exitCode);
//       } catch (IllegalActionException e) {
//         fail("Exception " + e);
//       }
//     }
//    
//     /**
//      * Remote to Remote
//      * Connection to Remote Source
//      * Use BBCP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//      * Source files can have full path only.
//      */
//
//      public final void testBBCPRemoteSrcToRemoteDestMultDirectoryAndFiles() {
//        try {
//          gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources));
//          gfc.destination.setToken(remote_host3);
//          gfc.destinationFile.setToken(new StringToken(remote_dir_destion2));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testBBCPRemoteSrcToRemoteDestMultDirectoryAndFiles: exitcode= " + exitCode + " error= "
//              + error);
//          assertTrue("Error: " + error, error.equals(""));
//          assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//      }
//    
//    
//      /**
//       * Remote to Remote
//       * Connection to Remote Source
//       * Use BBCP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//       * Source files can have full path only.
//       */
//
//       public final void testBBCPRemoteSrcToRemoteDestMultDirectoryAndFilesAndRegExp() {
//         try {
//           gfc.protocol.setToken(new StringToken(protocolBBCP));
//           gfc.source.setToken(new StringToken(remote_host1));
//           gfc.sourceFile.setToken(new StringToken(remote_dir_files_regexp_sources));
//           gfc.destination.setToken(remote_host3);
//           gfc.destinationFile.setToken(new StringToken(remote_dir_destion3));
//           gfc.recursive.setToken(new BooleanToken(true));
//           gfc.fire();
//           log.info("testBBCPRemoteSrcToRemoteDestMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode + " error= "
//               + error);
//           assertTrue("Error: " + error, error.equals(""));
//           assertEquals(0, exitCode);
//         } catch (IllegalActionException e) {
//           fail("Exception " + e);
//         }
//       }
//    
//       /**
//        * Remote to Remote
//        * Connection to Remote Dest
//        * Use BBCP to copy multiple remote directories to a another remote directory from a remote host to another remote destination.
//        * Source files can have full path only.
//        */
//
//        public final void testBBCPRemoteDestToRemoteSrcMultDirectory() {
//          try {
//        	gfc.connectFromDest.setToken(BooleanToken.TRUE);
//            gfc.protocol.setToken(new StringToken(protocolBBCP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir_sources));
//            gfc.destination.setToken(remote_host4);
//            gfc.destinationFile.setToken(new StringToken(remote_dir_destion4));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//            log.info("testBBCPRemoteSrcToRemoteDestMultDirectory: exitcode= " + exitCode + " error= "
//                + error);
//            assertTrue("Error: " + error, error.equals(""));
//            assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//        }
//       
//        /**
//         * Remote to Remote
//         * Connection to Remote Dest
//         * Use BBCP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//         * Source files can have full path only.
//         */
//
//         public final void testBBCPRemoteDestToRemoteSrcMultDirectoryAndFiles() {
//           try {
//        	 gfc.connectFromDest.setToken(BooleanToken.TRUE);
//             gfc.protocol.setToken(new StringToken(protocolBBCP));
//             gfc.source.setToken(new StringToken(remote_host1));
//             gfc.sourceFile.setToken(new StringToken(remote_dir_files_sources));
//             gfc.destination.setToken(remote_host4);
//             gfc.destinationFile.setToken(new StringToken(remote_dir_destion5));
//             gfc.recursive.setToken(new BooleanToken(true));
//             gfc.fire();
//             log.info("testBBCPRemoteSrcToRemoteDestMultDirectoryAndFiles: exitcode= " + exitCode + " error= "
//                 + error);
//             assertTrue("Error: " + error, error.equals(""));
//             assertEquals(0, exitCode);
//           } catch (IllegalActionException e) {
//             fail("Exception " + e);
//           }
//         }
//       
//       
//         /**
//          * Remote to Remote
//          * Connection to Remote Dest
//          * Use BBCP to copy multiple remote directories & files to a another remote directory from a remote host to another remote destination.
//          * Source files can have full path only.
//          */
//
//          public final void testBBCPRemoteDestToRemoteSrcMultDirectoryAndFilesAndRegExp() {
//            try {
//              gfc.connectFromDest.setToken(BooleanToken.TRUE);
//              gfc.protocol.setToken(new StringToken(protocolBBCP));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_dir_files_regexp_sources));
//              gfc.destination.setToken(remote_host4);
//              gfc.destinationFile.setToken(new StringToken(remote_dir_destion6));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//              log.info("testBBCPRemoteDestToRemoteSrcMultDirectoryAndFilesAndRegExp: exitcode= " + exitCode + " error= "
//                  + error);
//              assertTrue("Error: " + error, error.equals(""));
//              assertEquals(0, exitCode);
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//          }
//          
//         
///**
// * Test Timeout for Local to Remote
// * For Protocol BBCP
// */   
//      public final void testTimeoutL2RBBCP() {
//        	    try {
//        	    	gfc.protocol.setToken(new StringToken(protocolBBCP));
//        	        gfc.source.setToken(new StringToken("localhost"));
//        	        gfc.sourceFile.setToken(new StringToken(local_dir1));
//        	        gfc.destination.setToken(remote_host1);
//        	        gfc.destinationFile.setToken(new StringToken(remote_dir2));
//        	        gfc.recursive.setToken(new BooleanToken(true));
//        	        gfc.timeoutSeconds.setToken(new IntToken(1));
//        	        gfc.fire();
//                    log.info("testTimeoutL2R: exitcode= " + exitCode + " error= "
//        	            + error);
//        	        if (error.equals("")) {
//        	          fail("No error message returned for timeout exception");
//        	        }
//        	        assertNotSame(0, exitCode);
//
//        	      } catch (IllegalActionException e) {
//        	        fail("Exception " + e);
//        	      }
//        }
//          
///**
//* Test Timeout for Local to Remote
//* For Protocol SRMLite
//*/   
//            public final void testTimeoutL2RSRMLite() {
//              	    try {
//                	    	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                	        gfc.source.setToken(new StringToken("localhost"));
//                	        gfc.sourceFile.setToken(new StringToken(local_dir1));
//                	        gfc.destination.setToken(remote_host1);
//                	        gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                	        gfc.recursive.setToken(new BooleanToken(true));
//                	        gfc.timeoutSeconds.setToken(new IntToken(1));
//                	        gfc.fire();
//                          log.info("testTimeoutL2R: exitcode= " + exitCode + " error= "
//                	            + error);
//                	        if (error.equals("")) {
//                	          fail("No error message returned for timeout exception");
//                	        }
//                	        assertNotSame(0, exitCode);
//
//                	      } catch (IllegalActionException e) {
//                	        fail("Exception " + e);
//                	      }
//              }
//          
///**
//* Test Timeout for Remote to Local
//* For Protocol BBCP
//*/   
//            
//            public final void testTimeoutR2LBBCP() {
//                try {
//                	gfc.protocol.setToken(new StringToken(protocolBBCP));
//                    gfc.source.setToken(new StringToken(remote_host1));
//                    gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                    gfc.destination.setToken("");
//                    gfc.destinationFile.setToken(new StringToken(local_dir2));
//                    gfc.recursive.setToken(new BooleanToken(true));
//                    gfc.timeoutSeconds.setToken(new IntToken(1));
//                    gfc.fire();
//
//                    log.info("testTimeoutR2L: exitcode= " + exitCode + " error= "
//                        + error);
//                    if (error.equals("")) {
//                      fail("No error message returned for timeout exception");
//                    }
//                    assertNotSame(0, exitCode);
//                  } catch (IllegalActionException e) {
//                    fail("Exception " + e);
//                  }
//                
//              }
//         
//            
///**
//* Test Timeout for Remote to Local
//* For Protocol SRMLite
//*/          
//            public final void testTimeoutR2LSRMLite() {
//                try {
//                	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                    gfc.source.setToken(new StringToken(remote_host1));
//                    gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                    gfc.destination.setToken("");
//                    gfc.destinationFile.setToken(new StringToken(local_dir2));
//                    gfc.recursive.setToken(new BooleanToken(true));
//                    gfc.timeoutSeconds.setToken(new IntToken(1));
//                    gfc.fire();
//
//                    log.info("testTimeoutR2L: exitcode= " + exitCode + " error= "
//                        + error);
//                    if (error.equals("")) {
//                      fail("No error message returned for timeout exception");
//                    }
//                    assertNotSame(0, exitCode);
//                  } catch (IllegalActionException e) {
//                    fail("Exception " + e);
//                  }
//                
//              } 
//            
//            
///**
//* Test Timeout for Remote to Remote
//* For Protocol SCP, SFTP, BBCP & SRMLite
//*/       
//            public final void testTimeoutR2R() {
//                try {
//                  gfc.protocol.setToken(new StringToken(protocolSCP));
//                  gfc.source.setToken(new StringToken(remote_host1));
//                  gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                  gfc.destination.setToken(remote_host3);
//                  gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                  gfc.recursive.setToken(new BooleanToken(true));
//                  gfc.timeoutSeconds.setToken(new IntToken(1));
//                  gfc.fire();
//
//                  log.info("testTimeoutR2R: exitcode= " + exitCode + " error= "
//                      + error);
//                  if (error.equals("")) {
//                    fail("No error message returned for timeout exception");
//                  }
//                  assertNotSame(0, exitCode);
//                } catch (IllegalActionException e) {
//                  fail("Exception " + e);
//                }
//                
//                exitCode = -999;
//                error = "Initialized value";
//                
//                try {
//                    gfc.protocol.setToken(new StringToken(protocolSFTP));
//                    gfc.source.setToken(new StringToken(remote_host1));
//                    gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                    gfc.destination.setToken(remote_host3);
//                    gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                    gfc.recursive.setToken(new BooleanToken(true));
//                    gfc.timeoutSeconds.setToken(new IntToken(1));
//                    gfc.fire();
//
//                    log.info("testTimeoutR2R: exitcode= " + exitCode + " error= "
//                        + error);
//                    if (error.equals("")) {
//                      fail("No error message returned for timeout exception");
//                    }
//                    assertNotSame(0, exitCode);
//                  } catch (IllegalActionException e) {
//                    fail("Exception " + e);
//                  }
//                
//                  
//                  exitCode = -999;
//                  error = "Initialized value";
//                  
//                  try {
//                      gfc.protocol.setToken(new StringToken(protocolBBCP));
//                      gfc.source.setToken(new StringToken(remote_host1));
//                      gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                      gfc.destination.setToken(remote_host3);
//                      gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                      gfc.recursive.setToken(new BooleanToken(true));
//                      gfc.timeoutSeconds.setToken(new IntToken(1));
//                      gfc.fire();
//
//                      log.info("testTimeoutR2R: exitcode= " + exitCode + " error= "
//                          + error);
//                      if (error.equals("")) {
//                        fail("No error message returned for timeout exception");
//                      }
//                      assertNotSame(0, exitCode);
//                    } catch (IllegalActionException e) {
//                      fail("Exception " + e);
//                    }
//                    
//                    
//                    exitCode = -999;
//                    error = "Initialized value";
//                    
//                    try {
//                        gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                        gfc.source.setToken(new StringToken(remote_host1));
//                        gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                        gfc.destination.setToken(remote_host3);
//                        gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                        gfc.recursive.setToken(new BooleanToken(true));
//                        gfc.timeoutSeconds.setToken(new IntToken(1));
//                        gfc.fire();
//
//                        log.info("testTimeoutR2R: exitcode= " + exitCode + " error= "
//                            + error);
//                        if (error.equals("")) {
//                          fail("No error message returned for timeout exception");
//                        }
//                        assertNotSame(0, exitCode);
//                      } catch (IllegalActionException e) {
//                        fail("Exception " + e);
//                      }
//                
//              }
//
// /**
//  * Test for Local to Remote Valid Command Options
//  * For Protocol SCP, SFTP, BBCP & SRMLite
//  */                   
//            public final void testValidCmdOptionsL2R() {
//                  try {
//                	gfc.protocol.setToken(new StringToken(protocolSCP));
//                    gfc.source.setToken(new StringToken("localhost"));
//                    gfc.sourceFile.setToken(new StringToken(local_dir1));
//                    gfc.destination.setToken(remote_host1);
//                    gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                    gfc.recursive.setToken(new BooleanToken(true));
//                    // set expert mode
//                    setExpertMode();
//                    // set command line options
//                    gfc.cmdOptions.setToken(new StringToken("-C"));
//                    gfc.fire();
//                    log.info("testValidCmdOptionsL2R: exitcode= " + exitCode
//                        + " error= " + error);
//                    assertTrue("Error: " + error, error.equals(""));
//                    assertEquals(0, exitCode);
//
//                  } catch (IllegalActionException e) {
//                    fail("Exception " + e);
//                  }
//                  
//                  exitCode = -999;
//                  error = "Initialized value";
//                  
//                  try {
//                  	gfc.protocol.setToken(new StringToken(protocolSFTP));
//                      gfc.source.setToken(new StringToken("localhost"));
//                      gfc.sourceFile.setToken(new StringToken(local_dir1));
//                      gfc.destination.setToken(remote_host1);
//                      gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                      gfc.recursive.setToken(new BooleanToken(true));
//                      // set expert mode
//                      setExpertMode();
//                      // set command line options
//                      gfc.cmdOptions.setToken(new StringToken("-C"));
//                      gfc.fire();
//                      log.info("testValidCmdOptionsL2R: exitcode= " + exitCode
//                          + " error= " + error);
//                      assertTrue("Error: " + error, error.equals(""));
//                      assertEquals(0, exitCode);
//
//                    } catch (IllegalActionException e) {
//                      fail("Exception " + e);
//                    }
//                    
//                    exitCode = -999;
//                    error = "Initialized value";
//                    
//                    try {
//                    	gfc.protocol.setToken(new StringToken(protocolBBCP));
//                        gfc.source.setToken(new StringToken("localhost"));
//                        gfc.sourceFile.setToken(new StringToken(local_dir1));
//                        gfc.destination.setToken(remote_host1);
//                        gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                        gfc.recursive.setToken(new BooleanToken(true));
//                        // set expert mode
//                        setExpertMode();
//                        // set command line options
//                        gfc.cmdOptions.setToken(new StringToken("-c"));
//                        gfc.fire();
//                        log.info("testValidCmdOptionsL2R: exitcode= " + exitCode
//                            + " error= " + error);
//                        assertTrue("Error: " + error, error.equals(""));
//                        assertEquals(0, exitCode);
//
//                      } catch (IllegalActionException e) {
//                        fail("Exception " + e);
//                      }
//                      
//                      exitCode = -999;
//                      error = "Initialized value";
//                      
//                      try {
//                      	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                          gfc.source.setToken(new StringToken("localhost"));
//                          gfc.sourceFile.setToken(new StringToken(local_dir1));
//                          gfc.destination.setToken(remote_host1);
//                          gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                          gfc.recursive.setToken(new BooleanToken(true));
//                          // set expert mode
//                          setExpertMode();
//                          // set command line options
//                          gfc.cmdOptions.setToken(new StringToken("-autoclean"));
//                          gfc.fire();
//                          log.info("testValidCmdOptionsL2R: exitcode= " + exitCode
//                              + " error= " + error);
//                          assertTrue("Error: " + error, error.equals(""));
//                          assertEquals(0, exitCode);
//
//                        } catch (IllegalActionException e) {
//                          fail("Exception " + e);
//                        }
//              }
//            
///**
// * Test for Remote to Local Valid Command Options
// * For Protocol SCP, SFTP, BBCP & SRMLite
// */            
//            
//    public final void testValidCmdOptionsR2L() {
//             try {
//            	    gfc.protocol.setToken(new StringToken(protocolSCP));
//                    gfc.source.setToken(new StringToken(remote_host1));
//                    gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                    gfc.destination.setToken("");
//                    gfc.destinationFile.setToken(new StringToken(local_dir2));
//                    gfc.recursive.setToken(new BooleanToken(true));
//                    // set expert mode
//                    setExpertMode();
//                    // set command line options
//                    gfc.cmdOptions.setToken(new StringToken("-C"));
//                    gfc.fire();
//                    log.info("testValidCmdOptionsR2L: exitcode= " + exitCode
//                        + " error= " + error);
//                    assertTrue("Error: " + error, error.equals(""));
//                    assertEquals(0, exitCode);
//                  } catch (IllegalActionException e) {
//                    fail("Exception " + e);
//                  }
//                  
//                  exitCode = -999;
//                  error = "Initialized value";
//                  
//                  try {
//                	  gfc.protocol.setToken(new StringToken(protocolSFTP));
//                      gfc.source.setToken(new StringToken(remote_host1));
//                      gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                      gfc.destination.setToken("");
//                      gfc.destinationFile.setToken(new StringToken(local_dir2));
//                      gfc.recursive.setToken(new BooleanToken(true));
//                      // set expert mode
//                      setExpertMode();
//                      // set command line options
//                      gfc.cmdOptions.setToken(new StringToken("-C"));
//                      gfc.fire();
//                      log.info("testValidCmdOptionsR2L: exitcode= " + exitCode
//                          + " error= " + error);
//                      assertTrue("Error: " + error, error.equals(""));
//                      assertEquals(0, exitCode);
//                    } catch (IllegalActionException e) {
//                      fail("Exception " + e);
//                    }
//                    
//                    exitCode = -999;
//                    error = "Initialized value";
//                    
//                    try {
//                  	  gfc.protocol.setToken(new StringToken(protocolBBCP));
//                        gfc.source.setToken(new StringToken(remote_host1));
//                        gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                        gfc.destination.setToken("");
//                        gfc.destinationFile.setToken(new StringToken(local_dir2));
//                        gfc.recursive.setToken(new BooleanToken(true));
//                        // set expert mode
//                        setExpertMode();
//                        // set command line options
//                        gfc.cmdOptions.setToken(new StringToken("-c"));
//                        gfc.fire();
//                        log.info("testValidCmdOptionsR2L: exitcode= " + exitCode
//                            + " error= " + error);
//                        assertTrue("Error: " + error, error.equals(""));
//                        assertEquals(0, exitCode);
//                      } catch (IllegalActionException e) {
//                        fail("Exception " + e);
//                      }
//                      
//                      exitCode = -999;
//                      error = "Initialized value";
//                      
//                      try {
//                    	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                          gfc.source.setToken(new StringToken(remote_host1));
//                          gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                          gfc.destination.setToken("");
//                          gfc.destinationFile.setToken(new StringToken(local_dir2));
//                          gfc.recursive.setToken(new BooleanToken(true));
//                          // set expert mode
//                          setExpertMode();
//                          // set command line options
//                          gfc.cmdOptions.setToken(new StringToken("-autoclean"));
//                          gfc.fire();
//                          log.info("testValidCmdOptionsR2L: exitcode= " + exitCode
//                              + " error= " + error);
//                          assertTrue("Error: " + error, error.equals(""));
//                          assertEquals(0, exitCode);
//                        } catch (IllegalActionException e) {
//                          fail("Exception " + e);
//                        }    
//              }
//    
//    
///**
// *Test for Remote to Remote Valid Command Options
// * For Protocol SCP, SFTP, BBCP & SRMLite
// */
//    
//    public final void testValidCmdOptionsR2R() {
//        try {
//          gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir1));
//          gfc.destination.setToken(remote_host3);
//          gfc.destinationFile.setToken(new StringToken(remote_dir2));
//          gfc.recursive.setToken(new BooleanToken(true));
//          // set expert mode
//          setExpertMode();
//          // set command line options
//          gfc.cmdOptions.setToken(new StringToken("-C"));
//          gfc.fire();
//          log.info("testValidCmdOptionsR2R: exitcode= " + exitCode
//              + " error= " + error);
//          assertTrue("Error: " + error, error.equals(""));
//          assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//            gfc.protocol.setToken(new StringToken(protocolSFTP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir1));
//            gfc.destination.setToken(remote_host3);
//            gfc.destinationFile.setToken(new StringToken(remote_dir2));
//            gfc.recursive.setToken(new BooleanToken(true));
//            // set expert mode
//            setExpertMode();
//            // set command line options
//            gfc.cmdOptions.setToken(new StringToken("-C"));
//            gfc.fire();
//            log.info("testValidCmdOptionsR2R: exitcode= " + exitCode
//                + " error= " + error);
//            assertTrue("Error: " + error, error.equals(""));
//            assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//              gfc.protocol.setToken(new StringToken(protocolBBCP));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_dir1));
//              gfc.destination.setToken(remote_host3);
//              gfc.destinationFile.setToken(new StringToken(remote_dir2));
//              gfc.recursive.setToken(new BooleanToken(true));
//              // set expert mode
//              setExpertMode();
//              // set command line options
//              gfc.cmdOptions.setToken(new StringToken("-c"));
//              gfc.fire();
//              log.info("testValidCmdOptionsR2R: exitcode= " + exitCode
//                  + " error= " + error);
//              assertTrue("Error: " + error, error.equals(""));
//              assertEquals(0, exitCode);
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//            
//            exitCode = -999;
//            error = "Initialized value";
//            
//            try {
//                gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                gfc.source.setToken(new StringToken(remote_host1));
//                gfc.sourceFile.setToken(new StringToken(remote_dir1));
//                gfc.destination.setToken(remote_host3);
//                gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                gfc.recursive.setToken(new BooleanToken(true));
//                // set expert mode
//                setExpertMode();
//                // set command line options
//                gfc.cmdOptions.setToken(new StringToken("-autoclean"));
//                gfc.fire();
//                log.info("testValidCmdOptionsR2R: exitcode= " + exitCode
//                    + " error= " + error);
//                assertTrue("Error: " + error, error.equals(""));
//                assertEquals(0, exitCode);
//              } catch (IllegalActionException e) {
//                fail("Exception " + e);
//              }
//      }
//    
//    
//TODO     
//  //******************************************************************//
//    //******************************************************************//
//    //////////////////////   Error Scenarios  ////////////////////////////
//    // ****************************************************************//
//    //****************************************************************//
//
//    /**
//     * Test for Local to Remote InValid Command Options
//     * For Protocol SCP, BBCP & SRMLite
//     */
//    
//    public final void testInvalidCmdOptionsL2R() {
//        try {
//          gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken("localhost"));
//          gfc.sourceFile.setToken(new StringToken(local_dir_source));
//          gfc.destination.setToken(remote_host1);
//          gfc.destinationFile.setToken(new StringToken(remote_dir_source));
//          gfc.recursive.setToken(new BooleanToken(true));
//          // set expert mode
//          setExpertMode();
//          // set command line options
//          gfc.cmdOptions.setToken(new StringToken("-j "));
//          gfc.fire();
//          log.info("testInvalidCmdOptionsL2R: exitcode= " + exitCode
//              + " error= " + error);
//          if (error.equals("")) {
//            fail("No error message returned for invalid command line arguments");
//          }
//          assertNotSame(0, exitCode);
//
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//            gfc.protocol.setToken(new StringToken(protocolBBCP));
//            gfc.source.setToken(new StringToken("localhost"));
//            gfc.sourceFile.setToken(new StringToken(local_dir_source));
//            gfc.destination.setToken(remote_host1);
//            gfc.destinationFile.setToken(new StringToken(remote_dir_source));
//            gfc.recursive.setToken(new BooleanToken(true));
//            // set expert mode
//            setExpertMode();
//            // set command line options
//            gfc.cmdOptions.setToken(new StringToken("-j "));
//            gfc.fire();
//            log.info("testInvalidCmdOptionsL2R: exitcode= " + exitCode
//                + " error= " + error);
//            if (error.equals("")) {
//              fail("No error message returned for invalid command line arguments");
//            }
//            assertNotSame(0, exitCode);
//
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//              gfc.protocol.setToken(new StringToken(protocolSRMLite));
//              gfc.source.setToken(new StringToken("localhost"));
//              gfc.sourceFile.setToken(new StringToken(local_dir_source));
//              gfc.destination.setToken(remote_host1);
//              gfc.destinationFile.setToken(new StringToken(remote_dir_source));
//              gfc.recursive.setToken(new BooleanToken(true));
//              // set expert mode
//              setExpertMode();
//              // set command line options
//              gfc.cmdOptions.setToken(new StringToken("-j "));
//              gfc.fire();
//              log.info("testInvalidCmdOptionsL2R: exitcode= " + exitCode
//                  + " error= " + error);
//              if (error.equals("")) {
//                fail("No error message returned for invalid command line arguments");
//              }
//              assertNotSame(0, exitCode);
//
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//    }
//    
//    
//    /**
//     * Test for Remote to Local InValid Command Options
//     * For Protocol SCP, BBCP & SRMLite
//     */
//    
//
//    public final void testInvalidCmdOptionsR2L() {
//      try {
//    	  gfc.protocol.setToken(new StringToken(protocolSCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//          gfc.destination.setToken("");
//          gfc.destinationFile.setToken(new StringToken(local_dir_source));
//          gfc.recursive.setToken(new BooleanToken(true));
//          // set expert mode
//          setExpertMode();
//          // set command line options
//          gfc.cmdOptions.setToken(new StringToken("-j "));
//          gfc.fire();
//          log.info("testInvalidCmdOptionsR2L: exitcode= " + exitCode
//              + " error= " + error);
//          if (error.equals("")) {
//            fail("No error message returned for invalid command line arguments");
//          }
//          assertNotSame(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//        	gfc.protocol.setToken(new StringToken(protocolBBCP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//            gfc.destination.setToken("");
//            gfc.destinationFile.setToken(new StringToken(local_dir_source));
//            gfc.recursive.setToken(new BooleanToken(true));
//            // set expert mode
//            setExpertMode();
//            // set command line options
//            gfc.cmdOptions.setToken(new StringToken("-j "));
//            gfc.fire();
//            log.info("testInvalidCmdOptionsR2L: exitcode= " + exitCode
//                + " error= " + error);
//            if (error.equals("")) {
//              fail("No error message returned for invalid command line arguments");
//            }
//            assertNotSame(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//        	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//              gfc.destination.setToken("");
//              gfc.destinationFile.setToken(new StringToken(local_dir_source));
//              gfc.recursive.setToken(new BooleanToken(true));
//              // set expert mode
//              setExpertMode();
//              // set command line options
//              gfc.cmdOptions.setToken(new StringToken("-j "));
//              gfc.fire();
//              log.info("testInvalidCmdOptionsR2L: exitcode= " + exitCode
//                  + " error= " + error);
//              if (error.equals("")) {
//                fail("No error message returned for invalid command line arguments");
//              }
//              assertNotSame(0, exitCode);
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//    }
//    
//    /**
//     * Test for Remote to Remote InValid Command Options
//     * For Protocol SCP, BBCP & SRMLite
//     */
//
//    public final void testInvalidCmdOptionsR2R() {
//       try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//        gfc.destination.setToken(remote_host3);
//        gfc.destinationFile.setToken(new StringToken(remote_dir_source));
//        gfc.recursive.setToken(new BooleanToken(true));
//        // set expert mode
//        setExpertMode();
//        // set command line options
//        gfc.cmdOptions.setToken(new StringToken("-j "));
//        gfc.fire();
//        log.info("testInvalidCmdOptionsR2R: exitcode= " + exitCode
//            + " error= " + error);
//        if (error.equals("")) {
//          fail("No error message returned for invalid command line arguments");
//        }
//        assertNotSame(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//      	gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//          gfc.destination.setToken(remote_host3);
//          gfc.destinationFile.setToken(new StringToken(remote_dir_source));
//          gfc.recursive.setToken(new BooleanToken(true));
//          // set expert mode
//          setExpertMode();
//          // set command line options
//          gfc.cmdOptions.setToken(new StringToken("-j "));
//          gfc.fire();
//          log.info("testInvalidCmdOptionsR2R: exitcode= " + exitCode
//              + " error= " + error);
//          if (error.equals("")) {
//            fail("No error message returned for invalid command line arguments");
//          }
//          assertNotSame(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//        	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir_source));
//            gfc.destination.setToken(remote_host3);
//            gfc.destinationFile.setToken(new StringToken(remote_dir_source));
//            gfc.recursive.setToken(new BooleanToken(true));
//            // set expert mode
//            setExpertMode();
//            // set command line options
//            gfc.cmdOptions.setToken(new StringToken("-j "));
//            gfc.fire();
//            log.info("testInvalidCmdOptionsR2R: exitcode= " + exitCode
//                + " error= " + error);
//            if (error.equals("")) {
//              fail("No error message returned for invalid command line arguments");
//            }
//            assertNotSame(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//    }

//
//    /*
//     * Destination is not a directory but it should be for a non recursive copy
//     */
//    
//    /**
//     * Test for Local to Remote Non- Directory Destination
//     * For Protocol SCP, SFTP, BBCP & SRMLite
//     */
//    public final void testNonDirDestL2R() {
//      try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken("localhost"));
//        gfc.sourceFile.setToken(new StringToken(local_dir1));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_file1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testNonDirDestL2R: exitcode= " + exitCode
//            + " error= " + error);
//        if (error.equals("")) {
//          fail("No error message returned");
//        }
//        assertNotSame(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//      	gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken("localhost"));
//          gfc.sourceFile.setToken(new StringToken(local_dir1));
//          gfc.destination.setToken(remote_host1);
//          gfc.destinationFile.setToken(new StringToken(remote_file1));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testNonDirDestL2R: exitcode= " + exitCode
//              + " error= " + error);
//          if (error.equals("")) {
//            fail("No error message returned");
//          }
//          assertNotSame(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//        	gfc.protocol.setToken(new StringToken(protocolBBCP));
//            gfc.source.setToken(new StringToken("localhost"));
//            gfc.sourceFile.setToken(new StringToken(local_dir1));
//            gfc.destination.setToken(remote_host1);
//            gfc.destinationFile.setToken(new StringToken(remote_file1));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//            log.info("testNonDirDestL2R: exitcode= " + exitCode
//                + " error= " + error);
//            if (error.equals("")) {
//              fail("No error message returned");
//            }
//            assertNotSame(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//          	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//              gfc.source.setToken(new StringToken("localhost"));
//              gfc.sourceFile.setToken(new StringToken(local_dir1));
//              gfc.destination.setToken(remote_host1);
//              gfc.destinationFile.setToken(new StringToken(remote_file1));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//              log.info("testNonDirDestL2R: exitcode= " + exitCode
//                  + " error= " + error);
//              if (error.equals("")) {
//                fail("No error message returned");
//              }
//              assertNotSame(0, exitCode);
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//    }
//
//    /*
//     * Destination is not a directory but it should be for a recursive copy
//     */
//    
//    /**
//     * Test for Remote to Local Non- Directory Destination
//     * For Protocol SCP, SFTP, BBCP & SRMLite
//     */
//    
//    public final void testNonDirDestR2L() {
//      try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_dir1));
//        gfc.destination.setToken("");
//        gfc.destinationFile.setToken(new StringToken(local_file1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//
//        log.info("testNonDirDestR2L: exitcode= " + exitCode
//            + " error= " + error);
//        if (error.equals("")) {
//          fail("No error message returned");
//        }
//        assertNotSame(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//      	gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir1));
//          gfc.destination.setToken("");
//          gfc.destinationFile.setToken(new StringToken(local_file1));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//
//          log.info("testNonDirDestR2L: exitcode= " + exitCode
//              + " error= " + error);
//          if (error.equals("")) {
//            fail("No error message returned");
//          }
//          assertNotSame(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//        	gfc.protocol.setToken(new StringToken(protocolBBCP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir1));
//            gfc.destination.setToken("");
//            gfc.destinationFile.setToken(new StringToken(local_file1));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//
//            log.info("testNonDirDestR2L: exitcode= " + exitCode
//                + " error= " + error);
//            if (error.equals("")) {
//              fail("No error message returned");
//            }
//            assertNotSame(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//          	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_dir1));
//              gfc.destination.setToken("");
//              gfc.destinationFile.setToken(new StringToken(local_file1));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//
//              log.info("testNonDirDestR2L: exitcode= " + exitCode
//                  + " error= " + error);
//              if (error.equals("")) {
//                fail("No error message returned");
//              }
//              assertNotSame(0, exitCode);
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//    }
//
//    /*
//     * Destination is not a directory but it should be for a recursive copy
//     */
//    
//    /**
//     * Test for Remote to Remote Non- Directory Destination
//     * For Protocol SCP, SFTP, BBCP & SRMLite
//     */
//    public final void testNonDirDestR2R() {
//      try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_dir1));
//        gfc.destination.setToken(remote_host3);
//        gfc.destinationFile.setToken(new StringToken(remote_file2));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testNonDirDestR2R: exitcode= " + exitCode
//            + " error= " + error);
//        if (error.equals("")) {
//          fail("No error message returned");
//        }
//        assertNotSame(0, exitCode);
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//      	gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_dir1));
//          gfc.destination.setToken(remote_host3);
//          gfc.destinationFile.setToken(new StringToken(remote_file2));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testNonDirDestR2R: exitcode= " + exitCode
//              + " error= " + error);
//          if (error.equals("")) {
//            fail("No error message returned");
//          }
//          assertNotSame(0, exitCode);
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//        	gfc.protocol.setToken(new StringToken(protocolSFTP));
//            gfc.source.setToken(new StringToken(remote_host1));
//            gfc.sourceFile.setToken(new StringToken(remote_dir1));
//            gfc.destination.setToken(remote_host3);
//            gfc.destinationFile.setToken(new StringToken(remote_file2));
//            gfc.recursive.setToken(new BooleanToken(true));
//            gfc.fire();
//            log.info("testNonDirDestR2R: exitcode= " + exitCode
//                + " error= " + error);
//            if (error.equals("")) {
//              fail("No error message returned");
//            }
//            assertNotSame(0, exitCode);
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//          	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_dir1));
//              gfc.destination.setToken(remote_host3);
//              gfc.destinationFile.setToken(new StringToken(remote_file2));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//              log.info("testNonDirDestR2R: exitcode= " + exitCode
//                  + " error= " + error);
//              if (error.equals("")) {
//                fail("No error message returned");
//              }
//              assertNotSame(0, exitCode);
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//    }
//
//    /*
//     * Source is not a directory but it should be for a recursive copy
//     */
//    /**
//     * Test for Local to Remote Non- Directory Source
//     * For Protocol SCP, SFTP, BBCP & SRMLite
//     */
//    public final void testNonDirSrcL2R() {
//      try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken("localhost"));
//        gfc.sourceFile.setToken(new StringToken(local_file1));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_dir1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//        log.info("testNonDirSrcL2R: exitcode= " + exitCode + " error= "
//            + error);
//        assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//      	  gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken("localhost"));
//          gfc.sourceFile.setToken(new StringToken(local_file1));
//          gfc.destination.setToken(remote_host1);
//          gfc.destinationFile.setToken(new StringToken(remote_dir1));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//          log.info("testNonDirSrcL2R: exitcode= " + exitCode + " error= "
//              + error);
//          assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//          fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//          	  gfc.protocol.setToken(new StringToken(protocolSFTP));
//              gfc.source.setToken(new StringToken("localhost"));
//              gfc.sourceFile.setToken(new StringToken(local_file1));
//              gfc.destination.setToken(remote_host1);
//              gfc.destinationFile.setToken(new StringToken(remote_dir1));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//              log.info("testNonDirSrcL2R: exitcode= " + exitCode + " error= "
//                  + error);
//              if (error.equals("")) {
//                  fail("No error message returned");
//                }
//                assertNotSame(0, exitCode);
//              } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//              
//              exitCode = -999;
//              error = "Initialized value";
//              
//              try {
//              	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                  gfc.source.setToken(new StringToken("localhost"));
//                  gfc.sourceFile.setToken(new StringToken(local_file1));
//                  gfc.destination.setToken(remote_host1);
//                  gfc.destinationFile.setToken(new StringToken(remote_dir1));
//                  gfc.recursive.setToken(new BooleanToken(true));
//                  gfc.fire();
//                  log.info("testNonDirSrcL2R: exitcode= " + exitCode + " error= "
//                      + error);
//                  if (error.equals("")) {
//                      fail("No error message returned");
//                    }
//                    assertNotSame(0, exitCode);
//                  } catch (IllegalActionException e) {
//                  fail("Exception " + e);
//                }
//    }
//
//    /*
//     * Source is not a directory but it should be for a recursive copy
//     */
//    
//    /**
//     * Test for Remote to Local Non- Directory Source
//     * For Protocol SCP, SFTP, BBCP & SRMLite
//     */
//    
//    public final void testNonDirSrcR2L() {
//      try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_file1));
//        gfc.destination.setToken("");
//        gfc.destinationFile.setToken(new StringToken(local_dir1));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//
//        log.info("testNonDirSrcR2L: exitcode= " + exitCode + " error= "
//            + error);
//        assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//    	  gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_file1));
//          gfc.destination.setToken("");
//          gfc.destinationFile.setToken(new StringToken(local_dir1));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//
//          log.info("testNonDirSrcR2L: exitcode= " + exitCode + " error= "
//              + error);
//          assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//        	  gfc.protocol.setToken(new StringToken(protocolSFTP));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_file1));
//              gfc.destination.setToken("");
//              gfc.destinationFile.setToken(new StringToken(local_dir1));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//
//              log.info("testNonDirSrcR2L: exitcode= " + exitCode + " error= "
//                  + error);
//              if (error.equals("")) {
//                  fail("No error message returned");
//                }
//                assertNotSame(0, exitCode);
//              } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//              
//              exitCode = -999;
//              error = "Initialized value";
//              
//              try {
//            	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                  gfc.source.setToken(new StringToken(remote_host1));
//                  gfc.sourceFile.setToken(new StringToken(remote_file1));
//                  gfc.destination.setToken("");
//                  gfc.destinationFile.setToken(new StringToken(local_dir1));
//                  gfc.recursive.setToken(new BooleanToken(true));
//                  gfc.fire();
//
//                  log.info("testNonDirSrcR2L: exitcode= " + exitCode + " error= "
//                      + error);
//                  if (error.equals("")) {
//                      fail("No error message returned");
//                    }
//                    assertNotSame(0, exitCode);
//                  } catch (IllegalActionException e) {
//                  fail("Exception " + e);
//                }
//    }
//
//    /*
//     * Source is not a directory but it should be for a recursive copy
//     */
//    /**
//     * Test for Remote to Remote Non- Directory Source
//     * For Protocol SCP, SFTP, BBCP & SRMLite
//     */
//    public final void testNonDirSrcR2R() {
//      try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken(remote_host1));
//        gfc.sourceFile.setToken(new StringToken(remote_file1));
//        gfc.destination.setToken(remote_host3);
//        gfc.destinationFile.setToken(new StringToken(remote_dir2));
//        gfc.recursive.setToken(new BooleanToken(true));
//        gfc.fire();
//
//        log.info("testNonDirSrcR2R: exitcode= " + exitCode + " error= "
//            + error);
//        assertEquals(0, exitCode);
//        } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//      	  gfc.protocol.setToken(new StringToken(protocolBBCP));
//          gfc.source.setToken(new StringToken(remote_host1));
//          gfc.sourceFile.setToken(new StringToken(remote_file1));
//          gfc.destination.setToken(remote_host3);
//          gfc.destinationFile.setToken(new StringToken(remote_dir2));
//          gfc.recursive.setToken(new BooleanToken(true));
//          gfc.fire();
//
//          log.info("testNonDirSrcR2R: exitcode= " + exitCode + " error= "
//              + error);
//          assertEquals(0, exitCode);
//          } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//          	  gfc.protocol.setToken(new StringToken(protocolSFTP));
//              gfc.source.setToken(new StringToken(remote_host1));
//              gfc.sourceFile.setToken(new StringToken(remote_file1));
//              gfc.destination.setToken(remote_host3);
//              gfc.destinationFile.setToken(new StringToken(remote_dir2));
//              gfc.recursive.setToken(new BooleanToken(true));
//              gfc.fire();
//
//              log.info("testNonDirSrcR2R: exitcode= " + exitCode + " error= "
//                  + error);
//              if (error.equals("")) {
//                  fail("No error message returned");
//                }
//                assertNotSame(0, exitCode);
//              } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//              
//              exitCode = -999;
//              error = "Initialized value";
//              
//              try {
//              	  gfc.protocol.setToken(new StringToken(protocolSRMLite));
//                  gfc.source.setToken(new StringToken(remote_host1));
//                  gfc.sourceFile.setToken(new StringToken(remote_file1));
//                  gfc.destination.setToken(remote_host3);
//                  gfc.destinationFile.setToken(new StringToken(remote_dir2));
//                  gfc.recursive.setToken(new BooleanToken(true));
//                  gfc.fire();
//
//                  log.info("testNonDirSrcR2R: exitcode= " + exitCode + " error= "
//                      + error);
//                  if (error.equals("")) {
//                      fail("No error message returned");
//                    }
//                    assertNotSame(0, exitCode);
//                  } catch (IllegalActionException e) {
//                  fail("Exception " + e);
//                }
//    }
//
//    /*
//     * Source is not a regular file but it should be for a non recursive copy
//     */
//    /**
//     * Test for Local to Remote Non- File Source
//     * For Protocol SCP, SFTP, BBCP & SRMLite
//     */
//    public final void testNonFileSrcL2R() {
//      try {
//    	gfc.protocol.setToken(new StringToken(protocolSCP));
//        gfc.source.setToken(new StringToken("localhost"));
//        gfc.sourceFile.setToken(new StringToken(local_dir1));
//        gfc.destination.setToken(remote_host1);
//        gfc.destinationFile.setToken(new StringToken(remote_file1));
//        gfc.fire();
//
//        log.info("testNonFileSrcL2R: exitcode= " + exitCode
//            + " error= " + error);
//        if (error.equals("")) {
//          fail("No error message returned");
//        }
//        assertNotSame(0, exitCode);
//
//      } catch (IllegalActionException e) {
//        fail("Exception " + e);
//      }
//      
//      exitCode = -999;
//      error = "Initialized value";
//      
//      try {
//      	gfc.protocol.setToken(new StringToken(protocolSFTP));
//          gfc.source.setToken(new StringToken("localhost"));
//          gfc.sourceFile.setToken(new StringToken(local_dir1));
//          gfc.destination.setToken(remote_host1);
//          gfc.destinationFile.setToken(new StringToken(remote_file1));
//          gfc.fire();
//
//          log.info("testNonFileSrcL2R: exitcode= " + exitCode
//              + " error= " + error);
//          if (error.equals("")) {
//            fail("No error message returned");
//          }
//          assertNotSame(0, exitCode);
//
//        } catch (IllegalActionException e) {
//          fail("Exception " + e);
//        }
//        
//        exitCode = -999;
//        error = "Initialized value";
//        
//        try {
//        	gfc.protocol.setToken(new StringToken(protocolBBCP));
//            gfc.source.setToken(new StringToken("localhost"));
//            gfc.sourceFile.setToken(new StringToken(local_dir1));
//            gfc.destination.setToken(remote_host1);
//            gfc.destinationFile.setToken(new StringToken(remote_file1));
//            gfc.fire();
//
//            log.info("testNonFileSrcL2R: exitcode= " + exitCode
//                + " error= " + error);
//            if (error.equals("")) {
//              fail("No error message returned");
//            }
//            assertNotSame(0, exitCode);
//
//          } catch (IllegalActionException e) {
//            fail("Exception " + e);
//          }
//          
//          exitCode = -999;
//          error = "Initialized value";
//          
//          try {
//          	gfc.protocol.setToken(new StringToken(protocolSRMLite));
//              gfc.source.setToken(new StringToken("localhost"));
//              gfc.sourceFile.setToken(new StringToken(local_dir1));
//              gfc.destination.setToken(remote_host1);
//              gfc.destinationFile.setToken(new StringToken(remote_file1));
//              gfc.fire();
//
//              log.info("testNonFileSrcL2R: exitcode= " + exitCode
//                  + " error= " + error);
//              if (error.equals("")) {
//                fail("No error message returned");
//              }
//              assertNotSame(0, exitCode);
//
//            } catch (IllegalActionException e) {
//              fail("Exception " + e);
//            }
//    }
//
//    
//            
            
/**
 * Set expert mode to true
 */
private void setExpertMode() {

  StringBuffer moml = new StringBuffer();
  if (gfc.getAttribute("_expertMode") == null) {
    moml.append("<property name=\"_expertMode\" "
        + "class=\"ptolemy.kernel.util.SingletonAttribute\"></property>");
    MoMLChangeRequest request = new MoMLChangeRequest(this, // originator
        gfc, // context
        moml.toString(), // MoML code
        null);
    gfc.requestChange(request);
  }

}

private void removeExpertMode(){
  StringBuffer moml = new StringBuffer();
  if (gfc.getAttribute("_expertMode") != null) {
	        moml.append("<deleteProperty name=\"_expertMode\"/>");
	        MoMLChangeRequest request = new MoMLChangeRequest(this, // originator
	          gfc, // context
	          moml.toString(), // MoML code
	          null);
	        gfc.requestChange(request);
    }
}
  
  
}
