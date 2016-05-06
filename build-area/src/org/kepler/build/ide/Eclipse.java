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
package org.kepler.build.ide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.kepler.build.modules.Module;
import org.kepler.build.project.Classpath;
import org.kepler.build.project.LibPath;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.util.DotKeplerManager;

/**
 * Clean generated eclipse files
 * Created by David Welker.
 * Date: Aug 25, 2008
 * Time: 11:29:03 AM
 */
public class Eclipse extends Ide
{

    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        for (Module module : moduleTree)
        {
            if (!module.getDir().isDirectory())
            {
                continue;
            }
            makeProjectFile(module);
            makeClasspathFile(module);
        }
        
        // create .project and .classpath for build-area
        Module buildModule = Module.make("build-area");
        makeProjectFile(buildModule);
        makeClasspathFile(buildModule);
    }

    /**
     * make a project file for a module
     */
    private void makeProjectFile(Module module)
    {
        // XXX why is build-area called something else?
        String name = module.toString();
        if(name.equals("build-area"))
        {
            name = "_kepler.build";
        }
        
        File projectFile = new File(module.getDir(), ".project");
        PrintWriter pw = FileMerger.getPrintWriter(projectFile);
        pw.println("<projectDescription>");
        pw.println("  <name>" + name + "</name>");
        pw.println("  <projects/>");
        pw.println("  <buildSpec>");
        pw.println("    <buildCommand>");
        pw.println("      <name>org.eclipse.jdt.core.javabuilder</name>");
        pw.println("    </buildCommand>");
        pw.println("  </buildSpec>");
        pw.println("  <natures>");
        pw.println("    <nature>org.eclipse.jdt.core.javanature</nature>");
        pw.println("  </natures>");
        
        // add the workflow demos directory so that the demos may be accessed
        // by ptolemy.actor.gui.HTMLViewer when showing the Help documentation
        // see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5194
        File moduleWorkflowDir = new File(DotKeplerManager.getInstance()
                .getPersistentModuleWorkflowsDirString()
                + File.separator
                + module.getName()
                + File.separator
                + "demos");
        if (moduleWorkflowDir.exists() && moduleWorkflowDir.isDirectory()) {
            pw.println("  <linkedResources>");
            pw.println("    <link>");
            pw.println("      <name>demos</name>");
            pw.println("      <type>2</type>");
            pw.println("      <location>" + moduleWorkflowDir + "</location>");
            pw.println("    </link>");
            pw.println("  </linkedResources>");
        }
        
        pw.println("</projectDescription>");
        pw.close();
        System.out.println("Writing " + projectFile);
    }

    /**
     * make a classpath file for a module
     */
    private void makeClasspathFile(Module module)
    {

        String moduleName = module.getName();
        String versionSuffix = "-\\d+\\.\\d+(\\.\\d+)?";
        if (moduleName.equals(Module.PTOLEMY) ||
            moduleName.matches(Module.PTOLEMY+versionSuffix) ||
            moduleName.matches(Module.PTOLEMY_KEPLER+versionSuffix))
        {
            makePtolemyClasspathFile(module);
            return;
        }
        if (moduleName.equals(Module.COMMON) ||
            moduleName.matches(Module.COMMON+versionSuffix))
        {
            makeCommonClasspathFile(module);
            return;
        }

        File src = module.getSrc();
        File testsSrc = module.getTestsSrc();
        File resources = module.getResourcesDir();
        File configs = module.getConfigsDir();
        File lib = module.getLibDir();
        File libImages = module.getLibImagesDir();

        File classpathFile = new File(module.getDir(), ".classpath");
        PrintWriter pw = FileMerger.getPrintWriter(classpathFile);
        pw.println("<classpath>");
        pw
                .println("  <classpathentry kind=\"output\" path=\"target/eclipse/classes\"/>");
        if (src.isDirectory())
        {
            // Have to use a full path here and avoid "Build path contains duplicate entry: 'src' for project 'ptolemy'"
            //pw.println("  <classpathentry kind=\"lib\" path=\"" + module.getDir() + "/src\"/>");

            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"src\"/>");
        }
        if (testsSrc.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"tests/src\"/>");
        }
        if (configs.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"configs\"/>");
        }
        if (lib.isDirectory())
        {
            String excludesStr = getLibExcludePath(lib.getAbsolutePath(), module);
            pw
                    .println("  <classpathentry excluding=\"" + excludesStr + "**/.svn/|**/CVS/|images/\" kind=\"src\" path=\"lib\">");
            pw
                    .println("    <attributes>");
            pw
                    .println("      <attribute name=\"org.eclipse.jdt.launching.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY\" " +
                            "value=\"" + module.getName() + "/lib\"/>");
            pw
                    .println("    </attributes>");
            pw
                    .println("  </classpathentry>");
        }
        printNativeLibraryPaths(pw, module);

        if (libImages.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"lib/images\"/>");
        }
        if (resources.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/|**/*.java\" kind=\"src\" path=\"resources\"/>");
        }
        // add the workflow demos directory so that the demos may be accessed
        // by ptolemy.actor.gui.HTMLViewer when showing the Help documentation
        // see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5194
        File moduleWorkflowDir = new File(DotKeplerManager.getInstance()
                .getPersistentModuleWorkflowsDirString()
                + File.separator
                + module.getName()
                + File.separator
                + "demos");
        if (moduleWorkflowDir.exists() && moduleWorkflowDir.isDirectory()) {
            pw.println("  <classpathentry excluding=\"**.svn/|**/CVS/|**/*.java|**/*.class\" kind=\"src\" " +
                    "path=\"demos\"/>");
        }

        pw
                .println("  <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>");
        printJarEntries(pw, module);
        printModuleDependencies(pw, module);
        pw.println("</classpath>");
        pw.close();
        System.out.println("Writing " + classpathFile);
    }

    /**
     * make a common classpath file for a module
     */
    private void makeCommonClasspathFile(Module module)
    {
        File src = module.getSrc();
        File testsSrc = module.getTestsSrc();
        File resources = module.getResourcesDir();
        File configs = module.getConfigsDir();
        File lib = module.getLibDir();
        File libImages = module.getLibImagesDir();

        File classpathFile = new File(module.getDir(), ".classpath");
        PrintWriter pw = FileMerger.getPrintWriter(classpathFile);
        pw.println("<classpath>");
        pw
                .println("  <classpathentry kind=\"output\" path=\"target/eclipse/classes\"/>");
        if (src.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"src\"/>");
            // Have to use a full path here and avoid "Build path contains duplicate entry: 'src' for project 'ptolemy'"
            //pw.println("  <classpathentry kind=\"lib\" path=\"" + module.getDir() + "/src\"/>");

        }
        if (testsSrc.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"tests/src\"/>");
        }
        if (configs.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"configs\"/>");
        }
        if (lib.isDirectory())
        {
            String excludesStr = getLibExcludePath(lib.getAbsolutePath(), module);
            pw
                    .println("  <classpathentry excluding=\"" + excludesStr + "**/.svn/|**/CVS/|images/|testdata/\" kind=\"src\" path=\"lib\">");
            pw
                    .println("    <attributes>");
            pw
                    .println("      <attribute name=\"org.eclipse.jdt.launching.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY\" " +
                            "value=\"" + module.getName() + "/lib\"/>");
            pw
                    .println("    </attributes>");
            pw
                    .println("  </classpathentry>");
        }
        printNativeLibraryPaths(pw, module);

        if (resources.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"resources\"/>");
        }
        if (libImages.isDirectory())
        {
            pw
                    .println("  <classpathentry excluding=\"**/.svn/|**/CVS/\" kind=\"src\" path=\"lib/images\"/>");
        }
        pw
                .println("  <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>");
        printJarEntries(pw, module);
        printModuleDependencies(pw, module);
        
        // export the javafx jar if found
        String jfxJarPath = Classpath.getJavaFXJarPath();
        if(jfxJarPath != null) {
            pw.println("  <classpathentry exported=\"true\" kind=\"lib\" path=\""
                    + jfxJarPath + "\"/>");
        }
        
        pw.println("</classpath>");
        pw.close();
        System.out.println("Writing " + classpathFile);
    }

    /**
     * make a ptolemy claspath file for a module
     */
    private void makePtolemyClasspathFile(Module module)
    {
        File classpathFile = new File(module.getDir(), ".classpath");
        PrintWriter pw = FileMerger.getPrintWriter(classpathFile);
   
        // write the header
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<classpath>");
        pw.println("    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>");
        
        
        // add excludes
        final File excludesFile = new File(basedir, "build-area/settings/ptolemy-excludes");
        BufferedReader reader = null;
        
        try
        {
            reader = new BufferedReader(new FileReader(excludesFile));
        }
        catch(FileNotFoundException e)
        {
            PrintError.message("Could not find ptolemy-excludes file.", e);
        }
        
        if(reader != null)
        {
            pw.print("  <classpathentry excluding=\"**/*.c|**/*.class|**/*.htm|**/*.html" +
                "|**/*.jnlp|**/*.tcl|**/*svn*|**/*svn-base|**/README.txt|" +
                "**/makefile");
            
            // read the ptolemy-excludes file
            try
            {
                String line = reader.readLine();
                while(line != null)
                {
                    pw.print("|" + line.replaceAll("\n", ""));
                    line = reader.readLine();
                }
                reader.close();
            }
            catch(IOException e)
            {
                PrintError.message("Error reading ptolemy-excludes file.", e);
            }       
            
            pw.println("\" including=\"com/|diva/|ptolemy/|util/|org/\" kind=\"src\" path=\"src\"/>");
        }
                
        // add the jars in ptolemy/src/lib/
        File srcFile = module.getSrc();
        File srcLibFile = new File(srcFile, "lib");
        FileSet srcLibJarFileset = new FileSet();
        srcLibJarFileset.setProject(ProjectLocator.getAntProject());
        srcLibJarFileset.setDir(srcLibFile);
        srcLibJarFileset.setIncludes("**/*.jar");

        Iterator<?> i = srcLibJarFileset.iterator();
        while (i.hasNext())
        {
            FileResource fr = (FileResource) i.next();
            String fn = fr.getFile().getName();
            pw.println("\t<classpathentry exported=\"true\" kind=\"lib\" path=\"src/lib/" + fn + "\"/>");
        }
        
        // add any jars in ptolemy/lib/
        printJarEntries(pw, module);
        
        // write the footer
        pw.println("  <classpathentry kind=\"output\" path=\"target/eclipse/classes\"/>");
        // Have to use a full path here and avoid "Build path contains duplicate entry: 'src' for project 'ptolemy'"
        //pw.println("  <classpathentry kind=\"lib\" path=\"" + module.getDir() + "/src\"/>");
        pw.println("</classpath>");

        
        pw.close();
        System.out.println("Writing " + classpathFile);
    }

    /**
     * print the jar entries
     */
    private void printJarEntries(PrintWriter pw, Module module)
    {
        //String module = moduleDir.getName();
        if (module.isReleased())
        {
            File targetJar = module.getTargetJar();
            if (targetJar.exists())
            {
                printJarEntry(pw, module, targetJar);
            }
        }

        if (!module.getLibDir().exists())
        {
            return;
        }

        FileSet jars = new FileSet();
        jars.setProject(getProject());
        jars.setDir(module.getLibDir());
        jars.setIncludes("**/*.jar");

        Iterator<Resource> i = jars.iterator();

        while (i.hasNext())
        {
            Resource resource = i.next();
            if(resource instanceof FileResource) {
                File jar = ((FileResource)resource).getFile();
                
                // do not add swt jar to classpath since eclipse comes with its own
                // version of swt, and adding it causes problems.
                // XXX this may only be a problem on Mac.
                Matcher matcher = SWT_PATTERN.matcher(jar.getName());
                if(matcher.matches())
                {
                    System.out.println("WARNING: not adding " + jar.getPath() + 
                        " to class the path since it appears to be an SWT jar.");
                }
                else
                {
                    printJarEntry(pw, module, jar);
                }
            }
        }
    }

    /**
     * print a jar entry
     *
     * @param pw
     * @param module
     * @param jar
     */
    public void printJarEntry(PrintWriter pw, Module module, File jar)
    {
        String jarPath = jar.getAbsolutePath();
        String modulePath = module.getDir().getAbsolutePath();
        jarPath = jarPath.substring(modulePath.length() + 1);

        pw.println("  <classpathentry exported=\"true\" kind=\"lib\" path=\""
                + jarPath + "\"/>");
    }

    /**
     * print module depends
     *
     * @param pw
     * @param module
     */
    private void printModuleDependencies(PrintWriter pw, Module module)
    {
        for (Module m : moduleTree.getLowerPriorityModules(module))
        {
            pw.println("  <classpathentry combineaccessrules=\"false\" kind=\"src\" path=\"/" + m + "\"/>");
        }
    }

    /**
     * Add entries for directories containing native libraries.
     *
     * @return Returns a list of directories added
     */
    private List<String> printNativeLibraryPaths(PrintWriter pw, Module module)
    {
        List<String> retval = new LinkedList<String>();
        String libPathStr = new LibPath(module).toString();
        String modulePathStr = module.getDir().getAbsolutePath();

        //System.out.println("lib path = " + libPathStr);
        if (libPathStr.length() > 0)
        {
            String[] paths = libPathStr.split(File.pathSeparator);
            for (String path : paths)
            {
                // skip <module>/lib since it has already been added
                if (path.equals(module.getLibDir().getAbsolutePath()))
                {
                    continue;
                }

                // get the path relative to the module
                String relPath = path.substring(modulePathStr.length() + 1);
                String excludeStr = getLibExcludePath(path, module);
                //System.out.println("excludes = " + excludeStr);

                pw.println("  <classpathentry excluding=\"" + excludeStr + "\" kind=\"src\" path=\"" + relPath + "\">");
                pw.println("    <attributes>");
                pw.println("      <attribute name=\"org.eclipse.jdt.launching.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY\" " +
                        "value=\"" + module.getName() + File.separatorChar + relPath + "\"/>");
                pw.println("    </attributes>");
                pw.println("  </classpathentry>");
            }
        }
        return retval;
    }

    /**
     * Get a string containing paths to exclude inside of a given path.
     */
    private String getLibExcludePath(String path, Module module)
    {
        StringBuilder excludes = new StringBuilder();
        String libPathStr = new LibPath(module).toString();
        if (libPathStr.length() > 0)
        {
            String[] paths = libPathStr.split(File.pathSeparator);
            for (String curPath : paths)
            {
                if (curPath.equals(path))
                {
                    continue;
                }
                else if (curPath.contains(path) &&
                        curPath.charAt(path.length()) == File.separatorChar)
                {
                    excludes.append(curPath.substring(path.length() + 1));
                    excludes.append(File.separatorChar);
                    excludes.append("|");
                }
            }
        }
        return excludes.toString();
    }
    
    /** Regex to match swt jar. */
    private final static Pattern SWT_PATTERN = Pattern.compile("swt.*\\.jar");

}
