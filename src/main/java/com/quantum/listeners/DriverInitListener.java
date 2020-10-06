package com.quantum.listeners;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.SkipException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Function;
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
		String finalBaseURL = String .format(baseUrl, remoteServer.substring(remoteServer.indexOf("https://"), remoteServer.indexOf(".perfectomobile")));
		String commonDevicesUrl = String.format("%s%s%s", finalBaseURL, credentialString, optionalParameters);

		System.out.println(commonDevicesUrl);

		if (!isDeviceCombinationValid(commonDevicesUrl)) {
			return false;
		}

		String availableDevicesUrl = String.format("%s%s", commonDevicesUrl, "&inUse=false");

		System.out.println(availableDevicesUrl);
		
		int deviceAvailableTimeout = ConfigurationManager.getBundle().getInt("device.available.api.check.timeout", 30);
		int deviceAvailableCheckPollTime = ConfigurationManager.getBundle().getInt("device.available.api.check.poll.time", 10);
		

		FluentWait<String> deviceWait = new FluentWait<String>(availableDevicesUrl);
		deviceWait.withTimeout(deviceAvailableTimeout, TimeUnit.SECONDS);
		deviceWait.pollingEvery(deviceAvailableCheckPollTime, TimeUnit.SECONDS);

		return deviceWait.until(new Function<String, Boolean>() {

			@Override
			public Boolean apply(String apiUrl) {

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

		});
	}

	public void beforeInitialize(Capabilities desiredCapabilities) {

		Device device = new Device();
		Map<String, ?> capabilities = desiredCapabilities.asMap();

		Map<String, String> credentials = new HashMap<>();

		if (capabilities.get("securityToken") != null) {
			credentials.put("securityToken", capabilities.get("securityToken").toString());
		}

		if (capabilities.get("user") != null) {
			credentials.put("user", capabilities.get("user").toString());
		}

		if (capabilities.get("password") != null) {
			credentials.put("password", capabilities.get("password").toString());
		}

		if (capabilities.get("model") != null) {
			device.addAttribute("model", capabilities.get("model").toString());
		}

		if (capabilities.get("deviceId") != null) {
			device.addAttribute("deviceId", capabilities.get("deviceId").toString());
		}

		if (capabilities.get("deviceName") != null) {
			device.addAttribute("deviceId", capabilities.get("deviceName").toString());
		}

		if (capabilities.get("os") != null) {
			device.addAttribute("os", capabilities.get("os").toString());
		}

		if (capabilities.get("osVersion") != null) {
			device.addAttribute("osVersion", capabilities.get("osVersion").toString());
		}

//		System.out.println(desiredCapabilities);

//		waitForDeviceAvailable(device,credentials);
		if (!ConfigurationManager.getBundle().getBoolean("device_not_available", false)) {
			try {
				boolean deviceAvailableFlag = waitForDeviceAvailable(device, credentials);
				System.out.println("Device availability flag - " + deviceAvailableFlag);
				if (!deviceAvailableFlag) {
					throw new SkipException("Device not available");
				}
			} catch (Exception e) {
				try {

					ConfigurationManager.getBundle().setProperty("device_not_available", true);
					new RemoteWebDriver(new URL(ConfigurationManager.getBundle().getString("remote.server")),
							desiredCapabilities);

				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				throw e;
			}
		} else {
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
