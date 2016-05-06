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
package org.kepler.build.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.tools.ant.BuildException;

/**
 * class to represent command line execution
 *
 * @author berkley
 */
public class CommandLine
{
    private static int exitCode = -99;

    /**
     * return true if there was an error
     *
     * @return
     */
    public static boolean errorExecutingLastProcess()
    {
        return exitCode != 0;
    }

    /**
     * print the command
     *
     * @param command
     */
    private static void printCommand(String[] command)
    {
        for (String s : command)
        {
            System.out.print(s + " ");
        }
        System.out.println();
    }

    /**
     * execute a command
     *
     * @param command
     * @throws IOException
     */
    public static void exec(String command) throws IOException
    {
        exec(command.split("\\s+"));
    }

    /** Execute a set of commands. */
    public static void exec(String[] command) throws IOException
    {
        exec(command, null);
    }
    
    /** Execute a set of commands in a specific directory. */
    public static void exec(String[] command, File directory) throws IOException
    {
        printCommand(command);
        ProcessBuilder procBuilder = new ProcessBuilder(command);
        if(directory != null)
        {
            procBuilder.directory(directory);
        }
        procBuilder.redirectErrorStream(true);
        Process p = procBuilder.start();
        try
        {
            printProcess(p);
        }
        catch(CommandLineExecutionException e)
        {
            p.destroy();
            System.out.println(e.getMessage());
            System.out.println("The command must be run by hand:");
            for(String str : command)
            {
                System.out.print(str + " ");
            }
            System.out.println("");
            if (System.getProperty("os.name").startsWith("Windows")) {
                System.out.println("Under Cygwin, use forward slashes:");
                for(String str : command)
                {
                    System.out.print(str.replace("\\","/"));
                }
            }
            return;
        }

    }

    /**
     * execute a list of commands
     *
     * @param command
     * @throws IOException
     */
    public static void exec(List<String> command) throws IOException
    {
        exec(command.toArray(new String[command.size()]));
    }

    /**
     * print out the command
     *
     * @param command
     */
    public static void mockExec(String command)
    {
        System.out.println(command);
    }

    /**
     * print out the command array
     *
     * @param command
     */
    public static void mockExec(String[] command)
    {
        printCommand(command);
    }

    /**
     * print out the list of commands
     *
     * @param command
     */
    public static void mockExec(List<String> command)
    {
        mockExec(command.toArray(new String[command.size()]));
    }

    /**
     * print the process
     *
     * @param p
     * @throws IOException
     */
    private static void printProcess(Process p) throws IOException, CommandLineExecutionException
    {
        printInputStream(p.getInputStream());
        System.out.println();
        try
        {
            exitCode = p.waitFor();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        if (exitCode != 0)
        {
            throw new BuildException(
                    "ERROR: It appears that the command did not execute properly and exited with an exit code of: "
                            + exitCode);
            //System.out.println("WARNING: It appears that the command did not execute properly and exited with an exit code of: " + exitCode);
        }
    }

    /**
     * print the intput stream
     *
     * @param is
     * @throws IOException
     */
    private static void printInputStream(InputStream is) throws IOException, CommandLineExecutionException
    {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                System.out.println(line);
                if (line.startsWith("Error validating server certificate for"))
                {
                    // See http://chess.eecs.berkeley.edu/ptexternal/wiki/Main/Subversion#svnCertficateIsNotIssuedByATrustedAuthority
                    throw new CommandLineExecutionException("SVN server certificate could not be validated.");
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /** An exception thrown which the child process encounters an error. */
    private static class CommandLineExecutionException extends Exception
    {
        public CommandLineExecutionException(String message)
        {
            super(message);
        }
    }
}
