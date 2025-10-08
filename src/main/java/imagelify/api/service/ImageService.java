package imagelify.api.service;

import imagelify.api.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {
    Image uploadImage(MultipartFile file, Long userId);
    List<Image> getImagesByUserId(Long userId);
}
