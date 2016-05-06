/*
 * Copyright (c) 2008 The Regents of the University of California.
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
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleUtil;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.MemoryProperties;

/**
 * Created by Chad Berkley
 * Date: June 5, 2009
 * Implements the NMI task which will create all config files needed to run
 * an NMI nightly build for a given suite. The files then need to be installed
 * on the NMI submit host. See
 * https://kepler-project.org/developers/teams/build/nmi-build-notes
 * for more information
 */
public class NMI extends ModulesTask
{
    private SubmitFile submitFile;
    private String project;
    private String component;
    private String component_version;
    private String description;
    private String run_type;
    private String platforms;
    private String prereqs;
    private String notify;
    private String suiteName;
    private boolean overwrite;

    /**
     * set the project name
     */
    public void setProject(String project)
    {
        this.project = project;
    }

    /**
     * set the component name
     */
    public void setComponent(String component)
    {
        this.component = component;
    }

    /**
     * set the component_version
     */
    public void setComponentVersion(String component_version)
    {
        this.component_version = component_version;
    }

    /**
     * set the build description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * set the project run_type
     */
    public void setRunType(String run_type)
    {
        this.run_type = run_type;
    }

    /**
     * set the list of platforms
     */
    public void setPlatforms(String platforms)
    {
        this.platforms = platforms;
    }

    /**
     * set the list of prereqs
     */
    public void setPrereqs(String prereqs)
    {
        this.prereqs = prereqs;
    }

    /**
     * set the notify email address
     */
    public void setNotify(String notify)
    {
        this.notify = notify;
    }

    /**
     * set whether to overwrite files in the working directory
     */
    public void setOverwrite(String overwrite)
    {
        this.overwrite = false;
        if (overwrite.equals("true"))
        {
            this.overwrite = true;
        }
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        suiteName = ModuleUtil.getCurrentSuiteName();
        if (suiteName.equals("undefined"))
        {
            throw new Exception(
                    "The suite must be defined to create the NMI configuration.");
        }
        File nmidir = new File(basedir, "build-area/resources/nmi/runs");
        File outputdir = new File(nmidir, suiteName);
        if (!outputdir.exists())
        {
            System.out.println("creating directory " + outputdir.getAbsolutePath());
            outputdir.mkdirs();
        }
        else
        {
            if (!overwrite)
            {
                System.out
                        .println("ERROR: Directory "
                                + outputdir.getAbsolutePath()
                                + " exists and overwrite is not set to true.  The NMI task will now "
                                + "exit.  Either remove this directory or set overwrite=\"true\" in "
                                + "your task call in the build file.");
                return;
            }

            System.out.println("WARNING: directory " + outputdir.getAbsolutePath()
                    + " already exists and overwrite is set to true.  Some files "
                    + "might have been overwritten.");
        }
        submitFile = new SubmitFile(project, component, component_version,
                description, run_type, notify, "nmi/runs/" + suiteName);

        //need to parse:
        //prereqs
        parsePrereqs(submitFile, prereqs);
        //platforms
        parsePlatforms(submitFile, platforms);

        //need to create files for and add:
        //pre_all
        createPreAll(submitFile, outputdir);
        //post_all
        createPostAll(submitFile, outputdir);
        //remote_task
        createRemoteTask(submitFile, outputdir);
        //remote_task_args
        submitFile.setRemoteTaskArgs("\"\"");
        //remote_post
        createRemotePost(submitFile, outputdir, suiteName);

        //need to generate automatically:
        //inputs
        createInputs(submitFile, outputdir);
        //outputs
        createOutputs(submitFile, outputdir);

        //System.out.println(submitFile.toString());
        String submitFilename = suiteName + "-submit";
        writeToFile(new File(outputdir, submitFilename), submitFile.toString());
        System.out
                .println("Done creating NMI configuration.  Configuration "
                        + "is in "
                        + outputdir.getAbsolutePath()
                        + ". It must be checked in and "
                        + "installed on the NMI host before it can be run.  To install, run '"
                        + "java NMIInstaller' from your checked in suite directory on the submit host.");
        //create the installer to install the NMI build
        createInstaller(suiteName);
    }

    /**
     * create the NMI installer
     */
    private void createInstaller(String suiteName) throws Exception
    {
        //compile the class
        Javac javac = new Javac();
        javac.bindToOwner(this);
        File srcDir = new File(basedir, "build-area/resources/nmi/src");
        javac.setSrcdir(new Path(getProject(), srcDir.getAbsolutePath()));
        File classes = new File(basedir, "build-area/resources/nmi/runs/"
                + suiteName);
        classes.mkdirs();
        javac.setDestdir(classes);

        javac.setDebug(true);
        javac.setFork(true);
        javac.setMemoryMaximumSize(MemoryProperties.getMaxMemory());
        javac.setFailonerror(true);
        javac.execute();

        //create the shell script to execute the installer
        String script = "#! /bin/sh\n";
        script += "java NMIInstaller " + suiteName + "\n";
        File scriptFile = new File(basedir, "build-area/resources/nmi/runs/"
                + suiteName + "/install.sh");
        FileWriter fw = new FileWriter(scriptFile);
        fw.write(script, 0, script.length());
        fw.flush();
        fw.close();

        makeFileExecutable(scriptFile);
    }

    /**
     * create the outputs file
     */
    private void createOutputs(SubmitFile submitFile, File outputdir)
            throws Exception
    {
        String s = "method = scp\n";
        s += "platform = common\n";
        s += "source = results/*.*\n";
        s += "dest = webuser@ceres.nceas.ucsb.edu:/var/www/org.kepler-project.www/dist/nightly/.\n";
        writeToFile(new File(outputdir, suiteName + "-upload.out"), s);
        submitFile.addOutput(suiteName + "-upload.out");
    }

    /**
     * create the remote post script and add it to the submit file
     */
    private void createRemotePost(SubmitFile submitFile, File outputdir,
                                  String suiteName) throws Exception
    {
        String s = "#! /bin/sh\n";
        s += "set -v on\n";
        s += "date=`date +%Y-%m-%d-%H-%M`\n";
        s += "echo \"Starting REMOTE_POST\"\n";
        s += "echo -n \"***Working directory is: \"\n";
        s += "echo `pwd`\n";

        s += "file1=kepler-nightly-" + suiteName + "-$date.tar.gz\n";
        s += "tar -zcvf $file1 kepler-run\n";

        s += "file2=kepler-nightly-" + suiteName + "-osx-$date.jar\n";
        s += "file3=kepler-nightly-" + suiteName + "-win-$date.jar\n";
        s += "file3=kepler-nightly-" + suiteName + "-lin-$date.jar\n";
        s += "cp kepler-run/modules/build-area/installer/kepler-nightly-"
                + suiteName + "-osx.jar $file2\n";
        s += "cp kepler-run/modules/build-area/installer/kepler-nightly-"
                + suiteName + "-win.jar $file3\n";
        s += "cp kepler-run/modules/build-area/installer/kepler-nightly-"
                + suiteName + "-linux.jar $file4\n";
        s += "tar -zcvf results.tar.gz $file1 $file2 $file3 $file4\n";

        s += "echo \"REMOTE_POST completed\"\n";
        s += "exit\n";

        File f = new File(outputdir, "remote_post.sh");
        writeToFile(f, s);
        makeFileExecutable(f);
        submitFile.setRemotePost("remote_post.sh");
    }

    /**
     * create the remote task file and add it to the submitFile
     */
    private void createRemoteTask(SubmitFile submitFile, File outputdir)
            throws Exception
    {
        String s = "#! /bin/sh\n";
        s += "set -v on\n";
        s += "cd kepler-run/modules/build-area\n";
        s += "ant change-to -Dsuite=kepler\n";
        s += "ant compile\n";
        s += "ant test\n";
        s += "ant build-windows-installer -Dno-win-exe=true -DcopyDocumentation=false -Dreleasename=kepler-nightly-"
                + suiteName + "\n";
        s += "ant build-mac-installer -Dno-mac-app=true -DcopyDocumentation=false -Dreleasename=kepler-nightly-"
                + suiteName + "\n";
        s += "ant build-linux-installer -DcopyDocumentation=false -Dreleasename=kepler-nightly-"
                + suiteName + "\n";

        File f = new File(outputdir, "build.sh");
        writeToFile(f, s);
        makeFileExecutable(f);
        submitFile.setRemoteTask("build.sh");
    }

    /**
     * create the post_all script
     */
    private void createPostAll(SubmitFile submitFile, File outputdir)
            throws Exception
    {
        String s = "#! /bin/sh\n";
        s += "set -v on\n";
        s += "date=`date +%Y-%m-%d-%H-%M`\n";
        s += "echo -n \"Working directory is: \"\n";
        s += "echo `pwd`\n";

        //get err stream for notify.nmi file for each platform
        Vector platforms = submitFile.getPlatforms();
        for (int i = 0; i < platforms.size(); i++)
        {
            String platform = (String) platforms.elementAt(i);
            s += "echo \"\\n\\n\" >> notify.nmi\n";
            s += "echo \"errors for platform " + platform + "\" >> notify.nmi\n";
            s += "cat ../" + platform + "/remote_task.err >> notify.nmi\n";
        }

        s += "mkdir results\n";
        s += "cp ../x86_macos_10.4/results.tar.gz .\n";
        s += "cd results\n";
        s += "cp ../results.tar.gz .\n";
        s += "tar -zxvf results.tar.gz\n";
        s += "rm results.tar.gz\n";

        s += "echo \"POST_ALL completed\"\n";
        s += "exit\n";
        File f = new File(outputdir, "post_all.sh");
        writeToFile(f, s);
        makeFileExecutable(f);
        submitFile.setPostAll("post_all.sh");
    }

    /**
     * create the input files and add them to the submitFile
     */
    private void createInputs(SubmitFile submitFile, File outputdir)
            throws Exception
    {
        for (Module module : moduleTree)
        { //for each module, we need the following files and information:
            //* an .svn file
            //* an entry in the submit file
            String inputFilename = suiteName + "-" + module + ".svn";
            File svnFile = new File(outputdir, inputFilename);
            String s = "method = svn\n";
            s += "url = " + module.getLocation() + " " + module.getName() + "\n";
            writeToFile(svnFile, s);
            submitFile.addInput(inputFilename);
        }

        //besides the modules in the moduleTree, we also need to add an svn file for
        //build-area and build-area/resources/nmi
        String s = "method = svn\n";
        s += "url = https://code.kepler-project.org/code/kepler/trunk/modules/build-area build-area\n";
        String buildAreaFilename = suiteName + "-build-area.svn";
        writeToFile(new File(outputdir, buildAreaFilename), s);
        submitFile.addInput(buildAreaFilename);

        s = "method = svn\n";
        s += "url = https://code.kepler-project.org/code/kepler/trunk/modules/build-area/resources/nmi nmi\n";
        String nmiFilename = suiteName + "-nmi.svn";
        writeToFile(new File(outputdir, nmiFilename), s);
        submitFile.addInput(nmiFilename);
    }

    /**
     * create the pre_all file and add it to the submitFile
     */
    private void createPreAll(SubmitFile submitFile, File outputdir)
            throws Exception
    {
        String s = "";
        s += "#! /bin/sh\n";
        s += "set -v on\n";
        s += "mkdir kepler-run\n";
        s += "mkdir kepler-run/modules\n";

        for (Module module : moduleTree)
        {
            s += "mv " + module.getName() + " kepler-run/modules/.\n";
        }

        s += "mv build-area kepler-run/modules/.\n";

        File f = new File(outputdir, "pre_all.sh");
        writeToFile(f, s);
        makeFileExecutable(f);
        submitFile.setPreAll("pre_all.sh");
    }

    /**
     * parses the string brought in by the ant task into the SubmitFile
     */
    private void parsePrereqs(SubmitFile submitFile, String prereqsString)
    {
        StringTokenizer st = new StringTokenizer(prereqsString, ",");
        while (st.hasMoreTokens())
        {
            StringTokenizer st2 = new StringTokenizer(st.nextToken(), ":");
            String key = st2.nextToken().trim();
            String val = st2.nextToken().trim();
            //System.out.println("adding prereq key " + key + " and val " + val);
            submitFile.addPrereq(key, val);
        }
    }

    /**
     * parses the string brought in by the ant task into the SubmitFile
     */
    private void parsePlatforms(SubmitFile submitFile, String platforms)
    {
        StringTokenizer st = new StringTokenizer(platforms, ",");
        while (st.hasMoreTokens())
        {
            String platform = st.nextToken().trim();
            //System.out.println("adding platform: " + platform);
            submitFile.addPlatform(platform);
        }
    }

    /**
     * write a string to a file
     */
    private void writeToFile(File f, String contents, boolean append)
            throws Exception
    {
        System.out.println("Writing file " + f.getAbsolutePath());
        FileWriter fw = new FileWriter(f, append);
        fw.write(contents, 0, contents.length());
        fw.flush();
        fw.close();
    }

    /**
     * write to a file, overwriting its contents if it already exists
     */
    private void writeToFile(File f, String contents) throws Exception
    {
        writeToFile(f, contents, false);
    }

    /**
     * set the mode on the file to 777
     */
    private void makeFileExecutable(File f) throws Exception
    {
        String[] cmd = {"chmod", "777", f.getAbsolutePath()};
        Runtime.getRuntime().exec(cmd);
    }

    /**
     * private class to build the submit file
     */
    private class SubmitFile
    {
        //settable in constructor
        private String project;
        private String component;
        private String component_version;
        private String description;
        private String run_type;
        private String notify;
        //settable in add*
        private Vector inputs;
        private Vector outputs;
        private Vector platforms;
        private Hashtable prereqs;
        //settable individualy
        private String pre_all;
        private String post_all;
        private String remote_task;
        private String remote_task_args;
        private String remote_post;

        private String scriptDir;

        /**
         * constructor
         */
        public SubmitFile(String project, String component,
                          String component_version, String description, String run_type,
                          String notify, String scriptDir)
        {
            this.project = project;
            this.component = component;
            this.component_version = component_version;
            this.description = description;
            this.run_type = run_type;
            this.notify = notify;
            this.inputs = new Vector();
            this.outputs = new Vector();
            this.platforms = new Vector();
            this.prereqs = new Hashtable();
            this.scriptDir = scriptDir;
        }

        /**
         * get the names of all of the inputs
         */
        public Vector getInputs()
        {
            return this.inputs;
        }

        /**
         * get the names of all of the outputs
         */
        public Vector getOutputs()
        {
            return this.outputs;
        }

        /**
         * add an input
         */
        public void addInput(String input)
        {
            inputs.addElement(input);
        }

        /**
         * add an output
         */
        public void addOutput(String output)
        {
            outputs.addElement(output);
        }

        /**
         * add a platform
         */
        public void addPlatform(String platform)
        {
            platforms.addElement(platform);
        }

        /**
         * add an input
         */
        public void addPrereq(String prereqArch, String prereq)
        {
            prereqs.put(prereqArch, prereq);
        }

        /**
         * set the pre_all task
         */
        public void setPreAll(String preAll)
        {
            this.pre_all = scriptDir + "/" + preAll;
        }

        /**
         * set the post_all task
         */
        public void setPostAll(String postAll)
        {
            this.post_all = scriptDir + "/" + postAll;
        }

        /**
         * set the remote_task task
         */
        public void setRemoteTask(String remoteTask)
        {
            this.remote_task = scriptDir + "/" + remoteTask;
        }

        /**
         * set the remote_task_args
         */
        public void setRemoteTaskArgs(String remoteTaskArgs)
        {
            this.remote_task_args = remoteTaskArgs;
        }

        /**
         * set the remote_post task
         */
        public void setRemotePost(String remotePost)
        {
            this.remote_post = scriptDir + "/" + remotePost;
        }

        /**
         * return a vector with the names of all platforms to run on
         */
        public Vector getPlatforms()
        {
            return this.platforms;
        }

        /**
         * return a string rep of the submit file
         */
        public String toString()
        {
            String s = "";
            s += "# WARNING: These configuration files were autogenerated by the 'ant NMI' task\n"
                    + "# in the Kepler build.  They may be overwritten in the future if you \n"
                    + "# edit them by hand.\n";
            s += addLine("project", project);
            s += addLine("component", component);
            s += addLine("component_version", component_version);
            s += addLine("description", description);
            s += addLine("run_type", run_type);
            if (inputs.size() != 0)
            {
                s += "inputs = ";

                for (int i = 0; i < inputs.size(); i++)
                {
                    String input = (String) inputs.elementAt(i);
                    s += input;
                    if (i != inputs.size() - 1)
                    {
                        s += ",";
                    }
                }
                s += "\n";
            }

            if (outputs.size() != 0)
            {
                s += "outputs = ";

                for (int i = 0; i < outputs.size(); i++)
                {
                    String output = (String) outputs.elementAt(i);
                    s += output;
                    if (i != outputs.size() - 1)
                    {
                        s += ",";
                    }
                }
                s += "\n";
            }

            s += addLine("pre_all", pre_all);
            s += addLine("post_all", post_all);
            s += addLine("remote_task", remote_task);
            s += addLine("remote_task_args", remote_task_args);
            s += addLine("remote_post", remote_post);

            if (platforms.size() != 0)
            {
                s += "platforms = ";

                for (int i = 0; i < platforms.size(); i++)
                {
                    String platform = (String) platforms.elementAt(i);
                    s += platform;
                    if (i != platforms.size() - 1)
                    {
                        s += ",";
                    }
                }
                s += "\n";
            }

            if (prereqs.size() != 0)
            {
                Enumeration keys = prereqs.keys();
                while (keys.hasMoreElements())
                {
                    String key = (String) keys.nextElement();
                    String val = (String) prereqs.get(key);
                    s += addLine(key, val);
                }
            }

            s += addLine("notify", notify);

            return s;
        }

        /**
         * adds a line to the string if val is not null
         */
        private String addLine(String key, String val)
        {
            if (val != null && !val.trim().equals(""))
            {
                return key + " = " + val + "\n";
            }
            else
            {
                return "";
            }
        }
    }
}
