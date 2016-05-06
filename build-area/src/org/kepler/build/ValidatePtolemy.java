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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Properties;

import org.apache.tools.ant.taskdefs.email.EmailTask;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;

/**
 * validate ptolemy to make sure it compiles
 * TODO: add additional tests.  See bugzilla for details
 *
 * @author berkley
 */
public class ValidatePtolemy extends ModulesTask
{

    /**
     * run the task
     */
    @Override
	public void run() {
		try {
			_run();
		}
		catch(Exception ex) {
			// Send email
			//sendEmail();
		}
	}
	
    private void _run() throws Exception
    {
        ChangeTo changeTo = new ChangeTo();
        changeTo.bindToOwner(this);
        changeTo.init();
        changeTo.setSuite(Module.KEPLER);
        changeTo.execute();

        CleanAll cleanAll = new CleanAll();
        cleanAll.bindToOwner(this);
        cleanAll.init();
        cleanAll.execute();

        UpdateModules update = new UpdateModules();
        update.bindToOwner(this);
        update.init();
        update.execute();

        UpdatePtolemy updatePtolemy = new UpdatePtolemy();
        updatePtolemy.bindToOwner(this);
        updatePtolemy.init();
        updatePtolemy.setRevision("head");
        updatePtolemy.execute();

        CompileModules compile = new CompileModules();
        compile.bindToOwner(this);
        compile.init();
        compile.execute();

        Integer revision = getCurrentPtolemyRevision();
        if(revision == null) {
            PrintError.message("Could not find Ptolemy revision.");
            return;
        }
        writeRevision(revision);

        CheckIn checkIn = new CheckIn();
        checkIn.bindToOwner(this);
        checkIn.init();
        checkIn.setModule(Module.make(Module.PTOLEMY));
        checkIn.execute();
    }

    /**
     * get the current "stable" ptolemy revision
     *
     * @return If the Ptolemy II svn revision can be parsed, the revision number.
     * Otherwise, returns null.
     * @throws IOException
     */
    protected Integer getCurrentPtolemyRevision() throws IOException
    {
        String[] infoCommand = {"svn", "info", Module.make(Module.PTOLEMY).getSrc().getAbsolutePath()};
        Process p = Runtime.getRuntime().exec(infoCommand);

        try(Reader reader = new InputStreamReader(p.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(reader)) {
            
            String line = null;
        
            while ((line = bufferedReader.readLine()) != null)
            {
                if (line.startsWith("Revision:"))
                {
                    String[] parts = line.split("\\s+");
                    if(parts.length == 2) {
                        return Integer.valueOf(parts[1]);
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * write a revision file
     *
     * @param revision
     */
    private void writeRevision(Integer revision)
    {
        try
        {
            writeRevisionHelper(revision);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * write the file
     *
     * @param revision
     * @throws IOException
     */
    private void writeRevisionHelper(Integer revision) throws IOException
    {
        File ptolemyRevisionTxt = new File(Module.make(Module.PTOLEMY).getModuleInfoDir(), "revision.txt");
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(ptolemyRevisionTxt)));
        pw.println(revision);
        pw.close();
    }
	
	private void sendEmail() {
		System.out.println("Sending email...");
		EmailTask et = new EmailTask();
		et.bindToOwner(this);
		et.init();
		File file = ProjectLocator.getBuildResourceFile("email", "credentials.xml");
		if (!file.isFile()) {
			System.out.println("Credentials not found, aborting email");
			return;
		}
		Properties credentials = readCredentials(file);
		et.setMailhost(credentials.getProperty("ERROR_SMTP_HOST"));
		et.setUser(credentials.getProperty("ERROR_SMTP_USERNAME"));
		et.setSSL(true);
		et.setSubject("Test Message");
		et.setFrom("swriddle@gmail.com");
		et.setToList("swriddle@gmail.com");
		et.setMailport(Integer.valueOf(credentials.getProperty("ERROR_SMTP_PORT")));
		et.setPassword(credentials.getProperty("ERROR_SMTP_PASSWORD"));
		System.out.println("All configured");
		et.execute();
		System.out.println("Executed");
	}

	private Properties readCredentials(File file) {
				
		Properties props = new Properties();
		InputStream fis = null;
		try {
			fis = new FileInputStream(file);
			props.load(fis);
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		finally {
			if (fis != null) {
				try {
					fis.close();
				}
				catch(IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		
		return props;
	}
	

}
