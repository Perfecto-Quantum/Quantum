package com.qmetry.qaf.automation.ui;

import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;

public interface ProxyEnableConnectionProvider {
	
	/**
	 * 
	 * Implement this interface to provide implementation of Proxy connection required. For example NTLM based connection etc.
	 * 
	 * @return {@link HttpClient.Factory}
	 * 
	 **/
	
	default HttpClient.Factory getProxyEnabledCommandExecutor(ClientConfig clientConfig){
		return null;
		
	}

}
