# testng-browserstack-qr-scan

[TestNG](http://testng.org) Integration with BrowserStack.

![BrowserStack Logo](https://d98b8t1nnulk5.cloudfront.net/production/images/layout/logo-header.png?1469004780)

## Using Maven

### Run sample build

- Clone the repository
- Replace YOUR_USERNAME and YOUR_ACCESS_KEY with your BrowserStack access credentials in browserstack.yml.
- Install dependencies `mvn compile`
- To run the test suite having cross-platform with parallelization, run `mvn test -P sample-test`
- To run local tests, run `mvn test -P sample-local-test`

Understand how many parallel sessions you need by using our [Parallel Test Calculator](https://www.browserstack.com/automate/parallel-calculator?ref=github)

### Integrate your test suite

This repository uses the BrowserStack SDK to run tests on BrowserStack. Follow the steps below to install the SDK in your test suite and run tests on BrowserStack:

* Create sample browserstack.yml file with the browserstack related capabilities with your [BrowserStack Username and Access Key](https://www.browserstack.com/accounts/settings) and place it in your root folder.
* Add maven dependency of browserstack-java-sdk in your pom.xml file
```sh
<dependency>
    <groupId>com.browserstack</groupId>
    <artifactId>browserstack-java-sdk</artifactId>
    <version>LATEST</version>
    <scope>compile</scope>
</dependency>
```
* Modify your build plugin to run tests by adding argLine `-javaagent:${com.browserstack:browserstack-java-sdk:jar}` and `maven-dependency-plugin` for resolving dependencies in the profiles `sample-test` and `sample-local-test`.
```
            <plugin>
               <artifactId>maven-dependency-plugin</artifactId>
                 <executions>
                   <execution>
                     <id>getClasspathFilenames</id>
                       <goals>
                         <goal>properties</goal>
                       </goals>
                   </execution>
                 </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0-M5</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>config/sample-local-test.testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                    <argLine>
                        -javaagent:${com.browserstack:browserstack-java-sdk:jar}
                    </argLine>
                </configuration>
            </plugin>
```
* Install dependencies `mvn compile`

## Using Gradle

### Prerequisites
- If using Gradle, Java v9+ is required.

### Run sample build

- Clone the repository
- Install dependencies `gradle build`
- To run the test suite having cross-platform with parallelization, run `gradle sampleTest`
- To run local tests, run `gradle sampleLocalTest`

Understand how many parallel sessions you need by using our [Parallel Test Calculator](https://www.browserstack.com/automate/parallel-calculator?ref=github)

### Integrate your test suite

This repository uses the BrowserStack SDK to run tests on BrowserStack. Follow the steps below to install the SDK in your test suite and run tests on BrowserStack:

* Following are the changes required in `gradle.build` -
    * Add `compileOnly 'com.browserstack:browserstack-java-sdk:latest.release'` in dependencies
    * Fetch Artifact Information and add `jvmArgs` property in tasks *SampleTest* and *SampleLocalTest* :
  ```
  def browserstackSDKArtifact = configurations.compileClasspath.resolvedConfiguration.resolvedArtifacts.find { it.name == 'browserstack-java-sdk' }
  
  task sampleTest(type: Test) {
    useTestNG() {
      dependsOn cleanTest
      useDefaultListeners = true
      suites "config/sample-test.testng.xml"
      jvmArgs "-javaagent:${browserstackSDKArtifact.file}"
    }
  }
  ```

* Install dependencies `gradle build`


## Notes
* You can view your test results on the [BrowserStack Automate dashboard](https://www.browserstack.com/automate)

---

## QRScannerPage — Page Object

**File:** `src/test/java/com/browserstack/pages/QRScannerPage.java`

**Target page:** [https://nimiq.github.io/qr-scanner/demo/](https://nimiq.github.io/qr-scanner/demo/)

The `QRScannerPage` class is a Selenium Page Object that wraps the QR Scanner Demo page and exposes three methods for scanning QR codes via different image-injection strategies.

### Constructor

```java
QRScannerPage page = new QRScannerPage(driver);
```

Accepts a `WebDriver` instance. The driver must already be connected to a BrowserStack Automate session.

---

### Method 1 — Webcam injection (`scanViaWebcamInjection`)

```java
String qrCode = page.scanViaWebcamInjection(mediaUrl);
```

**Parameter:** `mediaUrl` — a BrowserStack media URL (e.g. `media://...`) previously uploaded via the BrowserStack media upload API. *(Note: this parameter is accepted but the actual camera injection is configured via the `cameraInjectionUrl` session capability set at driver creation time.)*

**Flow:**
1. Navigates to the QR Scanner Demo page.
2. Waits for the **Start** button (`#start-button`) to be clickable and clicks it to activate the webcam scanner.
3. Waits (up to 30 s) for the `#cam-qr-result` span to update from `"None"`.
4. Returns the detected QR code string.

**Use when:** running on BrowserStack Automate with `cameraInjection: true` and `cameraInjectionUrl` set to a BrowserStack MP4 media URL in the session capabilities.

---

### Method 2 — File upload via BrowserStack media name (`scanViaFileUploadMediaName`)

```java
String qrCode = page.scanViaFileUploadMediaName(mediaName);
```

**Parameter:** `mediaName` — the `media_name` of an image previously uploaded to BrowserStack media storage (retrieved via `Utilities.getBsMediaNameById`).

**Flow:**
1. Navigates to the QR Scanner Demo page.
2. Resolves the OS-specific path to the pre-uploaded file on the BrowserStack remote machine:
   - **Windows:** `C:\Users\hello\Documents\images\<mediaName>`
   - **macOS/Linux:** `/Users/test1/Documents/images/<mediaName>`
3. Scrolls the `#file-selector` file input into view and sends the resolved path via `sendKeys`.
4. Waits (up to 30 s) for the `#file-qr-result` span to update from `"None"`.
5. Returns the detected QR code string.

**Use when:** the image has been uploaded to BrowserStack via the `uploadMedia` session capability and you want to test the **Scan from File** flow using the pre-staged remote file.

> **Note:** The `uploadRemoteFile` helper (using the `uploadFile` BrowserStack executor) is available in the class but is currently disabled — it only works for Windows sessions.

---

### Method 3 — File upload via local path (`scanViaLocalFileUpload`)

```java
String qrCode = page.scanViaLocalFileUpload(imagePath);
```

**Parameter:** `imagePath` — the absolute path to a local image file (e.g. `/tmp/qr_code.png`).

**Flow:**
1. Navigates to the QR Scanner Demo page.
2. Scrolls the `#file-selector` file input into view.
3. Sends the absolute file path directly to the file input using `sendKeys` (BrowserStack Automate transfers the file automatically via `LocalFileDetector`).
4. Waits (up to 30 s) for the `#file-qr-result` span to update from `"None"`.
5. Returns the detected QR code string.

**Use when:** the image file is available on the machine running the test (e.g. generated at runtime with `Utilities.generateQRCode`).

**Reference:** [BrowserStack — Test File Upload (Java)](https://www.browserstack.com/docs/automate/selenium/test-file-upload?fw-lang=java)

---

### Page element reference

| Element | Selector | Description |
|---|---|---|
| Start button | `#start-button` | Starts the webcam QR scanner |
| Stop button | `#stop-button` | Stops the webcam QR scanner |
| Webcam QR result | `#cam-qr-result` | Displays the QR code detected via webcam |
| File input | `#file-selector` | `<input type="file">` for the Scan from File section |
| File QR result | `#file-qr-result` | Displays the QR code detected from the uploaded file |

---

### Example usage

```java
import com.browserstack.pages.QRScannerPage;

public class QRScannerTest {

    @Test
    public void testWebcamInjection() throws InterruptedException {
        QRScannerPage page = new QRScannerPage(driver);
        String detected = page.scanViaWebcamInjection(bsMediaUrl);
        Assert.assertNotNull(detected);
        Assert.assertFalse(detected.equals("None"));
    }

    @Test
    public void testFileUploadFromMediaName() {
        String mediaId = bsMediaUrl.replace("media://", "");
        String mediaName = Utilities.getBsMediaNameById(mediaId, username, accessKey);
        QRScannerPage page = new QRScannerPage(driver);
        String detected = page.scanViaFileUploadMediaName(mediaName);
        Assert.assertNotNull(detected);
    }

    @Test
    public void testLocalFileUpload() throws Exception {
        String qrPath = "/tmp/test_qr.png";
        Utilities.generateQRCode("https://example.com", qrPath, 300, 300);
        QRScannerPage page = new QRScannerPage(driver);
        String detected = page.scanViaLocalFileUpload(qrPath);
        Assert.assertEquals(detected, "https://example.com");
    }
}
```

---

## Utilities Class

The `Utilities` class (`src/test/java/com/browserstack/Utilities.java`) provides helper methods for QR code generation/scanning, image-to-video encoding, and BrowserStack media management.

### Dependencies

The following libraries are required (already declared in `pom.xml`):

| Library | Artifact | Version | Purpose |
|---------|----------|---------|---------|
| ZXing Core | `com.google.zxing:core` | 3.5.2 | QR code encoding & decoding |
| ZXing JavaSE | `com.google.zxing:javase` | 3.5.2 | BufferedImage integration for ZXing |
| JCodec | `org.jcodec:jcodec` | 0.2.5 | MP4 video encoding |
| JCodec JavaSE | `org.jcodec:jcodec-javase` | 0.2.5 | AWT/BufferedImage integration for JCodec |
| JSON | `org.json:json` | — | JSON parsing for BrowserStack API responses |

### Methods

#### `generateQRCode(String url, String outputPath, int width, int height)`

Generates a QR code PNG image for the given URL and saves it to disk.

```java
Utilities.generateQRCode("https://www.example.com", "/tmp/qrcode.png", 300, 300);
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `url` | `String` | The URL to encode in the QR code |
| `outputPath` | `String` | File path where the PNG will be saved |
| `width` | `int` | Width of the output image in pixels |
| `height` | `int` | Height of the output image in pixels |

**Throws:** `Exception` if encoding or file writing fails.

---

#### `encodeImageToMp4(String imageFilePath)`

Reads an image file, centers it on a 500×500 black background, and encodes it as a single-frame MP4 video using [JCodec](http://jcodec.org/). The output file is saved alongside the source image with an `_output.mp4` suffix.

```java
String videoPath = Utilities.encodeImageToMp4("/tmp/screenshot.png");
// videoPath → "/tmp/screenshot_output.mp4"
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `imageFilePath` | `String` | Path to the source image file (PNG, JPEG, etc.) |

**Returns:** `String` — absolute path to the generated MP4 file.  
**Throws:** `IOException` if the image cannot be read or the video cannot be written.

> **Note:** The output video is always 500×500 pixels. The source image is centered on a black background using `pasteImageCenter`. Fixed even dimensions satisfy the H.264 codec requirement.

---

#### `pasteImageCenter(BufferedImage background, BufferedImage foreground)`

Draws `foreground` centered on `background` using `Graphics2D`. Modifies `background` in place.

```java
Utilities.pasteImageCenter(backgroundImage, qrImage);
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `background` | `BufferedImage` | The destination image (modified in place) |
| `foreground` | `BufferedImage` | The image to draw centered on the background |

---

#### `getBsMediaNameById(String mediaId, String username, String accessKey)`

Queries the BrowserStack `recent_media_files` API to look up the `media_name` for a given `media_id`.

```java
String mediaId = bsMediaUrl.replace("media://", "");
String mediaName = Utilities.getBsMediaNameById(mediaId, username, accessKey);
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `mediaId` | `String` | The media ID (the part of the media URL after `media://`) |
| `username` | `String` | BrowserStack username |
| `accessKey` | `String` | BrowserStack access key |

**Returns:** `String` — the `media_name` if found, or `null` if not found or on error.

**API endpoint:** `GET https://api-cloud.browserstack.com/automate/recent_media_files` (Basic Auth)

---

#### `scanQRCodeFromImage(BufferedImage image)`

Scans a `BufferedImage` for a QR code and extracts any embedded URL.

```java
BufferedImage img = ImageIO.read(new File("/tmp/qrcode.png"));
String url = Utilities.scanQRCodeFromImage(img);
// url → "https://www.example.com"  (or "" if no http/https link, or null if no QR code)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `image` | `BufferedImage` | The image to scan |

**Returns:**
- `String` containing the `http://` or `https://` URL if a QR code with a web link is found.
- `""` (empty string) if a QR code is detected but contains no `http`/`https` link.
- `null` if no QR code is detected in the image.

---

#### `scanQRCodeFromBytes(byte[] imageBytes)`

Scans a raw byte array representing an image file for a QR code. Internally decodes the bytes to a `BufferedImage` and delegates to `scanQRCodeFromImage`.

```java
byte[] imageBytes = Files.readAllBytes(Paths.get("/tmp/qrcode.png"));
String url = Utilities.scanQRCodeFromBytes(imageBytes);
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `imageBytes` | `byte[]` | Raw bytes of an image file (PNG, JPEG, etc.) |

**Returns:** Same as `scanQRCodeFromImage` — the detected URL, `""`, or `null`.

---

## QRScannerTest — Test Class

**File:** `src/test/java/com/browserstack/QRScannerTest.java`

`QRScannerTest` is a standalone TestNG test class that exercises all three QR-scanning strategies provided by `QRScannerPage`, plus a direct utility-level scan. It runs entirely on **BrowserStack Automate** using a `RemoteWebDriver` session with camera permissions and media injection pre-configured.

### Prerequisites

| Requirement | Details |
|---|---|
| BrowserStack account | Username and Access Key from [BrowserStack Settings](https://www.browserstack.com/accounts/settings) |
| Environment variables | `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` must be set before running |
| Java | 8 or later |
| Maven | 3.x |

### Setup

#### `@BeforeClass` — `oneTimeSetUp()`

Runs once before all tests. Performs one-time resource preparation:

1. **Reads credentials** from `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` environment variables (throws `IllegalStateException` if either is missing).
2. **Generates a QR code** PNG image (300 × 300 px) encoding `https://www.bstackdemo.com` using `Utilities.generateQRCode`, saved to a system temp file.
3. **Encodes the QR image to MP4** using `Utilities.encodeImageToMp4` (single-frame 500×500 video on a black background).
4. **Uploads the QR image** to BrowserStack media storage via a multipart `POST` to `https://api-cloud.browserstack.com/automate/upload-media` with Basic Auth. The returned `media_url` (e.g. `media://...`) is stored as `bsMediaUrl`.
5. **Uploads the QR video** to BrowserStack media storage via the same endpoint. The returned `media_url` is stored as `bsVideoMediaUrl`.

#### `@BeforeMethod` — `setUp()`

Runs before each test method. Creates a fresh `RemoteWebDriver` session on BrowserStack with:
- Chrome prefs granting camera/mic access without a permission prompt.
- `--use-fake-ui-for-media-stream` and `--use-fake-device-for-media-stream` Chrome flags.
- `cameraInjection: true` — enables BrowserStack camera injection.
- `cameraInjectionUrl: <bsVideoMediaUrl>` — the MP4 video to inject into the camera feed.
- `uploadMedia: [<bsMediaUrl>]` — pre-stages the QR image on the BrowserStack remote machine.
- `LocalFileDetector` enabled for local file uploads.

#### `@AfterMethod` — `tearDown()`

Calls `driver.quit()` after each test method to close the BrowserStack session.

---

### Test Methods

#### `testScanViaWebcamInjection` (priority 1)

Tests QR scanning via BrowserStack camera injection (webcam simulation using the pre-injected MP4 video).

**Steps:**
1. Calls `QRScannerPage.scanViaWebcamInjection(bsMediaUrl)` — navigates to the demo page, clicks Start, and waits for a webcam result.
2. Asserts the result is not `null` and not `"None"`.
3. If the result is an `http`/`https` URL, navigates the driver to that URL.

---

#### `testScanViaFileUploadMediaName` (priority 2)

Tests QR scanning via the "Scan from File" section using the BrowserStack-staged file name.

**Steps:**
1. Resolves the `media_name` for `bsMediaUrl` by calling `Utilities.getBsMediaNameById`.
2. Calls `QRScannerPage.scanViaFileUploadMediaName(mediaName)` — constructs the OS-specific path to the pre-staged file on the BrowserStack machine and sends it to the file input.
3. Asserts the result is not `null` and not `"None"`.
4. If the result is an `http`/`https` URL, navigates the driver to that URL.

---

#### `testScanViaLocalFileUpload` (priority 3)

Tests QR scanning via the "Scan from File" section using the local temp file path.

**Steps:**
1. Calls `QRScannerPage.scanViaLocalFileUpload(qrImagePath)` — sends the absolute local path to the file input using standard Selenium `sendKeys` (BrowserStack Automate transfers the file automatically via `LocalFileDetector`).
2. Asserts the result is not `null` and not `"None"`.
3. If the result is an `http`/`https` URL, navigates the driver to that URL.

---

#### `testScanViaUtilitiesImagePath` (priority 4)

Tests QR decoding directly from the local image file using `Utilities`, without involving the browser's file-upload UI.

**Steps:**
1. Reads the QR code image bytes from the local temp file using `Files.readAllBytes`.
2. Calls `Utilities.scanQRCodeFromBytes(imageBytes)` to decode the QR code using ZXing.
3. Asserts the result is not `null` and not empty.
4. If the result is an `http`/`https` URL, navigates the driver to that URL.

---

### Running the Tests

Set your BrowserStack credentials as environment variables, then run via Maven:

```bash
export BROWSERSTACK_USERNAME=your_username
export BROWSERSTACK_ACCESS_KEY=your_access_key

mvn test -Dtest=QRScannerTest
```

Or add `QRScannerTest` to a TestNG XML suite file and run with the existing Maven profiles:

```bash
mvn test -P sample-test
```

### Browser Support

The test class includes factory methods for all three supported browsers. The default session uses **Chrome**. To switch browsers, call the appropriate factory method in `setUp`:

| Browser | Factory method | Notes |
|---|---|---|
| Chrome (default) | `createBrowserStackDriver(username, accessKey, qrImageMediaUrl, camVideoMediaUrl)` | Default session |
| Firefox | `createFirefoxDriver(username, accessKey, qrImageMediaUrl, camVideoMediaUrl)` | Reference / future use |
| Safari | `createSafariDriver(username, accessKey, qrImageMediaUrl, camVideoMediaUrl)` | Reference / future use |

All browser factory methods accept `qrImageMediaUrl` and `camVideoMediaUrl` and set the same `cameraInjection`, `cameraInjectionUrl`, and `uploadMedia` capabilities. OS/browser/version capabilities are commented out — configure them in `browserstack.yml` when using the BrowserStack SDK.

### BrowserStack Capabilities Reference

| Capability | Value | Purpose |
|---|---|---|
| `cameraInjection` | `true` | Enables BrowserStack camera injection |
| `cameraInjectionUrl` | `<bsVideoMediaUrl>` | MP4 video URL to inject into the camera feed |
| `uploadMedia` | `[<bsMediaUrl>]` | Pre-stages media files on the BrowserStack remote machine |
| `--use-fake-ui-for-media-stream` | Chrome arg | Suppresses the camera permission dialog |
| `--use-fake-device-for-media-stream` | Chrome arg | Provides a fake media device for camera access |
| `media.navigator.permission.disabled` | Firefox pref | Grants camera access without a prompt |
| `media.navigator.streams.fake` | Firefox pref | Enables fake media streams in Firefox |

**References:**
- [BrowserStack — Camera Image Injection](https://www.browserstack.com/docs/app-automate/appium/advanced-features/camera-image-injection)
- [BrowserStack — Test File Upload (Java)](https://www.browserstack.com/docs/automate/selenium/test-file-upload?fw-lang=java)
- [BrowserStack — List of Browsers & Platforms](https://www.browserstack.com/list-of-browsers-and-platforms/automate)