package organizationmanagement.service;

import organizationmanagement.model.Organization;
import java.util.List;
import java.util.UUID;

public interface OrganizationService {
    List<Organization> getAll();
    Organization create(Organization org);
    boolean exists(UUID id);
    Organization getById(UUID id);
    Organization update(UUID id, Organization updatedOrg);
    void delete(UUID id);
}