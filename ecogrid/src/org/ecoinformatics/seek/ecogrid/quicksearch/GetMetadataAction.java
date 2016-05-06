/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:04 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31121 $'
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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.awt.event.ActionEvent;
import java.io.Reader;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.toolbox.FigureAction;
import util.StaticUtil;

//import ptolemy.kernel.util.Configurable;
/**
 * This is an action to get metadata for search result item in data search
 * result panel
 * 
 * @author Jing Tao
 */
public class GetMetadataAction extends FigureAction {
  private static final String XMLFILEEXTENSION = ".xml";
  private static final String HTMLFILEEXTENSION = ".html";
	private Reader metadataSource = null;
	private String nameSpace = null;
	private String htmlFileName = null;
	private URL metadata = null;
	private ResultRecord item = null;
	private Configuration configuration = null;
	private static final String LABEL = "Get Metadata";

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.GetMetadataAction");
	}

	/**
	 * Constructor base a given ResultRecord object
	 * 
	 * @param item
	 *            ResultRecord
	 */
	public GetMetadataAction(ResultRecord item) {
		super(LABEL);
		this.item = item;

	}
	
	public GetMetadataAction(TableauFrame parent) 
	{
    super("Get Metadata");
	}

	/**
	 * Method to get the metadata(the generated html file after transfering
	 * original metadata)
	 * 
	 * @return URL
	 */
	public URL getMetadata() {
		return metadata;
	}

	/**
	 * Invoked when an action occurs. It will transfer the original metadata
	 * into a html file. The namespace will be the key to find stylesheet. If no
	 * sytlesheet found, metadata will be null.
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {

		super.actionPerformed(e);
		NamedObj object = getTarget();
    if(object instanceof ResultRecord)
    {
      this.item = (ResultRecord)object;
    }
    
		if(item == null)
		{
		  JOptionPane.showMessageDialog(null, "There is no metadata associated with this component.");
		  return;
		}
		
		ProgressMonitor progressMonitor = new ProgressMonitor(null, 
        "Acquiring Metadata ", "", 0, 5);
    progressMonitor.setMillisToDecideToPopup(100);
    progressMonitor.setMillisToPopup(10);
    progressMonitor.setProgress(0);
    this.metadataSource = item.getFullRecord();
    progressMonitor.setProgress(1);
    this.nameSpace = item.getNamespace();
    progressMonitor.setProgress(2);
    this.htmlFileName = item.getRecordId();
    //System.out.println("the html file name is ====== "+htmlFileName);
    progressMonitor.setProgress(3);
    
		if (configuration == null) {
			configuration = getConfiguration();
		}
		if (metadataSource != null && nameSpace != null && htmlFileName != null
				&& configuration != null) {
      if(htmlFileName.endsWith(XMLFILEEXTENSION)) {
        htmlFileName = htmlFileName+HTMLFILEEXTENSION;
      }
			try {
			  progressMonitor.setProgress(4);
				metadata = StaticUtil.getMetadataHTMLurl(metadataSource, nameSpace,
						htmlFileName);
				//System.out.println("before open html page");
				configuration.openModel(null, metadata, metadata
						.toExternalForm());
				progressMonitor.setProgress(5);
				//System.out.println("after open html page");
				progressMonitor.close(); 
			} catch (Exception ee) {
				log.debug("The error to get metadata html ", ee);
			}

		}
	}

	/*
	 * Method to get Configuration object in the top level container
	 */
	private Configuration getConfiguration() {
		Configuration config = (Configuration)Configuration.configurations().get(0);
		return config;
	}
}