package com.saroj.machine_maintenance_portal.selenium;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Failure screenshot mechanism.
 * JUnit calls this when a test method throws (assertion failure or Selenium error),
 * BEFORE the browser is closed, so the screenshot shows the page at the moment of failure.
 * Files are saved to target/screenshots/.
 */
public class ScreenshotOnFailureExtension implements TestExecutionExceptionHandler {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {

        Object instance = context.getRequiredTestInstance();
        if (instance instanceof BaseSeleniumTest base && base.driver != null) {
            String name = "FAILED_" + context.getRequiredTestClass().getSimpleName()
                    + "_" + context.getRequiredTestMethod().getName();
            capture(base.driver, name);
        }
        throw throwable;   // keep the test marked as failed
    }

    /** Saves a PNG to target/screenshots and returns its path (or null if it could not be taken). */
    static Path capture(WebDriver driver, String name) {
        try {
            if (driver instanceof TakesScreenshot shooter) {
                byte[] png = shooter.getScreenshotAs(OutputType.BYTES);
                Path dir = Path.of("target", "screenshots");
                Files.createDirectories(dir);
                Path file = dir.resolve(name + "_" + LocalDateTime.now().format(STAMP) + ".png");
                Files.write(file, png);
                System.out.println("Screenshot saved: " + file.toAbsolutePath());
                return file;
            }
        } catch (Exception e) {
            System.err.println("Could not take screenshot: " + e.getMessage());
        }
        return null;
    }
}
