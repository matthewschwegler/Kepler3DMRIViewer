/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.kepler.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.ExecFactory;
import org.kepler.ssh.ExecInterface;
import org.kepler.util.FilenameFilter_RegularPattern;

/**
 * Get the actual list of files in a directory and provide a difference from the
 * previous listing. Do the listing either on a local dir with Java or on a
 * remote machine using SSH. Do the listing with a set of patterns. For each
 * file, provide its - name, - size (in bytes) - date (in UTC seconds)
 */
public class DirectoryListing {

	private boolean localExec = true; // local directory or a remote site?
	private File ldir; // the local directory (as File)
	private FilenameFilter_RegularPattern localFilter; // the local filename filter
	private ExecInterface execObj; // class for remote execution
	private String target; // the remote machine (user@host:port) if not local
	private String rdir; // the directory on the remote site (as String)

	private String[] filemasks;     // the file masks (as used in ls command)
	private FileInfo[] currentList; // the list of files after the last listing
	private Hashtable prevList; // the previous list of files (in hashTable for
								// fast lookup)

	private static final Log log = LogFactory.getLog(DirectoryListing.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private boolean sortByDate = true;

	private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm");
	private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private String currentYear;

	/**
	 * Constructor only for local directories. Throws IllegalArgumentException
	 * if local directory does not exist.
	 */
	public DirectoryListing(File directory, String[] filemasks)
			throws IllegalArgumentException {
		this(null, directory.getAbsolutePath(), filemasks);
	}

	/**
	 * Constructor for remote or local directories. For local machine, specify
	 * target as null, "", or "local"
	 */
	public DirectoryListing(String target, String directory, String[] filemasks)
			throws IllegalArgumentException {

		if (target == null)
			target = "local";
		this.target = target; // just for logging

		// get the host from target string
		String host;
		int atPos = target.indexOf('@');
		int colonPos = target.indexOf(':');
		if (colonPos >= 0 && colonPos > atPos)
			host = target.substring(atPos + 1, colonPos);
		else
			host = target.substring(atPos + 1);

		// local or remote target?
		if (host.trim().equals("") || host.trim().equals("local")) {
			localExec = true;
			ldir = new File(directory);
		} else {
			localExec = false;
                        try {
                                rdir = directory;
                                execObj = ExecFactory.getExecObject(target);

                        } catch (ExecException e) {
                                String errText = new String("Error at execution:\n"
                                        + e.getMessage());

                            log.error(errText);
                        }
		}

		this.filemasks = filemasks;
		setMask(this.filemasks);

		// set current year to parse output of 'ls -l' dates correctly
		int year = Calendar.getInstance().get(Calendar.YEAR);
		currentYear = new String("" + year);
		// System.out.println("Current year is: "+currentYear);

	}

	/**
	 * (Re)set the file mask. This can be reset before any listing. Previous
	 * listing will be forgotten.
	 */
	public void setMask(String[] filemasks) {
		// convert file masks pattern to regular expression
		if (filemasks != null && filemasks.length > 0) {
			localFilter = new FilenameFilter_RegularPattern();
			for (int i = 0; i < filemasks.length; i++) {
				String p1 = filemasks[i].replaceAll("\\.", "\\\\.");
				String p2 = p1.replaceAll("\\*", ".*");
				String p3 = p2.replaceAll("\\?", ".");
				String p4 = p3.replaceAll("\\+", "\\\\+");
				localFilter.add(p4);

				if (isDebugging)
					log.debug("pattern conversion: [" + p4 + "] = [" + p1
							+ "] -> [" + p2 + "] -> [" + p3 + "]");
			}
		}

		prevList = new Hashtable(); // forget all past elements
		currentList = null; // forget all last listed elements
	}

	/**
	 * Kept for compatibility with older codes. 
	 */
	public int list() throws ExecException {
		return list(false);
	}
	
	/**
	 * List the directory now. Returns the number of files, -1 on error;
	 * Input: useLsOnly: false -> execute 'ls -l' on remote machine
	 */
	public int list( boolean useLsOnly ) throws ExecException {
		FileInfo[] newList;
		if (localExec) {
			if (!ldir.isDirectory()) {
				throw new ExecException("org.kepler.io.DirectoryListing: "
						+ ldir + " is not an existing local directory.");
			}
			File[] files = ldir.listFiles(localFilter);
			newList = new FileInfo[files.length];
			for (int i = 0; i < files.length; i++)
				newList[i] = new FileInfo(files[i].getName(),
						files[i].length(), files[i].lastModified() / 1000);
		} else {
			// String command = new String("ls -tr " + rdir + (pattern!=null ?
			// " | egrep "+pattern : ""));
			String command;
			if (useLsOnly) {
				command = new String("ls " + rdir);
			} else {
		 		StringBuffer cmd = new StringBuffer("cd " + rdir + "; ls -ld");
				for (int i = 0; i < filemasks.length; i++) {
					cmd.append(" "+filemasks[i]);
				}
				command = cmd.toString();
			}
			
			ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
			ByteArrayOutputStream streamErr = new ByteArrayOutputStream();

			//System.out.println("Exec command: " + command);
			int exitCode = execObj.executeCmd(command, streamOut, streamErr);

			if (exitCode != 0 && exitCode != 2) { // 2: not all masks produced result
				log.error("Error when making connection to " + target
						+ ": exitCode = " + exitCode + "  Stdout= \n"
						+ streamOut + "  Stderr= \n" + streamErr);
				return -1;
			}

			//System.out.println("---- result from machine ----\n" +
			//     streamOut);
			newList = filter(streamOut.toString().split("\n"), useLsOnly);
			//System.out.println("---- result after filter: " + newList);

		}

		if (sortByDate && !useLsOnly) {
			long stime = System.currentTimeMillis();
			Arrays.sort(newList, new Comparator() {
				public int compare(Object a, Object b) {
					FileInfo f1 = (FileInfo) a;
					FileInfo f2 = (FileInfo) b;

					long diff = (f1.getDate() - f2.getDate());
					int retval = 0;
					if (diff < 0)
						retval = -1;
					else if (diff > 0)
						retval = 1;
					else
						retval = f1.getName().compareTo(f2.getName());

					// System.out.println(f1.getDate() + "\t" + f1.getName() +
					// "\t" +
					// f2.getDate() + "\t" + f2.getName() + "\t" +
					// (int) diff + "\t" + retval);

					return retval;

					// return f1.getName().compareTo(f2.getName());
				}
			});

			long len = System.currentTimeMillis() - stime;

			// System.out.println("--- Sorted list ---");
			// for (int i=0; i<files.length; i++)
			// System.out.println(files[i].lastModified() + "\t" +
			// files[i].getName() );
			// System.out.println("-----------------");
			// System.out.println("Time to sort: " + len);
			// System.exit(1);
		}

		// first, put the previous list into a hash set
		if (currentList != null) {
			for (int i = 0; i < currentList.length; i++) {
				prevList.put(currentList[i].getName(), currentList[i]);
			}
			if (isDebugging)
				log.debug("\n" + currentList.length
						+ " items added to hash set of total size "
						+ prevList.size() + "\n");
		}
		currentList = newList;
		return currentList.length;
	}

	private FileInfo[] filter(String[] files, boolean lsOnly) {
		ArrayList al = new ArrayList();
		// System.out.println("---- Filtering ----\n");

		if (files == null)
			return new FileInfo[] {};

		for (int i = 0; i < files.length; i++) {
			String[] fi = files[i].split(" +", 9); // split the a line of
													// "ls -l"
			// 1. ls output format is just a single file name per file
			// 2. traditional ls -l output format
			//  -rw-r--r--   1 pnb  ORNL\dom  775280 Aug 21 14:45 coupling.pdf
			//  -rw-r--r--   1 pnb  ORNL\dom  988107 Jul 30  2008 xgcmonwf.pdf
			// fi[4] is the size, fi[5]-fi[7] gives the date, fi[8] is the name
			//
			// 3. newer ls -l output format
			// -rw-r--r--  1 pnorbert ccsstaff       572 2008-02-01 13:15 sshterm.txt
			// fi[4] is the size, fi[5]-fi[6] gives the date, fi[7] is the name

			// System.out.println("    " + files[i]);

			if (lsOnly) {
				// 1. ls output
				if (localFilter.accept(null, fi[0])) {
					FileInfo fileInfo = new FileInfo( fi[0], -1, -1);
					al.add(fileInfo);
				}

			} else if (fi.length >= 9 && localFilter.accept(null, fi[8])) {
				// 2. traditional ls -l output format
				// System.out.println("--- " + fi[0] + " | " + fi[1] + " | " +
				// fi[2] + " | " +
				// fi[3] + " | " + fi[4] + " | " + fi[5] + " | " +
				// fi[6] + " | " + fi[7] + " | " + fi[8] );
				Long size = new Long(fi[4]);
				FileInfo fileInfo = new FileInfo(fi[8], size.longValue(),
						getUTC(fi[5], fi[6], fi[7]));
				al.add(fileInfo);
				// System.out.println("+ " + files[i]);

			} else if ( fi.length == 8 && localFilter.accept(null, fi[7].trim() )) {
				// 3. newer ls -l output format
				//System.out.println("--- " + fi[0] + " | " + fi[1] + " | " + fi[2] + " | " +
				//				fi[3] +  " | " + fi[4] + " | " + fi[5] + " | " +
				//				fi[6] +  " | " + fi[7] );
                Long size = new Long(fi[4]);
                FileInfo fileInfo = new FileInfo( fi[7].trim(), size.longValue(),
                                                  getUTC(fi[5], fi[6]) );
                al.add(fileInfo);
				//System.out.println("+ " + files[i]);
			} else {
				// System.out.println("  " + files[i]);
			}
		}

		FileInfo[] newList = new FileInfo[al.size()];
		for (int i = 0; i < al.size(); i++) {
			newList[i] = (FileInfo) al.get(i);
		}
		return newList;
	}

	/*
	 * create the UTC seconds date from the 'ls -l' output (max minute
	 * resolution...)
	 */
	private long getUTC(String month, String day, String timeORyear) {
		String dateStr;
		if (timeORyear.indexOf(':') > -1) { // this is hh:mm
			dateStr = month + " " + day + " " + currentYear + " " + timeORyear;
		} else { // this is yyyy year
			dateStr = month + " " + day + " " + timeORyear + " 00:00";
		}

		ParsePosition pp = new ParsePosition(0);
		Date d = sdf.parse(dateStr, pp);
		// System.out.println(" Parsed " + dateStr + " to " + d);
		long utc = d.getTime() / 1000;
		return utc;
	}

/* create the UTC seconds date from the new 'ls -l' output (max minute resolution...)
        -rw-r--r--  1 pnorbert ccsstaff       572 2008-02-01 13:15 sshterm.txt
*/
	private long getUTC( String date_, String time_) {
		String dateStr = date_ + " " + time_;
		ParsePosition pp = new ParsePosition(0);
		Date d = sdf2.parse( dateStr, pp);
		//System.out.println("getUTC: Parsed " + dateStr + " to " + d);
		long utc = d.getTime() / 1000;
		return utc;
	}

	/** Get the list itself. */
	public FileInfo[] getList() {
		return currentList;
	}

	/**
	 * Get the list of 'new' files, i.e. that are in currentList and not in
	 * prevList. If parameter 'checkModifications' is false, the difference
	 * between currentList and prevList will be based on the file names only. If
	 * it is true, files' date and size is also checked, i.e. modified files
	 * will also be retruned.
	 * 
	 * @return FileInfo[] of filenames
	 */
	public FileInfo[] getNewFiles(boolean checkModifications) {

		if (currentList == null)
			return null;

		int initial_length = currentList.length - prevList.size();
		if (initial_length <= 0)
			initial_length = 1;

		ArrayList newList = new ArrayList(initial_length);
		// the final size can be greater if modified files are inserted as well

		boolean isNew;
		for (int i = 0; i < currentList.length; i++) {
			// it is a new file
			// if the current element's name is not in the hash table
			isNew = !prevList.containsKey(currentList[i].getName());

			if (checkModifications && !isNew) {
				// file is in the hash table
				// but we should check the size and date as well
				FileInfo old = (FileInfo) prevList
						.get(currentList[i].getName());
				if (old.getDate() != currentList[i].getDate()
						|| old.getSize() != currentList[i].getSize()) {

					isNew = true;
					log.debug("Modified file found: "
							+ currentList[i].getName() + "; size "
							+ currentList[i].getSize() + "/" + old.getSize()
							+ "; date " + currentList[i].getDate() + "/"
							+ old.getDate());
				}

			}

			if (isNew) {
				newList.add(currentList[i]);
			}
		}

		log.debug("Number of new files = " + newList.size());

		return (FileInfo[]) newList.toArray(new FileInfo[0]);
	}

}
