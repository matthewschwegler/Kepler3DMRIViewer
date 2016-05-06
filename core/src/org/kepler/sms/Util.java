/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.kepler.sms;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.semanticweb.owl.model.OWLOntology;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: May 26, 2009
 * Time: 1:14:05 PM
 */
public class Util {
	public static String join(Object[] strings, String delimiter) {
		if (strings.length == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder(strings[0].toString());
		for (int i = 1; i < strings.length; i++) {
			builder.append(delimiter).append(strings[i].toString());
		}
		return builder.toString();
	}
	
	public static String join(List strings, String delimiter) {
		return join(strings.toArray(new Object[strings.size()]), delimiter);
	}
	
	public static List<String> splitTags(String rawTags) {
		rawTags = rawTags.trim();
		String[] tags = rawTags.split(",");
		List<String> currentTags = new ArrayList<String>();
		for (String tag : tags) {
			String effectiveTag = tag.replace(",", "").trim();
			if (!"".equals(effectiveTag)) {
				currentTags.add(effectiveTag);
			}
		}
		
		if ("".equals(rawTags)) {
			return Arrays.asList((String[]) new String[] {""});
		}
		// Bit of a hack
		if (rawTags.charAt(rawTags.length() - 1) == ',') {
			currentTags.add("");
		}
				
		return currentTags;
	}
	
	public static String truncate(String string, int length) {
		try {
			return string.substring(0, length) + "...";
		}
		catch(StringIndexOutOfBoundsException ex) {
			return string;
		}
	}	
	
	private static int countSubstring(String string, String substring) {
		int searchFrom = -1;
		int count = 0;
		do {
			searchFrom = string.indexOf(substring, searchFrom+1);
			if (searchFrom != -1) {
				count++;
			}
		}
		while (searchFrom != -1);		
		return count;
	}
	
	protected static Pattern GET_STRING_FROM_OWL_SERIALIZATION = Pattern.compile("\"(.*)\"\\^\\^string");
	
	public static String parseOWLSerializedString(String string) {
		Matcher matcher = GET_STRING_FROM_OWL_SERIALIZATION.matcher(string);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return string;
	}
	
	public static String format(List<?> tags) {
		return join(tags, ",");
	}
	
//	private static final Log log = LogFactory.getLog(Util.class);

	public static String[] tagSplit(String rawTags) {
		// The main reason we can't use String.split() is that if the
		// delimiter is the end of the string, we don't get an empty string
		// as one of the pieces at the end.
		// 
		// For example, "one,two,".split(",") -> {"one", "two"} and not
		// {"one", "two", ""}, as I would like. For the moment, we just cover
		// this case of the trailing empty tag, to solve the problem of the
		// 'Add new tag' dropdown option not going away after it is selected.
		
		String[] tags = rawTags.split(",");
		if ("".equals(rawTags)) return new String[] {""};
		if (rawTags.charAt(rawTags.length() - 1) == ',') {
			String[] newTags = new String[tags.length + 1];
			System.arraycopy(tags, 0, newTags, 0, tags.length);
			newTags[newTags.length - 1] = "";
			return newTags;
		}
		
		return tags;
	}

	public static String getOntologyLabel(OWLOntology ontology) {
		OntologyCatalog catalog = OntologyCatalog.instance();
		Iterator<NamedOntModel> iterator = catalog.getNamedOntModels();
		while (iterator.hasNext()) {
			NamedOntModel model = iterator.next();
			if (model.getOntology().equals(ontology)) {
				return model.getName();
			}
		}
		
		return "(ontology)";
	}
}
