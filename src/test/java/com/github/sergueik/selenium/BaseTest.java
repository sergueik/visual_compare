package com.github.sergueik.selenium;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class BaseTest {

	private static String osName = null;
	private static String tmpDir = (getOSName().equals("windows")) ? "c:\\temp"
			: "/tmp";

	public WebDriver driver;
	public WebDriverWait wait;
	public Actions actions;
	public JavascriptExecutor js;
	private TakesScreenshot screenshot;

	private static Map<String, String> config = new HashMap<>();
	static {
		config.put("success", "screenshots" + File.separator + "pass");
		config.put("failure", "screenshots" + File.separator + "fail");
	}

	protected static final String browser = "chrome";

	private int scriptTimeout = 5;
	private int flexibleWait = 120;
	private int implicitWait = 1;
	private long pollingInterval = 500;

	public String baseURL = "about:blank";

	private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

	@AfterClass
	public void afterSuiteMethod() throws Exception {
		driver.quit();
	}

	@SuppressWarnings("deprecation")
	@BeforeClass
	public void setupTestClass(ITestContext context) throws IOException {

		getOSName();
		if (browser.equals("chrome")) {

			System.setProperty("webdriver.chrome.driver", osName.equals("windows")
					? (new File("c:/java/selenium/chromedriver.exe")).getAbsolutePath()
					: String.format("%s/Downloads/chromedriver", System.getenv("HOME")));

			DesiredCapabilities capabilities = DesiredCapabilities.chrome();
			ChromeOptions options = new ChromeOptions();

			HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
			chromePrefs.put("profile.default_content_settings.popups", 0);
			String downloadFilepath = System.getProperty("user.dir") + File.separator
					+ "target" + File.separator;
			chromePrefs.put("download.default_directory", downloadFilepath);
			chromePrefs.put("enableNetwork", "true");
			options.setExperimentalOption("prefs", chromePrefs);
			options.addArguments("allow-running-insecure-content");
			options.addArguments("allow-insecure-localhost");
			options.addArguments("enable-local-file-accesses");
			options.addArguments("disable-notifications");
			// options.addArguments("start-maximized");
			options.addArguments("browser.download.folderList=2");
			options.addArguments(
					"--browser.helperApps.neverAsk.saveToDisk=image/jpg,text/csv,text/xml,application/xml,application/vnd.ms-excel,application/x-excel,application/x-msexcel,application/excel,application/pdf");
			options.addArguments("browser.download.dir=" + downloadFilepath);
			// options.addArguments("user-data-dir=/path/to/your/custom/profile");
			capabilities
					.setBrowserName(DesiredCapabilities.chrome().getBrowserName());
			capabilities.setCapability(ChromeOptions.CAPABILITY, options);
			capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
			driver = new ChromeDriver(capabilities);
		} else if (browser.equals("firefox")) {

			System.setProperty("webdriver.gecko.driver", osName.equals("windows")
					? new File("c:/java/selenium/geckodriver.exe").getAbsolutePath()
					: String.format("%s/Downloads/geckodriver", System.getenv("HOME")));
			// the '/tmp' or '/var/run' directory may have noexec set

			System
					.setProperty("webdriver.firefox.bin",
							osName.equals("windows") ? new File(
									"c:/Program Files (x86)/Mozilla Firefox/firefox.exe")
											.getAbsolutePath()
									: "/usr/bin/firefox");
			// https://github.com/SeleniumHQ/selenium/wiki/DesiredCapabilities
			DesiredCapabilities capabilities = DesiredCapabilities.firefox();
			// use legacy FirefoxDriver
			capabilities.setCapability("marionette", false);
			// http://www.programcreek.com/java-api-examples/index.php?api=org.openqa.selenium.firefox.FirefoxProfile
			capabilities.setCapability("locationContextEnabled", false);
			capabilities.setCapability("acceptSslCerts", true);
			capabilities.setCapability("elementScrollBehavior", 1);
			FirefoxProfile profile = new FirefoxProfile();
			profile.setAcceptUntrustedCertificates(true);
			profile.setAssumeUntrustedCertificateIssuer(true);

			System.out.println(System.getProperty("user.dir"));
			capabilities.setCapability(FirefoxDriver.PROFILE, profile);
			try {
				driver = new FirefoxDriver(capabilities);
			} catch (WebDriverException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot initialize Firefox driver");
			}
		}
		actions = new Actions(driver);

		driver.manage().timeouts().setScriptTimeout(scriptTimeout,
				TimeUnit.SECONDS);
		// Declare a wait time
		wait = new WebDriverWait(driver, flexibleWait);
		wait.pollingEvery(pollingInterval, TimeUnit.MILLISECONDS);
		screenshot = ((TakesScreenshot) driver);
		js = ((JavascriptExecutor) driver);
		driver.manage().window().maximize();
		context.setAttribute("driver", driver);
		context.setAttribute("config", config);
		// Go to URL
		driver.get(baseURL);
	}

	public void highlight(WebElement element) {
		highlight(element, 100);
	}

	@SuppressWarnings("deprecation")
	public void highlight(WebElement element, long highlight_interval) {
		if (wait == null) {
			wait = new WebDriverWait(driver, flexibleWait);
		}
		wait.pollingEvery(pollingInterval, TimeUnit.MILLISECONDS);
		try {
			wait.until(ExpectedConditions.visibilityOf(element));
			js.executeScript("arguments[0].style.border='3px solid yellow'", element);
			Thread.sleep(highlight_interval);
			js.executeScript("arguments[0].style.border=''", element);
		} catch (InterruptedException e) {
			// System.err.println("Ignored: " + e.toString());
		}
	}

	public static String getOSName() {
		if (osName == null) {
			osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows")) {
				osName = "windows";
			}
		}
		return osName;
	}
	// based on:
	// https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java

	/**
	* Adds an environment variable the containing jvm run
	* leaves the underlying system environment unmodified
	* @param key The Name of the variable to set
	* @param value The value of the variable to set
	*/
	@SuppressWarnings("unchecked")
	public static <K, V> void setenv(final String key, final String value) {
		try {
			final Class<?> processEnvironmentClass = Class
					.forName("java.lang.ProcessEnvironment");
			final Field theEnvironmentField = processEnvironmentClass
					.getDeclaredField("theEnvironment");
			final boolean environmentAccessibility = theEnvironmentField
					.isAccessible();
			theEnvironmentField.setAccessible(true);

			final Map<K, V> env = (Map<K, V>) theEnvironmentField.get(null);

			if (osName.equals("windows")) {
				if (value == null) {
					env.remove(key);
				} else {
					env.put((K) key, (V) value);
				}
			} else {
				final Class<K> variableClass = (Class<K>) Class
						.forName("java.lang.ProcessEnvironment$Variable");
				final Method convertToVariable = variableClass.getMethod("valueOf",
						String.class);
				final boolean conversionVariableAccessibility = convertToVariable
						.isAccessible();
				convertToVariable.setAccessible(true);

				final Class<V> valueClass = (Class<V>) Class
						.forName("java.lang.ProcessEnvironment$Value");
				final Method convertToValue = valueClass.getMethod("valueOf",
						String.class);
				final boolean conversionValueAccessibility = convertToValue
						.isAccessible();
				convertToValue.setAccessible(true);

				if (value == null) {
					env.remove(convertToVariable.invoke(null, key));
				} else {
					env.put((K) convertToVariable.invoke(null, key),
							(V) convertToValue.invoke(null, value));

					convertToValue.setAccessible(conversionValueAccessibility);
					convertToVariable.setAccessible(conversionVariableAccessibility);
				}
			}
			theEnvironmentField.setAccessible(environmentAccessibility);

			// OSX: apply the same to the case insensitive environment
			final Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
					.getDeclaredField("theCaseInsensitiveEnvironment");
			final boolean insensitiveAccessibility = theCaseInsensitiveEnvironmentField
					.isAccessible();
			theCaseInsensitiveEnvironmentField.setAccessible(true);
			// Not entirely sure if this cast is needed
			final Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField
					.get(null);
			if (value == null) {
				cienv.remove(key);
			} else {
				cienv.put(key, value);
			}
			theCaseInsensitiveEnvironmentField
					.setAccessible(insensitiveAccessibility);
		} catch (final InvocationTargetException | ClassNotFoundException
				| NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException(
					String.format("Failed setting environment variable \"%s\" to \"%s\"",
							key, value),
					e);
		} catch (final NoSuchFieldException e) {
			final Map<String, String> env = System.getenv();
			Arrays.asList(Collections.class.getDeclaredClasses()).stream()
					.filter(_class -> "java.util.Collections$UnmodifiableMap"
							.equals(_class.getName()))
					.map(_class -> {
						try {
							return _class.getDeclaredField("m");
						} catch (final NoSuchFieldException e1) {
							throw new IllegalStateException(String.format(
									"Failed setting environment variable \"%s\" to \"%s\" "
											+ "when locating in-class memory map of environment",
									key, value), e1);
						}
					}).forEach(field -> {
						try {
							final boolean fieldAccessibility = field.isAccessible();
							field.setAccessible(true);
							final Map<String, String> map = (Map<String, String>) field
									.get(env);
							if (value == null) {
								map.remove(key);
							} else {
								map.put(key, value);
							}
							// reset the accessibility
							field.setAccessible(fieldAccessibility);
						} catch (final ConcurrentModificationException e1) {
							System.err.println("Exception from attempt to modify source map: "
									+ field.getDeclaringClass() + "#" + field.getName() + " : "
									+ e1);
						} catch (final IllegalAccessException e1) {
							throw new IllegalStateException(String.format(
									"Failed setting environment variable \"%s\" to \"%s\" "
											+ "Unable to access field!",
									key, value), e1);
						}
					});
		}
		System.err.println(
				String.format("Set environment variable \"%s\" to \"%s\"", key, value));
	}

	protected static final String buildPathtoResourceFile(String fileName) {
		return String.join(File.separator, Arrays.asList(
				System.getProperty("user.dir"), "src", "test", "resources", fileName));
	}

	public void sleep(int milis) {
		try {
			Thread.sleep((long) milis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void waitJS() {
		// Wait for core Javascript to load
		ExpectedCondition<Boolean> jsLoad = driver -> ((JavascriptExecutor) driver)
				.executeScript("return document.readyState").toString()
				.equals("complete");
		wait.until(jsLoad);

		// wait for JQuery
		ExpectedCondition<Boolean> jQueryLoad = driver -> ((Long) ((JavascriptExecutor) driver)
				.executeScript("return jQuery.active") == 0);
		wait.until(jQueryLoad);
	}

	protected Object executeScript(String script, Object... arguments) {
		if (driver instanceof JavascriptExecutor) {
			JavascriptExecutor javascriptExecutor = JavascriptExecutor.class
					.cast(driver);
			return javascriptExecutor.executeScript(script, arguments);
		} else {
			throw new RuntimeException("Script execution failed.");
		}
	}

	public static String resolveEnvVars(String input) {
		if (null == input) {
			return null;
		}
		Pattern pattern = Pattern.compile("\\$(?:\\{(\\w+)\\}|(\\w+))");
		Matcher matcher = pattern.matcher(input);
		StringBuffer stringBuffer = new StringBuffer();
		while (matcher.find()) {
			String envVarName = null == matcher.group(1) ? matcher.group(2)
					: matcher.group(1);
			String envVarValue = System.getenv(envVarName);
			matcher.appendReplacement(stringBuffer,
					null == envVarValue ? "" : envVarValue.replace("\\", "\\\\"));
		}
		matcher.appendTail(stringBuffer);
		return stringBuffer.toString();
	}

}