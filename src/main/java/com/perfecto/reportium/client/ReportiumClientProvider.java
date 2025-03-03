package com.perfecto.reportium.client;

public class ReportiumClientProvider {
    private static ThreadLocal<DigitalZoomClient> reportiumClient = new ThreadLocal<>();

    public static DigitalZoomClient get() {
        return reportiumClient.get();
    }

    public static void set(DigitalZoomClient digitalZoomClient) {
        if (digitalZoomClient == null) {
            throw new IllegalArgumentException("digitalZoomClient cannot be null");
        }
        reportiumClient.set(digitalZoomClient);
    }
}
