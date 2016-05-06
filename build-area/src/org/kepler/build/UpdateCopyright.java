/*
 * Copyright (c) 2009-2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 16:12:44 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31487 $'
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;

/**
 * @author berkley
 *         A task to update the copyright statements in the source tree
 *         This task will replace the token $date$ in the copyright statement
 *         with a provided copyrightDate. The file that specifies the copyright
 *         text is specified in the copyrightFile argument. You can specify
 *         that this task only runs on one module with -Dmodule=xxx. If you do
 *         not specify the module, this task will run on the entire module
 *         tree.
 *         An example running of this task is:
 *         <pre>ant update-copyright -DcopyrightFile=copyright.txt
 *                         -DcopyrightDate=2003-2010</pre>
 */
public class UpdateCopyright extends ModulesTask
{
    private String copyrightFileName = null;
    private String copyrightYear = null;
    private String module = null;
    private String file = null;
    private boolean dryRun = false;
    private int filesProcessed = 0;
    private int filesProcessedInModule = 0;
    private boolean tests = false;

    /**
     * set the file to use to get the copyright text
     *
     * @param filename
     */
    public void setCopyrightFile(String filename)
    {
        this.copyrightFileName = filename;
    }

    /**
     * set the copyright year
     *
     * @param year
     */
    public void setCopyrightYear(String year)
    {
        this.copyrightYear = year;
    }

    /**
     * set the module to use
     *
     * @param module
     */
    public void setModule(String module)
    {
        this.module = module;
    }

    /**
     * set the file to run on
     *
     * @param file
     */
    public void setFile(String file)
    {
        this.file = file;
    }

    /**
     * set tests to true if you want the copyrights updated on the tests
     * @param tests
     */
    public void setTests(String tests)
    {
        if (tests.equals("true"))
        {
            this.tests = true;
        }
        else
        {
            this.tests = false;
        }
    }

    /**
     * set dryRun to true if you want no files to be changed. Changes will
     * be printed to std.out instead.
     *
     * @param dryRun
     */
    public void setDryRun(String dryRun)
    {
        if (dryRun.equals("true"))
        {
            this.dryRun = true;
        }
    }

    /**
     * run the task
     */
    public void run() throws BuildException
    {
        try
        {
            File f = new File(this.file);
            if (f.exists())
            { //run on just one file
                System.out.println("Updating copyright for file: " + file);

                updateSrcFile(f);
                return;
            }
            else
            {
                if (!file.equals("${file}"))
                {
                    System.out.println("The file " + file + " was not found.");
                    return;
                }
            }

            if (module == null || module.equals("${module}")
                    || module.equals("undefined"))
            {
                System.out
                        .println("Updating copyright for all modules in current suite.");
                updateAllModules();
            }
            else
            {
                System.out.println("Updating copyright for module " + module);
                updateModule(module);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new BuildException("Error updating copyrights: " + e.getMessage());
        }
    }

    /**
     * update all module src files with the new copyright text
     *
     * @param copyrightText
     */
    private void updateAllModules() throws Exception
    {
        ModuleTree moduleTree = ModuleTree.instance();
        //go through each module and call updateModule on it
        for (Module module : moduleTree)
        {
            System.out.println("updating module " + module.getName());
            updateModule(module);
        }
        
        // update build area
        File buildAreaSrcs = new File(ProjectLocator.getBuildDir(), "src");
        if(buildAreaSrcs.exists()) {
            System.out.println("updating build-area");
        	updateSrcFile(buildAreaSrcs);
        }
        
        System.out.println(filesProcessed + " files processed in all modules.");
    }

    private void updateModule(String moduleName) throws Exception
    {
        ModuleTree moduleTree = ModuleTree.instance();
        Module m = moduleTree.getModule(moduleName);
        updateModule(m);
        System.out.println(filesProcessed + " total files processed");
    }

    /**
     * update the copyright text on the src files for a single module
     *
     * @param modulename
     */
    private void updateModule(Module module) throws Exception
    {
        if (module.getName().equals(Module.PTOLEMY))
        { //don't update ptolemy src
            return;
        }
        filesProcessedInModule = 0;
        //read each src file looking for the copyright statement
        File srcDir = module.getSrc();
        if (tests)
        {
            System.out.println("tests");
            srcDir = module.getTestsSrc();
        }
        System.out.println("srcdir: " + srcDir.getAbsolutePath());
        updateSrcFile(srcDir);
        System.out.println(filesProcessedInModule + " files processed in module "
                + module.getName());
    }

    /**
     * do the actual updating of the src file
     *
     * @param f
     * @param copyrightText
     */
    private void updateSrcFile(File f) throws Exception
    {
        //System.out.println("processing " + f.getAbsolutePath());
        if (f.isDirectory())
        { //recurse into this
            String[] dirs = f.list();
            for (int i = 0; i < dirs.length; i++)
            {
                updateSrcFile(new File(f, dirs[i]));
            }
        }
        else
        { //look for src files to update
            String name = f.getName();
            if (name.endsWith(".java"))
            { //only operate on java files
                filesProcessed++;
                filesProcessedInModule++;
                boolean update = true;
                printDryRunMessage("");
                printDryRunMessage("File: " + f.getAbsolutePath());
                String fileContents = readFile(f);
                boolean existingCopyright = checkForExistingCopyright(fileContents);
                boolean regentsCopyright = checkForUCRegentsCopyright(fileContents);
                String newCopyrightDate = copyrightYear;

                if (existingCopyright && regentsCopyright)
                { //there is already a copyright statement and it's a UC copyright, update it
                    String date = getExistingDate(fileContents);
                    if (date != null)
                    { //if there is an existing date, use that in the update
                        if (date.indexOf("-") != -1)
                        {
                            String startDate = date.substring(0, date.indexOf("-"));
                            String endDate = date.substring(date.indexOf("-") + 1, date
                                    .length());
                            if (endDate.equals(copyrightYear))
                            { //its the same, so don't do anything.
                                printDryRunMessage("Copyright is up to date (" + endDate
                                        + ").  No changes will be made.");
                                update = false;
                            }
                            else
                            {
                                newCopyrightDate = startDate + "-" + this.copyrightYear;
                                printDryRunMessage("Date " + date + " will be changed to "
                                        + newCopyrightDate);
                            }
                        }
                        else
                        {
                            if (date.equals(copyrightYear))
                            {
                                printDryRunMessage("Copyright is up to date (" + date
                                        + ").  No changes will be made.");
                                update = false;
                            }
                            else
                            {
                                newCopyrightDate = date + "-" + this.copyrightYear;
                                printDryRunMessage("Date " + date + " will be changed to "
                                        + date + "-" + this.copyrightYear);
                            }
                        }
                    }

                    if (update)
                    {
                        String copyrightText = getCopyrightText(newCopyrightDate);
                        fileContents = removeOldCopyrightStatement(fileContents);
                        fileContents = addCopyrightStatement(fileContents, copyrightText);
                    }
                }
                else if (existingCopyright && !regentsCopyright)
                { //not a UCRegents copyright, so leave it alone
                    printDryRunMessage("This file contains a non-UC Regents copyright so it will not be touched.");
                    update = false;
                }
                else
                { //there is no copyright statement on the file, add it
                    fileContents = addCopyrightStatement(fileContents,
                            getCopyrightText(this.copyrightYear));
                    printDryRunMessage("Missing copyright: " + f.getAbsolutePath());
                }

                //System.out.println("fileContents: " + fileContents);
                if (!dryRun && update)
                {
                    System.out.println("Writing copyright changes to "
                            + f.getAbsolutePath());
                    writeFile(f, fileContents);
                }
            }
        }
    }

    /**
     * print a message if we are doing a dry run.
     *
     * @param message
     */
    private void printDryRunMessage(String message)
    {
        if (dryRun)
        {
            System.out.println(message);
        }
    }

    /**
     * return the current copyright date if it exists, null otherwise.
     *
     * @param content
     * @return
     */
    private String getExistingDate(String content) throws Exception
    {
        content = getCurrentCopyrightStatement(content);
        int dateStartIndex = content.indexOf("(c)");
        Pattern p = Pattern.compile("\\d\\d\\d\\d");
        Matcher m = p.matcher(content);
        String date1 = null;
        String date2 = null;
        int start = 0;
        int end = 0;
        if (m.find(dateStartIndex))
        { //look for the first date
            start = m.start();
            end = m.end();
            date1 = content.substring(start, end);
        }

        if (date1 != null && m.find(end))
        {
            start = m.start();
            end = m.end();
            date2 = content.substring(start, end);
        }

        if (date1 != null && date2 != null)
        {
            return date1 + "-" + date2;
        }
        else
        {
            return date1;
        }
    }

    /**
     * check to see if the file contains a copyright statement already
     *
     * @param f
     * @return
     */
    private boolean checkForExistingCopyright(String contents) throws Exception
    {
        int start = 0;
        int end = 0;

        if (contents.trim().startsWith("/*"))
        {
            start = contents.trim().indexOf("/*");
        }
        else if (contents.trim().startsWith("/**"))
        {
            start = contents.trim().indexOf("/**");
        }

        end = contents.trim().indexOf("*/");
        if (start == -1 || end == -1)
        {
            return false;
        }

        String stmt = contents.substring(start, end);
        if (stmt.indexOf("(c)") != -1 || stmt.indexOf("copyright") != -1)
        {
            return true;
        }

        return false;
    }

    /**
     * return true if the copyright string contains the words
     * "regents", "university" and "california"
     *
     * @param content
     * @return
     */
    private boolean checkForUCRegentsCopyright(String content) throws Exception
    {
        content = getCurrentCopyrightStatement(content);
        content = content.toLowerCase();
        if (content.indexOf("regents") != -1 && content.indexOf("california") != -1
                && content.indexOf("university") != -1)
        {
            return true;
        }
        return false;
    }

    /**
     * remove an old copyright statement. return the contents with the statement
     * removed
     *
     * @param f
     */
    private String removeOldCopyrightStatement(String contents)
    {
        String commentChar = "";
        if (contents.trim().startsWith("/*"))
        {
            commentChar = "/*";
        }
        else if (contents.trim().startsWith("/**"))
            ;
        {
            commentChar = "/**";
        }
        int start = contents.indexOf(commentChar);
        int end = contents.indexOf("*/", 0);

        //System.out.println("removing: " + contents.substring(0, end + 2));

        return contents.substring(end + 2, contents.length()).trim();
    }

    /**
     * return the header comment that includes the copyright statement
     *
     * @param contents
     * @return
     */
    private String getCurrentCopyrightStatement(String contents) throws Exception
    {
        if (!checkForExistingCopyright(contents))
        {
            return "";
        }

        String commentChar = "";
        if (contents.trim().startsWith("/*"))
        {
            commentChar = "/*";
        }
        else if (contents.trim().startsWith("/**"))
            ;
        {
            commentChar = "/**";
        }
        int start = contents.indexOf(commentChar);
        int end = contents.indexOf("*/", 0);

        return contents.substring(0, end + 2);
    }

    /**
     * add the copyright statement to the top of the file. return the contents
     * with the new statement added
     *
     * @param f
     * @param stmt
     */
    private String addCopyrightStatement(String contents, String stmt)
    {
        return stmt + "\n" + contents;
    }

    /**
     * get the copyright text that should be inserted into the file
     *
     * @return
     * @throws BuildException
     */
    private String getCopyrightText(String copyrightDate) throws BuildException
    {
        if (copyrightFileName == null || copyrightDate == null)
        {
            throw new BuildException("You mus provide a file to read the copyright "
                    + "statement out of (-DcopyrightFile=xxx.txt) and a year "
                    + "(-DcopyrightYear=xxx) to set the copyright to.");
        }

        File f = new File(ProjectLocator.getProjectDir()
                + "/build-area/resources/release/" + copyrightFileName);
        if (!f.exists())
        {
            throw new BuildException("The file " + f.getAbsolutePath()
                    + " does not exist.  Please put your copyright file into "
                    + "build-area/resources/release/ and specify it with "
                    + "-DcopyrightFile=filename.txt.");
        }

        if (copyrightDate == null || copyrightDate.trim().equals(""))
        {
            throw new BuildException(
                    "You must provide a year string (-DcopyrightYear=xxx) to update the "
                            + "copyright statements with.");
        }

        try
        {
            String crStmt = readFile(f);
            crStmt = crStmt.replaceAll("\\$date\\$", copyrightDate);
            return crStmt;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new BuildException("Error getting the copyright file: "
                    + e.getMessage());
        }
    }

    /**
     * read a file into a string
     *
     * @param f
     * @return
     */
    private static String readFile(File f) throws Exception
    {
        FileReader fr = new FileReader(f);
        char[] c = new char[1024];
        int numread = fr.read(c, 0, 1024);
        StringBuffer sb = new StringBuffer();
        while (numread != -1)
        {
            sb.append(new String(c, 0, numread));
            numread = fr.read(c, 0, 1024);
        }

        String s = sb.toString();
        return s;
    }

    /**
     * write contents to the file
     *
     * @param f
     * @param contents
     * @throws Exception
     */
    private static void writeFile(File f, String contents) throws Exception
    {
        FileWriter fw = new FileWriter(f);
        fw.write(contents);
        fw.flush();
        fw.close();
  }
}