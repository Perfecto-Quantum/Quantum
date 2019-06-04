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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;

import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.webdriver.CommandTracker;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;
import com.qmetry.qaf.automation.util.StringUtil;
import com.quantum.utils.ConfigurationUtils;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.DeviceUtils;
import com.quantum.utils.ReportUtils;

public class PerfectoDriverListener extends QAFWebDriverCommandAdapter {
	@Override
	public void beforeCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {
		
		if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.QUIT)) {
			ConfigurationUtils.setActualDeviceCapabilities(driver.getCapabilities().asMap());
			try {
				String appName = (String) driver.getCapabilities().getCapability("applicationName");
				if (StringUtil.isNotBlank(appName)
						&& StringUtil.isBlank((String) driver.getCapabilities().getCapability("eclipseExecutionId"))) {

					DeviceUtils.closeApp(appName, "name", true, driver);
				}
			} catch (Exception ex) {
			}
			if (ConfigurationManager.getBundle().getString("remote.server").toLowerCase()
					.contains(".perfectomobile.com")) {

				try {
					Map<String, Object> params = new HashMap<>();
					driver.executeScript("mobile:execution:close", params);
				} catch (Exception ex) {
				}

				try {
					driver.close();
				} catch (Exception ex) {
				}
			}
		}

	}

	@Override
	public void afterCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {
		if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.QUIT)) {

			try {

				if(ConfigurationUtils.getBaseBundle().getString("remote.server", "").contains("perfecto")) {
					if (ConfigurationManager.getBundle().getString("perfecto.download.reports", "false").toLowerCase()
							.equals("true")) {
						try {
							System.out.println("downloading test reports");
							ReportUtils.generateTestReport(ConfigurationManager.getBundle().getString("executionId"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ConsoleUtils.logError(e.toString());
						}
					}
					if (ConfigurationManager.getBundle().getString("perfecto.download.summaryReports", "false")
							.toLowerCase().equals("true")) {

						try {
							System.out.println("downloading summary reports");
							ReportUtils.generateSummaryReports(ConfigurationManager.getBundle().getString("executionId"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ConsoleUtils.logError(e.toString());
						}
					}
					if (ConfigurationManager.getBundle().getString("perfecto.download.video", "false").toLowerCase()
							.equals("true")) {
						try {
							System.out.println("downloading video");
							ReportUtils.downloadReportVideo(ConfigurationManager.getBundle().getString("executionId"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ConsoleUtils.logError(e.toString());
						}
					}
					if (ConfigurationManager.getBundle().getString("perfecto.download.attachments", "false").toLowerCase()
							.equals("true")) {
						try {
							System.out.println("downloading attachments");
							ReportUtils
									.downloadReportAttachments(ConfigurationManager.getBundle().getString("executionId"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ConsoleUtils.logError(e.toString());
						}
					}
				}
				
			} catch (Exception ex) {

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

	}

	@Override
	public void onInitialize(QAFExtendedWebDriver driver) {
		// Commenting the code to get device info as it fails in two conditions - when
		// there is no security token mentioned and when the execution environment is
		// beind proxy.
		// MutableCapabilities dcaps =
		// CloudUtils.getDeviceProperties((MutableCapabilities)
		// driver.getCapabilities());
		 ConfigurationUtils.setActualDeviceCapabilities(driver.getCapabilities().asMap());
		// ConsoleUtils.logWarningBlocks("DEVICE PROPERTIES: " + dcaps.toString());
		ConsoleUtils.logWarningBlocks("DEVICE PROPERTIES: " + driver.getCapabilities().toString());

		Long implicitWait = ConfigurationManager.getBundle().getLong("seleniun.wait.implicit", 0);
		driver.manage().timeouts().implicitlyWait(implicitWait, TimeUnit.MILLISECONDS);

		if (ConfigurationManager.getBundle().getString("remote.server").toLowerCase().contains(".perfectomobile.com")) {
			ConfigurationManager.getBundle().addProperty("executionId",
					driver.getCapabilities().getCapability("executionId"));
		}
	}

}
