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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.LibPath;
import org.kepler.build.project.MemoryProperties;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RunClasspath;
import org.kepler.build.runner.Kepler;

/**
 * Created by David Welker.
 * Date: Aug 21, 2008
 * Time: 1:05:17 PM
 * Implements the Run task in the kepler build system
 */
public class Run extends ModulesTask
{
    private final String DEFAULT_MAIN = "org.kepler.Kepler";

    private String main = "undefined";

    private String module = "undefined";
    private String suite = "undefined";
    //the location of kepler dir.
    private String location = "undefined";
    private String args = "";
    private String workflow = "";
    private Properties runProperties = new Properties();
    private String properties = "undefined";
    private String jvmMaxMemory = "";
    private String jvmMinMemory = "";
    private String jvmStackSize = "";
    private boolean debug;
    private boolean fork = true;
    private boolean spawn = false;

    /**
     * set the location
     *
     * @param location
     */
    public void setLocation(String location)
    {
        this.location = location;
        //set kepler dir based on the basedir of build.xml.
        ProjectLocator.setKeplerModulesDir(new File(this.location));
    }
    
    /**
     * set the module
     *
     * @param module
     */
    public void setModule(String module)
    {
        this.module = module;
    }

    /**
     * set the suite
     *
     * @param suite
     */
    public void setSuite(String suite)
    {
        this.suite = suite;
    }

    /**
     * set the args
     *
     * @param args
     */
    public void setArgs(String args)
    {
        this.args = args;        
    }

    /**
     * set a workflow
     *
     * @param workflow
     */
    public void setWorkflow(String workflow)
    {
        this.workflow = workflow;
    }

    /**
     * set the main class
     *
     * @param main
     */
    public void setMain(String main)
    {
        this.main = main;
    }

    /**
     * set the props
     *
     * @param properties
     */
    public void setProperties(String properties)
    {
        this.properties = properties;
    }

    /**
     * set max memory
     *
     * @param jvmMaxMemory
     */
    public void setJvmMaxMemory(String jvmMaxMemory)
    {
    	//if jvmMaxMemory is not set, use MemoryProperties to get it.
    	if (jvmMaxMemory.equalsIgnoreCase("${max}"))
    		this.jvmMaxMemory = MemoryProperties.getMaxMemory();
    	else
    		this.jvmMaxMemory = jvmMaxMemory;
    }

    /**
     * set min memory
     *
     * @param jvmMinMemory
     */
    public void setJvmMinMemory(String jvmMinMemory)
    {
    	//if jvmMinMemory is not set, use MemoryProperties to get it.
    	if (jvmMinMemory.equalsIgnoreCase("${min}"))
    		this.jvmMinMemory = MemoryProperties.getMinMemory();
    	else
    		this.jvmMinMemory = jvmMinMemory;
    }

    /**
     * set compiler debug option
     *
     * @param debug
     */
    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    /**
     * set this to fork to a new process
     *
     * @param fork
     */
    public void setFork(boolean fork)
    {
        this.fork = fork;
    }

    /**
     * set to spawn a new thread
     *
     * @param spawn
     */
    public void setSpawn(boolean spawn)
    {
        this.spawn = spawn;
    }

    /**
     * execute the task
     */
    @Override
    public void run() throws Exception
    {
        jvmMinMemory = MemoryProperties.getMinMemory();
        jvmMaxMemory = MemoryProperties.getMaxMemory();
        jvmStackSize = MemoryProperties.getStackSize();
        System.out.println("JVM Memory: min = " + jvmMinMemory +
                ",  max = " + jvmMaxMemory +
                ", stack = " + jvmStackSize);


        //If they are running "ant run-workflow" or "ant run-workflow-w/gui" do a quick sanity check and make sure the workflow file the specified exists."
        if (!workflow.equals(""))
        {
            File workflowFile = new File(workflow);
            if (!workflowFile.exists())
            {
                System.out.println("The workflow you specified does not exist.");
                System.out
                        .println("Please enter a valid workflow including either an absolute path or a path relative to <kepler.modules>/build-area.");
                return;
            }
            workflow = workflowFile.getAbsolutePath();
        }

        if (module.equals("undefined"))
        {
            runSuite();
        }
        else
        {
            runModule();
        }
    }

    /**
     * run kepler from a suite
     */
    private void runSuite() throws IOException
    {
        if (!suite.equals("undefined"))
        {
            getSystemPropertiesFromModule(suite);
        }

        ModuleTree moduleTree = ModuleTree.instance();

        if (!moduleTree.allModulesPresent())
        {
            StringBuilder message = new StringBuilder("The following modules are missing:\n");
            for (Module m : moduleTree.getMissingModules())
            {
                message.append("  " + m + "\n");
            }
            PrintError.message(message.toString());


            // Kepler can't launch due to missing modules, launch MM GUI instead.
            // Note this uses getKeplerModulesDir(), so the modules-manager-gui.txt 
            // utilized could be in either "application dir" or KeplerData/kepler.modules
            String[] args = new String[]{"UseModuleManager"};
            File basePath = ProjectLocator.getApplicationModulesDir();
            if (basePath != null){
            	System.out.println("Kepler failed to launch. Trying to launch " +
            			"Module Manager GUI using basePath:"+basePath.getAbsolutePath());
            	Kepler.setBaseDirPath(basePath.getAbsolutePath());
            	Kepler.main(args);
            }else{
            	System.out.println("Unable to fall back to Module Manager GUI, couldn't find application dir");
            }
            return;
        }

        if (main.equals("undefined"))
        {
            main = DEFAULT_MAIN;
        }

        Java java = new Java();
        java.bindToOwner(this);
        java.init();
        java.setFork(fork);
        java.setSpawn(spawn);
        java.setFailonerror(!spawn);

        RunClasspath runClasspath = new RunClasspath();
        if (debug)
        {
            runClasspath.print();
        }
        java.setClasspath(runClasspath);
        if (moduleTree.firstStartsWith(Module.PTOLEMY))
        {
            java.setClassname("ptolemy.vergil.VergilApplication");
        }
        else
        {
            java.setClassname(main);
        }

        if(jvmMaxMemory != null && !jvmMaxMemory.trim().isEmpty()) {
            java.createJvmarg().setLine("-Xmx" + jvmMaxMemory);
        }
        
        if(jvmMinMemory != null && !jvmMinMemory.trim().isEmpty()) {
            java.createJvmarg().setLine("-Xms" + jvmMinMemory);
        }
        
        if(jvmStackSize != null && !jvmStackSize.trim().isEmpty()) {
            java.createJvmarg().setLine("-Xss" + jvmStackSize);
        }

        // Pass user.home to sub-process
        java.createJvmarg().setLine("-Duser.home='" + System.getProperty("user.home") + "'");

        if ( System.getProperty("os.name").equals("Mac OS X") && main.equals(DEFAULT_MAIN) && workflow.equals("") )
        {
            Module commonModule = ModuleTree.instance().getModuleByStemName("common");
            if(commonModule != null)
            {
                String commonModuleName = commonModule.getName();
                String iconpath = basedir + "/" + commonModuleName + "/resources/icons/kepler-dock-icon.png";
                System.out.println("setting dock icon to " + "-Xdock:icon=" + iconpath);
                java.createJvmarg().setLine("-Xdock:name=Kepler");
                java.createJvmarg().setLine("-Xdock:icon=" + iconpath);
            }
        }

        //DO NOT remove the quotes on this command line argument.  If there are
        //no quotes, the command will fail if kepler is installed in a
        //path that contains spaces.  !!!!!
        //System.out.println("java.library.path: " + new LibPath().toString());
        if (args.indexOf("-printClasspath") != -1)
        {
            System.out.println("classpath: " + runClasspath);
        }

        if(workflow != null && !workflow.isEmpty()) 
        {
            if (workflow.endsWith(".kar"))
            {
                args = " -runkar -nogui " + args;
            }
            else
            {
                args = " -runwf " + args;
            }
        }
        java.createJvmarg()
                .setLine("-Djava.library.path=\"" + new LibPath() + "\"");

        handleRunProperties(java);
        if (workflow == null || workflow.equals(""))
        {
            java.createArg().setLine(args);
        }
        else
        {
            java.createArg().setLine(args + " \"" + workflow + "\"");
        }


        setLocalVariables(java);
        
        //System.out.println("args = " + args);

        java.execute();
    }

    /**
     * set any architecture specific environment variables that the VM needs
     * to know about
     */
    private void setLocalVariables(Java java)
    {
        //System.out.println("os: " + System.getProperty("os.name"));
        if (System.getProperty("os.name").indexOf("Windows") != -1
                || System.getProperty("os.name").indexOf("Mac") != -1)
        { //add the path for system libraries to the PATH variable
            //so we don't have to copy dlls to System32.
            //may need to do this with LD_LIBRARY_PATH for linux
            Environment.Variable envvar = new Environment.Variable();
            String pathVar = System.getenv("PATH");
            loadVariable("PATH", pathVar + System.getProperty("path.separator")
                    + new LibPath(), java);
        }

        setModuleSpecificVariables(java);
    }

    /**
     * check for an environment.txt file in each module's module-info dir
     * If it exists, load the variables.
     */
    private void setModuleSpecificVariables(Java java)
    {
    	
    	if(moduleTree == null)
    	{
    		init();
    	}
    	
        String osName = System.getProperty("os.name");
        //go through each module looking for the environment.txt file
        for (Module module : moduleTree)
        {
            File f = new File(basedir, module + "/module-info/environment.txt");
            if (f.exists())
            {
                EnvironmentFile ef = new EnvironmentFile(f);
                Vector efEntries = ef.getEntries();
                for (int i = 0; i < efEntries.size(); i++)
                { //add each entry to the runtime
                    EnvironmentFileEntry efe = (EnvironmentFileEntry) efEntries
                            .elementAt(i);
                    Environment.Variable envvar = new Environment.Variable();
                    if (efe.variableOS.equals("all"))
                    {
                        loadVariable(efe.variableName, efe.variableValue, java);
                    }
                    else
                    { //os specific variables
                        if (osName.indexOf(efe.variableOS) != -1)
                        {
                            loadVariable(efe.variableName, efe.variableValue, java);
                        }
                    }
                }
            }
        }
    }

    /**
     * load a variable into the runtime
     */
    private void loadVariable(String name, String value, Java java)
    {
        Environment.Variable envvar = new Environment.Variable();
        envvar.setKey(name);
        envvar.setValue(value);
        java.setFork(true);
        java.addEnv(envvar);
        System.out.println("Set environment variable: " + name + " = " + value);
    }

    /**
     * handle the run properties
     */
    private void handleRunProperties(Java java)
    {
        Set<Object> runPropertyKeys = runProperties.keySet();
        for (Object keyObject : runPropertyKeys)
        {
            String key = (String) keyObject;
            String value = runProperties.getProperty(key);
            java.createJvmarg().setLine("-D" + key + "=" + value);
        }
        if (!properties.equals("undefined"))
        {
            String[] commandLineProperties = properties.split("[=,;:]");
            if (commandLineProperties.length % 2 != 0)
            {
                System.out
                        .println("ERROR: You did not enter properties with the correct syntax.");
                System.out
                        .println("  -Dproperties=PROPERTY1=VALUE1,PROPERTY2=VALUE2,PROPERTY3=VALUE3");
                return;
            }
            for (int i = 0; i < commandLineProperties.length; i += 2)
            {
                java.createJvmarg().setLine(
                        "-D" + commandLineProperties[i] + "="
                                + commandLineProperties[i + 1]);
            }
        }
    }

    /**
     * get the system properties from a module
     */
    private void getSystemPropertiesFromModule(String module) throws IOException
    {
        File systemPropertiesDir = new File(basedir, module
                + "/resources/system.properties");
        System.out.println("Loading system properties from "
                + systemPropertiesDir.getAbsolutePath());
        if (!systemPropertiesDir.exists())
        {
            return;
        }
        File[] children = systemPropertiesDir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String filename)
            {
                return filename.endsWith(".properties");
            }
        });
        for (File systemPropertiesFile : children)
        {
            InputStream stream = null;
            try {
                stream = new FileInputStream(systemPropertiesFile);
                runProperties.load(stream);
            } finally {
                if(stream != null) {
                    stream.close();
                }
            }
        }
        makeSubstitutes();
    }

    /**
     * run a module
     */
    private void runModule() throws IOException
    {
        getSystemPropertiesFromModule(module);

        if (main.equals("undefined"))
        {
            File mainTxt = new File(basedir, module + "/resources/main.txt");
            if (!mainTxt.exists())
            {
                System.out
                        .println("ERROR: You did not specify a main class and main.txt does not exist.");
                return;
            }
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(mainTxt));
                String line;
                while ((line = br.readLine()) != null)
                {
                    String[] tokens = line.split("\\s+");
                    main = tokens[0];
                    if (args.equals("") && line.length() != main.length())
                    {
                        args = line.substring(main.length()).trim();
                    }
                }
            }
            finally
            {
                if(br != null)
                {
                    br.close();
                }
            }

        }

        System.out.println("Running " + main);
        if (args.equals(""))
        {
            System.out.println("    without using any arguments.");
        }
        else
        {
            System.out.println("    using the following arguments: " + args + "\n");
        }

        RunClasspath runClasspath = new RunClasspath();
        if (debug)
        {
            runClasspath.print();
        }

        Java java = new Java();
        java.bindToOwner(this);
        java.init();
        java.setFork(fork);
        java.setSpawn(spawn);
        java.setFailonerror(true);
        java.setClasspath(runClasspath);
        //java.setClasspath(modulesTree.getHighestPriorityModule().getCompileClasspath());
        java.setClassname(main);
        
        if(jvmMaxMemory != null && !jvmMaxMemory.trim().isEmpty()) {
            java.createJvmarg().setLine("-Xmx" + jvmMaxMemory);
        }
        
        if(jvmMinMemory != null && !jvmMinMemory.trim().isEmpty()) {
            java.createJvmarg().setLine("-Xms" + jvmMinMemory);
        }
        
        if(jvmStackSize != null && !jvmStackSize.trim().isEmpty()) {
            java.createJvmarg().setLine("-Xss" + jvmStackSize);
        }

        // Pass user.home to sub-process
        java.createJvmarg().setLine("-Duser.home='" + System.getProperty("user.home") + "'");

        handleRunProperties(java);
        java.createArg().setLine(args);

        java.execute();
    }

    /**
     * make substitues for user variables and environment variables in the
     * vars passed to kepler at runtime
     */
    private void makeSubstitutes()
    {
        Enumeration<?> propertyNames = runProperties.propertyNames();
        while (propertyNames.hasMoreElements())
        {
            String name = (String) propertyNames.nextElement();
            String value = runProperties.getProperty(name);
            if (value.contains("${project.path}"))
            {
                value = value.replace("${project.path}", basedir.getAbsolutePath());
                runProperties.setProperty(name, value);
            }
            if (value.contains("${user.home}"))
            {
                value = value.replace("${user.home}", System.getProperty("user.home"))
                        .trim();
                runProperties.setProperty(name, value);
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
                    runProperties.setProperty(name, prop);
                }
            }
        }
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
     * print the classpath
     *
     * @param classpath
     */
    protected void printClasspath(String classpath)
    {
        System.out.println("RUN CLASSPATH");
        System.out.println("=========");
        String[] parts = classpath.split(File.pathSeparator);
        for (String part : parts)
        {
            System.out.println(part);
        }
        System.out.println();
    }

    /**
     * private class to represent an environment.txt file
     */
    private class EnvironmentFile
    {
        Vector efEntries;

        /**
         * Constructor
         */
        public EnvironmentFile(File f)
        {
            try
            {
                StringBuffer sb = new StringBuffer();
                efEntries = new Vector();
                FileReader fr = null;
                try
                {
                    fr = new FileReader(f);
                    char[] c = new char[1024];
                    int numread = fr.read(c, 0, 1024);
                    while (numread != -1)
                    {
                        sb.append(c, 0, numread);
                        numread = fr.read(c, 0, 1024);
                    }
                }
                finally
                {
                    fr.close();
                }

                StringTokenizer st = new StringTokenizer(sb.toString(), "\n");
                while (st.hasMoreTokens())
                {
                    String entry = st.nextToken();
                    EnvironmentFileEntry efe = new EnvironmentFileEntry(entry);
                    efEntries.addElement(efe);
                }
            }
            catch (Exception e)
            {
                System.out.println("WARNING: Could not read environment file "
                        + f.getAbsolutePath() + ".  Skipping this file.  Your environment "
                        + "may not have loaded correctly: " + e.getMessage());
            }
        }

        /**
         * return a Vector of the EnvironmentFileEntries
         */
        public Vector getEntries()
        {
            return efEntries;
        }
    }

    /**
     * class to represent an entry in the environment.txt file
     */
    private class EnvironmentFileEntry
    {
        public String variableName;
        public String variableOS;
        public String variableValue;

        /**
         * Constructor
         */
        public EnvironmentFileEntry(String entry)
        {
            String key = entry.substring(0, entry.indexOf("="));
            String value = entry.substring(entry.indexOf("=") + 1, entry.length());
            //parse an entry that looks like
            //R_HOME:Mac OS X=/Library/Frameworks/R.framework/Resources
            if (key.indexOf(":") != -1)
            {
                variableName = key.substring(0, key.indexOf(":"));
                variableOS = key.substring(key.indexOf(":") + 1, key.length());
            }
            else
            {
                //parse an entry that looks like
                //R_HOME=/Library/Frameworks/R.framework/Resources
                variableName = key;
                variableOS = "all";
            }

            variableValue = value;
        }
    }
}
