/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-07-09 15:28:34 -0700 (Wed, 09 Jul 2014) $'
 * '$Revision: 32828 $'
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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by David Welker.
 * Date: Feb 17, 2010
 * Time: 6:14:29 PM
 */
public class Format extends ModulesTask
{
    private CodeFormatter cf;

    @Override
    public void run() throws Exception
    {
        File formatOptionsFile = new File(ProjectLocator.getBuildDir(), "resources/formatter/SunFormat.xml");
        cf = ToolFactory.createCodeFormatter(readFormatOptionsFile(formatOptionsFile));

        for (Module module : moduleTree)
        {
            if (module.getName().equals(Module.PTOLEMY) || 
            		module.getName().matches(Module.PTOLEMY_KEPLER+"-\\d+.\\d+(.\\d+)?") ||
            		module.getName().matches(Module.PTOLEMY+"-\\d+.\\d+(.\\d+)?"))
                continue;
            System.out.println("MODULE: " + module + "...");
            formatModule(module);
            System.out.println();
        }
    }

    private void formatModule(Module module)
    {
        File src = module.getSrc();
        if (!src.isDirectory())
            return;

        FileSet fs = new FileSet();
        fs.setProject(getProject());
        fs.setDir(module.getSrc());
        fs.setIncludes("**/*.java");

        Iterator<Resource> i = fs.iterator();

        while (i.hasNext())
        {
            Resource resource = i.next();
            if(resource instanceof FileResource) {
                File javaFile = ((FileResource)resource).getFile();
                formatFile(javaFile);
            }
        }


    }

    private Properties readFormatOptionsFile(File file)
    {
        try
        {
            return readFormatFileHelper(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private void formatFile(File file)
    {
        try
        {
            formatFileHelper(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }
    }

    private void formatFileHelper(File file) throws IOException, BadLocationException
    {
        IDocument doc = new Document();
        String contents = new String(Util.getFileCharContent(file, null));
        doc.set(contents);
        TextEdit edit = cf.format(CodeFormatter.K_COMPILATION_UNIT, contents, 0, contents.length(), 0, null);
        System.out.print("Formatting: " + file.getAbsolutePath() + ". ");
        if (edit != null)
        {
            System.out.println();
            edit.apply(doc);
        }
        else
        {
            System.out.println("FAILED.");
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(doc.get());
        out.flush();
    }

    private Properties readFormatFileHelper(File file) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        Properties properties = new Properties();
        properties.load(bis);
        return properties;
    }


}