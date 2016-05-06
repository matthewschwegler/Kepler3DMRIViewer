/*
 * 
 * Copyright (c) 2013 FP7 EU EUFORIA (211804) & POZNAN SUPERCOMPUTING AND
 * NETWORKING CENTER All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, copy, modify, and distribute this software and its
 * documentation for any purpose, provided that the above copyright notice and
 * the following two paragraphs appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE FP7 EU EUFORIA (211804) & POZNAN SUPERCOMPUTING AND
 * NETWORKING CENTER BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF THE FP7 EU EUFORIA (211804) & POZNAN
 * SUPERCOMPUTING AND NETWORKING CENTER HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE FP7 EU EUFORIA (211804) & POZNAN SUPERCOMPUTING AND NETWORKING CENTER
 * SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE FP7 EU
 * EUFORIA (211804) & POZNAN SUPERCOMPUTING AND NETWORKING CENTER HAS NO
 * OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS.
 *
 */
package pl.psnc.kepler.common.actor;

import java.awt.Container;
import java.util.HashMap;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.kernel.util.NamedObj;
import au.edu.jcu.kepler.hydrant.ReplacementManager;
import au.edu.jcu.kepler.hydrant.ReplacementUtils;

public class MultipleTabDisplayBatch implements MultipleTabDisplayInterface {
    @Override
    public void display(String displayName, String content,
            boolean isAddNewLines) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("name", workflowName + "." + displayName);
        map.put("type", "txt");
        map.put("append", true);
        map.put("output", content);
        manager.writeData(map);
    }

    @Override
    public void place(Container container) {
        // do nothing
    }

    @Override
    public void initialize(TypedAtomicActor self) {
        manager = ReplacementUtils.getReplacementManager(self);
        NamedObj root = self.getContainer();
        while (root.getContainer() != null)
            root = root.getContainer();
        workflowName = root.getDisplayName();
    }
    
    @Override
    public void prepare() {
        // do nothing
    }

    // /////////////////////////////////////////////////////////////////////////
    // private variables
    private ReplacementManager manager;
    private String workflowName;
}
