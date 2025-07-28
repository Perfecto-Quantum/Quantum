package com.qmetry.qaf.automation.ui.webdriver;

//import static org.openqa.selenium.remote.CapabilityType.SUPPORTS_JAVASCRIPT;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.CommandPayload;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.ScreenshotException;
import org.openqa.selenium.remote.internal.WebElementToJsonConverter;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.MessageTypes;
import com.qmetry.qaf.automation.core.QAFListener;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.ui.JsToolkit;
import com.qmetry.qaf.automation.ui.WebDriverCommandLogger;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.util.DynamicWait;
import com.qmetry.qaf.automation.ui.util.QAFWebDriverExpectedConditions;
import com.qmetry.qaf.automation.ui.util.QAFWebDriverWait;
import com.qmetry.qaf.automation.ui.util.QAFWebElementExpectedConditions;
import com.qmetry.qaf.automation.ui.webdriver.CommandTracker.Stage;
import com.qmetry.qaf.automation.util.LocatorUtil;
import com.qmetry.qaf.automation.util.StringMatcher;
import com.quantum.utils.Appium2Capabilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

/**
 * com.qmetry.qaf.automation.ui.webdriver.QAFWebDriver.java
 * 
 * @author chirag
 * 
 * 
 *         Note: Moved the file out of QAF framework to handle deprecation of
 * 
 *         TouchScreen class
 */

public class QAFExtendedWebDriver extends RemoteWebDriver implements QAFWebDriver, QAFWebDriverCommandListener {

	protected final Log logger = LogFactory.getLog(getClass());
	private WebDriverCommandLogger commandLogger;
	private Set<QAFWebDriverCommandListener> listners;
	private WebDriver underLayingDriver;
	private Capabilities capabilities;
	private QAFElementConverter qafElementConverter;

	public QAFExtendedWebDriver(URL url, Capabilities capabilities) {
		this(url, capabilities, null);
	}

	public QAFExtendedWebDriver(WebDriver driver) {
		this(driver, null);
	}

	public QAFExtendedWebDriver(URL url, Capabilities capabilities, WebDriverCommandLogger reporter) {
		super(url, capabilities);
		init(reporter);
	}

	public QAFExtendedWebDriver(CommandExecutor cmdExecutor, Capabilities capabilities,
			WebDriverCommandLogger reporter) {
		super(cmdExecutor, capabilities);
		init(reporter);
	}

	public QAFExtendedWebDriver() {
		init(null);
	}

	public QAFExtendedWebDriver(WebDriver driver, WebDriverCommandLogger reporter) {
		super();
		underLayingDriver = driver;
		setCommandExecutor(((RemoteWebDriver) driver).getCommandExecutor());
		setSessionId(((RemoteWebDriver) driver).getSessionId().toString());
		capabilities = ((RemoteWebDriver) driver).getCapabilities();
		init(reporter);

	}

	public QAFExtendedWebDriver getKeyboard() {
		return this;
	}

	public void sendKeys(String text) {
		new Actions(underLayingDriver).sendKeys(text).perform();
	}

	@Override
	public Capabilities getCapabilities() {

		if (capabilities == null)
			capabilities = super.getCapabilities();

		Capabilities currentCapabilities = capabilities;

		if (underLayingDriver instanceof AppiumDriver) {
			currentCapabilities = new Appium2Capabilities(currentCapabilities);
		}

		return currentCapabilities;

	}

	public WebDriver getUnderLayingDriver() {
		if (underLayingDriver == null)
			underLayingDriver = this;
		return underLayingDriver;
	}

	private void init(WebDriverCommandLogger reporter) {
		// setElementConverter(new QAFExtendedWebElement.JsonConvertor(this));
		qafElementConverter = new QAFElementConverter(this);
		try {
			listners = new LinkedHashSet<QAFWebDriverCommandListener>();
			commandLogger = (null == reporter) ? new WebDriverCommandLogger() : reporter;
			listners.add(commandLogger);
			String[] listners = ConfigurationManager.getBundle()
					.getStringArray(ApplicationProperties.WEBDRIVER_COMMAND_LISTENERS.key);
			for (String listenr : listners) {
				registerListeners(listenr);
			}
			listners = ConfigurationManager.getBundle().getStringArray(ApplicationProperties.QAF_LISTENERS.key);
			for (String listener : listners) {
				try {

					Constructor<?> qafListnerConstructor = Class.forName(listener).getConstructors()[0];

					QAFListener cls = (QAFListener) (qafListnerConstructor.newInstance());

					if (QAFWebDriverCommandListener.class.isAssignableFrom(cls.getClass()))
						this.listners.add((QAFWebDriverCommandListener) cls);
				} catch (Exception e) {
					logger.error("Unable to register class as driver listener:  " + listener, e);
				}
			}

			onInitialize(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected WebDriverCommandLogger getReporter() {
		return commandLogger;
	}

	public void setReporter(WebDriverCommandLogger reporter) {
		commandLogger = reporter;
	}

	@Override
	public QAFExtendedWebElement findElement(By by) {

//		WebElement element1 = (QAFExtendedWebElement)super.findElement(by);

//		new QAFExtendedWebEl
//		
		QAFExtendedWebElement element = (QAFExtendedWebElement) super.findElement(by);
		element.setBy(by);
		element.cacheable = true;
		return element;
	}

	/**
	 * @param locator - selenium 1 type locator for example "id=eleid",
	 *                "name=eleName" etc...
	 * @return
	 */
	public QAFWebElement findElement(String locator) {
		return ElementFactory.$(locator);
	}

	@SuppressWarnings("unchecked")
	public List<QAFWebElement> findElements(String loc) {
		return (List<QAFWebElement>) (List<? extends WebElement>) findElements(LocatorUtil.getBy(loc));
	}

	public QAFExtendedWebElement createElement(By by) {
		return new QAFExtendedWebElement(this, by);
	}

	public QAFExtendedWebElement createElement(String locator) {
		return new QAFExtendedWebElement(locator);
	}

	@SuppressWarnings("unchecked")
	public List<QAFWebElement> getElements(By by) {

		List<QAFWebElement> proxy;
		proxy = (List<QAFWebElement>) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { List.class }, new QAFExtendedWebElementListHandler(this, by));
		return proxy;
	}

	public void load(QAFExtendedWebElement... elements) {
		if (elements != null) {
			for (QAFExtendedWebElement element : elements) {
				final By by = element.getBy();

//				WebDriverWait webDriverWait = new WebDriverWait(this.getUnderLayingDriver(), 
//						Duration.ofSeconds(15));
//				webDriverWait.ignoring(NoSuchElementException.class)
//				.ignoring(StaleElementReferenceException.class)
//				.ignoring(RuntimeException.class);
//				
//				WebElement elem = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(by));

//						StaleElementReferenceException.class, RuntimeException.class);

				QAFWebDriverWait wait = new QAFWebDriverWait(this);

				wait.ignoring(NoSuchElementException.class, StaleElementReferenceException.class,
						RuntimeException.class);
				
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(by));
				
				element.setId(((QAFExtendedWebElement)elem).getId());

//				element.setId(((QAFExtendedWebElement) 
//						.until(ExpectedConditions.presenceOfElementLocated(by))).getId());
			}
		}

	}

	protected Response execute(CommandPayload payload) {

//		String className = underLayingDriver.getClass().getName();
//		
//		if(className.startsWith("io.appium")) {
//			return ((AppiumDriver)underLayingDriver).execute(payload.getName(), payload.getParameters());
//		}

		return execute(payload.getName(), payload.getParameters());
	}

	private Response executeSuper(CommandPayload payload) {
		try {
			MethodHandle h1 = MethodHandles.lookup().findSpecial(getClass().getSuperclass(), "execute",
					MethodType.methodType(Response.class, CommandPayload.class), getClass());
			return (Response) h1.invoke(this, payload);

		} catch (NoSuchMethodException | NoSuchFieldException | SecurityException e) {
			return super.execute(payload.getName(), payload.getParameters());
		} catch (Throwable e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
	}

	private Response executeWithoutLog(CommandPayload payload) {
		// in order to compatibility with 3.x, compiled with 3.x. so method from 4.x
		// super class will not be available.
		// Once min version set to 4.0 we don't need executeSuper method.

		// Response response = super.execute(payload);
		Response response = executeSuper(payload);

		if (response == null) {
			return null;
		}
		Object value = getQafElementConverter().apply(response.getValue());
		response.setValue(value);
		return response;
	}

	private QAFElementConverter getQafElementConverter() {
		if (null == qafElementConverter)
			qafElementConverter = new QAFElementConverter(this);
		return qafElementConverter;
	}

	@Override
	protected Response execute(String driverCommand, Map<String, ?> parameters) {
		CommandTracker commandTracker = new CommandTracker(driverCommand, parameters);

		try {
			beforeCommand(this, commandTracker);
			// already handled in before command?
			if (commandTracker.getResponce() == null) {
				commandTracker.setStartTime(System.currentTimeMillis());
				commandTracker
						.setResponce(executeWitoutLog(commandTracker.getCommand(), commandTracker.getParameters()));
				commandTracker.setEndTime(System.currentTimeMillis());
			}
			afterCommand(this, commandTracker);
		} catch (RuntimeException wde) {
			commandTracker.setException(wde);
			onFailure(this, commandTracker);

		}
		if (commandTracker.hasException()) {
			if (commandTracker.retry) {
				commandTracker
						.setResponce(executeWitoutLog(commandTracker.getCommand(), commandTracker.getParameters()));
				commandTracker.setException(null);
				commandTracker.setEndTime(System.currentTimeMillis());
			} else {
				throw commandTracker.getException();
			}
		}
		return commandTracker.getResponce();
	}

	protected Response executeWitoutLog(String driverCommand, Map<String, ?> parameters) {
		return executeWithoutLog(new CommandPayload(driverCommand, parameters));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openqa.selenium.TakesScreenshot#getScreenshotAs(org.openqa.selenium
	 * .OutputType)
	 */
	@Override
	public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
		boolean takeScreenshot = true;

		// getCapabilities().getCapability(CapabilityType.TAKES_SCREENSHOT);
		if (/* null == takeScreenshot || (Boolean) */ takeScreenshot) {

			String className = underLayingDriver.getClass().getName();

			String base64Str = "";

			if (className.startsWith("io.appium")) {

//				base64Str = AppiumUtils.getAppiumDriver().getScreenshotAs(OutputType.BASE64);
				if (className.contains("AndroidDriver")) {
					base64Str = ((AndroidDriver) underLayingDriver).getScreenshotAs(OutputType.BASE64);
				} else {
					base64Str = ((IOSDriver) underLayingDriver).getScreenshotAs(OutputType.BASE64);
				}

			} else {
				base64Str = execute(DriverCommand.SCREENSHOT).getValue().toString();
				;
			}

			return target.convertFromBase64Png(base64Str);
		}
		return null;
	}

	public <T> T extractScreenShot(WebDriverException e, OutputType<T> target) {
		if (e.getCause() instanceof ScreenshotException) {
			String base64Str = ((ScreenshotException) e.getCause()).getBase64EncodedScreenshot();
			return target.convertFromBase64Png(base64Str);
		}
		return null;
	}

	public Alert getAlert() {
		return new QAFWebDriverWait(this).until(QAFWebDriverExpectedConditions.alertPresent());
	}

	@Override
	public void afterCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {
		commandTracker.setStage(Stage.executingAfterMethod);

		if ((listners != null) && !listners.isEmpty()) {
			for (QAFWebDriverCommandListener listener : listners) {
				listener.afterCommand(driver, commandTracker);
			}
		}

	}

	public void updateSessionId() {
		String sessionId = new WebDriverTestBase().getDriver().getSessionId().toString();

		this.setSessionId(sessionId);
		logger.debug("Current session: " + getSessionId() + " updated with:" + sessionId);
	}

	@Override
	public void beforeCommand(QAFExtendedWebDriver driver, final CommandTracker commandTracker) {
		commandTracker.setStage(Stage.executingBeforeMethod);
		if ((listners != null) && !listners.isEmpty()) {
			for (QAFWebDriverCommandListener listener : listners) {
				listener.beforeCommand(driver, commandTracker);
			}
		}
	}

	@Override
	public void onFailure(QAFExtendedWebDriver driver, CommandTracker commandTracker) {
		commandTracker.setStage(Stage.executingOnFailure);
		commandTracker.setEndTime(System.currentTimeMillis());

		if (commandTracker.getException() instanceof UnsupportedOperationException) {
			logger.warn(commandTracker.getException().getMessage());
			commandTracker.setException(null);
		}
		if (null != listners) {
			for (QAFWebDriverCommandListener listener : listners) {
				listener.onFailure(driver, commandTracker);
			}
		}
	}

	@Override
	public void onInitialize(QAFExtendedWebDriver driver) {
		if ((listners != null) && !listners.isEmpty()) {
			for (QAFWebDriverCommandListener listener : listners) {
				listener.onInitialize(driver);
			}
		}
	}

	@Override
	public void onInitializationFailure(Capabilities desiredCapabilities, Throwable t) {

	}

	private void registerListeners(String className) {
		try {

			Constructor<?> cmdListenerConstructor = Class.forName(className).getConstructors()[0];

			QAFWebDriverCommandListener cls = (QAFWebDriverCommandListener) cmdListenerConstructor.newInstance();
			listners.add(cls);
		} catch (Exception e) {
			logger.error("Unable to register listener class " + className, e);
		}
	}

	public void registerListeners(QAFWebDriverCommandListener listener) {
		listners.add(listener);

	}

	/*
	 * @Override public QAFExtendedWebElement findElementByClassName(String using) {
	 * return (QAFExtendedWebElement) super.findElementByClassName(using); }
	 * 
	 * @Override public QAFExtendedWebElement findElementByCssSelector(String using)
	 * { return (QAFExtendedWebElement) super.findElementByCssSelector(using); }
	 * 
	 * @Override public QAFExtendedWebElement findElementById(String using) { return
	 * (QAFExtendedWebElement) super.findElementById(using); }
	 * 
	 * @Override public QAFExtendedWebElement findElementByLinkText(String using) {
	 * return (QAFExtendedWebElement) super.findElementByLinkText(using); }
	 * 
	 * @Override public QAFExtendedWebElement findElementByName(String using) {
	 * return (QAFExtendedWebElement) super.findElementByName(using); }
	 * 
	 * @Override public QAFExtendedWebElement findElementByPartialLinkText(String
	 * using) { return (QAFExtendedWebElement)
	 * super.findElementByPartialLinkText(using); }
	 * 
	 * @Override public QAFExtendedWebElement findElementByTagName(String using) {
	 * return (QAFExtendedWebElement) super.findElementByTagName(using); }
	 * 
	 * @Override public QAFExtendedWebElement findElementByXPath(String using) {
	 * return (QAFExtendedWebElement) super.findElementByXPath(using); }
	 */

	/**
	 * Under evaluation only
	 * 
	 * @param using
	 * @return
	 */
	public QAFExtendedWebElement findElementBySizzleCss(String using) {
		List<QAFExtendedWebElement> elements = findElementsBySizzleCss(using);
		if (elements.size() > 0) {
			return elements.get(0);
		}
		return null;
	}

	/**
	 * Under evaluation only
	 * 
	 * @param using
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<QAFExtendedWebElement> findElementsBySizzleCss(String using) {
		injectSizzleIfNeeded();
		String javascriptExpression = createSizzleSelectorExpression(using);
		return (List<QAFExtendedWebElement>) executeScript(javascriptExpression);
	}

	private String createSizzleSelectorExpression(String using) {
		return "return Sizzle(\"" + using + "\")";
	}

	private void injectSizzleIfNeeded() {
		if (!sizzleLoaded()) {
			injectSizzle();
		}
	}

	private Boolean sizzleLoaded() {
		Boolean loaded;
		try {
			loaded = (Boolean) executeScript("return Sizzle()!=null");
		} catch (WebDriverException e) {
			loaded = false;
		}
		return loaded;
	}

	private void injectSizzle() {
		executeScript(" var headID = document.getElementsByTagName(\"head\")[0];"
				+ "var newScript = document.createElement('script');" + "newScript.type = 'text/javascript';"
				+ "newScript.src = 'https://raw.github.com/jquery/sizzle/master/sizzle.js';"
				+ "headID.appendChild(newScript);");
	}

//	@Override
//	public TouchScreen getTouchScreen() {
//		return new RemoteTouchScreen(getExecuteMethod());
//	}

	@Override
	public String takeScreenShot() {
		return getScreenshotAs(OutputType.BASE64);
	}

	public void waitForAjax(JsToolkit toolkit, long... timeout) {
		new QAFWebDriverWait(this, timeout).withMessage("AJAX load Wait time out.")
				.until(QAFWebDriverExpectedConditions.jsCondition(toolkit.waitCondition()));
	}

	public void waitForAjax(long... timeout) {
		new QAFWebDriverWait(this, timeout).withMessage("AJAX load Wait time out.")
				.until(QAFWebDriverExpectedConditions.jsCondition(JsToolkit.globalWaitCondition()));
	}

	public void waitForAnyElementPresent(QAFWebElement... elements) {
		new DynamicWait<List<QAFWebElement>>(Arrays.asList(elements))
				.until(QAFWebElementExpectedConditions.anyElementPresent());
	}

	public void waitForAllElementPresent(QAFWebElement... elements) {
		new DynamicWait<List<QAFWebElement>>(Arrays.asList(elements))
				.until(QAFWebElementExpectedConditions.allElementPresent());
	}

	public void waitForAnyElementVisible(QAFWebElement... elements) {
		new DynamicWait<List<QAFWebElement>>(Arrays.asList(elements))
				.until(QAFWebElementExpectedConditions.anyElementVisible());
	}

	public void waitForAllElementVisible(QAFWebElement... elements) {
		new DynamicWait<List<QAFWebElement>>(Arrays.asList(elements))
				.until(QAFWebElementExpectedConditions.allElementVisible());
	}

	public void waitForWindowTitle(StringMatcher titlematcher, long... timeout) {
		new QAFWebDriverWait(this, timeout).withMessage("Timed out waiting for window title " + titlematcher.toString())
				.until(QAFWebDriverExpectedConditions.windowTitle(titlematcher));
	}

	public void waitForCurrentUrl(StringMatcher matcher, long... timeout) {
		new QAFWebDriverWait(this, timeout)
				.withMessage("Timed out waiting for url " + matcher.toString() + " timed out")
				.until(QAFWebDriverExpectedConditions.currentURL(matcher));
	}

	public void waitForNoOfWindows(int count, long... timeout) {
		new QAFWebDriverWait(this, timeout).withMessage("Timed out waiting for no of windows " + count)
				.until(QAFWebDriverExpectedConditions.noOfwindowsPresent(count));
	}

	public boolean verifyTitle(StringMatcher text, long... timeout) {

		boolean outcome = true;
		try {
			waitForWindowTitle(text, timeout);
		} catch (Exception e) {
			outcome = false;
		}
		report("title", outcome, text, getTitle());

		return outcome;
	}

	public boolean verifyCurrentUrl(StringMatcher text, long... timeout) {

		boolean outcome = true;
		try {
			waitForCurrentUrl(text, timeout);
		} catch (Exception e) {
			outcome = false;
		}
		report("currentUrl", outcome, text, getCurrentUrl());

		return outcome;
	}

	public boolean verifyNoOfWindows(int count, long... timeout) {

		boolean outcome = true;
		try {
			waitForNoOfWindows(count, timeout);
			;
		} catch (Exception e) {
			outcome = false;
		}
		report("NoOfWindows", outcome, count, getWindowHandles().size());

		return outcome;
	}

	public void assertTitle(StringMatcher text, long... timeout) {
		if (!verifyTitle(text, timeout)) {
			throw new AssertionError();
		}
	}

	public void assertCurrentUrl(StringMatcher text, long... timeout) {
		if (!verifyCurrentUrl(text, timeout)) {
			throw new AssertionError();
		}
	}

	public void assertNoOfWindows(int count, long... timeout) {
		if (!verifyNoOfWindows(count, timeout)) {
			throw new AssertionError();
		}
	}

	protected void report(String op, boolean outcome, Object... args) {
		getReporter().addMessage(WebDriverCommandLogger.getMsgForDriverOp(op, outcome, args),
				(outcome ? MessageTypes.Pass : MessageTypes.Fail));
	}

	@Override
	public void beforeInitialize(Capabilities desiredCapabilities) {
		// can't do anything over here...
	}

	/**
	 * This method is useful to extract underlying driver specific interface
	 * implementation from the driver object. For Example, Assuming desired
	 * capability rotatable is true you can extract Rotatable interface from driver
	 * as below. <br />
	 * <code>
	 * {@link Rotatable} rotatable = driver.getCapabilityImpl();<br />
	 * rotatable.rotate(ScreenOrientation.LANDSCAPE);
	 * </code>
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getCapabilityImpl() {
		WebDriver augmentedDriver = new Augmenter().augment(this);
		return ((T) augmentedDriver);
	}

	/*
	 * public <X extends RemoteWebDriver> X toX(X x){ x.getSessionId(); return x; }
	 */

	public QAFExtendedWebElement findElementByCustomStretegy(String stetegy, String loc) {
		return (QAFExtendedWebElement) execute(DriverCommand.FIND_ELEMENT,
				ImmutableMap.of("using", stetegy, "value", loc)).getValue();
	}

	@SuppressWarnings("unchecked")
	public List<WebElement> findElementsByCustomStretegy(String stetegy, String loc) {
		return (List<WebElement>) execute(DriverCommand.FIND_ELEMENTS, ImmutableMap.of("using", stetegy, "value", loc))
				.getValue();
	}

	@Override
	public void stop() {
		quit();
	}

	@Override
	public Object executeScript(String script, Object... args) {
		if (!isJavascriptEnabled()) {
			throw new UnsupportedOperationException(
					"You must be using an underlying instance of WebDriver that supports executing javascript");
		}

		// Escape the quote marks
		script = script.replaceAll("\"", "\\\"");

		List<Object> convertedArgs = Stream.of(args).map(new WebElementToJsonConverter()).collect(Collectors.toList());
		Map<String, ?> params = ImmutableMap.of("script", script, "args", Lists.newArrayList(convertedArgs));

		return execute(DriverCommand.EXECUTE_SCRIPT, params).getValue();
	}

	@Override
	public Object executeAsyncScript(String script, Object... args) {
		if (!isJavascriptEnabled()) {
			throw new UnsupportedOperationException(
					"You must be using an underlying instance of " + "WebDriver that supports executing javascript");
		}

		// Escape the quote marks
		script = script.replaceAll("\"", "\\\"");

		List<Object> convertedArgs = Stream.of(args).map(new WebElementToJsonConverter()).collect(Collectors.toList());
		Map<String, ?> params = ImmutableMap.of("script", script, "args", Lists.newArrayList(convertedArgs));

		return execute(DriverCommand.EXECUTE_ASYNC_SCRIPT, params).getValue();
	}

	boolean isJavascriptEnabled() {
		return true;
	}

}
