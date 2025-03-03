package com.perfecto.reportium.client;

import com.perfecto.reportium.model.PerfectoExecutionContext;

/**
 * Factory for creating the ReportiumClient instances
 */
public class ReportiumClientFactory {

    /**
     * Creates a ReportiumClient is based on Perfecto's grid, sending messages to Reportium backend
     *
     * @param perfectoExecutionContext Perfecto test execution environment details
     * @return Client instance based on Perfecto's execution environment
     */
    public ReportiumClient createPerfectoReportiumClient(PerfectoExecutionContext perfectoExecutionContext) {
        return new PerfectoReportiumClient(perfectoExecutionContext);
    }

    /**
     * Creates a ReportiumClient that sends log messages to a local log
     *
     * @return Client instance
     */
    public ReportiumClient createLoggerClient() {
        return new LoggerReportiumClient();
    }

}
