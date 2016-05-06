/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-05-14 16:47:32 -0700 (Mon, 14 May 2012) $' 
 * '$Revision: 29830 $'
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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.kepler.actor.rest.ServiceUtils;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////CAMERAURLProcessor
/**
* This CAMERA specific URL Process automatically generates the
* drop down based on the URL provided by the user from the StringParameter.
* Choices only become visible after hitting the commit button and close the dialog and reopen
* it by double clicking on the actor.
* 
* Selected value is programmatically saved in the moml file. Inside the
* String Parameter that contains the URL a new property is created
* dropdownValue of type StringParameter and value selected by the user from dropdown.
* User must save the worfklow for changes to come in effect.
* 
* If the value of the StringParameter on the Canvas does not start
* with http:// then actor outputs value contained in the StringParameter.
* 
*@author Madhu, SDSC  madhu@sdsc.edu
*@version $Id
*
*/
public class CAMERAURLProcessor extends LimitedFiringSource{
	private static final long serialVersionUID = 1L;
	
	/* 
	 * URL of the service,it is supplied through String Parameter
	 * on the Canvas
	 */
    public StringParameter urlValue;
    
    public Parameter delimiter;
    
    /* drop down value*/   
    public StringParameter dropDownValue;
    
    public TypedIOPort outputPort;
    
    private String gVal = ServiceUtils.NOSPACE;
    private String selectedValue =  ServiceUtils.NOSPACE;
	private String parDelimiter = null;
	private Map<String, String> keyValueMap = null;
    
    public CAMERAURLProcessor(CompositeEntity container,  String name)
				throws NameDuplicationException, IllegalActionException  {
    	
    	super(container, name);
    	
    	delimiter = new Parameter(this, "delimiter");
		delimiter.setExpression(ServiceUtils.PARAMDELIMITER);
		delimiter.setStringMode(true);
    	
    	urlValue = new StringParameter(this, "urlValue");
    	urlValue.setExpression(""); 
    	urlValue.setStringMode(true);	
    	
    	dropDownValue = new StringParameter(this, "dropDownValue");
    	dropDownValue.setExpression(""); 
    	dropDownValue.setStringMode(true);
    	
    	
    	
    	outputPort = new TypedIOPort(this, "outputPort", false, true);
    	outputPort.setTypeEquals(BaseType.STRING);
    }
    
    
    /* (non-Javadoc)
     * @see ptolemy.actor.AtomicActor#fire()
     */
    @Override 
    public void fire() throws IllegalActionException{
    	super.fire();
    	
    	StringToken tk = (StringToken)urlValue.getToken();
    	String urlVal = tk.stringValue();
    	if(urlVal.startsWith("http://")){
    		if(keyValueMap.size() == 0){
    			outputPort.send(0, new StringToken(dropDownValue.getExpression()));
    		}else if(keyValueMap.containsKey(dropDownValue.getExpression())){
    			String mapValue = dropDownValue.getExpression();
    			if(ServiceUtils.checkEmptyString(mapValue)){
    				outputPort.send(0, new StringToken(dropDownValue.getExpression()));
    			}else{
    				outputPort.send(0, new StringToken(keyValueMap.get(dropDownValue.getExpression())));
    			}
    		}else {
    			outputPort.send(0, new StringToken(dropDownValue.getExpression()));
    		}
    	}else{
 //   		process(ServiceUtils.NOSPACE);//Not sure, if it is needed.
    		outputPort.send(0, new StringToken(urlVal));
    	}
    }
    
    @Override
    public void attributeChanged(Attribute at){
   	
    	StringToken tk = null;
    	if(at == urlValue){
 //   		System.out.println( "++000++");
    		
    		StringBuilder contents = new StringBuilder();
    		String urlVal = null;
    		try{
    			tk = (StringToken)urlValue.getToken();
    		}catch(IllegalActionException iae){
 		
    			System.out.println("ILLEGAL EXCEPTION: " + "CAUGHT");
    			iae.printStackTrace();
    		}
    		urlVal = tk.stringValue();
 //   		System.out.println("URLVALUE: " + urlVal + ": " + "++1111++");
 //   		System.out.println("GVALUE: " + gVal + ": " + "++1111++");
    		
    		if(gVal != ServiceUtils.NOSPACE && !urlVal.equals(gVal)){
    			dropDownValue.removeAllChoices();
        		dropDownValue.setExpression("");
 //      		System.out.println("Inside remove choices: " + "++3333++");
    		}
    		if(urlVal.startsWith("http://") && !urlVal.equals(gVal) ){
    			try {	
//    				System.out.println("URLVALUE: " + urlVal + ": " + "++555++");
    				BufferedReader input = null;									
    				URL urlLocation = new URL(urlVal);
    				input = new BufferedReader(new InputStreamReader(urlLocation.openStream()));
					
    				String line = null; //not declared within while loop	
//    				System.out.println("URLVALUE: " + urlVal + ": " + "++666++");
    				while (( line = input.readLine()) != null){
    					contents.append(line);
    					contents.append(System.getProperty("line.separator"));
    				}
 //   				System.out.println("URLVALUE: " + urlVal+ ": " + "++777++");
    				String[] values = contents.toString().split(ServiceUtils.LINESEP);
    				keyValueMap = new HashMap<String, String>();
    				setParDelimiter();
 //  				dropDownValue.addChoice(values[0]);
    				for(String pVal: values){
    					System.out.println("PVAL: " + pVal);
    					System.out.println("DELIMITER: " + parDelimiter);
    					String[] splitValues = pVal.split(parDelimiter);
    					if(splitValues.length >= 2){
    						keyValueMap.put(splitValues[0], splitValues[1]);
    					}
    					dropDownValue.addChoice(splitValues[0]);
    				}
    				
    			}catch(Exception e){
    				System.err.println("exception is thrown when processing url:" + urlVal);
    				e.printStackTrace();
    			}
    			gVal = urlVal;
    		}else if (!urlVal.startsWith("http://") || ServiceUtils.checkEmptyString(urlVal) ){
 //   			  System.out.println("URLVALUE: " + urlVal + ": " + "++4444++");
    			  gVal = urlVal;			    		
    		}
  		      	
    	}else if(at == dropDownValue){
    		process(dropDownValue.getExpression(), parDelimiter);   		
    	}else {
    		try{
    			super.attributeChanged(at);
    		}catch(IllegalActionException ioe){
    			ioe.printStackTrace();
    			System.out.println("PROBLEM WITH SUPER.ATTRIBUTECHANGED");
    		}
    	}
    	
    	
    }
    /**
     * 
     * @throws IllegalActionException
     */
    private void setParDelimiter() throws IllegalActionException {

		String tmp = ((StringToken) delimiter.getToken()).stringValue().trim();

		if (!ServiceUtils.checkEmptyString(tmp)) {

			parDelimiter = tmp;
		} else {
			parDelimiter = ServiceUtils.PARAMDELIMITER;
		}
	}
    /**
     * This method saves the selected value in the moml file.
     * It creates the property dropdownValue id it does not exist and
     * sets the value. If property already exists then value is simply 
     * updated if user makes any changes.
     * 
     * @param param it is the value selected made by the user.
     */
    private void process(String param, String delimiterValue){
    	if(ServiceUtils.checkEmptyString(urlValue.getExpression().trim())){
    		return;
    	}
 //   	System.out.println("SUBSTRING: " + urlValue.getExpression().trim().substring(1));
		StringParameter a = (StringParameter)getContainer().getAttribute(urlValue.getExpression().substring(1));
		StringParameter SelectedValue = (StringParameter)a.getAttribute("SelectedValue");
		StringParameter DelimiterValue = (StringParameter)a.getAttribute("DelimiterValue");
		if(SelectedValue== null){
			try{
//				System.out.println("I am in if block");
				SelectedValue = new StringParameter(a,"SelectedValue");
				SelectedValue.setExpression(param);
				DelimiterValue = new StringParameter(a,"DelimiterValue");
				DelimiterValue.setExpression(delimiterValue);
			}catch(NameDuplicationException nde){
				System.out.println("NAME DUPLICATION EXCEPTION CAUGHT");
				nde.printStackTrace();
			}catch(IllegalActionException iae){
				System.out.println("CAUGHT ILLEGAL EXCEPTION");
				iae.printStackTrace();
			}
		}else{			
				SelectedValue.setExpression(dropDownValue.getExpression());
				DelimiterValue.setExpression(parDelimiter);
		}	
    }
	
}