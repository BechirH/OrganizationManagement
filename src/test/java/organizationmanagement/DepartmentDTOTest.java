package organizationmanagement;

import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.mapper.DepartmentMapper;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DepartmentDTOTest {

    @Test
    void testDepartmentDTOStructure() {
        // Create test data
        UUID orgId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        
        Organization org = new Organization();
        org.setId(orgId);
        org.setName("Test Organization");
        
        Department dept = new Department();
        dept.setId(deptId);
        dept.setName("Test Department");
        dept.setOrganization(org);
        
        // Test the mapper
        DepartmentDTO dto = DepartmentMapper.toDTO(dept);
        
        // Verify the structure
        assertNotNull(dto);
        assertEquals(deptId, dto.getId());
        assertEquals("Test Department", dto.getName());
        assertNotNull(dto.getOrganization());
        assertEquals(orgId, dto.getOrganization().getId());
        assertEquals("Test Organization", dto.getOrganization().getName());
        
        // Verify that the old redundant fields are not accessible
        // This test will fail if the fields still exist, which is what we want
        try {
            // Try to access the old fields using reflection (they should not exist)
            dto.getClass().getDeclaredField("departmentId");
            fail("departmentId field should not exist");
        } catch (NoSuchFieldException e) {
            // Expected - the field should not exist
        }
        
        try {
            // Try to access the old fields using reflection (they should not exist)
            dto.getClass().getDeclaredField("departmentName");
            fail("departmentName field should not exist");
        } catch (NoSuchFieldException e) {
            // Expected - the field should not exist
        }
    }
    
    @Test
    void testDepartmentDTOSerialization() {
        // Create test data
        UUID orgId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        
        Organization org = new Organization();
        org.setId(orgId);
        org.setName("Test Organization");
        
        Department dept = new Department();
        dept.setId(deptId);
        dept.setName("Test Department");
        dept.setOrganization(org);
        
        // Test the mapper
        DepartmentDTO dto = DepartmentMapper.toDTO(dept);
        
        // Verify the JSON structure would be correct
        // The JSON should look like:
        // {
        //   "id": "dept-id",
        //   "name": "Test Department",
        //   "organization": {
        //     "id": "org-id",
        //     "name": "Test Organization"
        //   }
        // }
        
        assertNotNull(dto.getId());
        assertNotNull(dto.getName());
        assertNotNull(dto.getOrganization());
        assertNotNull(dto.getOrganization().getId());
        assertNotNull(dto.getOrganization().getName());
    }
} 