/*
 * Copyright (c) 1997-2010 The Regents of the University of California.
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

package org.kepler.domains;

import java.util.Iterator;

import org.sdm.spa.WebService;

import ptolemy.actor.Actor;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.sched.Firing;
import ptolemy.actor.sched.Schedule;
import ptolemy.actor.sched.Scheduler;
import ptolemy.domains.sdf.kernel.SDFDirector;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InvalidStateException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// SDFDirector for Web Services

/**
 * The SDF4WS is an SDFDirector for the domain of WebServices. SDF4WS provides
 * the abilty to monitor WebService actors in a workflow, catching their
 * exceptions and error messages. Additional domain-specific operations can be
 * "three re-trials" of failing web services before finally switching to another
 * server providing the same service (if available) OR determining if the web
 * service operation failure was caused due to invalid user input or internal
 * web service errors.
 * 
 * Based on the Director for the synchronous dataflow (SDF) model of
 * computation.
 * 
 * <h1>SDF4WS overview</h1> The Synchronous Dataflow(SDF) for Web Services
 * director, like its parent SDF Director supports the efficient execution of
 * Dataflow graphs that lack control structures. SDF4WS is a director made
 * specifically for web services with added functionality. This director catches
 * possible exception from Web Service incase of a scenario and hence failures
 * in web service access.
 * 
 * <h2>More SDF Director Information</h2> Dataflow graphs that contain control
 * structures should be executed using the Process Networks(PN) domain instead.
 * SDF allows efficient execution, with very little overhead at runtime. It
 * requires that the rates on the ports of all actors be known before hand. SDF
 * also requires that the rates on the ports not change during execution. In
 * addition, in some cases (namely systems with feedback) delays, which are
 * represented by initial tokens on relations must be explicitly noted. SDF uses
 * this rate and delay information to determine the execution sequence of the
 * actors before execution begins. <h3>Schedule Properties</h3>
 * <ul>
 * <li>The number of tokens accumulated on every relation is bounded, given an
 * infinite number of executions of the schedule.
 * <li>Deadlock will never occur, given an infinite number of executions of the
 * schedule.
 * </ul>
 * <h1>Class comments</h1> An SDFDirector is the class that controls execution
 * of actors under the SDF domain. By default, actor scheduling is handled by
 * the SDFScheduler class. Furthermore, the newReceiver method creates Receivers
 * of type SDFReceiver, which extends QueueReceiver to support optimized gets
 * and puts of arrays of tokens.
 * <p>
 * Actors are assumed to consume and produce exactly one token per channel on
 * each firing. Actors that do not follow this convention should set the
 * appropriate parameters on input and output ports to declare the number of
 * tokens they produce or consume. See the
 * 
 * @link ptolemy.domains.sdf.kernel.SDFScheduler for more information. The @link
 *       ptolemy.domains.sdf.lib.SampleDelay actor is usually used in a model to
 *       specify the delay across a relation.
 *       <p>
 *       The <i>allowDisconnectedGraphs</i> parameter of this director
 *       determines whether disconnected graphs are permitted. A model may have
 *       two or more graphs of actors that are not connected. The schedule can
 *       jump from one graph to another among the disconnected graphs. There is
 *       nothing to force the scheduler to finish executing all actors on one
 *       graph before firing actors on another graph. However, the order of
 *       execution within an graph should be correct. Usually, disconnected
 *       graphs in an SDF model indicates an error. The default value of the
 *       allowDisconnectedGraphs parameter is a BooleanToken with the value
 *       false.
 *       <p>
 *       The <i>iterations</i> parameter of this director corresponds to a limit
 *       on the number of times the director will fire its hierarchy before it
 *       returns false in postfire. If this number is not greater than zero,
 *       then no limit is set and postfire will always return true. The default
 *       value of the iterations parameter is an IntToken with value zero.
 *       <p>
 *       The <i>vectorizationFactor</i> parameter of this director sets the
 *       number of times that the basic schedule is executed during each firing
 *       of this director. This might allow the director to execute the model
 *       more efficiently, by combining multiple firings of each actor. The
 *       default value of the vectorizationFactor parameter is an IntToken with
 *       value one.
 * @see ptolemy.domains.sdf.kernel.SDFScheduler
 * @see ptolemy.domains.sdf.kernel.SDFReceiver
 * @author Nandita Mangal
 * @version $Id: SDF4WS.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 0.2
 * @Pt.ProposedRating Green (neuendor)
 * @Pt.AcceptedRating Green (neuendor)
 */
public class SDF4WS extends SDFDirector {
	/**
	 * Construct a director in the default workspace with an empty string as its
	 * name. The director is added to the list of objects in the workspace.
	 * Increment the version number of the workspace.
	 * 
	 * The SDFD4WS will have a default scheduler of type SDFScheduler.
	 * 
	 * @exception IllegalActionException
	 *                If the name has a period in it, or the director is not
	 *                compatible with the specified container.
	 * @exception NameDuplicationException
	 *                If the container already contains an entity with the
	 *                specified name.
	 */
	public SDF4WS() throws IllegalActionException, NameDuplicationException {

		super();
		// Parameters inherited from SDF Director.

	}

	/**
	 * Construct a director in the workspace with an empty name. The director is
	 * added to the list of objects in the workspace. Increment the version
	 * number of the workspace. The SDFDirector will have a default scheduler of
	 * type SDFScheduler.
	 * 
	 * @param workspace
	 *            The workspace for this object.
	 * @exception IllegalActionException
	 *                If the name has a period in it, or the director is not
	 *                compatible with the specified container.
	 * @exception NameDuplicationException
	 *                If the container already contains an entity with the
	 *                specified name.
	 */
	public SDF4WS(Workspace workspace) throws IllegalActionException,
			NameDuplicationException {

		super(workspace);

	}

	/**
	 * Construct a director in the given container with the given name. The
	 * container argument must not be null, or a NullPointerException will be
	 * thrown. If the name argument is null, then the name is set to the empty
	 * string. Increment the version number of the workspace. The SDFD4WS
	 * Director will have a default scheduler of type SDFScheduler.
	 * 
	 * @param container
	 *            Container of the director.
	 * @param name
	 *            Name of this director.
	 * @exception IllegalActionException
	 *                If the director is not compatible with the specified
	 *                container. May be thrown in a derived class.
	 * @exception NameDuplicationException
	 *                If the container is not a CompositeActor and the name
	 *                collides with an entity in the container.
	 */
	public SDF4WS(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {

		super(container, name);
	}

	// ///////////////////////////////////////////////////////////////
	// // parameters ////

	// all parameters extended from SDF Directors.

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * BASIC SDF director functionality :Calculate the current schedule, if
	 * necessary, and iterate the contained actors in the order given by the
	 * schedule. No internal state of the director is updated during fire, so it
	 * may be used with domains that require this property, such as CT.
	 * <p>
	 * Iterating an actor involves calling the actor's iterate() method, which
	 * is equivalent to calling the actor's prefire(), fire() and postfire()
	 * methods in succession. If iterate() returns NOT_READY, indicating that
	 * the actor is not ready to execute, then an IllegalActionException will be
	 * thrown. The values returned from iterate() are recorded and are used to
	 * determine the value that postfire() will return at the end of the
	 * director's iteration.
	 * <p>
	 * 
	 * SDF4WS : This method overridden by web service to perform additional
	 * domain-specific operations such as three re-trials of failing web
	 * services before finally switching to another server providing the same
	 * service (if available).
	 * 
	 * @exception IllegalActionException
	 *                If any actor executed by this actor return false in
	 *                prefire.
	 * @exception InvalidStateException
	 *                If this director does not have a container.
	 */
	public void fire() throws IllegalActionException {

		int returnValue = 0;

		Scheduler scheduler = getScheduler();
		if (scheduler == null) {
			throw new IllegalActionException("Attempted to fire "
					+ "system with no scheduler");
		}
		// This will throw IllegalActionException if this director
		// does not have a container.
		Schedule schedule = scheduler.getSchedule();
		Iterator firings = schedule.firingIterator();

		while (firings.hasNext() && !_stopRequested) {

			Firing firing = (Firing) firings.next();
			Actor actor = (Actor) firing.getActor();
			int iterationCount = firing.getIterationCount();

			if (_debugging) {
				_debug(new FiringEvent(this, actor, FiringEvent.BEFORE_ITERATE,
						iterationCount));
			}

			// get the current actor in the iteration.
			actorCurrent = actor;

			// iterate the actor, catching any exceptions due to failed web
			// service access.
			try {

				returnValue = actor.iterate(iterationCount);

			} catch (Exception ex) {
				if (actorCurrent instanceof WebService
						&& ex.getMessage().equals(
								"\nWebService WSDL Not Responding.")) {

					GraphicalMessageHandler
							.message("\nSDF4WS re-trying web service access");

					// re-try accessing the web service three times.
					int reTrialCount = 3;
					boolean webServiceSuccess = _reFireWebService(3);
					if (webServiceSuccess == false) {
						GraphicalMessageHandler
								.message("\nWebService WSDL failed to respond in "
										+ reTrialCount
										+ " trials."
										+ "\nSDF4WS will try to access web service via different server.");

						// TO BE DONE: SDF4WS will try to access web service
						// from another server if available.

					} else {
						GraphicalMessageHandler
								.message("\nWebService WSDL access successfull!");
						returnValue = actorCurrentReturnValue;

					}

				}
			}

			if (returnValue == STOP_ITERATING) {

				// _postfireReturns = false;
			} else if (returnValue == NOT_READY) {
				throw new IllegalActionException(this, (ComponentEntity) actor,
						"Actor " + "is not ready to fire.");
			}

			if (_debugging) {
				_debug(new FiringEvent(this, actor, FiringEvent.AFTER_ITERATE,
						iterationCount));
			}

		}

	}// end of fire

	// ///////////////////////////////////////////////////////////////////////////
	// // private methods ////

	private boolean _reFireWebService(int retrialCount) {
		int counter = 0;
		boolean webServiceSuccess = false;
		for (counter = 0; counter < retrialCount; counter++) {
			// iterate the actor retrialCount number of times.
			try {

				actorCurrent.stopFire();
				actorCurrent.prefire();
				actorCurrent.fire();
				// actor was iterated successfully w/o exception.
				// get actor's return Value
				boolean returnValue = actorCurrent.postfire();
				if (returnValue == false)
					actorCurrentReturnValue = STOP_ITERATING;

				webServiceSuccess = true;

				return webServiceSuccess;
			} catch (Exception e) {
				// do nothing but try again
				webServiceSuccess = false;
			}

		}
		return webServiceSuccess;

	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////
	private int _iterationCount = 0;
	private Actor actorCurrent;
	private int actorCurrentReturnValue = 0;

}