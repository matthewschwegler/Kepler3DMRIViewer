import java.io.*;
import java.util.*;

/**
 * Class to install the NMI build onto the submit host.  It creates all of the
 * symlinks in the home directory and alters the submit file so that it 
 * has the correct link names.  If the links already exist, the names are changed
 * on the symlinks and in the submit file.
 */
public class NMIInstaller
{ 
  private static Hashtable filenameHash;
  
  public static void main(String args[])
  { 
    if(args.length == 0)
    {
      System.out.println("ERROR: you must provide the suite name as the first " +
        "argument to the NMIInstaller.");
      System.exit(0);
    }
    
    String suiteName = args[0];
    
    System.out.println("****Running NMI Installer.****");
    System.out.println("Suite is " + suiteName);
    try
    {
      File homedir = new File("../");
      File currentdir = new File("./");
      String[] currentdirFiles = currentdir.list();
      String[] homedirFiles = homedir.list();
      
      //find the input and output files
      Vector inputs = new Vector();
      for(int i=0; i<currentdirFiles.length; i++)
      {
        if(currentdirFiles[i].endsWith(".svn"))
        {
          inputs.addElement(currentdirFiles[i]);
        }
      }
      
      Vector outputs = new Vector();
      for(int i=0; i<currentdirFiles.length; i++)
      {
        if(currentdirFiles[i].endsWith(".out"))
        {
          outputs.addElement(currentdirFiles[i]);
        }
      }
            
      Hashtable newInputs = createSymlinks(inputs, homedirFiles, currentdir);
      Hashtable newOutputs = createSymlinks(outputs, homedirFiles, currentdir);
      createSubmitSymlink(suiteName, homedirFiles);
      
      updateSubmitFile(suiteName + "-submit", inputs, outputs, newInputs, newOutputs);
      
      
      System.out.println("****NMI Installer Done.****");
    }
    catch(Exception e)
    {
      System.out.println("Error: Installer has failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /**
   * create the symlink for the submit file
   */
  private static void createSubmitSymlink(String suiteName, String[] homedirFiles)
    throws Exception
  {
    Runtime rt = Runtime.getRuntime();
    //need to make the link for the submit file
    String submitName = suiteName + "-submit";
    String path = (new File(submitName)).getAbsolutePath();
    String link;
    boolean exists = false;
    for(int j=0; j<homedirFiles.length; j++)
    { //check for duplicate filenames
      if(submitName.equals(homedirFiles[j]))
      { //need to change the name of this symlink
        exists = true;
        break;
      }
    }
    
    link = "../" + submitName;
      
    System.out.println("Creating symlink for submit file: ln -s " + path + " " + link);
    String[] cmd = {"ln", "-s", path, link};
    Process p = rt.exec(cmd);
    InputStream stderr = p.getErrorStream();
    InputStreamReader isr = new InputStreamReader(stderr);
    BufferedReader br = new BufferedReader(isr);
    String line = null;
    
    while ( (line = br.readLine()) != null)
    {
      System.out.print("ERROR: ");
      System.out.println(line);
    }
  }
  
  /**
   * update the submit file with the new names of the inputs and outputs if
   * there are any.
   */
  private static void updateSubmitFile(String submitFilename, Vector inputs,
    Vector outputs, Hashtable newInputs, Hashtable newOutputs)
      throws Exception
  {
    File submitFile = new File(submitFilename);
    FileReader fr = new FileReader(submitFile);
    char[] c = new char[1024];
    int numread = fr.read(c, 0, 1024);
    StringBuffer sb = new StringBuffer();
    while(numread != -1)
    {
      sb.append(c, 0, numread);
      numread = fr.read(c, 0, 1024);
    }
    String submitFileContents = sb.toString();
    
    for(int i=0; i<inputs.size(); i++)
    {
      String input = (String)inputs.elementAt(i);
      String newInput = (String)newInputs.get(input);
      if(!newInput.trim().equals(input.trim()))
      { //find the old instance of the name and replace it with the new one
        System.out.println("replacing " + input + " with " + newInput + " in the submit file");
        submitFileContents = submitFileContents.replaceAll(input, newInput);
      }
    }
    
    for(int i=0; i<outputs.size(); i++)
    {
      String output = (String)outputs.elementAt(i);
      String newOutput = (String)newOutputs.get(output);
      if(!newOutput.trim().equals(output.trim()))
      { //find the old instance of the name and replace it with the new one
        System.out.println("replacing " + output + " with " + newOutput + " in the submit file");
        submitFileContents = submitFileContents.replaceAll(output, newOutput);
      }
    }
    
    //re-write the contents of the submit file
    FileWriter fw = new FileWriter(submitFilename);
    fw.write(submitFileContents, 0, submitFileContents.length());
    fw.flush();
    fw.close();
    
  }
  
  /**
   * process the links
   */
  private static Hashtable createSymlinks(Vector inputs, String[] homedirFiles, 
    File currentdir)  throws Exception
  {
    Hashtable newInputs = new Hashtable();
    Runtime rt = Runtime.getRuntime();
    for(int i=0; i<inputs.size(); i++)
    {
      String inputName;
      String input = (String)inputs.elementAt(i);
      String path = currentdir.getAbsolutePath()  + "/" + input;
      String link;
      
      inputName = input.substring(0, input.length() - 4) + 
        input.substring(input.length() - 4, input.length());
      link = "../" + inputName;
      
      System.out.println("Creating symlink: ln -s " + path + " " + link);
      String[] cmd = {"ln", "-s", path, link};
      Process p = rt.exec(cmd);
      InputStream stderr = p.getErrorStream();
      InputStreamReader isr = new InputStreamReader(stderr);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      
      while ( (line = br.readLine()) != null)
      {
        System.out.print("ERROR: ");
        System.out.println(line);
      }
      
      newInputs.put(input, inputName);
    }
    
    return newInputs;
  }
}
