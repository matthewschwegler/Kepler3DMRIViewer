/* An actor whose execution is specified by a Groovy script.

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

import javax.script.ScriptException;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/** An actor whose execution is defined by a Groovy script.
 *  The script can be edited by double-clicking on the actor.   
 *  
 *  <p>The following example defines an actor in Groovy that
 *  computes factorials:</p>
<pre>
1. import ptolemy.data.IntToken
2. 
3. // Groovy actor to compute factorials.
4. 
5. public class Actor {
6.     def fire() {
7.         int val = input.get(0).intValue()
8.         if(val < 0) {
9.             actor.error("Input must be non-negative.");
10.            return
11.        }
12.        int total = factorial(val)
13.        output.broadcast(new IntToken(total))
14.    }
15.
16.    def factorial = { n -> n == 0 ? 1 : n * factorial(n - 1) }
17.}
</pre>
 *
 *  <p>Line 1 imports the Java class used by the actor. Lines 5-17 define
 *  the Actor object; the actor object must be named "Actor" in Groovy scripts.
 *  Lines 6-14 define the fire() method of the actor, which is called each
 *  time this actor executes in the workflow. Line 7 reads an integer from the
 *  input port called "input". A port or parameter called "foo" can be accessed
 *  in the Groovy script by using the same name. Line 12 calls the closure
 *  defined on line 16 that recursively computes the factorial of the integer
 *  read from the port. Line 13 writes the result to the output port called
 *  "output".</p>
 *  
 *  @author Daniel Crawl
 *  @version $Id: Groovy.java 32535 2013-11-12 22:35:43Z crawl $
 */
public class Groovy extends ScriptEngineActor {

    /** Construct a new Groovy for a specified workspace. */
    public Groovy(Workspace workspace) {
        super(workspace);
    }

    /** Construct a new Groovy with the given container and name. */
    public Groovy(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
        language.setToken("groovy");
        
        script.setExpression("public class Actor {\n" +
                "  public void fire() {\n" +
                "     println(\"in fire\");\n" +
                "  }\n" +
                "}\n");
    }

    /** Put the given object to the actor instance in the script.
     *  @param name the name of the object to put.
     *  @param object the object to put.
     */
    @Override
    protected void _putObjectToActorInstance(String name, Object object) throws ScriptException {
        String globalName = "_yyy_" + name;
        _engine.put(globalName, object);
        _engine.eval(_ACTOR_INSTANCE_NAME + ".metaClass." + name + " = " + globalName);
    }
}
