package com.saroj.machine_maintenance_portal.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/** Journey 4: Role-based status workflow (technician) */
class WorkflowJourneyTest extends BaseSeleniumTest {

    @Test
    @DisplayName("J4-TC1 Technician moves own job PENDING -> IN_PROGRESS -> COMPLETED")
    void technicianCanStartAndCompleteAssignedJob() {
        String problem = createJobForTech1();

        login("tech1", "tech123");
        searchMaintenance(problem);
        assertEquals("PENDING", statusOf(problem));

        click(By.xpath(rowXpath(problem) + "//button[normalize-space()='Start']"));
        assertTrue(successMessage().contains("IN_PROGRESS"));

        searchMaintenance(problem);
        assertEquals("IN_PROGRESS", statusOf(problem));
        snapshot("J4_in_progress");

        click(By.xpath(rowXpath(problem) + "//button[normalize-space()='Complete']"));
        assertTrue(successMessage().contains("COMPLETED"));

        searchMaintenance(problem);
        assertEquals("COMPLETED", statusOf(problem));
        snapshot("J4_completed");
    }

    @Test
    @DisplayName("J4-TC2 Technician can start a job but is not offered Cancel")
    void technicianCannotCancelAssignedJob() {
        String problem = createJobForTech1();

        login("tech1", "tech123");
        searchMaintenance(problem);
        find(By.xpath(rowXpath(problem)));

        assertEquals(1, driver.findElements(
                By.xpath(rowXpath(problem) + "//button[normalize-space()='Start']")).size());
        assertTrue(driver.findElements(
                By.xpath(rowXpath(problem) + "//button[normalize-space()='Cancel']")).isEmpty());
    }
}
