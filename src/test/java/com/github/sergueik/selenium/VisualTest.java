package com.github.sergueik.selenium;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
// import com.google.common.io.Files;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.im4java.core.CommandException;
import org.im4java.core.CompareCmd;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.process.ProcessStarter;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import java.awt.image.RasterFormatException;
import org.yaml.snakeyaml.Yaml;

public class VisualTest extends BaseTest {

	static String diffScript = null;

	final static String propertiesFilename = String.format(
			"%s/src/test/resources/%s", System.getProperty("user.dir"),
			"application.properties");

	private static Properties properties = new Properties();
	private static Map<String, String> propertiesMap = new HashMap<>();

	public String baseURL = "http://www.kariyer.net";

	public String testName;
	public String testScreenShotDirectory;
	public String parentScreenShotsLocation = System.getProperty("user.dir")
			+ File.separator + "screenshots" + File.separator;

	// Main differences directory
	// TODO: remove
	public String parentDifferencesLocation = System.getProperty("user.dir")
			+ File.separator + "differences" + File.separator;

	// Element screenshot paths
	public String baselinePath;
	public String screenshotPath;
	public String differencePath;

	public File baselineImageFile;
	public File actualImageFile;
	public File differenceImageFile;
	public File differenceFileForParent;

	private String toolPathSetting;
	// NOTE: optional. Set to null to disable
	private String envKey = "IM4JAVA_TOOLPATH";

	@SuppressWarnings("unchecked")
	@BeforeClass

	// there could be no "Current" subkey
	public void setupTestClass(ITestContext context) throws IOException {

		String fileName = buildPathtoResourceFile("application.yaml");
		InputStream in;
		try {
			// load with snakeyaml
			// reasonably deeply nested configuration makes it inpractical to have
			// multiple
			// inner classes

			Map<String, Object> configuration = (Map<String, Object>) new Yaml()
					.load(Files.newInputStream(Paths.get(fileName)));
			Map<String, Object> browserDrivers = (Map<String, Object>) configuration
					.get("driver");
			Map<String, Object> browserDriver = (Map<String, Object>) browserDrivers
					.get(browser);
			String driverPath = resolveEnvVars(
					(String) browserDriver.get(getOSName()));

			Map<String, Object> browsers = (Map<String, Object>) configuration
					.get("browser");
			Map<String, Object> browserPaths = (Map<String, Object>) browsers
					.get(browser);
			String browserPath = resolveEnvVars(
					(String) browserPaths.get(getOSName()));

			System.err.println(
					"Reading new application configuration:" + " \n" + "browser: "
							+ browser + " on " + getOSName() + "\n" + "browser driver path: "
							+ driverPath + "\n" + "browser path: " + browserPath);
		} catch (NullPointerException e) {
			System.err.println("Configuration missing or invalid: " + e.toString());
		} catch (IOException e) {
			System.err.println("Exception (ignored): " + e.toString());
		}
		try {
			System.err.println("Reading properties file: " + propertiesFilename);
			properties.load(new FileInputStream(propertiesFilename));
			@SuppressWarnings("unchecked")
			Enumeration<String> propertiesEnum = (Enumeration<String>) properties
					.propertyNames();
			for (; propertiesEnum.hasMoreElements();) {
				String key = propertiesEnum.nextElement();
				String val = properties.get(key).toString();
				System.err.println(String.format("Reading: '%s' = '%s'", key, val));
				propertiesMap.put(key, resolveEnvVars(val));
			}

		} catch (FileNotFoundException e) {
			System.err.println(String.format("Properties file was not found: '%s'",
					propertiesFilename));
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(String.format("Properties file is not readable: '%s'",
					propertiesFilename));
			e.printStackTrace();
		}
		if (getOSName().equals("windows")) {
			String imagemagickRegistryPath = (propertiesMap
					.containsKey("imagemagickRegistryPath"))
							? propertiesMap.get("imagemagickRegistryPath")
							: "SOFTWARE\\ImageMagick\\Current";
			try {
				System.err
						.println("Reading ImageMagick configuration from registry path "
								+ imagemagickRegistryPath);
				toolPathSetting = Advapi32Util.registryGetStringValue(
						WinReg.HKEY_LOCAL_MACHINE, imagemagickRegistryPath, "BinPath");
				System.err.println("ImageMagick path: " + toolPathSetting);
			} catch (Win32Exception e) {
				throw new RuntimeException("Error reading registry path: "
						+ imagemagickRegistryPath + "\n" + e.toString());
			}
			diffScript = (propertiesMap.containsKey("diffScript"))
					? propertiesMap.get("diffScript") : "diff_script";
			// Make sure the 'convert.exe' and 'compare.exe' exist in the
			// Image Magick home directory and can be executed
			for (String app : Arrays.asList(new String[] { "convert", "compare" })) {
				assertTrue(new File(toolPathSetting + "\\" + app + ".exe").exists(),
						String.format("\"%s.exe\" has to be present", app));
			}
			if (envKey != null) {
				setenv(envKey.toLowerCase(), toolPathSetting);
				assertEquals(System.getenv(envKey), toolPathSetting,
						String.format("The env %s has to be present and equal to: \"s\"",
								envKey, toolPathSetting));
			}
		}
		// setup WebDriver
		super.setupTestClass(context);
		// Go to URL
		driver.get(baseURL);

		// Create screenshot and differences folders if they are not exist
		createFolder(parentScreenShotsLocation);
		createFolder(parentDifferencesLocation);
		// Clean Differences Root Folder
		File differencesFolder = new File(parentDifferencesLocation);
		FileUtils.cleanDirectory(differencesFolder);

		// Add Cookie for top banner
		addCookieforTopBanner();

	}

	@BeforeMethod
	public void setupTestMethod(Method method) {
		// Retrieve the test method name and create a specific screenshot folder
		// for
		// each test method.
		testName = method.getName();
		System.err.println("Test Name: " + testName + "\n");

		testScreenShotDirectory = parentScreenShotsLocation + testName
				+ File.separator;
		createFolder(testScreenShotDirectory);

		// Declare element screenshot paths
		// Concatenate with the test name.
		declareScreenShotPaths(testName + "_Baseline.png", testName + "_Actual.png",
				testName + "_Diff.png");
	}

	@Test(enabled = true)
	public void imageCompareTest() throws Exception {
		// Handle popup
		handlePopup(".ui-dialog-titlebar-close");

		// Close banner
		closeBanner();

		WebElement element = driver.findElement(By.cssSelector(".item.uzman>a"));

		// Unhide Text which is changing A lot
		unhideElement(
				"document.getElementsByClassName('count')[0].style.display='none';");

		// Move To Operation
		moveToElement(element);

		// Wait for 2 second for violet color animation
		Thread.sleep(2000);

		if (baselineImageFile.exists()) {
			takeScreenshotOfWebelement(driver, element,
					// TODO: breakdown into local variables
					actualImageFile.getAbsolutePath());
			// Compare screenshot with baseline
			System.err.println("Comparison method will be called!\n");
			System.err.println("Baseline: " + baselinePath + "\n" + "Actual: "
					+ screenshotPath + "\n" + "Diff: " + differencePath);
			compareImagesWithImageMagick(baselinePath, screenshotPath,
					differencePath);

			// Try to use IM4Java for comparison
		} else {
			System.err.println(
					"BaselineScreenshot is not exist! We put it into test screenshot folder.\n");
			// Put the screenshot to the specified folder
			takeScreenshotOfWebelement(driver, element,
					// TODO: breakdown into local variables
					baselineImageFile.getAbsolutePath());
		}

	}

	@Test(enabled = true)
	public void imageResizeTest() throws Exception {
		// Handle popup
		handlePopup(".ui-dialog-titlebar-close");

		// Close banner
		closeBanner();

		WebElement element = driver.findElement(By.cssSelector(".item.uzman>a"));

		// Unhide Text which is changing A lot
		unhideElement(
				"document.getElementsByClassName('count')[0].style.display='none';");

		// Move To Operation
		moveToElement(element);

		// Wait for 2 second for violet color animation
		Thread.sleep(2000);

		takeScreenshotOfWebelement(driver, element,
				testScreenShotDirectory + File.separator + "test.png");

		// Resize
		resizeImagesWithImageMagick(
				testScreenShotDirectory + File.separator + "test.png");
	}

	// utils

	// Add Cookie to suppress top banner animation
	public void addCookieforTopBanner() {
		// Set cookie expiration to Next Month Last Date
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DATE,
				calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		Date nextMonthLastDay = calendar.getTime();
		// provide name, domain, path, expiration date of the cookie
		Cookie topBannerCloseCookie = new Cookie.Builder("AA-kobiBannerClosed", "4") // Name
				.domain(baseURL.replaceAll("(?:https?://)([^/]+)(?:/.*)?$", "$1"))
				.path("/").expiresOn(nextMonthLastDay).build();

		driver.manage().addCookie(topBannerCloseCookie);
	}

	public void createFolder(String path) {
		File testDirectory = new File(path);
		if (!testDirectory.exists()) {
			if (testDirectory.mkdir()) {
				System.err.println("Directory: " + path + " is created!");
			} else {
				System.err.println("Failed to create directory: " + path);
			}
		} else {
			System.err.println("Directory already exists: " + path);
		}
	}

	// Close popup if exists
	public void handlePopup(String selector) throws InterruptedException {
		waitJS();
		List<WebElement> popup = driver.findElements(By.cssSelector(selector));
		if (!popup.isEmpty()) {
			popup.get(0).click();
			sleep(200);
		}
	}

	// Close Banner
	public void closeBanner() {
		waitJS();
		List<WebElement> banner = driver
				.findElements(By.cssSelector("body > div.kobi-head-banner > div > a"));
		if (!banner.isEmpty()) {
			banner.get(0).click();
			// Wait for 2 second for closing banner
			sleep(2000);
		}
	}

	// Unhide an Element with JSExecutor
	public void unhideElement(String unhideJS) {
		executeScript(unhideJS);
		waitJS();
	}

	// Move to Operation
	public void moveToElement(WebElement element) {
		waitJS();
		actions.moveToElement(element).build().perform();
	}

	// origin:
	// https://github.com/TsvetomirSlavov/JavaScriptForSeleniumMyCollection/blob/master/src/utils/UtilsQAAutoman.java
	// https://github.com/roydekleijn/WebDriver-take-screenshot-of-element/blob/master/src/main/java/TakeScreenshotOfElement/TakeElementScreenshot.java
	public void takeScreenshotOfWebelement(WebDriver driver, WebElement element,
			String Destination) throws Exception {
		highlight(element);
		try {
			driver = new Augmenter().augment(driver);
		} catch (Exception ignored) {
		}
		File screen = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		BufferedImage bi = ImageIO.read(screen);
		org.openqa.selenium.Point point = element.getLocation();

		int width = element.getSize().getWidth();
		int height = element.getSize().getHeight();

		Rectangle rect = new Rectangle(width, height);

		BufferedImage img = null;
		img = ImageIO.read(screen);
		System.err.println(String.format(
				"Getting sub image of screen (width = %d, height = %d) "
						+ " for rectangle (x = %d, y = %d, width = %d, height = %d)",
				img.getWidth(), img.getHeight(), point.getX(), point.getY(), rect.width,
				rect.height));
		BufferedImage dest = null;
		// NOTE: with Chrome 67 / Chromedriver 40 / Ubuntu 16.04
		// the element position and size is computed incorrectly
		// Getting sub image of screen(width = 1366 height = 632)
		// for rectangle (x = 81, y = 526, width = 395, height = 164)
		// leading to the exception:
		// (y + height) is outside of Raster

		int subimage_width = (point.getX() + rect.width <= img.getWidth())
				? rect.width : img.getWidth() - point.getX();
		int subimage_height = (point.getY() + rect.height <= img.getHeight())
				? rect.height : img.getHeight() - point.getY();
		// subimage_width = rect.width;
		// subimage_height = rect.height;
		try {
			dest = img.getSubimage(point.getX(), point.getY(), subimage_width,
					subimage_height);
		} catch (RasterFormatException e) {
			System.err.println("Exception (ignored) " + e.getMessage());
		}
		if (dest != null) {
			ImageIO.write(dest, "png", screen);
			FileUtils.copyFile(screen, new File(Destination));
		}
	}

	// Screenshot paths
	public void declareScreenShotPaths(String baseline, String actual,
			String diff) {
		// BaseLine, Actual, Difference Photo Paths
		baselinePath = testScreenShotDirectory + baseline;
		screenshotPath = testScreenShotDirectory + actual;
		differencePath = testScreenShotDirectory + diff;

		// BaseLine, Actual Photo Files
		baselineImageFile = new File(baselinePath);
		actualImageFile = new File(screenshotPath);
		differenceImageFile = new File(differencePath);

		// For copying difference to the parent Difference Folder
		differenceFileForParent = new File(parentDifferencesLocation + diff);
	}

	public void removeImageBackground(String imagePath) throws RuntimeException {
		File file = new File(imagePath);
		if (!file.exists()) {
			throw new RuntimeException("File does not exist:" + imagePath);
		}
		ConvertCmd convertcmd = new ConvertCmd();
		IMOperation imOperation = new IMOperation();
		imOperation.addRawArgs("-fill", "none");
		imOperation.addRawArgs("-fuzz", "1%");
		imOperation.addRawArgs("-draw", "matte 0,0 floodfill");
		imOperation.addRawArgs("--flop", "");
		imOperation.addRawArgs("-draw", "matte 0,0 floodfill");
		imOperation.addRawArgs("--flop", "");
		imOperation.addImage();
		Object[] listOfFiles = { imagePath };
		try {
			convertcmd.run(imOperation, listOfFiles);
		} catch (Exception e) {
			throw new RuntimeException("Unable to rotate Image", e);
		}
	}

	public void resizeImagesWithImageMagick(String... pImageNames)
			throws Exception {
		ConvertCmd cmd = new ConvertCmd();
		cmd.setSearchPath(toolPathSetting);
		IMOperation imOperation = new IMOperation();
		imOperation.addImage();
		imOperation.resize(200, 150);
		imOperation.addImage();
		for (String srcImage : pImageNames) {
			String dstImage = srcImage.substring(0, srcImage.lastIndexOf('.') - 1)
					+ "_small.jpg";
			try {
				// TODO: optionally detect if the source image is missing
				System.err.println(String.format("Resized image: \"%s\"", dstImage));
				cmd.run(imOperation, srcImage, dstImage);
			} catch (IOException | InterruptedException ex) {
				ex.printStackTrace();
				throw ex;
			} catch (IM4JavaException ex) {
				System.err.println("Exception (ignored): " + ex.getClass());
				ex.printStackTrace();
			}
		}
	}

	// ImageMagick Compare Method
	public void compareImagesWithImageMagick(String expected, String actual,
			String difference) throws Exception {

		ProcessStarter.setGlobalSearchPath(toolPathSetting);

		CompareCmd compare = new CompareCmd();
		compare.setSearchPath(toolPathSetting);

		// fix java.lang.NullPointerExceptionTests
		// compare.setErrorConsumer(StandardStream.STDERR);

		IMOperation imOperation = new IMOperation();
		imOperation.fuzz(5.0);

		// The special "-metric" setting of 'AE' (short for "Absolute Error"
		// count),
		// will report (to standard error),
		// a count of the actual number of pixels that were masked, at the
		// current
		// fuzz factor.
		imOperation.metric("AE");

		// Add the expected image
		imOperation.addImage(expected);

		// Add the actual image
		imOperation.addImage(actual);

		// This stores the difference
		imOperation.addImage(difference);

		try {
			System.err.println("Comparison Started");
			compare.createScript(diffScript, imOperation);
			System.err.println("Comparison Script written to " + diffScript);
			compare.run(imOperation);
		} catch (CommandException ex) {
			// ignore
			System.err.println("Exception (ignored):" + ex.getClass());
			System.err.print(ex);
		} catch (Exception ex) {
			System.err.println("Comparison Failed!");
			System.err.print(ex);
			throw ex;
		}
		// Put the difference image to the global differences folder
		com.google.common.io.Files.copy(differenceImageFile,
				differenceFileForParent);
	}

}
