package com.qmetry.qaf.automation.ui;
import com.google.auto.service.AutoService;

import java.time.Duration;

import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpClientName;
import org.openqa.selenium.remote.http.jdk.JdkHttpClient;


@AutoService(HttpClient.Factory.class)
@HttpClientName("quantum-netty-client-factory")
public class NettyClientFactory extends JdkHttpClient.Factory {

  @Override
  public HttpClient createClient(ClientConfig config) {
    ClientConfig configWithShorterTimeout = config.readTimeout(Duration.ofSeconds(15));
    return super.createClient(configWithShorterTimeout);
  }
  
  
  
}