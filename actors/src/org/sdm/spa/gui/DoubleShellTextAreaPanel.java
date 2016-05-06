/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: brooks $'
 * '$Date: 2012-06-17 17:36:48 -0700 (Sun, 17 Jun 2012) $' 
 * '$Revision: 29975 $'
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

package org.sdm.spa.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import ptolemy.gui.MessageHandler;
import ptolemy.gui.ShellInterpreter;

//////////////////////////////////////////////////////////////////////////
//// DoubleShellTextArea

/**
 * A read-only text area showing the result of the previous step and a text area
 * supporting shell-style interactions.
 * 
 * @author Ilkay Altintas
 * @version $Id: DoubleShellTextAreaPanel.java 11161 2005-11-01 20:39:16Z ruland
 *          $
 */
public class DoubleShellTextAreaPanel extends JPanel {

	/**
	 * Create a new instance with no initial message.
	 */
	public DoubleShellTextAreaPanel() {
		this(null);
	}

	/**
	 * Create a new instance with the specified initial message.
	 * 
	 * @param initialMessage
	 *            The initial message.
	 */
	public DoubleShellTextAreaPanel(String initialMessage) {
		// Graphics
		super(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createLineBorder(Color.black), ""));

		// FIXME: Border and shell size needs to be configurable.
		shell1 = new JTextArea("", 20, 80);
		JScrollPane jSP_up = new JScrollPane(shell1);
		// add(jSP1, BorderLayout.NORTH);
		shell2 = new JTextArea("", 20, 80);
		JScrollPane jSP_down = new JScrollPane(shell2);
		// add(jSP2, BorderLayout.SOUTH);
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jSP_up,
				jSP_down);
		pane.setDividerLocation(0.5);
		add(pane, BorderLayout.CENTER);

		shell2.addKeyListener(new ShellKeyListener());
	}

	public static void main(String[] args) {
		JFrame jFrame = new JFrame("ShellTextArea Example");
		WindowListener windowListener = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		};
		jFrame.addWindowListener(windowListener);

		final DoubleShellTextAreaPanel exec = new DoubleShellTextAreaPanel();
		jFrame.getContentPane().add(exec);
		jFrame.setSize(600, 400);
		jFrame.pack();
		jFrame.show();
	}

	/**
	 * Override the base class to output the first prompt. We need to do this
	 * here because we can't write to the TextArea until the peer has been
	 * created.
	 */
	public void addNotify() {
		super.addNotify();
		initialize(_initialMessage);
	}

	/**
	 * Set the associated text area editable (with a true argument) or not
	 * editable (with a false argument). This should be called in the swing
	 * event thread.
	 * 
	 * @param editable
	 *            True to make the text area editable, false to make it
	 *            uneditable.
	 */
	public void setEditable() {
		shell2.setEditable(true);
		shell1.setEditable(false);

	}

	/**
	 * Append the specified text to the JTextArea and update the prompt cursor.
	 * The text will actually be appended in the swing thread, not immediately.
	 * This method immediately returns.
	 * 
	 * @param text
	 *            The text to append to the text area.
	 */
	public void appendShell2(final String text) {
		Runnable doAppendSh2 = new Runnable() {
			public void run() {
				shell2.append(text);
				// Scroll down as we generate text.
				shell2.setCaretPosition(shell2.getText().length());
				// To prevent _promptCursor from being
				// updated before the JTextArea is actually updated,
				// this needs to be inside the Runnable.
				_promptCursor += text.length();
			}
		};
		SwingUtilities.invokeLater(doAppendSh2);
	}

	public void appendShell1(final String text) {
		shell1.setEditable(true);
		Runnable doAppendSh1 = new Runnable() {
			public void run() {
				shell1.append(text);
				// Scroll down as we generate text.
				shell1.setCaretPosition(shell1.getText().length());
			}
		};
		SwingUtilities.invokeLater(doAppendSh1);
	}

	/**
	 * Clear the JTextArea and reset the prompt cursor. The clearing is done in
	 * the swing thread, not immediately. This method immediately returns.
	 */
	public void clearShell1() {
		Runnable doClearShell1 = new Runnable() {
			public void run() {
				// shell1.setText(_greetSh1);
				shell1.setCaretPosition(0);
			}
		};
		SwingUtilities.invokeLater(doClearShell1);
	}

	/**
	 * Clear the JTextArea and reset the prompt cursor. The clearing is done in
	 * the swing thread, not immediately. This method immediately returns.
	 */
	public void clearShell2() {
		Runnable doClearShell2 = new Runnable() {
			public void run() {
				shell2.setText("");
				shell2.setCaretPosition(0);
				_promptCursor = 0;
			}
		};
		SwingUtilities.invokeLater(doClearShell2);
	}

	/**
	 * Initialize the text area with the given starting message, followed by a
	 * prompt. If the argument is null or the empty string, then only a prompt
	 * is shown.
	 * 
	 * @param initialMessage
	 *            The initial message.
	 */
	public void initialize(String initialMessage) {
		clearShell1();
		appendShell1(initialMessage + "\n" + _greetSh1 + "\n");
		clearShell2();
		appendShell2(mainPrompt);
		shell1.setEditable(false);
	}

	/**
	 * Replace a range in the JTextArea.
	 */
	public void replaceRangeJTextArea(final String text, final int start,
			final int end) {
		Runnable doReplaceRangeJTextArea = new Runnable() {
			public void run() {
				shell2.replaceRange(text, start, end);
			}
		};
		SwingUtilities.invokeLater(doReplaceRangeJTextArea);
	}

	/**
	 * Return the result of a command evaluation. This method is used when it is
	 * impractical to insist on the result being returned by evaluateCommand()
	 * of a ShellInterpreter. For example, computing the result may take a
	 * while.
	 * 
	 * @param result
	 *            The result to return.
	 */
	public void returnResult(final String result) {
		// Make the text area editable again.
		Runnable doMakeEditable = new Runnable() {
			public void run() {
				setEditable();
				String toPrint = result + "\n" + _greetSh1;
				appendShell1(toPrint);
				appendShell2("\n" + mainPrompt);
			}
		};
		SwingUtilities.invokeLater(doMakeEditable);
	}

	/**
	 * Set the interpreter.
	 * 
	 * @param interpreter
	 *            The interpreter.
	 * @see #getInterpreter()
	 */
	public void setInterpreter(ShellInterpreter interpreter) {
		_interpreter = interpreter;
	}

	/**
	 * Get the interpreter that has been registered with setInterpreter().
	 * 
	 * @return The interpreter, or null if none has been set.
	 * @see #setInterpreter(ShellInterpreter)
	 */
	public ShellInterpreter getInterpreter() {
		return _interpreter;
	}

	// /////////////////////////////////////////////////////////////////
	// // public variables ////

	public JTextArea shell1;
	public JTextArea shell2;

	/** Main prompt. */
	public String mainPrompt = ">> ";

	/** Size of the history to keep. */
	public int historyLength = 20;

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// Update the command history.
	private void _updateHistory(String command) {
		_historyCursor = 0;
		if (_historyCommands.size() == historyLength) {
			_historyCommands.removeElementAt(0);
		}
		_historyCommands.addElement(command);
	}

	// Evaluate the command so far, if possible, printing
	// a continuation prompt if not.
	// NOTE: This must be called in the swing event thread.
	private void _evalCommand() {
		String newtext = shell2.getText().substring(_promptCursor);
		_promptCursor += newtext.length();
		if (_commandBuffer.length() > 0) {
			_commandBuffer.append("\n");
		}
		_commandBuffer.append(newtext);
		String command = _commandBuffer.toString();

		if (_interpreter == null) {
			appendShell2("\n" + mainPrompt);
		} else {
			if (_interpreter.isCommandComplete(command)) {
				// Process it
				appendShell2("\n");
				Cursor oldCursor = shell2.getCursor();
				shell2.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				String result;
				try {
					result = _interpreter.evaluateCommand(command);
				} catch (RuntimeException e) {
					// RuntimeException are due to bugs in the expression
					// evaluation code, so we make the stack trace available.
					MessageHandler.error("Failed to evaluate expression", e);
					result = "Internal error evaluating expression.";
					throw e;
				} catch (Exception e) {
					result = e.getMessage();
					// NOTE: Not ideal here to print the stack trace, but
					// if we don't, it will be invisible, which makes
					// debugging hard.
					// e.printStackTrace();
				}
				if (result != null) {
					if (result.trim().equals("")) {
						appendShell2(mainPrompt);
					} else {
						appendShell2(result + "\n" + mainPrompt);
					}
				} else {
					// Result is incomplete.
					// Make the text uneditable to prevent further input
					// until returnResult() is called.
					// NOTE: We are assuming this called in the swing thread.
					setEditable();
				}
				_commandBuffer.setLength(0);
				shell2.setCursor(oldCursor);
				_updateHistory(command);
			} else {
				appendShell2("\n" + contPrompt);
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	// The initial message, if there is one.
	private String _initialMessage = null;
	private String _greetSh1 = "The outputs of the previous step.\nPlease double click on your selections to be sent to the next step\n";

	private int _promptCursor = 0;

	/** Prompt to use on continuation lines. */
	public String contPrompt = "";

	// The command input
	private StringBuffer _commandBuffer = new StringBuffer();

	// The interpreter.
	private ShellInterpreter _interpreter;

	// History
	private int _historyCursor = 0;
	private Vector _historyCommands = new Vector();

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////
	// The key listener
	private class ShellKeyListener extends KeyAdapter {
		public void keyTyped(KeyEvent keyEvent) {
			switch (keyEvent.getKeyCode()) {
			case KeyEvent.VK_UNDEFINED:
				if (keyEvent.getKeyChar() == '\b') {
					if (shell2.getCaretPosition() == _promptCursor) {
						keyEvent.consume(); // don't backspace over prompt!
					}
				}
				break;

			case KeyEvent.VK_BACK_SPACE:
				if (shell2.getCaretPosition() == _promptCursor) {
					keyEvent.consume(); // don't backspace over prompt!
				}
				break;
			default:
			}
		}

		public void keyReleased(KeyEvent keyEvent) {
			switch (keyEvent.getKeyCode()) {
			case KeyEvent.VK_BACK_SPACE:
				if (shell2.getCaretPosition() == _promptCursor) {
					keyEvent.consume(); // don't backspace over prompt!
				}
				break;
			default:
			}
		}

		public void keyPressed(KeyEvent keyEvent) {
			// Process keys
			switch (keyEvent.getKeyCode()) {
			case KeyEvent.VK_ENTER:
				keyEvent.consume();
				_evalCommand();
				break;
			case KeyEvent.VK_BACK_SPACE:
				if (shell2.getCaretPosition() <= _promptCursor) {
					// FIXME: Consuming the event is useless...
					// The backspace still occurs. Why? Java bug?
					keyEvent.consume(); // don't backspace over prompt!
				}
				break;
			case KeyEvent.VK_LEFT:
				if (shell2.getCaretPosition() == _promptCursor) {
					keyEvent.consume();
				}
				break;
			case KeyEvent.VK_HOME:
				shell2.setCaretPosition(_promptCursor);
				keyEvent.consume();
				break;
			default:
				switch (keyEvent.getModifiers()) {
				case InputEvent.CTRL_MASK:
					switch (keyEvent.getKeyCode()) {
					case KeyEvent.VK_A:
						shell2.setCaretPosition(_promptCursor);
						keyEvent.consume();
						break;
					default:
					}
					break;
				default:
					// Otherwise we got a regular character.
					// Don't consume it, and TextArea will
					// take care of displaying it.
				}
			}
		}
	}
}