/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-27 11:53:36 -0700 (Wed, 27 Mar 2013) $' 
 * '$Revision: 31773 $'
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

package org.kepler;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.util.Version;
import org.kepler.gui.kar.ImportModuleDependenciesAction;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KARFile;
import org.kepler.kar.handlers.ActorMetadataKAREntryHandler;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObjectInterface;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.DotKeplerManager;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.Manager;
import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.ConfigurationApplication;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.ModelDirectory;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.PtolemyPreferences;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.attributes.VersionAttribute;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.KernelException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.moml.MoMLParser;
import ptolemy.moml.filter.BackwardCompatibility;
import ptolemy.util.MessageHandler;
import ptolemy.util.StringUtilities;

/**
 * This class is the startup class for all non-gui applications of kepler. It
 * handles the command line interface and executes the proper base class in
 * ptolemy.
 * 
 * This application supports executing kepler when the -nogui AND -cache command
 * line flags are set.  See org.kepler.Kepler.parseArgsAndRun() to see how
 * this application is launched.  
 *
 * The reason this application needs to exist is so Kepler can startup a 
 * headless runtime with the configuration system in place.  Without the
 * configuration, the cache cannot be started.  This is working around a 
 * limitation in Ptolemy where the GUI generally needs to be instantiated
 * in order to get an instance of Configuration.  This class could possibly
 * go away after the new configuration system comes on line if we can
 * pull the configuration options needed for the cache.
 * 
 * You can execute this class with the following command:
 * ./kepler.sh -runwf -nogui -cache workflow.xml
 *
 * To generate the kepler.sh script, run 'ant startup-script' from the build-area.
 * 
 * @author Chad Berkley, Christopher Brooks, Dan Crawl
 * @version $Id: KeplerConfigurationApplication.java 31773 2013-03-27 18:53:36Z crawl $
 */
public class KeplerConfigurationApplication extends ConfigurationApplication {
	private static final Log log = LogFactory
			.getLog(KeplerConfigurationApplication.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * constructor. @See ptolemy.actor.gui.ConfigurationApplication for more
	 * information.
	 * 
	 * @param args
	 */
	public KeplerConfigurationApplication(String[] args) throws Exception {
		if (isDebugging)
			log.debug("construct: " + args);
		_initializeApplication();
		_basePath = "ptolemy/configs";

		// Create a parser to use.
		_parser = new MoMLParser();

		// We set the list of MoMLFilters to handle Backward Compatibility.
		MoMLParser.setMoMLFilters(BackwardCompatibility.allFilters());

		MessageHandler.setMessageHandler(new /* Graphical */MessageHandler());

		try {
			java.util.Locale.setDefault(java.util.Locale.US);
		} catch (java.security.AccessControlException accessControl) {
			// FIXME: If the application is run under Web Start, then this
			// exception will be thrown.
		}

		try {
			_configuration = readConfiguration(specToURL(args[0]));

			//replace kepler display actors into Discard if they are set in configuration xml.
			_setGUIConfiguration(_configuration, "_keplerDisplayClassesWithRedirect");
			_setGUIConfiguration(_configuration, "_keplerDisplayClassesNoRedirect");

			/*
			List momlFilters = MoMLParser.getMoMLFilters();
			if (momlFilters != null) {
				Iterator filters = momlFilters.iterator();
				while (filters.hasNext()) {
					MoMLFilter filter = (MoMLFilter) filters.next();
					if (filter instanceof RemoveGraphicalClasses) {
						RemoveGraphicalClasses rgc = (RemoveGraphicalClasses) filter;
						// rgc.put("org.geon.BrowserDisplay",
						// "ptolemy.actor.lib.Discard");
						break;
					}
				}
			}
			*/
		
			// initialize the KAR entries
			String fileName = args[args.length - 1];
			if (isDebugging)
				log.debug(fileName);
			
			// parse the command-line arguments
			parseArgs(args);		

			// add special parameters to configuration
			
			if (_server) {
			  // if the server flag is set, kepler is running on a server so
			  // the _server configuration attribute will be set so that other
			  // processes can behave accordingly
			  StringAttribute sa = new StringAttribute(_configuration,
            "_server");
			  sa.setExpression("true");

			}

			if (_repository != null) {
			  StringAttribute sa = new StringAttribute(_configuration,
            "_repository");
			  sa.setExpression(_repository);
			}
			if (_domain != null) {
			  StringAttribute sa = new StringAttribute(_configuration,
            "_domain");
			  sa.setExpression(_domain);
      }
			if (_username != null) {
			  StringAttribute sa = new StringAttribute(_configuration,
            "_username");
			  sa.setExpression(_username);
			}
			if (_password != null) {
			  StringAttribute sa = new StringAttribute(_configuration,
            "_password");
			  sa.setExpression(_password);
			}
			
			// extract the workflow moml from the kar
			
			if (fileName.trim().endsWith(".kar")) {
			    if(isDebugging) {
			        log.debug("Extracting workflow from KAR");
			    }
				String karFileName = fileName;
				File file = new File(karFileName);
				KARFile karf = null;
				try {
					karf = new KARFile(file);

					// see if KAR is openable
					if(!karf.isOpenable()) {
						
						// check dependencies.
						Map<String, Version> missingDeps = karf.getMissingDependencies();
						if(!missingDeps.isEmpty()) {
							
							// print out the missing dependencies
							System.out.println("WARNING: Missing module dependencies:");
							for(Map.Entry<String, Version> entry : missingDeps.entrySet()) {
								System.out.println("   " + entry.getKey());
							}
							
							if(!Kepler.getForceOpen()) {						
								if(Kepler.getRunWithGUI()) {
									ImportModuleDependenciesAction action = new ImportModuleDependenciesAction(new TableauFrame());
									action.setArchiveFile(file);
									ImportModuleDependenciesAction.ImportChoice choice = action.checkDependencies();
									if(choice == ImportModuleDependenciesAction.ImportChoice.DO_NOTHING) {
										_exit();
										return;
									} else if(choice == ImportModuleDependenciesAction.ImportChoice.DOWNLOADING_AND_RESTARTING) {
									    action.waitForDownloadAndRestart();
									}
								} else {
									System.out.println("ERROR: Cannot execute due to missing dependencies. " +
											"Either add missing modules or run with -force.");
									_exit();
									return;
								}
							}
						} else {
							MessageHandler.error("ERROR: this KAR cannot be opened.");
							_exit();
							return;
						}
					}
	
					
					karf.cacheKARContents();
	                if(isDebugging) {
	                    log.debug("Cached KAR contents");
	                }
	                
					// For each Actor in the KAR open the MOML
					for (KAREntry entry : karf.karEntries()) {
						KeplerLSID lsid = entry.getLSID();
						if(isDebugging) {
						    log.debug("Processing entry, LSID=" + lsid + ", type=" + entry.getType());
						}
						if (!ActorMetadataKAREntryHandler.handlesClass(entry.getType())) {
							if(isDebugging) {
							    log.debug("Opening entry, LSID=" + lsid + ", type=" + entry.getType());
							}
							//WARNING - using null TableauFrame here
							karf.open(entry, null);
							continue;
						}
						// get the object from the cache (it is ActorMetadata
						// even though it is a workflow)
						CacheObjectInterface co = CacheManager.getInstance()
								.getObject(lsid);
						
						// make sure we were able to load it
						if(co == null) {
						    MessageHandler.error("ERROR: Could not find LSID for workflow in the Kepler cache.\n" +
						            "Make sure there are no other Kepler instances running " +
						            "and remove " + DotKeplerManager.getDotKeplerPath());
						    _exit();
						    return;
						}
						
						ActorMetadata am = (ActorMetadata) co.getObject();
	
						// get the workflow from the metadata
						NamedObj entity = am.getActorAsNamedObj(null);
	
						// extract MOML to temp file
						File tmpFile = File.createTempFile("moml", ".xml");
						tmpFile.deleteOnExit();
						FileWriter writer = null;
						try {
						    writer = new FileWriter(tmpFile);
	    					entity.exportMoML(writer);
	    					writer.flush();
						} finally {
						    if(writer != null) {
						        writer.close();
						    }
						}
						
						// _openModel here so it's found later. This avoids dropping 
						// listeners added before the next _openModel call.
						// see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5434
						URL url = specToURL(tmpFile.toString());
						if (_configuration != null) {
							ModelDirectory directory = (ModelDirectory) _configuration
									.getEntity("directory");
							if (directory == null) {
								throw new InternalErrorException(
										"No model directory!");
							}
							_openModel(url, url, url.toString());
						}
						
						// set for MOML application to run the workflow
						args[args.length - 1] = tmpFile.getAbsolutePath();
						System.out.println("Running workflow "
								+ tmpFile.getAbsolutePath()
								+ " extracted from kar file " + karFileName);
					}
				} finally {
					if(karf != null) {
						karf.close();
					}
				}
			} else {
			    // the file is not a KAR, so try to read it directly.
			    _readFile(fileName);
			}

			// set any parameters in the workflow that were specified on the
			// command-line.
			_setParameters();
			
			if (_configuration == null) {
				throw new IllegalActionException("No configuration provided.");
			}

			// show the configuration after we've loaded the workflow
			_configuration.showAll();

			// set additional parameters in the configuration
			
			if (_run) {
				StringAttribute sa = (StringAttribute) _configuration
						.getAttribute("_server");
				if (sa != null) {
					String value = sa.getExpression();
					System.out.println("Running in server mode: " + true);
				}

				sa = (StringAttribute) _configuration
						.getAttribute("_repository");
				if (sa != null) {
					String value = sa.getExpression();
					System.out.println("Default save repository is set to: "
							+ _repository);
				}

				if (_printPDF) {
					// Need to set background
					PtolemyPreferences preferences = PtolemyPreferences
							.getPtolemyPreferencesWithinConfiguration(_configuration);
					preferences.backgroundColor
							.setExpression("{1.0, 1.0, 1.0, 1.0}");
				}
				
				// run the workflow
				runModels();

				if (_exit) {
					_exit();
				}
			} else {
				if (_printPDF) {
					_printPDF();
				}
			}

		} catch (Throwable ex) {
			// Make sure that we do not eat the exception if there are
			// problems parsing. For example, "ptolemy -FOO bar bif.xml"
			// will crash if bar is not a variable. Instead, use
			// "ptolemy -FOO \"bar\" bif.xml"
			throwArgsException(ex, args);
		}
	}
	
	/** Start a new thread that calls Kepler.shutdown() and exits the JVM. */
	protected void _exit() {
		// In vergil, this gets called in the
		// swing thread, which hangs everything
		// if we call waitForFinish() directly.
		// So instead, we create a new thread to
		// do it.
		Thread waitThread = new Thread() {
			public void run() {
				waitForFinish();
				if (_printPDF) {
					try {
						_printPDF();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				Kepler.shutdown();
				StringUtilities.exit(0);
			}
		};

		// Note that we start the thread here, which could
		// be risky when we subclass, since the thread will be
		// started before the subclass constructor finishes
		// (FindBugs)
		waitThread.start();
	}

	/**
	 * Parse the command-line arguments.
	 * 
	 * @param args
	 *            The command-line arguments to be parsed.
	 * @exception Exception
	 *                If an argument is not understood or triggers an error.
	 */
	protected void parseArgs(String[] args) throws Exception {

	    _parameterNames.clear();
	    _parameterValues.clear();
	    
	    // NOTE: do not try to parse the last argument since that is
	    // the kar file.
	    // Do not parse the first argument since it is the configuration file.
		for (int i = 1; i < args.length - 1; i++) {
			String arg = args[i];

			if (parseArg(arg) == false) {
				if (arg.trim().startsWith("-")) {
					if (i >= (args.length - 1)) {
						throw new IllegalActionException("Cannot set "
								+ "parameter " + arg + " when no value is "
								+ "given.");
					}

					// Save in case this is a parameter name and value.
					_parameterNames.add(arg.substring(1));
					_parameterValues.add(args[i + 1]);
					i++;
				} else {
					// Unrecognized option.
					throw new IllegalActionException("Unrecognized option: "
							+ arg);
				}
			}
		}

		if (_expectingClass) {
			throw new IllegalActionException("Missing classname.");
		}
	}

	/**
	 * Parse a command-line argument.
	 * 
	 * @param arg
	 *            The command-line argument to be parsed.
	 * @return True if the argument is understood, false otherwise.
	 * @exception Exception
	 *                If something goes wrong.
	 */
	protected boolean parseArg(String arg) throws Exception {
		if (arg.equals("-class")) {
			_expectingClass = true;
		} else if (arg.equals("-exit")) {
			_exit = true;
		} else if (arg.equals("-help")) {
			System.out.println(_usage());

			// NOTE: This means the test suites cannot test -help
			StringUtilities.exit(0);
		} else if (arg.equals("-printPDF")) {
			_printPDF = true;
		} else if (arg.equals("-run")) {
			_run = true;
		} else if (arg.equals("-runThenExit")) {
			_run = true;
			_exit = true;
		} else if (arg.equals("-statistics")) {
			_statistics = true;
		} else if (arg.equals("-server")) {
			_server = true;
		} else if (arg.startsWith("-repository=")) {
			int equalsIndex = arg.indexOf("=");
			if (equalsIndex != -1) {
				_repository = arg.substring(equalsIndex + 1, arg.length());
			} else {
				System.out
						.println("The -repository argument must be followed by '=<repository'");
				StringUtilities.exit(0);
			}
		} else if (arg.startsWith("-domain=")) {
			int equalsIndex = arg.indexOf("=");
			if (equalsIndex != -1) {
				_domain = arg.substring(equalsIndex + 1, arg.length());
			} else {
				System.out
						.println("The -domain argument must be followed by '=<domain>'");
				StringUtilities.exit(0);
			}
		} else if (arg.startsWith("-username=")) {
			int equalsIndex = arg.indexOf("=");
			if (equalsIndex != -1) {
				_username = arg.substring(equalsIndex + 1, arg.length());
			} else {
				System.out
						.println("The -username argument must be followed by '=<username>'");
				StringUtilities.exit(0);
			}
		} else if (arg.startsWith("-password=")) {
			int equalsIndex = arg.indexOf("=");
			if (equalsIndex != -1) {
				_password = arg.substring(equalsIndex + 1, arg.length());
			} else {
				System.out
						.println("The -password argument must be followed by '=<password>'");
				StringUtilities.exit(0);
			}	
		} else if (arg.equals("-test")) {
			_test = true;
		} else if (arg.equals("-version")) {
			System.out
					.println("Version "
							+ VersionAttribute.CURRENT_VERSION.getExpression()
							+ ", Build $Id: KeplerConfigurationApplication.java 31773 2013-03-27 18:53:36Z crawl $");

			// NOTE: This means the test suites cannot test -version
			StringUtilities.exit(0);
		} else if (arg.equals("")) {
			// Ignore blank argument.
		} else {
			if (_expectingClass) {
				// $PTII/bin/ptolemy -class
				// ptolemy.domains.sdf.demo.Butterfly.Butterfly
				// Previous argument was -class
				_expectingClass = false;

				// Create the class.
				Class newClass = Class.forName(arg);

				// Instantiate the specified class in a new workspace.
				Workspace workspace = new Workspace();

				// Workspace workspace = _configuration.workspace();
				// Get the constructor that takes a Workspace argument.
				Class[] argTypes = new Class[1];
				argTypes[0] = workspace.getClass();

				Constructor constructor = newClass.getConstructor(argTypes);

				Object[] args = new Object[1];
				args[0] = workspace;

				NamedObj newModel = (NamedObj) constructor.newInstance(args);

				if (_configuration != null) {
					_openModel(newModel);

				} else {
					System.err.println("No configuration found.");
					throw new IllegalActionException(newModel,
							"No configuration found.");
				}
			} else {
				if (!arg.startsWith("-")) {
				    // Assume the argument is a file name or URL.
			        // Attempt to read it.
				    _readFile(arg);
				} else {
					// Argument not recognized.
					return false;
				}
			}
		}

		return true;
	}
	
	/** Attempt to read a file name. */
	private void _readFile(String fileName) throws Exception {
        
	    URL inURL;

        try {
            inURL = specToURL(fileName);
        } catch (Exception ex) {
            try {
                // Create a File and get the URL so that commands
                // like
                // $PTII/bin/vergil $PTII/doc/index.htm#in_browser
                // work.
                File inFile = new File(fileName);
                inURL = inFile.toURI().toURL();
            } catch (Exception ex2) {
                // FIXME: This is a fall back for relative
                // filenames,
                // I'm not sure if it will ever be called.
                inURL = new URL(new URL("file://./"), fileName);
            }
        }

        // Strangely, the XmlParser does not want as base the
        // directory containing the file, but rather the
        // file itself.
        URL base = inURL;

        // If a configuration has been found, then
        // defer to it to read the model. Otherwise,
        // assume the file is an XML file.
        if (_configuration != null) {
            ModelDirectory directory = (ModelDirectory) _configuration
                    .getEntity("directory");

            if (directory == null) {
                throw new InternalErrorException(
                        "No model directory!");
            }

            String key = inURL.toExternalForm();

            _openModel(base, inURL, key);

        } else {

            _parser.reset();

        }
	}
	
	/** Set any parameters specified by the command-line arguments in
	 *  the model. 
	 */
	private void _setParameters() throws IllegalActionException {
	    
	       // Check saved options to see whether any is setting an attribute.
        Iterator names = _parameterNames.iterator();
        Iterator values = _parameterValues.iterator();

        while (names.hasNext() && values.hasNext()) {
            String name = (String) names.next();
            String value = (String) values.next();

            boolean match = false;
            ModelDirectory directory = (ModelDirectory) _configuration
                    .getEntity("directory");

            if (directory == null) {
                throw new InternalErrorException("No model directory!");
            }

            Iterator effigies = directory.entityList(Effigy.class).iterator();

            while (effigies.hasNext()) {
                Effigy effigy = (Effigy) effigies.next();

                if (effigy instanceof PtolemyEffigy) {
                    NamedObj model = ((PtolemyEffigy) effigy).getModel();

                    // System.out.println("model = " + model.getFullName());
                    Attribute attribute = model.getAttribute(name);

                    if (attribute instanceof Settable) {
                        match = true;

                        // Use a MoMLChangeRequest so that visual rendition (if
                        // any) is updated and listeners are notified.
                        String moml = "<property name=\"" + name
                                + "\" value=\"" + value + "\"/>";
                        MoMLChangeRequest request = new MoMLChangeRequest(this,
                                model, moml);
                        model.requestChange(request);
                    }

                    if (model instanceof CompositeActor) {
                        Director director = ((CompositeActor) model)
                                .getDirector();

                        if (director != null) {
                            attribute = director.getAttribute(name);

                            if (attribute instanceof Settable) {
                                match = true;

                                // Use a MoMLChangeRequest so that visual
                                // rendition (if
                                // any) is updated and listeners are notified.
                                String moml = "<property name=\"" + name
                                        + "\" value=\"" + value + "\"/>";
                                MoMLChangeRequest request = new MoMLChangeRequest(
                                        this, director, moml);
                                director.requestChange(request);
                            }
                        }
                    }
                }
            }

            if (!match) {
                // Unrecognized option.
                throw new IllegalActionException("Unrecognized option: "
                        + "No parameter exists with name " + name);
            }
        }
	}

	/** Print a stack trace of the error. */
    public synchronized void executionError(Manager manager, Throwable throwable) {
        MessageHandler.error("Command failed.", throwable);
        super.executionError(manager, throwable);
    }
    
	/**
	 * main method
	 */
	public static void main(String[] args) {
		try {
			new KeplerConfigurationApplication(args);
		} catch (Throwable throwable) {
			MessageHandler.error("Command failed", throwable);
			// Be sure to print the stack trace so that
			// "$PTII/bin/moml -foo" prints something.
			System.err.print(KernelException.stackTraceToString(throwable));
			System.exit(1);
		}
	}
	
	// ////////////////////////////////////////////////////////////////////
	// // private methods ////

	/** read xml and set GUI filter based on classes set in xml file 
	 * @throws IllegalActionException */
	private static void _setGUIConfiguration(Configuration configuration, String attName) throws IllegalActionException {
		Attribute displayAtt = configuration.getAttribute(attName);
		if (displayAtt != null)
		{
			String displayValue = ((StringParameter)displayAtt).stringValue();
			String[] displayArray = displayValue.split(",");
			ptolemy.moml.filter.ClassChanges changes = new ptolemy.moml.filter.ClassChanges();
			for (String displayClass : displayArray) {
				changes.put(displayClass, "ptolemy.actor.lib.Discard");
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	// Flag indicating that the previous argument was -class.
	private boolean _expectingClass = false;

	// List of parameter names seen on the command line.
	private List _parameterNames = new LinkedList();

	// List of parameter values seen on the command line.
	private List _parameterValues = new LinkedList();

	// URL from which the configuration was read.
	private static URL _initialSpecificationURL;

	private boolean _server = false;

	private String _repository;
	
	private String _domain;
	
	private String _username;
	
	private String _password;
}