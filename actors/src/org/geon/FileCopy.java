/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package org.geon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// FileCopy
/**
 * This actor copies a source file to a destination file and outputs the
 * destination file URL.
 * 
 * The source and destination file paths can be accepted either through a port
 * or a parameter.
 * 
 * If the append attribute has value true, then the file will be appended to. If
 * it has value false, then if the file exists, the user will be queried for
 * permission to overwrite, and if granted, the file will be overwritten.
 * 
 * If the confirmOverwrite parameter has value false, then this actor will
 * overwrite the specified file if it exists without asking. If true (the
 * default), then if the file exists, then this actor will ask for confirmation
 * before overwriting.
 * 
 * @see FileParameter
 * @author Efrat Jaeger
 * @version $Id: FileCopy.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0.1
 */

public class FileCopy extends TypedAtomicActor {

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
	public FileCopy(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		sourceFile = new TypedIOPort(this, "sourceFile", true, false);
		sourceFile.setTypeEquals(BaseType.STRING);

		destinationFile = new TypedIOPort(this, "destinationFile", true, false);
		destinationFile.setTypeEquals(BaseType.STRING);

		outputFile = new TypedIOPort(this, "outputFile", false, true);
		outputFile.setTypeEquals(BaseType.STRING);

		sourceFileParam = new FileParameter(this, "sourceFileParam");
		sourceFileParam.setDisplayName("source File");

		destFileParam = new FileParameter(this, "destFileParam");
		destFileParam.setDisplayName("destination File");

		new Parameter(sourceFileParam, "allowDirectories", BooleanToken.TRUE);
		new Parameter(destFileParam, "allowDirectories", BooleanToken.TRUE);

		append = new Parameter(this, "append");
		append.setTypeEquals(BaseType.BOOLEAN);
		append.setToken(BooleanToken.FALSE);

		confirmOverwrite = new Parameter(this, "confirmOverwrite");
		confirmOverwrite.setTypeEquals(BaseType.BOOLEAN);
		confirmOverwrite.setToken(BooleanToken.TRUE);
		
		recursive = new Parameter(this, "recursive");
		recursive.setTypeEquals(BaseType.BOOLEAN);
		recursive.setToken(BooleanToken.TRUE);

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
	 * Source file.
	 */
	public TypedIOPort sourceFile;

	/**
	 * Destination file.
	 */
	public TypedIOPort destinationFile;

	/**
	 * Output destination file URL.
	 */
	public TypedIOPort outputFile;

	/**
	 * Source file name or URL. This is a string with any form accepted by
	 * FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter sourceFileParam;

	/**
	 * Destination file name or URL. This is a string with any form accepted by
	 * FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter destFileParam;

	/**
	 * If true, then append to the specified file. If false (the default), then
	 * overwrite any preexisting file after asking the user for permission.
	 */
	public Parameter append;

	/**
	 * If false, then overwrite the specified file if it exists without asking.
	 * If true (the default), then if the file exists, ask for confirmation
	 * before overwriting.
	 */
	public Parameter confirmOverwrite;
	
	/**
	 * If false, then only copy the files, not the sub-directories, in the source location. 
	 */
	public Parameter recursive;

	/**
	 * Copy the source file to the destination file. Broadcast the destination
	 * file path.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		File _sourceFile = null, _destFile = null;
		String fileName = "";

		// get source file.
		if (sourceFile.getWidth() > 0) {
			fileName = ((StringToken) sourceFile.get(0)).stringValue();
			int lineEndInd = fileName.indexOf("\n");
			if (lineEndInd != -1) { // The string contains a CR.
				fileName = fileName.substring(0, lineEndInd);
			}
			sourceFileParam.setExpression(fileName);
		}
		_sourceFile = sourceFileParam.asFile();

		if (!_sourceFile.exists()) {
			throw new IllegalActionException(this, "file " + fileName
					+ " doesn't exist.");
		}

		// get dest file.
		fileName = "";
		if (destinationFile.getWidth() > 0) {
			fileName = ((StringToken) destinationFile.get(0)).stringValue();
			int lineEndInd = fileName.indexOf("\n");
			if (lineEndInd != -1) { // The string contains a CR.
				fileName = fileName.substring(0, lineEndInd);
			}
			destFileParam.setExpression(fileName);
		}
		_destFile = destFileParam.asFile();

		boolean appendValue = ((BooleanToken) append.getToken()).booleanValue();
		boolean confirmOverwriteValue = ((BooleanToken) confirmOverwrite
				.getToken()).booleanValue();
		boolean recursiveValue = ((BooleanToken) recursive
				.getToken()).booleanValue();
		// Don't ask for confirmation in append mode, since there
		// will be no loss of data.
		if (_destFile.exists() && !appendValue && confirmOverwriteValue) {
			// Query for overwrite.
			// FIXME: This should be called in the event thread!
			// There is a chance of deadlock since it is not.
			if (!GraphicalMessageHandler.yesNoQuestion("OK to overwrite "
					+ _destFile + "?")) {
				throw new IllegalActionException(this,
						"Please select another file name.");
			}
		}

		try {
			copyFiles(_sourceFile,_destFile, recursiveValue);

		} catch (Exception ex) {
			throw new IllegalActionException(this, ex, "Error copying "
					+ _sourceFile.getPath() + " to " + _destFile.getPath()
					+ ".");
		}
		outputFile.broadcast(new StringToken(_destFile.getAbsolutePath()));
	}
	
    void copyFiles(File sourceLocation , File targetLocation, boolean recursive)
    throws IOException {
        
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
            	File sourceChildFile = new File(sourceLocation, children[i]);
            	if (recursive)
            	copyFiles(sourceChildFile,
                        new File(targetLocation, children[i]), true);
            	else
            	{
            		if (sourceChildFile.isFile())
                	copyFiles(sourceChildFile,
                            new File(targetLocation, children[i]), false);
            	}
            }
        } else {
            
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
    
    void copyFile(File sourceLocation , File targetLocation)
    throws IOException {
        InputStream in = new FileInputStream(sourceLocation);
        OutputStream out = new FileOutputStream(targetLocation);
        
        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

}