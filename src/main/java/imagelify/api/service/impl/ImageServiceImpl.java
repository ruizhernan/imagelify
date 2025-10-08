package imagelify.api.service.impl;

import imagelify.api.entity.Image;
import imagelify.api.entity.User;
import imagelify.api.repository.ImageRepository;
import imagelify.api.repository.UserRepository;
import imagelify.api.service.ContentModerationService;
import imagelify.api.service.ImageService;
import imagelify.api.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final StorageService storageService;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ContentModerationService contentModerationService;

    @Override
    public Image uploadImage(MultipartFile file, Long userId) {
        // 1. Validate image content before anything else
        contentModerationService.checkImageForInappropriateContent(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPlan() != null) {
            // Check if the user has reached the image limit
            if (user.getPlan().getMaxImages() != null) {
                long imageCount = imageRepository.countByUser(user);
                if (imageCount >= user.getPlan().getMaxImages()) {
                    throw new RuntimeException("Image upload limit reached for your plan.");
                }
            }

        }

        String fileUrl = storageService.uploadFile(file);

        Image image = Image.builder()
                .filename(file.getOriginalFilename())
                .s3Url(fileUrl)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadDate(LocalDateTime.now())
                .user(user)
                .build();

        return imageRepository.save(image);
    }

    @Override
    public List<Image> getImagesByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return imageRepository.findByUser(user);
    }
}

