/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jones $'
 * '$Date: 2014-02-06 15:25:50 -0800 (Thu, 06 Feb 2014) $' 
 * '$Revision: 32582 $'
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

package org.ecoinformatics.seek.datasource.eml.eml2;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.client.IdentifierServiceClient;
import org.ecoinformatics.seek.dataquery.DBTableNameResolver;
import org.ecoinformatics.seek.dataquery.DBTablesGenerator;
import org.ecoinformatics.seek.dataquery.HsqlDataQueryAction;
import org.ecoinformatics.seek.datasource.DataSourceIcon;
import org.ecoinformatics.seek.datasource.EcogridDataCacheItem;
import org.ecoinformatics.seek.datasource.EcogridGZippedDataCacheItem;
import org.ecoinformatics.seek.datasource.EcogridMetaDataCacheItem;
import org.ecoinformatics.seek.datasource.EcogridTarArchivedDataCacheItem;
import org.ecoinformatics.seek.datasource.EcogridZippedDataCacheItem;
import org.ecoinformatics.seek.ecogrid.EcoGridService;
import org.ecoinformatics.seek.ecogrid.EcoGridServicesController;
import org.ecoinformatics.seek.ecogrid.quicksearch.ResultRecord;
import org.ecoinformatics.seek.ecogrid.quicksearch.ResultTreeRoot;
import org.ecoinformatics.seek.querybuilder.DBQueryDef;
import org.ecoinformatics.seek.querybuilder.DBQueryDefParserEmitter;
import org.ecoinformatics.seek.querybuilder.DBSchemaParserEmitter;
import org.kepler.actor.preview.Previewable;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheManager;
import org.kepler.objectmanager.cache.DataCacheObject;
import org.kepler.objectmanager.data.DataSourceControllerFactory;
import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.db.DSSchemaDef;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.Entity;
import org.kepler.objectmanager.data.db.QBTableauFactory;
import org.kepler.objectmanager.data.text.TextComplexFormatDataReader;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.DelimitedReader;
import org.kepler.util.DotKeplerManager;
import org.xml.sax.InputSource;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.style.TextStyle;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.CancelException;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;
import util.PersistentTableModel;
import util.PersistentTableModelWindowListener;
import util.PersistentVector;
import util.StaticUtil;
import util.TableSorter;
import EDU.oswego.cs.dl.util.concurrent.CountDown;
import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * <p>
 * The Eml200DataSource is used to gain access to a wide variety of data
 * packages that have been described using Ecological Metadata Language (EML).
 * Each data package contains an EML metadata description and one or more data
 * entities (data tables, spatial raster images, spatial vector images). The
 * data packages can be accessed from the local filesystem or through any
 * EcoGrid server that provides access to its collection of data objects.
 * </p>
 * <p>
 * The metadata provided by the EML description of the data allows the data to
 * be easily ingested into Kepler and exposed for use in downstream components.
 * The Eml200DataSource handles all of the mechanical issues associated with
 * parsing the metadata, downloading the data from remote servers if applicable,
 * understanding the logical structure of the data, and emitting the data for
 * downstream use when required. The supported data transfer protocols include
 * http, ftp, file, ecogrid and srb.
 * </p>
 * <p>
 * After parsing the EML metadata, the actor automatically reconfigures its
 * exposed ports to provide one port for each attribute in the first entity that
 * is described in the EML description. For example, if the first entity is a
 * data table with four columns, the ports might be "Site", "Date", "Plot", and
 * "Rainfall" if that's what the data set contained. These details are obtained
 * from the EML document.
 * </p>
 * <p>
 * By default, the ports created by the EML200DataSource represent fields in the
 * data entities, and one tuple of data is emitted on these ports during each
 * fire cycle. Alternatively, the actor can be configured to so that the ports
 * instead represent an array of values for a field ("AsColumnVector"), or so
 * that the ports represent an entire table of data ("AsTable") formatted in
 * comma-separated-value (CSV) format.
 * </p>
 * <p>
 * If more than one data entity is described in the EML metadata, then the
 * output of the actor defaults to the first entity listed in the EML. To select
 * the other entities, one must provide a query statement that describes the
 * filter and join that should be used to produce the data to be output. This is
 * accomplished by selecting 'Open actor', which shows the Query configuration
 * dialog, which can be used to select the columns to be output and any
 * filtering constraints to be applied.
 * </p>
 * 
 * @author Matt Jones, Jing Tao, Chad Berkley
 * @since kepler-1.0.0
 * @Pt.ProposedRating Red (jones)
 * @Pt.AcceptedRating Red (jones)
 */
public class Eml200DataSource extends ResultRecord implements
		DataCacheListener, Previewable {

	/*
	 * Brief discussion of threads:
	 * 
	 * The actor startup will call attributeChanged multiple time to configure
	 * the object. When the recordId and endpoint attributes are set, the object
	 * will attempt to load the EcoGridMetaDataCacheItem. This operation will
	 * fork a thread named "MetaData XYZZY".
	 * 
	 * The MetaData thread is configured to use the inner class MetadataComplete
	 * listener when it completes. The thread, in this listener, initializes the
	 * CountDown _entityCountDown and kicks off threads to load each entity. The
	 * thread then waits using _entityCountDown.acquire() until all the data
	 * entity thread are complete.
	 * 
	 * Each data entity is loaded using a class derived from
	 * EcogridDataCacheItem. The specific type of class is determined from ...
	 */

	// /////////////////////////////////////////////////////////////////
	// // private variables ////
	static final String DATATYPES[] = { DataType.INT, DataType.FLOAT,
			DataType.DOUBLE, DataType.LONG, DataType.STR };

	static final BaseType BASETYPES[] = { BaseType.INT, BaseType.DOUBLE,
			BaseType.DOUBLE, BaseType.LONG, BaseType.STRING };

	static private Hashtable _TypeHash = new Hashtable();

	// for looking using the default documentation from the actor lib
	private static KeplerDocumentationAttribute defaultDocumentation = null;

	static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.eml.eml2.Eml200DataSource");
	}

	private QBTableauFactory _qbTableauFactory = null;

	private DataSourceControllerFactory _nodeController = null;

	/**
	 * Output indicator parameter.
	 */
	private Eml200DataOutputFormatBase _dataOutputFormat = null;

	private Vector<Entity> _entityList = new Vector<Entity>();
	private Entity _selectedTableEntity = null;

	private Vector _columns = null;

	private DBQueryDef _queryDef = null;

	private DSSchemaIFace _schemaDef = null;
	private DSSchemaDef _schema = new DSSchemaDef();

	private boolean _ignoreSchemaChange = false;

	private boolean _schemaDefinitionIsSet = false;

	private DBTablesGenerator _tableGenerator = null;

	private EcogridDataCacheItem _selectedCachedDataItem = null;

	private EcogridMetaDataCacheItem _cacheMetaDataItem = null;

	/**
	 * _metadataCompleted is a Latch object (from oswego) which is used to
	 * synchronize the start of the workflow. The Latch is released when the
	 * _cacheMetaDataItem has been completed. It is also used to notify a thread
	 * blocked in initialized() when the stop method is executed.
	 */
	private Latch _metadataCompleted = new Latch();

	private InputStream _reader = null;

	private HsqlDataQueryAction _resultSet = null;

	/*
	 * Indicates when there is no more data in the _resultSet to output.
	 */
	private boolean _endOfResultSet = false;

	private DelimitedReader _simpleDelimitedReader = null;

	private TextComplexFormatDataReader _complexFormatReader = null;

	private CountDown _entityCountDown = null;
	private int _numberOfEntities = 0;

	private int _numberOfFailedDownloadEntities = 0;

	private boolean _hasSQLCommand = false;

	private String[] _selectedColumnLabelList = null;

	private Type[] _selectedColumnTypeList = null;

	private static final int INDEXFORNOTFOUND = -1;

	private static final int DEFAULTINDEX = 0;

	private DataSourceIcon _icon;

	private String emlFile = null;
	
	private String emlFileFinalPath = null;

	private Vector failedDownloadEntityName = new Vector();

	/**
	 * Tracks if user has been asked about newer version of eml before (Kind of
	 * a hack to get around mysterious multiple attributeChanged() calls)
	 */
	private int checkVersionPromptCount = 0;

	/**
	 * The default endpoint for EcoGrid messages, which is overridden in the
	 * configuration file.
	 */
	private static final String ENDPOINT = "http://ecogrid.ecoinformatics.org/knb/services/QueryService";

	private static final String HTMLEXTENSION = ".html";

	public static final Settable.Visibility DEFAULTFORSQL = Settable.EXPERT;

	public static final Settable.Visibility DEFAULTFORSCHEMA = Settable.EXPERT;

	private static final boolean SHOWGUIERROR = true;

	// private boolean isDoneParseEmlFile = false;

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The file path for locating an EML file that is available from a local
	 * file.
	 */
	public FileParameter emlFilePath = null;

	/**
	 * The file path for locating a data file that is available from the local
	 * file system
	 */
	public FileParameter dataFilePath = null;

	/**
	 * The SQL command which will be applied to the data entity to filter data
	 * values. This is usually generated using the Query configuration dialog.
	 */
	public StringAttribute sqlDef = null;

	/**
	 * Schema definition for the entities in this package. The schema definition
	 * is obtained automatically by parsing the EMl document and does not need
	 * to be edited by the end user.
	 */
	public StringAttribute schemaDef = null;

	/**
	 * The format of the output to be produced for the data entity. This
	 * parameter controls which ports are created for the actor and what data is
	 * emitted on those ports during each fire cycle. For example, this field
	 * can be configured to produce one port for each column in a data table, or
	 * one port that emits the entire data table at once in CSV format.
	 * Specifically, the output format choices are:
	 * <p>
	 * As Field: This is the default. One output port is created for each field
	 * (aka column/attribute/variable) that is described in the EML metadata for
	 * the data entity. If the SQL statement has been used to subset the data,
	 * then only those fields selected in the SQL statement will be configured
	 * as ports.
	 * </p>
	 * <p>
	 * As Table: The selected entity will be sent out as a string which contains
	 * the entire entity data. It has three output ports: DataTable - the data
	 * itself, Delimiter - delimiter to seperate fields, and NumColumns - the
	 * number of fields in the table.
	 * </p>
	 * <p>
	 * As Row: In this output format, one tuple of selected data is formatted as
	 * an array and sent out. It only has one output port (DataRow) and the data
	 * type is a record containing each of the individuals fields.
	 * </p>
	 * <p>
	 * As Byte Array: Selected data will be sent out as an array of bytes which
	 * are read from the data file. This is the raw data being sent in binary
	 * format. It has two output ports: BinaryData - contains data itself, and
	 * EndOfStream - a tag to indicate if it is end of data stream.
	 * </p>
	 * <p>
	 * As UnCompressed File Name: This format is only used when the entity is a
	 * compressed file (zip, tar et al). The compressed archive file is
	 * uncompressed after it is downloaded. It has only one output port which
	 * will contain an array of the filenames of all of the uncompressed files
	 * from the archive. If the parameter "Target File Extension in Compressed
	 * File" is provided, then instead the array that is returned will only
	 * contain the files with the file extension provided.
	 * </p>
	 * <p>
	 * As Cache File Name: Kepler stores downloaded data files from remote sites
	 * into its cache system. This output format will send the local cache file
	 * path for the entity so that workflow designers can directly access the
	 * cache files. It has two output ports: CacheLocalFileName - the local file
	 * path, and CacheResourceName - the data link in eml for this enity.
	 * </p>
	 * <p>
	 * As Column Vector: This output format is similar to "As Field". The
	 * difference is instead sending out a single value on each port, it sends
	 * out an array of all of the data for that field. The type of each port is
	 * an array of the base type for the field.
	 * </p>
	 * <p>
	 * As ColumnBased Record: This output format will send all data on one port
	 * using a Record structure that encapsulates the entire data object. The
	 * Record will contain one array for each of the fields in the data, and the
	 * type of that array will be determined by the type of the field it
	 * represents.
	 * </p>
	 */
	public StringParameter dataOutputFormat = null;

	/**
	 * This parameter specifies a file extension that is used to limit the array
	 * of filenames returned by the data source actor when "As unCompressed File
	 * Name" is selected as the ouput type. Please see more information in
	 * "As Uncompressed File Name" in the description of the output format
	 * parameter.
	 */
	public StringParameter fileExtensionFilter = null;

	/**
	 * This parameter determines if extra data columns that are NOT described in
	 * the EML should be ignored (isLenient=true) or if an error should be
	 * raised when the data and EML description do not match (isLenient=false)
	 * TRUE - extra data columns are ignored FALSE - an error is raised when
	 * data and metadata conflict
	 */
	public Parameter isLenient = null;

	/**
	 * This parameter determines if remote source should be queried for latest
	 * revision of metadata file. TRUE - check performed FALSE - do not check
	 * for latest version
	 */
	public Parameter checkVersion = null;

	/**
	 * If this EML package has mutiple entities, this parameter specifies which
	 * entity should be used for output. By default when this parameter is
	 * unset, data from the first entity described in an EML package is output.
	 * This parameter is only used if the SQL parameter is not used, or if the
	 * SQL parameter is used and the output format is one of "As Table",
	 * "As Byte Array", "As Uncompressed File Name", and "As Cache File Name".
	 */
	public StringParameter selectedEntity = null;

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
	 *                If the container already has an actor with this name.
	 * @since
	 */
	public Eml200DataSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		_icon = new DataSourceIcon(this);

		emlFilePath = new FileParameter(this, "emlFilePath");
		emlFilePath.setDisplayName("EML File");

		dataFilePath = new FileParameter(this, "dataFilePath");
		dataFilePath.setDisplayName("Data File");

		schemaDef = new StringAttribute(this, "schemaDef");
		TextStyle schemaDefTS = new TextStyle(schemaDef, "schemaDef");
		schemaDef.setDisplayName("Schema Definition");
		schemaDef.setVisibility(DEFAULTFORSCHEMA);

		sqlDef = new StringAttribute(this, "sqlDef");
		TextStyle sqlDefTS = new TextStyle(sqlDef, "sqlDef");
		sqlDef.setDisplayName("SQL Command");
		sqlDef.setVisibility(DEFAULTFORSQL);
		selectedEntity = new StringParameter(this, "selectedEntity");
		selectedEntity.setDisplayName("Selected Entity");
		dataOutputFormat = new StringParameter(this, "dataOutputFormat");
		dataOutputFormat.setDisplayName("Data Output Format");
		dataOutputFormat.setExpression(Eml200DataOutputFormatFactory._AsField);
		dataOutputFormat.addChoice(Eml200DataOutputFormatFactory._AsField);
		dataOutputFormat.addChoice(Eml200DataOutputFormatFactory._AsTable);
		dataOutputFormat.addChoice(Eml200DataOutputFormatFactory._AsRow);
		dataOutputFormat.addChoice(Eml200DataOutputFormatFactory._AsByteArray);
		dataOutputFormat
				.addChoice(Eml200DataOutputFormatFactory._AsUnzippedFileName);
		dataOutputFormat.addChoice(Eml200DataOutputFormatFactory._AsFileName);
		dataOutputFormat.addChoice(Eml200DataOutputFormatFactory._AsAllFileNames);
		dataOutputFormat
				.addChoice(Eml200DataOutputFormatFactory._AsColumnVector);
		dataOutputFormat
				.addChoice(Eml200DataOutputFormatFactory._AsColumnRecord);
		_dataOutputFormat = Eml200DataOutputFormatFactory.newInstance(this);

		fileExtensionFilter = new StringParameter(this, "fileExtensionFilter");
		fileExtensionFilter.setDisplayName("File Extension Filter");

		isLenient = new Parameter(this, "isLenient");
		isLenient.setDisplayName("Allow lenient data parsing");
		isLenient.setTypeEquals(BaseType.BOOLEAN);
		isLenient.setToken(BooleanToken.FALSE);

		checkVersion = new Parameter(this, "checkVersion");
		checkVersion.setDisplayName("Check for latest version");
		checkVersion.setTypeEquals(BaseType.BOOLEAN);
		checkVersion.setToken(BooleanToken.FALSE);

		// create tableau for editting the SQL String
		_qbTableauFactory = new QBTableauFactory(this, "_tableauFactory");

		// Create a node controller to control the context menu
		_nodeController = new DataSourceControllerFactory(this,
				"_controllerFactory");

		if (_TypeHash.size() == 0) {
			for (int i = 0; i < DATATYPES.length; i++) {
				_TypeHash.put(DATATYPES[i], BASETYPES[i]);
			}
		}

	}

	/**
	 * Accessor to _columns member. Default permissions for use by
	 * Eml200DataOutputFormatBase derived classes only.
	 * 
	 * 	 */
	public Vector getColumns() {
		return _columns;
	}

	/**
	 * @return Returns the _selectedTableEntity.
	 */
	Entity getSelectedTableEntity() {
		return _selectedTableEntity;
	}
	
	public Vector<Entity> getEntityList() {
		return _entityList;
	}

	/**
	 * @return Returns the _selectedCachedDataItem.
	 */
	EcogridDataCacheItem getSelectedCachedDataItem() {
		return _selectedCachedDataItem;
	}

	/**
	 * @return Returns the fileExtensionFilter.
	 */
	String getFileExtensionInZip() {
		try {
			return fileExtensionFilter.stringValue();
		} catch (IllegalActionException e) {
			return "";
		}
	}

	/**
	 * @return Returns the _selectedColumnLabelList.
	 */
	String[] getColumnLabels() {
		return _selectedColumnLabelList;
	}

	/**
	 * @return Returns the _selectedColumnTypeList.
	 */
	Type[] getColumnTypes() {
		return _selectedColumnTypeList;
	}

	public void preinitialize() throws IllegalActionException {

		// check for latest
		this.checkForMostRecentRecordId(false);
		
		// First block for metadata download to finish.
		try {
			_metadataCompleted.acquire();
			log.debug("Is stop requested? " + getDirector().isStopRequested());
		} catch (InterruptedException e) {
			log.debug("Is stop requested? " + getDirector().isStopRequested());
			if (getDirector().isStopRequested()) {
				throw new IllegalActionException("Execution interrupted");
			}
		}

		super.preinitialize();
	}

	/**
	 * Initialize the actor prior to running in the workflow. This reads the
	 * metadata and configures the ports.
	 * 
	 * @throws IllegalActionException
	 */
	public void initialize() throws IllegalActionException {
		log.debug("In initialize method");

		// Now block waiting for the entity data to finish.
		try {
			synchronized (_entityCountDown) {
				while (_entityCountDown.currentCount() > 0
						&& (getDirector() != null && !getDirector()
								.isStopRequested())) {
					_entityCountDown.wait();
					log.debug("Is stop requested? "
							+ getDirector().isStopRequested());
				}
			}
		} catch (InterruptedException e) {
			throw new IllegalActionException("Downloads not completed");
		} catch (Exception e) {
			throw new IllegalActionException("Download error encountered");
		}

		if (getDirector() != null && getDirector().isStopRequested()) {
			throw new IllegalActionException("Execution interrupted");
		}

		if (_selectedTableEntity == null) {
			throw new IllegalActionException("_selectedTableEnity is NULL!");
		}

		if (_selectedCachedDataItem == null) {
			_selectedCachedDataItem = (EcogridDataCacheItem) _selectedTableEntity
					.getDataCacheObject();
		}

		if (_selectedCachedDataItem == null) {
			throw new IllegalActionException(
					" The selected entity has a null data (Maybe data download failed)");
		}

		// This was the initializeAsTableRowOrField method
		String sqlStr = "";
		String sqlXMLStr = ((Settable) sqlDef).getExpression();
		if (sqlXMLStr == null || sqlXMLStr.length() == 0) {
			sqlStr = "SELECT * FROM "
					+ (_selectedTableEntity.getMappedName() != null ? _selectedTableEntity
							.getMappedName()
							: _selectedTableEntity.getName());

		} else {
			Hashtable mappedNameHash = new Hashtable();
			// should go through all enities
			int size = _entityList.size();
			if (size == 0) {
				// no entity in this package and throw an exception
				throw new IllegalActionException(
						"There is no downloadable entity or no entity in this eml package");
			}
			for (int i = 0; i < size; i++) {
				Entity entity = (Entity) _entityList.elementAt(i);
				if (entity.getMappedName() != null) {
					mappedNameHash
							.put(entity.getName(), entity.getMappedName());
				}
			}
			DBQueryDef queryDef = DBQueryDefParserEmitter.parseQueryDef(
					_schemaDef, sqlXMLStr, mappedNameHash);

			sqlStr = DBQueryDefParserEmitter.createSQL(_schemaDef, queryDef);
		}

		log.debug("The sql command is " + sqlStr);
		// excuted query
		if (sqlStr != null && !sqlStr.trim().equals("")) {
			// if table gnerated successfully, we will run query
			if (_tableGenerator != null && _tableGenerator.getSuccessStatus()) {
				try {
					_icon.setBusy();
					_resultSet = new HsqlDataQueryAction();
					_resultSet.setSQL(sqlStr);
					_resultSet.actionPerformed(null);

				} catch (Exception e) {
					log.debug("Error to run query is ", e);
					throw new IllegalActionException(e.getMessage());
				}

			}
		}
		// if reustlset is null(this can be caused by db couldn't create table)
		// and we don't have any sql command
		// (this means we only have one data entity involve) or only has one
		// entity at all
		// we would like to try read the selected entity data from datachache
		// rather than
		// from db
		if (_resultSet == null && (!_hasSQLCommand || _numberOfEntities == 1)) {
			// System.out.println("in result set is null!!!!!!!!!!!!!!!!!!");
			_reader = _selectedCachedDataItem.getDataInputStream();
			try {
				createDelimitedReader();
			} catch (Exception e) {
				log.debug("Error to run delimiter reader is  ", e);
				throw new IllegalActionException(e.getMessage());
			}

		}
		_icon.setReady();
		// This was the initializeAsTableRowOrField method

		// Set marker to say we have data. This might not be true though and is
		// perhaps
		// a bug. The correct thing to do, is check for data in the prefire and
		// in the
		// postfire.
		_endOfResultSet = false;

		_dataOutputFormat.initialize();

	}

	/**
	 * This method will read a row vector from data source, either from
	 * resultset which excuted by data query or delimiterdReader which reader
	 * from data inputtream - _reader. This method will be called in asFired
	 * method
	 */
	public Vector gotRowVectorFromSource() throws Exception {
		Vector rowVector = new Vector();
		if (_resultSet != null) {

			ResultSet rs = _resultSet.getResultSet();
			ResultSetMetaData metadata = rs.getMetaData();
			int columnSize = metadata.getColumnCount();

			if (rs.next()) {
				for (int i = 0; i < columnSize; i++) {
					String str = rs.getString(i + 1);
					rowVector.add(str);
				}
			}
		} else if (_reader != null
				&& (!_hasSQLCommand || _numberOfEntities == 1)) {
			if (_selectedTableEntity.isSimpleDelimited()) {
				_simpleDelimitedReader
						.setCollapseDelimiter(_selectedTableEntity
								.getCollapseDelimiter());
				_simpleDelimitedReader.setNumFooterLines(_selectedTableEntity
						.getNumFooterLines());
				rowVector = _simpleDelimitedReader.getRowDataVectorFromStream();
			} else {
				rowVector = _complexFormatReader.getRowDataVectorFromStream();
			}

		}

		if (rowVector.isEmpty()) {
			_endOfResultSet = true;
		}
		return rowVector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ptolemy.actor.lib.Source#prefire()
	 */
	public boolean prefire() throws IllegalActionException {
		return _dataOutputFormat.prefire();
	}

	/**
	 * Send a record's tokens over the ports on each fire event.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		// log.debug("In fire method");
		super.fire();
		_dataOutputFormat.fire();
	}

	/**
	 * This method is only for output as byte array. Read the next bytes from
	 * the input stream into array. Fire method will send the array out. If
	 * there reached EOF , return false. Otherwise, return whatever the
	 * superclass returns.
	 * 
	 * @exception IllegalActionException
	 *                If there is a problem reading the file.
	 */
	public boolean postfire() throws IllegalActionException {

		if (!_dataOutputFormat.postfire()) {
			return false;
		}

		try {
			if (_resultSet != null && _resultSet.getResultSet().isAfterLast()) {
				return false;
			}
		} catch (SQLException e) {
			throw new IllegalActionException(this, e,
					"Unable to determine end of result set");
		}

		if (_endOfResultSet) {
			return false;
		}

		return super.postfire();

	}

	void initializePort(String aPortName, Type aPortType)
			throws IllegalActionException {
		try {
			String columnName = aPortName.trim();
			// Create a new port for each Column in the resultset
			TypedIOPort port = (TypedIOPort) this.getPort(columnName);
			boolean aIsNew = (port == null);
			if (aIsNew) {
				// Create a new typed port and add it to this container
				port = new TypedIOPort(this, columnName, false, true);
				log.debug("Creating port [" + columnName + "]" + this);
			}
			port.setTypeEquals(aPortType);

		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			throw new IllegalActionException(
					"One or more attributes has the same name.  Please correct this and try again.");
		}

	}

	/*
	 * Remove all ports which's name is not in the selected vector
	 */
	void removeOtherOutputPorts(Collection nonRemovePortName)
			throws IllegalActionException {
		// Use toArray() to make a deep copy of this.portList().
		// Do this to prevent ConcurrentModificationExceptions.
		TypedIOPort[] l = new TypedIOPort[0];
		l = (TypedIOPort[]) this.portList().toArray(l);

		for (int i = 0; i < l.length; i++) {
			TypedIOPort port = l[i];
			if (port == null || port.isInput()) {
				continue;
			}
			String currPortName = port.getName();
			if (!nonRemovePortName.contains(currPortName)) {
				try {
					port.setContainer(null);
				} catch (Exception ex) {
					throw new IllegalActionException(this,
							"Error removing port: " + currPortName);
				}
			}
		}
	}

	/**
	 * Issue a ChangeRequest to change the output ports.
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	private void reconfigurePorts(String why) {
		log.debug("Creating reconfigure ports change request " + why);
		this.requestChange(new ChangeRequest(this, why) {
			public void _execute() throws Exception {
				log.debug("Executing reconfigure ports change request "
						+ this.getDescription());
				_dataOutputFormat.reconfigurePorts();
			}
		});
	}

	/**
	 * Callback for changes in attribute values.
	 */
	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws ptolemy.kernel.util.IllegalActionException {
		log.debug("In attribute change method");
		// System.out.println("In attribute change method!!!!!!!!!!!!!!!");
		if (attribute == emlFilePath || attribute == dataFilePath) {
			log.debug("Processing new EML or data file path...");
			try {
				String url = ((Settable) emlFilePath).getExpression();
				log.debug("EML File Path is: " + emlFilePath);

				String dataurl = dataFilePath.getExpression();
				if (dataurl == null || dataurl.trim().equals("")) {
					log.debug("Data file is null so returning...");
					return;
				}

				if (url == null || url.trim().equals("")) {
					log.debug("URL is null so returning...");
					return;
				}

				if (emlFile != null && emlFile.equals(url)) {
					log.debug("EML File is null so returning...");
					return;
				}
				emlFile = url;
				String endpoint = null;
				if (!url.startsWith("http://") 
						&& !url.startsWith("https://")
						&& !url.startsWith("file:///")
						&& !url.startsWith("ftp://")
						&& !url.startsWith("ecogrid://")
						&& !url.startsWith("srb://")) {
					log.debug("In url mangling block");
					if (emlFilePath == null) {
						return;
					}
					File emlFileName = emlFilePath.asFile();
					//System.out.println("the name of eml file name" +emlFileName.getName());
					setRecordId(emlFileName.getName());
					if (emlFileName == null) {
						return;
					}
					url = emlFileName.getPath();
					// System.out.println("the url from getpath is "+url);
					// it is file path we need add file protocal
					if (url.startsWith("file:/") && !url.startsWith("file://")
							&& !url.startsWith("file:///")) {

						// somehow windows url will look like "file:/C:
						url = url.replaceFirst("file:/", "file:///");

					} else if (url.startsWith("file://")
							&& !url.startsWith("file:///")) {

						url = url.replaceFirst("file://", "file:///");
					} else if (!url.startsWith("file:///")) {
						// it is file path we need add file protocal
						url = "file:///" + url;
					}
				}
				emlFileFinalPath = url;
				log.debug("Final EML url is: " + emlFileFinalPath);

				_icon.setBusy();
				clearTableRelatedParameter(true);
				_cacheMetaDataItem = (EcogridMetaDataCacheItem) DataCacheManager
						.getCacheItem(new MetadataComplete(),
								"MetaData " + emlFileFinalPath, endpoint,
								EcogridMetaDataCacheItem.class.getName());
				if (_cacheMetaDataItem.isEmpty()) {
					_cacheMetaDataItem.setEndPoint(endpoint);
					_cacheMetaDataItem.setRecordId(emlFileFinalPath);
					_cacheMetaDataItem.start();
				} else {
					log.debug("in not empty============");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (attribute == sqlDef) {
			String sqlDefStr = ((Settable) attribute).getDefaultExpression();
			sqlDefStr = ((Settable) attribute).getExpression();
			if (sqlDefStr.length() > 0) {
				_hasSQLCommand = true;
				_queryDef = DBQueryDefParserEmitter.parseQueryDef(_schemaDef,
						sqlDefStr);
				_columns = _queryDef.getSelects();
				generteLabelListAndTypteListFromColumns();
				reconfigurePorts("Sql attribute changed");
			} else if (_dataOutputFormat instanceof Eml200DataOutputFormatField) {
				// if sql command is empty, we will think it will be select *
				// from selected table
				if (_selectedTableEntity != null) {
					_columns = _selectedTableEntity.getFields();
					generteLabelListAndTypteListFromColumns();
					reconfigurePorts("Sql attribute changed");
				}
			}

		} else if (attribute == schemaDef && !_ignoreSchemaChange) {
			// NOTE: We may skip setting it here because _ignoreSchemaChange
			// may be true
			String schemaDefStr = ((Settable) schemaDef).getExpression();

			// MOML may have a blank definition
			if (schemaDefStr.length() > 0) {
				_schemaDefinitionIsSet = true; // remember that we have been
				// set by the MOML

				log.debug("schemaDef >>" + schemaDefStr + "<<");

				_schemaDef = DBSchemaParserEmitter.parseSchemaDef(schemaDefStr);
			}

		} else if (attribute.getName().equals("checkVersion")) {
			log.debug("=========================change checkVersion");
			// use the endpoint to determine where to search for the most recent
			// version
			this.checkForMostRecentRecordId(true);
		} else if ((attribute.getName().equals(ResultRecord.RECORDID) || attribute
				.getName().equals(ResultRecord.ENDPOINT))
				&& this.hasConnectionValues()
				&& !(attribute.getContainer().getContainer() instanceof ResultTreeRoot)) {
			log.debug("=========================change recordid or endpoints");
			if (getRecordId() != null && getEndpoint() != null) {
				_icon.setBusy();
				// start over!
				clearTableRelatedParameter(false);
				//_entityList = new Vector<Entity>();
				_cacheMetaDataItem = (EcogridMetaDataCacheItem) DataCacheManager
						.getCacheItem(new MetadataComplete(), "MetaData "
								+ getRecordId(), getEndpoint(),
								EcogridMetaDataCacheItem.class.getName());
				if (_cacheMetaDataItem.isEmpty()) {
					_cacheMetaDataItem.setEndPoint(getEndpoint());
					_cacheMetaDataItem.setRecordId(getRecordId());
					_cacheMetaDataItem.start();
				}
			}

		} else if (attribute == dataOutputFormat) {
			String format = ((Settable) attribute).getExpression();
			log.debug("=========================change dataOutputFormat "
					+ format);
			String strDataOutputFormat = dataOutputFormat.stringValue();
			_dataOutputFormat = Eml200DataOutputFormatFactory.newInstance(
					strDataOutputFormat, this);
			reconfigurePorts("Output type changed");

		} else if (attribute == selectedEntity) {
			// reset selected entity
			String selectedEntityName = ((Settable) attribute).getExpression();
			log.debug("=========================selected entity "
					+ selectedEntityName);
			setSelectedEntityValue(true);
		}
	}

	/**
	 * This method allows default documentation to be added to the actor
	 * specified in the parameter. The KeplerDocumentation is retrieved from the
	 * 'EML 2 Dataset' that exists in the actor library. The default
	 * documentation is only loaded once since it will likely remain quite
	 * static. If the given actor instance already contains the
	 * KeplerDocumentation attribute, the existing attribute is preserved
	 * (nothing is changed).
	 * 
	 * @param emlActor
	 *            the instance to which documentation will be added
	 */
	public static void generateDocumentationForInstance(
			Eml200DataSource emlActor) {
		try {
			// only look up the documentation only once
			if (defaultDocumentation == null) {
				Iterator cacheIter = CacheManager.getInstance()
						.getCacheObjectIterator();
				while (cacheIter.hasNext()) {
					Object co = cacheIter.next();
					// is this an actor cache object?
					if (co instanceof ActorCacheObject) {
						ActorCacheObject aco = (ActorCacheObject) co;
						// for this class?
						if (aco.getClassName().equals(emlActor.getClassName())) {
							// get the metadata
							ActorMetadata am = aco.getMetadata();
							// get the default documentation from the metadata
							defaultDocumentation = am
									.getDocumentationAttribute();
							log
									.debug("looked up default KeplerDocumentation contained in "
											+ am.getName());
							break;
						}
					}
				}
			}

			// add the documentation for this actor if it is not there already
			if (emlActor.getAttribute("KeplerDocumentation") == null) {
				// make an instance of the documentation attribute for the input
				// actor
				KeplerDocumentationAttribute keplerDocumentation = new KeplerDocumentationAttribute(
						emlActor, "KeplerDocumentation");

				// copy the default and set it for this one
				keplerDocumentation
						.createInstanceFromExisting(defaultDocumentation);
				keplerDocumentation.setContainer(emlActor);
				log.debug("set the KeplerDocumentation for actor instance: "
						+ emlActor.getName());

			}
		} catch (Exception e) {
			log
					.error("error encountered whilst generating default documentation for actor instance: "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * First checks if checkVersion parameter is selected. If it is selected the
	 * Ecogrid is checked for a more recent version of the recordid. If there is
	 * a newer version available, user is asked if they would like to use the
	 * newer version in the workflow. If they really do want to use the newer
	 * version, the most recent recorid is set in the actor attributes.
	 * 
	 */
	private void checkForMostRecentRecordId(boolean prompt) {

		// check if we even want to do this for the actor
		boolean boolCheckVersion = false;
		try {
			boolCheckVersion = 
				((BooleanToken) this.checkVersion.getToken()).booleanValue();
			log.debug("checkVersion flag=" + boolCheckVersion);
		} catch (IllegalActionException e1) {
			log.error("could not parse checkVersion parameter");
			e1.printStackTrace();
		}
		if (!boolCheckVersion) {
			checkVersionPromptCount = 0; // ask again if they turn it on later
			return;
		}

		// check for newer version of the eml
		String mostRecentRecordId = getMostRecentRecordId();

		// peek
		log.debug("Original recordId=" + getRecordId()
				+ " - Most recent recordId=" + mostRecentRecordId);
		// different?
		if (mostRecentRecordId != null
				&& !mostRecentRecordId.equalsIgnoreCase(getRecordId())) {
			
			// are we prompting?
			if (prompt) {
				if (checkVersionPromptCount < 1) {
					boolean response = MessageHandler
							.yesNoQuestion("This workflow uses an old version of: "
									+ getName()
									+ "\nCurrent workflow version: "
									+ getRecordId()
									+ "\nMost recent repository version: "
									+ mostRecentRecordId
									+ "\nWould you like to update the workflow to use the most recent version?"
									+ "\nNOTE:  Both the data and data structure can vary widely from version to version."
									+ "\nNewer version available - " + getName());
	
					if (response == true) {
						// reset the check counter for 'yes' answer so that we'll be
						// able to ask again later
						checkVersionPromptCount = 0;
						setRecordId(mostRecentRecordId);
					} else {
						// I told you once, and i won't tell you again, I said NO!
						checkVersionPromptCount++;
					}
				}
			}
			else {
				setRecordId(mostRecentRecordId);
			}
		}
	}

	/**
	 * This method will clear sql, schema and selected entity parameters in the
	 * configure window.
	 */
	private void clearTableRelatedParameter(boolean all)
			throws ptolemy.kernel.util.IllegalActionException {
		// clean up entity list and sql and schema attribute
		_entityList = new Vector();
		if (all) {
			selectedEntity.setExpression("");
		}
		selectedEntity.removeAllChoices();
		_queryDef = new DBQueryDef();
		sqlDef.setExpression("");
		_schema = new DSSchemaDef();
		schemaDef.setExpression("");
	}

	/**
	 * Creates the schema definition from the incoming data columns
	 */
	private void createSchemaFromData(Entity tableEntity) {
		try // XXX shouldn't catch this exception here
		{
			// _schemaDefinitionIsSet gets set when the schema has come
			// from the MOML
			// So if it is false here, then we set the attribute from the data.
			//
			// If after this the attr gets set from the MOML it will override
			// what we set here.
			// Entity tableEntity = dataItem.getEntity();
			DataCacheObject dataItem = tableEntity.getDataCacheObject();
			if (tableEntity != null) {
				_schema.addTable(tableEntity);
				_schemaDef = _schema;

				DBTableNameResolver nameResolver = new DBTableNameResolver();
				try {
					tableEntity = nameResolver.resolveTableName(tableEntity);
				} catch (Exception e) {
				}
				boolean refresh = false;
				if (tableEntity.isSimpleDelimited()) {
					// for simple dimilter, we can use text table
					_tableGenerator = new DBTablesGenerator(tableEntity,
							dataItem.getBaseFileName(), refresh);
				} else {
					// for complex format, we should use inpustream to create
					// table
					InputStream input = dataItem.getDataInputStream();
					_tableGenerator = new DBTablesGenerator(tableEntity, input,
							refresh);
				}

				_tableGenerator.run(); // don't do thread

				String schemaDefXML = DBSchemaParserEmitter.emitXML(_schemaDef);
				_ignoreSchemaChange = true;
				((Settable) schemaDef).setExpression(schemaDefXML);
				_ignoreSchemaChange = false;
			}
		} catch (IllegalActionException e) {
			log.debug("In createSchemaFromData: " + e);
		}

	}

	public void preview() {

		String displayText = "PREVIEW NOT IMPLEMENTED FOR THIS ACTOR";
		JFrame frame = new JFrame(this.getName() + " Preview");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane scrollPane = null;
		JTable jtable = null;

		try {

			// set everything up (datawise)
			this.initialize();

			// check the entity - different displays for different formats
			// Compressed file
			if (this._selectedTableEntity.getHasGZipDataFile()
					|| this._selectedTableEntity.getHasTarDataFile()
					|| this._selectedTableEntity.getHasZipDataFile()) {
				displayText = "Selected entity is a compressed file.  \n"
						+ "Preview not implemented for output format: "
						+ this.dataOutputFormat.getExpression();
				if (this._dataOutputFormat instanceof Eml200DataOutputFormatUnzippedFileName) {
					Eml200DataOutputFormatUnzippedFileName temp = (Eml200DataOutputFormatUnzippedFileName) this._dataOutputFormat;
					displayText = "Files: \n";
					for (int i = 0; i < temp.getTargetFilePathInZip().length; i++) {
						displayText += temp.getTargetFilePathInZip()[i] + "\n";
					}
				}

			}
			// SPATIALRASTERENTITY or SPATIALVECTORENTITY are "image entities"
			// as far as the parser is concerned
			else if (this._selectedTableEntity.getIsImageEntity()) {
				// use the content of the cache file
				displayText = new String(this.getSelectedCachedDataItem()
						.getData());
			}
			// TABLEENTITY
			else {
				// holds the rows for the table on disk with some in memory
				String vectorTempDir = DotKeplerManager.getInstance().getCacheDirString();
				// + "vector"
				// + File.separator;
				PersistentVector rowData = new PersistentVector(vectorTempDir);

				// go through the rows and add them to the persistent vector
				// model
				Vector row = this.gotRowVectorFromSource();
				while (!row.isEmpty()) {
					rowData.addElement(row);
					row = this.gotRowVectorFromSource();
				}
				// the column headers for the table
				Vector columns = this.getColumns();

				/*
				 * with java 6, there is a more built-in sorting mechanism that
				 * does not require the custom table sorter class
				 */
				TableModel tableModel = new PersistentTableModel(rowData,
						columns);
				TableSorter tableSorter = new TableSorter(tableModel);
				jtable = new JTable(tableSorter) {
					// make this table read-only by overriding the default
					// implementation
					public boolean isCellEditable(int row, int col) {
						return false;
					}
				};
				// sets up the listeners for sorting and such
				tableSorter.setTableHeader(jtable.getTableHeader());
				// set up the listener to trash persisted data when done
				frame.addWindowListener(new PersistentTableModelWindowListener(
						(PersistentTableModel) tableModel));
			}
		} catch (Exception e) {
			displayText = "Problem encountered while generating preview: \n"
					+ e.getMessage();
			log.error(displayText);
			e.printStackTrace();
		}

		// make sure there is a jtable, otherwise show just a text version of
		// the data
		if (jtable != null) {
			jtable.setVisible(true);
			// jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			scrollPane = new JScrollPane(jtable);
		} else {
			JTextArea textArea = new JTextArea();
			textArea.setColumns(80);
			textArea.setText(displayText);
			textArea.setVisible(true);
			scrollPane = new JScrollPane(textArea);
		}
		scrollPane.setVisible(true);
		panel.setOpaque(true);
		panel.add(scrollPane, BorderLayout.CENTER);
		frame.setContentPane(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	/**
	 * Get a URL pointer to the documentation for this data source. The XML
	 * source of the EML document is retrieved from the cache, and then passed
	 * to an XSLT parser to be transformed into HTML format, which is saved in a
	 * temporary file. The URL of the temporary file containing the HTML result
	 * is returned.
	 * 
	 * @return URL the URL of the HTML file containing the documentation
	 */
	public URL getDocumentation() {

		String namespace = getNamespace();
		if (namespace == null) {
			namespace = EML2MetadataSpecification.EML200NAMESPACE;
		}
		log.debug("The name space is " + namespace);
		URL htmlUrl = null;
		// Get the metadata XML document and transform it to html
		if (_cacheMetaDataItem.isReady()) {
			try {
				String htmlFileName = _cacheMetaDataItem.getBaseFileName()
						+ HTMLEXTENSION;
				InputStream is = _cacheMetaDataItem.getDataInputStream();
				InputStreamReader source = new InputStreamReader(is);
				htmlUrl = StaticUtil.getMetadataHTMLurl(source, namespace,
						htmlFileName);

			} catch (Exception fnfe) {
				log.debug("Could not open temporary output file.");
			}
		}
		return htmlUrl;
	}

	/**
	 * Creates the delimtedReader from the data item inputstream
	 */
	private void createDelimitedReader() throws Exception {
		/*
		 * String data = aDataStr.toString(); log.debug("-----------------\n" +
		 * data + "\n-----------------\n", 2);
		 */

		// log.debug("entityhash: " + entityhash.toString());
		if (_selectedTableEntity.isSimpleDelimited()) {
			boolean stripHeaderLine = true;
			boolean isLenientBool = ((BooleanToken) this.isLenient.getToken())
					.booleanValue();
			_simpleDelimitedReader = new DelimitedReader(_reader,
					_selectedTableEntity.getAttributes().length,
					_selectedTableEntity.getDelimiter(), _selectedTableEntity
							.getNumHeaderLines(), _selectedTableEntity
							.getRecordDelimiter(), _selectedTableEntity
							.getNumRecords(), stripHeaderLine);
			_simpleDelimitedReader.setLenient(isLenientBool);
		} else {
			_complexFormatReader = new TextComplexFormatDataReader(_reader,
					_selectedTableEntity);
		}

		// _dataVectors = dr.getTokenizedData(true);
	}

	/**
	 * Returns a Ptolemly type for a given Kepler DataSource Type
	 * 
	 * @param aType
	 *            DataSource type
	 * @return Ptolemy type
	 */
	BaseType getBaseType(String aType) {
		BaseType type = (BaseType) _TypeHash.get(aType);
		if (type == null) {
			return BaseType.UNKNOWN;
		}
		return type;
	}

	/**
	 * Inner class used for completion notification of EcoGridMetaDataCacheItem
	 * objects.
	 */
	private class MetadataComplete implements DataCacheListener {

		public void complete(DataCacheObject aItem) {
			log.debug("MetadataComplete: " + this);

			try {
				aItem.removeListener(this);

				if (!aItem.isReady()) {
					log.error("Unable to download MetaData");
					/*
					 * if (SHOWGUIERROR) { throw new
					 * IllegalActionException(this,
					 * "Unable to download MetaData");
					 * 
					 * }
					 */
					MessageHandler.error("Unable to download MetaData");
					_icon.setError();
					return;
				}

				try {
					parsePackage(new InputStreamReader(new FileInputStream(
							new File(aItem.getAbsoluteFileName()))));
				} catch (Exception e) {
					log
							.error(
									"Exception occurred during MetaDataCompletion",
									e);
					/*
					 * if (SHOWGUIERROR) { throw new
					 * IllegalActionException(this,
					 * "Unable to parse the MetaData: " +e.getMessage() ,
					 * "alert", JOptionPane.ERROR_MESSAGE); }
					 */
					MessageHandler.error("Unable to parse the MetaData: "
							+ e.getMessage());
					_icon.setError();
				}
				// if no sql command, we need set columns
				if (!_hasSQLCommand) {
					log
							.debug("There is no sql command attribute and set up columns in compelete method");
					_columns = _selectedTableEntity.getFields();
				}

				generteLabelListAndTypteListFromColumns();
				reconfigurePorts("Metadata Complete");

			} finally {
				_metadataCompleted.release();
			}
			// System.out.println("metadata complete !!!!!!!!!!!!!!!!1");
		}
	}

	// ------------------------------------------------------------------------
	// -- DataCacheListener
	// ------------------------------------------------------------------------

	public void complete(DataCacheObject aItem) {
		log.debug("complete: " + this);

		log.debug("Class of aItem " + aItem.getClass().getName());
		aItem.removeListener(this);
		if (aItem.isReady()) {
			log.debug("aItem is instanceof EcogridDataCacheItem");

			EcogridDataCacheItem item = (EcogridDataCacheItem) aItem;
			String entityIdentifier = item.getEntityIdentifier();
			int index = lookupEntityListName(entityIdentifier);
			Entity entity = null;
			if (index != INDEXFORNOTFOUND) {
				entity = (Entity) _entityList.elementAt(index);
				entity.setDataCacheObject(item);
			}

			if (entity != null && !entity.getIsImageEntity()) {

				createSchemaFromData(entity);

			}
		} else if (aItem.isError()) {
			log.debug("In failed download path");
			EcogridDataCacheItem item = (EcogridDataCacheItem) aItem;
			String entityIdentifier = item.getEntityIdentifier();
			int index = lookupEntityListName(entityIdentifier);
			Entity entity = null;
			if (index != INDEXFORNOTFOUND) {
				entity = (Entity) _entityList.elementAt(index);
			}
			// because fail to download, set null as data cache item
			entity.setDataCacheObject(null);
			_numberOfFailedDownloadEntities++;
			failedDownloadEntityName.add(entityIdentifier);
		}
		// Decrement the number of downloads completed.
		_entityCountDown.release();

		// Check for completion
		finished();
	}

	/**
	 * Parses the package and fills in the names and types arrays.
	 * 
	 * @param eml
	 * @throws IllegalActionException
	 */
	private void parsePackage(Reader eml) throws IllegalActionException {
		Eml200Parser eml2parser;

		try { // parse the package for the names and types of the atts
			log.debug("creating parser");
			eml2parser = new Eml200Parser();
			log.debug("parsing...");
			eml2parser.parse(new InputSource(eml));
			if(getNamespace() == null ){
			  log.debug("the namespace from parser is " + eml2parser.getNameSpace() + " and set it to ResultRecord");
			  setNamespace(eml2parser.getNameSpace());
			}
			// get if this package has image entity
			// _hasImageEntity = eml2parser.getHasImageEntity();
			// log.debug("This pakcage has image entity " + _hasImageEntity);
			log.debug("Done parsing");
		} catch (Exception e) {
      e.printStackTrace();
			throw new IllegalActionException("Error parsing the eml package: "
					+ e.getMessage());
		}

		getData(eml2parser); // fills in the _dataVectors data member
		setOptionsForSelectedEntityAttribute();

		// finished();
	}

	private void finished() {

		if (_entityCountDown.currentCount() > 0) {
			return;
		}

		try {
			// this method will set up columns info if not sql command
			setSelectedEntityValue(false);

		} catch (Exception e) {
			log.debug("The error in set up selected entity is ", e);

		}
		if (_numberOfFailedDownloadEntities == 0) {
			_icon.setReady();
		} else {
			log.error("Some downloads failed");
			StringBuffer entityNameList = new StringBuffer();
			for (int i = 0; i < failedDownloadEntityName.size(); i++) {
				if (entityNameList.length() > 0) {
					entityNameList.append(", ");
				}
				String name = (String) failedDownloadEntityName.elementAt(i);

				entityNameList.append(name);
			}

			_icon.setError();
			
			//make an exception, but just use the message handler for it
			String msg = 
				"Data entity/entities: "
				+ entityNameList.toString()
				+ " failed to be downloaded, please check the data link in metadata";
			
			InternalErrorException exception = 
				new InternalErrorException(
					this,
					null,
					"Download error");
			try {
				MessageHandler.warning(msg, exception);
			} catch (CancelException e) {
				//do nothing
			}

		}

	}

	/**
	 * get the data based on the contents of the package
	 * 
	 * @param parser
	 *            the parser that has already parsed the package.
	 */
	private void getData(Eml200Parser parser) throws IllegalActionException {
		if (parser != null) {
			_numberOfEntities = parser.getEntityCount();
			_entityCountDown = new CountDown(_numberOfEntities);
			/*
			 * if (parser.getEntityCount() > 1) { throw new
			 * IllegalActionException(
			 * "Currently this parser only deals with one entity. " +
			 * "Please use a package with only one entity.");
			 */
			if (_numberOfEntities == 0) {
				throw new IllegalActionException(
						"There must be at least one entity in the EML package.");
			}
			Hashtable entityList = parser.getEntityHash();
			// initialize selected entity
			_selectedTableEntity = (Entity) entityList.get(entityList.keys()
					.nextElement());
			// log.debug("entityhash: " + _entityList.toString());

			// start a thread to get data chachedItem
			Enumeration enu = entityList.elements();
			while (enu.hasMoreElements()) {

				Entity singleEntity = (Entity) enu.nextElement();
				log.debug("Adding Entity " + singleEntity);
				// System.out.println("Data URL = "+singleEntity.getURL());
				String dataurl = dataFilePath.getExpression();
				// check for absolute path - relative was not resolving correctly
				try {
					URL url = dataFilePath.asURL();
					if (url != null) {
						dataurl = url.getPath();
					}
				} catch (Exception e) {
					// do nothing - just ignore it
				}
				// use the dataurl parameter if it has been entered
				// ie replace the url in the Entity object
				// use: to set local data file
				if (dataurl != null && dataurl.length() > 0) {
					if (dataurl.startsWith("/")) {
						dataurl = "file://" + dataurl;
					} else if (dataurl.startsWith("file:/")) {
						if ((!dataurl.startsWith("file://"))
								&& (!dataurl.startsWith("file:///"))) {
							dataurl = dataurl
									.replaceFirst("file:/", "file:///");
						}
					} else {
						dataurl = "file:///" + dataurl;
					}
					singleEntity.setURL(dataurl);
					// System.out.println("Data URL(1) = "+singleEntity.getURL());
				}
				_entityList.add(singleEntity);
				getDataItemFromCache(singleEntity);
			}

		}
	}

	/**
	 * This method will start a thread to get the cached data item at tableEntity.getURL()
	 * 
	 * @param tableEntity
	 */
	private void getDataItemFromCache(Entity tableEntity) {
		if (tableEntity == null) {
			log.debug("The table enity is null and couldn't get cached data item");
			return;
		}
		String fileURLStr = tableEntity.getURL();
		log.debug("Data URL is: " + fileURLStr);
		// we need to distinguish zip file and generate
		// String compression = tableEntity.getCompressionMethod();
		EcogridDataCacheItem cachedDataItem = null;
		String dataItemName = "Data " + fileURLStr + " from " + this.getEndpoint();
		if (tableEntity.getHasZipDataFile()) {
			log.debug("This is a zip data cacheItem");
			cachedDataItem = (EcogridZippedDataCacheItem) DataCacheManager
					.getCacheItem(this, dataItemName, tableEntity.getName(), fileURLStr,
							EcogridZippedDataCacheItem.class.getName());
			// _isZipDataFile = true;
		} else if (tableEntity.getHasGZipDataFile()) {
			log.debug("This is a gzip data cacheItem");
			cachedDataItem = (EcogridGZippedDataCacheItem) DataCacheManager
					.getCacheItem(this, dataItemName, tableEntity.getName(), fileURLStr,
							EcogridGZippedDataCacheItem.class.getName());
			if (tableEntity.getHasTarDataFile()) {
				log.debug("This is gizp and tar data cache item");
				cachedDataItem.setIsTarFile(true);
			}

		} else if (tableEntity.getHasTarDataFile()) {
			log.debug("This is a tar data cacheItem");
			cachedDataItem = (EcogridTarArchivedDataCacheItem) DataCacheManager
					.getCacheItem(this, dataItemName, tableEntity.getName(), fileURLStr,
							EcogridTarArchivedDataCacheItem.class.getName());
		} else {
			log.debug("This is a uncompressed data cacheItem");
			cachedDataItem = (EcogridDataCacheItem) DataCacheManager
					.getCacheItem(this, dataItemName, tableEntity.getName(), fileURLStr,
							EcogridDataCacheItem.class.getName()); 
		}
		if (cachedDataItem.isEmpty()) {
			String endPoint = null;
			try {
				// System.out.println("before get endpoint .........");
				endPoint = this.getEndpoint();
				// System.out.println("after get endpoint ............");
			} catch (Exception e) {
				log.debug("the exeption for get endpoint is " + e.getMessage());
			}

			if (endPoint == null) {
				cachedDataItem.setEndPoint(ENDPOINT);
			} else {
				cachedDataItem.setEndPoint(endPoint);
			}
			// add entity identifier order to track the cachedata item associated
			// with which entity(In complete method)
			cachedDataItem.setEntityIdentifier(tableEntity.getName());
			cachedDataItem.start();
		}

	}

	/**
	 * This method will set options for "Selected Entity" after getting all data
	 * entities. The options will be the list of entity names.
	 */
	private void setOptionsForSelectedEntityAttribute() {
		if (_entityList != null) {
			int length = _entityList.size();
			for (int i = 0; i < length; i++) {
				Entity entity = (Entity) _entityList.elementAt(i);
				String entityName = entity.getName();
				if (entityName != null && !entityName.trim().equals("")) {
					selectedEntity.addChoice(entityName);
				}

			}

		}
	}

	/**
	 * Method to set up selected entity. If Attribute "selectedEntity" already
	 * has value, look up the enitytList and found the selected entity. If
	 * attribute "selectedEntity doesn't have any value, we choose the index 0
	 * as selected entity.
	 */
	private void setSelectedEntityValue(boolean fromAttributeChange)
			throws IllegalActionException {

		String selectedEntityName = selectedEntity.stringValue();
		log.debug("The selected entity name is " + selectedEntityName);
		if (!_entityList.isEmpty()) {

			if (selectedEntityName != null
					&& !selectedEntityName.trim().equals("")) {
				// already has a selected entity in momol
				log.debug("There is a selected entity in actor");
				int selectedIndex = lookupEntityListName(selectedEntityName);
				// System.out.println("index of selected entity is "+selectedIndex);
				if (selectedIndex == INDEXFORNOTFOUND) {
					throw new IllegalActionException(
							"The selected Entity in momol cound't be found");
				} else {
					_selectedTableEntity = (Entity) _entityList
							.elementAt(selectedIndex);
					if (!_hasSQLCommand) {
						_columns = _selectedTableEntity.getFields();
					}
					_selectedCachedDataItem = (EcogridDataCacheItem) _selectedTableEntity
							.getDataCacheObject();
					generteLabelListAndTypteListFromColumns();
					reconfigurePorts("Selected Entity changed");
				}
			} else {
				// no selected enity in moml and we need selected one
				log.debug("There is NOT a selected entity in actor");
				_selectedTableEntity = (Entity) _entityList
						.elementAt(DEFAULTINDEX);
				_selectedCachedDataItem = (EcogridDataCacheItem) _selectedTableEntity
						.getDataCacheObject();
				reconfigurePorts("Selected Entity changed");
				String entityName = _selectedTableEntity.getName();
				log.debug("set the default entity name " + entityName
						+ "because the there is no selected entity");
				selectedEntity.setExpression(entityName);
				if (!fromAttributeChange) {
					log
							.debug("send a moml request for adding selected Entity parameter");
					StringBuffer buffer = new StringBuffer();
					buffer.append("<property name=\"");
					buffer.append("selectedEntity");
					buffer.append("\" class=\"");
					buffer.append(selectedEntity.getClassName());
					buffer.append("\" value=\"");
					buffer.append(entityName);
					buffer.append("\"/>");
					String moml = buffer.toString();
					log.debug("The moml string is " + moml);
					NamedObj container = (NamedObj) this.getContainer();
					NamedObj composite = (NamedObj) container.getContainer();
					MoMLChangeRequest request = new MoMLChangeRequest(this,
							this, moml.toString());
					request.setUndoable(true);
					this.requestChange(request);

				}
			}
		}
	}

	/**
	 * Method to find the entity index in EntityList which has the same name as
	 * the given string. If no entity index is found, -1 will be returned.
	 */
	private int lookupEntityListName(String givenString) {
		log.debug("Looking for entity named " + givenString);
		int index = INDEXFORNOTFOUND;
		if (givenString != null && !givenString.trim().equals("")) {
			int size = _entityList.size();
			for (int i = 0; i < size; i++) {
				Entity entity = (Entity) _entityList.elementAt(i);
				String entityName = entity.getName();
				if (entityName != null && !entityName.trim().equals("")
						&& entityName.equals(givenString)) {
					index = i;

				}
			}
		}
		log.debug("The selected index is " + index);
		return index;
	}

	/**
	 * This method will generate selected column list.
	 */
	private void generteLabelListAndTypteListFromColumns() {

		if (_columns != null) {
			int size = _columns.size();
			_selectedColumnLabelList = new String[size];
			_selectedColumnTypeList = new Type[size];
			for (int i = 0; i < size; i++) {
				DSTableFieldIFace column = (DSTableFieldIFace) _columns
						.elementAt(i);
				_selectedColumnLabelList[i] = column.getName();
				String type = column.getDataType();
				_selectedColumnTypeList[i] = getBaseType(type);

			}
		}

	}

	/**
	 * Method to transform a string array to token array based on given type.
	 */
	static Token[] transformStringVectorToTokenArray(Vector stringVector,
			Type type, Vector missingValuecode) throws IllegalActionException {
		if (stringVector == null) {
			return null;
		}
		int size = stringVector.size();
		Token[] columnToken = new Token[size];
		for (int j = 0; j < size; j++) {
			String eleStr = (String) stringVector.elementAt(j);
			log.debug("The column value " + eleStr);
			Token val = transformStringToToken(eleStr, type, missingValuecode,
					null);
			columnToken[j] = val;
		}

		return columnToken;
	}

	/**
	 * Method to transform a string to token based on given type and
	 * missingValue.
	 */
	static Token transformStringToToken(String eleStr, Type type,
			Vector missingValue, String columnName)
			throws IllegalActionException {
		Token val = null;
		if (missingValue != null && !missingValue.isEmpty()) {
			if (missingValue.contains(eleStr)) {
				eleStr = null;
			}
		}
		String elementError = "Element \"";
		String errorMessage1 = "\" couldn't be in the ";
		String errorMessage2 = " column " + columnName
				+ ". It probably is a missing value code, however metadata "
				+ "doesn't describe it. Please make a double check.";
		// find the data type for each att
		if (type == BaseType.INT) {

			if (eleStr != null && !eleStr.equals("")) {
				try {
					val = new IntToken(new Integer(eleStr).intValue());
				} catch (NumberFormatException e) {
					throw (new IllegalActionException(elementError + eleStr
							+ errorMessage1 + "integer" + errorMessage2));
				}
			} else {
				// eleStr = null;
				val = IntToken.NIL;
				// val.nil();
			}

		} else if (type == BaseType.DOUBLE) {
			if (eleStr != null && !eleStr.equals("")) {
				try {
					val = new DoubleToken(new Double(eleStr).doubleValue());
				} catch (NumberFormatException e) {
					throw (new IllegalActionException(elementError + eleStr
							+ errorMessage1 + "numerical" + errorMessage2));
				}
			} else {
				// eleStr = null;
				val = DoubleToken.NIL;
			}

		} else if (type == BaseType.LONG) {
			if (eleStr != null && !eleStr.equals("")) {
				try {
					val = new LongToken(new Long(eleStr).longValue());
				} catch (NumberFormatException e) {
					throw (new IllegalActionException(elementError + eleStr
							+ errorMessage1 + "numerical" + errorMessage2));
				}
			} else {
				// eleStr = null;
				val = LongToken.NIL;
				// val.nil();
			}

		} else if (type == BaseType.STRING) {
			if (eleStr != null) {
				val = new StringToken(eleStr);
			} else {
				// eleStr = "nil";
				val = StringToken.NIL;
				// val.nil();
			}

		} else {
			val = new IntToken(0);
		}
		return val;
	}

	/**
	 * Callback method that indicates that the workflow is being stopped. This
	 * method is executed from the Director when the user presses the "stop"
	 * button. All this does is release the execution thread if it happens to be
	 * blocked in the initialize method waiting for _metaDataCompleted or
	 * _entityCountDown to complete.
	 */
	public void stop() {
		log.debug("Stopping");

		synchronized (_metadataCompleted) {
			_metadataCompleted.notifyAll();
		}
		synchronized (_entityCountDown) {
			_entityCountDown.notifyAll();
		}
		// TODO Auto-generated method stub
		super.stop();
	}

	/**
	 * This method will determine if the resultset is complete.
	 * 
	 * @return true if the result has been completely retrieved
	 */
	public boolean isEndOfResultset() throws SQLException {
		if (_resultSet != null && _resultSet.getResultSet() != null
				&& _resultSet.getResultSet().isAfterLast()) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * This method will remove RecordDetails attribute from this entity. Those
	 * attribute is useful for ResultRecord, but is useless for this class and
	 * we wouldn't let it show up.
	 */
	private void removeResultRecordDetailsAtrribute()
			throws IllegalActionException, NameDuplicationException {
		// System.out.println("at begining");
		List list = this.attributeList();
		if (list != null) {
			// System.out.println("attribute list is not null");
			for (int j = 0; j < list.size(); j++) {
				// System.out.println("the attribute list's size is "+recordDetailList.size());
				ptolemy.kernel.util.Attribute att = (ptolemy.kernel.util.Attribute) list
						.get(j);
				String attName = att.getName();
				// System.out.println("------- the attribute "+att.getName());
				Vector recordDetailList = this.getRecordDetailList();

				if (recordDetailList != null && attName != null
						&& recordDetailList.contains(attName)) {
					// System.out.println("------- remove the attribute "+att.getName());
					att.setContainer(null);

				}
			}
		}

	}

	/**
	 * First checks if checkVersion parameter is selected. If it is selected the
	 * Ecogrid is checked for a more recent version of the recordid. If there is
	 * a newer version available, user is asked if they would like to use the
	 * newer version in the workflow. If they really do want to use the newer
	 * version, the most recent recorid is set in the actor attributes.
	 * 
	 */
	private String getMostRecentRecordId() {
	
		// look up the identifcation service based on the query/get service
		EcoGridService queryService = 
			EcoGridServicesController.getInstance().getService(getEndpoint());
		EcoGridService lsidService = 
			EcoGridServicesController.getInstance().getService(
					queryService.getServiceGroup(),
					EcoGridServicesController.IDENTIFIERSERVICETYPE);
		log.debug("identifier service endpoint: " + lsidService.getEndPoint());
	
		// check for newer version of the eml
		String mostRecentRecordId = null;
		try {
	
			// translate the recordId to an lsid
			KeplerLSID recordLsid = 
				new KeplerLSID(getRecordId(), "kepler-project.org");
			log.debug("translated recordLsid=" + recordLsid);
	
			// look up the next revision
			IdentifierServiceClient lsidClient = 
				new IdentifierServiceClient(lsidService.getEndPoint());
			KeplerLSID temp = 
				new KeplerLSID(lsidClient.getNextRevision(recordLsid.toString()));
			log.debug("next recordLsid=" + temp);
	
			// subtract from the next revision to get the current latest
			mostRecentRecordId = 
				temp.getNamespace() 
				+ "." + temp.getObject()
				+ "." + (temp.getRevision().longValue() - 1);
			log.debug("mostRecentRecordId=" + mostRecentRecordId);
			
		} catch (Exception e) {
			log.error("Problem looking up most recent record for id: "
					+ getRecordId() + "\nError is: " + e.getMessage());
			e.printStackTrace();
		}
		
		return mostRecentRecordId;
	}
	
	/**
   * Overwrite the method in Parent class -- ResultRecord
   */
  public Reader getFullRecord() {
    //System.out.println("in eml actor ==================");
    Reader recordReader = null;
    if(this.getEndpoint() != null) {
      //System.out.println("end point is not null");
      return super.getFullRecord();
    } else  {
      //System.out.println("end point is === null");
      //System.out.println("final path is "+emlFileFinalPath);
      String endpoint = null;
      EcogridMetaDataCacheItem item = (EcogridMetaDataCacheItem) DataCacheManager
          .getCacheItem(new MetadataComplete(),
              "MetaData " + emlFileFinalPath, endpoint,
              EcogridMetaDataCacheItem.class.getName());
      if (item.isEmpty()) {
        //System.out.println(" in item is empty branch");
        item.setEndPoint(endpoint);
        item.setRecordId(emlFileFinalPath);
        item.start();
        while (!item.isError() && !item.isReady()) {
          // do nothing, just waiting
          //System.out.println("Waiting!!!!!!!!!!");
        }
      } 
      // make sure the item is finished    
      // when it is ready
      InputStream stream = item.getDataInputStream();
      if (stream != null) {
        //System.out.println(" stream is not null");
        recordReader = new InputStreamReader(stream);
      }
      //System.out.println("the return reader is "+recordReader);
      return recordReader;
    }
  
  }
}
