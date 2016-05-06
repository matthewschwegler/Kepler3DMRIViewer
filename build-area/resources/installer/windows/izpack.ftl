<installation version="1.0">
    <info>
        <appname>${appname}</appname>
        <appversion>${appversion}</appversion>
        <url>http://www.kepler-project.org</url>
        <appsubpath>${appname}-${appversion}</appsubpath>
        <run-privileged condition="izpack.windowsinstall"/>
    </info>
    <guiprefs width="800" height="600" resizable="no"/>
    <conditions>
      <condition type="variable" id="startKepler">
	<name>startKepler</name>
	<value>true</value>
      </condition>
    </conditions>
    <locale><langpack iso3="eng"/></locale>
    <variables>
        <variable name="DesktopShortcutCheckboxEnabled" value="true"/>
    </variables>
    <resources>
        <res id="HTMLInfoPanel.info" src="../../finished-kepler-installers/temp/README.html"/>
        <res src="resources/installer/izpack/border1.png" id="Installer.image.0"/>
        <res src="resources/installer/izpack/border2.png" id="Installer.image.1"/>
        <res src="resources/installer/izpack/border3.png" id="Installer.image.2"/>
        <res src="resources/installer/izpack/border4.png" id="Installer.image.3"/>
        <res src="resources/installer/izpack/border5.png" id="Installer.image.4"/>
        <res src="resources/installer/izpack/border5.png" id="Installer.image.5"/>
        <res src="resources/installer/izpack/border6.png" id="Installer.image.6"/>
        <res src="resources/installer/izpack/border7.png" id="Installer.image.7"/>
        <res src="resources/installer/izpack/border8.png" id="Installer.image.8"/>
        <res src="resources/installer/izpack/ProcessPanel.Spec.xml" id="ProcessPanel.Spec.xml"/>
 	<res src="resources/installer/izpack/userInputSpec.xml" id="userInputSpec.xml" />
        <res src="../../finished-kepler-installers/temp/shortcutSpec.xml" id="shortcutSpec.xml"/>
    </resources>
    <native type="izpack" name="ShellLink_x64.dll"/>
    <native type="izpack" name="ShellLink.dll"/>
    <panels>
        <panel classname="HelloPanel"/>
        <panel classname="HTMLInfoPanel"/>
        <panel classname="TargetPanel"/>
	<!-- If Kepler is the only panel, then don't list it. http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5020 -->
        <!-- panel classname="PacksPanel"/ -->

        <panel classname="InstallPanel"/>
        <panel classname="ShortcutPanel" os="windows"/>

	<!-- Ask the user if they want to start Kepler.  http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4989 -->
 	<panel classname="UserInputPanel"/>
        <panel classname="ProcessPanel" condition="startKepler"/>

        <panel classname="SimpleFinishPanel"/>
    </panels>
    <packs>
        <pack name="Kepler" required="yes">
            <description>The Kepler workflow system.</description>
            <#list standardModules as module>
            <fileset dir="${basedir}" targetdir="$INSTALL_PATH">
                <include name="${module.name}/configs/**/*"/>
                <include name="${module.name}/demos/**/*"/>
                <include name="${module.name}/lib/**/*"/>
                <include name="${module.name}/lib64/**/*"/>
                <include name="${module.name}/module-info/**/*"/>
                <include name="${module.name}/resources/**/*"/>           
                <include name="${module.name}/src/**/*"/>
                <include name="${module.name}/target/**/*"/>
                <include name="${module.name}/workflows/**/*"/>
                <exclude name="${module.name}/src/**/*.java"/>
            </fileset>
            </#list>
            <singlefile src="${basedir}/build-area/lib/ant.jar" target="$INSTALL_PATH/build-area/lib/ant.jar"/>
            <singlefile src="${basedir}/build-area/settings/build-properties.xml" target="$INSTALL_PATH/build-area/settings/build-properties.xml"/>
            <fileset dir="${basedir}/build-area/resources/installer/windows" targetdir="$INSTALL_PATH/">
				<include name="kepler.exe" os="windows"/>
				<include name="module-manager.exe" os="windows"/>
			</fileset>
			<fileset dir="${basedir}" targetdir="$INSTALL_PATH/">
                <include name="kepler.bat"/>
                <include name="module-manager.bat"/>
                <include name="kepler.jar"/>
                <include name="build-area/install-id.txt"/>
                <include name="build-area/modules.txt"/>
                <include name="build-area/current-suite.txt"/>
                <include name="build-area/module-manager-gui.txt"/>
                <include name="build-area/module-location-registry.txt"/>
                <include name="build-area/registry.txt"/>
                <include name="build-area/os-registry.txt"/>
                <include name="build-area/module-manager-gui.txt"/>
                <include name="build-area/use.keplerdata"/>
            </fileset>
        </pack>
        <#list moduleDependentsPacks as pack>
            <#include "${pack.installXml}"> 
        </#list>
    </packs>
</installation>
