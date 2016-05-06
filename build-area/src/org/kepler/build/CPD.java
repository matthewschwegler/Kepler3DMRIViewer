/*
 * Copyright (c) 2014 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-07-09 15:28:34 -0700 (Wed, 09 Jul 2014) $' 
 * '$Revision: 32828 $'
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
import java.util.Iterator;

import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.CommandLine;

/** An an task to run CPD (duplicate code detection) on the source
 *  code in the current suite. 
 *  
 *  @author Daniel Crawl
 *  @version $Id: CPD.java 32828 2014-07-09 22:28:34Z crawl $
 */
public class CPD extends ModulesTask {

    /** Run CPD and transform output to HTML. */
    @Override
    public void run() throws Exception {

        Java java = new Java();
        java.bindToOwner(this);
        
        // set class path
        Path classpath = new Path(ProjectLocator.getAntProject());
        FileSet jars = new FileSet();
        jars.setProject(ProjectLocator.getAntProject());
        jars.setDir(new File(_home,"lib"));
        jars.setIncludes("**/*.jar");
        Iterator<Resource> i = jars.iterator();
        while (i.hasNext())
        {
            Resource resource = i.next();
            if(resource instanceof FileResource) {
                classpath.createPathElement().setLocation(((FileResource)resource).getFile());
            }
        }
        
        java.setClasspath(classpath);
        java.setClassname("net.sourceforge.pmd.cpd.CPD");
        
        if(_jvmargs != null) {
            java.createJvmarg().setLine(_jvmargs);
        }

        for(Module module : moduleTree) {
            if(!module.isPtolemy()) {
                File srcDir = module.getSrc();
                if(srcDir.exists()) {
                    java.createArg().setLine("--files " + srcDir.getAbsolutePath());
                }
            }
        }
        
        java.createArg().setLine("--format net.sourceforge.pmd.cpd.XMLRenderer");
        java.createArg().setLine("--minimum-tokens " + _minTokens);
        java.setOutput(new File(_outputfile));
                        
        //java.setTimeout(_timeout);
        java.setFork(true);
        // FIXME CPD always returns non-zero
        //java.setFailonerror(true);
        java.execute();

        // convert the XML output to HTML
        String htmlName =  _outputfile + ".html";
        CommandLine.exec(new String[] {"xsltproc",
                "--output",
                htmlName,
                // FIXME verify xslt exists
                _home + File.separator + "etc" + File.separator + "xslt" + File.separator + "cpdhtml.xslt",
                _outputfile});
        System.out.println("Wrote " + htmlName);
    }

    /** Set the directory containing CPD/PMD. */
    public void setHome(String home) {
        _home = home;
    }

    /** Set the arguments to pass to the JVM that runs CPD. */
    public void setJvmargs(String jvmargs) {
        _jvmargs = jvmargs;
    }

    /** Set the minimum duplicate size. */
    public void setMintokens(String minTokens) {
        _minTokens = minTokens;
    }
    
    /** Set the output file. */
    public void setOutputfile(String outputfile) {
        _outputfile = outputfile;
    }

    /** The directory containing CPD/PMD. */
    private String _home;
    
    /** Arguments to pass to the JVM that runs CPD. */
    private String _jvmargs;
    
    /** The output file. */
    private String _outputfile;
    
    /** The minimum duplicate size. */
    private String _minTokens = "100";

}
