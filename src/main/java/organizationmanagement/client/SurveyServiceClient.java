package organizationmanagement.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "survey-service",
        url = "${survey-service.url}",
        configuration = organizationmanagement.config.FeignConfig.class
)
public interface SurveyServiceClient {
    @GetMapping("/{surveyId}/exists")
    ResponseEntity<Boolean> surveyExists(@PathVariable("surveyId") UUID surveyId);
}

