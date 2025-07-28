package com.quantum.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.ContentBody;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.quantum.axe.PerfectoRuntimeException;

public class RepositoryUtils {

	private String cloudName;
	private String securityToken;

	public RepositoryUtils() {

		this.cloudName = (ConfigurationManager.getBundle().getString("remote.server").split("\\.")[0])
				.replace("http://", "").replace("https://", "");
		this.securityToken = ConfigurationManager.getBundle().getString("perfecto.capabilities.securityToken");
	}
	
	private boolean isProxyHostProvided() {

		String proxyHost = ConfigurationManager.getBundle().getString("proxyHost", "");
		return !"".equals(proxyHost);

	}

	protected void addProxyDetailsIfRequired(HttpClientBuilder clientBuilder) throws Exception {

		String proxyHost = ConfigurationManager.getBundle().getString("proxyHost", "");
		String proxyPort = ConfigurationManager.getBundle().getString("proxyPort", "");

		if (isProxyHostProvided()) {
			
			System.setProperty("http.proxyHost", proxyHost);
	        System.setProperty("http.proxyPort", proxyPort);
	        System.setProperty("https.proxyHost", proxyHost);
	        System.setProperty("https.proxyPort", proxyPort);

		}

	}

	public void uploadFile(String localFilePath, String repositoryID) throws IOException, ParseException {

		File fileToUpload = new File(localFilePath);

		if (!fileToUpload.exists()) {

			throw new PerfectoRuntimeException("Upload Repository - File not found - " + localFilePath);
		}

		String uploadAPIURL = String.format("https://%s.app.perfectomobile.com/repository/api/v1/artifacts",
				this.cloudName);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(Timeout.ofSeconds(10)).build();
		
		HttpClientBuilder clientBuilder = HttpClientBuilder.create()				
				.setDefaultRequestConfig(requestConfig);

		try (CloseableHttpClient httpClient = clientBuilder.build()) {

			Map<String, Object> requestPartMap = new HashMap<String, Object>();
			requestPartMap.put("artifactLocator", repositoryID);
			requestPartMap.put("override", true);

			ObjectMapper objectMapper = new ObjectMapper();

			String requestPartStr = objectMapper.writeValueAsString(requestPartMap);

			ContentBody inputStream = new FileBody(fileToUpload, ContentType.APPLICATION_OCTET_STREAM);

			ContentBody requestPart = new StringBody(requestPartStr, ContentType.APPLICATION_JSON);

			// Create a MultipartEntityBuilder
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();

			builder.addPart("inputStream", inputStream);
			builder.addPart("requestPart", requestPart);

			// Build the multipart entity
			HttpEntity multipartEntity = builder.build();

			// Create an HttpPost request
			HttpPost httpPost = new HttpPost(uploadAPIURL);
			httpPost.setEntity(multipartEntity);
			httpPost.setHeader("Perfecto-Authorization", this.securityToken);

			// Execute the request
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				// Get the response status code
				int statusCode = response.getCode();

				// Get the response body
				HttpEntity responseEntity = response.getEntity();

				if (statusCode != 200) {
					String responseString = EntityUtils.toString(responseEntity);
					throw new PerfectoRuntimeException(responseString);
				}
			}

		}

	}

	public static void main(String[] args) throws ParseException, IOException {
		RepositoryUtils repo = new RepositoryUtils();
		repo.uploadFile("/Users/Shared/Whatsapp.png", "PUBLIC:qrcode.png");
	}

}
