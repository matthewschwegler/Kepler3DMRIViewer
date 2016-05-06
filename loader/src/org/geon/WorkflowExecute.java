/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-20 15:41:24 -0700 (Thu, 20 Sep 2012) $' 
 * '$Revision: 30724 $'
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

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.kepler.gui.KeplerInitializer;
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
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLParser;

//////////////////////////////////////////////////////////////////////////
////PTModelService
/**
 * Invoke a model when the service method is called.
 * 
 * @author Efrat Jaeger, Yang Zhao
 * @deprecated This class is mostly duplicated by org.geon.LidarWorkflowExecute.
 * Use org.geon.LidarWorkflowExecute instead since it provides more functionality.
 */
public class WorkflowExecute {
	public WorkflowExecute() {
		// Initialize the cache manager, if it is not present.
		try {
			KeplerInitializer.initializeSystem();
		} catch (Exception ex) {
			ex.printStackTrace();
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
		System.out.println(System.getProperty("user.dir"));
		URL url = new URL(modelURL);
		if (url != null) {
			MoMLParser parser = new MoMLParser();
			NamedObj model;
			try {
				System.out.println("before parsing");
				model = parser.parse(null, url);
				System.out.println("after parsing");
				boolean in = setInputs(model, inputs);
				if (!in)
					return null;

			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
			if (model instanceof CompositeActor) {
				System.out.println("after parsing model");
				return executeModel((CompositeActor) model);
			} else {
				return null;
			}
		} else {
			return null;
		}
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

	/*
	 * private ComponentEntity getEntity(ComponentEntity CE, String inputName) {
	 * int ind = inputName.indexOf("."); while (ind != -1) { String aName =
	 * inputName.substring(0,ind); inputName = inputName.substring(ind+1); ind =
	 * inputName.indexOf("."); CE = ((CompositeActor)CE).getEntity(aName); } CE
	 * = ((CompositeActor)CE).getEntity(inputName); return CE; }
	 */
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

	private String xml = "";

	static public void main(String args[]) throws Exception {
		System.out.println("init");
		String inputWF = "file:///C:/Util/jakarta-tomcat-4.1.30/webapps/examples/data/Atype/Original/testAtts.xml";
		File momlFile = new File(inputWF);
		System.out.println("BEFORE!!");
		WorkflowExecute we = new WorkflowExecute();

		Map inputs = new TreeMap();
		inputs.put("classificationType", "A-type");
		inputs.put("area", "VA");
		inputs.put("bodiesType", "Plutonic");
		inputs.put("diagramsInfo", "diagrams.txt");

		try {
			String res = we.execute(inputWF, inputs);
			System.out.println(res);
			System.out.println("AFTER");

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}