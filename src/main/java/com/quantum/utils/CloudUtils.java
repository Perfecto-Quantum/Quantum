package com.quantum.utils;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;
import static com.quantum.utils.ConfigurationUtils.getBaseBundle;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.json.JSONObject;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.google.common.base.CaseFormat;
import com.perfectomobile.httpclient.Credentials;
import com.perfectomobile.httpclient.HttpClientException;
import com.perfectomobile.httpclient.ParameterValue;
import com.perfectomobile.httpclient.device.DeviceParameter;
import com.perfectomobile.httpclient.device.DeviceResult;
import com.perfectomobile.httpclient.device.DevicesHttpClient;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;

/**
 * Created by mitchellw on 9/27/2016.
 */
public class CloudUtils {

	private static final String HTTPS = "https://";
	private static final String MEDIA_REPOSITORY = "/services/repositories/media/";
	private static final String UPLOAD_OPERATION = "operation=upload&overwrite=true";
	private static final String UTF_8 = "UTF-8";

	public static DevicesHttpClient getHttpClient() {
		return new DevicesHttpClient(getHostName(), getCredentials(ConfigurationUtils.getDesiredDeviceCapabilities()));
	}

	public static MutableCapabilities getDeviceProperties() {
		return getDeviceProperties(ConfigurationUtils.getActualDeviceCapabilities(ConfigurationUtils.getBaseBundle()));
	}

	public static MutableCapabilities getDeviceProperties(MutableCapabilities desiredCapabilities) {

		if (!ConfigurationUtils.isDevice(desiredCapabilities))
			return desiredCapabilities;

		DeviceResult device = null;
		try {
			device = getHttpClient().deviceInfo(desiredCapabilities.getCapability("deviceName").toString(), false);
		} catch (HttpClientException e) {
			e.printStackTrace();
		}

		for (DeviceParameter parameter : DeviceParameter.values()) {
			String paramValue = device.getResponseValue(parameter);
			String capName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, parameter.toString().toLowerCase());
			if (!StringUtils.isEmpty(paramValue))
				desiredCapabilities.setCapability(capName, paramValue);
		}

		return desiredCapabilities;
	}

	public static List<DeviceResult> getDeviceList(List<ParameterValue> inputParameters) {
		try {
			return getHttpClient().listDevices(inputParameters, false);
		} catch (HttpClientException e1) {
			ConsoleUtils.logError(e1.getMessage());
			return null;
		}
	}

	public static List<ParameterValue> convertMapToInputParameters(Map<String, ?> mapValues) {
		List<ParameterValue> inputParameters = new LinkedList<>();
		inputParameters.add(new ParameterValue("availableTo",
				getCredentials(ConfigurationUtils.getDesiredDeviceCapabilities()).getUser()));
		inputParameters.add(new ParameterValue("inUse", "false"));

		mapValues.forEach((k, v) -> {
			if ("deviceName".equals(k))
				k = "deviceId";
			if ("platformName".equals(k))
				k = "os";
			if (new ArrayList<>(
					Arrays.asList("model", "os", "platformName", "deviceName", "platformVersion", "deviceId", "inUse"))
					.contains(k))
				inputParameters.add(new ParameterValue(k, v + ""));
		});

		return inputParameters;
	}

	/**
	 * Use to get connected devices and supply reults to TestNG Factory
	 *
	 */
	public static Iterator<Object[]> getConnectedDevices(Map<String, ?> mapValues) {
		List<Object[]> deviceResultList = new ArrayList<>();
		CloudUtils.getDeviceList(CloudUtils.convertMapToInputParameters(mapValues))
		.forEach(deviceResult -> deviceResultList.add(new Object[] { deviceResult }));
		return deviceResultList.iterator();
	}

	public static boolean isDeviceAvailable() {

		try {
			List<DeviceResult> deviceList = getDeviceList(
					convertMapToInputParameters(ConfigurationUtils.getDesiredDeviceCapabilities().asMap()));
			if (deviceList == null || deviceList.size() == 0)
				return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static Credentials getCredentials(DesiredCapabilities dcaps) {
		Map<String, Object> dCapMap = (Map<String, Object>) dcaps.asMap();
		String user = dCapMap.get("user") != null ? (String) dCapMap.get("user") : "";
		String password = dCapMap.get("password") != null ? (String) dCapMap.get("password") : "";
		String offlineToken = dCapMap.get("offlineToken") != null ? (String) dCapMap.get("securityToken") : "";
		String accessToken = dCapMap.get("securityToken") != null ? (String) dCapMap.get("securityToken") : "";
		offlineToken = accessToken != null ? accessToken : offlineToken;
		return new Credentials(user, password, offlineToken, accessToken);
	}

	public static String getHostName() {
		return getBaseBundle().getPropertyValue("remote.server").replace("https://", "")
				.replace("/nexperience/perfectomobile/wd/hub", "");
	}

	/**
	 * Download the report. type - pdf, html, csv, xml Example:
	 * downloadReport(driver, "pdf", "C:\\test\\report");
	 */

	public static void downloadReport(String type, String fileName) throws IOException {
		try {
			String command = "mobile:report:download";
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("type", type);
			String report = (String) new WebDriverTestBase().getDriver().executeScript(command, params);
			File reportFile = new File(fileName + "." + type);
			BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(reportFile));
			byte[] reportBytes = OutputType.BYTES.convertFromBase64Png(report);
			output.write(reportBytes);
			output.close();
		} catch (Exception ex) {
			System.out.println("Got exception " + ex);
		}
	}

	/**
	 * Download all the report attachments with a certain type. type - video, image,
	 * vital, network Examples: downloadAttachment(driver, "video",
	 * "C:\\test\\report\\video", "flv"); downloadAttachment(driver, "image",
	 * "C:\\test\\report\\images", "jpg");
	 */
	public static void downloadAttachment(String type, String fileName, String suffix) throws IOException {
		try {
			String command = "mobile:report:attachment";
			boolean done = false;
			int index = 0;

			while (!done) {
				Map<String, Object> params = new HashMap<String, Object>();

				params.put("type", type);
				params.put("index", Integer.toString(index));

				String attachment = (String) new WebDriverTestBase().getDriver().executeScript(command, params);

				if (attachment == null) {
					done = true;
				} else {
					File file = new File(fileName + index + "." + suffix);
					BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
					byte[] bytes = OutputType.BYTES.convertFromBase64Png(attachment);
					output.write(bytes);
					output.close();
					index++;
				}
			}
		} catch (Exception ex) {
			System.out.println("Got exception " + ex);
		}
	}

	/**
	 * Uploads a file to the media repository. Example:
	 * uploadMedia("C:\\test\\ApiDemos.apk", "PRIVATE:apps/ApiDemos.apk");
	 * @throws Exception 
	 */
	public static void uploadMedia(String path, String repositoryKey) throws Exception {
		URI uri = new URI(ConfigurationManager.getBundle().getString("remote.server"));
		String hostName = uri.getHost();
		String encodedPassword = ReportUtils.getToken();
		sendRequest(hostName, encodedPassword, new File(path), repositoryKey);
	}

	@SuppressWarnings("unused")
	private static void sendRequest(String host, String token, File file, String repositoryKey) throws Exception {
		if(host.contains(".perfectomobile.com")) {
			host = host.replace(".perfectomobile.com", "");
		}
		URIBuilder taskUriBuilder = new URIBuilder("https://"+host+".app.perfectomobile.com/repository/api/v1/artifacts");

		CloseableHttpClient httpClient = null;
		HttpPost httppost = null;
		HttpResponse response = null;

		if (ConfigurationManager.getBundle().getString("proxyHost") != null
				&& !ConfigurationManager.getBundle().getString("proxyHost").equals("")) {

			if (ConfigurationManager.getBundle().getString("proxyPort") == null) {
				throw new Exception("Please mention the NTLM port in the application properties file");
			}
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			NTCredentials ntCreds = new NTCredentials(ConfigurationManager.getBundle().getString("proxyUser", ""),
					ConfigurationManager.getBundle().getString("proxyPassword", ""), hostname,
					ConfigurationManager.getBundle().getString("proxyDomain", ""));

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(ConfigurationManager.getBundle().getString("proxyHost"),
					Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort"))), ntCreds);
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();

			clientBuilder.useSystemProperties();
			clientBuilder.setProxy(new HttpHost(ConfigurationManager.getBundle().getString("proxyHost"),
					Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort"))));
			clientBuilder.setDefaultCredentialsProvider(credsProvider);
			clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
			httpClient = clientBuilder.build();
		}else {
			httpClient = new DefaultHttpClient();
		}

		httppost = new HttpPost(taskUriBuilder.build());

		httppost.setHeader("Perfecto-Authorization", token);

		MultipartEntity mpEntity = new MultipartEntity();

		ContentBody inputStream = new FileBody(file, ContentType.APPLICATION_OCTET_STREAM);

		JSONObject req = new JSONObject();
		req.put("artifactLocator", repositoryKey);
		req.put("override", true);
		//req.put(ARTIFACT_NAME_LOCATOR_KEY, appFileName);

		String rp = req.toString();

		ContentBody requestPart = new StringBody(rp, ContentType.APPLICATION_JSON);

		mpEntity.addPart("inputStream", inputStream);
		mpEntity.addPart("requestPart", requestPart);
		httppost.setEntity(mpEntity);
		response = httpClient.execute(httppost);
		int statusCode = response.getStatusLine().getStatusCode();
		System.out.println("Status code:"+statusCode);
	}

	/**
	 * Uploads a file to the media repository. Example:
	 * uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com", "123456",
	 * "C:\\test\\ApiDemos.apk", "PRIVATE:apps/ApiDemos.apk");
	 * @throws Exception 
	 */
	public static void uploadMedia(String host, String user, String token, String path, String repositoryKey)
			throws Exception {
		sendRequest(host, token, new File(path), repositoryKey);
	}

	/**
	 * Uploads a file to the media repository. Example: URL url = new URL(
	 * "http://file.appsapk.com/wp-content/uploads/downloads/Sudoku%20Free.apk") ;
	 * uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com", "123456",
	 * url, "PRIVATE:apps/ApiDemos.apk");
	 * @throws Exception 
	 */
	public static void uploadMedia(String host, String user, String token, URL mediaURL, String repositoryKey)
			throws Exception {
		File file = new File(mediaURL.toURI());
		sendRequest(host, token, file, repositoryKey);
	}

	@SuppressWarnings("unused")
	private static String getQueryString(Map<String, String> params) throws UnsupportedEncodingException {

		StringBuilder sb = new StringBuilder();
		if (!params.containsKey("user") || !params.containsKey("password")) {
			params.put("user", "driver.capabilities.user");
			params.put("password", "driver.capabilities.password");
		}
		for (Map.Entry<String, String> e : params.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.displayName())).append('=').append(URLEncoder
					.encode(getBundle().getString(e.getValue(), e.getValue()), StandardCharsets.UTF_8.displayName()));
		}
		return sb.toString();
	}

	private static void handleError(HttpURLConnection connection) throws IOException {
		String msg = "Failed to upload media.";
		InputStream errorStream = connection.getErrorStream();
		if (errorStream != null) {
			InputStreamReader inputStreamReader = new InputStreamReader(errorStream, UTF_8);
			BufferedReader bufferReader = new BufferedReader(inputStreamReader);
			try {
				StringBuilder builder = new StringBuilder();
				String outputString;
				while ((outputString = bufferReader.readLine()) != null) {
					if (builder.length() != 0) {
						builder.append("\n");
					}
					builder.append(outputString);
				}
				String response = builder.toString();
				msg += "Response: " + response;
			} finally {
				bufferReader.close();
			}
		}
		throw new RuntimeException(msg);
	}

	private static byte[] readFile(File path) throws FileNotFoundException, IOException {
		int length = (int) path.length();
		byte[] content = new byte[length];
		InputStream inStream = new FileInputStream(path);
		try {
			inStream.read(content);
		} finally {
			inStream.close();
		}
		return content;
	}

	private static byte[] readURL(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		int code = connection.getResponseCode();
		if (code > HttpURLConnection.HTTP_OK) {
			handleError(connection);
		}
		InputStream stream = connection.getInputStream();

		if (stream == null) {
			throw new RuntimeException("Failed to get content from url " + url + " - no response stream");
		}
		byte[] content = read(stream);
		return content;
	}

	private static byte[] read(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int nBytes = 0;
			while ((nBytes = input.read(buffer)) > 0) {
				output.write(buffer, 0, nBytes);
			}
			byte[] result = output.toByteArray();
			return result;
		} finally {
			try {
				input.close();
			} catch (IOException e) {

			}
		}
	}
}
