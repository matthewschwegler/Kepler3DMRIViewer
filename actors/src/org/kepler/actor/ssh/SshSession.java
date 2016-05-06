/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-05-23 10:32:39 -0700 (Thu, 23 May 2013) $' 
 * '$Revision: 32079 $'
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

package org.kepler.actor.ssh;

import java.util.Hashtable;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.ExecFactory;
import org.kepler.ssh.ExecInterface;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// SshSession
/**
 * <p>
 * Creates an ssh session to a remote host. If requested, the session will not
 * be opened at first firing of this actor but postponed to the first actor that
 * uses ssh (e.g.ExecuteCmd.) This actor is useful for three things:<br/>
 * - to provide a private-key for public-key authentication. <br/>
 * - to connect to a remote machine at a certain point of the workflow and thus
 * ask for password (e.g. at the very beginning) not sometime during execution.<br/>
 * - to forward local ports and/or remote ports.
 * 
 * </p>
 * <p>
 * This actor uses the org.kepler.ssh package to have longlasting connections
 * 
 * </p>
 * <p>
 * Its output is the input target, which can be used to identify the created
 * SshSession object anywhere (all ssh related classes use a session factory to
 * create/retrieve sessions based on the target name) and thus use the
 * connection.
 * 
 * </p>
 * <p>
 * If the host is empty string or equals "local", nothing happens within this
 * actor. All related actors will use the Java Runtime for local execution
 * instead of ssh.
 * 
 * </p>
 * <p>
 * If the parameter <i>postpone</i> is true, the establishment of connection is
 * postponed until the first remote operation to be executed. One of the main
 * purpose of this actor is, however, to make the connection and thus ask for
 * password at the beginning of the workflow. The default is false.
 * 
 * </p>
 * <p>
 * If the parameter <i>closeAtEnd</i> is true, the session will be closed at the
 * end of the workflow. If it is false, the session will be kept open. The
 * latter is good for authentication to a host secured with one-time-password,
 * so that all workflows can share the same connection. Be careful, however, as
 * the underlyting ssh package has only one session (within Kepler) to a given
 * user@host:port. If you have two workflows running at once connecting to the
 * same host, and the session is closed at the end of one of the workflows, the
 * other will likely experience a broken operation. Therefore, by default, this
 * parameter is false.
 * 
 * </p>
 * <p>
 * On the <i>failed</i> output port, the actor emits a BooleanToken indicating
 * whether the connection opening failed. It emits 'true' only if the
 * <i>postpone</i> flag is false and the connection failed. Otherwise it emits a
 * 'false' token. This can be used to throw an exception or stop the workflow
 * that cannot work without a connection, or to successively try out other
 * hosts.
 * 
 * </p>
 * <p>
 * Port forwarding is supported. The 'portforwarding' parameter should have the
 * format "-L port:host:hostport -R port:host:hostport ...". Many local and/or
 * remote forwarding specification can be given.
 * </p>
 * 
 * Reference: Ant version 1.6.2.
 * 
 * @author Norbert Podhorszki
 * @version $Revision: 32079 $
 * @category.name remote
 * @category.name connection
 * @category.name external execution
 */

public class SshSession extends TypedAtomicActor {

    /**
     * Construct an SshSession actor with the given container and name. Create
     * the parameters, initialize their values.
     * 
     * @param container
     *            The container.
     * @param name
     *            The name of this actor.
     * @exception IllegalActionException
     *                If the entity cannot be contained by the proposed
     *                container.
     * @exception NameDuplicationException
     *                If the container already has an actor with this name.
     */
    public SshSession(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        // target selects the machine where to connect to
        target = new PortParameter(this, "target", new StringToken(
                "[user@]host[:port]"));
        new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);
        target.setStringMode(true);

        paramIdentity = new FileParameter(this, "identity");
        identity = new TypedIOPort(this, "identity", true, false);
        identity.setTypeEquals(BaseType.STRING);

        paramGridCert = new FileParameter(this, "gridcert");
        gridcert = new TypedIOPort(this, "gridcert", true, false);
        gridcert.setTypeEquals(BaseType.STRING);

        paramGridProxy = new FileParameter(this, "gridproxy");
        gridproxy = new TypedIOPort(this, "gridproxy", true, false);
        gridproxy.setTypeEquals(BaseType.STRING);

        // port forwarding specifications
        portforwarding = new PortParameter(this, "portforwarding",
                new ArrayToken(BaseType.STRING));
        new Parameter(portforwarding.getPort(), "_showName", BooleanToken.FALSE);

        postpone = new Parameter(this, "postpone", new BooleanToken(false));
        postpone.setTypeEquals(BaseType.BOOLEAN);

        closeAtEnd = new Parameter(this, "closeAtEnd", new BooleanToken(false));
        closeAtEnd.setTypeEquals(BaseType.BOOLEAN);

        target_out = new TypedIOPort(this, "target_out", false, true);
        target_out.setTypeEquals(BaseType.STRING);

        failed = new TypedIOPort(this, "failed", false, true);
        failed.setTypeEquals(BaseType.BOOLEAN);

        // Set the type constraints.

        _attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
                + "width=\"75\" height=\"50\" style=\"fill:gray\"/>\n"
                + "<text x=\"5\" y=\"30\""
                + "style=\"font-size:25; fill:yellow; font-family:SansSerif\">"
                + "SSH2</text>\n" + "</svg>\n");
    }

    // //////////////// Public ports and parameters ///////////////////////

    /**
     * Target in user@host:port format. If user is not provided, the local
     * username will be used. If port is not provided, the default port 22 will
     * be applied. If target is "local" or empty string, nothing happens in this
     * actor and all commands (in other ssh actors) will be executed locally,
     * using Java Runtime.
     */
    public PortParameter target;

    /**
     * The file path for the ssh identity file if the user wants to connect
     * without having to enter the password all the time.
     * 
     * The user can browse this file as it is a parameter.
     */
    public FileParameter paramIdentity;

    /**
     * The string representation of the file path for the ssh identity file if
     * the user wants to connect without having to enter the password all the
     * time.
     * 
     * This is the input option for the identity file.
     */
    public TypedIOPort identity;

    /**
     * The file path for the Grid Certificate file if the user wants to connect
     * to an GSI-SSH server without having to enter the password all the time
     * (the passphrase for the certificate will be asked once).
     * 
     * The user can browse this file as it is a parameter.
     */
    public FileParameter paramGridCert;

    /**
     * The string representation of the file path for the Grid Certificate file if
     * the user wants to connect to an GSI-SSH server without having to enter the 
     * password all the time (the passphrase for the certificate will be asked once).
     * 
     * This is the input option for the Grid Certificate file.
     */
    public TypedIOPort gridcert;

    /**
     * The file path for the Grid Certificate Proxy file if the user wants to connect
     * to an GSI-SSH server without having to enter the password all the time.
     * 
     * The user can browse this file as it is a parameter.
     */
    public FileParameter paramGridProxy;

    /**
     * The string representation of the file path for the Grid Certificate Proxy file if
     * the user wants to connect to an GSI-SSH server without having to enter the 
     * password all the time.
     * 
     * This is the input option for the Grid Certificate Proxy file.
     */
    public TypedIOPort gridproxy;

    /**
     * Port forwarding specification. Format:
     * "-L port:host:hostport -R port:host:hostport ..." Many forwarding spec
     * can be given.
     */
    public PortParameter portforwarding;

    /**
     * String output: same as input target. It can be used for identifying the
     * created session. ... so you do not need to wire actors together even, if
     * you have the target as a parameter.
     */
    public TypedIOPort target_out;

    /**
     * Boolean output to indicate whether the connection opening failed. It is
     * true if and only if the <i>postpone</i> parameter is false and the
     * connection to the target fails.
     */
    public TypedIOPort failed;

    /**
     * Specifying whether actual connection to the host should be postponed
     * until the first usage somewhere in the workflow. If false, connection is
     * made here, which is good for password based authentications: at least you
     * know when to expect the password dialog popping up.
     */
    public Parameter postpone;

    /**
     * Specifying whether actual connection to the host should be closed when
     * the workflow terminates. If you run more than one workflow at once using
     * the same remote host, this flag should be false to avoid closing the
     * session in one workflow while the others are still using it.
     */
    public Parameter closeAtEnd;

    // /////////////////////////////////////////////////////////////////
    // // public methods ////

    /**
     * initialize() runs once before first exec
     * 
     * @exception IllegalActionException
     *                If the parent class throws it.
     */
    public void initialize() throws IllegalActionException {
        super.initialize();
        execObjectSet = new Hashtable<String,ExecInterface>();
    }

    /**
     * fire. Create a session and open the connection if not postponed.
     * 
     * @exception IllegalActionException
     *                is thrown if the session cannot be opened.
     */
    public void fire() throws IllegalActionException {
        super.fire();

        // process inputs
        target.update();
        StringToken tg = (StringToken) target.getToken();
        String strTarget = tg.stringValue();
		//back compatibility, remove the double quotes at the very beginning and at the very last.
        strTarget  = strTarget.replaceAll("^\"|\"$", "");

        portforwarding.update();
        Token t = portforwarding.getToken();
        ArrayToken pft = null;
        if (t instanceof ArrayToken)
            pft = (ArrayToken) t;
        else if (t.getType() == BaseType.STRING) {
            StringToken[] sta = { (StringToken) t };
            pft = new ArrayToken(sta);
        } else {
            pft = new ArrayToken(BaseType.STRING); // just an empty array
        }

        boolean bPostpone = ((BooleanToken) postpone.getToken()).booleanValue();
        boolean bCloseAtEnd = ((BooleanToken) closeAtEnd.getToken())
                .booleanValue();

        if (isDebugging)
            log.debug("Create session to " + strTarget + ". postpone = "
                    + bPostpone + " closeAtEnd = " + bCloseAtEnd);

        // process grid certificate files if given
        String strCert = null;
        int n = gridcert.numberOfSources();
        if (n > 0) {
            // take from input port (last will be effective)
            for (int i = 0; i < n; i++) {
                if (gridcert.hasToken(i)) {
                    strCert = ((StringToken) gridcert.get(0)).stringValue();
                    strCert = trimFileName(strCert);
                }
            }
        } else {
            // take from parameter
            strCert = ((StringToken) paramGridCert.getToken()).stringValue();
            strCert = trimFileName(strCert);
        }
        if (strCert != null && strCert.length() > 0) {
            log.debug("Use Grid Certificate: " + strCert);
            System.setProperty("X509_USER_CERT", strCert);
        }


        // process grid proxy files if given
        String strProxy = null;
        n = gridproxy.numberOfSources();
        if (n > 0) {
            // take from input port (last will be effective)
            for (int i = 0; i < n; i++) {
                if (gridproxy.hasToken(i)) {
                    strProxy = ((StringToken) gridproxy.get(0)).stringValue();
                    strProxy = trimFileName(strProxy);
                }
            }
        } else {
            // take from parameter
            strProxy = ((StringToken) paramGridProxy.getToken()).stringValue();
            strProxy = trimFileName(strProxy);
        }
        if (strProxy != null && strProxy.length() > 0) {
            log.debug("Use Grid Proxy: " + strProxy);
            System.setProperty("X509_USER_PROXY", strProxy);
        }


        // Get ExecInterface object 
        boolean connectionFailed = false;

        try {
            ExecInterface execObj = ExecFactory.getExecObject(strTarget);

            // process ssh identity files if given
            String strIdentity;
            n = identity.numberOfSources();
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    if (identity.hasToken(i)) {
                        strIdentity = ((StringToken) identity.get(0)).stringValue();
                        strIdentity = trimFileName(strIdentity);
                        if (strIdentity != null && strIdentity.length() > 0)
                            execObj.addIdentity(strIdentity);
                    }
                }
            } else {
                strIdentity = ((StringToken) paramIdentity.getToken()).stringValue();
                strIdentity = trimFileName(strIdentity);

                if (strIdentity != null && strIdentity.length() > 0)
                    execObj.addIdentity(strIdentity);
            }

            // process port forwarding specs
            for (int i = 0; i < pft.length(); i++) {
                StringToken st = (StringToken) pft.getElement(i);
                String fwd = st.stringValue().trim();
                try {
                    if (fwd.length() > 0) {
                        if (fwd.startsWith("-R")) {
                            String foo = fwd.substring(2).trim();
                            execObj.setPortForwardingR(foo);
                            if (isDebugging)
                                log.debug("Remote port forwarding: " + foo);
                        } else if (fwd.startsWith("-L")) {
                            String foo = fwd.substring(2).trim();
                            execObj.setPortForwardingL(foo);
                            if (isDebugging)
                                log.debug("Local port forwarding: " + foo);
                        } else {
                            log.error("Invalid forwarding request. Start with -L or -R : " + fwd);
                        }
                    } else {
                        // if (isDebugging)
                        // log.debug("fwd spec <empty>. skip.");
                    }

                } catch (ExecException e) {
                    log.error("Port forwarding request failed: " + e);
                    throw new ExecException("Port forwarding request failed: " + e);
                }
            }

            // if postpone is not requested, open the connection now
            // (and ask for password if needed)
            if (!bPostpone) {
                if (isDebugging)
                    log.debug("Open connection right now to " + strTarget);
                execObj.openConnection();
            }
            
            /* add ssh object to hash table for closing at end */
            if (bCloseAtEnd)
                execObjectSet.put(strTarget, execObj);
    
        } catch (ExecException e) {
        	String errorMsg = "Establishing connection to " + strTarget + " failed: " + e;
            log.error(errorMsg);
            connectionFailed = true;
            throw new IllegalActionException(this, errorMsg);
        }

        target_out.send(0, tg);
        failed.send(0, new BooleanToken(connectionFailed));

    } // end-of-method fire()

    /**
     * Close all sessions. This method is invoked exactly once per execution of
     * an application. None of the other action methods should be be invoked
     * after it.
     * 
     * @exception IllegalActionException
     *                Not thrown in this base class.
     */
    public void wrapup() throws IllegalActionException {
        if (isDebugging)
            log.debug("wrapup begin");
        super.wrapup();
        closeAll();
        if (isDebugging)
            log.debug("wrapup end");
    }

    /**
     * Close all sessions. This method is invoked exactly once per execution of
     * an application. None of the other action methods should be be invoked
     * after it.
     * 
     * @exception IllegalActionException
     *                Not thrown in this base class.
     */
    public void stop() {
        if (isDebugging)
            log.debug("stop begin");
        super.stop();
        closeAll();
        if (isDebugging)
            log.debug("stop end");
    }

    // /////////////////////////////////////////////////////////////////
    // // private methods ////

    /**
     * Close all sessions.
     */
    private void closeAll() {
        if (execObjectSet == null)
            return;
        Iterator keys = execObjectSet.keySet().iterator();
        while (keys.hasNext()) {
            String target = (String) keys.next();
            ExecInterface execObj = (ExecInterface) execObjectSet.get(target);
            execObj.closeConnection();
            if (isDebugging)
                log.debug("Closed session to " + target);
        }
        execObjectSet = null;
    }

    /**
     * Hack the "path in FileParameter" to a path string that can be handled.
     */
    private String trimFileName(String path) {
        if (path != null && path.length() > 0) {
            // Hack the path because we can't deal with "file:" or "file://"
            if (path.startsWith("file:")) {
                path = path.substring(5);

                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
            }
        }
        return path;
    }

    /**
     * Hash table for the execution objects, used only for closing their
     * sessions at the end of the workflow. key: user@host:port as String value:
     * the ExecInterface object
     * 
     */
    private Hashtable<String,ExecInterface> execObjectSet;

    private static final Log log = LogFactory
            .getLog(SshSession.class.getName());
    private static final boolean isDebugging = log.isDebugEnabled();

}

// vim: sw=4 ts=4 et
