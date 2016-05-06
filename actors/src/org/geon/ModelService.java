/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-20 15:41:53 -0700 (Thu, 20 Sep 2012) $' 
 * '$Revision: 30725 $'
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.Manager;
import ptolemy.actor.lib.Const;
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
public class ModelService {
	public ModelService() {

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
	public String execute(String modelURL, String xmlParams) throws Exception {
		URL url = new URL(modelURL);
		if (url != null) {
			MoMLParser parser = new MoMLParser();
			NamedObj model;
			try {
				model = parser.parse(null, url);
				setInputs(model, xmlParams);

			} catch (Exception ex) {
				throw new IllegalActionException(ex
						+ "Failed to pass the model URL." + url.toString());
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
	 * Iterate over the resultLabels and check whether the specified model has
	 * Attribute with the same name of a label. If so, get the value of the
	 * attribute and return a record token with labels equal to resultLabels and
	 * values equal to the corresponding attribute value.
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

		xml = "<variables>\n";

		Iterator atts = model.attributeList().iterator();
		while (atts.hasNext()) {
			Attribute att = (Attribute) atts.next();
			if (att instanceof Variable) {
				String attName = att.getName();
				if (attName.trim().toLowerCase().startsWith("out")) {
					Variable var = (Variable) att;
					System.out.println(var.getType().toString());
					if (var.getType().equals(BaseType.UNKNOWN)) {
						var.setTypeEquals(BaseType.GENERAL);
					}

					xml += "<output>\n";
					xml += "<name>" + attName + "</name>\n";
					processToken(var.getToken());
					xml += "</output>\n";
				}
			}
		}
		xml += "</variables>";
		return xml;
	}

	private void setInputs(NamedObj model, String xml) {
		try {
			// TODO:: If an input is left empty remove the connected const
			// actor.
			ComponentEntity CE = (ComponentEntity) model;
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			byte[] xmlBytes = xml.getBytes();
			InputStream is = new ByteArrayInputStream(xmlBytes);
			Document doc = builder.parse(is);

			NodeList inputs = doc.getElementsByTagName("input");
			for (int i = 0; i < inputs.getLength(); i++) {
				NodeList nameNode = ((Element) inputs.item(i))
						.getElementsByTagName("name");
				String name = "";
				if (nameNode != null) {
					// get the name of the input and process it.
					name = nameNode.item(0).getFirstChild().getNodeValue()
							.trim();
					name = name.replace('_', '.');
					// find the const actor with this name.

					NodeList channels = ((Element) inputs.item(i))
							.getElementsByTagName("channel");
					for (int j = 0; j < channels.getLength(); j++) {
						Element data = (Element) channels.item(j);
						NodeList childs = data.getChildNodes();
						for (int k = 0; k < childs.getLength(); k++) {
							Node node = childs.item(k);
							if (node instanceof Element) {
								// get the token for element nodes.
								Token dataToken = process((Element) node);
								// instantiate the desired variable with the
								// token value.
								// get the tag name const actor.
								ComponentEntity actor = getEntity(CE, name);
								if (actor instanceof Const) {
									((Const) actor).value
											.setExpression(dataToken.toString());
									System.out.println("lalala");
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ComponentEntity getEntity(ComponentEntity CE, String inputName) {
		int ind = inputName.indexOf(".");
		while (ind != -1) {
			String aName = inputName.substring(0, ind);
			inputName = inputName.substring(ind + 1);
			ind = inputName.indexOf(".");
			CE = ((CompositeActor) CE).getEntity(aName);
		}
		CE = ((CompositeActor) CE).getEntity(inputName);
		return CE;
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

	private void processToken(Token token) {

		if (token instanceof UnsignedByteToken) {
			xml += "<unsignedbyte>";
			String val = ((UnsignedByteToken) token).unitsString();
			xml += val + "</unsignedbyte>\n";

		} else if (token instanceof IntToken) {
			xml += "<int>";
			String val = ((IntToken) token).toString();
			xml += val + "</int>\n";

		} else if (token instanceof DoubleToken) {
			xml += "<double>";
			String val = ((DoubleToken) token).toString();
			xml += val + "</double>\n";

		} else if (token instanceof LongToken) {
			xml += "<long>";
			String val = ((LongToken) token).toString();
			xml += val + "</long>\n";

		} else if (token instanceof IntMatrixToken) {
			xml += "<intmatrix>\n";
			int[][] Mat = ((IntMatrixToken) token).intMatrix();
			for (int i = 0; i < Mat.length; i++) {
				xml += "<row>\n";
				for (int j = 0; j < Mat[0].length; j++) {
					xml += "<value>" + Mat[i][j] + "</value>\n";
				}
				xml += "</row>\n";
			}
			xml += "</intmatrix>\n";

		} else if (token instanceof DoubleMatrixToken) {
			xml += "<doublematrix>\n";
			double[][] Mat = ((DoubleMatrixToken) token).doubleMatrix();
			for (int i = 0; i < Mat.length; i++) {
				xml += "<row>\n";
				for (int j = 0; j < Mat[0].length; j++) {
					xml += "<value>" + Mat[i][j] + "</value>\n";
				}
				xml += "</row>\n";
			}
			xml += "</doublematrix>\n";

		} else if (token instanceof LongMatrixToken) {
			xml += "<longmatrix>\n";
			long[][] Mat = ((LongMatrixToken) token).longMatrix();
			for (int i = 0; i < Mat.length; i++) {
				xml += "<row>\n";
				for (int j = 0; j < Mat[0].length; j++) {
					xml += "<value>" + Mat[i][j] + "</value>\n";
				}
				xml += "</row>\n";
			}
			xml += "</longmatrix>\n";

		} else if (token instanceof BooleanMatrixToken) {
			xml += "<booleanmatrix>\n";
			boolean Mat[][] = ((BooleanMatrixToken) token).booleanMatrix();
			for (int i = 0; i < Mat.length; i++) {
				xml += "<row>\n";
				for (int j = 0; j < Mat[0].length; j++) {
					xml += "<value>" + Mat[i][j] + "</value>\n";
				}
				xml += "</row>\n";
			}
			xml += "</booleanmatrix>\n";

		} else if (token instanceof StringToken) {
			xml += "<string>";
			String val = ((StringToken) token).stringValue();
			xml += val + "</string>\n";

		} else if (token instanceof ArrayToken) {
			xml += "<array>\n";
			ArrayToken at = (ArrayToken) token;
			Token[] tArr = at.arrayValue();
			for (int i = 0; i < tArr.length; i++) {
				processToken(tArr[i]);
			}
			xml += "</array>\n";

		} else if (token instanceof RecordToken) {
			xml += "<record>\n";
			RecordToken rt = (RecordToken) token;
			Object[] labelObjects = rt.labelSet().toArray();
			for (int i = 0; i < labelObjects.length; i++) {
				String label = (String) labelObjects[i];
				xml += "<label>" + label + "</label>\n";
				Token value = rt.get(label);
				xml += "<value>\n";
				processToken(value);
				xml += "</value>\n";
			}
			xml += "</record>\n";
		}

	}

	private String xml = "";

}