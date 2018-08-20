package com.qmetry.qaf.automation.ui;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.http.HttpClient.Factory;

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
import com.qmetry.qaf.automation.ui.webdriver.ChromeDriverHelper;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandListener;
import com.qmetry.qaf.automation.util.StringUtil;

public class UiDriverFactory implements DriverFactory<UiDriver> {
	private static final Log logger = LogFactoryImpl.getLog(UiDriverFactory.class);

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
				// TODO Auto-generated catch block
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
	 * @param driverName
	 * @return
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

	private static void onInitializationFailure(DesiredCapabilities desiredCapabilities, Throwable e,
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
				QAFWebDriverCommandListener cls = (QAFWebDriverCommandListener) Class.forName(listenr).newInstance();
				listners.add(cls);
			} catch (Exception e) {
				logger.error("Unable to register listener class " + listenr, e);
			}
		}
		clistners = ConfigurationManager.getBundle().getStringArray(ApplicationProperties.QAF_LISTENERS.key);
		for (String listener : clistners) {
			try {
				QAFListener cls = (QAFListener) Class.forName(listener).newInstance();
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
		String urlStr = STBArgs.sel_server.getFrom(args).startsWith("http") ? STBArgs.sel_server.getFrom(args)
				: String.format("http://%s:%s/wd/hub", STBArgs.sel_server.getFrom(args), STBArgs.port.getFrom(args));

		Browsers browser = Browsers.getBrowser(b);
		loadDriverResouces(browser);

		ConfigurationManager.getBundle().setProperty("driver.desiredCapabilities",
				browser.getDesiredCapabilities().asMap());
		QAFExtendedWebDriver driver = b.contains("remote") ? browser.getDriver(urlStr, reporter)
				: browser.getDriver(reporter, urlStr);
		ConfigurationManager.getBundle().setProperty("driver.actualCapabilities", driver.getCapabilities().asMap());
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

	private static WebDriver getDriverObj(Class<? extends WebDriver> of, Capabilities capabilities, String urlStr) {
		try {
			// give it first try
			Constructor<? extends WebDriver> constructor = of.getConstructor(URL.class, Capabilities.class);
			return constructor.newInstance(new URL(urlStr), capabilities);
		} catch (Exception ex) {
			try {
				Constructor<? extends WebDriver> constructor = of.getConstructor(Capabilities.class);
				return constructor.newInstance(capabilities);
			} catch (Exception e) {
				if (e.getCause() != null && e.getCause() instanceof WebDriverException) {
					throw (WebDriverException) e.getCause();
				}
				try {
					return of.newInstance();
				} catch (Exception e1) {
					try {
						// give it another try
						Constructor<? extends WebDriver> constructor = of.getConstructor(URL.class, Capabilities.class);

						return constructor.newInstance(new URL(urlStr), capabilities);
					} catch (InvocationTargetException e2) {
						throw new WebDriverException(e2);
					} catch (InstantiationException e2) {
						throw new WebDriverException(e2);
					} catch (IllegalAccessException e2) {
						throw new WebDriverException(e2);
					} catch (IllegalArgumentException e2) {
						throw new WebDriverException(e2);
					} catch (MalformedURLException e2) {
						throw new WebDriverException(e2);
					} catch (NoSuchMethodException e2) {
						throw new WebDriverException(e2);
					} catch (SecurityException e2) {
						throw new WebDriverException(e2);
					}
				}
			}
		}
	}

	private enum Browsers {
		edge(DesiredCapabilities.edge(), EdgeDriver.class),

		firefox(DesiredCapabilities.firefox(), FirefoxDriver.class), iexplorer(DesiredCapabilities.internetExplorer(),
				InternetExplorerDriver.class), chrome(DesiredCapabilities.chrome(), ChromeDriver.class), opera(
						new DesiredCapabilities("opera", "", Platform.ANY),
						"com.opera.core.systems.OperaDriver"), android(DesiredCapabilities.android(),
								"org.openqa.selenium.android.AndroidDriver"), iphone(
										new DesiredCapabilities("iPhone", "", Platform.MAC),
										"org.openqa.selenium.iphone.IPhoneDriver"), ipad(
												new DesiredCapabilities("iPad", "", Platform.MAC),
												"org.openqa.selenium.iphone.IPhoneDriver"), safari(
														new DesiredCapabilities("safari", "", Platform.ANY),
														"org.openqa.selenium.safari.SafariDriver"), appium(
																new DesiredCapabilities(),
																"io.appium.java_client.AppiumDriver"), perfecto(
																		new DesiredCapabilities()),

		/**
		 * can with assumption that you have set desired capabilities using property.
		 * This is to provide support for future drivers or custom drivers if any. You
		 * can provide driver class as capability : driver.class, for example :<br>
		 * driver.class=org.openqa.selenium.safari.SafariDriver
		 */
		other(new DesiredCapabilities());

		private DesiredCapabilities desiredCapabilities;

		// private Class<? extends WebDriver> driverCls = null;
		private String browserName = name();

		private Class<? extends WebDriver> getDriverCls() {
			return (Class<? extends WebDriver>) ConfigurationManager.getBundle().getProperty("DriverCLS");
		}

		private void setDriverCls(Class<? extends WebDriver> driverCls) {
			ConfigurationManager.getBundle().setProperty("DriverCLS", driverCls);
		}

		private Browsers(DesiredCapabilities desiredCapabilities) {
			this.desiredCapabilities = desiredCapabilities;
			this.desiredCapabilities.setJavascriptEnabled(true);
			this.desiredCapabilities.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
			this.desiredCapabilities.setCapability(CapabilityType.SUPPORTS_FINDING_BY_CSS, true);

		}

		@SuppressWarnings("unchecked")
		private Browsers(DesiredCapabilities desiredCapabilities, String drivercls) {
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

		private Browsers(DesiredCapabilities desiredCapabilities, Class<? extends WebDriver> driver) {
			this(desiredCapabilities);
			if (null == getDriverCls()) {
				// not overridden by extra capability
				setDriverCls(driver);
			}
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
			// capabilities specific to this driver
			String driverCapsKey = String.format(ApplicationProperties.DRIVER_ADDITIONAL_CAPABILITIES_FORMAT.key,
					name());
			extraCapabilities = gson.fromJson(ConfigurationManager.getBundle().getString(driverCapsKey, "{}"),
					Map.class);
			capabilities.putAll(extraCapabilities);
			// individual capability property with driver name prefix
			String driverCapKey = String.format(ApplicationProperties.DRIVER_CAPABILITY_PREFIX_FORMAT.key, browserName);
			config = ConfigurationManager.getBundle().subset(driverCapKey);
			capabilities.putAll(new ConfigurationMap(config));

			Object driverclass = capabilities.get(ApplicationProperties.CAPABILITY_NAME_DRIVER_CLASS.key);
			if (null == driverclass) {// backward compatibility only
				driverclass = capabilities.get("driver.class");
			}
			if (null != driverclass) {
				try {
					setDriverCls((Class<? extends WebDriver>) Class.forName(String.valueOf(driverclass)));
				} catch (Exception e) {
					// throw new AutomationError(e);
				}
			}
			for (String key : capabilities.keySet()) {
				Object value = capabilities.get(key);
				if (value instanceof String) {
					capabilities.put(key, ConfigurationManager.getBundle().getSubstitutor().replace(value));
				}
			}
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

		private QAFExtendedWebDriver getDriver(WebDriverCommandLogger reporter, String urlstr) throws Exception {
			if (!ConfigurationManager.getBundle().getString("ntlmProxyHost", "").equalsIgnoreCase("")) {
				return proxyConnectForNormalDriver(urlstr, reporter);
			} else {
				return standardConnect(reporter, urlstr);
			}
		}

		private QAFExtendedWebDriver getDriver(String url, WebDriverCommandLogger reporter) throws Exception {
			if (!ConfigurationManager.getBundle().getString("ntlmProxyHost", "").equalsIgnoreCase("")) {
				return proxyConnect(url, reporter);
			} else {
				return standardConnect(url, reporter);
			}
		}

		// Following methods are used for Connecting with Appium based drivers
		private QAFExtendedWebDriver standardConnect(WebDriverCommandLogger reporter, String urlstr) {
			logger.info("Direct Driver Connect");
			DesiredCapabilities desiredCapabilities = getDesiredCapabilities();

			Collection<QAFWebDriverCommandListener> listners = getDriverListeners();
			beforeInitialize(desiredCapabilities, listners);
			try {
				if (this.name().equalsIgnoreCase("chrome")) {
					return new QAFExtendedWebDriver(ChromeDriverHelper.getService().getUrl(), desiredCapabilities,
							reporter);
				}
				WebDriver driver = getDriverObj(getDriverCls(), desiredCapabilities, urlstr);// driverCls.newInstance();
				return new QAFExtendedWebDriver(driver, reporter);
			} catch (Throwable e) {
				onInitializationFailure(desiredCapabilities, e, listners);

				throw new AutomationError("Unable to Create Driver Instance for " + browserName + ": " + e.getMessage(),
						e);
			}
		}

		private QAFExtendedWebDriver proxyConnectForNormalDriver(String url, WebDriverCommandLogger reporter)
				throws Exception {
			logger.info("Proxy Driver Connect");
			DesiredCapabilities desiredCapabilities = getDesiredCapabilities();
			Collection<QAFWebDriverCommandListener> listners = getDriverListeners();

			String proxyHost = ConfigurationManager.getBundle().getString("ntlmProxyHost");
			int proxyPort = Integer.parseInt(ConfigurationManager.getBundle().getString("ntlmProxyPort"));
			String proxyUserDomain = ConfigurationManager.getBundle().getString("ntlmProxyDomain");
			String proxyUser = ConfigurationManager.getBundle().getString("ntlmProxyUser");
			String proxyPassword = ConfigurationManager.getBundle().getString("ntlmProxyPassword");

			URL urls;
			try {
				urls = new URL(ConfigurationManager.getBundle().getString("remote.server"));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			HttpClientBuilder builder = HttpClientBuilder.create();
			HttpHost proxy = new HttpHost(proxyHost, proxyPort);
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
					new NTCredentials(proxyUser, proxyPassword, getWorkstation(), proxyUserDomain));
			if (urls.getUserInfo() != null && !urls.getUserInfo().isEmpty()) {
				credsProvider.setCredentials(
						new AuthScope(urls.getHost(), (urls.getPort() > 0 ? urls.getPort() : urls.getDefaultPort())),
						new UsernamePasswordCredentials(urls.getUserInfo()));
			}
			builder.setProxy(proxy);
			builder.setDefaultCredentialsProvider(credsProvider);
			Factory factory = new MyHttpClientFactory(builder);
			HttpCommandExecutor executor = new HttpCommandExecutor(new HashMap<String, CommandInfo>(), urls, factory);

			beforeInitialize(desiredCapabilities, listners);
			try {

				WebDriver driver = getDriverProxyObj(getDriverCls(), desiredCapabilities, urls, factory);// driverCls.newInstance();
				return new QAFExtendedWebDriver(driver, reporter);

			} catch (Throwable e) {

				throw new AutomationError("Unable to Create Driver Instance " + e.getMessage(), e);
			}
		}

		private static WebDriver getDriverProxyObj(Class<? extends WebDriver> of, Capabilities capabilities, URL url,
				Factory factory) {
			try {
				// give it first try
				// Constructor<? extends WebDriver> constructor =
				// of.getConstructor(HttpCommandExecutor.class,
				// Capabilities.class);
				Constructor<? extends WebDriver> constructor = of.getConstructor(URL.class, Factory.class,
						Capabilities.class);
				return constructor.newInstance(url, factory, capabilities);
			} catch (Exception ex) {
				return null;
			}
		}

		// Following methods are used for connecting with Remote drivers
		private QAFExtendedWebDriver standardConnect(String url, WebDriverCommandLogger reporter) {
			logger.info("Direct Driver Connect");
			DesiredCapabilities desiredCapabilities = getDesiredCapabilities();
			Collection<QAFWebDriverCommandListener> listners = getDriverListeners();
			beforeInitialize(desiredCapabilities, listners);
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

		private QAFExtendedWebDriver proxyConnect(String url, WebDriverCommandLogger reporter) throws Exception {
			logger.info("Proxy Driver Connect");
			DesiredCapabilities desiredCapabilities = getDesiredCapabilities();
			Collection<QAFWebDriverCommandListener> listners = getDriverListeners();

			String proxyHost = ConfigurationManager.getBundle().getString("ntlmProxyHost");
			int proxyPort = Integer.parseInt(ConfigurationManager.getBundle().getString("ntlmProxyPort"));
			String proxyUserDomain = ConfigurationManager.getBundle().getString("ntlmProxyDomain");
			String proxyUser = ConfigurationManager.getBundle().getString("ntlmProxyUser");
			String proxyPassword = ConfigurationManager.getBundle().getString("ntlmProxyPassword");
			URL urls;
			try {
				urls = new URL(ConfigurationManager.getBundle().getString("remote.server"));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			HttpClientBuilder builder = HttpClientBuilder.create();
			HttpHost proxy = new HttpHost(proxyHost, proxyPort);
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
					new NTCredentials(proxyUser, proxyPassword, getWorkstation(), proxyUserDomain));
			if (urls.getUserInfo() != null && !urls.getUserInfo().isEmpty()) {
				credsProvider.setCredentials(
						new AuthScope(urls.getHost(), (urls.getPort() > 0 ? urls.getPort() : urls.getDefaultPort())),
						new UsernamePasswordCredentials(urls.getUserInfo()));
			}
			builder.setProxy(proxy);
			builder.setDefaultCredentialsProvider(credsProvider);
			Factory factory = new MyHttpClientFactory(builder);
			HttpCommandExecutor executor = new HttpCommandExecutor(new HashMap<String, CommandInfo>(), urls, factory);

			beforeInitialize(desiredCapabilities, listners);
			try {
				return new QAFExtendedWebDriver(executor, desiredCapabilities, reporter);

			} catch (Throwable e) {

				throw new AutomationError("Unable to Create Driver Instance " + e.getMessage(), e);
			}
		}

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