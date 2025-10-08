package imagelify.api.service;

import org.springframework.web.multipart.MultipartFile;

public interface ContentModerationService {
    void checkImageForInappropriateContent(MultipartFile file);
}
