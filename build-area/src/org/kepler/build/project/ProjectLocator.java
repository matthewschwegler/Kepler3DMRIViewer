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
package org.kepler.build.project;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.tools.ant.Project;
import org.kepler.build.modules.Module;
import org.kepler.build.runner.Kepler;
import org.kepler.util.DotKeplerManager;

/**
 * class to handle project locations
 *
 * @author berkley
 */
public class ProjectLocator
{
    private static Boolean shouldUtilizeUserKeplerModules = null;

    // Project Locations
    private static KeplerModulesDir keplerModulesDir;
    private static File userKeplerModulesDir;
    private static File buildDir;
    private static File userBuildDir;
    private static File tempDir;
    private static Project project;

    /**
     * Redundant - maintained for backwards compatibility. Use getKeplerModulesDir() instead.
     *
     * @return
     * @deprecated
     */
    public static ProjectDir getProjectDir()
    {
        return (ProjectDir) getKeplerModulesDir();
    }

    public static KeplerModulesDir getKeplerModulesDir()
    {
        if (keplerModulesDir == null)
        {
            keplerModulesDir = findKeplerModulesDir();
        }
        return keplerModulesDir;
    }

    /**
     * @param basedir
     */
    public static void setKeplerModulesDir(File basedir)
    {
        ProjectLocator.keplerModulesDir = new ProjectDir(basedir);
        buildDir = new File(basedir, "build-area");
    }

    public static File getCacheFile(String filename)
    {
        File cacheDir = DotKeplerManager.getInstance().getCacheDir();
        return new File(cacheDir, filename);
    }

    public static boolean shouldUtilizeUserKeplerModules()
    {
        if (shouldUtilizeUserKeplerModules == null)
        {
            File dotUtilizeUserKeplerModules = new File(ProjectLocator.getBuildDir(), "use.keplerdata");
            shouldUtilizeUserKeplerModules = dotUtilizeUserKeplerModules.exists();
        }
        return shouldUtilizeUserKeplerModules;
    }

    public static File getUserKeplerModulesDir()
    {
        if (userKeplerModulesDir == null)
        {
            userKeplerModulesDir = new File(DotKeplerManager.getInstance().getPersistentDir(), "kepler.modules");
            if (!userKeplerModulesDir.isDirectory())
            {
                userKeplerModulesDir.mkdirs();
            }
            return userKeplerModulesDir = initUserKeplerModulesDir();
        }
        return userKeplerModulesDir;
    }

    private static File initUserKeplerModulesDir()
    {

        return userKeplerModulesDir;
    }

    /**
     * get the temp dir
     *
     * @return
     */
    public static File getTempDir()
    {
        if (tempDir == null)
        {
            tempDir = new File(getKeplerModulesDir(), ".temp");
        }
        tempDir.deleteOnExit();
        return tempDir;
    }

    /**
     * 
     * FIXME replace this with something(s) more robust.
	 * http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5669 
	 * Be careful when doing so, some things probably always want 
     * KeplerData/kepler.modules, others always the other "application modules dir", others one or the other depending on 
     * what exists.
     * 
     * On linux(at least) during ant run, MemoryProperties invokes this, and the search begins based on the location of ant.jar
     * i.e. /usr/share/ant/lib/ant.jar/build-area/modules.txt can be the first attempt.
     * 
     * http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5669 is fixed at r31074. Now Run.setLocation() invoke it first. By utilizing 
     * the location of build.xml, the path can be set correctly.
     * 
     * find the project dir
     * WARNING this can return either the application modules dir, 
     * or KeplerData/kepler.modules, or a false positive (e.g. some arbitrary parent dir that happens to contain build-area/modules.txt), 
     * or the system-dependent default directory (in the catch).
     * @return
     */
    private static KeplerModulesDir findKeplerModulesDir()
    {
        KeplerModulesDir projectLocation;
        try
        {
            URI classesURI = ProjectLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File classesFile = new File(classesURI);
            //System.out.println("classesFile:" + classesFile);
            File projectLocationFile = findProjectLocation(classesFile);
            if (projectLocationFile == null) {
                System.out.println("WARNING: No build-area found.");
                projectLocation = null;
            } else {
                projectLocation = new ProjectDir(projectLocationFile);
                // FUTURE: Warning, depends on current module structure.
            }
        } catch (URISyntaxException e) {
            System.out.println("WARNING: Could not find project class location.");
            e.printStackTrace();
            System.out.println("WARNING: Using virtual project location.");
            File virtualFile = new File("");
            // FUTURE!
            virtualFile = new File(virtualFile.getAbsolutePath());
            projectLocation = new ProjectDir(virtualFile.getParent());
            System.out.println("WARNING: Virtual project location:"+projectLocation);
        }
        return projectLocation;
    }

    /**
     * get the project location
     *
     * @param candidate
     * @return
     */
    private static File findProjectLocation(File candidate)
    {
        File modulesTxt = new File(candidate, "build-area/modules.txt");
        if (modulesTxt.exists())
        {
            return candidate;
        }
        if (candidate.getParent() == null)
        {
            return null;
        }
        return findProjectLocation(candidate.getParentFile());
    }

    /**
     * get the build dir
     *
     * @return
     */
    public static File getBuildDir()
    {
        if (buildDir == null)
        {
            KeplerModulesDir kmd = findKeplerModulesDir();
            if (kmd != null)
                setKeplerModulesDir(kmd);
        }
        return buildDir;
    }

    public static File getBuildResourcesDir()
    {
        return new File(getBuildDir(), "resources");
    }

    public static File getBuildResourcesDir(String type)
    {
        return new File(getBuildResourcesDir(), type);
    }

    public static File getBuildResourceFile(String type, String name)
    {
        return new File(getBuildResourcesDir(type), name);
    }

    public static File getUserBuildDir()
    {
        if (userBuildDir == null)
        {
            setKeplerModulesDir(findKeplerModulesDir());
            userBuildDir = new File(getUserKeplerModulesDir(), "build-area");
        }
        return userBuildDir;
    }
    
    /** Set the user build directory. */
    public static void setUserBuildDir(File dir)
    {
    	userBuildDir = dir;
    }

    /**
     * set the ant project dir
     *
     * @param project
     */
    public static void setAntProject(Project project)
    {
        ProjectLocator.project = project;
    }

    /**
     * get the ant project dir
     *
     * @return
     */
    public static Project getAntProject()
    {
        return project;
    }
    
    /**
     * Guess at the "application modules directory"
     * @return The "application modules directory", e.g. 
     * /Applications/Kepler/Kepler.app/Contents/Resources/Java/ for OS X
     * Get rid of this when possible, it's not guaranteed to be right.
     * null if none
     */
    public static File getApplicationModulesDir(){
    		    	
        RunClasspath runClasspath = new RunClasspath();
    	String userKeplerModulesDir = ProjectLocator.getUserKeplerModulesDir().toString();

        if (runClasspath != null && runClasspath.toString() == null)
        {
        	String[] classpathItems = runClasspath.toString().split(File.pathSeparator);
        	for (String path: classpathItems)
        	{
        		if (path.matches(".*" + File.separator + Module.RELEASED_MODULE_NAME_PATTERN + File.separator + ".*"))
        		{
        			String[] pathParts = path.split(Module.RELEASED_MODULE_NAME_PATTERN);
        			String possibleNonUserKeplerModulesAppDir = pathParts[0];

        			if (!possibleNonUserKeplerModulesAppDir.endsWith(File.separator))
        			{
        				possibleNonUserKeplerModulesAppDir = possibleNonUserKeplerModulesAppDir.concat(File.separator);
        			}
        			if (!userKeplerModulesDir.endsWith(File.separator))
        			{
        				userKeplerModulesDir = userKeplerModulesDir.concat(File.separator);
        			}

        			if (!possibleNonUserKeplerModulesAppDir.equals(userKeplerModulesDir))
        			{
        				File possibleAppDir = new File(possibleNonUserKeplerModulesAppDir);
        				if (possibleAppDir.isDirectory())
        				{
        					return possibleAppDir;
        				}
        			}
        		}
        	}
        }
        
    	// try to find it via runner.Kepler.class
        String codePath = Kepler.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File codeDir = new File(codePath);
        File basedir = codeDir.getParentFile();
        String baseDirPath = basedir.getAbsolutePath();
        
        if (baseDirPath.indexOf("%20") != -1)
        {
        	//put in quotes and get rid of the %20
        	baseDirPath = baseDirPath.replaceAll("%20", "\\ ");
        }
		if (!baseDirPath.endsWith(File.separator))
		{
			baseDirPath = baseDirPath.concat(File.separator);
		}
        
		if (!baseDirPath.equals(userKeplerModulesDir))
		{
			File possibleAppDir = new File(baseDirPath);
			return possibleAppDir;
		}
		
		return null;
    }

}
