package organizationmanagement.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import organizationmanagement.client.SurveyServiceClient;
import organizationmanagement.client.UserServiceClient;
import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.exception.ServiceUnavailableException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.OrganizationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserServiceClient userServiceClient;
    private final SurveyServiceClient surveyServiceClient;
    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    public Department getById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));
    }

    public void delete(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id " + id);
        }
        departmentRepository.deleteById(id);
    }

    public List<Department> getByOrganizationId(UUID organizationId) {
        return departmentRepository.findByOrganizationId(organizationId);
    }

    // New method for organization-scoped access
    public List<Department> getAllByOrganization(UUID organizationId) {
        return departmentRepository.findByOrganizationId(organizationId);
    }

    // New method for organization-scoped access
    public Department getByIdAndOrganization(UUID id, UUID organizationId) {
        return departmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id + " in organization " + organizationId));
    }

    // New method for organization-scoped deletion
    public void deleteByIdAndOrganization(UUID id, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id + " in organization " + organizationId));

        departmentRepository.delete(department);
    }

    public Department createUnderOrganization(UUID orgId, Department dept) {
        validateDepartmentName(dept.getName());

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id " + orgId));

        boolean exists = departmentRepository.existsByNameAndOrganizationId(dept.getName().trim(), orgId);
        if (exists) {
            throw new BadRequestException("A department with the name '" + dept.getName().trim() + "' already exists in this organization.");
        }

        dept.setOrganization(org);
        return departmentRepository.save(dept);
    }

    public Department update(Department dept) {
        validateDepartmentName(dept.getName());

        if (dept.getId() == null || !departmentRepository.existsById(dept.getId())) {
            throw new ResourceNotFoundException("Cannot update department. Department not found with id " + dept.getId());
        }

        return departmentRepository.save(dept);
    }

    private void validateDepartmentName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Department name must not be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new BadRequestException("Department name must be between 2 and 100 characters.");
        }
    }

    // Organization-scoped versions of assignment methods

    public void assignUserToDepartmentInOrganization(UUID departmentId, UUID userId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));

        // Verify user exists using Feign client
        ResponseEntity<Boolean> userExistsResponse = userServiceClient.userExists(userId);
        if (userExistsResponse.getBody() == null || !userExistsResponse.getBody()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Check if user is already assigned
        if (department.getUserIds().contains(userId)) {
            throw new BadRequestException("User is already assigned to this department");
        }

        department.getUserIds().add(userId);
        departmentRepository.save(department);
    }

    public void removeUserFromDepartmentInOrganization(UUID departmentId, UUID userId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));

        if (!department.getUserIds().contains(userId)) {
            throw new BadRequestException("User is not assigned to this department");
        }

        department.getUserIds().remove(userId);
        departmentRepository.save(department);
    }
    @Transactional
    public void assignSurveyToDepartmentInOrganization(UUID departmentId, UUID surveyId, UUID organizationId) {
        try {
            log.debug("Attempting to assign survey {} to department {} in organization {}",
                    surveyId, departmentId, organizationId);

            // 1. Verify department exists
            Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                    .orElseThrow(() -> {
                        log.error("Department not found: {} in organization {}", departmentId, organizationId);
                        return new ResourceNotFoundException(
                                "Department not found with id " + departmentId + " in organization " + organizationId);
                    });

            // 2. Verify survey exists - with improved error handling
            try {
                ResponseEntity<Boolean> surveyExistsResponse = surveyServiceClient.surveyExists(surveyId);

                // Handle non-2xx responses
                if (!surveyExistsResponse.getStatusCode().is2xxSuccessful()) {
                    log.error("Survey service returned status: {}", surveyExistsResponse.getStatusCode());
                    throw new ResourceNotFoundException("Survey service unavailable or survey not found");
                }

                // Handle null or false response
                if (surveyExistsResponse.getBody() == null || !surveyExistsResponse.getBody()) {
                    log.warn("Survey not found: {}", surveyId);
                    throw new ResourceNotFoundException("Survey not found with id: " + surveyId);
                }
            } catch (FeignException.NotFound e) {
                log.warn("Survey not found (Feign 404): {}", surveyId);
                throw new ResourceNotFoundException("Survey not found with id: " + surveyId);
            } catch (FeignException e) {
                log.error("Feign communication error: {}", e.getMessage());
                throw new ServiceUnavailableException("Survey service unavailable: " + e.getMessage());
            }

            // 3. Check for duplicate assignment
            if (department.getSurveyIds().contains(surveyId)) {
                log.warn("Survey already assigned: {} to department {}", surveyId, departmentId);
                throw new BadRequestException("Survey is already assigned to this department");
            }

            // 4. Perform assignment
            department.getSurveyIds().add(surveyId);
            departmentRepository.save(department);
            log.info("Survey {} successfully assigned to department {}", surveyId, departmentId);

        } catch (ResourceNotFoundException | BadRequestException e) {
            // Re-throw expected exceptions with proper status codes
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error assigning survey: {}", e.getMessage(), e);
            throw new ServiceUnavailableException("Failed to assign survey: " + e.getMessage());
        }
    }

    // Optional: Verify survey belongs to organization if needed
    /*
    ResponseEntity<Boolean> surveyInOrgResponse = surveyServiceClient.surveyExistsInOrganization(surveyId, organizationId);
    if (surveyInOrgResponse.getBody() == null || !surveyInOrgResponse.getBody()) {
        throw new BadRequestException("Survey does not belong to this organization");
    }
    */

    public void removeSurveyFromDepartmentInOrganization(UUID departmentId, UUID surveyId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));

        if (!department.getSurveyIds().contains(surveyId)) {
            throw new BadRequestException("Survey is not assigned to this department");
        }

        department.getSurveyIds().remove(surveyId);
        departmentRepository.save(department);
    }

    public DepartmentRepository getDepartmentRepository() {
        return departmentRepository;
    }
}