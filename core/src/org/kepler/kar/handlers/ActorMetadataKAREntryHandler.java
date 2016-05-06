/*
 *  The node controller for actor instances.
 *  Copyright (c) 2010 The Regents of the University of California.
 *  All rights reserved.
 *  Permission is hereby granted, without written agreement and without
 *  license or royalty fees, to use, copy, modify, and distribute this
 *  software and its documentation for any purpose, provided that the above
 *  copyright notice and the following two paragraphs appear in all copies
 *  of this software.
 *  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 *  FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 *  THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 *  PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 *  PT_COPYRIGHT_VERSION_2
 *  COPYRIGHTENDKEY
 */
package org.kepler.kar.handlers;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KAREntryHandler;
import org.kepler.kar.KAREntryHandlerFactory;
import org.kepler.kar.KARFile;
import org.kepler.kar.KARManager;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.cache.CacheObjectInterface;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.ModelDirectory;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLParser;

/**
 * @author Aaron Schultz
 */
public class ActorMetadataKAREntryHandler implements KAREntryHandler {

	private static final Log log = LogFactory
			.getLog(ActorMetadataKAREntryHandler.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// backwards compatibility type string
	// KAR version 2.0 and 2.1 uses the binary class name as the type
	public static final String TYPE = "actorMetadata";

	// The class types that we'll handle with this KAREntryHandler
	private static Vector<Class> handledClassTypes;

	static {
		handledClassTypes = new Vector<Class>(5);

		try {
			handledClassTypes.add(Class
					.forName("ptolemy.kernel.ComponentEntity"));
			handledClassTypes
					.add(Class
							.forName("org.kepler.objectmanager.cache.ActorCacheObject"));
			handledClassTypes.add(Class
					.forName("ptolemy.actor.TypedAtomicActor"));
			handledClassTypes.add(Class
					.forName("ptolemy.actor.TypedCompositeActor"));
			handledClassTypes.add(Class
					.forName("ptolemy.kernel.CompositeEntity"));
			handledClassTypes.add(Class
					.forName("org.kepler.moml.CompositeClassEntity"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public ActorMetadataKAREntryHandler() {

	}

	/**
	 * Method for backwards compatibility with KAR version 1.0 KAR version 2.0
	 * and 2.1 uses the binary class name as the type.
	 * 
	 * @see org.kepler.kar.KAREntryHandler#getTypeName()
	 */
	public String getTypeName() {
		return TYPE;
	}

	/**
	 * If the typeName is equal to or a subclass of any of the handled class
	 * types, then we return true.
	 */
	public boolean handlesType(String typeName) {
		return handlesClass(typeName);
	}

	/**
	 * If the typeName is equal to or a subclass of any of the handled class
	 * types, then we return true.
	 */
	public static boolean handlesClass(String className) {

		Class clazz;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			// e.printStackTrace();
			return false;
		}

		if (handledClassTypes.contains(clazz)) {
			return true;
		}

		// check superclasses
		Class superClazz = clazz.getSuperclass();
		while (superClazz != null) {
			if (handledClassTypes.contains(superClazz)) {
				return true;
			}
			superClazz = superClazz.getSuperclass();
		}
		return false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.kar.KAREntryHandler#initialize()
	 */
	public void initialize() {
		if (isDebugging) {
			log.debug("initialize()");
		}
	}
	
	/*
	private static class KarEntryReference {
		public KarEntryReference(KARFile karFile, KAREntry karEntry) {
			this.karFile = karFile.getFileLocation();
			this.karEntryString = karEntry.getName();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			KarEntryReference that = (KarEntryReference) o;

			if (karEntryString != null ? !karEntryString.equals(that.karEntryString) : that.karEntryString != null)
				return false;
			if (karFile != null ? !karFile.equals(that.karFile) : that.karFile != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = karFile != null ? karFile.hashCode() : 0;
			result = 31 * result + (karEntryString != null ? karEntryString.hashCode() : 0);
			return result;
		}

		private final File karFile;
		private String karEntryString;
	}
	*/
	
	//private static Map<KarEntryReference, ActorCacheObject> acoCache = new HashMap<KarEntryReference, ActorCacheObject>();
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.kar.KAREntryHandler#cache(org.kepler.kar.KARFile,
	 * java.util.jar.JarEntry)
	 */
	public synchronized CacheObject cache(KARFile karFile, KAREntry entry) throws Exception {
		if (isDebugging) {
			log.debug("cache(" + karFile.toString() + "," + entry.toString()
					+ ")");
		}
		// System.out.println("caching from amkeh" + karFile.toString() + "," +
		// entry.toString());

		// NOTE: the hashCode for KarEntryReference relies on the LSID.
		// Sometimes the entry contents change, but not the LSID, e.g.,
		// when the Display actor's window moves the LSID revision is not
		// incremented. This results in the stale entry being found in
		// acoCache and the updated entry not being updated in the cache.
		/* 
		KarEntryReference ker = new KarEntryReference(karFile, entry);
		if (acoCache.containsKey(ker)) {
			return acoCache.get(ker);
		}
		*/
				
		ActorCacheObject aco = new ActorCacheObject(karFile
				.getInputStream(entry));
		//acoCache.put(ker, aco);

		if (isDebugging) {
			log.debug("created actor cache object: " + aco);
		}
		return aco;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.kar.KAREntryHandler#open(java.util.jar.JarEntry)
	 */
	public boolean open(KARFile karFile, KAREntry entry, TableauFrame tableauFrame) throws Exception {
		if (isDebugging) {
			log.debug("open(" + karFile.toString() + "," + entry.toString()
					+ ")");
		}

		KeplerLSID lsid = entry.getLSID();
		if (isDebugging)
			log.debug(lsid);

		CacheManager cache = CacheManager.getInstance();

		// get the object from the cache (it is GraphicalActorMetadata even
		// though it is a workflow)
		CacheObjectInterface co = cache.getObject(lsid);
		NamedObj entity = null;

		if (co == null) {
			if (isDebugging)
				log.debug(lsid + " was not found in the cache");
			if (isDebugging)
				log.debug("Opening from file");

			MoMLParser parser = new MoMLParser();
			InputStream stream = null;
			try {
			    stream = karFile.getInputStream(entry);
			    entity = parser.parse(null, karFile.getFileLocation().getCanonicalPath(), stream);
			} finally {
			    if(stream != null) {
			        stream.close();
			    }
			}

			if (entity == null) {
				return false;
			}
		} else {
			Object o = co.getObject();
			if (o == null) {
				return false;
			}
			if (isDebugging)
				log.debug(o.getClass().getName());

			if (o instanceof ActorMetadata) {
				ActorMetadata am = (ActorMetadata) o;
				try {
					// get the workflow from the metadata
					entity = am.getActorAsNamedObj(null);

					if (isDebugging)
						log.debug(entity.getName() + " "
								+ NamedObjId.getIdFor(entity) + " "
								+ entity.getClass().getName());
					if (entity instanceof CompositeEntity
							|| entity instanceof TypedCompositeActor) {
						if (isDebugging)
							log.debug("Opening CompositeEntity");

						// get the xml representation - needs parsing to be
						// correct!
						String moml = entity.exportMoML();
						MoMLParser parser = new MoMLParser();
						entity = parser.parse(moml);
					} else {
						if (isDebugging)
							log.debug("Not a CompositeEntity");
						return false;
					}
				} catch (Exception e) {
					e.printStackTrace();
					log.error("error opening the workflow: " + e.getMessage());
					return false;
				}
			}
		}

		Configuration configuration = (Configuration) Configuration
				.configurations().iterator().next();

		// TODO check on this
		// ----begin questionable title bar code:
		PtolemyEffigy effigy = new PtolemyEffigy(configuration.workspace());
		effigy.setModel(entity);
		ModelDirectory directory = (ModelDirectory) configuration
				.getEntity("directory");
		
		// is this the right name for the effigy?
		effigy.setName(entity.getName());
		
		effigy.identifier.setExpression(entity.getName());
		if (directory != null) {
			if (directory.getEntity(entity.getName()) != null) {
				// Name is already taken.
				int count = 2;
				String newName = effigy.getName() + " " + count;
				while (directory.getEntity(newName) != null) {
					newName = effigy.getName() + " " + ++count;
				}
				effigy.setName(newName);
			}
		}
		effigy.setContainer(directory);
		// ---end questionable title bar code

		// open a new window for the workflow
		Tableau t = configuration.openModel(entity);
		
		if (t != null) {
			//if (isDebugging)
				//log.debug(entity.getName() + " was opened successfully");

			// no need to put this in ObjectManager for ReportLayoutKAREntry's
			// open,
			// it gets in there somehow anyways
			// ObjectManager.getInstance().addNamedObj(entity);
			// ObjectManager.assignIdTo(entity, lsid);

			//Add JFrame => KARFile mapping to KARManager
			KARManager karManager = KARManager.getInstance();
			karManager.add(t.getFrame(), karFile);
			
			return true;
		}

		return false;

	}

	/**
	 * The save method for ActorMetadataKAREntryHandler is not used since Actors
	 * and Workflows are not dependent on anything else. They are the top of the
	 * dependency tree for KARs and are therefore created by the
	 * KARBuilder.handleInitiatorList Method. However, the
	 * ActorMetadataKAREntryHandler is still listed as the handler that created
	 * the file in the KAR manifest since this handler is used for opening and
	 * cacheing.
	 * 
	 * @see org.kepler.kar.KAREntryHandler#save(org.kepler.objectmanager.lsid.KeplerLSID
	 *      )
	 */
	public Hashtable<KAREntry, InputStream> save(Vector<KeplerLSID> lsids,
			KeplerLSID karLsid, TableauFrame tableauFrame) throws Exception {
		if (isDebugging)
			log.debug("save(" + lsids + ")");

		return null;
	}

	/**
	 * A factory that creates a KAREntryHandler object.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends KAREntryHandlerFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a library pane that displays the given library of actors.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public KAREntryHandler createKAREntryHandler() {
			if (isDebugging)
				log.debug("createKAREntryHandler()");
			ActorMetadataKAREntryHandler amkeh = new ActorMetadataKAREntryHandler();
			return amkeh;
		}
	}
}
