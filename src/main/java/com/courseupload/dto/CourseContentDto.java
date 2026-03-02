package com.courseupload.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseContentDto {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String fileSizeFormatted;
    private LocalDateTime uploadDate;
    private String fileUrl;
    private String description;
    private String downloadUrl;
}
