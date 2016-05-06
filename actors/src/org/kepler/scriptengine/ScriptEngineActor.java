/* An actor whose execution is specified by a script.

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

import java.lang.reflect.InvocationTargetException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.process.TerminateProcessException;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.toolbox.TextEditorConfigureFactory;

/** An actor whose execution is specified by a script. The Java
 *  ScriptEngine API is used to invoke methods in the script.
 * 
 *  @author Daniel Crawl
 *  @version $Id: ScriptEngineActor.java 32808 2014-07-02 21:55:51Z crawl $
 */
public abstract class ScriptEngineActor extends TypedAtomicActor {

    /** Construct a new ScriptEngineActor for a specified workspace. */
    public ScriptEngineActor(Workspace workspace) {
        super(workspace);
    }

    /** Construct a new ScriptEngineActor with the given container and name. */
    public ScriptEngineActor(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
        actorClassName = new StringParameter(this, "actorClassName");
        actorClassName.setToken("Actor");
        actorClassName.setVisibility(Settable.EXPERT);
        
        language = new StringParameter(this, "language");
        language.setVisibility(Settable.EXPERT);

        script = new StringAttribute(this, "script");
        
        TextEditorConfigureFactory factory = new TextEditorConfigureFactory(this, "_editorFactory");
        factory.attributeName.setExpression("script");
    }
    
    /** React to an attribute change. When the language changes, this
     *  method loads the script engine for that language. */
    @Override
    public void attributeChanged(Attribute attribute) throws IllegalActionException {
        
        if(attribute == language) {
            String val = language.stringValue();
            if(!val.equals(_languageStr)) {

                _manager = new ScriptEngineManager();
                final ScriptEngine engine = _manager.getEngineByName(val);
                if(engine == null) {
                    throw new IllegalActionException(this,
                            "Could not find script engine for " + val);
                }
                
                if(!(engine instanceof Invocable)) {
                    throw new IllegalActionException(this,
                            "Script engine for " + _languageStr + " is not invocable.");
                }
                                
                _languageStr = val;
                _engine = engine;
                
            }
        } else if(attribute == script) {
          if(_engine != null) {
              _initializeScript();
          }
        } else {
            super.attributeChanged(attribute);
        }
    }
    
    /** Clone the actor into the specified workspace. */
    @Override
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        final ScriptEngineActor newObject = (ScriptEngineActor) super.clone(workspace);
        newObject._actorObject = null;
        newObject._engine = null;
        newObject._languageStr = "";
        newObject._missingMethods = new boolean[MethodType.values().length];
        return newObject;
    }
    
    /** Send the message to all registered debug listeners.
     *  @param message The debug message.
     */
    public void debug(String message) {
        if (_debugging) {
            _debug("From script: ", message);
        }
    }
    
    /** Set an error message that will be thrown in an exception after the
     *  current or next method is invoked in the script.
     *  @param message The debug message.
     */
    public void error(String message) {
        _errorMessage = message;
    }

    /** Invoke the fire() method in the script. */
    @Override
    public void fire() throws IllegalActionException {
        
        super.fire();
        
        if(!_missingMethods[MethodType.fire.ordinal()]) {
            _invokeMethod(MethodType.fire);
        }
    }

    /** Invoke the initialize() method in the script. */
    @Override
    public void initialize() throws IllegalActionException {
        super.initialize();
        if(!_missingMethods[MethodType.initialize.ordinal()]) {
        	_invokeMethod(MethodType.initialize);
        }
    }
    
    /** Invoke the postfire() method in the script. 
     *  @return If the postfire method is present in the script,
     *  returns the return value from executing this method. Otherwise,
     *  returns the value from the parent class's postfire(). 
     */
    @Override
    public boolean postfire() throws IllegalActionException {
        
        if(!_missingMethods[MethodType.postfire.ordinal()]) {
            final InvocationType type = _invokeMethod(MethodType.postfire);
            
            // see if the method returned false or true
            if(type == InvocationType.ReturnedFalse) {
                return false;
            } else if(type == InvocationType.ReturnedTrue) {
                return true;
            }
        }

        // postfire method not implemented so use the super class postfire
        return super.postfire();
    }

    /** Invoke the prefire() method in the script. 
     *  @return If the prefire method is present in the script,
     *  returns the return value from executing this method. Otherwise,
     *  returns the value from the parent class's prefire(). 
     */
    @Override
    public boolean prefire() throws IllegalActionException {
        
        if(!_missingMethods[MethodType.prefire.ordinal()]) {
            final InvocationType type = _invokeMethod(MethodType.prefire);
            
            // see if the method returned false or true
            if(type == InvocationType.ReturnedFalse) {
                return false;
            } else if(type == InvocationType.ReturnedTrue) {
                return true;
            }
        }

        // prefire method not implemented so use the super class prefire
        return super.prefire();
    }
    
    
    /** Invoke the preinitialize() method of the script. */
    @Override
    public void preinitialize() throws IllegalActionException {
        
        super.preinitialize();
        
        // make sure a language was chosen and engine is not null
        if(_languageStr.isEmpty()) {
            throw new IllegalActionException(this, "Chose a scripting language.");
        }
                
        _initializeScript();
        
        // NOTE: we don't check if the preinitialize method exists, since
        // initializeScript() clears missingMethods[].
        
        _invokeMethod(MethodType.preinitialize);        
    }

    /** Invoke the stop() method in the script. */
    @Override
    public void stop() {
        super.stop();
        if(!_missingMethods[MethodType.stop.ordinal()]) {
        	try {
        		_invokeMethod(MethodType.stop);
        	} catch (IllegalActionException e) {
        		MessageHandler.error("Error invoking stop().", e);
        	}
        }
    }

    /** Invoke the stopFire() method in the script. */
    @Override
    public void stopFire() {
        super.stopFire();
        if(_engine != null) {
		    if(!_missingMethods[MethodType.stopFire.ordinal()]) {
		    	try {
		    		_invokeMethod(MethodType.stopFire);
		    	} catch (IllegalActionException e) {
		    		MessageHandler.error("Error invoking stopFire().", e);
		    	}
		    }
        }
    }

    /** Invoke the terminate() method in the script. */
    @Override
    public void terminate() {
        super.terminate();
        if(!_missingMethods[MethodType.terminate.ordinal()]) {
        	try {
        		_invokeMethod(MethodType.terminate);
        	} catch (IllegalActionException e) {
        		MessageHandler.error("Error invoking terminate().", e);
        	}
        }
        _clearScriptObjects();
    }

    /** Invoke the wrapup() method in the script. */
    @Override
    public void wrapup() throws IllegalActionException {
        super.wrapup();
        if(_engine != null) {
	        if(!_missingMethods[MethodType.wrapup.ordinal()]) {
	        	_invokeMethod(MethodType.wrapup);
	        }
	        _clearScriptObjects();
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    /** The name of the class in the script defining the execution methods. */
    public StringParameter actorClassName;
    
    /** The script language. */
    public StringParameter language;
    
    /** The contents of the script. */
    public StringAttribute script;

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Create an instance of the actor object in the script.
     *  @return the actor instance.
     */
    protected Object _createActorInstance(String actorClassNameStr) throws ScriptException {
        _engine.eval(_ACTOR_INSTANCE_NAME + " = new " + actorClassNameStr + "()");
        return _engine.get(_ACTOR_INSTANCE_NAME);
    }
        
    /** Put the given object to the actor instance in the script.
     *  @param name the name of the object to put.
     *  @param object the object to put.
     */
    protected void _putObjectToActorInstance(String name, Object object) throws ScriptException {
        String globalName = "_yyy_" + name;
        _engine.put(globalName, object);
        _engine.eval(_ACTOR_INSTANCE_NAME + "." + name + " = " + globalName);
        //System.out.println("put " + object + " as " + globalName);
        //System.out.println(_ACTOR_INSTANCE_NAME + "." + name + " = " + globalName);
    }
        
    ///////////////////////////////////////////////////////////////////
    ////                         protected variables                 ////
        
    /** The engine to parse and execute scripts. */
    protected ScriptEngine _engine;
        
    /** The name of the actor object in the script. */
    protected final static String _ACTOR_INSTANCE_NAME = "_TOP_ACTOR_INSTANCE";
    
    /** The actor object in the script. */
    protected Object _actorObject;

    /** If true, use reflection to invoke the method. Otherwise, use the
     *  Invocable interface of the ScriptEngine to invoke the method.
     */
    protected boolean _invokeMethodWithReflection = false;
    
    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////
    
    /** Remove all java objects, e.g., ports and parameters, added to the script. */
    private void _clearScriptObjects() {
        //Bindings bindings = _engine.getBindings(ScriptContext.ENGINE_SCOPE);
        //bindings.clear();        
        //_engine.setBindings(_engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        _engine = null;
        _errorMessage = null;
    }
    
    /** Clear any existing objects put to the existing script, get a new
     *  script engine based on the value in the language parameter, create
     *  the actor instance in the engine, and puts ports and parameters
     *  to the actor instance.
     */
    private void _initializeScript() throws IllegalActionException {
        
        // remove any ports and parameters we previously put to the script
        _clearScriptObjects();

        try {
                 
            if(_engine == null) {
                _engine = _manager.getEngineByName(_languageStr);
            }
            
            if(_engine == null) {
                throw new IllegalActionException(this,
                        "Scripting engine not found for " + _languageStr);
            }
            
            _engine.eval(script.getExpression());
            //System.out.println("script changed:\n" + script.getExpression());

            String actorClassNameStr = actorClassName.stringValue();
            if(!actorClassNameStr.isEmpty()) {
                _actorObject = _createActorInstance(actorClassNameStr);
            }

            //System.out.println("actor object = " + _actorObject);
            
            _putObjectsToEngine();

            // initialize the error string
           // _engine.put("error", null);
            
            // initialize the set of missing methods since the methods could
            // have been added/removed since last execution.
            for(int i = 0; i < _missingMethods.length; i++) {
                _missingMethods[i] = false;
            }
            
        } catch (ScriptException e) {
            throw new IllegalActionException(this, e, "Error evaluating script.");
        }
    }
    
    /** Attempt to invoke the specified method in the script. If the method is defined
     *  in the script, the method is invoked and the object returned by the method is
     *  returned. If the method does not exist, this returns InvocationType.MethodNotFound.
     *  If invoking the method in the script causes an exception, the exception is wrapped
     *  in an IllegalActionException, which is then thrown by this method. If the
     *  method in the script successfully finishes, and if the error message has been
     *  (by calling error()), then the error message is wrapped in an IllegalActionException
     *  and thrown.
     */
    private InvocationType _invokeMethod(MethodType method) throws IllegalActionException {
        
    	//System.out.println("in invokeMethod for " + method);

        // see if we already know the method is not implemented in the script
        if(_missingMethods[method.ordinal()]) {
            return InvocationType.MethodNotFound;
        }
        
        Object retval = null;
        
        final String methodName = method.toString();
        
        // try invoking the method
        try {
            //System.out.println("going to invoke " + methodName);
            
            // see if the actor object was found in the script.
            // if it was found, invoke the method on that object,
            // otherwise invoke the function with the same name as the method.
            if(_actorObject != null) {
                if(_invokeMethodWithReflection) {
                    retval = _actorObject.getClass().getMethod(methodName).invoke(_actorObject);
                } else {
                    retval = ((Invocable)_engine).invokeMethod(_actorObject, methodName);
                }
            } else {
                retval = ((Invocable)_engine).invokeFunction(methodName);
            }
        } catch (ScriptException e) {
            
            // check if exception cause, or cause's cause is TerminateProcessException
            // thrown when PN is done.
            Throwable cause = e.getCause();
            if(cause != null) {
                if(cause instanceof TerminateProcessException) {
                    throw (TerminateProcessException) cause;
                }
                Throwable innerCause = cause.getCause();
                if(innerCause != null && (innerCause instanceof TerminateProcessException)) {
                    throw (TerminateProcessException) innerCause;
                }
            } else {
                // check the exception message.
                // when a TerminateProcessException occurs, javascript throws a ScriptException
                // whose cause is null, but message something line:
                // sun.org.mozilla.javascript.internal.WrappedException: Wrapped ptolemy.actor.process.TerminateProcessException:
                // 
                final String message = e.getMessage();
                if(message.contains("ptolemy.actor.process.TerminateProcessException")) {
                    throw new TerminateProcessException("");
                }
            }
            throw new IllegalActionException(this, e, "Error excuting " + methodName + " in script.");
        } catch (NoSuchMethodException e) {
            // the method was not implemented, so update the array
            _missingMethods[method.ordinal()] = true;
            //System.out.println("does not have " + methodName);
            return InvocationType.MethodNotFound;
        } catch(InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if(target != null) {
                Throwable cause = target.getCause();
                if(cause instanceof TerminateProcessException) {
                    throw (TerminateProcessException)cause;
                }
            }
            throw new IllegalActionException(this, e, "Error executing " + methodName + " in script.");        
        } catch(Exception e) {
            throw new IllegalActionException(this, e, "Error executing " + methodName + " in script.");        
        }
        
        // the method was implemented in the script and executed
        
        // check for an error
        if(_errorMessage != null) {
            throw new IllegalActionException(this, _errorMessage);
        }
        
        /*
        final Object error = _engine.get("error");
        if(error != null) {
            throw new IllegalActionException(this, error.toString());
        } 
        */       

        // check for a return type
        if(retval == null) {
            return InvocationType.ReturnedNothing;
        } else if(retval == Boolean.FALSE) {
            return InvocationType.ReturnedFalse;
        } else if(retval == Boolean.TRUE) {
            return InvocationType.ReturnedTrue;
        }
        
        // the method returned something unexpected: not true or false.
        System.out.println("WARNING: unknown return type: " + retval);
        return InvocationType.ReturnedNothing;

    }
    
    /** Put the ports, parameters, etc. of this actor to the script's actor
     *  object instance. If the script does not define an actor class,
     *  put the ports and parameters as global variables.
     */
    private void _putObjectsToEngine() throws ScriptException {
        
        // put all the ports and parameters to the script
        for(TypedIOPort port : portList()) {
            //System.out.println("putting port " + port.getName());
            final String portName = port.getName();
            if(_actorObject == null) {
                _engine.put(portName, port);
            } else {
                _putObjectToActorInstance(portName, port);
            }
        }
        
        for(Attribute attribute : attributeList(Attribute.class)) {
            final String attributeName = attribute.getName();
            if(attribute == actorClassName || attribute == language ||
                    attributeName.equals("class") ||
                    attributeName.startsWith("_") ||
                    attributeName.isEmpty()) {
                continue;
            }
            
            //System.out.println("putting attribute " + attribute.getName());
            if(_actorObject == null) {
                _engine.put(attributeName, attribute);
            } else {
                _putObjectToActorInstance(attributeName, attribute);
            }
        }
        
        // put the java actor (this object) to the actor instance
        if(_actorObject == null) {
            _engine.put("actor", this);
        } else {
            _putObjectToActorInstance("actor", this);
        }
        
        // put the script engine object to the actor instance.
        // in the script, engine.eval() can be used to load libraries.
        if(_actorObject == null) {
            _engine.put("engine", _engine);
        } else {
            _putObjectToActorInstance("engine", _engine);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
    
    /** The value of language parameter. */
    private String _languageStr = "";
        
    /** An array denoting if a method is implemented by the script. */
    private boolean[] _missingMethods = new boolean[MethodType.values().length];
    
    /** An object to load script engines. */
    private ScriptEngineManager _manager;
    
    /** A string containing an error message set by the script. */
    private String _errorMessage;
    
    /** Possible outcomes when calling invoking a method. */
    private enum InvocationType {
        MethodNotFound,
        ReturnedFalse,
        ReturnedNothing,
        ReturnedTrue,
    };
    
    /** The types of methods that can be implemented by the script. */
    private enum MethodType {
        fire,
        initialize,
        prefire,
        preinitialize,
        postfire,
        stop,
        stopFire,
        terminate,
        wrapup
    };
}
