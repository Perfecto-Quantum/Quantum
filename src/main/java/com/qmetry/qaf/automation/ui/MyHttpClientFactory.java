package com.qmetry.qaf.automation.ui;

import java.net.URL;

import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.internal.OkHttpClient;
import org.openqa.selenium.remote.http.HttpClient.Builder;
import org.openqa.selenium.remote.http.HttpClient.Factory;

public class MyHttpClientFactory implements Factory {
	final OkHttpClient okHttpClient;

	public MyHttpClientFactory(OkHttpClient okHttpClient2) {
		this.okHttpClient = okHttpClient2;
	}

	@Override
	public HttpClient createClient(URL url) {
		return (HttpClient) okHttpClient;
	}

	@Override
	public void cleanupIdleClients() {
		// TODO Auto-generated method stub
	}

	@Override
	public Builder builder() {
		// TODO Auto-generated method stub
		return null;
	}

}