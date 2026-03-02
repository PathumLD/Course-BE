package com.courseupload.service;

import com.courseupload.exception.FileStorageException;
import com.courseupload.exception.InvalidFileException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.max-file-size}")
    private long maxFileSize;

    private Path fileStoragePath;

    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "video/mp4", "image/jpeg", "image/png");

    static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".mp4", ".jpg", ".jpeg", ".png");

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStoragePath);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + e.getMessage());
        }
    }

    @Override
    public String store(MultipartFile file) {
        validateFile(file);
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = getFileExtension(originalFilename);
        String storedName = UUID.randomUUID() + ext;
        try {
            Files.copy(file.getInputStream(), fileStoragePath.resolve(storedName),
                    StandardCopyOption.REPLACE_EXISTING);
            return storedName;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }

    @Override
    public Resource load(String fileIdentifier) {
        try {
            Path filePath = fileStoragePath.resolve(fileIdentifier).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) return resource;
            throw new FileStorageException("File not found: " + fileIdentifier);
        } catch (MalformedURLException e) {
            throw new FileStorageException("File not found: " + fileIdentifier, e);
        }
    }

    @Override
    public void delete(String fileIdentifier) {
        try {
            Files.deleteIfExists(fileStoragePath.resolve(fileIdentifier).normalize());
        } catch (IOException e) {
            throw new FileStorageException("Could not delete file: " + fileIdentifier, e);
        }
    }

    @Override
    public String buildFileUrl(String fileIdentifier) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/download/").path(fileIdentifier).toUriString();
    }

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new InvalidFileException("Cannot upload an empty file.");
        if (file.getSize() > maxFileSize)
            throw new InvalidFileException("File exceeds maximum size of " + (maxFileSize / 1024 / 1024) + "MB.");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))
            throw new InvalidFileException("File type not allowed. Accepted: PDF, MP4, JPG, PNG.");
        String ext = getFileExtension(file.getOriginalFilename() != null ? file.getOriginalFilename() : "").toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new InvalidFileException("File extension not allowed. Accepted: .pdf, .mp4, .jpg, .jpeg, .png");
    }

    public String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot != -1) ? filename.substring(dot) : "";
    }
}
