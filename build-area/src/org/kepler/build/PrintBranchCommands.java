/*
 * Copyright (c) 2013 The Regents of the University of California.
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
import java.util.HashMap;
import java.util.Map;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.build.util.Version;

/** A task to print out commands for branching, and creating and deleting
 *  test releases of modules.
 * 
 *  @author Daniel Crawl
 *  @version $Id: PrintBranchCommands.java 31815 2013-04-01 20:54:48Z crawl $
 */
public class PrintBranchCommands extends Brancher {

    @Override
    public void run() throws Exception {

        System.out.print("# getting existing branches in repository....");
        calculateExistingBranches();
        System.out.println("done");
        System.out.println("");
     
        System.out.println("");
        System.out.println("# Commands to branch this suite:");
        System.out.println("# WARNING: this assumes that every module will be branched into a new version.");
        System.out.println("");
        ModuleTree tree = ModuleTree.instance();
        for(Module module : tree) {
            _printBranchCommand(module);
        }
        
        System.out.println("");
        System.out.println("# Branched suite:");
        System.out.println("# WARNING: this assumes that every module has a new version.");
        System.out.println("");
        for(String name : _branchNames.values()) {
            System.out.println(name);
        }
        
        System.out.println("");
        System.out.println("# To create test releases:");
        System.out.println("# WARNING: this assumes that every module has a new version.");
        System.out.println("");
        for(Module module : tree) {
            _printCreateTestReleaseCommands(module);
        }
        
        System.out.println("");
        System.out.println("# To delete test releases:");
        System.out.println("# WARNING: this assumes that every module has a new version.");
        System.out.println("");
        for(Module module : tree) {
            _printDeleteTestReleaseCommands(module);
        }

        System.out.println("");
        System.out.println("# To copy test releases to released:");
        System.out.println("# WARNING: this assumes that every module has a new version.");
        System.out.println("");
        for(Module module : tree) {
            _printCopyTestReleaseToReleases(module);
        }
    }
        
    /** Set whether to increment version numbers or use existing versions. */
    public void setIncrement(boolean increment)
    {
        System.out.println("set increment = " + increment);
        _increment = increment;
    }

    /** Print the svn command to create a branch for a module. */
    private void _printBranchCommand(Module module) {
     
        String name = module.getName();
        
        VersionNumber lastBranch = null;
        
        // find the latest version of this module
        for(String branchedModule : existingBranches) {
            // see if it's the same module
            final VersionNumber currentBranch = new VersionNumber(branchedModule);
            if(name.equals(Version.fromVersionString(branchedModule).getBasename()) &&
                (lastBranch == null || currentBranch.isHigherThan(lastBranch))) {
                lastBranch = currentBranch;
            }
        }

        VersionNumber newVersion;
        if(lastBranch == null) {
            System.out.println("# No previous branches found for " + name);
            newVersion = new VersionNumber(1, 0); 
        } else if(_increment) {
            // increment the minor version
            newVersion = lastBranch.incrementMinor();
        } else {
            newVersion = lastBranch;
        }
        
        final String branchName = name + "-" + newVersion;
        System.out.println("svn cp " +
                RepositoryLocations.MODULES + "/" + name + " " +
                RepositoryLocations.BRANCHES + "/" + branchName +
                " -m " + "\"Branching " + name + " as " + branchName + "\"");
        System.out.println("");
        _branchNames.put(module, branchName);
    }
    
    /** Print the command to create e a test release of a module. */
    private void _printCreateTestReleaseCommands(Module module) {

        String branchedName = _branchNames.get(module);
        
        File modulesTxtFile = new File(module.getModuleInfoDir(), "modules.txt");
        
        // isSuite does not work
        // if(module.isSuite()) {
        if(modulesTxtFile.exists()) {
            System.out.println("ant release -Dtest=true -Dsuite=" + branchedName);
        } else {
            System.out.println("ant release -Dtest=true -Dmodule=" + branchedName);
        }
    }
    
    /** Print the command to delete a test release of a module. */
    private void _printDeleteTestReleaseCommands(Module module) {

        String branchedName = _branchNames.get(module);      
        System.out.println("svn delete -m \"going to recreate\" " +
                RepositoryLocations.TEST_RELEASES + "/" + branchedName);
    }

    /** Print the command to copy a test release to the released area. */
    private void _printCopyTestReleaseToReleases(Module module) {
        String branchedName = _branchNames.get(module);
        System.out.println("svn cp -m \"copying from test releases\" " +
                RepositoryLocations.TEST_RELEASES + "/" + branchedName + ".0 " +
                RepositoryLocations.RELEASED + "/" + branchedName + ".0");
        
    }
    
    /** A mapping of module to name of module with branched version. */
    private final Map<Module, String> _branchNames = new HashMap<Module,String>();
    
    /** If true, increment the version numbers. */
    private boolean _increment = true;
}
