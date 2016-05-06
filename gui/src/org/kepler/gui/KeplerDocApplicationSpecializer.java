/*
 * Copyright (c) 2006-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-06-05 10:08:31 -0700 (Wed, 05 Jun 2013) $' 
 * '$Revision: 32122 $'
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

package org.kepler.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import org.kepler.kar.SuperClassPathFinderDoclet;

import ptolemy.actor.IOPort;
import ptolemy.actor.gui.Effigy;
import ptolemy.gui.ComponentDialog;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.ExecuteCommands;
import ptolemy.util.StringUtilities;
import ptolemy.vergil.actor.DocApplicationSpecializer;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;

//////////////////////////////////////////////////////////////////////////
//// KeplerDocApplicationSpecializer

/**
 * Kepler specific specialization of the Ptolemy II Documentation system.
 * 
 * @author Christopher Brooks
 * @version $Id: KeplerDocApplicationSpecializer.java 14354 2008-03-04 21:07:33Z
 *          berkley $
 * @since Ptolemy II 6.0
 * @Pt.ProposedRating Red (cxh)
 * @Pt.AcceptedRating Red (cxh)
 */
public class KeplerDocApplicationSpecializer implements
		DocApplicationSpecializer {

	// the standard width of the components in the edit dialog
	private int standardX = 500;
	// a hash of the editable components in the edit dialog
	private Hashtable<String, JTextComponent> editableComponents;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Given a dot separated class name, return the URL of the PtDoc .xml file
	 * documentation or the Actor Index. All other document types (Javadoc,
	 * source documentation) are currently not not searched for. Thus the either
	 * the lookForPtDoc or lookForActorIndex parameter must be true, otherwise
	 * this method will return null.
	 * 
	 * <p>
	 * If the KEPLER_DOCS property is set, then its value is assumed to point to
	 * the Kepler documentation. If KEPLER_DOCS is not set, then the file
	 * ./doc/docsInfo.txt is read, and if that file contains the string of the
	 * format <code>KeplerDocs: <i>path to kepler-docs</i></code> then the value
	 * of <code><i>path to kepler-docs</i></code> is used. For example,
	 * docsInfo.txt might contain <code>KeplerDocs: c:\src\kepler-docs</code>,
	 * which means that the documentation is to be found in
	 * <code>c:\src\kepler-docs</code>. docsInfo.txt is created by running
	 * <code>cd $KEPLER; ant generateDoc</code>. The documentation for
	 * <code><i>className</i></code> is assumed to be in
	 * <code>$KEPLER_DOCS/dev/documentationFramework/generatedJavadocs/<i>className</i>.doc.xml</code>.
	 * 
	 * @param remoteDocumentationURLBase
	 *            If non-null, the URL of the documentation. Usually, this is
	 *            set by reading the _remoteDocumentationBase parameter from the
	 *            configuration in the caller.
	 * @param className
	 *            The dot separated class name.
	 * @param lookForPtDoc
	 *            True if we should look for ptdoc .xml files.
	 * @param lookForJavadoc
	 *            Ignored
	 * @param lookForSource
	 *            Ignored
	 * @param lookForActorIndex
	 *            True if we should search for the actor index *Idx.htm file.
	 * @return The URL of the documentation, if any. If no documentation was
	 *         found, return null.
	 */
	public URL docClassNameToURL(String remoteDocumentationURLBase,
			String className, boolean lookForPtDoc, boolean lookForJavadoc,
			boolean lookForSource, boolean lookForActorIndex) {
		if (!lookForPtDoc && !lookForActorIndex) {
			return null;
		}
		URL toRead = null;
		try {
			String actorName = className
					.substring(className.lastIndexOf('.') + 1);
			
			/** $KEPLER_DOCS no longer set in Kepler 2.0
			
			// Javadocs located under a elsewhere in Kepler

			// Use StringUtilities.getProperty() so that we properly
			// handle getting properties under applets or webstart.
			String keplerDocsHome = StringUtilities.getProperty("KEPLER_DOCS");

			if (keplerDocsHome.equals("")
					|| keplerDocsHome.equals("${env.KEPLER_DOCS}")) {
				// Read ./doc/docsInfo.txt
				BufferedReader inDocsInfo = new BufferedReader(new FileReader(
						"./doc/docsInfo.txt"));
				String info = inDocsInfo.readLine();

				if (info != null) {
					String docsInfo = info.substring(info.indexOf(":") + 1);
					docsInfo = docsInfo.replace('\\', '/');
					docsInfo = docsInfo.trim();
					keplerDocsHome = docsInfo;
				}
			}
			if (keplerDocsHome.equals("")
					|| keplerDocsHome.equals("${env.KEPLER_DOCS}")) {
				// If $KEPLER_DOCS is not set, look for
				// $KEPLER/dev/documentationFramework.

				// Use StringUtilities.getProperty() so that we properly
				// handle getting properties under applets or webstart.
				String keplerHome = StringUtilities.getProperty("KEPLER");
				File file = new File(keplerHome + "/dev/documentationFramework");
				if (file.isDirectory()) {
					keplerDocsHome = keplerHome;
				}
			}

			InputStream toReadStream = null;
			if (!keplerDocsHome.equals("")
					&& !keplerDocsHome.equals("${env.KEPLER_DOCS}")) {

				keplerDocsHome = keplerDocsHome.replace('\\', '/');

				File file = null;
				if (lookForActorIndex) {
					file = new File(keplerDocsHome
							+ "/dev/documentationFramework/generatedJavadocs/"
							+ className.replace('.', '/') + "Idx.htm");
				} else {
					file = new File(keplerDocsHome
							+ "/dev/documentationFramework/generatedJavadocs/"
							+ actorName + ".doc.xml");

				}
				toRead = file.toURL();
				// Verify that we can read the URL.
				try {
					toReadStream = toRead.openStream();
				} catch (IOException ex) {
					toRead = null;
				} finally {
					if (toReadStream != null) {
						try {
							toReadStream.close();
						} catch (IOException ex2) {
							// Ignore.
						}
					}
				}
			}
			if (toRead == null && remoteDocumentationURLBase != null) {
				try {
					if (lookForActorIndex) {
						toRead = new URL(remoteDocumentationURLBase
								+ className.replace('.', '/') + "Idx.htm");
					} else {
						toRead = new URL(remoteDocumentationURLBase + actorName
								+ ".doc.xml");
					}
					toReadStream = toRead.openStream();
				} catch (Exception ex) {
					toRead = null;
				} finally {
					if (toReadStream != null) {
						try {
							toReadStream.close();
						} catch (IOException ex2) {
							// Ignore.
						}
					}
				}
			}
        */
			
    		if(lookForSource) {
    		    String path = SuperClassPathFinderDoclet.getFileNameForClassName(className);
    		    File file = new File(path);
    		    if(file.isFile()) {
    		        toRead = file.toURI().toURL();
    		    }
    		}

		} catch (Throwable throwable) {
			// Ignore and return null.
			return null;
		}
		
			
		return toRead;
	}

	/**
	 * Set up the commands necessary to build the documentation. For Kepler, we
	 * run "ant generateDoc" in $KEPLER>
	 * 
	 * @param executeCommands
	 *            The command execution environment necessary to build the
	 *            documentation.
	 * @return A List of Strings, where each String represents the a command to
	 *         be executed.
	 */
	public List buildCommands(ExecuteCommands executeCommands) {
		List<String> commands = new LinkedList<String>();

		String osName = StringUtilities.getProperty("os.name");
		if (osName != null && osName.startsWith("Windows")) {
			commands.add("ant.bat generateDoc");
		} else {
			commands.add("ant generateDoc");
		}
		executeCommands.setCommands(commands);

		File kepler = new File(StringUtilities.getProperty("KEPLER"));
		executeCommands.setWorkingDirectory(kepler);
		return commands;
	}

	/**
	 * returns the name of the documentation attribute
	 */
	public String getDocumentationAttributeClassName() {
		return "ptolemy.vergil.basic.KeplerDocumentationAttribute";
	}

	/**
	 * create a gui to edit the documentation in the attribute
	 * 
	 * @param owner
	 *            the editors gui parent
	 * @param attribute
	 *            the documentation attribute to edit
	 * @param target
	 *            the parent component to the attribute
	 */
	public void editDocumentation(Frame owner, Attribute attribute,
			NamedObj target) {
		String actorName = target.getName();
		String title = "Editing Documentation for " + actorName;
		JPanel panel = createEditingPanel(attribute, target, owner
				.getBackground());
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
		ComponentDialog editor = new ComponentDialog(owner, title, scrollPane);		
		String buttonpressed = editor.buttonPressed();
		// System.out.println("the " + buttonpressed + " button was pressed.");
		if (buttonpressed.equals("OK")) { 
			// save the documentation. if cancel was clicked, do nothing
			saveDocumentation((KeplerDocumentationAttribute) attribute, panel);
		}
	}

	/**
	 * handle the case where the component does not have a KeplerDocumentation
	 * attribute
	 * 
	 * @param target
	 *            the component
	 */
	public void handleDocumentationAttributeDoesNotExist(Frame owner,
			NamedObj target) {
		try {
			KeplerDocumentationAttribute kda = new KeplerDocumentationAttribute(
					target, "KeplerDocumentation");
			kda.createEmptyFields(target);
			editDocumentation(owner, kda, target);
		} catch (Exception e) {
			System.out.println("Error adding documentation attribute");
		}
	}

	/**
	 * handle the case where the user tried to edit documentation and the
	 * attribute does not exist.
	 */
	public void handleDocumentationNotFound(String classname, Effigy effigy) {
		String msg = "Sorry, but there is no documentation associated with this "
				+ "component.\n"
                                + "If you built Kepler from sources, please run "
                                + "'ant javadoc' and restart Kepler.\n"
                                + "To add custom "
                                + "documentation, please right click on the actor and "
                                + "choose 'Documentation/Customize'.";

		try {
			JOptionPane.showMessageDialog(effigy.getTableauFactory()
					.createTableau(effigy).getFrame(), msg, "alert",
					JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
		}
	}

	/**
	 * save the user entered documentation in the panel to the attribute
	 */
	private void saveDocumentation(KeplerDocumentationAttribute att,
			JPanel panel) {
		Enumeration<String> keys = editableComponents.keys();
		while (keys.hasMoreElements()) {
			String name = (String) keys.nextElement();
			JTextComponent comp = (JTextComponent) editableComponents.get(name);
			String value = comp.getText();
			ConfigurableAttribute a = (ConfigurableAttribute) att
					.getAttribute(name);
			if (a == null) {
				try {
					a = new ConfigurableAttribute(att, name);
					a.setExpression(value);
				} catch (Exception e) {
					System.out.println("error saving documentation: "
							+ e.getMessage());
				}
			} else {
				String oldval = a.getExpression();
				if (!value.equals(oldval)) {
					try {
						a.setExpression(value);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null,
								"Error saving attribute " + name + " : "
										+ e.getMessage(), "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}

		// Make sure the revision gets rolled on the lsid by issuing a change
		// request on one of the fields, chose here to change "author", could be
		// any of them
		JTextComponent component = (JTextComponent) editableComponents
				.get("author");
		String value = component.getText();
		String caStr = "ptolemy.kernel.util.ConfigurableAttribute";

		String updateMoml = "<property name=\"author\" " + "class=\"" + caStr
				+ "\" value=\"" + value + "\"/>";
		MoMLChangeRequest updateRequest = new MoMLChangeRequest(this, att
				.getContainer(), updateMoml);
		att.requestChange(updateRequest);
	}

	/**
	 * create the panel that does the editing
	 */
	private JPanel createEditingPanel(Attribute attribute, NamedObj target,
			Color background) {
		editableComponents = new Hashtable<String, JTextComponent>();
		// System.out.println("attribute: " + attribute.exportMoML());
		KeplerDocumentationAttribute att = (KeplerDocumentationAttribute) attribute;
		att.createInstanceFromExisting(att);
		JPanel outerPanel = new JPanel();
		// outerPanel.setBackground(background);
		BoxLayout outerBox = new BoxLayout(outerPanel, BoxLayout.Y_AXIS);
		outerPanel.setLayout(outerBox);

		// header - name of component
		JLabel componentNameLabel = new JLabel(target.getName());
		componentNameLabel.setFont(new Font("Times", Font.BOLD, 20));
		componentNameLabel.setOpaque(false);
		JPanel componentNamePanel = new JPanel();
		componentNamePanel.add(componentNameLabel);
		componentNamePanel.setMaximumSize(new Dimension(standardX, 50));
		componentNamePanel.setOpaque(false);
		outerPanel.add(componentNamePanel);
		outerPanel.add(Box.createVerticalStrut(10));

		// author and version
		JTextField authorTextField = new JTextField(att.getAuthor());
		editableComponents.put("author", authorTextField);
		authorTextField.setColumns(50);
		JLabel authorLabel = new JLabel("Author(s):");
		JPanel authorPanel = createEditBox(authorLabel, authorTextField);
		JTextField versionTextField = new JTextField(att.getVersion());
		editableComponents.put("version", versionTextField);
		versionTextField.setColumns(10);
		JLabel versionLabel = new JLabel("Version:");
		JPanel versionPanel = createEditBox(versionLabel, versionTextField);

		JPanel authorVersionPanel = new JPanel();
		BoxLayout authorVersionBox = new BoxLayout(authorVersionPanel,
				BoxLayout.X_AXIS);
		authorVersionPanel.setLayout(authorVersionBox);
		authorVersionPanel.add(authorPanel);
		authorVersionPanel.add(Box.createHorizontalStrut(10));
		authorVersionPanel.add(versionPanel);
		authorVersionPanel.setOpaque(false);
		authorVersionPanel.setMaximumSize(new Dimension(standardX, 40));

		outerPanel.add(authorVersionPanel);
		outerPanel.add(Box.createVerticalStrut(10));

		// description
		JTextArea descriptionTextArea = new JTextArea(att.getDescription());
		editableComponents.put("description", descriptionTextArea);
		descriptionTextArea.setLineWrap(true);
		JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
		descriptionTextArea.setColumns(50);
		descriptionTextArea.setRows(5);
		JLabel descriptionLabel = new JLabel("Description:");
		JPanel descriptionPanel = createEditBox(descriptionLabel,
				descriptionScrollPane);
		descriptionPanel.setMaximumSize(new Dimension(standardX, 100));
		outerPanel.add(descriptionPanel);
		outerPanel.add(Box.createVerticalStrut(10));

		// userLevelDescription
		JTextArea uldTextArea = new JTextArea(att.getUserLevelDocumentation());
		editableComponents.put("userLevelDocumentation", uldTextArea);
		uldTextArea.setLineWrap(true);
		JScrollPane uldScrollPane = new JScrollPane(uldTextArea);
		uldTextArea.setColumns(50);
		uldTextArea.setRows(8);
		JLabel uldLabel = new JLabel("Documentation:");
		JPanel uldPanel = createEditBox(uldLabel, uldScrollPane);
		uldPanel.setMaximumSize(new Dimension(standardX, 100));
		outerPanel.add(uldPanel);
		outerPanel.add(Box.createVerticalStrut(10));

		// TODO: compare the kepler documentation att props and ports with the
		// actual props and ports in the component. if there are any missing,
		// add them or remove ones from the docs that no longer exist

		// check for properties that do not have a documentation entry
		Iterator atts = target.attributeList().iterator();
		while (atts.hasNext()) {
			Attribute a = (Attribute) atts.next();
			String aName = a.getName();
			Hashtable props = att.getPropertyHash();
			Enumeration keys = props.keys();
			boolean add = true;
			while (keys.hasMoreElements()) {
				String name = (String) keys.nextElement();
				if (name.equals(aName)) {
					add = false;
				}
			}

			if (!aName.trim().equals("") && add) {
				if (!aName.substring(0, 1).equals("_")
						&& !aName.equals("KeplerDocumentation")
						&& !aName.equals("entityId") && !aName.equals("class")
						&& aName.indexOf("semanticType") == -1) {
					try {
						att.addProperty(aName, "");
						// ConfigurableAttribute ca = new
						// ConfigurableAttribute(att, "prop:" + aName);
						// ca.setExpression("");
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null,
								"Error adding attribute " + aName + " : "
										+ e.getMessage(), "Error",
								JOptionPane.ERROR_MESSAGE);
					}

				}
			}
		}

		// check for ports that have been added that don't have a doc property
		if (target instanceof Entity) {
			Enumeration portEnum = ((Entity) target).getPorts();
			while (portEnum.hasMoreElements()) {
				IOPort iop = (IOPort) portEnum.nextElement();
				String iopName = iop.getName();
				Hashtable ports = att.getPortHash();
				Enumeration keys = ports.keys();
				boolean add = true;
				while (keys.hasMoreElements()) {
					String name = (String) keys.nextElement();
					if (name.equals(iopName)) {
						add = false;
					}
				}

				if (add) {
					try {
						att.addPort(iopName, "");
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null,
								"Error adding port " + iopName + " : "
										+ e.getMessage(), "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}

			// ports
			JPanel portHeaderLabelPanel = new JPanel();
			JLabel portHeaderLabel = new JLabel("Ports");
			portHeaderLabel.setFont(new Font("Times", Font.BOLD, 20));
			portHeaderLabelPanel.add(portHeaderLabel);
			portHeaderLabelPanel.setMaximumSize(new Dimension(standardX, 50));
			portHeaderLabelPanel.setOpaque(false);
			outerPanel.add(portHeaderLabelPanel);

			Hashtable portHash = att.getPortHash();
			JPanel portPanel = createHashPanel(portHash, "port");
			portPanel.setMaximumSize(new Dimension(standardX, 1000));
			outerPanel.add(portPanel);
			outerPanel.add(Box.createVerticalStrut(20));
		}

		// params
		JPanel propHeaderLabelPanel = new JPanel();
		JLabel propHeaderLabel = new JLabel("Properties");
		propHeaderLabel.setFont(new Font("Times", Font.BOLD, 20));

		propHeaderLabelPanel.add(propHeaderLabel);
		propHeaderLabelPanel.setMaximumSize(new Dimension(standardX, 50));
		propHeaderLabelPanel.setOpaque(false);
		outerPanel.add(propHeaderLabelPanel);

		Hashtable propHash = att.getPropertyHash();
		JPanel propPanel = createHashPanel(propHash, "prop");
		propPanel.setMaximumSize(new Dimension(standardX, 1000));
		outerPanel.add(propPanel);
		outerPanel.add(Box.createVerticalStrut(20));

		Component[] comps = outerPanel.getComponents();
		int y = 0;
		for (int i = 0; i < comps.length; i++) {
			// set the size of the dialog based on the size of the components in
			// it
			y += comps[i].getMinimumSize().getHeight();
		}

		outerPanel.setPreferredSize(new Dimension(standardX + 50, y + 100));
		return outerPanel;
	}

	/**
	 * create a panel of inputs based on a hashtable
	 */
	private JPanel createHashPanel(Hashtable hash, String type) {
		JPanel propLabelPanel = new JPanel();
		BoxLayout propLabelPanelBox = new BoxLayout(propLabelPanel,
				BoxLayout.Y_AXIS);
		propLabelPanel.setLayout(propLabelPanelBox);
		propLabelPanel.setMaximumSize(new Dimension(100, 1000));

		JPanel propTextBoxPanel = new JPanel();
		BoxLayout propTextBoxPanelBox = new BoxLayout(propTextBoxPanel,
				BoxLayout.Y_AXIS);
		propTextBoxPanel.setLayout(propTextBoxPanelBox);
		propTextBoxPanel.setMaximumSize(new Dimension(400, 1000));

		JPanel propPanel = new JPanel();
		BoxLayout propPanelBox = new BoxLayout(propPanel, BoxLayout.Y_AXIS);
		propPanel.setLayout(propPanelBox);

		Enumeration propEnum = hash.keys();
		while (propEnum.hasMoreElements()) {
			String name = (String) propEnum.nextElement();
			String value = (String) hash.get(name);
			JLabel propLabel = new JLabel(name.trim() + ":");
			propLabel.setMaximumSize(new Dimension(150, 1000));
			JTextField propTextField = new JTextField(value.trim());
			propTextField.setMaximumSize(new Dimension(350, 1000));
			propTextField.setMinimumSize(new Dimension(350, 20));
			propTextField.setPreferredSize(new Dimension(350, 20));
			Caret caret = propTextField.getCaret();
			// move the carot in the text to the beginning instead of the end
			caret.setDot(0);
			propTextField.setCaret(caret);
			editableComponents.put(type + ":" + name, propTextField);
			JPanel editBoxPanel = createEditBox(propLabel, propTextField);

			propPanel.add(editBoxPanel);
		}

		propPanel.setMaximumSize(new Dimension(standardX, 1000));

		return propPanel;
	}

	/**
	 * create an edit box with a label
	 */
	private JPanel createEditBox(JLabel label, JComponent textbox) {
		JPanel panel = new JPanel();
		BoxLayout box = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(box);
		double labelWidth = label.getMinimumSize().getWidth();
		double textboxWidth = textbox.getMaximumSize().getWidth();
		int offset = (int) (150 - labelWidth);

		panel.add(label);
		panel.add(Box.createHorizontalStrut(offset));
		panel.add(textbox);
		panel.setOpaque(false);

		return panel;
	}

}