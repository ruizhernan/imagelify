package imagelify.api.service.impl;

import imagelify.api.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
public class S3Service implements StorageService {

    private final S3Client s3Client;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.public-url}")
    private String minioPublicUrl;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Se ejecuta al iniciar la aplicación.
     * Verifica la existencia del bucket y, si es necesario, lo crea y establece la política de lectura pública.
     */
    @PostConstruct
    public void init() {
        boolean bucketExists = true;
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 Bucket '{}' already exists.", bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                bucketExists = false;
                try {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                    log.info("S3 Bucket '{}' created.", bucketName);
                } catch (S3Exception createException) {
                    log.error("Error creating S3 bucket: {}", createException.getMessage());
                    throw new RuntimeException("Could not create S3 bucket", createException);
                }
            } else {
                log.error("Error initializing S3 bucket: {}", e.getMessage());
                throw new RuntimeException("Could not initialize S3 bucket", e);
            }
        }

        // Always try to set the public policy to ensure it's correctly configured
        try {
            setPublicPolicy(bucketName);
        } catch (S3Exception e) {
            log.error("CRITICAL: Failed to set public read policy on bucket '{}'. Public URLs will not work.", bucketName);
            log.error("This is likely a permissions issue. The user/key needs the 's3:PutBucketPolicy' permission.");
            // We don't throw an exception here, to allow the app to start.
            // However, image URLs will be inaccessible.
        }
    }

    /**
     * Método auxiliar para aplicar la política JSON que permite el acceso de solo lectura al bucket.
     * Esto es el equivalente programático de 'mc policy set download'.
     */
    private void setPublicPolicy(String bucketName) {
        String publicReadPolicy = String.format("""
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": [
                    "s3:GetObject"
                  ],
                  "Resource": [
                    "arn:aws:s3:::%s/*"
                  ]
                }
              ]
            }
            """, bucketName);

        PutBucketPolicyRequest policyRequest = PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(publicReadPolicy)
                .build();

        s3Client.putBucketPolicy(policyRequest);
        log.info("S3 Bucket policy for '{}' set to Public Read.", bucketName);
    }

    /**
     * Sube el archivo al Object Storage.
     * @param file Archivo multipart a subir.
     * @return La URL pública del archivo subido.
     */
    @Override
    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
            return String.format("%s/%s/%s", minioPublicUrl, bucketName, encodedFileName);

        } catch (IOException | S3Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Error uploading file to S3-compatible storage: " + e.getMessage(), e);
        }
    }
}