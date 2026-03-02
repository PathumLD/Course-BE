package com.courseupload.service;

import com.courseupload.dto.CourseContentDto;
import com.courseupload.exception.ResourceNotFoundException;
import com.courseupload.model.CourseContent;
import com.courseupload.repository.CourseContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseContentService {

    @Autowired
    private CourseContentRepository courseContentRepository;

    @Autowired
    private StorageService storageService; // @Primary bean — local or S3 depending on config

    public CourseContentDto uploadFile(MultipartFile file, String description) {
        String fileIdentifier = storageService.store(file);
        String fileUrl = storageService.buildFileUrl(fileIdentifier);

        CourseContent content = CourseContent.builder()
                .fileName(fileIdentifier)
                .originalFileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileUrl(fileUrl)
                .description(description)
                .build();

        CourseContent saved = courseContentRepository.save(content);
        return toDto(saved);
    }

    public List<CourseContentDto> getAllFiles() {
        return courseContentRepository.findAllByOrderByUploadDateDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CourseContentDto getFileById(Long id) {
        CourseContent content = courseContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + id));
        return toDto(content);
    }

    public Resource downloadFile(String fileIdentifier) {
        return storageService.load(fileIdentifier);
    }

    public void deleteFile(Long id) {
        CourseContent content = courseContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + id));
        storageService.delete(content.getFileName());
        courseContentRepository.delete(content);
    }

    public List<CourseContentDto> getFilesByType(String type) {
        return courseContentRepository.findByFileTypeContainingIgnoreCase(type)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CourseContentDto toDto(CourseContent content) {
        String downloadUrl = storageService.buildFileUrl(content.getFileName());

        return CourseContentDto.builder()
                .id(content.getId())
                .fileName(content.getFileName())
                .originalFileName(content.getOriginalFileName())
                .fileType(content.getFileType())
                .fileSize(content.getFileSize())
                .fileSizeFormatted(formatFileSize(content.getFileSize()))
                .uploadDate(content.getUploadDate())
                .fileUrl(content.getFileUrl())
                .description(content.getDescription())
                .downloadUrl(downloadUrl)
                .build();
    }

    private String formatFileSize(Long sizeInBytes) {
        if (sizeInBytes < 1024) return sizeInBytes + " B";
        if (sizeInBytes < 1024 * 1024) return String.format("%.1f KB", sizeInBytes / 1024.0);
        if (sizeInBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", sizeInBytes / (1024.0 * 1024));
        return String.format("%.1f GB", sizeInBytes / (1024.0 * 1024 * 1024));
    }
}
