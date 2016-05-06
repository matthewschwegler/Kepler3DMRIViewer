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

package org.kepler.sms.actors;

import java.util.Iterator;
import java.util.Vector;

import org.kepler.sms.SMSServices;
import org.kepler.sms.SemanticType;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.kernel.util.NamedObj;

/**
 * SIMPLE MERGE ALGORITHM:
 * 
 * ComputeMerge() 1. Let Matches = {} 2. Let Conflicts = {} 3. foreach A in
 * MergeActors 4. foreach output port P of A involved in merge 5.
 * ComputeMatches(A, P, Matches, Conflicts) 6. ComputeMappings(Matches) 7.
 * ComputeTargetSemTypes(matches);
 * 
 * ComputeMatches(A, P, Matches, Conflicts) 1. Let Actors = MergeActors - {A} 2.
 * foreach A' in Actors and !HasConflict(P, A', Conflicts) 3. foreach output
 * port P' of A' involved in merge 4. if( semType(P) subtypeof semType(P') ) 5.
 * if( HasMatch(P', A, Matches) ) 6. AddConflict(P, A', Matches, Conflicts) 7.
 * else 8. AddMatch(P, P', Matches) ... here we can add a condition for
 * conversion functions ...
 * 
 * ComputeMappings(Matches) 1. Let MergeSets = PartitionMatches(Matches) 2.
 * foreach S in MergeSets 3. Let P' = CreateNewOutputPort() 4. foreach P in S 5.
 * AddMapping(P, P') 6. foreach A in MergeActors 7. foreach otuput port P of A
 * involved in merge 8. if( !InMatch(P, Matches) ) 9. Let P' =
 * CreateNewOutputPort() 10. AddMapping(P, P')
 */

public class SimpleComputeMergeAlgorithm {

	/**
     *
     */
	public SimpleComputeMergeAlgorithm(MergeActor mergeActor) {
		_mergeActor = mergeActor;
	}

	/**
     *
     */
	public void computeMerge() {
		Vector matches = new Vector();
		Vector conflicts = new Vector();
		Iterator actors = _mergeActor.getActors().iterator();
		while (actors.hasNext()) {
			NamedObj a = (NamedObj) actors.next();
			Iterator ports = _mergeActor.getActorPorts(a).iterator();
			while (ports.hasNext()) {
				IOPort p = (IOPort) ports.next();
				_computeMatches(a, p, matches, conflicts);
			}
		}

		System.out.print("COMPUTE MATCHES: ");
		_printMatches(matches);

		_computeMappings(matches);
		_computeTargetSemTypes(matches);
	}

	/**
     *
     */
	private void _computeMatches(NamedObj a, IOPort p, Vector matches,
			Vector conflicts) {
		Vector actors = new Vector();
		Iterator iter = _mergeActor.getActors().iterator();
		while (iter.hasNext()) {
			NamedObj actor = (NamedObj) iter.next();
			if (!actor.equals(a))
				actors.add(actor);
		}
		iter = actors.iterator();
		while (iter.hasNext()) {
			NamedObj ap = (NamedObj) iter.next();
			if (!_hasConflict(p, ap, conflicts)) {
				Iterator ports = _mergeActor.getActorPorts(ap).iterator();
				while (ports.hasNext()) {
					IOPort pp = (IOPort) ports.next();
					Vector<SemanticType> p_semtypes = SMSServices.getPortSemanticTypes(p);
					Vector<SemanticType> pp_semtypes = SMSServices.getPortSemanticTypes(pp);
					if (SMSServices.compatible(p_semtypes, pp_semtypes)) {
						System.out.println(p.getContainer().getName() + "."
								+ p.getName() + " is compatible with "
								+ pp.getContainer().getName() + "."
								+ pp.getName());
						if (_hasMatch(pp, a, matches))
							_addConflict(p, ap, matches, conflicts);
						else
							_addMatch(p, pp, matches);
					}
					// else if(SMSServices.compatible(pp_semtypes, p_semtypes))
					// {
					// System.out.println(pp.getContainer().getName() + "." +
					// pp.getName() +
					// " is compatible with " +
					// p.getContainer().getName() + "." + p.getName());
					// if(_hasMatch(pp, a, matches))
					// _addConflict(p, ap, matches, conflicts);
					// else
					// _addMatch(pp, p, matches);
					// }
				}
			}
		}
	}

	/**
     *
     */
	private void _computeMappings(Vector matches) {
		Vector mergeSets = _partitionMatches(matches);
		Iterator iter = mergeSets.iterator();
		while (iter.hasNext()) {
			Vector s = (Vector) iter.next();
			System.out.println("ADDING PARTITION");
			TypedIOPort pp = _createNewOutputPort();
			Iterator ports = s.iterator();
			while (ports.hasNext()) {
				IOPort p = (IOPort) ports.next();
				System.out.println("... ADDING PORT: "
						+ p.getContainer().getName() + "." + p.getName());
				_addMapping(p, pp);
			}
		}
		iter = _mergeActor.getActors().iterator();
		while (iter.hasNext()) {
			NamedObj a = (NamedObj) iter.next();
			Iterator ports = _mergeActor.getActorPorts(a).iterator();
			while (ports.hasNext()) {
				IOPort p = (IOPort) ports.next();
				// System.out.println("CHECKING a = " + a.getName() +
				// " and p = " + p.getName());
				if (!_inMatch(p, matches)) {
					TypedIOPort pp = _createNewOutputPort();
					_addMapping(p, pp);
				}
			}
		}
	}

	/**
     *
     */
	private void _addMapping(IOPort p, IOPort pp) {
		String name = _mergeActor.uniqueName("_merge");
		String actor = p.getContainer().getName();
		String port = p.getName();
		String target = pp.getName();
		try {
			SimpleMergeMapping m = new SimpleMergeMapping(_mergeActor, name,
					actor, port, target);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
     *
     */
	private boolean _inMatch(IOPort p, Vector matches) {
		Iterator iter = matches.iterator();
		while (iter.hasNext()) {
			IOPort[] match = (IOPort[]) iter.next();
			if (p.equals(match[0]) || p.equals(match[1]))
				return true;
		}
		return false;
	}

	/**
     *
     */
	private TypedIOPort _createNewOutputPort() {
		TypedIOPort p = null;
		try {
			p = new TypedIOPort(_mergeActor, _mergeActor.uniqueName("target"),
					false, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return p;
	}

	/**
     *
     */
	private Vector _partitionMatches(Vector matches) {
		Vector matches_copy = new Vector();
		Iterator iter = matches.iterator();
		while (iter.hasNext())
			matches_copy.add(iter.next());
		Vector partitions = new Vector();
		_partitionMatches(matches_copy, partitions);
		return partitions;
	}

	/**
     *
     */
	private void _partitionMatches(Vector matches, Vector partitions) {
		if (matches.isEmpty())
			return;
		IOPort[] match = (IOPort[]) matches.elementAt(0);
		boolean found = false;
		Iterator iter = partitions.iterator();
		while (!found && iter.hasNext()) {
			Vector partition = (Vector) iter.next();
			if (partition.contains(match[0]) || partition.contains(match[1])) {
				// add both to be sure
				if (!partition.contains(match[0]))
					partition.add(match[0]);
				if (!partition.contains(match[1]))
					partition.add(match[1]);
				found = true;
			}
		}
		matches.remove(match);
		if (!found) {
			Vector partition = new Vector();
			partition.add(match[0]);
			partition.add(match[1]);
			partitions.add(partition);
		}
		_partitionMatches(matches, partitions);
	}

	/**
     *
     */
	private void _computeTargetSemTypes(Vector matches) {

	}

	/**
     *
     */
	private boolean _hasConflict(IOPort p, NamedObj ap, Vector conflicts) {
		Iterator iter = conflicts.iterator();
		while (iter.hasNext()) {
			Object[] conflict = (Object[]) iter.next();
			if (p.equals(conflict[0]) && ap.equals(conflict[1]))
				return true;
		}
		return false;
	}

	/**
     *
     */
	private boolean _hasMatch(IOPort pp, NamedObj a, Vector matches) {
		Iterator iter = matches.iterator();
		while (iter.hasNext()) {
			IOPort[] match = (IOPort[]) iter.next();
			if (pp.equals(match[0]) && a.equals(match[1]))
				return true;
		}
		return false;
	}

	/**
     *
     */
	private void _addConflict(IOPort p, NamedObj ap, Vector matches,
			Vector conflicts) {
		Object[] conflict = { p, ap };
		if (!conflicts.contains(conflict))
			conflicts.add(conflict);
		// remove all <p,pp> or <pp,p> matches
		Iterator iter = matches.iterator();
		while (iter.hasNext()) {
			IOPort[] match = (IOPort[]) iter.next();
			if (p.equals(match[0])) {
				Iterator ports = _mergeActor.getActorPorts(ap).iterator();
				while (ports.hasNext()) {
					IOPort pp = (IOPort) ports.next();
					if (pp.equals(match[1]))
						matches.remove(match);
				}
			}
			if (p.equals(match[1])) {
				Iterator ports = _mergeActor.getActorPorts(ap).iterator();
				while (ports.hasNext()) {
					IOPort pp = (IOPort) ports.next();
					if (pp.equals(match[0]))
						matches.remove(match);
				}
			}
		}
	}

	/**
     *
     */
	private void _addMatch(IOPort p, IOPort pp, Vector matches) {
		IOPort[] match = { p, pp };
		Iterator iter = matches.iterator();
		while (iter.hasNext()) {
			IOPort[] m = (IOPort[]) iter.next();
			if (m[0].equals(p) && m[1].equals(pp))
				return;
		}
		matches.add(match);
	}

	/**
	 * Helper function for printing out a set of matches
	 */
	private void _printMatches(Vector matches) {
		Iterator iter = matches.iterator();
		System.out.print("{");
		while (iter.hasNext()) {
			IOPort[] match = (IOPort[]) iter.next();
			System.out.print("<" + match[0].getName() + ", "
					+ match[1].getName() + ">");
			if (iter.hasNext())
				System.out.print(", ");
		}
		System.out.println("}");
	}

	// //////////////////////////////////////////////////////////////////////////
	// PRIVATE MEMBERS

	private MergeActor _mergeActor;

}// SimpleComputeMergeAlgorithm