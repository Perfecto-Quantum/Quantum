package com.qmetry.qaf.automation.ui;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.safari.SafariOptions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.qmetry.qaf.automation.core.AutomationError;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.DriverFactory;
import com.qmetry.qaf.automation.core.LoggingBean;
import com.qmetry.qaf.automation.core.QAFListener;
import com.qmetry.qaf.automation.core.QAFTestBase.STBArgs;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.ui.selenium.webdriver.SeleniumDriverFactory;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandListener;
import com.qmetry.qaf.automation.util.StringUtil;
import com.quantum.utils.QuantumPatch;

import io.appium.java_client.remote.AppiumCommandExecutor;
import io.github.bonigarcia.wdm.WebDriverManager;

public class UiDriverFactory implements DriverFactory<UiDriver> {
	private static final Log logger = LogFactoryImpl.getLog(UiDriverFactory.class);

	private static final Map<String, String> ANDROID_CAP_MAP = new HashMap<String, String>();
	private static final Map<String, String> IOS_CAP_MAP = new HashMap<String, String>();

	static {
		ANDROID_CAP_MAP.put("platformName", "Android");
		ANDROID_CAP_MAP.put("browser", "");
		ANDROID_CAP_MAP.put("version", "");

		IOS_CAP_MAP.put("platformName", "iOS");
		IOS_CAP_MAP.put("browser", "");
		IOS_CAP_MAP.put("version", "");
	}
	private static final DesiredCapabilities ANDROID_BASE_CAPABILITIES = new DesiredCapabilities(ANDROID_CAP_MAP);

	private static final DesiredCapabilities IOS_BASE_CAPABILITIES = new DesiredCapabilities(IOS_CAP_MAP);

	/*
	 * (non-Javadoc)
	 *
	 * @see com.qmetry.qaf.automation.core.DriverFactory#get(java.lang.String[])
	 */
	@Override
	public UiDriver get(ArrayList<LoggingBean> commandLog, String[] stb) {
		WebDriverCommandLogger cmdLogger = new WebDriverCommandLogger(commandLog);
		String browser = STBArgs.browser_str.getFrom(stb);
		logger.info("Driver: " + browser);

		if (browser.toLowerCase().contains("driver") && !browser.startsWith("*")) {
			try {
				return getDriver(cmdLogger, stb);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return new SeleniumDriverFactory().getDriver(cmdLogger, stb);
	}

	@Override
	public void tearDown(UiDriver driver) {
		try {
			driver.stop();
			logger.info("UI-driver tear down complete...");
		} catch (Throwable t) {
			logger.error(t.getMessage());
		}
	}

	/**
	 * Utility method to get capability that will be used by factory to create
	 * driver object. It will not include any modification done by
	 * {@link QAFWebDriverCommandListener#beforeInitialize(Capabilities)}
	 *
	 * @param driverName - String driver name
	 * @return DesiredCapabilities object
	 */
	public static DesiredCapabilities getDesiredCapabilities(String driverName) {
		return Browsers.getBrowser(driverName).getDesiredCapabilities();
	}

	public static String[] checkAndStartServer(String... args) {
		if (!isServerRequired(args)) {
			return args;
		}
		if (isSeverRunning(STBArgs.sel_server.getFrom(args), Integer.parseInt(STBArgs.port.getFrom(args)))) {
			return args;
		}

		// override args values to default
		args = STBArgs.sel_server.set(STBArgs.sel_server.getDefaultVal(), args);
		if (isSeverRunning(STBArgs.sel_server.getFrom(args), Integer.parseInt(STBArgs.port.getFrom(args)))) {
			logger.info("Assigning server running on localhost");

			return args;
		}
		return args;
	}

	private static boolean isServerRequired(String... args) {
		String browser = STBArgs.browser_str.getFrom(args).toLowerCase();
		return browser.contains("*") || browser.contains("remote");
	}

	private static boolean isSeverRunning(String host, int port) {
		boolean isRunning = false;

		Socket socket = null;
		try {
			socket = new Socket(host, (port));
			isRunning = socket.isConnected();
		} catch (Exception exp) {
			logger.error("Error occured while checking Selenium : " + exp.getMessage());
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {

			}
		}
		return isRunning;
	}

	private static void beforeInitialize(Capabilities desiredCapabilities,
			Collection<QAFWebDriverCommandListener> listners) {
		if ((listners != null) && !listners.isEmpty()) {
			for (QAFWebDriverCommandListener listener : listners) {
				listener.beforeInitialize(desiredCapabilities);
			}
		}
	}

	private static void onInitializationFailure(Capabilities desiredCapabilities, Throwable e,
			Collection<QAFWebDriverCommandListener> listners) {
		if ((listners != null) && !listners.isEmpty()) {
			for (QAFWebDriverCommandListener listener : listners) {
				listener.onInitializationFailure(desiredCapabilities, e);
			}
		}

	}

	private static Collection<QAFWebDriverCommandListener> getDriverListeners() {
		LinkedHashSet<QAFWebDriverCommandListener> listners = new LinkedHashSet<QAFWebDriverCommandListener>();
		String[] clistners = ConfigurationManager.getBundle()
				.getStringArray(ApplicationProperties.WEBDRIVER_COMMAND_LISTENERS.key);

		for (String listenr : clistners) {
			try {
				QAFWebDriverCommandListener cls = (QAFWebDriverCommandListener) Class.forName(listenr).getConstructor()
						.newInstance();
				listners.add(cls);
			} catch (Exception e) {
				logger.error("Unable to register listener class " + listenr, e);
			}
		}
		clistners = ConfigurationManager.getBundle().getStringArray(ApplicationProperties.QAF_LISTENERS.key);
		for (String listener : clistners) {
			try {
				QAFListener cls = (QAFListener) Class.forName(listener).getConstructor().newInstance();
				if (QAFWebDriverCommandListener.class.isAssignableFrom(cls.getClass()))
					listners.add((QAFWebDriverCommandListener) cls);
			} catch (Exception e) {
				logger.error("Unable to register class as driver listener:  " + listener, e);
			}
		}
		return listners;
	}

	private static QAFExtendedWebDriver getDriver(WebDriverCommandLogger reporter, String... args) throws Exception {

		String b = STBArgs.browser_str.getFrom(args).toLowerCase();

		String seleniumGridUrl = null;

		String selServer = STBArgs.sel_server.getFrom(args);

		if (!"".equals(selServer)) {
			seleniumGridUrl = selServer.startsWith("http") ? selServer
					: String.format("http://%s:%s/wd/hub", selServer, STBArgs.port.getFrom(args));
		}

		// Added based on new http client
		System.setProperty("webdriver.http.factory", "jdk-http-client");

		Browsers browser = Browsers.getBrowser(b);
		loadDriverResouces(browser);

		DesiredCapabilities desiredCapabilities = browser.getDesiredCapabilities();

		Map<String, Object> desiredCapAsMap = desiredCapabilities.asMap();

		ConfigurationManager.getBundle().setProperty("driver.desiredCapabilities", desiredCapAsMap);

		QAFExtendedWebDriver driver = browser.getRemoteWebDriver(reporter, seleniumGridUrl, desiredCapabilities);

		if (null != driver) {
			Map<String, Object> actualCapabilities = driver.getCapabilities().asMap();
			ConfigurationManager.getBundle().setProperty("driver.actualCapabilities", actualCapabilities);
		}

		return driver;

	}

	private static void loadDriverResouces(Browsers browser) {
		String driverResourcesKey = String.format(ApplicationProperties.DRIVER_RESOURCES_FORMAT.key,
				browser.browserName);
		String driverResources = ConfigurationManager.getBundle().getString(driverResourcesKey, "");
		if (StringUtil.isNotBlank(driverResources)) {
			ConfigurationManager.addBundle(driverResources);
		}
	}

	public static void loadDriverResouces(String driverName) {
		Browsers browser = Browsers.getBrowser(driverName);
		loadDriverResouces(browser);
	}

	private static WebDriver getRemoteSeleniumDriver(Class<? extends WebDriver> of, Capabilities capabilities,
			String seleniumGridUrl) {

		int connTimeOutInMSecFromProp = ConfigurationManager.getBundle().getInt("connection.timeout.ms", 60_000);
		int readTimeOutInMSecFromProp = ConfigurationManager.getBundle().getInt("read.timeout.ms", 60_000);

		Duration connTimeOutInMSecs = Duration.ofMillis(connTimeOutInMSecFromProp);
		Duration readTimeOutInMSecs = Duration.ofMillis(readTimeOutInMSecFromProp);

		try {

			// Remote instance of Browser Execution
			ClientConfig config = ClientConfig.defaultConfig().connectionTimeout(connTimeOutInMSecs)
					.readTimeout(readTimeOutInMSecs);

			return RemoteWebDriver.builder().config(config).addAlternative(capabilities).address(seleniumGridUrl)
					.build();

		} catch (Exception e) {
			throw e;
		}
	}

	private static WebDriver getLocalSeleniumDriver(Class<? extends WebDriver> of, Capabilities capabilities) {
		try {

			logger.info("Local Driver initialization");

			Constructor<? extends WebDriver> constructor = null;

			String className = of.getName().toUpperCase();

			String beforeLocalDriverInitClass = ConfigurationManager.getBundle().getString("local.before.driver.init",
					"com.qmetry.qaf.automation.ui.DefaultBeforeLocalDriverInit");

			IBeforeLocalDriverInit initClass = (IBeforeLocalDriverInit) Class.forName(beforeLocalDriverInitClass)
					.getDeclaredConstructor().newInstance();
			
			AbstractDriverOptions<?> driverOptions = null;

			if (className.contains("CHROMEDRIVER")) {
				constructor = of.getConstructor(ChromeOptions.class);
			}

			if (className.contains("FIREFOXDRIVER")) {
				constructor = of.getConstructor(FirefoxOptions.class);
			}

			if (className.contains("EDGEDRIVER")) {
				constructor = of.getConstructor(EdgeOptions.class);
			}
			
			if(className.contains("SAFARIDRIVER")) {
				constructor = of.getConstructor(SafariOptions.class);
			}
			
			initClass.setUpBrowserExec(of);
			
			driverOptions = initClass.getDriverOptions(of, capabilities);

			return constructor.newInstance(driverOptions);
		} catch (Exception e) {
			throw (WebDriverException) e.getCause();
		}
	}

	private static WebDriver getAppiumDriverObject(Class<? extends WebDriver> of, Capabilities capabilities,
			String urlStr) {

		try {
			Constructor<? extends WebDriver> constructor = of.getConstructor(URL.class, Capabilities.class);
			return constructor.newInstance(new URL(urlStr), capabilities);
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			throw new WebDriverException(ex);
		}
	}

	private enum Browsers {

		chrome(new ChromeOptions(), ChromeDriver.class), edge(new EdgeOptions(), EdgeDriver.class),
		firefox(new FirefoxOptions(), FirefoxDriver.class),
		iexplorer(new InternetExplorerOptions(), InternetExplorerDriver.class),
		safari(new SafariOptions(), "org.openqa.selenium.safari.SafariDriver"),
//		opera(new OperaOptions(), "com.opera.core.systems.OperaDriver"),

		android(ANDROID_BASE_CAPABILITIES, "io.appium.java_client.android.AndroidDriver"),
		iOS(IOS_BASE_CAPABILITIES, "io.appium.java_client.ios.IOSDriver"),

//				new DesiredCapabilities("android", "", Platform.ANDROID), 
//				"io.appium.java_client.android.AndroidDriver"),//"org.openqa.selenium.android.AndroidDriver"),
//		iphone(new DesiredCapabilities("iPhone", "", Platform.MAC),
//				"io.appium.java_client.ios.IOSDriver"),//"org.openqa.selenium.iphone.IPhoneDriver"),
//		ipad(new DesiredCapabilities("iPad", "", Platform.MAC),"io.appium.java_client.ios.IOSDriver"), //"org.openqa.selenium.iphone.IPhoneDriver"),

		perfecto(new DesiredCapabilities()),

		/**
		 * can with assumption that you have set desired capabilities using property.
		 * This is to provide support for future drivers or custom drivers if any. You
		 * can provide driver class as capability : driver.class, for example :<br>
		 * driver.class=org.openqa.selenium.safari.SafariDriver
		 */
		other(new DesiredCapabilities());

		private DesiredCapabilities desiredCapabilities;

		// Replaced with getDriverCls and getDriverCls method call to fix Parallel
		// execution issues.
//		private Class<? extends WebDriver> driverCls = null;

		@SuppressWarnings("unchecked")
		private Class<? extends WebDriver> getDriverCls() {
			return (Class<? extends WebDriver>) ConfigurationManager.getBundle().getProperty("DriverCLS");
		}

		private void setDriverCls(Class<? extends WebDriver> driverCls) {
			ConfigurationManager.getBundle().setProperty("DriverCLS", driverCls);
		}

		private String browserName = name();

		private Browsers(Capabilities desiredCapabilities) {
			this.desiredCapabilities = new DesiredCapabilities(desiredCapabilities.asMap());
//			this.desiredCapabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
//			this.desiredCapabilities.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
			// this.desiredCapabilities.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS,
			// true);

		}

		@SuppressWarnings("unchecked")
		private Browsers(Capabilities desiredCapabilities, String drivercls) {
			this(desiredCapabilities);

			if (null == getDriverCls()) {
				// not overridden by extra capability
				try {
					setDriverCls((Class<? extends WebDriver>) Class.forName(drivercls));
				} catch (Exception e) {
					// throw new AutomationError(e);
				}
			}

		}

		private Browsers(Capabilities desiredCapabilities, Class<? extends WebDriver> driver) {
			this(desiredCapabilities);
			if (null == getDriverCls()) {
				// not overridden by extra capability
				setDriverCls(driver);
			}
		}

		private String getPlatformName(Map<String, Object> capabilities, Configuration config) {
			Object platformObject = capabilities.get("platformName");

			String platformName = null;

			if (null == platformObject) {

				platformObject = capabilities.get("perfecto:platformName");

				if (null != platformObject) {
					platformName = (String) platformObject;
				} else {
					platformObject = capabilities.get("perfecto:options");

					if (null != platformObject) {
						if (platformObject instanceof Map<?, ?>) {
							platformObject = capabilities.get("platformName");

							if (platformObject != null) {
								platformName = (String) platformObject;
							}
						}
					}
				}

			} else {
				platformName = (String) platformObject;
			}

			platformName = (null == platformName ? null : platformName.toUpperCase());

			if (null == platformName) {
				platformName = (String) config.getProperty("driverClass");
				platformName = (null == platformName ? "WEB-PLATFORM" : platformName.toUpperCase());
			}

			return platformName;
		}

		@SuppressWarnings("unchecked")
		private DesiredCapabilities getDesiredCapabilities() {

			Map<String, Object> capabilities = new HashMap<String, Object>(desiredCapabilities.asMap());
			Gson gson = new GsonBuilder().create();

			// capabilities provided for all driver
			Map<String, Object> extraCapabilities = gson
					.fromJson(ApplicationProperties.DRIVER_ADDITIONAL_CAPABILITIES.getStringVal("{}"), Map.class);
			capabilities.putAll(extraCapabilities);

			// individual capability property for all driver
			Configuration config = ConfigurationManager.getBundle()
					.subset(ApplicationProperties.DRIVER_CAPABILITY_PREFIX.key);
			capabilities.putAll(new ConfigurationMap(config));

			// #332 add default capabilities for standard driver
			if (!name().equalsIgnoreCase(other.name())) {
				String driverCapsKey = String.format(ApplicationProperties.DRIVER_ADDITIONAL_CAPABILITIES_FORMAT.key,
						name());
				extraCapabilities = gson.fromJson(ConfigurationManager.getBundle().getString(driverCapsKey, "{}"),
						Map.class);
				capabilities.putAll(extraCapabilities);
			}

			// capabilities specific to this driver
			String driverCapsKey = String.format(ApplicationProperties.DRIVER_ADDITIONAL_CAPABILITIES_FORMAT.key,
					browserName);
			extraCapabilities = gson.fromJson(ConfigurationManager.getBundle().getString(driverCapsKey, "{}"),
					Map.class);
			capabilities.putAll(extraCapabilities);

			// individual capability property with driver name prefix
			String driverCapKey = String.format(ApplicationProperties.DRIVER_CAPABILITY_PREFIX_FORMAT.key, browserName);
			config = ConfigurationManager.getBundle().subset(driverCapKey);
			capabilities.putAll(new ConfigurationMap(config));

			// ======== Patch for Appium 2.0 and Selenium 4 vendor specific prefix ========

			QuantumPatch quantumPatch = new QuantumPatch();

			String platformName = getPlatformName(capabilities, config);

			if (platformName.contains("ANDROID") || platformName.contains("IOS")) {
				quantumPatch.capabilitiesPatchAppium2(config, capabilities);
				checkForAutomationNameCapability(capabilities);
			} else {
				quantumPatch.capabilitiesPatchSelenium4(config, capabilities);
			}

			// ======== Patch for Appium 2.0 vendor specific prefix completes ========

			Object driverclass = config.getString("driverClass", null);

			// capabilities.get(ApplicationProperties.CAPABILITY_NAME_DRIVER_CLASS.key);
			if (null == driverclass) {// backward compatibility only
				driverclass = capabilities.get("driver.class");
			}
			if (null != driverclass) {
				try {
					setDriverCls((Class<? extends WebDriver>) Class.forName(String.valueOf(driverclass)));
				} catch (Exception e) {
					System.out.println("Error while setting Driver class : " + e.getMessage());
					// throw new AutomationError(e);
				}
			}

			for (String key : capabilities.keySet()) {
				Object value = capabilities.get(key);
				if (value instanceof String) {
					capabilities.put(key, ConfigurationManager.getBundle().getSubstitutor().replace(value));
				}
			}

//			if(ConfigurationUtils.isPerfectoExecution()) {
//				String quantumVersion = ConfigurationUtils.getQuantumVersion();
//
//				if (null != quantumVersion) {
//					
//					if(capabilities.containsKey("perfecto:options")) {
//						((Map<String,Object>)capabilities.get("perfecto:options")).put("quantumVersion", quantumVersion);
//					}else {
//						capabilities.put("perfecto:quantumVersion", quantumVersion);
//					}
//					
//				}
//			}

			return new DesiredCapabilities(capabilities);
		}

		private static Browsers getBrowser(String name) {
			for (Browsers browser : Browsers.values()) {
				if (name.contains(browser.name())) {
					browser.setBrowserName(name);
					return browser;
				}
			}
			Browsers b = Browsers.other;
			b.setBrowserName(name);
			return b;
		}

		private void setBrowserName(String name) {

			// remove driver and remote from name
			browserName = name.replaceAll("(?i)remote|driver", "");
		}

		private boolean doesRequireProxyConnection() {
			String proxyEnabledConnectionProvider = ConfigurationManager.getBundle()
					.getString("proxy.enabled.connection.provider", "");
			return !"".equals(proxyEnabledConnectionProvider);
		}

		private QAFExtendedWebDriver getRemoteWebDriver(WebDriverCommandLogger wdCommandLogger,
				String seleniumGridURLStr, DesiredCapabilities desiredCapabilities) throws Exception {

			if (doesRequireProxyConnection()) {
				return proxyConnectForNormalDriver(seleniumGridURLStr, wdCommandLogger, desiredCapabilities);
			} else {
				return standardConnect(wdCommandLogger, seleniumGridURLStr, desiredCapabilities);
			}

		}

		@SuppressWarnings("unused")
		@Deprecated
		private QAFExtendedWebDriver getRemoteWebDriver(String seleniumGridURLStr,
				WebDriverCommandLogger wdCommandLogger, DesiredCapabilities desiredCapabilities) throws Exception {

			if (doesRequireProxyConnection()) {
				return proxyConnect(seleniumGridURLStr, wdCommandLogger, desiredCapabilities);
			} else {
				return standardConnect(seleniumGridURLStr, wdCommandLogger);
			}

		}

		private void checkForAutomationNameCapability(Map<String, Object> desiredCapabilities) {

			Set<String> capabilityNames = desiredCapabilities.keySet();

			String automationName = "";
			for (String capName : capabilityNames) {

				if (capName.contains("automationName")) {
					automationName = desiredCapabilities.getOrDefault(capName, "").toString();
					break;
				}
			}

			if (automationName.isBlank()) {
				desiredCapabilities.put("appium:automationName", "Appium");
			}
		}

		// Following methods are used for Connecting with Selenium based drivers
		private QAFExtendedWebDriver standardConnect(WebDriverCommandLogger wdCommandLogger, String seleniumGridURLStr,
				DesiredCapabilities desiredCapabilities) {
			logger.info("Direct Driver Connect");

			Collection<QAFWebDriverCommandListener> driverListeners = UiDriverFactory.getDriverListeners();
			UiDriverFactory.beforeInitialize(desiredCapabilities, driverListeners);

			try {

				WebDriver webDriverObject = null;

				Class<? extends WebDriver> driverClass = getDriverCls();

				if (null != driverClass) {

					String className = driverClass.getName().toLowerCase();

					if (className.startsWith("io.appium")) {
						// Create Driver related to Appium.
						webDriverObject = getAppiumDriverObject(driverClass, desiredCapabilities, seleniumGridURLStr);
					} else {
						// Create Driver related to Selenium.
						if (null == seleniumGridURLStr) {
							// Local instance of Browser Execution
							webDriverObject = getLocalSeleniumDriver(driverClass, desiredCapabilities);
						} else {
							// Remote Selenium Driver
							webDriverObject = getRemoteSeleniumDriver(driverClass, desiredCapabilities,
									seleniumGridURLStr);
						}
					}
				} else {

					// Create Driver with RemoteWebDriver as we don't have specific webDriverObject
					// request
					webDriverObject = getRemoteSeleniumDriver(driverClass, desiredCapabilities, seleniumGridURLStr);
				}

				return new QAFExtendedWebDriver(webDriverObject, wdCommandLogger);
			} catch (Throwable e) {
				onInitializationFailure(desiredCapabilities, e, driverListeners);

				throw new AutomationError("Unable to Create Driver Instance for " + browserName + ": " + e.getMessage(),
						e);
			}
		}

		@SuppressWarnings("unused")
		@Deprecated
		private static void setUpDriverExecutable(Class<? extends WebDriver> driverClass) {
			try {
				if (ConfigurationManager.getBundle().getBoolean("manage.driver.executable", true)) {
					logger.info(
							"Automatic driver executable management is enabled. Set manage.driver.executable=false to disable it!...");
					WebDriverManager.getInstance(driverClass).setup();
				} else {
					logger.info(
							"Automatic driver executable management is disabled. Set manage.driver.executable=true to enable it!...");
				}
			} catch (Throwable e) {
				logger.error("Unable to setup driver executable: " + e.getMessage());

			}
		}

		private QAFExtendedWebDriver proxyConnectForNormalDriver(String seleniumGridURLStr,
				WebDriverCommandLogger reporter, DesiredCapabilities desiredCapabilities) throws Exception {

			logger.info("Proxy Driver Connect");

			ClientConfig defaultClientConfig = this.getDefaultClientConfig();

			HttpClient.Factory factory = this.getProxyEnabledProxyConnector(defaultClientConfig);

			Collection<QAFWebDriverCommandListener> driverListeners = UiDriverFactory.getDriverListeners();

			UiDriverFactory.beforeInitialize(desiredCapabilities, driverListeners);

			WebDriver webDriverObject = null;

			Class<? extends WebDriver> driverClass = this.getDriverCls();

			URL seleniumGridUrl = this.getSeleniumGridURL();

			try {
				if (null != driverClass) {

					String className = driverClass.getName().toLowerCase();

					if (className.startsWith("io.appium")) {

						webDriverObject = getDriverProxyObj(driverClass, desiredCapabilities, seleniumGridUrl, factory);// driverCls.newInstance();

						return new QAFExtendedWebDriver(webDriverObject, reporter);

					} else {
						HttpCommandExecutor executor = new AppiumCommandExecutor(new HashMap<String, CommandInfo>(),
								seleniumGridUrl, factory);

						return new QAFExtendedWebDriver(executor, desiredCapabilities, reporter);
					}
				} else {
					HttpCommandExecutor executor = new HttpCommandExecutor(new HashMap<String, CommandInfo>(),
							seleniumGridUrl, factory);
					return new QAFExtendedWebDriver(executor, desiredCapabilities, reporter);
				}

			} catch (Exception e) {
				throw new AutomationError("Unable to Create Driver Instance " + e.getMessage(), e);
			}
		}

		private static WebDriver getDriverProxyObj(Class<? extends WebDriver> of, Capabilities capabilities, URL url,
				HttpClient.Factory factory) {
			try {
				Constructor<? extends WebDriver> constructor = of.getConstructor(URL.class, HttpClient.Factory.class,
						Capabilities.class);
				return constructor.newInstance(url, factory, capabilities);
			} catch (Exception ex) {
				return null;
			}
		}

		// Following methods are used for connecting with Remote drivers
		private QAFExtendedWebDriver standardConnect(String url, WebDriverCommandLogger reporter) {
			logger.info("Direct Driver Connect");

			Capabilities desiredCapabilities = this.getDesiredCapabilities();
			Collection<QAFWebDriverCommandListener> listners = UiDriverFactory.getDriverListeners();

			UiDriverFactory.beforeInitialize(desiredCapabilities, listners);
			try {
				if (StringUtil.isNotBlank(ApplicationProperties.WEBDRIVER_REMOTE_SESSION.getStringVal())
						|| desiredCapabilities.asMap()
								.containsKey(ApplicationProperties.WEBDRIVER_REMOTE_SESSION.key)) {

					Constructor<?> constructor = Class
							.forName("com.qmetry.qaf.automation.ui.webdriver.LiveIsExtendedWebDriver")
							.getDeclaredConstructor(URL.class, Capabilities.class, WebDriverCommandLogger.class);

					return (QAFExtendedWebDriver) constructor.newInstance(new URL(url), desiredCapabilities, reporter);
				}
				return new QAFExtendedWebDriver(new URL(url), desiredCapabilities, reporter);
			} catch (Throwable e) {
				onInitializationFailure(desiredCapabilities, e, listners);

				throw new AutomationError("Unable to Create Driver Instance " + e.getMessage(), e);
			}
		}

		private HttpClient.Factory getProxyEnabledProxyConnector(ClientConfig clientConfig)
				throws InstantiationException, IllegalAccessException, IllegalArgumentException,
				InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
			HttpClient.Factory factory = null;

			String proxyConnectionProviderClassName = ConfigurationManager.getBundle()
					.getString("proxy.connection.provider", "");

			if ("".equals(proxyConnectionProviderClassName))
				throw new AutomationError(
						"Unable to Create Driver Instance. Proxy connection provider is not provided.");

			Object proxyConnectionProviderObj = Class.forName(proxyConnectionProviderClassName).getDeclaredConstructor()
					.newInstance();

			if (proxyConnectionProviderObj instanceof ProxyEnableConnectionProvider) {
				ProxyEnableConnectionProvider proxyConnectionProvider = (ProxyEnableConnectionProvider) proxyConnectionProviderObj;
				factory = proxyConnectionProvider.getProxyEnabledCommandExecutor(clientConfig);
			} else {
				throw new AutomationError(
						"Proxy connection provider must be instance of ProxyEnableConnectionProvider Interface");
			}

			return factory;
		}

		private ClientConfig getDefaultClientConfig() {

			int connTimeOutInSecFromProp = ConfigurationManager.getBundle().getInt("connection.timeout.ms", 60_000);
			int readTimeOutInSecFromProp = ConfigurationManager.getBundle().getInt("read.timeout.ms", 60_000);

			Duration connTimeOutInSecs = Duration.ofMillis(connTimeOutInSecFromProp);
			Duration readTimeOutInSecs = Duration.ofMillis(readTimeOutInSecFromProp);

			URL seleniumGridURL = this.getSeleniumGridURL();

			ClientConfig clientConfig = ClientConfig.defaultConfig().baseUrl(seleniumGridURL)
					.connectionTimeout(connTimeOutInSecs).readTimeout(readTimeOutInSecs);

			return clientConfig;

		}

		private URL getSeleniumGridURL() {
			URL seleniumGridURL;
			try {
				seleniumGridURL = new URL(ConfigurationManager.getBundle().getString("remote.server", ""));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e.getMessage(), e);
			}

			return seleniumGridURL;
		}

		private QAFExtendedWebDriver proxyConnect(String url, WebDriverCommandLogger reporter,
				DesiredCapabilities desiredCapabilities) throws Exception {

			logger.info("Proxy Driver Connect");

			ClientConfig defaultClientConfig = this.getDefaultClientConfig();

			HttpClient.Factory factory = this.getProxyEnabledProxyConnector(defaultClientConfig);

			Collection<QAFWebDriverCommandListener> listners = UiDriverFactory.getDriverListeners();

			UiDriverFactory.beforeInitialize(desiredCapabilities, listners);

			URL seleniumGridURL = this.getSeleniumGridURL();

			try {

				HttpCommandExecutor executor = new AppiumCommandExecutor(new HashMap<String, CommandInfo>(),
						seleniumGridURL, factory);
				return new QAFExtendedWebDriver(executor, desiredCapabilities, reporter);

			} catch (Throwable e) {
				throw new AutomationError("Unable to Create Driver Instance " + e.getMessage(), e);
			}
		}

		@SuppressWarnings("unused")
		@Deprecated
		private String getWorkstation() {
			Map<String, String> env = System.getenv();
			if (env.containsKey("COMPUTERNAME")) {
				// Windows
				return env.get("COMPUTERNAME");
			} else if (env.containsKey("HOSTNAME")) {
				// Unix/Linux/MacOS
				return env.get("HOSTNAME");
			} else {
				// From DNS
				try {
					return InetAddress.getLocalHost().getHostName();
				} catch (UnknownHostException ex) {
					return "Unknown";
				}
			}
		}
	}

}