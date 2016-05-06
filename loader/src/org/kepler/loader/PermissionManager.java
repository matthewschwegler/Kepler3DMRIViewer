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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;

/**
 * Created by David Welker. Date: Aug 7, 2008 Time: 4:03:53 PM
 */
public class PermissionManager
{
	public static void makeNativeLibsExecutable() throws URISyntaxException
	{		
		for (Module module : ModuleTree.instance())
		{
			File executableLibDir = new File(module.getLibDir(), "exe");
			if( !executableLibDir.isDirectory() )
				continue;
			setAllExecutable(executableLibDir);
		}
	}

	private static void setAllExecutable(File dir)
	{
		boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.US).contains("windows");
		if (isWindows) return;
		try
		{
			Runtime.getRuntime().exec("chmod -R 777 " + dir.getPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
