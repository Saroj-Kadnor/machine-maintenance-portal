package com.saroj.machine_maintenance_portal.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/** Journey 1: Login */
class LoginJourneyTest extends BaseSeleniumTest {

    @Test
    @DisplayName("J1-TC1 Admin logs in and sees the dashboard")
    void adminCanLoginAndSeesDashboard() {
        login("admin", "admin123");

        assertEquals("Dashboard", find(By.cssSelector("h1.page-title")).getText());
        assertTrue(find(By.id("nav-machines")).isDisplayed());
        snapshot("J1_admin_dashboard");
    }

    @Test
    @DisplayName("J1-TC2 Wrong password shows an error message")
    void wrongPasswordShowsError() {
        login("admin", "wrong-password");

        assertTrue(driver.getCurrentUrl().contains("/login?error"));
        assertEquals("Invalid username or password.", find(By.cssSelector(".alert.error")).getText());
    }
}
