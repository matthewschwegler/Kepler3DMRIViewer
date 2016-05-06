/*
 * Copyright (c) 2009 The Regents of the University of California.
 * All rights reserved.
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
package org.kepler.build.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * class with misc util methods
 *
 * @author berkley
 */
public class MiscUtil
{
    /**
     * create a horiz line
     *
     * @param size
     * @param c
     * @return
     */
    private static String horizontalLine(int size, char c)
    {
        String line = "";
        for (int i = 0; i < size; i++)
        {
            line += c;
        }
        return line;
    }

    /**
     * create a single horiz line
     *
     * @param size
     * @return
     */
    public static String singleHorizontalLine(int size)
    {
        return horizontalLine(size, '-');
    }

    /**
     * create a double horiz line
     *
     * @param size
     * @return
     */
    public static String doubleHorizontalLine(int size)
    {
        return horizontalLine(size, '=');
    }

    /**
     * create a table line
     *
     * @param data
     * @param numColumns
     * @return
     */
    public static String tableHorizontalLine(List<String> data, int numColumns)
    {
        String result = "||";
        for (int i = 0; i < numColumns; i++)
        {
            result += "-" + singleHorizontalLine(getColumnSize(data, i)) + "-|";
        }
        return result += "|";
    }

    /**
     * create a box line
     *
     * @param line
     * @param size
     * @return
     */
    private static String boxLine(String line, int size)
    {
        return "| " + line + getSpaces(size - line.length()) + " |";
    }

    /**
     * return n spaces
     *
     * @param n
     * @return
     */
    private static String getSpaces(int n)
    {
        String spaces = "";
        for (int i = 0; i < n; i++)
        {
            spaces += " ";
        }
        return spaces;
    }

    /**
     * print a box
     *
     * @param data
     */
    public static void printBox(List<String> data)
    {
        if (data == null || data.size() <= 0)
        {
            return;
        }
        int size = 0;
        for (String line : data)
        {
            if (line.length() > size)
            {
                size = line.length();
            }
        }
        int adjustedSize = size + 4; //Add 4 to the size for "| " and " |" in the box.
        System.out.println(singleHorizontalLine(adjustedSize));
        for (String line : data)
        {
            System.out.println(boxLine(line, adjustedSize));
        }
        System.out.println(singleHorizontalLine(adjustedSize));
    }

    /**
     * get the column size
     *
     * @param data
     * @param columnIndex
     * @return
     */
    private static int getColumnSize(List<String> data, int columnIndex)
    {
        int size = 0;
        for (String line : data)
        {
            int lineLength = line.split("\\s")[columnIndex].length();
            if (size < lineLength)
            {
                size = lineLength;
            }
        }
        return size;
    }

    /**
     * print the table
     *
     * @param data
     */
    public static void printTable(List<String> data)
    {
        if (isNotProperTableData(data))
        {
            return;
        }

        int numColumns = data.get(0).split("\\s").length;

        int size = 0;
        for (int i = 0; i < numColumns; i++)
        {
            size += getColumnSize(data, i);
        }
        int adjustedSize = size + 6 + (numColumns - 1) * 3; //"|| ", " | ", " ||"

        System.out.println(doubleHorizontalLine(adjustedSize));
        for (int i = 0; i < data.size(); i++)
        {
            String line = data.get(i);
            System.out.print("||");
            String[] columnData = line.split("\\s");
            for (int j = 0; j < numColumns; j++)
            {
                System.out.print(" " + columnData[j]
                        + getCellSpaces(data, j, columnData[j]) + " |");
            }
            System.out.println("|");
            //			if( i != data.size() - 1)
            //				System.out.println( tableHorizontalLine(data,numColumns) );
        }
        System.out.println(doubleHorizontalLine(adjustedSize));
    }

    /**
     * return spaces for a cell
     *
     * @param data
     * @param columnIndex
     * @param columnData
     * @return
     */
    private static String getCellSpaces(List<String> data, int columnIndex,
                                        String columnData)
    {
        return getSpaces(getColumnSize(data, columnIndex) - columnData.length());
    }

    /**
     * return true if data is not proper table data
     *
     * @param data
     * @return
     */
    private static boolean isNotProperTableData(List<String> data)
    {
        if (data == null || data.size() == 0)
        {
            return true;
        }
        int numColumns = data.get(0).split("\\s").length;
        if (numColumns == 0)
        {
            return true;
        }
        for (String line : data)
        {
            if (line.split("\\s").length != numColumns)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * return a date from a string
     *
     * @param dateTime
     * @return
     * @throws ParseException
     */
    public static Date parseDateTime(String dateTime) throws ParseException
    {
        Calendar time = Calendar.getInstance();
        time.set(0, 0, 0, 0, 0);
        String rest = "";
        for (String part : dateTime.split("\\s"))
        {
            if (part.contains(":"))
            {
                time = parseTime(time, part);
            }
            else
            {
                rest += part + " ";
            }
        }
        rest = rest.trim();
        Date date = parseDate(rest);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        return dateTimeFormat.parse(dateFormat.format(date) + " "
                + timeFormat.format(time.getTime()));
    }

    /**
     * return a calendar from a cal and time
     *
     * @param c
     * @param time
     * @return
     * @throws ParseException
     */
    private static Calendar parseTime(Calendar c, String time)
            throws ParseException
    {
        if (time.matches("\\d(\\d)?:\\d(\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            c.setTime(sdf.parse(time));
            return c;
        }
        else
        {
            return null;
        }
    }

    /**
     * parse a date
     *
     * @param date
     * @return
     * @throws ParseException
     */
    private static Date parseDate(String date) throws ParseException
    {
        Date result = null;
        if (date.matches("\\d\\d\\d\\d-\\d(\\d)?-\\d(\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            result = sdf.parse(date);
        }
        else if (date.matches("\\d(\\d)?-\\d(\\d)?-\\d\\d(\\d\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy");
            result = sdf.parse(date);
        }
        else if (date.matches("\\d(\\d)?-\\d(\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy");
            result = sdf
                    .parse(date + "-" + Calendar.getInstance().get(Calendar.YEAR));
        }
        else if (date.matches("[a-zA-Z]+\\s+\\d(\\d)?,\\s+\\d\\d(\\d\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yy");
            result = sdf.parse(date);
        }
        else if (date.matches("[a-zA-Z]+\\s+\\d(\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yy");
            result = sdf.parse(date + ", "
                    + Calendar.getInstance().get(Calendar.YEAR));
        }
        else if (date.matches("\\d(\\d)?\\s+[a-zA-Z]+\\s+\\d\\d\\d\\d"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
            result = sdf.parse(date);
        }
        else if (date.matches("\\d\\d\\d\\d\\s+[a-zA-Z]+\\s+\\d(\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMMM dd");
            result = sdf.parse(date);
        }
        else if (date.matches("\\d(\\d)?[a-zA-Z][a-zA-Z][a-zA-Z]\\d\\d\\d\\d"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy");
            result = sdf.parse(date);
        }
        else if (date.matches("\\d\\d\\d\\d[a-zA-Z][a-zA-Z][a-zA-Z]\\d(\\d)?"))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMMdd");
            result = sdf.parse(date);
        }
        else
        {
            throw new ParseException("", 0);
        }
        return result;
    }

}
