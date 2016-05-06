/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.kepler.configuration.ConfigurationProperty;
 
/**
 * A class to implement a query object that looks like this:

 &lt;query&gt;
      &lt;queryId&gt;eml210-quick-search-query&lt;/queryId&gt; 
      &lt;system&gt;http://knb.ecoinformatics.org&lt;/system&gt;
      &lt;namespace&gt;
        &lt;prefix&gt;eml&lt;/prefix&gt;
        &lt;value&gt;eml://ecoinformatics.org/eml-2.1.0&lt;/value&gt;
      &lt;/namespace&gt;
      &lt;returnField&gt;dataset/title&lt;/returnField&gt;
      &lt;returnField&gt;entityName&lt;/returnField&gt;
      &lt;title&gt;eml210-quick-search-query&lt;/title&gt;
      &lt;AND&gt;
        &lt;OR&gt;
          &lt;condition&gt;
            &lt;concept&gt;dataset/title&lt;/concept&gt;
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;#value#&lt;/value&gt;
          &lt;/condition&gt;
          &lt;condition&gt;
            &lt;concept&gt;keyword&lt;/concept&gt; 
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;#value#&lt;/value&gt;
          &lt;/condition&gt;
          &lt;condition&gt; 
            &lt;concept&gt;creator/individualName/surName&lt;/concept&gt;
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;#value#&lt;/value&gt;
          &lt;/condition&gt;
          &lt;condition&gt; 
            &lt;concept&gt;taxonRankValue&lt;/concept&gt; 
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;#value#&lt;/value&gt;
          &lt;/condition&gt;
          &lt;condition&gt; 
            &lt;concept&gt;abstract/para&lt;/concept&gt;
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;#value#&lt;/value&gt;
          &lt;/condition&gt;
        &lt;/OR&gt;
        &lt;OR&gt;
          &lt;condition&gt;
            &lt;concept&gt;dataset/dataTable/physical/distribution/online/url&lt;/concept&gt;
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;http://%&lt;/value&gt;
          &lt;/condition&gt;
          &lt;condition&gt;
            &lt;concept&gt;dataset/dataTable/physical/distribution/online/url&lt;/concept&gt; 
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;ecogrid://%&lt;/value&gt;
          &lt;/condition&gt;
          &lt;condition&gt; 
            &lt;concept&gt;dataset/spatialRaster/physical/distribution/online/url&lt;/concept&gt; 
            &lt;operator&gt;LIKE&lt;/operator&gt;
            &lt;value&gt;ecogrid://%&lt;/value&gt;
          &lt;/condition&gt;
        &lt;/OR&gt;
        &lt;OR&gt;
          &lt;condition&gt;
            &lt;concept&gt;dataset/dataTable/physical/distribution/online/url/@function&lt;/concept&gt; 
            &lt;operator&gt;EQUALS&lt;/operator&gt;
            &lt;value&gt;download&lt;/value&gt;
          &lt;/condition&gt;
          &lt;condition&gt; 
            &lt;concept&gt;dataset/spatialRaster/physical/distribution/online/url/@function&lt;/concept&gt;
            &lt;operator&gt;EQUALS&lt;/operator&gt;
            &lt;value&gt;download&lt;/value&gt;
          &lt;/condition&gt;
        &lt;/OR&gt;
      &lt;/AND&gt;
    &lt;/query&gt;

*/
public class SearchQuery
{
  public String queryId;
  public String system;
  public Namespace namespace;
  public Vector<String> returnField;
  public String title;
  public Condition condition;
  public BooleanLogic andOrConditions;
  
  /**
   * constructor
   */
  public SearchQuery(ConfigurationProperty queryProp)
  {
    parseProp(queryProp);
  }
  
  /**
   * replace the values in the conditions with the values in the hash
   */
  public void replaceValues(Hashtable valueMap)
  {
    if(andOrConditions != null)
    {
      andOrConditions.replaceValues(valueMap);
    }
    
    if(condition != null)
    {
      condition.replaceValue(valueMap);
    }
  }
  
  /**
   * toString
   */
  public String toString()
  {
    String s = new String();
    s += "<query queryId=\"" + queryId + "\" system=\"" + system + "\">\n";
    s += namespace.toString();

    for(int i=0; i<returnField.size(); i++)
    {
      String rf = (String)returnField.get(i);
      s += "<returnField>" + rf + "</returnField>\n";
    }
    
    s += "<title>" + title + "</title>\n";
    
    if(condition != null)
    {
      s += condition.toString();
    }
    if(andOrConditions != null)
    {
      s += andOrConditions.toString();
    }
    s += "</query>\n";
    return s;
  }
  
  /**
   * parse the property 
   */
  private void parseProp(ConfigurationProperty queryProp)
  {
    //get the basic fields
    queryId = getValueIfNotNull(queryProp.getProperty("queryId"));
    system = getValueIfNotNull(queryProp.getProperty("system"));
    title = getValueIfNotNull(queryProp.getProperty("title"));
    
    //get the returnfields
    List returnfields = queryProp.getProperties("returnField");
    returnField = new Vector();
    for(int i=0; i<returnfields.size(); i++)
    {
      String returnfield = ((ConfigurationProperty)returnfields.get(i)).getValue();
      returnField.add(returnfield);
    }
    
    //namespace
    ConfigurationProperty namespaceProp = queryProp.getProperty("namespace");
    if(namespaceProp != null)
    {
      namespace = new Namespace();
      namespace.prefix = getValueIfNotNull(namespaceProp.getProperty("prefix"));
      namespace.value = getValueIfNotNull(namespaceProp.getProperty("value"));
    }
    
    //conditions
    ConfigurationProperty conditionProp = queryProp.getProperty("condition");
    if(conditionProp != null)
    {
      condition = new Condition(conditionProp);
    }
    
    //AND/OR/Conditions    
    andOrConditions = parseAndOrProp(queryProp);

  }
  
  /**
   * parse the conditions
   */
  private BooleanLogic parseAndOrProp(ConfigurationProperty queryProp)
  {
    List l = queryProp.getProperties();
    BooleanLogic bl = new BooleanLogic();
    
    for(int i=0; i<l.size(); i++)
    {
      ConfigurationProperty cp = (ConfigurationProperty)l.get(i);
      if(cp.getName().equals("AND"))
      {
        bl.isAnd = true;
        bl.logic.add(parseAndOrProp(cp));
      }
      else if(cp.getName().equals("OR"))
      {
        bl.isOr = true;
        bl.logic.add(parseAndOrProp(cp));
      }
      else if(cp.getName().equals("condition"))
      {
        bl.addCondition(new Condition(cp));
      }
      else
      {
        continue;
      }
    }
    return bl;
  }
  
  /**
   * get the value or null if there isn't one
   */
  private String getValueIfNotNull(ConfigurationProperty cp)
  {
    if(cp != null)
    {
      return cp.getValue();
    }
    else
    {
      return null;
    }
  }
  
  /**
   * class to hold a namespace
   */
  private class Namespace
  {
    public String prefix;
    public String value;
    
    public Namespace()
    {
      
    }
    
    public String toString()
    {
      String s = "";
      s += "<namespace prefix=\"" + prefix + "\">" + value + "</namespace>\n";
      return s;
    }
  }
  
  /**
   * class to hold a condition
   */
  private class Condition
  {
    public String concept;
    public String operator;
    public String value;
    
    public Condition(ConfigurationProperty conditionProp)
    {
      concept = getValueIfNotNull(conditionProp.getProperty("concept"));
      operator = getValueIfNotNull(conditionProp.getProperty("operator"));
      value = getValueIfNotNull(conditionProp.getProperty("value"));
    }
    
    /**
     * replace the value of this condition with the value in the valueMap
     */
    public void replaceValue(Hashtable valueMap)
    {
      String v = (String)valueMap.get(value);
      if(v != null)
      {
        value = v;
      }
    }
    
    public String toString()
    {
      String s = "";
      s += "<condition concept=\"" + concept + "\" operator=\"" + operator + "\">" + value + "</condition>\n";
      return s;
    }
  }
  
  /**
   * class to contain the boolean logic
   */
  private class BooleanLogic
  {
    public Vector<Condition> conditions;
    public Vector<BooleanLogic> logic;
    public boolean isOr = false;
    public boolean isAnd = false;
    
    public BooleanLogic()
    {
      logic = new Vector();
      conditions = new Vector();
    }
    
    public void addCondition(Condition c)
    {
      conditions.add(c);
    }
    
    public void addBooleanLogic(BooleanLogic b)
    {
      logic.add(b);
    }
    
    public String toString()
    {
      return toString(this);
    }
    
    /**
     * replace the values in the conditions with the values in the valueMap
     */
    public void replaceValues(Hashtable valueMap)
    {
      replaceValues(valueMap, this);
    }
    
    private void replaceValues(Hashtable valueMap, BooleanLogic bl)
    {
      if(bl.conditions != null && bl.conditions.size() > 0)
      {
        for(int i=0; i<bl.conditions.size(); i++)
        {
          Condition c = (Condition)bl.conditions.get(i);
          c.replaceValue(valueMap);
        }
      }
      
      if(bl.logic != null && bl.logic.size() > 0)
      {
        for(int i=0; i<bl.logic.size(); i++)
        {
          BooleanLogic newbl = (BooleanLogic)bl.logic.get(i);
          replaceValues(valueMap, newbl);
        }
      }
    }
    
    private String toString(BooleanLogic bl)
    {
      String s = "";
      
      if(bl.conditions != null && bl.conditions.size() > 0)
      {
        for(int i=0; i<bl.conditions.size(); i++)
        {
          Condition c = (Condition)bl.conditions.get(i);
          s += c.toString();
        }
      }
      
      if(bl.logic != null && bl.logic.size() > 0)
      {
        for(int i=0; i<bl.logic.size(); i++)
        {
          BooleanLogic newbl = (BooleanLogic)bl.logic.get(i);
          if(bl.isAnd)
          {
            s += "<AND>\n";
            s += toString(newbl);
            s += "</AND>\n";
          }
          else if(bl.isOr)
          {
            s += "<OR>\n";
            s += toString(newbl);
            s += "</OR>\n";
          }
        }
      }
      
      return s;
    }
  }
}