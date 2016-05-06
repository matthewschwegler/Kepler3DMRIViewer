/* Base exception for analyses.

 Copyright (c) 2003-2005 The University of Maryland.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF MARYLAND BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF MARYLAND HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF MARYLAND SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 MARYLAND HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 */
package ptolemy.graph.analysis;

//////////////////////////////////////////////////////////////////////////
//// AnalysisException

/**
 Base exception for non-checked exceptions in the analyses package.

 @since Ptolemy II 4.0
 @Pt.ProposedRating Red (shahrooz)
 @Pt.AcceptedRating Red (ssb)
 @author Shahrooz Shahparnia
 @version $Id: AnalysisException.java 68297 2014-02-05 00:35:55Z cxh $
 */
@SuppressWarnings("serial")
public class AnalysisException extends RuntimeException {
    /** The default constructor without arguments.
     */
    public AnalysisException() {
    }

    /** Constructor with a text description as argument.
     *
     *  @param message The text description of the exception.
     */
    public AnalysisException(String message) {
        super(message);
    }

    /** Constructor with a text description and cause as argument.
     *
     *  @param message The text description of the exception.
     *  @param cause The exception that caused this exception.
     */
    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
