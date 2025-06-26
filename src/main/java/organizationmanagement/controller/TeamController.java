package organizationmanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.TeamCreateDTO;
import organizationmanagement.dto.TeamDTO;
import organizationmanagement.model.Department;
import organizationmanagement.model.Team;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.TeamService;
import organizationmanagement.util.OrganizationContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import organizationmanagement.mapper.TeamMapper;
import organizationmanagement.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final DepartmentService departmentService;
    private final OrganizationContextUtil organizationContextUtil;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEAM_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<List<TeamDTO>> getAll() {
        List<TeamDTO> teams;

        if (organizationContextUtil.isRootAdmin()) {
            teams = teamService.getAll().stream()
                    .map(TeamMapper::toDTO)
                    .toList();
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teams = teamService.getAllByOrganization(organizationId).stream()
                    .map(TeamMapper::toDTO)
                    .toList();
        }

        return ResponseEntity.ok(teams);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEAM_CREATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<TeamDTO> create(@RequestBody TeamCreateDTO teamDto) {
        if (teamDto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID must be provided to create a team.");
        }

        TeamDTO createdTeam;
        Team teamEntity = convertToEntity(teamDto);

        if (organizationContextUtil.isRootAdmin()) {
            // Root admin can create teams in any organization
            // The organization context should be derived from the department
            Team saved = teamService.createUnderDepartment(teamDto.getDepartmentId(), teamEntity);
            createdTeam = TeamMapper.toDTO(saved);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            // Verify department belongs to the current organization
            Team saved = teamService.createUnderDepartmentInOrganization(
                    teamDto.getDepartmentId(), teamEntity, organizationId);
            createdTeam = TeamMapper.toDTO(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdTeam);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TEAM_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<TeamDTO> getById(@PathVariable UUID id) {
        TeamDTO team;

        if (organizationContextUtil.isRootAdmin()) {
            Team teamEntity = teamService.getById(id);
            team = TeamMapper.toDTO(teamEntity);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Team teamEntity = teamService.getByIdAndOrganization(id, organizationId);
            team = TeamMapper.toDTO(teamEntity);
        }

        return ResponseEntity.ok(team);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TEAM_DELETE','SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (organizationContextUtil.isRootAdmin()) {
            teamService.delete(id);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teamService.deleteByIdAndOrganization(id, organizationId);
        }

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TEAM_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<TeamDTO> update(@PathVariable UUID id, @RequestBody TeamCreateDTO teamDto) {
        if (teamDto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID must be provided to update a team.");
        }

        TeamDTO updatedTeam;
        Team updatedTeamEntity = convertToEntity(teamDto);

        if (organizationContextUtil.isRootAdmin()) {
            Team updated = teamService.update(id, teamDto.getDepartmentId(), updatedTeamEntity);
            updatedTeam = TeamMapper.toDTO(updated);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Team updated = teamService.updateInOrganization(
                    id, teamDto.getDepartmentId(), updatedTeamEntity, organizationId);
            updatedTeam = TeamMapper.toDTO(updated);
        }

        return ResponseEntity.ok(updatedTeam);
    }

    // Additional endpoint to get teams by department within organization scope
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyAuthority('TEAM_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<List<TeamDTO>> getTeamsByDepartment(@PathVariable UUID departmentId) {
        List<TeamDTO> teams;

        if (organizationContextUtil.isRootAdmin()) {
            teams = teamService.getByDepartmentId(departmentId).stream()
                    .map(TeamMapper::toDTO)
                    .toList();
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teams = teamService.getByDepartmentIdAndOrganization(departmentId, organizationId).stream()
                    .map(TeamMapper::toDTO)
                    .toList();
        }

        return ResponseEntity.ok(teams);
    }

    @PostMapping("/{teamId}/assign-user/{userId}")
    @PreAuthorize("hasAnyAuthority('TEAM_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<String> assignUserToTeam(@PathVariable UUID teamId, @PathVariable UUID userId) {
        if (organizationContextUtil.isRootAdmin()) {
            Team team = teamService.getById(teamId);
            if (team == null) {
                throw new IllegalArgumentException("Team not found with ID: " + teamId);
            }
            teamService.assignUserToTeamInOrganization(teamId, userId, team.getDepartment().getOrganization().getId());
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teamService.assignUserToTeamInOrganization(teamId, userId, organizationId);
        }
        return ResponseEntity.ok("User assigned to team successfully");
    }

    @PostMapping("/{teamId}/remove-user/{userId}")
    @PreAuthorize("hasAnyAuthority('TEAM_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<String> removeUserFromTeam(@PathVariable UUID teamId, @PathVariable UUID userId) {
        if (organizationContextUtil.isRootAdmin()) {
            Team team = teamService.getById(teamId);
            if (team == null) {
                throw new IllegalArgumentException("Team not found with ID: " + teamId);
            }
            teamService.removeUserFromTeamInOrganization(teamId, userId, team.getDepartment().getOrganization().getId());
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teamService.removeUserFromTeamInOrganization(teamId, userId, organizationId);
        }
        return ResponseEntity.ok("User removed from team successfully");
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable UUID id) {
        boolean exists = teamService.existsById(id);
        return ResponseEntity.ok().body(exists);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UUID> getTeamIdByUserId(@PathVariable UUID userId) {
        try {
            Team team = teamService.findByUserId(userId);
            return ResponseEntity.ok(team.getId());
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Mapping methods

    private Team convertToEntity(TeamCreateDTO dto) {
        Team team = new Team();
        team.setName(dto.getName());
        // Note: Department will be set in the service layer to ensure organization scope
        return team;
    }
}