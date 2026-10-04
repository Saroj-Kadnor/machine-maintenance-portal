package com.saroj.machine_maintenance_portal.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Journey 3: Manager creates a maintenance request and finds it again */
class MaintenanceJourneyTest extends BaseSeleniumTest {

    @Test
    @DisplayName("J3-TC1 Manager creates a request assigned to a technician and finds it by search")
    void managerCanCreateMaintenanceRequest() {
        String suffix = uniqueSuffix();
        String code = "SEL-" + suffix;
        String machineName = "Selenium Lathe " + suffix;
        String problem = "Oil leak " + suffix;

        login("manager", "manager123");
        addMachine(code, machineName);
        successMessage();

        createMaintenance(code + " - " + machineName, problem, "HIGH", "tech1");
        assertTrue(successMessage().contains("created"));

        searchMaintenance(problem);
        WebElement row = find(By.xpath(rowXpath(problem)));
        assertEquals("PENDING", statusOf(problem));
        assertTrue(row.getText().contains("HIGH"));
        assertTrue(row.getText().contains("tech1"));
        snapshot("J3_request_created");
    }
}
