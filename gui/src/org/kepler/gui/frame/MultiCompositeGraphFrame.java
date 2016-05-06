/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-21 15:42:51 -0700 (Fri, 21 Sep 2012) $' 
 * '$Revision: 30737 $'
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.kepler.gui.KeplerGraphFrame;
import org.kepler.gui.ModelToFrameManager;
import org.kepler.gui.state.StateChangeMonitor;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.lib.hoc.MultiCompositeActor;
import ptolemy.actor.lib.hoc.Refinement;
import ptolemy.gui.ComponentDialog;
import ptolemy.gui.Query;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.LibraryAttribute;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.MessageHandler;
import ptolemy.util.StringUtilities;
import ptolemy.vergil.actor.ActorEditorGraphController;
import ptolemy.vergil.actor.ActorGraphModel;
import ptolemy.vergil.basic.AbstractBasicGraphModel;
import ptolemy.vergil.basic.EditorDropTarget;
import ptolemy.vergil.toolbox.FigureAction;
import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.graph.GraphController;
import diva.graph.GraphModel;
import diva.graph.GraphPane;
import diva.graph.JGraph;

//////////////////////////////////////////////////////////////////////////
//// MultiCompositeGraphFrame

/**
 This is a graph editor frame for actors containing composite actors.
 
 FIXME: this class needs a better name. MultiCompositeActor
 contains Refinements and mirrors its ports, whereas this class
 provides graph views for composites containing other composites. 

 @author Daniel Crawl, Possibly based on CaseGraphFrame by Edward A. Lee.
 @version $Id: MultiCompositeGraphFrame.java 30737 2012-09-21 22:42:51Z crawl $
 */
public class MultiCompositeGraphFrame extends KeplerGraphFrame implements ChangeListener {
    /** Construct a frame associated with the specified composite actor.
     *  After constructing this, it is necessary
     *  to call setVisible(true) to make the frame appear.
     *  This is typically done by calling show() on the controlling tableau.
     *  This constructor results in a graph frame that obtains its library
     *  either from the model (if it has one) or the default library defined
     *  in the configuration.
     *  @see Tableau#show()
     *  @param entity The model to put in this frame.
     *  @param tableau The tableau responsible for this frame.
     */
    public MultiCompositeGraphFrame(CompositeActor entity, Tableau tableau) {
        this(entity, tableau, null);
    }

    /** Construct a frame associated with the specified composite actor.
     *  After constructing this, it is necessary
     *  to call setVisible(true) to make the frame appear.
     *  This is typically done by calling show() on the controlling tableau.
     *  This constructor results in a graph frame that obtains its library
     *  either from the model (if it has one), or the <i>defaultLibrary</i>
     *  argument (if it is non-null), or the default library defined
     *  in the configuration.
     *  @see Tableau#show()
     *  @param entity The model to put in this frame.
     *  @param tableau The tableau responsible for this frame.
     *  @param defaultLibrary An attribute specifying the default library
     *   to use if the model does not have a library.
     */
    public MultiCompositeGraphFrame(CompositeActor entity, Tableau tableau,
            LibraryAttribute defaultLibrary) {
        super(entity, tableau, defaultLibrary);

        _composite = entity;

        // Override the default help file.
        // FIXME
        // helpFile = "ptolemy/configs/doc/vergilFsmEditorHelp.htm";
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Get the controller for a model contained in this frame. */
    public GraphController getControllerForModel(NamedObj model) {
        int tabCount = _tabbedPane.getTabCount();
        for(int i = 0; i < tabCount; i++) {
            GraphPane pane = ((JGraph) _tabbedPane.getComponentAt(i)).getGraphPane();
            if(((AbstractBasicGraphModel)pane.getGraphModel()).getPtolemyModel() == model) {
                return pane.getGraphController();
            }
        }
        return null;
    }

    /** Return the model associated with the selected frame. If no frame
     *  is selected, returns null.
     */
    public NamedObj getSelectedModel() {
        if (_tabbedPane != null) {
            Component tab = _tabbedPane.getSelectedComponent();
            if (tab instanceof JGraph) {
                GraphPane pane = ((JGraph) tab).getGraphPane();
                return ((AbstractBasicGraphModel)pane.getGraphModel()).getPtolemyModel();
            }
        }
        return null;
    }
        
    /** Open the container, if any, of the entity.
     *  If this entity has no container, then do nothing.
     */
    @Override
    public void openContainer() {
        // Method overridden since the parent will go from the refinement to
        // the composite, which is where we were in the first place.
        if (_composite != _composite.toplevel()) {
            try {
                Configuration configuration = getConfiguration();
                // FIXME: do what with the return value?
                configuration.openInstance(_composite.getContainer());
            } catch (Throwable throwable) {
                MessageHandler.error("Failed to open container", throwable);
            }
        }
    }

    /** React to a change in the state of the tabbed pane.
     *  @param event The event.
     */
    @Override
    public void stateChanged(ChangeEvent event) {
        Object source = event.getSource();
        if (source instanceof JTabbedPane) {
            Component selected = ((JTabbedPane) source).getSelectedComponent();
            if (selected instanceof JGraph) {
                setJGraph((JGraph) selected);
                selected.requestFocus();
                
                //System.out.println(source);
                
                // notify any listeners that we've changed selected componentsa
                
                // this cast to ActorGraphModel is safe since that's what
                // we create in _createGraphPane()
                NamedObj model = ((AbstractBasicGraphModel) ((JGraph) selected)
                        .getGraphPane().getGraphModel()).getPtolemyModel();
                StateChangeMonitor.getInstance().notifyStateChange(
                        new MultiCompositeStateChangeEvent(
                                (JTabbedPane) source, this, model));
            }
            if (_graphPanner != null) {
                _graphPanner.setCanvas((JGraph) selected);
                _graphPanner.repaint();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Create a new graph pane. Note that this method is called in
     *  constructor of the base class, so it must be careful to not reference
     *  local variables that may not have yet been created.
     *  This overrides the base class to create a specialized
     *  graph controller (an inner class).
     *  @param entity The object to be displayed in the pane.
     *  @return The pane that is created.
     */
    @Override
    protected GraphPane _createGraphPane(NamedObj entity) {
        
        ActorEditorGraphController controller;

        // see if we've already created the controller
        if(_controller == null) {
            _controller = new MultiCompositeController();
            controller = _controller;
        } else {
            //controller = new ActorEditorGraphController();
            controller = new MultiCompositeController();    
        }
        
        controller.setConfiguration(getConfiguration());
        controller.setFrame(this);

        // record the new graph model so it can be returned in
        // MultiCompositeController.getGraphModel(), which is
        // eventually called in new GraphPane().
        // see the comments for MultiCompositeController.getGraphModel().
        _creatingGraphModel = new ActorGraphModel(entity);
        GraphPane graphPane = new GraphPane(controller, _creatingGraphModel);
        _creatingGraphModel = null;
        return graphPane;
    }

    /** Create the component that goes to the right of the library.
     *  NOTE: This is called in the base class constructor, before
     *  things have been initialized. Hence, it cannot reference
     *  local variables.
     *  @param entity The entity to display in the component.
     *  @return The component that goes to the right of the library.
     */
    @Override
    protected JComponent _createRightComponent(NamedObj entity) {
        if (!(entity instanceof CompositeActor)) {
            return super._createRightComponent(entity);
        }
        _tabbedPane = new JTabbedPane();
        _tabbedPane.addChangeListener(this);
        Iterator<?> compositeIterator = ((CompositeEntity) entity).entityList(CompositeEntity.class)
                .iterator();
        boolean first = true;
        while (compositeIterator.hasNext()) {
            CompositeEntity composite = (CompositeEntity) compositeIterator.next();
            JGraph jgraph = _addTabbedPane(composite, false);
            // The first JGraph is the one with the focus.
            if (first) {
                first = false;
                setJGraph(jgraph);
            } else {
                ((MultiCompositeController) _controller)._addHotKeys(jgraph);
            }
        }
        return _tabbedPane;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Add a tabbed pane for the specified composite.
     *  @param composite The composite.
     *  @param newPane True to add the pane prior to the last pane.
     *  @return The pane.
     */
    protected JGraph _addTabbedPane(CompositeEntity composite, boolean newPane) {
        GraphPane pane = _createGraphPane(composite);
        pane.getForegroundLayer().setPickHalo(2);
        pane.getForegroundEventLayer().setConsuming(false);
        pane.getForegroundEventLayer().setEnabled(true);
        pane.getForegroundEventLayer().addLayerListener(new LayerAdapter() {
            /** Invoked when the mouse is pressed on a layer
             * or figure.
             */
            public void mousePressed(LayerEvent event) {
                Component component = event.getComponent();

                if (!component.hasFocus()) {
                    component.requestFocus();
                }
            }
        });
        JGraph jgraph = new JGraph(pane);
        String name = composite.getName();
        jgraph.setName(name);
        int index = _tabbedPane.getComponentCount();
        // Put before the default pane, unless this is the default.
        if (newPane) {
            index--;
        }
        _tabbedPane.add(jgraph, index);
        _tabbedPane.setTitleAt(index, composite.getDisplayName());
        jgraph.setBackground(BACKGROUND_COLOR);
        // Create a drop target for the jgraph.
        // FIXME: Should override _setDropIntoEnabled to modify all the drop targets created.
        new EditorDropTarget(jgraph);
        
        ModelToFrameManager m2fm = ModelToFrameManager.getInstance();
        m2fm.add(composite, this);

        return jgraph;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** The CompositeActor actor displayed by this frame. */
    protected CompositeActor _composite;
    
    /** The tabbed pane for composites. */
    protected JTabbedPane _tabbedPane;

    ///////////////////////////////////////////////////////////////////
    ////                     public inner classes                  ////

    /** Class implementing the Add Refinement menu command. */
    public abstract class AddRefinementAction extends FigureAction {

        /** Create an action. 
         * @param title the window title
         * @param refinementTypeName the type of sub-workflow to add
         * @param nameQuery the string displayed in the dialog
         */
        public AddRefinementAction(String title, String refinementTypeName,
                String nameQuery) {
            super(title);
            _title = title;
            _refinementTypeName = refinementTypeName;
            _nameQuery = nameQuery;
            putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_A));
        }
        
        ///////////////////////////////////////////////////////////////////////////////
        ////                            public methods                             ////

        /** Perform the action. */
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            // Dialog to ask for a refinement name.
            Query query = new Query();
            query.addLine(_refinementTypeName, _nameQuery, "");
            ComponentDialog dialog = new ComponentDialog(MultiCompositeGraphFrame.this,
                    _title, query);
            if (dialog.buttonPressed().equals("OK")) {
                final String pattern = query.getStringValue(_refinementTypeName);
                // NOTE: We do not use a TransitionRefinement because we don't
                // want the sibling input ports that come with output ports.
                
                // FIXME: refinementClassName() should be a method of MultiCompositeActor
                //String refinementClassName = _case.refinementClassName();
                String refinementClassName = "ptolemy.actor.lib.hoc.Refinement";
                
                String moml = "<entity name=\""
                        + StringUtilities.escapeForXML(pattern) + "\" class=\""
                        + refinementClassName + "\"/>";

                // The following is, regrettably, copied from ModalTransitionController.
                MoMLChangeRequest change = new MoMLChangeRequest(this, _composite,
                        moml) {
                    protected void _execute() throws Exception {
                        super._execute();

                        
                        // Mirror the ports of the container in the refinement.
                        // Note that this is done here rather than as part of
                        // the MoML because we have set protected variables
                        // in the refinement to prevent it from trying to again
                        // mirror the changes in the container.
                        Refinement entity = (Refinement) _composite
                                .getEntity(pattern);

                        // Get the initial port configuration from the container.
                        Iterator<?> ports = ((CompositeActor)entity.getContainer()).portList().iterator();
                        Set<Port> portsToMirror = new HashSet<Port>();
                        while (ports.hasNext()) {
                            Port port = (Port) ports.next();
                            
                            // see if we should mirror the port
                            if(mirrorPort(port)) {
                                portsToMirror.add(port);
                            }
                        }
                        
                        MultiCompositeActor.mirrorContainerPortsInRefinement(entity, portsToMirror);

                        JGraph jgraph = _addTabbedPane(entity, true);
                        ((MultiCompositeController) _controller)._addHotKeys(jgraph);
                    }
                };

                _composite.requestChange(change);
            }
        }
        
        /** Returns true if port should be mirrored. */
        public abstract boolean mirrorPort(Port port);
        
        ///////////////////////////////////////////////////////////////////
        ////                         private variables                 ////
        
        /** The type of the sub-workflow. */
        private String _refinementTypeName;
        
        /** The string displayed in the dialog. */
        private String _nameQuery;

        /** The title of the window. */
        private String _title;
    }

    /** Specialized graph controller that handles multiple graph models. */
    public class MultiCompositeController extends ActorEditorGraphController {
        /** Override the base class to select the graph model associated
         *  with the selected pane.
         */
        public GraphModel getGraphModel() {
            
            // NOTE: during the creation of a new pane, we should NOT return the
            // controller of the first, i.e., selected, composite. if the
            // controller of the first composite is different from the rest, e.g.,
            // an FSM model, it will result in an error, such as the controller
            // trying to find an element contained in the other component.
            // 
            // instead, when we're creating a new pane, use the newly created
            // model in _creatingGraphModel.
            //
            // see if we're in createGraphPane()
            if(_creatingGraphModel != null) {
                // use the newly created model
                return _creatingGraphModel;
            } else if (_tabbedPane != null) {
                // use the model in the selected tab
                Component tab = _tabbedPane.getSelectedComponent();
                if (tab instanceof JGraph) {
                    GraphPane pane = ((JGraph) tab).getGraphPane();
                    return pane.getGraphModel();
                }
            }

            // Fallback position.
            return super.getGraphModel();
        }

        /** Add hot keys to the actions in the given JGraph.
         *
         *  @param jgraph The JGraph to which hot keys are to be added.
         */
        protected void _addHotKeys(JGraph jgraph) {
            super._addHotKeys(jgraph);
        }
    }

    /** Class implementing the Remove Refinement menu command. */
    public class RemoveRefinementAction extends FigureAction {

        /** Create a tabbed composite action with a label. */
        public RemoveRefinementAction(String refinementTypeName,
                String refinementTypePluralName) {
            super("Remove " + refinementTypeName);
            _refinementTypeName = refinementTypeName;
            _refinementTypePluralName = refinementTypePluralName;
            putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_R));
        }
        
        ///////////////////////////////////////////////////////////////////////////////
        ////                            public methods                             ////

        /** Perform the action. */
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            // Dialog to ask for a refinement name.
            Query query = new Query();
            List<?> refinements = _composite.entityList(Refinement.class);
            if (refinements.size() < 2) {
                MessageHandler.error("No " + _refinementTypePluralName + " to remove.");
            } else {
                String[] refinementNames = new String[refinements.size() - 1];
                Iterator<?> refinementIterator = refinements.iterator();
                int i = 0;
                while (refinementIterator.hasNext()) {
                    String name = ((Nameable) refinementIterator.next()).getName();
                    if (!name.equals("default")) {
                        refinementNames[i] = name;
                        i++;
                    }
                }
                String title = "Remove " + _refinementTypeName;
                query.addChoice(_refinementTypeName, title, refinementNames,
                        refinementNames[0]);
                ComponentDialog dialog = new ComponentDialog(
                        MultiCompositeGraphFrame.this, title, query);
                if (dialog.buttonPressed().equals("OK")) {
                    final String name = query.getStringValue(_refinementTypeName);
                    String moml = "<deleteEntity name=\""
                            + StringUtilities.escapeForXML(name) + "\"/>";

                    // The following is, regrettably, copied from ModalTransitionController.
                    MoMLChangeRequest change = new MoMLChangeRequest(this,
                            _composite, moml) {
                        protected void _execute() throws Exception {
                            super._execute();
                            // Find the tabbed pane that matches the name and remove it.
                            int count = _tabbedPane.getTabCount();
                            for (int i = 0; i < count; i++) {
                                if (name.equals(_tabbedPane.getTitleAt(i))) {
                                    _tabbedPane.remove(i);
                                    break;
                                }
                            }
                        }
                    };
                    _composite.requestChange(change);
                }
            }
        }

        ///////////////////////////////////////////////////////////////////
        ////                         private variables                 ////

        /** The type of sub-workflow. */
        private String _refinementTypeName;
        
        /** The plural of the type of sub-workflow. */
        private String _refinementTypePluralName;

    }
    
    /** This is set in _createGraphPane() when creating the new GraphPane.
     *  Otherwise, it is null. See the comments for
     *  MultiCompositeController.getGraphModel().
     */
    private GraphModel _creatingGraphModel;
}