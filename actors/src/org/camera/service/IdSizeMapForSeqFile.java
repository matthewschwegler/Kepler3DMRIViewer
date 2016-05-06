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

package org.camera.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
//////////////////////////////////////////////////////////////////////////
////IdSizeMapForSeqFile
/**
* It reads the sequence file in Fasta format for a 
* meta genome sample and outputs the map with id
* and sequence length.
* 
* @author Madhu, SDSC  madhu@sdsc.edu
* @version $Id
*/

public class IdSizeMapForSeqFile extends TypedAtomicActor {
	private static final long serialVersionUID = 1L;
	
	public TypedIOPort inputPort, outputPort;
	public IdSizeMapForSeqFile(CompositeEntity container,  String name)
	throws NameDuplicationException, IllegalActionException  {

		super(container, name);
		inputPort = new TypedIOPort(this, "inputPort", true, false);
		outputPort = new TypedIOPort(this, "outputPort", false, true);

	}
	
	/* (non-Javadoc)
	 * @see ptolemy.actor.AtomicActor#fire()
	 */
	@Override
	public void fire() throws IllegalActionException {
	    
		super.fire();
		
		String  inputP  = null;
	    //In case no port is connected to the paramInputPort
		Map<String, String> map = null;
	    if(inputPort.getWidth() > 0){
	    	Token tkP = inputPort.get(0);
	    	if(tkP != null){
	    		inputP=((StringToken)tkP).stringValue().trim();
	    	}
	    	
	    	map = getInputGenomeFileMap(inputP);
	    }
	    
	    if(map != null && map.size() > 0){
	    	outputPort.send(0, new ObjectToken(map));
	    }
	}
	
	
	/**
	 * @param fName is the meta genome sequence file in Fasta format
	 * @return map for seq id and its length for all the sequences
	 * in the meta genome sample file.
	 */
	private Map<String, String> getInputGenomeFileMap(String fName){
		
		Map<String,String> map = new HashMap<String, String>();
//		String genomeFileName = "C:"+ServiceUtils.FILESEP+"Camera"+ServiceUtils.FILESEP+"Ocean_Alaska.fa";
			
		String valueRead = null;
		String paramName = null;
		String paramNameHolder = null;
		StringBuilder paramValue = new StringBuilder();
		boolean writeToMapCheck = false;
		
		try{
			//replaceFirst method for file Name is added because in case one is using FileParameter
			//then file:/ prefix may be added before the file name 
			//This comment added on 21 November, 2008
			
//			File afile = new File(System.getProperty("user.home") + ServiceUtils.FILESEP + "mapData");
//			if(!afile.exists()){
//				afile.createNewFile();
//			}
//		    BufferedWriter out = new BufferedWriter(new FileWriter(afile));
		  		    
			BufferedReader br = null;
			if(ServiceUtils.OS.startsWith("Windows")){
				br = new BufferedReader(new FileReader(fName.replaceFirst("file:/", "")));
			}else{
				br = new BufferedReader(new FileReader(fName.replaceFirst("file:", "")));
			}
			while((valueRead = br.readLine())!= null){
				if(!ServiceUtils.checkEmptyString(valueRead)){
					String valueReadTrim = valueRead.trim();
					if(valueReadTrim.startsWith(ServiceUtils.GT)){
						paramNameHolder = paramName;
						writeToMapCheck = true; //first time it is true, but paramValue is empty
						String[] fragements = valueReadTrim.split("/");
						paramName = fragements[0].trim().replace(ServiceUtils.GT, ServiceUtils.NOSPACE);
						
					}else{
						paramValue.append(valueReadTrim);
					}
					if(writeToMapCheck == true){
						writeToMapCheck = false;						
						if(!ServiceUtils.checkEmptyString(paramNameHolder)&& !ServiceUtils.checkEmptyString(paramValue.toString())){
							map.put(paramNameHolder, String.valueOf(paramValue.length()));
//							out.write("PARAM NAME: " + paramNameHolder + " VALUE: " + paramValue);
//							out.write(ServiceUtils.LINESEP);
							paramNameHolder = null;
							paramValue = new StringBuilder();
						}
					}
					
				}
						
				
			}
			map.put(paramName, String.valueOf(paramValue.length())); //Last value has to written outside the loop
//			Set<String> keys = map.keySet();
//			for(String myKey: keys){
//				out.write("KEY: " + myKey + " VALUE: " + map.get(myKey));
//				out.write(ServiceUtils.LINESEP);
//			}
//			out.close();
		}catch (IOException ioe) {
			ioe.printStackTrace();
		}catch(Exception e){						
			e.printStackTrace();
		}
//		System.out.println("Inside getInputGenomeFileMap");
//		System.out.println("Genome fragments for input file: " + map.size());		
		
		return map;
	}

}