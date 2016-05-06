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

package org.srb;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.IntToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.grid.io.MetaDataCondition;
import edu.sdsc.grid.io.MetaDataRecordList;
import edu.sdsc.grid.io.MetaDataSelect;
import edu.sdsc.grid.io.MetaDataSet;
import edu.sdsc.grid.io.MetaDataTable;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import edu.sdsc.grid.io.srb.SRBMetaDataSet;

//////////////////////////////////////////////////////////////////////////
//// SRBCreateQueryInterface
/**
 * <p>
 * Create an html interface for querying the SRB.
 * 
 * The following actor expects as input a reference to the SRB file system. This
 * reference connection is obtained via the SRBConnect Actor in Kepler. <i>See
 * SRBConnect and its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SCreateQueryInterface actor is able to
 * gain access to the SRB file space. Once an alive SRB file connection system
 * has been established the actor gets the attributes and number of conditions
 * as inputs and creates an HTML template for all the conditions.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, attributes
 * and number of conditions.
 * </p>
 * <p>
 * <B>Actor Output:</B> Outputs an HTML document with the appropriate conditions
 * in HTML template form. The HTML document's file content can be further viewed
 * by the BrowserUI actor. The BrowserUI outputs the XML output form of the
 * above HTML document. This XML output can be further given to the
 * SRBCreateQueryConditions actor to create an array of string conditions.
 * </p>
 * 
 * @author Efrat Jaeger
 * @version $Id: SRBCreateQueryInterface.java 13429 2007-02-01 20:18:02Z berkley
 *          $
 * @category.name srb
 * @category.name put
 */

public class SRBCreateQueryInterface extends TypedAtomicActor {

	/**
	 * Construct a constant source with the given container and name. Create the
	 * <i>value</i> parameter, initialize its value to the default value of an
	 * IntToken with value 1.
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
	public SRBCreateQueryInterface(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
    
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    //get the specific configuration we want
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty serversProperty = commonProperty.getProperty("servers.server");
    ConfigurationProperty geonProperty = serversProperty.findProperties("name", "geon").get(0);
    serverPath = geonProperty.getProperty("url").getValue();

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		attributes = new PortParameter(this, "attributes");
		attributes.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(attributes, "_showName");

		numberOfConditions = new PortParameter(this, "numberOfConditions");
		numberOfConditions.setTypeEquals(BaseType.INT);
		new Attribute(numberOfConditions, "_showName");

		html = new TypedIOPort(this, "html", false, true);
		html.setTypeEquals(BaseType.STRING);
		new Attribute(html, "_showName");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"4\" y=\"20\""
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "[SRB]</text>\n" + "<text x=\"45\" y=\"22\""
				+ "style=\"font-size:20; fill:blue; font-family:SansSerif\">"
				+ "$</text>\n" + "</svg>\n");
	}

	/**
	 * A pointer to the SRB file system.
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * HTML string.
	 */
	public TypedIOPort html;

	/**
	 * A list of attribute names for querying.
	 */
	public PortParameter attributes;

	/**
	 * The number of query conditions.
	 */
	public PortParameter numberOfConditions;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Get the physical location of SRB logical file paths.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown if the SRB file cannot be accessed or the
	 *                current directory cannot be broadcasted.
	 */
	public void fire() throws IllegalActionException {

		SRBFile srbFile;
		LocalFile localFile;
		String localFilePath;
		String _exitCode = "";

		StringBuffer sb = new StringBuffer();
		sb.append("<HTML><HEAD> <BASE TARGET = \"sub29809\"> </HEAD>" + "\n");
		sb
				.append("<body background=\"https://srb.npaci.edu/srb3.jpg\"><H2>Query MetaData</H2>"
						+ "\n");
		sb.append("<FORM METHOD=\"post\" ACTION=\"" + serverPath
				+ "pt2/jsp/pts.jsp\"  >" + "\n");
		// sb.append("<input type=hidden name=\"function\" value=\"browsequeryvalues\">");
		sb.append("<STRONG><FONT COLOR=#FF0000>File Metadata</STRONG></FONT>");
		sb.append("<table><tr><th align=center>MetaData Name</th>" + "\n");
		sb
				.append("<th></th><th align = center>MetaData Value</th></tr><tr></tr>"
						+ "\n");

		try {
			// make sure there is an alive connection.
			try {
				srbFileSystem.getHost();
			} catch (Exception ex) { // connection was closed.
				srbFileSystem = null;
				ObjectToken SRBConOT = null;
				try { // try to get a new connection in case the previous one
						// has terminated.
					SRBConOT = (ObjectToken) SRBFileSystem.get(0);
				} catch (NoTokenException ntex) {
				}
				if (SRBConOT != null) {
					srbFileSystem = (SRBFileSystem) SRBConOT.getValue();
				}
			}
			if (srbFileSystem == null) {
				throw new IllegalActionException(this,
						"No SRB connection available in actor "
								+ this.getName() + ".");
			}

			int _numCond = NUMCOND;
			numberOfConditions.update();
			if (!numberOfConditions.getExpression().trim().equals("")) {
				_numCond = ((IntToken) numberOfConditions.getToken())
						.intValue();
			}

			String[] atts = null;
			attributes.update();
			if (!attributes.getExpression().trim().equals("")) {
				Token[] attTokens = ((ArrayToken) attributes.getToken())
						.arrayValue();
				atts = new String[attTokens.length];
				for (int i = 0; i < attTokens.length; i++) {
					atts[i] = ((StringToken) attTokens[i]).stringValue();
				}
			} else {
				atts = _getAttributes(SRBMetaDataSet.DEFINABLE_METADATA_FOR_FILES);
				if (atts == null) {
					srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
					throw new IllegalActionException(this,
							"Failed to get query attributes in actor "
									+ this.getName() + ".");
				}
			}
			StringBuffer opsb = new StringBuffer();
			opsb
					.append("<OPTION SELECTED> = <OPTION value=\"not=\"> &lt;&gt; ");
			opsb
					.append("<OPTION value=\"gt\"> &gt; <OPTION value=\"num_gt\"> num &gt; <OPTION value=\"lt\"> &lt; <OPTION value=\"num_lt\"> num &lt; <OPTION value=\"ge\"> &gt;= ");
			opsb
					.append("<OPTION value=\"num_ge\"> num &gt;= <OPTION value=\"le\"> &lt;= <OPTION value=\"num_le\"> num &lt;= <OPTION> between <OPTION value=\"num_between\"> num between ");
			opsb
					.append("<OPTION value=\"not_between\"> not between <OPTION value=\"num_not_between\"> num not between <OPTION> like <OPTION value=\"not_like\"> ");
			opsb
					.append("not like <OPTION value=\"sounds_like\"> sounds like <OPTION value=\"sounds_not_like\"> sounds not like <OPTION> in <OPTION value=\"not_in\"> ");
			opsb
					.append("not in <OPTION> contains <OPTION value=\"not_contains\"> not contains </SELECT></td>"
							+ "\n");

			// create templates for all conditions (and two more..).
			for (int i = 0; i < _numCond; i++) {
				sb.append("<tr><td><SELECT SIZE=1 NAME=d_att" + i
						+ "><OPTION SELECTED> ");
				for (int j = 0; j < atts.length; j++) {
					sb.append("<OPTION> " + atts[j]);
				}
				sb.append("</SELECT></td>");
				sb.append("<td><SELECT SIZE=1 NAME=\"d_op" + i + "\">"
						+ opsb.toString());
				sb.append("<td><INPUT NAME=\"d_newmdval" + i
						+ "\", VALUE= \"\", SIZE=20,12></td></tr>" + "\n");
			}
			sb
					.append("<tr><td  align=right><input type=hidden name=\"c1\" value=\"74\">Annotation</td>");
			sb.append("<td><SELECT SIZE=1 NAME=\"AnnotationOp\">"
					+ opsb.toString());
			sb
					.append("<td><INPUT NAME=\"Annotation\", VALUE= \"\", SIZE=20,12></td></tr>"
							+ "\n");

			sb
					.append("<tr><td  align=right><input type=hidden name=\"c1\" value=\"72\">Annotator</td>");
			sb
					.append("<td align=center><input type=hidden name=\"AnnotatorOp\" value=\"=\"><B>=</B></td>");
			sb
					.append("<td><INPUT NAME=\"Annotator\", VALUE= \"kepler_dev@sdsc\", SIZE=20,12></td></tr>"
							+ "\n");

			sb
					.append("<tr><td  align=right><input type=hidden name=\"c1\" value=\"35\">Owner</td>");
			sb
					.append("<td align=center><input type=hidden name=\"OwnerOp\" value=\"=\"><B>=</B></td>");
			sb
					.append("<td><INPUT NAME=\"Owner\", VALUE= \"kepler_dev@sdsc\", SIZE=20,12></td></tr>"
							+ "\n");

			sb
					.append("<tr><td  align=right><input type=hidden name=\"c1\" value=\"2\">Data Name</td>");
			sb.append("<td><SELECT SIZE=1 NAME=\"dataNameOp\">"
					+ opsb.toString());
			sb
					.append("<td><INPUT NAME=\"dataName\", VALUE= \"\", SIZE=20,12></td></tr>"
							+ "\n");

			sb
					.append("<tr><td  align=right><input type=hidden name=\"c1\" value=\"15\">Collection Name</td>");
			sb.append("<td><SELECT SIZE=1 NAME=\"collNameOp\">"
					+ opsb.toString());
			sb
					.append("<td><INPUT NAME=\"collName\", VALUE= \"\", SIZE=20,12></td></tr>"
							+ "\n");

			sb
					.append("<tr><td  align=right><input type=hidden name=\"c1\" value=\"4\">Data Type</td>");
			sb
					.append("<td align=center><input type=hidden name=\"dataTypeOp\" value=\"=\"><B>=</B></td>");
			sb
					.append("<td><SELECT SIZE=1 NAME=dataType><OPTION SELECTED> <OPTION> AIX DLL<OPTION> AIX Executable<OPTION> AVI");
			sb
					.append("<OPTION> C code<OPTION> C include file<OPTION> Cray DLL<OPTION> Cray Executable<OPTION> DICOM header");
			sb
					.append("<OPTION> DICOM image<OPTION> DLL<OPTION> DVI format<OPTION> Document<OPTION> Excel Spread Sheet");
			sb
					.append("<OPTION> Executable<OPTION> FITS image<OPTION> LaTeX format<OPTION> MPEG<OPTION> MPEG 3 Movie");
			sb
					.append("<OPTION> MPEG Movie<OPTION> MSWord Document<OPTION> Mac DLL<OPTION> Mac Executable");
			sb
					.append("<OPTION> Mac OSX Executable<OPTION> Movie<OPTION> NSF Award Abstracts<OPTION> NT DLL");
			sb
					.append("<OPTION> NT Executable<OPTION> PDF Document<OPTION> Postscript format<OPTION> Power Point Slide");
			sb
					.append("<OPTION> Quicktime Movie<OPTION> SGI DLL<OPTION> SGI Executable<OPTION> SGML File<OPTION> SQL script");
			sb
					.append("<OPTION> Slide<OPTION> Solaris DLL<OPTION> Solaris Executable<OPTION> Spread Sheet");
			sb
					.append("<OPTION> Troff format<OPTION> URL<OPTION> Wave Audio<OPTION> Word format<OPTION> ascii compressed Huffman");
			sb
					.append("<OPTION> ascii compressed Lempel-Ziv<OPTION> ascii text<OPTION> audio streams<OPTION> binary file");
			sb
					.append("<OPTION> compressed PDB file<OPTION> compressed file<OPTION> compressed mmCIF file");
			sb
					.append("<OPTION> compressed tar file<OPTION> data file<OPTION> database<OPTION> database shadow object");
			sb
					.append("<OPTION> datascope data<OPTION> deleted<OPTION> directory shadow object<OPTION> ebcdic compressed Huffman");
			sb
					.append("<OPTION> ebcdic compressed Lempel-Ziv<OPTION> ebcdic text<OPTION> email<OPTION> fig image");
			sb
					.append("<OPTION> fortran code<OPTION> generic<OPTION> gif image<OPTION> home<OPTION> html<OPTION> image");
			sb
					.append("<OPTION> java code<OPTION> jpeg image<OPTION> level1<OPTION> level2<OPTION> level3<OPTION> level4");
			sb
					.append("<OPTION> library code<OPTION> link code<OPTION> object code<OPTION> orb data<OPTION> pbm image");
			sb
					.append("<OPTION> perl script<OPTION> print-format<OPTION> printout<OPTION> program code<OPTION> realAudio");
			sb
					.append("<OPTION> realVideo<OPTION> shadow object<OPTION> streams<OPTION> tar file<OPTION> tcl script<OPTION> text");
			sb
					.append("<OPTION> tiff image<OPTION> uuencoded tiff<OPTION> video streams<OPTION> xml</SELECT></td></tr>"
							+ "\n");
			sb.append("</table><br/>");

			if (attributes.getExpression().trim().equals("")) {
				atts = _getAttributes(SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES);
				System.out.println("atts for dirs");
			}

			sb
					.append("<STRONG><FONT COLOR=#FF0000>Collection Metadata</STRONG></FONT>");
			sb.append("<table><tr><th align=center>MetaData Name</th>" + "\n");
			sb
					.append("<th></th><th align = center>MetaData Value</th></tr><tr></tr>"
							+ "\n");

			for (int i = 0; i < _numCond; i++) {
				sb.append("<tr><td><SELECT SIZE=1 NAME=c_att" + i
						+ "><OPTION SELECTED> ");
				for (int j = 0; j < atts.length; j++) {
					sb.append("<OPTION> " + atts[j]);
				}
				sb.append("</SELECT></td>");
				sb.append("<td><SELECT SIZE=1 NAME=\"c_op" + i + "\">"
						+ opsb.toString());
				sb.append("<td><INPUT NAME=\"c_newmdval" + i
						+ "\", VALUE= \"\", SIZE=20,12></td></tr>" + "\n");
			}
			sb.append("</table><br/>");

			sb.append("<INPUT TYPE=\"submit\" VALUE=\"Find\">" + "\n");
			// sb.append("<INPUT TYPE=\"reset\" VALUE=\"Clear\">"); // ADD!!!
			sb.append("</FORM> \n </body></html>");

			System.out.println(sb.toString());
			html.broadcast(new StringToken(sb.toString()));
		} catch (Exception ex) {
			srbFile = null;
			srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
			ex.printStackTrace();
			throw new IllegalActionException(this, "Exception in actor "
					+ this.getName() + ": " + ex.getMessage() + ".");
		}
	}

	/**
	 * Initialize the srb file system to null.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		srbFileSystem = null;
	}

	/**
	 * Disconnect from SRB.
	 */
	public void wrapup() {
		srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Querying the SRB metadata from the head to get all attributes.
	 * 
	 * @param type
	 * 	 */
	private String[] _getAttributes(String type) {
		/*
		 * String hd = srbFileSystem.getHomeDirectory(); SRBFile homeDir = new
		 * SRBFile(srbFileSystem, hd);
		 * 
		 * if (homeDir.isDirectory()) {
		 * System.out.println("homeDir is a directory"); }
		 * 
		 * SRBFile srbFile = new SRBFile(srbFileSystem, ".");
		 */
		String[] attsArr = null;
		try {
			// All user files (recursive).
			MetaDataCondition conditions[] = { MetaDataSet.newCondition(
					SRBMetaDataSet.USER_GROUP_NAME, MetaDataCondition.LIKE,
					srbFileSystem.getUserName()), };

			// The return fields.
			String[] selectFieldNames = { type };
			MetaDataSelect selects[] = MetaDataSet
					.newSelection(selectFieldNames);

			// querying mcat
			MetaDataRecordList[] rl = srbFileSystem.query(conditions, selects);
			Set atts = new TreeSet();
			for (int i = 0; i < rl.length; i++) {
				// getting the metadata field index, if exists.
				int ind = rl[i].getFieldIndex(type);
				if (ind != -1) {
					int dirInd = rl[i]
							.getFieldIndex(SRBMetaDataSet.DIRECTORY_NAME);
					if (dirInd != -1) {
						String dirName = rl[i].getStringValue(dirInd);
						// ignore files that have been deleted.
						int trashInd = dirName.toLowerCase().indexOf("/trash/");
						if (trashInd == -1) {
							MetaDataTable mdt = rl[i].getTableValue(ind);
							int rc = mdt.getRowCount();
							for (int j = 0; j < rc; j++) {
								System.out.println(mdt.getStringValue(j, 0));
								atts.add(mdt.getStringValue(j, 0));
							}
							System.out.println("here");
						}
					}
				}
			}
			attsArr = new String[atts.size()];
			Iterator it = atts.iterator();
			int i = 0;
			while (it.hasNext()) {
				attsArr[i++] = (String) it.next();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return attsArr;
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * A reference to the SRB file system
	 */
	private SRBFileSystem srbFileSystem = null;

	/**
	 * A static number of condition
	 */
	private static final int NUMCOND = 5;

	/**
	 * Path to the geon server url in the config file.
	 */
	private static final String SERVERPATH = "//servers/server[@name=\"geon\"]/url";

	/**
	 * URL to backend server
	 */
	private String serverPath;

}