package organizationmanagement.service;

import organizationmanagement.model.Department;
import organizationmanagement.repository.DepartmentRepository;
import java.util.List;
import java.util.UUID;

public interface DepartmentService {
    List<Department> getAll();
    Department getById(UUID id);
    void delete(UUID id);
    List<Department> getByOrganizationId(UUID organizationId);
    List<Department> getAllByOrganization(UUID organizationId);
    Department getByIdAndOrganization(UUID id, UUID organizationId);
    void deleteByIdAndOrganization(UUID id, UUID organizationId);
    Department createUnderOrganization(UUID orgId, Department dept);
    Department update(Department dept);
    void assignUserToDepartmentInOrganization(UUID departmentId, UUID userId, UUID organizationId);
    void removeUserFromDepartmentInOrganization(UUID departmentId, UUID userId, UUID organizationId);
    void assignSurveyToDepartmentInOrganization(UUID departmentId, UUID surveyId, UUID organizationId);
    void removeSurveyFromDepartmentInOrganization(UUID departmentId, UUID surveyId, UUID organizationId);
    DepartmentRepository getDepartmentRepository();
}