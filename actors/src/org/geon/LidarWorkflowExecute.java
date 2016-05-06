/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-20 15:42:39 -0700 (Thu, 20 Sep 2012) $' 
 * '$Revision: 30726 $'
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

package org.geon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.Manager;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanMatrixToken;
import ptolemy.data.DoubleMatrixToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntMatrixToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongMatrixToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.expr.Variable;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.ComponentPort;
import ptolemy.kernel.ComponentRelation;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.moml.MoMLParser;

//////////////////////////////////////////////////////////////////////////
////LidarWorkflowExecute
/**
 * Creates and Invokes the lidar workflow on the fly from portlet inputs.
 * 
 * @author Efrat Jaeger
 */
public class LidarWorkflowExecute {
	public LidarWorkflowExecute() {

	}

	public synchronized String executeQuery(String modelURL, Map inputs)
			throws Exception {

		URL url = new URL(modelURL);
		if (url != null) {
			MoMLParser parser = new MoMLParser();
			NamedObj model;
			try {
				System.out.println("before parsing query template");
				model = parser.parse(null, url);
				System.out.println("after parsing query template");
				boolean in = setInputs(model, inputs);
				if (!in)
					return null;
				// TEMPORARY HERE TO SAVE THE GENERATED FILLED QUERY TEMPLATE...
				try {
					String uniqueId = (String) inputs.get("uniqueId");
					String appPath = (String) inputs.get("appPath");
					String filePath = appPath + "data/tmp/queryFilled"
							+ uniqueId + ".xml";
					File queryFilled = new File(filePath);
					String queryFilledURL = queryFilled.getAbsolutePath();
					BufferedWriter out = new BufferedWriter(new FileWriter(
							queryFilledURL, false));

					out.write(model.exportMoML());
					out.close();
				} catch (Exception ex1) {
					System.out
							.println("unable to create query template file queryFilled"
									+ uniqueId + ".xml: " + ex1.getMessage());
					_log("unable to create query template file queryFilled"
							+ uniqueId + ".xml: " + ex1.getMessage());
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				try {
					PrintWriter pw = new PrintWriter(new FileWriter(logURL,
							true));
					ex.printStackTrace(pw);
					pw.close();
				} catch (Exception e) {
				}
				return null;
			}
			if (model instanceof CompositeActor) {
				return executeModel((CompositeActor) model);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public synchronized String executeProcess(String modelURL, Map inputs)
			throws Exception {

		uniqueId = (String) inputs.get("id");
		appPath = (String) inputs.get("appPath");
		String host = (String) inputs.get("host");
		String port = (String) inputs.get("port");

		// create workflow
		Map workflowInputs = new TreeMap();
		workflowInputs.put("uniqueId", uniqueId);
		workflowInputs.put("appPath", appPath);
		workflowInputs.put("host", host);
		workflowInputs.put("port", port);
		String email = (String) inputs.get("email");
		if (email != null && !email.equals("")) {
			workflowInputs.put("email", email);
		}

		// System.out.println(System.getProperty("user.dir"));
		URL url = new URL(modelURL);
		if (url != null) {
			MoMLParser parser = new MoMLParser();
			NamedObj model;
			try {
				System.out.println("before parsing process template");
				model = parser.parse(null, url);
				System.out.println("after parsing process template");
				boolean in = setInputs(model, workflowInputs);
				if (!in)
					return null;
				updateWorkflow(model, inputs);
			} catch (Exception ex) {
				ex.printStackTrace();
				try {
					PrintWriter pw = new PrintWriter(new FileWriter(logURL,
							true));
					ex.printStackTrace(pw);
					pw.close();
				} catch (Exception e) {
				}

				return null;
			}
			if (model instanceof CompositeActor) {

				/*
				 * ComponentEntity CA = ((CompositeEntity)
				 * model).getEntity("EmailNotification"); CA =
				 * ((CompositeEntity) CA).getEntity("Expression"); String exp =
				 * ((Expression) CA).expression.getExpression();
				 * System.out.println("Email content:\n" + exp);
				 */
				System.out.println("after updating model");
				return executeModel((CompositeActor) model);
			} else {
				return null;
			}
		} else {
			return null;
		}

	}

	/**
	 * This method takes a url specifying the model to be execute. The
	 * <i>args<i> argument is a record token that will be used to set
	 * corresponding attributes of the spedified model by naming match, (see
	 * _setAttribute() method). The results of executing the model is returned
	 * back by setting the value of some Attributes. In particular, only
	 * Attributes that have name matches the <i>resultLabels<i> are returned.
	 * The return result is a RecordToken which has the resultLabels as its
	 * feild.
	 * 
	 * @param url
	 *            The Model url.
	 * @param args
	 *            A set of attributes of the specified model.
	 * @param resultLabels
	 *            Labels of the returned result.
	 * @return The execution result.
	 * @exception IllegalActionException
	 *                If can not parse the url or failed to execute the model.
	 */
	public synchronized String execute(String modelURL, Map inputs)
			throws Exception {

		uniqueId = (String) inputs.get("id");
		appPath = (String) inputs.get("appPath");

		String queryFile = createQueryFile(inputs);
		System.out.println("query file ==> " + queryFile);

		String paramsFile = createParamsFile(inputs);
		System.out.println("params file ==> " + paramsFile);

		inputs.put("download", download);

		// create workflow
		Map workflowInputs = new TreeMap();
		workflowInputs.put("uniqueId", uniqueId);
		String email = (String) inputs.get("email");
		if (email != null && !email.equals("")) {
			workflowInputs.put("email", email);
		}

		// System.out.println(System.getProperty("user.dir"));
		URL url = new URL(modelURL);
		if (url != null) {
			MoMLParser parser = new MoMLParser();
			NamedObj model;
			try {
				System.out.println("before parsing query template");
				model = parser.parse(null, url);
				System.out.println("after parsing query template");
				boolean in = setInputs(model, workflowInputs);
				if (!in)
					return null;
				updateWorkflow(model, inputs);
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
			if (model instanceof CompositeActor) {
				System.out.println("after parsing model");
				// return "done"; /////////////
				return executeModel((CompositeActor) model);
			} else {
				return null;
			}
		} else {
			return null;
		}

		// remove params file and query file

	}

	/**
	 * This method takes model argument which is type of CompositeActor. The
	 * <i>args<i> argument is a record token that will be used to set
	 * corresponding attributes of the spedified model by naming match, (see
	 * _setAttribute() method). The results of executing the model is returned
	 * back by setting the value of some Attributes. In particular, only
	 * Attributes that have name matches the <i>resultLabels<i> are returned.
	 * The return result is a RecordToken which has the resultLabels as its
	 * feild.
	 * 
	 * @param model
	 *            The Model.
	 * @param args
	 *            A set of attributes of the specified model.
	 * @param resultLabels
	 *            Labels of the returned result.
	 * @return The execution result.
	 * @exception IllegalActionException
	 *                If failed to execute the model.
	 */
	public String executeModel(CompositeActor model) throws Exception {
		Manager manager = model.getManager();
		if (manager == null) {
			// System.out.println("create manager for the model");
			manager = new Manager(model.workspace(), "Manager");
			model.setManager(manager);
		}
		// _setAttribute(model, args);

		manager.execute();

		return _getResult(model);
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Get the output from the parameter (assuming that there is a single output
	 * variable name <i>output</i>
	 * 
	 * @param model
	 *            The model executed.
	 * @param resultLabels
	 *            Labels of the returned result.
	 * @return The execution result.
	 * @exception IllegalActionException
	 *                If reading the ports or setting the parameters causes it.
	 */
	private String _getResult(CompositeActor model)
			throws IllegalActionException {

		String val = "";
		Iterator atts = model.attributeList().iterator();
		while (atts.hasNext()) {
			Attribute att = (Attribute) atts.next();
			if (att instanceof Variable) {
				String attName = att.getName();
				if (attName.trim().toLowerCase().equals("output")) {
					Variable var = (Variable) att;
					System.out.println(var.getType().toString());
					if (var.getType().equals(BaseType.UNKNOWN)) {
						var.setTypeEquals(BaseType.GENERAL);
					}

					val = processToken(var.getToken());
					System.out.println("after process token");
					break;
				}
			}
		}
		return val;
	}

	private boolean setInputs(NamedObj model, Map inputs) {
		Iterator atts = model.attributeList().iterator();
		while (atts.hasNext()) {
			Attribute att = (Attribute) atts.next();
			if (att instanceof Variable) {
				String attName = att.getName();
				if (inputs.containsKey(attName)) {
					try {
						Variable var = (Variable) att;
						var.setToken((String) inputs.get(attName));
						System.out.println(attName + ":" + var.getExpression());
					} catch (Exception ex) {
						ex.printStackTrace();
						return false;
					}
				}

			}
		}
		return true;
	}

	private Token process(Element tag) throws IllegalActionException { // elem =
																		// channel..
		String tagName = tag.getTagName();

		if (tagName.equals("unsignedbyte")) {
			String val = tag.getFirstChild().getNodeValue().trim();
			UnsignedByteToken t = new UnsignedByteToken(val);
			return t;
		}

		if (tagName.equals("int")) {
			String val = tag.getFirstChild().getNodeValue().trim();
			IntToken t = new IntToken(val);
			return t;
		}

		if (tagName.equals("double")) {
			String val = tag.getFirstChild().getNodeValue().trim();
			DoubleToken t = new DoubleToken(val);
			return t;
		}

		if (tagName.equals("long")) {
			String val = tag.getFirstChild().getNodeValue().trim();
			LongToken t = new LongToken(val);
			return t;
		}

		if (tagName.equals("intmatrix")) {
			NodeList rows = tag.getElementsByTagName("row");
			int rowNum = rows.getLength();
			if (rowNum > 0) {
				NodeList tempCols = ((Element) rows.item(0))
						.getElementsByTagName("value");
				int colNum = tempCols.getLength();
				int[][] Mat = new int[rowNum][colNum];
				for (int j = 0; j < rowNum; j++) {
					NodeList Cols = ((Element) rows.item(j))
							.getElementsByTagName("value");
					for (int k = 0; k < colNum; k++) {
						String val = Cols.item(k).getFirstChild()
								.getNodeValue().trim();
						Mat[j][k] = Integer.parseInt(val);
					}
				}
				IntMatrixToken t = new IntMatrixToken(Mat);
				return t;
			} else
				return new IntMatrixToken();
		}

		if (tagName.equals("doublematrix")) {
			NodeList rows = tag.getElementsByTagName("row");
			int rowNum = rows.getLength();
			if (rowNum > 0) {
				NodeList tempCols = ((Element) rows.item(0))
						.getElementsByTagName("value");
				int colNum = tempCols.getLength();
				double[][] Mat = new double[rowNum][colNum];
				for (int j = 0; j < rowNum; j++) {
					NodeList Cols = ((Element) rows.item(j))
							.getElementsByTagName("value");
					for (int k = 0; k < colNum; k++) {
						String val = Cols.item(k).getFirstChild()
								.getNodeValue().trim();
						Mat[j][k] = Double.parseDouble(val);
					}
				}
				DoubleMatrixToken t = new DoubleMatrixToken(Mat);
				return t;
			} else
				return new DoubleMatrixToken();
		}

		if (tagName.equals("longmatrix")) {
			NodeList rows = tag.getElementsByTagName("row");
			int rowNum = rows.getLength();
			if (rowNum > 0) {
				NodeList tempCols = ((Element) rows.item(0))
						.getElementsByTagName("value");
				int colNum = tempCols.getLength();
				long[][] Mat = new long[rowNum][colNum];
				for (int j = 0; j < rowNum; j++) {
					NodeList Cols = ((Element) rows.item(j))
							.getElementsByTagName("value");
					for (int k = 0; k < colNum; k++) {
						String val = Cols.item(k).getFirstChild()
								.getNodeValue().trim();
						Mat[j][k] = Long.parseLong(val);
					}
				}
				LongMatrixToken t = new LongMatrixToken(Mat);
				return t;
			} else
				return new LongMatrixToken();
		}

		if (tagName.equals("booleanmatrix")) {
			NodeList rows = tag.getElementsByTagName("row");
			int rowNum = rows.getLength();
			if (rowNum > 0) {
				NodeList tempCols = ((Element) rows.item(0))
						.getElementsByTagName("value");
				int colNum = tempCols.getLength();
				boolean[][] Mat = new boolean[rowNum][colNum];
				for (int j = 0; j < rowNum; j++) {
					NodeList Cols = ((Element) rows.item(j))
							.getElementsByTagName("value");
					for (int k = 0; k < colNum; k++) {
						String val = Cols.item(k).getFirstChild()
								.getNodeValue().trim();
						Mat[j][k] = Boolean.getBoolean(val);
					}
				}
				BooleanMatrixToken t = new BooleanMatrixToken(Mat);
				return t;
			} else
				return new BooleanMatrixToken();
		}

		if (tagName.equals("string")) {
			String val = tag.getFirstChild().getNodeValue().trim();
			StringToken t = new StringToken(val);
			return t;
		}

		if (tagName.equals("array")) {
			Vector tVec = new Vector();
			NodeList arrChilds = tag.getChildNodes();

			// find only the element children
			for (int j = 0; j < arrChilds.getLength(); j++) {
				if (arrChilds.item(j) instanceof Element) {

					Token token = process((Element) arrChilds.item(j));
					tVec.add(token);
				}
			}
			Token tArr[] = new Token[tVec.size()];
			tVec.toArray(tArr);
			ArrayToken t = new ArrayToken(tArr);
			return t;
		}

		if (tagName.equals("record")) {
			NodeList labelNodes = tag.getElementsByTagName("label");
			NodeList valueNodes = tag.getElementsByTagName("value");
			int recLen = labelNodes.getLength();
			String[] labels = new String[recLen];
			Token[] tokens = new Token[recLen];
			for (int j = 0; j < recLen; j++) {
				labels[j] = labelNodes.item(j).getFirstChild().getNodeValue()
						.trim();
			}
			int k = 0;
			for (int j = 0; j < valueNodes.getLength(); j++) {
				Element value = (Element) valueNodes.item(j);
				NodeList childs = value.getChildNodes();
				for (int l = 0; l < childs.getLength(); l++) {
					Node node = childs.item(l);
					if (node instanceof Element) {
						tokens[k++] = process((Element) node);
					}
				}
			}
			RecordToken t = new RecordToken(labels, tokens);
			return t;
		}

		Token t = new Token();
		return t;
	}

	private String processToken(Token token) {

		String val = "";
		if (token instanceof UnsignedByteToken) {
			val = ((UnsignedByteToken) token).unitsString();
			return val;

		} else if (token instanceof IntToken) {
			val = ((IntToken) token).toString();
			return val;

		} else if (token instanceof DoubleToken) {
			val = ((DoubleToken) token).toString();
			return val;

		} else if (token instanceof LongToken) {
			val = ((LongToken) token).toString();
			return val;

		} else if (token instanceof IntMatrixToken) {
			val = "[";
			int[][] Mat = ((IntMatrixToken) token).intMatrix();
			for (int i = 0; i < Mat.length; i++) {
				for (int j = 0; j < Mat[0].length; j++) {
					val += Mat[i][j] + ",";
				}
				val += ":";
			}
			val += "]";
			return val;

		} else if (token instanceof DoubleMatrixToken) {
			val = "[";
			double[][] Mat = ((DoubleMatrixToken) token).doubleMatrix();
			for (int i = 0; i < Mat.length; i++) {
				for (int j = 0; j < Mat[0].length; j++) {
					val += Mat[i][j] + ",";
				}
				val += ":";
			}
			val += "]";
			return val;

		} else if (token instanceof LongMatrixToken) {
			val = "[";
			long[][] Mat = ((LongMatrixToken) token).longMatrix();
			for (int i = 0; i < Mat.length; i++) {
				for (int j = 0; j < Mat[0].length; j++) {
					val += Mat[i][j] + ",";
				}
				val += ":";
			}
			val += "]";
			return val;

		} else if (token instanceof BooleanMatrixToken) {
			val = "[";
			boolean Mat[][] = ((BooleanMatrixToken) token).booleanMatrix();
			for (int i = 0; i < Mat.length; i++) {
				for (int j = 0; j < Mat[0].length; j++) {
					val += Mat[i][j] + ",";
				}
				val += ":";
			}
			val += "]";
			return val;

		} else if (token instanceof StringToken) {
			val = ((StringToken) token).stringValue();
			return val;

		} else if (token instanceof ArrayToken) {
			val = "{";
			ArrayToken at = (ArrayToken) token;
			Token[] tArr = at.arrayValue();
			for (int i = 0; i < tArr.length; i++) {
				val += processToken(tArr[i]) + ",";
			}
			val += "}";
			return val;

		} else if (token instanceof RecordToken) {
			val = "{";
			RecordToken rt = (RecordToken) token;
			Object[] labelObjects = rt.labelSet().toArray();
			for (int i = 0; i < labelObjects.length; i++) {
				String label = (String) labelObjects[i];
				val += label + "=";
				Token value = rt.get(label);
				val += processToken(value);
				val += ",";
			}
			val += "}";
			return val;
		}
		return val;
	}

	private String createParamsFile(Map inputs) {
		try {
			String filePath = appPath + "data/tmp/params" + uniqueId + ".txt";
			// //String filePath = appPath + "params" + uniqueId + ".txt";
			File paramsFile = new File(filePath);
			String paramsFileURL = paramsFile.getAbsolutePath();
			BufferedWriter out = new BufferedWriter(new FileWriter(
					paramsFileURL, false));

			String algs[] = { "elev", "slope", "aspect", "pcurv" };
			String formats[] = { "view", "arc", "ascii", "tiff" };
			download = "0";

			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					String type = algs[i] + formats[j];
					String typeVal = (String) inputs.get(type);
					System.out.println(type + "=" + typeVal);
					if (typeVal != null && !typeVal.equals(""))
						out.write(type + "=" + typeVal + "\n");
					if (typeVal != null && typeVal.equals("1") && j > 0) {
						download = "1";
					}
				}
			}

			String res = (String) inputs.get("res");
			if (res != null && !res.equals(""))
				out.write("res=" + res + "\n");

			String dmin = (String) inputs.get("dmin");
			String tension = (String) inputs.get("tension");
			String smooth = (String) inputs.get("smooth");

			if (dmin != null && !dmin.equals(""))
				out.write("dmin=" + dmin + "\n");
			if (tension != null && !tension.equals(""))
				out.write("tension=" + tension + "\n");
			if (smooth != null && !smooth.equals(""))
				out.write("smooth=" + smooth + "\n");

			out.close();

			return paramsFileURL;

		} catch (Exception ex) {
			System.out.println("unable to create parameters file params"
					+ uniqueId + ": " + ex.getMessage());
			_log("unable to create parameters file params" + uniqueId + ": "
					+ ex.getMessage());
		}
		return "";
	}

	private String createQueryFile(Map inputs) {
		try {
			String filePath = appPath + "data/tmp/" + uniqueId + ".sql";
			// //String filePath = appPath + uniqueId + ".sql";
			File queryFile = new File(filePath);
			String queryFileURL = queryFile.getAbsolutePath();
			BufferedWriter out = new BufferedWriter(new FileWriter(
					queryFileURL, false));

			String MinX = (String) inputs.get("MinX");
			String MaxX = (String) inputs.get("MaxX");
			String MinY = (String) inputs.get("MinY");
			String MaxY = (String) inputs.get("MaxY");

			out.write("CONNECT TO LIDAR;\n");
			out.write("EXPORT TO db2temp/" + uniqueId + " OF DEL\n");
			out.write("MODIFIED by decplusblank striplzeros\n");
			out.write("select dec(X,10,2) , dec(Y,10,2), dec(Z,10,2)\n");
			out.write("from NSAF.Q39123C73\n"); // the table name will be
												// variable!!!
			out.write("WHERE db2gse.EnvelopesIntersect ( geometry, ");
			out.write(MinX + ", " + MinY + ", " + MaxX + ", " + MaxY
					+ ", 1005 ) = 1;\n");
			// 1005 will be variable as well!!!
			out.write("DISCONNECT LIDAR;");

			out.close();

			return queryFileURL;

		} catch (Exception ex) {
			System.out.println("unable to create query file " + uniqueId
					+ ".sql: " + ex.getMessage());
			_log("unable to create query file " + uniqueId + ".sql: "
					+ ex.getMessage());
		}
		return "";
	}

	private void updateWorkflow(NamedObj model, Map inputs) {

		String elevview = (String) inputs.get("elevview");
		String slopeview = (String) inputs.get("slopeview");
		String aspectview = (String) inputs.get("aspectview");
		String pcurvview = (String) inputs.get("pcurvview");
		String queryCount = (String) inputs.get("queryCount");

		StringBuffer imageSrcs = new StringBuffer();

		if (queryCount != null) {
			if (!queryCount.equals("-1")) {
				imageSrcs.append("&lt;TABLE cellpadding=2&gt;&#10;");
				imageSrcs.append("&lt;TR&gt;&lt;TD&gt;");
				imageSrcs.append("Processing of " + queryCount
						+ " points in Lidar point cloud.");
				imageSrcs.append("&lt;/TD&gt;&lt;/TR&gt;&#10;");
				imageSrcs.append("&lt;/TABLE&gt;&lt;BR&gt;&#10;");
			}
		}

		if (elevview != null && elevview.equals("1")) {
			addEntityAndLinks(model, "Spline");
			imageSrcs.append(createImageSrc("Elevation (spline)", "Spline"));
		}
		if (slopeview != null && slopeview.equals("1")) {
			addEntityAndLinks(model, "Slope");
			imageSrcs.append(createImageSrc("Slope", "Slope"));
		}
		if (aspectview != null && aspectview.equals("1")) {
			addEntityAndLinks(model, "Aspect");
			imageSrcs.append(createImageSrc("Aspect", "Aspect"));
		}
		if (pcurvview != null && pcurvview.equals("1")) {
			addEntityAndLinks(model, "Pcurv");
			imageSrcs.append(createImageSrc("Pcurv", "Pcurv"));
		}

		// Connect TGZ entity (component to pass the tgz url) to Transmitter's
		// output.
		addRelationAndLinks(model, "TGZ", "filePath", "Transmitter", "output");

		String dl = (String) inputs.get("download");
		if (dl.equals("1")) {
			// Connect TGZ output to CreateHTML input
			addRelationAndLinks(model, "CreateHTML", "tgz", "TGZ", "output");

			// /add download to imageSrc!!!!
			imageSrcs.append("&lt;HR&gt;&lt;BR&gt;&#10;");
			imageSrcs
					.append("&lt;p&gt;Below you can download an archive file (tgz format) with the results of your job");
			imageSrcs.append("&lt;/P&gt;&#10;");
			imageSrcs
					.append("&lt;A href=\\&quot;&quot; + tgz + &quot;\\&quot;&gt;Results&lt;/A&gt;&#10;");
		}

		String rawdata = (String) inputs.get("rawdata");
		if (rawdata.equals("1")) {
			String MinX = (String) inputs.get("MinX");
			String MinY = (String) inputs.get("MinY");
			String MaxX = (String) inputs.get("MaxX");
			String MaxY = (String) inputs.get("MaxY");
			String cStr = (String) inputs.get("classification");
			String rawdataURL = (String) inputs.get("rawdataURL");

			// /add rawdataURL to imageSrc!!!!
			imageSrcs.append("&lt;HR&gt;&lt;BR&gt;&#10;");
			imageSrcs.append("&lt;p&gt;Raw data for " + cStr);
			imageSrcs.append("bounding box selection: MinX = " + MinX + ", ");
			imageSrcs.append("MaxX = " + MaxX + ", MinY = " + MinY
					+ ", MaxY = " + MaxY + " is available at ");
			imageSrcs.append("&lt;/P&gt;&#10;");
			imageSrcs.append("&lt;A href=\\&quot;" + rawdataURL
					+ "\\&quot;&gt;Results&lt;/A&gt;&#10;");
		}

		imageSrcs.append("&lt;HR&gt;&lt;BR&gt;&#10;");
		imageSrcs.append("&lt;p&gt;Download ");
		imageSrcs
				.append("&lt;A href=\\&quot;http://activetectonics.la.asu.edu/GEONatASU/LViz.html\\&quot;&gt;");
		imageSrcs.append("LViz&lt;/A&gt;&#10;");
		imageSrcs
				.append(" - A free application for visualization of LiDAR point cloud and interpolated surface ");
		imageSrcs
				.append("data developed in the Active Tectonics Research Group at Arizona State University.");
		imageSrcs.append("&lt;/P&gt;&#10;");

		StringBuffer html = createHTML(imageSrcs);

		// add the html string to the expression attribute
		setHTML(model, html);

		// System.out.println(model.exportMoML());
		try {
			String filePath = appPath + "data/tmp/templateFilled" + uniqueId
					+ ".xml";
			// //String filePath = appPath + "templateFilled" + uniqueId +
			// ".xml";
			File templateFile = new File(filePath);
			String templateFileURL = templateFile.getAbsolutePath();
			BufferedWriter out = new BufferedWriter(new FileWriter(
					templateFileURL, false));

			out.write(model.exportMoML());
			out.close();
		} catch (Exception ex) {
			System.out
					.println("unable to create template filled file templateFilled"
							+ uniqueId + ".xml: " + ex.getMessage());
			_log("unable to create template filled file templateFilled"
					+ uniqueId + ".xml: " + ex.getMessage());
		}

	}

	private StringBuffer createHTML(StringBuffer imageSrcs) {
		StringBuffer html = new StringBuffer();
		html.append("&quot;&lt;html&gt;&#10;&lt;HEAD&gt;&#10;");
		html
				.append("&lt;TITLE&gt;LiDAR Processing Workflow Outputs&lt;/TITLE&gt;&#10;");
		html
				.append("&lt;/HEAD&gt;&#10;&lt;body bgcolor=\\&quot;#FFFFFF\\&quot;&gt;&#10;&lt;TABLE&gt;&#10;");
		html.append("&lt;TR&gt;&#10;&lt;TD&gt;");
		html
				.append("&lt;A HREF=\\&quot;http://activetectonics.la.asu.edu/GEONatASU/index.htm\\&quot; ");
		html
				.append("target=\\&quot;_new\\&quot;&gt;&lt;IMG SRC=\\&quot;http://agassiz.la.asu.edu/logos/GEONASUWebBanner.jpg\\&quot; ");
		html
				.append("alt=\\&quot;GEON at ASU homepage\\&quot;&gt;&lt;/A&gt;&lt;/TD&gt;&#10;&lt;TD&gt;");
		html
				.append("&lt;a href=\\&quot;http://www.sdsc.edu\\&quot; target=\\&quot;_new\\&quot;&gt;");
		html
				.append("&lt;img src=\\&quot;http://www.sdsc.edu/logos/SDSClogo-plusname-red.gif\\&quot; ");
		html
				.append("alt=\\&quot;San Diego Supercomputer Center\\&quot; height=\\&quot;60\\&quot; width=\\&quot;216\\&quot;&gt;&lt;/a&gt;");
		html.append("&lt;/TD&gt;&#10;&lt;/TR&gt;&#10;&lt;/TABLE&gt;&#10;");
		html
				.append("&lt;BR&gt;&lt;BR&gt;&#10;&lt;h1&gt;LiDAR Processing Workflow Outputs&lt;/h1&gt;&#10;");
		html.append("&lt;BR&gt;&#10;");
		html.append(imageSrcs.toString());
		html.append("&lt;/body&gt;&#10;&lt;/html&gt;&quot;");

		return html;
	}

	private String createImageSrc(String caption, String name) {
		StringBuffer imageSrc = new StringBuffer();
		imageSrc.append("&lt;TABLE cellpadding=2&gt;&#10;");
		imageSrc.append("&lt;TR&gt;&lt;TD align=center&gt;");
		imageSrc.append(caption);
		imageSrc.append("&lt;/TD&gt;&lt;/TR&gt;&#10;");
		imageSrc.append("&lt;TR&gt;&lt;TD&gt;&lt;IMG SRC=\\&quot;&quot;");
		imageSrc.append(" + " + name.toLowerCase());
		imageSrc.append(" + &quot;\\&quot; ");
		imageSrc.append("alt=\\&quot;");
		imageSrc.append(name);
		imageSrc.append("\\&quot;&gt;&lt;/TD&gt;&lt;/TR&gt;&#10;");
		imageSrc.append("&lt;/TABLE&gt;&#10;&lt;BR&gt;&lt;BR&gt;&#10;");

		return imageSrc.toString();
	}

	private void setHTML(NamedObj model, StringBuffer html) {

		String request = "<property name=\"" + "expression" + "\" value=\""
				+ html.toString() + "\"/>";
		// System.out.println("request: "+request+"\n\n");

		try {
			ComponentEntity CA = ((CompositeEntity) model)
					.getEntity("CreateHTML");
			// System.out.println("prev expression: " +
			// ((Expression)CA).expression.getExpression());
			CA.requestChange(new MoMLChangeRequest(model, CA, request));

			ComponentEntity CA1 = ((CompositeEntity) model)
					.getEntity("CreateHTML");
			// System.out.println("new expression: " +
			// ((Expression)CA1).expression.getExpression());
		} catch (Exception ex) {
			System.out
					.println("Exception in momlChangeRequest setting html string: "
							+ ex.getMessage());
			_log("Exception in momlChangeRequest setting html string: "
					+ ex.getMessage());
		}
	}

	private void addEntityAndLinks(NamedObj model, String name) {
		// create composite vis component moml description.
		StringBuffer vis = createCompositeVis(name);

		// add entity to model
		model.requestChange(new MoMLChangeRequest(this, model, vis.toString()));

		// ComponentEntity CA = ((CompositeEntity)
		// model).getEntity("SplineURL");
		// System.out.println(model.exportMoML());

		// // add links and relations to model.

		// Connect Transmitter's output to dynamic component input.
		// ///// addRelationAndLinks(model, name+".input",
		// "Transmitter.output");
		addRelationAndLinks(model, name, "input", "Transmitter", "output");
		// Connect dynamic component's output to CreatHTML input.
		// ///// addRelationAndLinks(model, "CreateHTML."+name.toLowerCase(),
		// name+".output");
		addRelationAndLinks(model, "CreateHTML", name.toLowerCase(), name,
				"output");
	}

	/** Create a new relation and connect outP to inP */
	private void addRelationAndLinks(NamedObj model, String inComp, String inP,
			String outComp, String outP) {
		// search for the first available relation number
		// CompositeEntity CE = (CompositeEntity)model;

		ComponentEntity inEnt = ((CompositeEntity) model).getEntity(inComp);
		ComponentPort inPort = (ComponentPort) inEnt.getPort(inP);

		ComponentEntity outEnt = ((CompositeEntity) model).getEntity(outComp);
		ComponentPort outPort = (ComponentPort) outEnt.getPort(outP);

		try {
			ComponentRelation rel = ((CompositeEntity) model).connect(outPort,
					inPort);
			rel.setContainer((CompositeEntity) model);
			// System.out.println(rel.exportMoML());
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				PrintWriter pw = new PrintWriter(new FileWriter(logURL, true));
				ex.printStackTrace(pw);
				pw.close();
			} catch (Exception e) {
			}
		}

		// model.requestChange(new MoMLChangeRequest(this, model, request));

		// relNum++;

	}

	/** Create a new relation and connect outP to inP */
	/*
	 * private void addRelationAndLinks(NamedObj model, String inP, String outP)
	 * { // search for the first available relation number CompositeEntity CE =
	 * (CompositeEntity)model; while(true) { Relation rel =
	 * CE.getRelation("relation"+relNum); if (rel == null) { break; } else
	 * relNum++; }
	 * 
	 * // add relation StringBuffer rels = new StringBuffer();
	 * rels.append("    <relation name=\"relation"+ relNum +
	 * "\" class=\"ptolemy.actor.TypedIORelation\">\n");rels.append(
	 * "        <property name=\"width\" class=\"ptolemy.data.expr.Parameter\" value=\"1\">\n"
	 * ); rels.append("        </property>\n");
	 * rels.append("    </relation>\n");
	 * 
	 * model.requestChange(new MoMLChangeRequest(this, model, rels.toString()));
	 * 
	 * // add Transmitter's output to the relation. String request =
	 * "<link port=\"" + outP + "\" relation=\"relation" + relNum + "\"/>";
	 * model.requestChange(new MoMLChangeRequest(this, model, request));
	 * 
	 * // connect Transmitter's output to dynamic component input request =
	 * "<link port=\"" + inP + "\" relation=\"relation" + relNum + "\"/>";
	 * model.requestChange(new MoMLChangeRequest(this, model, request));
	 * 
	 * relNum++;
	 * 
	 * }
	 */
	private StringBuffer createCompositeVis(String name) {

		// URL extract component, tried using class file unsuccessfully..
		StringBuffer urlExtractContent = new StringBuffer();
		urlExtractContent
				.append("        <property name=\"tag\" class=\"ptolemy.data.expr.StringParameter\" value=\"value\">\n");
		urlExtractContent.append("        </property>\n");
		urlExtractContent
				.append("        <port name=\"url\" class=\"ptolemy.actor.TypedIOPort\">\n");
		urlExtractContent.append("            <property name=\"output\"/>\n");
		urlExtractContent.append("        </port>\n");
		urlExtractContent
				.append("        <port name=\"xmlStr\" class=\"ptolemy.actor.TypedIOPort\">\n");
		urlExtractContent.append("            <property name=\"input\"/>\n");
		urlExtractContent.append("        </port>\n");
		urlExtractContent
				.append("        <entity name=\"TokenToString\" class=\"org.geon.TokenToString\">\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"ArrayToElements\" class=\"ptolemy.actor.lib.ArrayToElements\">\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"Const2\" class=\"ptolemy.actor.lib.Const\">\n");
		urlExtractContent
				.append("            <property name=\"value\" class=\"ptolemy.data.expr.Parameter\" value=\"&quot;&lt;/&quot;+tag+&quot;&gt;&quot;\">\n");
		urlExtractContent.append("            </property>\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"imageTag\" class=\"org.sdm.spa.XPath\">\n");
		urlExtractContent
				.append("            <property name=\"xpath\" class=\"ptolemy.actor.parameters.PortParameter\" value=\"&quot;//&quot; + tag\">\n");
		urlExtractContent.append("            </property>\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"StringSubstring\" class=\"ptolemy.actor.lib.string.StringSubstring\">\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"Const\" class=\"ptolemy.actor.lib.Const\">\n");
		urlExtractContent
				.append("            <property name=\"value\" class=\"ptolemy.data.expr.Parameter\" value=\"&quot;&lt;&quot;+tag+&quot;&gt;&quot;\">\n");
		urlExtractContent.append("            </property>\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"StringIndexOf\" class=\"ptolemy.actor.lib.string.StringIndexOf\">\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"StringLength\" class=\"ptolemy.actor.lib.string.StringLength\">\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <entity name=\"StringToXML\" class=\"ptolemy.actor.lib.conversions.StringToXML\">\n");
		urlExtractContent.append("        </entity>\n");
		urlExtractContent
				.append("        <relation name=\"relation9\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation8\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation2\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation7\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation11\" class=\"ptolemy.actor.TypedIORelation\">\n");
		// <vertex name="vertex1" value="[260.0, 325.0]">
		// </vertex>
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation6\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation5\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation4\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <relation name=\"relation10\" class=\"ptolemy.actor.TypedIORelation\">\n");
		urlExtractContent
				.append("            <property name=\"width\" class=\"ptolemy.data.expr.Parameter\" value=\"1\">\n");
		urlExtractContent.append("            </property>\n");
		// <vertex name="vertex1" value="[240.0, 580.0]">
		// </vertex>
		urlExtractContent.append("        </relation>\n");
		urlExtractContent
				.append("        <link port=\"url\" relation=\"relation4\"/>\n");
		urlExtractContent
				.append("        <link port=\"xmlStr\" relation=\"relation10\"/>\n");
		urlExtractContent
				.append("        <link port=\"TokenToString.input\" relation=\"relation8\"/>\n");
		urlExtractContent
				.append("        <link port=\"TokenToString.output\" relation=\"relation11\"/>\n");
		urlExtractContent
				.append("        <link port=\"ArrayToElements.input\" relation=\"relation7\"/>\n");
		urlExtractContent
				.append("        <link port=\"ArrayToElements.output\" relation=\"relation8\"/>\n");
		urlExtractContent
				.append("        <link port=\"Const2.output\" relation=\"relation6\"/>\n");
		urlExtractContent
				.append("        <link port=\"Const2.trigger\" relation=\"relation10\"/>\n");
		urlExtractContent
				.append("        <link port=\"imageTag.input\" relation=\"relation\"/>\n");
		urlExtractContent
				.append("        <link port=\"imageTag.output\" relation=\"relation7\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringSubstring.input\" relation=\"relation11\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringSubstring.output\" relation=\"relation4\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringSubstring.start\" relation=\"relation2\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringSubstring.stop\" relation=\"relation9\"/>\n");
		urlExtractContent
				.append("        <link port=\"Const.output\" relation=\"relation5\"/>\n");
		urlExtractContent
				.append("        <link port=\"Const.trigger\" relation=\"relation10\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringIndexOf.searchFor\" relation=\"relation6\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringIndexOf.inText\" relation=\"relation11\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringIndexOf.output\" relation=\"relation9\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringLength.input\" relation=\"relation5\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringLength.output\" relation=\"relation2\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringToXML.input\" relation=\"relation10\"/>\n");
		urlExtractContent
				.append("        <link port=\"StringToXML.output\" relation=\"relation\"/>\n");

		StringBuffer urlExtract = new StringBuffer();
		urlExtract.append("    <entity name=\"" + name
				+ "URL\" class=\"ptolemy.actor.TypedCompositeActor\">\n");
		urlExtract.append(urlExtractContent.toString());
		urlExtract.append("    </entity>\n");

		// Global Mapper web service.
		StringBuffer GlobalMapper = new StringBuffer();
		GlobalMapper.append("    <entity name=\"" + name
				+ "GM\" class=\"org.sdm.spa.WebService\">\n");
		GlobalMapper
				.append("        <property name=\"wsdlUrl\" class=\"ptolemy.data.expr.StringParameter\" ");
		GlobalMapper
				.append("value=\"http://titan.geongrid.org:8080/axis/services/GlobalMapper?wsdl\">\n");
		GlobalMapper.append("        </property>\n");
		GlobalMapper
				.append("        <property name=\"methodName\" class=\"ptolemy.data.expr.StringParameter\" ");
		GlobalMapper.append("value=\"getImageForGridAscii\">\n");
		GlobalMapper.append("        </property>\n");
		GlobalMapper
				.append("        <port name=\"in0\" class=\"ptolemy.actor.TypedIOPort\">\n");
		GlobalMapper.append("            <property name=\"input\"/>\n");
		GlobalMapper.append("        </port>\n");
		GlobalMapper
				.append("        <port name=\"getImageForGridAsciiReturn\" class=\"ptolemy.actor.TypedIOPort\">\n");
		GlobalMapper.append("             <property name=\"output\"/>\n");
		GlobalMapper.append("        </port>\n");
		GlobalMapper.append("    </entity>\n");

		// Setting up the composite of GM and extract URL
		StringBuffer vis = new StringBuffer();
		vis.append("<entity name=\"" + name
				+ "\" class=\"ptolemy.actor.TypedCompositeActor\">\n");
		vis
				.append("    <port name=\"input\" class=\"ptolemy.actor.TypedIOPort\">\n");
		vis.append("        <property name=\"input\"/>\n");
		vis.append("    </port>\n");
		vis
				.append("    <port name=\"output\" class=\"ptolemy.actor.TypedIOPort\">\n");
		vis.append("        <property name=\"output\"/>\n");
		vis.append("    </port>\n");

		// add the entities.
		vis.append(urlExtract.toString());
		vis.append(GlobalMapper.toString());

		// add the links and relations
		vis
				.append("    <relation name=\"relation4\" class=\"ptolemy.actor.TypedIORelation\">\n");
		vis
				.append("        <property name=\"width\" class=\"ptolemy.data.expr.Parameter\" value=\"1\">\n");
		vis.append("        </property>\n");
		vis.append("    </relation>\n");
		vis
				.append("    <relation name=\"relation3_1\" class=\"ptolemy.actor.TypedIORelation\">\n");
		vis.append("    </relation>\n");
		vis
				.append("    <relation name=\"relation7_4\" class=\"ptolemy.actor.TypedIORelation\">\n");
		vis.append("    </relation>\n");
		vis.append("    <link port=\"input\" relation=\"relation7_4\"/>\n");
		vis.append("    <link port=\"output\" relation=\"relation3_1\"/>\n");
		vis.append("    <link port=\"" + name
				+ "URL.url\" relation=\"relation3_1\"/>\n");
		vis.append("    <link port=\"" + name
				+ "URL.xmlStr\" relation=\"relation4\"/>\n");
		vis.append("    <link port=\"" + name
				+ "GM.in0\" relation=\"relation7_4\"/>\n");
		vis.append("    <link port=\"" + name
				+ "GM.getImageForGridAsciiReturn\" relation=\"relation4\"/>\n");
		vis.append("</entity>\n");

		return vis;

		// System.out.println(urlExtract.toString());

	}

	/** Enumerate relations */
	private int relNum = 2;

	/** specify whether the user selected download formats */
	private String download = "0";

	// private String xml = "";

	/** Execution uniqueId */
	private String uniqueId = "";

	/** Remote application path */
	private String appPath = "";

	static public void main(String args[]) throws Exception {
		System.out.println("init");
		String inputWF = "file://C:/Projects/kepler/workflows/geo/lidar/lidarTemplate.xml";
		File momlFile = new File(inputWF);
		System.out.println("BEFORE!!");
		LidarWorkflowExecute we = new LidarWorkflowExecute();

		Map inputs = new TreeMap();
		inputs.put("id", "1");

		// UPDATE PATH!!!!!!
		inputs.put("appPath", "C:/Projects/kepler/workflows/geo/lidar/");
		inputs.put("elevarc", "1");
		inputs.put("slopearc", "0");
		inputs.put("aspectarc", "0");
		inputs.put("pcurvarc", "0");
		inputs.put("elevview", "1");
		inputs.put("slopeview", "1");
		inputs.put("pcurvview", "1");
		inputs.put("elevtiff", "1");

		try {
			String res = "";
			we.execute(inputWF, inputs);
			System.out.println(res);
			System.out.println("AFTER");

		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				PrintWriter pw = new PrintWriter(new FileWriter(logURL, true));
				ex.printStackTrace(pw);
				pw.close();
			} catch (Exception e) {
			}
		}
	}

	public void _log(String log) {
		try {
			File logFile = new File(logURL);
			FileWriter fw = new FileWriter(logFile, true);
			fw.write(log);
			fw.flush();
			fw.close();
		} catch (IOException ioex) {
			System.out.println("Unable to write " + log + " to " + logURL
					+ ".\n" + ioex.getMessage());
		}
	}

	private static String logURL = System.getProperty("user.home")
			+ File.separator + ".lidar" + File.separator + "lidarLog";
}