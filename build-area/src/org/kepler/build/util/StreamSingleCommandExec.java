/* Run a single command

 Copyright (c) 2009 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY
 */
package org.kepler.build.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Execute commands in a subprocess and send the results to stderr and stdout.
 * <p>As an alternative to this class, see {@link ptolemy.gui.JTextAreaExec},
 * which uses Swing; and {@link ptolemy.util.StringBufferExec}, which writes to
 * a StringBuffer.
 * <p>Sample usage:
 * <pre>
 * List<String> execCommands = new ArrayList<String>();
 * execCommands.add("ls");
 * execCommands.add("-l");
 * execCommands.add(".");
 * final StreamSingleCommandExec exec = new StreamSingleCommandExec();
 * exec.setCommands(execCommands);
 * exec.start();
 * </pre>
 * <p>Loosely based on Example1.java from
 * <a
 * href="http://java.sun.com/products/jfc/tsc/articles/threads/threads2.html">
 * http://java.sun.com/products/jfc/tsc/articles/threads/threads2.html</a>
 * <p>See also
 * <a href="http://developer.java.sun.com/developer/qow/archive/135/index.jsp">
 * http://developer.java.sun.com/developer/qow/archive/135/index.jsp</a>
 * and
 * <a
 * href="http://jw.itworld.com/javaworld/jw-12-2000/jw-1229-traps.html">http:/
 * /jw.itworld.com/javaworld/jw-12-2000/jw-1229-traps.html</a>
 *
 * @author Christopher Brooks
 * @version $Id: StreamSingleCommandExec.java 22329 2010-01-06 21:27:01Z welker
 *          $
 * @Pt.ProposedRating Red (cxh)
 * @Pt.AcceptedRating Red (cxh)
 * @see ptolemy.gui.JTextAreaExec
 * @see ptolemy.util.StreamSingleCommandExec
 * @see ptolemy.util.StringBufferExec
 * @since Ptolemy II 8.0
 */
public class StreamSingleCommandExec implements ExecuteCommands
{

    /**
     * Create a StreamSingleCommandExec.
     */
    public StreamSingleCommandExec()
    {
        // Does nothing?
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /**
     * Append to the path of the subprocess. If directoryName is already
     * in the path, then it is not appended.
     *
     * @param directoryName The name of the directory to append to the path.
     */
    public void appendToPath(String directoryName)
    {
        // FIXME: Code Duplication from JTextAreaExec.java
        if (_debug)
        {
            stdout("StreamSingleCommandExec.appendToPath(): " + directoryName + "\n");
        }

        // Might be Path, might be PATH
        String keyPath = "PATH";
        String path = getenv(keyPath);
        if (path == null)
        {
            path = getenv("Path");
            if (path != null)
            {
                keyPath = "Path";
            }
            if (_debug)
            {
                stdout("StreamSingleCommandExec.appendToPath() Path: " + path + "\n");
            }
        }
        else
        {
            if (_debug)
            {
                stdout("StreamSingleCommandExec.appendToPath() PATH: " + path + "\n");
            }
        }

        if (path == null
                || path.indexOf(File.pathSeparatorChar + directoryName
                + File.pathSeparatorChar) == -1)
        {
            if (_debug)
            {
                stdout("StreamSingleCommandExec.appendToPath() updating\n");
            }
            _envp = StreamSingleCommandExec.updateEnvironment(keyPath,
                    File.pathSeparatorChar + directoryName + File.pathSeparatorChar);

            if (_debug)
            {
                // For debugging
                for (int i = 0; i < _envp.length; i++)
                {
                    stdout("StreamSingleCommandExec.appendToPath() " + _envp[i]);
                }
            }
        }
    }

    /**
     * Cancel any running commands.
     */
    public void cancel()
    {
        //_worker.interrupt();
        if (_process != null)
        {
            _process.destroy();
        }
    }

    /**
     * Clear the text area, status bar and progress bar.
     */
    public void clear()
    {
        updateStatusBar("");
        _updateProgressBar(0);
    }

    /**
     * Get the value of the environment of the subprocess.
     *
     * @param key The key for which to search.
     * @return The value of the key. If the key is not set, then
     *         null is returned. If appendToPath() has been called, and
     *         the then the environment for the subprocess is checked, which
     *         might be different than the environment for the current process
     *         because appendToPath() was called. Note that that key is searche
     *         for in a case-insensitive mode.
     */
    public String getenv(String key)
    {
        // FIXME: Code Duplication from JTextAreaExec.java
        if (_envp == null)
        {
            // Sigh.  System.getEnv("PATH") and System.getEnv("Path")
            // will return the same thing, even though the variable
            // is Path.  Updating PATH is wrong, the subprocess will
            // not see the change.  So, we search the env for a direct
            // match
            Map<String, String> environmentMap = System.getenv();
            return environmentMap.get(key);
        }
        for (int i = 0; i < _envp.length; i++)
        {
            if (key.regionMatches(false /* ignoreCase */, 0, _envp[i], 0, key
                    .length()))
            {
                return _envp[i].substring(key.length() + 1, _envp[i].length());
            }
        }
        return null;
    }

    /**
     * Return the return code of the last subprocess that was executed.
     *
     * @return the return code of the last subprocess that was executed.
     */
    public int getLastSubprocessReturnCode()
    {
        return _subprocessReturnCode;
    }

    /**
     * Set the list of tokens to this command.
     *
     * @param commands A list of Strings, where each element is a
     *                 token to a single command.
     */
    public void setCommands(List<String> commands)
    {
        _commands = commands;
    }

    /**
     * Set the working directory of the subprocess.
     *
     * @param workingDirectory The working directory of the
     *                         subprocess. If this argument is null, then the subprocess is
     *                         executed in the working directory of the current process.
     */
    public void setWorkingDirectory(File workingDirectory)
    {
        _workingDirectory = workingDirectory;
    }

    /**
     * Start running the commands.
     */
    public void start()
    {
        String returnValue = _executeCommands();
        updateStatusBar(returnValue);
        stdout(returnValue);
    }

    /**
     * Append the text message to stderr. A derived class could
     * append to a StringBuffer. @link{JTextAreaExec} appends to a
     * JTextArea. The output automatically gets a trailing newline
     * appended.
     *
     * @param text The text to append to standard error.
     */
    public void stderr(final String text)
    {
        System.err.println(text);
        System.err.flush();
    }

    /**
     * Append the text message to the output. A derived class could
     * append to a StringBuffer. @link{JTextAreaExec} appends to a
     * JTextArea.
     * The output automatically gets a trailing newline appended.
     *
     * @param text The text to append to standard out.
     */
    public void stdout(final String text)
    {
        System.out.println(text);
        System.out.flush();
    }

    /**
     * Update the environment and return the results.
     * Read the environment for the current process, append the value
     * of the value parameter to the environment variable named by
     * the key parameter.
     *
     * @param key   The environment variable to be updated.
     * @param value The value to append
     * @return An array of strings that consists of the subprocess
     *         environment variable names and values in the form
     *         <code>name=value</code> with the environment variable
     *         named by the key parameter updated to include the value
     *         of the value parameter.
     */
    public static String[] updateEnvironment(String key, String value)
    {
        // This is static so that we can share it among
        // ptolemy.util.StreamSingleCommandExec
        // StringBufferExec, which extends Stream Exec
        // and
        // ptolemy.gui.JTextAreaExec, which extends JPanel

        Map<String, String> env = new HashMap(System.getenv());

        env.put(key, value + env.get(key));
        String[] envp = new String[env.size()];

        int i = 0;
        Iterator entries = env.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry entry = (Map.Entry) entries.next();
            envp[i++] = entry.getKey() + "=" + entry.getValue();
            // System.out.println("StreamSingleCommandExec(): " + envp[i-1]);
        }
        return envp;
    }

    /**
     * Set the text of the status bar. In this base class, do
     * nothing, derived classes may update a status bar.
     *
     * @param text The text with which the status bar is updated.
     */
    public void updateStatusBar(final String text)
    {
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /**
     * Set the maximum of the progress bar. In this base class, do
     * nothing, derived classes may update the size of the progress bar.
     *
     * @param size The maximum size of the progress bar.
     */
    protected void _setProgressBarMaximum(int size)
    {
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /**
     * Execute the commands in the list. Update the output with
     * the command being run and the output.
     */
    private String _executeCommands()
    {
        try
        {
            Runtime runtime = Runtime.getRuntime();

            try
            {
                if (_process != null)
                {
                    _process.destroy();
                }

                _setProgressBarMaximum(4);

                if (Thread.interrupted())
                {
                    throw new InterruptedException();
                }

                if (_workingDirectory != null)
                {
                    stdout("In \"" + _workingDirectory + "\", about to execute:");
                }
                else
                {
                    stdout("About to execute:");
                }

                StringBuffer commandBuffer = new StringBuffer();
                StringBuffer statusCommand = new StringBuffer();
                for (String token : _commands)
                {
                    if (commandBuffer.length() > 1)
                    {
                        commandBuffer.append(" ");
                    }
                    commandBuffer.append(token);

                    // Accumulate the first 50 chars for use in
                    // the status buffer.
                    if (statusCommand.length() < 50)
                    {
                        if (statusCommand.length() > 0)
                        {
                            statusCommand.append(" ");
                        }

                        statusCommand.append(token);
                    }
                }
                stdout("    " + commandBuffer.toString());
                if (statusCommand.length() >= 50)
                {
                    statusCommand.append(" . . .");
                }

                updateStatusBar("Executing: " + statusCommand.toString());

                _updateProgressBar(2);
                // If _envp is null, then no environment changes.
                _process = runtime.exec(
                        _commands.toArray(new String[_commands.size()]), _envp,
                        _workingDirectory);
                _updateProgressBar(3);

                // Set up a Thread to read in any error messages
                _StreamReaderThread errorGobbler = new _StreamReaderThread(_process
                        .getErrorStream(), this);

                // Set up a Thread to read in any output messages
                _StreamReaderThread outputGobbler = new _StreamReaderThread(_process
                        .getInputStream(), this);

                // Start up the Threads
                errorGobbler.start();
                outputGobbler.start();

                try
                {
                    _subprocessReturnCode = _process.waitFor();

                    synchronized (this)
                    {
                        _process = null;
                    }

                    if (_subprocessReturnCode != 0)
                    {
                        throw new IOException("Subprocess \"" + commandBuffer
                                + "\" returned " + _subprocessReturnCode);
                    }
                }
                catch (InterruptedException interrupted)
                {
                    stderr("InterruptedException: " + interrupted);
                    throw interrupted;
                }
            }
            catch (final IOException io)
            {
                stderr("IOException: " + io);
            }
        }
        catch (InterruptedException e)
        {
            _process.destroy();
            _updateProgressBar(0);
            return "Interrupted"; // SwingWorker.get() returns this
        }

        _updateProgressBar(4);

        return "All Done"; // or this
    }

    /**
     * Update the progress bar. In this base class, do nothing.
     *
     * @i The current location of the progress bar.
     */
    private void _updateProgressBar(final int i)
    {
    }

    ///////////////////////////////////////////////////////////////////
    ////                         inner classes                     ////

    /**
     * Private class that reads a stream in a thread and updates the
     * the StreamSingleCommandExec.
     */
    private static class _StreamReaderThread extends Thread
    {

        // FindBugs suggests making this class static so as to decrease
        // the size of instances and avoid dangling references.

        /**
         * Construct a StreamReaderThread.
         *
         * @param inputStream the stream from which to read.
         * @param streamExec  The StreamSingleCommandExec to be written.
         */
        _StreamReaderThread(InputStream inputStream,
                            StreamSingleCommandExec streamExec)
        {
            _inputStream = inputStream;
            _streamExec = streamExec;
        }

        /**
         * Read lines from the _inputStream and output them.
         */
        public void run()
        {
            try
            {
                InputStreamReader inputStreamReader = new InputStreamReader(
                        _inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = null;

                while ((line = bufferedReader.readLine()) != null)
                {
                    _streamExec.stdout( /* _streamType + ">" + */
                            line);
                }
            }
            catch (IOException ioe)
            {
                _streamExec.stderr("IOException: " + ioe);
            }
        }

        /**
         * Stream from which to read.
         */
        private InputStream _inputStream;

        /**
         * StreamSingleCommandExec which is written.
         */
        private StreamSingleCommandExec _streamExec;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /**
     * The list of tokens in the command to be executed.
     */
    private List<String> _commands;

    private final boolean _debug = false;

    /**
     * The environment, which is an array of Strings of the form
     * <code>name=value</code>. If this variable is null, then
     * the environment of the calling process is used.
     */
    private String[] _envp;

    /**
     * The Process that we are running.
     */
    private Process _process;

    /**
     * The return code of the last Runtime.exec() command.
     */
    private int _subprocessReturnCode;

    /**
     * The working directory of the subprocess. If null, then
     * the subprocess is executed in the working directory of the current
     * process.
     */
    private File _workingDirectory;
}
