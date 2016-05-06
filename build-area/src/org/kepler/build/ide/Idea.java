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

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.kepler.build.modules.Module;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.DirectoryIterator;
import org.kepler.build.util.IteratorIterable;

/**
 * create idea project files
 * Created by David Welker.
 * Date: Aug 24, 2008
 * Time: 8:42:35 PM
 */
public class Idea extends Ide
{
    private File ideaDir;

    /**
     * init the task
     */
    public void init()
    {
        super.init();
        ideaDir = new File(basedir, "build-area/resources/idea");
    }

    /**
     * merge a string to a printwriter
     */
    private void merge(String from, PrintWriter to)
    {
        FileMerger.merge(ideaDir, from, to);
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        makeProjectFile();
        makeWorkspace();
        for (Module module : moduleTree)
        {
            if (!module.getDir().isDirectory())
            {
                continue;
            }
            makeModuleFile(module);
        }
        makeMainModule();
    }

    /**
     * make a project file
     */
    private void makeProjectFile()
    {
        File projectFile = new File(basedir, "kepler.ipr");
        PrintWriter pw = FileMerger.getPrintWriter(projectFile);
        merge("ProjectIntro", pw);
        printProjectModules(pw);
        pw.close();
        System.out.println("Writing " + projectFile);
    }

    /**
     * create a workspace
     */
    private void makeWorkspace()
    {
        String runModule = Module.LOADER;
        try
        {
            runModule = moduleTree.getFirst().getName();
        }
        catch (IndexOutOfBoundsException ignored)
        {
        }

        File workspaceFile = new File(basedir, "kepler.iws");
        PrintWriter pw = FileMerger.getPrintWriter(workspaceFile);
        File workspaceStart = new File(ideaDir, "WorkspaceStart");
        FileMerger.merge(workspaceStart, pw);
        pw.println("      <module name=\"" + runModule + "\" />");
        File workspaceEnd = new File(ideaDir, "WorkspaceEnd");
        FileMerger.merge(workspaceEnd, pw);
        pw.close();
        System.out.println("Writing " + workspaceFile);
    }

    /**
     * print the project modules
     */
    private void printProjectModules(PrintWriter pw)
    {
        pw.println("  <component name='ProjectModuleManager'>");
        pw.println("    <modules>");
        for (Module module : moduleTree)
        {
            if (!module.getDir().exists())
            {
                continue;
            }
            pw.println("      <module fileurl=\"file://$PROJECT_DIR$/" + module + "/"
                    + module + ".iml\" filepath=\"$PROJECT_DIR$/" + module + "/" + module
                    + ".iml\" />");
        }
        pw
                .println("      <module fileurl=\"file://$PROJECT_DIR$/_kepler.build.iml\" filepath=\"$PROJECT_DIR$/_kepler.build.iml\" />");
        pw.println("    </modules>");
        pw.println("  </component>");
        pw.println("</project>");
    }

    /**
     * get the module print writer
     */
    private PrintWriter getModulePrintWriter(Module module)
    {
        if (!module.getDir().isDirectory())
        {
            return null;
        }
        File moduleFile = new File(module.getDir(), module + ".iml");
        System.out.println("Writing " + moduleFile);
        return FileMerger.getPrintWriter(moduleFile);
    }

    /**
     * make a main module
     */
    private void makeMainModule()
    {
        File mainModuleFile = new File(basedir, "_kepler.build.iml");
        PrintWriter pw = FileMerger.getPrintWriter(mainModuleFile);
        File mainModule = new File(ideaDir, "MainModule");
        FileMerger.merge(mainModule, pw);

        pw.close();
        System.out.println("Writing " + mainModuleFile);
    }

    /**
     * make a module file
     */
    private void makeModuleFile(Module module)
    {
        if (module.getName().equals("kepler-1.0-jar-tag"))
        {
            makeKepler10TagJarModule();
            return;
        }

        if (module.getName().equals(Module.PTOLEMY) || module.getName().matches(Module.PTOLEMY+"-\\d+\\.\\d+"))
        {
            makePtolemyModule();
            return;
        }

        PrintWriter pw = getModulePrintWriter(module);
        printModuleIntro(pw, module);
        printModuleJars(pw, module);
        printModuleDependencies(pw, module);
        printModuleEnd(pw);
        pw.close();
    }

    /**
     * make a kepler 1.0 tag
     */
    private void makeKepler10TagJarModule()
    {
        PrintWriter pw = getModulePrintWriter(Module.make("kepler-1.0-jar-tag"));
        File kepler10JarTagModule = new File(ideaDir, "Kepler1.0JarTagModule");
        FileMerger.merge(kepler10JarTagModule, pw);
        pw.close();
    }

    /**
     * make a ptolemy module
     */
    private void makePtolemyModule()
    {
        Module module = Module.make(getPtolemyModuleName());
        PrintWriter pw = getModulePrintWriter(module);
        File ptolemyModuleStart = new File(ideaDir, "PtolemyModuleStart");
        FileMerger.merge(ptolemyModuleStart, pw);

        File srcFile = module.getSrc();
        File srcLibFile = new File(srcFile, "lib");
        FileSet srcLibJarFileset = new FileSet();
        srcLibJarFileset.setProject(ProjectLocator.getAntProject());
        srcLibJarFileset.setDir(srcLibFile);
        srcLibJarFileset.setIncludes("** /*.jar");

        Iterator<Resource> i = srcLibJarFileset.iterator();
        while (i.hasNext())
        {
            Resource fr = i.next();
            if(fr instanceof FileResource) {
                String fn = ((FileResource)fr).getFile().getName();
                pw.println("    <orderEntry type=\"module-library\" exported=\"\">");
                pw.println("      <library>");
                pw.println("        <CLASSES>");
                pw.println("          <root url=\"jar://$MODULE_DIR$/src/lib/" + fn + "!/\" />");
                pw.println("        </CLASSES>");
                pw.println("        <JAVADOC />");
                pw.println("        <SOURCES />");
                pw.println("      </library>");
                pw.println("    </orderEntry>");
            }
        }

        File ptolemyModuleEnd = new File(ideaDir, "PtolemyModuleEnd");
        FileMerger.merge(ptolemyModuleEnd, pw);
        pw.close();
    }

    private String getPtolemyModuleName()
    {
        for (Module module : moduleTree)
        {
            if (module.isPtolemy())
            {
                return module.getName();
            }
        }
        return Module.PTOLEMY;

    }
/*
    <orderEntry type="module-library" exported="">
      <library>
        <CLASSES>
          <root url="jar://$MODULE_DIR$/src/lib/kieler.jar!/" />
        </CLASSES>
        <JAVADOC />
        <SOURCES />
      </library>
    </orderEntry>
*/

    /**
     * print the module intro
     */
    private void printModuleIntro(PrintWriter pw, Module module)
    {

        File moduleIntroStart = new File(ideaDir, "ModuleIntroStart");
        FileMerger.merge(moduleIntroStart, pw);
        String base = module.getDir().getAbsolutePath();
        // Exclude the top level of all directories that aren't src or tests
        for (File directory : new IteratorIterable<File>(new DirectoryIterator(
                module.getDir(), new String[]{"src", "tests"})))
        {
            String directoryStr = directory.getAbsolutePath();
            if (!directoryStr.startsWith(base))
            {
                System.out.println("Error excluding '" + directoryStr + "' from '"
                        + base + "'");
            }
            else
            {
                pw.println("      <excludeFolder url=\"file://$MODULE_DIR$"
                        + directoryStr.substring(base.length()) + "\" />");
            }
        }

        // Exclude the top level of all directories in tests that aren't src
        for (File directory : new IteratorIterable<File>(new DirectoryIterator(
                new File(module.getDir(), "tests"), new String[]{"src"})))
        {
            String directoryStr = directory.getAbsolutePath();
            if (!directoryStr.startsWith(base))
            {
                System.out.println("Error exluding '" + directoryStr + "' from '"
                        + base + "'");
            }
            else
            {
                pw.println("      <excludeFolder url=\"file://$MODULE_DIR$"
                        + directoryStr.substring(base.length()) + "\" />");
            }
        }
        File moduleIntroEnd = new File(ideaDir, "ModuleIntroEnd");
        FileMerger.merge(moduleIntroEnd, pw);
    }

    /**
     * print module depends
     */
    private void printModuleDependencies(PrintWriter pw, Module module)
    {
        for (Module m : moduleTree.getLowerPriorityModules(module))
        {
            pw
                    .println("    <orderEntry type=\"module\" module-name=\"" + m
                            + "\" />");
        }
    }

    /**
     * print module jars
     */
    private void printModuleJars(PrintWriter pw, Module module)
    {
        File libDir = module.getLibDir();
        if (!libDir.exists())
        {
            return;
        }

        FileSet jars = new FileSet();
        jars.setProject(getProject());
        jars.setDir(libDir);
        jars.setIncludes("**/*.jar");

        Iterator<Resource> i = jars.iterator();

        while (i.hasNext())
        {
            Resource resource = i.next();
            if(resource instanceof FileResource) {
                File jar = ((FileResource)resource).getFile();
                printModuleJar(pw, module, jar);
            }
        }
    }

    /**
     * print module jar
     *
     * @param pw
     * @param module
     * @param jar
     */
    private void printModuleJar(PrintWriter pw, Module module, File jar)
    {
        String jarPath = jar.getAbsolutePath();
        String modulePath = module.getDir().getAbsolutePath();
        jarPath = jarPath.substring(modulePath.length());

        pw.println("    <orderEntry type=\"module-library\" exported=\"\">");
        pw.println("      <library>");
        pw.println("        <CLASSES>");
        pw
                .println("          <root url=\"jar://$MODULE_DIR$" + jarPath
                        + "!/\" />");
        pw.println("        </CLASSES>");
        pw.println("        <JAVADOC />");
        pw.println("        <SOURCES />");
        pw.println("      </library>");
        pw.println("    </orderEntry>");
    }

    /**
     * print module end
     *
     * @param pw
     */
    private void printModuleEnd(PrintWriter pw)
    {
        File moduleIntro = new File(ideaDir, "ModuleEnd");
        FileMerger.merge(moduleIntro, pw);
    }
}
