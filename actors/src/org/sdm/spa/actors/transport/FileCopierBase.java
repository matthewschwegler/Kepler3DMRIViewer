/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-09 11:05:40 -0700 (Wed, 09 May 2012) $' 
 * '$Revision: 29823 $'
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

package org.sdm.spa.actors.transport;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.LocalExec;
import org.kepler.ssh.SshExec;
import org.sdm.spa.actors.transport.vo.ConnectionDetails;

import ptolemy.kernel.util.IllegalActionException;

/**
 * This class provides functionality to copy files between two machine using
 * SSH. Remote machines should be accessible using SSH to be able to copy files.
 * This is a base class that can be extended by Protocol specific class that  
 * generate different commands to perform the copy operation. 
 * <p>
 * Whether the copy operation would by default overwrite existing files depends
 * on the actual protocol used for file copy. Optional command line options may
 * be used to override the default behavior in some cases depending on the 
 * protocol used. Refer to the documentation of specific sub class for details.
 * <p>
 * When both source and destination machines are local hosts, the protocol 
 * specified is ignored and file copy is done using Java. 
 * <p>
 * When both source and destination are the same machine a simple cp command is
 * used instead of any specific protocol
 * @author Chandrika Sivaramakrishnan
 *
 */
public abstract class FileCopierBase {
  /////////////////Private Variables///////////////////////
  private static final Log log = LogFactory.getLog(FileCopierBase.class.getName());
  private static final boolean isDebugging = log.isDebugEnabled();
  
  /////////////////Protected variables ///////////////////
  protected boolean forcedCleanup = false;
  protected int timeout = 0;
  protected String cmdLineOptions = "";
  protected String protocolPathSrc = "";
  protected String protocolPathDest = "";

  //////////////////Protected Methods//////////////////
  /**
   * Copies files to destination path on the same machine.  
   * @param srcFile - source file to be copied
   * @param destFile - local path into which source should be copied 
   * @param recursive - flag to indicate if directories should be copied recursively
   * @return CopyResult object containing the exit code and error message if any.
   * exit code 0 represents successful file transfer.
   */
  protected CopyResult copyLocal(String sourceFile, String destinationFile,
      boolean recursive){
    try {
      int count = 0;
      LocalExec exObject = new LocalExec();
      File file = new File(sourceFile);
      count = exObject.copyTo(file, destinationFile, recursive);
      if(count>0){
        return new CopyResult();
      }else{
        return new CopyResult(1,"No files where copied","");
      }
    }catch(ExecException e){
        return new CopyResult(1,e.getMessage(),"");
    }
  }
  
  /**
   * Connects to the remote machines and execute a 'cp' command to copy files
   * to destination dir on the same remote machine
   * @param srcFile - source file to be copied
   * @param destFile - local path into which source should be copied 
   * @param recursive - flag to indicate if directories should be copied recursively
   * @return CopyResult object containing the exit code and error message if any.
   * exit code 0 represents successful file transfer.
   */
  protected CopyResult copyLocalOnRemoteMachine(ConnectionDetails srcDetails,
			String srcFile, ConnectionDetails destDetails, String destFile,
			boolean recursive) throws ExecException {
	    
	    ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
	    ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();
	    // Conenct to source by ssh
	    SshExec sshObject = new SshExec(srcDetails.getUser(), srcDetails.getHost(),
	        srcDetails.getPort());
	    sshObject.setTimeout(timeout, false, false);
	    sshObject.setForcedCleanUp(forcedCleanup);
	    
	    try
	    {
	      //no pseudo terminal is required as password is not required for cp
	      sshObject.setPseudoTerminal(false);
	      StringBuffer cmd = new StringBuffer(100);
	      int exitCode = 0;
	  
	      cmd.append("cp ");
	      if (recursive) {
	        cmd.append(" -r ");
	      } else {
	        cmd.append("  ");
	      }
	      cmd.append(srcFile);
	      cmd.append("  ");
	      cmd.append(destFile);
	  
	      if (isDebugging)
	        log.debug("remote copy cmd=" + cmd);
	      exitCode = sshObject.executeCmd(cmd.toString(), cmdStdout, cmdStderr,
	          destDetails.toString());
	      log.debug("ExitCode:"+ exitCode);
	      log.debug("stdout:"+cmdStdout);
	      log.debug("stderr:"+cmdStderr);
	      
	      String message = cmdStderr.toString();
	      if(message==null || message.trim().equals("")){
	          message = cmdStdout.toString();
	      }
	      return new CopyResult(exitCode,message,null);
	    }catch(Exception e){
	      return new CopyResult(1, e.getMessage(),null);
	    }
  }

  /**
   * Copies file from a remote host to local machine
   * 
   * @param srcDetails - object containing the source machine connection details
   * @param srcFile - source file to be copied
   * @param destFile - local path into which source should be copied 
   * @param recursive - flag to indicate if directories should be copied recursively
   * @return CopyResult object containing the exit code and error message if any.
   * exit code 0 represents successful file transfer.
   * @throws ExecException
   */
  protected abstract CopyResult copyFrom(ConnectionDetails srcDetails, String srcFile, 
		  String destFile, boolean recursive) throws ExecException;

  /**
   * Copies files from local machine to a remote host
   * 
   * @param srcFile - local source file to be copied
   * @param destDetails - object containing the destination machine connection details
   * @param destFile - path into which source should be copied 
   * @param recursive - flag to indicate if directories should be copied recursively
   * @return CopyResult object containing the exit code and error message if any.
   * exit code 0 represents successful file transfer.
   * @throws ExecException
   */
  protected abstract CopyResult copyTo(String srcFile, ConnectionDetails destDetails,
      String destFile, boolean recursive) throws ExecException;

  /**
   * Copies files between two remote machines. 
   * 
   * @param srcDetails - object containing the source machine connection details
   * @param srcFile - source file to be copied
   * @param destDetails - object containing the destination machine connection details
   * @param destFile - path into which source should be copied 
   * @param recursive - flag to indicate if directory should be copied recursively
   * @return CopyResult object containing the exit code and error message if any.
   * exit code 0 represents successful file transfer.
   * @throws ExecException
   */
  protected abstract CopyResult copyRemote(ConnectionDetails srcDetails,
      String srcFile, ConnectionDetails destDetails, String destFile,
      boolean recursive) throws ExecException;

  /**
   * Generic copy method that does the initial input validation and calls the
   * copyTo, copyFrom or copyRemote of the appropriate FileCopier subclass.  
   * Subclasses of FileCopier implement these methods based on the protocol that
   * it uses for file copy. If both source and destination are local host, 
   * ignores the protocol specified by the user and copies file using java
   * <p> 
   * @param srcDetails - ConnectionDetails object with source machine details
   * @param srcFile - File to be copied
   * @param destDetails - ConnectionDetails object with destination machine details
   * @param destFile - Destination file or directory
   * @param recursive - whether directory should be copied recursively
   * @return exitCode
   * @throws IllegalActionException
   * @throws ExecException
   */
  protected CopyResult copy(ConnectionDetails srcDetails, String srcFile,
      ConnectionDetails destDetails, String destFile, boolean recursive)
      throws IllegalActionException, ExecException {

    if (srcDetails.isLocal()) {
      srcFile = handleRelativePath(srcFile);
    }

    if (destDetails.isLocal()) {
      destFile = handleRelativePath(destFile);
    }

    if (srcDetails.getPort() == -1) {
      srcDetails.setPort(getDefaultPort());
    }
    if (destDetails.getPort() == -1) {
      destDetails.setPort(getDefaultPort());
    }

    if (isDebugging) {
      log.debug("Source= " + srcDetails);
      log.debug("Destination= " + destDetails);
    }

    //Both source and destination are local hosts
    if (srcDetails.isLocal() && destDetails.isLocal()) {
      return copyLocal(srcFile, destFile, recursive);
    }
    
    //Either is a local host
    if (srcDetails.isLocal()) {
      // copy to remote destination
      return copyTo(srcFile, destDetails, destFile, recursive);
    } else if (destDetails.isLocal()) {
      return copyFrom(srcDetails, srcFile, destFile, recursive);
    }
    
    //Check if both the src and destination remote machines are same
    if(srcDetails.getHost().equals(destDetails.getHost())){
    	return copyLocalOnRemoteMachine(srcDetails, srcFile, destDetails, destFile, recursive);
    }
    return copyRemote(srcDetails, srcFile, destDetails, destFile, recursive);
  }

  

/**
   * This is used to set the users PATH variable, if the user has not specified 
   * the path where the protocol is installed. In such cases the 
   * program will search for it in a default list of path. 
   * @param cmd - command to execute for file copy 
   * @return original command prefixed with command to set PATH variable. 
   */
  protected String getCmdWithDefaultPath(StringBuffer cmd) {
    StringBuffer cmdWithPath = new StringBuffer(100);
    cmdWithPath
        .append("bash -c 'export PATH=/usr/bin:/bin:/usr/local/bin:~:.:$PATH; ");
    cmdWithPath.append(cmd);
    cmdWithPath.append("'");
    return cmdWithPath.toString();
  }

  /**
   * default port for the file transfer protocol. Child class should either
   * return a specific port number or -1 if it doesn't want to enforce a 
   * specific port number
   *    */
  protected abstract int getDefaultPort();
  
  //////////////////Private methods ///////////////////////////////////
  
private String handleRelativePath(String localfile){
	String fileWithPath = localfile.trim();
	
	String userhome = System.getProperty("user.home");
	//Work around for java 1.6 bug. Java doesn't return the correct
	//user home directory on vista or windows 7. 
	//http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6519127
	if ( System.getProperty("os.name").toLowerCase().indexOf("win") >= 0 ) {
		userhome = System.getenv().get("HOMEPATH");
	} 
	
	if (localfile.contains(",")) {
		log.debug("***** Detected file list *******");
		String[] srcFile_list = localfile.split(",");
		StringBuffer newlist = new StringBuffer();
		for (int i = 0; i < srcFile_list.length; i++) {
			srcFile_list[i] = srcFile_list[i].trim();
			// Anand: The first predicate applies to Windows; the
			// selected offset assumes that
			// drive identifiers will be 1 character long.
			if (!(srcFile_list[i].startsWith(":\\", 1) || 
					srcFile_list[i].startsWith(":/", 1) || 
					srcFile_list[i].startsWith("/"))) {
				 newlist.append(userhome);
				 newlist.append(File.separatorChar);
			}
			newlist.append(srcFile_list[i]);
			newlist.append(",");
		}
		fileWithPath = newlist.toString();
		if(fileWithPath.endsWith(",")){
			fileWithPath = fileWithPath.substring(0, fileWithPath.length() -1);
		}
		
	} else {
		// single file 
		// The first predicate applies to Windows; the selected offset
		// assumes that
		// drive identifiers will be 1 character long.
		// We can access file using relative path
		if (!(fileWithPath.startsWith(":\\", 1) 
				|| fileWithPath.startsWith(":/", 1) 
				|| fileWithPath.startsWith("/"))) {
			fileWithPath = userhome + File.separatorChar + fileWithPath;
		}
	}

	log.debug("From handleRelativePath - Returning "+fileWithPath);
	return fileWithPath;
}
  
  /////////////////Public getters and setters/////////////////////////
  public boolean isForcedCleanup() {
    return forcedCleanup;
  }

  public void setCleanup(boolean cleanup) {
    this.forcedCleanup = cleanup;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public String getCmdLineOptions() {
    return cmdLineOptions;
  }

  public void setCmdLineOptions(String cmdLineOptions) {
    if (cmdLineOptions == null) {
      this.cmdLineOptions = "";
    } else {
      this.cmdLineOptions = cmdLineOptions.trim();
    }
  }

  public String getProtocolPathSrc() {
    return protocolPathSrc;
  }

  public void setProtocolPathSrc(String protocolPathSrc) {
    if (protocolPathSrc == null || protocolPathSrc.trim().equals("")) {
      this.protocolPathSrc = "";
    } else {
      protocolPathSrc = protocolPathSrc.trim();
      String seperator = "/";
      if (protocolPathSrc.contains("\\")) {
        seperator = "\\";
      }
      if (protocolPathSrc.endsWith(seperator)) {
        this.protocolPathSrc = protocolPathSrc;
      } else {
        this.protocolPathSrc = protocolPathSrc + seperator;
      }
    }
  }

  public String getProtocolPathDest() {
    return protocolPathDest;
  }

  public void setProtocolPathDest(String protocolPathDest) {
    if (protocolPathDest == null || protocolPathDest.trim().equals("")) {
      this.protocolPathDest = "";
    } else {
      String seperator = "/";
      if (protocolPathDest.contains("\\")) {
        seperator = "\\";
      }
      if (protocolPathDest.endsWith(seperator)) {
        this.protocolPathDest = protocolPathDest;
      } else {
        this.protocolPathDest = protocolPathDest + seperator;
      }
    }
  }
  
  //Inner class 
  /**Object that contains the exit code and (error) message associated with the
   *copy operation. Expects a exit code of 0 to denote successful file transfer.
   *If exit code is zero, the error message is set to empty string
   */
  public class CopyResult {
    private int exitCode;
    private String errorMsg;
    private String warningMsg;
     
    /**
     * Default constructor. Represents a successful file transfer. 
     * Defaults exit code to zero and message to empty string 
     */
    public CopyResult(){
      exitCode = 0;
      errorMsg = "";
      warningMsg = "";
    }
   
    public CopyResult(int exitCode, String message, String warningMessage){
      this.exitCode = exitCode;
      if(warningMessage == null){
    	  warningMessage = "";
      }
      if(exitCode==0){
        //operation successful
        this.errorMsg ="";
        this.warningMsg = warningMessage;
      }else{
        this.errorMsg = message;
        this.warningMsg = warningMessage;
      }
    }
    
    @Override
    public String toString(){
      return exitCode+":"+errorMsg + ", warnings:" + warningMsg;
    }

    public int getExitCode() {
      return exitCode;
    }

    public void setExitCode(int exitCode) {
      this.exitCode = exitCode;
    }

    public String getErrorMsg() {
      return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
    }

    public String getWarningMsg() {
      return warningMsg;
    }

    public void setWarningMsg(String warningMsg) {
      this.warningMsg = warningMsg;
    }
  }

}
