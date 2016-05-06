package org.sdm.spa.actors.transport;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RunGenericFileCopierTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for org.sdm.spa.actors.transport");
    //$JUnit-BEGIN$
    try {
      //Test Basic functionality
       
   
      suite.addTest(new GenericFileCopierTest("testLocalCopy"));
      suite.addTest(new GenericFileCopierTest("testLocalCopyDir"));
      
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteMultFilesWithSpacesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteRegularExprFullPathToDirectory"));
      
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalMultFilesWithSpacesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalRegularExprFullPathToDirectory"));
      
      
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestMultFilesWithSpacesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestRegularExprFullPathToDirectory"));
      
      suite.addTest(new GenericFileCopierTest("testSCPRemoteDestToRemoteSrcSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteDestToRemoteSrcSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteDestToRemoteSrcMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteDestToRemoteSrcRegularExprFullPathToDirectory"));
      
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteDir"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalDir"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestDir"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSCPLocalToRemoteMultDirectoryAndFilesAndRegExp"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteToLocalMultDirectoryAndFilesAndRegExp")); 
      
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteSrcToRemoteDestMultDirectoryAndFilesAndRegExp"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteDestToRemoteSrcMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteDestToRemoteSrcMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSCPRemoteDestToRemoteSrcMultDirectoryAndFilesAndRegExp"));
      
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteRegularExprFullPathToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteToLocalSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteToLocalSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteToLocalMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteToLocalRegularExprFullPathToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestRegularExprFullPathToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteDestToRemoteSrcSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteDestToRemoteSrcSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteDestToRemoteSrcMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteDestToRemoteSrcRegularExprFullPathToDirectory"));
      
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteDir"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteToLocalDir"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestDir"));
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSFTPLocalToRemoteMultDirectoryAndFilesAndRegExp"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteToLocalMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteToLocalMultDirectoryAndFilesAndRegExp"));
      
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteSrcToRemoteDestDirectoryAndFilesAndRegExp"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteDestToRemoteSrcMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteDestToRemoteSrcMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testSFTPRemoteDestToRemoteSrcDirectoryAndFilesAndRegExp"));
      
      suite.addTest(new GenericFileCopierTest("testDefaultProtocolLocalToRemote")); 
      suite.addTest(new GenericFileCopierTest("testDefaultProtocolRemoteToLocal"));
      suite.addTest(new GenericFileCopierTest("testDefaultProtocolRemoteToRemote")); 
      
      
      
      
      suite.addTest(new GenericFileCopierTest("testSRMLiteLocalToRemoteSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteLocalToRemoteSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteLocalToRemoteMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteToLocalSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteToLocalSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteToLocalMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteSrcToRemoteDestSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteSrcToRemoteDestSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteSrcToRemoteDestMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteDestToRemoteSrcSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteDestToRemoteSrcSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteDestToRemoteSrcMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteLocalToRemoteDir"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteToLocalDir"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteSrcToRemoteDestDir"));
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteDestToRemoteSrcDir"));
      
      suite.addTest(new GenericFileCopierTest("testSRMLiteLocalToRemoteRegularExprFullPathToDirectory")); 
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteToLocalRegularExprFullPathToDirectory")); 
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteSrcToRemoteDestRegularExprFullPathToDirectory")); 
      
      suite.addTest(new GenericFileCopierTest("testSRMLiteRemoteDestToRemoteSrcRegularExprFullPathToDirectory")); //---Should Fail gracefully---
          
     
    	
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestMultFilesToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcSingleFileDefaultPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcSingleFileFullPath"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcSingleFileToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcMultFilesToDirectory"));
        
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteRegularExprFullPathToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalRegularExprFullPathToDirectory")); 
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestRegularExprFullPathToDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcRegularExprFullPathToDirectory")); //---Should Fail gracefully---
        
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteDir"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalDir"));
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteMultDirectoryAndFiles")); 
      suite.addTest(new GenericFileCopierTest("testBBCPLocalToRemoteMultDirectoryAndFilesAndRegExp")); 
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalMultDirectoryAndFiles")); 
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteToLocalMultDirectoryAndFilesAndRegExp"));
      
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteSrcToRemoteDestMultDirectoryAndFilesAndRegExp"));
      
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcMultDirectory"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcMultDirectoryAndFiles"));
      suite.addTest(new GenericFileCopierTest("testBBCPRemoteDestToRemoteSrcMultDirectoryAndFilesAndRegExp")); //---Should Fail gracefully---
    	
    	
    //******************************Testing Existing methods****************************************************************
    suite.addTest(new GenericFileCopierTest("testTimeoutL2RBBCP"));
    suite.addTest(new GenericFileCopierTest("testTimeoutL2RSRMLite"));
    suite.addTest(new GenericFileCopierTest("testTimeoutR2LBBCP"));
    suite.addTest(new GenericFileCopierTest("testTimeoutR2LSRMLite"));
    suite.addTest(new GenericFileCopierTest("testTimeoutR2RSCP"));
    suite.addTest(new GenericFileCopierTest("testTimeoutR2RSFTP"));
    suite.addTest(new GenericFileCopierTest("testTimeoutR2RBBCP"));
    suite.addTest(new GenericFileCopierTest("testTimeoutR2RSRMLite"));
    suite.addTest(new GenericFileCopierTest("testValidCmdOptionsL2R"));
    suite.addTest(new GenericFileCopierTest("testValidCmdOptionsR2L"));
    suite.addTest(new GenericFileCopierTest("testValidCmdOptionsR2R"));
    suite.addTest(new GenericFileCopierTest("testInvalidCmdOptionsL2R"));
    suite.addTest(new GenericFileCopierTest("testInvalidCmdOptionsR2L"));
    suite.addTest(new GenericFileCopierTest("testInvalidCmdOptionsR2R"));
    	
    suite.addTest(new GenericFileCopierTest("testNonDirDestL2R"));
    suite.addTest(new GenericFileCopierTest("testNonDirDestR2L"));
    suite.addTest(new GenericFileCopierTest("testNonDirDestR2R"));
    suite.addTest(new GenericFileCopierTest("testNonDirSrcL2R"));
    suite.addTest(new GenericFileCopierTest("testNonDirSrcR2L"));
    suite.addTest(new GenericFileCopierTest("testNonDirSrcR2R"));
    suite.addTest(new GenericFileCopierTest("testNonFileSrcL2R"));   
    
        
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    //$JUnit-END$
    return suite;
  }

}
