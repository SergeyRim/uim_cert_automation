package uim;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.*;


public class TestCase1 {

	String version = "3.0.3";
	String build = "10042019";

	private static final Logger log = LogManager.getLogger("TestCase1");
	
	WebDriver driver;

	//To pass driver to ScreenshotListener
	public WebDriver getDriver () {
		return driver;
	}


	@BeforeMethod(alwaysRun = true)
	@Parameters({"Driver", "RemoteDriverURL", "logLevel"})
	public void beforeSetup(String browserDriver, @Optional("http://127.0.0.1:4444/wd/hub") String RemoteDriverURL, @Optional("") String logLevel) throws MalformedURLException {

		log.info("UIM Cert Automation Testing, version " + version + " (build " + build + ")");
		if (!logLevel.equals("") && !logLevel.toLowerCase().equals("info")) {

			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
			LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

			switch (logLevel.toLowerCase()) {
				case "debug":	log.info("Changing log level to DEBUG according to XML file configuration.");
								loggerConfig.setLevel(Level.DEBUG);
								ctx.updateLoggers();
								//Bypass debug output for org.apache.http logger
								org.apache.logging.log4j.core.config.Configurator.setLevel("org.apache.http",Level.WARN);
								break;
				case "error":	log.info("Changing log level to ERROR according to XML file configuration.");
								loggerConfig.setLevel(Level.ERROR);
								ctx.updateLoggers();
								break;

				default: 		log.warn("Unsupported log level specified. Switching to INFO log level.");
								break;
			}
		}

		DesiredCapabilities dc = new DesiredCapabilities();
		Map<String, Object> prefs = new HashMap<String, Object>();
		FirefoxProfile profile = new FirefoxProfile();
		FirefoxOptions options = new FirefoxOptions();
		ChromeOptions chromeOptions = new ChromeOptions();

		String downloadedPath = System.getProperty("user.dir");

		switch (browserDriver.toLowerCase()) {
			case "firefox":			System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null");
									profile.setPreference("browser.download.folderList",2);
									profile.setPreference("browser.download.manager.showWhenStarting",false);
									profile.setPreference("browser.download.dir",downloadedPath);
									profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/zip");
									options.setProfile(profile);
									driver = new FirefoxDriver(options);
									driver.manage().window().maximize();
									break;

			case "chrome":			chromeOptions.setExperimentalOption("prefs", prefs);
									chromeOptions.addArguments("--disable-extensions");
									prefs.put("download.default_directory", downloadedPath);
									dc.setBrowserName("chrome");
									dc.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
									driver = new ChromeDriver(dc);
									driver.manage().window().maximize();
									break;

			case "ie"	 :			driver = new InternetExplorerDriver();
									break;

			case "remotefirefox" :  dc.setBrowserName("firefox");
									dc.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
									profile.setPreference("browser.download.folderList",2);
									profile.setPreference("browser.download.manager.showWhenStarting",false);
									profile.setPreference("browser.download.dir","/home/downloads/");
									profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/zip");
									dc.setCapability(FirefoxDriver.PROFILE, profile);
									driver = new RemoteWebDriver(new URL(RemoteDriverURL), dc);
									driver.manage().window().maximize();
									break;

			case "remotechrome"  :	dc.setBrowserName("chrome");
									prefs.put("download.default_directory", "/home/downloads/");
									dc.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
									chromeOptions.setExperimentalOption("prefs", prefs);
									chromeOptions.addArguments("--disable-extensions");
									chromeOptions.addArguments("--headless");
									chromeOptions.addArguments("--start-maximized");
									chromeOptions.addArguments("--disable-gpu");
									chromeOptions.addArguments("--window-size=1920,1080");
									dc.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
									driver = new RemoteWebDriver(new URL(RemoteDriverURL), dc);
									break;

			default: 				log.warn("No correct driver specified. Will use FireFox driver by default.");
									profile.setPreference("browser.download.folderList",2);
									profile.setPreference("browser.download.manager.showWhenStarting",false);
									profile.setPreference("browser.download.dir","d:\\1");
									profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/zip");
									options.setProfile(profile);
									driver = new FirefoxDriver(options);
									driver.manage().window().maximize();
									break;
		}
		//driver.manage().window().maximize();
		
	}
	
	@AfterMethod(alwaysRun = true)
	public void afterSetup() {
		//driver.quit();
	}
	
	
	@Test(description="Create Discovery Profile", groups = {"CreateDiscoveryProfile"})	
	@Parameters({"uimServer", "simIDs"})
	public void createDP(String uimServer, String simIDs) throws Exception {
		
		
		Navigation navi = new Navigation(driver);
		SnmpCollector snmp = new SnmpCollector(driver);
		Simdepot sim = new Simdepot(driver);
				
		// Get IPs for simdepot simulators
		driver.get("http://nhuser:1QAZ2wsx@simdepot.ca.com");
		String[] sims = simIDs.split(",");
			
		ArrayList<String> ips = sim.getSimIP(sims);
		
		if (ips==null) {
			log.fatal("IPs not found.");
			return;
		}
		
		//Get substring with UIM server from URL
		Matcher matcher = Pattern.compile("(?<=http://)(.+)").matcher(uimServer);
		matcher.find();
		String path2 = matcher.group();
		
		//Long way to get snmpC configuration page
		/*
		driver.get(uimServer+"/adminconsoleapp/");
		navi.loginToAdminConsole();		
		navi.gotoSNMPCollectorProbeConfiguration(path2);
		*/
		
		//Short way to get snmpC configuration page
		driver.get(uimServer+"/adminconsoleapp/jsp/ProbeConfig.jsp?probe=/"+path2+"_domain/"+path2+"_hub/"+path2+"/snmpcollector");
		navi.loginToAdminConsole();
		
		//Rewrite for multi IPs 
		snmp.createNewProfile(ips.get(0),sims[0]);
	
	}
	
	
	@Test(description="Verify MF/VC already certified", groups = {"VerifyAlreadyCertified"})	
	@Parameters({"uimServer", "readmeFile", "simIDs"})
	public void verifyMFVC (String uimServer, String readmeFile, String simIDs) throws Exception {
		
		
		Navigation navi = new Navigation(driver);
		SnmpCollector snmp = new SnmpCollector(driver);
		TxtParser parser = new TxtParser();
		
		ArrayList<String[]> vcmfList = new ArrayList<String[]>();
		vcmfList = parser.getMFVC(readmeFile);
		Assert.assertNotNull(vcmfList,"Parsed already certified MF/VC");

		
		//Get substring with UIM server from URL
		Matcher matcher = Pattern.compile("(?<=http://)(.+)").matcher(uimServer);
		matcher.find();
		String path2 = matcher.group();
		
		//Long way to get snmpC configuration page
		/*
		driver.get(uimServer+"/adminconsoleapp/");
		navi.loginToAdminConsole();		
		navi.gotoSNMPCollectorProbeConfiguration(path2);
		*/
				
		//Short way to get snmpC configuration page
		driver.get(uimServer+"/adminconsoleapp/jsp/ProbeConfig.jsp?probe=/"+path2+"_domain/"+path2+"_hub/"+path2+"/snmpcollector");
		navi.loginToAdminConsole();
		
		navi.findDevice(simIDs);
		snmp.verifyAlreadyCertifiedMFVC(vcmfList);
	
	}
	
	
	
	@Test(description="Newly added MF/VC", groups = {"NewVCMF"})	
	@Parameters({"uimServer", "readmeFile", "simIDs", "excelFile"})
	public void newVCMF (String uimServer, String readmeFile, String simIDs, String excelFile) throws Exception {
		
		ArrayList<ArrayList<String[]>> metricsList = new ArrayList<>();
		
		XLSXParser xlsx = new XLSXParser();
		SnmpCollector snmp = new SnmpCollector(driver);
		Navigation navi = new Navigation(driver);
		
		//Get substring with UIM server from URL
		Matcher matcher = Pattern.compile("(?<=http://)(.+)").matcher(uimServer);
		matcher.find();
		String path2 = matcher.group();
		
		/*
		for (int i=0; i<metricsList.size(); i++) {
			for (int j=0; j<metricsList.get(i).size(); j++) 
				System.out.println(metricsList.get(i).get(j)[0]+" "+metricsList.get(i).get(j)[1]+" ");
			System.out.println(" ");
		}
		*/
		metricsList = xlsx.getMetrics(excelFile);
		
		driver.get(uimServer+"/adminconsoleapp/jsp/ProbeConfig.jsp?probe=/"+path2+"_domain/"+path2+"_hub/"+path2+"/snmpcollector");
		navi.loginToAdminConsole();
		navi.findDevice(simIDs);

		snmp.verifyNewMFVC(metricsList, simIDs);
	}


	@Test(description="Update DCD", groups="UpdateDCD")
	@Parameters({"uimServer", "username", "password", "dcdLocation", "dcdVersion"})
	public void uploadNewDCD (String uimServer, String username, String password, @Optional String dcdLocation, @Optional String dcdVersion) throws Exception {

		Navigation navi = new Navigation(driver);
		UpdateDCD dcd = new UpdateDCD(driver);

		//Get substring with UIM server from URL
		Matcher matcher = Pattern.compile("(?<=http://)(.+)").matcher(uimServer);
		matcher.find();
		String path2 = matcher.group();

		//Download DCD archive to user workspace directory
		String downloadedDCD = dcd.downloadDCDFromTeamCity(username, password);


		//Login to Admin console
		driver.get(uimServer+"/adminconsoleapp");
		navi.loginToAdminConsole();
		navi.gotoArchive();

		//Upload DCD package
		log.info("Upload new DCD package.");
		dcd.uploadAndDeployDCD(downloadedDCD);

		//Restart SNMPd probe
		log.info("Restarting snmpCollector probe.");
		dcd.restartSnmpProbe(path2);

	}

	 
}


