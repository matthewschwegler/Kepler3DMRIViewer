/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.cache;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 *
 */
public class DataCacheGetActor extends TypedAtomicActor {
	public TypedIOPort inputObjectName = new TypedIOPort(this,
			"inputObjectName", true, false);
	public TypedIOPort inputObjectType = new TypedIOPort(this,
			"inputObjectType", true, false);
	public TypedIOPort trigger = new TypedIOPort(this, "trigger", true, false);
	public TypedIOPort outputCacheID = new TypedIOPort(this, "outputCacheID",
			false, true);
	public TypedIOPort foundInCache = new TypedIOPort(this, "foundInCache",
			false, true);
	public Parameter outputCacheIDTokenProductionRate;
	private DataCacheManager cacheManager;

	/**
   *
   */
	public DataCacheGetActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		inputObjectName.setTypeEquals(BaseType.STRING);
		inputObjectName.setMultiport(true);
		inputObjectType.setTypeEquals(BaseType.STRING);
		inputObjectType.setMultiport(true);
		outputCacheID.setTypeEquals(BaseType.STRING);
		foundInCache.setTypeEquals(BaseType.BOOLEAN);
		outputCacheIDTokenProductionRate = new Parameter(outputCacheID,
				"tokenProductionRate");
		outputCacheIDTokenProductionRate.setExpression("0");
		cacheManager = DataCacheManager.getInstance();
	}

	/**
	 * Notify this entity that the links to the specified port have been
	 * altered. This sets the production rate of the output port and notifies
	 * the director that the schedule is invalid, if there is a director.
	 * 
	 * @param port
	 */
	public void connectionsChanged(Port port) {
		super.connectionsChanged(port);
		if (port == inputObjectName) {
			try {
				outputCacheIDTokenProductionRate.setToken(new IntToken(
						inputObjectName.getWidth()));
				// NOTE: schedule is invalidated automatically already
				// by the changed connections.
			} catch (IllegalActionException ex) {
				throw new InternalErrorException(this, ex,
						"Failed to set the token production rate for the outputCacheID port.");
			}
		}
	}

	/**
   *
   */
	public void initialize() throws IllegalActionException {
	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
   *
   */
	public void fire() throws IllegalActionException {
		super.fire();
		if (trigger.getWidth() > 0 && !trigger.hasToken(0)) { // make sure we're
																// ready to fire
			return;
		}

		// find out how many files we're getting from the cache
		int inputObjectNameWidth = inputObjectName.getWidth();

		for (int i = 0; i < inputObjectNameWidth; i++) { // loop through each
															// input on the port
			// get the input file name
			StringToken inputObjectNameToken = (StringToken) inputObjectName
					.get(i);
			// get the resource type of the file
			StringToken inputObjectTypeToken = (StringToken) inputObjectType
					.get(i);

			try { // try to get the file
				DataCacheFileObject fileItem = cacheManager.getFile(
						inputObjectNameToken.stringValue(),
						inputObjectTypeToken.stringValue());
				outputCacheID.broadcast(new StringToken(fileItem.getFile()
						.getAbsolutePath()));
				foundInCache.broadcast(new BooleanToken(true));
			} catch (NullPointerException npe) { // if it is null, return 0 to
													// show that the cache
				// doesn't include the current file
				outputCacheID.broadcast(new StringToken("0"));
				foundInCache.broadcast(new BooleanToken(false));
			} catch (Exception e) {
				throw new IllegalActionException(
						"Error getting item from cache: " + e.getMessage());
			}
		}
	}
}