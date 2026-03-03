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
package com.qmetry.qaf.automation.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.configuration2.event.Event;
import org.apache.commons.configuration2.event.EventListener;

import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.step.JavaStepFinder;
import com.qmetry.qaf.automation.step.TestStep;
import com.qmetry.qaf.automation.step.client.ScenarioFactory;
import com.qmetry.qaf.automation.step.client.csv.KwdTestFactory;
import com.qmetry.qaf.automation.step.client.excel.ExcelTestFactory;
import com.qmetry.qaf.automation.step.client.text.BDDTestFactory;
import com.qmetry.qaf.automation.util.PropertyUtil;
import com.qmetry.qaf.automation.util.StringUtil;

/**
 * Configuration manager class. Singleton with early initialization.
 * <p>
 * This class loads file provided by system property
 * <code>application.properties.file</code> (Default value is
 * "resources/application.properties"). Also loads all property files form
 * <code>test.props.dir</code>(default value is "resources") if
 * <code>resources.load.subdirs</code> flag is 1.
 * <p>
 * To access any property value within automation, use following way
 * {@link PropertyUtil} props={@link #ConfigurationManager}.
 * {@link #getInstance()}.{@link#getApplicationProperties()};<br>
 * String sval = props.{@link PropertyUtil#getPropertyValue(String)}
 * 
 * @author chirag
 */
public class ConfigurationManager {
	// early initialization
	static final org.apache.commons.logging.Log log = org.apache.commons.logging.impl.LogFactoryImpl.getLog(ConfigurationManager.class);
	private static final ConfigurationManager INSTANCE = new ConfigurationManager();
	private static final ServiceLoader<QAFConfigurationListener> CONFIG_LISTENERS = ServiceLoader
    .load(QAFConfigurationListener.class);

	/**
	 * Private constructor, prevents instantiation from other classes
	 */
	private ConfigurationManager() {
		setHostName();
	}

	private void setHostName() {
		try {
			System.setProperty("host.name",
					InetAddress.getLocalHost().getHostName());
		} catch (Exception | Error e) {
			// This code added for MAC to fetch hostname
			InputStream stream=null;
			Scanner s = null;
			try {
				@SuppressWarnings("deprecation")
				Process proc = Runtime.getRuntime().exec("hostname");
				stream = proc.getInputStream();
				if (stream != null) {
					s = new Scanner(stream);
					s.useDelimiter("\\A");
					String val = s.hasNext() ? s.next() : "";
					stream.close();
					s.close();
					System.setProperty("host.name",val);
				}
			} catch (Exception | Error e1) {
				log.trace(e1);
			}finally {
				try {
					if(null!=s)s.close();
					if(null!=stream)stream.close();
				} catch (Exception e2) {
					log.trace(e2);
				}
			}
		}
	}

	private void registerLookups() {
	    // Commons-configuration2 does not support global lookups registration in the same way
	    // If needed, set interpolators per PropertyUtil instance
	}
	public static ConfigurationManager getInstance() {
		return INSTANCE;
	}

	private static InheritableThreadLocal<PropertyUtil> LocalProps =
    new InheritableThreadLocal<PropertyUtil>() {
        @Override
        protected PropertyUtil initialValue() {
            PropertyUtil p = new PropertyUtil(
                System.getProperty("application.properties.file", "resources/application.properties"));
            p.setProperty("isfw.build.info", getBuildInfo());
            p.setProperty("execution.start.ts", System.currentTimeMillis());
            File prjDir = new File(".").getAbsoluteFile().getParentFile();
            p.setProperty("project.path", prjDir.getAbsolutePath());
            if (!p.containsKey("project.name"))
                p.setProperty("project.name", prjDir.getName());
            log.debug("ISFW build info: " + p.getProperty("isfw.build.info"));
            String[] resources = p.getStringArray("env.resources", "resources");
            for (String resource : resources) {
                p.addBundle(resource);
            }
            executeOnLoadListeners(p);
            return p;
        }
        @Override
        protected PropertyUtil childValue(PropertyUtil parentValue) {
            PropertyUtil cp = new PropertyUtil(parentValue);
            return cp;
        }
    };

	/**
	 * To add local resources.
	 * 
	 * @param fileOrDir
	 */
	public static void addBundle(String fileOrDir) {
		getBundle().addBundle(fileOrDir);
	}

	public static void addAll(Map<String, ?> props) {
		ConfigurationManager.getBundle().addAll(props);
	}

	public static PropertyUtil getBundle() {
		return ConfigurationManager.LocalProps.get();
	}

	public static void setBundle(PropertyUtil bundle) {
		LocalProps.set(bundle);
	}

	private static Map<String, String> getBuildInfo() {
		Manifest manifest = null;
		Map<String, String> buildInfo = new HashMap<String, String>();
		JarFile jar = null;
		try {
			URL url = ConfigurationManager.class.getProtectionDomain().getCodeSource()
					.getLocation();
			File file = new File(url.toURI());
			jar = new JarFile(file);
			manifest = jar.getManifest();
		} catch (NullPointerException ignored) {
		} catch (URISyntaxException ignored) {
		} catch (IOException ignored) {
		} catch (IllegalArgumentException ignored) {
		} finally {
			if (null != jar)
				try {
					jar.close();
				} catch (IOException e) {
					log.warn(e.getMessage());
				}
		}

		if (manifest == null) {
			return buildInfo;
		}

		try {
			Attributes attributes = manifest.getAttributes("Build-Info");
			Set<Entry<Object, Object>> entries = attributes.entrySet();
			for (Entry<Object, Object> e : entries) {
				buildInfo.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
			}
		} catch (NullPointerException e) {
			// Fall through
		}

		return buildInfo;
	}

	/**
	 * Get test-step mapping for current configuration
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, TestStep> getStepMapping() {
		if (!ConfigurationManager.getBundle().containsKey("teststep.mapping")) {
			ConfigurationManager.getBundle().setProperty("teststep.mapping",
					JavaStepFinder.getAllJavaSteps());
			if (ConfigurationManager.getBundle()
					.containsKey(ApplicationProperties.STEP_PROVIDER_PKG.key)) {
				for (String pkg : ConfigurationManager.getBundle()
						.getStringArray(ApplicationProperties.STEP_PROVIDER_PKG.key)) {
					for (ScenarioFactory factory : getStepFactories()) {
						factory.process(pkg.replaceAll("\\.", "/"));
					}
				}
			}
		}
		return (Map<String, TestStep>) ConfigurationManager.getBundle()
				.getObject("teststep.mapping");
	}

	private static ScenarioFactory[] getStepFactories() {
		return new ScenarioFactory[]{new BDDTestFactory(Arrays.asList("bdl")),
				new KwdTestFactory(Arrays.asList("kwl")), new ExcelTestFactory()};
	}

	private static void executeOnLoadListeners(PropertyUtil bundle) {
		String[] listners = bundle.getStringArray(ApplicationProperties.QAF_LISTENERS.key);
		Iterator<QAFConfigurationListener> iter = CONFIG_LISTENERS.iterator();
		while (iter.hasNext()) {
			iter.next().onLoad(bundle);
		}
		for (String listener : listners) {
			try {
				@SuppressWarnings("deprecation")
				QAFListener cls = (QAFListener) Class.forName(listener).newInstance();
				if (QAFConfigurationListener.class.isAssignableFrom(cls.getClass()))
					((QAFConfigurationListener) cls).onLoad(bundle);
			} catch (Exception e) {
				log.error("Unable to invoke onLoad(PropertyUtil) from " + listener, e);
			}
		}
	}
	private static void executeOnChangeListeners() {
		String[] listners = getBundle().getStringArray(ApplicationProperties.QAF_LISTENERS.key);
		Iterator<QAFConfigurationListener> iter = CONFIG_LISTENERS.iterator();
		while (iter.hasNext()) {
			iter.next().onChange();
		}
		for (String listener : listners) {
			try {
				@SuppressWarnings("deprecation")
				QAFListener cls = (QAFListener) Class.forName(listener).newInstance();
				if (QAFConfigurationListener.class.isAssignableFrom(cls.getClass()))
					((QAFConfigurationListener)cls).onChange();
			} catch (Exception e) {
				log.error("Unable to invoke onChange() from " + listener, e);
			}
		}
	}
	

	private static class PropertyConfigurationListener implements EventListener<Event> {
		@Override
		public void onEvent(Event event) {
			// Update logic to handle property events as needed
		}
	}
}

