/*******************************************************************************
 * QMetry Automation Framework provides a powerful and versatile platform to
 * author
 * Automated Test Cases in Behavior Driven, Keyword Driven or Code Driven
 * approach
 * Copyright 2016 Infostretch Corporation
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 * You should have received a copy of the GNU General Public License along with
 * this program in the name of LICENSE.txt in the root folder of the
 * distribution. If not, see https://opensource.org/licenses/gpl-3.0.html
 * See the NOTICE.TXT file in root folder of this source files distribution
 * for additional information regarding copyright ownership and licenses
 * of other open source software / files used by QMetry Automation Framework.
 * For any inquiry or need additional information, please contact
 * support-qaf@infostretch.com
 *******************************************************************************/

package com.quantum.listeners;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Function;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;
import com.quantum.listeners.DriverInitListener.Device;

public class DeviceIdsDriverListener extends QAFWebDriverCommandAdapter implements ITestListener {
	private static String baseUrl = "%s.perfectomobile.com/services/handsets?operation=list&status=connected";
//	public static Map<String, String> deviceIdCap = new HashMap<String, String>();
//	public static List<String> deviceAvailableList = new ArrayList<String>();
//	public static List<String> deviceAllocatedList = new ArrayList<String>();
//	public static List<String> deviceUnAvailableList = new ArrayList<String>();

	public static void main(String[] args) {
		String testString = "3133BB296C46FA2250362A227BA462A56ED11A45,xyz";
		System.out.println(testString.contains(","));
		System.out.println(testString.split(",")[0]);
		
	}

	@Override
	public void beforeInitialize(Capabilities desiredCapabilities) {

//		((DesiredCapabilities) desiredCapabilities).setCapability("captureHAR", true);
		Map<String, String> deviceIdCap = (Map<String, String>)ConfigurationManager.getBundle().getProperty("deviceIdCap");
		synchronized (deviceIdCap) {
//			 System.out.println(Thread.currentThread().getId());
//			 for(String key : deviceIdCap.keySet()) {
//				 System.out.println("Key = " + key + " Value - " + deviceIdCap.get(key));
//			 }
			String threadPlatform = desiredCapabilities.getCapability("platformName").toString();
			String threadDeviceID = desiredCapabilities.getCapability("deviceName").toString();
			if (threadDeviceID.contains(",")) {
				for (String key : deviceIdCap.keySet()) {
					ConfigurationManager.getBundle().setProperty("deviceIdKey", key);
					if (key.contains(threadPlatform) && key.contains(threadDeviceID)) {
						try {
							Iterator<String> itr = ConfigurationManager.getBundle().getKeys();
							while(itr.hasNext()) {
								String s = itr.next();
							}
							String delimeter = ConfigurationManager.getBundle().getPropertyValue("deviceId.delimeter");
							List<String> devices = new LinkedList<>(Arrays.asList(deviceIdCap.get(key).split(",")));
							for(String d : devices) {
								threadDeviceID = d;
								System.out.println("DeviceId allocated to thread - " + threadDeviceID);
								
//								deviceIdCap.put(key, deviceIdCap.get(key).split(",", 2)[1]);
								
								Device device = new Device();
								Map<String, ?> capabilities = desiredCapabilities.asMap();

								Map<String, String> credentials = new HashMap<>();

								if (capabilities.get("securityToken") != null) {
									credentials.put("securityToken", capabilities.get("securityToken").toString());
								}

								if (capabilities.get("model") != null) {
									device.addAttribute("model", capabilities.get("model").toString());
								}

								if (capabilities.get("deviceId") != null) {
									device.addAttribute("deviceId", capabilities.get("deviceId").toString());
								}

								if (capabilities.get("deviceName") != null) {
									device.addAttribute("deviceId", d);
								}

								if (capabilities.get("os") != null) {
									device.addAttribute("os", capabilities.get("os").toString());
								}

								if (capabilities.get("osVersion") != null) {
									device.addAttribute("osVersion", capabilities.get("osVersion").toString());
								}
								boolean isDeviceAvailable = waitForDeviceAvailable(device, credentials);
								if(isDeviceAvailable) {
									((DesiredCapabilities) desiredCapabilities).setCapability("deviceName", d);
									devices.remove(d);
									System.out.println("devicethread"+Thread.currentThread().hashCode());
									ConfigurationManager.getBundle().setProperty("devicethread"+Thread.currentThread().hashCode(), d);
									String deviceTHread = ConfigurationManager.getBundle().getString("devicethread"+Thread.currentThread().hashCode());
									//deviceAllocatedList.add(d);
									System.out.println("Device availabile:"+isDeviceAvailable);
									deviceIdCap.put(key, devices.toString().replace("[","").replace("]", ""));
									ConfigurationManager.getBundle().setProperty("deviceIdCap", deviceIdCap);
									break;
								}else {
//									deviceAvailableList.remove(d);
//									deviceUnAvailableList.add(d);
									continue;
								}
								
							}
							
						} catch (Exception e) {
							e.printStackTrace();
							deviceIdCap.put(key, "");
							ConfigurationManager.getBundle().setProperty("deviceIdCap", deviceIdCap);
							System.out.println("No More devices left for allocating!");
						}
						System.out.println(" Updated list of device Ids for parent thread " + deviceIdCap.get(key));
					}
				}
			}

		}

	}

	@Override
	public void onTestStart(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestSuccess(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestFailure(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestSkipped(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStart(ITestContext context) {
		Map<String, String> params = context.getCurrentXmlTest().getAllParameters();
		String deviceName = "";
		String platformName = "";
		for (String key : params.keySet()) {
			if (key.contains("deviceName")) {
				deviceName = params.get(key);
			}
			if (key.contains("platformName")) {
				platformName = params.get(key);
			}
		}
//		deviceAvailableList.addAll(Arrays.asList(deviceName.split(",")));
//		deviceIdCap.put(platformName + deviceName, deviceName);
		Map<String, String> deviceIdCap = new HashMap<String, String>();
		deviceIdCap.put(platformName + deviceName, deviceName);
		ConfigurationManager.getBundle().setProperty("deviceIdCap", deviceIdCap);
		if(params.containsKey("deviceId.delimeter")) {
			String s = params.get("deviceId.delimeter");
			ConfigurationManager.getBundle().setProperty("deviceId.delimeter", params.get("deviceId.delimeter"));
		}
	}

	@Override
	public void onFinish(ITestContext context) {
		// TODO Auto-generated method stub
		Map<String, String> deviceIdCap = (Map<String, String>)ConfigurationManager.getBundle().getProperty("deviceIdCap");
		System.out.println(deviceIdCap);
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

//		int deviceAvailableTimeout = ConfigurationManager.getBundle().getInt("device.available.api.check.timeout", 15);
//		int deviceAvailableCheckPollTime = ConfigurationManager.getBundle()
//				.getInt("device.available.api.check.poll.time", 3);

		Wait wait = new FluentWait<String>(availableDevicesUrl).withTimeout(1, TimeUnit.SECONDS)
				.pollingEvery(1, TimeUnit.SECONDS).ignoring(Exception.class);
		//		FluentWait<String> deviceWait = new FluentWait<String>(availableDevicesUrl);
		//		deviceWait.withTimeout(deviceAvailableTimeout, TimeUnit.SECONDS);
		//		deviceWait.pollingEvery(deviceAvailableCheckPollTime, TimeUnit.SECONDS);
		try {
			return (boolean) wait.until(new Function<String, Boolean>() {

				@Override
				public Boolean apply(String apiUrl) {
					//					System.out.println("Polling for device availability");
//					if (!isDeviceCombinationValid(commonDevicesUrl)) {
//						//						System.out.println("Polled for device combination check and it failed.");
//						return false;
//						//						return new Exception("Device Not Found");
//					}

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

}
