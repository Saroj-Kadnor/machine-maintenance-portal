package com.saroj.machine_maintenance_portal.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.saroj.machine_maintenance_portal.model.Maintenance;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    long countByStatus(String status);

    long countByPriorityAndStatusIn(String priority, Collection<String> statuses);

    long countByAssignedToAndStatusIn(String assignedTo, Collection<String> statuses);

    long countByAssignedToAndStatus(String assignedTo, String status);

    boolean existsByMachineId(Long machineId);

    List<Maintenance> findByMachineIdOrderByIdDesc(Long machineId);

    List<Maintenance> findTop5ByOrderByIdDesc();

    List<Maintenance> findByAssignedToOrderByIdDesc(String assignedTo);

    List<Maintenance> findByAssignedToAndStatusInOrderByIdDesc(String assignedTo, Collection<String> statuses);

    /**
     * Search by request ID, description, technician, or the machine's
     * name / code / type / location, with optional exact filters.
     * Empty strings mean "no filter"; qId = -1 means "not a number".
     */
    @Query("""
            SELECT m FROM Maintenance m
            WHERE (:status = '' OR m.status = :status)
              AND (:priority = '' OR m.priority = :priority)
              AND (:technician = '' OR m.assignedTo = :technician)
              AND (:q = ''
                   OR m.id = :qId
                   OR LOWER(m.description) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.assignedTo) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR m.machineId IN (
                        SELECT mc.id FROM Machine mc
                        WHERE LOWER(mc.machineName) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(mc.machineCode) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(mc.machineType) LIKE LOWER(CONCAT('%', :q, '%'))
                           OR LOWER(mc.location) LIKE LOWER(CONCAT('%', :q, '%'))))
            ORDER BY m.id DESC
            """)
    List<Maintenance> search(@Param("q") String q,
                             @Param("qId") Long qId,
                             @Param("status") String status,
                             @Param("priority") String priority,
                             @Param("technician") String technician);
}
