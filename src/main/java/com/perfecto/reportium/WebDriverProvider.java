package com.perfecto.reportium;

import org.openqa.selenium.WebDriver;

/**
 * An interface for providing the instance of {@link WebDriver} used by the test.
 *
 * The plugin expects the test class to implement this method for providing extended information on the testing platform, e.g.
 * operating system, device type etc.
 */
public interface WebDriverProvider {

    /**
     * Returns the instance of {@link WebDriver} used by the test, if available. Otherwise returns {@code null}
     * @return WebDriver instance if available or null
     */
    WebDriver getWebDriver();
}
