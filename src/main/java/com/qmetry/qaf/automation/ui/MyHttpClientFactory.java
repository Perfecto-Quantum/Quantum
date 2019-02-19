package com.qmetry.qaf.automation.ui;

import java.net.URL;

import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.remote.internal.ApacheHttpClient;

public class MyHttpClientFactory implements org.openqa.selenium.remote.http.HttpClient.Factory {
	final HttpClientBuilder builder;

	public MyHttpClientFactory(HttpClientBuilder builder) {
		this.builder = builder;
	}

	@Override
	public org.openqa.selenium.remote.http.HttpClient createClient(URL url) {
		return new ApacheHttpClient(builder.build(), url);
	}

	@Override
	public void cleanupIdleClients() {
		// TODO Auto-generated method stub

	}
}