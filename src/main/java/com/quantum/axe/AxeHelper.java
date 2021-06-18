package com.quantum.axe;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A helper class that wraps the functionality of the Axe accessibility testing
 * library. This code will inject the Axe library into the web browser at the
 * end of a Selenium session and then expose the Axe API to Java.
 * <p>
 * It's an incomplete proof of concept as yet...
 */
public class AxeHelper {
//  private static final Logger log = LoggerFactory.getLogger(AxeHelper.class);

  /**
   * The default AXE URL used by script injection.
   */
  //  https://cdnjs.cloudflare.com/ajax/libs/axe-core/2.3.1/axe.min.js

  public static final String AXE_DEFAULT = "https://cdnjs.cloudflare.com/ajax/libs/axe-core/3.5.3/axe.min.js";
  /**
   * The RWD for command dispatch.
   */
  private final RemoteWebDriver driver;

  /**
   * The object mapper we use to deserialise JSON responses.
   */
  private final ObjectMapper jsonObjectMapper = new ObjectMapper();

  /**
   * The list of rules available.
   */
  private List<AxeTestResultRule> rules;

  /**
   * The tags that describe standard compatibility for rules.
   */
  private Set<String> ruleTags;

  private static final String highlightScript;

  /**
   * A script that executes all of the Axe rule. Since Axe executes
   * asynchronously, the results of the accessibility tests are made available
   * in a callback. So, this function returns true to signal that the
   * accessibility tests have started. The results are checked by polling.
   */
  public static final String runAxeScript;

  public static final String startHighlighterScript;

  /**
   * A script that executes within a polling loop to determine whether the Axe
   * accessibility test has completed.
   */
  public static final String checkAxeSuccessScript = "return JSON.stringify(document.perfectoAxeError);";

  public static final String getAxeResultsScript = "return JSON.stringify(document.perfectoAxeResults);";

  static {
    highlightScript = loadResource("/axeHighlighter.js");
    runAxeScript = loadResource("/runAxe.js");
    startHighlighterScript = loadResource("/startHighlighter.js");
  }

  private static String loadResource(final String scriptName) {
    try {
//      log.info("Loading helper script {}", scriptName);
      final InputStream highlighterSource = AxeHelper.class.getResourceAsStream(scriptName);
      return IOUtils.toString(highlighterSource, "UTF-8");
//      return new String(IOUtils.readFully(highlighterSource, 1));
    } catch (final Throwable e) {
//      log.error("Unable to load script: {}", scriptName, e);
      throw new PerfectoRuntimeException("Unable to load script: " + scriptName, e);
    }
  }

  /**
   * Inject the default Axe version into the browser on the given Remote Web
   * Driver.
   *
   * @param driver
   *          The driver.
   */
  public AxeHelper(final RemoteWebDriver driver) {
    this(driver, AXE_DEFAULT);
  }

  /**
   * Inject Axe from the given URL into the given remote web web driver.
   *
   * @param driver
   *          The driver.
   * @param axeUrl
   *          The URL of an Axe script.
   */
  public AxeHelper(final RemoteWebDriver driver, final String axeUrl) {
    this.driver = driver;

//    log.debug("Injecting Axe from \"{}\"", axeUrl);
    try {
      final URL url = new URL(axeUrl);
      JavascriptInjector.injectJavaScript(driver, url);
    } catch (final MalformedURLException e) {
      throw new AxeHelperException("Invalid AXE URL: " + axeUrl);
    }

    // After script injection, a global Axe object will be available.
    final Object success = driver.executeScript("return typeof axe === 'object';", Collections.EMPTY_MAP);
    if (!Boolean.valueOf(String.valueOf(success))) {
      throw new AxeHelperException("Axe injection failed (no axe object in browser document).");
    }
//    log.info("Axe injection succeded.");

    getRules();
  }

  /**
   * Return the list of rule available from Axe.
   *
   * @return the list of rule available from Axe.
   */
  public List<AxeTestResultRule> getRules() {
    if (rules != null) {
      return rules;
    }

//    log.debug("Getting axe rules...");
    final String rawRules = String
        .valueOf(driver.executeScript("return JSON.stringify(axe.getRules());", Collections.EMPTY_MAP));
//    log.trace("Axe rules: {}", rawRules);

    try {
      rules = jsonObjectMapper.readValue(rawRules, new TypeReference<ArrayList<AxeTestResultRule>>() {
      });
//      log.info("{} AXE rules available.", rules.size());
    } catch (final Exception e) {
//      log.error("Internal error - ", e);
      throw new AxeHelperException("Unable to read AXE rules.");
    }

    ruleTags = new HashSet<>();
    for (final AxeTestResultRule rule : rules) {
      for (final String tag : rule.tags) {
        ruleTags.add(tag);
      }
    }

    return rules;
  }

  /**
   * Return the available rule tags.
   *
   * @return the available rule tags.
   */
  public Set<String> getRuleTags() {
    getRules();
    return ruleTags;
  }

  /**
   * Run all of the Axe rules. Note that this is known to cause problems if
   * there are a very large number of errors: a problem occurs in older iOS
   * versions which seems to be due to the size of the returned JSON object. The
   * work around is to run smaller sets of rules.
   *
   * @return The test results.
   */
  public AxeTestResultContainer axeEverything() throws InterruptedException {
    final String script = String.format(runAxeScript);
    // log.info("{}", script);
    driver.executeScript(script, Collections.EMPTY_MAP);
//    log.info("Axe run: {}", result);

    // Poll for a result...
    boolean success = false;
    String axeError = null;
    String axeResults = null;
    for (int retries = 0; retries < 30; retries++) {
//      log.trace("Polling to check Axe results..");
      axeError = String.valueOf(driver.executeScript(checkAxeSuccessScript, Collections.EMPTY_MAP));

      if (!"null".equals(axeError)) {
//        log.warn("Axe execution failed: {}", axeError);
        throw new AxeHelperException("Error executing Axe: " + axeError);
      }

      axeResults = String.valueOf(driver.executeScript(getAxeResultsScript, Collections.EMPTY_MAP));
      if (!"null".equals(axeResults)) {
//        log.trace("Axe execution succeded.");
//        log.trace("{}", axeResults);
        success = true;
        break;
      }

      driver.wait(10000);
    }

    if (!success) {
      throw new AxeHelperException("Timeout waiting for Axe to complete.");
    }

    try {
      final AxeTestResultContainer resultContainer = jsonObjectMapper.readValue(axeResults,
          new TypeReference<AxeTestResultContainer>() {
          });
      return resultContainer;
    } catch (final Exception e) {
//      log.error("Internal error - ", e);
      throw new AxeHelperException("Unable to parse AXE result.");
    }
  }

  /**
   * Run axe storing the results in document.perfectoAxeResult on the browser;
   */
  public void runAxe() {
    final Object result = driver.executeAsyncScript(runAxeScript, Collections.EMPTY_MAP);

//    log.info("Axe returned: {}", result);
  }

  /**
   * The highlighter is an iterator over axe violations. It should be called after AXE has run.
   *
   * @param type
   * @return
   */
  public boolean startHighlighter(final String type) {
    Object result = driver.executeScript(highlightScript, Collections.EMPTY_MAP);
//    log.info("Axe highlight script: {}", result);

    final String toExecute = String.format(startHighlighterScript, type);

    result = driver.executeScript(toExecute, Collections.EMPTY_MAP);
//    log.info("Axe start highlighter returned: {}", result);

    return true;
  }

  public Map<String, String> nextHighlight() {
    final Object result = driver.executeScript("return document.perfectoViolationHighlighter.highlightNext();",
        Collections.EMPTY_MAP);
    if (result == null) {
      return null;
    }

//    log.trace("Highlight result: {}", result);
    try {
      return jsonObjectMapper.readValue(String.valueOf(result), new TypeReference<HashMap<String, ?>>() {
      });
    } catch (final Exception e) {
//      log.error("Internal error - ", e);
      throw new AxeHelperException("Unable to read AXE rules.");
    }
  }

  public AxeTestResultRule getRuleById(final String ruleId) {
    for (final AxeTestResultRule rule : getRules()) {
      if (rule.ruleId.equals(ruleId)) {
        return rule;
      }
    }
    return null;
  }

  /**
   * A runtime exception raised when when there is some problem with Axe.
   */
  static class AxeHelperException extends PerfectoRuntimeException {
    private static final long serialVersionUID = 1L;

    AxeHelperException(final String reason) {
      super(reason);
    }

    AxeHelperException(final String reason, final Throwable e) {
      super(reason, e);
    }
  }

}

