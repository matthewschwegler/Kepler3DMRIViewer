<launch4jConfig>
  <headerType>gui</headerType>
  <dontWrapJar>false</dontWrapJar>
  <jar>${jarname}</jar>
  <outfile>${exefile}</outfile>
  <errTitle>keplersetup</errTitle>
  <chdir>.</chdir>
  <cmdLine></cmdLine>
  <customProcName>true</customProcName>
  <stayAlive>true</stayAlive>
  <icon>${icon}</icon>
  <jre>
    <path>jre</path> 
    <minVersion>1.6.0</minVersion>
  </jre>
  <versionInfo>
    <fileVersion>${appversion}</fileVersion>
    <txtFileVersion>${appversion}</txtFileVersion>
    <fileDescription>Ptolemy II Installer</fileDescription>
    <copyright>Copyright (c) 1995-2013 The Regents of the University of California. All rights reserved. http://kepler-project.org</copyright>
    <productVersion>${appversion}</productVersion>
    <txtProductVersion>${appversion}</txtProductVersion>
    <productName>${appname}</productName>
    <companyName>Kepler Project, University of California</companyName>
    <internalName>keplersetup</internalName>
    <originalFilename>keplersetup.exe</originalFilename>
  </versionInfo>
</launch4jConfig>
