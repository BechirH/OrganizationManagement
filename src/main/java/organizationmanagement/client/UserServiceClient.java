package organizationmanagement.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.UUID;
import organizationmanagement.dto.UserDTO;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}/exists")
    ResponseEntity<Boolean> userExists(@PathVariable("userId") UUID userId);

    @GetMapping("/api/users/bulk")
    ResponseEntity<List<UserDTO>> getUsersBulk(@RequestParam("ids") String ids);

    @GetMapping("/api/users")
    ResponseEntity<List<UserDTO>> getUsersByOrganizationId(@RequestParam("organizationId") UUID organizationId);
}