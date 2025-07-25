package organizationmanagement.controller;

import organizationmanagement.dto.DepartmentCreateDTO;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.dto.UserDTO;
import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.OrganizationService;
import organizationmanagement.util.OrganizationContextUtil;
import organizationmanagement.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;
    private final OrganizationService organizationService;
    private final OrganizationContextUtil organizationContextUtil;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<List<DepartmentDTO>> getAll() {
        List<DepartmentDTO> departments;

        if (organizationContextUtil.isRootAdmin()) {
            departments = service.getAll().stream()
                    .map(DepartmentMapper::toDTO)
                    .collect(Collectors.toList());
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            departments = service.getAllByOrganization(organizationId).stream()
                    .map(DepartmentMapper::toDTO)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(departments);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_CREATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<DepartmentDTO> create(@RequestBody DepartmentCreateDTO deptDto) {
        DepartmentDTO createdDepartment;

        if (organizationContextUtil.isRootAdmin()) {
            if (deptDto.getOrganizationId() == null) {
                throw new BadRequestException("Organization ID is required for sys admin department creation");
            }
            Organization org = organizationService.getById(deptDto.getOrganizationId());
            Department deptEntity = DepartmentMapper.toEntity(deptDto, org);
            Department saved = service.createUnderOrganization(deptDto.getOrganizationId(), deptEntity);
            createdDepartment = DepartmentMapper.toDTO(saved);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Organization org = organizationService.getById(organizationId);
            Department deptEntity = DepartmentMapper.toEntity(deptDto, org);
            Department saved = service.createUnderOrganization(organizationId, deptEntity);
            createdDepartment = DepartmentMapper.toDTO(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepartment);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<DepartmentDTO> getById(@PathVariable UUID id) {
        DepartmentDTO department;

        if (organizationContextUtil.isRootAdmin()) {
            Department dept = service.getById(id);
            if (dept == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }
            department = DepartmentMapper.toDTO(dept);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Department dept = service.getByIdAndOrganization(id, organizationId);
            if (dept == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }
            department = DepartmentMapper.toDTO(dept);
        }

        return ResponseEntity.ok(department);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<DepartmentDTO> update(@PathVariable UUID id, @RequestBody DepartmentCreateDTO deptDto) {
        DepartmentDTO updatedDepartment;

        if (organizationContextUtil.isRootAdmin()) {
            if (deptDto.getOrganizationId() == null) {
                throw new BadRequestException("Organization ID is required for sys admin department update");
            }

            Department existing = service.getById(id);
            if (existing == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }

            existing.setName(deptDto.getName());
            Organization org = organizationService.getById(deptDto.getOrganizationId());
            if (org == null) {
                throw new BadRequestException("Organization not found with ID: " + deptDto.getOrganizationId());
            }
            existing.setOrganization(org);

            Department updated = service.update(existing);
            updatedDepartment = DepartmentMapper.toDTO(updated);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Department existing = service.getByIdAndOrganization(id, organizationId);
            if (existing == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }

            existing.setName(deptDto.getName());

            Department updated = service.update(existing);
            updatedDepartment = DepartmentMapper.toDTO(updated);
        }

        return ResponseEntity.ok(updatedDepartment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_DELETE','SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (organizationContextUtil.isRootAdmin()) {
            service.delete(id);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            service.deleteByIdAndOrganization(id, organizationId);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable UUID id) {
        boolean exists = service.getDepartmentRepository().existsById(id);
        return ResponseEntity.ok().body(exists);
    }

    @PostMapping("/{departmentId}/assign-user/{userId}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<String> assignUserToDepartment(@PathVariable UUID departmentId, @PathVariable UUID userId) {
        if (organizationContextUtil.isRootAdmin()) {
            Department department = service.getById(departmentId);
            if (department == null) {
                throw new BadRequestException("Department not found with ID: " + departmentId);
            }
            service.assignUserToDepartmentInOrganization(departmentId, userId, department.getOrganization().getId());
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            service.assignUserToDepartmentInOrganization(departmentId, userId, organizationId);
        }
        return ResponseEntity.ok("User assigned to department successfully");
    }

    @PostMapping("/{departmentId}/remove-user/{userId}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<String> removeUserFromDepartment(@PathVariable UUID departmentId, @PathVariable UUID userId) {
        if (organizationContextUtil.isRootAdmin()) {
            Department department = service.getById(departmentId);
            if (department == null) {
                throw new BadRequestException("Department not found with ID: " + departmentId);
            }
            service.removeUserFromDepartmentInOrganization(departmentId, userId, department.getOrganization().getId());
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            service.removeUserFromDepartmentInOrganization(departmentId, userId, organizationId);
        }
        return ResponseEntity.ok("User removed from department successfully");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UUID> getDepartmentIdByUserId(@PathVariable UUID userId) {
        try {
            Department department = service.findByUserId(userId);
            return ResponseEntity.ok(department.getId());
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{departmentId}/users")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<List<UserDTO>> getUsersByDepartment(@PathVariable UUID departmentId) {
        UUID orgId = organizationContextUtil.isRootAdmin() ? 
            service.getById(departmentId).getOrganization().getId() : 
            organizationContextUtil.getCurrentOrganizationId();
        List<UserDTO> users = service.getUsersForDepartment(departmentId, orgId);
        return ResponseEntity.ok(users);
    }
}