package organizationmanagement.service.impl;

import organizationmanagement.client.SurveyServiceClient;
import organizationmanagement.client.UserServiceClient;
import organizationmanagement.exception.*;
import organizationmanagement.model.Department;
import organizationmanagement.model.Team;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import organizationmanagement.service.TeamService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;
    private final UserServiceClient userServiceClient;
    private final SurveyServiceClient surveyServiceClient;

    @Override
    public List<Team> getAll() { return teamRepository.findAll(); }

    @Override
    public Team getById(UUID id) { return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id)); }

    @Override
    public void delete(UUID id) { if (!teamRepository.existsById(id)) { throw new ResourceNotFoundException("Team not found with id: " + id); } teamRepository.deleteById(id); }

    @Override
    public List<Team> getByDepartmentId(UUID departmentId) { return teamRepository.findByDepartmentId(departmentId); }

    @Override
    public Team createUnderDepartment(UUID deptId, Team team) {
        validateTeamName(team.getName());
        Department department = departmentRepository.findById(deptId).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + deptId));
        boolean exists = teamRepository.existsByNameAndDepartmentId(team.getName().trim(), deptId);
        if (exists) {
            throw new BadRequestException("A team with the name '" + team.getName().trim() + "' already exists in this department.");
        }
        team.setDepartment(department);
        return teamRepository.save(team);
    }

    @Override
    public Team update(UUID id, UUID departmentId, Team updatedTeam) {
        validateTeamName(updatedTeam.getName());
        Team existingTeam = teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
        boolean exists = teamRepository.existsByNameAndDepartmentId(updatedTeam.getName().trim(), departmentId);
        if (exists && !existingTeam.getName().equalsIgnoreCase(updatedTeam.getName().trim())) {
            throw new BadRequestException("A team with the name '" + updatedTeam.getName().trim() + "' already exists in this department.");
        }
        existingTeam.setName(updatedTeam.getName().trim());
        existingTeam.setDepartment(department);
        return teamRepository.save(existingTeam);
    }

    @Override
    public List<Team> getAllByOrganization(UUID organizationId) { return teamRepository.findByDepartmentOrganizationId(organizationId); }

    @Override
    public Team getByIdAndOrganization(UUID id, UUID organizationId) {
        return teamRepository.findByIdAndDepartmentOrganizationId(id, organizationId).orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id + " in organization: " + organizationId));
    }

    @Override
    public void deleteByIdAndOrganization(UUID id, UUID organizationId) {
        Team team = getByIdAndOrganization(id, organizationId);
        teamRepository.delete(team);
    }

    @Override
    public List<Team> getByDepartmentIdAndOrganization(UUID departmentId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId + " in organization: " + organizationId));
        return teamRepository.findByDepartmentId(departmentId);
    }

    @Override
    public Team createUnderDepartmentInOrganization(UUID deptId, Team team, UUID organizationId) {
        validateTeamName(team.getName());
        Department department = departmentRepository.findByIdAndOrganizationId(deptId, organizationId).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + deptId + " in organization: " + organizationId));
        boolean exists = teamRepository.existsByNameAndDepartmentId(team.getName().trim(), deptId);
        if (exists) {
            throw new BadRequestException("A team with the name '" + team.getName().trim() + "' already exists in this department.");
        }
        team.setDepartment(department);
        return teamRepository.save(team);
    }

    @Override
    public Team updateInOrganization(UUID id, UUID departmentId, Team updatedTeam, UUID organizationId) {
        validateTeamName(updatedTeam.getName());
        Team existingTeam = getByIdAndOrganization(id, organizationId);
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId).orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId + " in organization: " + organizationId));
        boolean exists = teamRepository.existsByNameAndDepartmentId(updatedTeam.getName().trim(), departmentId);
        if (exists && !existingTeam.getName().equalsIgnoreCase(updatedTeam.getName().trim())) {
            throw new BadRequestException("A team with the name '" + updatedTeam.getName().trim() + "' already exists in this department.");
        }
        existingTeam.setName(updatedTeam.getName().trim());
        existingTeam.setDepartment(department);
        return teamRepository.save(existingTeam);
    }

    private void validateTeamName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Team name must not be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new BadRequestException("Team name must be between 2 and 100 characters.");
        }
    }

    @Override
    @Transactional
    public void assignUserToTeamInOrganization(UUID teamId, UUID userId, UUID organizationId) {
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId).orElseThrow(() -> new ResourceNotFoundException("Team not found with id " + teamId + " in organization " + organizationId));
        ResponseEntity<Boolean> userExistsResponse = userServiceClient.userExists(userId);
        if (userExistsResponse.getBody() == null || !userExistsResponse.getBody()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        if (team.getUserIds().contains(userId)) {
            throw new BadRequestException("User is already assigned to this team");
        }
        team.getUserIds().add(userId);
        teamRepository.save(team);
    }

    @Override
    @Transactional
    public void removeUserFromTeamInOrganization(UUID teamId, UUID userId, UUID organizationId) {
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId).orElseThrow(() -> new ResourceNotFoundException("Team not found with id " + teamId + " in organization " + organizationId));
        if (!team.getUserIds().contains(userId)) {
            throw new BadRequestException("User is not assigned to this team");
        }
        team.getUserIds().remove(userId);
        teamRepository.save(team);
    }

    @Override
    @Transactional
    public void assignSurveyToTeamInOrganization(UUID teamId, UUID surveyId, UUID organizationId) {
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId).orElseThrow(() -> new ResourceNotFoundException("Team not found with id " + teamId + " in organization " + organizationId));
        ResponseEntity<Boolean> surveyExistsResponse = surveyServiceClient.surveyExists(surveyId);
        if (surveyExistsResponse.getBody() == null || !surveyExistsResponse.getBody()) {
            throw new ResourceNotFoundException("Survey not found with id: " + surveyId);
        }
        if (team.getSurveyIds().contains(surveyId)) {
            throw new BadRequestException("Survey is already assigned to this team");
        }
        team.getSurveyIds().add(surveyId);
        teamRepository.save(team);
    }

    @Override
    @Transactional
    public void removeSurveyFromTeamInOrganization(UUID teamId, UUID surveyId, UUID organizationId) {
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId).orElseThrow(() -> new ResourceNotFoundException("Team not found with id " + teamId + " in organization " + organizationId));
        if (!team.getSurveyIds().contains(surveyId)) {
            throw new BadRequestException("Survey is not assigned to this team");
        }
        team.getSurveyIds().remove(surveyId);
        teamRepository.save(team);
    }

    @Override
    public boolean existsById(UUID id) {
        return teamRepository.existsById(id);
    }

    @Override
    public Team findByUserId(UUID userId) {
        return teamRepository.findAll().stream()
            .filter(team -> team.getUserIds() != null && team.getUserIds().contains(userId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Team not found for userId: " + userId));
    }
} 