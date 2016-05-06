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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////AverageGenomeSizeParserV2
/**
 * Calculates the Average Genome Size for a meta genome sample.
 * The dependencies for the calculation are e value cutoff
 * and the sample database of phages.
 * 
 * @author Madhu, SDSC  madhu@sdsc.edu
 * @version $Id
 */
public class AverageGenomeSizeParserV2 extends TypedAtomicActor {
	private static final long serialVersionUID = 1L;
	private static final double LOWEST_EVALUE = 1.0E-99;
	
	/** Input port for the File map */
	public TypedIOPort inFileMap;
	
	/** Outputs the average genome size */
	public TypedIOPort outAverageGenomeSize;
	
	/** Input port with ECutOff limit */
	public TypedIOPort	inputECutOffPort;
	
	/** Input port with Blast Results */
	public TypedIOPort inBlastResultsPort;
	
	/** Input port for Database */
	public TypedIOPort inDatabasePort;
	
	public AverageGenomeSizeParserV2(CompositeEntity container,  String name)
	throws NameDuplicationException, IllegalActionException  {

		super(container, name);
		inFileMap = new TypedIOPort(this, "inFileMap", true, false);
		
		outAverageGenomeSize = new TypedIOPort(this, "outAverageGenomeSize", false, true);
		outAverageGenomeSize.setTypeEquals(BaseType.STRING);
		inputECutOffPort =  new TypedIOPort(this, "inputECutOffPort", true, false);
		inBlastResultsPort =  new TypedIOPort(this, "inBlastResultsPort", true, false);
		inDatabasePort =  new TypedIOPort(this, "inDatabasePort", true, false);
	}
	
	/* (non-Javadoc)
	 * @see ptolemy.actor.AtomicActor#fire()
	 */
	@Override
	public void fire() throws IllegalActionException {	    
		super.fire();
		Double genomeSize = 0.0;
		Map<String, String> seqFileMap = null;
//		Map<String,List<NameValuePair>> blastWtsMap = null;
		Map<String,List<NameValuePair>> IDeValuesMap = null;
		Map<String, String> databaseMap = null; 
		
		String  inputP  = null;
		String eCutOff = null;
		double eCutOffValue = 1.0E-5;
		String dbLocation = null;
	
		if(inputECutOffPort.getWidth() > 0){
	    	Token tkE = inputECutOffPort.get(0);
	    	if(tkE != null){
	    		eCutOff=((StringToken)tkE).stringValue().trim();
	    	}	    	
	    	eCutOffValue = Double.parseDouble(eCutOff.toUpperCase());
	    	System.out.println("CutOffEValue: " + eCutOffValue);
	    }
		
		if(inDatabasePort.getWidth() > 0){
	    	Token dBase = inDatabasePort.get(0);
	    	if(dBase != null){
	    		dbLocation=((StringToken)dBase).stringValue().trim();
	    	}	    	
	    	System.out.println("DatabaseLocation: " + dbLocation);
	    }
		
		databaseMap = getDatabaseMap(dbLocation);
		
		if(inBlastResultsPort.getWidth() > 0){
	    	Token tkP = inBlastResultsPort.get(0);
	    	if(tkP != null){
	    		inputP=((StringToken)tkP).stringValue().trim();
	    	}
	    	
	    	IDeValuesMap = getGenomeIDeValuesMap(inputP, eCutOffValue);
	    	for(String key : IDeValuesMap.keySet()){
	    		double ewtTotal = 0.0;
	    		List<NameValuePair> pairList = IDeValuesMap.get(key);
	    		for(NameValuePair pair: pairList){
	    			double wt =  (1.0/Double.parseDouble(pair.getValue()));
	    			System.out.println("WT for each one is: " + wt);
	    			ewtTotal = ewtTotal + wt;
	    		}
	    		   		
	    		for(NameValuePair pair: pairList){
	    			double wt =  (1.0/Double.parseDouble(pair.getValue()));
	    			double abundance = wt / ewtTotal;
	    			System.out.println("Abundance in ComputeEvalWeights: " + abundance);
	    			pair.setValue(String.valueOf(abundance));
	    		}
		    }
	    }
		
		
		Map<String, Double> compoMap = new HashMap<String, Double>();
		
		Token tkFMap = inFileMap.get(0);
	
    	if(tkFMap != null){
    		seqFileMap= (Map<String, String>)((ObjectToken)tkFMap).getValue();
    	}
    	
    	double grandTotal = 0.0;
    	
    	for(String key : IDeValuesMap.keySet()){
    		List<NameValuePair> pairList = IDeValuesMap.get(key);
    		int querySize = 0;
    		String seqSize = null;
    		if((seqSize = seqFileMap.get(key)) != null){
    			querySize = Integer.parseInt(seqSize);
    		}
    		
    		for(NameValuePair pair: pairList){
    			
    			double abundance = Double.parseDouble(pair.getValue());
    			int targetSize = 0;
    			String genSize = null;
    			if((genSize = databaseMap.get(pair.getName())) != null){
        			targetSize = Integer.parseInt(genSize);
        		}
    			if(querySize > 0 && targetSize > 0){
    				abundance = abundance /(querySize * targetSize);
    				System.out.println("Abundance after division: " + abundance);
    				grandTotal = grandTotal + abundance;
    				if(compoMap.containsKey(pair.getName())){
    					double tmp = compoMap.get(pair.getName());
    					compoMap.put(pair.getName(), abundance + tmp);
    				}else{
    					compoMap.put(pair.getName(), abundance);
    				}
    			}
    			
    		}
    	}
    	
    	System.out.println("Grand Total is: " + grandTotal);
    	System.out.println("Element is CompoMap: " + compoMap.size());
    	double size = 0.0;
    	for(String key: compoMap.keySet()){
    		double tmp = compoMap.get(key);
    		tmp = tmp / grandTotal;
    		System.out.println("TMP Values: " + tmp);
    		
    		int val = Integer.parseInt(databaseMap.get(key));
    		size = size + (tmp * val);
    		
    		compoMap.put(key, tmp );   		
    		
    	}
    	genomeSize = size;
    	
    	int decimalPlace = 1;
    	BigDecimal bd = new BigDecimal( Double.toString(genomeSize) );
    	bd = bd.setScale( decimalPlace, BigDecimal.ROUND_HALF_UP );
    	System.out.println( "Up to first decimal size is: " + bd.doubleValue());

    	System.out.println("Size is: " + size);
    	System.out.println("Element in CompoMap: " + compoMap.size());
 //   	outAverageGenomeSize.send(0, new StringToken("GenomeSize: " + genomeSize + " bp"));
 //   	outAverageGenomeSize.send(0, new StringToken(String.valueOf(genomeSize)));
    	outAverageGenomeSize.send(0, new StringToken(String.valueOf(bd.doubleValue())));
	}
	
	/**
	 * @param databaseLocation is the location of the database file.
	 * 
	 * @return Map<String, String> for the Genomes database
	 *  key in the map consists of genome symbol and value consists of genome length.
	 */
	private Map<String, String> getDatabaseMap(String databaseLocation){
		
		Map<String,String> map = new HashMap<String, String>();
//		String genomeDBName = "C:"+ServiceUtils.FILESEP+"Camera"+ServiceUtils.FILESEP+"all_viruses_nt.fa";
			
		String valueRead = null;
		String paramName = null;
		String paramValue = null;
		String paramNamePlaceHolder = null;
		StringBuilder genome = null;
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(databaseLocation));
			while((valueRead = br.readLine())!= null){
				if(!ServiceUtils.checkEmptyString(valueRead)){
					String valueReadTrim = valueRead.trim();
					if(valueReadTrim.startsWith(">")){
						int start = valueReadTrim.indexOf(">");
						int end = valueReadTrim.indexOf(ServiceUtils.ANEMPTYSPACE);
						
						if(end < 0){
							end = valueReadTrim.length();
						}
						
//						System.out.print("Start: "+ start + "  End: " + end);
						paramName = valueReadTrim.substring(start + 1, end);
//						System.out.println(ServiceUtils.TAB + paramName);	
						if(genome != null){
							
							paramValue = String.valueOf(genome.toString().length());
							map.put(paramNamePlaceHolder, paramValue);
							
							if(genome.toString().length() > 200000){
								System.out.println("More than 200000 bases " + paramNamePlaceHolder + ServiceUtils.TAB + paramValue);
							}
						}
						genome = new StringBuilder();
					}else{
						paramNamePlaceHolder = paramName;
						genome.append(valueReadTrim);
					
					}
				}
			}
			
			paramValue = String.valueOf(genome.toString().length());
			map.put(paramNamePlaceHolder, paramValue);
//			System.out.println(paramNamePlaceHolder + ServiceUtils.TAB + paramValue);
//			System.out.println("# of genomes in file: " + map.size());
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return map;
	}
	
	/**
	 * It reads the blast results and e value cut off provided
	 * by user.
	 * @param blastResults
	 * @param cutOff E-value cut off
	 * @return Map consisting of sample ID as key and value is
	 * a List consisting of genome name and e value. We have a list 
	 * because a particular sequence from sample can get multiple hits
	 * against a genome or genomes.
	 */
	private Map<String,List<NameValuePair>> getGenomeIDeValuesMap(String blastResults, double cutOff){
		String[] blastOutputArray = blastResults.split(ServiceUtils.LINESEP);
		Map<String,List<NameValuePair>> map = new HashMap<String, List<NameValuePair>>();
		
    	for(String lineRead: blastOutputArray){	
    		if(!(ServiceUtils.checkEmptyString(lineRead))){
    			
    			String[] strArray = lineRead.trim().split(ServiceUtils.TAB);
    			System.out.println(strArray[0] + ServiceUtils.TAB + strArray[1] + ServiceUtils.TAB + strArray[10]);
    				
    			if(strArray.length >= 10 && Double.parseDouble(strArray[10].toUpperCase()) <= cutOff ){
    				Double testingEValue = Double.parseDouble(strArray[10].toUpperCase());
    				if (testingEValue < LOWEST_EVALUE){
    					testingEValue = LOWEST_EVALUE;
    				}
    				if(map.containsKey(strArray[0])){
    					List<NameValuePair> p = map.get(strArray[0]);
    					p.add(getNMVLPair(strArray[1], String.valueOf(testingEValue)));
    				}else{
    					List<NameValuePair> list = new ArrayList<NameValuePair>();
    					list.add(getNMVLPair(strArray[1], String.valueOf(testingEValue)));
    					map.put(strArray[0], list);
    				}
    			}
    		}
    	}
    	
    	
    	for(String test :map.keySet()){
    		System.out.println("Key: " + test);
    		for(NameValuePair nmvlPair: map.get(test)){
    			System.out.println("Name " + nmvlPair.getName() + " Value: " + nmvlPair.getValue());
    		}
    	}
    	
    	return map;
	
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 * @return NameValuePair object
	 */
	NameValuePair getNMVLPair(String name, String value){
		return new NameValuePair(name, value);
	}


}