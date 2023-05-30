package com.qmetry.qaf.automation.ui;

import org.openqa.selenium.remote.http.HttpClient;

public interface ProxyHandler {
	
	public HttpClient.Factory getHttpClientFactory();

}
