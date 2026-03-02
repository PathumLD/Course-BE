package com.courseupload.service;

import com.courseupload.dto.CourseContentDto;
import com.courseupload.exception.ResourceNotFoundException;
import com.courseupload.model.CourseContent;
import com.courseupload.model.User;
import com.courseupload.repository.CourseContentRepository;
import com.courseupload.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseContentService {

    @Autowired private CourseContentRepository courseContentRepository;
    @Autowired private StorageService storageService;
    @Autowired private UserRepository userRepository;

    public CourseContentDto uploadFile(MultipartFile file, String description, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        String fileIdentifier = storageService.store(file);
        String fileUrl = storageService.buildFileUrl(fileIdentifier);

        CourseContent content = CourseContent.builder()
                .fileName(fileIdentifier)
                .originalFileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileUrl(fileUrl)
                .description(description)
                .uploadedBy(owner)
                .build();

        return toDto(courseContentRepository.save(content));
    }

    public List<CourseContentDto> getAllFiles(String username) {
        return courseContentRepository.findByUploadedByUsernameOrderByUploadDateDesc(username)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public CourseContentDto getFileById(Long id, String username) {
        return toDto(findByIdAndOwner(id, username));
    }

    public Resource downloadFile(String fileIdentifier) {
        return storageService.load(fileIdentifier);
    }

    public void deleteFile(Long id, String username) {
        CourseContent content = findByIdAndOwner(id, username);
        storageService.delete(content.getFileName());
        courseContentRepository.delete(content);
    }

    public List<CourseContentDto> getFilesByType(String type, String username) {
        return courseContentRepository
                .findByFileTypeContainingIgnoreCaseAndUploadedByUsername(type, username)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private CourseContent findByIdAndOwner(Long id, String username) {
        return courseContentRepository.findByIdAndUploadedByUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + id));
    }

    private CourseContentDto toDto(CourseContent c) {
        return CourseContentDto.builder()
                .id(c.getId()).fileName(c.getFileName())
                .originalFileName(c.getOriginalFileName())
                .fileType(c.getFileType()).fileSize(c.getFileSize())
                .fileSizeFormatted(formatFileSize(c.getFileSize()))
                .uploadDate(c.getUploadDate()).fileUrl(c.getFileUrl())
                .description(c.getDescription())
                .downloadUrl(storageService.buildFileUrl(c.getFileName()))
                .build();
    }

    private String formatFileSize(Long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
