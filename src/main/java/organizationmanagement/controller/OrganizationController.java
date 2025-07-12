package organizationmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.dto.TeamDTO;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.model.Organization;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.OrganizationService;
import organizationmanagement.service.TeamService;
import organizationmanagement.util.OrganizationContextUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import organizationmanagement.mapper.OrganizationMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final OrganizationContextUtil organizationContextUtil;

    // ===== ORGANIZATION ENDPOINTS =====
    @GetMapping
    @PreAuthorize("hasAuthority('SYS_ADMIN_ROOT')")
    public ResponseEntity<List<OrganizationDTO>> getAll() {
        List<OrganizationDTO> organizations = organizationService.getAll().stream()
                .map(OrganizationMapper::toDTO)
                .toList();
        return ResponseEntity.ok(organizations);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody Organization organization) {
        System.out.println("Organization Service - Received organization: " + organization);
        System.out.println("Organization Service - Organization name: " + (organization != null ? organization.getName() : "[null]"));
        return organizationService.create(organization);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity< Boolean> exists(@PathVariable UUID id) {
        boolean exists = organizationService.exists(id);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_READ', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<OrganizationDTO> getById(@PathVariable UUID id) {
        Organization organization;

        if (organizationContextUtil.isRootAdmin()) {
            organization = organizationService.getById(id);
        } else {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only access your own organization");
            }
            organization = organizationService.getById(id);
        }

        return ResponseEntity.ok(OrganizationMapper.toDTO(organization));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<OrganizationDTO> update(@PathVariable UUID id, @RequestBody Organization organization) {
        Organization updatedOrganization;

        if (organizationContextUtil.isRootAdmin()) {
            updatedOrganization = organizationService.update(id, organization);
        } else {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only update your own organization");
            }
            updatedOrganization = organizationService.update(id, organization);
        }

        return ResponseEntity.ok(OrganizationMapper.toDTO(updatedOrganization));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (organizationContextUtil.isRootAdmin()) {
            organizationService.delete(id);
        } else {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only delete your own organization");
            }
            organizationService.delete(id);
        }

        return ResponseEntity.noContent().build();
    }

    // ===== USER ASSIGNMENT ENDPOINTS =====
    @PostMapping("/{organizationId}/departments/{departmentId}/assign-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> assignUserToDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID departmentId,
            @PathVariable UUID userId) {

        verifyOrganizationAccess(organizationId);
        departmentService.assignUserToDepartmentInOrganization(departmentId, userId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("User %s successfully assigned to department %s in organization %s",
                        userId, departmentId, organizationId)
        ));
    }

    @PostMapping("/{organizationId}/teams/{teamId}/assign-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> assignUserToTeam(
            @PathVariable UUID organizationId,
            @PathVariable UUID teamId,
            @PathVariable UUID userId) {

        verifyOrganizationAccess(organizationId);
        teamService.assignUserToTeamInOrganization(teamId, userId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("User %s successfully assigned to team %s in organization %s",
                        userId, teamId, organizationId)
        ));
    }

    @DeleteMapping("/{organizationId}/departments/{departmentId}/remove-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> removeUserFromDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID departmentId,
            @PathVariable UUID userId) {

        verifyOrganizationAccess(organizationId);
        departmentService.removeUserFromDepartmentInOrganization(departmentId, userId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("User %s successfully removed from department %s in organization %s",
                        userId, departmentId, organizationId)
        ));
    }

    @DeleteMapping("/{organizationId}/teams/{teamId}/remove-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> removeUserFromTeam(
            @PathVariable UUID organizationId,
            @PathVariable UUID teamId,
            @PathVariable UUID userId) {

        verifyOrganizationAccess(organizationId);
        teamService.removeUserFromTeamInOrganization(teamId, userId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("User %s successfully removed from team %s in organization %s",
                        userId, teamId, organizationId)
        ));
    }

    // ===== SURVEY ASSIGNMENT ENDPOINTS =====
    @PostMapping("/{organizationId}/departments/{departmentId}/assign-survey/{surveyId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> assignSurveyToDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID departmentId,
            @PathVariable UUID surveyId) {

        verifyOrganizationAccess(organizationId);
        departmentService.assignSurveyToDepartmentInOrganization(departmentId, surveyId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("Survey %s successfully assigned to department %s in organization %s",
                        surveyId, departmentId, organizationId)
        ));
    }

    @PostMapping("/{organizationId}/teams/{teamId}/assign-survey/{surveyId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> assignSurveyToTeam(
            @PathVariable UUID organizationId,
            @PathVariable UUID teamId,
            @PathVariable UUID surveyId) {

        verifyOrganizationAccess(organizationId);
        teamService.assignSurveyToTeamInOrganization(teamId, surveyId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("Survey %s successfully assigned to team %s in organization %s",
                        surveyId, teamId, organizationId)
        ));
    }

    @DeleteMapping("/{organizationId}/departments/{departmentId}/remove-survey/{surveyId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> removeSurveyFromDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID departmentId,
            @PathVariable UUID surveyId) {

        verifyOrganizationAccess(organizationId);
        departmentService.removeSurveyFromDepartmentInOrganization(departmentId, surveyId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("Survey %s successfully removed from department %s in organization %s",
                        surveyId, departmentId, organizationId)
        ));
    }

    @DeleteMapping("/{organizationId}/teams/{teamId}/remove-survey/{surveyId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Map<String, String>> removeSurveyFromTeam(
            @PathVariable UUID organizationId,
            @PathVariable UUID teamId,
            @PathVariable UUID surveyId) {

        verifyOrganizationAccess(organizationId);
        teamService.removeSurveyFromTeamInOrganization(teamId, surveyId, organizationId);

        return ResponseEntity.ok(Collections.singletonMap(
                "message",
                String.format("Survey %s successfully removed from team %s in organization %s",
                        surveyId, teamId, organizationId)
        ));
    }
    // ===== HIERARCHY ENDPOINTS =====
    @GetMapping("/{id}/children")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN_ROOT')")
    public ResponseEntity<ChildrenResponse> getChildren(@PathVariable UUID id) {
        if (!organizationContextUtil.isRootAdmin()) {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only access children of your own organization");
            }
        }

        Organization org = organizationService.getById(id);
        OrganizationDTO orgDTO = OrganizationMapper.toDTO(org);

        List<DepartmentDTO> departments = departmentService.getByOrganizationId(id).stream()
                .map(dept -> {
                    DepartmentDTO dto = new DepartmentDTO();
                    dto.setId(dept.getId());
                    dto.setName(dept.getName());
                    dto.setOrganization(orgDTO);
                    return dto;
                })
                .collect(Collectors.toList());

        List<TeamDTO> teams = departments.stream()
                .flatMap(deptDTO -> teamService.getByDepartmentId(deptDTO.getId()).stream()
                        .map(team -> {
                            TeamDTO teamDTO = new TeamDTO();
                            teamDTO.setId(team.getId());
                            teamDTO.setName(team.getName());
                            teamDTO.setDepartment(deptDTO);
                            return teamDTO;
                        }))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ChildrenResponse(departments, teams));
    }

    // ===== HELPER METHODS =====
    private void verifyOrganizationAccess(UUID organizationId) {
        if (!organizationService.exists(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }

        if (!organizationContextUtil.isRootAdmin()) {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!organizationId.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only manage resources in your own organization");
            }
        }
    }

    // ===== RESPONSE CLASSES =====
    public static class ChildrenResponse {
        private List<DepartmentDTO> departments;
        private List<TeamDTO> teams;

        public ChildrenResponse(List<DepartmentDTO> departments, List<TeamDTO> teams) {
            this.departments = departments;
            this.teams = teams;
        }

        // Getters and setters
        public List<DepartmentDTO> getDepartments() { return departments; }
        public void setDepartments(List<DepartmentDTO> departments) { this.departments = departments; }
        public List<TeamDTO> getTeams() { return teams; }
        public void setTeams(List<TeamDTO> teams) { this.teams = teams; }
    }
}