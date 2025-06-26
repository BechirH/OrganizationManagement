package organizationmanagement;

import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void testFindByIdAndOrganizationId() {
        // Create an organization
        Organization org = new Organization();
        org.setName("Test Organization");
        Organization savedOrg = organizationRepository.save(org);

        // Create a department
        Department dept = new Department();
        dept.setName("Test Department");
        dept.setOrganization(savedOrg);
        Department savedDept = departmentRepository.save(dept);

        // Test the method that was failing
        Optional<Department> found = departmentRepository.findByIdAndOrganizationId(savedDept.getId(), savedOrg.getId());
        
        assertTrue(found.isPresent());
        assertEquals(savedDept.getId(), found.get().getId());
        assertEquals(savedOrg.getId(), found.get().getOrganization().getId());
    }

    @Test
    void testFindByIdAndOrganizationIdWithWrongOrganization() {
        // Create two organizations
        Organization org1 = new Organization();
        org1.setName("Test Organization 1");
        Organization savedOrg1 = organizationRepository.save(org1);

        Organization org2 = new Organization();
        org2.setName("Test Organization 2");
        Organization savedOrg2 = organizationRepository.save(org2);

        // Create a department in org1
        Department dept = new Department();
        dept.setName("Test Department");
        dept.setOrganization(savedOrg1);
        Department savedDept = departmentRepository.save(dept);

        // Try to find the department with org2's ID (should not find it)
        Optional<Department> found = departmentRepository.findByIdAndOrganizationId(savedDept.getId(), savedOrg2.getId());
        
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByNameAndOrganizationId() {
        // Create an organization
        Organization org = new Organization();
        org.setName("Test Organization");
        Organization savedOrg = organizationRepository.save(org);

        // Create a department
        Department dept = new Department();
        dept.setName("Test Department");
        dept.setOrganization(savedOrg);
        departmentRepository.save(dept);

        // Test the exists method
        boolean exists = departmentRepository.existsByNameAndOrganizationId("Test Department", savedOrg.getId());
        assertTrue(exists);

        // Test with non-existent name
        boolean notExists = departmentRepository.existsByNameAndOrganizationId("Non-existent Department", savedOrg.getId());
        assertFalse(notExists);
    }
} 