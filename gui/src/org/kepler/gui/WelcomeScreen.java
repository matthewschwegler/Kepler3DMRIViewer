/*
 * Copyright (c) 1997-2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

//////////////////////////////////////////////////////////////////////////
//// WelcomeScreen
/**
 * A welcome screen which is displayed on Kepler Startup. The user can further
 * select links (Scientists/Programmers) from the above screen, leading to
 * tutorial getting started pages. The proposed design for the above screen can
 * be found at: http://kepler-project.org/Wiki.jsp?page=SplashAndWelcomeScreens
 * 
 *@author nmangal
 *@since November 9, 2006
 *@version $Id: WelcomeScreen.java,v 1.0 2006/13/02 20:39:04
 */

public class WelcomeScreen extends JDialog {

	private String keplerLogoURL;
	private JCheckBox startupCheck;
	private JButton close;
	private JLabel scientistLink;
	private JLabel programmerLink;

	/**
	 * Construct the main panel.Further add subPanels to the main panel, namely
	 * infoPanel and buttonPanel. infoPanel is the bordered panel, consisting of
	 * Welcome message and "Getting Started" links for scientist and
	 * programmers. The button Panel simply consists of a Close button and a
	 * CheckBox to indicate the setting to show the welcome screen on future
	 * startups.
	 * 
	 *@param keplerLogoURL
	 *            Description of the Parameter
	 */
	public WelcomeScreen(String keplerLogoURL) {

		// super(new BorderLayout());
		this.keplerLogoURL = keplerLogoURL;

		// Create and set up the window.
		int frameWidth = 508;
		int frameHeight = 304;
		setSize(frameWidth, frameHeight);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width / 2 - frameWidth / 2, screenSize.height
				/ 2 - frameHeight / 2);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setResizable(false);

		// Add Icon Image to the welcome screen window
		// Image img = Toolkit.getDefaultToolkit().getImage(keplerLogoURL);
		// setIconImage( img );

		// Set up the content pane.
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new GridLayout(1, 1));

		// mainPanel
		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(new Color(190, 200, 211));
		mainPanel.setLayout(new FlowLayout());
		// A border that puts extra pixels at the sides & bottom
		Border emptyBorder = BorderFactory.createEmptyBorder(9, 18, 52, 18);
		mainPanel.setBorder(emptyBorder);

		// Add the sub panels
		addWelcomePanel(mainPanel);
		addButtonPanel(mainPanel);

		// Add the mainPanel to WelcomeScreen
		contentPane.add(mainPanel);

		// this is kind of a hack so as to make
		// WelomeWindow on top of Kepler Main Page
		// due to fact taht Kepler Main Page takes about
		// 6 seconds to startup.
		try {
			Thread.sleep(6500);
		} catch (InterruptedException ex) {
			setVisible(false);
			dispose();
		}

		toFront();
		requestFocus();

		// Display the window.
		setVisible(true);

	}

	/**
	 * This panel adds a startup setting checkbox and a close button to the
	 * right.
	 * 
	 *@param container
	 *            , to which the buttons are added
	 */
	void addButtonPanel(Container container) {

		// mainButtonPanel
		JPanel mainButtonPanel = new JPanel();
		// Setting layout to null, allows us to give
		// specific alignments to components
		mainButtonPanel.setLayout(null);
		mainButtonPanel.setBackground(new Color(190, 200, 211));
		mainButtonPanel.setPreferredSize(new Dimension(470, 201));

		// add the components
		addStartupCheck(mainButtonPanel);
		addCloseButton(mainButtonPanel);

		container.add(mainButtonPanel);
	}

	/**
	 * Add a checkBox to the button Panel
	 * 
	 *@param container
	 *            , to which the buttons are added
	 */
	void addStartupCheck(Container container) {

		startupCheck = new JCheckBox(
				"<html><table cellpadding=0><tr><td width=9/><td><font size=3>Show this dialog upon startup </font></td></tr></table></html>");
		startupCheck.setLocation(0, -28);
		startupCheck.setSize(170, 100);
		startupCheck.setBorder(null);
		startupCheck.setFont(new Font("Times New Roman", Font.PLAIN, 12));
		startupCheck.setBackground(new Color(190, 200, 211));
		startupCheck.setSelected(true);

		container.add(startupCheck);
	}

	/**
	 * Add a close button to the button panel
	 * 
	 *@param container
	 *            , to which the buttons are added
	 */
	void addCloseButton(Container container) {

		close = new JButton("Close");
		close.setSize(75, 22);
		close.setLocation(395, 10);
		close.setFont(new Font("Times New Roman", Font.PLAIN, 12));

		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				setVisible(false);
				dispose();
			}
		});

		container.add(close);
	}

	/**
	 * Add All the Message Labels & Links to the main WelcomePanel
	 * 
	 *@param container
	 *            , to which the buttons are added
	 */
	void addWelcomePanel(Container container) {

		JPanel mainWelcomePanel = new JPanel();
		// to be able to add components at specific locations.
		mainWelcomePanel.setLayout(null);
		mainWelcomePanel.setBorder(BorderFactory.createLineBorder(Color.black));
		mainWelcomePanel.setBackground(new Color(227, 231, 236));
		mainWelcomePanel.setPreferredSize(new Dimension(470, 208));
		mainWelcomePanel.setLocation(100, 100);

		// Add the Image in the Left Area in the bordered Panel
		JLabel logo = new JLabel(new ImageIcon(keplerLogoURL));
		logo.setLocation(-28, -30);
		logo.setSize(200, 200);
		mainWelcomePanel.add(logo);

		// Add the Main Welcome Message
		JLabel welcomeMsg = new JLabel("Welcome to Kepler 1.0");
		welcomeMsg.setLocation(130, -30);
		welcomeMsg.setSize(400, 100);
		// set Font
		Font font = new Font("Arial", Font.BOLD, 28);
		Map fontAttr = new HashMap();
		fontAttr.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		font = font.deriveFont(fontAttr);
		welcomeMsg.setFont(font);
		mainWelcomePanel.add(welcomeMsg);

		// Engrave Welcome Msg if Required/Make Bolder
		// TextAttribute.WEIGHT_EXTRABOLD bug in jdk1.5,1.4.2, hence
		// making msg bold this way.
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4920831
		JLabel bolderMsg = new JLabel("Welcome to Kepler 1.0");
		bolderMsg.setLocation(129, -30);
		bolderMsg.setSize(400, 100);
		bolderMsg.setFont(font);
		// All the above label does is make
		// the welcome message look bolder.
		mainWelcomePanel.add(bolderMsg);

		// Add the Kepler Intro message
		JLabel introMsg = new JLabel();
		introMsg.setLocation(162, -127);
		introMsg.setSize(235, 400);
		introMsg
				.setText("<html>A collaborative environment for creating and executing scientific workflows</html>");
		introMsg.setFont(new Font("Arial", Font.BOLD, 16));
		mainWelcomePanel.add(introMsg);

		// Add the Getting Started message
		JLabel getStartedMsg = new JLabel("Getting Started");
		getStartedMsg.setLocation(137, 85);
		getStartedMsg.setSize(400, 100);
		getStartedMsg.setFont(new Font("Arial", Font.BOLD, 20));
		mainWelcomePanel.add(getStartedMsg);

		// Engrave Getting Started Msg if Required/Make Bolder
		// TextAttribute.WEIGHT_EXTRABOLD bug in jdk1.5,1.4.2
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4920831
		JLabel bolderMsg2 = new JLabel("Getting Started");
		bolderMsg2.setLocation(136, 85);
		bolderMsg2.setSize(400, 100);
		bolderMsg2.setFont(new Font("Arial", Font.BOLD, 20));
		mainWelcomePanel.add(bolderMsg2);

		// Add the Scientists Link
		scientistLink = new JLabel();
		scientistLink.setLocation(162, 150);
		scientistLink.setSize(77, 40);
		scientistLink.setFont(new Font("Arial", Font.PLAIN, 18));
		scientistLink.setText("<html><a href=" + "" + ">" + "Scientists"
				+ "</a>");
		scientistLink.addMouseListener(new MouseListener() {

			String url = "http://kepler-project.org/Wiki.jsp?page=Documentation";

			public void mouseClicked(MouseEvent evt) {
				new BrowserLauncher().openFrameLink(url);
			}

			public void mouseExited(MouseEvent evt) {
			}

			public void mouseEntered(MouseEvent evt) {

				scientistLink.setToolTipText(url);
			}

			public void mousePressed(MouseEvent evt) {
			}

			public void mouseReleased(MouseEvent evt) {
			}
		});
		mainWelcomePanel.add(scientistLink);

		// Add the Programmers Link
		programmerLink = new JLabel();
		programmerLink.setLocation(162, 177);
		programmerLink.setSize(108, 20);
		programmerLink.setFont(new Font("Arial", Font.PLAIN, 18));
		programmerLink.setText("<html><a href=" + "" + ">" + "Programmers"
				+ "</a>");
		programmerLink.addMouseListener(new MouseListener() {

			String url = "http://kepler-project.org/Wiki.jsp?page=DevelopmentForKepler";

			public void mouseClicked(MouseEvent evt) {
				new BrowserLauncher().openFrameLink(url);
			}

			public void mouseExited(MouseEvent evt) {
			}

			public void mouseEntered(MouseEvent evt) {

				programmerLink.setToolTipText(url);
			}

			public void mousePressed(MouseEvent evt) {
			}

			public void mouseReleased(MouseEvent evt) {
			}

		});
		mainWelcomePanel.add(programmerLink);

		container.add(mainWelcomePanel);

	}

	/**
	 *@return Selected State of startup setting
	 */
	public boolean getStartupCheck() {

		return startupCheck.isSelected();
	}

	/**
	 * The main program for the WelcomeScreen class
	 * 
	 *@param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				String keplerLogo = System.getProperty("KEPLER")
						+ "common/resources/images/KeplerLogoNew.png";
				WelcomeScreen w = new WelcomeScreen(keplerLogo);
			}
		});
	}

	/**
	 * BrowserLauncher to launch URL on the WelcomeScreen
	 * 
	 *@author berkley
	 *@since November 9, 2006
	 */

	public static class BrowserLauncher {

		/**
		 * open browser depending on operating system.
		 * 
		 *@param url
		 *            Description of the Parameter
		 */
		public static void openFrameLink(String url) {

			try {
				String os = System.getProperty("os.name");
				if (os.startsWith("Windows")) {
					// os is windows
					Runtime.getRuntime().exec(
							"rundll32 url.dll,FileProtocolHandler " + url);
				} else if (os.startsWith("Mac OS")) {
					// os is mac
					Method openURLMethod = (Class
							.forName("com.apple.eio.FileManager"))
							.getDeclaredMethod("openURL",
									new Class[] { String.class });
					openURLMethod.invoke(null, new Object[] { url });
				} else {
					// os is either unix or linux
					String[] browsers = { "firefox", "opera", "konqueror",
							"epiphany", "mozilla", "netscape" };
					String browser = "";
					for (int i = 0; i < browsers.length && browser == null; i++) {
						if (Runtime.getRuntime().exec(
								new String[] { "which", browsers[i] })
								.waitFor() == 0) {
							browser = browsers[i];
						}
					}
					if (browser != null) {
						Runtime.getRuntime()
								.exec(new String[] { browser, url });
					}
				}

			} catch (Exception e) {
				System.err.println(e.toString());
			}

		}

	}

}