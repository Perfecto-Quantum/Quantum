/*******************************************************************************
 * Copyright (c) 2019 Infostretch Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.qmetry.qaf.automation.step.client.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import com.google.gson.Gson;
import com.qmetry.qaf.automation.step.client.AbstractScenarioFileParser;
import com.qmetry.qaf.automation.step.client.ScenarioFactory;
import com.qmetry.qaf.automation.step.client.ScenarioFileParser;
import com.qmetry.qaf.automation.util.StringUtil;

/**
 * @author chirag.jayswal
 *
 */
public class XMLScenarioFactory extends ScenarioFactory {
	private ScenarioFileParser xmlParser;
	Gson gson = new Gson();

	public XMLScenarioFactory() {
		super(Arrays.asList("xml"));
		xmlParser = new XmlScenarioFileParser();
	}

	@Override
	protected ScenarioFileParser getParser() {
		return xmlParser;
	}

	public class XmlScenarioFileParser extends AbstractScenarioFileParser {

		//		@Override
		//		protected Collection<Object[]> parseFile(String xmlFile) {
		//			ArrayList<Object[]> statements = new ArrayList<Object[]>();
		//			try {
		//				Configurations configs = new Configurations();
		//				HierarchicalConfiguration processor = configs.xml(xmlFile);
		//				List<?> definations = processor.getRoot().getChildren();
		//				for (Object definationObj : definations) {
		//					ImmutableNode root = processor.getNodeModel().getRootNode();
		//					List<ImmutableNode> definitions = root.getChildren();
		//					String type = defination.getName();
		//					String[] entry = new String[3];
		//
		//					if (type.equalsIgnoreCase("SCENARIO") || type.equalsIgnoreCase("STEP-DEF")) {
		//						entry[0] = type;
		//
		//						Map<?, ?> metaData = getMetaData(defination);
		//						entry[1] = (String) metaData.get("name");
		//						metaData.remove("name");
		//						entry[2] = gson.toJson(metaData);
		//						statements.add(entry);
		//						System.out.println("META-DATA:" + entry[2]);
		//						addSteps(defination, statements);
		//						statements.add(new String[] { "END", "", "" });
		//					}
		//				}
		//			} catch (ConfigurationException e) {
		//				e.printStackTrace();
		//			}
		//
		//			return statements;
		//		}

		@Override
		protected Collection<Object[]> parseFile(String xmlFile) {
			ArrayList<Object[]> statements = new ArrayList<Object[]>();
			try {
				Configurations configs = new Configurations();
				// Specify the generic type <ImmutableNode>
				XMLConfiguration processor = configs.xml(xmlFile);

				// Access the root node from the NodeModel
				ImmutableNode root = processor.getNodeModel().getRootNode();
				List<ImmutableNode> definitions = root.getChildren();

				for (ImmutableNode definition : definitions) {
					String type = definition.getNodeName(); // v2 uses getNodeName()
					String[] entry = new String[3];

					if (type.equalsIgnoreCase("SCENARIO") || type.equalsIgnoreCase("STEP-DEF")) {
						entry[0] = type;

						// Pass the definition node to your metadata helper
						Map<?, ?> metaData = getMetaData(definition);
						entry[1] = (String) metaData.get("name");
						metaData.remove("name");
						entry[2] = gson.toJson(metaData);

						statements.add(entry);
						System.out.println("META-DATA:" + entry[2]);

						// Ensure addSteps is updated to accept ImmutableNode
						addSteps(definition, statements);
						statements.add(new String[] { "END", "", "" });
					}
				}
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}

			return statements;
		}

		//		private void addSteps(ImmutableNode defination, ArrayList<Object[]> statements) {
		//			for (Object o : defination.getChildren()) {
		//				Node stepNode = (Node) o;
		//				if (stepNode.getName().equalsIgnoreCase("STEP")) {
		//					String name = getAttribute(stepNode, "name", null);
		//					String inParams = getAttribute(stepNode, "params", "[]");
		//					if (!inParams.startsWith("[")) {
		//						Object[] params = new Object[] { toObject(inParams) };
		//						inParams = gson.toJson(params);
		//					}
		//					String outParams = getAttribute(stepNode, "result", "");
		//					statements.add(new String[] { name, inParams, outParams });
		//				}
		//			}
		//		}

		private void addSteps(ImmutableNode definition, ArrayList<Object[]> statements) {
			// 1. Use ImmutableNode for the loop
			for (ImmutableNode stepNode : definition.getChildren()) {

				// 2. Use getNodeName() instead of getName()
				if (stepNode.getNodeName().equalsIgnoreCase("STEP")) {

					// 3. Access attributes via the getAttributes() Map
					String name = (String) stepNode.getAttributes().get("name");

					Object paramsAttr = stepNode.getAttributes().get("params");
					String inParams = (paramsAttr != null) ? paramsAttr.toString() : "[]";

					if (!inParams.startsWith("[")) {
						Object[] params = new Object[] { toObject(inParams) };
						inParams = gson.toJson(params);
					}

					Object resultAttr = stepNode.getAttributes().get("result");
					String outParams = (resultAttr != null) ? resultAttr.toString() : "";

					statements.add(new String[] { name, inParams, outParams });
				}
			}
		}


		//		private Map<?, ?> getMetaData(ConfigurationNode defination) {
		//			Map<String, Object> metaData = new HashMap<String, Object>();
		//			for (Object obj : defination.getAttributes()) {
		//				Node node = (Node) obj;
		//				metaData.put(node.getName(), toObject((String) node.getValue()));
		//			}
		//			return metaData;
		//		}

		private Map<String, Object> getMetaData(ImmutableNode definition) {
			Map<String, Object> metaData = new HashMap<>();

			// In v2, getAttributes() returns Map<String, Object> directly
			Map<String, Object> attributes = definition.getAttributes();

			for (Map.Entry<String, Object> entry : attributes.entrySet()) {
				// Map the attribute name and convert the value
				metaData.put(entry.getKey(), toObject(String.valueOf(entry.getValue())));
			}

			return metaData;
		}


		//		private String getAttribute(Node node, String attrName, String defValue) {
		//			List<?> attribute = node.getAttributes(attrName);
		//			if (attribute.size() > 0) {
		//				return (String) ((Node) attribute.get(0)).getValue();
		//			}
		//
		//			if (null == defValue) {
		//				throw new RuntimeException("Missing attribute " + attrName + " in " + node.getName() + " xml element");
		//			}
		//			return defValue;
		//
		//		}
		//	}

		private String getAttribute(ImmutableNode node, String attrName, String defValue) {
			// 1. Get the attribute directly from the Map
			Object value = node.getAttributes().get(attrName);

			if (value != null) {
				return String.valueOf(value);
			}

			// 2. Handle the missing attribute case
			if (null == defValue) {
				throw new RuntimeException("Missing attribute " + attrName + " in " + node.getNodeName() + " xml element");
			}

			return defValue;
		}


		private Object toObject(String s) {
			if (StringUtil.isNumeric(s))
				return gson.fromJson(s, Long.class);
			if (StringUtil.startsWith(s, "[")) {
				return (Object) gson.fromJson(s, List.class);
			}
			if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"))
				return Boolean.valueOf(s);
			return s;
		}

	}
}
