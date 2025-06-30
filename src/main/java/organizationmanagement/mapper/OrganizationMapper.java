package organizationmanagement.mapper;

import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.model.Organization;

public class OrganizationMapper {
    public static OrganizationDTO toDTO(Organization org) {
        if (org == null) return null;
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        return dto;
    }
} 