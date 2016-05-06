/**
 *
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.kepler.gui.kar;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.jdesktop.swingworker.SwingWorker;
import org.kepler.build.Run;
import org.kepler.build.modules.CurrentSuiteTxt;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.Version;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationManagerException;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.kar.KARFile;
import org.kepler.kar.ModuleDependencyUtil;
import org.kepler.kar.karxml.KarXml;
import org.kepler.modulemanager.ModuleDownloader;
import org.kepler.modulemanager.gui.ModuleDownloadProgressMonitor;
import org.kepler.util.ShutdownNotifier;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action opens the ModuleManager for downloading new modules that are
 * specified in the KAR dependencies.
 */
public class ImportModuleDependenciesAction extends FigureAction {

	private static String DISPLAY_NAME = "Import Dependent Modules";
	private static String TOOLTIP = "Import Kepler Modules that are needed for this KAR file.";
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	// //////////////////////////////////////////////////////////////////////////////

	private TableauFrame parent;

	private final static Log log = LogFactory.getLog(ImportModuleDependenciesAction.class);

	private File _archiveFile = null;
	private boolean _updateHistoryAndLastDirectory = true;
	private KarXml _karXml = null;
	private List<String> _dependencies = null;
	private boolean _exportMode = false;
	private boolean _needToDoAForceExport = false;
	private Object _downloadLock = new Object();
	
	/** The user choice when dealing with a KAR with missing dependencies. */
	public enum ImportChoice { FORCE_OPEN, FORCE_EXPORT, DO_NOTHING, DOWNLOADING_AND_RESTARTING };

	/**
	 * Constructor
	 * 
	 *@param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public ImportModuleDependenciesAction(TableauFrame parent) {
		super(DISPLAY_NAME);
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"ViewManifestAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.parent = parent;

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);
	}

	/**
	 * Explicitly set the dependencies for this dependeny broken KAR
	 * @param deps
	 */
	public void setDependencies(List<String> deps) {
		_dependencies = deps;
	}
	
	/**
	 * Explicitly set the Archive file for this dependency broken KAR file.
	 * 
	 * @param archiveFile
	 */
	public void setArchiveFile(File archiveFile) {
		_archiveFile = archiveFile;
	}
	
	/**
	 * By default, dialogs describe and attempt to Open KAR. 
	 * Change from Open to Export using this variable.
	 * @param export
	 */
	public void setExportMode(boolean export){
		_exportMode = export;
	}
	
    /**
     * Change whether or not to update Kepler's Recent Files menu, and 
     * last directory setting.
     * Default is true.
     *
     * @param updateHistoryAndLastDirectory
     */
	public void updateHistoryAndLastDirectory(boolean updateHistoryAndLastDirectory) {
		_updateHistoryAndLastDirectory = updateHistoryAndLastDirectory;
	}
	
	public void setKarXml(KarXml karXml) {
		_karXml = karXml;
	}
	
	public boolean getNeedToDoAForceExport(){
		return _needToDoAForceExport;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
				
		ImportChoice choice = checkDependencies();
		
		if (_archiveFile != null) {
			if(choice == ImportChoice.FORCE_OPEN) {
				OpenArchiveAction oaa = new OpenArchiveAction(parent);
				try {
					log.debug("FORCE OPEN, call OpenArchiveAction.openKAR");
					oaa.openKAR(_archiveFile, true, _updateHistoryAndLastDirectory);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}else if (choice == ImportChoice.FORCE_EXPORT){
				_needToDoAForceExport = true;
			}
		}
		
	}
	
	/** Check the dependencies and ask the user how to proceed. */ 
	public ImportChoice checkDependencies() {
		
		ConfigurationManager cman = ConfigurationManager.getInstance();
		ConfigurationProperty cprop = cman.getProperty(KARFile.KARFILE_CONFIG_PROP_MODULE);
		ConfigurationProperty KARComplianceProp = 
			cprop.getProperty(KARFile.KAR_COMPLIANCE_PROPERTY_NAME);
		String KARCompliance = KARComplianceProp.getValue();

		final ArrayList<String> dependencies = new ArrayList<String>();
		try {
			if (_dependencies != null){
				// dependencies were given
				dependencies.addAll(_dependencies);
			}
			else if (_archiveFile != null) {
				// kar file was given
				KARFile karFile = null;
				try {
				    karFile = new KARFile(_archiveFile);
				    dependencies.addAll(karFile.getModuleDependencies());
				} finally {
				    if(karFile != null) {
				        karFile.close();
				    }
				}
			}
			else {
				// karxml was given
				dependencies.addAll(_karXml.getModuleDependencies());
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		//ModuleTree moduleTree = ModuleTree.instance();
		//String currentModList = formattedCurrentModuleList(moduleTree);
		
		boolean dependencyMissingFullVersion = !(ModuleDependencyUtil.
				isDependencyVersioningInfoComplete(dependencies));
		LinkedHashMap<String, Version> unsatisfiedDependencies = 
			ModuleDependencyUtil.getUnsatisfiedDependencies(dependencies);

		String keplerRestartMessage = null;
		String unableToOpenOrExportInStrictKARComplianceMessage = null;
		String manualActionRequired = null;
		String unSats = formattedUnsatisfiedDependencies(unsatisfiedDependencies);
		final List<String> unSatsAsList = new ArrayList<String>(unsatisfiedDependencies.keySet()); 

		String formattedDependencies = formatDependencies(dependencies);
		String htmlBGColor = "#" + Integer.toHexString( 
				UIManager.getColor("OptionPane.background").getRGB() & 0x00ffffff );
		//XXX augment if additional strictness levels added
		if (KARCompliance.equals(KARFile.KAR_COMPLIANCE_STRICT)){
			if (dependencyMissingFullVersion){
				if (_exportMode){
					unableToOpenOrExportInStrictKARComplianceMessage = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR " +
						"lacks complete versioning information in its module-dependency list:<strong>"+
						formattedDependencies +"</strong><br><br>You must change your KAR opening compliance " +
						"preference to Relaxed before trying to export this KAR.<br><br>" +
						"You can Force Export, but some artifacts may not be included in the KAR.</body></html>";
				}else{
					unableToOpenOrExportInStrictKARComplianceMessage = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR " +
						"lacks complete versioning information in its module-dependency list:<strong>"+
						formattedDependencies +"</strong><br><br>You must change your KAR opening compliance " +
						"preference to Relaxed before trying to open this KAR.<br><br>" +
						"You can attempt a Force Open, but this may cause unexpected errors.</body></html>";
				}
			}
			else{
				if (!unsatisfiedDependencies.isEmpty()){
					if (_exportMode){
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">Your KAR opening compliance preference is set to Strict. To export this KAR in<br>" +
							"Strict mode you must restart Kepler with these additional module(s):<strong>" + 
							unSats +"</strong><br><br>Would you like to download (if necessary) and restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost, and auto-updating turned off if it's on<br>"+
							"(re-enable using the Tools=>Module Manager...)</strong><br><br>" +
							"You can Force Export, but some artifacts may not be included in the KAR.</body></html>";
					}else{
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">Your KAR opening compliance preference is set to Strict. To open this KAR in<br>" +
							"Strict mode you must restart Kepler with these additional module(s):<strong>" + 
							unSats +"</strong><br><br>Would you like to download (if necessary) and restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost, and auto-updating turned off if it's on<br>"+
							"(re-enable using the Tools=>Module Manager...)</strong><br><br>" +
							"You can attempt a Force Open, but this may cause unexpected errors.</body></html>";
					}
				}
				else{
					if (_exportMode){
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">Your KAR opening compliance preference is set to Strict. To export this KAR in<br>" +
							"Strict mode you must restart Kepler using this module set in this order:<strong>" + 
							formattedDependencies +"</strong><br><br>Would you like to restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost, and auto-updating turned off if it's on<br>"+
							"(re-enable using the Tools=>Module Manager...)</strong><br><br>" +
							"You can Force Export, but some artifacts may not be included in the KAR.</body></html>";
					}else{
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">Your KAR opening compliance preference is set to Strict. To open this KAR in<br>" +
							"Strict mode you must restart Kepler using this module set in this order:<strong>" + 
							formattedDependencies +"</strong><br><br>Would you like to restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost, and auto-updating turned off if it's on<br>"+
							"(re-enable using the Tools=>Module Manager...)</strong><br><br>" +
							"You can attempt a Force Open, but this may cause unexpected errors.</body></html>";
					}
				}
			}
		}
		else if (KARCompliance.equals(KARFile.KAR_COMPLIANCE_RELAXED)) {
			if (dependencyMissingFullVersion){
				// if there's a dependency missing full version info, situation should be either 1) a 2.0 kar, in which case
				// it lacks the full mod dep list and so user must use MM, or 2) it's a 2.1 kar created from an svn 
				// checkout of kepler, in which case, the power user should use the build system to 
				// change to unreleased versions of a suite containing the required modules as necessary
				if (_exportMode){
					manualActionRequired = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR " +
						"requires the following unsatisfied module dependencies that lack complete versioning information:<strong>"+
						unSats +"</strong><br><br>Please use the Module Manager or build system to change to a suite that uses these modules.</strong>" +
						"<br><br>You can Force Export, but some artifacts may not be included in the KAR.</body></html>";
				}else{
					manualActionRequired = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR " +
						"requires the following unsatisfied module dependencies that lack complete versioning information:<strong>"+
						unSats +"</strong><br><br>Please use the Module Manager or build system to change to a suite that uses these modules.</strong>" +
						"<br><br>You can attempt a Force Open, but this may cause unexpected errors.</body></html>";
				}
			}
			else{
				if (!unsatisfiedDependencies.isEmpty()){
					if (_exportMode){
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR requires you restart Kepler with these " +
							"additional module(s):<strong>" + 
							unSats + "</strong><br><br>Would you like to " +
							"download (if necessary) and restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost</strong><br><br>" +
							"You can Force Export, but some artifacts may not be included in the KAR.</body></html>";
					}else{
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR requires you restart Kepler with these " +
							"additional module(s):<strong>" + 
							unSats + "</strong><br><br>Would you like to " +
							"download (if necessary) and restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost</strong><br><br>" +
							"You can attempt a Force Open, but this may cause unexpected errors.</body></html>";
					}
				}
				else{
					//THIS SHOULDN'T HAPPEN
					log.error("ImportModuleDependenciesAction WARNING unsatisfiedDependencies is empty, this shouldn't happen, but is non fatal");
					if (_exportMode){
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR requires you restart Kepler with these " +
							"module(s):<strong>" + 
							formattedDependencies + "</strong><br><br>Would you like to " +
							"restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost</strong><br><br>" +
							"You can Force Export, but some artifacts may not be included in the KAR.</body></html>";
					}else{
						keplerRestartMessage = "<html><body bgcolor=\""+htmlBGColor+"\">This KAR requires you restart Kepler with these " +
							"module(s):<strong>" + 
							formattedDependencies + "</strong><br><br>Would you like to " +
							"restart Kepler using these modules now?" +
							"<br><br><strong>WARNING: All unsaved work will be lost</strong><br><br>" +
							"You can attempt a Force Open, but this may cause unexpected errors.</body></html>";
					}
				}
			}
		}
		
		String[] optionsOkForceopen = { "OK", "Force Open" };
		String[] optionsOkForceexport = { "OK", "Force Export" };
		String[] optionsYesNoForceopen = { "Yes", "No", "Force Open" };
		String[] optionsYesNoForceexport = { "Yes", "No", "Force Export" };

		
		if (unableToOpenOrExportInStrictKARComplianceMessage != null){
			JLabel label = new JLabel(unableToOpenOrExportInStrictKARComplianceMessage);
			if (_exportMode){
				int choice = JOptionPane.showOptionDialog(parent, label, "Unable to export in Strict mode", 
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, optionsOkForceexport, optionsOkForceexport[0]);
				if (optionsOkForceexport[choice].equals("Force Export")){
					return ImportChoice.FORCE_EXPORT;
				}
			}else{
				int choice = JOptionPane.showOptionDialog(parent, label, "Unable to open in Strict mode", 
						JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, optionsOkForceopen, optionsOkForceopen[0]);
				if (optionsOkForceopen[choice].equals("Force Open")){
					return ImportChoice.FORCE_OPEN;
				}
			}
			return ImportChoice.DO_NOTHING;
		}
		
		if (manualActionRequired != null){
			JLabel label = new JLabel(manualActionRequired);
			if (_exportMode){
				int choice = JOptionPane.showOptionDialog(parent, label, "Use Module Manager", JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE, null, optionsOkForceexport, optionsOkForceexport[0]);
				if (optionsOkForceexport[choice].equals("Force Export")){
					return ImportChoice.FORCE_EXPORT;
				}
			}else{
				int choice = JOptionPane.showOptionDialog(parent, label, "Use Module Manager", JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE, null, optionsOkForceopen, optionsOkForceopen[0]);
				if (optionsOkForceopen[choice].equals("Force Open")){
					return ImportChoice.FORCE_OPEN;
				}
			}
			return ImportChoice.DO_NOTHING;
		}
		
		JLabel label = new JLabel(keplerRestartMessage);
		if (_exportMode){
			int choice = JOptionPane.showOptionDialog(parent, label, "Confirm Kepler Restart", 
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, 
					optionsYesNoForceexport, optionsYesNoForceexport[1]);
	
			if (optionsYesNoForceexport[choice] == "No") {
				// user doesn't want to download.
				return ImportChoice.DO_NOTHING;
			} else if (optionsYesNoForceexport[choice].equals("Force Export")){
				return ImportChoice.FORCE_EXPORT;
			}
		}else{
			int choice = JOptionPane.showOptionDialog(parent, label, "Confirm Kepler Restart", 
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, 
					optionsYesNoForceopen, optionsYesNoForceopen[1]);
	
			if (optionsYesNoForceopen[choice] == "No") {
				// user doesn't want to download.
				return ImportChoice.DO_NOTHING;
			} else if (optionsYesNoForceopen[choice].equals("Force Open")){
				return ImportChoice.FORCE_OPEN;
			}	
		}
		
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
		    public Void doInBackground() throws Exception {
				try {
					
					//download needed modules
					ModuleDownloader downloader = new ModuleDownloader();
					ModuleDownloadProgressMonitor mdpm = new ModuleDownloadProgressMonitor(
							parent);
					downloader.addListener(mdpm);
					if (!unSatsAsList.isEmpty()){
						downloader.downloadModules(unSatsAsList);
					}
					else{
						// this shouldn't happen, but if it does, resorting
						// to downloading all dependencies should be a safe bet
						log.error("ImportModuleDependenciesAction WARNING unSatsAsList is empty, " +
								"this shouldn't happen, but is non fatal");
						downloader.downloadModules(dependencies);
					}
					
					//rewrite modules.txt
				    ModulesTxt modulesTxt = ModulesTxt.instance();
				    modulesTxt.clear();
					for (String dependency : dependencies) {
						//System.out.println("ImportModuleDependency doInBackground modulesTxt.add("+dependency+")");
						modulesTxt.add(dependency);
					}
				    modulesTxt.write();
				    
				    //delete and write "unknown" to current-suite.txt
				    CurrentSuiteTxt.delete();
				    CurrentSuiteTxt.setName("unknown");
					
				    // if KARCompliance is Strict, user is restarting w/ specific versions of modules
				    // and we don't want them to potentially auto update on restart to available patches
				    turnOffAutoUpdatesIfStrictMode();
				    
				    //restart Kepler using new modules
				    spawnNewKeplerAndQuitCurrent();
					
					return null;
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(parent,
							"Error downloading module: " + ex.getMessage());
					return null;
				}
			}

	         @Override
	         protected void done() {
				//never reached.
			}
			
		};

		worker.execute();
				
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
		return ImportChoice.DOWNLOADING_AND_RESTARTING;
	}
	
	/** Wait for the download to complete. This method does not return
	 *  since Kepler restarts once the download finishes.
	 */
	public void waitForDownloadAndRestart() {
	    synchronized(_downloadLock) {
	        try {
                _downloadLock.wait();
            } catch (InterruptedException e) {
                MessageHandler.error("Error waiting for download to complete.", e);
            }
	    }
	}
	
	/**
	 * If KARCompliance is STRICT, turn off auto-download of new patches (
	 * "check-for-patches" config param)
	 */
	private void turnOffAutoUpdatesIfStrictMode() {
		ConfigurationManager cman = ConfigurationManager.getInstance();
		ConfigurationProperty cprop = cman
				.getProperty(KARFile.KARFILE_CONFIG_PROP_MODULE);
		ConfigurationProperty KARComplianceProp = cprop
				.getProperty(KARFile.KAR_COMPLIANCE_PROPERTY_NAME);
		String KARCompliance = KARComplianceProp.getValue();
		if (KARCompliance.equals(KARFile.KAR_COMPLIANCE_STRICT)) {
			ConfigurationProperty mmConfigProp = ConfigurationManager.getInstance()
					.getProperty(ConfigurationManager.getModule("module-manager"));
			ConfigurationProperty checkForPatches = mmConfigProp
					.getProperty("check-for-patches");
			try {
				if (checkForPatches.getValue() != null && 
						checkForPatches.getValue().equals("true")){
					checkForPatches.setValue("false");
					ConfigurationManager.getInstance().saveConfiguration();
				}
			} catch (ConfigurationManagerException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void spawnNewKeplerAndQuitCurrent(){
		
        Project project = new Project();
        project.setBaseDir(ProjectLocator.getProjectDir());
        DefaultLogger logger = new DefaultLogger();
        logger.setMessageOutputLevel(Project.MSG_INFO);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.out);
        project.addBuildListener(logger);

        //XXX call ShutdownNotifer.shutdown() before spawning new process,
        // to avoid 2nd instance potentially interfering with quitting first.
        // see: http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5484
        System.out.println("ImportModuleDependency notifiying shutdown listeners " +
        		"of impending shutdown. Closing any open databases may take awhile...");
        ShutdownNotifier.shutdown();
        
        System.out.println("ImportModuleDependency Spawning new Kepler process");
        Run run = new Run();
        run.setTaskName("run");
        run.setProject(project);
        run.init();
        run.setSpawn(true);
        run.execute();
        
        System.out.println("ImportModuleDependency Ending current Kepler process");
        System.out.println("NOTE: This will probably throw an exception as the current" +
        		" process is terminated while a new Kepler process starts. This is normal and expected.");
        
        //XXX why exit with error arg here?
        System.exit(1);
	}
	
	/**
	 * simple helper method
	 * @param unsatisfiedDependencies
	 * @return formatted html unsatisfied dependency list
	 */
	private static String formattedUnsatisfiedDependencies(HashMap<String, Version> unsatisfiedDependencies){
		StringBuilder strBuilder = new StringBuilder("");
		
		Set<String> unsatisfieds = unsatisfiedDependencies.keySet();
		
		if (!unsatisfieds.isEmpty()){
			strBuilder.append("<br>");
		}		
		Iterator<String> itr = unsatisfieds.iterator();
		int cnt =0;
		while (itr.hasNext()){
			if (cnt%5 == 0){
				strBuilder.append("<br>");
			}
			strBuilder.append(itr.next());
			if (itr.hasNext()){
				strBuilder.append(", ");
			}

			cnt++;
		}
		return strBuilder.toString();
	}
	
	/**
	 * simple helper method
	 * @param dependencies
	 * @return formatted html dependency list
	 */
	private static String formatDependencies(List<String> dependencies){
		StringBuilder strBuilder = new StringBuilder("");
		
		if (!dependencies.isEmpty()){
			strBuilder.append("<br>");
		}
		
		Iterator<String> itr = dependencies.iterator();
		int cnt =0;
		while (itr.hasNext()){
			if (cnt%5 == 0){
				strBuilder.append("<br>");
			}
			strBuilder.append(itr.next());
			if (itr.hasNext()){
				strBuilder.append(", ");
			}

			cnt++;
		}
		return strBuilder.toString();
	}

	/**
	 * simple helper method
	 * @param moduleTree
	 * @return return formatted html module list
	 */
	private static String formattedCurrentModuleList(ModuleTree moduleTree){
		StringBuilder strBuilder = new StringBuilder("");

		List<Module> modList = moduleTree.getModuleList();
		
		if (!modList.isEmpty()){
			strBuilder.append("<br>");
		}
		
		for (int i=0; i<modList.size(); i++){
			if (i%5 == 0){
				strBuilder.append("<br>");
			}
			strBuilder.append(modList.get(i));
			if (i != modList.size() -1){
				strBuilder.append(", ");
			}
		}
		return strBuilder.toString();
	}
	
}
