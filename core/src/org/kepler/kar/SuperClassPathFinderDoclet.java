/**
 *  '$Author: crawl $'
 *  '$Date: 2012-07-02 14:08:09 -0700 (Mon, 02 Jul 2012) $'
 *  '$Revision: 30098 $'
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

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import ptolemy.util.ClassUtilities;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

/** A doclet to find the paths of all super classes for an object.
 * 
 *  @author Daniel Crawl
 *  @version $Id: SuperClassPathFinderDoclet.java 30098 2012-07-02 21:08:09Z crawl $
 */

public class SuperClassPathFinderDoclet {
    
    /** The entry point called from javadoc. */
    public static boolean start(RootDoc root) {

        _classFiles.clear();
        
        try {
            ClassDoc[] classes = root.classes();
            _className = classes[0].qualifiedName();
            for (int i = 0; i < classes.length; i++) {
                String name = classes[i].qualifiedName();
                // skip nested classes
                if(i > 0 && name.startsWith(_className)) {
                    //System.out.println("skipping nested class " + name);
                    continue;
                }
                String filename = getFileNameForClassName(name);
                if (filename != null && !_classFiles.contains(filename)) {
                    _classFiles.add(filename);
                    _addSuperClasses(classes[i]);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR " + e.getClass() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
    
    /** Get the collection of path names. */
    public static Set<String> getClassFiles() {
        return new HashSet<String>(_classFiles);
    }
    
    /** Get the class name of the primary object. */
    public static String getClassName() {
        return _className;
    }

    /** Recursively add the super classes of the object. */
    private static void _addSuperClasses(ClassDoc classDoc) throws ClassNotFoundException {
        ClassDoc superClassDoc = classDoc.superclass();
        if (superClassDoc != null) {
            String name = superClassDoc.qualifiedName();
            String filename = getFileNameForClassName(name);
            //System.out.println("name = " + name);
            if (superClassDoc != null && filename != null && !_classFiles.contains(filename)
                    // for actors, stop at TypedAtomicActor and TypedCompositeActor
                    && !name.equals("ptolemy.actor.TypedAtomicActor")
                    && !name.equals("ptolemy.actor.TypedCompositeActor")
                    // for parameters stop at Variable
                    && !name.equals("ptolemy.data.expr.Variable")
                    // for directors stop at Attribute
                    && !name.equals("ptolemy.kernel.util.Attribute")) {
                _classFiles.add(filename);
                _addSuperClasses(superClassDoc);
            }
        }
    }
    
    /** Return the path for a fully-qualified class name. */
    public static String getFileNameForClassName(String className) throws ClassNotFoundException {
        String filename = className.replace('.', '/');

        // Inner classes: Get rid of everything past the first $
        if (filename.indexOf("$") != -1) {
            filename = filename.substring(0, filename.indexOf("$"));
        }

        filename += ".java";
        //System.out.println("filename = " + filename);
        URL url = ClassLoader.getSystemResource(filename);
        //System.out.println("url = " + url);        
        
        if(url == null) {

            try {
                url = ClassUtilities.sourceResource(filename);
            } catch (IOException e) {
                System.out.println("ERROR trying to find file for class " + className + ": " + e.getMessage());
            }
            
            if(url == null) {
                // see if class is a nested class
                Class<?> clazz = null;
                try {
                     clazz = Class.forName(className);
                } catch(ClassNotFoundException e) {
                   System.out.println("WARNING: could not instiante class " + className +
                           " (Maybe it's a customized composite actor?)");
                   return null;
                }
                if(clazz.getEnclosingClass() == null) {
                    System.out.println("WARNING: could not find path for class " + className);
                }// else { System.out.println("ignoring nested class " + className); }
            }
        }
        
        if(url != null) {
            return url.getPath();
        }
        return null;
        
    }

    /** A collection of path names. */
    private static final Set<String> _classFiles = new HashSet<String>();
    
    /** The class name of the primary object. */
    private static String _className;
}
