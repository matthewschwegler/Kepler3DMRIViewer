/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;

public abstract class AbstractDialogTab extends JPanel {

	/**
	 * Construct a new instance of this object - to be called from implementing
	 * class like this: <code>
	 *     public MyImplementationNameHere(NamedObj target) {
	 *       super(target);
	 *     }
   * </code>
	 * 
	 * @param target
	 *            NamedObj the object to be configured (actor, director etc)
	 * 
	 * @param targetType
	 *            the string representing this type of target, to be used in the
	 *            resourcebundle keys. - such as "actor", "director" etc.
	 */
	public AbstractDialogTab(NamedObj target, String targetType,
			TableauFrame frame) {
		super();
		this._target = target;
		this._targetType = targetType;
		this._frame = frame;
		this.setLayout(new BorderLayout());
		this.setBorder(TabbedDialog.tabPanePadding);
		this.setOpaque(true);
		this.add(getTopPanel(), BorderLayout.NORTH);
		this.add(getCenterPanel(), BorderLayout.CENTER);
		this.add(getBottomPanel(), BorderLayout.SOUTH);
	}

	// ////////////////////////////////////////////////////////////////////////////
	// protected methods //
	// ////////////////////////////////////////////////////////////////////////////

	/**
	 * get the Component that will be displayed in the NORTH section of the
	 * BorderLayout. Note that if the dialog is resizable, this Component will
	 * need to stretch along the x axis, while retaining its aesthetic qualities
	 * 
	 * @return Component
	 */
	protected abstract Component getTopPanel();

	/**
	 * get the Component that will be displayed in the CENTER section of the
	 * BorderLayout. Note that if the dialog is resizable, this Component will
	 * need to stretch along both the x <em>and</em> y axes, while retaining its
	 * aesthetic qualities
	 * 
	 * @return Component
	 */
	protected abstract Component getCenterPanel();

	/**
	 * get the Component that will be displayed in the SOUTH section of the
	 * BorderLayout. Note that if the dialog is resizable, this Component will
	 * need to stretch along the x axis, while retaining its aesthetic qualities
	 * 
	 * @return Component
	 */
	protected abstract Component getBottomPanel();

	/**
	 * check the user input for errors/omissions. Return true if everything is
	 * OK and we can proceed with a save(). Return false if there are problems
	 * that need to be corrected, and preferably request focus for the "problem"
	 * UI component
	 * 
	 * @return boolean true if everything is OK and we can proceed with a
	 *         save(). Return false if there are problems that need to be
	 *         corrected, and preferably request focus for the "problem" UI
	 *         component
	 */
	protected abstract boolean validateInput();

	/**
	 * Save the user-editable values associated with this tab. The container
	 * should probably call validateInput() on each tab before saving
	 */
	protected abstract void save();

	/**
	 * get the string representing this type of target, to be used in the
	 * resourcebundle keys. For example, all the resourcebundle keys for actors
	 * are of the form:
	 * 
	 * dialogs.actor.general.id
	 * 
	 * and for directors, of the form:
	 * 
	 * dialogs.director.general.id
	 * 
	 * ...etc - thus, the value returned by this method if implemented in an
	 * actor dialog should be "actor"; for a director dialog, it should be
	 * "director", etc, so it can be used to generate the above example keys as
	 * follows:
	 * 
	 * key = "dialogs." + getTargetType() + ".general.id"
	 * 
	 * @return String representing this type of target - such as "actor",
	 *         "director" etc
	 */
	protected String setTargetType(String targetType) {
		return _targetType;
	}

	// ////////////////////////////////////////////////////////////////////////////
	// protected variables //
	// ////////////////////////////////////////////////////////////////////////////

	protected final NamedObj _target;
	protected final TableauFrame _frame;
	protected final String _targetType;

	protected static final String ACTOR_TARGET_TYPE = "actor";
	protected static final String DIRECTOR_TARGET_TYPE = "director";
	protected static final String WORKFLOW_TARGET_TYPE = "workflow";

	// ////////////////////////////////////////////////////////////////////////////
	// private methods //
	// ////////////////////////////////////////////////////////////////////////////

}
