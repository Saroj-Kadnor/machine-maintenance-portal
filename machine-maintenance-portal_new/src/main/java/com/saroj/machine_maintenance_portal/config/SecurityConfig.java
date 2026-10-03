package com.saroj.machine_maintenance_portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.saroj.machine_maintenance_portal.model.Roles;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/error", "/error/**").permitAll()

                // Machines: Admin + Manager can add/edit, only Admin can delete
                .requestMatchers("/machines/add", "/machines/edit/**", "/machines/save")
                    .hasAnyRole(Roles.ADMIN, Roles.MANAGER)
                .requestMatchers("/machines/delete/**").hasRole(Roles.ADMIN)

                // Maintenance: Admin + Manager can create / assign
                // (updates and status changes are checked per-record in the controller)
                .requestMatchers("/maintenance/add", "/maintenance/save")
                    .hasAnyRole(Roles.ADMIN, Roles.MANAGER)

                // Machine list is for Admin / Manager (technicians only reach a machine through their own job)
                .requestMatchers("/machines").hasAnyRole(Roles.ADMIN, Roles.MANAGER)

                // Technicians: list = Admin + Manager, adding a technician = Admin only
                .requestMatchers("/technicians/add", "/technicians/save").hasRole(Roles.ADMIN)
                .requestMatchers("/technicians").hasAnyRole(Roles.ADMIN, Roles.MANAGER)

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        // CSRF protection is ON (Thymeleaf th:action adds the token to every form)
        return http.build();
    }
}
