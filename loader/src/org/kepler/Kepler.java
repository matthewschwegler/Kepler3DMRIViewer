/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-06-28 10:42:49 -0700 (Fri, 28 Jun 2013) $' 
 * '$Revision: 32178 $'
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
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.kepler.build.MakeKars;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.gui.SplashWindow;
import org.kepler.loader.PermissionManager;
import org.kepler.loader.SystemPropertyLoader;
import org.kepler.module.ModuleHSQLManager;
import org.kepler.module.ModuleInitializer;
import org.kepler.module.ModuleShutdownable;
import org.kepler.modulemanager.gui.patch.PatchChecker;
import org.kepler.moml.NamedObjId;
import org.kepler.sms.util.OntologyConfiguration;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.FileUtil;
import org.kepler.util.ShutdownListener;
import org.kepler.util.ShutdownNotifier;

import ptolemy.actor.injection.ActorModuleInitializer.Initializer;
import ptolemy.actor.injection.PtolemyInjector;
import ptolemy.actor.injection.PtolemyModule;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.moml.filter.BackwardCompatibility;
import ptolemy.util.FileUtilities;
import ptolemy.vergil.VergilApplication;

/**
 * A class to initialize the build system, then start the Kepler GUI or execute
 * workflows from the command line. In the latter case, command line arguments
 * specify different run time configurations.
 * 
 * @author Chad Berkely, Daniel Crawl, David Welker
 * @version $Id: Kepler.java 32178 2013-06-28 17:42:49Z crawl $
 */

public class Kepler {

	public static void restart() {
		main(_args);
	}

	public static void main(String[] args) {
	    
	    //System.out.println("java.library.path = " + System.getProperty("java.library.path"));

        // log4j uses the first log4j.properties file found on the classpath. 
        // since kepler's classpath is complicated, it is not always obvious
        // which log4j.properties is used; it may be hidden in a jar file.
        // this prints the log4j.properties file found on the classpath.
        File log4jFile = FileUtilities.nameToFile("$CLASSPATH/log4j.properties", null);
        if(log4jFile == null) {
            System.out.println("log4j.properties not found in CLASSPATH.");
        } else {
            System.out.println("log4j.properties found in CLASSPATH: " + log4jFile);
        }

	    
		// Save the args in case they are needed later.
		// long startTime = System.currentTimeMillis();
		_args = args;
		CommandLineArgs.store(args);
		List<String> argList = Arrays.asList(args);
		
		// parse the command line arguments
		if(!parseArgs(args)) {
		    // an error occurred, so exit.
		    return;
		}

		RepositoryLocations
				.setReleaseLocation(org.kepler.modulemanager.RepositoryLocations
						.getReleaseLocation());
		ModuleTree.init();
		
		// if we are executing a workflow from the command line, check now for
		// patches.
		if(_action == Action.RunKAR || _action == Action.RunWf) {
		    // if we are running headless, do not display a dialog if patches
		    // are available
		    if (!_runWithGui || _displayRedirectOutputPath != null) {
		        PatchChecker.check(true);
		    } else {
		        PatchChecker.check(false);
		    }
		// show the splash if the Kepler UI is starting
		} else if(_action == Action.Kepler && _showSplash) {
		    _showSplash();
		}
		
		System.gc();
		ShutdownNotifier.addShutdownListener(new Shutdown());
		try {
		    setJavaPropertiesAndCopyModuleDirectories();
		    
			// System.out.println("os: " + System.getProperty("os.name"));
			String OSName = System.getProperty("os.name");

			// Hashtable properties = getProject().getProperties();
			ModuleTree moduleTree = ModuleTree.instance();
			
			String classpath = System.getProperty("java.class.path");
			String[] classpathItems = classpath.split(File.pathSeparator);
			
			for (Module module : moduleTree) {
				
				// XXX since Module dir variables can be wrong, utilizing classpath
				// to determine module location on disk. Use below line instead when 
				// that's fixed.
				//File osextension = new File(module.getModuleInfoDir() + File.separator + "osextension.txt");
				File osextension = null;
				String sought = File.separator + module.getName() + File.separator;				
				for (String path: classpathItems){
					// must check each match since parent path could possibly have a module name
					// in it (don't break on first):
					if (path.contains(sought)){
						int lastIndex = path.lastIndexOf(sought);
						String p = path.substring(0, lastIndex);
						p = p.concat(sought + "module-info" + 
							File.separator + "osextension.txt");
						osextension = new File(p);
						if (osextension.exists()){
							break;
						}
					}
				}
				if (osextension == null || !osextension.exists()){
					continue;
				}
				
				System.out.println("Found OS Extension file: "
						+ osextension.getAbsolutePath());
				Hashtable<String, String> properties = readOSExtensionFile(osextension);
				Enumeration<String> keys = properties.keys();
				while (keys.hasMoreElements()) {
					String extClass = keys.nextElement();
					String os = properties.get(extClass);
					if (OSName.trim().equals(os.trim())) {
						// if we're in an OS that an extension
						// needs to be loaded for attempt to load
						// the OSExtension via reflection
						// and run the addOSExtension method

						Class<?> c = Class.forName(extClass);
						try {
							OSExtension extension = (OSExtension) c
									.newInstance();
							extension.addOSExtensions();
							System.out.println("loading OS extensions for OS "
									+ os + " with class " + extClass);
						} catch (ClassCastException cce) {
							// System.out.println(extClass +
							// " is not an instance of OSExtension");
						}
					}
				}
			}

			Project project = new Project();
			File projDir = ProjectLocator.getProjectDir();
			project.setBaseDir(projDir);
			setOntologyIndexFile();

			
			if (!argList.contains("-runwf")) {
				// Allow developers to turn off MakeKars by creating
				// a file called "skipMakeKars" in the project root
				File skipMakeKars = new File(projDir, "skipMakeKars");
				if (!skipMakeKars.exists()) {
					MakeKars kar = new MakeKars();
					kar.setProject(project);
					kar.init();
					kar.run();
				}
			}

			PermissionManager.makeNativeLibsExecutable();
			// CreateIntroFileTask createIntroFileTask = new
			// CreateIntroFileTask();
			// createIntroFileTask.execute();

		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		runApplication();
		// System.exit(0);

		// long endTime = System.currentTimeMillis();
		// System.out.println( (endTime-startTime)/1000.0 );
	}

	public static void setOntologyIndexFile() {
		OntologyConfiguration oc = OntologyConfiguration.instance();
		File f = FileUtil
				.getHighestRankedFile("configs/ptolemy/configs/kepler/ontologies/ontology_catalog.xml");
		oc.setIndexFile(f);
		oc.initialize();
	}
    
	/**
	 * Parse the command line arguments and run the appropriate application.
	 * @return If true, no error occurred parsing the arguments.
	 */
	public static boolean parseArgs(String[] args) {

		try {
			// parse the switches and remaining arguments
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-runwf")) {
					_action = Action.RunWf;
				} else if (args[i].equals("-runkar")) {
					_action = Action.RunKAR;
				} else if (args[i].equals("-nogui")) {
					_assertRunWF(args[i]);
					_runWithGui = false;
				} else if (args[i].equals("-nocache")) {
					_assertRunWF(args[i]);
					_runWithCache = false;
				} else if (args[i].equals("-nosplash")) {
					_showSplash = false;
				} else if (args[i].equals("-gui")) {
					_assertRunWF(args[i]);
					_runWithGui = true;
				} else if (args[i].equals("-cache")) {
					_assertRunWF(args[i]);
					_runWithCache = true;
				} else if (args[i].equals("-vergil")) {
					_action = Action.Vergil;
                } else if (args[i].equals("-createActorXML")) {
                    _action = Action.CreateActorXML;
				} else if (args[i].equals("-hsql")) {
				    if(_action != Action.Kepler) {
				        throw new IllegalArgumentException("ERROR: -hsql may not be used with " +
				            _action.getArg());
				    }
				    
				    i++;
				    if(i == args.length) {
                        throw new IllegalArgumentException("ERROR: must specify -hsql start or -hsql stop.");				        
				    }
				    
				    if(args[i].equals("start")) {
	                    _action = Action.HSQLStart;				        
				    } else if(args[i].equals("stop")) {
	                    _action = Action.HSQLStop;
				    } else {
				        throw new IllegalArgumentException("ERROR: must specify -hsql start or -hsql stop.");
				    }
				} else if (args[i].equals("-force")) {
					_forceOpen = true;
                } else if (args[i].equals("-noilwc")) {
                    NamedObjId.incrementLSIDOnWorkflowChange(false);
                } else if (args[i].equals("-updateActorDocs")) {
                	_action = Action.UpdateActorDocs;
                } else if (args[i].equals("-h") || args[i].equals("-help")) {
					_showHelp();
					return false;
				}
                // NOTE: -redirectdisplay is kept for backwards-compatibility 
                else if(args[i].equals("-redirectgui") || args[i].equals("-redirectdisplay")){
					_assertRunWF(args[i]);
	                _runWithGui = false;
            		if (i >= args.length) {
            			throw new IllegalArgumentException("ERROR: cannot set "
                            		+ "redirectgui argument because no redirect dir for display actors is "
                            	+ "given.");
            		}
            		_displayRedirectOutputPath = new String(args[++i]);
            		if (!(new File(_displayRedirectOutputPath)).isDirectory()){
            			throw new IllegalArgumentException("ERROR: cannot set "
                        		+ "redirectdisplay argument because the argument after '-redirectdisplay' is not a directory.");
            		}
            	} else {
					_applicationArgsList.add(args[i]);
				}
			}
		} catch (IllegalArgumentException e) {
			System.out.print(e.getMessage());
			return false;
		}
		
		return true;
		
	}
	
	/** Run the appropriate application based on command line arguments. */
	public static int runApplication() {
	    
		try {
			// see if we're running the gui editor
			if (_action == Action.Kepler) {
				addActorModule("org/kepler/ActorModuleDefault");
				initialize();
				_applicationArgsList.addFirst("-kepler");
				String[] loadArgs = _applicationArgsList.toArray(new String[0]);
				if(_showSplash) {
					SplashWindow.invokeMain("ptolemy.vergil.VergilApplication", loadArgs);
			        SplashWindow.disposeSplash();
				} else {
					VergilApplication.main(loadArgs);
				}
				
				// check for patches now that the UI has started.
				PatchChecker.check(false);
				
			} else if (_action == Action.Vergil) {
				addActorModule("org/kepler/ActorModuleDefault");
				String[] loadArgs = _applicationArgsList.toArray(new String[0]);
				VergilApplication.main(loadArgs);
			} else if (_action == Action.RunWf || _action == Action.RunKAR) {
				String className = null;
				
				// if we are running a workflow from the cmd line and have
				// the gui enabled, the program does not quit until the user
				// enters control-c.
				if(_runWithGui) {
					_mustManuallyQuit = true;
				}
				addActorModule("org/kepler/ActorModuleDefault");
				if (_runWithGui && _runWithCache) {
					if(_action == Action.RunKAR) {
						if (_applicationArgsList.getLast().endsWith(".xml")) {
	            			throw new IllegalArgumentException("ERROR: -runkar "
                            		+ "can only be used to run kar file, not xml file."
	            					+ "to run xml file, please use -runwf option");
						}
						className = "org.kepler.KeplerConfigurationApplication";
						_applicationArgsList.addFirst("-run");
						_applicationArgsList.addFirst("ptolemy/configs/kepler/ConfigGUIAndCache.xml");
					} else { // _action == Action.RunWf
						if (_applicationArgsList.getLast().endsWith(".kar")) {
	            			throw new IllegalArgumentException("ERROR: -runwf "
                            		+ "can only be used to run xml file, not kar file."
	            					+ " To run kar file, please use -runkar option");
						}
						className = "ptolemy.actor.gui.PtExecuteApplication";
						_applicationArgsList.addFirst("ptolemy/configs/kepler/ConfigGUIAndCache.xml");
						_applicationArgsList.addFirst("-conf");
					}
				} else if (_runWithGui && !_runWithCache) {
					if (_applicationArgsList.getLast().endsWith(".kar")) {
            			throw new IllegalArgumentException("ERROR: -runkar "
                        		+ "cannot be used with -nocache option."
            					+ " Please remove nocache option and try again.");
					}
					className = "ptolemy.actor.gui.PtExecuteApplication";
					_applicationArgsList.addFirst("ptolemy/configs/kepler/ConfigGUINoCache.xml");
					_applicationArgsList.addFirst("-conf");
				} else if (!_runWithGui && _runWithCache) {
					String spec;
					if (_displayRedirectOutputPath != null){
						//add display redirection filter
						BackwardCompatibility.addFilter(new au.edu.jcu.kepler.hydrant.DisplayRedirectFilter(_displayRedirectOutputPath));
						addActorModule("org/kepler/ActorModuleBatch");
						spec = "ptolemy/configs/kepler/ConfigRedirectGUIWithCache.xml";
					} else {
						spec = "ptolemy/configs/kepler/ConfigNoGUIWithCache.xml";
					}
					className = "org.kepler.KeplerConfigurationApplication";
					_applicationArgsList.addFirst("-runThenExit");
					_applicationArgsList.addFirst(spec);
					// ConfigurationApplication.readConfiguration(ConfigurationApplication.specToURL(spec));
				} else { // if(!gui && !cache)
					if (_applicationArgsList.getLast().endsWith(".kar")) {
            			throw new IllegalArgumentException("ERROR: -runkar "
                        		+ "cannot be used with -nocache option."
            					+ " Please remove '-nocache' option and try again.");
					}
					String spec;
	         		if (_displayRedirectOutputPath != null)	         			
	            		//enter into display redirection mode
	            		{
	            			BackwardCompatibility.addFilter(
	            			        new au.edu.jcu.kepler.hydrant.DisplayRedirectFilter(_displayRedirectOutputPath));
	            			addActorModule("org/kepler/ActorModuleBatch");
//	            			DisplayRediectClassChanges.classChanges();
//	            			BackwardCompatibility.addFilter(new DisplayRedirectFilter(outputPath));	    					
	    					spec = "ptolemy/configs/kepler/ConfigRedirectGUINoCache.xml";

	            		} else {
	            			spec = "ptolemy/configs/kepler/ConfigNoGUINoCache.xml";
	            		}
	         			className = "org.kepler.KeplerConfigurationApplication";
    					_applicationArgsList.addFirst("-runThenExit");
    					_applicationArgsList.addFirst(spec);
//						else{
//							className = "ptolemy.moml.MoMLCommandLineApplication";
//						}
				}

				if (className != null) {
					// initialize and run the class.
					String[] loadArgs = _applicationArgsList.toArray(new String[0]);
					load(className, loadArgs);
				}
            } else if (_action == Action.CreateActorXML) {
                String[] loadArgs = _applicationArgsList.toArray(new String[0]);
                load("org.kepler.loader.util.UpdateActorTreeFiles", "buildXMLs", loadArgs);
            } else if (_action == Action.UpdateActorDocs) {
            	String[] loadArgs = _applicationArgsList.toArray(new String[0]);
            	load("org.kepler.loader.util.UpdateActorTreeFiles", "updateKarXMLDocsForFile", loadArgs);
			} else if (_action == Action.HSQLStart) {
			    System.out.println("going to start hsql servers.");
                _runHSQLServers(true);
			} else if (_action == Action.HSQLStop) {
                System.out.println("going to stop hsql servers.");		
                _runHSQLServers(false);
			}
		} catch (IllegalArgumentException e) {
			System.out.print(e.getMessage());
			return 1;
		} catch (Exception e) {
			System.out.println(e.getClass() + ": " + e.getMessage());
			e.printStackTrace();
			return 1;
		}

		if (_exitAfterRun) {
			System.exit(0);
		}
		return 0;
	}

	/** Show the splash screen and start the kepler GUI. */
	private static void _showSplash() {
		try {

			ConfigurationProperty commonProperty = ConfigurationManager
					.getInstance().getProperty(
							ConfigurationManager.getModule("common"));
			ConfigurationProperty splashscreenProp = commonProperty
					.getProperty("splash.image");
			String splashname = splashscreenProp.getValue();

			final URL splashURL = ClassLoader.getSystemClassLoader()
					.getResource(splashname);

			SplashWindow.splash(splashURL);
			
		} catch (Exception ex) {
			System.err.println("Failed to find splash screen image."
					+ "Ignoring, use the Java coffee cup");
			ex.printStackTrace();
		}
	}

	/**
	 * This method provides a generic loader. It first updates the classpath
	 * with all the jars in $KEPLER/lib/jar, and then uses reflection to invoke
	 * the method. NOTE: the method must be static.
	 */
	public static void load(String className, String methodName, String[] args)
			throws IllegalActionException {
		// sanity checks
		if (className == null || className.equals("")) {
			throw new IllegalActionException("Must supply class to load.");
		} else if (methodName == null || methodName.equals("")) {
			throw new IllegalActionException("Must supply method to invoke.");
		}

		try {
			initialize();

			if (className.equals("ptolemy.actor.gui.PtExecuteApplication")) {
				ptolemy.actor.gui.PtExecuteApplication application = new ptolemy.actor.gui.PtExecuteApplication(
						args);
				application.runModels();
				application.waitForFinish();
			} else if(className.equals("org.kepler.KeplerConfigurationApplication")) {
				KeplerConfigurationApplication application = new KeplerConfigurationApplication(args);
				application.waitForFinish();
			} else {
				System.out.print("loading: " + className + " args: ");
				for (String arg : args) {
					System.out.print(arg + " ");
				}
				System.out.println();

				// invoke the class's method
				Class<?> cl = Class.forName(className);
				Method mthd = cl.getMethod(methodName, String[].class);
				System.out.println("invoking: " + className + "." + methodName);
				mthd.invoke(null, new Object[] { args });
			}
						
			if (_mustManuallyQuit) {
				System.out.println("Done.");
				System.out.println("Ctrl-c to exit.");

			}
			
			// clean up modules unless we ran KeplerConfigurationApplication
			// since that class calls shutdown() after the models have been
			// executed.
			if(!className.equals("org.kepler.KeplerConfigurationApplication")) {
			    _initializeModules(false);
			}

		} catch (ClassNotFoundException e) {
			System.err.println("ERROR: could not find start-up class: "
					+ className);
		} catch (NoSuchMethodException e) {
			System.err.println("ERROR: class " + className
					+ " does not have a method called " + methodName);
		} catch (Throwable throwable) {
			System.err.println("Unable to start application.");
			throwable.printStackTrace();
		}
	}

	/** Load classes and invoke "main" method. */
	public static void load(String className, String[] args)
			throws IllegalActionException {
		load(className, "main", args);
	}

	/** Perform initialization. */
	public static void initialize() throws Exception {
		if (!_haveInitialized) {
			_initializeModules(true);
		}
	}
	
	/** Perform module cleanup. */
	public static void shutdown() {
	    _initializeModules(false);
	}

	/** Returns true if -force was specified on the command line. */
	public static boolean getForceOpen() {
		return _forceOpen;
	}

	/** Returns true if -nogui was specified on the command line. */
	public static boolean getRunWithGUI() {
		return _runWithGui;
	}

	/** Set Kepler java properties and copy the module directories into KeplerData/. */
	public static void setJavaPropertiesAndCopyModuleDirectories() throws URISyntaxException, IOException {
        
	    SystemPropertyLoader.load();
        System.setProperty("KEPLER", ProjectLocator.getProjectDir()
                .getAbsolutePath());
        String persistentDir = DotKeplerManager.getInstance()
                .getPersistentDirString();
        System.setProperty("KEPLERDATA", persistentDir);

        String dotKepler = DotKeplerManager.getDotKeplerPath();
        System.setProperty(".kepler", dotKepler);

        String keplerUserData = DotKeplerManager.getInstance()
                .getPersistentUserDataDirString();
        System.setProperty("KEPLERUSERDATA", keplerUserData);

        String personalModuleWorkflowDirStr = DotKeplerManager
                .getInstance().getPersistentModuleWorkflowsDirString();
        String docDirStr = DotKeplerManager.getInstance()
                .getPersistentDocumentationDirString();

        List<Module> modules = ModuleTree.instance().getModuleList();
        Iterator<Module> moduleItr = modules.iterator();
        while (moduleItr.hasNext()) {
            Module m = moduleItr.next();
            File applicationModuleWorkflowsDir = m.getWorkflowsDir();
            File applicationModuleDocDir = m.getDocumentationDir();
            // we want the full name, use getName not getStemName:
            File moduleWorkflowDir = new File(personalModuleWorkflowDirStr
                    + File.separator + m.getName());
            File personalModuleDocDir = new File(docDirStr + File.separator
                    + m.getName());

            System.setProperty(m.getStemName() + ".workflowdir",
                    moduleWorkflowDir.toString() + File.separator);

            if (applicationModuleWorkflowsDir.exists()) {
                FileUtil.copyDirectory(applicationModuleWorkflowsDir,
                        moduleWorkflowDir, false);
            } else {
                log.debug(m + " workflow dir does not exist.");
            }

            if (applicationModuleDocDir.exists()) {
                if (!personalModuleDocDir.exists()) {
                    log.warn(personalModuleDocDir + " does not exist.");
                    log.warn("copy(" + applicationModuleDocDir + ","
                            + personalModuleDocDir + ")");
                    FileUtil.copyDirectory(applicationModuleDocDir,
                            personalModuleDocDir, false);
                }
            }
        
        }

	    
	}
	
	// ////////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Make sure that -runwf was specified. */
	private static void _assertRunWF(String arg)
			throws IllegalArgumentException {
		if (_action != Action.RunWf && _action != Action.RunKAR) {
			throw new IllegalArgumentException("ERROR: " + arg
					+ " may only be used with -runwf");
		}
	}

	/** Run any module initializers.
	 * @param start if true, run module start initializations. otherwise,
	 * perform module cleanup. 
	 */
	private static void _initializeModules(boolean start) {
		ModuleTree tree = ModuleTree.instance();

		Iterable<Module> moduleList = null;
		String className;
		
		// on startup, call initializers starting at lowest dependency,
		// i.e., at the bottom of modules.txt.
		// on shutdown, start at top of modules.txt
		if(start) {
		    moduleList = tree.reverse();
		    className = "Initialize";
		} else {
		    moduleList = tree;
		    className = "Shutdown";
		}
		
		for (Module module : moduleList) {
			String name = module.getName();
			name = module.getStemName();

			// construct the class name
			if (name.indexOf("-") != -1) { // dashes are illegal characters in
											// package names so we need to
											// remove them
				name = name.replaceAll("-", "");
			}
			String fullClassName = "org.kepler.module." + name + "." + className;
			//System.out.println("looking for class: " + className);

			try {
				// attempt to find and instantiate it
				Class<?> moduleClass = Class.forName(fullClassName);
				
                // call the initializer
                if (start) {
                    ModuleInitializer initializer = (ModuleInitializer) moduleClass
                            .newInstance();
                    initializer.initializeModule();
                    if(_isDebugging) {
                        log.debug("Ran additional initialization for module "
                            + name + " from class " + fullClassName);
                    }
                } else {
                    ModuleShutdownable shutdownable = (ModuleShutdownable) moduleClass
                            .newInstance();
                    shutdownable.shutdownModule();
                    if(_isDebugging) {
                        log.debug("Ran additional cleanup for module "
                            + name + " from class " + fullClassName);
                    }
                }
				
			} catch (ClassNotFoundException e) {
				// it's not required that every module have an initializer.
				// System.out.println("initializer class not found for " +
				// module);
			} catch (InstantiationException e) {
				System.out.println("ERROR instantiating " + fullClassName + ": "
						+ e.getMessage());
			} catch (IllegalAccessException e) {
				System.out.println(e.getMessage());
			}
		}

        // update whether we initialized or shut down.
        _haveInitialized = start;
	}

    /**
     * read the osextension.txt file and return a hashtable of the properties
     * 
     * NOTE this method is duplicated in CompileModules.java. Change both if you
     * change one.
     */
    private static Hashtable<String, String> readOSExtensionFile(File f)
            throws Exception {
        // String newline = System.getProperty("line.separator");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        FileReader fr = new FileReader(f);
        StringBuffer sb = new StringBuffer();
        char[] c = new char[1024];
        int numread = fr.read(c, 0, 1024);
        while (numread != -1) {
            sb.append(c, 0, numread);
            numread = fr.read(c, 0, 1024);
        }
        fr.close();
    
        String propertiesStr = sb.toString();
        // String[] props = propertiesStr.split(newline);
        String[] props = propertiesStr.split(";");
        for (int i = 0; i < props.length; i++) {
            String token1 = props[i];
            StringTokenizer st2 = new StringTokenizer(token1, ",");
            String key = st2.nextToken();
            String val = st2.nextToken();
            properties.put(key, val);
        }
    
        return properties;
    }   

    /** Start or stop any module HSQL servers. */
    private static void _runHSQLServers(boolean start) {
        
        org.kepler.util.sql.HSQL.setForkServers(true);              

        ModuleTree tree = ModuleTree.instance();

        Iterable<Module> moduleList = null;
        
        // if start, call at lowest dependency, i.e., at the bottom of modules.txt.
        // if stop, call at top of modules.txt
        if(start) {
            moduleList = tree.reverse();
        } else {
            moduleList = tree;
        }
        
        for (Module module : moduleList) {
            String name = module.getName();
            name = module.getStemName();

            // construct the class name
            if (name.indexOf("-") != -1) { // dashes are illegal characters in
                                            // package names so we need to
                                            // remove them
                name = name.replaceAll("-", "");
            }
            String fullClassName = "org.kepler.module." + name + ".HSQLManager";
            //System.out.println("looking for class: " + className);

            try {
                // attempt to find and instantiate it
                Class<?> moduleClass = Class.forName(fullClassName);
                ModuleHSQLManager manager = (ModuleHSQLManager) moduleClass.newInstance();
                
                if (start) {
                    manager.start();
                } else {
                    manager.stop();
                }
                
            } catch (ClassNotFoundException e) {
                // it's not required that every module have an hsql manager.
            } catch (InstantiationException e) {
                System.out.println("ERROR instantiating " + fullClassName + ": "
                        + e.getMessage());
            } catch (IllegalAccessException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /** Add the display related actor implementation information, different ActorModule properties 
     * file specify different display implementation.
     * The two ActorModule files in Kepler are located at display-redirect/src/org/kepler/.
     * The ActorModule file in ptolemy is located at ptolemy/src/ptolemy/actor/.
     * More info can be found at ptolemy.actor.injection.ActorModuleInitializer 
     */
    private static void addActorModule(String bundleFile){
		final ArrayList<PtolemyModule> actorModules = new ArrayList<PtolemyModule>();
	    actorModules.add(new PtolemyModule(ResourceBundle
	                .getBundle(bundleFile)));
		Initializer _defaultInitializer = new Initializer() {
	        public void initialize() {
	            PtolemyInjector.createInjector(actorModules);
	        }
		};
		ptolemy.actor.injection.ActorModuleInitializer.setInitializer(_defaultInitializer);
    }
    
	/** Print usage and exit. */
	private static void _showHelp() {
        System.out.println("USAGE:");
        System.out.println();
        System.out.println("To run the Kepler GUI:");
        System.out.println("kepler [-nosplash] [workflow.xml | workflow.kar]");
        System.out.println("-nosplash               start without showing splash screen.");
        System.out.println();
        System.out.println("To run the Ptolemy GUI:");
        System.out.println("kepler -vergil [workflow.xml]");
        System.out.println();
        System.out.println("To run a workflow XML from the command line:");
        System.out.println("kepler -runwf [-nogui | -redirectgui dir] [-nocache] [-noilwc] "
                + "[-param1 value1 ...] workflow.xml");
        System.out.println("-nogui                  run without GUI support.");
        System.out.println("-nocache                run without kepler cache.");
        System.out.println("-noilwc                 run without incrementing LSIDs when the workflow changes.");
        System.out.println("-redirectgui dir        redirect the contents of GUI actors to the specified directory.");
        System.out.println();
        System.out.println("To run a workflow KAR from the command line:");
        System.out.println("kepler -runkar [-nogui | -redirectgui dir] [-force] [-param1 value1 ...] workflow.kar");
        System.out.println("-force                  attempt to run ignoring missing module dependencies.");
        System.out.println("-nogui                  run without GUI support.");
        System.out.println("-redirectgui dir        redirect the contents of GUI actors to the specified directory.");
        System.out.println();
        System.out.println("To start or stop the HSQL database servers:");
        System.out.println("kepler -hsql start");
        System.out.println("kepler -hsql stop");
        System.out.println();
        System.out.println("The following options are for actor developers:");
        System.out.println();
        System.out.println("To create XML file(s) describing an actor using either the source file or class name:");
        System.out.println("kepler -createActorXML file1.java|class1 [file2.java|class2 ...]");
        System.out.println();
        System.out.println("To update the documentation for an item in the actor tree:");
        System.out.println("kepler -updateActorDocs file1.xml [file2.xml ...]");
	}

	////////////////////////////////////////////////////////////////////
    // private classes                                              ////

	/** A class that implements ShutdownListener. The class 
	 *  org.kepler.Kepler cannot be used since it is never
	 *  instantiated.
	 */
	private static class Shutdown implements ShutdownListener
	{
        /** Perform any module cleanup tasks. */
        public void shutdown() {
            Kepler._initializeModules(false);
        }
	}
	
	////////////////////////////////////////////////////////////////////
	// private variables                                            ////

	/** The types of actions. */
	private enum Action {
		Kepler(""),
		RunWf("-runwf"),
		RunKAR("-runkar"),
        CreateActorXML("-createActorXML"),
		Vergil("-vergil"),
		HSQLStart("-hsql"),
		HSQLStop("-hsql"),
		UpdateActorDocs("-updateActorDocs");
		
		Action(String arg) {
		    _arg = arg;
		}
		
		public String getArg() {
		    return _arg;
		}
		
		private String _arg;
	};

	/** The action to perform. By default, start kepler GUI. */
	private static Action _action = Action.Kepler;

	/** Boolean to see if we've initialized. */
	private static boolean _haveInitialized = false;

	/** A copy of the command line arguments. */
	private static String[] _args;

	/** Logging. */
	private final static Log log = LogFactory.getLog(Kepler.class);
	
	/** True if log level is set to DEBUG. */
	private final static boolean _isDebugging = log.isDebugEnabled();
	
	/** If true, the program must be manually quit by the user. */
	private static boolean _mustManuallyQuit = false;

	/** If true, -nogui was specified on the command line. */
	private static boolean _runWithGui = true;
	
	/** If true, start the Kepler cache. If -nocache specified on
	 *  the command line, this is false.
	 */
    private static boolean _runWithCache = true;
    
    /** If true, call System.exit() after running the application. */
    private static boolean _exitAfterRun = false;
    
    /** If true, show the splash screen. */
    private static boolean _showSplash = true;
    
    /** The output path for redirectdisplay option. */
    private static String _displayRedirectOutputPath = null;

	/** If true, -force was specified on the command line. */
	private static boolean _forceOpen = false;
	
    private static LinkedList<String> _applicationArgsList = new LinkedList<String>();

}
