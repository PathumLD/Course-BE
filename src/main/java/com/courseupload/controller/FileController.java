package com.courseupload.controller;

import com.courseupload.dto.ApiResponse;
import com.courseupload.dto.CourseContentDto;
import com.courseupload.service.CourseContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private CourseContentService courseContentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CourseContentDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails) {

        CourseContentDto uploaded = courseContentService.uploadFile(file, description, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(uploaded, "File uploaded successfully."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseContentDto>>> getAllFiles(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CourseContentDto> files = courseContentService.getAllFiles(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseContentDto>> getFileById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        CourseContentDto file = courseContentService.getFileById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(file, "File retrieved successfully."));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<CourseContentDto>>> getFilesByType(
            @RequestParam String type,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CourseContentDto> files = courseContentService.getFilesByType(type, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully."));
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = courseContentService.downloadFile(fileName);
        String contentType = "application/octet-stream";
        String name = resource.getFilename();
        if (name != null) {
            if (name.endsWith(".pdf")) contentType = "application/pdf";
            else if (name.endsWith(".mp4")) contentType = "video/mp4";
            else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (name.endsWith(".png")) contentType = "image/png";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        courseContentService.deleteFile(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully."));
    }
}
