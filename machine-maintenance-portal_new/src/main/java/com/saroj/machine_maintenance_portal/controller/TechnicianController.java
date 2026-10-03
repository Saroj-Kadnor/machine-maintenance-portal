package com.saroj.machine_maintenance_portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.saroj.machine_maintenance_portal.model.AppUser;
import com.saroj.machine_maintenance_portal.model.Roles;
import com.saroj.machine_maintenance_portal.repository.MaintenanceRepository;
import com.saroj.machine_maintenance_portal.repository.UserRepository;
import com.saroj.machine_maintenance_portal.service.StatusWorkflowService;

@Controller
public class TechnicianController {

    private final UserRepository userRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PasswordEncoder passwordEncoder;

    public TechnicianController(UserRepository userRepository,
                                MaintenanceRepository maintenanceRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- List (Admin + Manager) ----------
    @GetMapping("/technicians")
    public String technicians(Model model) {

        List<AppUser> technicians = userRepository.findByRoleOrderByFullNameAsc(Roles.TECHNICIAN);

        Map<String, Long> openJobs = new HashMap<>();
        Map<String, Long> completedJobs = new HashMap<>();
        for (AppUser t : technicians) {
            openJobs.put(t.getUsername(), maintenanceRepository
                    .countByAssignedToAndStatusIn(t.getUsername(), StatusWorkflowService.OPEN_STATUSES));
            completedJobs.put(t.getUsername(), maintenanceRepository
                    .countByAssignedToAndStatus(t.getUsername(), StatusWorkflowService.COMPLETED));
        }

        model.addAttribute("technicians", technicians);
        model.addAttribute("openJobs", openJobs);
        model.addAttribute("completedJobs", completedJobs);
        return "technicians";
    }

    // ---------- Add technician (Admin only - enforced in SecurityConfig) ----------
    @GetMapping("/technicians/add")
    public String addForm() {
        return "technician-form";
    }

    @PostMapping("/technicians/save")
    public String save(@RequestParam String fullName,
                       @RequestParam String username,
                       @RequestParam String password,
                       @RequestParam String confirmPassword,
                       RedirectAttributes ra) {

        String name = fullName.trim();
        String user = username.trim();

        // keep what was typed so the form can be re-filled
        ra.addFlashAttribute("fullName", name);
        ra.addFlashAttribute("username", user);

        if (name.isEmpty() || user.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            ra.addFlashAttribute("error", "All fields are required.");
            return "redirect:/technicians/add";
        }
        if (!user.matches("^[A-Za-z0-9_]{3,30}$")) {
            ra.addFlashAttribute("error",
                    "Username must be 3-30 characters: letters, numbers or underscore only.");
            return "redirect:/technicians/add";
        }
        if (password.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/technicians/add";
        }
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/technicians/add";
        }
        if (userRepository.existsByUsername(user)) {
            ra.addFlashAttribute("error", "Username '" + user + "' is already taken.");
            return "redirect:/technicians/add";
        }

        userRepository.save(new AppUser(user, passwordEncoder.encode(password), name, Roles.TECHNICIAN));

        ra.addFlashAttribute("success", "Technician '" + name + "' added. They can log in as '" + user + "'.");
        return "redirect:/technicians";
    }
}
