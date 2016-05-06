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

package org.kepler.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * tokenizes a delimited file. This reader assumes that one record is on one
 * line which ends with the line
 */
public class DelimitedReader {
	private InputStreamReader dataReader;
	private Vector[] lines;
	private Vector linesVector;
	private int numHeaderLines;
	private int numRecords;
	private boolean stripHeader = false;
	private int numCols;
	private String delimiter;
	private String lineEnding;
	private boolean collapseDilimiter = false;
	private int numFooterLines = 0;
	private Vector footerBuffer = new Vector();
	private boolean initializedFooterBuffer = false;
	private int headLineNumberCount = 0;
	private boolean isLenient = false;
	private String discoveredLineEnding = null;
	private static Vector possibleLineEndings = null;

	private static Log log;
	static {
		log = LogFactory.getLog("org.kepler.util.DelimitedReader");
		possibleLineEndings = new Vector();
		possibleLineEndings.add("\n");
		possibleLineEndings.add("\r");
		possibleLineEndings.add("\r\n");
	}

	/**
	 * constructor. reads the csv stream.
	 * 
	 * @param delimString
	 *            the delimited stream to read
	 * @param numCols
	 *            the number of columns in the stream
	 * @param delimiter
	 *            the delimiter to tokenize on
	 * @param numHeaderLines
	 *            the number of lines to skip at the top of the file
	 * @param lineEnding
	 *            the line ending char(s)...either "\n"lo (unix),
	 * @param isLenient
	 *            specifies if extra columns should be ignored "\r\n" (windoze)
	 *            or "\r" (mac)
	 */
	public DelimitedReader(String data, int numCols, String delimiter,
			int numHeaderLines, String lineEnding, int numRecords,
			boolean isLenient) throws Exception {
		this.numHeaderLines = numHeaderLines;
		this.numCols = numCols;
		this.numRecords = numRecords;
		log.debug("Delimiter is: " + delimiter);
		this.delimiter = unescapeDelimiter(delimiter);
		log.debug("LineEnding is: " + lineEnding);
		this.lineEnding = unescapeDelimiter(lineEnding);
		this.isLenient = isLenient;

		// lines = new Vector[numRecords + numHeaderLines + 1];
		linesVector = new Vector();

		int begin = 0;
		int end = 0;
		// int i = 0;
		while (end < data.length()) { // add each line of the string as an
										// element in a vector
			end = data.indexOf(this.lineEnding, begin); // DFH 'this.' added
			if (end == -1) {
				end = data.length();
			}
			String line = data.substring(begin, end);
			if (!line.trim().equals("")) {
				// take off the line ending
				// MBJ: I commented out the next line as it was improperly
				// truncating lines
				// I'm not sure why it was there in the first place, as the
				// previous substring
				// removed the delimiter
				// line = line.substring(0, line.length() -
				// lineEnding.length());

				// split the line based on the delimiter
				Vector v = splitDelimitedRowStringIntoVector(line);
				/*
				 * String[] s = line.split(delimiter.trim(), numCols); Vector v
				 * = new Vector(); for(int j=0; j<s.length; j++) {
				 * v.addElement(s[j]); }
				 * 
				 * if(v.size() < numCols) { int vsize = v.size(); for(int j=0;
				 * j<numCols - vsize; j++) { //add any elements that aren't
				 * there so that all the records have the //same number of cols
				 * v.addElement(""); } }
				 */
				// lines[i] = v;
				linesVector.add(v);
				// i++;
			}
			// go to the next line
			begin = end + this.lineEnding.length(); // DFH 'this.' added
		}

		int records = linesVector.size();
		if (records != this.numRecords) {
			this.numRecords = records;
			log
					.warn("Metadata disagrees with actual data. Changing number of records to: "
							+ records);
		}
		lines = new Vector[records];
		for (int k = 0; k < records; k++) {
			lines[k] = (Vector) linesVector.get(k);
		}
		/*
		 * for(int j=0; j<lines.length; j++) { if(lines[j] == null) { lines[j] =
		 * new Vector(); } }
		 */

	}

	/**
	 * This constructor will read delimitered data from stream rather a string
	 * 
	 * @param dataStream
	 *            InputStream The input stream
	 * @param numCols
	 *            int the number of columns
	 * @param delimiter
	 *            String delimiter the delimiter to tokenize on
	 * @param numHeaderLines
	 *            int numHeaderLines the number of lines to skip at the top of
	 *            the file
	 * @param lineEnding
	 *            String lineEnding the line ending char(s)...either "\n"
	 *            (unix),"\r\n" (windoze) or "\r" (mac)
	 * @param numRecords
	 *            int number of rows in the input stream
	 */
	public DelimitedReader(InputStream dataStream, int numCols,
			String delimiter, int numHeaderLines, String lineEnding,
			int numRecords, boolean stripHeader) {
		this.dataReader = new InputStreamReader(dataStream);
		this.numHeaderLines = numHeaderLines;
		this.numCols = numCols;
		this.numRecords = numRecords;
		log.debug("Delimiter is: " + delimiter);
		this.delimiter = unescapeDelimiter(delimiter);
		log.debug("LineEnding is: " + lineEnding);
		this.lineEnding = unescapeDelimiter(lineEnding);
		this.stripHeader = stripHeader;

	}

	/**
	 * Method to set up data stream as source
	 * 
	 * @param dataStream
	 *            InputStream
	 */
	public void setInputStream(InputStream dataStream) {
		this.dataReader = new InputStreamReader(dataStream);
	}

	/**
	 * Method to set up collapseDelimiter. If it is yes, consecutive dilimiters
	 * will be consider as single dilimiter.
	 * 
	 * @param collapseDelimiter
	 */
	public void setCollapseDelimiter(boolean collapseDelimiter) {
		this.collapseDilimiter = collapseDelimiter;
	}

	/**
	 * Set up the footer line number.
	 * 
	 * @param numFooterLines
	 */
	public void setNumFooterLines(int numFooterLines) {
		this.numFooterLines = numFooterLines;
	}

	public boolean isLenient() {
		return isLenient;
	}

	public void setLenient(boolean isLenient) {
		this.isLenient = isLenient;
	}

	/**
	 * This method is from data source as a input stream This method will read
	 * one row from and return a data vector which element is String and the
	 * value is field data. After reach the end of stream, empty vector will be
	 * returned. So this method can be iterated by a while loop until a empty
	 * vector hited. During the iteration, every data in the stream will be
	 * pulled out.
	 * 
	 * @return Vector
	 */
	public Vector getRowDataVectorFromStream() throws Exception {
		// System.out.println("the numFootLines is "+numFooterLines);
		if (!initializedFooterBuffer) {
			for (int i = 0; i < numFooterLines; i++) {
				// System.out.println("the initialize with footer lines");
				String rowData = readOneRowDataString();
				// System.out.println("the data vector in initailize is "+rowData.toString());
				footerBuffer.add(rowData);
			}
			// this is for no footer lines
			if (numFooterLines == 0) {
				// System.out.println("the initialize without footer lines");
				String rowData = readOneRowDataString();
				// System.out.println("The initial buffere vector is "+rowData.toString());
				footerBuffer.add(rowData);
			}
			initializedFooterBuffer = true;
		}
		String nextRowData = readOneRowDataString();
		// System.out.println("the row string data from next row "+nextRowData.toString());
		String oneRowDataString = null;
		Vector oneRowDataVector = new Vector();

		if (nextRowData != null) {
			// System.out.println("before nextRowData is empty and nextRowData is "+nextRowData.toString());
			oneRowDataString = (String) footerBuffer.remove(0);
			reIndexFooterBufferVector();
			footerBuffer.add(nextRowData);
		} else if (numFooterLines == 0 && !footerBuffer.isEmpty()) {
			// System.out.println("find the last line in fottlines num is 0!!!!!!!!");
			oneRowDataString = (String) footerBuffer.remove(0);
		}
		// System.out.println("helere!!!");
		if (oneRowDataString != null) {
			log.debug("in dataReader is not null");
			oneRowDataVector = splitDelimitedRowStringIntoVector(oneRowDataString);
		}
		// System.out.println("the row data from buffer "+oneRowDataVector.toString());
		return oneRowDataVector;
	}

	/*
	 * This method will read a row data from vector. It discard the head lines.
	 * but it doesn't dsicard footer lines This method will be called by
	 * getRowDataVectorFromStream
	 */
	private String readOneRowDataString() {
		// Vector oneRowDataVector = new Vector();
		StringBuffer rowData = new StringBuffer();
		String rowDataString = null;
		int singleCharactor = -2;

		if (dataReader != null) {
			// log.debug("in dataReader is not null");
			try {
				while (singleCharactor != -1) {
					// log.debug("in singleCharactor is not null");
					singleCharactor = dataReader.read();
					char charactor = (char) singleCharactor;
					rowData.append(charactor);
					// find string - line ending in the row data
					boolean foundLineEnding = (rowData.indexOf(lineEnding) != -1);

					// if we are being lenient, try some other line endings for
					// parsing the data
					if (!foundLineEnding && this.isLenient()) {
						// have we discovered the ending already in this data?
						if (this.discoveredLineEnding != null) {
							foundLineEnding = (rowData
									.indexOf(this.discoveredLineEnding) != -1);
						}
						// otherwise we need to try a few of them out
						else {
							for (int i = 0; i < possibleLineEndings.size(); i++) {
								String possibleLineEnding = (String) possibleLineEndings
										.get(i);
								foundLineEnding = (rowData
										.indexOf(possibleLineEnding) != -1);
								if (foundLineEnding) {
									this.discoveredLineEnding = possibleLineEnding;
									break;
								}
							}
						}
					}
					// finally see if we found the end of the line
					if (foundLineEnding) {
						log.debug("found line ending");
						// strip the header lines
						if (stripHeader && numHeaderLines > 0
								&& headLineNumberCount < numHeaderLines) {
							// reset string buffer(descard the header line)
							rowData = null;
							rowData = new StringBuffer();

						} else {
							rowDataString = rowData.toString();
							log.debug("The row data is " + rowDataString);
							break;
						}
						headLineNumberCount++;
					}
				}
			} catch (Exception e) {
				log.debug("Couldn't read data from input stream");
			}
		}
		// System.out.println("the row data before reutrn is "+rowDataString);
		return rowDataString;
	}

	/*
	 * This method will forward one index for every element, 1 -> 0, 2->1
	 */
	private void reIndexFooterBufferVector() {
		for (int i = 0; i < numFooterLines - 2; i++) {
			Vector element = (Vector) footerBuffer.elementAt(i + 1);
			footerBuffer.add(i, element);
		}
	}

	/*
	 * This method will read a delimitered string and put a delimitered part
	 * into an element in a vector. If the vector size is less than the column
	 * number empty string will be added.
	 */
	private Vector splitDelimitedRowStringIntoVector(String data)
			throws Exception {
		Vector result = new Vector();
		if (data == null) {
			return result;
		}
		String[] s = null;
		if (!collapseDilimiter) {
			s = data.split(delimiter);
		} else {
			String newDelimiterWithRegExpress = delimiter + "+";
			s = data.split(newDelimiterWithRegExpress);

		}

		if (s != null) {
			if (!isLenient && s.length > numCols) {
				throw new Exception("Metadata sees data has " + numCols
						+ " columns but actually data has " + s.length
						+ " columns. Please make sure metadata is correct!");
			}
			int columnCount = Math.min(s.length, numCols);
			for (int j = 0; j < columnCount; j++) {

				if (s[j] != null) {
					result.addElement(s[j].trim());
				} else {
					result.addElement("");
				}
			}
			// add any elements that aren't there so that all the records have
			// the
			// same number of cols
			if (result.size() < numCols) {
				int vsize = result.size();
				for (int j = 0; j < numCols - vsize; j++) {
					result.addElement("");
				}
			}
		}
		return result;
	}

	/**
	 * returns the data as an array of vectors. each vector will have the same
	 * number of elements as there are columns in the data.
	 * 
	 * @param stripHeaderLines
	 *            true if the header lines should not be included in the
	 *            returned data, false otherwise
	 */
	public Vector[] getTokenizedData(boolean stripHeaderLines) {
		if (stripHeaderLines) {
			Vector[] strip = null;
			if (numRecords > numHeaderLines) {
				strip = new Vector[numRecords - numHeaderLines];
				for (int i = numHeaderLines; i < lines.length; i++) {
					strip[i - numHeaderLines] = lines[i];
				}
			}
			return strip;
		} else {
			return lines;
		}
	}

	/**
	 * returns a string representation of the data
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < lines.length; i++) {
			log.debug("line[" + (i + 1) + "]: " + lines[i].toString());
			for (int j = 0; j < lines[i].size(); j++) {
				sb.append((String) lines[i].elementAt(j));
				if (j != lines[i].size() - 1) {
					sb.append(" || ");
				}
			}
			sb.append(lineEnding);
		}
		return sb.toString();
	}

	/**
	 * Convert a string escaped representation of a delimiter character into an
	 * the actual String for that delimiter. This is used for translating
	 * escaped versions of tab, newline, and carriage return characters to their
	 * real character values.
	 * 
	 * @param delimiter
	 *            the String representing the delimiter
	 * @return the actual String for the delimiter
	 */
	public static String unescapeDelimiter(String delimiter) {
		String newDelimiter = delimiter;

		if (delimiter == null) {
			log.debug("Delimiter is null and we set up to \n.");
			newDelimiter = "\n";
		} else if (delimiter.equals("\\t")) {
			log.debug("Tab interpreted incorrectly as string.");
			newDelimiter = "\t";
		} else if (delimiter.equals("\\n")) {
			log.debug("Newline interpreted incorrectly as string.");
			newDelimiter = "\n";
		} else if (delimiter.equals("\\r")) {
			log.debug("CR interpreted incorrectly as string.");
			newDelimiter = "\r";
		} else if (delimiter.equals("\\r\\n")) {
			log.debug("CRNL interpreted incorrectly as string.");
			newDelimiter = "\r\n";
		} else if (delimiter.startsWith("#")) {
			log.debug("XML entity charactor.");
			String digits = delimiter.substring(1, delimiter.length());
			int radix = 10;
			if (digits.startsWith("x")) {
				log.debug("Radix is " + 16);
				radix = 16;
				digits = digits.substring(1, digits.length());
			}
			log.debug("Int value of  delimiter is " + digits);

			newDelimiter = transferDigitsToCharString(radix, digits);

		} else if (delimiter.startsWith("0x") || delimiter.startsWith("0X")) {
			int radix = 16;
			String digits = delimiter.substring(2, delimiter.length());
			log.debug("Int value of  delimiter is " + digits);
			newDelimiter = transferDigitsToCharString(radix, digits);
		}

		return newDelimiter;
	}

	private static String transferDigitsToCharString(int radix, String digits) {
		if (digits == null) {
			return null;
		}
		Integer integer = Integer.valueOf(digits, radix);
		int inter = integer.intValue();
		log.debug("The decimal value of char is " + inter);
		char charactor = (char) inter;
		String newDelimiter = Character.toString(charactor);
		log.debug("The new delimter is " + newDelimiter);
		return newDelimiter;
	}
}