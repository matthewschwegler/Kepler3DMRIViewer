/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data;

import java.util.List;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

/**
 * This class takes in information from the metadata parsers and tries to
 * intelligently guess as to the data type of an attribute based on provided
 * metadata. since different metadata standards provide differing levels of
 * complexity in their attribute level descriptions, some guesses may be better
 * than others, but this class, should at least be able to distinguish between,
 * whole (integer), real (float) and text (string) types.
 */
public class DataTypeResolver
{
  private static DataTypeResolver resolver = null; // singleton variable
  private static String DEFAULT_TYPE = "STRING";

  private DataType[] dataTypes;

  /**
   * constructor
   */
  private DataTypeResolver()
  {
    try
    {
      ConfigurationManager confMan = ConfigurationManager.getInstance();
      ConfigurationProperty commonProperty = confMan
          .getProperty(ConfigurationManager.getModule("common"));
      ConfigurationProperty dtdProperty = commonProperty
          .getProperty("dataTypeDictionary");
      List types = dtdProperty.getProperties();

      dataTypes = new DataType[types.size()];
      for (int i = 0; i < types.size(); i++)
      {
        ConfigurationProperty type = (ConfigurationProperty) types.get(i);
        ConfigurationProperty nameProp = type.getProperty("name");
        ConfigurationProperty numberTypeProp = type.getProperty("numberType");
        if (nameProp == null || numberTypeProp == null)
        {
          throw new RuntimeException("Expecting a name and numberType "
              + "child of dataType in " + "configuration.xml.");
        }

        String name = nameProp.getValue();
        String numberType = numberTypeProp.getValue();
        List aliasList = type.getProperties("alias");
        String[] aliases = new String[aliasList.size()];
        for (int j = 0; j < aliasList.size(); j++)
        {
          aliases[j] = ((ConfigurationProperty) aliasList.get(j)).getValue();
        }
        ConfigurationProperty numericTypeProp = type.getProperty("numericType");
        String maxval = null;
        String minval = null;
        String textencoding = null;
        if (numericTypeProp != null)
        {
          ConfigurationProperty minValProp = numericTypeProp
              .getProperty("minValue");
          ConfigurationProperty maxValProp = numericTypeProp
              .getProperty("maxValue");
          if (minValProp == null || maxValProp == null)
          {
            throw new RuntimeException("Expecting a minValue and "
                + "maxValue children of numericType in configuration.xml.");
          }
          minval = minValProp.getValue();
          maxval = maxValProp.getValue();
        }
        else
        {
          ConfigurationProperty textTypeProp = type.getProperty("textType");
          if (textTypeProp == null)
          {
            throw new RuntimeException(
                "Each dataType defined in the "
                    + "config.xml file must have either a textType or a numericType "
                    + "defined to be valid.");
          }
          ConfigurationProperty textEncodingProp = textTypeProp
              .getProperty("encoding");
          if (textEncodingProp == null)
          {
            throw new RuntimeException("The textType node must have an "
                + "encoding node as a child in config.xml.");
          }

          textencoding = textEncodingProp.getValue();
        }

        double min = -1.0;
        double max = -1.0;
        if (minval != null && maxval != null)
        {
          min = Double.parseDouble(minval);
          max = Double.parseDouble(maxval);
        }
        //System.out.println("name: " + name + " min: " + min + " max: " + max + " numberType: " + numberType + " aliases: " + aliases);
        DataType dt = new DataType(name, min, max, numberType, aliases);
        dataTypes[i] = dt;

      }
    }
    catch (javax.xml.transform.TransformerException te)
    {
      throw new RuntimeException("Error executing xpath expression in "
          + "DataTypeResolver.");
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw new RuntimeException("Exception in DataTypeResolver.");
    }
  }

  /**
   * returns a singleton instance of the DataTypeResolver
   */
  public static DataTypeResolver instanceOf()
  {
    if (resolver == null)
    {
      resolver = new DataTypeResolver();
    }
    return resolver;
  }

  /**
   * returns an array of the data types defined in the config.xml file
   */
  public DataType[] getDataTypes()
  {
    return dataTypes;
  }

  /**
   * this method will look up the numberType parameter which come from
   * metadata to find the type defined in configure.xml
   * 
   * @param min
   *          the minimum value for the attribute
   * @param max
   *          the maximum value for the attribute
   * @param typeName
   *          the name of the numberType declared in the metadata
   * @throws UnresolvableTypeException
   * @return one of the DataType constants (INT, LONG, FLOAT, DOUBLE, STRING)
   *         declared in DataType.java
   */
  public DataType resolveDataType(String numberType, Double min, Double max)
      throws UnresolvableTypeException
  {
    DataType type = numberTypeLookup(numberType, min, max);
    return type;
  }

  /**
   * resolves the type based solely on the name. This does an alias lookup on
   * the types in config.xml.
   * 
   * @param name
   *          the name of the type to resolve.
   */
  public DataType resolveTypeByAlias(String name, Double min, Double max)
      throws UnresolvableTypeException
  {
    DataType type = aliasLookup(name, min, max);
    return type;
  }

  /*
   * finad a DataType based on a numberType and range
   */
  private DataType numberTypeLookup(String type, Double min, Double max)
      throws UnresolvableTypeException
  {
    if (type == null)
    {
      throw new UnresolvableTypeException("The given number type is null");
    }
    if (min == null || max == null)
    {
      for (int i = 0; i < dataTypes.length; i++)
      {
        String numberType = dataTypes[i].getNumberType();
        if (numberType != null && type.trim().equals(numberType))
        {
          return dataTypes[i];
        }

      }
    }
    else
    {
      double minNum = min.doubleValue();
      double maxNum = max.doubleValue();
      for (int i = 0; i < dataTypes.length; i++)
      {
        String numberType = dataTypes[i].getNumberType();
        if (numberType != null && type.trim().equals(numberType)
            && rangeLookup(minNum, maxNum, dataTypes[i]))
        {
          return dataTypes[i];
        }

      }

    }
    throw new UnresolvableTypeException(
        "The number type '"
            + type.trim()
            + "' cannot be "
            + "resolved with any type in the config.xml file.  Please add a numberType "
            + "for this type to one of the DataType elements in config.xml.");
  }

  /**
   * find a DataType based on an alias name and range
   */
  private DataType aliasLookup(String typeName, Double min, Double max)
      throws UnresolvableTypeException
  {
    if (typeName == null)
    {
      throw new UnresolvableTypeException("The given alias type is null");
    }
    if (min == null || max == null)
    {
      for (int i = 0; i < dataTypes.length; i++)
      {
        String aliases[] = dataTypes[i].getNames();
        String name = dataTypes[i].getName();
        for (int j = 0; j < aliases.length; j++)
        {
          if (typeName.trim().equals(aliases[j].trim())
              || typeName.trim().equals(name))
          {

            return dataTypes[i];
          }
        }
      }
    }
    else
    {
      double minNum = min.doubleValue();
      double maxNum = max.doubleValue();
      for (int i = 0; i < dataTypes.length; i++)
      {
        String aliases[] = dataTypes[i].getNames();
        String name = dataTypes[i].getName();
        for (int j = 0; j < aliases.length; j++)
        {
          if ((typeName.trim().equals(aliases[j].trim()) || typeName.trim()
              .equals(name))
              && rangeLookup(minNum, maxNum, dataTypes[i]))
          {

            return dataTypes[i];
          }
        }
      }
    }
    throw new UnresolvableTypeException(
        "The type '"
            + typeName.trim()
            + "' cannot be "
            + "resolved with any type in the config.xml file.  Please add an alias "
            + "for this type to one of the DataType elements in config.xml.");
  }

  /**
   * If the given range fits the type.
   */
  private boolean rangeLookup(double min, double max, DataType type)
  {
    boolean fit = false;
    if (min > max)
    {
      return fit;
    }

    double dtmin = type.getMin();
    double dtmax = type.getMax();
    if (dtmin < min && dtmax > max)
    {
      fit = true;
    }
    return fit;

  }
}