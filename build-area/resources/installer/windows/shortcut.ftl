<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>

<shortcuts>

  <skipIfNotSupported />

  <programGroup defaultName="Kepler" location="applications"/>

  <shortcut
     name="Kepler ${appversion}"
     programGroup="yes"
     desktop="yes"
     applications="no"
     startMenu="yes"
     startup="no"
     target="$INSTALL_PATH\kepler.exe"
     commandLine=""
     description="Kepler - Scientific Workflows Tool"
     iconFile="$INSTALL_PATH\${commonmodule}\resources\icons\kepler.ico">
   </shortcut>

   <shortcut
     name="Kepler Module Manager ${appversion}"
     programGroup="yes"
     desktop="no"
     applications="no"
     startMenu="no"
     startup="no"
     target="$INSTALL_PATH\module-manager.exe"
     commandLine=""
     description="Kepler Module Manager - Scientific Workflows Tool"
     iconFile="$INSTALL_PATH\${commonmodule}\resources\icons\kepler.ico">
   </shortcut>

   <shortcut
     name="Kepler ${appversion} Uninstaller"
     programGroup="yes"
     desktop="no"
     applications="no"
     startMenu="yes"
     startup="no"
     target="$INSTALL_PATH\Uninstaller\uninstaller.jar"
     commandLine=""
	   iconFile="%SystemRoot%\system32\SHELL32.dll"
     iconIndex="31"
     description="This uninstalls Kepler">
   </shortcut>

</shortcuts>
