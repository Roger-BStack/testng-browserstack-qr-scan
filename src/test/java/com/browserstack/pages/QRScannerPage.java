package com.browserstack.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object for the QR Scanner Demo page at:
 * https://nimiq.github.io/qr-scanner/demo/
 *
 * Provides three methods for scanning QR codes:
 *   1. Via webcam with BrowserStack camera image injection
 *   2. Via file upload using a BrowserStack media URL
 *   3. Via file upload using a local file path
 */
public class QRScannerPage {

    private static final String PAGE_URL = "https://nimiq.github.io/qr-scanner/demo/";
    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(30);

    // Selectors
    private static final By START_BUTTON      = By.id("start-button");
    private static final By STOP_BUTTON      = By.id("stop-button");
    private static final By CAM_QR_RESULT     = By.id("cam-qr-result");
    private static final By FILE_SELECTOR     = By.id("file-selector");
    private static final By FILE_QR_RESULT    = By.id("file-qr-result");

    private final WebDriver driver;
    private final JavascriptExecutor js;

    public QRScannerPage(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
    }

    // -------------------------------------------------------------------------
    // Method 1: Webcam injection via BrowserStack cameraImageInjection
    // -------------------------------------------------------------------------

    /**
     * Injects a media URL into the device camera using BrowserStack's
     * cameraImageInjection executor, then starts the webcam scanner and
     * returns the detected QR code value.
     *
     * @param mediaUrl The BrowserStack media URL to inject (e.g. media://...).
     * @return The detected QR code string, or "None" if nothing was detected.
     */
    public String scanViaWebcamInjection(String mediaUrl) throws InterruptedException {
        // Step 1: Navigate to the page
        driver.get(PAGE_URL);

        // Step 2: Click the Start button to activate the webcam scanner
        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_WAIT);
        WebElement startButton = wait.until(
                ExpectedConditions.elementToBeClickable(START_BUTTON));
        startButton.click();

        // Step 3: Wait for the QR result to update from "None"
        wait.until(driver -> {
            String result = driver.findElement(CAM_QR_RESULT).getText();
            return result != null && !result.equals("None") && !result.isEmpty();
        });

        return driver.findElement(CAM_QR_RESULT).getText();
    }

    // -------------------------------------------------------------------------
    // Method 2: File upload using a BrowserStack media URL
    // -------------------------------------------------------------------------

    /**
     * Uploads a QR code image to the "Scan from File" section using a
     * BrowserStack media URL (remote file upload via BrowserStack executor).
     *
     * @param mediaName The BrowserStack media_name of the image uploaded via Session Capabilities.
     * @return The detected QR code string, or "None" if nothing was detected.
     */
    public String scanViaFileUploadMediaName(String mediaName) {
        driver.get(PAGE_URL);

        // Use BrowserStack's file upload executor to set the remote file

        /* Using uploadRemoteFile ONLY works for Windows sessions! */
//        String localPath = uploadRemoteFile(mediaName);

        String localPath = "";
        Capabilities cap = ((RemoteWebDriver) driver).getCapabilities();
        String osName = cap.getPlatformName().toString();

        if(osName.equalsIgnoreCase("windows")){
            localPath = "C:\\Users\\hello\\Documents\\images\\" + mediaName;
        } else {
            localPath = "/Users/test1/Documents/images/" + mediaName;
        }

        // Send the returned local path to the file input
        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_WAIT);
        WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(FILE_SELECTOR));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Scrolls the element into view (aligns to the top of the viewport by default)
        js.executeScript("arguments[0].scrollIntoView(true);", fileInput);
        fileInput.sendKeys(localPath);

        // Wait for the QR result to update from "None"
        wait.until(driver -> {
            String result = driver.findElement(FILE_QR_RESULT).getText();
            return result != null && !result.equals("None") && !result.isEmpty();
        });

        return driver.findElement(FILE_QR_RESULT).getText();
    }

    /**
     * Uses the BrowserStack uploadMedia executor to upload a file from a
     * remote URL and returns the local path on the remote machine.
     *
     * @param mediaName The BrowserStack media_name from previous upload.
     * @return The local file path on the remote BrowserStack machine.
     */
    private String uploadRemoteFile(String mediaName) {
        String script = String.format(
            "{\"action\": \"uploadFile\", \"fileName\": \"%s\"}",
                mediaName);
        Object result = js.executeScript("browserstack_executor: " + script);
        return result != null ? result.toString() : "";
    }

    // -------------------------------------------------------------------------
    // Method 3: File upload using a local file path
    // -------------------------------------------------------------------------

    /**
     * Uploads a QR code image to the "Scan from File" section using a local
     * file path (standard Selenium LocalFileDetector file upload via sendKeys).
     *
     * @param imagePath The absolute local path to the image file.
     * @return The detected QR code string, or "None" if nothing was detected.
     */
    public String scanViaLocalFileUpload(String imagePath) {
        driver.get(PAGE_URL);

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_WAIT);
        WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(FILE_SELECTOR));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Scrolls the element into view (aligns to the top of the viewport by default)
        js.executeScript("arguments[0].scrollIntoView(true);", fileInput);

        // Standard Selenium local file upload
        fileInput.sendKeys(imagePath);

        // Wait for the QR result to update from "None"
        wait.until(driver -> {
            String result = driver.findElement(FILE_QR_RESULT).getText();
            return result != null && !result.equals("None") && !result.isEmpty();
        });

        return driver.findElement(FILE_QR_RESULT).getText();
    }
}