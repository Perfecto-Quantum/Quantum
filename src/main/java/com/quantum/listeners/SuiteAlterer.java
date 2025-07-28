package com.quantum.listeners;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

public class SuiteAlterer implements IAlterSuiteListener {
	
	private final Log logger = LogFactoryImpl.getLog(SuiteAlterer.class);

	@Override
	public void alter(List<XmlSuite> suites) {

		XmlSuite originalSuite = suites.get(0);
		XmlSuite newSuite = originalSuite.shallowCopy();
//		List<XmlSuite> tempSuite = new ArrayList<XmlSuite>();
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

					Map<String, String> testPars = originalTest.get(z).getAllParameters();

					Map<String, String> testParams = new HashMap<String, String>();
					Iterator<Entry<String, String>> it2 = testPars.entrySet().iterator();
					while (it2.hasNext()) {
						Map.Entry<String, String> pair = it2.next();

						testParams.put(pair.getKey().toString(), pair.getValue().toString());
					}

					// params
					Map<String, String> temp = parameterList.get(r);
					Iterator<Entry<String, String>> parameterListIterator = temp.entrySet().iterator();
					
					// include list
					List<String> testIncludeList = getTestIncludeGroupList(originalTest.get(z));
					List<String> includeList = new ArrayList<>();
					Iterator<String> includeGroupIterator = testIncludeList.iterator();
					
					while (includeGroupIterator.hasNext()) {
						includeList.add(includeGroupIterator.next());
					}

					String key;
					while (parameterListIterator.hasNext()) {
						Map.Entry<String, String> pair = parameterListIterator.next();

						key = pair.getKey();
						switch (key) {
							case "includedGroups":
								String[] includeArray = pair.getValue().toString().split(",");
								for (int j = 0; j < includeArray.length; j++) {
									includeList.add(includeArray[j]);
								}
								break;
							case "excludedGroups":
								break;
							case "thread-count":
								threadCount = "".equals(pair.getValue())? 0: Integer.parseInt(pair.getValue());
								break;
							default:
								testParams.put(key, pair.getValue());
						}

//						if (!key.equals("includedGroups") && !key.equals("excludedGroups")
//								&& !key.equals("thread-count")) {
//							
//						}
//
//						if (key.equals("thread-count")) {
//							if (pair.getValue().toString() != "") {
//								threadCount = Integer.parseInt(pair.getValue().toString());
//							} else {
//								threadCount = 0;
//							}
//						}
					}

//					// include list
//					List<String> testIncludeList = getTestIncludeGroupList(originalTest.get(z));
//					List<String> includeList = new ArrayList<>();
//					Iterator<String> it3 = testIncludeList.iterator();
//					while (it3.hasNext()) {
//						includeList.add(it3.next().toString());
//					}

					temp = parameterList.get(r);
					parameterListIterator = temp.entrySet().iterator();
					while (parameterListIterator.hasNext()) {
						Map.Entry<String, String> pair = parameterListIterator.next();
						key = pair.getKey();
//						if (pair.getKey().toString().equals("includedGroups")) {
						
						if("includedGroups".equals(key)) {
							String[] includeArray = pair.getValue().toString().split(",");
							for (int j = 0; j < includeArray.length; j++) {
								includeList.add(includeArray[j]);
							}
						}
					}

					// exclude list
					List<String> testExcludeList = getTestExcludeGroupList(originalTest.get(z));
					List<String> excludeList = new ArrayList<>();
					Iterator<String> it4 = testExcludeList.iterator();
					while (it4.hasNext()) {

						excludeList.add(it4.next().toString());
					}

					parameterListIterator = temp.entrySet().iterator();
					while (parameterListIterator.hasNext()) {
						Map.Entry<String, String> pair = parameterListIterator.next();
						if (pair.getKey().toString().equals("excludedGroups")) {
							String[] excludeArray = pair.getValue().toString().split(",");
							for (int j = 0; j < excludeArray.length; j++) {
								excludeList.add(excludeArray[j]);
							}
						}
					}

					// finalize tests
					List<XmlClass> testClass = originalTest.get(z).getClasses();
//					List<XmlClass> testClass1 = new ArrayList<XmlClass>();

					newTest.setClasses(testClass);

					newTest.setName(originalTest.get(z).getName().split(":")[0] + " :  " + (r + 1));

					newTest.setIncludedGroups(includeList);

					newTest.setExcludedGroups(excludeList);

					if (threadCount != 0) {
						newTest.setThreadCount(threadCount);
					} else {
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
//				List<XmlClass> testClass1 = new ArrayList<XmlClass>();

				newTest.setClasses(testClass);

				Map<String, String> testPars = originalTest.get(z).getAllParameters();
				Map<String, String> testParams = new HashMap<String, String>();
				Iterator<Entry<String, String>> it2 = testPars.entrySet().iterator();
				while (it2.hasNext()) {
					Map.Entry<String, String> pair = it2.next();
					testParams.put(pair.getKey().toString(), pair.getValue().toString());
				}

				newTest.setParameters(testParams);

				finalTest.add(newTest);
				inList = false;
			}

		}
		originalSuite.setTests(finalTest);

		logger.debug("Alter method : " + originalSuite.toXml());

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

		Iterator<String> it2 = testPars.iterator();
		while (it2.hasNext()) {

			hasParams.add(it2.next().toString());
		}

		return hasParams;

	}

	public List<String> getTestExcludeGroupList(XmlTest originalTest) {
		List<String> hasParams = new ArrayList<String>();

		List<String> testPars = originalTest.getExcludedGroups();

		Iterator<String> it2 = testPars.iterator();
		while (it2.hasNext()) {

			hasParams.add(it2.next().toString());
		}

		return hasParams;

	}

	public List<String> getTestNameList(List<XmlTest> originalTest) {
		List<String> hasParams = new ArrayList<String>();
		for (int z = 0; z < originalTest.size(); z++) {

			Map<String, String> testPars = originalTest.get(z).getAllParameters();

			Iterator<Entry<String, String>> it2 = testPars.entrySet().iterator();
			while (it2.hasNext()) {
				Map.Entry<String, String> pair = it2.next();
				if (pair.getKey().toString().equals("csvParams")) {
					hasParams.add(originalTest.get(z).getName());
				}
			}

		}
		return hasParams;

	}

	public List<Map<String, String>> getParameterList(XmlTest test) throws IOException {
		Map<String, String> testPars = test.getAllParameters();

		Iterator<Entry<String, String>> it2 = testPars.entrySet().iterator();
		String path = null;
		while (it2.hasNext()) {
			Map.Entry<String, String> pair = it2.next();
			if (pair.getKey().toString().equals("csvParams")) {
				path = pair.getValue().toString();
				break;
			}
		}

		return getArrayFromCsv(new File(path));
	}

}