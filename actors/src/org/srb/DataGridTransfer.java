/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.srb;

import java.io.OutputStream;
import java.net.URI;
import java.util.Vector;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.util.MessageHandler;
import edu.sdsc.grid.io.FileFactory;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.GeneralFileSystem;
import edu.sdsc.grid.io.GeneralMetaData;
import edu.sdsc.grid.io.GeneralRandomAccessFile;
import edu.sdsc.grid.io.MetaDataCondition;
import edu.sdsc.grid.io.MetaDataSelect;
import edu.sdsc.grid.io.MetaDataSet;

//////////////////////////////////////////////////////////////////////////
//// DataGridTransfer
/** <p>DataGridTransfer is a Kepler Actor which has a functionality similar to 
 *  the SRB/IRODS commands, namely Sget, Sput, iget, and iput. 
 *  DataGridTransfer copies one or more objects to/from a remote filesystem into 
 *  the local file system.
 *  The following actor expects as input a reference to local or remote file systems support by the Jargon API. 
 *  This reference connection is created from the source and destination URL values, 
 *  Currently available filesystem URLs are, file:///myDir/myfile.txt,
 *  irods://username:password@myhost.org:1247/myDir/myfile.txt, 
 *  srb://username.domain:password@myhost.org:5544/myDir/myfile.txt, or ftp and http urls.
 *  </p><p>
 *  Currently, the source and destination filesystems can not be changed once the workflow is running. 
 *  The filepaths can be changed.
 *  </p><p>
 *  The file reference system is created with a unique user account and 
 *  with this connection reference as input the DataGridTransfer actor is 
 *  able to gain access to various files on the file systems.
 *  Once an alive DataGridTransfer file connection system has been 
 *  established the actor gets the destination directory and the source files 
 *  to establish the DataGridTransfer file path. 
 *  If the DataGridTransfer destination directory doesn't exist, a new directory
 *  is created. Once the DataGridTransfer files path are determined, 
 *  the files are copied from the source file space to the
 *  local drive. In case the above process of parallel copy fails, 
 *  a streaming copy process is carried out with random access streams 
 *  where the file is downloaded as a sequence of byte arrays.
 *  </p><p>
 *  There is a parameter to overwrite existing files.
 *  </p><p>
 *  <B>Actor Input:</B> Accepts a reference to the
 *  the files systems, and two arrays of URL file paths.
 *  </p><p>
 *  <B>Actor Output:</B> Outputs the destination file paths and an exit status. The exit status gives
 *  a message of "success" or appropriate error to indicate the status of file get process.
 *
 * </p><p>The following actor uses the Dice Research Jargon API provided. 
 * <A href="http://www.sdsc.edu/srb"><I>Further information on SRB</I> </A>
 * <A href="http://www.irods.org"><I>Further information on IRODS</I> </A>

   @author Lucas Gilbert, Efrat Jaeger
   @category.name datagrid
   @category.name transfer
*/

public class DataGridTransfer extends TypedAtomicActor {

	/** Construct a constant source with the given container and name.
	 *  Create the <i>value</i> parameter, initialize its value to
	 *  the default value of an IntToken with value 1.
	 *  @param container The container.
	 *  @param name The name of this actor.
	 *  @exception IllegalActionException If the entity cannot be contained
	 *   by the proposed container.
	 *  @exception NameDuplicationException If the container already has an
	 *   actor with this name.
	 */
	public DataGridTransfer( CompositeEntity container, String name )
		throws NameDuplicationException, IllegalActionException 
    {
		super(container, name);

	    source = new TypedIOPort(this, "sourceURL", true, false);
	    source.setTypeEquals(new ArrayType(BaseType.STRING));
	    new Attribute(source, "_showName");

	    fetchedFiles = new TypedIOPort(this, "fetchedFiles", false, true);
	    fetchedFiles.setTypeEquals(new ArrayType(BaseType.STRING));
	    new Attribute(fetchedFiles, "_showName");

	    exitCode = new TypedIOPort(this, "exitCode", false, true);
	    exitCode.setTypeEquals(BaseType.STRING);
	    new Attribute(exitCode, "_showName");

        destination  = new TypedIOPort(this, "destinationDirectoryURL", true, false);
        destination.setTypeEquals(BaseType.STRING);
        new Attribute(destination, "_showName");

//        destinationURLParameter = new Parameter(this, "destinationURLParameter");
//        destinationURLParameter.setTypeEquals(BaseType.STRING);

	    overwriteParameter = new Parameter(this, "overwrite");
        overwriteParameter.setTypeEquals(BaseType.BOOLEAN);
        overwriteParameter.setToken(BooleanToken.FALSE);

	    _attachText("_iconDescription", "<svg>\n"
            + "<rect x=\"-25\" y=\"-20\" "
            + "width=\"50\" height=\"40\" "
            + "style=\"fill:white\"/>\n"
            + "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
            + " 15,-10 15,10, -15,10\" "
            + "style=\"fill:red\"/>\n"
            + "<text x=\"-13.5\" y=\"7\" "
            + "style=\"font-size:14\">\n"
            + "DataGrid \n"
            + "</text>\n"
            + "<text x=\"-12\" y=\"19\""
            + "style=\"font-size:11; fill:black; font-family:SansSerif\">"
            + "Transfer</text>\n"
            + "</svg>\n");

	}

   /** Paths to the files to fetch
    */
	public TypedIOPort source;


    /** Paths to the destination
     */
	public TypedIOPort fetchedFiles;

    /** Exit status of the operation.
     */
	public TypedIOPort exitCode;

    /** Where to put the files on the destination drive.
     */
	public TypedIOPort destination;

    /** Where to put the files on the destination drive.
     */
//	public Parameter destinationURLParameter;

    /** Overwrite when file is copied
     */
	public Parameter overwriteParameter;



	///////////////////////////////////////////////////////////////////
	////                         public methods                    ////
        /** Transfer the file or directory, 
         *  from the source URL to the destination URL. 
         *  @exception IllegalActionException If it is thrown if the
         *  file cannot be accessed.
         */
    public void fire() throws IllegalActionException 
    {
        GeneralFile sourceFile;
        GeneralFile destinationFile;
        String _exitCode = "";
        String destinationURL = null;
        MetaDataCondition conditions[] = {
          MetaDataSet.newCondition( GeneralMetaData.FILE_NAME,
          MetaDataCondition.EQUAL, "fake")
        };
        MetaDataSelect selects[] = {
            MetaDataSet.newSelection( GeneralMetaData.FILE_NAME )
        };  


        try {     
            //TODO not sure why it is done this way
        	if (destination.getWidth() > 0) {  
        	    destinationURL = ((StringToken)destination.get(0)).stringValue();
//        		destinationURLParameter.setExpression(destinationURL);
//        		destinationURL = ((StringToken)destinationURLParameter.getToken()).stringValue();
        	}
        
            //TODO a bit excessive?
            // make sure there is an alive connection. Use the old ones if possible.
        	try {
    		    if (destinationDir == null)
        		    destinationDir = FileFactory.newFile(new URI(destinationURL));
    		    else
        		    destinationDir.query(selects);
        	} catch (Exception ex) { // connection did not exist or was closed.
        	    // try to get a new connection in case the previous one has terminated.
                destinationDir = FileFactory.newFile(new URI(destinationURL));
        	}
        	destinationDir.mkdirs();

        	// Getting the source files list token and copying each file to the destination drive.
        	ArrayToken sourceFilesTokenArr = null;
        	try {
        		sourceFilesTokenArr = (ArrayToken) source.get(0);
        	} catch (Exception ex) {
        		_debug("filesToGet port is null.");
            }
        	if (sourceFilesTokenArr != null) {
        		Token[] sourceFilesToken = sourceFilesTokenArr.arrayValue();
        		Vector fetchedFilesVec = new Vector();
        		
    			String fileStr = ((StringToken) sourceFilesToken[0]).stringValue();
                //TODO a bit excessive?
                // make sure there is an alive connection. Use the old ones if possible.
            	try {
                	if (sourceFileSystem == null)
        		        sourceFileSystem = FileFactory.newFileSystem(new URI(fileStr));                	    
                	else
                		sourceFileSystem.query(conditions, selects, 1);
            	} catch (Exception ex) { // connection did not exist or was closed.
                    // try to get a new connection in case the previous one has terminated.
                    sourceFile = FileFactory.newFile(new URI(fileStr));
                    sourceFileSystem = sourceFile.getFileSystem();
            	}
            	
        		for (int i=0; i<sourceFilesToken.length; i++) 
        		{
        		    // source file path.
        			fileStr = ((StringToken) sourceFilesToken[i]).stringValue();
        			_debug("<FILE_TO_GET>" + fileStr + "<FILE_TO_GET>");

        		    sourceFile = FileFactory.newFile(sourceFileSystem, new URI(fileStr).getPath());
        		    if (sourceFile.exists()) {
	        		    _debug("<LOCAL_FILE_PATH>" + destinationDir + "/" + sourceFile.getName() + "</LOCAL_FILE_PATH>");

	        		    // copying the source file to the destination drive.
                        destinationFile = FileFactory.newFile(destinationDir, sourceFile.getName());
	                    boolean overwrite = ((BooleanToken)overwriteParameter.getToken()).booleanValue();
	                    // Don't ask for confirmation in append mode, since there
	                    // will be no loss of data.
	                    if (destinationFile.exists() && !overwrite) {
		                    // Query for overwrite.
		                    // FIXME: This should be called in the event thread!
		                    // There is a chance of deadlock since it is not.
		                    if (!MessageHandler.yesNoQuestion(
		                                "OK to overwrite " + destinationFile + "?")) {
		                    	throw new IllegalActionException(this,
		                                "Please select another file name.");
		                    }
	                    }
	                	try {
	                	    //overwrite should be checked above
	                		sourceFile.copyTo( destinationFile, true );
	                		fetchedFilesVec.add(new StringToken(destinationFile.getAbsolutePath()));
	                	} catch (Exception ex) {
	                		// If the paralel copy fails try to do a stream copy.
	                		System.out.println("Paralel get failed due to " + ex.getMessage());
	                		System.out.println("Trying Stream get.");
	                		try {
                                if (destinationFile.exists()) {
                                    destinationFile.delete();
                                }
                                _streamGet(sourceFile, destinationFile);
	                		    // adding successfully fetched files output path to the fetched files array.
	                    		fetchedFilesVec.add(new StringToken(destinationFile.getAbsolutePath()));

	                		} catch (Exception stex) {
                                              stex.printStackTrace();
	                    		// even if there is an execption output the successfully fetched files.
	                			System.out.println("failed to copy file " + fileStr +
	                					" to " + destinationDir + "/" + sourceFile.getName() + ".");
	                			_exitCode += "unable to fetch file " + fileStr;
	                			_exitCode += " to " + destinationDir + "/" + sourceFile.getName() + ".\n";
	                		}
	                	}
	        		} else {
	        			System.out.println("file " + fileStr + " does not exist.");
	        			_exitCode += "file " + fileStr + " does not exist.\n";
	        		}
        		}

        		Token[] fetchedFilesArr = new StringToken[fetchedFilesVec.size()];
        		fetchedFilesVec.toArray(fetchedFilesArr);
        		// broadcast the array only if it's non-empty
        		if (fetchedFilesArr.length > 0) {
        			fetchedFiles.broadcast(new ArrayToken(fetchedFilesArr));
        		} else {
        			_exitCode = "no files were fetched.";
        		}
        		if (_exitCode.equals("")) {
        			_exitCode = "success";
        		}
        		exitCode.broadcast(new StringToken(_exitCode));
        	} else {
        		// no more files to get.
        		_refire = false;
        	}
        } catch (Exception ex) {
        	ex.printStackTrace();
        	sourceFile = null;
//        	fileSystem = SRBUtil.closeConnection(fileSystem);
        	throw new IllegalActionException(this, ex.getMessage());
        }

    }

        /** Initialize the source file system to null.
        */
        public void initialize() throws IllegalActionException {
            super.initialize();
            sourceFileSystem = null;
            destinationDir = null;        
        }

	/** Post fire the actor. Return false to indicated that the
	 *  process has finished. If it returns true, the process will
	 *  continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {
		if (_refire) {
			return super.postfire();
		} else return _refire;
	}

	/** Reset the _refire variable and disconnect
	 */
	public void wrapup() {
		_refire = true;
//		fileSystem = SRBUtil.closeConnection(fileSystem);
	}

	///////////////////////////////////////////////////////////////////
	////                         private methods                   ////

	/** Stream read the file. Use in case the parallel get fails.
	 */
	private void _streamGet(GeneralFile sourceFile, GeneralFile destinationFile) throws Exception {

		GeneralRandomAccessFile randomAccessFile= null;
		byte[] bytesRead = new byte[20000];;
		int nBytesRead;
		OutputStream out = FileFactory.newFileOutputStream( destinationFile );

		randomAccessFile = FileFactory.newRandomAccessFile( sourceFile, "r" );
		nBytesRead = randomAccessFile.read(bytesRead);
		while (nBytesRead > 0) {
			out.write(bytesRead);
			nBytesRead = randomAccessFile.read(bytesRead);
		}
		out.close();
	}

	///////////////////////////////////////////////////////////////////
	////                         private members                   ////

	/** Indicator whether the actor should fire again
	 */
	private boolean _refire = true;

	/** Source file system reference.
	 */
	private GeneralFileSystem sourceFileSystem = null;

	/** Source file system reference.
	 */
	private GeneralFile destinationDir = null;
}