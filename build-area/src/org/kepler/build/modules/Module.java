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
package org.kepler.build.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.kepler.build.UpdatePresentTxt;
import org.kepler.build.UpdateReleasedTxt;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.build.util.Version;

/**
 * Class that represents a module
 *
 * @author welker
 */
public class Module
{
    protected String name;
    protected boolean upArrow = false;
    protected String location;
    protected File dir;
    protected File src;
    protected File lib;
    protected File target;
    protected File targetClasses;
    protected File targetJar;
    protected File resources;
    protected File karResourcesDir;
    protected File systemPropertiesDir;
    /**
     * the 'old' module/configs location *
     */
    protected File configs;
    /**
     * the 'new' module/resources/configurations location. Use this for
     * all development after Sept. 2009
     */
    protected File configurations;
    protected File configurationDirectivesDir;
    protected File libImages;
    protected File tests;
    protected File testsSrc;
    protected File testsResources;
    protected File testsClasses;
    protected File testsWorkflows;
    protected File moduleInfoDir;
    protected ModulesTxt modulesTxt;
    protected File licensesTxt;
    protected File osTxt;
    protected File documentationDir;
    protected File workflowsDir;
    protected File demosDir;
    protected boolean isSuite;
    boolean hasModulesTxtBeenRead = false;
    
    private static List<String> present = null;
    private static List<String> released = null;

    public static final String PTOLEMY_LOCATION = "https://repo.eecs.berkeley.edu/svn-anon/projects/eal/ptII/trunk";
    
    public static final String PTOLEMY = "ptolemy";
    public static final String PTOLEMY_8_0 = "ptolemy-8.0";
    public static final String PTOLEMY_KEPLER = "ptolemy-kepler";
    public static final String PTOLEMY_KEPLER_2_2 = "ptolemy-kepler-2.2";
//    public static final String PTOLEMY_KEPLER_2_3 = "ptolemy-kepler-2.3";
    public static final String PTOLEMY_FOR = "ptolemy-for";
    public static final String PTOLEMY_LIB = "ptolemy-lib";
    public static final String KEPLER = "kepler";
    public static final String COMMON = "common";
    public static final String LOADER = "loader";
    
    public static final String RELEASED_MODULE_NAME_PATTERN = "[a-zA-Z-]+-\\d+\\.\\d+\\.\\d+";



    /**
     * constructor
     *
     * @param module
     * @return
     */
    public static Module make(Module module)
    {
        if (module.isSuite())
        {
            return Suite.make(module.getName(), module.getLocation());
        }
        return Module.make(module.getName(), module.getLocation());
    }

    /**
     * make a module with a given name
     *
     * @param name
     * @return
     */
    public static Module make(String name)
    {
        if (name.startsWith("*"))
        {
            return Suite.make(name.substring(1));
        }
        return new Module(name);
    }

    /**
     * make a module with a name and location
     *
     * @param name
     * @param location
     * @return
     */
    public static Module make(String name, String location)
    {
        if (name.startsWith("*"))
        {
            return Suite.make(name.substring(1), location);
        }
        return new Module(name, location);
    }

    /**
     * constructor
     *
     * @param name
     */
    protected Module(String name)
    {
        this.name = name.startsWith("*") ? name.substring(1) : name;
        if (name.endsWith("^"))
        {
            this.name = transformName(name);
            name = this.name;
        }
        location = RepositoryLocations.getLocation(name);
        isSuite = false;

        dir = new File(ProjectLocator.getKeplerModulesDir(), name);
        if (ProjectLocator.shouldUtilizeUserKeplerModules())
        {
            if (!dir.isDirectory())
            {
                dir = new File(ProjectLocator.getUserKeplerModulesDir(), name);
            }
        }

        if (isPtolemy())
        {
            this.location = PTOLEMY_LOCATION;
        }

        src = new File(dir, "src");
        resources = new File(dir, "resources");
        tests = new File(dir, "tests");
        testsSrc = new File(tests, "src");
        moduleInfoDir = new File(dir, "module-info");

        File moduleDirConfigPropertiesFile = new File(moduleInfoDir, "module-dir-config.properties");
        if( moduleDirConfigPropertiesFile.exists() )
        {
            Properties moduleDirConfigProperties = new Properties();
            try
            {
                Reader reader = null;
                try {
                    reader =  new FileReader(moduleDirConfigPropertiesFile);
                    moduleDirConfigProperties.load(reader);
                } finally {
                    if(reader != null) {
                        reader.close();
                    }
                }
                if( moduleDirConfigProperties.containsKey("src") )
                {
                    src = new File(dir, moduleDirConfigProperties.getProperty("src") );
                }
                if( moduleDirConfigProperties.containsKey("resources") )
                {
                    resources = new File(dir, moduleDirConfigProperties.getProperty("resources") );
                }
                if( moduleDirConfigProperties.containsKey("test-src") )
                {
                    testsSrc = new File(dir, moduleDirConfigProperties.getProperty("test-src") );
                }
                if( moduleDirConfigProperties.containsKey("test-resources") )
                {
                    testsResources = new File(dir, moduleDirConfigProperties.getProperty("test-resources") );
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }

        lib = new File(dir, "lib");
        target = new File(dir, "target");
        targetClasses = new File(target, "classes");
        if (name.matches(RELEASED_MODULE_NAME_PATTERN))
        {
            targetJar = new File(target, name.substring(0, name.length() - 2)
                    + ".jar");
        }
        else
        {
            targetJar = new File(target, name + ".jar");
        }

        karResourcesDir = new File(resources, "kar");
        systemPropertiesDir = new File(resources, "system.properties");
        configs = new File(dir, "configs");
        configurations = new File(resources, "configurations");
        configurationDirectivesDir = new File(resources, "configuration-directives");
        libImages = new File(lib, "images");

        testsClasses = new File(tests, "classes");
        testsWorkflows = new File(tests, "workflows");
        licensesTxt = new File(moduleInfoDir, "licenses.txt");
        modulesTxt = new ModulesTxt(moduleInfoDir, "modules.txt");
        osTxt = new File(moduleInfoDir, "os.txt");
        documentationDir = new File(resources, "documentation");
        workflowsDir = new File(dir, "workflows");
        demosDir = new File(workflowsDir, "demos");
    }

    /**
     * constructor
     *
     * @param name
     * @param location
     */
    protected Module(String name, String location)
    {
        this(name);
        if (location != null)
        {
            this.location = location;
        }
    }

    public static void reset()
    {
        present = null;
        released = null;
    }
    
    /*
     * Update the present.txt and Module.present static list to 
     * reflect what modules exist. Call this whenever you add 
     * (e.g. download) or remove (delete) a module on disk.
     */
    public static void updatePresentModuleList(){
        present = readPresent();
    }
    
    /*
     * Update the released.txt and Module.released static list to 
     * reflect what modules exist. Call this before attempting to 
     * download modules.
     */
    public static void updateReleasedModuleList(){
        released = readReleased();
    }

    /**
     * read the present present.txt file
     *
     * @return
     */
    private static List<String> readPresent()
    {
        UpdatePresentTxt updatePresentTxt = new UpdatePresentTxt();
        updatePresentTxt.execute();
        return readListFromFilename("present.txt");
    }

    /**
     * read the released.txt file
     *
     * @return
     */
    public static List<String> readReleased()
    {
        UpdateReleasedTxt updateReleasedTxt = new UpdateReleasedTxt();
        updateReleasedTxt.execute();
        return readListFromFilename("released.txt");
    }

    /**
     * get a list from a file name
     *
     * @param filename
     * @return
     */
    private static List<String> readListFromFilename(String filename)
    {
        try
        {
            return readListFromFile(ProjectLocator.getCacheFile(filename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * read a list from a file
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static List<String> readListFromFile(File file) throws IOException
    {
        List<String> list = new ArrayList<String>();
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null)
            {
                list.add(line);
            }
            return list;
        }
        finally
        {
            if(br != null)
            {
                br.close();
            }
        }
    }

    /**
     * transform an arrow name
     *
     * @param name
     * @return
     */
    private String tranformArrowNameForGet(String name)
    {
        if (!name.endsWith("^"))
        {
            return name;
        }
        if (released == null)
        {
            released = readReleased();
            if (released == null)
            {
                return name;
            }
        }
        return transformNameWithList(name, released);
    }

    /**
     * transform a name
     *
     * @param name
     * @return
     */
    private String transformName(String name)
    {
        if (!name.endsWith("^"))
        {
            return name;
        }
        upArrow = true;
        // XXX note: skipping the readPresent() call (which probably speeds things up) when present !=null 
        // will only work if we always Module.updatePresentModulesList() after changing
        // what modules exist in the system. e.g. see end of ModulesDownloader.downloadModules
        if (present == null)
        {
            present = readPresent();
            if (present == null)
            {
                return name;
            }
        }
        String transformedName = transformNameWithList(name, present);
        if (transformedName.endsWith("^"))
        {
            transformedName = tranformArrowNameForGet(name);
        }
        return transformedName;
    }

    /**
     * transform a name with a list
     *
     * @param name
     * @param list
     * @return
     */
    public String transformNameWithList(String name, List<String> list)
    {
        String pattern = name.substring(0, name.length() - 2);
        String bestCandidate = name;
        int patch = -1;
        for (String candidate : list)
        {
            if (candidate.startsWith(pattern))
            {
                String[] parts = candidate.split("\\.");
                String lastPart = parts[parts.length - 1];
                int lastPartInt = Integer.parseInt(lastPart);
                if (lastPartInt > patch)
                {
                    patch = lastPartInt;
                    bestCandidate = candidate;
                }
            }
        }
        return bestCandidate;
    }

    /**
     * change the name
     *
     * @param name
     */
    public void changeName(String name)
    {
        this.name = name;
    }

    /**
     * make a module part of a suite
     */
    public Suite asSuite()
    {
        return (Suite) this;
    }

    /**
     * get the name
     *
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the base name of the module, assuming it is in the following
     * format: basename-[major version number]-[minor version number]-[micro
     * version number][revision level, like alpha, beta, etc.][revision number].
     * Example: common-1.0rc3. The micro revision can only be used if a minor
     * revision is present, and the minor revision can only be used if a major
     * version is present. The revision number can only be used if a revision
     * level is present.
     * Note: If this is not recognizable as a versioned module, it will be
     * assumed to be unversioned. getStemName() will be equivalent to getName().
     *
     * @return The basename of the module
     */
    public String getStemName()
    {
        return Version.stem(name);
    }

    /**
     * return true if the name contains an upArrow
     *
     * @return
     */
    public boolean hasUpArrow()
    {
        return upArrow;
    }

    /**
     * get the patch number
     *
     * @return
     */
    public int getPatch()
    {
        if (name.matches(RELEASED_MODULE_NAME_PATTERN))
        {
            String[] parts = name.split("\\.");
            String lastPart = parts[parts.length - 1];
            return Integer.parseInt(lastPart);
        }
        return -1;
    }

    /**
     * get the location
     *
     * @return
     */
    public String getLocation()
    {
        return location;
    }

    /**
     * set the location of a module
     *
     * @param location
     */
    public void setLocation(String location)
    {
        this.location = location;
    }

    /**
     * get the dir
     *
     * @return
     */
    public File getDir()
    {
        return dir;
    }

    /**
     * get the src dir
     *
     * @return
     */
    public File getSrc()
    {
        return src;
    }

    /**
     * get the src path
     *
     * @return
     */
    public Path getSrcPath()
    {
        Path srcPath = new Path(ProjectLocator.getAntProject());
        srcPath.setPath(src.getAbsolutePath());
        return srcPath;
    }

    /**
     * get the lib dir
     *
     * @return
     */
    public File getLibDir()
    {
        return lib;
    }

    /**
     * get the 64 bit library directory
     *
     * @return
     */
    public File getLib64Dir()
    {
        return new File(lib.getAbsolutePath() + "64");
    }

    /**
     * get the target dir
     *
     * @return
     */
    public File getTargetDir()
    {
        return target;
    }

    /**
     * get the target/classes dir
     *
     * @return
     */
    public File getTargetClasses()
    {
        return targetClasses;
    }

    /**
     * get the target/jar dir
     *
     * @return
     */
    public File getTargetJar()
    {
        return targetJar;
    }

    /**
     * get the resources dir
     *
     * @return
     */
    public File getResourcesDir()
    {
        return resources;
    }

    /**
     * get the kar resources dir
     *
     * @return
     */
    public File getKarResourcesDir()
    {
        return karResourcesDir;
    }

    /**
     * get the systemProperties dir
     *
     * @return
     */
    public File getSystemPropertiesDir()
    {
        return systemPropertiesDir;
    }

    /**
     * returns the module/configs dir. this location should not be used for
     * new development after Sept. 2009.
     */
    public File getConfigsDir()
    {
        return configs;
    }

    /**
     * returns the module/resources/configurations dir. this location should
     * be used for all development after Sept. 2009. Please see the
     * configuration-manager for more information.
     */
    public File getConfigurationsDir()
    {
        return configurations;
    }

    public File getConfigurationDirectivesDir()
    {
        return configurationDirectivesDir;
    }

    /**
     * get the lib/images dir
     *
     * @return
     */
    public File getLibImagesDir()
    {
        return libImages;
    }

    /**
     * get the tests dir
     *
     * @return
     */
    public File getTestsDir()
    {
        return tests;
    }

    /**
     * get the tests/src dir
     *
     * @return
     */
    public File getTestsSrc()
    {
        return testsSrc;
    }

    /**
     * get the tests/classes dir
     *
     * @return
     */
    public File getTestsClassesDir()
    {
        return testsClasses;
    }

    /**
     * get the tests/workflows dir
     *
     * @return
     */
    public File getTestsWorkflowsDir()
    {
        return testsWorkflows;
    }

    /**
     * get the module-info dir
     *
     * @return
     */
    public File getModuleInfoDir()
    {
        return moduleInfoDir;
    }

    public File getLicensesTxt()
    {
        return licensesTxt;
    }

    /**
     * get the modulesTxt object
     *
     * @return
     */
    public ModulesTxt getModulesTxt()
    {
        if (modulesTxt.exists() && !hasModulesTxtBeenRead)
        {
            modulesTxt.read();
            hasModulesTxtBeenRead = true;
        }
        return modulesTxt;
    }

    /**
     * get the os.txt file
     *
     * @return
     */
    public File getOsTxt()
    {
        return osTxt;
    }

    /**
     * get the docs dir
     *
     * @return
     */
    public File getDocumentationDir()
    {
        return documentationDir;
    }

    /**
     * get the workflows dir
     *
     * @return
     */
    public File getWorkflowsDir()
    {
        return workflowsDir;
    }

    /**
     * get the demos dir
     *
     * @return
     */
    public File getDemosDir()
    {
        return demosDir;
    }

    /**
     * get the system props
     *
     * @return
     */
    public Properties getSystemProperties()
    {
        //File systemPropertiesDir = new File( basedir, module + "/resources/system.properties");
        Properties systemProperties = new Properties();
        if (!systemPropertiesDir.exists())
        {
            return systemProperties;
        }
        File[] children = systemPropertiesDir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String filename)
            {
                return filename.endsWith(".properties");
            }
        });

        for (File systemPropertiesFile : children)
        {
            System.out.println("Loading system properties from "
                    + systemPropertiesFile.getAbsolutePath());
            try {
                InputStream stream = null; 
                try
                {
                    stream = new FileInputStream(systemPropertiesFile);
                    systemProperties.load(stream);
                }
                finally
                {
                    if(stream != null)
                    {
                        stream.close();
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return makeSubstitutes(systemProperties);
    }

    /**
     * make substitues for user variables and environment variables in the
     * vars passed to kepler at runtime
     */
    private Properties makeSubstitutes(Properties systemProperties)
    {
        Enumeration<?> propertyNames = systemProperties.propertyNames();
        while (propertyNames.hasMoreElements())
        {
            String name = (String) propertyNames.nextElement();
            String value = systemProperties.getProperty(name);
            if (value.contains("${project.path}"))
            {
                value = value.replace("${project.path}", ProjectLocator.getProjectDir()
                        .getAbsolutePath());
                systemProperties.setProperty(name, value);
            }
            if (value.contains("${user.home}"))
            {
                value = value.replace("${user.home}", System.getProperty("user.home"))
                        .trim();
                systemProperties.setProperty(name, value);
            }
            if (value.startsWith("env."))
            {
                Properties envProps = loadEnvironment("env");
                String prop = envProps.getProperty("env."
                        + value.substring(4, value.length()));
                if (prop != null)
                {
                    //System.out.println("setting prop " + name + " with " + prop);
                    if (prop.trim().indexOf(" ") != -1)
                    {
                        prop = "\"" + prop.trim() + "\"";
                    }
                    systemProperties.setProperty(name, prop);
                }
            }
        }
        return systemProperties;
    }

    /**
     * load the environment variables
     */
    @SuppressWarnings("unchecked")
    protected Properties loadEnvironment(String prefix)
    {
        Properties props = new Properties();
        if (!prefix.endsWith("."))
        {
            prefix += ".";
        }
        Vector<String> osEnv = Execute.getProcEnvironment();
        for (Enumeration<String> e = osEnv.elements(); e.hasMoreElements();)
        {
            String entry = e.nextElement();
            int pos = entry.indexOf('=');
            if (pos != -1)
            {
                props.put(prefix + entry.substring(0, pos), entry.substring(pos + 1));
            }
        }
        return props;
    }

    /**
     * get the fileset
     *
     * @return
     */
    public FileSet getFileSet()
    {
        FileSet fileset = new FileSet();
        fileset.setProject(ProjectLocator.getAntProject());
        fileset.setDir(getDir());
        return fileset;
    }

    /**
     * return true if this is a suite
     *
     * @return
     */
    public boolean isSuite()
    {
        return isSuite;
    }

    /** Returns true if this module is Ptolemy. */
    public boolean isPtolemy()
    {
        return(name.equals(Module.PTOLEMY) || name.matches(Module.PTOLEMY+"-\\d+\\.\\d+") 
                || name.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+")
                || name.matches(Module.PTOLEMY+"-\\d+\\.\\d+\\.\\d+")
                || name.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+\\.\\d+"));
    }
    
    /**
     * return true if this is released
     *
     * @return
     */
    public boolean isReleased()
    {
        return ModuleUtil.hasReleasedName(this);
    }

    /**
     * write a string
     *
     * @return
     */
    public String writeString()
    {
        return writeStringHelper(name);
    }

    /**
     * write a string
     *
     * @param name
     * @return
     */
    protected String writeStringHelper(String name)
    {
        String result = upArrow ? getPattern(name) + ".^" : name;
        if (!location.startsWith(RepositoryLocations.REPO) && !isReleased())
        {
            result += spaces(40 - name.length()) + location;
        }
        return result;
    }

    /**
     * get a pattern
     *
     * @param name
     * @return
     */
    private String getPattern(String name)
    {
        String[] parts = name.split("\\.");
        String patch = parts[parts.length - 1];
        return name.substring(0, name.length() - patch.length() - 1);
    }

    /**
     * return n spaces
     *
     * @param n
     * @return
     */
    protected String spaces(int n)
    {
        String result = "";
        for (int i = 0; i < n; i++)
        {
            result += " ";
        }
        return result;
    }

    /**
     * delete this module
     */
    public void delete()
    {
        Delete delete = new Delete();
        delete.setProject(ProjectLocator.getAntProject());
        delete.setDir(dir);
        delete.execute();
    }

    /**
     * return the name of this module
     */
    public String toString()
    {
        return name;
    }

    /**
     * return a hashCode for this module
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * return true if this == obj or obj is a Module object with the same name as this one.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        else if (obj == null)
        {
            return false;
        }
        else if(!(obj instanceof Module))
        {
            return false;
        }
        
        Module other = (Module) obj;
        if (name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!name.equals(other.name))
        {
            return false;
        }
        return true;
    }
    
    /** Get the jars contained in this module. */
    public List<File> getJars()
    {
        
        final List<File> jars = new LinkedList<File>();
        
        Project project = ProjectLocator.getAntProject();
        // NOTE: getAntProject() may return null; in this case
        // create a new one.
        if(project == null)
        {
            project = new Project();
        }
        
        final File lib64Dir = getLib64Dir();
        if(lib64Dir.exists())
        {
           final FileSet fileSet = new FileSet();
           fileSet.setProject(project);
           fileSet.setDir(lib64Dir);
           fileSet.setIncludes("**/*.jar");
           final String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
           for(String name : files)
           {
               jars.add(new File(lib64Dir, name));
           }
        }
        
        final File libDir = getLibDir();
        if(libDir.exists())
        {
            final FileSet fileSet = new FileSet();
            fileSet.setProject(project);
            fileSet.setDir(libDir);
            fileSet.setIncludes("**/*.jar");
            final String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
            for(String name : files)
            {
                jars.add(new File(libDir, name));
            }
        }
        
        if(isPtolemy())
        {
            final File ptolemyLibDir = new File(getSrc(), "lib");
            final FileSet fileSet = new FileSet();
            fileSet.setProject(project);
            fileSet.setDir(ptolemyLibDir);
            fileSet.setIncludes("**/*.jar");
            final String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
            for(String name : files)
            {
                jars.add(new File(ptolemyLibDir, name));
            }
        }
        
        return jars;
    }
    
    /** Get a list of source file names. */
    public List<String> getSourceFileNames() {
                
        Project project = ProjectLocator.getAntProject();
        // NOTE: getAntProject() may return null; in this case
        // create a new one.
        if(project == null)
        {
            project = new Project();
        }

        final FileSet fileSet = new FileSet();
        fileSet.setProject(project);
        final File SrcDirFile = getSrc();
        // make sure it exists
        if(!SrcDirFile.isDirectory()) {
            return new LinkedList<String>();
        }
        fileSet.setDir(getSrc());
        fileSet.setIncludes("**/*.java");
        if (name.equals(Module.PTOLEMY) || name.matches(Module.PTOLEMY+"-\\d+\\.\\d+") || 
                name.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+"))
        {
            fileSet.setExcludesfile(new File(project.getBaseDir(), "build-area/settings/ptolemy-excludes"));
        }
        final String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
        return Arrays.asList(files);
    }

}
