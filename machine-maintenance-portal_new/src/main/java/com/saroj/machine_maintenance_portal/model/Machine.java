package com.saroj.machine_maintenance_portal.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String machineCode;
    private String machineName;
    private String machineType;
    private String location;
    private String manufacturer;
    private String model;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate installationDate;

    // ACTIVE, INACTIVE, UNDER_MAINTENANCE, DOWN
    private String status;

    @Column(length = 1000)
    private String description;

    public Machine() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }

    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }

    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public LocalDate getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDate installationDate) { this.installationDate = installationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
