/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.helloworld;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// FileCopier

/**
 * This actor copies a file with path name into a directory that is given via a
 * port or as a parameter. If this directory does not exist, it is created. The
 * user can decide if an existing file with the same name is overwritten. The
 * file may also be moved instead of copied. The corresponding new file with
 * path name is given out.
 *
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: FileCopier.java 24234 2010-05-06 05:21:26Z welker $
 */
public class filecopier extends TypedAtomicActor {

    /**
     * Construct a FileCopier with the given container and name.
     *
     * @param container
     *            The container.
     * @param name
     *            The name of this actor.
     * @exception IllegalActionException
     *                If the entity cannot be contained by the proposed
     *                container.
     * @exception NameDuplicationException
     *                If the container already has an actor with this name.
     */
    public filecopier(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        directory = new PortParameter(this, "directory");
        directory.setStringMode(true);

        oldFile = new TypedIOPort(this, "oldFile", true, false);
        oldFile.setTypeEquals(BaseType.STRING);

        newFile = new TypedIOPort(this, "newFile", false, true);
        newFile.setTypeEquals(BaseType.STRING);

        overwrite = new Parameter(this, "Overwrite existing", new BooleanToken(
                false));
        overwrite.setTypeEquals(BaseType.BOOLEAN);

        move = new Parameter(this, "Move files", new BooleanToken(false));
        move.setTypeEquals(BaseType.BOOLEAN);

        _attachText("_iconDescription", "<svg>\n"
                + "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
                + "style=\"fill:white\"/>\n"
                + "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
                + " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
                + "</svg>\n");
    }

    // /////////////////////////////////////////////////////////////////
    // // ports and parameters ////

    /**
     * The port or parameter, which is a string with the directory name.
     */
    public PortParameter directory = null;
    /**
     * The input port, which contains the old file path and name.
     */
    public TypedIOPort oldFile = null;
    /**
     * The output port, which contains the new file path and name.
     */
    public TypedIOPort newFile = null;
    /**
     * The boolean parameter, which decides if existing files are overwritten.
     */
    public Parameter overwrite = null;
    /**
     * The boolean parameter, which decides if the files are moved instead of
     * copied.
     */
    public Parameter move = null;

    // /////////////////////////////////////////////////////////////////
    // // public methods ////

    /**
     * Take the files and put them into the new directory.
     *
     * @exception IllegalActionException
     *                If there's no director or if file copying does not work.
     */
    public void fire() throws IllegalActionException {
        super.fire();
        // Get all information.
        directory.update();
        _path = ((StringToken) directory.getToken()).stringValue();
        _original = ((StringToken) oldFile.get(0)).stringValue();
        _overwriteFile = ((BooleanToken) overwrite.getToken()).booleanValue();
        _moveFile = ((BooleanToken) move.getToken()).booleanValue();
        // Make the directory if necessary.
        _dir = new File(_path);
        if (!_dir.exists()) {
            _mkdirsSuccess = _dir.mkdirs();
            if (!_mkdirsSuccess) {
                throw new IllegalActionException(this, "Directory " + _dir
                        + " was not successfully made.");
            }
        } else {
            if (!_dir.isDirectory() || !_dir.canWrite()) {
                throw new IllegalActionException(this,
                        "Cannot write into directory " + _dir + ".");
            }
        }
        // Move or copy the file.
        _file = new File(_original);
        _target = new File(_dir, _file.getName());
        if (_overwriteFile || !_target.exists()) {
            try {
                if (_moveFile) {
                    _renameSuccess = _file.renameTo(_target);
                    if (!_renameSuccess) {
                        throw new IllegalActionException(this, "File " + _file
                                + " was not successfully moved.");
                    }
                } else {
                    _inStream = new FileInputStream(_file);
                    _outStream = new FileOutputStream(_target);
                    int i = 0;
                    while ((i = _inStream.read()) != -1) {
                        _outStream.write(i);
                    }
                    _inStream.close();
                    _outStream.close();
                }
                _copy = new StringToken(_target.getCanonicalPath());
            } catch (Exception ex) {
                _debug("File cannot be copied or moved.");
            }
            newFile.send(0, _copy);
        }
    }

    // /////////////////////////////////////////////////////////////////
    // // protected members ////

    // /////////////////////////////////////////////////////////////////
    // // private methods ////

    // /////////////////////////////////////////////////////////////////
    // // private members ////

    private String _path;
    private String _original;
    private boolean _overwriteFile;
    private boolean _moveFile;
    private File _dir;
    private boolean _mkdirsSuccess;
    private File _file;
    private File _target;
    private StringToken _copy;
    private boolean _renameSuccess;
    private FileInputStream _inStream;
    private FileOutputStream _outStream;
}
