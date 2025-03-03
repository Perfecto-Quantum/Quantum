package com.perfecto.reportium.model;

import com.perfecto.reportium.exception.ReportiumException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.WebDriver;

import java.util.LinkedList;
import java.util.List;

/**
 * Execution context for MCM-based ReportiumClient clients
 */
public class PerfectoExecutionContext extends BaseExecutionContext {

    private final List<Pair<String, WebDriver>> webDriverPairs = new LinkedList<>();

    protected PerfectoExecutionContext(PerfectoExecutionContextBuilder perfectoExecutionContextBuilder) {
        super(perfectoExecutionContextBuilder);
        if (perfectoExecutionContextBuilder.webDriverPairs.isEmpty()) {
            throw new ReportiumException("Missing required web driver(s) argument. Call your builder's withWebDriver() method");
        }

        webDriverPairs.addAll(perfectoExecutionContextBuilder.webDriverPairs);
    }

    @Deprecated
    public WebDriver getWebdriver() {
        return getWebDriver();
    }

    public WebDriver getWebDriver() {
        return webDriverPairs.get(0).getValue();
    }

    public List<Pair<String, WebDriver>> getWebDriverPairs() {
        return webDriverPairs;
    }

    public static class PerfectoExecutionContextBuilder extends BaseExecutionContext.Builder<PerfectoExecutionContextBuilder> {
        private Integer index = 1;
        private List<Pair<String, WebDriver>> webDriverPairs = new LinkedList<>();

        public PerfectoExecutionContextBuilder withWebDriver(WebDriver webDriver) {
            return withWebDriver(webDriver, index.toString());
        }

        public PerfectoExecutionContextBuilder withWebDriver(WebDriver webDriver, String alias) {
            webDriverPairs.add(new ImmutablePair<>(alias, webDriver));
            index++;
            return this;
        }

        public PerfectoExecutionContext build() {
            return new PerfectoExecutionContext(this);
        }
    }
}
