package organizationmanagement.service;

import organizationmanagement.model.Team;
import java.util.List;
import java.util.UUID;
import organizationmanagement.dto.UserDTO;

public interface TeamService {
    List<Team> getAll();
    Team getById(UUID id);
    void delete(UUID id);
    List<Team> getByDepartmentId(UUID departmentId);
    Team createUnderDepartment(UUID deptId, Team team);
    Team update(UUID id, UUID departmentId, Team updatedTeam);
    List<Team> getAllByOrganization(UUID organizationId);
    Team getByIdAndOrganization(UUID id, UUID organizationId);
    void deleteByIdAndOrganization(UUID id, UUID organizationId);
    List<Team> getByDepartmentIdAndOrganization(UUID departmentId, UUID organizationId);
    Team createUnderDepartmentInOrganization(UUID deptId, Team team, UUID organizationId);
    Team updateInOrganization(UUID id, UUID departmentId, Team updatedTeam, UUID organizationId);
    void assignUserToTeamInOrganization(UUID teamId, UUID userId, UUID organizationId);
    void removeUserFromTeamInOrganization(UUID teamId, UUID userId, UUID organizationId);
    void assignSurveyToTeamInOrganization(UUID teamId, UUID surveyId, UUID organizationId);
    void removeSurveyFromTeamInOrganization(UUID teamId, UUID surveyId, UUID organizationId);
    boolean existsById(UUID id);
    Team findByUserId(UUID userId);
    List<UserDTO> getUsersByTeamId(UUID teamId);
}