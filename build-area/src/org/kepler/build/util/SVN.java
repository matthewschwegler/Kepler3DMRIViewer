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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Input;
import org.kepler.build.project.ProjectLocator;

/**
 * class that represents an SVN object
 *
 * @author berkley
 */
public class SVN
{
    public static String username;
    public static String password;

    /**
     * get the username and password from std.in
     *
     * @param task
     */
    public static void getUsernameAndPassword(Task task)
    {
        Input usernameInput = new Input();
        usernameInput.bindToOwner(task);
        usernameInput.setMessage("Enter your SVN username:");
        usernameInput.setAddproperty("svn.username");
        usernameInput.execute();
        Input passwordInput = new Input();
        passwordInput.bindToOwner(task);
        //Note: It is not possible to mask the password until we upgrade to Java 1.6.
        //passwordInput.createHandler().setClassname("org.apache.tools.ant.input.SecureInputHandler");
        passwordInput.setMessage("Enter your SVN password:");
        passwordInput.setAddproperty("svn.password");
        passwordInput.execute();

        username = ProjectLocator.getAntProject().getProperty("svn.username");
        password = ProjectLocator.getAntProject().getProperty("svn.password");
    }
}
