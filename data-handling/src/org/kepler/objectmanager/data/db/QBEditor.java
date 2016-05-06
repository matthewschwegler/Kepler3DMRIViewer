/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data.db;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.ecoinformatics.seek.querybuilder.DBQueryDef;
import org.ecoinformatics.seek.querybuilder.DBQueryDefParserEmitter;
import org.ecoinformatics.seek.querybuilder.DBSchemaParserEmitter;
import org.ecoinformatics.seek.querybuilder.QBApp;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.StringUtilities;

//////////////////////////////////////////////////////////////////////////
//// QBEditor
/**
 * QBEditor is a top-level window containing the Query Builder UI.
 */
public class QBEditor extends TableauFrame implements TableModelListener {

	/**
	 * Construct an QB with the Schema and SQl. The SQL can be empty, but there
	 * needs to be a schema
	 * 
	 * @param factory
	 *            a Tableau Factory
	 * @param sqlAttr
	 *            a SQL XML String document
	 * @param schemaAttr
	 *            a Schema XML String document
	 * @param title
	 *            the title
	 */
	public QBEditor(QBTableauFactory factory, StringAttribute sqlAttr,
			StringAttribute schemaAttr, String title) {
		// NOTE: Create with no status bar, since we have no use for it now.
		super(null, null);

		_factory = factory;
		_sqlAttr = sqlAttr;
		_schemaAttr = schemaAttr;
		_title = title;

		String sqlStr = _sqlAttr.getExpression();
		String schemaStr = _schemaAttr.getExpression();

		JComponent compToAdd = null;

		boolean parsedOK = false;
		if (schemaStr != null && schemaStr.length() > 0) {
			DSSchemaIFace schemaIFace = DBSchemaParserEmitter
					.parseSchemaDef(schemaStr);
			if (schemaIFace != null) {
				DBQueryDef queryDef = null;
				if (sqlStr != null && sqlStr.length() > 0) {
					queryDef = DBQueryDefParserEmitter.parseQueryDef(
							schemaIFace, sqlStr);
				}

				_qbApp = new QBApp(this);
				_qbApp.setExternalTMListener(this);

				_qbApp.set(schemaIFace, queryDef);
				compToAdd = _qbApp;
				parsedOK = true;
			}
		}

		if (!parsedOK) {
			JLabel label = new JLabel(
					"\nThere was a problem loading the schema or it was empty.\n\n");
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setForeground(Color.RED);
			compToAdd = label;
		}

		getContentPane().add(compToAdd, BorderLayout.CENTER);
		_initialSaveAsFileName = "qb.query";

		super.hideMenuBar();
	}

	public void pack() {
		// this sets off the Top.pack() method, which kicks off a thread...
		super.pack();

		// ...so we need to start our own thread to wait for it to finish
		deferIfNecessary(new WaitThread(this, _title));
	}

	// /////////////////////////////////////////////////////////////////
	// // public variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * React to notification that an attribute or set of attributes changed.
	 */
	/*
	 * public void changedUpdate(DocumentEvent e) { // Do nothing... We don't
	 * care about attributes. }
	 */

	// -----------------------------------------------------------------------------
	// -- TableModelListener Interface
	// -----------------------------------------------------------------------------
	public void tableChanged(TableModelEvent e) {
		setModified(true);
	}

	/**
	 * For cancel button
	 * 
	 * 	 */
	public boolean closeWindows() {
		_needPopDialog = false; // don't need pop dialog
		_fromCancel = true;
		boolean close = _close();
		_needPopDialog = true; // reset it
		_fromCancel = false; // reset it
		return close;
	}

	/**
	 * For okay button
	 * 
	 * 	 */
	public boolean save() {
		_needPopDialog = false; // don't need pop dialog

		if (isModified()) {
			_save();
		}
		boolean close = _close();
		_needPopDialog = true; // reset it
		return close;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	/**
	 * Clear the current contents. First, check to see whether the contents have
	 * been modified, and if so, then prompt the user to save them. A return
	 * value of false indicates that the user has canceled the action.
	 * 
	 * @return False if the user cancels the clear.
	 */
	protected boolean _clear() {
		if (super._clear()) {
			// text.setText("");
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Display more detailed information than given by _about().
	 */
	protected void _help() {
		// FIXME: Give instructions for the editor here.
		_about();
	}

	/**
	 * Print the contents.
	 */
	protected void _print() {
		// FIXME: What should we print?
		super._print();
	}

	/**
	 * Override to query whether to apply the changes, if any.
	 * 
	 * @return False if the user cancels on a apply query.
	 */
	protected boolean _close() {
		// NOTE: The superclass doesn't do the right thing here,
		// since it requires an associated Tableau.

		// NOTE: We use dispose() here rather than just hiding the
		// window. This ensures that derived classes can react to
		// windowClosed events rather than overriding the
		// windowClosing behavior given here.
		boolean returnValue = true;
		if (isModified() && _needPopDialog) {

			if (_queryForApply()) {
				dispose();
			} else {
				return false;
			}
		} else {
			// Window is not modified, so just dispose.
			if (_fromCancel || !isModified()) {
				setModified(false);
			}
			dispose();
		}

		// Ensure that a new editor is opened next time.
		this._factory.clear();

		return returnValue;
	}

	/**
	 * Override the base class to apply the change to the attribute.
	 * 
	 * @return True if the save succeeded.
	 */
	protected boolean _save() {
		// Issue a change request to ensure the change is
		// applied at a safe time and that the model is marked
		// modified.
		DBQueryDef queryDef = new DBQueryDef();
		_qbApp.fillQueryDef(queryDef);
		NamedObj context = (NamedObj) _sqlAttr.getContainer();
		String request = "<property name=\""
				+ _sqlAttr.getName()
				+ "\" value=\""
				// + StringUtilities.escapeForXML(_factory.getText())
				+ StringUtilities.escapeForXML(DBQueryDefParserEmitter
						.emitXML(queryDef)) + "\"/>";
		// System.out.println("request: "+request+"\n\n");
		context.requestChange(new MoMLChangeRequest(this, context, request));
		setModified(true);
		return true;
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// Open a dialog to prompt the user to apply the data.
	// Return false if the user clicks "cancel", and otherwise return true.
	private boolean _queryForApply() {
		Object[] options = { "Apply", "Discard changes", "Cancel" };
		String query = "Apply changes to " + _sqlAttr.getFullName() + "?";
		// Show the MODAL dialog
		int selected = JOptionPane.showOptionDialog(this, query,
				"Apply Changes?", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (selected == 0) {
			return _save();
		} else if (selected == 1) {
			setModified(false);
			return true;
		}
		return false;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected variables ////

	/** The scroll pane containing the text area. */
	protected JScrollPane _scrollPane;
	protected QBApp _qbApp;

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private final QBTableauFactory _factory;
	private StringAttribute _sqlAttr;
	private StringAttribute _schemaAttr;
	private String _title;
	private boolean _needPopDialog = true;
	private boolean _fromCancel = false;

}

class WaitThread extends Thread {

	private QBEditor frame;
	private String title;

	public WaitThread(QBEditor frame, String title) {
		this.frame = frame;
		this.title = title;
		this.setPriority(this.getPriority() - 2);
	}

	public void run() {

		int safetyCounter = 0;

		while (!frame.isMenuPopulated()) {
			this.yield();
			try {
				this.sleep(5);
			} catch (InterruptedException ex) {
			}
			if (safetyCounter++ > 200)
				break;
		}
		frame.setTitle(title);
	}
}