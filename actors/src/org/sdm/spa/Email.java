/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.sdm.spa;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// Email
/**
 * <p>
 * Email actor is a notification actor that allows communication with the user
 * via email especially from the remote execution of the long-runing workflows.
 * </p>
 * 
 * <p>
 * Given the configuration parameters for the host SMTP server, to and from
 * addresses, the Email actor sends the data that is linked to its "messageBody"
 * multi-port as an output notification email from Kepler.
 * </p>
 * 
 * @UserLevelDocumentation <p>
 *                         An example usage of the Email actor can be found at
 *                         "workflows/test/emailTest.xml" in your SPA directory.
 *                         </p>
 * 
 * @author Ilkay Altintas
 * @version $Id: Email.java 24234 2010-05-06 05:21:26Z welker $
 */

public class Email extends TypedAtomicActor {

	/**
	 * Construct an Email actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public Email(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		toAddress = new StringParameter(this, "toAddress");
		toAddress.setExpression("your_login@yourisp.com");
		fromAddress = new StringParameter(this, "fromAddress");
		fromAddress.setExpression("your_login@yourisp.com");
		host = new StringParameter(this, "host");
		host.setExpression("smtp.yourisp.com");

		subject = new PortParameter(this, "subject");
		subject.setStringMode(true);
		subject.setTypeEquals(BaseType.STRING);
		subject.setExpression("Notification email from Kepler");

		messageBody = new TypedIOPort(this, "messageBody", true, false);
		messageBody.setMultiport(true);
		messageBody.setTypeEquals(BaseType.GENERAL);
		new Attribute(messageBody, "_showName");

		_attachText("_iconDescription", "<svg>\n" + "<text x=\"0\" y=\"30\""
				+ "style=\"font-size:40; fill:blue; font-family:SansSerif\">"
				+ "@</text>\n" + "</svg>\n");

	} // end of constructor

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * Email address that the email will be sent to.
	 */
	public StringParameter toAddress;
	/**
	 * Email address that the email will be sent from.
	 */
	public StringParameter fromAddress;
	/**
	 * The SMTP host of the from address.
	 */
	public StringParameter host;
	/**
	 * Generic typed message body. It is a multi-port meaning more than one
	 * input of different types could be connected to it.
	 */
	public TypedIOPort messageBody;

	/**
	 * @entity.description Generic typed message subject.
	 */
	public PortParameter subject;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Given a ...., Email actor ...
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		String _host = ((StringToken) host.getToken()).stringValue();
		String _toAddress = ((StringToken) toAddress.getToken()).stringValue();
		String _fromAddress = ((StringToken) fromAddress.getToken())
				.stringValue();

		// _messageBodyStr = ( (StringToken)
		// (messageBody.get(0))).stringValue();
		String _messageBodyStr = "";
		int i = 0;
		int width = messageBody.getWidth();
		for (i = 0; i < width; i++) {
			if (messageBody.hasToken(i)) {
				Token tokenArg = messageBody.get(i);
				String value = tokenArg.toString();
				_debug("messageBody(i) = " + value);
				value = value.substring(1, value.length() - 1);
				_messageBodyStr += value + "\n";
			}
		}
		// _debug(_host + "===" + _toAddress + "===" +
		// _fromAddress + "===" +_messageBodyStr);

		subject.update();
		String messageSubject = ((StringToken) subject.getToken())
				.stringValue();

		// Create properties, get Session
		Properties props = new Properties();

		// If using static Transport.send(),
		// need to specify which host to send it to
		props.put("mail.smtp.host", _host);
		// To see what is going on behind the scene
		props.put("mail.debug", "false");
		// props.put("mail.smtp.auth", "true");
		Session session = Session.getInstance(props);

		try {
			// Instantiate a message
			Message msg = new MimeMessage(session);

			// Set message attributes
			msg.setFrom(new InternetAddress(_fromAddress));
			InternetAddress[] address = { new InternetAddress(_toAddress) };
			msg.setRecipients(Message.RecipientType.TO, address);
			msg.setSubject(messageSubject);
			msg.setSentDate(new Date());

			// Set message content
			msg.setText(_messageBodyStr);

			// Send the message
			Transport.send(msg);
			// Transport transport = session.getTransport("smtp");
			// transport.connect(_host, _fromAddress, password);

		} catch (MessagingException mex) {
			// Prints all nested (chained) exceptions as well
			mex.printStackTrace();
		}

	} // end of fire

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	} // end of postfire

	/**
	 * Callback for changes in attribute values.
	 * 
	 * @param at
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the offsets array is not nondecreasing and nonnegative.
	 */
	/*
	 * public void attributeChanged(Attribute at) throws IllegalActionException
	 * { if (at == host){ _host = ((StringToken)host.getToken()).stringValue();
	 * _debug("host set to: " + _host); } else if (at == toAddress) { _toAddress
	 * = ((StringToken)toAddress.getToken()).stringValue();
	 * _debug("to address set to: " + _toAddress); } else if (at == fromAddress)
	 * { _fromAddress = ((StringToken)fromAddress.getToken()).stringValue();
	 * _debug("from address set to: " + _fromAddress); } } // end of
	 * attributeChanged
	 */

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////
	// private static String _host = "";
	// private static String _toAddress = "";
	// private static String _fromAddress = "";
}