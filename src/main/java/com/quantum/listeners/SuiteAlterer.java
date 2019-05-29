package com.quantum.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.IAlterSuiteListener;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.qmetry.qaf.automation.core.ConfigurationManager;

public class SuiteAlterer implements IAlterSuiteListener {

	@Override
	public void alter(List<XmlSuite> suites) {

		XmlSuite originalSuite = suites.get(0);
		XmlSuite newSuite = originalSuite.shallowCopy();
		List<XmlSuite> tempSuite = new ArrayList<XmlSuite>();
		boolean inList = false;
		List<XmlTest> originalTest = originalSuite.getTests();
		List<XmlTest> finalTest = new ArrayList<XmlTest>();
		int threadCount = 0;
		List<String> testParamsList = getTestNameList(originalTest);

		List<Map<String, String>> parameterList = null;

		List<String> newListener = newSuite.getListeners();
		List<String> newListenerReplace = new ArrayList<String>();
		for (int i = 0; i < newListener.size(); i++) {
			if (!newListener.get(i).contains("SuiteAlt")) {
				newListenerReplace.add(newListener.get(i));
			}
		}
		originalSuite.setListeners(newListenerReplace);

		int u = 0;
		for (int z = 0; z < originalTest.size(); z++) {

			for (int b = 0; b < testParamsList.size(); b++) {
				if (testParamsList.get(b).equals(originalTest.get(z).getName())) {
					inList = true;
					break;
				}
			}

			if (z == 0) {
				u = originalTest.size();
			}

			if (u <= 0) {
				break;
			}

			if (u == (z)) {
				break;
			}

			if (inList) {
				try {
					parameterList = getParameterList(originalTest.get(z));
				} catch (IOException e) {
					e.printStackTrace();
				}

				for (int r = 0; r < parameterList.size(); r++) {
					XmlTest newTest = (XmlTest) originalTest.get(z).clone();

					Map<String, String> testPars = originalTest.get(z).getParameters();

					Map<String, String> testParams = new HashMap<String, String>();
					Iterator it2 = testPars.entrySet().iterator();
					while (it2.hasNext()) {
						Map.Entry pair = (Map.Entry) it2.next();

						testParams.put(pair.getKey().toString(), pair.getValue().toString());
					}

					// params
					Map<String, String> temp = parameterList.get(r);
					Iterator it = temp.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();
						if (!pair.getKey().toString().equals("includedGroups")
								&& !pair.getKey().toString().equals("excludedGroups")
								&& !pair.getKey().toString().equals("thread-count")) {
							testParams.put(pair.getKey().toString(), pair.getValue().toString());
						}

						if (pair.getKey().toString().equals("thread-count")) {
							if(pair.getValue().toString()!="")
							{
							threadCount = Integer.parseInt(pair.getValue().toString());
							}
							else
							{
								threadCount=0;
							}
						}
					}

					// include list
					List<String> testIncludeList = getTestIncludeGroupList(originalTest.get(z));
					List<String> includeList = new ArrayList<>();
					Iterator it3 = testIncludeList.iterator();
					while (it3.hasNext()) {

						includeList.add(it3.next().toString());
					}

					temp = parameterList.get(r);
					it = temp.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();
						if (pair.getKey().toString().equals("includedGroups")) {
							String[] includeArray = pair.getValue().toString().split(",");
							for (int j = 0; j < includeArray.length; j++) {
								includeList.add(includeArray[j]);
							}

						}
					}

					// exclude list
					List<String> testExcludeList = getTestExcludeGroupList(originalTest.get(z));
					List<String> excludeList = new ArrayList<>();
					Iterator it4 = testExcludeList.iterator();
					while (it4.hasNext()) {

						excludeList.add(it4.next().toString());
					}

					it = temp.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry) it.next();
						if (pair.getKey().toString().equals("excludedGroups")) {
							String[] excludeArray = pair.getValue().toString().split(",");
							for (int j = 0; j < excludeArray.length; j++) {
								excludeList.add(excludeArray[j]);
							}
						}
					}

					// finalize tests
					List<XmlClass> testClass = originalTest.get(z).getClasses();
					List<XmlClass> testClass1 = new ArrayList<XmlClass>();

					newTest.setClasses(testClass);

					newTest.setName(originalTest.get(z).getName().split(":")[0] + " :  " + (r + 1));

					newTest.setIncludedGroups(includeList);

					newTest.setExcludedGroups(excludeList);

					if (threadCount != 0) {
						newTest.setThreadCount(threadCount);
					}
					else
					{
						newTest.setThreadCount(originalTest.get(r).getThreadCount());
					}

					newTest.setParameters(testParams);

					finalTest.add(newTest);
				}
				inList = false;
			} else {
				XmlTest newTest = (XmlTest) originalTest.get(z).clone();

				newTest.setName(originalTest.get(z).getName().split(":")[0]);

				List<XmlClass> testClass = originalTest.get(z).getClasses();
				List<XmlClass> testClass1 = new ArrayList<XmlClass>();

				newTest.setClasses(testClass);

				Map<String, String> testPars = originalTest.get(z).getParameters();
				Map<String, String> testParams = new HashMap<String, String>();
				Iterator it2 = testPars.entrySet().iterator();
				while (it2.hasNext()) {
					Map.Entry pair = (Map.Entry) it2.next();
					testParams.put(pair.getKey().toString(), pair.getValue().toString());
				}

				newTest.setParameters(testParams);

				finalTest.add(newTest);
				inList = false;
			}

		}
		originalSuite.setTests(finalTest);

		System.out.println(originalSuite.toXml());

	}

	public List<Map<String, String>> getArrayFromCsv(File file) {
		List<Map<String, String>> lines = new ArrayList<>();
		List<String> headers = new ArrayList<>();
		Scanner inputStream;

		try {
			inputStream = new Scanner(file);

			if (inputStream.hasNext()) {
				String line = inputStream.nextLine();
				String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				for (int i = 0; i < values.length; i++) {

					StringBuilder sb = new StringBuilder(values[i]);

					if (Character.toString(sb.charAt(0)).equals("\"")) {
						sb.deleteCharAt(0);
					}
					if (Character.toString(sb.charAt(sb.length() - 1)).equals("\"")) {
						sb.deleteCharAt(sb.length() - 1);
					}

					headers.add(sb.toString());

				}

			}

			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			inputStream = new Scanner(file);
			inputStream.nextLine();
			while (inputStream.hasNext()) {
				String line = inputStream.nextLine();
				String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				Map<String, String> temp = new HashMap<>();
				for (int i = 0; i < values.length; i++) {

					StringBuilder sb = new StringBuilder(values[i]);

					if (!sb.toString().isEmpty()) {
						if (Character.toString(sb.charAt(0)).equals("\"")) {
							sb.deleteCharAt(0);
						}
						if (Character.toString(sb.charAt(sb.length() - 1)).equals("\"")) {
							sb.deleteCharAt(sb.length() - 1);
						}

						temp.put(headers.get(i), sb.toString());
					}
				}
				lines.add(temp);
			}

			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return lines;
	}

	public List<String> getTestIncludeGroupList(XmlTest originalTest) {
		List<String> hasParams = new ArrayList<String>();

		List<String> testPars = originalTest.getIncludedGroups();

		Iterator it2 = testPars.iterator();
		while (it2.hasNext()) {

			hasParams.add(it2.next().toString());
		}

		return hasParams;

	}

	public List<String> getTestExcludeGroupList(XmlTest originalTest) {
		List<String> hasParams = new ArrayList<String>();

		List<String> testPars = originalTest.getExcludedGroups();

		Iterator it2 = testPars.iterator();
		while (it2.hasNext()) {

			hasParams.add(it2.next().toString());
		}

		return hasParams;

	}

	public List<String> getTestNameList(List<XmlTest> originalTest) {
		List<String> hasParams = new ArrayList<String>();
		for (int z = 0; z < originalTest.size(); z++) {

			Map<String, String> testPars = originalTest.get(z).getParameters();

			Iterator it2 = testPars.entrySet().iterator();
			while (it2.hasNext()) {
				Map.Entry pair = (Map.Entry) it2.next();
				if (pair.getKey().toString().equals("csvParams")) {
					hasParams.add(originalTest.get(z).getName());
				}
			}

		}
		return hasParams;

	}

	public List<Map<String, String>> getParameterList(XmlTest test) throws IOException {
		Map<String, String> testPars = test.getParameters();

		Iterator it2 = testPars.entrySet().iterator();
		String path = null;
		while (it2.hasNext()) {
			Map.Entry pair = (Map.Entry) it2.next();
			if (pair.getKey().toString().equals("csvParams")) {
				path = pair.getValue().toString();
				break;
			}
		}

		return getArrayFromCsv(new File(path));
	}

}