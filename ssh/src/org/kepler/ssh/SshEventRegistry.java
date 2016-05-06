/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:24:32 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31129 $'
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

package org.kepler.ssh;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class provides a registry to store subscriptions to SshEvents.
 * 
 * <p>
 * 
 * @author Norbert Podhorszki
 */

public class SshEventRegistry {

	/* Singleton object */
	public final static SshEventRegistry instance = new SshEventRegistry();

	private SshEventRegistry() {
	}

	/**
	 * Append a listener to the current set of ssh event listeners. If the
	 * listener is already in the set, it will not be added again.
	 * 
	 * @param listener
	 *            The listener to which to send SshEvent messages.
	 * @see #removeListener(SshEventListener)
	 */
	public void addListener(SshEventListener listener) {
		// NOTE: This has to be synchronized to prevent
		// concurrent modification exceptions.
		synchronized (_listeners) {
			if (_listeners.contains(listener)) {
				return;
			} else {
				_listeners.add(new WeakReference(listener));
			}

			_hasListeners = true;
		}
	}

	/**
	 * Send a SshEvent to all listeners.
	 * 
	 * @param event
	 *            The event.
	 */
	protected final void notifyListeners(SshEvent event) {
		if (_hasListeners) {
			Iterator listeners = _listeners.iterator();

			while (listeners.hasNext()) {
				WeakReference<SshEventListener> listenerWf = (WeakReference<SshEventListener>) listeners.next();
//				((SshEventListener) listeners.next()).sshEvent(event);
				SshEventListener listener = (SshEventListener)listenerWf.get();
				if (listener != null)
					listener.sshEvent(event);
			}
		}
	}

	/**
	 * Unregister an event listener. If the specified listener has not been
	 * previously registered, then do nothing.
	 * 
	 * @param listener
	 *            The listener to remove from the list of listeners to which
	 *            SshEvent messages are sent.
	 * @see #addListener(SshEventListener)
	 */
	public void removeListener(SshEventListener listener) {
		// NOTE: This has to be synchronized to prevent
		// concurrent modification exceptions.
		synchronized (_listeners) {
			_listeners.remove(listener);

			if (_listeners.size() == 0) {
				_hasListeners = false;
			}
		}
	}

	/* listeners will be iterated on by Ssh classes */
//	private static LinkedList _listeners = new LinkedList();
	private static LinkedList<WeakReference<SshEventListener>> _listeners = new LinkedList<WeakReference<SshEventListener>>();
	private static boolean _hasListeners = false;

}