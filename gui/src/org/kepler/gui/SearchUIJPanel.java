/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-03-18 19:24:12 -0700 (Fri, 18 Mar 2011) $' 
 * '$Revision: 27324 $'
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.InsetsUIResource;

import org.kepler.util.StaticResources;

/**
 * Class to build the Search User Interface JPanel that allows the user to enter
 * a single search term to search by. 5 buttons can be optionally configured by
 * setting Actions for those buttons.
 * <ul>
 * The 5 configurable buttons are
 * <li>Search Button</li>
 * <li>Reset Button</li>
 * <li>Cancel Button</li>
 * <li>Source Button</li>
 * <li>Advanced Button</li>
 * </ul>
 * 
 * @author brooke
 * @since 4 Nov 2005
 */
public class SearchUIJPanel extends JPanel {

	// constants

	// SPACING is the exterior space around buttons
	private static final int SPACING_HORIZ = 0; //originally:1 but now trying to minimize pane width
	private static final int SPACING_VERT = 1;

	private static final int BUTTON_WIDTH = 83; //originally:72 but didn't fit on os X default jbutton 
	private static final int BUTTON_HEIGHT = 25;
	private static final int SPACER_ABOVE_BELOW_TITLEDBORDER = 10;
	private static final int WIDE_BUTTON_SPACER = 10;

	private int PANEL_WIDTH = 200; // recalculated during init()
	public static boolean SEARCHREPOS = false;
	public static final String SEARCH_BUTTON_CAPTION = StaticResources
			.getDisplayString("search.search", "");
	//public static final String RESET_BUTTON_CAPTION = StaticResources
	//		.getDisplayString("search.reset", "");
	public static final String CANCEL_BUTTON_CAPTION = StaticResources
			.getDisplayString("search.cancel", "");
	public static final String SOURCE_BUTTON_CAPTION = StaticResources
			.getDisplayString("search.sources", "");
	public static final String ADV_BUTTON_CAPTION = StaticResources
			.getDisplayString("search.advanced", "");

	private final Dimension BUTTON_DIMS = new Dimension(BUTTON_WIDTH,
			BUTTON_HEIGHT);

	// this seemed too wide
	//private final Dimension WIDE_BUTTON_DIMS = new Dimension(SPACING_HORIZ + 2
	//		* BUTTON_WIDTH, BUTTON_HEIGHT);
	private final Dimension WIDE_BUTTON_DIMS = new Dimension(WIDE_BUTTON_SPACER+
			  BUTTON_WIDTH, BUTTON_HEIGHT);

	// set the inside space between the button's borders
	// and the text it contains
	private final InsetsUIResource BUTTON_INSIDE_PADDING = new InsetsUIResource(
			2, 0, 2, 0); // (top, left, bottom, right)

	private static int BUTTON_FONT_SIZE = StaticResources.getSize(
			"button.limitedSpace.maxFontSize", 11);

	private JTextField searchValueField = null;

	private String borderTitle = "Search";
	private Border titledBorder;

	private Action searchAction;
	//private Action resetAction;
	private Action cancelAction;
	private Action sourceAction;
	private Action advancedAction;

	private JButton searchButton;
	//private JButton resetButton;
	private JButton cancelButton;
	private JButton sourceButton;
	private JButton advancedButton;

	/**
	 * Empty constructor.
	 */
	public SearchUIJPanel() {
	}

	/**
	 * Constructor - creates a panel with a titled border (from String param
	 * panelBorderTitle), containing a textfield with up to 5 buttons beneath
	 * it. Constructor can accept <code>javax.swing.Action</code> objects for
	 * these 5 buttons; if any of these Action objects is null, the button will
	 * not be displayed on the user interface at runtime
	 * 
	 * @param panelBorderTitle
	 *            String
	 * @param searchButtonAction
	 *            Action
	 * @param resetButtonAction
	 *            Action
	 * @param cancelButtonAction
	 *            Action
	 * @param sourceButtonAction
	 *            Action
	 * @param advancedButtonAction
	 *            Action
	 */
	public SearchUIJPanel(String panelBorderTitle, Action searchButtonAction,
			Action resetButtonAction, Action cancelButtonAction,
			Action sourceButtonAction, Action advancedButtonAction) {

		setBorderTitle(panelBorderTitle);
		setSearchAction(searchButtonAction);
		//setResetAction(resetButtonAction);
		setCancelAction(cancelButtonAction);
		setSourceAction(sourceButtonAction);
		setAdvancedAction(advancedButtonAction);
	}

	/**
	 * Returns the border title for this search panel. The default title is
	 * "Search".
	 * 
	 * @return the search border title
	 */
	public String getBorderTitle() {
		return borderTitle;
	}

	public void setBorderTitle(String borderTitle) {
		this.borderTitle = borderTitle;
	}

	/**
	 * Returns the javax.swing.Action object that is called when the Search
	 * button is pressed.
	 * 
	 * @return the Action object that handles the search button
	 */
	public Action getSearchAction() {
		return searchAction;
	}

	/**
	 * Sets the Action to be used when the Search button is pressed. If this
	 * Action is not set then the Search button will not be visible to the user.
	 * 
	 * @param searchAction
	 *            the Action to be used when the Search button is pressed
	 */
	public void setSearchAction(Action searchAction) {
		this.searchAction = searchAction;
	}

	/**
	 * Returns the javax.swing.Action object that is called when the Reset
	 * button is pressed.
	 * 
	 * @return the Action object that handles the reset button
	 */
	//public Action getResetAction() {
	//	return resetAction;
	//}

	/**
	 * Sets the Action to be used when the Reset button is pressed. If this
	 * Action is not set then the Reset button will not be visible to the user.
	 * 
	 * @param resetAction
	 *            the Action to be used when the Reset button is pressed
	 */
	//public void setResetAction(Action resetAction) {
	//	this.resetAction = resetAction;
	//}

	/**
	 * Returns the javax.swing.Action object that is called when the Cancel
	 * button is pressed.
	 * 
	 * @return the Action object that handles the Cancel button
	 */
	public Action getCancelAction() {
		return cancelAction;
	}

	/**
	 * Sets the Action to be used when the Cancel button is pressed. If this
	 * Action is not set then the Cancel button will not be visible to the user.
	 * 
	 * @param cancelAction
	 *            the Action to be used when the Cancel button is pressed
	 */
	public void setCancelAction(Action cancelAction) {
		this.cancelAction = cancelAction;
	}

	/**
	 * Returns the javax.swing.Action object that is called when the Source
	 * button is pressed.
	 * 
	 * @return the Action object that handles the Source button
	 */
	public Action getSourceAction() {
		return sourceAction;
	}

	/**
	 * Sets the Action to be used when the Source button is pressed. If this
	 * Action is not set then the Source button will not be visible to the user.
	 * 
	 * @param sourceAction
	 *            the Action to be used when the Source button is pressed
	 */
	public void setSourceAction(Action sourceAction) {
		this.sourceAction = sourceAction;
	}

	/**
	 * Returns the javax.swing.Action object that is called when the Advanced
	 * button is pressed.
	 * 
	 * @return the Action object that handles the Advanced button
	 */
	public Action getAdvancedAction() {
		return advancedAction;
	}

	/**
	 * Sets the Action to be used when the Advanced button is pressed. If this
	 * Action is not set then the Advanced button will not be visible to the
	 * user.
	 * 
	 * @param advancedAction
	 *            the Action to be used when the Advanced button is pressed
	 */
	public void setAdvancedAction(Action advancedAction) {
		this.advancedAction = advancedAction;
	}

	/**
	 * 
	 * @return the current search term String from the textfield
	 */
	public String getSearchTerm() {
		return searchValueField.getText();
	}

	/**
	 * get the preferred/minimum width of this panel - calculated to allow
	 * enough space for all buttons and spacers etc
	 * 
	 * @return the minimum allowable width of this panel
	 */
	public final int getMinimumWidth() {
		return PANEL_WIDTH;
	}

	/**
	 * set the current search term String in the textfield
	 * 
	 * @param searchTerm
	 *            String
	 */
	public void setSearchTerm(String searchTerm) {
		if (searchTerm == null){
			searchTerm = "";
		}
		searchValueField.setText(searchTerm.trim());
	}

	/**
	 * if enabled==true, enable the textfield, search, reset, source and
	 * advanced buttons, and disable the cancel button. If enabled==false, do
	 * the reverse.
	 * 
	 * @param enabled
	 *            boolean
	 */
	public void setSearchEnabled(boolean enabled) {
		searchValueField.setEnabled(enabled);
		if (searchButton != null){
			searchButton.setEnabled(enabled);
		}
		//if (resetButton != null)
		//	resetButton.setEnabled(enabled);
		if (cancelButton != null){
			cancelButton.setEnabled(!enabled);
		}
		if (sourceButton != null){
			sourceButton.setEnabled(enabled);
		}
		if (advancedButton != null){
			advancedButton.setEnabled(enabled);
		}
	}
	
	//enable/disable cancel button
	public void setCancelButtonEnabled(boolean enabled) {
		if (cancelButton != null){
			cancelButton.setEnabled(enabled);
		}
	}

	/**
	 * Enables/disables _all_ Search buttons we will use this to disable all
	 * buttons as we fetch datasources from the repository if enabled==true,
	 * enable the textfield, search, reset, source and advanced buttons, and
	 * cancel button. If enabled==false, do the reverse.
	 * 
	 * @param enabled
	 *            boolean
	 */
	public void setAllSearchEnabled(boolean enabled) {
		searchValueField.setEnabled(enabled);
		if (searchButton != null){
			searchButton.setEnabled(enabled);
		}
		//if (resetButton != null)
		//	resetButton.setEnabled(enabled);
		if (cancelButton != null){
			cancelButton.setEnabled(enabled);
		}
		if (sourceButton != null){
			sourceButton.setEnabled(enabled);
		}
		if (advancedButton != null){
			advancedButton.setEnabled(enabled);
		}
	}

	/**
	 * Initialize the search panel. This should be called after constructing and
	 * adding whichever Actions you want the search panel to contain.
	 */
	public void init() {

		// the panel that contains the textfield
		JPanel searchPanel = createPanel();
		searchPanel.setBackground(TabManager.BGCOLOR);
		JPanel checkBoxPanel = createPanel();
		checkBoxPanel.setBackground(TabManager.BGCOLOR);
		
		// remember default button margins
		final InsetsUIResource defaultUIMgrButtonMargin = (InsetsUIResource) UIManager
				.get("Button.margin");
		// now set our custom ones
		UIManager.put("Button.margin", BUTTON_INSIDE_PADDING);

		// remember default button font
		final Font defaultUIMgrButtonFont = (Font) UIManager.get("Button.font");

		// now set our custom size, provided it's smaller than the default:
		int buttonFontSize = (defaultUIMgrButtonFont.getSize() < BUTTON_FONT_SIZE) ? defaultUIMgrButtonFont
				.getSize()
				: BUTTON_FONT_SIZE;

		final Font BUTTON_FONT = new Font(defaultUIMgrButtonFont.getFontName(),
				defaultUIMgrButtonFont.getStyle(), buttonFontSize);

		UIManager.put("Button.font", BUTTON_FONT);
		
		searchValueField = new JTextField();
		//special search style for mac os X
		searchValueField.putClientProperty("JTextField.variant", "search");
		//when mac os X user clicks x in textfield, Reset
		searchValueField.putClientProperty("JTextField.Search.CancelAction", cancelAction);
		searchValueField.setAction(searchAction);
		
		searchPanel.add(searchValueField);
		
		if (searchAction != null) {
			searchButton = createButton(SEARCH_BUTTON_CAPTION, searchAction);
			searchButton.setMnemonic(KeyEvent.VK_ENTER);
			searchPanel.add(searchButton);
			//searchValueField.addActionListener(searchAction);
		}

		JPanel buttonsPanel = createButtonPanel();
		buttonsPanel.setBackground(TabManager.BGCOLOR);

		if (advancedAction != null) {
			advancedButton = createWideButton(ADV_BUTTON_CAPTION,
					advancedAction);
			buttonsPanel.add(advancedButton);
		}
		else{
			buttonsPanel.add(Box.createHorizontalStrut(BUTTON_DIMS.width));
		}
		if (sourceAction != null) {
			sourceButton = createButton(SOURCE_BUTTON_CAPTION, sourceAction);

			buttonsPanel.add(sourceButton);
			if (advancedAction != null) {
				buttonsPanel
						.add(Box.createHorizontalStrut(SPACING_HORIZ));
			}
		}
		
		if (cancelAction != null) {
			cancelButton = createButton(CANCEL_BUTTON_CAPTION, cancelAction);
			buttonsPanel.add(cancelButton);			
		}

		// restore default button margins
		if (defaultUIMgrButtonMargin != null) {
			UIManager.put("Button.margin", defaultUIMgrButtonMargin);
		}
		// restore default button font
		if (defaultUIMgrButtonFont != null) {
			UIManager.put("Button.font", defaultUIMgrButtonFont);
		}

		JPanel titledPanel = new JPanel();
		titledPanel.setBackground(TabManager.BGCOLOR);
		titledPanel.setLayout(new BoxLayout(titledPanel, BoxLayout.Y_AXIS));
		titledBorder = BorderFactory.createTitledBorder(borderTitle);
		titledPanel.setBorder(titledBorder);
		titledPanel.add(searchPanel);
		titledPanel.add(checkBoxPanel);
		titledPanel.add(buttonsPanel);

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(Box.createVerticalStrut(SPACER_ABOVE_BELOW_TITLEDBORDER));
		this.add(titledPanel);
		this.add(Box.createVerticalStrut(SPACER_ABOVE_BELOW_TITLEDBORDER));

		final JPanel instance = this;
		this.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				if (getRootPane() == null){
					return;
				}
				if (!instance.isShowing()){
					return;
				}
				// having searchButton be defaultButton 
				// means Enters get accepted a little too liberally
				// http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4544
				//getRootPane().setDefaultButton(searchButton);
			}
		});

		final Insets borderInsets = titledBorder.getBorderInsets(this);
		PANEL_WIDTH = (3 * (BUTTON_WIDTH + (2 * SPACING_HORIZ)))
				+ borderInsets.left + borderInsets.right;
		
		setSearchEnabled(true);
		this.setBackground(TabManager.BGCOLOR);
	}
	
	public void closing() {
		//System.out.println("SearchUIJPanel.closing()");
		searchButton.setAction(null);
		sourceButton.setAction(null);
		cancelButton.setAction(null);
		advancedButton.setAction(null);
		searchAction = null;
		sourceAction = null;
		cancelAction = null;
		advancedAction = null;
	}

	private JPanel createButtonPanel() {
		JPanel buttonPanel = createPanel();
		buttonPanel.setBackground(TabManager.BGCOLOR);
		buttonPanel.add(Box.createHorizontalGlue());
		return buttonPanel;
	}

	private JPanel createPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(SPACING_VERT,
				SPACING_HORIZ, SPACING_VERT, SPACING_HORIZ));
		return panel;
	}

	private JButton createButton(String caption, ActionListener listener) {
		return createButton(caption, listener, BUTTON_DIMS);
	}

	private JButton createWideButton(String caption, ActionListener listener) {
		return createButton(caption, listener, WIDE_BUTTON_DIMS);
	}

	private JButton createButton(String caption, ActionListener listener,
			Dimension dims) {
		JButton button = new JButton(caption);
		button.addActionListener(listener);
		button.setMinimumSize(dims);
		button.setPreferredSize(dims);
		button.setMaximumSize(dims);
		button.setAlignmentX(SwingConstants.CENTER);
		return button;
	}
}