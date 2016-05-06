/**
 *  '$RCSfile$'
 *  '$Author: barseghian $'
 *  '$Date: 2010-10-13 12:22:12 -0700 (Wed, 13 Oct 2010) $'
 *  '$Revision: 26060 $'
 *
 *  For Details:
 *  http://www.kepler-project.org
 *
 *  Copyright (c) 2010 The Regents of the
 *  University of California. All rights reserved. Permission is hereby granted,
 *  without written agreement and without license or royalty fees, to use, copy,
 *  modify, and distribute this software and its documentation for any purpose,
 *  provided that the above copyright notice and the following two paragraphs
 *  appear in all copies of this software. IN NO EVENT SHALL THE UNIVERSITY OF
 *  CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 *  OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 *  DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY
 *  DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
 *  SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.kepler.kar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;

/**
 * This singleton keeps a mapping of JFrames to KARFiles.
 */
public class KARManager {

	static private KARManager _instance = null;
	private Map<JFrame, KARFile> _mapping = new HashMap<JFrame, KARFile>();
	
	public KARManager() {
	
	}
	
	public synchronized static KARManager getInstance() {
		if (_instance == null) {
			_instance = new KARManager();
		}
		return _instance;
	}

	/**
	 * Add mapping of karFile to jFrame
	 * @param karFile
	 * @param tableau
	 */
	public synchronized void add(JFrame jFrame, KARFile karFile){
		if (jFrame == null && karFile == null){
			System.out.println("KARManager add WARNING adding null => null mapping");
		}else if (jFrame == null){
			System.out.println("KARManager add WARNING adding null =>"+karFile.getName()+" mapping");
		}else if (karFile == null){
			System.out.println("KARManager add WARNING adding "+jFrame.getName()+" => null mapping");
		}
		_mapping.put(jFrame, karFile);
		//debug();
	}
	
	public synchronized KARFile get(JFrame jFrame){
		return _mapping.get(jFrame);
	}
	
	public synchronized void remove(JFrame jFrame){
		_mapping.remove(jFrame);
		//debug();
	}
	
	public void debug(){
		System.out.println("------KARManager _mapping:");
		Iterator<JFrame> itr = _mapping.keySet().iterator();
		while (itr.hasNext()){
			JFrame jFrame = itr.next();
			KARFile karFile = _mapping.get(jFrame);
			System.out.println(jFrame.getName()+"=>"+karFile.getName());
		}
		System.out.println("------");
	}
}