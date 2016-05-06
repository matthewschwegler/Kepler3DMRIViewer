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
package org.kepler.build.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Move;
import org.apache.tools.ant.types.FileSet;
import org.kepler.build.ChangeTo;
import org.kepler.build.Run;
import org.kepler.build.installer.InstallationIdTxt;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModuleUtil;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.modules.Suite;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.util.DotKeplerManager;

/**
 * The runner that runs kepler for the build system
 * Created by David Welker.
 * Date: Oct 24, 2008
 * Time: 2:39:09 PM
 */
public class Kepler extends ModulesTask
{
    private static String BASEDIRINITVALUE = "Unknown";
    private String[] args = {""};
    private static String baseDirPath = BASEDIRINITVALUE;

    /**
     * If true, clear args, set args to UseModuleManager.
     * If false do nothing.
     * @param runModuleManager
     */
    public void setRunModuleManager(boolean runModuleManager)
    {
        if (runModuleManager)
        {
            args = new String[]{"UseModuleManager"};
        }
    }

    /**
     * start kepler
     *
     * @param args
     */
    public static void main(String[] args)
    {
        if(baseDirPath == null || baseDirPath.equals(BASEDIRINITVALUE))
        {
//          String codePath = Kepler.class.getProtectionDomain().getCodeSource().getLocation().getPath();
//          File codeDir = new File(codePath);
//          File basedir = codeDir.getParentFile();
//          baseDirPath = basedir.getAbsolutePath();
        	//new code can start kepler module manager correctly both in eclipse and command line.
        	baseDirPath = ProjectLocator.getBuildDir().getParentFile().getAbsolutePath();
          
        }
        if (baseDirPath.indexOf("%20") != -1)
        {   //put in quotes and get rid of the %20
            baseDirPath = baseDirPath.replaceAll("%20", "\\ ");
        }
        System.out.println("The base dir is "+baseDirPath);
        basedir = new File(baseDirPath);
        Project project = new Project();
        project.setBaseDir(basedir);
        DefaultLogger logger = new DefaultLogger();
        logger.setMessageOutputLevel(Project.MSG_INFO);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.out);
        project.addBuildListener(logger);

        //Kepler.args = args;
        Kepler kepler = new Kepler();
        kepler.setProject(project);
        kepler.setArgs(args);
        kepler.init();
        kepler.execute();
    }
    
    /**
     * Set the arguments for the kepler.
     * @param args the arguments will be set
     */
    public void setArgs(String[] args)
    {
      this.args = args;
    }
    
    /**
     * Specify the base dir path of the kepler.
     * project.setBaseDir will not be called until this class's main 
     * method is called.
     * 
     * @param baseDirPath
     */
    public static void setBaseDirPath(String basePath)
    {
      baseDirPath = basePath;
    }

    @Override
    public void run() throws Exception
    {
        InstallationIdTxt.check();

        //BEGIN HACK
        //Author: David Welker, February 2, 2011
        //Hack: If this is is kepler-2.0.0 then check if this is the first time it has been run using a
        //flag in the form of a file build-area/kepler-2.2.0-run-before. If this is the first time kepler-2.0.0
        //has been run, move any configuration from KeplerData
        String suiteName = ModuleUtil.getCurrentSuiteName();
        if( suiteName.equals("kepler-2.2.0") )
        {
            File kepler220RunBefore = new File(ProjectLocator.getUserBuildDir(), "kepler-2.2.0-run-before");
            if( !kepler220RunBefore.exists() )
            {
                System.out.println("First time running 2.2.0. Moving older configuration out of the way.");
                File keplerDataDir = DotKeplerManager.getInstance().getPersistentDir();
                File configurationDir = new File(keplerDataDir, "modules");
                if( configurationDir.isDirectory() )
                {
                    File configurationDirBefore22 = new File(keplerDataDir, "configuration.before-2.2.0");
                    configurationDirBefore22.mkdirs();

                    File[] moduleDirs = configurationDir.listFiles();
                    for( File moduleDir : moduleDirs )
                    {
                        System.out.println("DEBUG: moduleDir = " + moduleDir.getAbsolutePath());
                        File moduleSpecificConfigDir = new File(moduleDir, "configuration");
                        if( moduleSpecificConfigDir.isDirectory() )
                        {
                            System.out.println("DEBUG: " + moduleSpecificConfigDir.getAbsolutePath() + " exists! Moving...");

                            FileSet configFileSet = new FileSet();
                            configFileSet.setProject(this.getProject());
                            configFileSet.setDir(moduleSpecificConfigDir);

                            File destDir = new File(configurationDirBefore22, moduleDir.getName());
                            destDir.mkdir();

                            Move move = new Move();
                            move.bindToOwner(this);
                            move.init();
                            move.addFileset(configFileSet);
                            move.setTodir(destDir);
                            move.execute();
                        }

                    }

                }
                if( !kepler220RunBefore.getParentFile().exists() )
                {
					kepler220RunBefore.getParentFile().mkdirs();
                }
                kepler220RunBefore.createNewFile();
            }
        }

        //END HACK


        String argStr = "";
        String argProp = "";
        String firstArgument = args.length > 0 ? args[0] : "";
        boolean useModuleManager = firstArgument.startsWith("UseModuleManager");
        int startIndex = useModuleManager ? 1 : 0;
        for (int i = startIndex; i < args.length; i++)
        {
            if (!args[i].trim().equals(""))
            {
                if (args[i].trim().startsWith("-D"))
                {
                    argProp += args[i].substring(2) + ",";
                }
                else
                {
                    // NOTE: put quotes around each argument in case it contains spaces
                    argStr += "\"" + args[i] + "\" ";
                }
            }
        }

        if (useModuleManager)
        {
            File moduleManagerGuiTxt = new File(ProjectLocator.getBuildDir(), "module-manager-gui.txt");
            BufferedReader br = new BufferedReader(new FileReader(moduleManagerGuiTxt));
            String moduleManagerGuiSuiteName = br.readLine().trim();
            br.close();

            Suite moduleManagerGuiSuite = null;
            for (Module module : ProjectLocator.getKeplerModulesDir())
            {
                if (module.getName().equals(moduleManagerGuiSuiteName))
                {
                    moduleManagerGuiSuite = module.asSuite();
                }
            }
            if (moduleManagerGuiSuite == null)
            {
                PrintError.message("There is no module with the name " + moduleManagerGuiSuiteName + " in the current kepler.modules directory.");
                return;
            }

            ModulesTxt moduleManagerGuiModulesTxt = moduleManagerGuiSuite.getModulesTxt();
            ModuleTree moduleManagerGuiModuleTree = new ModuleTree(moduleManagerGuiModulesTxt);

            for (Module module : moduleManagerGuiModuleTree)
            {
                if (!moduleTree.contains(module))
                {
                    ChangeTo changeTo = new ChangeTo();
                    changeTo.bindToOwner(this);
                    changeTo.init();
                    changeTo.setSuite(moduleManagerGuiSuiteName);
                    changeTo.execute();
                    break;
                }
            }

        }

        Run run = new Run();
        if (!argProp.trim().equals(""))
        {
            run.setProperties(argProp);
        }
        run.setArgs(argStr);
        run.bindToOwner(this);
        run.init();
        String main = useModuleManager ? "org.kepler.modulemanager.gui.ModuleManagerPane" : "org.kepler.Kepler";
        System.out.println("Kepler.run going to run.setMain("+main+")");
        run.setMain(main);
        run.execute();
    }
}
