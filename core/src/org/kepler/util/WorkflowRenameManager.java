/*
Copyright (c) 2010 The Regents of the University of California.
All rights reserved.
Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the above
copyright notice and the following two paragraphs appear in all copies
of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

*/

package org.kepler.util;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.kernel.util.NamedObj;


public class WorkflowRenameManager {

	static private WorkflowRenameManager _instance = null;

	
    /** The list of WorkflowRenameListener. */
    private List<WeakReference<WorkflowRenameListener>> _renameListenerList =
    	new LinkedList<WeakReference<WorkflowRenameListener>>();
    
    
	public synchronized static WorkflowRenameManager getInstance() {
		if (_instance == null) {
			_instance = new WorkflowRenameManager();
		}
		return _instance;
	}
    
	private WorkflowRenameManager() {
	}

	/**
	 * Notify all WorkflowRenameListeners that a workflow was renamed.
	 * @param namedObj the workflow
	 * @param oldLSID the previous LSID
	 * @param newLSID the new LSID
	 * @param oldName the previous name
	 * @param newName the new name
	 */
    public void renamedWorkflow(NamedObj namedObj, KeplerLSID oldLSID,
            KeplerLSID newLSID, String oldName, String newName){
       
        for(WeakReference<WorkflowRenameListener> listenerWf : _renameListenerList){
        	WorkflowRenameListener listener = listenerWf.get();
        	if (listener != null)
	            listener.renamedWorkflow(namedObj, oldLSID, newLSID,
	            		oldName, newName);
        }	
    }
    
    /** Add a WorkflowRenameListener to the list of listeners. */
    public void addRenameListener(WorkflowRenameListener listener) {
        _renameListenerList.add(new WeakReference(listener));
    }

    /** Clear the list of WorkflowRenameListener. */
    public void clearRenameListeners() {
        _renameListenerList.clear();
    }

    /** Remove a WorkflowRenameListener from the list of listeners. */
    public void removeRenameListener(WorkflowRenameListener listener) {
        _renameListenerList.remove(listener);
    }
    
    
}