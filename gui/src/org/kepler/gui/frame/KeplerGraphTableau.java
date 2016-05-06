/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-06-28 14:28:16 -0700 (Thu, 28 Jun 2012) $' 
 * '$Revision: 30086 $'
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

package org.kepler.gui.frame;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.KeplerGraphFrame;
import org.kepler.gui.kar.KAREffigy;
import org.kepler.kar.KARManager;
import org.kepler.moml.NamedObjId;
import org.kepler.moml.NamedObjIdReferralList;
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.TransientStringAttribute;

import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFactory;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.InstantiableNamedObj;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.LibraryAttribute;

//////////////////////////////////////////////////////////////////////////
//// Kepler GraphTableau

/**
 A graph editor for ptolemy models.  

 This is a graph editor for ptolemy models.  It constructs an instance
 of GraphFrame, which contains an editor pane based on diva.

 @see ptolemy.vergil.actor.ActorGraphTableau
 @author  Based on GraphTableau by Steve Neuendorffer and Edward A. Lee
 @version $Id: KeplerGraphTableau.java 30086 2012-06-28 21:28:16Z crawl $
 @since Ptolemy II 2.0
 @Pt.ProposedRating Red (neuendor)
 @Pt.AcceptedRating Red (johnr)
 */
public class KeplerGraphTableau extends Tableau {
	private static final Log log = LogFactory.getLog(KeplerGraphTableau.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
    // This class extends Tableau so that highlighting of actors
    // works.  See
    // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=2321
    // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4050
    // util/src/org/kepler/sms/gui/WorkflowTypeCheckerDialog.java
    // ptII/ptolemy/vergil/unit/UnitSolverDialog.java

    // The only substantive difference between this class and
    // ptolemy.vergil.actor.ActorGraphTableau is that this class
    // constructs an instance of KeplerGraphFrame.
    
	/**
	 * Create a tableau in the specified workspace.
	 * 
	 * @param workspace
	 *            The workspace.
	 * @exception IllegalActionException
	 *                If thrown by the superclass.
	 * @exception NameDuplicationException
	 *                If thrown by the superclass.
	 */
	public KeplerGraphTableau(Workspace workspace)
			throws IllegalActionException, NameDuplicationException {
		super(workspace);
		if (isDebugging) {
			log.debug("KeplerGraphTableau(workspace)");
		}
	}

	/**
	 * Create a tableau with the specified container and name, with no specified
	 * default library.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @exception IllegalActionException
	 *                If thrown by the superclass.
	 * @exception NameDuplicationException
	 *                If thrown by the superclass.
	 */
	public KeplerGraphTableau(PtolemyEffigy container, String name)
			throws IllegalActionException, NameDuplicationException {
		this(container, name, null);
	}

	/**
	 * Create a tableau with the specified container, name, and default library.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @param defaultLibrary
	 *            The default library, or null to not specify one.
	 * @exception IllegalActionException
	 *                If thrown by the superclass.
	 * @exception NameDuplicationException
	 *                If thrown by the superclass.
	 */
	public KeplerGraphTableau(PtolemyEffigy container, String name,
			LibraryAttribute defaultLibrary) throws IllegalActionException,
			NameDuplicationException {
		super(container, name);
		if (isDebugging) {
			log.debug("KeplerGraphTableau");
			if (container == null) {
				log.debug("container is null");
			} else {
				log.debug(container.getName() + " : " + container.getClassName());
			}
			log.debug("name: " + name);
		}

		NamedObj model = container.getModel();

		if (model == null) {
			return;
		}

		if (!(model instanceof CompositeEntity)) {
			throw new IllegalActionException(this,
					"Cannot graphically edit a model "
							+ "that is not a CompositeEntity. Model is a "
							+ model);
		}
		
		InstantiableNamedObj inoModel = (InstantiableNamedObj) model;
		boolean isClass = inoModel.isClassDefinition();
		//System.out.println("isClass: " + isClass);
		//System.out.println("model class name: " + inoModel.getClassName());
		
		ObjectManager om = ObjectManager.getInstance();
		
		if (isClass) {
			// This is a class so assign a new id to it
			NamedObjId noid = NamedObjId.getIdAttributeFor(model);
			if (noid == null) {
				// probably will always get this
				try {
					NamedObjId.assignIdTo(model);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// And figure out the class name
			if (model instanceof InstantiableNamedObj) {
				InstantiableNamedObj ino = (InstantiableNamedObj) model;
				List inoc = ino.getChildren();
				if (inoc != null) {
					if (isDebugging) log.debug("has " + inoc.size() + " children");
					if (inoc.size() > 0) {
						Vector<NamedObj> noChildren = new Vector<NamedObj>();
						for (Object o : inoc) {
							if (isDebugging) log.debug(o.getClass().getName());
							if (o instanceof WeakReference) {
								WeakReference w = (WeakReference) o;
								Object o2 = w.get();
								if (isDebugging) log.debug(o2.getClass().getName());
								if (o2 instanceof NamedObj) {
									noChildren.add((NamedObj) o2);
								}
							} else if (o instanceof NamedObj) {
								noChildren.add( (NamedObj) o );
							}
						}
						NamedObj theChild = null;
						Vector<NamedObj> noChildrenWithClass = new Vector<NamedObj>();
						for (NamedObj aChild : noChildren) {
							Attribute classAtt = aChild.getAttribute("class");
							if (classAtt != null) {
								StringAttribute classAttribute = (StringAttribute) classAtt;
								String className = classAttribute.getExpression();
								noChildrenWithClass.add(aChild);
								//System.out.println("class name: " + className);
							}
						}
						if (noChildrenWithClass.size() == 1) {
							theChild = noChildrenWithClass.get(0);
						}
						if (theChild != null) {
							// Tag this object with the classname from the child so it can be saved properly
							// See ActorMetadata(NamedObj)
							Attribute classAtt = theChild.getAttribute("class");
							if (classAtt != null) {
								Attribute tcn = model.getAttribute("tempClassName");
								if (tcn == null) {
									StringAttribute classAttribute = (StringAttribute) classAtt;
									String className = classAttribute.getExpression();
									TransientStringAttribute newClassAttribute = new TransientStringAttribute(model,"tempClassName");
									newClassAttribute.setExpression(className);
									Attribute idAtt = classAttribute.getAttribute("id");
									if (idAtt != null) {
										String classId = ((StringAttribute) idAtt).getExpression();
										TransientStringAttribute newClassIdAttribute = new TransientStringAttribute(newClassAttribute,"id");
										newClassIdAttribute.setExpression(classId);
									}
								}
							}
							// Also, populate the Referral list with the child's Id, and set the parent
							NamedObjId childId = NamedObjId.getIdAttributeFor(theChild);
							if (childId != null) {
								NamedObjId newNoId = NamedObjId.getIdAttributeFor(model);
								try {
									//NamedObjId.assignIdTo(model);
									NamedObjIdReferralList childIdList = NamedObjId.getIDListAttributeFor(model);
									if (childIdList == null) {
										childIdList = new NamedObjIdReferralList(model,NamedObjIdReferralList.NAME);
									}
									KeplerLSID childLsid = childId.getId();
									boolean alreadyInList = childIdList.hasReferral(childLsid);
									boolean matchesLsid = childLsid.equals(newNoId.getId());
									if (!alreadyInList && !matchesLsid) {
										String childLsidStr = childLsid.toString();
										KeplerLSID newId = new KeplerLSID(childLsidStr);
										childIdList.addReferral(newId);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								// Also, we need to set the child id as the parent of the new id
								// This allows the id to be updated when the new opened actor is edited.
								try {
									NamedObjId.assignIdTo(theChild, newNoId.getId());
								} catch (Exception e) {
									e.printStackTrace();
								}
								newNoId.setParentId(NamedObjId.getIdAttributeFor(theChild));
							}
							
						}
					}
				}
			}
			

		} else {
			// Check to see if this model has a KeplerLSID associated with it
			try {
				NamedObjId noid = NamedObjId.getIdAttributeFor(model);
				if (noid == null) {
					// if there is no KeplerLSID we'll check to see if this model
					// was cloned from a child through instantiation, if it was
					// we will assign the child's id to this model
					if (model instanceof InstantiableNamedObj) {
						InstantiableNamedObj ino = (InstantiableNamedObj) model;
						List inoc = ino.getChildren();
						if (inoc != null) {
							if (isDebugging) log.debug("has " + inoc.size() + " children");
							if (inoc.size() > 0) {
								Vector<NamedObj> noChildren = new Vector<NamedObj>();
								for (Object o : inoc) {
									if (isDebugging) log.debug(o.getClass().getName());
									if (o instanceof WeakReference) {
										WeakReference w = (WeakReference) o;
										Object o2 = w.get();
										if (isDebugging) log.debug(o2.getClass().getName());
										if (o2 instanceof NamedObj) {
											noChildren.add((NamedObj) o2);
										}
									} else if (o instanceof NamedObj) {
										noChildren.add( (NamedObj) o );
									}
								}
								NamedObj theChild = null;
								Vector<NamedObj> noChildrenWithIds = new Vector<NamedObj>();
								for (NamedObj aChild : noChildren) {
									NamedObjId aNoi = NamedObjId.getIdAttributeFor(aChild);
									if (aNoi != null) {
										noChildrenWithIds.add(aChild);
									}
								}
								if (noChildrenWithIds.size() == 1) {
									theChild = noChildrenWithIds.get(0);
								}
								if (theChild != null) {	
									if (isDebugging) log.debug("child: "+theChild.getName() + " : " + theChild.getClassName());
									/*
									System.out.println("***************************************************************");
									System.out.println(model.exportMoML());
									System.out.println("***************************************************************");
									System.out.println(no.exportMoML());
									System.out.println("***************************************************************");
									*/
									NamedObjId theChildId = NamedObjId.getIdAttributeFor(theChild);
									if (theChildId != null) {
										noid = theChildId;
									}
								}
							}
						} else {
							log.debug("instantiable model has no children");
						}
					}
				}
				if (noid == null) {
					NamedObjId.assignIdTo(model);
				} else {
					NamedObjId.assignIdTo(model,noid.getId());
				}
				if (isDebugging) {
					NamedObjId tnoid = NamedObjId.getIdAttributeFor(model);
					if (tnoid == null) {
						log.debug("NamedObjId is still null");
					} else {
						log.debug(tnoid.getId().toString());
					}
				}
			} catch (Exception e) {
                            throw new IllegalActionException(model, e,
						"No Kepler ID was or could be assigned to model: "
								+ e.toString());
			}
		}

		CompositeEntity entity = (CompositeEntity) model;
		try {
			om.addNamedObj(entity);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		createGraphFrame(entity, defaultLibrary);
		
		if(container instanceof KAREffigy) {
			KAREffigy effigy = (KAREffigy) container;
			// associate the frame
			KARManager.getInstance().add(getFrame(), effigy.getKARFile());
			
			// if it's a kar, open the non-actor entries, e.g., report layout
			effigy.openKAREntries((TableauFrame) getFrame(), false);
		}
		
	}
	
    /** Create the graph frame that displays the model associated with
     *  this tableau together with the specified library.
     *  @param model The Ptolemy II model to display in the graph frame.
     *  @param defaultLibrary The default library, or null to not specify
     *   one.
     */
     public void createGraphFrame(CompositeEntity model,
            LibraryAttribute defaultLibrary)
     {
        KeplerGraphFrame frame = new KeplerGraphFrame(model, this,
                defaultLibrary);

        try {
            setFrame(frame);
        } catch (IllegalActionException e) {
            throw new InternalErrorException(e);
        }
        frame.setBackground(BACKGROUND_COLOR);
    }

	// /////////////////////////////////////////////////////////////////
	// // private members ////
	// The background color.
	private static Color BACKGROUND_COLOR = new Color(0xe5e5e5);

	// /////////////////////////////////////////////////////////////////
	// // public inner classes ////

	/**
	 * A factory that creates graph editing tableaux for Ptolemy models.
	 */
	public static class Factory extends TableauFactory {
		/**
		 * Create an factory with the given name and container.
		 * 
		 * @param container
		 *            The container.
		 * @param name
		 *            The name.
		 * @exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 * @exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a tableau in the default workspace with no name for the given
		 * Effigy. The tableau will created with a new unique name in the given
		 * model effigy. If this factory cannot create a tableau for the given
		 * effigy (perhaps because the effigy is not of the appropriate
		 * subclass) then return null. It is the responsibility of callers of
		 * this method to check the return value and call show().
		 * 
		 * @param effigy
		 *            The model effigy.
		 * @return A new KeplerGraphTableau, if the effigy is a PtolemyEffigy,
		 *         or null otherwise.
		 * @exception Exception
		 *                If an exception occurs when creating the tableau.
		 */
		public Tableau createTableau(Effigy effigy) throws Exception {
			if (effigy instanceof PtolemyEffigy) {
				// First see whether the effigy already contains a graphTableau.
				KeplerGraphTableau tableau = (KeplerGraphTableau) effigy
						.getEntity("graphTableau");

				if (tableau == null) {
					// Check to see whether this factory contains a
					// default library.
					LibraryAttribute library = (LibraryAttribute) getAttribute(
							"_library", LibraryAttribute.class);
					tableau = new KeplerGraphTableau((PtolemyEffigy) effigy,
							"graphTableau", library);
				}

				// Don't call show() here, it is called for us in
				// TableauFrame.ViewMenuListener.actionPerformed()
				return tableau;
			} else {
				return null;
			}
		}
	}
}