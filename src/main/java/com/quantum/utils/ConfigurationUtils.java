package com.quantum.utils;

import com.perfectomobile.intellij.connector.ConnectorConfiguration;
import com.perfectomobile.intellij.connector.impl.client.ClientSideLocalFileSystemConnector;
import com.perfectomobile.intellij.connector.impl.client.ProcessOutputLogAdapter;
import com.perfectomobile.selenium.util.EclipseConnector;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.qmetry.qaf.automation.util.PropertyUtil;
import org.json.JSONObject;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by mitchellw on 9/27/2016.
 */
public class ConfigurationUtils {

    public static PropertyUtil getBaseBundle() {
        return ConfigurationManager.getBundle();
    }
    public static PropertyUtil getTestBundle() {
        return TestBaseProvider.instance().get().getContext();
    }

    public static void setMavenCapabilities() {
        String caps = getBaseBundle().getProperty("driver.mavenCapabilities") + "";
        if (caps == null || caps.indexOf("=") < 0)
            return;
        DesiredCapabilities dcaps = new DesiredCapabilities();
        for (String capKeyValue : caps.split(","))
            if (capKeyValue != null && capKeyValue.length() > 3 && capKeyValue.indexOf("=") > 0)
                dcaps.setCapability(capKeyValue.split("=")[0], capKeyValue.split("=")[1]);
        addAdditionalCapabilities(dcaps);
    }

    public static void addAdditionalCapabilities(DesiredCapabilities caps) {
        JSONObject addCapsJson = new JSONObject();
        PropertyUtil properties = getBaseBundle();
        if (properties.getProperty("driver.additional.capabilities") != null)
            addCapsJson = new JSONObject(properties.getProperty("driver.additional.capabilities").toString());

        for (Map.Entry<String, ?> cap : caps.asMap().entrySet())
            addCapsJson.put(cap.getKey(), cap.getValue());
        properties.setProperty("driver.additional.capabilities", addCapsJson.toString());
    }

    public static String getExecutionIdCapability() {
        String pluginType = getBaseBundle().getPropertyValue("driver.pluginType");
        try {
            if ("intellij".equalsIgnoreCase(pluginType)) {
                ClientSideLocalFileSystemConnector intellijConnector = new ClientSideLocalFileSystemConnector(new ProcessOutputLogAdapter(System.err, System.out, System.out, System.out));
                ConnectorConfiguration connectorConfiguration = intellijConnector.getConnectorConfiguration();
                if (connectorConfiguration != null && connectorConfiguration.getHost() != null) {
                    return connectorConfiguration.getExecutionId();
                }
            } else if ("eclipse".equalsIgnoreCase(pluginType)) {
                try {
                    EclipseConnector connector = new EclipseConnector();
                    if (connector.getHost() != null) {
                        return connector.getExecutionId();
                    }
                } catch (Exception e) {
                    System.err.println("Eclipse Connector Plugin socket not found");
                }
            }

        } catch (Exception e) {
            System.err.println("Could not connect to device in " + pluginType + " IDE Perfecto Plugin");
        }
        return "";
    }
    /**
     * Checks if is device.
     *
     * param driver capabilities
     * @return true, if is device
     */
    public static boolean isDevice(Capabilities caps){
        //first check if driver is a mobile device:
        if (isDesktopBrowser(caps))
            return false;
        return caps.getCapability("deviceName") != null;
    }

    public static boolean isDesktopBrowser(Capabilities caps){
        //first check if deviceName set to browser name which triggers desktop:
        return Arrays.asList("firefox", "chrome", "iexplorer", "internet explorer", "safari")
                .contains((caps.getCapability("browserVersion") + "").toLowerCase());
    }

    /**
     * Checks if is device.
     *
     * @param driver the driver
     * @return true, if is device
     */
    public static boolean isDevice(RemoteWebDriver driver){
        return isDevice(driver.getCapabilities());
    }

    @SuppressWarnings("unchecked")
    public static DesiredCapabilities getDesiredDeviceCapabilities() {
        Object dcaps = getBaseBundle().getObject("driver.desiredCapabilities");
        if (dcaps != null)
            return new DesiredCapabilities((Map<String, ?>) dcaps);
        return null;
    }

    @SuppressWarnings("unchecked")
    public static DesiredCapabilities getActualDeviceCapabilities(PropertyUtil bundle) {
        Object dcaps = bundle.getObject("driver.actualCapabilities");
        if (dcaps != null)
            return new DesiredCapabilities((Map<String, ?>) dcaps);
        return null;
    }

    public static void setActualDeviceCapabilities(Map<String, ?> capMap) {
        getTestBundle().setProperty("driver.actualCapabilities", capMap);
        ConsoleUtils.setThreadName();
    }

}