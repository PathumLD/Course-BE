package com.courseupload.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(MultipartFile file);
    Resource load(String fileIdentifier);
    void delete(String fileIdentifier);
    String buildFileUrl(String fileIdentifier);
}
