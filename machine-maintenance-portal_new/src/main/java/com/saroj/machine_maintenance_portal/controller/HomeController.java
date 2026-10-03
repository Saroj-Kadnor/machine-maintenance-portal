package com.saroj.machine_maintenance_portal.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.saroj.machine_maintenance_portal.model.Maintenance;
import com.saroj.machine_maintenance_portal.model.Roles;
import com.saroj.machine_maintenance_portal.repository.MachineRepository;
import com.saroj.machine_maintenance_portal.repository.MaintenanceRepository;
import com.saroj.machine_maintenance_portal.security.CurrentUser;
import com.saroj.machine_maintenance_portal.service.StatusWorkflowService;

@Controller
public class HomeController {

    private static final String C_PENDING = "#f59e0b";
    private static final String C_PROGRESS = "#3b82f6";
    private static final String C_DONE = "#22c55e";
    private static final String C_CANCEL = "#9ca3af";
    private static final String C_DOWN = "#ef4444";

    private final MachineRepository machineRepository;
    private final MaintenanceRepository maintenanceRepository;

    public HomeController(MachineRepository machineRepository,
                          MaintenanceRepository maintenanceRepository) {
        this.machineRepository = machineRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String dashboard(Authentication auth, Model model) {

        if (Roles.TECHNICIAN.equals(CurrentUser.role(auth))) {
            technicianDashboard(CurrentUser.username(auth), model);
            return "index";
        }

        adminDashboard(model);
        return "index";
    }

    // ------------------------------------------------------------------
    // Admin / Manager: whole-plant overview
    // ------------------------------------------------------------------
    private void adminDashboard(Model model) {

        long totalMachines = machineRepository.count();
        long active = machineRepository.countByStatus("ACTIVE");
        long inactive = machineRepository.countByStatus("INACTIVE");
        long underMaintenance = machineRepository.countByStatus("UNDER_MAINTENANCE");
        long down = machineRepository.countByStatus("DOWN");

        long pending = maintenanceRepository.countByStatus(StatusWorkflowService.PENDING);
        long inProgress = maintenanceRepository.countByStatus(StatusWorkflowService.IN_PROGRESS);
        long completed = maintenanceRepository.countByStatus(StatusWorkflowService.COMPLETED);
        long cancelled = maintenanceRepository.countByStatus(StatusWorkflowService.CANCELLED);
        long totalMaintenance = maintenanceRepository.count();
        long open = pending + inProgress;

        long critical = openByPriority("CRITICAL");
        long high = openByPriority("HIGH");
        long medium = openByPriority("MEDIUM");
        long low = openByPriority("LOW");

        model.addAttribute("totalMachines", totalMachines);
        model.addAttribute("active", active);
        model.addAttribute("inactive", inactive);
        model.addAttribute("underMaintenance", underMaintenance);
        model.addAttribute("down", down);

        model.addAttribute("totalMaintenance", totalMaintenance);
        model.addAttribute("pending", pending);
        model.addAttribute("inProgress", inProgress);
        model.addAttribute("completed", completed);
        model.addAttribute("cancelled", cancelled);
        model.addAttribute("open", open);

        model.addAttribute("critical", critical);
        model.addAttribute("high", high);
        model.addAttribute("medium", medium);
        model.addAttribute("low", low);
        model.addAttribute("pctCritical", percent(critical, open));
        model.addAttribute("pctHigh", percent(high, open));
        model.addAttribute("pctMedium", percent(medium, open));
        model.addAttribute("pctLow", percent(low, open));

        model.addAttribute("completionRate", percent(completed, totalMaintenance));

        model.addAttribute("maintenanceDonut", donut(
                new long[] {pending, inProgress, completed, cancelled},
                new String[] {C_PENDING, C_PROGRESS, C_DONE, C_CANCEL}));
        model.addAttribute("machineDonut", donut(
                new long[] {active, underMaintenance, down, inactive},
                new String[] {C_DONE, C_PENDING, C_DOWN, C_CANCEL}));

        model.addAttribute("recent", maintenanceRepository.findTop5ByOrderByIdDesc());
        model.addAttribute("machineLabels", machineLabels());
    }

    // ------------------------------------------------------------------
    // Technician: only their own jobs
    // ------------------------------------------------------------------
    private void technicianDashboard(String username, Model model) {

        List<Maintenance> mine = maintenanceRepository.findByAssignedToOrderByIdDesc(username);

        long pending = count(mine, StatusWorkflowService.PENDING);
        long inProgress = count(mine, StatusWorkflowService.IN_PROGRESS);
        long completed = count(mine, StatusWorkflowService.COMPLETED);
        long cancelled = count(mine, StatusWorkflowService.CANCELLED);
        long total = mine.size();

        LocalDate today = LocalDate.now();
        long overdue = mine.stream()
                .filter(m -> StatusWorkflowService.OPEN_STATUSES.contains(m.getStatus()))
                .filter(m -> m.getDueDate() != null && m.getDueDate().isBefore(today))
                .count();

        List<Maintenance> myActive = maintenanceRepository.findByAssignedToAndStatusInOrderByIdDesc(
                username, StatusWorkflowService.OPEN_STATUSES);

        model.addAttribute("myTotal", total);
        model.addAttribute("myPending", pending);
        model.addAttribute("myInProgress", inProgress);
        model.addAttribute("myCompleted", completed);
        model.addAttribute("myOverdue", overdue);
        model.addAttribute("myCompletionRate", percent(completed, total));
        model.addAttribute("myDonut", donut(
                new long[] {pending, inProgress, completed, cancelled},
                new String[] {C_PENDING, C_PROGRESS, C_DONE, C_CANCEL}));
        model.addAttribute("myActive", myActive);
        model.addAttribute("today", today);
        model.addAttribute("machineLabels", machineLabels());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private long openByPriority(String priority) {
        return maintenanceRepository.countByPriorityAndStatusIn(priority, StatusWorkflowService.OPEN_STATUSES);
    }

    private long count(List<Maintenance> list, String status) {
        return list.stream().filter(m -> status.equals(m.getStatus())).count();
    }

    private Map<Long, String> machineLabels() {
        Map<Long, String> labels = new HashMap<>();
        machineRepository.findAll().forEach(m ->
                labels.put(m.getId(), m.getMachineCode() + " - " + m.getMachineName()));
        return labels;
    }

    private int percent(long part, long total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }

    /** Builds a CSS conic-gradient() for a donut chart. */
    private String donut(long[] values, String[] colors) {
        long total = 0;
        for (long v : values) {
            total += v;
        }
        if (total == 0) {
            return "conic-gradient(#e5e7eb 0% 100%)";
        }
        StringBuilder sb = new StringBuilder("conic-gradient(");
        double start = 0;
        boolean first = true;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) {
                continue;
            }
            double end = start + values[i] * 100.0 / total;
            if (!first) {
                sb.append(", ");
            }
            sb.append(colors[i]).append(' ')
              .append(String.format(Locale.ROOT, "%.2f", start)).append("% ")
              .append(String.format(Locale.ROOT, "%.2f", end)).append('%');
            start = end;
            first = false;
        }
        return sb.append(')').toString();
    }
}
