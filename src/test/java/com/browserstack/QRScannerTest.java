package com.browserstack;

import com.browserstack.pages.QRScannerPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TestNG test class for QR code scanning via BrowserStack.
 *
 * Setup:
 *   1. Generates a QR code image for https://www.bstackdemo.com
 *   2. Saves the image to a temp file
 *   3. Uploads the image to BrowserStack media storage
 *   4. Stores the local image path and BrowserStack media URL for use in tests
 *
 * Test methods:
 *   - testScanViaWebcamInjection        : webcam injection using BrowserStack media URL
 *   - testScanViaFileUploadMediaUrl     : file upload using BrowserStack media URL
 *   - testScanViaWebcamInjectionRepeat  : second webcam injection test (duplicate scenario)
 *   - testScanViaLocalFileUpload        : file upload using local image path
 *   - testScanViaUtilitiesImagePath     : direct QR decode from local image using Utilities
 */
public class QRScannerTest {

    private static final String TARGET_URL      = "https://www.bstackdemo.com";
    private static final String BS_HUB_URL      = "https://%s:%s@hub-cloud.browserstack.com/wd/hub";
    private static final String UPLOAD_ENDPOINT = "https://api-cloud.browserstack.com/automate/upload-media";
    private static final int    QR_SIZE         = 300;

    // Resolved at @BeforeClass
    private String bsUsername;
    private String bsAccessKey;
    private String qrImagePath;
    private String bsMediaUrl;
    private String qrVideoPath;
    private String bsVideoMediaUrl;

    private WebDriver driver;
    private QRScannerPage qrScannerPage;

    // ------------------------------------------------------------------
    // @BeforeClass: generate QR code, upload to BrowserStack, init driver
    // ------------------------------------------------------------------

    @BeforeClass(alwaysRun = true)
    public void oneTimeSetUp() throws Exception {
        // 1. Resolve BrowserStack credentials from environment
        bsUsername  = System.getenv("BROWSERSTACK_USERNAME");
        bsAccessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (bsUsername == null || bsUsername.isEmpty()) {
            throw new IllegalStateException(
                    "BROWSERSTACK_USERNAME environment variable is not set.");
        }
        if (bsAccessKey == null || bsAccessKey.isEmpty()) {
            throw new IllegalStateException(
                    "BROWSERSTACK_ACCESS_KEY environment variable is not set.");
        }

        // 2. Generate a SAMPLE QR code image and save to a temp file
        File qrFile = File.createTempFile("qr_bstackdemo_", ".png");
        qrImagePath = qrFile.getAbsolutePath();
        Utilities.generateQRCode(TARGET_URL, qrImagePath, QR_SIZE, QR_SIZE);
        qrVideoPath = Utilities.encodeImageToMp4(qrImagePath);

        // 3.a. Upload the QR code image to BrowserStack media storage
        bsMediaUrl = uploadMediaToBrowserStack(qrImagePath, bsUsername, bsAccessKey);
        System.out.println("[QRScannerTest] QR image path : " + qrImagePath);
        System.out.println("[QRScannerTest] BrowserStack media URL: " + bsMediaUrl);

        // 3.b. Upload the QR code video to BrowserStack media storage
        bsVideoMediaUrl = uploadMediaToBrowserStack(qrVideoPath, bsUsername, bsAccessKey);
        System.out.println("[QRScannerTest] QR video path : " + qrVideoPath);
        System.out.println("[QRScannerTest] BrowserStack video media URL: " + bsVideoMediaUrl);
    }
    
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        // Create a RemoteWebDriver session on BrowserStack (Chrome with camera permissions)
        driver = createBrowserStackDriver(bsUsername, bsAccessKey, bsMediaUrl, bsVideoMediaUrl);
        qrScannerPage = new QRScannerPage(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ------------------------------------------------------------------
    // Test 1: Webcam injection via BrowserStack media URL
    // ------------------------------------------------------------------

    @Test(priority = 1)
    public void testScanViaWebcamInjection() throws InterruptedException {
        String result = qrScannerPage.scanViaWebcamInjection(bsMediaUrl);
        System.out.println("[testScanViaWebcamInjection] Detected: " + result);
        Assert.assertNotNull(result, "QR scan result should not be null");
        Assert.assertFalse(result.equals("None"), "QR scan should detect a value");
        if (result.startsWith("http://") || result.startsWith("https://")) {
            driver.get(result);
            System.out.println("[testScanViaWebcamInjection] Navigated to: " + result);
        }
    }

    // ------------------------------------------------------------------
    // Test 2: File upload via BrowserStack media URL
    // ------------------------------------------------------------------

    @Test(priority = 2)
    public void testScanViaFileUploadMediaName() {
        String bsMediaId =  bsMediaUrl.replace("media://", "");
        String bsMediaName = Utilities.getBsMediaNameById(bsMediaId, bsUsername, bsAccessKey);
        String result = qrScannerPage.scanViaFileUploadMediaName(bsMediaName);
        System.out.println("[testScanViaFileUploadMediaName] Detected: " + result);
        Assert.assertNotNull(result, "QR scan result should not be null");
        Assert.assertFalse(result.equals("None"), "QR scan should detect a value");
        if (result.startsWith("http://") || result.startsWith("https://")) {
            driver.get(result);
            System.out.println("[testScanViaFileUploadMediaName] Navigated to: " + result);
        }
    }

    // ------------------------------------------------------------------
    // Test 3: File upload via local file path
    // ------------------------------------------------------------------

    @Test(priority = 3)
    public void testScanViaLocalFileUpload() {
        String result = qrScannerPage.scanViaLocalFileUpload(qrImagePath);
        System.out.println("[testScanViaLocalFileUpload] Detected: " + result);
        Assert.assertNotNull(result, "QR scan result should not be null");
        Assert.assertFalse(result.equals("None"), "QR scan should detect a value");
        if (result.startsWith("http://") || result.startsWith("https://")) {
            driver.get(result);
            System.out.println("[testScanViaLocalFileUpload] Navigated to: " + result);
        }
    }

    // ------------------------------------------------------------------
    // Test 4: Direct QR decode from local image using Utilities
    // ------------------------------------------------------------------

    @Test(priority = 4)
    public void testScanViaUtilitiesImagePath() throws Exception {
        byte[] imageBytes = Files.readAllBytes(new File(qrImagePath).toPath());
        String result = Utilities.scanQRCodeFromBytes(imageBytes);
        System.out.println("[testScanViaUtilitiesImagePath] Detected: " + result);
        Assert.assertNotNull(result, "Utilities QR scan should detect a QR code");
        Assert.assertFalse(result.isEmpty(), "Utilities QR scan should return a non-empty URL");
        if (result.startsWith("http://") || result.startsWith("https://")) {
            driver.get(result);
            System.out.println("[testScanViaUtilitiesImagePath] Navigated to: " + result);
        }
    }

    // ------------------------------------------------------------------
    // Helper: upload image file to BrowserStack media storage
    // ------------------------------------------------------------------

    /**
     * Uploads a local image file to BrowserStack's media storage endpoint
     * using multipart/form-data and Basic Auth.
     *
     * @param filePath   Absolute path to the image file.
     * @param username   BrowserStack username.
     * @param accessKey  BrowserStack access key.
     * @return           The media URL returned by BrowserStack (e.g. "media://...").
     * @throws Exception if the upload fails or the response cannot be parsed.
     */
    private static String uploadMediaToBrowserStack(String filePath,
                                                    String username,
                                                    String accessKey) throws Exception {
        File file = new File(filePath);
        String boundary = "----BrowserStackBoundary" + System.currentTimeMillis();
        String auth = Base64.getEncoder().encodeToString(
            (username + ":" + accessKey).getBytes("UTF-8"));

        URL url = new URL(UPLOAD_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream()) {
            // Part header
            String partHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\""
                + file.getName() + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n";
            out.write(partHeader.getBytes("UTF-8"));

            // File bytes
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            out.write(fileBytes);

            // Closing boundary
            String closing = "\r\n--" + boundary + "--\r\n";
            out.write(closing.getBytes("UTF-8"));
            out.flush();
        }

        int responseCode = conn.getResponseCode();
        InputStream responseStream = (responseCode == 200)
            ? conn.getInputStream()
            : conn.getErrorStream();

        // Java 8-compatible response reading via BufferedReader
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(responseStream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String responseBody = sb.toString();

        if (responseCode != 200) {
            throw new RuntimeException(
                "BrowserStack media upload failed [HTTP " + responseCode + "]: " + responseBody);
        }

        // Parse "media_url" from JSON response using regex (no external JSON library needed)
        // Expected response: {"media_url": "media://..."}
        Pattern pattern = Pattern.compile("\"media_url\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(responseBody);
        if (!matcher.find()) {
            throw new RuntimeException(
                "BrowserStack media upload response missing 'media_url': " + responseBody);
        }
        return matcher.group(1);
    }

    // ------------------------------------------------------------------
    // Helper: create BrowserStack RemoteWebDriver with camera permissions
    // ------------------------------------------------------------------

    /**
     * Creates a BrowserStack RemoteWebDriver session using Chrome with all
     * capabilities required to allow camera access (for webcam QR scanning).
     *
     * Supports Chrome, Firefox, and Safari via the browserName capability.
     * Defaults to Chrome for this session.
     *
     * @param username   BrowserStack username.
     * @param accessKey  BrowserStack access key.
     * @return           Configured RemoteWebDriver instance.
     * @throws Exception if the driver cannot be created.
     */
    private static WebDriver createBrowserStackDriver(String username,
                                                      String accessKey,
                                                      String qrImageMediaUrl,
                                                      String camVideoMediaUrl) throws Exception {

        String hubUrl = String.format(BS_HUB_URL, username, accessKey);

        // --- Chrome (default) ---
        ChromeOptions chromeOptions = new ChromeOptions();

        // Grant camera permission without a prompt
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.media_stream_camera", 1);
        prefs.put("profile.default_content_setting_values.media_stream_mic", 1);
        chromeOptions.setExperimentalOption("prefs", prefs);

        // Allow camera on insecure origins (GitHub Pages uses HTTPS, but kept for safety)
        chromeOptions.addArguments("--use-fake-ui-for-media-stream");
        chromeOptions.addArguments("--use-fake-device-for-media-stream");

        // BrowserStack-specific capabilities
        Map<String, Object> bsOptions = new HashMap<>();

        /* When using the BrowserStack SDK, set these capabilities in browserstack.yml */
//        bsOptions.put("os", "OS X");
//        bsOptions.put("osVersion", "Big Sur");
//        bsOptions.put("browserName", "Chrome");
//        bsOptions.put("browserVersion", "latest");
//        bsOptions.put("projectName", "QR Scanner Tests");
//        bsOptions.put("buildName", "QR Scanner Build");
//        bsOptions.put("sessionName", "QRScannerTest");

        // Enable camera image injection for webcam tests
        bsOptions.put("cameraInjection", true);
        bsOptions.put("cameraInjectionUrl", camVideoMediaUrl);
        bsOptions.put("uploadMedia", new String[]{qrImageMediaUrl});

        chromeOptions.setCapability("bstack:options", bsOptions);

        RemoteWebDriver driver = new RemoteWebDriver(new URL(hubUrl), chromeOptions);

        // Needed for Local File detection
        driver.setFileDetector(new LocalFileDetector());

        return driver;
    }

    // ------------------------------------------------------------------
    // Factory methods for Firefox and Safari (for reference / future use)
    // ------------------------------------------------------------------

    /**
     * Creates a Firefox RemoteWebDriver with camera permissions on BrowserStack.
     */
    @SuppressWarnings("unused")
    private static WebDriver createFirefoxDriver(String username,
                                                 String accessKey,
                                                 String qrImageMediaUrl,
                                                 String camVideoMediaUrl) throws Exception {
        String hubUrl = String.format(BS_HUB_URL, username, accessKey);

        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addPreference("media.navigator.permission.disabled", true);
        firefoxOptions.addPreference("media.navigator.streams.fake", true);

        Map<String, Object> bsOptions = new HashMap<>();

        /* When using the BrowserStack SDK, set these capabilities in browserstack.yml */
//        bsOptions.put("os", "Windows");
//        bsOptions.put("osVersion", "10");
//        bsOptions.put("browserName", "Firefox");
//        bsOptions.put("browserVersion", "latest");
//        bsOptions.put("projectName", "QR Scanner Tests");
//        bsOptions.put("buildName", "QR Scanner Build");
//        bsOptions.put("sessionName", "QRScannerTest-Firefox");

        // Enable camera image injection for webcam tests
        bsOptions.put("cameraInjection", true);
        bsOptions.put("cameraInjectionUrl", camVideoMediaUrl);
        bsOptions.put("uploadMedia", new String[]{qrImageMediaUrl});

        firefoxOptions.setCapability("bstack:options", bsOptions);
        return new RemoteWebDriver(new URL(hubUrl), firefoxOptions);
    }

    /**
     * Creates a Safari RemoteWebDriver with camera permissions on BrowserStack.
     */
    @SuppressWarnings("unused")
    private static WebDriver createSafariDriver(String username,
                                                String accessKey,
                                                String qrImageMediaUrl,
                                                String camVideoMediaUrl) throws Exception {

        String hubUrl = String.format(BS_HUB_URL, username, accessKey);

        SafariOptions safariOptions = new SafariOptions();

        Map<String, Object> bsOptions = new HashMap<>();

        /* When using the BrowserStack SDK, set these capabilities in browserstack.yml */
//        bsOptions.put("os", "OS X");
//        bsOptions.put("osVersion", "Monterey");
//        bsOptions.put("browserName", "Safari");
//        bsOptions.put("browserVersion", "latest");
//        bsOptions.put("projectName", "QR Scanner Tests");
//        bsOptions.put("buildName", "QR Scanner Build");
//        bsOptions.put("sessionName", "QRScannerTest-Safari");

        // Enable camera image injection for webcam tests
        bsOptions.put("cameraInjection", true);
        bsOptions.put("cameraInjectionUrl", camVideoMediaUrl);
        bsOptions.put("uploadMedia", new String[]{qrImageMediaUrl});

        safariOptions.setCapability("bstack:options", bsOptions);
        return new RemoteWebDriver(new URL(hubUrl), safariOptions);
    }
}