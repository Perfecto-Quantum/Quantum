package com.quantum.utils;


import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.DriverUtils;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

public class RestAPIUtils {

	public static void exeRestCmd(String cmd,String subcmd,Map<String, String> params) throws Exception {
		QAFExtendedWebDriver d=DriverUtils.getDriver();
		Map<String, String> actions=new HashMap<String, String>();
		
		actions.put("command", cmd);
		actions.put("subcommand", subcmd);
		
		params.put("deviceId",d.getCapabilities().getCapability("deviceName").toString());
		String svcStr="executions/"+d.getCapabilities().getCapability("executionId").toString();
		String res=exeRestOps("command",svcStr,actions,params);
		ConsoleUtils.logWarningBlocks("Step result:" +res);
		Assert.assertTrue(res.toLowerCase().contains("success"),res);
		
	}

	public static String retrieveDeviceInfo(String deviceId) throws Exception {
//		QAFExtendedWebDriver d=DriverUtils.getDriver();
		Map<String, String> actions=new HashMap<String, String>();
		Map<String, String> params=new HashMap<String, String>();

		String svcStr="handsets/"+deviceId;

		return exeRestOps("info",svcStr,actions,params);

	}
	public static String exeRestOps(String Ops,String serviceStr,Map<String, String> actions,Map<String, String> params) throws Exception {


		String cloudServer =
				new URL(ConfigurationManager.getBundle().getString("remote.server")).getHost();
	    String securityToken = ConfigurationManager.getBundle().getString("perfecto.capabilities.securityToken") ;
	    String user = ConfigurationManager.getBundle().getString("perfecto.capabilities.user") ;
	    String password = ConfigurationManager.getBundle().getString("perfecto.capabilities.password") ;
	    String authStr;
	    String actionStr;
	    String paramStr;

	    
	    if (  null ==securityToken || securityToken.trim().isEmpty())
	    	authStr="&user=" + user
					+ "&password=" + password;
	    else
	    	authStr="&securityToken=" + securityToken;
	    
	    actionStr="";
		for (Map.Entry<String, String> et:actions.entrySet())
		{
			actionStr=actionStr+"&"+et.getKey()+"="+et.getValue();
			
		}
		
				
		paramStr="";
		for (Map.Entry<String, String> et:params.entrySet())
		{
			paramStr=paramStr+"&param." + et.getKey()+"="+URLEncoder.encode(et.getValue(), "UTF-8");
			
		}
		
		String url = "https://"
				+ cloudServer
				+ "/services/"
				+ serviceStr
				+ "?operation=" +Ops
				+ authStr
				+ actionStr
				+ paramStr;
			
		
		URL obj = new URL(url);
		Proxy proxy=getProxy();
		HttpURLConnection con=null;

	    con = proxy!=null?
	    		(HttpURLConnection) obj.openConnection(proxy):
	    			(HttpURLConnection) obj.openConnection();
		
		con.setRequestMethod("GET");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		return response.toString();
	}
	
	private static Proxy getProxy() {
        Proxy proxy = null;
        
        if (ConfigurationManager.getBundle().getString("proxyHost") != null
                      && !ConfigurationManager.getBundle().getString("proxyHost").toString().equals("")) {
               
               String authUser=
                           (ConfigurationManager.getBundle().getString("proxyDomain")+"").trim()!=""?
                                          ConfigurationManager.getBundle().getString("proxyUser")+"":
                                                 ConfigurationManager.getBundle().getString("proxyDomain")+"" 
                             +"\\"+ ConfigurationManager.getBundle().getString("proxyUser")+"";
               String authPass=
                            ConfigurationManager.getBundle().getString("proxyPassword")+"";
               
               Authenticator authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(authUser,authPass.toCharArray()));
                }
            };
           
            Authenticator.setDefault(authenticator);
           
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ConfigurationManager.getBundle().getString("proxyHost").toString(), 
                      Integer.parseInt(
                                          ConfigurationManager.getBundle().getString("proxyPort").toString())
                      ));
           
     }
        return proxy;
 }



	public static void loadMyCert() throws Exception {
	 KeyStore keyStore = KeyStore.getInstance("JKS");
	 
	 System.setProperty("javax.net.ssl.trustStore", "resources/cacerts");
	}

}


