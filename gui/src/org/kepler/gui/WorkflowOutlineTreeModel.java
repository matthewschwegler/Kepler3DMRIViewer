/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

package org.kepler.gui;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.tree.ClassAndEntityTreeModel;

/**
 * This class displays all class and entities as well as directors in the Tree.
 * 
 * @author Aaron Schultz
 * 
 */
public class WorkflowOutlineTreeModel extends ClassAndEntityTreeModel {
	private static final Log log = LogFactory
			.getLog(WorkflowOutlineTreeModel.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public boolean includeAttributes = false;
	public boolean includePorts = true;
	public boolean includeRelations = false;
	
	
	/**
	 * @param root
	 */
	public WorkflowOutlineTreeModel(CompositeEntity root) {
		super(root);
		if (isDebugging) {
			log.debug("Construct KeplerVisibleTreeModel");
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Get the child of the given parent at the given index. If the child does
	 * not exist, then return null.
	 * 
	 * @param parent
	 *            A node in the tree.
	 * @param index
	 *            The index of the desired child.
	 * @return A node, or null if there is no such child.
	 */
	public Object getChild(Object parent, int index) {

		List<Object> visibleAtts = _visibleAttributes(parent);
		int numVisibleAtts = visibleAtts.size();

		if (index >= numVisibleAtts) {
			return super.getChild(parent, index - numVisibleAtts);
		} else if (index >= 0) {
			return visibleAtts.get(index);
		} else {
			return null;
		}
	}

	/**
	 * Return the number of children of the given parent. This is the number
	 * directors and contained entities.
	 * 
	 * @param parent
	 *            A parent node.
	 * @return The number of children.
	 */
	public int getChildCount(Object parent) {
		List<Object> visibleAtts = _visibleAttributes(parent);
		int numVisibleAtts = visibleAtts.size();

		return numVisibleAtts + super.getChildCount(parent);
	}

	/**
	 * Return the index of the given child within the given parent. If the
	 * parent is not contained in the child, return -1.
	 * 
	 * @param parent
	 *            The parent.
	 * @param child
	 *            The child.
	 * @return The index of the specified child.
	 */
	public int getIndexOfChild(Object parent, Object child) {
		List<Object> visibleAtts = _visibleAttributes(parent);

		int index = visibleAtts.indexOf(child);

		if (index >= 0) {
			return index;
		}

		return -1;
	}

	/**
	 * Return true if the object is a leaf node. An object is a leaf node if it
	 * has no children that are instances of one of the classes specified by
	 * setFilter(), if a filter has been specified.
	 * 
	 * @param object
	 *            The object.
	 * @return True if the node has no children.
	 */
	public boolean isLeaf(Object object) {
		if (_visibleAttributes(object).size() > 0) {
			return false;
		}

		return super.isLeaf(object);
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	/**
	 * Return the list of Attributes that we want to be visible in the outline
	 * view, or an empty list if there are none. Override this method if you
	 * wish to show only a subset of the attributes.
	 * 
	 * @param object
	 *            The object.
	 * @return A list of attributes.
	 */
	protected List<Object> _visibleAttributes(Object object) {
		if (!(object instanceof NamedObj)) {
			return Collections.emptyList();
		}
		Vector<Object> visibleAtts = new Vector<Object>(1);

		if (includeAttributes) { 
			//  Look for attributes that we want to include
			List<?> attributes = ((NamedObj) object).attributeList();
			for (Object att : attributes) {
				visibleAtts.add(att);
			}
		}
	
		if (includePorts) {
			if (object instanceof Entity) {
		
				// Look for ports we want to include
				List<?> ports = ((Entity) object).portList();
				for (Object port : ports) {
					if (port instanceof Port) {
						visibleAtts.add(port);
					}
				}
			}
		}

		if (includeRelations) {
	        if ((object instanceof CompositeEntity)) {
	        	List<?> relations = ((CompositeEntity) object).relationList();
	        	for (Object relation : relations) {
	        		visibleAtts.add(relation);
	        	}
	        }
		}

		return visibleAtts;
	}
}