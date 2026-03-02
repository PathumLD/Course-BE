package com.courseupload.service;

import com.courseupload.exception.FileStorageException;
import com.courseupload.exception.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

/**
 * AWS S3 storage implementation (Bonus Task).
 * Active when app.storage.strategy=s3.
 * Set credentials and bucket in application.properties.
 */
@Service("s3StorageService")
public class S3StorageService implements StorageService {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    private S3Client s3Client;

    private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "application/pdf", "video/mp4", "image/jpeg", "image/png"
    );

    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
            ".pdf", ".mp4", ".jpg", ".jpeg", ".png"
    );

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Override
    public String store(MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String s3Key = "uploads/" + UUID.randomUUID() + fileExtension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return s3Key;

        } catch (IOException e) {
            throw new FileStorageException("Failed to upload file to S3: " + originalFilename, e);
        } catch (S3Exception e) {
            throw new FileStorageException("S3 upload error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public Resource load(String fileIdentifier) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileIdentifier)
                    .build();

            return new InputStreamResource(s3Client.getObject(getRequest));
        } catch (S3Exception e) {
            throw new FileStorageException("Could not retrieve file from S3: " + fileIdentifier, e);
        }
    }

    @Override
    public void delete(String fileIdentifier) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileIdentifier)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            throw new FileStorageException("Could not delete file from S3: " + fileIdentifier, e);
        }
    }

    @Override
    public String buildFileUrl(String fileIdentifier) {
        // Returns the public S3 object URL
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileIdentifier);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Cannot upload an empty file.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                    "File type not allowed. Accepted types: PDF, MP4, JPG, PNG. Got: " + contentType);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new InvalidFileException("File name cannot be null.");
        }
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                    "File extension not allowed. Accepted: .pdf, .mp4, .jpg, .jpeg, .png");
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex != -1) ? filename.substring(dotIndex) : "";
    }
}
