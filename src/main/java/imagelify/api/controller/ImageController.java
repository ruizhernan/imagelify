package imagelify.api.controller;

import imagelify.api.entity.Image;
import imagelify.api.entity.User;
import imagelify.api.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<Image> uploadImage(@RequestParam("file") MultipartFile file,
                                             @AuthenticationPrincipal User user) {
        Image image = imageService.uploadImage(file, user.getId());
        return ResponseEntity.ok(image);
    }

    @GetMapping
    public ResponseEntity<List<Image>> getUserImages(@AuthenticationPrincipal User user) {
        List<Image> images = imageService.getImagesByUserId(user.getId());
        return ResponseEntity.ok(images);
    }
}
