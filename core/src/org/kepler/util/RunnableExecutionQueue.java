/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-19 12:19:59 -0700 (Thu, 19 Jul 2012) $' 
 * '$Revision: 30239 $'
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
package org.kepler.util;

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.SwingUtilities;

import ptolemy.util.MessageHandler;

/** A queue that executes Runnables.
 * 
 *  @author Daniel Crawl
 *  @version $Id: RunnableExecutionQueue.java 30239 2012-07-19 19:19:59Z crawl $
 */

public class RunnableExecutionQueue {

    /** Submit a Runnable for execution. */
    public static void submit(Runnable runnable) {
        _queue.add(new QueueItem(runnable, false));
    }
    
    /** Submit a Runnable for execution in the Swing Thread. */
    public static void submitToSwing(Runnable runnable) {
        _queue.add(new QueueItem(runnable, true));
    }
    
    /** Execute all the submitted Runnables. */
    public static void execute() {
        QueueItem item;
        while((item = _queue.poll()) != null) {
            
            if(item.swing) {
                try {
                    SwingUtilities.invokeAndWait(item.r);
                } catch (Exception e) {
                    MessageHandler.error("Error executing runnable in Swing Thread.", e);
                }
            } else {
                item.r.run();
            }
        }
    }
    
    /** An item for the queue. */
    private static class QueueItem {
        
        QueueItem(Runnable r, boolean swing) {
            this.r = r;
            this.swing = swing;
        }
        
        public Runnable r;
        public boolean swing;
    }
    
    /** A queue of items to be executed. */
    private static Queue<QueueItem> _queue = new LinkedList<QueueItem>(); 
}
