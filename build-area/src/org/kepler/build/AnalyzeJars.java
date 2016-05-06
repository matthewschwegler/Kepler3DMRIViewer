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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.CRC32;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;

/**
 * Created by Chad Berkley
 * Date: May 13, 2009
 * Analyze the jars in a suite and locate possible duplicates
 */
public class AnalyzeJars extends ModulesTask
{
    private int keyLength = 0;
    private String keyType;
    private File logfile;

    /**
     * set keylength to '0' to use the entire key (either filename or crc)
     */
    public void setKeyLength(String keyLength)
    {
        this.keyLength = (new Integer(keyLength)).intValue();
    }

    /**
     * allowed values are "filename" and "checksum"
     */
    public void setKeyType(String keyType)
    {
        this.keyType = keyType;
    }

    public void setLogfile(String logfile)
    {
        this.logfile = new File(basedir, logfile);
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        //get a list of all the jars in the system
        Vector jarList = getJarList();
        //System.out.println("jarList: " + jarList.toString());

        //get a vector of vectors of similar jars
        Hashtable similarJars = findSimilarJars(jarList);
        String output = printHashtable(similarJars);
        if (logfile != null)
        {
            writeLogfile(output);
        }
        //System.out.println("similars: " + similarJars.toString());

        //Hashtable similarChecksumJars = findSameChecksums(similarJars);
        //printHashtable(similarChecksumJars);
    }

    /**
     * write a log file from output
     */
    private void writeLogfile(String output)
    {
        System.out.println("Writing log file to " + logfile.getAbsolutePath());
        try
        {
            FileWriter fw = new FileWriter(logfile);
            fw.write(output, 0, output.length());
            fw.flush();
            fw.close();
        }
        catch (Exception e)
        {
            System.out.println("Could not write the log file: " + e.getMessage());
        }
    }

    /**
     * check jars hashed together by other methods with a checksum
     */
    private Hashtable findSameChecksums(Hashtable h)
    {
        Enumeration enu = h.keys();
        while (enu.hasMoreElements())
        {
            String key = (String) enu.nextElement();
            Vector v = (Vector) h.get(key);
            for (int i = 0; i < v.size(); i++)
            {

            }
        }

        return h;
    }

    /**
     * create a vector of vectors of similar jars
     * the bases for similarity are:
     * 1) filename
     * 2) size
     * 3) contents
     */
    private Hashtable findSimilarJars(Vector jarList)
    {
        Hashtable similars = new Hashtable();
        //first look for jars with similar filenames
        for (int i = 0; i < jarList.size(); i++)
        {
            Vector sims = new Vector();
            FileContainer jarFileContainer = (FileContainer) jarList.elementAt(i);
            File jarFile = jarFileContainer.file;
            String fileName = jarFile.getName();
            String key = getKey(jarFileContainer);

            for (int j = 0; j < jarList.size(); j++)
            {
                if (i == j)
                {
                    continue;
                }

                FileContainer jarFileContainer2 = (FileContainer) jarList.elementAt(j);
                File jarFile2 = jarFileContainer2.file;
                String fileName2 = jarFile2.getName();
                String key2 = getKey(jarFileContainer2);
                if (key.equals(key2))
                {
                    Vector v = (Vector) similars.get(key);
                    if (v != null && v.size() > 0)
                    {
                        if (!checkForDuplicate(v, jarFileContainer2))
                        {
                            v.addElement(new FileContainer(jarFile2));
                            similars.put(key, v);
                        }
                    }
                    else
                    {
                        v = new Vector();
                        v.addElement(new FileContainer(jarFile2));
                        similars.put(key, v);
                    }
                }
            }
        }
        return similars;
    }

    /**
     * check to see if obj is already in v. return true if it does
     */
    private boolean checkForDuplicate(Vector v, FileContainer fc)
    {
        for (int i = 0; i < v.size(); i++)
        {
            FileContainer fc2 = (FileContainer) v.elementAt(i);
            if (fc2.compare(fc))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * return a hash key from a seed
     */
    private String getKey(FileContainer fc)
    {
        String seed;
        String key;
        if (keyType.equals("filename"))
        {
            seed = fc.file.getName();
            if (keyLength == 0)
            { //use the whole filename
                return seed;
            }
            else if (seed.length() > keyLength)
            {
                key = seed.substring(0, keyLength);
            }
            else
            {
                key = seed;
            }
        }
        else if (keyType.equals("checksum"))
        {
            key = new Long(fc.crc).toString();
        }
        else
        {
            key = fc.file.getName();
        }

        return key;
    }

    /**
     * get the list of jars in the suite
     */
    private Vector getJarList()
    {
        Vector jarFiles = new Vector();
        for (Module module : moduleTree)
        { //go through each module
            //System.out.println("analyzing jars in module " + module);
            File libJarsDir = new File(basedir, module + "/lib/jar");
            if (libJarsDir.exists())
            {
                Vector libJarDirFiles = getJarsInDir(libJarsDir);
                jarFiles.addAll(libJarDirFiles);
            }
        }

        return jarFiles;
    }

    /**
     * get the jars in a directory
     */
    private Vector getJarsInDir(File dir)
    {
        Vector v = new Vector();
        if (!dir.isDirectory())
        {
            return v;
        }

        String[] dirContent = dir.list();
        for (int i = 0; i < dirContent.length; i++)
        {
            File libJarFile = new File(dir, dirContent[i]);
            if (libJarFile.isDirectory())
            {
                //System.out.println("recursing into directory " + libJarFile.getAbsolutePath());
                v.addAll(getJarsInDir(libJarFile));
            }
            else
            {
                if (libJarFile.getName().endsWith(".jar"))
                {
                    //System.out.println("adding " + libJarFile.getAbsolutePath());
                    v.add(new FileContainer(libJarFile));
                }
            }
        }

        return v;
    }

    /**
     * print a hashtable in a nice fashion
     */
    private String printHashtable(Hashtable h)
    {
        String output = "";
        Enumeration enu = h.keys();
        while (enu.hasMoreElements())
        {
            String key = (String) enu.nextElement();
            Vector v = (Vector) h.get(key);
            output += key + " = ";
            output += "{\n";
            for (int i = 0; i < v.size(); i++)
            {
                output += "     " + v.elementAt(i).toString() + "\n";
            }
            output += "}\n\n";
        }

        return output;
    }

    /**
     * class to hold a file and a crc checksum
     */
    private class FileContainer
    {
        public File file;
        public long crc;

        /**
         * create a new FileContainer
         */
        public FileContainer(File f)
        {
            try
            {
                this.file = f;
                CRC32 crc = new CRC32();
                FileInputStream fis = new FileInputStream(f);
                byte[] b = new byte[1024];
                int numread = fis.read(b, 0, 1024);
                while (numread != -1)
                {
                    crc.update(b, 0, numread);
                    numread = fis.read(b, 0, 1024);
                }
                this.crc = crc.getValue();
            }
            catch (Exception e)
            {
                System.out.println("Could not read file " + f);
            }
        }

        /**
         * compare this FileContainer to another one and return true if they have
         * the same filename and same crc
         */
        public boolean compare(FileContainer fc)
        {
            if (this.file.getAbsolutePath().equals(fc.file.getAbsolutePath())
                    && this.crc == fc.crc)
            {
                return true;
            }
            return false;
        }

        /**
         * return a string rep of the FileContainer
         */
        public String toString()
        {
            return "{" + file.toString() + ", " + crc + "}";
        }
    }
}
