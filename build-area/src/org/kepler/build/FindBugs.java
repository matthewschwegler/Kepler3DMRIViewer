/*
 * Copyright (c) 2012 The Regents of the University of California.
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

import java.io.File;

import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTask;

/** An ant task to run FindBugs for the current suite.
 * 
 *  @author Daniel Crawl
 *  @version $Id: FindBugs.java 31111 2012-11-26 22:17:14Z crawl $
 *
 */
public class FindBugs extends ModulesTask {
    
    @Override
    public void run() throws Exception {

        Java java = new Java();
        java.bindToOwner(this);
             
        String jarFilePath = _home + File.separator + "lib" + File.separator + "findbugs.jar";
        java.setJar(new File(jarFilePath));
        
        if(_jvmargs != null) {
            java.createJvmarg().setLine(_jvmargs);
        }
        
        java.createArg().setLine("-textui");
        
        if(_excludefilter != null) {
            java.createArg().setLine("-exclude " + _excludefilter);
        }
        
        if(_effort == null) {
            _effort = "min";
        }
        java.createArg().setLine("-effort:" + _effort);
        
        if(_home != null) {
            java.createArg().setLine("-home " + _home);
        }
        
        if(_output == null) {
            _output = "-xml";
        }
        java.createArg().setLine(_output);
        
        if(_outputfile == null) {
            _outputfile = "findbugs.xml";
        }
        java.createArg().setLine("-output " + _outputfile);
        
        String auxClassPath = _getAuxClassPath();
        java.createArg().setLine("-auxclasspath " + auxClassPath);
        
        String sourcePath = _getSourcePath();
        java.createArg().setLine("-sourcepath " + sourcePath);
        
        String classesPath = _getClassesPath();
        java.createArg().setLine(classesPath);
        
        if(_debug) {
            System.out.println("Running:");
            System.out.println(java.getCommandLine());
        }
                    
        java.setTimeout(_timeout);
        java.setFork(true);
        java.setFailonerror(true);
        java.execute();
        
    }
    
    public void setDebug(boolean debug) {
        _debug = debug;
    }
    
    public void setEffort(String effort) {
        _effort = effort;
    }

    public void setExcludefilter(String excludefilter) {
        _excludefilter = excludefilter;
    }
    
    public void setHome(String home) {
        _home = home;
    }
        
    public void setJvmargs(String jvmargs) {
        _jvmargs = jvmargs;
    }
    
    public void setOutput(String output) {
        _output = output;
    }
    
    public void setOutputfile(String outputfile) {
        _outputfile = outputfile;
    }
    
    public void setTimeout(long timeout) {
        _timeout = timeout;
    }
    
    /** Get a list of directories containing sources for the current
     *  suite separated by ":".
     */
    private String _getSourcePath() {
        StringBuilder buf = new StringBuilder();
        ModuleTree tree = ModuleTree.instance();
        for(Module module : tree.getModuleList()) {
            buf.append(module.getSrc().getAbsolutePath());
            buf.append(File.pathSeparator);
        }
        
        // include build-area since it is not part of the module tree
        final File buildAreaSrcDir = new File("src");
        buf.append(buildAreaSrcDir.getAbsolutePath());

        return buf.toString();
    }
    
    /** Get a list of directories containing .class files for the
     *  current suite separated by spaces.
     */
    private String _getClassesPath() {
        StringBuilder buf = new StringBuilder();
        ModuleTree tree = ModuleTree.instance();
        for(Module module : tree.getModuleList()) {
            File classesDir = module.getTargetClasses();
            if(classesDir.exists()) {
                buf.append(classesDir.getAbsolutePath());
                buf.append(" ");
            }
        }
        
        // include build-area since it is not part of the module tree
        final File buildAreaJar = new File("target/kepler-tasks.jar");
        buf.append(buildAreaJar.getAbsolutePath());
        return buf.toString();
    }
    
    /** Get a list of 3rd party jars used by the current suite
     *  separated by ":".
     */
    private String _getAuxClassPath() {
        StringBuilder buf = new StringBuilder();
        ModuleTree tree = ModuleTree.instance();
        for(Module module : tree.getModuleList()) {
            for(File jarFile : module.getJars()) {
                // dlese.jar causes findbugs to crash:
// Exception analyzing org.dlese.adn.ObjectFactory using detector edu.umd.cs.findbugs.detect.CalledMethods
// java.lang.ArrayIndexOutOfBoundsException: 27586
//    At org.objectweb.asm.ClassReader.readClass(Unknown Source)
                if(! jarFile.getName().equals("dlese.jar")) {
                    buf.append(jarFile.getAbsolutePath());
                    buf.append(File.pathSeparator);
                }
            }
        }

        // include build-area since it is not part of the module tree
        final File buildAreaLibDir = new File("lib");
        final FileSet fileSet = new FileSet();
        fileSet.setProject(project);
        fileSet.setDir(buildAreaLibDir);
        fileSet.setIncludes("**/*.jar");
        final String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
        for(String fileName : files) {
            buf.append(buildAreaLibDir.getAbsolutePath() + File.separator + fileName);
            buf.append(File.pathSeparator);
        }
        buf.deleteCharAt(buf.length()-1);
        return buf.toString();
    }

    private boolean _debug;
    private String _excludefilter;
    private String _effort;
    private String _home;
    private String _jvmargs;
    private String _output;
    private String _outputfile;
    private long _timeout;

}
