/* An actor whose execution is specified by Java code.

Copyright (c) 2014 The Regents of the University of California.
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

import java.lang.reflect.Field;

import javax.script.ScriptException;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/** An actor whose execution is defined by Java code.
 *  The code can be edited by double-clicking on the actor.   
 *  
 *  <p>The following example defines an actor in Java that
 *  computes factorials:</p>
 *  
<pre>
1. import ptolemy.data.IntToken;
2.
3. public class Actor {
4.   public void fire() {
5.     int val = ((IntToken)super.in.get(0)).intValue();
6.
7.     if(val < 0) {
8.       error("Input must be non-negative");
9.     }
10.    int total = 1;
11.    while(val > 1) {
12.      total *= val;
13.      val--;
14.    } 
15.    out.broadcast(new IntToken(total));
16.  }
17.}
</pre>
 *
 *  <p>Line 1 imports the Java class used by the actor. Line 3 defines the
 *  Actor object. Lines 4-16 define the fire() method of the actor; this
 *  method is called each time this actor executes in the workflow. Line 5
 *  reads an integer from the input port called "input". A port or parameter
 *  called "foo" can be accessed in the Java code by simply using "foo".
 *  Lines 10-14 compute the factorial of the integer read from the port.
 *  Line 15 writes the result to the output port called "output".
 *  
 *  @author Daniel Crawl
 *  @version $Id: BeanShell.java 32809 2014-07-02 21:56:15Z crawl $
 */
public class BeanShell extends ScriptEngineActor {

    public BeanShell(Workspace workspace) {
        super(workspace);
    }

    public BeanShell(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        _invokeMethodWithReflection = true;
        
        language.setToken("beanshell");
        
        script.setExpression("public class Actor {\n" +
                "  public void fire() {\n" +
                "     System.out.println(\"in fire\");\n" +
                "  }\n" +
                "}\n");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////
    
    /** Put the given object to the actor instance in the script.
     *  @param name the name of the object to put.
     *  @param object the object to put.
     */
    @Override
    protected void _putObjectToActorInstance(String name, Object object) throws ScriptException {
        
        // see if the actor class has this field
        Field field = null;
        try {
            field = _actorObject.getClass().getField(name);
        } catch(Exception e) {
            // do nothing
        }
        
        if(field == null) {
            // actor class does not have the field so put as global            
            String globalName = "_yyy_" + name;
            _engine.put(globalName, object);
            _engine.eval(object.getClass().getName() + " " + name + " = " + globalName);
        } else {
            try {
                // set the field in the actor instance.
                field.set(_actorObject, object);
            } catch (Exception e) {
                throw new ScriptException("Error setting port/parameter " + name + ": " + e.getMessage());
            }
        }
    }

}
