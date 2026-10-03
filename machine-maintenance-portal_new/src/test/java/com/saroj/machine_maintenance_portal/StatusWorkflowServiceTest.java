package com.saroj.machine_maintenance_portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.saroj.machine_maintenance_portal.model.Maintenance;
import com.saroj.machine_maintenance_portal.model.Roles;
import com.saroj.machine_maintenance_portal.service.StatusWorkflowService;

class StatusWorkflowServiceTest {

    private final StatusWorkflowService workflow = new StatusWorkflowService();

    private Maintenance request(String status, String assignedTo) {
        Maintenance m = new Maintenance();
        m.setStatus(status);
        m.setAssignedTo(assignedTo);
        return m;
    }

    @Test
    void managerCanStartOrCancelPendingRequest() {
        List<String> next = workflow.nextStatuses(request("PENDING", "tech1"), Roles.MANAGER, "manager");
        assertEquals(List.of("IN_PROGRESS", "CANCELLED"), next);
    }

    @Test
    void completedRequestHasNoFurtherTransitions() {
        assertTrue(workflow.nextStatuses(request("COMPLETED", "tech1"), Roles.ADMIN, "admin").isEmpty());
    }

    @Test
    void cannotSkipInProgress() {
        assertFalse(workflow.canTransition(request("PENDING", "tech1"), "COMPLETED", Roles.ADMIN, "admin"));
    }

    @Test
    void technicianCanProgressOwnRequestButNotCancel() {
        Maintenance m = request("PENDING", "tech1");
        assertTrue(workflow.canTransition(m, "IN_PROGRESS", Roles.TECHNICIAN, "tech1"));
        assertFalse(workflow.canTransition(m, "CANCELLED", Roles.TECHNICIAN, "tech1"));
    }

    @Test
    void technicianCannotTouchSomeoneElsesRequest() {
        assertTrue(workflow.nextStatuses(request("PENDING", "tech2"), Roles.TECHNICIAN, "tech1").isEmpty());
    }
}
