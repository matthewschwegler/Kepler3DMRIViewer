/*
 * Copyright (c) 2001-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-07-17 14:45:25 -0700 (Tue, 17 Jul 2012) $' 
 * '$Revision: 30210 $'
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

package util;

import ij.ImageJ;
import ij.macro.MacroRunner;

import java.io.File;
import java.net.URL;

import ptolemy.actor.gui.style.TextStyle;
import ptolemy.actor.lib.Sink;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// IJMacro
/**
 * <p>
 * This actor run ImageJ macros; see http://rsb.info.nih.gov/ij/ for information
 * on the ImageJ system for image processing. An image file name (String) is
 * input on the 'input' port and the ImageJ macro that is in the 'macroString'
 * parameter is carried out. ImageJ macros can be created by recording menu
 * actions, using the ImageJ menu toolbar.
 * </p>
 * 
 * @author Dan Higgins NCEAS UC SantaBarbara
 */
public class IJMacro extends Sink {

    /**
     * Construct an actor with the given container and name.
     * 
     * @param container
     *            The container.
     * @param name
     *            The name of this actor.
     * @exception IllegalActionException
     *                If the actor cannot be contained by the proposed
     *                container.
     * @exception NameDuplicationException
     *                If the container already has an actor with this name.
     */
    public IJMacro(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        macroString = new StringParameter(this, "macroString");
        new TextStyle(macroString, "macro");
        macroString.setExpression("run(\"Open...\", \"open=_FILE_\");");

        fileOrURL = new FileParameter(this, "fileOrURL");
    }

    // /////////////////////////////////////////////////////////////////
    // // ports and parameters ////

    /**
     * The file name or URL from which to read. This is a string with any form
     * accepted by File Attribute. This file name may also be input through the
     * 'input' port.
     */
    public FileParameter fileOrURL;

    /**
     * The ImageJ macro to execute. Note that if the expression "_FILE_" is
     * included in this string, it is replaced by the fileOrUrl parameter
     * string, enabling the insertion of the input image file
     */
    public StringParameter macroString;

    static ImageJ ij;

    // /////////////////////////////////////////////////////////////////
    // // public methods ////

    /**
     * If the specified attribute is <i>URL</i>, then close the current file (if
     * there is one) and open the new one.
     * 
     * @param attribute
     *            The attribute that has changed.
     * @exception IllegalActionException
     *                If the specified attribute is <i>URL</i> and the file
     *                cannot be opened.
     */
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {
        if (attribute == fileOrURL) {
            // Would it be worth checking to see if the URL exists and
            // is readable?
            try {
                _file = fileOrURL.asFile();
            } catch (Exception eee) {
            }
            if (_file != null) {
                try {
                    if (!_file.getParentFile().exists()) {
                        // If the directory does not exist, create it.  See
                        // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4143
                        if (!_file.getParentFile().mkdirs()) {
                            throw new IllegalActionException(this,
                                    "Failed to make directory \""
                                    + _file.getParentFile() + "\"");
                        }
                    }
                    _file.createNewFile(); // creates a new empty file if one
                    // does not exist
                } catch (Exception ex) {
                    throw new IllegalActionException(this, ex,
                            "Problem creating the output file \"" + _file
                                    + "\"");
                }
            }
            try {
                _url = fileOrURL.asURL();
            } catch (Exception ee) {
            }
        }
        super.attributeChanged(attribute);
    }

    /**
     * Clone the actor into the specified workspace. This calls the base class
     * and then set the filename public member.
     * 
     * @param workspace
     *            The workspace for the new object.
     * @return A new actor.
     * @exception CloneNotSupportedException
     *                If a derived class contains an attribute that cannot be
     *                cloned.
     */
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        IJMacro newObject = (IJMacro) super.clone(workspace);
        // newObject.output.setMultiport(true);
        return newObject;
    }

    /**
     * Open the file at the URL, and set the width of the output.
     */
    public void initialize() throws IllegalActionException {
        attributeChanged(fileOrURL);
    }

    /**
     * Read in an image.
     * 
     * @exception IllegalActionException
     *                If an IO error occurs.
     */
    public boolean prefire() throws IllegalActionException {
        if (_url == null) {
            _fileRoot = null;
        } else {
            try {
                _fileRoot = _url.getFile();
            } catch (Exception e) {
                _fileRoot = null;
            }
        }
        return super.prefire();
    }

    /**
    *
    */
    public synchronized void fire() throws IllegalActionException {
        super.fire();

        String name = "";
        // If the fileOrURL input port is connected and has data, then
        // get the file name from there.
        if (input.getWidth() > 0) {
            if (input.hasToken(0)) {
                name = ((StringToken) input.get(0)).stringValue();
            }
        } else {
            name = fileOrURL.getExpression();
        }

        if (name.indexOf(" ") > -1) {
            name = "'" + name + "'";
        }
        System.out.println("firing IJMacro");
        System.out.println("name: " + name);
        if (ij == null) {
            if (ShowLocations.ij != null) {
                ij = ShowLocations.ij;
            } else {
                ij = new ImageJ();
            }
        }
        if (ij != null && !ij.isShowing()) {
            ij.show();
        }

        String macro = macroString.getExpression();
        // System.out.println("macro: "+macro);
        name = name.replace('\\', '/');
        System.out.println("name(1): " + name);
        macro = macro.replaceAll("_FILE_", name);
        // System.out.println("macro: "+macro);
        // MacroRunner mr = new MacroRunner(macro);
        // The following arbitrary delay is needed to avoid problems with the
        // MacroRunner thread when this method is called several times.
        // (MacroRunner is an ImageJ class)
        // Obviously, this is a 'hack'!! (Dan Higgins - April 2007)
        try {
            Thread.currentThread();
            Thread.sleep(3000);
        } catch (Exception e) {
        }
        new MacroRunner(macro);
    }

    // /////////////////////////////////////////////////////////////////
    // // private members ////

    // The URL as a string.
    private String _fileRoot;

    // The File
    private File _file;

    // The URL of the file.
    private URL _url;
}