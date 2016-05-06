/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: brooks $'
 * '$Date: 2012-08-06 15:21:30 -0700 (Mon, 06 Aug 2012) $' 
 * '$Revision: 30352 $'
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
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.DOMParser;
import org.kepler.ssh.ExecException;
import org.sdm.spa.actors.transport.vo.ConnectionDetails;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

/**
 * Copy files between local and remote machine or between two remote machines,
 * using scp, sftp, bbcp or srmlite protocol. The actor uses the SSH protocol to 
 * connect to remote hosts. If the	host is an empty string or "local" or 
 * "localhost", the Java Runtime will be used for execution instead of SSH. 
 * Features such as overwriting of existing files,errors during partial copy of 
 * directories, error codes, error messages etc. depend on the specific protocol
 * selected. User may overwrite some of the default settings by specifying 
 * additional command line options
 * <p>
 * For copying files between two remote machines, connects to the source remote
 * host using Ssh protocol and executes the command on a terminal. The actor
 * org.kepler.ssh.SshExec is used for this
 * </p>
 * <p>
 * This actor uses the org.kepler.ssh package to have long lasting connections.
 * </p>
 * <p>
 * If the <i>timeoutSeconds</i> is set greater than zero, the command will be
 * timed out after the specified amount of time (in seconds).
 * </p>
 * <p>
 * In case there is an ssh connection related error (or timeout) the <i>exitcode</i>
 * will be -32767, <i>errors</i> will contain the error message, <i>stdout</i>
 * and <i>stderr</i> will be empty string.
 * </p>
 * <p>
 * To ensure fixed rate of token production for SDF, the actor emits an empty
 * string on <i>errors</i> if the command is executed without ssh related
 * errors.
 * </p>
 * <p>
 * If <i>cleanupAfterError</i> is set, the remote process and its children will
 * be killed (provided, we have the connection still alive). Very useful in case
 * of timeout because that leaves remote processes running. Use only when
 * connecting to a unix machine. In case of <i>local</i> or <i>localhost</i>, 
 * this flag is not used.
 * </p>
 * <p>
 * Streaming of output during the command execution is not implemented.
 * </p>
 * 
 * @author Chandrika Sivaramakrishnan, Anand Kulkarni
 * @version $Revision: 30352 $
 * @category.name remote
 * @category.name connection
 * @category.name external execution
 */

// TODO: Check if source and dest machine are same - use cp instead of scp
// Make main parameters as port parameters
@SuppressWarnings("serial")
public class GenericFileCopier extends TypedAtomicActor {

	// /////////////////////////Private Static Variables////////////////////////
	private static final Log log = LogFactory.getLog(GenericFileCopier.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// private static ArrayList<MachineEntry> machineConfigurationsList = new
	// ArrayList<MachineEntry>();
	private static HashMap<String, String> protocolPaths = new HashMap<String, String>();
	private static HashSet<String> hostSet = new HashSet<String>();
	private static HashSet<String> protocolsSet = new HashSet<String>();

	// ////////////////////// Private variables //////////////////////////////
	private ConnectionDetails srcDetails;
	private ConnectionDetails destDetails;

	// ///////////////// Public ports and parameters ///////////////////////

	// /////////////Output ports/////////////////////

	/**
	 * The exit code of the command.
	 */
	public TypedIOPort exitcode;

	/**
	 * The string representation of all the errors that happened during the
	 * execution of the actor, if there are any. In case of remote execution
	 * this would contain the contents of standard output and standard error
	 * from the remote machine. In case of successful file transfer this is set
	 * to empty string
	 */
	public TypedIOPort errors;
	/**
	 * The string representation of all the warnings that happened during the
	 * execution of the actor, if there are any. In case of remote execution
	 * this would contain the contents of standard output and standard warnings
	 * from the remote machine. 
	 */
	public TypedIOPort warnings;

	// /////////////////////////Input ports/parameters/////////////////////

	/**
	 * source machine information in user@host:port format.
	 */
	public PortParameter source;

	/**
	 * source file/directory to be copied.
	 */
	public PortParameter sourceFile;

	/**
	 * destination machine information in user@host:port format.
	 */
	public PortParameter destination;

	/**
	 * destination file/directory to which source should be copied.
	 */
	public PortParameter destinationFile;

	/**
	 * Specifying whether directories can be copied recursively.
	 */
	public Parameter recursive;

	/**
	 * Type of a protocol to be used to do file transfer.
	 */
	public StringParameter protocol;

	// Expert parameters

	/**
	 * Path where the protocol is installed in the source machine
	 */
	public Parameter protocolPathSrc;

	/**
	 * Path where the protocol is installed in the destination machine
	 */
	public Parameter protocolPathDest;

	/**
	 * Additional command line options to be used for the selected protocol.
	 */
	public Parameter cmdOptions;

	/**
	 * Timeout in seconds for the command to be executed. 0 means waiting
	 * indefinitely for command termination.
	 */
	public Parameter timeoutSeconds;

	/**
	 * Enforce killing remote process(es) after an error or timeout. Unix
	 * specific solution is used, therefore you should not set this flag if
	 * connecting to other servers. But it is very useful for unix as timeout
	 * leaves processes living there, and sometimes errors too. All processes
	 * belonging to the same group as the remote command (i.e. its children)
	 * will be killed.
	 */
	public Parameter cleanupAfterError;

	/**
	 * Protocol that srmlite should internally use.
	 */
	public StringParameter srmProtocol;
	
    
	public PortParameter connectFromDest; //Added by Chandrika

	// /////////////////////////////////////////////////////////////////////
	// ///////////////////////Static block ////////////////////////////////
	static {
		// Parse xml file for parameter values
		parseMachineConfigurationsFile();
		log.debug("Loaded protocol paths as :" + protocolPaths);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Construct an FileCopierBetweenRemoteMachines actor with the given
	 * container and name. Create the parameters, initialize their values.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public GenericFileCopier(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		String defaultHostStr = "[user@]host[:port]";
		// sourceTarget selects the machine where to copy from
		source = new PortParameter(this, "source machine");
		// sourceTarget.setTypeEquals(BaseType.STRING);
		source.setToken(new StringToken(defaultHostStr));
		new Parameter(source.getPort(), "_showName", BooleanToken.TRUE);

		// file or directory to be copied
		sourceFile = new PortParameter(this, "source file", new StringToken());
		new Parameter(sourceFile.getPort(), "_showName", BooleanToken.TRUE);

		// destinationTarget selects the machine where to copy to
		destination = new PortParameter(this, "destination machine");
		destination.setToken(new StringToken(defaultHostStr));

		// file or directory to be copied
		destinationFile = new PortParameter(this, "destination file",
				new StringToken());
		new Parameter(destinationFile.getPort(), "_showName", BooleanToken.TRUE);
		
		// recursive parameter
		recursive = new Parameter(this, "recursive", new BooleanToken(false));
		recursive.setTypeEquals(BaseType.BOOLEAN);

		protocol = new StringParameter(this, "protocol");

		protocolPathSrc = new Parameter(this, "protocol path on source");
		protocolPathSrc.setVisibility(Settable.EXPERT);

		protocolPathDest = new Parameter(this, "protocol path on destination");
		protocolPathDest.setVisibility(Settable.EXPERT);

		cmdOptions = new Parameter(this, "command line options");
		cmdOptions.setVisibility(Settable.EXPERT);

		timeoutSeconds = new Parameter(this, "timeoutSeconds", new IntToken(0));
		timeoutSeconds.setTypeEquals(BaseType.INT);

		cleanupAfterError = new Parameter(this, "cleanupAfterError",
				new BooleanToken(false));
		cleanupAfterError.setTypeEquals(BaseType.BOOLEAN);

		srmProtocol = new StringParameter(this, "srm protocol");
		srmProtocol.setVisibility(Settable.EXPERT);
		
		//Added by Chandrika - Starts
		connectFromDest = new PortParameter (this, "connect from destination to source",new BooleanToken(false));
		connectFromDest.setTypeEquals(BaseType.BOOLEAN);
		//Added by Chandrika - Ends
		// Output ports
		exitcode = new TypedIOPort(this, "exitcode", false, true);
		exitcode.setTypeEquals(BaseType.INT);
		new Parameter(exitcode, "_showName", BooleanToken.TRUE);

		errors = new TypedIOPort(this, "errors", false, true);
		errors.setTypeEquals(BaseType.STRING);
		new Parameter(errors, "_showName", BooleanToken.TRUE);
		
		warnings = new TypedIOPort(this, "warnings", false, true);
		warnings.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(warnings, "_showName", BooleanToken.TRUE);

		// initialize parameters
		initalizeParameters();

		// myEditor = new DynamicEditorFactory(this, "_editorFactory");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"75\" height=\"50\" style=\"fill:blue\"/>\n"
				+ "<text x=\"5\" y=\"30\""
				+ "style=\"font-size:14; fill:yellow; font-family:SansSerif\">"
				+ "SshExec</text>\n" + "</svg>\n");

	}

	/**
	 * Send the token in the <i>value</i> parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {

    super.fire();
    
    source.update();
    sourceFile.update();
    sourceFile.update();
    destinationFile.update();
    connectFromDest.update();
    
    // process inputs
    boolean recursiveVal = ((BooleanToken) recursive.getToken()).booleanValue();
    
    boolean connectFromDestVal = ((BooleanToken) connectFromDest.getToken()).booleanValue(); //Added by Chandrika
    FileCopierBase.CopyResult result = null;
    String srcFile = "";
    String destFile = "";
    String strSource = "";
    String strDestination = "";
    
    Token token = source.getToken();
    if(token != null){
    	strSource = ((StringToken) token).stringValue().trim();
    }
    token = destination.getToken();
	if(token != null){ 
		strDestination = ((StringToken) token).stringValue().trim();
	}  
    StringToken sourceFileToken = ((StringToken) sourceFile.getToken());
    StringToken destFileToken = (StringToken) destinationFile.getToken();

    // initialize
    if(sourceFileToken!=null){
      srcFile = sourceFileToken.stringValue().trim();
    }
    if(destFileToken!=null){
      destFile = destFileToken.stringValue().trim();
    }
    log.debug("Src File-"+srcFile);
    log.debug("Dest File-"+destFile);
    
    // Do basic validation
    if (srcFile.equals("")) {
      exitcode.send(0, new IntToken(1));
      errors.send(0, new StringToken("No source file specified to copy"));
      warnings.send(0, new BooleanToken(false));

      return;
    }
    if (destFile.equals("")) {
      exitcode.send(0, new IntToken(1));
      errors.send(0, new StringToken("Please specify a target file/directory"));
      warnings.send(0, new BooleanToken(false));
      return;
    }
    
    // Create instance of copier and pass on all the user inputs
    FileCopierBase copier = createFileCopier(strSource, strDestination);
	//Added by Chandrika - Start
    //set source and destination details
    destDetails.setConnectionOrigin(connectFromDestVal);
    srcDetails.setConnectionOrigin(!connectFromDestVal);
    //Added by Chandrika - Ends
    int exitCode = 0;
    ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
    ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();
    log.debug("Copier -" + copier);

    try {
      
      result = copier.copy(srcDetails, srcFile, destDetails, destFile,
          recursiveVal);

    } catch (ExecException e) {
      String errText = new String("ExecuteCmd error:\n" + e.getMessage());
      exitcode.send(0, new IntToken(-32767));
      errors.send(0, new StringToken(errText));
      warnings.send(0, new BooleanToken(false));
      return;
    }
    exitcode.send(0, new IntToken(result.getExitCode()));
    if (result.getWarningMsg().length() > 0) {
      warnings.send(0, new BooleanToken(true));
      errors.send(0, new StringToken(result.getErrorMsg() + " Warnings: " + result.getWarningMsg()));
    }
    else {
      warnings.send(0, new BooleanToken(false));
      errors.send(0, new StringToken(result.getErrorMsg()));
    }
  } // end-method fire()

	// /////////////////////////////////////////////////////////////////////////
	// ////// Private methods ////////////

	// To parse xml file
	private static void parseMachineConfigurationsFile() {

		String keplerHome = System.getProperty("KEPLER");
		log.info("Kepler Home dir is "+keplerHome);
		File configFile = new File(keplerHome
				+ "/common/configs/ptolemy/configs/spa/machineConfig.xml");
		if (configFile.exists()) {
			log.info("GenericFileCopier:Loding config file: " + configFile);
			try {
				DOMParser parser = new DOMParser();
				org.xml.sax.InputSource iSource = new org.xml.sax.InputSource(
						new FileReader(configFile));
				parser.parse(iSource);
				Document doc = parser.getDocument();

				Element root = doc.getDocumentElement();
				NodeList list = root.getChildNodes();
				int length = list.getLength();
				boolean nonStdPaths = false;
				for (int i = 0; i < length; i++) {
					Node node = list.item(i);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						if ("machineEntry".equals(node.getNodeName())) {
							nonStdPaths = false;
							NamedNodeMap attributes = node.getAttributes();

							// Load into separate static variables
							String user = attributes.getNamedItem("user")
									.getNodeValue();
							String host = attributes.getNamedItem("host")
									.getNodeValue();
							if (user == null || user.trim().equals("")) {
								hostSet.add(host);
							} else {
								hostSet.add(user + "@" + host);
							}
							Node namedItem = attributes
									.getNamedItem("protocolPaths");
							String paths[] = null;
							if (namedItem != null) {
								paths = namedItem.getNodeValue().split("\\|");
								nonStdPaths = true;
							}
							namedItem = attributes
									.getNamedItem("transferProtocols");
							if (namedItem != null) {
								String[] protocols = namedItem.getNodeValue()
										.split("\\|");
								for (int j = 0; j < protocols.length; j++) {
									protocolsSet.add(protocols[j]);
									if (nonStdPaths && j < paths.length
											&& (!paths[j].equals(""))) {
										// if non empty path is specified for
										// this protocol add it Map
										protocolPaths.put(protocols[j] + "@"
												+ host, paths[j]);
									}
								} // added all protocols for this machine/host
							}

						}
					}
				}// end of for loop
			} catch (Exception e) {
				log
						.error(
								"Exception reading machine configuration of GenericFileCopier",
								e);
				System.err
						.println("Error loading machine configuration for GenericFileCopier actor");
				e.printStackTrace(System.err);
			}
		} else {
			log.info("No machine configurations found for Remote FileCopier");
		}

	}

	// To load values into class parameters
	private void initalizeParameters() {

		Iterator<String> it = hostSet.iterator();
		while (it.hasNext()) {
			String item = "\"" + (String) it.next() + "\"";
			source.addChoice(item);
			destination.addChoice(item);
		}

		it = protocolsSet.iterator();
		while (it.hasNext()) {
			String item = (String) it.next();
			protocol.addChoice(item);
		}
		srmProtocol.addChoice("scp");
		srmProtocol.addChoice("sftp");
		srmProtocol.addChoice("gsiftp");
	}

	// Create an instance of copier and set the necessary variables based on
	// the user input
	private FileCopierBase createFileCopier(String src, String dest)
			throws IllegalActionException {

		String strProtocol = ((StringToken) protocol.getToken()).stringValue()
				.trim();
		// Parse connection string
		srcDetails = parseConnectionString(src);
		destDetails = parseConnectionString(dest);
		
		
		if (strProtocol == null || strProtocol.trim().equals("")){
			// if either source or destination is a remote machines default to
			// scp
			if (!srcDetails.isLocal() || !destDetails.isLocal()) {
			//Change by ANUSUA - Starts
				// strProtocol = "scp";//----------Commented by ANUSUA
				ScpCopier scpcopier = new ScpCopier();
				if(scpcopier.isScpPresent(srcDetails,destDetails)){
					strProtocol = "scp";
					log.debug("DEFAULT SCP");
				}else{
				    strProtocol = "sftp";
				    log.debug("DEFAULT SFTP");
				}
			//Change by ANUSUA - Ends
			} else {
				strProtocol = ""; // set it to empty to get a local copier
			}
		}
		
		
		FileCopierBase copier = FileCopierFactory.getFileCopier(strProtocol);
		String strPathSrc;
		String strPathDest;
		Token temp = null;

		copier.setCleanup(((BooleanToken) cleanupAfterError.getToken())
				.booleanValue());
		IntToken token = (IntToken) timeoutSeconds.getToken();
		if (token != null) {
			copier.setTimeout(token.intValue());
		}

		// set default path to protocol based on config file entry
		copier.setProtocolPathSrc(protocolPaths.get(strProtocol + "@"
				+ srcDetails.getHost()));
		copier.setProtocolPathDest(protocolPaths.get(strProtocol + "@"
				+ destDetails.getHost()));

		// If on expert mode pass all the expert options to the copier
		if (this.getAttribute("_expertMode") != null) {

			if (isDebugging) {
				log.debug("Expert mode");
			}
			temp = cmdOptions.getToken();
			if (temp != null) {
				copier.setCmdLineOptions(((StringToken) temp).stringValue());
			}

			temp = protocolPathSrc.getToken();
			if (temp != null) {
				strPathSrc = ((StringToken) temp).stringValue().trim();
				copier.setProtocolPathSrc(strPathSrc);
				log.debug("protocol path src set=" + strPathSrc);
			}

			temp = protocolPathDest.getToken();
			if (temp != null) {
				strPathDest = ((StringToken) temp).stringValue().trim();
				copier.setProtocolPathDest(strPathDest);
			}

			if (copier instanceof SrmliteCopier) {
				temp = srmProtocol.getToken();
				if (temp != null) {
					((SrmliteCopier) copier)
							.setSrmProtocol(((StringToken) temp).stringValue()
									.trim());
				}
			}
		} else {
			if (isDebugging) {
				log.debug("Normal mode");
			}
		}
		return copier;
	}

	private ConnectionDetails parseConnectionString(String connectStr)
			throws IllegalActionException {

		int port = -1;
		String host = null;
		String user = null;
		ConnectionDetails conn = new ConnectionDetails();

		// boolean relativePath = false;
		connectStr = connectStr.trim();

		if (connectStr.equals("") || connectStr.equals("localhost")) {
			user = System.getProperty("user.name");
			host = "localhost";
		} else {
			// get USER
			int atPos = connectStr.indexOf('@');
			if (atPos >= 0)
				user = connectStr.substring(0, atPos);

			// get HOST
			int colonPos = connectStr.indexOf(':');
			if (colonPos >= 0) {
				if (atPos >= 0) {
					host = connectStr.substring(atPos + 1, colonPos);
				} else {
					host = connectStr.substring(0, colonPos);
				}
				String portStr = connectStr.substring(colonPos + 1);
				try {
					port = Integer.parseInt(portStr);
				} catch (java.lang.NumberFormatException ex) {
					throw new IllegalActionException(
							"The port should be a number or omitted in source path "
									+ connectStr);
				}
			} else {
				if (atPos >= 0) {
					host = connectStr.substring(atPos + 1);
				} else {
					host = connectStr;
				}
			}
		}
		conn.setUser(user);
		conn.setHost(host);
		conn.setPort(port);
		return conn;
	}

}
