package imagelify.api.repository;

import imagelify.api.entity.Image;
import imagelify.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByUser(User user);
    long countByUser(User user);
}
