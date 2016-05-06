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

package org.kepler.dataproxy.datasource.geon;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.DriverManager;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.ecoinformatics.seek.datasource.EcogridMetaDataCacheItem;
import org.ecoinformatics.seek.ecogrid.quicksearch.ResultTreeRoot;
import org.ecoinformatics.seek.querybuilder.DBQueryDef;
import org.geon.DatabaseQuery;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.dataproxy.datasource.DataSourceInterface;
import org.kepler.dataproxy.metadata.ADN.ADNMetadataSpecification;
import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheManager;
import org.kepler.objectmanager.cache.DataCacheObject;
import org.kepler.objectmanager.data.DataSourceControllerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ptolemy.actor.gui.style.TextStyle;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.vergil.icon.EditorIcon;
import ptolemy.vergil.kernel.attributes.EllipseAttribute;
import ptolemy.vergil.kernel.attributes.ResizablePolygonAttribute;
import ptolemy.vergil.kernel.attributes.TextAttribute;

/**
 * An actor that serves as a proxy to query a specific database (defined by geon
 * id).
 */
public class GEONDatabaseResource extends DatabaseQuery implements
		DataCacheListener, DataSourceInterface {

	private static final String SCHEMAENTITY = "//databaseMetaData/schema";
	private static final String TABLEENTITY = "//databaseMetaData/schema/table";
	private static final String VIEWENTITY = "//databaseMetaData/schema/view";
	private static final String SCHEMANAMEATT = "name";
	private static final String TABLENAMEATT = "name";
	private static final String FIELDTAG = "column";
	private static final String FIELDATT = "name";
	private static final String FIELDDATATYPE = "datatype";
	private static final String FIELDSIZE = "size";
	private static final String FIELDRESTRICTION = "nullable";

	private static final String EMAILPATH = "//sqlEngine[sqlEngineName=\"geon\"]/email";
	private static final String USERNAMEPATH = "//sqlEngine[sqlEngineName=\"geon\"]/userName";
	private static final String DRIVERPATH = "//sqlEngine[sqlEngineName=\"geon\"]/dbDriver";
	private static final String JDBCCONNECTPATH = "//sqlEngine[sqlEngineName=\"geon\"]/jdbcConnect";

	private DataSourceControllerFactory _nodeController = null;

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private DBQueryDef _queryDef = null;

	private EcogridMetaDataCacheItem _cacheMetaDataItem = null;

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/** Icon indicating the communication region. */
	protected EllipseAttribute _elli;
	protected ResizablePolygonAttribute _rect;
	protected TextAttribute _text;

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public StringAttribute _idAtt = null;

	public StringAttribute _endpointAtt = null;

	public StringAttribute _namespaceAtt = null;

	public StringAttribute _descriptionAtt = null;

	public SingletonParameter hideCon;

	public SingletonParameter _hideNS;

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.kepler.dataproxy.datasource.geon.GEONDatabaseResource");
	}

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name. s * @since
	 */
	public GEONDatabaseResource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		TextStyle queryDefTS = new TextStyle(query, "query");

		hideCon = new SingletonParameter(dbcon, "_hide");
		hideCon.setToken(BooleanToken.TRUE);

		// _sqlAttr = new StringAttribute(this, "sqlDef");
		// _sqlAttr.setVisibility(Settable.NONE);
		// TextStyle sqlDefTS = new TextStyle(_sqlAttr, "sqlDef");

		_idAtt = new StringAttribute(this, RECORDID);
		_idAtt.setVisibility(Settable.NOT_EDITABLE);

		_endpointAtt = new StringAttribute(this, ENDPOINT);
		_endpointAtt.setVisibility(Settable.NOT_EDITABLE);

		_namespaceAtt = new StringAttribute(this, NAMESPACE);
		_namespaceAtt.setVisibility(Settable.NONE);

		_descriptionAtt = new StringAttribute(this, "description");
		// _descriptionAtt.setVisibility(Settable.NOT_EDITABLE);
		TextStyle descTSatt = new TextStyle(_descriptionAtt, "descriptionTS");

		// create tableau for editting the SQL String
		// _qbTableauFactory = new QBTableauFactory(this, "_tableauFactory");

		// Create a node controller to control the context menu
		_nodeController = new DataSourceControllerFactory(this,
				"_controllerFactory");

		EditorIcon node_icon = new EditorIcon(this, "_icon");

		_rect = new ResizablePolygonAttribute(node_icon, "inner");
		// _rect.rounding.setToken("5.0");
		_rect.vertices.setToken("{-20,0, -20,40, 20,40, 20,0}");
		_rect.width.setToken("40");
		_rect.height.setToken("40");
		_rect.centered.setToken("false");
		_rect.lineColor.setToken(BLACK);

		// Create the folder
		_elli = new EllipseAttribute(node_icon, "outer");
		// _elli.
		// _poly1.vertices.setToken("{7,-27, 7,-8, 28,-10, 28,-27, 16,-27, 15,-29, 7,-29}");
		// _elli.vertices.setToken("{0,0, 8,-2, 9,1, 24,-2, 24,20, 0,24, 0,0}");
		_elli.width.setToken("40");
		_elli.height.setToken("30");
		_elli.centered.setToken("true");
		_elli.lineColor.setToken(BLACK);

		_text = new TextAttribute(node_icon, "text");
		_text.textSize.setExpression("11");

		setIconStatus(TITLE_BINARY, YELLOW);

		node_icon.setPersistent(false);
		/*
		 * if (_TypeHash.size() == 0) { for (int i = 0; i < DATATYPES.length;
		 * i++) { _TypeHash.put(DATATYPES[i], BASETYPES[i]); } }
		 */}

	/**
	 * Callback for changes in attribute values.
	 */
	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws ptolemy.kernel.util.IllegalActionException {
		// dbg.print("In attribute change method", 2);
		/*
		 * if (attribute == _sqlAttr) { if (_sqlAttr != null &&
		 * !_sqlAttr.equals("")) { String sqlXMLStr = ((Settable)
		 * _sqlAttr).getExpression(); DBQueryDef queryDef =
		 * DBQueryDefParserEmitter.parseQueryDef( _schemaDef, sqlXMLStr); String
		 * sqlStr = DBQueryDefParserEmitter.createSQL(_schemaDef, queryDef); if
		 * (sqlStr != null) { query.setToken(new StringToken(sqlStr)); } } }
		 * else if (attribute == _schemaAttr) { String schemaDef = ((Settable)
		 * _schemaAttr).getExpression();
		 * 
		 * if (schemaDef.length() > 0) { dbg.print("schemaDef >>" + schemaDef +
		 * "<<", 2); _schemaDef =
		 * DBSchemaParserEmitter.parseSchemaDef(schemaDef); } }
		 */
		if ((attribute.getName().equals(RECORDID) || attribute.getName()
				.equals(ENDPOINT))
				// && this.hasConnectionValues()
				&& !(attribute.getContainer().getContainer() instanceof ResultTreeRoot)) {
			log.debug("change recorid or endpoints");
			setIconStatus(TITLE_BUSY, RED);
			String _recordId = getRecordId();
			String _endpoint = getEndpoint();
			if (_recordId != null && !_recordId.equals("") && _endpoint != null
					&& !_endpoint.equals("")) {
				_cacheMetaDataItem = (EcogridMetaDataCacheItem) DataCacheManager
						.getCacheItem(this, "MetaData " + _recordId, _endpoint,
								EcogridMetaDataCacheItem.class.getName());
				_cacheMetaDataItem.setEndPoint(_endpoint);
				_cacheMetaDataItem.setRecordId(_recordId);
				_cacheMetaDataItem.start();
			}
		} else
			super.attributeChanged(attribute);
	}

	/**
	 * Get the database connection.
	 * 
	 * @throws IllegalActionException
	 */
	public void getConnection() throws IllegalActionException {

    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty geonSqlEngine = commonProperty
      .getProperty("sqlEngines").findProperties("sqlEngineName", "geon", true).get(0);
    
    String dbURL = geonSqlEngine.getProperty("jdbcConnect").getValue() + "/" + getRecordId();

		try {
			Class.forName(geonSqlEngine.getProperty("dbDriver").getValue()).newInstance();
			_db = DriverManager.getConnection(dbURL,
					geonSqlEngine.getProperty("email").getValue(),
          geonSqlEngine.getProperty("userName").getValue());
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalActionException(this, e, "CONNECTION FAILURE");
		}
	}

	/**
	 * Get a URL pointer to the ADN documentation for this data source.
	 * 
	 * @return URL the URL of the HTML file containing the documentation
	 */
	public URL getDocumentation() // TODO:: MODIFY THIS FUNCTION!!!
	{
		try {
			URL htmlDoc = ADNMetadataSpecification
					.getDocumentation(getRecordId());
			return htmlDoc;
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Get the endpoint of this record. The endpoint indicates where the service
	 * generating the record can be accessed.
	 * 
	 * @return endpoint the URL of the service that contains the record
	 */
	public String getEndpoint() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(ENDPOINT);
			value = attribute.getExpression();
		} catch (Exception e) {
			System.err.println("getEndpoint - ENDPOINT attr is null.");
		}
		return value;
	}

	/**
	 * Get the namespace of this record.
	 * 
	 * @return namespace the URL of the service that contains the record
	 */
	public String getNamespace() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(NAMESPACE);
			value = attribute.getExpression();
		} catch (Exception e) {
			System.err.println("getNamespace - NAMESPACE attr is null.");
		}
		return value;
	}

	/**
	 * Get the identifier of this record.
	 * 
	 * @return the String that uniquely identifies the record
	 */
	public String getRecordId() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(RECORDID);
			value = attribute.getExpression();
		} catch (Exception e) {
			System.err.println("getRecordId - RECORDID attr is null.");
		}
		return value;
	}

	/**
	 * Creates the schema definition from the cached metadata file
	 */
	private void createSchemaFromData(File cachedMetaDataFile) {
		StringBuffer schema = new StringBuffer();
		schema.append("<schema>\n");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new FileInputStream(cachedMetaDataFile);
			Document doc = builder.parse(is);

			int numTables = 0;

			// NodeList tables = XPathAPI.selectNodeList(doc, TABLEENTITY);
			NodeList schemas = XPathAPI.selectNodeList(doc, SCHEMAENTITY);
			for (int x = 0; x < schemas.getLength(); x++) {
				String schemaName = ((Element) schemas.item(x))
						.getAttribute(SCHEMANAMEATT);
				NodeList tables = schemas.item(x).getChildNodes();
				for (int i = 0; i < tables.getLength(); i++) {
					Node child = tables.item(i);
					if (!(child instanceof Element))
						continue;
					// get table name
					String tableName = ((Element) tables.item(i))
							.getAttribute(TABLENAMEATT);
					if (tableName == null || tableName.equals(""))
						continue; // table with no name
					else {
						// get fields
						if (schemaName != null && !schemaName.equals("")
								&& schemas.getLength() > 1)
							tableName = schemaName + "." + tableName;
						StringBuffer table = new StringBuffer();
						table.append("  <table name=\"" + tableName + "\">\n");
						int numFields = 0;

						NodeList fields = ((Element) tables.item(i))
								.getElementsByTagName(FIELDTAG);
						for (int j = 0; j < fields.getLength(); j++) {
							Element field = (Element) fields.item(j);
							// get field name
							String fieldName = field.getAttribute(FIELDATT);
							if (fieldName == null || fieldName.equals(""))
								continue;
							else {
								numFields++;

								// get field type
								NodeList datatypes = field
										.getElementsByTagName(FIELDDATATYPE);
								String datatype = datatypes.item(0)
										.getFirstChild().getNodeValue();
								if (datatype == null || datatype.equals(""))
									datatype = "UNKNOWN";
								NodeList fieldsizes = field
										.getElementsByTagName(FIELDSIZE);
								String size = fieldsizes.item(0)
										.getFirstChild().getNodeValue();
								if (size != null && !size.equals(""))
									datatype += "(" + size + ")";
								NodeList constraints = field
										.getElementsByTagName(FIELDRESTRICTION);
								String constraint = constraints.item(0)
										.getFirstChild().getNodeValue();
								if (constraint != null
										&& constraint.trim().toLowerCase()
												.startsWith("no"))
									datatype += "  not null";
								table.append("    <field name=\"" + fieldName
										+ "\" dataType=\"" + datatype
										+ "\"/>\n");
							}
						}
						table.append("  </table>\n");
						if (numFields > 0) {
							numTables++;
							schema.append(table.toString());
						}
					}
				}
			}
			schema.append("</schema>");
			if (numTables > 0) {
				_schemaAttr.setExpression(schema.toString());
			} else {
				setIconStatus(TITLE_ERROR, MAGENTA);
			}

		} catch (Exception ex) {
			System.out.println("Unable to populate schema: " + ex.getMessage());
		}

	}

	/**
	 * Set the text and color of the icon
	 * 
	 * @param aText
	 * @param aColor
	 */
	private void setIconStatus(String aText, String aColor) {
		try {
			_text.text.setExpression(aText);
			_elli.fillColor.setToken(aColor);
			_rect.fillColor.setToken(aColor);
		} catch (Exception e) {
		}
	}

	// ------------------------------------------------------------------------
	// -- DataCacheListener
	// ------------------------------------------------------------------------

	public void complete(DataCacheObject aItem) {
		log.debug("complete: " + this);

		aItem.removeListener(this);

		// setIconStatus(TITLE_BUSY, RED);
		if (aItem.isReady()) {

			boolean resultsetWasOK = false;
			try {
				String cachedFileName = aItem.getAbsoluteFileName();
				if (cachedFileName != null && cachedFileName.length() > 0) {
					File cachedMetaDataFile = new File(cachedFileName);
					if (cachedMetaDataFile.length() > 0) {
						createSchemaFromData(cachedMetaDataFile);
						setIconStatus(TITLE_BINARY, YELLOW);
					} else {
						setIconStatus(TITLE_ERROR, MAGENTA);
						System.err.println("File " + cachedFileName
								+ " is empty.");
					}
				} else {
					setIconStatus(TITLE_ERROR, MAGENTA);
					System.err
							.println("Cached File Name (LocalName) is null or empty!");
				}
			} catch (Exception e) {
				setIconStatus(TITLE_ERROR, MAGENTA);
			}

			// } else {
			// setIconStatus(TITLE_ERROR, MAGENTA);
		}
	}
}