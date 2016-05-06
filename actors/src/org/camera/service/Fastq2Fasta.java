/* An actor that converts fastq format file to fasta format.

 Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.camera.service;

import static org.biojavax.bio.seq.RichSequence.Tools.createRichSequence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.BioException;
import org.biojava.bio.program.fastq.Fastq;
import org.biojava.bio.program.fastq.FastqReader;
import org.biojava.bio.program.fastq.IlluminaFastqReader;
import org.biojava.bio.program.fastq.SangerFastqReader;
import org.biojava.bio.program.fastq.SolexaFastqReader;
import org.biojava.bio.seq.DNATools;
import org.biojavax.SimpleNamespace;
import org.biojavax.bio.seq.RichSequence;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;


/**
 * This actor converts fastq file to fasta format. It can take three
 * different types of fastq files (Sanger, Solexa, and Illumina). However,
 * user must defined the correct type of file else by default the 'Sanger'
 * is selected from dropdown list.
 * Actor can take regular fastq as well as gzip file for input,
 * but the gzip file must have .gz extension. User should define the output
 * file path else the output will be written to the user home directory with
 * the same file prefix as input. User can define the name space if there
 * is need to make modification to the sequence description else leave it
 * empty.
 * 
 * Actor has four input ports and two output ports. The message output port
 * provides the general information about the input/output file. The status
 * output port outputs true/false if the file processed correctly then 'true'
 * will be emitted from the status port.
 * 
 * Caution: Since biojava keeps the input file in memory, it is likely that
 * this tool might break with very large files. It will help to gzip the fastq
 * file to begin with.
 * 
 * @author madhu
 * @version $Id: Fastq2Fasta.java 31113 2012-11-26 22:19:36Z crawl $
 *
 */
public class Fastq2Fasta extends TypedAtomicActor {
	
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_BUFFER_SIZE = 5096;
	private static Log log = LogFactory.getLog(Fastq2Fasta.class);
	
	/* drop down value*/   	
	private static final String SANGER = "Sanger";
	private static final String SOLEXA = "Solexa";
	private static final String ILLUMINA = "Illumina";
	
	/** 
	 * Full path to the input file.
     */
	public TypedIOPort inputFilePath;
	
	/** 
	 * Full path to output file. If this field is left empty 
	 * then the output will be written to the home directory.
     */
	public TypedIOPort outputFilePath;
	
	/** Name space is a string that is added to the sequence description.*/
	public PortParameter nameSpace;
	
	/** Input sequence data type */
	public TypedIOPort inputPortType;
	
	/** Output status, it is boolean. */
	public TypedIOPort outputPortStatus;
	
	/** Output message */
	public TypedIOPort outputPortMessage;
	
    public Fastq2Fasta(CompositeEntity container,  String name) 
	throws NameDuplicationException, IllegalActionException  {
	
    	super(container, name);

    	inputFilePath = new TypedIOPort(this, "inputFilePath", true, false);
    	outputFilePath = new TypedIOPort(this, "outputFilePath", true, false);
    	inputPortType = new TypedIOPort(this, "inputPortType", true, false);		
    	outputPortMessage = new TypedIOPort(this, "outputPortMessage", false, true);
    	outputPortStatus = new TypedIOPort(this, "outputPortStatus", false, true);
	
    	inputFilePath.setTypeEquals(BaseType.STRING);
    	outputFilePath.setTypeEquals(BaseType.STRING);
    	inputPortType.setTypeEquals(BaseType.STRING);
    	outputPortStatus.setTypeEquals(BaseType.BOOLEAN);
    	outputPortMessage.setTypeEquals(BaseType.STRING);
	
    	inputFileParameter = new FileParameter(this, "inputFile");
    	outputFileParameter = new FileParameter(this, "outputFile");
	
    	dropDownValue = new StringParameter(this, "dropDownValue");
    	dropDownValue.addChoice(ILLUMINA); 
    	dropDownValue.addChoice(SANGER);
    	dropDownValue.addChoice(SOLEXA); 
	
    	nameSpace = new PortParameter(this, "nameSpace");
    	nameSpace.setExpression("");
    	nameSpace.setStringMode(true);
	
    	_attachText("_iconDescription", "<svg>\n"
            + "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
            + "style=\"fill:white\"/>\n"
            + "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
            + " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
            + "</svg>\n");
    }

    /** Drop down values for choices to be selected. 
	 *  If user does not make a selection then 
	 *  default value of 'DNA' is used.
	 */
    public StringParameter dropDownValue;
   

	/** The file's full path.
     *  @see FileParameter
     */
    public FileParameter inputFileParameter;
    
    /** The output file's full path.
     *  @see FileParameter
     */
    public FileParameter outputFileParameter;
    
	/**
	 * 
	 */
    public void fire() throws IllegalActionException {
    	super.fire();
    	
    	boolean monitor = false;
         
    	String inFilePath = null;
    	String outFilePath = null;
    	String type = null;
    	String nmSpace = nameSpace.getExpression().trim();
    	StringBuilder message = new StringBuilder();
    	
    	if (inputFilePath.isOutsideConnected()) {
            if (inputFilePath.hasToken(0)) {
                String name = ((StringToken) inputFilePath.get(0))
                        .stringValue();

                // Using setExpression() rather than setToken() allows
                // the string to refer to variables defined in the
                // scope of this actor.
                inputFileParameter.setExpression(name);
                
            }
        }
        inFilePath = inputFileParameter.getExpression();
        
        if (outputFilePath.isOutsideConnected()) {
            if (outputFilePath.hasToken(0)) {
                String name = ((StringToken) outputFilePath.get(0))
                        .stringValue();

                // Using setExpression() rather than setToken() allows
                // the string to refer to variables defined in the
                // scope of this actor.
                outputFileParameter.setExpression(name);
                
            }
        }
        outFilePath = outputFileParameter.getExpression();
        
        if (inputPortType.isOutsideConnected()) {
        	if (inputPortType.hasToken(0)) {
                type = ((StringToken) inputPortType.get(0)).stringValue();
                if(type.equalsIgnoreCase(SANGER)){
                	dropDownValue.setExpression(SANGER);
                }else if(type.equalsIgnoreCase(ILLUMINA)){
                	dropDownValue.setExpression(ILLUMINA);
                }else if(type.equalsIgnoreCase(SOLEXA)){
                	dropDownValue.setExpression(SOLEXA);
                }
        	}
        }
        
        if(ServiceUtils.checkEmptyString(dropDownValue.getExpression())){
        	dropDownValue.setExpression(SANGER);
        	message.append("NO SELECTION MADE FOR TYPE: DEFAULT VALUE IS SET TO SANGER");
        }else{
        	message.append("SELECTION MADE BY USER FOR TYPE: ").append(dropDownValue.getExpression());
        }
        message.append(ServiceUtils.LINESEP);       
        
        type = dropDownValue.getExpression();
        InputStream insrdr = null;
        try{
        
        	if(inFilePath.endsWith(".gz")){
        		insrdr = new GZIPInputStream(new FileInputStream(inFilePath),DEFAULT_BUFFER_SIZE);
        	}else{
        		insrdr = new FileInputStream(inFilePath);
        	}
        	message.append("Input file: " + inFilePath + ServiceUtils.LINESEP);
       
        	FastqReader qReader = null;
	    
        	if(type.equals("Illumuna")){
        		qReader = new IlluminaFastqReader();
        	}else if(type.equals("Solexa")){
        		qReader = new SolexaFastqReader();
        	}else{
        		qReader = new SangerFastqReader();
        	}
	    
        	if(ServiceUtils.checkEmptyString(outFilePath)){
        		String homeDir = System.getProperty("user.home");
        		String fName = inFilePath.replaceAll("^.*" + File.separator, "").split("[.]")[0];
        		outFilePath = homeDir + File.separator + fName + ".fasta";
        	}
        	message.append("Output file: " + outFilePath + ServiceUtils.LINESEP);
        	
        	FileOutputStream outputFasta = new FileOutputStream(outFilePath);

        	int numOfSequences = 0;
        	for (Fastq fastq : qReader.read(insrdr)) {
        		numOfSequences++;
        		String sequence = fastq.getSequence();
        		SimpleNamespace ns = new SimpleNamespace(nmSpace);
        		RichSequence richSequence = createRichSequence(ns, fastq.getDescription(), sequence, DNATools.getDNA());
        		RichSequence.IOTools.writeFasta(outputFasta, richSequence, ns);
        	}
        	message.append("Total # of sequences in a file: " + numOfSequences + ServiceUtils.LINESEP);
        	monitor = true;
        }catch(FileNotFoundException fnfe){
        	fnfe.printStackTrace();
        	log.debug("Input file does not exist.\n");
        	//message.append("Input file does not exist.\n");
			throw new IllegalArgumentException("Input file does not exist!");
		}catch(IOException ioe){
			ioe.printStackTrace();
			throw new IllegalArgumentException("IOException thrown!");
		}catch(BioException bioe){
			bioe.printStackTrace();
			throw new IllegalArgumentException("BioException thrown!");
		}
	    System.out.println("DONE");
	    
	    outputPortMessage.send(0, new StringToken(String.valueOf(message.toString())));
        outputPortStatus.send(0, new BooleanToken(monitor));
        
        log.debug("MESSAGE: " + message.toString());
        message = null;
    }	

}
