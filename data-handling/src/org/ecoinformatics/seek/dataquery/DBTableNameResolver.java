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

package org.ecoinformatics.seek.dataquery;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.data.db.Entity;

/**
 * This class will try to generate a unique table name base on a given
 * TableEntity object.
 * 
 * @author Jing Tao
 * 
 */
public class DBTableNameResolver {
	private static final String PREFIX = "T";

	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory.getLog("org.ecoinformatics.seek.dataquery");
		isDebugging = log.isDebugEnabled();
	}

	/**
	 * If the url in this TableEntity is null, we will use the TableEntity name
	 * as url. First we will check if the url is already in the system table
	 * which store the url and table mapping. If the url already existed, the
	 * table name will be set as the table name stored in system table. If the
	 * url doesn't exited, the hashcode of this url (add profix "T") as table
	 * name.
	 * 
	 * @param table
	 *            TableEntity
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @return TableEntity
	 */
	public Entity resolveTableName(Entity table) throws SQLException,
			ClassNotFoundException {
		String newTableName = null;
		String url = table.getURL();
		if (url == null) {
			url = table.getName();
		}
		// check if the url already existed in system table
		DBTableExistenceChecker checker = new DBTableExistenceChecker();
		// if the table already existed, we don't need
		// generate table name again(url is the key)
		if (checker.isURLExisted(url)) {
			// to do get the table name and set to table entity
			newTableName = checker.getTableName(url);
			if (isDebugging) {
				log
						.debug("Get the table name for system table "
								+ newTableName);
			}
			table.setDBTableName(newTableName);
			return table;
		}
		// if this is a record we need to generate the tablename
		newTableName = generateTableName(table);
		table.setDBTableName(newTableName);
		return table;
	}

	/*
	 * Method to generate a talbe name base a given TableEnity object. Now it
	 * use url to generate hash and add a given letter in first position. If the
	 * generate table name already in persistant table, it will append random
	 * strings
	 */
	private String generateTableName(Entity table) throws SQLException,
			ClassNotFoundException {
		int maxTime = 5;

		String tableName = null;
		String url = table.getURL();
		// if url is null, set url as same as entity name
		if (url == null) {
			url = table.getName();
		}
		int hashNumber = url.hashCode();
		// remove "-" because hsql doesn't allow it in table name
		hashNumber = removeNegativeSymbolFromHashNumber(hashNumber);
		if (isDebugging) {
			log.debug("The hash number is " + hashNumber);
		}
		tableName = PREFIX + hashNumber;
		// check if the generate table already existed, if exited, append some
		// random character. If the while loop runs > maxTime times, throw a
		// exception
		DBTableExistenceChecker checker = new DBTableExistenceChecker();
		int times = 0;
		while (checker.isTableNameExisted(tableName)) {
			String random = generateRandomString();
			tableName = tableName + random;
			times++;
			if (times > maxTime) {
				throw new SQLException(
						"Couldn't generate a non duplicate table name");
			}
		}
		if (isDebugging) {
			log.debug("The table name is " + tableName);
		}
		return tableName;
	}// generateTableName

	/*
	 * method to generate a random string which lenght is between 1 to 10.
	 */
	private String generateRandomString() {
		String randomString = "";
		char[] letters = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
				'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
				'w', 'x', 'y', 'z' };
		int length = (new Double(Math.random() * 10)).intValue() + 1;
		if (isDebugging) {
			log.debug("The appendix string lenghth is " + length);
		}
		for (int i = 0; i < length; i++) {
			int random = (new Double(Math.random() * 26)).intValue();
			if (random >= 25) {
				random = 25;
			}
			char selectChar = letters[random];
			randomString = randomString + selectChar;
		}
		if (isDebugging) {
			log.debug("the random string is " + randomString);
		}
		return randomString;
	}// generateRandomString

	/*
	 * Method to remove negative symbol for hash number. Table name doesn't
	 * allow "-" in hsql
	 */
	private int removeNegativeSymbolFromHashNumber(int hashNumber) {
		int newHashNumber = hashNumber;
		String hashNumberString = (new Integer(hashNumber)).toString();
		if (isDebugging) {
			log.debug("transform hashnumber from int " + hashNumber
					+ " to string " + hashNumberString);
		}
		if (hashNumberString != null && hashNumberString.startsWith("-")) {
			hashNumberString = hashNumberString.substring(1);
			if (isDebugging) {
				log.debug("The new string after removing negative symbol is "
						+ hashNumberString);
			}
			newHashNumber = (new Integer(hashNumberString)).intValue();

		}
		if (isDebugging) {
			log.debug("The new hash number after removing netative symbol is "
					+ newHashNumber);
		}
		return newHashNumber;
	}// removeNegativeSymbolFromHashNumber

}
