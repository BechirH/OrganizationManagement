package organizationmanagement.repository;

import organizationmanagement.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    @Query("SELECT d FROM Department d WHERE d.organization.id = :organizationId")
    List<Department> findByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT COUNT(d) > 0 FROM Department d WHERE d.name = :name AND d.organization.id = :organizationId")
    boolean existsByNameAndOrganizationId(@Param("name") String name, @Param("organizationId") UUID organizationId);

    // New method: Find department by ID within a specific organization
    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.organization.id = :organizationId")
    Optional<Department> findByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    // New method: Check if department name exists in organization excluding a specific ID (for updates)
    @Query("SELECT COUNT(d) > 0 FROM Department d WHERE d.name = :name AND d.organization.id = :organizationId AND d.id != :excludeId")
    boolean existsByNameAndOrganizationIdAndIdNot(@Param("name") String name, @Param("organizationId") UUID organizationId, @Param("excludeId") UUID excludeId);
}