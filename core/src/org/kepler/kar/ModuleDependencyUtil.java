/**
 *  '$RCSfile$'
 *  '$Author: crawl $'
 *  '$Date: 2012-11-26 14:21:34 -0800 (Mon, 26 Nov 2012) $'
 *  '$Revision: 31119 $'
 *
 *  For Details:
 *  http://www.kepler-project.org
 *
 *  Copyright (c) 2010 The Regents of the
 *  University of California. All rights reserved. Permission is hereby granted,
 *  without written agreement and without license or royalty fees, to use, copy,
 *  modify, and distribute this software and its documentation for any purpose,
 *  provided that the above copyright notice and the following two paragraphs
 *  appear in all copies of this software. IN NO EVENT SHALL THE UNIVERSITY OF
 *  CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 *  OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 *  DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY
 *  DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
 *  SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 */
package org.kepler.kar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.OSRegistryTxt;
import org.kepler.build.util.Version;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationManagerException;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.configuration.NamespaceException;


/**
 * @author Aaron Schultz 
 */
public class ModuleDependencyUtil {

	/**
	 * Return a Vector<String> that contains the module dependencies found in
	 * the given formatted String list (of the type created by
	 * generateDependencyString(Vector<String>). This method can be reversed
	 * using generateDependencyString(Vector<String>).
	 * 
	 * @param depStr
	 * @return
	 */
	public static Vector<String> parseDependencyString(String depStr) {
		Vector<String> dependencies = new Vector<String>();
		if (depStr == null || depStr.trim().equals("")) {
			return dependencies;
		}
		StringTokenizer st = new StringTokenizer(depStr, KARFile.DEP_SEPARATOR);
		while (st.hasMoreTokens()) {
			String dep = st.nextToken();
			if (!dependencies.contains(dep)) {
				dependencies.add(dep);
			}
		}
		return dependencies;
	}

	/**
	 * Return a delimited string containing the given list of module
	 * dependencies. This method can be reversed using
	 * parseDependencyString(String).
	 * 
	 * @param dependencies
	 * @return String formatted list of Module dependencies.
	 */
	public static String generateDependencyString(Vector<String> dependencies) {
		String dependencyStr = "";
		for (String dep : dependencies) {
			dependencyStr += dep + KARFile.DEP_SEPARATOR;
		}
		// remove end separator
		if (dependencyStr.endsWith(KARFile.DEP_SEPARATOR)) {
			dependencyStr = dependencyStr.substring(0, dependencyStr.length()
					- KARFile.DEP_SEPARATOR.length());
		}
		return dependencyStr;
	}

	/**
	 * @return currently active modules list as 
	 * delimited string
	 */
	public static String buildModuleDependenciesString() {
		String dependenciesStr = "";
		Vector<String> dependencies = new Vector<String>();
		
		ModuleTree moduleTree = ModuleTree.instance();
		for( Module m : moduleTree ){
			dependencies.add(m.toString());
		}
		for (String dependency : dependencies) {
			dependenciesStr += dependency + KARFile.DEP_SEPARATOR;
		}
		if (dependenciesStr.endsWith(KARFile.DEP_SEPARATOR)) {
			// remove the last semicolon
			dependenciesStr = dependenciesStr.substring(0, dependenciesStr
					.length()
					- KARFile.DEP_SEPARATOR.length());
		}
		return dependenciesStr;
	}
	
	/**
	 * 
	 * @param moduleDependencies
	 * @return
	 */
	public static boolean checkIfModuleDependenciesSatisfied(Vector<String> moduleDependencies) {
		
		//first fetch KAR compliance level
		ConfigurationManager cman = ConfigurationManager.getInstance();
		ConfigurationProperty coreProperty = cman.getProperty(KARFile.KARFILE_CONFIG_PROP_MODULE);
		ConfigurationProperty KARComplianceProp = coreProperty.getProperty(KARFile.KAR_COMPLIANCE_PROPERTY_NAME);
		// user has a core configuration.xml from before KARComplianceProp existed, need to add it
		// if KARPreferencesTab.initializeTab hasn't already done so.
		if (KARComplianceProp == null){
			KARComplianceProp = new ConfigurationProperty(KARFile.KARFILE_CONFIG_PROP_MODULE, KARFile.KAR_COMPLIANCE_PROPERTY_NAME, KARFile.KAR_COMPLIANCE_DEFAULT);
			try {
				coreProperty.addProperty(KARComplianceProp);
			} catch (NamespaceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConfigurationManagerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String KARCompliance = KARComplianceProp.getValue();
		
		
		int moduleDepsSize = moduleDependencies.size();
		int currentModuleListSize = ModuleTree.instance().getModuleList().size();
	
		if (moduleDepsSize == 0){
			return true;
		}
		
		//if strict, and different mod dep lengths, we can just return false
		if (KARCompliance.equals(KARFile.KAR_COMPLIANCE_STRICT) && 
				moduleDepsSize != currentModuleListSize){
			return false;
		}

		
		boolean allDependenciesSatisfied = true;
		for (String dep : moduleDependencies) {
			// NOTE after r26418 ignoring dependencies that are incompatible
			// with OS. See http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5232
			// for suggestions for a better solution.
            if( !OSRegistryTxt.isCompatibleWithCurrentOS(Module.make(dep)) ){
                continue;
            }
			boolean depIsInModuleConfiguration = false;
			for (Module m : ModuleTree.instance()) {
				
				//TODO augment if additional compliance levels added
				if (KARCompliance.equals(KARFile.KAR_COMPLIANCE_STRICT)){
					if (m.toString().trim().equals(dep.trim())) {
						//System.out.println("KARFile areAllModuleDependenciesSatisfied() KARCompliance is:"+KARCompliance +
						//		" "+m.toString().trim() + " equals:"+dep.trim());
						depIsInModuleConfiguration = true;
						break;
					}
				}
				else if (KARCompliance.equals(KARFile.KAR_COMPLIANCE_RELAXED)){
					
					// fix for http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5419.
					// deal with special situation introduced in kepler 2.2: ptolemy-kepler-* > ptolemy-*
					// e.g. we should consider ptolemy-kepler-2.2 newer than ptolemy-8.0. Except if user 
					// is running from trunk, in which case module is called ptolemy, and we assume it's most recent
					if (m.getStemName().trim().contains(Module.PTOLEMY) && Version.stem(dep).contains(Module.PTOLEMY)) {
					
						// this section is for letting trunk users slide, and for letting old unversioned
						// kars be opened.
						try{
							Version moduleVersion = Version.fromVersionString(m.getName());
							Version depVersion = Version.fromVersionString(dep);
							if (!moduleVersion.hasMajor() || !moduleVersion.hasMinor() || !moduleVersion.hasMicro()
									|| !depVersion.hasMajor() || !depVersion.hasMinor() || !depVersion.hasMicro()){
								//module or dep might not have version info, just be ok with this
								//System.out.println("ModuleDependencyUtil.checkIfModuleDependenciesSatisfied KARCompliance is:"+KARCompliance +
								//		" "+m.getName() +" or dep:"+ dep + " does not specify proper version info, will just let this slide - ok.");
								depIsInModuleConfiguration = true;
								break;
							}
						}
						catch(IllegalArgumentException e){
							//module or dep might not have version info, just be ok with this
							//System.out.println("caught exception. ModuleDependencyUtil.checkIfModuleDependenciesSatisfied KARCompliance is:"+KARCompliance +
							//		" "+m.getName() +" or dep:"+ dep + " does not specify proper version info, will just let this slide - ok.");
							depIsInModuleConfiguration = true;
							break;
						}
						
						// this section is for official release users
						if (m.getStemName().trim().contains(Module.PTOLEMY_KEPLER) 
								&& !Version.stem(dep).contains(Module.PTOLEMY_KEPLER)){
							//System.out.println("ModuleDependencyUtil.checkIfModuleDependenciesSatisfied KARCompliance is:"+KARCompliance +
							//		" "+m.getName() + " >= " + dep + " - ok.");
							depIsInModuleConfiguration = true;
							break;
						}
						else if (!m.getStemName().trim().contains(Module.PTOLEMY_KEPLER) 
								&& Version.stem(dep).contains(Module.PTOLEMY_KEPLER)){
							//System.out.println("ModuleDependencyUtil.checkIfModuleDependenciesSatisfied KARCompliance is:"+KARCompliance +
							//		" "+m.getName() + " < "+ dep + " so THIS DEP NOT SATISFIED! - fail.");
							depIsInModuleConfiguration = false;
							break;
						}
					}
					
					
					if (m.getStemName().trim().equals(Version.stem(dep))) {
						try{
							Version moduleVersion = Version.fromVersionString(m.getName());
							Version depVersion = Version.fromVersionString(dep);
							if (moduleVersion.hasMajor() && moduleVersion.hasMinor() && moduleVersion.hasMicro()
									&& depVersion.hasMajor() && depVersion.hasMinor() && depVersion.hasMicro()){
								
								if (moduleVersion.getMajor() > depVersion.getMajor() ||
								(moduleVersion.getMajor() == depVersion.getMajor() 
										&& moduleVersion.getMinor() > depVersion.getMinor()) ||
								(moduleVersion.getMajor() == depVersion.getMajor() 
										&& moduleVersion.getMinor() == depVersion.getMinor() 
										&& moduleVersion.getMicro() > depVersion.getMicro()) ||
								(moduleVersion.getMajor() == depVersion.getMajor() 
										&& moduleVersion.getMinor() == depVersion.getMinor() 
										&& moduleVersion.getMicro() == depVersion.getMicro())){
								
									//System.out.println("KARFile areAllModuleDependenciesSatisfied() KARCompliance is:"+KARCompliance +
									//		" "+m.getName() + " >= " + dep + " - ok.");
									depIsInModuleConfiguration = true;
									break;
								}
								else{
									//System.out.println("ModuleDependencyUtil.checkIfModuleDependenciesSatisfied KARCompliance is:"+KARCompliance +
									//		" "+m.getName() + " < "+ dep + " so THIS DEP NOT SATISFIED! - fail.");
									depIsInModuleConfiguration = false;
									break;
								}
							}
							else{
								//module or dep might not have version info, just be ok with this
								//System.out.println("KARFile areAllModuleDependenciesSatisfied() KARCompliance is:"+KARCompliance +
								//		" "+m.getName() +" or dep:"+ dep + " does not specificy proper version info, will just let this slide - ok.");
								depIsInModuleConfiguration = true;
								break;
							}
						}
						catch(IllegalArgumentException e){
							//module or dep might not have version info, just be ok with this
							//System.out.println("caught exception. KARFile areAllModuleDependenciesSatisfied() KARCompliance is:"+KARCompliance +
							//		" "+m.getName() +" or dep:"+ dep + " does not specificy proper version info, will just let this slide - ok.");
							depIsInModuleConfiguration = true;
							break;
						}
					}
				}
			}
			if (!depIsInModuleConfiguration) {
				// Specified module dependency was not found in the
				// current set of modules
				//System.out.println("KARFile module dependency not found:" + dep);
				allDependenciesSatisfied = false;
			}
		}
		
		//System.out.println("ModuleDependencyUtil.checkIfModuleDependenciesSatisfied. returning allDependenciesSatisfied:"+allDependenciesSatisfied);
		return allDependenciesSatisfied;
	}

	/**
	 * Check if versions of dependencies are all complete, defined as having specified a Major,
	 * Minor, and Micro number.
	 * @param dependencies
	 * @return
	 */
	public static boolean isDependencyVersioningInfoComplete(List<String> dependencies) {
		
		for (String dependency : dependencies) {			
			Version v = null;
			try{
				v = Version.fromVersionString(dependency);
				if (!v.hasMajor() || !v.hasMinor() || !v.hasMicro()){
					//System.out.println("ModuleDependencyUtil isDependencyVersioningInfoComplete dependency:"
					//		+dependency+ " missing full version info. Returning false");
					return false;
				}
			}
			catch(IllegalArgumentException iae){
				//System.out.println("ModuleDependencyUtil isDependencyVersioningInfoComplete dependency:"
				//		+dependency+ " missing full version info. Returning false");
				return false;
			}
		}
		
		//System.out.println("ModuleDependencyUtil.isDependencyVersioningInfoComplete Returning true");
		return true;
	}
	
	/**
	 * Check dependencies against currently running modules to see if they're satisfied.
	 * Return a LinkedHashMap of any unsatisfied dependencies and their versions
	 * @param dependencies
	 * @return
	 */
	public static LinkedHashMap<String, Version> getUnsatisfiedDependencies(List<String> dependencies){
		
		LinkedHashMap<String, Version> unsatisfiedDependencies = new LinkedHashMap<String, Version>();
		ModuleTree moduleTree = ModuleTree.instance();
	
		for (String dependency : dependencies) {	
			Version v = null;
			try{
				v = Version.fromVersionString(dependency);
			}
			catch(IllegalArgumentException iae){
			}
			
			if (!moduleTree.contains(dependency)) {	
				//System.out.println("ModuleDependencyUtil getUnsatisfiedDependencies adding unsat dependency:"+dependency);
				unsatisfiedDependencies.put(dependency, v);
			}
			else{
				//System.out.println("ModuleDependencyUtil getUnsatisfiedDependencies moduleTree.contains("+dependency+") no need to add this as unsat dep");
			}
		}
		
		return unsatisfiedDependencies;
	}
	
}
