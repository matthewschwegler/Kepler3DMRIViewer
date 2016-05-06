package org.kepler.actor.database;
/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.geon.OpenDBConnection;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Create a database table if the table doesn't exist.
 * 
 * @author Jing Tao
 */

public class DatabaseTableCreator extends LimitedFiringSource {
  
  public DatabaseTableCreator(CompositeEntity container, String name)
      throws NameDuplicationException, IllegalActionException {
    super(container, name);
    tableNameParam = new PortParameter(this, "Table Name");
    tableNameParam.setStringMode(true);
    tableNameParam.setTypeEquals(BaseType.STRING);
    tableNameParam.getPort().setTypeEquals(BaseType.STRING);
    
    createTableIfNotExistParam = new PortParameter(this, "Create the table if it doesn't exist");
    createTableIfNotExistParam.setStringMode(false);
    createTableIfNotExistParam.setTypeEquals(BaseType.BOOLEAN);
    createTableIfNotExistParam.getPort().setTypeEquals(BaseType.BOOLEAN);
    createTableIfNotExistParam.setExpression("true");
    
    sqlScriptParam = new PortParameter(this, "SQL Script");
    sqlScriptParam.setStringMode(true);
    sqlScriptParam.setTypeEquals(BaseType.STRING);
    sqlScriptParam.getPort().setTypeEquals(BaseType.STRING);
    
    dbParams = new PortParameter(this, "Database Param");
    Type type = OpenDBConnection.getDBParamsType();
    dbParams.setTypeEquals(type);
    dbParams.getPort().setTypeEquals(type);
    dbParams.setExpression("{driver = \"org.hsqldb.jdbcDriver\", password = \"pass\", url = \"jdbc:hsqldb:hsql://localhost/hsqldb\", user = \"sa\"}");
    boolean isInput = false;
    boolean isOutput = true;
    statusPort = new TypedIOPort(this, "Status", isInput, isOutput);
    statusPort.setMultiport(false);
    statusPort.setTypeEquals(BaseType.BOOLEAN);
    
  }

  // /////////////////////////////////////////////////////////////////
  // // ports and parameters ////

 
  /** Output of this actor. Ture if the table exists or was created successfully, false otherwise*/
  public TypedIOPort statusPort;
  
  /** The name of the table which will be created**/
  public PortParameter tableNameParam;
  
  /** Flag indicates to create the table if the table doesn't exist*/
  public PortParameter createTableIfNotExistParam;
  
  /** A sql script to create the table**/
  public PortParameter sqlScriptParam;
   

  /** Parameter to access a database. It includes user name, password, 
        driver name and db url**/
  public PortParameter dbParams;
  
 

  // /////////////////////////////////////////////////////////////////
  // // public methods ////

  /** Close the connection if open. */
  public void initialize() throws IllegalActionException {
    super.initialize();
    _closeConnection();
  }
  
  /**
   * Reconfigure actor when certain attributes change.
   * 
   * @param attribute
   *            The changed Attribute.
   * @throws ptolemy.kernel.util.IllegalActionException
   * 
   */
  public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
      throws ptolemy.kernel.util.IllegalActionException {
        
    if (attribute == tableNameParam) {
      if (tableNameParam != null && tableNameParam.getToken() != null){
          tableName = ((StringToken)tableNameParam.getToken()).stringValue();
          //System.out.println("get the table name "+tableName+" from attributedChanged method");
        }
    } else if (attribute == createTableIfNotExistParam) {
      if (createTableIfNotExistParam != null && createTableIfNotExistParam.getToken() != null){
        createTableIfNotExisted = ((BooleanToken)createTableIfNotExistParam.getToken()).booleanValue();
        //System.out.println("get the flag createTableIfNotExisted "+createTableIfNotExisted+" from attributedChanged method");
      }
    } else if(attribute == sqlScriptParam){
      if (sqlScriptParam != null && sqlScriptParam.getToken() != null){
        sqlScript = ((StringToken)sqlScriptParam.getToken()).stringValue();
        //System.out.println("get the sql "+sqlScript+" from attributedChanged method");
      }
    } else if(attribute == dbParams){
      dbParams.update();
    } else {
      super.attributeChanged(attribute);
    }
  }
  

  /** If connection is closed, open a new one. */
  public void fire() throws IllegalActionException {
    super.fire();
    //System.out.println("the fire in DatabaseTable creator !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    boolean ifTableExist = true;
    if (tableNameParam != null && tableNameParam.getToken() != null) {
      tableName = ((StringToken)tableNameParam.getToken()).stringValue();
      //System.out.println("get the table name "+tableName+" from fire method");
    }
    if (createTableIfNotExistParam != null && createTableIfNotExistParam.getToken() != null) {
      createTableIfNotExisted = ((BooleanToken)createTableIfNotExistParam.getToken()).booleanValue();
      //System.out.println("get the flag createTableIfNotExisted "+createTableIfNotExisted+" from fire method");
    }
    if (sqlScriptParam != null && sqlScriptParam.getToken() != null) {
      sqlScript = ((StringToken)sqlScriptParam.getToken()).stringValue();
      //System.out.println("get the sql "+sqlScript+" from fire method");
    }
    if (_db == null) {
      getConnection();
    }
    
    //System.out.println("final table name: "+tableName);
    String selectionSQL = _createSelectSQL(tableName);
   
    ResultSet rs;
    try {
      Statement st = _db.createStatement();
      rs = st.executeQuery(selectionSQL);
    } catch (SQLException e) {
      ifTableExist = false;
    }
    if(ifTableExist){
      //table does exist, sent status "true" to output
      //System.out.println("The table "+tableName+" does exist");
      statusPort.send(0, new BooleanToken(true));
    } else{
      //System.out.println("the final createTableIfNotExisted is "+createTableIfNotExisted);
      //System.out.println("the final sql is "+sqlScript);
      if(!createTableIfNotExisted){
        //since we don't need create the table, just send the false status
        //System.out.println("The table "+tableName+" doesn't exist but we don't need to create it");
        statusPort.send(0, new BooleanToken(false));
      } else {
        try {
          
          Statement st = _db.createStatement();
          rs = st.executeQuery(sqlScript);
          //System.out.println("Successfully created the table "+tableName+" with sql "+sqlScript);
          statusPort.send(0, new BooleanToken(true));
        } catch (SQLException e) {
          //System.out.println("Failed to create the table "+tableName+" with sql "+sqlScript);
          ifTableExist = false;
        }
      }
      
    }
  }

  /** Close the connection if open. */
  public void wrapup() throws IllegalActionException {
    super.wrapup();
    _closeConnection();
  }

  // /////////////////////////////////////////////////////////////////
  // // protected methods ////

  /** Get the database connection. */
  protected void getConnection() throws IllegalActionException {
   dbParams.update();
    RecordToken params = (RecordToken) dbParams.getToken();
    if (params != null) {
      _db = OpenDBConnection.getConnection(params);
    } else {
      throw new IllegalActionException(this, 
          "Please specify the database parameters. Actor couldn't access the database without those paramters."+
          "The parameter looks like: {driver = \"org.hsqldb.jdbcDriver\", password = \"pass\", url = \"jdbc:hsqldb:hsql://localhost/hsqldb\", user = \"sa\"}");
      } 
  }

  // /////////////////////////////////////////////////////////////////
  // // protected variables ////

  /** A JDBC database connection. */
  protected Connection _db = null;
  protected String tableName = null;
  protected boolean createTableIfNotExisted= false;
  protected String sqlScript = null;

  // /////////////////////////////////////////////////////////////////
  // // private methods ////

  /** Close the connection if open. */
  private void _closeConnection() throws IllegalActionException {
    try {
      if (_db != null) {
        _db.close();
      }
      _db = null;
    } catch (SQLException e) {
      throw new IllegalActionException(this, "SQLException: "
          + e.getMessage());
    }
  }
  
  /** Create the select sql command with the given table name */
  private String _createSelectSQL(String tableName) throws IllegalActionException {
    String selectionSql = null;
    if(tableName != null && !tableName.trim().equals("")){
      selectionSql = "SELECT * FROM "+tableName;
    } else {
      throw new IllegalActionException(this, "Exception: Please specify the table name which you want to create. ");
    }
    return selectionSql;
  }
}
