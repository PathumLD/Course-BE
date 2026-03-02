package com.courseupload.controller;

import com.courseupload.dto.ApiResponse;
import com.courseupload.dto.CourseContentDto;
import com.courseupload.service.CourseContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private CourseContentService courseContentService;

    /**
     * Upload a file (PDF, MP4, JPG, PNG)
     * POST /api/files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CourseContentDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        CourseContentDto uploaded = courseContentService.uploadFile(file, description);
        return ResponseEntity.ok(ApiResponse.success(uploaded, "File uploaded successfully."));
    }

    /**
     * Get all uploaded file metadata
     * GET /api/files
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseContentDto>>> getAllFiles() {
        List<CourseContentDto> files = courseContentService.getAllFiles();
        return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully."));
    }

    /**
     * Get a single file metadata by ID
     * GET /api/files/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseContentDto>> getFileById(@PathVariable Long id) {
        CourseContentDto file = courseContentService.getFileById(id);
        return ResponseEntity.ok(ApiResponse.success(file, "File retrieved successfully."));
    }

    /**
     * Get files filtered by type
     * GET /api/files?type=pdf
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<CourseContentDto>>> getFilesByType(
            @RequestParam String type) {
        List<CourseContentDto> files = courseContentService.getFilesByType(type);
        return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully."));
    }

    /**
     * Download a file by stored file name
     * GET /api/files/download/{fileName}
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = courseContentService.downloadFile(fileName);

        String contentType = "application/octet-stream";
        String filename = resource.getFilename();
        if (filename != null) {
            if (filename.endsWith(".pdf")) contentType = "application/pdf";
            else if (filename.endsWith(".mp4")) contentType = "video/mp4";
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (filename.endsWith(".png")) contentType = "image/png";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Delete a file by ID
     * DELETE /api/files/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteFile(@PathVariable Long id) {
        courseContentService.deleteFile(id);
        return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully."));
    }
}
