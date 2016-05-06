/* An actor whose execution is specified by a JRuby script.

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

/** An actor whose execution is defined by a JRuby script.
 *  The script can be edited by double-clicking on the actor.   
 *  
 *  <p><b>NOTE:</b>The JRuby jar must be downloaded separately
 *  since it is released with the GPL license.</p>
 *  
 *  <p>The following example defines an actor in JRuby that
 *  computes factorials:</p>
<pre>
1.  include Java
2. 
3.  import Java::ptolemy.data.IntToken
4. 
5.  class Actor
6.      # create accessor methods for self field
7.      # the java actor will be assigned to this field.
8.      attr_accessor :self
9. 
10.     def fire
11.         # read the input value from the port "input"
12.         val = @self.getPort('input').get(0).intValue
13. 
14.         # calculate the factorial
15.         total = 1
16.         if val < 0
17.             @self.error("Input must be greater than or equal to 0")
18.         else
19.             while val > 1 do
20.                 total *= val
21.                 val -= 1
22.             end
23.        end
24. 
25.         # write the factorial to the port "output"
26.         @self.getPort('output').broadcast(IntToken.new(total))
27.     end
28. end
</pre>
 *
 *  <p>Line 1 includes the module for the JVM, and line 3 imports the Java
 *  class used by the actor. Lines 5-28 define the Actor object; the actor
 *  object must be named "Actor" in JRuby scripts. Line 8 specifies that
 *  accessor methods should be created for a field called "self". This
 *  field is a reference to the Java object of this actor, and can be used
 *  to access ports and parameters. Lines 10-27 define the fire() method of
 *  the actor, which is called each time this actor executes in the workflow.
 *  Line 12 reads an integer from the input port called "input". A port or
 *  parameter called "foo" can be accessed in the JRuby script by using
 *  @self.getPort("foo") or @self.attribute("foo"), respectively. Lines 15-23
 *  compute the factorial of the input number, and line 26 writes the result
 *  to the output port called "output".</p>
 *  
 *  @author Daniel Crawl
 *  @version $Id: JRuby.java 32806 2014-07-02 20:43:47Z crawl $
 */
public class JRuby extends ScriptEngineActor {

    /** Construct a new JRuby for a specified workspace. */
    public JRuby(Workspace workspace) {
        super(workspace);
    }

    /** Construct a new JRuby with the given container and name. */
    public JRuby(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
        // this property must be set to persistent so that assignment
        // statements in the script are persistent across calls to
        // engine.eval() and engine.put().
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");

        try {
            language.setToken("ruby");
        } catch(IllegalActionException e) {
            // an exception is probably due to the jruby jar missing
            throw new IllegalActionException(this, e,
                    "Error loading the script engine for JRuby. This can happen if\n" +
                    "the JRuby jar is not present. This jar is not included with\n" +
                    "Kepler due to licensing and must be downloaded separately.\n" +
                    "To use the JRuby actor: download the jar, either add it to\n" +
                    "your $CLASSPATH environment variable or copy it into\n" +
                    "kepler.modules/actors/lib/jar/, and restart Kepler.");
        }
        
        // a simple actor that prints when fire() is called
        script.setExpression("class Actor\n" +
        		"  attr_accessor :self\n" +
                "  def fire\n" +
                "     puts \"in fire\"\n" +
                "  end\n" +
                "end\n");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Create an instance of the actor object in the script.
     *  @return the actor instance.
     */
    @Override
    protected Object _createActorInstance(String actorClassNameStr) throws ScriptException {
        _engine.eval(_ACTOR_INSTANCE_NAME + " = " + actorClassNameStr + ".new");
        return _engine.get(_ACTOR_INSTANCE_NAME);
    }
    
    /** Put the given object to the actor instance in the script.
     *  @param name the name of the object to put.
     *  @param object the object to put.
     */
    @Override
    protected void _putObjectToActorInstance(String name, Object object) throws ScriptException {
        String globalName = "_yyy_" + name;
        _engine.put(globalName, this);
        _engine.eval(_ACTOR_INSTANCE_NAME + ".self = " + globalName);
    }
}