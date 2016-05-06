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

import ptolemy.actor.TypedAtomicActor;

public interface MultipleTabDisplayInterface {
    void display(String displayName, String content, boolean isAddNewLines);

    void initialize(TypedAtomicActor self);

    void place(Container container);

    void prepare();
}
