/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-02-28 14:40:32 -0800 (Fri, 28 Feb 2014) $' 
 * '$Revision: 32614 $'
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

package edu.sdsc.nbcr.opal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.FilePortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.nbcr.opal.gui.common.AppMetadata;
import edu.sdsc.nbcr.opal.gui.common.AppMetadataParser;
import edu.sdsc.nbcr.opal.gui.common.ArgFlag;
import edu.sdsc.nbcr.opal.gui.common.ArgParam;
import edu.sdsc.nbcr.opal.gui.common.Group;

//////////////////////////////////////////////////////////////////////////
//// OpalClient
/**
 * <p>
 * 
 * This actor can be used to execute Opal based applications.
 * 
 * </p>
 * TODO: - given the URL it should display a list of methods provided by the
 * server
 * 
 * @author Luca Clementi
 * @version $Id: OpalClient.java 32614 2014-02-28 22:40:32Z crawl $
 */

public class OpalClient extends TypedAtomicActor {
	/**
	 * Construct a OpalClient source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public OpalClient(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		serverName = new StringParameter(this, "serviceURL");
		// serverName.setExpression("http://ws.nbcr.net/opal/services/ApbsOpalService");
		serverName.setExpression("");
		// methods = new StringParameter(this, "methods");
		// methods.setExpression("not used");

		output = new TypedIOPort(this, "output", false, true);// output port
		output.setTypeEquals(BaseType.STRING);
		new Parameter(output, "_showName", BooleanToken.FALSE);

		baseUrl = new TypedIOPort(this, "baseUrl", false, true);// output port
		baseUrl.setTypeEquals(BaseType.STRING);
		new Parameter(baseUrl, "_showName", BooleanToken.FALSE);

		numberFiles = new StringParameter(this, "numberOfExtraInputFiles");
		numberFiles.setToken(new StringToken("0"));

        _attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
            + "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
            + "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The name of the server to query
	 */
	public StringParameter serverName = null;

	/**
	 * The web service method name to run.
	 */
	// not used at the moment todo list
	// public StringParameter methods = null;
	/**
	 * This is the output port of this actor.
	 */
	public TypedIOPort output;

	/**
	 * This is the output port of this actor.
	 */
	public TypedIOPort baseUrl;

	/**
	 * The web service method name to run.
	 */
	public StringParameter numberFiles = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * React to a change in an attribute.
	 * 
	 * @param attribute
	 *            The changed parameter.
	 * @exception IllegalActionException
	 *                If the parameter set is not valid.
	 */
        public void attributeChanged(Attribute attribute) throws IllegalActionException {
            String tmp = null;
            if (attribute == serverName) {
              // --------------    the user has changed serverName -----------
              String strServerName = serverName.getExpression();
              if (_serverNameString == null) {
                //the value is loaded from the moml file
                //don't cal the _createSubmission
                _serverNameString = strServerName;
                log.info("_serverNameString is set according to original value of moml file.");
                if (!strServerName.equals("")) {
                  _appMetadata = AppMetadataParser.parseAppMetadata(strServerName);
                  if (_appMetadata == null) {
                    // something bad happen while getting the appMetadata
                    log.error("Failed to parse metadata at "
                        + strServerName);
                    throw new IllegalActionException(this,
                        "The selected URL does not point to a valid Opal service");
                  }
                }
              } else if (!strServerName.equals(_serverNameString)) {
                // the user changed the value, we have to update the actor
                log.info("Got a new server name: " + strServerName);
                _appMetadata = AppMetadataParser.parseAppMetadata(strServerName);
                if (_appMetadata != null) {
                  if (_appMetadata.isArgMetadataEnable())
                    // complex submission form
                    _createSubmission(_appMetadata);
                  else
                    // simple submission form
                    _createSimpleSubmission(_appMetadata);
                  _addDocumentation(_appMetadata);
                } else {
                  // something bad happen while getting the appMetadata
                  log.error("Failed to parse metadata at " + strServerName);
                  throw new IllegalActionException(this,
                      "The selected URL does not point to a valid Opal service");
                }
                _serverNameString = strServerName;
                this.propagateValues();
              }
            } else if (attribute == numberFiles) {
              // --------------    the user has changed the number of files -----------
                int numFiles = 1;
                try {
                  numFiles = Integer.parseInt(numberFiles.stringValue());
                } catch (NumberFormatException e) {
                  throw new IllegalActionException(this,
                      "The numberFiles parameter is not a valid integer, please correct the value");
                }
                if (numFiles != _numberFiles) {
                  _updateUploadFileNumber(numFiles);
                  _numberFiles = numFiles;
                }
            } else {
              log.debug("the user has changed: " + attribute.toString());
            }
            super.attributeChanged(attribute);
        }// attributeChanged

	/**
	 * Initialize the actor by getting the input and output parameters from the
	 * soap service.
	 */
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();
	}

	/**
	 * Create and send the request, and send the response to the appropriate
	 * output ports.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		_updateFilePortParameters();
		String commandLine = _makeCmd();
		if (commandLine == null) {
			log.error("Command line is null.");
			throw new IllegalActionException(
					"Unable to built the command line.");
		}
        int cpuNumber = _getCpuNumber();

		// let's invoke the remote opal service
		JobInputType jobInputType = new JobInputType();
		jobInputType.setArgList(commandLine);
        if ( cpuNumber != 0 ) { 
            jobInputType.setNumProcs(new Integer(cpuNumber));
        }
		// preparing the input files
		InputFileType[] files = _getInputFiles();
		if (files != null) {
			log.info(files.length + " files have been submitted");
			String names = "";
			for (int i = 0; i < files.length; i++)
				names += files[i].getName() + " ";
			log.info("their names are: " + names);
			jobInputType.setInputFile(files);
		} else {
			log.info("No file has been submitted.");
		}

		// finally invoke opal service!
		JobSubOutputType subOut = null;
		AppServicePortType appServicePort = null;
		try {
			AppServiceLocator asl = new AppServiceLocator();
			appServicePort = asl.getAppServicePort(new java.net.URL( _appMetadata.getURL()));
			subOut = appServicePort.launchJob(jobInputType);// TODO check for null
			// Let's wait for the job to finish
			log.info("Job has been sumitted waiting for its completition: "
					+ subOut.getJobID());
			StatusOutputType status = subOut.getStatus();
			int code = status.getCode();
			while (code != 4 && code != 8) {
				// sleep
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				status = appServicePort.queryStatus(subOut.getJobID());
				log.info("Remote job status for job " + subOut.getJobID()
						+ " from server is: " + status.getMessage());
				code = status.getCode();
			}
			if (code == 8) {
				// success
				JobOutputType out = null;
				out = appServicePort.getOutputs(subOut.getJobID());
				// send base Url
				baseUrl
						.send(0,
								new StringToken(status.getBaseURL().toString()));
				// send stdout and error
				output.send(0, new StringToken(out.getStdOut().toString()));
				output.send(0, new StringToken(out.getStdErr().toString()));
				OutputFileType[] outfiles = out.getOutputFile();
				if (outfiles != null) {
					for (int i = 0; i < outfiles.length; i++)
						output.send(0, new StringToken(outfiles[i].getUrl()
								.toString()));
				}// if
			} else {
				// failure
				throw new IllegalActionException(
						"The exectuion of the OpalClient failed with the following error: "
								+ status.getMessage()
								+ "the application ID is "
								+ status.getBaseURL());
			}
		} catch (FaultType e) {
			log.error("Remote exception in OpalClient: " + e.getMessage1(), e);
			throw new IllegalActionException(this,
					"Remote exception in OpalClient: " + e.getMessage1());
		} catch (RemoteException e) {
			log.error("Exception while invoking OpalClient: " + e.getMessage(),
					e);
			throw new IllegalActionException(this,
					"Excetion while invoking OpalClient: " + e.getMessage());
		} catch (java.net.MalformedURLException e) {
			log.error("Exception while invoking OpalClient: " + e.getMessage(),
					e);
			throw new IllegalActionException(this,
					"Excetion while invoking OpalClient: " + e.getMessage());
		} catch (javax.xml.rpc.ServiceException e) {
			log.error("Exception while invoking OpalClient: " + e.getMessage(),
					e);
			throw new IllegalActionException(this,
					"Excetion while invoking OpalClient: " + e.getMessage());
		}
	}// fire()

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * This method instantiates the parameters needed for the submission of a
	 * Opal job without metadata
	 */
	private void _createSimpleSubmission(AppMetadata app)
			throws IllegalActionException {
		_emptyParamtersArray();
		// TODO delete this _interfaceParameters is useless
		log.info("Creating simple submission form. For service URL: "
				+ app.getURL());
		try {
			(new StringParameter(this, "commandLine")).setContainer(this);
            (new StringParameter(this, "cpuNumber")).setContainer(this);
			// let's try to get the number of files
			int numFiles;
			try {
				numFiles = Integer.parseInt(numberFiles.stringValue());
			} catch (NumberFormatException e) {
				throw new IllegalActionException(this,
						"The numberFiles parameter is not a valid integer, please correct the value");
			}
			_updateUploadFileNumber(numFiles);
		} catch (NameDuplicationException e) {
			log.error(
					"The parameter could not be instantiated. Error message: "
							+ e.getMessage(), e);
		}

	}

	/**
	 * this function instantiate all the parameter necessary to display an
	 * advanced submission form for the current selected opal service
	 */
	private void _createSubmission(AppMetadata app)
			throws IllegalActionException {
		_emptyParamtersArray();
		log.info("Creating complex submission form. For service URL: "
				+ app.getURL());
		try {
			Group[] group = app.getGroups();
			ArrayList tempParam = new ArrayList();
			if (group != null) {
				for (int i = 0; i < group.length; i++) {
					// first the parameters
					ArgParam[] argParams = group[i].getArgParams();
					if (argParams != null) {
						for (int paramCount = 0; paramCount < argParams.length; paramCount++) {
							if (argParams[paramCount].getType().equals("FILE")
									&& argParams[paramCount].getIoType()
											.equals("INPUT")) {
								// This is an input file box
								FilePortParameter param = new FilePortParameter(
										this, argParams[paramCount].getId());
								if (argParams[paramCount].getSelectedValue() != null)
									// setting the default value
									param.setExpression(argParams[paramCount]
											.getSelectedValue());
								tempParam.add(param);
							} else if (argParams[paramCount].getValues() != null) {
								String[] choices = argParams[paramCount]
										.getValues();
								// this is a multiple choices
								StringParameter param = new StringParameter(
										this, argParams[paramCount].getId());
								for (int z = 0; z < choices.length; z++)
									param.addChoice(choices[z]);
								if (argParams[paramCount].getSelectedValue() != null)
									// setting the default value
									param.setExpression(argParams[paramCount]
											.getSelectedValue());
								tempParam.add(param);
							} else {
								// this is a string
								StringParameter param = new StringParameter(
										this, argParams[paramCount].getId());
								if (argParams[paramCount].getSelectedValue() != null)
									// setting the default value
									param.setExpression(argParams[paramCount]
											.getSelectedValue());
								tempParam.add(param);
							}// else
						}// for argParam
					}// argParams
					// and then the flags
					ArgFlag[] argFlags = group[i].getArgFlags();
					if (argFlags != null) {
						for (int paramCount = 0; paramCount < argFlags.length; paramCount++) {
							Parameter check = null;
							if (argFlags[paramCount].isSelected())
								check = new Parameter(this,
										argFlags[paramCount].getId(),
										BooleanToken.TRUE);
							else
								check = new Parameter(this,
										argFlags[paramCount].getId(),
										BooleanToken.FALSE);
							check.setTypeEquals(BaseType.BOOLEAN);
							tempParam.add(check);
						}
					}// argFlags
				}
			}// group
			// set the numberFiles to 0
			StringParameter param = (StringParameter) this
					.getAttribute("numberFiles");
			if (param != null)
				param.setToken(new StringToken("0"));

			// not needed anymore delete the tempParam
			// _interfaceParameters = (Parameter []) tempParam.toArray(new
			// Parameter[tempParam.size()]);
		} catch (NameDuplicationException e) {
			log
					.error("The parameter could not be instantiated. Error message: "
							+ e.getMessage());
			throw new IllegalActionException(this,
					"The complex submission form could not be initialized: "
							+ e.getMessage());
		}
		return;
	}

	/**
	 * this functions empty the _interfaceParameters array from all the
	 * parameters basically it clear all the parameters that are currently used
	 */
	private void _emptyParamtersArray() throws IllegalActionException {
		// now we can delete the attribute
		Attribute[] removeAttrs = _getOpalAttributes();
		for (int i = 0; i < removeAttrs.length; i++) {
			try {
				if (removeAttrs[i] instanceof FilePortParameter) {
					// this is both a port and a parameters
					((FilePortParameter) removeAttrs[i]).getPort()
							.setContainer(null);
					((FilePortParameter) removeAttrs[i]).setContainer(null);
				} else
					removeAttrs[i].setContainer(null);
			} catch (Exception e) {
				log.error("Could not delete the attribute "
						+ removeAttrs[i].getName() + " with error:"
						+ e.getMessage() + "'.", e);
				throw new IllegalActionException("Can not delete attribute "
						+ removeAttrs[i].getName() + " from OpalClient: "
						+ e.getMessage());
			}// catch
		}
	}// _emptyParamtersArray

	/**
	 * this function adds all the parameters necessary to have the documentation
	 * of the OpalClient abviously this documentation is fetch from the remote
	 * server
	 */
	private void _addDocumentation(AppMetadata app)
			throws IllegalActionException {
		// let's get the variable we need
		Attribute documentation = this.getAttribute("KeplerDocumentation");
		if (documentation == null)
			throw new IllegalActionException(this,
					"Unable to set the custom documentation (KeplerDocumentation not present).");
		Attribute userDoc = documentation
				.getAttribute("userLevelDocumentation");
		if (userDoc == null)
			throw new IllegalActionException(this,
					"Unable to set the custom documentation (userLevelDocumentation not present).");
		// -- we have to delete all the documentation that is already there
		// before updating it
		List tempList = documentation.attributeList();
		Attribute[] paramArray = (Attribute[]) tempList
				.toArray(new Attribute[tempList.size()]);
		for (int i = 0; i < paramArray.length; i++) {
			try {
				if (paramArray[i].getName().startsWith("prop:")
						|| paramArray[i].getName().startsWith("port:output")
						|| paramArray[i].getName().startsWith("port:baseUrl"))
					// let's remove the parameters
					paramArray[i].setContainer(null);
			} catch (NameDuplicationException e) {
				log
						.error("The parameter could not be deleted from the documentation. Error message: "
								+ e.getMessage());
				throw new IllegalActionException(this,
						"Error resetting the ducumentation, there is a duplicated name: "
								+ e.getMessage());
			}
		}
		// After deleting we can finally set the documentation
		String stringDoc = _docHead;
		// let's set the general documentaion
		if (app.getUsage() != null)
			stringDoc += "<p>" + app.getUsage() + "</p>";
		if (app.getInfo() != null) {
			String[] infos = app.getInfo();
			for (int i = 0; i < infos.length; i++)
				stringDoc += "<pre>" + forXML(infos[i]) + "</pre>";
		}// if
		log.debug("the stringDoc is: " + stringDoc);
		((ConfigurableAttribute) userDoc).setExpression(stringDoc);

		// -- let's set the documentation for the various properties --
		// some properties are always there... hence also the documentation
		// should be always there
		try {
			((ConfigurableAttribute) userDoc).value();
			ConfigurableAttribute parameterDoc = new ConfigurableAttribute(
					documentation, "port:output");
			parameterDoc
					.setExpression("It returns a list of Strings, containing the URL of the output files");
			parameterDoc = new ConfigurableAttribute(documentation,
					"port:baseUrl");
			parameterDoc
					.setExpression("The base URL containing the working directory of the running jobs");
			parameterDoc = new ConfigurableAttribute(documentation,
					"prop:serviceURL");
			parameterDoc
					.setExpression("The URL of the Opal service that you want to execute");
			parameterDoc = new ConfigurableAttribute(documentation,
					"prop:numberOfExtraInputFiles");
			parameterDoc
					.setExpression("The number of extra input files that are needed to execture the application");
			// parameterDoc = new ConfigurableAttribute(documentation,
			// "prop:methods");
			// parameterDoc.setExpression("Not implemented yet.");
		} catch (NameDuplicationException e) {
			log
					.error("The parameter could not be instantiated. Error message: "
							+ e.getMessage());
			throw new IllegalActionException(this,
					"Error building the ducumentation, there is a duplicated name: "
							+ e.getMessage());
		} catch (IOException e) {
			log
					.error("There is an error while setting the general documentation: "
							+ e.getMessage());
		}
		// we have to list the documentation for every parameters
		if (app.isArgMetadataEnable()) {
			Group[] group = app.getGroups();
			try {
				if (group != null) {
					for (int i = 0; i < group.length; i++) {
						// first the parameters
						ArgParam[] argParams = group[i].getArgParams();
						if (argParams != null) {
							for (int paramCount = 0; paramCount < argParams.length; paramCount++) {
								ConfigurableAttribute parameterDoc = new ConfigurableAttribute(
										documentation, "prop:"
												+ argParams[paramCount].getId());
								parameterDoc
										.setExpression(forXML(argParams[paramCount]
												.getTextDesc()));
							}// for argParam
						}// argParams
						// and then the flags
						ArgFlag[] argFlags = group[i].getArgFlags();
						if (argFlags != null) {
							for (int paramCount = 0; paramCount < argFlags.length; paramCount++) {
								ConfigurableAttribute parameterDoc = new ConfigurableAttribute(
										documentation, "prop:"
												+ argFlags[paramCount].getId());
								parameterDoc
										.setExpression(forXML(argFlags[paramCount]
												.getTextDesc()));
								log.info("The parameter "
										+ argFlags[paramCount].getId()
										+ " has the following doc: "
										+ parameterDoc.value());
								parameterDoc.validate();
							}
						}// argFlags
					}
				}// group
			} catch (NameDuplicationException e) {
				log
						.error("The parameter could not be instantiated. Error message: "
								+ e.getMessage());
				throw new IllegalActionException(this,
						"Error building the ducumentation, there is a duplicated name: "
								+ e.getMessage());
			} catch (IOException e) {
				log
						.error("There is an error while setting the documentation of an element with messge: "
								+ e.getMessage());
			}
		}// if we have metadata
		else {
			// if we don't have metadata we document the standard parameters
			try {
				ConfigurableAttribute parameterDoc = new ConfigurableAttribute(
						documentation, "prop:commandLine");
				parameterDoc
						.setExpression("Insert the command line you want to execute (without the application name)");
				parameterDoc = new ConfigurableAttribute(documentation,
						"prop:cpuNumber");
				parameterDoc
						.setExpression("If it is a parallel application insert the CPU number");
				parameterDoc = new ConfigurableAttribute(documentation,
						"prop:inputFile");
				parameterDoc.setExpression("Select an input file");
			} catch (NameDuplicationException e) {
				log
						.error("The parameter could not be instantiated. Error message: "
								+ e.getMessage());
				throw new IllegalActionException(this,
						"Error building the ducumentation, there is a duplicated name: "
								+ e.getMessage());
			}
		}
	}// _addDocumentation


    /**
     * This function return the number of extra input files
     * currently present in the properties
     */
    private int _getUploadFileNumber() throws IllegalActionException {
        Attribute[] opalAttrs = _getOpalAttributes();
        int counter = 0;
        for (int i = 0; i < opalAttrs.length; i++) {
            if (opalAttrs[i].getName().startsWith("inputFile")
                    && opalAttrs[i].getName().endsWith("Dynamic")
                    && opalAttrs[i] instanceof FilePortParameter) {
                counter++;
            }
        }
        return counter;
    }

	/**
     * this function update the number of extra input file 
     * accorrdingly with the current number of files it deletes
     * or adds extra input files
     */
	private void _updateUploadFileNumber(int numFiles)
                throws IllegalActionException {
        int currentNumberOfFile = 0;
        try { currentNumberOfFile = _getUploadFileNumber();}
        catch (IllegalActionException e){
            throw new IllegalActionException(this, "Unable to load opal paraters:"
                    + e.getMessage());
        }
        int diff = numFiles - currentNumberOfFile;
        if ( diff > 0 ){
            //we have to add diff number of input files
            for (int i = 0; i < diff;  i++) {
                try { 
                    (new FilePortParameter(this, "inputFile" + 
                        (currentNumberOfFile + 1 + i) + "Dynamic")).setContainer(this);
                }catch (NameDuplicationException e) {
                    throw new IllegalActionException(this, 
                        "There is a name duplication: " + e.getMessage());
                }//catch
            }//for
        }else if ( diff < 0 ) {
            // we have to delete number of files
            for (int i = 0; i < Math.abs(diff); i++ ) {
                String fileName = "inputFile" + (currentNumberOfFile - i) + "Dynamic";
                try {
                    FilePortParameter fileToBeDeleted = (FilePortParameter) this.getAttribute(fileName, 
                        FilePortParameter.class);
                    fileToBeDeleted.getPort().setContainer(null);
                    fileToBeDeleted.setContainer(null);
                }catch (NameDuplicationException e) {
                    throw new IllegalActionException(this, "Unable to delete the input file " 
                        + fileName + ": " + e.getMessage());
                }//catch
            }//for
        }//if
        //diff == 0 don't do anything
    }//_updateUploadFileNumber

	/**
	 * This function calls .update() on every parameters which type is
	 * FilePortParameter
	 */
	private void _updateFilePortParameters() throws IllegalActionException {
		Attribute[] opalAttrs = _getOpalAttributes();
		for (int i = 0; i < opalAttrs.length; i++) {
			if (opalAttrs[i] instanceof FilePortParameter) {
				log.info("Calling update on: " + opalAttrs[i].getName());
				((FilePortParameter) opalAttrs[i]).update();
			}
		}
		return;
	}

	/**
	 * this function returns an array of InputFileType containing all the files
	 * selected by the user
     *
     * If the server is Opal2 the file are sent using attachment...
	 */
	private InputFileType[] _getInputFiles() throws IllegalActionException {
		InputFileType inputFile = null;
		ArrayList files = new ArrayList();
		// complex submission form
		Attribute[] opalAttrs = _getOpalAttributes();
		for (int i = 0; i < opalAttrs.length; i++) {
			if (opalAttrs[i] instanceof FilePortParameter) {
				// simple case
				File file = ((FilePortParameter) opalAttrs[i]).asFile();
				if (file == null)
					continue;
				if (file.exists()) {
                    inputFile = new InputFileType();
                    if ( _appMetadata.isOpal2() ) {
                        //use attachment to sent the file
                        DataHandler dh = new DataHandler(new FileDataSource(file));
                        inputFile.setName(file.getName());
                        inputFile.setAttachment(dh);
                        log.info("Uploading file using attahcment: " + inputFile.getName());
                    } else {
                        //use inline to sent the file
                        byte[] fileByte = new byte[(int) file.length()];// this is nasty
                        inputFile.setName(file.getName());
                        try {
                          (new FileInputStream(file)).read(fileByte);
                        } catch (Exception e) {
                          throw new IllegalActionException(
                              "Unable to read input file: " + file.getName());
                        }
                        inputFile.setContents(fileByte);
                        log.info("Uploading file using base64 encoding: " + inputFile.getName());
                    }
					files.add(inputFile);
				} else {
					// the file does not exist
					throw new IllegalActionException(
							"Unable to read input file: " + file.getName());
				}
			}
		}
		log.info("we are returning " + files.size() + " file(s)");
		return (InputFileType[]) files.toArray(new InputFileType[files.size()]);
	}// _getInputFiles


	/**
	 * this function returns an array conatining the Attributes added by the
	 * opal interface it took me lots of time to figure out this code, maybe
	 * there is a cleaner way to get them...
	 */
	private Attribute[] _getOpalAttributes() throws IllegalActionException {
		List attrList = this.attributeList();
		// list of attributes that will be deleted
		ArrayList opalAttrs = new ArrayList();
		Iterator attrs = attrList.iterator();
		// first we have to find the parameter we wanna delete
		while (attrs.hasNext()) {
			Attribute a = (Attribute) attrs.next();
			if ((!a.getName().equals("serviceURL"))
					&& // these are the types that we wanna save all the other
						// will be deleted
					(!a.getName().equals("methods"))
					&& (!a.getName().equals("class"))
					&& (!a.getName().equals("semanticType00"))
					&& (!a.getName().equals("semanticType11"))
					&& (!a.getName().equals("KeplerDocumentation"))
					&& (!a.getName().equals("entityId"))
					&& (!a.getName().equals("numberOfExtraInputFiles"))
					&& (!a.getName().startsWith("_"))) {
				log.info("Opal attribute: " + a.getName());
				opalAttrs.add(a);
				// a.setContainer(null);
			}// if
		}// while
		return (Attribute[]) opalAttrs.toArray(new Attribute[opalAttrs.size()]);
	}

	/**
	 * This is only a debugging function. It simply prints all the attributes
	 * name to the INFO logger
	 */
	private void _printAllAttribute() {
		List attrList = this.attributeList();
		String str = "the list of attributes is: ";
		Attribute[] attrs = (Attribute[]) attrList
				.toArray(new Attribute[attrList.size()]);
		for (int i = 0; i < attrs.length; i++)
			str += attrs[i].getName() + ", ";
		log.info(str);
	}

    /**
     * This function returns the number of CPU selected by the user
     */
    private int _getCpuNumber() throws IllegalActionException {
        StringParameter cpuNumber = null;
        int value = 0;
        try {
            cpuNumber = (StringParameter) this.getAttribute("cpuNumber", StringParameter.class);
        } catch (IllegalActionException e) {
            throw new IllegalActionException(this, "Unable to find the command line parameter: " + e.getMessage());
        }
        if ( cpuNumber == null ) {
            return 0;
        }
        if ( cpuNumber.getExpression().length() == 0 ) {
            return 0;
        }
        try { value =  Integer.valueOf(cpuNumber.getExpression());} 
        catch (Exception e) { throw new IllegalActionException(this, "The number of CPU must be an integer!"); }
        log.debug("Invoking Opal with " + cpuNumber.getExpression() + " CPUs");
        return value;
    }

	/**
	 * this function build the command line reading the parameters selected by
	 * the user
	 */
	private String _makeCmd() throws IllegalActionException {
		String str = "";
		if (_appMetadata.isArgMetadataEnable()) {
			// complex submission form
			if (_appMetadata.getArgFlags() != null) {
				// let's do the falgs
				ArgFlag[] flags = _appMetadata.getArgFlags();
				for (int i = 0; i < flags.length; i++) {
					Parameter flagTemp = null;
					if ((flagTemp = (Parameter) this.getAttribute(flags[i]
							.getId())) == null)
						throw new IllegalActionException(this,
								"Unable to find the command line parameter (flag): "
										+ flags[i].getId());
					if (((BooleanToken) flagTemp.getToken()).booleanValue())
						str += " " + flags[i].getTag();
				}// for
			}
			if (_appMetadata.getArgParams() != null) {
				// and then the params
				ArgParam[] params = _appMetadata.getArgParams();
				String taggedParams = "";
				String separator = _appMetadata.getSeparator();
				if (separator == null) {
					separator = " ";
				}
				String[] untaggedParams = new String[_appMetadata
						.getNumUnttagedParams()]; // we make an array to contain
													// the untagged params
				for (int i = 0; i < untaggedParams.length; i++)
					// we initialized it to empty
					untaggedParams[i] = "";
				log.info("We have " + _appMetadata.getNumUnttagedParams()
						+ " untaggged parameters.");
				for (int i = 0; i < params.length; i++) {
					log.info("Analizing param: " + params[i].getId());
					Attribute attr = this.getAttribute(params[i].getId());
					if (attr == null)
						throw new IllegalActionException(this,
								"We could not find the attribute "
										+ params[i].getId());
					if (params[i].getTag() != null) {
						// tagged params
						if (attr instanceof FilePortParameter) {
							if (((FilePortParameter) attr).asFile() != null)
								// we have a file!
								taggedParams += " "
										+ params[i].getTag()
										+ separator
										+ ((FilePortParameter) attr).asFile()
												.getName();
						} else if ((((StringParameter) attr).stringValue() != null)
								&& (((StringParameter) attr).stringValue()
										.length() > 0)) // basically the user
														// has entered some text
							taggedParams += " " + params[i].getTag()
									+ separator
									+ ((StringParameter) attr).stringValue();
					} else {
						// untagged parameters
						if (attr instanceof FilePortParameter) {
							if (((FilePortParameter) attr).asFile() != null)
								// we have a file
								untaggedParams[params[i].getPosition()] = " "
										+ ((FilePortParameter) attr).asFile()
												.getName();
						} else if ((((StringParameter) attr).stringValue() != null)
								&& (((StringParameter) attr).stringValue()
										.length() > 0)) { // basically the user
															// has entered some
															// text
							// untagged params this is a bit unreadable!!
							untaggedParams[params[i].getPosition()] = " "
									+ ((StringParameter) attr).stringValue();
							log.info("Adding the " + i
									+ " untagged paramters with: "
									+ untaggedParams[params[i].getPosition()]);
						}// if
					}// else
				}// for
				if (taggedParams.length() > 0)
					str += taggedParams;
				for (int i = 0; i < _appMetadata.getNumUnttagedParams(); i++)
					str += untaggedParams[i];
			}
			log.info("The command line is: " + str);
			return str;

		} else {
			// simple case
			StringParameter commandLine = null;
			try {
				commandLine = (StringParameter) this.getAttribute(
						"commandLine", StringParameter.class);
				log.debug("passing by here with commandLine = " + commandLine
						+ " for server name: " + _serverNameString);
			} catch (IllegalActionException e) {
				throw new IllegalActionException(this,
						"Unable to find the command line parameter: "
								+ e.getMessage());
			}
			return commandLine.getExpression();
		}
	}// _makeCmd


    /**
     *  This function is used to escape illegal character in html
     */
	public static String forXML(String aText) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(
				aText);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '<') {
				result.append("&lt;");
			} else if (character == '>') {
				result.append("&gt;");
			} else if (character == '\"') {
				result.append("&quot;");
			} else if (character == '\'') {
				result.append("&#039;");
			} else if (character == '&') {
				result.append("&amp;");
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}

	private static final Log log = LogFactory
			.getLog(OpalClient.class.getName());
	private AppMetadata _appMetadata = null;
	private String _serverNameString = null;
	private int _numberFiles = 0;
	private String _docHead = "<h1>Opal Client for Kepler.</h1>"
			+ "<p> This actor can be used to access Opal based serivce.  </p>"
			+ "<p>Modify the serviceURL parameter so that it points to a valid Opal Service. "
			+ "If you modify the serviceURL in the properties editor you have to commit and reopen the properties editor.</p>"
			+ "<p>Below there is the documentation of the service pointed by the current URL.</p> ";
}
