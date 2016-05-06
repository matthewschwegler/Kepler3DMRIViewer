/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-11 15:16:15 -0700 (Tue, 11 Sep 2012) $' 
 * '$Revision: 30633 $'
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

/**
 * 
 */
package org.kepler.gui.kar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KARFile;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * @author Aaron Schultz
 * 
 */
public class KarManifestViewer extends JFrame implements ActionListener {
	private static final long serialVersionUID = -739762970203187753L;
	private static final Log log = LogFactory.getLog(KarManifestViewer.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private KARFile _karFile;
	private JTextField _lsidField;
	private JTextArea _manifestField;
	private JScrollPane _manifestScrollPane;

	/**
	 * @throws HeadlessException
	 */
	public KarManifestViewer() throws HeadlessException {
		this("KAR Manifest Viewer");
	}

	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public KarManifestViewer(String title) throws HeadlessException {
		super(title);
	}

	public void initialize(KARFile karFile) {
		if (isDebugging) {
			log.debug("initialize( " + karFile + " )");
		}
		_karFile = karFile;

		JPanel layoutPanel = new JPanel();
		layoutPanel.setLayout(new BorderLayout());

		JPanel lsidPanel = new JPanel();
		lsidPanel.setLayout(new BorderLayout());

		JLabel lsidLabel = new JLabel("KAR LSID:");
		lsidPanel.add(lsidLabel, BorderLayout.WEST);

		_lsidField = new JTextField("");
		_lsidField.setEditable(false);
		lsidPanel.add(_lsidField, BorderLayout.CENTER);

		JPanel dfPanel = new JPanel();
		dfPanel.setLayout(new BorderLayout());

		_manifestField = new JTextArea("");
		_manifestField.setEditable(false);
		_manifestScrollPane = new JScrollPane(_manifestField);
		_manifestScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		_manifestScrollPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createCompoundBorder(BorderFactory
						.createTitledBorder("Manifest"), BorderFactory
						.createEmptyBorder(5, 5, 5, 5)), _manifestScrollPane
						.getBorder()));

		dfPanel.add(_manifestScrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());

		String infStr = _karFile.getName();
		JLabel objInfo = new JLabel(infStr);
		buttonPanel.add(objInfo, BorderLayout.CENTER);

		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(this);
		buttonPanel.add(refreshButton, BorderLayout.EAST);

		layoutPanel.add(lsidPanel, BorderLayout.NORTH);
		layoutPanel.add(dfPanel, BorderLayout.CENTER);
		layoutPanel.add(buttonPanel, BorderLayout.SOUTH);

		getContentPane().add(layoutPanel);

		refreshValues();

		getContentPane().setBackground(Color.WHITE);
	}

	/**
	 * Update the textfield and textarea to reflect the current LSID and LSID
	 * referral list values.
	 */
	public void refreshValues() {

		KeplerLSID lsid = _karFile.getLSID();

		StringBuilder manifest = new StringBuilder();
		try {
			ZipEntry mane = _karFile.getEntry(JarFile.MANIFEST_NAME);

			final char[] buffer = new char[0x10000];
			InputStream is = _karFile.getInputStream(mane);
		    Reader in = null;
		    try {
    			in = new InputStreamReader(is);
    			int read;
    			do {
    				read = in.read(buffer, 0, buffer.length);
    				if (read > 0) {
    					manifest.append(buffer, 0, read);
    				}
    			} while (read >= 0);
		    } finally {
		        if(in != null) {
		            in.close();
		        }
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}

		if (isDebugging) {
			log.debug(lsid.toString());
			log.debug(manifest.toString());
		}
		_lsidField.setText(lsid.toString());
		_manifestField.setText(manifest.toString());

		// not sure why this doesn't work
		// _manifestScrollPane.getVerticalScrollBar().setValue(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o instanceof JButton) {
			JButton b = (JButton) o;
			if (b.getText().equals("Refresh")) {
				log.debug("Refresh");
				refreshValues();
			}
		}
	}

}
