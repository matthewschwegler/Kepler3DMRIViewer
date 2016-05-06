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
import java.lang.ref.WeakReference;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TextEffigy;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

public class MultipleTabDisplayJavaSE implements MultipleTabDisplayInterface {
    @Override
    public void display(String displayName, String content,
            boolean isAddNewLines) {
        if (frame.isDisposed()) {
            String workflowName = getWorkflowName();
            MultipleTabDisplayFrame.removeInstance(workflowName);
            frame = MultipleTabDisplayFrame.getInstance(workflowName);
            try {
                tableau.setFrame(frame);
            } catch (IllegalActionException e) {
                throw new InternalErrorException(self, e,
                        "Failed to initialize MultipleTabDisplay actor");
            }
        }
        if (pane == null || frame.isVisible() == false) {
            frame.setVisible(true);
        }
        frame.addText(displayName, content, isAddNewLines);
    }

    @Override
    public void place(Container container) {
        if (container != null) {
            pane = frame.getPane();
            container.add(pane);
            frame.setVisible(false);
        } else {
            if (pane != null) {
                frame.setContentPane(pane);
                pane = null;
            }
            frame.setVisible(true);
        }
    }

    @Override
    public void initialize(TypedAtomicActor selfActor) {
        this.self = selfActor;
        try {
            Effigy effigy = Configuration.findEffigy(self.toplevel());
            effigy = TextEffigy.newTextEffigy(effigy, "");

            String workflowName = getWorkflowName();

            tableau = new MultipleTabDisplayTableau(effigy, "tableau",
                    workflowName);
            frame = tableau.frame.get();
            boolean isInstance = MultipleTabDisplayFrame
                    .isInstance(workflowName);
            if (!isInstance) {
                frame.initialize();
            }
        } catch (Exception e) {
            throw new InternalErrorException(self, e,
                    "Failed to initialize MultipleTabDisplay actor");
        }
    }

    @Override
    public void prepare() {
        frame.initialize();
    }

    private String getWorkflowName() {
        NamedObj root = self.getContainer();
        while (root.getContainer() != null)
            root = root.getContainer();
        return root.getDisplayName();
    }

    // /////////////////////////////////////////////////////////////////////////
    // private variables
    private Container pane;
    private MultipleTabDisplayFrame frame;
    private MultipleTabDisplayTableau tableau;
    private TypedAtomicActor self;

    private static class MultipleTabDisplayTableau extends Tableau {
        private static final long serialVersionUID = 3079747086891393696L;

        public MultipleTabDisplayTableau(CompositeEntity container,
                String name, String workflowName)
                throws IllegalActionException, NameDuplicationException {
            super(container, name);

            frame = new WeakReference<MultipleTabDisplayFrame>(
                    MultipleTabDisplayFrame.getInstance(workflowName));
            setTitle(workflowName);
            setFrame(frame.get());
        }

        public WeakReference<MultipleTabDisplayFrame> frame;
    }
}
