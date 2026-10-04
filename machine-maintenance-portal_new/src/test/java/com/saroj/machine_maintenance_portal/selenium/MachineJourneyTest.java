package com.saroj.machine_maintenance_portal.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Journey 2: Machine management (add, search, validation) */
class MachineJourneyTest extends BaseSeleniumTest {

    @Test
    @DisplayName("J2-TC1 Admin adds a machine and it appears in the list")
    void adminCanAddMachine() {
        String code = "SEL-" + uniqueSuffix();

        login("admin", "admin123");
        addMachine(code, "Selenium Press " + code);

        assertTrue(successMessage().contains("Machine added successfully"));
        assertTrue(find(By.id("machines-table")).getText().contains(code));
        snapshot("J2_machine_added");
    }

    @Test
    @DisplayName("J2-TC2 Searching by machine code returns only that machine")
    void searchFindsOnlyMatchingMachine() {
        String code = "SEL-" + uniqueSuffix();

        login("admin", "admin123");
        addMachine(code, "Selenium Mill " + code);
        successMessage();

        open("/machines");
        type(By.id("search"), code);
        click(By.id("search-btn"));

        find(By.id("machines-table"));
        List<WebElement> rows = driver.findElements(By.cssSelector("#machines-table tbody tr"));
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).getText().contains(code));
    }

    @Test
    @DisplayName("J2-TC3 Searching for something that does not exist shows 'No machines found'")
    void searchWithNoMatchShowsEmptyMessage() {
        login("admin", "admin123");

        open("/machines");
        type(By.id("search"), "NOPE-" + uniqueSuffix());
        click(By.id("search-btn"));

        assertEquals("No machines found.", find(By.cssSelector(".empty")).getText());
    }

    @Test
    @DisplayName("J2-TC4 Installation date picker does not allow future dates")
    void installationDateCannotBeInFuture() {
        login("admin", "admin123");
        open("/machines/add");

        String max = find(By.id("installationDate")).getDomAttribute("max");
        assertEquals(LocalDate.now().toString(), max);
    }
}
