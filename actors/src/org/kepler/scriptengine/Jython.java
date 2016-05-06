/* An actor whose execution is specified by a Jython script.

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
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;

/** An actor whose execution is controlled by a Jython script.
 *
 *  The purpose of this actor is to check backwards-compatibility
 *  with the Ptolemy Python actor.
 * 
 *  @see ptolemy.actor.lib.python.PythonScript
 * 
 *  @author Daniel Crawl
 *  @version $Id: Jython.java 32535 2013-11-12 22:35:43Z crawl $
 */
public class Jython extends ScriptEngineActor {

    /** Construct a new Jython for a specified workspace. */
	public Jython(Workspace workspace) {
        super(workspace);
    }

    /** Construct a new Jython with the given container and name. */
	public Jython(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
    	// the ptolemy PythonScript actor uses "Main" for the class name.
        actorClassName.setExpression("Main");
        
        language.setToken("python");
        
        script.setExpression("class Main:\n" +
        		"  \"hello\"\n" +
        		"  def fire(self):\n" +
        		"    print(\"in fire\")\n");
    }

	/** React to changes in by attributes named "jythonClassName". 
	 *  Changes in other attributes are handled by parent classes.
	 */
    @Override
    public void attributeChanged(Attribute attribute) throws IllegalActionException {
     
    	// the ptolemy PythonScript actor uses a parameter named "jythonClassName"
    	// to hold the class name in the script.
        if(attribute.getName().equals("jythonClassName") &&
                (attribute instanceof StringAttribute)) {
            actorClassName.setToken(((StringAttribute)attribute).getExpression());
        } else {
            super.attributeChanged(attribute);
        }
        
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Create an instance of the actor object in the script.
     *  @return the actor instance.
     */
    @Override
    protected Object _createActorInstance(String actorClassNameStr) throws ScriptException {
        _engine.eval(_ACTOR_INSTANCE_NAME + " = " + actorClassNameStr + "()");
        return _engine.get(_ACTOR_INSTANCE_NAME);
    }  

    /** Put the given object to the actor instance in the script.
     *  @param name the name of the object to put.
     *  @param object the object to put.
     */
    @Override
    protected void _putObjectToActorInstance(String name, Object object) throws ScriptException {
        String globalName = "_yyy_" + name;
        _engine.put(globalName, object);
        _engine.eval(_ACTOR_INSTANCE_NAME + "." + name + " = " + globalName);
    }
}
