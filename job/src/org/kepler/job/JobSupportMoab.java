/*
 * Copyright (c) 2011-2012 The Regents of the University of California.
 * All rights reserved.
 *
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

package org.kepler.job;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Support class for MOAB job manager support used on Chinook.
 *  Class Job uses the methods of a supporter class to
 *  submit jobs and check status
 *
 * History: Copied from JobSubmitPBS and modified.
 * Settings taken from Ecce.
 */
public class JobSupportMoab implements JobSupport
{

    private static final Log log = LogFactory.getLog( JobSupportMoab.class.getName() );
    private static final boolean isDebugging = log.isDebugEnabled();
    private String _moabSubmitCmd="msub ";
    private String _moabStatusCmd="checkjob -A "; // to be followed by jobid
    private String _moabTaskStatusCmd="squeue -h -as -o %i | grep "; // to be followed by jobid
    private String _moabDeleteCmd="mjobctl -c "; // to be followed by jobid

    public JobSupportMoab()
    {
    }


    public void init( String moabBinPath )
    {
       if ( moabBinPath != null && !moabBinPath.trim().equals("") )  {
          String binPath = new String(moabBinPath);
          if ( ! moabBinPath.endsWith("/") )
             binPath += "/";
          _moabSubmitCmd = binPath + _moabSubmitCmd;
          _moabStatusCmd = binPath + _moabStatusCmd;
          _moabDeleteCmd = binPath + _moabDeleteCmd;
       }
    }

    /** Create a submission file for the specific job manager,
     *  based on the information available in Job:
     *   - executable name
     *   - input files
     *   - output files
     *   - arguments for the job
     */
    public boolean createSubmitFile ( String filename, Job job )
    {
       return false;
    }



    /** Submit command for Moab
     *   return: the command for submission
     */
    public String getSubmitCmd(String submitFile, String options, Job job) throws JobException
    {
        
        if(job.getDependentJobs() != null) {
            throw new JobException("Support for job dependencies with Moab has not been implemented.");
        }

       String _commandStr;
       if (options != null)
          _commandStr = _moabSubmitCmd + " " + options + " " + submitFile;
       else
          _commandStr = _moabSubmitCmd + " " + submitFile;

       return _commandStr;
    }


    /** Parse output of submission and get information: jobID
     *  return String jobID on success
     *  throws JobException at failure (will contain the error stream or output stream)
     */
    public String parseSubmitOutput (
          String output,
          String error ) throws JobException
    {

       // For successful submissions, the interactive session looks like:
       // [d39974@cu0login1 mpp-moabtesting]$ msub submit__mpp-moabtesting
       //
       // 106165
       // Ecce uses the following parse expresssion [0-9]+
       // Don't know what the error condtion looks like....
       String jobID = null;
       Pattern pattern = Pattern.compile("([0-9]+).*");

       String lines[] = output.split("\n");
       for (int idx=0; idx<lines.length; idx++) {
    	   Matcher matcher = pattern.matcher(lines[idx]);
          if (matcher.matches()) {
             jobID = matcher.group(1);
             break;
          }
       }

       if (isDebugging) {
          log.debug("Moab submit output: "+output);
          log.debug("Moab jobID = " + jobID);
       }

       if (jobID == null) {
          if (error != null && error.length() > 0)
             throw new JobException("Error submitting Moab job: " + error);
          else
             throw new JobException("Error submitting Moab job: " + output);
       }
       return jobID;
    }


    /** Get the command to ask the status of the job
     *   return: the String of command
     */
    public String getStatusCmd (String jobID)
    {
       return _moabStatusCmd + jobID;
    }

    /** Get the command to ask the status of each task
     *   return: the String of command
     */
    public String getTaskStatusCmd (String jobID)
    {
       return getStatusCmd(jobID) + ";" + _moabTaskStatusCmd + jobID;
    }

    /**
     * Parse output of status check command and get status info
     * @return: a JobStatusInfo object, or throws an JobException with the error output
     */
    public JobStatusInfo parseStatusOutput (
        String jobID,
        int exitCode,
        String output,
        String error )  throws JobException
    {
       // Output should be a single word indicating the status.
       // If the job doesn't exist, the output will be empty
       // The known values include:
       //    RUNNING
       //    COMPLETING
       //    PENDING
       //    IDLE
       //    STARTING
       //    BATCHHOLD
       //    SYSTEMHOLD
       //    USERHOLD
       //    DEFERRED
       //    MIGRATED
       //    STAGING

       // PBS status does not use exitCode. It can show error, but in real it can mean only that
       // job is not in the queue anymore, which is good...

       String lines[] = output.split("\n");
       for (int idx=0; idx<lines.length; idx++) {
           Pattern pattern = Pattern.compile(".*STATE\\=(.+);UNAME.*");
           Matcher matcher = pattern.matcher(lines[idx]);
           if (matcher.matches()) {
              output = matcher.group(1).toUpperCase();
              idx = lines.length;
           }
       }

       JobStatusInfo stat = new TaskParallelJobStatusInfo();
       stat.statusCode = JobStatusCode.NotInQueue;
       stat.jobID = jobID;

       boolean foundStatus = false;
       if (output.length() > 0) {
          if (output.equals("PENDING") ||
                output.equals("IDLE") ||
                output.equals("STARTING") ||
                output.equals("BATCHHOLD") ||
                output.equals("SYSTEMHOLD") ||
                output.equals("USERHOLD") ||
                output.equals("DEFERRED") ||
                output.equals("MIGRATED") ||
                output.equals("STAGING")) {
             foundStatus = true;
             stat.statusCode = JobStatusCode.Wait;
          } else if (output.equals("RUNNING")) {
             foundStatus = true;
             stat.statusCode = JobStatusCode.Running;
          } else if (output.equals("COMPLETED") ||
        		  	 output.equals("REMOVED")) {
             // Note sure - leave it at not in queue?
             foundStatus = true;
             stat.statusCode = JobStatusCode.NotInQueue;
          } else {
             foundStatus = true;
             stat.statusCode = JobStatusCode.Wait;
          }
       } else {
          stat.statusCode = JobStatusCode.NotInQueue;
       }

       if (!foundStatus) {
          // May want to look at err string or something here
       }

       return stat;
    }



    /**
     * Parse output of task status check command and get status info
     * @return: a JobStatusInfo object, or throws an JobException with the error output
     */
    public TaskParallelJobStatusInfo parseTaskStatusOutput (
        String jobID,
        int numTasks,
        int exitCode,
        String output,
        String error )  throws JobException
    {

       String[] lines = output.split("\n");

       TaskParallelJobStatusInfo jobStatus = 
    	   (TaskParallelJobStatusInfo)parseStatusOutput (jobID, exitCode, lines[0], error);

       jobStatus.taskStatusCodes = new HashMap<String,JobStatusCode>(numTasks);

       /*
        * if(code == JobStatusCode.Running){
					// this is the only unambiguous state so record it
					
				} else if(oldCode == null) {
					result.put(taskId,JobStatusCode.Wait);
				} else if(oldCode == JobStatusCode.Running && 
					code == JobStatusCode.NotInQueue) {
					result.put(taskId,JobStatusCode.NotInQueue);
				} else if(code == JobStatusCode.Running) {
					result.put(taskId,JobStatusCode.Running);
				}
        */
       if( jobStatus.statusCode == JobStatusCode.Running ) {
         for (int idx=1; idx<lines.length; idx++) {

            Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(lines[idx]);
            if (matcher.matches()) {
               String jobid = matcher.group(1);
               if( jobid.equals(jobID) ) {
                  String taskId = matcher.group(2);
                  jobStatus.taskStatusCodes.put(taskId,JobStatusCode.Running);
               }
            }
         }
         for( int idx = 0; idx < numTasks; idx++ ) {
            if(! jobStatus.taskStatusCodes.containsKey(Integer.toString(idx))) {
            	jobStatus.taskStatusCodes.put(Integer.toString(idx),JobStatusCode.NotInQueue);
            }
         }
       } else {
         for( int idx = 0; idx < numTasks; idx++ ) {
        	 jobStatus.taskStatusCodes.put(Integer.toString(idx),jobStatus.statusCode) ;
         }
       }
       
       return jobStatus;
    }


    /**
     * @return: the String of command
     */
    public String getDeleteCmd (String jobID)
    {
       return  _moabDeleteCmd + jobID;
    }




    /**
     * Parse output of delete command.
     * @return: true or false indicating that the command was successful or not
     */
    public boolean parseDeleteOutput( String jobID,
          int exitCode,
          String output,
          String error ) throws JobException
    {
       if (exitCode == 0)
          return true;
       else
          return false;
    }

}
