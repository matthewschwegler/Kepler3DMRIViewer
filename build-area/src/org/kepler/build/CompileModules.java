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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.optional.depend.Depend;
import org.apache.tools.ant.types.Path;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.CompileClasspath;

/**
 * class to compile the modules
 *
 * @author Sean Riddle
 * @author David Welker
 */
public class CompileModules extends ModulesTask
{
    protected String moduleName = "undefined";
    protected String compilerArgs = "undefined";
    protected boolean debug = false;
    protected boolean depend = false;
    protected boolean dependClasspath = false;
    protected boolean dependClosure = false;
    protected List<String> exclude = new ArrayList<String>();
    protected List<String> include = new ArrayList<String>();

    /**
     * set the module to compile
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        this.moduleName = moduleName;
    }

    /**
     * set any compiler arguments
     *
     * @param compilerArgs
     */
    public void setCompilerArgs(String compilerArgs)
    {
        this.compilerArgs = compilerArgs;
    }

    /**
     * set whether this is a debug compilation
     *
     * @param debug
     */
    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    /**
     * set whether to use java dependencies to delete out of date files
     *
     * @param depend
     */
    public void setDepend(boolean depend)
    {
        this.depend = depend;
    }

    /**
     * set whether Depend should use the classpath
     *
     * @param dependClasspath
     */
    public void setDependClasspath(boolean dependClasspath)
    {
        this.dependClasspath = dependClasspath;
    }

    /**
     * set whether Depend should use closure
     *
     * @param dependClosure
     */
    public void setDependClosure(boolean dependClosure)
    {
        this.dependClosure = dependClosure;
    }

    /**
     * set any includes
     *
     * @param include
     */
    public void setInclude(String include)
    {
        if (include.equals("undefined"))
        {
            return;
        }
        String[] includeArray = include.split(",");
        for (String moduleToInclude : includeArray)
        {
            this.include.add(moduleToInclude);
        }
    }

    /**
     * set any excludes
     *
     * @param exclude
     */
    public void setExclude(String exclude)
    {
        if (exclude.equals("undefined"))
        {
            return;
        }
        String[] excludeArray = exclude.split(",");
        for (String moduleToExclude : excludeArray)
        {
            this.exclude.add(moduleToExclude);
        }
    }

    /**
     * set any skips.  any module set here will not be compiled
     *
     * @param skip
     */
    public void setSkip(String skip)
    {
        setExclude(skip);
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        // If a particular module is not specified, compile all modules in the
        // suite specified in modules.txt.
        if (moduleName.equals("undefined"))
        {
            compileSuite();
        }
        else
        {
            compileSingleModule();
        }
    }

    /**
     * Go through the modules in reverse order of priority and compile them.
     */
    protected void compileSuite()
    {
        for (Module module : moduleTree.reverse())
        {
            if (include.size() == 0 || include.contains(module.getName()))
            {
                if (!exclude.contains(module.getName()))
                {
                    compile(module);
                }
            }
        }
    }

    /**
     * Compile the module that was passed in (i.e. -Dmodule=<module.name>)
     */
    protected void compileSingleModule()
    {
        compile(Module.make(moduleName));
    }

    /**
     * compile the given module
     *
     * @param module
     */
    protected void compile(Module module)
    {
        String mn = module.getName();

        if (mn.equals(Module.PTOLEMY) || mn.matches(Module.PTOLEMY+"-\\d+\\.\\d+") 
        		|| mn.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+"))
        {
            if (module.getTargetClasses().isDirectory() && ptolemyCompiled.exists())
            {
                String name = moduleTree.getHighestPriorityModule().getName();
                if (!name.equals(Module.PTOLEMY) && !name.matches(Module.PTOLEMY+"-\\d+\\.\\d+") 
                		|| !name.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+"))
                {
                    return;
                }
            }
        }

        File src = module.getSrc();
        /*
         * Do a sanity check. If there is
         *
         * (1) No src directory, or (2) The src directory is empty, or (3) The
         * only file in the src directory is an .svn file
         *
         * Then, do not compile.
         */
        if (!src.isDirectory() || src.list().length == 0 || src.list().length == 1 && src.list()[0].equals(".svn"))
        {
            return;
        }

        boolean osDependentExcludes = setOSDependentExcludes(module);

        System.out.println("Compiling " + module + "...");
        Javac javac = new Javac();
        javac.bindToOwner(this);
        javac.setIncludeantruntime(true);
        javac.setSrcdir(module.getSrcPath());
        File classes = module.getTargetClasses();
        classes.mkdirs();
        javac.setDestdir(classes);
        CompileClasspath classpath = new CompileClasspath(module);
        if (debug)
        {
            classpath.print();
        }
        //System.out.println("classpath: " + classpath);
        javac.setClasspath(classpath);
        javac.setDebug(true);
        javac.setFork(true);
        javac.setMemoryMaximumSize("1024m");
        javac.setFailonerror(true);

        // Depend helps Javac by deleting out of date class files by
        // determining dependencies rather than just timestamps.
        Depend dep = null;
        if (this.depend) {
            dep = new Depend();
            dep.bindToOwner(this);
            dep.setSrcdir(module.getSrcPath());

            if (this.dependClasspath) {
                // Ant assumes that anything in the path that's not a directory
                // is a jar.  Kepler adds a lot of non-jar files to the
                // classpath including *.htm and *.in.  If you don't filter them
                // out of the classpath for Depend, it will throw an exception.
                Path cleanClasspath = new Path(classpath.getProject());
                for (String s : classpath.list()) {
                    if (s.toLowerCase().endsWith(".jar") || new File(s).isDirectory()) {
                        cleanClasspath.setPath(s);
                    }
                }
            } else {
                dep.createClasspath();
            }

            Path path = new Path(module.getSrcPath().getProject(), classes.getAbsolutePath());
            dep.setDestDir(path);
            dep.setWarnOnRmiStubs(true);
    
            if (this.dependClosure) {
                dep.setClosure(false);
            }
        }

        if (osDependentExcludes)
        {
            javac.setExcludesfile(new File(basedir, "build-area/settings/os-dependent-excludes"));

            if (this.depend) {
                dep.setExcludesfile(new File(basedir, "build-area/settings/os-dependent-excludes"));
            }
        }

        if (mn.equals(Module.PTOLEMY) || mn.matches(Module.PTOLEMY+"-\\d+\\.\\d+") || 
        		mn.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+"))
        {
            javac.setIncludesfile(new File(basedir, "build-area/settings/ptolemy-includes"));
            if (this.depend) {
                dep.setIncludesfile(new File(basedir, "build-area/settings/ptolemy-includes"));
            }

            javac.setExcludesfile(new File(basedir, "build-area/settings/ptolemy-excludes"));
            if (this.depend) {
                dep.setExcludesfile(new File(basedir, "build-area/settings/ptolemy-excludes"));
            }
        }

        if (this.depend) {
            File baseCache = new File("./depcache");
            if (! baseCache.exists()) {
                baseCache.mkdirs();
            }
    
            // Depend uses this path.  In practice, it will create
            // ./depcache/<modulename>/dependencies.txt
            File modDep = new File(baseCache, module.toString());
            dep.setCache(modDep);
    
            dep.execute();
        }

        if (!compilerArgs.equals("undefined"))
        {
            String[] args = compilerArgs.split(",");
            for (String arg : args)
            {
                javac.createCompilerArg().setLine(arg);
            }
        }
        javac.execute();

        if (mn.equals(Module.PTOLEMY) || mn.matches(Module.PTOLEMY+"-\\d+\\.\\d+") || 
        		mn.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+") || mn.startsWith(Module.PTOLEMY_FOR+"-"))
        {
            if (!this.ptolemyHead.exists())
            {
                try
                {
                    ptolemyCompiled.createNewFile();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * set excludes on any files that are not for the target OS. this is set in
     * a modules module-info/osextension.txt file. See the appleExtensions
     * module for an example.
     */
    private boolean setOSDependentExcludes(Module m)
    {
        File osextensionsFile = new File(basedir, m + "/module-info/osextension.txt");
        // System.out.println("osextensionFile: " +
        // osextensionsFile.getAbsolutePath());
        if (!osextensionsFile.exists())
        {
            return false;
        }

        try
        {
            boolean returnVal = false;
            String currentOSName = System.getProperty("os.name");

            // System.out.println("Found OS Extension file: " +
            // osextensionsFile.getAbsolutePath());
            Hashtable<String, String> properties = readOSExtensionFile(osextensionsFile);
            Enumeration<String> keys = properties.keys();
            File excludeFile = new File(basedir, "build-area/settings/os-dependent-excludes");
            excludeFile.delete();
            while (keys.hasMoreElements())
            {
                String className = keys.nextElement();
                String os = properties.get(className);

                // System.out.println("currentOS: " + currentOSName + "   os: "
                // + os);
                if (!currentOSName.trim().equals(os.trim()))
                {   // if we're in an OS that an extension needs to be loaded for
                    // attempt to load the OSExtension via reflection and
                    // run the addOSExtension method
                    System.out.println("adding compilation exclude: class " + className + " will only be compiled on " + os);

                    className += "\n";
                    addToExclude(className, excludeFile);

                    returnVal = true;
                }
            }

            /*
             * System.out.println("Classes excluded from this compilation: ");
             * FileReader fr = new FileReader(excludeFile); StringBuffer sb =
             * new StringBuffer(); char[] c = new char[1024]; int numread =
             * fr.read(c, 0, 1024); while(numread != -1) { sb.append(c, 0,
             * numread); numread = fr.read(c, 0, 1024); }
             *
             *
             * String excludesFileStr = sb.toString();
             * System.out.println(excludesFileStr);
             * System.out.println("To remove these excludes, change the OS in "
             * + "module " + m +
             * "'s osextensions.txt file or delete build-area/os-dependent-excludes"
             * );
             */
            return returnVal;
        }
        catch (Exception e)
        {
            System.out.println("Could not process osextension.txt file for module " + m + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * adds a class to the excludes file if it does not already exist there.
     */
    private static void addToExclude(String classname, File excludeFile) throws Exception
    {
        if (excludeFile.exists())
        {
            FileReader fr = new FileReader(excludeFile);
            StringBuffer sb = new StringBuffer();
            char[] c = new char[1024];
            int numread = fr.read(c, 0, 1024);
            while (numread != -1)
            {
                sb.append(c, 0, numread);
                numread = fr.read(c, 0, 1024);
            }

            String propertiesStr = sb.toString();
            String[] props = propertiesStr.split("\n");
            boolean add = true;
            for (int i = 0; i < props.length; i++)
            {
                // System.out.println("classname: " + classname + "   props: " +
                // props[i]);
                if (classname.trim().equals(props[i].trim()))
                {
                    add = false;
                    break;
                }
            }

            if (add)
            {
                FileWriter fw = new FileWriter(excludeFile, true);
                classname = classname.trim().replaceAll("\\.", "/") + ".java\n";
                fw.write(classname, 0, classname.length());
                fw.flush();
                fw.close();
            }
        }
        else
        {
            FileWriter fw = new FileWriter(excludeFile, true);
            classname = classname.trim().replaceAll("\\.", "/") + ".java\n";
            fw.write(classname, 0, classname.length());
            fw.flush();
            fw.close();
        }
    }

    /**
     * read the osextension.txt file and return a hashtable of the properties
     * <p/>
     * NOTE this method is duplicated in Kepler.java. Change both if you change one.
     */
    private static Hashtable<String, String> readOSExtensionFile(File f) throws Exception
    {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        FileReader fr = new FileReader(f);
        StringBuffer sb = new StringBuffer();
        char[] c = new char[1024];
        int numread = fr.read(c, 0, 1024);
        while (numread != -1)
        {
            sb.append(c, 0, numread);
            numread = fr.read(c, 0, 1024);
        }

        String propertiesStr = sb.toString();
        //String[] props = propertiesStr.split("\n");
        String[] props = propertiesStr.split(";");
        for (int i = 0; i < props.length; i++)
        {
            String token1 = props[i];
            StringTokenizer st2 = new StringTokenizer(token1, ",");
            String key = st2.nextToken();
            String val = st2.nextToken();
            properties.put(key, val);
        }

        return properties;
    }

}
