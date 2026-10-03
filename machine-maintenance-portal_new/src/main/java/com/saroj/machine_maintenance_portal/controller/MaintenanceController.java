package com.saroj.machine_maintenance_portal.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.saroj.machine_maintenance_portal.model.AppUser;
import com.saroj.machine_maintenance_portal.model.Machine;
import com.saroj.machine_maintenance_portal.model.Maintenance;
import com.saroj.machine_maintenance_portal.model.Roles;
import com.saroj.machine_maintenance_portal.repository.MachineRepository;
import com.saroj.machine_maintenance_portal.repository.MaintenanceRepository;
import com.saroj.machine_maintenance_portal.repository.UserRepository;
import com.saroj.machine_maintenance_portal.security.CurrentUser;
import com.saroj.machine_maintenance_portal.service.StatusWorkflowService;

@Controller
public class MaintenanceController {

    private static final List<String> PRIORITIES = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final MaintenanceRepository maintenanceRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final StatusWorkflowService workflow;

    public MaintenanceController(MaintenanceRepository maintenanceRepository,
                                 MachineRepository machineRepository,
                                 UserRepository userRepository,
                                 StatusWorkflowService workflow) {
        this.maintenanceRepository = maintenanceRepository;
        this.machineRepository = machineRepository;
        this.userRepository = userRepository;
        this.workflow = workflow;
    }

    // ------------------------------------------------------------------
    // List + search + filters
    // ------------------------------------------------------------------
    @GetMapping("/maintenance")
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "") String priority,
                       @RequestParam(defaultValue = "") String technician,
                       Authentication auth,
                       Model model) {

        String role = CurrentUser.role(auth);
        String username = CurrentUser.username(auth);

        // A technician only ever sees their own jobs
        if (Roles.TECHNICIAN.equals(role)) {
            technician = username;
        }

        String q = search.trim();
        List<Maintenance> list = maintenanceRepository.search(q, parseId(q), status, priority, technician);

        // Which quick-action buttons each row may show for this user
        Map<Long, List<String>> actions = new HashMap<>();
        for (Maintenance m : list) {
            actions.put(m.getId(), workflow.nextStatuses(m, role, username));
        }

        model.addAttribute("maintenanceList", list);
        model.addAttribute("actions", actions);
        model.addAttribute("machineLabels", machineLabels());

        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("technician", technician);

        model.addAttribute("statuses", StatusWorkflowService.ALL_STATUSES);
        model.addAttribute("priorities", PRIORITIES);
        model.addAttribute("technicians", userRepository.findByRoleOrderByFullNameAsc(Roles.TECHNICIAN));

        return "maintenance";
    }

    // ------------------------------------------------------------------
    // Details
    // ------------------------------------------------------------------
    @GetMapping("/maintenance/{id}")
    public String details(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {

        Optional<Maintenance> opt = maintenanceRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Maintenance request not found.");
            return "redirect:/maintenance";
        }
        Maintenance m = opt.get();

        if (Roles.TECHNICIAN.equals(CurrentUser.role(auth))
                && !CurrentUser.username(auth).equalsIgnoreCase(m.getAssignedTo())) {
            ra.addFlashAttribute("error", "You can only view maintenance requests assigned to you.");
            return "redirect:/maintenance";
        }

        model.addAttribute("maintenance", m);
        model.addAttribute("machine", m.getMachineId() == null ? null
                : machineRepository.findById(m.getMachineId()).orElse(null));
        model.addAttribute("technicianName", technicianName(m.getAssignedTo()));
        model.addAttribute("actions",
                workflow.nextStatuses(m, CurrentUser.role(auth), CurrentUser.username(auth)));
        model.addAttribute("canEdit", canEdit(m, auth));
        return "maintenance-detail";
    }

    // ------------------------------------------------------------------
    // Create (Admin / Manager)
    // ------------------------------------------------------------------
    @GetMapping("/maintenance/add")
    public String addForm(@RequestParam(required = false) Long machineId, Model model) {

        Maintenance m = new Maintenance();
        m.setMachineId(machineId);
        m.setPriority("MEDIUM");
        m.setStatus(StatusWorkflowService.PENDING);

        populateForm(model, m, false, true, List.of());
        return "maintenance-form";
    }

    @PostMapping("/maintenance/save")
    public String save(@ModelAttribute Maintenance form, RedirectAttributes ra) {

        String error = validate(form);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/maintenance/add";
        }

        // Build a fresh entity - never trust an id / status coming from the form
        Maintenance m = new Maintenance();
        m.setMachineId(form.getMachineId());
        m.setDescription(form.getDescription().trim());
        m.setPriority(isBlank(form.getPriority()) ? "MEDIUM" : form.getPriority());
        m.setAssignedTo(isBlank(form.getAssignedTo()) ? null : form.getAssignedTo());
        m.setDueDate(form.getDueDate());
        m.setRemarks(form.getRemarks());
        m.setStatus(StatusWorkflowService.PENDING);
        m.setCreatedDate(LocalDateTime.now());
        m.setUpdatedDate(LocalDateTime.now());

        maintenanceRepository.save(m);

        ra.addFlashAttribute("success", "Maintenance request #" + m.getId() + " created.");
        return "redirect:/maintenance";
    }

    // ------------------------------------------------------------------
    // Edit / update
    //   Admin + Manager : all fields
    //   Technician      : only remarks + status (own assigned jobs)
    // ------------------------------------------------------------------
    @GetMapping("/maintenance/edit/{id}")
    public String editForm(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {

        Optional<Maintenance> opt = maintenanceRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Maintenance request not found.");
            return "redirect:/maintenance";
        }
        Maintenance m = opt.get();

        if (!canEdit(m, auth)) {
            ra.addFlashAttribute("error", "You can only update maintenance requests assigned to you.");
            return "redirect:/maintenance";
        }

        List<String> next = workflow.nextStatuses(m, CurrentUser.role(auth), CurrentUser.username(auth));
        populateForm(model, m, true, CurrentUser.canManage(auth), next);
        return "maintenance-form";
    }

    @PostMapping("/maintenance/update")
    public String update(@ModelAttribute Maintenance form, Authentication auth, RedirectAttributes ra) {

        Optional<Maintenance> opt = form.getId() == null ? Optional.empty()
                : maintenanceRepository.findById(form.getId());
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Maintenance request not found.");
            return "redirect:/maintenance";
        }
        Maintenance m = opt.get();

        if (!canEdit(m, auth)) {
            ra.addFlashAttribute("error", "You can only update maintenance requests assigned to you.");
            return "redirect:/maintenance";
        }

        String backToForm = "redirect:/maintenance/edit/" + m.getId();

        if (CurrentUser.canManage(auth)) {
            String error = validate(form);
            if (error != null) {
                ra.addFlashAttribute("error", error);
                return backToForm;
            }
            m.setMachineId(form.getMachineId());
            m.setDescription(form.getDescription().trim());
            m.setPriority(isBlank(form.getPriority()) ? "MEDIUM" : form.getPriority());
            m.setAssignedTo(isBlank(form.getAssignedTo()) ? null : form.getAssignedTo());
            m.setDueDate(form.getDueDate());
        }

        // Remarks can be edited by everyone who can open the form
        m.setRemarks(form.getRemarks());

        // Status change must follow the workflow for this role
        String error = applyStatus(m, form.getStatus(), auth);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return backToForm;
        }

        m.setUpdatedDate(LocalDateTime.now());
        maintenanceRepository.save(m);

        ra.addFlashAttribute("success", "Maintenance request #" + m.getId() + " updated.");
        return "redirect:/maintenance";
    }

    // ------------------------------------------------------------------
    // Quick status change (buttons on list + details page)
    // ------------------------------------------------------------------
    @PostMapping("/maintenance/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam String newStatus,
                               @RequestParam(defaultValue = "list") String from,
                               Authentication auth,
                               RedirectAttributes ra) {

        String target = "detail".equals(from) ? "redirect:/maintenance/" + id : "redirect:/maintenance";

        Optional<Maintenance> opt = maintenanceRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Maintenance request not found.");
            return "redirect:/maintenance";
        }
        Maintenance m = opt.get();

        String error = applyStatus(m, newStatus, auth);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return target;
        }

        m.setUpdatedDate(LocalDateTime.now());
        maintenanceRepository.save(m);

        ra.addFlashAttribute("success", "Request #" + id + " is now " + newStatus + ".");
        return target;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Applies a status change if the workflow allows it. Returns an error message or null. */
    private String applyStatus(Maintenance m, String requested, Authentication auth) {

        String current = m.getStatus() == null ? StatusWorkflowService.PENDING : m.getStatus();

        if (isBlank(requested) || requested.equals(current)) {
            return null; // no change
        }

        if (!workflow.canTransition(m, requested, CurrentUser.role(auth), CurrentUser.username(auth))) {
            return "You are not allowed to change status from " + current + " to " + requested + ".";
        }

        m.setStatus(requested);
        return null;
    }

    private boolean canEdit(Maintenance m, Authentication auth) {
        if (CurrentUser.canManage(auth)) {
            return true;
        }
        return Roles.TECHNICIAN.equals(CurrentUser.role(auth))
                && CurrentUser.username(auth).equalsIgnoreCase(m.getAssignedTo());
    }

    private String validate(Maintenance form) {
        if (form.getMachineId() == null || !machineRepository.existsById(form.getMachineId())) {
            return "Please select a valid machine.";
        }
        if (isBlank(form.getDescription())) {
            return "Problem description is required.";
        }
        if (isBlank(form.getPriority()) || !PRIORITIES.contains(form.getPriority())) {
            return "Please select a priority.";
        }
        if (isBlank(form.getAssignedTo())) {
            return "Please assign a technician.";
        }
        if (form.getDueDate() == null) {
            return "Due date is required.";
        }
        if (!isBlank(form.getAssignedTo())) {
            Optional<AppUser> tech = userRepository.findByUsername(form.getAssignedTo());
            boolean ok = tech.isPresent() && Roles.TECHNICIAN.equals(tech.get().getRole());
            // allow keeping an old free-text assignee on existing records
            boolean legacy = form.getId() != null && maintenanceRepository.findById(form.getId())
                    .map(old -> form.getAssignedTo().equals(old.getAssignedTo())).orElse(false);
            if (!ok && !legacy) {
                return "Assigned user must be a technician.";
            }
        }
        return null;
    }

    private void populateForm(Model model, Maintenance m, boolean editMode,
                              boolean fullEdit, List<String> nextStatuses) {

        // status dropdown = current status + allowed next ones
        List<String> statusOptions = new ArrayList<>();
        statusOptions.add(m.getStatus() == null ? StatusWorkflowService.PENDING : m.getStatus());
        statusOptions.addAll(nextStatuses);

        List<AppUser> technicians = userRepository.findByRoleOrderByFullNameAsc(Roles.TECHNICIAN);
        String legacyAssignee = null;
        if (!isBlank(m.getAssignedTo())
                && technicians.stream().noneMatch(t -> t.getUsername().equals(m.getAssignedTo()))) {
            legacyAssignee = m.getAssignedTo();
        }

        model.addAttribute("maintenance", m);
        model.addAttribute("machines", machineRepository.findAll());
        model.addAttribute("technicians", technicians);
        model.addAttribute("legacyAssignee", legacyAssignee);
        model.addAttribute("priorities", PRIORITIES);
        model.addAttribute("statusOptions", statusOptions);
        model.addAttribute("editMode", editMode);
        model.addAttribute("fullEdit", fullEdit);
    }

    private Map<Long, String> machineLabels() {
        Map<Long, String> labels = new HashMap<>();
        for (Machine m : machineRepository.findAll()) {
            labels.put(m.getId(), m.getMachineCode() + " - " + m.getMachineName());
        }
        return labels;
    }

    private String technicianName(String username) {
        if (isBlank(username)) {
            return null;
        }
        return userRepository.findByUsername(username)
                .map(AppUser::getFullName)
                .orElse(username);
    }

    private Long parseId(String q) {
        try {
            return Long.parseLong(q);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
