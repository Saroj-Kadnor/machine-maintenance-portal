package com.saroj.machine_maintenance_portal.config;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.saroj.machine_maintenance_portal.model.Roles;
import com.saroj.machine_maintenance_portal.repository.UserRepository;
import com.saroj.machine_maintenance_portal.security.CurrentUser;

/** Adds the logged-in user's info to every page (used by the sidebar and role-based buttons). */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository userRepository;

    public GlobalModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addCurrentUser(Authentication auth, Model model) {

        String role = CurrentUser.role(auth);
        String username = CurrentUser.username(auth);

        model.addAttribute("currentRole", role);
        model.addAttribute("currentUsername", username);
        model.addAttribute("canManage", CurrentUser.canManage(auth));
        model.addAttribute("isAdmin", Roles.ADMIN.equals(role));
        model.addAttribute("isTechnician", Roles.TECHNICIAN.equals(role));

        String displayName = username;
        if (!username.isEmpty()) {
            displayName = userRepository.findByUsername(username)
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername())
                    .orElse(username);
        }
        model.addAttribute("displayName", displayName);
    }
}
