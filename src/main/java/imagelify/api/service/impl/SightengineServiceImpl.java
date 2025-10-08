package imagelify.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import imagelify.api.service.ContentModerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class SightengineServiceImpl implements ContentModerationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sightengine.api-user}")
    private String apiUser;

    @Value("${sightengine.api-secret}")
    private String apiSecret;

    private static final String SIGHTENGINE_API_URL = "https://api.sightengine.com/1.0/check.json";

    public SightengineServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void checkImageForInappropriateContent(MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("models", "nudity-2.1,weapon,recreational_drug,offensive-2.0,gore-2.0,violence,self-harm");
        body.add("api_user", apiUser);
        body.add("api_secret", apiSecret);

        try {
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("media", fileAsResource);
        } catch (IOException e) {
            log.error("Could not read file for moderation", e);
            throw new RuntimeException("Could not read file for moderation.", e);
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String responseJson = restTemplate.postForObject(SIGHTENGINE_API_URL, requestEntity, String.class);

        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
            validateResponse(responseMap);
        } catch (IOException e) {
            log.error("Could not parse Sightengine response", e);
            throw new RuntimeException("Could not parse Sightengine response.", e);
        }
    }

    private void validateResponse(Map<String, Object> responseMap) {
        if (!"success".equals(responseMap.get("status"))) {
            log.warn("Sightengine API call was not successful. Response: {}", responseMap);
            // We can choose to fail open (allow upload) or fail closed (block upload)
            // For safety, we'll fail closed if the check doesn't succeed.
            throw new RuntimeException("Content moderation check failed.");
        }

        // Check various moderation categories
        if (isContentInappropriate(responseMap, "weapon") ||
            isContentInappropriate(responseMap, "violence") ||
            isContentInappropriate(responseMap, "self-harm") ||
            isContentInappropriate(responseMap, "gore") ||
            isContentInappropriate(responseMap, "offensive") ||
            isContentInappropriate(responseMap, "nudity")) {
            throw new RuntimeException("Image contains inappropriate content and cannot be uploaded.");
        }
    }

    private boolean isContentInappropriate(Map<String, Object> responseMap, String category) {
        Object categoryResponse = responseMap.get(category);
        if (categoryResponse instanceof Map) {
            // For models like nudity that have sub-fields
            double rawScore = (Double) ((Map<?, ?>) categoryResponse).get("raw");
            return rawScore > 0.5;
        } else if (categoryResponse instanceof Double) {
            // For models like weapon, violence that return a single probability
            return (Double) categoryResponse > 0.5;
        }
        return false;
    }
}
