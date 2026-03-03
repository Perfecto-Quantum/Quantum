/*******************************************************************************
 * Copyright (c) 2019 Infostretch Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.qmetry.qaf.automation.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

import com.qmetry.qaf.automation.core.AutomationError;
import com.qmetry.qaf.automation.data.Base64PasswordDecryptor;
import com.qmetry.qaf.automation.data.PasswordDecryptor;
import com.qmetry.qaf.automation.http.UriProxySelector;
import com.qmetry.qaf.automation.keys.ApplicationProperties;

/**
 * com.qmetry.qaf.automation.util.PropUtil.java
 * 
 * @author chirag.jayswal
 */
// Remove all getSubstitutor() usages, fix file loading, and close class
public class PropertyUtil extends XMLConfiguration {
    private static final Log logger = LogFactoryImpl.getLog(PropertyUtil.class);

    public PropertyUtil() {
        super();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String skey = String.valueOf(entry.getKey());
            String sval = String.valueOf(entry.getValue());
            if (!StringMatcher.like("^(sun\\.|java\\.).*").match(skey)) {
                Object[] vals = sval != null && sval.indexOf(';') >= 0
                        ? sval.split(";") : new Object[] { sval };
                for (Object val : vals) {
                    super.addProperty(skey, val);
                }
            }
        }
    }

    public boolean loadProperties(InputStream in) {
        try {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                    .configure(params.properties().setEncoding("UTF-8"));
            PropertiesConfiguration propertiesConfiguration = builder.getConfiguration();
            propertiesConfiguration.read(new InputStreamReader(in, StandardCharsets.UTF_8));
            copy(propertiesConfiguration);
            propertiesConfiguration.clear();
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    public void storePropertyFile(File f) {
        try {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                    .configure(params.properties().setFile(f).setEncoding("UTF-8"));
            builder.save();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
        }
    }

    public String interpolate(String value) {
        Object result = getInterpolator().resolve(value);
        return result != null ? result.toString() : null;
    }

    public PropertyUtil(PropertyUtil prop) {
        this();
        append(prop);
    }

    public PropertyUtil(String... file) {
        this();
        load(file);
    }

    public void addAll(Map<String, ?> props) {
        props.keySet().removeAll(System.getProperties().keySet());
        copy(new MapConfiguration(props));
    }

    public PropertyUtil(File... file) {
        this();
        load(file);
    }

    public boolean load(String... files) {
        boolean r = true;
        for (String file : files) {
//            String resolvedFile = interpolate(file);
            loadFile(new File(file));
        }
        return r;
    }

    public boolean load(File... files) {
        boolean r = true;
        for (File file : files) {
            loadFile(file);
        }
        return r;
    }

    private boolean loadFile(File file) {
        String fileName = file.getName();
        try (InputStream fileInputStream = new FileInputStream(file)) {
            if (fileName.endsWith("xml") || fileName.contains(".xml.")) {
                this.read(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            } else if (fileName.endsWith(".wscj") || fileName.contains(".locj")) {
                @SuppressWarnings("unchecked")
                Map<String,Object> props = JSONUtil.getJsonObjectFromFile(file.getPath(), Map.class);
                if (props != null && !props.isEmpty()) {
                    props.entrySet().forEach(e -> {
                        String val = JSONUtil.toString(e.getValue()).replace("\\", "\\\\");
                        e.setValue(val);
                    });
                    addAll(props);
                }
            } else {
                loadProperties(fileInputStream);
            }
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    /**
	 * load property inside java/jar package
	 * 
	 * @param cls
	 * @param propertyFile
	 * @return
	 */
	public boolean load(Class<?> cls, String propertyFile) {
		boolean success = false;
		InputStream in = null;
		try {
			propertyFile = interpolate(propertyFile);
			in = cls.getResourceAsStream(propertyFile);
			if (propertyFile.endsWith("xml") || propertyFile.contains(".xml.")) {
				this.read(new InputStreamReader(in, StandardCharsets.UTF_8));
			}else {
				loadProperties(in);
			}
			success = true;
		} catch (Exception e) {
			logger.error("Unable to load properties from file:" + propertyFile, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return success;
	}

    public void addBundle(String fileOrDir) {
//        fileOrDir = interpolate(fileOrDir);
        String localResources = getString("local.reasources",
				getString("env.local.resources", "resources"));
		File resourceFile = new File(fileOrDir);
		String[] locals = getStringArray(ApplicationProperties.LOAD_LOCALES.key);
		/**
		 * will reload existing properties value(if any) if the last loaded
		 * dir/file is not the current one. case: suit-1 default, suit-2 :
		 */
		if (!fileOrDir.equalsIgnoreCase(getString("last.loaded.dir"))) {
			for (String locale : locals) {
				String key = "env." + locale + ".resources";
				String val = getString(key);
				if (val != null) {
					val = val.replace("${env.resources}", localResources);
					val = val.replace("${user.dir}", System.getProperty("user.dir"));
					val = val.replace("${project.basedir}", System.getProperty("project.basedir"));
					val = val.replace("${basedir}", System.getProperty("basedir"));
					addProperty(key, val);
				}
			}
		}
		addProperty("last.loaded.dir", fileOrDir);
		if (resourceFile.exists()) {
			loadFile(resourceFile);
		} else {
			logger.warn("Resource file or directory not found: " + fileOrDir);
		}
    }

    @Override
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public String[] getStringArray(String key, String... defaultValue) {
		String[] retVal = super.getStringArray(key);
		return (retVal != null) && (retVal.length > 0) ? retVal : defaultValue == null ? new String[] {} : defaultValue;
	}

	@Override
	public Boolean getBoolean(String key, Boolean defaultValue) {
		try {
			String sVal = getString(key, "").trim();
			boolean val = StringUtil.booleanValueOf(sVal, defaultValue);
			return val;
		} catch (Exception e) {
			return super.getBoolean(key, defaultValue);
		}
	}

	public Object getObject(String key) {
		return super.getProperty(key);
	}

	/**
	 * @param sPropertyName
	 * @return property-key value if key presents or key otherwise.
	 */
	public String getPropertyValue(String sPropertyName) {
		return getString(sPropertyName, sPropertyName);
	}

	/**
	 * @param sPropertyName
	 * @return property-key value if key presents or null otherwise
	 */
	public String getPropertyValueOrNull(String sPropertyName) {
		return getString(sPropertyName);
	}


	@SuppressWarnings("deprecation")
	public PasswordDecryptor getPasswordDecryptor() {
		String implName = getString(ApplicationProperties.PASSWORD_DECRYPTOR_IMPL.key);
		if (StringUtil.isBlank(implName)) {
			return new Base64PasswordDecryptor();
		} else {
			try {
				return (PasswordDecryptor) Class.forName(implName).newInstance();
			} catch (Exception e) {
				throw new AutomationError("Unable to get instance of PasswordDecryptor implementation", e);
			}
		}
	}

	private static void ignoreSSLCetrificatesAndHostVerification() throws NoSuchAlgorithmException, KeyManagementException {
		
		SSLContext sslContext = SSLContext.getInstance("SSL");

		// set up a TrustManager that trusts everything
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
			private final Log logger = LogFactoryImpl.getLog(this.getClass());

			public X509Certificate[] getAcceptedIssuers() {
				logger.info("======== AcceptedIssuers =============");
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				logger.info("========= ClientTrusted =============");
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				logger.info("======== ServerTrusted =============");
			}
		} }, new SecureRandom());

		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

		HostnameVerifier hostnameVerifier = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
	}
} // end of PropertyUtil class
