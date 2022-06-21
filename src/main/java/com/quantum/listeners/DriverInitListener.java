package com.quantum.listeners;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Function;
import com.perfecto.reportium.client.ReportiumClient;
import com.perfecto.reportium.client.ReportiumClientFactory;
import com.perfecto.reportium.model.CustomField;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.PerfectoExecutionContext;
import com.perfecto.reportium.model.Project;
import com.perfecto.reportium.model.PerfectoExecutionContext.PerfectoExecutionContextBuilder;
import com.perfecto.reportium.test.TestContext;
import com.perfecto.reportium.test.TestContext.Builder;
import com.perfecto.reportium.test.result.TestResultFactory;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;

public class DriverInitListener extends QAFWebDriverCommandAdapter {

	private static String baseUrl = "%s.perfectomobile.com/services/handsets?operation=list&status=connected";
	//	private static String getOptionalParameters(Device deviceInformation) {
	//		
	//		StringBuilder url = new StringBuilder();
	//		
	//		Map<String,String> deviceProperties = deviceInformation.getDeviceAttributes();
	//		
	//		Set<Entry<String,String>> entrySets = deviceProperties.entrySet();
	//		
	//		Iterator<Entry<String,String>> iterEntrySet = entrySets.iterator();
	//		
	//		Entry<String,String> entrySet;
	//		
	//		while(iterEntrySet.hasNext()) {
	//			entrySet = iterEntrySet.next();
	//			url = url.append("&");
	//			url = url.append(entrySet.getKey());
	//			url = url.append("=");
	//			url = url.append(entrySet.getValue());
	//		}
	//		
	//		return url.toString();
	//		
	//	}

	private static String getOptionalParameters(Device deviceInformation) {

		StringBuilder url = new StringBuilder();

		Map<String, String> deviceProperties = deviceInformation.getDeviceAttributes();

		Set<Entry<String, String>> entrySets = deviceProperties.entrySet();

		Iterator<Entry<String, String>> iterEntrySet = entrySets.iterator();

		Entry<String, String> entrySet;

		while (iterEntrySet.hasNext()) {
			entrySet = iterEntrySet.next();
			url = url.append("&");
			try {
				url = url.append(URLEncoder.encode(entrySet.getKey(), "UTF-8").replaceAll("\\++", "%20"));
				url = url.append("=");
				url = url.append(URLEncoder.encode(entrySet.getValue(), "UTF-8").replaceAll("\\++", "%20"));

			} catch (UnsupportedEncodingException e) {
			}

		}
		System.out.println("*************************");
		System.out.println(url);
		//
		//        try {
		//            return URLEncoder.encode(url.toString(), "UTF-8").replaceAll("\\++", "%20") ;
		//        } catch (UnsupportedEncodingException e) {
		//            return url.toString();
		//        }
		return url.toString();

	}

	private static boolean isDeviceCombinationValid(String url) {

		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		try {
			HttpResponse response = httpClient.execute(request);

			if (response.getStatusLine().getStatusCode() == 200) {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				try {
					builder = factory.newDocumentBuilder();
					Document document = builder.parse(new InputSource(new StringReader(result.toString())));
					return document.getElementsByTagName("handset").getLength() > 0;
				} catch (Exception e) {
					return false;
				}

			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
	public static boolean waitForDeviceAvailable(Device deviceInformation, Map<String, String> credentials)
			throws TimeoutException {

		if (deviceInformation == null)
			return true;

		String optionalParameters = getOptionalParameters(deviceInformation);

		String credentialString = "";

		try {

			if (credentials.containsKey("securityToken")) {
				credentialString = String.format("&securityToken=%s",
						URLEncoder.encode(credentials.get("securityToken"), "UTF-8"));
			} else {
				credentialString = String.format("&user=%s&password=%s",
						URLEncoder.encode(credentials.getOrDefault("user", ""), "UTF-8"),
						URLEncoder.encode(credentials.getOrDefault("password", ""), "UTF-8"));
			}
		} catch (Exception e) {
		}
		String remoteServer = ConfigurationManager.getBundle().getString("remote.server");
		String finalBaseURL = String.format(baseUrl,
				remoteServer.substring(remoteServer.indexOf("https://"), remoteServer.indexOf(".perfectomobile")));
		String commonDevicesUrl = String.format("%s%s%s", finalBaseURL, credentialString, optionalParameters);

		System.out.println(commonDevicesUrl);

		String availableDevicesUrl = String.format("%s%s", commonDevicesUrl, "&inUse=false");

		System.out.println(availableDevicesUrl);

		int deviceAvailableTimeout = ConfigurationManager.getBundle().getInt("device.available.api.check.timeout", 15);
		int deviceAvailableCheckPollTime = ConfigurationManager.getBundle()
				.getInt("device.available.api.check.poll.time", 3);

		Wait wait = new FluentWait<String>(availableDevicesUrl).withTimeout(deviceAvailableTimeout, TimeUnit.SECONDS)
				.pollingEvery(deviceAvailableCheckPollTime, TimeUnit.SECONDS).ignoring(Exception.class);
		//		FluentWait<String> deviceWait = new FluentWait<String>(availableDevicesUrl);
		//		deviceWait.withTimeout(deviceAvailableTimeout, TimeUnit.SECONDS);
		//		deviceWait.pollingEvery(deviceAvailableCheckPollTime, TimeUnit.SECONDS);
		try {
			return (boolean) wait.until(new Function<String, Boolean>() {

				@Override
				public Boolean apply(String apiUrl) {
					//					System.out.println("Polling for device availability");
					if (!isDeviceCombinationValid(commonDevicesUrl)) {
						//						System.out.println("Polled for device combination check and it failed.");
						return false;
						//						return new Exception("Device Not Found");
					}

					HttpClient httpClient = HttpClientBuilder.create().build();
					HttpGet request = new HttpGet(apiUrl);
					try {
						HttpResponse response = httpClient.execute(request);

						if (response.getStatusLine().getStatusCode() == 200) {
							BufferedReader rd = new BufferedReader(
									new InputStreamReader(response.getEntity().getContent()));

							StringBuffer result = new StringBuffer();
							String line = "";
							while ((line = rd.readLine()) != null) {
								result.append(line);
							}

							DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
							DocumentBuilder builder;
							try {
								builder = factory.newDocumentBuilder();
								Document document = builder.parse(new InputSource(new StringReader(result.toString())));
								//								System.out.println("Device Available API Response"
								//										+ document.getElementsByTagName("handset").getLength());
								return document.getElementsByTagName("handset").getLength() > 0;
							} catch (Exception e) {
								throw new Exception("Device Not Available");
							}

						} else {
							throw new Exception("Device Not Available");
						}
					} catch (Exception e) {
						//						throw new Exception("Device Not Available");
						return false;
					}
				}

			});
		} catch (Exception e) {
			return false;
		}

	}

	@SuppressWarnings("unchecked")
	public void beforeInitialize(Capabilities desiredCapabilities) {
		boolean skipDriverInitList = false;
		Iterator<String> it = ConfigurationManager.getBundle().getKeys();
		while (it.hasNext()) {
			String key = it.next();
			if (key.contains("capabilities.deviceSessionId") || key.contains("additional.capabilities")) {
				System.out.println("Value - " + ConfigurationManager.getBundle().getString(key));
				if (ConfigurationManager.getBundle().getString(key).contains("-")
						|| ConfigurationManager.getBundle().getString(key).contains("'useVirtualDevice':true")) {
					System.out.println("Executing on VD or Shared Session, therefore skipping DriverInitListener");
					skipDriverInitList = true;
				}
			}
		}
		if (!skipDriverInitList) {
			Device device = new Device();
			Map<String, ?> capabilities = desiredCapabilities.asMap();

			Map<String, String> credentials = new HashMap<>();

			if (capabilities.get("securityToken") != null) {
				credentials.put("securityToken", capabilities.get("securityToken").toString());
			}

			if (capabilities.get("model") != null && String.valueOf(capabilities.get("model"))!="") {
				device.addAttribute("model", capabilities.get("model").toString());
			}

			if (capabilities.get("deviceId") != null && String.valueOf(capabilities.get("deviceId"))!="") {
				device.addAttribute("deviceId", capabilities.get("deviceId").toString());
			}
			
			if (capabilities.get("deviceName") != null && !String.valueOf(capabilities.get("deviceName")).equals("")) {
				device.addAttribute("deviceId", capabilities.get("deviceName").toString());
			}

			if (capabilities.get("os") != null && String.valueOf(capabilities.get("os"))!="") {
				device.addAttribute("os", capabilities.get("os").toString());
			}

			if (capabilities.get("osVersion") != null && String.valueOf(capabilities.get("osVersion"))!= "") {
				device.addAttribute("osVersion", capabilities.get("osVersion").toString());
			}
			if (!ConfigurationManager.getBundle().getBoolean("device_not_available", false)) {
				try {
					boolean deviceAvailableFlag = waitForDeviceAvailable(device, credentials);
					System.out.println("Device availability flag - " + deviceAvailableFlag);
					if (!deviceAvailableFlag) {
						System.out.println("Throwing the Device Not Available exception");
						throw new SkipException("Device not available");
					}
				} catch (Exception e) {
					try {
						System.out.println("Device not found. Checking scenarioexecution condition - "+getBundle().getString("ScenarioExecution", "FromListener")
								.equalsIgnoreCase("FromListener"));
						if (!getBundle().getString("ScenarioExecution", "FromListener")
								.equalsIgnoreCase("FromListener")) {
							HashMap<String, WebDriver> driverList = (HashMap<String, WebDriver>)getBundle().getProperty("mydriverList");
							if(driverList!=null &&  driverList.size()>0) {
								PerfectoExecutionContextBuilder perfectoExecutionContextBuilder = new PerfectoExecutionContext.PerfectoExecutionContextBuilder();
								ConfigurationManager.getBundle().setProperty("device_not_available", true);
								for (String driverName : driverList.keySet()) {

									perfectoExecutionContextBuilder.withWebDriver(driverList.get(driverName), driverName);
								}
								//								ArrayList<String> groupsFinal = (ArrayList<String>) getBundle().getProperty("tagAndCustomFields");
								//								String tagAndCustomFields = "";
								//								if (null != groupsFinal && groupsFinal.size() > 0) {
								//									tagAndCustomFields
								//											+=(groupsFinal.toString().replace('[', ' ').replace(']', ' ').split(","));
								//								}

								ITestResult testResult = (ITestResult)getBundle().getProperty("testNGITestListener");

								String[] groups = testResult.getMethod().getGroups();
								ArrayList<String> groupsFinal = new ArrayList<String>();

								ArrayList<CustomField> cfc = new ArrayList<CustomField>();
								for (String string : groups) {
									groupsFinal.add(string);
								}
								
								for(String s : getBundle().getPropertyValue("allTags").split(",")) {
									groupsFinal.add(s);
								}
								String[] allTags = null;
								if (groupsFinal.size() > 0) {
									allTags = groupsFinal.toString().replace('[', ' ').replace(']', ' ').split(",");
								}
								String dataSet = "";
								if(!getBundle().getString("ScenarioExecution", "FromListener").contains("[Data") && null != getBundle().getProperty("testNGITestListener")) {
									dataSet = getDataDrivenText((ITestResult)getBundle().getProperty("testNGITestListener"));
								}

								ReportiumClient reportiumClient = new ReportiumClientFactory()
										.createPerfectoReportiumClient(perfectoExecutionContextBuilder.build());
								TestContext testContext = new TestContext.Builder().withTestExecutionTags(allTags).build();
								reportiumClient.testStart(getBundle().getString("ScenarioExecution", "FromListener")+dataSet, testContext);

								reportiumClient.testStop(TestResultFactory.createFailure(e));
								getBundle().setProperty("mydriverList",null);
								getBundle().setProperty("perfecto.report.client",null);
								System.out.println("Reportium setup for device skipped is done.");
							}
							else 
							{
								System.out.println("Creating driver for report result : "
										+ getBundle().getString("ScenarioExecution", "FromListener"));
								ConfigurationManager.getBundle().setProperty("device_not_available", true);

								ArrayList<String> groupsFinal = (ArrayList<String>) getBundle().getProperty("tagAndCustomFields");
								if(null == groupsFinal) {
									groupsFinal = new ArrayList<String>();
								}
								ITestResult testResult = (ITestResult)getBundle().getProperty("testNGITestListener");
								String[] groups = testResult.getMethod().getGroups();
								for (String string : groups) {
									groupsFinal.add(string);
								}
								
								for(String s : getBundle().getPropertyValue("allTags").split(",")) {
									groupsFinal.add(s);
								}
								
								String[] allTags = null;
								if (groupsFinal.size() > 0) {
									allTags = groupsFinal.toString().replace('[', ' ').replace(']', ' ').split(",");
								}
								
								String dataSet = "";
								if(!getBundle().getString("ScenarioExecution", "FromListener").contains("[Data") && null != getBundle().getProperty("testNGITestListener")) {
									dataSet = getDataDrivenText((ITestResult)getBundle().getProperty("testNGITestListener"));
								}
								
								((DesiredCapabilities) desiredCapabilities).setCapability("scriptName",
										getBundle().getString("ScenarioExecution", "FromListener")+dataSet);
								((DesiredCapabilities) desiredCapabilities).setCapability("report.tags",allTags);
								//			
								((DesiredCapabilities) desiredCapabilities).setCapability("report.jobName",getBundle().getString("JOB_NAME", System.getProperty("reportium-job-name")));
								((DesiredCapabilities) desiredCapabilities).setCapability("report.jobNumber",getBundle().getInt("BUILD_NUMBER",
										System.getProperty("reportium-job-number") == null ? 0
												: Integer.parseInt(System.getProperty("reportium-job-number"))));
								((DesiredCapabilities) desiredCapabilities).setCapability("report.jobBranch",System.getProperty("reportium-job-branch"));

								getBundle().setProperty("ScenarioExecution", "FromListener");
								WebDriver driver = new RemoteWebDriver(
										new URL(ConfigurationManager.getBundle().getString("remote.server")),
										desiredCapabilities);
								driver.quit();
								getBundle().setProperty("perfecto.report.client",null);
							}
							

						}
					} catch (MalformedURLException e1) {
						e1.printStackTrace();
					}
					throw e;
				}
			} else {
				throw new SkipException("Device not available");
			}
		}
	}

	public static class Device {
		private Map<String, String> deviceAttributes;
		public Device() {
			this.deviceAttributes = new HashMap<>();
		}
		void addAttribute(String attributeName, String attributeValue) {
			deviceAttributes.put(attributeName, attributeValue);
		}
		public Map<String, String> getDeviceAttributes() {
			return this.deviceAttributes;
		}
		@Override
		public String toString() {
			return deviceAttributes.toString();
		}
	}

	private String getDataDrivenText(ITestResult testResult) {
		String result = "";
		if (testResult.getParameters().length > 0) {
			Map map = (Map)testResult.getParameters()[0];
			if (map.containsKey("recDescription")) {
				result = " [" + map.get("recDescription") + "]";
			} else if (map.containsKey("recId")) {
				result = " [" + map.get("recId") + "]";
			}
		}
		return result;
	}
}
