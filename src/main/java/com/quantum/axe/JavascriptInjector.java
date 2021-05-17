package com.quantum.axe;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;

/**
 * A helper class that assists in injecting third party JavaScript libaries into
 * a page under test. Selenium's executeScript method executes arbitrary
 * JavaScript, which is wrapped at runtime in an Immediately Invoked Functional
 * Expression. This means that scripts implemented as an IIFE fail (and many or
 * most third party scripts are implemented as an IIFE).
 * <p>
 * The answer is to inject the script adding a script tag to a page. So long as
 * the script is available from a web server visible to the device under test,
 * that is.
 */
public class JavascriptInjector {
//  private static final Logger log = LoggerFactory.getLogger(JavascriptInjector.class);

  /**
   * A script template used to inject a script into a web page. It appends the
   * script URL to the document by creating a script element. It then adds a
   * listener to this element in order to get notified when the script has
   * loaded. Once loaded, it sets a global variable used to signal success.
   * Another script then polls this value to determine when the script has been
   * loaded. This technique borrows from the JQuery bookmarklet and work
   * from Paul Irish.
   * <p>
   * Why poll? Because executeAsyncScript fails reliably on desktop browsers...
   *
   * @see #checkSuccessScript
   */
  //  http://www.learningjquery.com/2009/04/better-stronger-safer-jquerify-bookmarklet
  public static final String injectorFormat = "var callback=arguments[arguments.length - 1];\n"
      + "var script=document.createElement('script');\n"
      + "script.src='%s';\n"
      + "var head=document.getElementsByTagName('head')[0];\n"
      + "var timeout=setTimeout( function() { callback(false); }, 30000 );\n"
      + "script.onerror = function() { callback(false); }\n"
      + "script.onload=script.onreadystatechange = function() {\n"
      + "   if ( !this.readyState || this.readyState == 'loaded' || this.readyState == 'complete') {\n"
      + "     clearTimeout(timeout);\n"
      + "     script.onload = script.onreadystatechange = null;\n"
      + "     head.removeChild(script);\n"
      + "     callback( true );\n"
      + "   }\n"
      + "}\n"
      + "head.appendChild(script);";

  /**
   * A script used to find whether a prior script injection succeeded.
   *
   * @see #injectorFormat
   */
  public static final String checkSuccessScript = "return document.perfectoScriptInjectionComplete;";

  /**
   * Inject some JavaScript identified by the given URL into the device. This is
   * intended for use with third party JavScript library dependencies required
   * by this Selenium test.
   *
   * @param driver
   *          The remote web driver.
   * @param url
   *          The URL to inject.
   * @throws ScriptInjectionException
   *           (a RuntimeException) if this fails.
   */
  public static void injectJavaScript(final RemoteWebDriver driver, final URL url) {

    final String toExecute = String.format(injectorFormat, url.toString());
//    log.info("Injecting JavaScript from URL: {}", url);
//    log.trace(toExecute);
    Object result;

    final String testCase = "var callback=arguments[arguments.length-1]; setTimeout( function() {callback(true);}, 2000);";
    final Capabilities capabilities = driver.getCapabilities();
    try {
      result = driver.executeAsyncScript(testCase, Collections.EMPTY_MAP);
      final String message = String.format(
          "executeAsyncScript works on: %s %s - %s %s",
          capabilities.getCapability("platformName"),
          capabilities.getCapability("osVersion"),
          capabilities.getCapability("browserName"),
          capabilities.getCapability("browserVersion"));
//      log.info(message);
    } catch (final Exception e) {
      final String message = String.format(
          "Asynchronous script execution test failed for: %s %s - %s %s - JavaScript: \"%s\"",
          capabilities.getCapability("platformName"),
          capabilities.getCapability("osVersion"),
          capabilities.getCapability("browserName"),
          capabilities.getCapability("browserVersion"),
          testCase);
      throw new RuntimeException(message, e);
    }

    result = driver.executeAsyncScript(toExecute, Collections.EMPTY_MAP);

    if (!(boolean) result) {
      // TODO - try and pull the console log (even though this only works for Chrome?)
      throw new ScriptInjectionException("Script Injection returned " + result + "for: " + url);
    }

    // Now poll to see whether the script loaded -
    //    Boolean success = false;
    //    for (int retries = 0; retries < 30; retries++) {
    //      log.info("Polling to check if script injection has completed for \"{}\".", url);
    //      success = Boolean.valueOf(String.valueOf(driver.executeScript(checkSuccessScript, Collections.EMPTY_MAP)));
    //      if (success) {
    //        log.debug("Script injection succeeded.");
    //        break;
    //      }
    //
    //      try {
    //        Sleep.quietly(1000);
    //      } catch (final InterruptedException e) {
    //        // Ignored
    //      }
    //    }

    // At this point, we have tried thirty times - so we throw...
    //    if (!success) {
    //      throw new ScriptInjectionException("Script Injection failed for: " + url);
    //    }
  }

  /**
   * A runtime exception that signals a failure in script injection.
   */
  static class ScriptInjectionException extends PerfectoRuntimeException {
    private static final long serialVersionUID = 1L;

    ScriptInjectionException(final String reason) {
      super(reason);
    }
  }
}

