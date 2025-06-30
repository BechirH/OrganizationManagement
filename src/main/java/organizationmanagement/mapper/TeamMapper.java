package organizationmanagement.mapper;

import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.TeamDTO;
import organizationmanagement.model.Department;
import organizationmanagement.model.Team;

public class TeamMapper {
    public static TeamDTO toDTO(Team team) {
        if (team == null) return null;
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        Department dept = team.getDepartment();
        if (dept != null) {
            DepartmentDTO deptDto = new DepartmentDTO();
            deptDto.setId(dept.getId());
            deptDto.setName(dept.getName());
            dto.setDepartment(deptDto);
        }
        return dto;
    }
} 