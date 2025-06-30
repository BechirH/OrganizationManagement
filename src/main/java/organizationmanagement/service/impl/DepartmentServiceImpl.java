package organizationmanagement.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import organizationmanagement.client.SurveyServiceClient;
import organizationmanagement.client.UserServiceClient;
import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.exception.ServiceUnavailableException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.OrganizationRepository;
import organizationmanagement.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserServiceClient userServiceClient;
    private final SurveyServiceClient surveyServiceClient;

    @Override
    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    @Override
    public Department getById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));
    }

    @Override
    public void delete(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id " + id);
        }
        departmentRepository.deleteById(id);
    }

    @Override
    public List<Department> getByOrganizationId(UUID organizationId) {
        return departmentRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<Department> getAllByOrganization(UUID organizationId) {
        return departmentRepository.findByOrganizationId(organizationId);
    }

    @Override
    public Department getByIdAndOrganization(UUID id, UUID organizationId) {
        return departmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + id + " in organization " + organizationId));
    }

    @Override
    public void deleteByIdAndOrganization(UUID id, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + id + " in organization " + organizationId));
        departmentRepository.delete(department);
    }

    @Override
    public Department createUnderOrganization(UUID orgId, Department dept) {
        validateDepartmentName(dept.getName());
        
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id " + orgId));
        
        boolean exists = departmentRepository.existsByNameAndOrganizationId(dept.getName().trim(), orgId);
        if (exists) {
            throw new BadRequestException(
                    "A department with the name '" + dept.getName().trim() + "' already exists in this organization.");
        }
        
        dept.setOrganization(org);
        return departmentRepository.save(dept);
    }

    @Override
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

    @Override
    public void assignUserToDepartmentInOrganization(UUID departmentId, UUID userId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));
        
        ResponseEntity<Boolean> userExistsResponse = userServiceClient.userExists(userId);
        if (userExistsResponse.getBody() == null || !userExistsResponse.getBody()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        if (department.getUserIds().contains(userId)) {
            throw new BadRequestException("User is already assigned to this department");
        }
        
        department.getUserIds().add(userId);
        departmentRepository.save(department);
    }

    @Override
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

    @Override
    @Transactional
    public void assignSurveyToDepartmentInOrganization(UUID departmentId, UUID surveyId, UUID organizationId) {
        try {
            Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id " + departmentId + " in organization " + organizationId));
            try {
                ResponseEntity<Boolean> surveyExistsResponse = surveyServiceClient.surveyExists(surveyId);
                if (!surveyExistsResponse.getStatusCode().is2xxSuccessful()) {
                    throw new ResourceNotFoundException("Survey service unavailable or survey not found");
                }
                if (surveyExistsResponse.getBody() == null || !surveyExistsResponse.getBody()) {
                    throw new ResourceNotFoundException("Survey not found with id: " + surveyId);
                }
            } catch (FeignException.NotFound e) {
                throw new ResourceNotFoundException("Survey not found with id: " + surveyId);
            } catch (FeignException e) {
                throw new ServiceUnavailableException("Survey service unavailable: " + e.getMessage());
            }
            if (department.getSurveyIds().contains(surveyId)) {
                throw new BadRequestException("Survey is already assigned to this department");
            }
            department.getSurveyIds().add(surveyId);
            departmentRepository.save(department);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException("Failed to assign survey: " + e.getMessage());
        }
    }

    @Override
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

    @Override
    public DepartmentRepository getDepartmentRepository() {
        return departmentRepository;
    }

    @Override
    public Department findByUserId(UUID userId) {
        return departmentRepository.findAll().stream()
                .filter(dept -> dept.getUserIds() != null && dept.getUserIds().contains(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Department not found for userId: " + userId));
    }
} 