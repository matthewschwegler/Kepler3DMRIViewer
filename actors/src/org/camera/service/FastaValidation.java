/* An actor that validates fasta file.

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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.io.ParseException;
import org.biojavax.Namespace;
import org.biojavax.SimpleNamespace;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequenceIterator;

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


//////////////////////////////////////////////////////////////////////////
//// FastaValidation

/**
 *
 * This actor reads a fasta file and user can select whether it
 * contains 'PROTEIN', 'DNA', or 'RNA' sequences. Actor informs user if file
 * is valid or not or there are any parsing issues through a boolean variable.
 * It makes use of biojava library and bytecode jar file.
 * User can provide information how many sequences they need to be parsed.
 * If user does not specify the number or provides a negative, or zero, or provides
 * a string instead then actor will parse the entire file.
 * 
 * Here if there is incorrect file format or there is problem with the first sequence
 * then all the errors are not listed. However, it is an irrelevant matter when 
 * there is a file format problem. However, the two error conditions are coupled in
 * biojava exception.
 * 
 * @author Madhu, SDSC 
 * @version $Id: FastaValidation.java 31113 2012-11-26 22:19:36Z crawl $
 */
public class FastaValidation extends TypedAtomicActor {
    /** Construct an actor with a name and a container.
     *  The container argument must not be null, or a
     *  NullPointerException will be thrown.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the container is incompatible
     *   with this actor.
     *  @exception NameDuplicationException If the name coincides with
     *   an actor already in the container.
     */
	
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(FastaValidation.class);
	
	/* drop down value*/   	
	private static final String PROTEIN = "Protein";
	private static final String RNA = "RNA";
	private static final String DNA = "DNA";
	private static final int MAXNOOFALLOWEDPROBLEMS = 25;
		
    
    /** The  Number of sequence to be parsed, if no positive integer is provided
     * then all the sequences in file will be parsed.
     */
	public PortParameter parseNumSequence;
	
	/** Input file. */
	public TypedIOPort inputPortFilePath;
	
	/** Input sequence type */
	public TypedIOPort inputPortType;
	
	/** Output sequence */
	public TypedIOPort outputPortStatus;
	
	/** Output message */
	public TypedIOPort outputPortMessage;
	
	public FastaValidation(CompositeEntity container,  String name) 
		throws NameDuplicationException, IllegalActionException  {
		
        super(container, name);

        inputPortFilePath = new TypedIOPort(this, "inputPortFilePath", true, false);
        inputPortType = new TypedIOPort(this, "inputPortType", true, false);		
		outputPortMessage = new TypedIOPort(this, "outputPortMessage", false, true);
		outputPortStatus = new TypedIOPort(this, "outputPortStatus", false, true);
		
		inputPortFilePath.setTypeEquals(BaseType.STRING);
		inputPortType.setTypeEquals(BaseType.STRING);
		outputPortStatus.setTypeEquals(BaseType.BOOLEAN);
		outputPortMessage.setTypeEquals(BaseType.STRING);
		
		fileParameter = new FileParameter(this, "fileOrURL");
		
		dropDownValue = new StringParameter(this, "dropDownValue");
    	dropDownValue.addChoice(DNA);
    	dropDownValue.addChoice(RNA); 
    	dropDownValue.addChoice(PROTEIN); 
    	
    	parseNumSequence = new PortParameter(this, "parseNumSequence");
		parseNumSequence.setExpression("");
		parseNumSequence.setStringMode(true);
    	

        _attachText("_iconDescription", "<svg>\n"
                + "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
                + "style=\"fill:white\"/>\n"
                + "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
                + " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
                + "</svg>\n");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    /** The file's full path.
     *  @see FileParameter
     */
    public FileParameter fileParameter;

    /** Drop down values for choices to be selected. 
	 *  If user does not make a selection then 
	 *  default value of 'DNA' is used.
	 */
    public StringParameter dropDownValue;
   

    private String getPortParamValue() throws IllegalActionException {

		return ((StringToken) parseNumSequence.getToken()).stringValue()
					.trim();

	}// end-method getPortParamValue()
    
    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Output the data read from the file or URL as a string.
     *  @exception IllegalActionException If there is no director or
     *   if reading the file triggers an exception.
     */
    public void fire() throws IllegalActionException {
        super.fire();
        parseNumSequence.update();
        
        boolean monitor = true;
        boolean parseAllSequences = true;
        
        String filePath = null;
        StringBuilder message = new StringBuilder();

        // If the fileOrURL input port is connected and has data, then
        // get the file name from there.
               
        
        String numOfSeqBeParsedValue = getPortParamValue();
        int numOfSeqBeParsed = -1;
        if(!ServiceUtils.checkEmptyString(numOfSeqBeParsedValue)){
        	     
        	try{
        		numOfSeqBeParsed = Integer.parseInt(numOfSeqBeParsedValue);
        		if(numOfSeqBeParsed > 0){
        			parseAllSequences = false;
        		}
        	}catch(NumberFormatException nfe){
        		nfe.printStackTrace();
        	}
        	
        }
                                   
		
        if (inputPortFilePath.isOutsideConnected()) {
            if (inputPortFilePath.hasToken(0)) {
                String name = ((StringToken) inputPortFilePath.get(0))
                        .stringValue();

                // Using setExpression() rather than setToken() allows
                // the string to refer to variables defined in the
                // scope of this actor.
                fileParameter.setExpression(name);
                
            }
        }
        filePath = fileParameter.getExpression();       
        
        if(ServiceUtils.checkEmptyString(filePath)){
        	message.append("NO FILE NAME PROVIDED");
        	outputPortStatus.send(0, new BooleanToken(monitor));
        	outputPortMessage.send(0, new StringToken(String.valueOf(message.toString())));
        	
        	log.debug("FILE PATH IS EMPTY");
            message = null;           
        	return;        	
        }
        
        log.debug("FILE NAME: " + filePath);
        
        String type = null;
        if (inputPortType.isOutsideConnected()) {
        	if (inputPortType.hasToken(0)) {
                type = ((StringToken) inputPortType.get(0)).stringValue();
                if(type.equalsIgnoreCase(PROTEIN)){
                	dropDownValue.setExpression(PROTEIN);
                }else if(type.equalsIgnoreCase(DNA)){
                	dropDownValue.setExpression(DNA);
                }else if(type.equalsIgnoreCase(RNA)){
                	dropDownValue.setExpression(RNA);
                }
        	}
        }
        
        if(ServiceUtils.checkEmptyString(dropDownValue.getExpression())){
        	dropDownValue.setExpression(DNA);
        	message.append("NO SELECTION MADE FOR TYPE: DEFAULT VALUE IS SET TO DNA");
        }else{
        	message.append("SELECTION MADE BY USER FOR TYPE: ").append(dropDownValue.getExpression());
        }
        message.append(ServiceUtils.LINESEP);
        
        type = dropDownValue.getExpression();
        
        log.debug("SELECTED VALUE FROM DROP DOWN: " + type);
        
        BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(filePath));
		}catch (FileNotFoundException ex) {
			//problem reading file
			System.out.println("FILE NOT FOUND");
			ex.printStackTrace();
			System.exit(1);
		}
        				
        Namespace nm = new SimpleNamespace("CAMERA");
//get a SequenceDB of all sequences in the file
        RichSequenceIterator db = null;
        if(type.equals(DNA)){
        	db = RichSequence.IOTools.readFastaDNA(br, nm); //readFasta(is, alpha);
        }else if(type.equals(RNA)){
        	db = RichSequence.IOTools.readFastaRNA(br, nm); //readFasta(is, alpha);
        }else if(type.equals(PROTEIN)){
        	db = RichSequence.IOTools.readFastaProtein(br, nm); //readFasta(is, alpha);
        }
        	
        int number_of_sequences = 0;
        
        int icheck = 0;
        int problems = 0;
        List<String> allM = new ArrayList<String>();
        RichSequence rseq = null;
        boolean fileStatus = true;
        
        
        /**
         * Here, I am attempting to read first sequence to check file status
         * before proceeding forward. I have to do this because of
         * clunky behavior or biojava.
         */
        if( db.hasNext() ){
        	try{
        		rseq = db.nextRichSequence();
        		number_of_sequences++;
        	}catch(BioException bioexcep){
        		fileStatus = false;
        		System.out.println("FILE STATUS: " + fileStatus);
        		message.append("INCORRECT FILE FORMAT OR FILE EMPTY OR FIRST SEQ HAS PROBLEM");
        		monitor = false;
        	}
        }
     
       
        while( db.hasNext() && fileStatus ){
        	number_of_sequences++;
        	try{
//        		Sequence seq = db.nextSequence();
        		rseq = db.nextRichSequence();
//        		log.debug("ACCESSION: " + rseq.getAccession()); //NAME, URN, & ACCESSION COULD BE VERY SIMILAR
//        		log.debug("SEQUENCE-NAME: " + rseq.getURN());
//        		log.debug("SEQUENCE-ENGTH: " + rseq.length());
//        		log.debug("SEQUENCE: " + rseq.seqString());
        		
/*        					
        		Annotation seqAn = rseq.getAnnotation();
        		for (Iterator i = seqAn.keys().iterator(); i.hasNext(); ) {
        			Object key = i.next();
        			Object value = seqAn.getProperty(key);
        			log.debug(key.toString() + ": " + value.toString());
        		}
*/        		
        		 
        		//user controls how many sequences need o be parsed.
        		if(!parseAllSequences && number_of_sequences >=  numOfSeqBeParsed){
        			break;
        		}
        		
        	}catch (ParseException parex) {
        		monitor = false;
            	message.append("INCORRECT FILE FORMAT OR FILE NOT PARSEABLE OR PROBLEM WITH SEQUENCES")
            	.append(ServiceUtils.LINESEP);
            	message.append("PROBLEM WITH SEQUENCE #: " + (number_of_sequences + 1));
//            	.append(ex.getMessage());
            	//not in fasta format or wrong alphabet
            	parex.printStackTrace();
            	       	
            }catch (BioException ex) {
//            	message.append(ex.getMessage() + "\n");
            	monitor = false;
//            	message.append("HI");
            	//no fasta sequences in the file
            	ex.printStackTrace();
            	
            	StringWriter writer = new StringWriter();
            	ex.printStackTrace(new PrintWriter(writer));
            	String trace = writer.toString();
            	
    		    
            	// The following line may give impression that the catching
            	// IOException should obviate the need for if block. But do
            	// not waste time trying because it does not work. The
            	// version 1.7.1 of Biojava currently available now is quirky.
            	if(trace.contains("IOException")){
            		++icheck;
            		if(icheck % 2 == 0){
            			++problems;
            			allM.add("Problem at sequence :: " + --number_of_sequences );
            		}    		        	
            	}else{
            		++problems;
            		allM.add("Problem at sequence : " + number_of_sequences );
            	}
            	if(problems > MAXNOOFALLOWEDPROBLEMS){
            		message.append("Number of problems exceeded :" + MAXNOOFALLOWEDPROBLEMS);
            		break;
            	}

            }catch (Throwable throwable) {
            	monitor = false;
            	message.append("IN THROWABLE\n");
    			throwable.printStackTrace();            
            } finally {
                if (fileParameter != null) {
                    fileParameter.close();
                }
            }//end try block
                  	
        }//end while
        
        if(allM.size() > 0){
        	for(String m: allM){
        		message.append(m + "\n");				
        	}
        }  
        
        message.append("Number of sequences parsed from the file: ")
    		.append(String.valueOf(number_of_sequences))
    		.append(ServiceUtils.LINESEP);
        
        outputPortMessage.send(0, new StringToken(String.valueOf(message.toString())));
        outputPortStatus.send(0, new BooleanToken(monitor));
        
        log.debug("MESSAGE: " + message.toString());
        message = null;
	
    }// end-method fire()
}
