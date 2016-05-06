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

package org.sdm.spa;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.io.DirectoryListing;
import org.kepler.io.FileInfo;
import org.kepler.io.MappedLog;
import org.kepler.io.SharedLog;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.ExecFactory;
import org.kepler.ssh.ExecInterface;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;

//////////////////////////////////////////////////////////////////////////
//// ProcessFileRT
/**
 * <p>
 * no documentation yet.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Revision: 24234 $
 * @category.name remote
 * @category.name connection
 * @category.name external execution
 */

public class ProcessFileRT extends TypedAtomicActor {

	/**
	 * Construct an ExecuteCmd actor with the given container and name. Create
	 * the parameters, initialize their values.
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
	public ProcessFileRT(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		String[] labels = { "name", "size", "date" };
		Type[] ctypes = { BaseType.STRING, BaseType.LONG, BaseType.LONG };
		_etype = new RecordType(labels, ctypes);

		// INPUT PORT: receiving a file recordtoken
		filein = new TypedIOPort(this, "filein", true, false);
		filein.setTypeEquals(_etype);
		new Parameter(filein, "_showName", BooleanToken.TRUE);

		// OUTPUT PORT: emitting a file recordtoken
		fileout = new TypedIOPort(this, "fileout", false, true);
		fileout.setTypeEquals(_etype);
		new Parameter(fileout, "_showName", BooleanToken.TRUE);

		String[] sublabels = { "name", "value" };
		Type[] subtypes = { BaseType.STRING, BaseType.STRING };
		_substype = new RecordType(sublabels, subtypes);

		// INPUT PORT: substitution token: an array of recordtokens
		subs = new TypedIOPort(this, "subs", true, false);
		subs.setTypeEquals(new ArrayType(_substype));
		new Parameter(subs, "_showName", BooleanToken.TRUE);

		// OUTPUT PORT: emitting a file recordtoken for provenance purposes
		// info should come back from the executed command
		provenanceout = new TypedIOPort(this, "provenanceout", false, true);
		provenanceout.setTypeEquals(_etype);
		new Parameter(provenanceout, "_showName", BooleanToken.FALSE);
		new Parameter(provenanceout, "_hide", BooleanToken.TRUE);
		// new Parameter(provenanceout, "_cardinal", new StringToken("SOUTH"));
		StringAttribute portDirection;
		portDirection = (StringAttribute) getAttribute("_cardinal");
		if (portDirection == null)
			portDirection = new StringAttribute(this, "_cardinal");
		portDirection.setExpression("south");

		// PARAMETERS

		// target selects the machine where to connect to
		RemoteMachine = new Parameter(this, "RemoteMachine", new StringToken(
				"Execution host as [user@]host[:port]"));

		Command = new Parameter(
				this,
				"Command",
				new StringToken(
						"The command string, using _INFILE_, _OUTFILE_, _INPATH_, _OUTPATH_ and _HOST_ as macros"));

		InDir = new Parameter(this, "InDir", new StringToken(
				"Directory of the incoming file (input dir)"));

		OutRemoteMachine = new Parameter(
				this,
				"OutRemoteMachine",
				new StringToken(
						"Target host of the expected output file (usually same as RemoteMachine)"));

		OutDir = new Parameter(this, "OutDir", new StringToken(
				"Directory of the expected output file"));

		Ext = new Parameter(this, "Ext", new StringToken(
				"output file extension (replacing the input file extension)"));

		ReplaceExt = new Parameter(this, "ReplaceExt", new BooleanToken(false));
		doProcessing = new Parameter(this, "doProcessing", new BooleanToken(
				true));
		doCkpt = new Parameter(this, "doCkpt", new BooleanToken(true));
		doCheckOutput = new Parameter(this, "doCheckOutput", new BooleanToken(
				true));

		LogHeader = new Parameter(this, "LogHeader", new StringToken(
				"Header string for logging"));

		LogFile = new FileParameter(this, "LogFile");
		LogFile.setExpression("$CWD" + File.separator + "kepler.log");

		ErrorLogFile = new FileParameter(this, "ErrorLogFile");
		ErrorLogFile.setExpression("$CWD" + File.separator + "kepler.err");

		LogFormat = new Parameter(this, "LogFormat", new StringToken(
				"txt | xml"));

		CkptFile = new FileParameter(this, "CkptFile");
		CkptFile.setExpression("$CWD" + File.separator + "kepler.ckp");

		StopFile = new Parameter(this, "StopFile", new StringToken(
				"Special file name which is bypassed in this actor"));

		ErrorTokenName = new Parameter(this, "ErrorTokenName", new StringToken(
				"Special (file) name which is bypassed in this actor"));

		HostStr = new Parameter(this, "HostStr", new StringToken(
				"replaces _HOST_ in the command string"));

		timeoutSeconds = new Parameter(this, "timeoutSeconds", new IntToken(0));
		timeoutSeconds.setTypeEquals(BaseType.INT);

		cleanupAfterError = new Parameter(this, "cleanupAfterError",
				new BooleanToken(false));
		cleanupAfterError.setTypeEquals(BaseType.BOOLEAN);

		/* Hidden, expert Parameter */
		thirdParty = new Parameter(this, "thirdParty", new StringToken(""));
		thirdParty.setVisibility(Settable.EXPERT);

	}

	// //////////////// Public ports and parameters ///////////////////////

	/**
	 * Input file to work on (recordtype {name=string, data=long, size=long})
	 */
	public TypedIOPort filein;

	/**
	 * Output file as result (recordtype {name=string, data=long, size=long})
	 */
	public TypedIOPort fileout;

	/**
	 * Substitutions (array of recordtype {name=string, value=string})
	 */
	public TypedIOPort subs;

	/**
	 * Output file as result (recordtype {name=string, data=long, size=long}).
	 * For provenance purposes. Info comes from the remote command.
	 */
	public TypedIOPort provenanceout;

	/**
	 * Target in user@host:port format. If user is not provided, the local
	 * username will be used. If port is not provided, the default port 22 will
	 * be applied. If target is "local" or empty string, the command will be
	 * executed locally, using Java Runtime.
	 */
	public Parameter RemoteMachine;

	/**
	 * The command to be executed on the remote host. It needs to be provided as
	 * a string.
	 */
	public Parameter Command;

	public Parameter InDir;
	public Parameter OutRemoteMachine;
	public Parameter OutDir;
	public Parameter Ext;
	public Parameter ReplaceExt;
	public Parameter doProcessing;
	public Parameter doCkpt;
	public Parameter doCheckOutput;
	public Parameter LogHeader;
	public FileParameter LogFile;
	public FileParameter ErrorLogFile;
	public Parameter LogFormat;
	public FileParameter CkptFile;
	public Parameter StopFile;
	public Parameter ErrorTokenName;
	public Parameter HostStr;

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
	 * Third party target in user@host:port format. If user is not provided, the
	 * local username will be used. If port is not provided, the default port 22
	 * will be applied.
	 */
	public Parameter thirdParty;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * initialize() runs once before first exec
	 * 
	 * @exception IllegalActionException
	 *                If the parent class throws it.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_p = new Params(); // private class to store all actor parameters
		_mappedLog = new MappedLog(); // for checkpoint file
		boolean _xmlFormat = _p.strLogFormat.trim().equalsIgnoreCase("xml");
		_sharedLog = new SharedLog(_xmlFormat); // for log and error log files

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

		// get the input token
		RecordToken fileInToken = (RecordToken) filein.get(0);
		String fileInName = getColumnString(fileInToken, "name");

		// get the substitution record if connected
		Token subsArray[] = null;
		if (subs.getWidth() > 0) {
			ArrayToken subsArrayToken = (ArrayToken) subs.get(0);
			subsArray = subsArrayToken.arrayValue();
		}

		// get parameters
		_p.update();

		// cases when we just pass the token and do nothing
		if (fileInName == null || fileInName.equals(_p.strStopFile) || // pass
																		// on
																		// Stop
																		// File
				fileInName.equals(_p.strErrorTokenName) || // pass on error
															// token
				!_p.bDoProcessing) { // pass on if doProcessing = false

			if (isDebugging)
				log.debug(_p.strLogHeader + ": Pass on token with name: "
						+ fileInName);
			// just send the incoming token out untouched
			fileout.send(0, fileInToken);
			return;
		}

		// get the expected output file name (replacing ext if requested)
		String fileOutName = getOutfileName(fileInName);
		if (isDebugging)
			log.debug(_p.strLogHeader + ": File output name = " + fileOutName);

		// prepare the command string
		// replace _INPUT_ _OUTPUT_ _INPATH_ _OUTPATH_ _HOST_ with the actual
		// values
		String command = prepareCommand(fileInName, fileOutName);
		if (isDebugging)
			log.debug(_p.strLogHeader + ": Command = " + command);

		// process the substitutions
		command = substituteCommand(command, subsArray);
		if (isDebugging)
			log.debug(_p.strLogHeader + ": Command = " + command);

		// check if command was not executed already (checkpoint)
		boolean ckptDone = _p.bDoCkpt
				&& _mappedLog.check(CkptFile.asFile(), command);
		if (isDebugging)
			log.debug(_p.strLogHeader + ":"
					+ (ckptDone ? " skip: " : " start: ") + command);
		// make log statement no. 1: start or skip this command
		_sharedLog.print(LogFile.asFile(), _p.strLogHeader,
				(ckptDone ? " skip: " : " start: ") + command);

		// Execute command
		int exitCode = 0;
		if (!ckptDone) {
			exitCode = execCmd(command);
			if (isDebugging)
				log.debug(_p.strLogHeader + ":"
						+ (exitCode == 0 ? " succ: " : " fail: ") + command);
			// make log statement no. 2: succ'd or failed this command
			_sharedLog.print(LogFile.asFile(), _p.strLogHeader,
					(exitCode == 0 ? " succ: " : " fail: ") + command);
		}

		// on success, add command to checkpoint (if requested)
		if (exitCode == 0 && _p.bDoCkpt)
			_mappedLog.add(CkptFile.asFile(), command);

		FileInfo outFile;
		// produce the output token
		if (exitCode == 0) { // successful execution
			// check the output file if requested
			if (_p.bDoCheckOutput) {

				outFile = checkOutputFile(fileOutName, command);

			} else { // just emit the name without checking it
				outFile = new FileInfo(fileOutName, getColumnLong(fileInToken,
						"size"), getColumnLong(fileInToken, "date"));

			}
		} else { // failed operation: emit an error token
			outFile = new FileInfo(_p.strErrorTokenName);
		}

		// send output file
		fileout.send(0, createRecordToken(outFile));

	} // end-method fire()

	/**
	 * List file using DirectoryListing. Note: command is passed only for error
	 * logging purposes
	 */
	private FileInfo checkOutputFile(String fname, String command)
			throws IllegalActionException {
		String masks[] = new String[1];
		masks[0] = fname;
		DirectoryListing dl = new DirectoryListing(_p.strOutRemoteMachine,
				_p.strOutDir, masks);
		FileInfo fi;

		try {
			int n = dl.list();
			if (n > 0) {
				FileInfo files[] = dl.getList();
				if (files.length > 1) {
					log.warn(_p.strLogHeader + ": Found more than one file ("
							+ files.length + ") in " + _p.strOutRemoteMachine
							+ ":" + _p.strOutDir + " with mask: " + fname);
					_sharedLog.print(ErrorLogFile.asFile(), _p.strLogHeader,
							"WARNING at command: " + command
									+ "\nFound more than one file ("
									+ files.length + ") in "
									+ _p.strOutRemoteMachine + ":"
									+ _p.strOutDir + " with mask: " + fname);
				}
				fi = files[0];
			} else {
				// did not find result: send an error token
				log.error(_p.strLogHeader
						+ ": No output file is found with mask " + fname
						+ " in " + _p.strOutRemoteMachine + ":" + _p.strOutDir);
				_sharedLog.print(ErrorLogFile.asFile(), _p.strLogHeader,
						"ERROR after command: " + command
								+ "\nNo output file is found with mask "
								+ fname + " in " + _p.strOutRemoteMachine + ":"
								+ _p.strOutDir);
				fi = new FileInfo(_p.strErrorTokenName);
			}
		} catch (ExecException ex) {
			log.error(_p.strLogHeader
					+ ": Error when trying to list output directory "
					+ _p.strOutRemoteMachine + ":" + _p.strOutDir + " :" + ex);
			_sharedLog.print(ErrorLogFile.asFile(), _p.strLogHeader,
					"ERROR at command: " + command
							+ "\nError when trying to list output directory "
							+ _p.strOutRemoteMachine + ":" + _p.strOutDir
							+ " :" + ex);
			fi = new FileInfo(_p.strErrorTokenName);
		}
		return fi;
	}

	/**
	 * Execute the command. Host and other parameters are taken from the global
	 * params. Return the exitcode, -32767 on ssh related error
	 */
	private int execCmd(String command) throws IllegalActionException {
		// Execute command on the remote machine (which can be the local
		// machine)

		int exitCode = 0;
		ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
		ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();

		// execute command
		try {
			ExecInterface execObj = ExecFactory.getExecObject(_p.strRemoteMachine);
			execObj.setTimeout(_p.iTimeoutSeconds, false, false);
			execObj.setForcedCleanUp(_p.bCleanupAfterError);
			log.info("Exec cmd: " + command);
			exitCode = execObj.executeCmd(command, cmdStdout, cmdStderr,
					_p.strThirdParty);
			if (exitCode != 0) {
				// print stdErr to error log if exitcode is non-zero (usually
				// means error)
				_sharedLog.print(ErrorLogFile.asFile(), _p.strLogHeader,
						"ERROR at command: " + command + "\nExit code = "
								+ exitCode + "\nStderr: " + cmdStderr);
			} else {
				// do some provenance`
				provenanceInfo(cmdStdout.toString(), command);
			}
		} catch (ExecException e) {
			exitCode = -32767;
			String errText = new String("ExecuteCmd error:\n" + e.getMessage());
			if (isDebugging)
				log.debug(_p.strLogHeader + ":" + errText);
			_sharedLog.print(ErrorLogFile.asFile(), _p.strLogHeader,
					"EXEC ERROR when trying to execute command: " + command
							+ "\n" + errText);
		}
		return exitCode;
	}

	/**
	 * Provenance stuff. Prototype. expect lines like: --ProvenanceInfo
	 * name=ion__density.0001.jpg, date=1923982434, size=2855
	 */
	private void provenanceInfo(String text, String command)
			throws IllegalActionException {
		String lines[] = text.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String fields[] = lines[i].trim().split("( )+", 9);
			if (fields[0].startsWith("--ProvenanceInfo")) {
				String name = null;
				long date = -1, size = -1;
				int idx;
				int end = (fields.length < 3 ? 3 : fields.length - 1);
				// log.debug("--Provenance: end="+end+" field[1]="+fields[1]);
				for (int j = 1; j <= end; j++) {
					idx = fields[j].indexOf("=");
					if (idx != -1) {
						String n = fields[j].substring(0, idx);
						String v = fields[j].substring(idx + 1);
						// log.debug("n="+n+"  v="+v);
						if (n.equals("name"))
							name = v;
						if (n.equals("date"))
							date = Long.parseLong(v);
						if (n.equals("size"))
							size = Long.parseLong(v);
					}
				}
				if (name != null) {
					// send output file
					provenanceout.send(0, createRecordToken(new FileInfo(name,
							size, date)));
				} else {
					log.error(_p.strLogHeader
							+ ": Invalid provenance info line from command '"
							+ command + "': " + lines[i]);
        			_sharedLog.print(ErrorLogFile.asFile(), _p.strLogHeader,
							"Invalid provenance info line from command '"
							+ command + "': " + lines[i]);

				}
			}
		}

	}

	/** RecordToken f --> String f.name */
	private String getColumnString(RecordToken rt, String label) {
		if (rt == null)
			return null;
		StringToken st = (StringToken) rt.get(label);
		if (st == null)
			return null;
		return st.stringValue();
	}

	/** RecordToken f --> long f.<field> */
	private long getColumnLong(RecordToken rt, String label) {
		if (rt == null)
			return 0L;
		LongToken lt = (LongToken) rt.get(label);
		if (lt == null)
			return 0L;
		return lt.longValue();
	}

	/**
	 * Replace the file extension of the file name if requested. extension and
	 * the boolean are taken from the global params.
	 */
	private String getOutfileName(String in) {
		if (!_p.bReplaceExt)
			return in;

		int idx = in.lastIndexOf('.');
		if (idx < 0)
			idx = in.length(); // no extension: append extension to the end of
								// name

		return new String(in.substring(0, idx) + "." + _p.strExt);
	}

	/**
	 * Make the actual command string from the parameter command using other
	 * parameters and the actual input/output file names. Replace _INPUT_
	 * _OUTPUT_ _INPATH_ _OUTPATH_ _HOST_ with the actual values inName : input
	 * file's name outName : output file's expected name
	 */
	private String prepareCommand(String inName, String outName) {
		String cmd = _p.strCommand.replaceAll("_INFILE_", inName);
		cmd = cmd.replaceAll("_OUTFILE_", outName);
		cmd = cmd.replaceAll("_INPATH_", _p.strInDir + "/" + inName);
		cmd = cmd.replaceAll("_OUTPATH_", _p.strOutDir + "/" + outName);
		cmd = cmd.replaceAll("_HOST_", _p.strHost);
		return cmd;
	}

	/**
	 * Make the actual command string from the parameter command by substituting
	 * name/value pairs in the string. command : the command string to be
	 * processed subsArray: array of RecordTokens of {name, value} In command,
	 * each 'name' found will be replaced by the corresponding value at all
	 * places.
	 */
	private String substituteCommand(String command, Token subs[]) {
		if (subs == null || subs.length == 0)
			return command;
		String cmd = command;
		for (int i = 0; i < subs.length; i++) {
			RecordToken rt = (RecordToken) subs[i];
			String name = getColumnString(rt, "name");
			String value = getColumnString(rt, "value");
			cmd = cmd.replaceAll(name, value);
		}
		return cmd;
	}

	/*
	 * Create one RecordToken of format {name=String, size=long, date=long} from
	 * the FileInfo struct.
	 */
	private RecordToken createRecordToken(FileInfo fi) {
		String[] labels = { "name", "size", "date" };
		Token[] values = new Token[3];
		values[0] = new StringToken(fi.getName());
		values[1] = new LongToken(fi.getSize());
		values[2] = new LongToken(fi.getDate());
		RecordToken rt = null;
		try {
			rt = new RecordToken(labels, values);
		} catch (IllegalActionException ex) {
			log.error(_p.strLogHeader
					+ ": Error at creating a record token for fileinfo: " + fi
					+ "\nlabels = " + labels + "\nvalues = " + values);
		}
		return rt;
	}

	private class Params {

		public String strRemoteMachine;
		public String strCommand;
		public String strInDir;
		public String strOutRemoteMachine;
		public String strOutDir;
		public String strExt;
		public boolean bReplaceExt;
		public boolean bDoProcessing;
		public boolean bDoCkpt;
		public boolean bDoCheckOutput;
		public String strLogHeader;
		public String strLogFormat;
		public String strStopFile;
		public String strErrorTokenName;
		public String strHost;
		public int iTimeoutSeconds;
		public boolean bCleanupAfterError;
		public String strThirdParty;

		public Params() throws IllegalActionException {
			this.update();
		}

		// process inputs
		public void update() throws IllegalActionException {

			strRemoteMachine = ((StringToken) RemoteMachine.getToken())
					.stringValue();
			strCommand = ((StringToken) Command.getToken()).stringValue();
			strInDir = ((StringToken) InDir.getToken()).stringValue();
			strOutRemoteMachine = ((StringToken) OutRemoteMachine.getToken())
					.stringValue();
			strOutDir = ((StringToken) OutDir.getToken()).stringValue();
			strExt = ((StringToken) Ext.getToken()).stringValue();
			bReplaceExt = ((BooleanToken) ReplaceExt.getToken()).booleanValue();
			bDoProcessing = ((BooleanToken) doProcessing.getToken())
					.booleanValue();
			bDoCkpt = ((BooleanToken) doCkpt.getToken()).booleanValue();
			bDoCheckOutput = ((BooleanToken) doCheckOutput.getToken())
					.booleanValue();
			strLogHeader = ((StringToken) LogHeader.getToken()).stringValue();
			strLogFormat = ((StringToken) LogFormat.getToken()).stringValue();
			strStopFile = ((StringToken) StopFile.getToken()).stringValue();
			strHost = ((StringToken) HostStr.getToken()).stringValue();
			strErrorTokenName = ((StringToken) ErrorTokenName.getToken())
					.stringValue();
			iTimeoutSeconds = ((IntToken) timeoutSeconds.getToken()).intValue();
			bCleanupAfterError = ((BooleanToken) cleanupAfterError.getToken())
					.booleanValue();
			strThirdParty = ((StringToken) thirdParty.getToken()).stringValue();

			if (isDebugging)
				log.debug(strLogHeader + ": Parameters" +
				// "\nRemoteMachine = "+strRemoteMachine+
						// "\nOutRemoteMachine = "+strOutRemoteMachine+
						"\nCommand = " + strCommand +
						// "\nInDir = "+strInDir+
						// "\nOutDir = "+strOutDir+
						"\ndoProcessing = " + bDoProcessing + "");

		}

	} // end class Params

	private Type _etype; // in/out token type (a record)
	private Type _substype; // substitution token type (a record)
	private Params _p; // actor parameters stored in private class
	private MappedLog _mappedLog; // checkpoint file (with memory hash)
	private SharedLog _sharedLog; // log file and error log file

	private static final Log log = LogFactory.getLog(ProcessFileRT.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
}

// vim: sw=4 ts=4 et