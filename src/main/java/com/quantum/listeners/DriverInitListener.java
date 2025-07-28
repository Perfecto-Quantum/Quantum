package com.quantum.listeners;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
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
import org.testng.SkipException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Function;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;

public class DriverInitListener extends QAFWebDriverCommandAdapter {
	
	private static final Log logger = LogFactoryImpl.getLog(DriverInitListener.class);

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
				logger.error(e);
			}

		}
	
		logger.debug("URL Parameters : " + url);
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
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

		logger.debug("Wait for device - Common Device URL - " + commonDevicesUrl);

		String availableDevicesUrl = String.format("%s%s", commonDevicesUrl, "&inUse=false");

		logger.debug("Wait for device - Available Device URL - " + availableDevicesUrl);

		int deviceAvailableTimeout = ConfigurationManager.getBundle().getInt("device.available.api.check.timeout", 15);
		int deviceAvailableCheckPollTime = ConfigurationManager.getBundle()
				.getInt("device.available.api.check.poll.time", 3);

		Wait wait = new FluentWait<String>(availableDevicesUrl).withTimeout(Duration.ofSeconds(deviceAvailableTimeout))
				.pollingEvery(Duration.ofSeconds(deviceAvailableCheckPollTime)).ignoring(Exception.class);
//		FluentWait<String> deviceWait = new FluentWait<String>(availableDevicesUrl);
//		deviceWait.withTimeout(deviceAvailableTimeout, TimeUnit.SECONDS);
//		deviceWait.pollingEvery(deviceAvailableCheckPollTime, TimeUnit.SECONDS);
		try {
			return (boolean) wait.until(new Function<String, Boolean>() {

				@Override
				public Boolean apply(String apiUrl) {
					logger.debug("Polling for device availability");
					if (!isDeviceCombinationValid(commonDevicesUrl)) {
						logger.debug("Polled for device combination check and it failed. Returning false");
						return false;
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
								logger.debug("Device Available API Response"
										+ document.getElementsByTagName("handset").getLength());
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
				logger.debug("Value - " + ConfigurationManager.getBundle().getString(key));
				if (ConfigurationManager.getBundle().getString(key).contains("-")
						|| ConfigurationManager.getBundle().getString(key).contains("'useVirtualDevice':true")) {
					logger.debug("Executing on VD or Shared Session, therefore skipping DriverInitListener");
					skipDriverInitList = true;
				}
			}
		}
		
		if (!skipDriverInitList) {
			Device device = new Device();
			Map<String, ?> capabilities = desiredCapabilities.asMap();

			Map<String, String> credentials = new HashMap<>();

			if (capabilities.get("perfecto:securityToken") != null) {
				credentials.put("securityToken", capabilities.get("perfecto:securityToken").toString());
			}

			if (capabilities.get("perfecto:model") != null) {
				device.addAttribute("model", capabilities.get("perfecto:model").toString());
			}

			if (capabilities.get("perfecto:deviceId") != null) {
				device.addAttribute("deviceId", capabilities.get("perfecto:deviceId").toString());
			}

			if (capabilities.get("perfecto:deviceName") != null) {
				device.addAttribute("deviceId", capabilities.get("perfecto:deviceName").toString());
			}

			if (capabilities.get("perfecto:os") != null) {
				device.addAttribute("perfecto:os", capabilities.get("perfecto:os").toString());
			}

			if (capabilities.get("perfecto:osVersion") != null) {
				device.addAttribute("osVersion", capabilities.get("perfecto:osVersion").toString());
			}
			if (!ConfigurationManager.getBundle().getBoolean("device_not_available", false)) {
				try {
					boolean deviceAvailableFlag = waitForDeviceAvailable(device, credentials);
					logger.debug("Device availability flag - " + deviceAvailableFlag);
					if (!deviceAvailableFlag) {
						logger.debug("Throwing the Device Not Available exception");
						throw new SkipException("Device not available");
					}
				} catch (Exception e) {
					try {
						if (!getBundle().getString("ScenarioExecution", "FromListener")
								.equalsIgnoreCase("FromListener")) {
							logger.debug("Creating driver for report result"
									+ getBundle().getString("ScenarioExecution", "FromListener"));
							ConfigurationManager.getBundle().setProperty("device_not_available", true);
							((DesiredCapabilities) desiredCapabilities).setCapability("perfecto:scriptName",
									getBundle().getString("ScenarioExecution", "FromListener"));
							getBundle().setProperty("ScenarioExecution", "FromListener");
							WebDriver driver = new RemoteWebDriver(
									new URL(ConfigurationManager.getBundle().getString("remote.server")),
									desiredCapabilities);
							driver.quit();

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
}
