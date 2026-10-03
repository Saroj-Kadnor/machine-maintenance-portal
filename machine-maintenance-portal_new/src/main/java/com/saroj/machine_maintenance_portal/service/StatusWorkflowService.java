package com.saroj.machine_maintenance_portal.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.saroj.machine_maintenance_portal.model.Maintenance;
import com.saroj.machine_maintenance_portal.model.Roles;

/**
 * Role-based status workflow:
 *
 *   PENDING -> IN_PROGRESS -> COMPLETED
 *   PENDING -> CANCELLED
 *
 * ADMIN / MANAGER : every allowed transition
 * TECHNICIAN      : only on requests assigned to them, and cannot cancel
 */
@Service
public class StatusWorkflowService {

    public static final String PENDING = "PENDING";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    public static final List<String> ALL_STATUSES =
            List.of(PENDING, IN_PROGRESS, COMPLETED, CANCELLED);

    public static final List<String> OPEN_STATUSES = List.of(PENDING, IN_PROGRESS);

    /** Statuses this user may move the request to right now (excluding the current one). */
    public List<String> nextStatuses(Maintenance maintenance, String role, String username) {

        String current = maintenance.getStatus() == null ? PENDING : maintenance.getStatus();

        List<String> next = switch (current) {
            case PENDING -> List.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> List.of(COMPLETED);
            default -> List.of();
        };

        if (Roles.ADMIN.equals(role) || Roles.MANAGER.equals(role)) {
            return next;
        }

        if (Roles.TECHNICIAN.equals(role)
                && username != null
                && username.equalsIgnoreCase(maintenance.getAssignedTo())) {
            return next.stream().filter(s -> !CANCELLED.equals(s)).toList();
        }

        return List.of();
    }

    public boolean canTransition(Maintenance maintenance, String newStatus, String role, String username) {
        return nextStatuses(maintenance, role, username).contains(newStatus);
    }
}
