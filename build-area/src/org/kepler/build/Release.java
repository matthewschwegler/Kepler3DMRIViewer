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

import java.io.File;

import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.project.PrintError;
import org.kepler.build.util.CommandLine;

/**
 * Class to publish a module or suite
 * Created by David Welker.
 * Date: Sep 8, 2009
 * Time: 8:34:11 PM
 */
public class Release extends Brancher
{

    private boolean isSuite = false;
    private boolean referencePatches = true;
    private boolean testRelease = false;

    /**
     * set the suite to publish
     *
     * @param suiteName
     */
    public void setSuite(String suiteName)
    {
        if (!suiteName.equals("undefined"))
        {
            isSuite = true;
            setModule(suiteName);
        }
    }

    public void setReferencePatches(boolean referencePatches)
    {
        this.referencePatches = referencePatches;
    }

    /**
     * Set whether the release should be done tentatively as a test release.
     */
    public void setTestRelease(boolean testRelease)
    {
        this.testRelease = testRelease;
    }


    /**
     * run the task
     */
    public void run() throws Exception
    {
        release(0);
    }

    private ModulesTxt modulesTxtBeforeRelease;
    private ModulesTxt modulesTxtForRelease;

    private void modifyModulesTxtForRelease()
    {
    	System.out.println("Modifying modules.txt for release...");
    	Module module = Module.make(moduleName);
        if (isSuite)
        {
            modulesTxtBeforeRelease = module.getModulesTxt();
            modulesTxtForRelease = new ModulesTxt(modulesTxtBeforeRelease.getAbsolutePath());
            //Quick sanity check
            if (!modulesTxtBeforeRelease.exists())
                return;

            for (Module m : modulesTxtBeforeRelease)
            {
                if (m.getName().matches("[a-zA-Z-]+-\\d+\\.\\d+"))
                {
                    String releaseReference = m.getName() + ".^";
                    if( m.isSuite() )
                        releaseReference = "*" + releaseReference;
                    modulesTxtForRelease.add(releaseReference);
                }
                else
                {
                	modulesTxtForRelease.add(m);
                }
            }
            modulesTxtForRelease.write();
        }
    }

    private void revertModulesTxtAfterRelease()
    {
    	System.out.println("Reverting modules.txt after release...");
        modulesTxtBeforeRelease.write();
    }


    /**
     * do the releasing
     *
     * @param patch
     * @throws Exception
     */
    protected void release(int patch) throws Exception
    {
        if (moduleName.equals("undefined"))
        {
            PrintError.moduleNotDefined();
            return;
        }
        if (!isBranch)
        {
            PrintError.message("The module you specified is not a branch.");
            return;
        }

        calculateExistingBranches();

        boolean branchReleased = false;
        for (String branch : existingBranches)
        {
            if (moduleName.equals(branch))
            {
                branchReleased = true;
                break;
            }
        }
        if (!branchReleased)
        {
            PrintError.message(moduleName + " is not branched. You may only release branched modules.");
            return;
        }

        Module module = Module.make(moduleName);

        File moduleDir = module.getDir();
        if (!moduleDir.exists())
        {
            PrintError.message("The module must exist and be compiled and jarred locally in order to release.");
            return;
        }

        if (moduleName.matches(Module.PTOLEMY+"-\\d+\\.\\d+") ||
        		moduleName.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+"))
        {
            Copy copy = new Copy();
            copy.bindToOwner(this);
            copy.setTodir(new File(module.getLibDir(), Module.PTOLEMY_LIB));
            FileSet dirToCopy = new FileSet();
            dirToCopy.setDir(new File(module.getSrc(), "lib"));
            copy.add(dirToCopy);
            copy.execute();
        }


        File src = module.getSrc();
        boolean needToCompile = !(!src.isDirectory() || src.list().length == 0 || src.list().length == 1 && src.list()[0].equals(".svn"));

        if (needToCompile)
        {
            File targetJar = module.getTargetJar();
            if (!targetJar.exists())
            {
                PrintError.message("You must jar " + moduleName + " before releasing.");
                return;
            }
        }

        //If there is no module-info/licenses.txt, do not release the module. All release modules must
        //have a licenses.txt that documents

        File licensesTxt = module.getLicensesTxt();
        if (!licensesTxt.exists())
        {
            PrintError.message("You must create a licenses.txt file in the module-info directory that lists the license for this module and all third-party dependencies before releasing.");
            return;
        }

        if (testRelease)
        {
            File testReleaseFile = new File(module.getModuleInfoDir(), "test-release");
            testReleaseFile.createNewFile();
        }

        if (referencePatches && isSuite)
        {
            modifyModulesTxtForRelease();
        }
        //SVN.getUsernameAndPassword(this);

        Zip zip = new Zip();
        zip.bindToOwner(this);
        ZipFileSet fileSet = new ZipFileSet();
        fileSet.setDir(module.getDir());
        fileSet.setFileMode("755");
        fileSet.setExcludes("src/**");
        fileSet.setExcludes("target/classes/**");
        fileSet.setExcludes("target/eclipse/**");
        fileSet.setExcludes("target/idea/**");
        fileSet.setExcludes("target/netbeans/**");
        zip.addFileset(fileSet);
        File releaseDir = new File(basedir, "release-area");
        File releaseModuleDir = new File(releaseDir, moduleName + "." + patch);
        releaseModuleDir.mkdirs();
        String destfilename = moduleName + "." + patch + ".zip";
        zip.setDestFile(new File(releaseModuleDir, destfilename));
        zip.execute();

        //Copy the module-info directory separately so that it is released and information (like modules.txt and licenses.txt) is separately available.
        if (module.getModuleInfoDir().isDirectory())
        {
            Copy copy = new Copy();
            copy.bindToOwner(this);
            copy.setTodir(releaseModuleDir);
            copy.setOverwrite(true);
            FileSet dirToCopy = new FileSet();
            dirToCopy.setDir(module.getDir());
            dirToCopy.setIncludes("module-info/**");
            copy.add(dirToCopy);
            copy.execute();
        }


        String osName = System.getProperty("os.name").toLowerCase().trim();

        String moduleReleaseLocation = releaseLocation + "/" + moduleName + "." + patch;

        if (osName.startsWith("windows"))
        {
            System.out.println("Use your SVN client to import " + moduleName + " to: ");
            System.out.println("  " + moduleReleaseLocation);
            return;
        }

        String[] importCommand = {"svn", "import", "-m \"[build-system] " + destfilename + "...\"", releaseModuleDir.getAbsolutePath(), moduleReleaseLocation};
        CommandLine.exec(importCommand);

        releaseDir.delete();

        if (referencePatches && isSuite )
        {
            revertModulesTxtAfterRelease();
        }

    }

}
