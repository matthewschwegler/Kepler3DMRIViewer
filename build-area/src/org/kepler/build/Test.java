/*
 * Copyright (c) 2009 The Regents of the University of California.
 * All rights reserved.
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
import java.io.File;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.optional.depend.ClassFile;
import org.apache.tools.ant.taskdefs.optional.junit.FormatterElement;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.types.Path;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RunClasspath;
import org.kepler.build.project.TestClasspath;
import org.kepler.build.util.ClassFileIterator;

/**
 * Created by David Welker. Date: Oct 30, 2008 Time: 3:53:03 PM
 */
public class Test extends ModulesTask 
{
    /** Set the fast fail value. */
    public void setFastfail(String fastFail) {
        
        if(fastFail.equals("unknown")) {
            _fastFail = false;
        } else {
            _fastFail = Boolean.valueOf(fastFail);
        }
    }

    /**
     * set the name of the test
     *
     * @param name
     */
    public void setName(String name)
    {
        this._name = name;
    }

    /**
     * set the module to test
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        this._moduleName = moduleName;
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        ModuleTree tree = ModuleTree.instance();
        if(_moduleName == null || _moduleName.equals("undefined")) {
            for(Module module : tree) {
                _checkModule(module);
                
                if(_fastFailed) {
                    break;
                }

            }
        } else {
            for(String name : _moduleName.split(",")) {
                Module module = tree.getModule(name);
                if(module == null) {
                    PrintError.message("Could not find module " + name +
                            " in suite.");
                } else {
                    _checkModule(module);

                    if(_fastFailed) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * make a horizontal line
     *
     * @param size
     * @return
     */
    private String _horizontalLine(int size)
    {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < size; i++)
        {
            line.append("-");
        }
        return line.toString();
    }

    /**
     * run the tests
     *
     * @param module
     * @throws Exception 
     */
    private void _checkModule(Module module) throws Exception
    {
        if (!module.getTestsSrc().isDirectory())
        {
            return;
        }
        
        
        final String moduleName = module.getName();
        
        System.out.println(_horizontalLine(32 + moduleName.length()));
        System.out.println("|  Running tests for " + moduleName + " module.  |");
        System.out.println(_horizontalLine(32 + moduleName.length()));
        // System.out.println(classpath);
        
        // First, compile the tests.
        
        Project proj = new Project();
        proj.setBaseDir(ProjectLocator.getKeplerModulesDir());

        Javac javac = new Javac();
        javac.bindToOwner(this);
        javac.setSrcdir(new Path(getProject(), module.getTestsSrc()
                .getAbsolutePath()));
        
        File testClassesDir = module.getTestsClassesDir();
        if(!testClassesDir.exists() && !testClassesDir.mkdirs())
        {
            throw new BuildException("Unable to create directory " + testClassesDir.getPath());
        }
        
        javac.setDestdir(module.getTestsClassesDir());
        javac.setClasspath(new TestClasspath());
        javac.setDebug(true);
        javac.setFork(true);
        javac.setMemoryMaximumSize("300m");
        javac.setProject(proj);
        javac.execute();
        
        for (ClassFile test : new ClassFileIterator(module.getTestsClassesDir()))
        {
            if (_name != null && !_name.equals("undefined"))
            {
                //System.out.println("test: " + test.getFullClassName());
                if (!test.getFullClassName().equals(_name))
                {
                    continue;
                }
            }

            runTest(test, moduleName);
            
            if(_fastFail && _fastFailed) {
            	return;
            }
        }       
    }

    /**
     * run the test
     *
     * @param test
     * @throws Exception 
     */
    public void runTest(ClassFile test, String moduleName) throws Exception
    {
        if (test.getFullClassName().contains("$"))
        {
            return;
        }
        
        final String className = test.getFullClassName(); 
        
        System.out.println("Running: " + className);

        Project proj = new Project();
        proj.setBaseDir(ProjectLocator.getKeplerModulesDir());

        File outputDir = new File(proj.getBaseDir(), "build-area" + File.separator + "tests");
        String outputNameNoExtension = "junit-" + moduleName + "." + className;
        
        JUnitTest junitTest = new JUnitTest(className);
        junitTest.setTodir(outputDir);
        junitTest.setOutfile(outputNameNoExtension);
        
        FormatterElement xmlFormatter = new FormatterElement();
        FormatterElement.TypeAttribute xmlTypeAttribute = new FormatterElement.TypeAttribute();
        xmlTypeAttribute.setValue("xml");
        xmlFormatter.setType(xmlTypeAttribute);

        FormatterElement plainFormatter = new FormatterElement();
        FormatterElement.TypeAttribute plainTypeAttribute = new FormatterElement.TypeAttribute();
        plainTypeAttribute.setValue("plain");
        plainFormatter.setType(plainTypeAttribute);

        //JUnitTask.SummaryAttribute sa = new JUnitTask.SummaryAttribute();
        //sa.setValue("withOutAndErr");
        
        JUnitTask junit = new JUnitTask();
        junit.setProject(proj);
        junit.setFork(true);
        junit.addTest(junitTest);
        junit.setShowOutput(true);
        //junit.setOutputToFormatters(true);
        junit.addFormatter(xmlFormatter);
        junit.addFormatter(plainFormatter);
        //junit.setPrintsummary(sa);
        
        Path path = junit.createClasspath();
        path.add(new RunClasspath());
        path.add(new TestClasspath());
        
        junit.execute();
        
        // read the results in the plain file and print the summary line
        BufferedReader reader = null;
        FileReader fileReader = null;
        try {
        	fileReader = new FileReader(new File(outputDir, outputNameNoExtension + ".txt"));
        	reader = new BufferedReader(fileReader);
        	String line = null;
        	while((line = reader.readLine()) != null) {
        		if(line.startsWith("Tests run:")) {
        			System.out.println(line);
        			
        			if(_fastFail && !line.contains("Failures: 0, Errors: 0")) {
        				_fastFailed = true;
        				System.out.println("Stopping because fastfail=true");
        			}
        			
        			break;
        		}
        	}
        } finally {
        	if(reader != null) {
        		reader.close();
        	}
        	if(fileReader != null) {
        		fileReader.close();
        	}
        }
    }
    
    /** The name of the test to run. If not specified, run all tests. */
    private String _name = "undefined";
    
    /** The name of the module to test. If not specified, test all modules. */
    private String _moduleName = "undefined";

    /** If true, stop after the first error. */
    private boolean _fastFail = false;
    
    /** This is set to true if _fastFail is true and an error occurred. */
    private boolean _fastFailed = false;
}
