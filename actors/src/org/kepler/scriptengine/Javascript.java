/* An actor whose execution is specified by a Javascript script.

Copyright (c) 2013 The Regents of the University of California.
All rights reserved.
Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the above
copyright notice and the following two paragraphs appear in all copies
of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

*/
package org.kepler.scriptengine;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/** An actor whose execution is defined by a Javascript script.
 *  The script can be edited by double-clicking on the actor.   
 *  
 *  <p>The following example defines an actor in Javascript that
 *  computes factorials:</p>
<pre>
1.  importClass(Packages.ptolemy.data.type.BaseType);
2.  importClass(Packages.ptolemy.data.IntToken);
3. 
4.  function Actor() {};
5. 
6.  // implement the "fire" function to be called each time the actor executes
7.  Actor.prototype.fire = function() {
8. 
9.    // read from the "input" port
10.   var val = this.input.get(0).intValue();
11.   
12.   if(val < 0) {
13.     error = "Input must be greater than or equal to 0";
14.   } else {
15.     var total = 1;
16.     while(val > 1) {
17.       total *= val;
18.       val--;
19.     }
20.     // write the result to the "output" port
21.     this.output.broadcast(new IntToken(total));
22.   }
23. };
24. 
25. // define the "preinitialize" function to be called once when the workflow starts.
26. Actor.prototype.preinitialize = function() {
27.   // set the data types for the "input" and "output" ports.
28.   this.input.setTypeEquals(BaseType.INT);
29.   this.output.setTypeAtLeast(this.input);
30. };
</pre>
 *
 *  <p>Lines 1 and 2 import the Java classes used by the actor. Line 4 defines the
 *  Actor object and is required in every Javascript. Lines 7-23 define the
 *  fire() method of the actor; this method is called each time this actor
 *  executes in the workflow. Line 10 reads an integer from the input port
 *  called "input". A port or parameter called "foo" can be accessed in the
 *  Javascript script by using "this.foo". Lines 12-19 compute the factorial
 *  of the integer read from the port. Line 21 writes the result to the output
 *  port called "output". Lines 26-29 define the preinitialize() method of this
 *  actor, which is executed once when the workflow starts. Lines 28-29 set
 *  the types for the input and output ports.</p>
 *  
 *  @author Daniel Crawl
 *  @version $Id: Javascript.java 32710 2014-05-07 23:39:45Z crawl $
 */
public class Javascript extends ScriptEngineActor {

    /** Construct a new Javascript for a specified workspace. */
    public Javascript(Workspace workspace) {
        super(workspace);
    }

    /** Construct a new Javascript with the given container and name. */
    public Javascript(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
        // use the rhino engine that is not bundled with the JDK
        // the engine is in actors/lib/jar/script-engine/js-engine.jar, and
        // uses the RhinoScript jar in Ptolemy $PTII/lib/js.jar.
        language.setToken("rhino-nonjdk");
        
        // create a simple script that prints something when
        // the actor fires
        script.setExpression("// this prints each time the actor executes\n" +
                "function Actor() {};\n" +
                "\n" +
                "Actor.prototype.fire = function() {\n" +
                "   println(\"in fire\");\n" +
                "};");
    }
}