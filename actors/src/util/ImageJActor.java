/*
 * Copyright (c) 2001-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31113 $'
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

import java.io.File;
import java.net.URL;

import ptolemy.actor.injection.ActorModuleInitializer;
import ptolemy.actor.injection.PtolemyInjector;
import ptolemy.actor.lib.Sink;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// ImageJActor
/**
 * This actor launches the ImageJ package from NIH using the input file. see
 * http://rsb.info.nih.gov/ij/ for information on the ImageJ system for image
 * processing
 * 
 * This actor simply extends Sink; it thus has an 'input' multiport The input is
 * assumed to a file name which ImageJ will try to open. ImageJ will succeed in
 * opening a number of different types of image files.
 * 
 * When this actor fires, the image file input should be displayed along with a
 * small 'ImageJ' window with a set of menu items. These menu can be used to
 * apply a number of image processing operation to the displayed image.
 * 
 * @author Dan Higgins
 */
public class ImageJActor extends Sink {

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
    public ImageJActor(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        fileOrURL = new FileParameter(this, "fileOrURL");
    }

    // /////////////////////////////////////////////////////////////////
    // // ports and parameters ////

    /**
     * The file name or URL from which to read. This is a string with any form
     * accepted by File Attribute.
     * 
     * The file name should refer to an image file that is one of the many image
     * files that ImageJ can display. These include tiffs, gifs, jpegs, etc. See
     * the ImageJ Help menu or http://rsb.info.nih.gov/ij/
     */
    public FileParameter fileOrURL;

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
            try { // apparently, if the path doesn't exist, asFile throws an
                // exception
                // This catches that and sets _file to null, avoiding problem
                _file = fileOrURL.asFile();
            } catch (Exception e) {
                _file = null;
            }
            if (_file != null) {
                try {
                    if (!_file.getParentFile().exists()) {
                        if (!_file.getParentFile().mkdirs()) {
                            // If the directory does not exist, create it.  See
                            // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4143
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
                try {
                    _url = fileOrURL.asURL();
                } catch (Exception ee) {
                }
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
        ImageJActor newObject = (ImageJActor) super.clone(workspace);
        // newObject.output.setMultiport(true);
        return newObject;
    }

    /**
     * Open the file at the URL, and set the width of the output.
     */
    public void initialize() throws IllegalActionException {
        // only do this if no input file is passed
        if (input.getWidth() == 0) {
            attributeChanged(fileOrURL);
        }
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
        
        // if (!input.hasToken(0)) return;

        // If the fileOrURL input port is connected and has data, then
        // get the file name from there.
        if (input.getWidth() > 0) {
            if (input.hasToken(0)) {
                String name = ((StringToken) input.get(0)).stringValue();
                // Using setExpression() rather than setToken() allows
                // the string to refer to variables defined in the
                // scope of this actor.
                fileOrURL.setExpression(name);
                _url = fileOrURL.asURL();
                if (_url.getProtocol().equalsIgnoreCase("file"))
                	_url = null;
                // _fileRoot = _url.getFile();
                File f = fileOrURL.asFile();
                try {
                    _fileRoot = f.getCanonicalPath();
                } catch (Exception e) {
                    System.out.println("Cannot get canonical path!");
                }
            }
        }
        _getImplementation().show(_fileRoot, _url);
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////


    /** Get the right instance of the implementation depending upon the
     *  of the dependency specified through dependency injection.
     *  If the instance has not been created, then it is created.
     *  If the instance already exists then return the same. 
     *
     *	<p>This code is used as part of the dependency injection needed for the
     *  HandSimDroid project, see $PTII/ptserver.  This code uses dependency
     *  inject to determine what implementation to use at runtime.
     *  This method eventually reads ptolemy/actor/ActorModule.properties.
     *  {@link ptolemy.actor.injection.ActorModuleInitializer#initializeInjector()}
     *  should be called before this method is called.  If it is not
     *  called, then a message is printed and initializeInjector() is called.</p>
     *
     *  @return the instance of the implementation.
     */
    protected ImageJInterface _getImplementation() {
        if (_implementation == null) {
        	
//			final ArrayList actorModules = new ArrayList<PtolemyModule>();
//		    actorModules.add(new PtolemyModule(ResourceBundle
//		                .getBundle("org/kepler/ActorModule")));
//			Initializer _defaultInitializer = new Initializer() {
//		        public void initialize() {
//		            PtolemyInjector.createInjector(actorModules);
//		        }
//			};
//			ActorModuleInitializer.setInitializer(_defaultInitializer);
        	
		    if (PtolemyInjector.getInjector() == null) {
				System.err.println("Warning: main() did not call "
					       + "ActorModuleInitializer.initializeInjector(), "
					       + "so ImageJActor is calling it for you.");
				ActorModuleInitializer.initializeInjector();
		    }
		    
            _implementation = PtolemyInjector.getInjector().getInstance(
            		ImageJInterface.class);
            try {
                _implementation.init(this);
            } catch (NameDuplicationException e) {
                throw new InternalErrorException(this, e,
                        "Failed to initialize implementation");
            } catch (IllegalActionException e) {
                throw new InternalErrorException(this, e,
                        "Failed to initialize implementation");
            }
        }
        return _implementation;
    }

    // /////////////////////////////////////////////////////////////////
    // // private members ////

    // Implementation of the ImageJInterface
    private ImageJInterface _implementation;
    
    // The URL as a string.
    private String _fileRoot;

    // The File
    private File _file;

    // The URL of the file.
    private URL _url;
}