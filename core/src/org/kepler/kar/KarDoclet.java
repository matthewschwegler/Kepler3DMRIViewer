/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-18 12:31:11 -0700 (Tue, 18 Sep 2012) $' 
 * '$Revision: 30705 $'
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

package org.kepler.kar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

/**
 * A doclet that creates documentation for KAR files.
 * 
 * @author Daniel Crawl
 * @version $Id: KarDoclet.java 30705 2012-09-18 19:31:11Z crawl $
 */

public class KarDoclet {
	/** The entry point called from javadoc. */
	public static boolean start(RootDoc root) {
		_table.clear();

		try {
			ClassDoc[] classes = root.classes();
			for (int i = 0; i < classes.length; i++) {
			    // make sure this is not deprecated
			    if(classes[i].tags("@deprecated").length > 0) {
			        if(!_generateForDeprecated) {
			            System.out.println("WARNING: skipping " + classes[i].qualifiedName() + " because it is deprecated.");
			            continue;
			        }
			        _deprecated.add(classes[i].qualifiedName());
			    }
			    
				// javadoc returns inner classes in RootDoc.classes()
				// make sure the current class is not an inner class
				// NOTE: this assumes we've already parsed the parent
				// class and put it in _table.
				String name = classes[i].qualifiedName();
				name = name.substring(0, name.lastIndexOf("."));
				if (_table.get(name) == null) {
					// _out("going to parse class: " + classes[i]);
					_parseClass(classes[i], true);
					// _out("-------");
					// _out(_doc.exportMoML());
					// _out("-------");
				} else {
					// _out("skipping inner class: " + classes[i]);
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR " + e.getClass() + ": " + e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Retrieve the documentation for a particular class after it has been
	 * created.
	 */
	public static KeplerDocumentationAttribute getDoc(String name) {
		return _table.get(name);
	}

	/** Retrieve all the documentation. */
	public static Map<String,KeplerDocumentationAttribute> getAllDocs() {
		return _table;
	}
	
	/** Returns true if the given class name is deprecated.
	 *  NOTE: this method does not look at the tags of the class;
	 *  the doclet must be run on the source file first.
	 */
	public static boolean isClassDeprecated(String className) {
	    return _deprecated.contains(className);
	}
	
	/** Set if documentation will be generated for deprecated classes. */
	public static void setGenerateForDeprecated(boolean generate) {
	    _generateForDeprecated = generate;
	}
	
	public static void setWorkspace(Workspace workspace) {
	    _workspace = workspace;
	}

	/** Create documentation for a class. */
	private static void _parseClass(ClassDoc classDoc, boolean printHeader)
			throws IllegalActionException, NameDuplicationException, Exception {
		// _out("");
		// _out("Class: " + classDoc);
		// _out("commentText: " + classDoc.commentText());

		//String name = classDoc.name();

		if (printHeader) {
			// _out("parsing for " + name);

		    if(_workspace == null) {
		        _workspace = new Workspace();
		    }
		    
			_doc = new KeplerDocumentationAttribute(_workspace);
			_doc.setName("KeplerDocumentation");

			_table.put(classDoc.qualifiedName(), _doc);

			// add docs for the authors
			String authors = _getAndCombineTags("@author", classDoc);
			if (authors.length() == 0) {
				_warn("No authors for " + classDoc.qualifiedName());
			} else {
				_doc.setAuthor(authors);
			}

			// add docs for the class
			String comment = classDoc.commentText();
			if (comment.length() == 0) {
				_warn("No comments for class " + classDoc.qualifiedName());
			} else {
				_doc.setUserLevelDocumentation("\n" + comment);
			}

			// add docs for the version
			String version = _getAndCombineTags("@version", classDoc);
			if(version.length() == 0) {
			    _warn("No version for " + classDoc.qualifiedName());
			} else {
			    // if the first character is $, remove everything after the second $
			    version = version.trim();
			    if(version.charAt(0) == '$') {
			        final int index = version.indexOf('$', 1);
			        if(index > 1) {
			            version = version.substring(0, index+1);
			        }
			    }
			    _doc.setVersion(version); 
			}
		}

		// add docs for all the fields
		FieldDoc[] fields = classDoc.fields();
		for (int i = 0; i < fields.length; i++) {
			FieldDoc curField = fields[i];
			Type type = curField.type();

			// add docs for a field if it's public, and a class
			if (curField.isPublic() && !type.isPrimitive()) {
				_parseField(curField);
			}
		}

        // recursively add documentation of the parent class
        ClassDoc superClass = classDoc.superclass();
        if (superClass != null) {
            if (!superClass.toString().equals("ptolemy.actor.TypedAtomicActor")
                    && !superClass.toString().equals(
                            "ptolemy.actor.TypedCompositeActor")) {
                // _out("recursing super class " + superClass.qualifiedName());
                _parseClass(superClass, false);
            }
        }
    }

    /** Return the FieldType of the field. */
	private static FieldType _getFieldType(String className)
			throws ClassNotFoundException {
		// _out("check field:" + className);

		if (className.equals("ptolemy.actor.TypedIOPort")) {
			return FieldType.Port;
		} else if (className.equals("ptolemy.actor.parameters.PortParameter")) {
		    return FieldType.PortParameter;
		} else if (className.equals("ptolemy.data.expr.Parameter") ||
		        className.equals("ptolemy.kernel.util.StringAttribute")) {
			return FieldType.Parameter;
		} else if (className.equals("java.lang.Object")) {
			return FieldType.Unknown;
		} else {
			String superClsStr = null;
			Class<?> cls = null;
			try {
				cls = Class.forName(className);
			} catch(ClassNotFoundException e) {
				// try converting to a nested class
				String nestedName = new StringBuilder(className).replace(
						className.lastIndexOf('.'),
						className.lastIndexOf('.') + 1, "$").toString();
				cls = Class.forName(nestedName);
			}
			if(cls != null) {
				if(cls.isInterface()) {
					return FieldType.Ignore;
				}
				Class<?> superCls = cls.getSuperclass();
				if(superCls != null) {
					superClsStr = superCls.toString();
				}
			}
	
			if (superClsStr == null) {
				_warn("could not determine super class for: " + className);
				return FieldType.Unknown;
			} else if(superClsStr.indexOf("class ") != 0) {
				_warn("super class name does not begin with class: " + superClsStr);
				return FieldType.Unknown;
			} else {
				return _getFieldType(superClsStr.substring(6));
			}
		}
	}

	/** Create documentation for a port or parameter. */
	private static void _parseField(FieldDoc fieldDoc)
			throws IllegalActionException, NameDuplicationException, Exception {
		// _out("");
		// _out("field: " + fieldDoc);
		// _out("type: " + fieldDoc.type());
		// _out("commentText: " + fieldDoc.commentText().replaceAll("\n", ""));

		String fullName = fieldDoc.qualifiedName();
		String name = fieldDoc.name();

		FieldType type = _getFieldType(fieldDoc.type().asClassDoc()
				.qualifiedName());

		if (type == FieldType.Unknown) {
			//_out("skipping unknown type of field " + fullName +
			    //" type: " + fieldDoc.type());
		} else if (type != FieldType.Ignore) {
		    boolean unhandled = true;
			String comment = fieldDoc.commentText().replaceAll("\n", "");
			if (comment.length() == 0) {
				_warn("No comments for " + type.getName() + " " + fullName);
			}
			
			if (type == FieldType.Port || type == FieldType.PortParameter) {
				_doc.addPort(name, comment);
				unhandled = false;
			}
			
			if (type == FieldType.Parameter || type == FieldType.PortParameter) {
				_doc.addProperty(name, comment);
				unhandled = false;
			} 
			
			if(unhandled) {
				_warn("Unhandled field type: " + type);
			}
		}
	}

	private static String _getAndCombineTags(String tag, Doc doc) {
		String retval = "";
		Tag[] tags = doc.tags(tag);
		if (tags != null && tags.length > 0) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < tags.length; i++) {
				buf.append(tags[i].text() + ", ");
			}

			retval = buf.toString();
			// remove last comma
			retval = retval.replaceAll("\\n", "");
			retval = retval.replaceAll("\\s*, $", "");
		}
		return retval;
	}

	private static void _out(String str) {
		System.out.println(str);
	}

	private static void _warn(String str) {
		_out("WARNING: " + str);
	}
	
	/** A mapping of class name to documentation. */
	private static Map<String, KeplerDocumentationAttribute> _table = 
        new HashMap<String,KeplerDocumentationAttribute>();

	/** The current doc. */
	private static KeplerDocumentationAttribute _doc;

	/** The types of fields. */
	private enum FieldType {
        Parameter("parameter"),
        Port("port"),
        PortParameter("port parameter"),
        Unknown("unknown"),
        Ignore("ignore");
        
        FieldType(String name) {
            _name = name;
        }
        
        public String getName() {
            return _name;
        }
        
        private String _name;
	};
	
	private static Workspace _workspace;
	
	/** If false, do not generate documentation for deprecated classes. */
	private static boolean _generateForDeprecated = true;
	
	/** A collection of deprecated classes. */
	private static Set<String> _deprecated = new HashSet<String>();
}