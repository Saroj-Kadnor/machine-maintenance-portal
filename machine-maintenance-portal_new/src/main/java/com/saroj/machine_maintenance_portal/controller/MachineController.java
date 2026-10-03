package com.saroj.machine_maintenance_portal.controller;

import java.time.LocalDate;
import java.util.List;
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

import com.saroj.machine_maintenance_portal.model.Machine;
import com.saroj.machine_maintenance_portal.model.Maintenance;
import com.saroj.machine_maintenance_portal.model.Roles;
import com.saroj.machine_maintenance_portal.repository.MachineRepository;
import com.saroj.machine_maintenance_portal.repository.MaintenanceRepository;
import com.saroj.machine_maintenance_portal.security.CurrentUser;

@Controller
public class MachineController {

    private static final List<String> MACHINE_STATUSES =
            List.of("ACTIVE", "INACTIVE", "UNDER_MAINTENANCE", "DOWN");

    private final MachineRepository machineRepository;
    private final MaintenanceRepository maintenanceRepository;

    public MachineController(MachineRepository machineRepository,
                             MaintenanceRepository maintenanceRepository) {
        this.machineRepository = machineRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    // ---------- List + search + filters ----------
    @GetMapping("/machines")
    public String machines(@RequestParam(defaultValue = "") String search,
                           @RequestParam(defaultValue = "") String status,
                           @RequestParam(defaultValue = "") String type,
                           @RequestParam(defaultValue = "") String location,
                           Model model) {

        model.addAttribute("machines",
                machineRepository.search(search.trim(), status, type, location));

        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("location", location);

        model.addAttribute("statuses", MACHINE_STATUSES);
        model.addAttribute("types", machineRepository.findDistinctTypes());
        model.addAttribute("locations", machineRepository.findDistinctLocations());

        return "machines";
    }

    // ---------- Details ----------
    @GetMapping("/machines/{id}")
    public String details(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {

        Optional<Machine> machine = machineRepository.findById(id);
        if (machine.isEmpty()) {
            ra.addFlashAttribute("error", "Machine not found.");
            return "redirect:/machines";
        }

        List<Maintenance> history = maintenanceRepository.findByMachineIdOrderByIdDesc(id);

        // Technicians only see their own jobs on this machine
        if (Roles.TECHNICIAN.equals(CurrentUser.role(auth))) {
            String me = CurrentUser.username(auth);
            history = history.stream().filter(m -> me.equalsIgnoreCase(m.getAssignedTo())).toList();
            if (history.isEmpty()) {
                ra.addFlashAttribute("error", "You have no maintenance jobs on that machine.");
                return "redirect:/";
            }
        }

        model.addAttribute("machine", machine.get());
        model.addAttribute("history", history);
        return "machine-detail";
    }

    // ---------- Add ----------
    @GetMapping("/machines/add")
    public String addMachineForm(Model model) {

        Machine machine = new Machine();
        machine.setStatus("ACTIVE");

        model.addAttribute("machine", machine);
        model.addAttribute("statuses", MACHINE_STATUSES);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editMode", false);
        return "machine-form";
    }

    // ---------- Edit ----------
    @GetMapping("/machines/edit/{id}")
    public String editMachineForm(@PathVariable Long id, Model model, RedirectAttributes ra) {

        Optional<Machine> machine = machineRepository.findById(id);
        if (machine.isEmpty()) {
            ra.addFlashAttribute("error", "Machine not found.");
            return "redirect:/machines";
        }

        model.addAttribute("machine", machine.get());
        model.addAttribute("statuses", MACHINE_STATUSES);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editMode", true);
        return "machine-form";
    }

    // ---------- Save (create + update) ----------
    @PostMapping("/machines/save")
    public String saveMachine(@ModelAttribute Machine form, RedirectAttributes ra) {

        boolean isUpdate = form.getId() != null;
        String backToForm = isUpdate ? "redirect:/machines/edit/" + form.getId() : "redirect:/machines/add";

        String code = form.getMachineCode() == null ? "" : form.getMachineCode().trim();
        String name = form.getMachineName() == null ? "" : form.getMachineName().trim();

        if (code.isEmpty() || name.isEmpty()
                || isBlank(form.getMachineType()) || isBlank(form.getLocation())) {
            ra.addFlashAttribute("error", "Machine code, name, type and location are required.");
            return backToForm;
        }

        if (form.getInstallationDate() != null && form.getInstallationDate().isAfter(LocalDate.now())) {
            ra.addFlashAttribute("error", "Installation date cannot be in the future.");
            return backToForm;
        }

        Machine target;
        if (isUpdate) {
            Optional<Machine> existing = machineRepository.findById(form.getId());
            if (existing.isEmpty()) {
                ra.addFlashAttribute("error", "Machine not found.");
                return "redirect:/machines";
            }
            target = existing.get();
        } else {
            target = new Machine();
        }

        // Machine code must be unique
        boolean duplicate = machineRepository.findByMachineCodeIgnoreCase(code).stream()
                .anyMatch(m -> !m.getId().equals(target.getId()));
        if (duplicate) {
            ra.addFlashAttribute("error", "Machine code '" + code + "' already exists.");
            return backToForm;
        }

        target.setMachineCode(code);
        target.setMachineName(name);
        target.setMachineType(form.getMachineType().trim());
        target.setLocation(form.getLocation().trim());
        target.setManufacturer(form.getManufacturer());
        target.setModel(form.getModel());
        target.setInstallationDate(form.getInstallationDate());
        target.setDescription(form.getDescription());
        target.setStatus(form.getStatus() == null || form.getStatus().isEmpty() ? "ACTIVE" : form.getStatus());

        machineRepository.save(target);

        ra.addFlashAttribute("success",
                isUpdate ? "Machine updated successfully." : "Machine added successfully.");
        return "redirect:/machines";
    }

    // ---------- Delete (admin only - enforced in SecurityConfig) ----------
    @PostMapping("/machines/delete/{id}")
    public String deleteMachine(@PathVariable Long id, RedirectAttributes ra) {

        if (maintenanceRepository.existsByMachineId(id)) {
            ra.addFlashAttribute("error",
                    "This machine has maintenance records and cannot be deleted. Set its status to INACTIVE instead.");
            return "redirect:/machines";
        }

        machineRepository.deleteById(id);
        ra.addFlashAttribute("success", "Machine deleted.");
        return "redirect:/machines";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
