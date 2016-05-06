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

import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.Suite;

/**
 * class to print an error
 *
 * @author berkley
 */
public class PrintError
{
    /**
     * print a message
     *
     * @param message
     */
    public static void message(String message)
    {
        // show error message in dialog
        try {
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        } catch(Throwable t) {
            // if there's an error, e.g., running headless, print to stderr
            System.err.println("Error: " + message);
        }
    }
    
    /** Display an error message along with a Throwable. */
    public static void message(String message, Throwable throwable) {
        
        try {
            
            // the following is from 
            // ptolemy.gui.UndeferredGraphicalMessageHandler._showStackTrace()
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
    
            JTextArea text = new JTextArea(sw.toString(), 60, 80);
            JScrollPane stext = new JScrollPane(text);
            stext.setPreferredSize(new Dimension(600, 300));
            text.setCaretPosition(0);
            text.setEditable(false);
            
            // We want to stack the text area with another message
            Object[] objects = new Object[2];
    
            if (throwable.getMessage() != null) {
                objects[0] = message + "\n" + throwable.getMessage();
            } else {
                objects[0] = message;
            }
            
            objects[1] = stext;
           
            JOptionPane.showMessageDialog(null, objects, "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch(Throwable t) {
            System.err.println(message);
            if(throwable.getMessage() != null) {
                System.err.println(throwable.getMessage());
            }
            t.printStackTrace();
        }
    }

    /**
     * print a module not defined error
     */
    public static void moduleNotDefined()
    {
        message("You have failed to specify -Dmodule=<module.name> which is required.");
    }

    /**
     * print a suite not defined error
     */
    public static void suiteNotDefined()
    {
        message("You have failed to specify -Dsuite=<suite.name> which is required.");
    }

    /**
     * print a module not compiled message
     *
     * @param module
     */
    public static void moduleNotCompiled(Module module)
    {
        message("The module " + module.getName() + " is not compiled.");
    }

    /**
     * print a must define suite error
     */
    public static void mustDefineSuiteOrModule()
    {
        message("You have failed to specify either -Dsuite=<suite.name> or -Dmodule=<module.name>, one of which is required.");
    }

    /**
     * print an invalid url syntax error
     */
    public static void invalidUrlSyntax()
    {
        message("The syntax of the url you entered is invalid.");
    }

    /**
     * print a not a suite error
     *
     * @param suite
     */
    public static void notASuite(Suite suite)
    {
        message(suite.getName()
                + " is either not a suite or wasn't downloaded correctly.");
    }

    /**
     * print a workflow does not exist error
     */
    public static void workflowDoesNotExist()
    {
        message("The workflow you specified does not exist.");
        System.out
                .println("Please enter a valid workflow using either an absolute path or relative path.");
    }

    /**
     * print a branch does not exist error
     */
    public static void branchDoesNotExist()
    {
        message("The branch you specified does not exist.");
    }
}
