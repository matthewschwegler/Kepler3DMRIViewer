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

package org.kepler.build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.kepler.build.modules.ModulesTask;

/**
 * Class to test the build
 *
 * @author welker
 */
public class BuildTest extends ModulesTask
{

    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        System.out.println("Running build test....");
        test();
    }

    /**
     * run the test
     *
     * @throws ParseException
     */
    public void test() throws ParseException
    {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd");
        String input = "3 3";
        System.out.println("input = " + input);
        Date d = sdf.parse("Mar 3");
        System.out.println(sdf.format(d));
        SimpleDateFormat sdf2 = new SimpleDateFormat("MM-dd-yyyy");
        System.out.println(sdf2.format(d));

    }

    public static void main(String[] args) throws Exception
    {

    }

}
