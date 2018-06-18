/*******************************************************************************
 * QMetry Automation Framework provides a powerful and versatile platform to
 * author
 * Automated Test Cases in Behavior Driven, Keyword Driven or Code Driven
 * approach
 * Copyright 2016 Infostretch Corporation
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 * You should have received a copy of the GNU General Public License along with
 * this program in the name of LICENSE.txt in the root folder of the
 * distribution. If not, see https://opensource.org/licenses/gpl-3.0.html
 * See the NOTICE.TXT file in root folder of this source files distribution
 * for additional information regarding copyright ownership and licenses
 * of other open source software / files used by QMetry Automation Framework.
 * For any inquiry or need additional information, please contact
 * support-qaf@infostretch.com
 *******************************************************************************/

package com.quantum.listeners;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;
import org.testng.TestNGException;

import com.google.common.base.Strings;
import com.qmetry.qaf.automation.core.AutomationError;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.webdriver.CommandTracker;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;
import com.qmetry.qaf.automation.util.StringUtil;
import com.quantum.utils.CloudUtils;
import com.quantum.utils.ConfigurationUtils;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.DeviceUtils;
import com.quantum.utils.ReportUtils;
import com.quantum.utils.RestAPIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class PerfectoDriverListener extends QAFWebDriverCommandAdapter {
	@Override
	public void beforeCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {
		if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.CLOSE)) {
			if (ConfigurationManager.getBundle().getString("remote.server").toLowerCase()
					.contains(".perfectomobile.com")) {
				ConfigurationManager.getBundle().addProperty("executionId",
						driver.getCapabilities().getCapability("executionId"));
			}
		} else if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.QUIT)) {

			try {
				String appName = (String) driver.getCapabilities().getCapability("applicationName");
				if (StringUtil.isNotBlank(appName)
						&& StringUtil.isBlank((String) driver.getCapabilities().getCapability("eclipseExecutionId"))) {
					try {
						DeviceUtils.closeApp(appName, "name", true, driver);

					} catch (Exception e) {
					}
				}
				driver.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void beforeInitialize(Capabilities desiredCapabilities) {
		if (ConfigurationUtils.getBaseBundle().getString("remote.server", "").contains("perfecto")) {
			String eclipseExecutionId = ConfigurationUtils.getExecutionIdCapability();

			if (StringUtil.isNotBlank(eclipseExecutionId)) {
				((DesiredCapabilities) desiredCapabilities).setCapability("eclipseExecutionId", eclipseExecutionId);
			}
		}

		String jobName = getBundle().getString("JOB_NAME", System.getProperty("reportium-job-name"));
		int jobNumber = getBundle().getInt("BUILD_NUMBER", System.getProperty("reportium-job-number") == null ? 0
				: Integer.parseInt(System.getProperty("reportium-job-number")));
		String jobBranch = System.getProperty("reportium-job-branch");
		String tags = System.getProperty("reportium-tags");

		if (jobName != null) {
			((DesiredCapabilities) desiredCapabilities).setCapability("report.jobName", jobName);
			((DesiredCapabilities) desiredCapabilities).setCapability("report.jobNumber", jobNumber);
		}
		if (jobBranch != null) {
			((DesiredCapabilities) desiredCapabilities).setCapability("report.jobBranch", jobBranch);
		}
		if (tags != null) {
			((DesiredCapabilities) desiredCapabilities).setCapability("report.tags", tags);
		}
		if(!Strings.isNullOrEmpty(((DesiredCapabilities)desiredCapabilities).getCapability("deviceName").toString()))
        {
            try {
                String res=RestAPIUtils.retrieveDeviceInfo(((DesiredCapabilities) desiredCapabilities).getCapability("deviceName").toString());
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

                DocumentBuilder builder = domFactory.newDocumentBuilder();
                Document dDoc = builder.parse(new InputSource((new StringReader(res))));

                XPath xPath = XPathFactory.newInstance().newXPath();
                Node ndInuse = (Node) xPath.evaluate("handset/inUse", dDoc, XPathConstants.NODE);
                Node ndavailable = (Node) xPath.evaluate("handset/available", dDoc, XPathConstants.NODE);
                Node ndStatus = (Node) xPath.evaluate("handset/status", dDoc, XPathConstants.NODE);
                if (Boolean.valueOf(ndInuse.getTextContent()) || !Boolean.valueOf(ndavailable.getTextContent())
                		||ndStatus.getTextContent()!="Connected")
                    throw new AutomationError("Device is unavailable or having error...");
            }catch (AutomationError se)
            {
                throw se;
            }
            catch(Exception e)
            {
                //ignore
            }
        }

	}

	@Override
	public void onInitialize(QAFExtendedWebDriver driver) {
		MutableCapabilities dcaps = CloudUtils.getDeviceProperties((MutableCapabilities) driver.getCapabilities());
		ConfigurationUtils.setActualDeviceCapabilities(dcaps.asMap());
		ConsoleUtils.logWarningBlocks("DEVICE PROPERTIES: " + dcaps.toString());

		Long implicitWait = ConfigurationManager.getBundle().getLong("seleniun.wait.implicit", 0);
		driver.manage().timeouts().implicitlyWait(implicitWait, TimeUnit.MILLISECONDS);
	}

	@Override
	public void afterCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {

		if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.CLOSE)) {
			{
				if (ConfigurationManager.getBundle().getString("remote.server").toLowerCase()
						.contains(".perfectomobile.com")) {

					if (ConfigurationManager.getBundle().getString("perfecto.download.reports", "false").toLowerCase()
							.equals("true")) {
						try {
							ReportUtils.generateTestReport(ConfigurationManager.getBundle().getString("executionId"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ConsoleUtils.logError(e.toString());
						}
					}
					if (ConfigurationManager.getBundle().getString("perfecto.download.summaryReports", "false")
							.toLowerCase().equals("true")) {

						try {
							ReportUtils
									.generateSummaryReports(ConfigurationManager.getBundle().getString("executionId"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ConsoleUtils.logError(e.toString());
						}
					}
				}
			}
		}
	}
}
