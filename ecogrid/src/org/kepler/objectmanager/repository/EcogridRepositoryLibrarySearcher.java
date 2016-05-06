/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-01-17 19:25:48 -0800 (Thu, 17 Jan 2013) $' 
 * '$Revision: 31348 $'
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

package org.kepler.objectmanager.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.authentication.AuthenticationException;
import org.kepler.gui.LibrarySearchPane;
import org.kepler.gui.LibrarySearchResults;
import org.kepler.gui.LibrarySearcher;
import org.kepler.gui.RepositorySearcher;
import org.kepler.kar.ModuleDependencyUtil;
import org.kepler.kar.karxml.KarXml;
import org.kepler.moml.DownloadableKAREntityLibrary;
import org.kepler.moml.RemoteKARErrorEntityLibrary;
import org.kepler.moml.RemoteRepositoryEntityLibrary;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheNamedObj;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.sms.NamedOntClass;
import org.kepler.sms.OntologyCatalog;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;

/**
 * this abstract class is should be extended by all classes that provide a
 * search engine for the actory library. Any local variables in the extending
 * classes should be initialized in the init method because it is called from
 * the constructor of this class.
 * 
 *@author berkley
 *@since November the ninth one thousand times two plus six
 */
public class EcogridRepositoryLibrarySearcher extends LibrarySearcher implements RepositorySearcher {
	private static final Log log = LogFactory
	.getLog(EcogridRepositoryLibrarySearcher.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// the name of the repository
	private String repositoryName = "defaultRepository"; // the default name.
	// this can get reset in the constructor

	// the repository
	private Repository repository;
	private boolean skipOntology = false;

	/**
	 * constructor
	 * 
	 *@param library
	 *            Description of the Parameter
	 *@param searchPane
	 *            Description of the Parameter
	 */
	public EcogridRepositoryLibrarySearcher(JTree library,
			LibrarySearchPane searchPane, String reposName)
	throws IllegalActionException {
		super(library, searchPane);
		try {
			if (reposName != null) {
				repositoryName = reposName;
			}
			if (isDebugging) {
				log.debug("repositoryName: " + repositoryName);
			}
			repository = RepositoryManager.getInstance().getRepository(
					repositoryName);
			if (isDebugging) {
				log.debug(repository.getClass());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalActionException(
					"Could not get an instance of the "
					+ "repository manager: " + e.getMessage());
		}

	}

	/**
	 * search a component hierarchy for a specific component in a specific
	 * component returns null if the component is not found
	 */
	private CompositeEntity findEntity(String name, CompositeEntity entity) {
		if (entity.getName().trim().equals(name.trim())) {
			return entity;
		} else {
			Iterator pathItt = entity.entityList().iterator();
			while (pathItt.hasNext()) {
				Entity nextEntity = (Entity) pathItt.next();
				if (nextEntity instanceof CompositeEntity) {
					CompositeEntity newEntity = findEntity(name,
							(CompositeEntity) nextEntity);
					if (newEntity != null) {
						return newEntity;
					}
				}
			}
		}
		return null;
	}

	/**
	 * build a path from the ontology tree from a given NamedOntClass
	 */
	private Object[] getTreePathObjectArray(NamedOntClass ontClass,
			Vector<String> v) {
		Iterator<NamedOntClass> itt = ontClass.getNamedSuperClasses(false);
		Object[] o = null;
		if (!itt.hasNext()) {
			v.addElement(ontClass.getName());
			o = new Object[v.size()];
			for (int i = 0; i < v.size(); i++) {
				o[i] = v.elementAt(i);
			}
		} else {
			while (itt.hasNext()) {
				NamedOntClass supClass = itt.next();
				v.addElement(ontClass.getName());
				return getTreePathObjectArray(supClass, v);
			}
		}

		// the array is backwards so it needs to be flipped
		Object[] result = new Object[o.length];
		for (int i = 0; i < o.length; i++) {
			result[(result.length - 1) - i] = o[i];
		}

		return result;
	}

	/**
	 * provides any initialization needed prior to searching. It is called when
	 * the class is constructed. Note that any local variables of extending
	 * classes should be initialized in init since it is called be the
	 * constructor of the super class (LibrarySearcher).
	 */
	@Override
	protected void init() {

	}

	/**
	 * If we need to skip ontology display
	 */
	public void setSkipOntology(boolean skipOntology)
	{
		this.skipOntology = skipOntology;
	}

	/**
	 * search for value in the library
	 * 
	 *@param value
	 *            the value to search for
	 *@return Description of the Return Value
	 * @throws RepositoryException 
	 * @throws AuthenticationException 
	 */
	@Override
	public LibrarySearchResults search(String value, boolean authenticate)
	throws IllegalActionException, RepositoryException, AuthenticationException {

		try {
			_results = new LibrarySearchResults();

			TreePath path = _library.getPathForRow(0); // get the container
			Object[] pathComps = path.getPath();
			EntityLibrary topLevel = (EntityLibrary) pathComps[0];
			EntityLibrary remoteLevel = null;
			try {
				remoteLevel = new EntityLibrary(topLevel, "Remote Components");
			} catch (NameDuplicationException nde) {
				Iterator itt = topLevel.entityList().iterator();
				while (itt.hasNext()) {
					CompositeEntity entity = (CompositeEntity) itt.next();
					if (entity.getName().equals("Remote Components")) {
						remoteLevel = (EntityLibrary) entity;
						remoteLevel.setContainer(null);
						remoteLevel = new EntityLibrary(topLevel,
						"Remote Components");
					}
				}
			}
			// setup the remote components part of the tree
			Object[] o = new Object[2];
			o[0] = topLevel;
			o[1] = remoteLevel;

			_results.add(new TreePath(o));

			// get the results from the repository
			OntologyCatalog ontCatalog = OntologyCatalog.instance();
			// search the repository
			//System.out.println("EcogridRepositoryLibrarySearcher search('" + value + "')");
			Iterator repoResults = repository.search(value, authenticate);
			Iterator<EcogridRepositoryResults> castRepoResultsIterator = (Iterator<EcogridRepositoryResults>) repoResults;
			List<EcogridRepositoryResults> repoResultsList = iteratorToList(castRepoResultsIterator);
			//System.out.println("EcogridRepositoryLibrarySearcher found " + repoResultsList.size() + " results");
			List<List<EcogridRepositoryResults>> groupedResults = groupResultsByIndex(repoResultsList);
			//System.out.println("having " +  groupedResults.size() + " groups");
			//			System.out.println("Matched KARs:");
			//			for (List<EcogridRepositoryResults> resultGroup : groupedResults) {
			//				System.out.println("* " + resultGroup.get(0).getKarEntry().getParent().getId());
			//			}
			if (repoResults != null) {
				EntityLibrary karLevel = null;
				EntityLibrary repositoryLevel = null;
				for (List<EcogridRepositoryResults> repoResultsGroup : groupedResults) {
					//System.out.println("A group has "+repoResultsGroup.size()+" EcogridResult");
					boolean isDone = false;
					for (EcogridRepositoryResults repoResult : repoResultsGroup) {					  
						if(!isDone){
							karLevel = createKarLevel(remoteLevel, repoResult);
							// Get the remote repository level
							repositoryLevel= getRepositoryLevel(remoteLevel, repoResult.getKarEntry().getParent());
							isDone = true;
						}

						// get each result from the repository
						CacheObject co = repoResult.getCacheObject();
						ActorCacheObject aco = (ActorCacheObject) co;
						KarXml.KarEntry karEntry = repoResult.getKarEntry();

						Object leafObject;

						if (co == null) {
							// Not an actor
							String name = repoResult.getName();
							if (name == null) {
								name = "unnamed";
							}

							NondraggableTreeItem nti = new NondraggableTreeItem(name);
							if(karEntry.isWorkflow())
							{
								nti.setWorkflowLSID(karEntry.getLsid());
								nti.setWorkflowName(karEntry.getWorkflowName());
							}
							StringAttribute alternateGetPopupActionAttribute = new StringAttribute(nti, "_alternateGetPopupAction");
							alternateGetPopupActionAttribute.setExpression(RepositoryPopup.class.getName());
							StringAttribute notDraggableAttribute = new StringAttribute(nti, "_notDraggable");
							notDraggableAttribute.setExpression("true"); // Not strictly needed, but makes reading the MOML a little nicer.

							//FIXME hardcode to add-on module
							if ("org.kepler.reporting.roml.ReportLayout".equals(repoResult.getKarEntry().getType())) {
								ConfigurableAttribute thumbnailAttribute = new ConfigurableAttribute(nti, "_thumbnailRasterIcon");
								thumbnailAttribute.setExpression("/actorthumbs/basic-report-sm.gif");
							}
							leafObject = nti;
						} else {
							// An actor
							CacheNamedObj cno = new CacheNamedObj(new CompositeEntity(new Workspace()), co);
							if(karEntry.isWorkflow())
							{
								cno.setWorkflowLSID(karEntry.getLsid());
								cno.setWorkflowName(karEntry.getWorkflowName());
							}
							StringAttribute entityIdAttribute = new StringAttribute(cno, "entityId");
							entityIdAttribute.setExpression(aco.getLSID().toString());
							StringAttribute alternateGetMomlActionAttribute = new StringAttribute(cno, "_alternateGetMomlAction");
							alternateGetMomlActionAttribute.setExpression(AlternateGetMoml.class.getName());
							StringAttribute alternateGetPopupActionAttribute = new StringAttribute(cno, "_alternateGetPopupAction");
							alternateGetPopupActionAttribute.setExpression(RepositoryPopup.class.getName());

							ConfigurableAttribute thumbnailAttribute = new ConfigurableAttribute(cno, "_thumbnailRasterIcon");
							thumbnailAttribute.setExpression("/actorthumbs/basic-actor-sm.gif");

							leafObject = cno;
						}

						// find the semantic types and loop through them to place
						// the results
						if(!skipOntology){
							Vector<String> semTypes = repoResult.getSemanticTypes();
							for (int k = 0; k < semTypes.size(); k++) {
								String semType = (String) semTypes.elementAt(k);
								// find the class, search only the library ontologies
								NamedOntClass ontClass = ontCatalog.getNamedOntClass(
										semType, true);

								if (ontClass == null) { // skip this component if it
									// doesn't have a known class
									continue;
								}

								// get the tree path that goes with this semantic type
								Object[] treePathArray = getTreePathObjectArray(
										ontClass, new Vector());

								// add the tree path to the topLevel and remoteLevel
								// objects to make the fullTreePath
								Object[] fullTreePath = new Object[treePathArray.length + 3];
								fullTreePath[0] = topLevel;
								fullTreePath[1] = remoteLevel;
								for (int i = 2; i < fullTreePath.length - 1; i++) {
									// put the treepath together to be added to the
									// results vector this just adds EntityLibraries
									try {
										// adding a space to the treePathArray content
										// is a hack to keep
										// the tree from stealing results from the
										// remote part and putting
										// them back into the local part. I'm not sure
										// why it does this
										// but keeping the names unique seems to be the
										// only way to
										// prevent results seepage.
										treePathArray[i - 2] = (String) treePathArray[i - 2]
										                                              + " ";
										fullTreePath[i] = new EntityLibrary(
												(CompositeEntity) fullTreePath[i - 1],
												(String) treePathArray[i - 2]);
									} catch(NameDuplicationException nde) {
										// if we get a NDE, we need to search the
										// existing tree for the
										// correct parent
										fullTreePath[i] = findEntity(
												(String) treePathArray[i - 2],
												(CompositeEntity) fullTreePath[i - 1]);
										if (fullTreePath[i] == null) {
											System.out
											.println("ERROR: no path found for fullTreePath["
													+ i + "]");
										}
									}
								}

								fullTreePath[fullTreePath.length - 1] = leafObject;

								_results.add(new TreePath(fullTreePath));
							}
						}


						Object[] containmentRepresentation = getContainmentRepresentation(topLevel, remoteLevel, 
								repositoryLevel, karLevel, leafObject);
						if (containmentRepresentation == null) {
							log.warn("Could not generate containment representation: " + repoResult.getName());							
						}
						else {
							_results.add(new TreePath(containmentRepresentation));
						}
					}
				}
			}

			if (_results.size() == 1) {// no results were added, so remove the
				// Remote Components result tree
				_results = new LibrarySearchResults();
			}

			return _results;
		} catch (NameDuplicationException nde) {
			nde.printStackTrace();
			throw new IllegalActionException(
					"Error building remote repository " + "search results: "
					+ nde.getMessage());
		}
	}



	private Object[] getContainmentRepresentation(EntityLibrary topLevel, EntityLibrary remoteLevel, EntityLibrary repositoryLevel,
			EntityLibrary karLevel, Object leafObject) {

		if (repositoryLevel != null) {
			Object[] newPath = new Object[5];
			newPath[0] = topLevel;	// Copy over "Search Results" top-level
			newPath[1] = remoteLevel;	// Copy over "Remote Components"
			newPath[2] = repositoryLevel;
			newPath[3] = karLevel;
			newPath[4] = leafObject;	// Copy over the component itself
			return newPath;
		}
		else {
			return null;
		}		
	}

	private EntityLibrary getRepositoryLevel(EntityLibrary parent, KarXml karXml) {
		EntityLibrary repositoryLevel = null;
		try {
			repositoryLevel = new RemoteRepositoryEntityLibrary(parent, karXml.getRepositoryName());
		}
		catch(IllegalActionException ex) {
			ex.printStackTrace();
		}
		catch(NameDuplicationException ex) {
			// The containment level is already there. Find it.
			repositoryLevel = (EntityLibrary) findEntity(karXml.getRepositoryName(), (CompositeEntity) parent);
		}
		return repositoryLevel;
	}

	private EntityLibrary createKarLevel(EntityLibrary parentLibrary, EcogridRepositoryResults repoResult) {
		KarXml kx = repoResult.getKarEntry().getParent();
		List<String> prefixes = Arrays.asList("urn:lsid:gamma.msi.ucsb.edu/OpenAuth/:", "urn:lsid:kepler-project.org/ns/:");
		String rawId = kx.getName();
		// Remove the prefix from the rawId before sanitizing. All that
		// boilerplate just gets in the way.
		for (String prefix : prefixes) {
			if (rawId.startsWith(prefix)) {
				rawId = rawId.substring(prefix.length());
				break;
			}
		}
		String karLevelName = rawId;
		if (!karLevelName.endsWith(".kar")) {
		    karLevelName += ".kar";
		}
		EntityLibrary karLevel = null;

		Vector<String> modDeps = new Vector<String>(kx.getModuleDependencies());
		boolean dependenciesSatisfied = ModuleDependencyUtil.checkIfModuleDependenciesSatisfied(modDeps);
		try {
		    // replace periods in the name since they are not allowed in NamedObj
		    String sanitizedName = karLevelName.replace(".", ",");
			if (dependenciesSatisfied) {
				karLevel = new DownloadableKAREntityLibrary(parentLibrary, sanitizedName, kx);
			}
			else {
				karLevel = new RemoteKARErrorEntityLibrary(parentLibrary, sanitizedName);
				((RemoteKARErrorEntityLibrary) karLevel).setKarXml(kx);
			}
			//			karLevel = new EntityLibrary(parentLibrary, karLevelName);
            
			// set the display name, which can have periods
			karLevel.setDisplayName(karLevelName);
		}
		catch (NameDuplicationException nde) {
			//System.out.println("name duplication exception "+nde.getMessage());
			try {
				// add an appendix for the name
				if(kx != null && kx.getLsid() != null)
				{
					//add docid as an appendix
					karLevelName = karLevelName+"-(karId-"+transformLSIDtoDocid(kx.getLsid())+")"+
					"-(karXmlId-"+repoResult.getDocid().replace('.', ':')+")";
				}
				else
				{
					//add random number
					double number = Math.random();
					long appendix = Math.round(number);
					karLevelName = karLevelName+"-("+appendix+")";
				}

		        // replace periods in the name since they are not allowed in NamedObj
				String sanitizedName = karLevelName.replace(".", ",");
				if (dependenciesSatisfied) {
					karLevel = new DownloadableKAREntityLibrary(parentLibrary, sanitizedName, kx);
				}
				else {
					karLevel = new RemoteKARErrorEntityLibrary(parentLibrary, sanitizedName);
					((RemoteKARErrorEntityLibrary) karLevel).setKarXml(kx);
				}
				// set the display name, which can have periods
				karLevel.setDisplayName(karLevelName);
			}
			catch(NameDuplicationException ex) {
				log.warn("This shouldn't happen", ex);
			}
			catch(IllegalActionException ex) {
				log.error("Illegal action exception", ex);
			}
			/*Iterator<CompositeEntity> iterator = (Iterator<CompositeEntity>) parentLibrary.entityList().iterator();
			while (iterator.hasNext()) {
				CompositeEntity entity = iterator.next();
				if (entity.getName().equals(karLevelName)) {
					try {
						karLevel = (EntityLibrary) entity;
						karLevel.setContainer(null);
						if (dependenciesSatisfied) {
							karLevel = new DownloadableKAREntityLibrary(parentLibrary, karLevelName, karEntry.getParent());
						}
						else {
							karLevel = new RemoteKARErrorEntityLibrary(parentLibrary, karLevelName);
							((RemoteKARErrorEntityLibrary) karLevel).setKarXml(kx);
						}
					}
					catch(NameDuplicationException ex) {
						log.warn("This shouldn't happen", ex);
					}
					catch(IllegalActionException ex) {
						log.error("Illegal action exception", ex);
					}
				}
			}*/
		}
		catch(IllegalActionException ex) {
			log.error("Illegal action exception", ex);
		}
		return karLevel;
	}

	/*
	 *Transform a urn:lsid:kepler-project.org/ns/:2099:21:2 to 2099.21.2
	 */
	private String transformLSIDtoDocid(String lsid) {
		String docid = null;
		char colon = ':';
		int targetNumber =3;
		if(lsid != null){
			int index =0;
			for(int i=0; i<lsid.length(); i++){
				char character = lsid.charAt(lsid.length()-1-i);
				if(character == colon){
					index = index+1;
					if(index == targetNumber){
						docid = lsid.substring(lsid.length()-i);
					}
				}
			}
		}
		return docid;
	}

	private List<List<EcogridRepositoryResults>> groupResultsByIndex(List<EcogridRepositoryResults> repoResults) {
		Map<Integer, List<EcogridRepositoryResults>> resultsByIndex = new HashMap<Integer, List<EcogridRepositoryResults>>();
		for (EcogridRepositoryResults repoResult : repoResults) {
			Integer indexValue = repoResult.getIndexValue();
			if (!resultsByIndex.containsKey(indexValue)) {
				resultsByIndex.put(indexValue, new ArrayList<EcogridRepositoryResults>());
			}
			resultsByIndex.get(indexValue).add(repoResult);
		}

		List<List<EcogridRepositoryResults>> results = new ArrayList<List<EcogridRepositoryResults>>();
		for (Integer indexValue : resultsByIndex.keySet()) {
			if (indexValue == null) {
				// These aren't in a single group of 'not a group'. Add them
				// as separate lists.
				for (EcogridRepositoryResults repoResult : resultsByIndex.get(null)) {
					results.add(Collections.singletonList(repoResult));
				}
			}
			else {
				results.add(resultsByIndex.get(indexValue));
			}
		}
		return results;
	}

	private <T> List<T> iteratorToList(Iterator<T> iterator) {
		if (iterator == null) {
			return Collections.emptyList();
		}
		List<T> list = new ArrayList<T>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}

	public class NondraggableTreeItem extends ComponentEntity {
		
		private String workflowName = null;
		private String workflowLSID = null;
		
		public NondraggableTreeItem(String name) throws IllegalActionException, NameDuplicationException {
			super(new CompositeEntity(new Workspace()), name.replace('.', ','));
			setDisplayName(name);
		}

		public String getWorkflowName(){
			return workflowName;
		}
		public void setWorkflowName(String workflowName){
			this.workflowName = workflowName;
		}

		public String getWorkflowLSID(){
			return workflowLSID;
		}

		public void setWorkflowLSID(String workflowLSID){
			this.workflowLSID = workflowLSID;
		}
	}
}
