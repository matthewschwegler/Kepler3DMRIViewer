/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-07-21 13:34:17 -0700 (Mon, 21 Jul 2014) $' 
 * '$Revision: 32850 $'
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
package org.kepler.build;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RunClasspath;

/** A task to execute the workflows in modules' test/workflows directory,
 *  and load workflows in workflows/demos. The output from checking these
 *  workflows is printed to stdout. Additionally, XML files are created in
 *  build-area/test that summarize the test results. The format of the XML
 *  files is based on the ant junit task, which can be used in Hudson.
 *  
 *  @author Daniel Crawl
 *  @version $Id: RunTestWorkflows.java 32850 2014-07-21 20:34:17Z crawl $
 */
public class RunTestWorkflows extends ModulesTask {

    /** Run the task. */
    @Override
    public void run() throws Exception {
        
        // TODO this is somewhat of a hack, but i don't know how
        // else to set the classpath: execute main() in a separate JVM.
        
        Java java = new Java();
        java.bindToOwner(this);
        java.init();
        java.setFork(true);
        java.setSpawn(false);

        RunClasspath runClasspath = new RunClasspath();
        java.setClasspath(runClasspath);

        // set the class to be this one
        java.setClassname(getClass().getName());
        
        if(_moduleName != null && !_moduleName.equals("undefined")) {
            java.createArg().setLine(_moduleName);
        }
        
        // pass command line properties 
        java.createJvmarg().setLine("-Dfastfail=" + Boolean.toString(_fastFail));        
        java.createJvmarg().setLine("-DtestActors=" + Boolean.toString(_testActors));
        java.createJvmarg().setLine("-DtestLoadDemos=" + Boolean.toString(_testLoadDemos));
        java.createJvmarg().setLine("-Dtimeout=" + _testTimeout);
        
        //System.out.println(java);
        //System.out.println(java.getCommandLine());
        
        java.execute();
    }
    
    /** Execute the test workflows.
     * 
     *  @param argv a list of modules to test
     */
    public static void main(String[] argv) {

    	try {
    		
    		// set the ant project; otherwise DirectoryScanner has an exception
            Project project = new Project();
            project.setBaseDir(ProjectLocator.getKeplerModulesDir());
            ProjectLocator.setAntProject(project);

	        final RunTestWorkflows runTestWorkflows = new RunTestWorkflows();
	        
	        // set the command line properties
	        
	        final String fastFailStr = System.getProperty("fastfail");
	        if(fastFailStr != null) {
	            runTestWorkflows.setFastfail(fastFailStr);
	        }
	
           final String testActorsStr = System.getProperty("testActors");
            if(testActorsStr != null) {
                runTestWorkflows.setTestActors(testActorsStr);
            }

            final String testLoadDemosStr = System.getProperty("testLoadDemos");
            if(testLoadDemosStr != null) {
                runTestWorkflows.setTestLoadDemos(testLoadDemosStr);
            }
            
            final String testTimeoutStr = System.getProperty("timeout");
            if(testTimeoutStr != null) {
                runTestWorkflows.setTimeout(testTimeoutStr);
            }

	        ModuleTree tree = ModuleTree.instance();
	        if(argv.length == 0 || argv[0].equals("undefined")) {
	            for(Module module : tree) {
	                runTestWorkflows._checkModule(module);
	                
	                if(_fastFailed) {
	                    break;
	                }
	
	            }
	        } else {
	            for(String name : argv) {
	                Module module = tree.getModule(name);
	                if(module == null) {
	                    PrintError.message("Could not find module " + argv[0] +
	                            " in suite.");
	                } else {
	                    runTestWorkflows._checkModule(module);
	
	                    if(_fastFailed) {
	                        break;
	                    }
	                }
	            }
	        }
    	} catch(Throwable t) {
    		PrintError.message("Error running tests.", t);
    	} finally {
    		
            // report which workflows failed
            for(TestInfo info: _failedTests) {
                System.out.println("FAILED: " + info.getPath());
            }
    	}
    }

    /** Set the fast fail value. */
    public void setFastfail(String fastFail) {
        
        if(fastFail.equals("unknown")) {
            _fastFail = false;
        } else {
            _fastFail = Boolean.valueOf(fastFail).booleanValue();
        }
    }
    
    /** Set the test actors value. */
    public void setTestActors(String testActors) {
        if(testActors.equals("false")) {
            _testActors = false;
        }
    }

    /** Set the test loading demos value. */
    public void setTestLoadDemos(String testLoadDemos) {
        if(testLoadDemos.equals("false")) {
            _testLoadDemos = false;
        }
    }

    /** Set the test timeout value. */
    public void setTimeout(String timeout) {
        _testTimeout = Long.valueOf(timeout).longValue();
    }
    
    /**
     * set the module to test
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        _moduleName = moduleName;
    }
        
    /** Load a workflow or actor and parse the output. */
    private void _loadWorkflowOrActor(TestInfo test) throws Exception {
    	
    	
    	if(_parseWorkflowMain == null) {
    		Class<?> clazz = Class.forName("org.kepler.loader.util.ParseWorkflow");
    		_parseWorkflowMain = clazz.getMethod("main", String[].class);
    	}
    	
    	// redirect stdout and stderr
    	PrintStream stdoutOrig = System.out;
    	PrintStream stderrOrig = System.err;
    	
    	ByteArrayOutputStream stdoutByteStream = new ByteArrayOutputStream();
    	PrintStream stdoutPrintStream = new PrintStream(stdoutByteStream);
    	System.setOut(stdoutPrintStream);
    	
    	ByteArrayOutputStream stderrByteStream = new ByteArrayOutputStream();
    	PrintStream stderrPrintStream = new PrintStream(stderrByteStream);
    	System.setErr(stderrPrintStream);
    	
    	// call the method
    	String[] args;
    	// if test is for an actor, add -a argument for ParseWorkflow.main()
    	if(test.getTestType() == TestType.Actor) {
    		args = new String[] { "-a", test.getPath() };
    	} else {
    		args = new String[] { test.getPath() };    		
    	}

    	long startTime = System.nanoTime();
    	
    	_parseWorkflowMain.invoke(null, (Object)args);
    	
        long elapsed = System.nanoTime() - startTime;
        if(elapsed > 0) {
            elapsed = elapsed / 1000000000;
        }
        test.setTime(elapsed);

    	// restore stdout and stderr
    	System.setOut(stdoutOrig);
    	System.setErr(stderrOrig);
    	    	
    	// parse stdout and stderr
        InputStream stream = new ByteArrayInputStream(stdoutByteStream.toByteArray());
        _parseOutput(test, stream, true);
        stdoutByteStream.close();
        stream.close();
        
        stream = new ByteArrayInputStream(stderrByteStream.toByteArray());
        _parseOutput(test, stream, false);
        stderrByteStream.close();
        stream.close();
    }
        
    /** Execute a class in Kepler to check a workflow and updates the
     *  WorkflowInfo object if there are errors.
     */
    private void _executeKeplerClassAsSeparateJVM(String className, final TestInfo info) {
    	
    	// use ant java task to get a command line
        Java java = new Java();
        java.bindToOwner(this);
        java.init();
        java.setFork(true);
        java.setSpawn(false);
        // set fail on error true so that if a timeout occurs, then an
        // exception is thrown.
        java.setFailonerror(true);

        RunClasspath runClasspath = new RunClasspath();
        java.setClasspath(runClasspath);
        java.setClassname(className);
        java.createJvmarg().setLine("-Djava.awt.headless=true");
        java.createArg().setValue(info.getPath());
        
        File stdoutFile = null;
        File stderrFile = null;
        try {
            try {
                stdoutFile = File.createTempFile("hudson", ".stdout");
                stderrFile = File.createTempFile("hudson", ".stderr");
            } catch(IOException e) {
                String message = "Error creating stdout/stderr file: " + e.getMessage();
                info.setError(message);
                System.err.println(message);
                return;
            }
            
            java.setOutput(stdoutFile);
            java.setError(stderrFile);
            
            java.setTimeout(Long.valueOf(_testTimeout * 1000));
    
            Project javaProject = new Project();
            javaProject.setBaseDir(ProjectLocator.getKeplerModulesDir());
            java.setProject(javaProject);
            
            try {
                java.execute();
            } catch(BuildException e) {
                String message = "Error running workflow: " + e.getMessage();
                System.err.println(message);
                info.setError(message);
                return;            
            }
    
            InputStream stdoutStream = null;
            InputStream stderrStream = null;
                    
            try {
                
                // read stdout and stderr         
                stdoutStream = new FileInputStream(stdoutFile);
                try {
                	_parseOutput(info, stdoutStream, true);
    			} catch (IOException e) {
    				System.err.println("Error reading stdout: " + e.getMessage());
    				e.printStackTrace();
    			}
        
                stderrStream = new FileInputStream(stderrFile);
                try {
                	_parseOutput(info, stderrStream, false);
    			} catch (IOException e) {
                    System.err.println("Error reading stderr: " + e.getMessage());
                    e.printStackTrace();
    			}
    
            } catch(Exception e) {
                System.err.println("Error running Kepler: " + e.getMessage());
                e.printStackTrace();
            }
            finally {
                if(stdoutStream != null) {
                    try {
                        stdoutStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if(stderrStream != null) {
                    try {
                        stderrStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            if(stdoutFile != null && !stdoutFile.delete()) {
                System.err.println("WARNING: unable to delete " + stdoutFile);
            }
            if(stderrFile != null && !stderrFile.delete()) {
                System.err.println("WARNING: unable to delete " + stderrFile);
            }
        }
    }

    /** Test loading the actors for a specific module. */
    private void _loadActorsForModule(Module module, Set<TestInfo> tests)
    		throws Exception {
       
        // find the actors to load
        File karDir = module.getKarResourcesDir();

        if(!karDir.exists()) {
        	System.out.println("  No actors found.");
        	return;
        }
        
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(karDir);
        scanner.setIncludes(new String[] {"**/*.xml"});
        scanner.scan();

        String[] actorFileNames = scanner.getIncludedFiles();
        
        // load the actors
        if(actorFileNames == null || actorFileNames.length == 0) {
            System.out.println("  No actors found.");
        } else {
            
            // sort by name
            Arrays.sort(actorFileNames);

            for(String name : actorFileNames) {
                System.out.println("  Loading actor " + name);
                TestInfo test = new TestInfo(name, TestType.Actor);
                tests.add(test);
                
                final String absoluteWorkflowPath = karDir.getAbsolutePath() +
                        File.separator + name;
                test.setPath(absoluteWorkflowPath);
                
                _loadWorkflowOrActor(test);
                
                if(_fastFailed) {
                    break;
                }       
            }
        }    
    }
    
    /** Test loading the demo workflows for a specific module. */
    private void _loadDemoWorkflowsForModule(Module module, Set<TestInfo> tests)
    		throws Exception {
       
        // find the demo workflows to load
        File workflowsDir = module.getDemosDir();

        if(!workflowsDir.exists()) {
        	System.out.println("  No demo workflows found.");
        	return;
        }
        
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(workflowsDir);
        scanner.setIncludes(new String[] {"**/*.xml", "**/*.kar"});
        scanner.setExcludes(new String[] {"**/unsupported/"});
        scanner.scan();

        String[] workflowFileNames = scanner.getIncludedFiles();
        
        // load the workflows
        if(workflowFileNames == null || workflowFileNames.length == 0) {
            System.out.println("  No demo workflows found.");
        } else {
            
            // sort by name
            Arrays.sort(workflowFileNames);

            for(String name : workflowFileNames) {
                System.out.println("  Loading workflow " + name);
                TestInfo test = new TestInfo(name, TestType.DemoWorkflow);
                tests.add(test);
                
                final String absoluteWorkflowPath = workflowsDir.getAbsolutePath() +
                        File.separator + name;
                test.setPath(absoluteWorkflowPath);
                
                _loadWorkflowOrActor(test);
                
                if(_fastFailed) {
                    break;
                }       
            }
        }    
    }

    /** Check a module: execute the test workflows, load the demo workflows,
     *  and load the actors.
     */
    private void _checkModule(Module module) throws Exception {
     
        final String moduleName = module.getName();
        
        System.out.println("Checking module " + moduleName + ".");

        final Set<TestInfo> tests = new HashSet<TestInfo>();
        _errors = 0;

        if(_testLoadDemos) {
            _loadDemoWorkflowsForModule(module, tests);
        }
        
        if(_testActors) {
            _loadActorsForModule(module, tests);
        }
        
        _executeTestWorkflowsForModule(module, tests);

        // update the list of failed workflows
        for(TestInfo test: tests) {
            if(test.hadError()) {
            	_failedTests.add(test);
            }
        }
        
        // create the summary XML file for this module
        FileWriter xmlWriter = null;
        try {
            try {
                File testsDir = new File(ProjectLocator.getBuildDir() + File.separator + "tests");
                if(!testsDir.exists() && !testsDir.mkdirs()) {
                    System.err.println("Unable to create directory for " + testsDir);
                    return;
                }

                xmlWriter = new FileWriter(new File(testsDir, "workflows-" + moduleName + ".xml"));
                
                xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xmlWriter.write("<testsuite errors=\"" + _errors
                        + "\" tests=\"" + tests.size() + "\">\n");
                for(TestInfo test : tests) {
                    String nameWithoutExtension = _removeExtension(test.getName());
                    String testTypeStr;
                    TestType type = test.getTestType();
                    if(type == TestType.TestWorkflow) {
                        testTypeStr = "RunTestWorkflow";
                    } else if(type == TestType.DemoWorkflow) {
                        testTypeStr = "LoadDemoWorkflow";
                    } else {
                    	testTypeStr = "LoadActor";
                    }
                    xmlWriter.write("  <testcase " + "classname=\""
                            +  moduleName + "." + testTypeStr
                            + "." + nameWithoutExtension + "\"" + " name=\""
                            + moduleName + " " + nameWithoutExtension
                            + "\" time=\"" + test.getTime() + "\"  >\n");
                    if(test.hadError()) {
                        xmlWriter.write("    <error message=\"" +
                        		StringEscapeUtils.escapeXml(test.getError()) + "\"/>\n");
                    }
                    xmlWriter.write("    <system-err>");
                    xmlWriter.write(StringEscapeUtils.escapeXml(test.getStderr()));
                    xmlWriter.write("</system-err>\n");
                    xmlWriter.write("    <system-out>");
                    xmlWriter.write(StringEscapeUtils.escapeXml(test.getStdout()));
                    xmlWriter.write("</system-out>\n");
                    xmlWriter.write("  </testcase>\n");
                }
                xmlWriter.write("</testsuite>\n");
                
            } finally {
                if(xmlWriter != null) {
                    xmlWriter.close();
                }
            }
        } catch(IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** Execute the test workflows for a specific module. */
    private void _executeTestWorkflowsForModule(Module module, Set<TestInfo> tests) {
        
        // find the test workflows to run
        File workflowsDir = module.getTestsWorkflowsDir();
        String[] workflowFileNames = workflowsDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.endsWith(".xml") || name.endsWith(".kar")) {
                    return true;
                }
                return false;
            }    
        });
                
        // execute the test workflows
        if(workflowFileNames == null || workflowFileNames.length == 0) {
            System.out.println("  No test workflows found.");
        } else {
            
            // sort by name
            Arrays.sort(workflowFileNames);

            for(String name : workflowFileNames) {
                System.out.println("  Executing workflow " + name);
                final TestInfo test = new TestInfo(name, TestType.TestWorkflow);
                tests.add(test);
                
                final String absoluteWorkflowPath = workflowsDir.getAbsolutePath() +
                        File.separator + name;
                test.setPath(absoluteWorkflowPath);

                long startTime = System.nanoTime();
                
                // XXX check training mode is off before running
                _executeKeplerClassAsSeparateJVM("org.kepler.ExecutionEngine", test);

                long elapsed = System.nanoTime() - startTime;
                if(elapsed > 0) {
                    elapsed = elapsed / 1000000000;
                }
                test.setTime(elapsed);
                       
                if(_fastFail && test.hadError()) {
                    _fastFailed = true;
                }

                if(_fastFailed) {
                    break;
                }       
            }
        }
    }

    /** Parse the output from a test.
     * @param test information about the test
     * @param output the output from the test
     * @param isStdout if true, output is from stdout. if false, output is stderr.
     */
    private void _parseOutput(TestInfo test, InputStream output, boolean isStdout) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(output));
        String line;
        try {
			while((line = reader.readLine()) != null) {
				if(isStdout) {
					System.out.println(line);
				} else {
					System.err.println(line);
				}
			    synchronized(test) {
			    	if(isStdout) {
			    		test.appendStdout(line);
			    	} else {
			    		test.appendStderr(line);
			    	}
			        if((line.toLowerCase().contains("exception") ||
			    		line.toLowerCase().contains("error")) && !test.hadError()) {
			        	test.setError(line);
			            _errors++;
			        }
			    }
			}
		} catch (IOException e) {
		    System.err.println("Error reading output: " + e.getMessage());
		    e.printStackTrace();
		}
        reader.close();
    }

    /** Remove the extension in a file name. */
    private static String _removeExtension(String name) {
        int index = name.lastIndexOf(".");
        if(index > -1) {
            return name.substring(0, index);
        }
        return name;
    }

    /** A utility class to hold information about a test execution. */
    private static class TestInfo {

        public TestInfo(String name, TestType type) {
        	_name = name;
            _type = type;
        }
        
        /** Append to the stderr buffer. */
        public void appendStderr(String line) {
            _stderr.append(line);
            _stderr.append("\n");
        }

        /** Get the stderr buffer. */
        public String getStderr() {
            return _stderr.toString();
        }

        /** Append to the stdout buffer. */
        public void appendStdout(String line) {
            _stdout.append(line);
            _stdout.append("\n");
        }

        /** Get the stdout buffer. */
        public String getStdout() {
            return _stdout.toString();
        }

        /** Returns the error line if an error occurred.
         *  Returns null if no error occurred.
         */
        public String getError() {
        	return _error;
        }
        
        /** Returns true if an error occurred. */
        public boolean hadError() {
            return _error != null;
        }
        
        public String getPath() {
            return _path;
        }
        
        /** Set if an error occurred. */
        public void setError(String error) {
            _error = error;
        }

        public void setPath(String path) {
            _path = path;
        }
        
        /** Get the amount of time in seconds elapsed running the test. */
        public String getTime() {
            return String.valueOf(_time);
        }

        /** Set the elapsed time in seconds. */
        public void setTime(long time) {
            _time = time;
        }

        /** Get the test type. */
        public TestType getTestType() {
            return _type;
        }
        
        /** Get the workflow/actor filename. */
        public String getName() {
        	return _name;
        }
        
        /** Workflow or actor file name. */
        private String _name;
        
        /**If true, an error occurred. */
        private String _error;
        
        /** The type of test. */
        private TestType _type;
        
        private String _path;
        
        /** Buffer of stdout. */
        private final StringBuilder _stdout = new StringBuilder();
        
        /** Buffer of stderr. */
        private final StringBuilder _stderr = new StringBuilder();
        
        /** The elapsed time in seconds. */
        private long _time;
    }
    
    /** The types of tests. */
    private enum TestType {
    	TestWorkflow,
    	DemoWorkflow,
    	Actor
    };

    /** If not null or "undefined", the module to test. */
    private String _moduleName;

    /** The number of errors per module. */
    private int _errors = 0;
    
    /** If true, stop after the first error. */
    private static boolean _fastFail = false;
    
    /** This is set to true if _fastFail is true and an error occurred. */
    private static boolean _fastFailed = false;
    
    /** A list of tests that failed. */
    private static List<TestInfo> _failedTests = new LinkedList<TestInfo>();
    
    /** The main() method for ParseWorkflow. */
    private static Method _parseWorkflowMain;
    
    /** If true, test loading the actors. */
    private static boolean _testActors = true;

    /** If true, test loading the demo workflows. */
    private static boolean _testLoadDemos = true;

    /** The test timeout in seconds. */
    private static long _testTimeout = 5*60;    
}
