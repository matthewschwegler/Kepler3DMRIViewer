/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

package org.kepler.loader;

import org.kepler.build.modules.ModulesTask;

/**
 * Created by David Welker. Date: Oct 7, 2008 Time: 12:55:56 PM
 */
public class CreateIntroFileTask extends ModulesTask {

	@Override
	public void run() throws Exception
	{
		
	}
//	public void init() throws BuildException {
//		Project project = new Project();
//		project.setBaseDir(ProjectLocator.getProjectDir());
//		this.setProject(project);
//		super.init();
//	}
//
//	public void run() throws Exception 
//	{
//		init();
//
////		SvnTask svn = new SvnTask();
////		svn.bindToOwner(this);
////		for (String module : modules) {
////			Info info = new Info();
////			info.setProject(getProject());
////			info.setTarget(module);
////			info.setPropPrefix(module);
////			svn.addInfo(info);
////		}
////		svn.execute();
//
//		File introDir = new File(basedir,"loader/configs/ptolemy/configs/kepler");
//
//		PrintWriter pw = FileMerger.getPrintWriter(new File(introDir,
//				"intro.htm"));
//
//		FileMerger.merge(getClass().getResourceAsStream(
//				"/ptolemy/configs/kepler/intro_begin"), pw);
//		pw.println("<br>");
//		pw.println("<table style=\"\">");
//		pw.println("<tr><td style=\"padding: 0px 10px 1px 10px; font-size: 8px;\"><b>Module</b></td><td style=\"padding: 1px 10px 1px 10px; font-size: 8px;\"><b>Revision</b></td><td style=\"padding: 1px 10px 1px 10px; font-size: 8px;\"><b>Modified</b></td></tr>");
//		for (String module : modules) {
//			String revision = getProperty(module + ".rev");
//			String modified = getProperty(module + ".lastRev");
//
//			pw.println("<tr>");
//			pw.println("<td style=\"padding: 0px 10px 1px 10px; font-size: 8px;\"> " + module + " </td>");
//			pw.println("<td style=\"padding: 0px 10px 1px 10px; font-size: 8px;\"> " + revision + " </td>");
//			pw.println("<td style=\"padding: 0px 10px 1px 10px; font-size: 8px;\"> " + modified + " </td>");
//			pw.println("</tr>");
//		}
//
//		pw.println("</table>");
//
//		FileMerger.merge(getClass().getResourceAsStream(
//				"/ptolemy/configs/kepler/intro_end"), pw);
//
//		pw.close();
//	}

}
