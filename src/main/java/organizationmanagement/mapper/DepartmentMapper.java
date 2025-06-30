package organizationmanagement.mapper;

import organizationmanagement.dto.DepartmentCreateDTO;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;

public class DepartmentMapper {
    public static DepartmentDTO toDTO(Department dept) {
        if (dept == null) return null;
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(dept.getId());
        dto.setName(dept.getName());
        Organization org = dept.getOrganization();
        if (org != null) {
            OrganizationDTO orgDto = new OrganizationDTO();
            orgDto.setId(org.getId());
            orgDto.setName(org.getName());
            dto.setOrganization(orgDto);
        }
        return dto;
    }

    public static Department toEntity(DepartmentCreateDTO dto, Organization org) {
        if (dto == null) return null;
        Department dept = new Department();
        dept.setName(dto.getName());
        dept.setOrganization(org);
        return dept;
    }
} 