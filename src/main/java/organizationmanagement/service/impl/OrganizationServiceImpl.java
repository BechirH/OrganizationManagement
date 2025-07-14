package organizationmanagement.service.impl;

import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.model.Organization;
import organizationmanagement.repository.OrganizationRepository;
import organizationmanagement.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import organizationmanagement.dto.UserDTO;
import organizationmanagement.client.UserServiceClient;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserServiceClient userServiceClient;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 100;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-']+$");
    // ... (all method implementations from the old OrganizationService)

    @Override
    public List<Organization> getAll() { return organizationRepository.findAll(); }

    @Override
    public Organization create(Organization org) {
        validateOrganization(org);
        String normalizedName = org.getName().trim();
        boolean exists = organizationRepository.findAll().stream().anyMatch(existingOrg -> existingOrg.getName() != null && existingOrg.getName().trim().equalsIgnoreCase(normalizedName));
        if (exists) {
            throw new BadRequestException("An organization with the name '" + org.getName().trim() + "' already exists.");
        }
        return organizationRepository.save(org);
    }

    @Override
    public boolean exists(UUID id) { return organizationRepository.existsById(id); }

    @Override
    public Organization getById(UUID id) {
        return organizationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    @Override
    public Organization update(UUID id, Organization updatedOrg) {
        validateOrganization(updatedOrg);
        Organization existing = getById(id);
        existing.setName(updatedOrg.getName().trim());
        return organizationRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Organization not found with id: " + id);
        }
        organizationRepository.deleteById(id);
    }

    @Override
    public List<UserDTO> getUsersByOrganizationId(UUID organizationId) {
        return userServiceClient.getUsersByOrganizationId(organizationId).getBody();
    }

    private void validateOrganization(Organization org) {
        if (org.getName() == null || org.getName().trim().isEmpty()) {
            throw new BadRequestException("Organization name must not be empty.");
        }
        String trimmedName = org.getName().trim();
        if (trimmedName.length() < NAME_MIN_LENGTH || trimmedName.length() > NAME_MAX_LENGTH) {
            throw new BadRequestException("Organization name must be between " + NAME_MIN_LENGTH + " and " + NAME_MAX_LENGTH + " characters.");
        }
        if (!NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new BadRequestException("Organization name contains invalid characters. Allowed: letters, numbers, spaces, hyphens, apostrophes.");
        }
    }
} 