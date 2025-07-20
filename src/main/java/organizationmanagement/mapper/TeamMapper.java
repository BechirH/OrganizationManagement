package organizationmanagement.mapper;

import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.TeamDTO;
import organizationmanagement.model.Department;
import organizationmanagement.model.Team;
import organizationmanagement.mapper.DepartmentMapper;

public class TeamMapper {
    public static TeamDTO toDTO(Team team) {
        if (team == null) return null;
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        Department dept = team.getDepartment();
        if (dept != null) {
            dto.setDepartment(DepartmentMapper.toDTO(dept)); // Use the full mapper!
        }
        return dto;
    }
} 