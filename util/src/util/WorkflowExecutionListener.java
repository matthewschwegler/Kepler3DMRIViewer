/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

package util;

import java.util.HashMap;
import java.util.Map;

import ptolemy.actor.ExecutionListener;
import ptolemy.actor.Manager;
import ptolemy.kernel.util.NamedObj;

/**
 * The singleton WorkflowExecutionListener allows the R actor to group output
 * per workflow execution
 * 
 * @author Ben Leinfelder, NCEAS, UC Santa Barbara
 */
public class WorkflowExecutionListener implements ExecutionListener {

	private static WorkflowExecutionListener instance;

	// private String id = null;
	private Map ids = null;

	public static WorkflowExecutionListener getInstance() {
		if (instance == null) {
			instance = new WorkflowExecutionListener();
		}
		return instance;
	}

	private WorkflowExecutionListener() {
		this.ids = new HashMap();
	}

	public void executionError(Manager manager, Throwable throwable) {
		NamedObj toplevel = manager.toplevel();
		instance.ids.remove(toplevel);
	}

	public void executionFinished(Manager manager) {
		NamedObj toplevel = manager.toplevel();
		instance.ids.remove(toplevel);
	}

	public void managerStateChanged(Manager manager) {
		// do nothing
	}

	public String getId(NamedObj topLevel) {
		if (!instance.ids.containsKey(topLevel)) {
			// new workflow should have a new timestamp
			long now = System.currentTimeMillis();
			String id = "" + now;
			instance.ids.put(topLevel, id);
		}
		return (String) instance.ids.get(topLevel);
	}

}