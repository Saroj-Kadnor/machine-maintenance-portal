package com.saroj.machine_maintenance_portal.selenium;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared setup + helper methods for all Selenium journeys.
 *
 * Run settings (all optional, passed with -D on the Maven command line):
 *   -DbaseUrl=http://localhost:8081   application under test
 *   -Dbrowser=edge|chrome|firefox     default: edge
 *   -Dheadless=true                   no visible window (needed on Jenkins)
 */
@Tag("selenium")
@ExtendWith(ScreenshotOnFailureExtension.class)
public abstract class BaseSeleniumTest {

    protected static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8081");

    protected WebDriver driver;
    protected WebDriverWait wait;

    // ------------------------------------------------------------------
    // Browser lifecycle (a fresh browser for every test = isolated sessions)
    // ------------------------------------------------------------------
    @BeforeEach
    void startBrowser() {
        String browser = System.getProperty("browser", "edge").toLowerCase();
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        switch (browser) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--window-size=1400,1000", "--remote-allow-origins=*");
                if (headless) {
                    options.addArguments("--headless=new", "--no-sandbox", "--disable-gpu",
                            "--disable-dev-shm-usage", "--user-data-dir=" + tempProfileDir());
                }
                driver = new ChromeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.addArguments("--width=1400", "--height=1000");
                if (headless) {
                    options.addArguments("-headless");
                }
                driver = new FirefoxDriver(options);
            }
            default -> {
                EdgeOptions options = new EdgeOptions();
                options.addArguments("--window-size=1400,1000", "--remote-allow-origins=*");
                if (headless) {
                    // extra flags make headless Edge work when Jenkins runs as a Windows service
                    options.addArguments("--headless=new", "--no-sandbox", "--disable-gpu",
                            "--disable-dev-shm-usage", "--user-data-dir=" + tempProfileDir());
                }
                driver = new EdgeDriver(options);
            }
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /** A throw-away browser profile folder, so Jenkins' service account never needs a real user profile. */
    private static String tempProfileDir() {
        try {
            return Files.createTempDirectory("selenium-profile").toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ------------------------------------------------------------------
    // Generic helpers
    // ------------------------------------------------------------------
    protected void open(String path) {
        driver.get(BASE_URL + path);
    }

    protected WebElement find(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    protected void type(By by, String text) {
        WebElement element = find(by);
        element.clear();
        element.sendKeys(text);
    }

    protected void click(By by) {
        wait.until(ExpectedConditions.elementToBeClickable(by)).click();
    }

    /** Date inputs are locale-dependent when typed, so set the value directly (yyyy-MM-dd). */
    protected void setDate(By by, String isoDate) {
        WebElement element = find(by);
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", element, isoDate);
    }

    protected String successMessage() {
        return find(By.cssSelector(".alert.success")).getText();
    }

    protected String uniqueSuffix() {
        return String.valueOf(System.currentTimeMillis() % 1_000_000_000L);
    }

    /** Takes an evidence screenshot (saved in target/screenshots) for the test report. */
    protected void snapshot(String label) {
        ScreenshotOnFailureExtension.capture(driver, "EVIDENCE_" + label);
    }

    // ------------------------------------------------------------------
    // Application actions (used by several journeys)
    // ------------------------------------------------------------------
    protected void login(String username, String password) {
        open("/login");
        type(By.id("username"), username);
        type(By.id("password"), password);
        click(By.id("login-btn"));
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("logout-btn")),
                ExpectedConditions.urlContains("/login?error")));
    }

    protected void logout() {
        click(By.id("logout-btn"));
        wait.until(ExpectedConditions.urlContains("/login?logout"));
    }

    protected void addMachine(String code, String name) {
        open("/machines/add");
        type(By.id("machineCode"), code);
        type(By.id("machineName"), name);
        type(By.id("machineType"), "CNC");
        type(By.id("location"), "Selenium Lab");
        click(By.id("save-machine-btn"));
        successMessage();
    }

    protected void createMaintenance(String machineLabel, String problem, String priority, String technician) {
        open("/maintenance/add");
        new Select(find(By.id("machineId"))).selectByVisibleText(machineLabel);
        type(By.id("description"), problem);
        new Select(find(By.id("priority"))).selectByValue(priority);
        new Select(find(By.id("assignedTo"))).selectByValue(technician);
        setDate(By.id("dueDate"), LocalDate.now().plusDays(7).toString());
        click(By.id("save-maintenance-btn"));
        successMessage();
    }

    protected void searchMaintenance(String text) {
        open("/maintenance");
        type(By.id("search"), text);
        click(By.id("search-btn"));
    }

    protected String rowXpath(String problem) {
        return "//table[@id='maintenance-table']//tr[td[contains(normalize-space(.),'" + problem + "')]]";
    }

    protected String statusOf(String problem) {
        return find(By.xpath(rowXpath(problem) + "//span[contains(@class,'st-')]")).getText();
    }

    /** Admin creates a machine + a maintenance request assigned to tech1, then logs out. Returns the problem text. */
    protected String createJobForTech1() {
        String suffix = uniqueSuffix();
        String code = "SEL-" + suffix;
        String machineName = "Selenium Drill " + suffix;
        String problem = "Belt slip " + suffix;

        login("admin", "admin123");
        addMachine(code, machineName);
        createMaintenance(code + " - " + machineName, problem, "MEDIUM", "tech1");
        successMessage();      // wait until the request is saved
        logout();
        return problem;
    }
}
