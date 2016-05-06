/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.camera.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.ColorAttribute;
import ptolemy.actor.lib.Const;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.vergil.icon.ValueIcon;
import ptolemy.vergil.toolbox.VisibleParameterEditorFactory;

//////////////////////////////////////////////////////////////////////////
////CAMERARESTService
/**
 * This CAMERA specific REST Service automatically generates the
 * ports and parameters upon reading the URL of the csdl file.
 * Choices are available for both Get and Post Services.
 * 
 *@author Madhu, SDSC  madhu@sdsc.edu
 *@version $Id
 */
public class CAMERARESTService extends TypedAtomicActor {
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(CAMERARESTService.class);
	
	private StringBuilder messageBldr = null;
	
	private static final String GET = "Get";
	private static final String POST = "Post";
	
	/** URL of the csdl file */
	public StringParameter paramF;
	 
	/** The REST service URL. */
	public PortParameter serviceSiteURL;
	
	/** old port list. */
	public List oPortList = null;

	/** The REST service method to use, either GET or POST. */
    public Parameter methodType;
    
    /**
     * List of the files. It is needed because file are treated
     * differently.
     */
    private List<String> fileList = new ArrayList<String>();
    
    /** 
     * Old display name for the actor,for associates parameters
     * with the actor, it acts as prefix. 
     */
    private String oldDisplayName = null;
    
    private String ResName = null;
    
    /**
     * This is the place name for the old file name.
     * initial value of this parameter is empty sting.
     * However its vale is updated to the current url
     * at the end of the attributedChanged method.
     */
    private String gFilePath = ServiceUtils.NOSPACE;
    
    /** Output of REST service. */
	public TypedIOPort outputPort;

	public static final int TIMEOUT = 10000;
	public CompositeEntity con;
	
	public CAMERARESTService(CompositeEntity container,  String name)throws NameDuplicationException, IllegalActionException  {
		super(container, name);
		con = container;
		paramF = new StringParameter(this, "Please provide URL");
		
		serviceSiteURL = new PortParameter(this, "serviceSiteURL");
	    serviceSiteURL.setExpression(""); 
	    serviceSiteURL.setStringMode(true);
	    
	    methodType = new Parameter(this, "methodType");
	    methodType.setExpression(""); 
	    methodType.setStringMode(true);
	    methodType.addChoice(GET);
	    methodType.addChoice(POST);
	      
	    outputPort = new TypedIOPort(this, "outputPort", false, true);
		outputPort.setTypeEquals(BaseType.STRING);

	}
	
	/* 
	 * Results from the output service are sent to the 
	 * output port.
	 * 
	 * (non-Javadoc)
	 * @see ptolemy.actor.AtomicActor#fire()
	 */
	@Override
	public void fire() throws IllegalActionException {
		super.fire();
		getContainer().attributeList(org.camera.service.CAMERARESTService.class);
		serviceSiteURL.update();
		
		String ssURL = getServiceSiteURL();
		
		if(ServiceUtils.checkEmptyString(ssURL)){
			outputPort.send(0, new StringToken("Please provide URL"));
			return;
		}else if(!ssURL.startsWith("http://")){
			outputPort.send(0, new StringToken("URL must start with http://"));
			return;
		}
		List<NameValuePair> completeList = getPortValuePairs();
		System.out.println("TOTAL ELEMENTS IN THE COMPLETE LIST: " + completeList.size());
		List<NameValuePair> filePairList = new ArrayList<NameValuePair>();
		for(NameValuePair nmvlPair: completeList){
			if(fileList.contains(nmvlPair.getName())){
				filePairList.add(nmvlPair);
			}
		}
		System.out.println("ELEMENTS IN THE FILE LIST: " + filePairList.size());
		if(filePairList != null && filePairList.size() > 0){
			completeList.removeAll(filePairList);
		}
		System.out.println("TOTAL ELEMENTS IN THE COMPLETE LIST AFTER REMOVAL: " + completeList.size());
		String returnedResults = null;
		if(methodType.getExpression().equals(GET)){
	    	log.debug("CALLING GET METHOD");
	    	try{
	    		returnedResults = executeGetMethod(completeList, ssURL);
	    	}catch(IllegalActionException ilae){
	    		ilae.printStackTrace();
	    		throw ilae;
	    	}
	    }else if(methodType.getExpression().equals(POST)){
	    	try{
	    		returnedResults = executePostMethod(completeList, filePairList, ssURL);
	    	}catch(IllegalActionException ilae){
	    		ilae.printStackTrace();
	    		throw ilae;
	    	}
	    	
	    }else {
	    	returnedResults = "NO WEB SERVICE CALL MADE";
	    	System.out.println("NEITHER GET NOR POST");
	    }
		
		messageBldr = null; // messageBldr is set to null again
		outputPort.send(0, new StringToken(returnedResults));
		
	}
	
	/**
	 * This method returns the appender that will be added to this
	 * actors display name. It gets the appender from the actors name
	 * that is taken care by kepler when multiple instances of the actor
	 * are added on the canvas.
	 *  
	 * 	 */
	private String getAppender(){
		String actorName = getName();
		char c1 = actorName.charAt(actorName.length()-1);
		System.out.println("Character 1: " + Character.toString(c1));
		char c2 = actorName.charAt(actorName.length()-2);
		System.out.println("Character 2: " + Character.toString(c2));
		boolean check2 = false;
		boolean check1 = false;
		StringBuilder tmp = new StringBuilder("");
		if( c2 > 47 && c2 < 58){
			check2 = true;
		}
		if( c1 > 47 && c1 < 58){
			check1 = true;
		}
		if(check1 && check2){
			return  tmp.append(c2).append(c1).toString();
		}
		else if(check1){
			return tmp.append(c1).toString();
		}
		return "";
	}
	
	/* (non-Javadoc)
	 * @see ptolemy.kernel.util.NamedObj#attributeChanged(ptolemy.kernel.util.Attribute)
	 */
	@Override
	public void attributeChanged(Attribute at) throws IllegalActionException {
			
		StringBuilder contents = new StringBuilder();
		String filePath = paramF.getExpression().trim();
		if(at == paramF && !ServiceUtils.checkEmptyString(paramF.getExpression())){

			log.debug("G File path: " + gFilePath + " : " + "FilePath: " + filePath);	
			try {	
				
 				BufferedReader input = null;
 				if(filePath.startsWith("http")){					
 					URL urlLocation = new URL(filePath);
 					input = new BufferedReader(new InputStreamReader(urlLocation.openStream()));
					
 				}

			    String line = null; //not declared within while loop
			       
			    while (( line = input.readLine()) != null){
			    	contents.append(line);
			    	contents.append(System.getProperty("line.separator"));
			    }
			    
			    SAXBuilder builder = new SAXBuilder();
				
				Document doc = builder.build(new StringReader(contents.toString()));
				
				Element elementURL = doc.getRootElement().getChild("resources");
				String serviceURL = elementURL.getAttributeValue("base_uri");
				
				Element elementRES = elementURL.getChild("resource");
				String resourceName = elementRES.getAttributeValue("name");
				ResName = resourceName;
							
				Element elementMethod = elementRES.getChild("method");
				String methodName = elementMethod.getAttributeValue("name");
				
				List<Element> elemParameters = elementMethod.getChild("request").getChildren("parameter");
//				Element elemStr = doc
//						.getRootElement().getChild("allNames").getChild("entity").getChild("NAMESTR");
				
				methodType.setExpression(methodName);
				serviceSiteURL.setExpression(serviceURL);
				
				double [] position = new double[2];
				position[0] = 150.0;
				position[1] = 110.0;
				System.out.println("Elements in the List: " + elemParameters.size());
				
				if(!filePath.equals(gFilePath)){

					ResName = resourceName;
					String appender = getAppender();
					this.setDisplayName(ResName+appender);
					fileList.clear();
					if(!ServiceUtils.checkEmptyString(gFilePath) && !ServiceUtils.checkEmptyString(getOldDisplayName())){
 						remAttributes(getOldDisplayName()+"_");
 					}
					oPortList = this.inputPortList();
					
					for(Element element: elemParameters){
						String tmp = element.getChild("name").getTextTrim();
						String type = element.getChild("type").getTextTrim();
						String descript = element.getChild("descriptive_name").getTextTrim();
//						System.out.println("TMP: " + tmp + ", i= " + i);
						boolean check = checkIfPortNameExists(tmp);
						if(!check){										
							configureActorPort(tmp, descript, type);
						}
//						System.out.println("A: " + tmp);
						
						if(type.equals("file")){
							fileList.add(tmp);
						}
						if(!ServiceUtils.checkEmptyString(filePath)){
//								if(!ServiceUtils.checkEmptyString(gFilePath) || (ServiceUtils.checkEmptyString(gFilePath) && (ServiceUtils.checkEmptyString(tmpParam.getExpression())))){
							position[1] = position[1] +  15.0;
							System.out.println("B: " + descript);	
							configureActorParameter(ResName,appender, descript, type, position);
//							configureActorParameter(ResName+"_"+descript, type, position);
						}						
						
					}

					gFilePath = filePath;
					System.out.println("AFTER G File path: " + gFilePath + " : " + "FilePath: " + filePath);
					removeMyPorts(); // It removes the remaining ports from the old list, which are redundant.
					setOldDisplayName(ResName);
				}
			
				System.out.println("TOTAL NUMBER OF FILES: " + fileList.size());	
				for(String fl: fileList){
					log.debug("File: " + fl);
				}
				
				System.out.println("Total number of elements in the Old p LISt: " + oPortList.size());
				
			}catch(MalformedURLException mfue){
					mfue.getStackTrace();
					throw new IllegalActionException("MalformedURLEXception thrown");
			}catch (IOException ex){
					ex.printStackTrace();
					throw new IllegalActionException("IOException thrown");
			}catch(NameDuplicationException nde){
				nde.printStackTrace();	
				throw new IllegalActionException("NameDuplicationException thrown");
			}catch(Exception e){
				e.printStackTrace();
				throw new IllegalActionException("Exception thrown");			
			}

		}else if(ServiceUtils.checkEmptyString(paramF.getExpression())){
			
			gFilePath = null;
			
			try{
				removePorts();
				if(ResName != null){	
					remAttributes(getOldDisplayName()+"_");
				}
			}catch(NameDuplicationException nde){
				System.out.println("Name is duplicated in....");
				nde.printStackTrace();
				throw new IllegalActionException("NameDuplicationException thrown");				
			}				

		}else{
			super.attributeChanged(at);
		}
		
	}
	/**
	 * It returns the oldDisplayName that is needed to remove
	 * parameters from the canvas in response to the changes
	 * user makes to the url parameter on the actor. 
	 * 
	 * 	 */
	private String getOldDisplayName(){
		return oldDisplayName;
	}
	/**
	 * @param name
	 */
	private void setOldDisplayName(String name){
		oldDisplayName = name;
	}
	
	/**
     * Issue a ChangeRequest to change the output ports.
     * @throws ptolemy.kernel.util.IllegalActionException
     */
    private void reconfigurePorts(String why) {
 //       log.debug("Creating reconfigure ports change request " + why);
        this.requestChange( new ChangeRequest(this,why ) {
            public void _execute() throws Exception {

            }
        });
    }
		
	/**
	 * This method checks if the port with the provided name
	 * exists or not.
	 * @param value Port name
	 * @return true if port with the provided name exists else return false
	 */
	public boolean checkIfPortNameExists(String value){
		boolean test = false;
	
		List pList = oPortList;
		
		if (pList != null && pList.size()> 0){
			System.out.println("Number of elements in the old list: " + pList.size());
			for(int i = 0; i < pList.size(); i++){
				if(((IOPort)pList.get(i)).getName().equals(value)){
					System.out.println("Found the port: " + value);
					oPortList.remove(i);
					test = true;
					break;
				}
			}
		}
		
		return test;
	}
	
	/**
	 * Removes port that were in the old list but not used currently
	 */
	public void removeMyPorts()throws NameDuplicationException, IllegalActionException{
		List inPortList = oPortList;
		Iterator ports = inPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.isInput()) {

				if (!(p.getName().equals("serviceSiteURL"))){
					System.out.println("Port removed: " + p.getName());
					p.setContainer(null);
				}

		    }
		}
	}
	/**
	 * This method removes all input ports
	 */
	public void removePorts() throws NameDuplicationException, IllegalActionException{
		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.isInput()) {
				if (!(p.getName().equals("serviceSiteURL"))){
					System.out.println("Port removed: " + p.getName());
					p.setContainer(null);
		        }
		       
		    }
		}
	}
	
		
	/**
	 * This method creates Parameters with name as value and data type
	 * it represents as valueType. The location of the parameter depends
	 * upon pos.
	 * 
	 * @param value
	 * @param valueType
	 * @param pos
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public void configureActorParameter(String value,  String appender, String description,String valueType, double[] pos)throws IllegalActionException{
//		System.out.println("In method configureActorParameter: " + value);
	
		Parameter param = null;
		try{
			if(valueType.equalsIgnoreCase("file")){
				param = new FileParameter(getContainer().toplevel(), value+appender+"_"+description);
			}else if(valueType.equalsIgnoreCase("string") ||valueType.equalsIgnoreCase("double")){
				param = new StringParameter(getContainer().toplevel(), value+appender+"_"+description);
			}else{
				param = new Parameter(getContainer().toplevel(), value+appender+"_"+description);
			}
			param.setDisplayName(description.replaceAll("_", ServiceUtils.ANEMPTYSPACE)+ServiceUtils.ANEMPTYSPACE+appender);
			Location location = new Location(param, "_location");
			location.setLocation(pos);
		
			ValueIcon vIcon = new ValueIcon(param,"_icon");		
			ColorAttribute colorAttr = new ColorAttribute(vIcon, "_color");
			vIcon.createIcon();
			VisibleParameterEditorFactory vpeFactor = new VisibleParameterEditorFactory(param,"_editorFactory");
		}catch(NameDuplicationException nde){
			
		}

	}

	
	/**
	 * Currently, this method is not used.
	 * @param name
	 * @param value
	 * @param pos
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public void configureActorConstant(String name, String value, double[] pos)throws IllegalActionException, NameDuplicationException{
		System.out.println("Constant: " + value);
		Const param = new Const(con, name);
		param.setDisplayName(value);
		Location location = new Location(param, "_location");
		location.setLocation(pos);
		
	}
	
	/**
	 * It dynamically creates port with name as value and display
	 * name as description and port type is set as valueType.
	 *
	 * @param value
	 * @param description
	 * @param valueType
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public void configureActorPort(String value,String description, String valueType)throws IllegalActionException, NameDuplicationException{
//		System.out.println("Port: " + value);
		TypedIOPort port = new TypedIOPort(this, value, true, false);		
		_setPortType(port, valueType);
		port.setDisplayName(description);
//		reconfigurePorts("XXX");

//		_addPort(_port);
	}
	
	/**
	 * Parameters whose names start with resName are removed by this method.
	 * 
	 * @param resName 
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public void remAttributes(String resName) throws IllegalActionException, NameDuplicationException{
		
		List paramList = getContainer().attributeList();
		System.out.println("Parameters in the LIST: " + paramList.size());
		List<Attribute> attrList = new ArrayList<Attribute>();
		Iterator attributes = paramList.iterator();
		
		while (attributes.hasNext()) {
			Attribute attr = (Attribute) attributes.next();
//			try{
			if(attr instanceof Parameter && !(attr.getName().equals("_parser"))){
				System.out.println("ATTR PARAM: " + attr.getName());
				if(resName != null && attr.getName().startsWith(resName)){
					attrList.add(attr);
				}
			}else{
				System.out.println("ATTR: " + attr.getName());
			}
//			}catch(Exception e){
//				e.printStackTrace();
//			}
		}
		
		for(Attribute attr: attrList){
			attr.setContainer(null);
		}
	}
	
	/**
	 * It returns the URL provided as a value of <i> serviceSiteURL </i> 
	 * Parameter for RESTService actor as a String.
	 * 
	 * @return ServiceSiteURL as String
	 */
	private String getServiceSiteURL() throws IllegalActionException{
		
		return ((StringToken)serviceSiteURL.getToken()).stringValue().trim();
		
	}
	
	/**
	 * It works only on user defined/configured ports and excludes actor
	 * defined ports.
	 * @return List of Name, Value pairs of port names and values
	 */		
	private List<NameValuePair> getPortValuePairs() throws IllegalActionException{
		
		List<NameValuePair> nvList = new ArrayList<NameValuePair>();
		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();
		
		while (ports.hasNext()) {
          	IOPort p = (IOPort) ports.next();
          	if(!p.getName().equals(serviceSiteURL.getName())){		
          		
          			String pValue = getPortValue(p);
          			
    				
          			if(pValue != null && pValue.trim().length() != 0){
          				if(pValue.startsWith("file:")){
        					String altered = null;
        					if(ServiceUtils.OS.startsWith("Windows")){
        						altered= pValue.replaceFirst("file:/", "");//.replace("/", "\\");
          					}else{
          						altered= pValue.replaceFirst("file:", "");
          					}
        					pValue = altered;
        				}
          				nvList.add(new NameValuePair(p.getName(), pValue));
          				log.debug("NAME: " + p.getName ()+ " VALUE: " + pValue);
          			}
          		
          	}
        }
		
		return nvList;

	}
	
	/**
	 * 
	 * @param iop
	 * @return the value associated with a particular port
	 * @throws IllegalActionException
	 */
	private String getPortValue(IOPort iop) throws IllegalActionException{
		
		String result = null;
		if(iop.getWidth() > 0){
			Token tk = iop.get(0);
			Type tp = tk.getType();
			if(tp == BaseType.INT || tp == BaseType.LONG ||
					tp == BaseType.FLOAT || tp == BaseType.DOUBLE){
				
				result = tk.toString().trim();
				System.out.println("String value of the Token: " + result);
			}else if(tp == BaseType.BOOLEAN){
				if(((BooleanToken)tk).booleanValue()){
					result = String.valueOf(1);
				}else{
					result = String.valueOf(0);
				}
					
			}else{
				result =((StringToken)tk).stringValue().trim();
			}
			if(_debugging){
				_debug("In getPortValue method RESULT: " + result);
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param val
	 * @return the File object for the supplied String
	 */
	private File getFileObject(String val){
		return new File(val);
	}
	
	
	/**
	 * File & regular parameters are passed as two separate lists and they are treated
	 * little differently. If flPairList is not null or empty then this method uses Part
	 * Object else no.
	 * @param pmPairList List of the name and value parameters that user has provided
	 * @param flPairList List of the name and value (full file path)of file parameters.
	 * 			It is essentially a list of files that user wishes to attach.
	 * @return  the results after executing the Post service.
	 */
	public String executePostMethod(List<NameValuePair> pmPairList, List<NameValuePair> flPairList, String serSiteURL) throws IllegalActionException{
		
		if(_debugging){
    		_debug("I AM IN POST METHOD");
		}
				
		log.debug("I AM IN POST METHOD");
    	String postAddress = serSiteURL;

    	HttpClient client = new HttpClient();
    	MultipartPostMethod method = new MultipartPostMethod(postAddress);
		List<NameValuePair> fullNameValueList = pmPairList;		
		if(flPairList != null && flPairList.size() > 0){
			fullNameValueList.addAll(flPairList);
			int totalSize = fullNameValueList.size();
			Part[] parts = new Part[totalSize];
			
			try{
				
				for(int i = 0; i < parts.length; i++){
			
					String nm = fullNameValueList.get(i).getName();
					String vl = fullNameValueList.get(i).getValue();
				
					if(i > totalSize - flPairList.size() - 1){
						System.out.println("FILE NAME: " + nm);
						File file = getFileObject(vl);
						System.out.println("FILE NAME: " + file.getName());
						parts[i] = new FilePart(nm, file);
						method.addPart(parts[i]);
						System.out.println("PARTS: " + i + " " + parts[i].getName());
						System.out.println( "file Name: " + vl);
					
					}else{
						
						System.out.println("PARAMETER NAME: " + nm);
						System.out.println("PARAMETER Value: " + vl);
						parts[i] = new StringPart(nm, vl);
						method.addPart(parts[i]);
						
					}
					if(_debugging){
			    		_debug("Value of i: " + i);
					}
				
				}
				
			
			}catch(FileNotFoundException fnfe){
				if(_debugging){
		    		_debug("File Not Found Exception: " + fnfe.getMessage());
				}
				fnfe.printStackTrace();
				throw new IllegalActionException("File Not Found: " + fnfe.getMessage());
			}
		}else{
			for(NameValuePair nmPair: fullNameValueList){			
				method.addParameter(nmPair.getName(), nmPair.getValue());
				System.out.println("Name: " + nmPair.getName()+ "  Value:" + nmPair.getValue());
			}
		}
		
		InputStream rstream = null;
		StringBuilder results = new StringBuilder();
		try{

			messageBldr = new StringBuilder();
			messageBldr.append("In excutePostMethod, communicating with service: ").
			append(serSiteURL).append("   STATUS Code: ");
			
			int statusCode = client.executeMethod(method);
			messageBldr.append(statusCode);
			
			log.debug("DEBUG: " + messageBldr.toString());
			System.out.println(messageBldr.toString());
			
			rstream = method.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
    	
			log.debug("BEFORE WHILE LOOP");
			String s;
			while( (s = br.readLine())!= null){	    		
				results.append(s).append(ServiceUtils.LINESEP);
			}
			
    	}catch (HttpException httpe) {
    		if(_debugging){
	    		_debug("Fatal protocol violation: " + httpe.getMessage());
			}
		   	httpe.printStackTrace();
//		   	return "Protocol Violation";
		   	throw new IllegalActionException("Fatal protocol violation: " + httpe.getMessage());
		}catch(ConnectException conExp){
			if(_debugging){
	    		_debug("Perhaps service not reachable: " + conExp.getMessage());
			}

			conExp.printStackTrace();
			throw new IllegalActionException("Perhaps service not reachable: " + conExp.getMessage());
//			return "Perhaps service not reachable";
		}catch (IOException ioe) {
			if(_debugging){
	    		_debug("Fatal transport error: " + ioe.getMessage());
			}

		   	ioe.printStackTrace();
//		   	return "IOException: Perhaps could not connect to the service.";
		   	throw new IllegalActionException("Fatal transport error: " + ioe.getMessage());
		}catch (Exception e) {
			if(_debugging){
	    		_debug("Unknown error: " + e.getMessage());
			}
		   	e.printStackTrace();
//		   	return "Exception: Unknown type error";
		   	throw new IllegalActionException("Error: " + e.getMessage());
		}finally {
		          // Release the connection.
	    	method.releaseConnection();
	    }
		return results.toString();
	}
	
	/**
	 *  
	 * @param pmPairList List of the name and value parameters that user has provided
	 * through paramInputPort. However in  method this list is combined with
	 * the user configured ports and the combined list name value pair parameters are
	 * added to the service URL separated by ampersand.
	 * @param nvPairList List of the name and value parameters that user has provided
	 * @return the results after executing the Get service.
	 */
	public String executeGetMethod(List<NameValuePair> nvPairList, String serSiteURL)throws IllegalActionException{
		
		if(_debugging){
    		_debug("I AM IN GET METHOD");
    	}
			
		HttpClient client = new HttpClient();
	    	 
		StringBuilder  results =new StringBuilder();
		results.append(serSiteURL);
		//Files cannot be attached with GET
		List<NameValuePair> fullPairList = nvPairList;// getCombinedPairList(nvPairList);

		if(fullPairList.size() > 0){
			
			results.append("?");
			
			int pairListSize = fullPairList.size();
			for(int j = 0; j < pairListSize; j++){
				NameValuePair nvPair = fullPairList.get(j);
				results.append(nvPair.getName()).append(ServiceUtils.EQUALDELIMITER).append(nvPair.getValue());
				if(j < pairListSize - 1){			
					results.append("&");
				}

			}
		}
		if(_debugging){
    		_debug("RESULTS :"  + results.toString());
		}
 
		// Create a method instance.
		GetMethod method = new GetMethod(results.toString());
		InputStream rstream = null;
		StringBuilder resultsForDisplay = new StringBuilder();		    	    
		    	
		try{
		 	
			messageBldr = new StringBuilder();
			messageBldr.append("In excutePostMethod, communicating with service: ").
			append(serSiteURL).append("   STATUS Code: ");
			
			int statusCode = client.executeMethod(method);
			messageBldr.append(statusCode);
			
			log.debug("DEBUG: " + messageBldr.toString());
			System.out.println(messageBldr.toString());
								
			rstream = method.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(rstream));		    	
			    	
			String s;
			while( (s = br.readLine())!= null){	    		
				resultsForDisplay.append(s).append(ServiceUtils.LINESEP);
			}
						
		}catch (HttpException httpe) {
			if(_debugging){
	    		_debug("Fatal protocol violation: " + httpe.getMessage());
			}
		   	httpe.printStackTrace();
//		   	return "Protocol Violation";
		   	throw new IllegalActionException("Fatal protocol violation: " + httpe.getMessage());
		}catch(ConnectException conExp){
			if(_debugging){
	    		_debug("Perhaps service not reachable: " + conExp.getMessage());
			}
			conExp.printStackTrace();
			throw new IllegalActionException("Perhaps service not reachable: " + conExp.getMessage());
		}catch (IOException ioe) {
			if(_debugging){
	    		_debug("Fatal transport error: " + ioe.getMessage());
			}
		   	ioe.printStackTrace();
//		   	return "IOException: fatal transport error";
		   	throw new IllegalActionException("Fatal transport error: " + ioe.getMessage());
		}catch (Exception e) {
			if(_debugging){
	    		_debug("Unknown error: " + e.getMessage());
			}
		   	e.printStackTrace();
//		   	return "Exception: Unknown type error";
		   	throw new IllegalActionException("Error: " + e.getMessage());
		}finally {
		          // Release the connection.
	    	method.releaseConnection();
	    }
		    
	    return resultsForDisplay.toString();

	}
		
		
	/**
	 * Set the type of a port based on a string representation of that type
	 * that was extracted from the WSDL description.
	 *
	 * @param arrayTypes a hash of defined array types by name
	 * @param port the port whose type is to be set
	 * @param typeStr the string representation of the type to be set
	 */
	private void _setPortType(TypedIOPort port, String typeStr) {
	    
		  if (typeStr.equals("int")) {
			  port.setTypeEquals(BaseType.INT);
		  }
		  else if (typeStr.equals("boolean")) {
			  port.setTypeEquals(BaseType.BOOLEAN);
		  }
		  else if (typeStr.equals("long")) {
			  port.setTypeEquals(BaseType.LONG);
		  }
		  else if (typeStr.equals("double")) {
			  port.setTypeEquals(BaseType.DOUBLE);
		  }
		  else if (typeStr.equals("float")) { //There is no float in Ptolemy type sys.
			  port.setTypeEquals(BaseType.DOUBLE);
		  }
		  else if (typeStr.equals("byte")) {
	      //->There is no byte in Ptolemy type sys. So I cast the byte to INT.
			  port.setTypeEquals(BaseType.INT);
		  }
		  else if (typeStr.equals("short")) {
	      //->There is no short in Ptolemy type sys. So again cast it to INT
			  port.setTypeEquals(BaseType.INT);
		  }
		  else if (typeStr.equals("string")) {
			  port.setTypeEquals(BaseType.STRING);
		  }
		  else if (typeStr.equals("string[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.STRING));
		  }
		  else if (typeStr.equals("byte[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.INT));
		  }
		  else if (typeStr.equals("short[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.INT));
		  }
		  else if (typeStr.equals("int[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.INT));
		  }
		  else if (typeStr.equals("long[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.LONG));
		  }
		  else if (typeStr.equals("double[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.DOUBLE));
		  }
		  else if (typeStr.equals("float[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.DOUBLE));
		  }
		  else if (typeStr.equals("boolean[]")) {
			  port.setTypeEquals(new ArrayType(BaseType.BOOLEAN));
		  }
		  else {
			  _debug(
	          	"<WARNING>Could not specify the type. Setting it to string. </WARNING>");
			  port.setTypeEquals(BaseType.STRING);
		  }
	  }

}