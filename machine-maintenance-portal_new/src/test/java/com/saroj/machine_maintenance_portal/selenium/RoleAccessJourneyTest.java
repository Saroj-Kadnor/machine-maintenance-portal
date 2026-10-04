package com.saroj.machine_maintenance_portal.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/** Journey 5: Role-based access + admin adds a technician */
class RoleAccessJourneyTest extends BaseSeleniumTest {

    @Test
    @DisplayName("J5-TC1 Technician has a limited menu and is blocked from admin pages")
    void technicianIsLimitedAndBlockedFromAdminPages() {
        login("tech1", "tech123");

        assertEquals("Dashboard", find(By.cssSelector("h1.page-title")).getText());
        assertTrue(driver.findElements(By.id("nav-machines")).isEmpty());
        assertTrue(driver.findElements(By.id("nav-technicians")).isEmpty());

        open("/machines/add");
        assertTrue(find(By.tagName("h1")).getText().contains("Access denied"));
        snapshot("J5_access_denied");
    }

    @Test
    @DisplayName("J5-TC2 Only Admin sees the Add Technician button")
    void onlyAdminSeesAddTechnicianButton() {
        login("manager", "manager123");
        open("/technicians");
        find(By.cssSelector("h1.page-title"));
        assertTrue(driver.findElements(By.id("add-technician-btn")).isEmpty());
        logout();

        login("admin", "admin123");
        open("/technicians");
        assertTrue(find(By.id("add-technician-btn")).isDisplayed());
    }

    @Test
    @DisplayName("J5-TC3 Admin adds a technician who can then log in")
    void adminCanAddTechnicianWhoCanLogin() {
        String username = "seltech" + uniqueSuffix();

        login("admin", "admin123");
        open("/technicians/add");
        type(By.id("fullName"), "Selenium Technician");
        type(By.id("username"), username);
        type(By.id("password"), "secret1");
        type(By.id("confirmPassword"), "secret1");
        click(By.id("save-technician-btn"));

        assertTrue(successMessage().contains("added"));
        logout();

        login(username, "secret1");
        assertEquals("Dashboard", find(By.cssSelector("h1.page-title")).getText());
    }
}
