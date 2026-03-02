package com.courseupload.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over storage backends (local disk or AWS S3).
 * Switch implementations via app.storage.strategy in application.properties.
 */
public interface StorageService {

    /**
     * Persist a file and return the stored file identifier (fileName or S3 key).
     */
    String store(MultipartFile file);

    /**
     * Retrieve a file as a Resource (for download).
     */
    Resource load(String fileIdentifier);

    /**
     * Delete a stored file.
     */
    void delete(String fileIdentifier);

    /**
     * Build the public URL for a given file identifier.
     */
    String buildFileUrl(String fileIdentifier);
}
