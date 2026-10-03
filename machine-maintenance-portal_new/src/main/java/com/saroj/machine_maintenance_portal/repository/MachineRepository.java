package com.saroj.machine_maintenance_portal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.saroj.machine_maintenance_portal.model.Machine;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    List<Machine> findByMachineCodeIgnoreCase(String machineCode);

    long countByStatus(String status);

    @Query("""
            SELECT m FROM Machine m
            WHERE (:status = '' OR m.status = :status)
              AND (:type = '' OR m.machineType = :type)
              AND (:location = '' OR m.location = :location)
              AND (:q = ''
                   OR LOWER(m.machineName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.machineCode) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.machineType) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.location) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.manufacturer) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY m.id DESC
            """)
    List<Machine> search(@Param("q") String q,
                         @Param("status") String status,
                         @Param("type") String type,
                         @Param("location") String location);

    @Query("SELECT DISTINCT m.machineType FROM Machine m WHERE m.machineType IS NOT NULL AND m.machineType <> '' ORDER BY m.machineType")
    List<String> findDistinctTypes();

    @Query("SELECT DISTINCT m.location FROM Machine m WHERE m.location IS NOT NULL AND m.location <> '' ORDER BY m.location")
    List<String> findDistinctLocations();
}
