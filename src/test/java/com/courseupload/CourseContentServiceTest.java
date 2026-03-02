package com.courseupload;

import com.courseupload.dto.CourseContentDto;
import com.courseupload.exception.ResourceNotFoundException;
import com.courseupload.model.CourseContent;
import com.courseupload.repository.CourseContentRepository;
import com.courseupload.service.CourseContentService;
import com.courseupload.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseContentService Unit Tests")
class CourseContentServiceTest {

    @InjectMocks
    private CourseContentService service;

    @Mock
    private CourseContentRepository courseContentRepository;

    @Mock
    private StorageService storageService;

    // ─────────────────────────────────────────────────────────────
    // uploadFile
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("uploadFile stores file, saves metadata and returns DTO")
    void uploadFile_ValidFile_SavesAndReturnsDto() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture.pdf", "application/pdf", "content".getBytes());

        when(storageService.store(any())).thenReturn("uuid-1234.pdf");
        when(storageService.buildFileUrl("uuid-1234.pdf"))
                .thenReturn("http://localhost/api/files/download/uuid-1234.pdf");

        CourseContent saved = CourseContent.builder()
                .id(1L).fileName("uuid-1234.pdf").originalFileName("lecture.pdf")
                .fileType("application/pdf").fileSize(7L)
                .fileUrl("http://localhost/api/files/download/uuid-1234.pdf")
                .description("Week 1").uploadDate(LocalDateTime.now())
                .build();

        when(courseContentRepository.save(any())).thenReturn(saved);

        CourseContentDto result = service.uploadFile(file, "Week 1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOriginalFileName()).isEqualTo("lecture.pdf");
        assertThat(result.getFileType()).isEqualTo("application/pdf");
        assertThat(result.getDownloadUrl()).contains("uuid-1234.pdf");

        verify(storageService, times(1)).store(file);
        verify(courseContentRepository, times(1)).save(any(CourseContent.class));
    }

    @Test
    @DisplayName("uploadFile description can be null (optional field)")
    void uploadFile_NullDescription_SavesSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "bytes".getBytes());

        when(storageService.store(any())).thenReturn("uuid-abc.png");
        when(storageService.buildFileUrl(any())).thenReturn("http://localhost/api/files/download/uuid-abc.png");

        CourseContent saved = CourseContent.builder()
                .id(2L).fileName("uuid-abc.png").originalFileName("image.png")
                .fileType("image/png").fileSize(5L).fileUrl("http://localhost/...").build();
        when(courseContentRepository.save(any())).thenReturn(saved);

        CourseContentDto result = service.uploadFile(file, null);
        assertThat(result).isNotNull();
    }

    // ─────────────────────────────────────────────────────────────
    // getAllFiles
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllFiles returns all files ordered by upload date")
    void getAllFiles_ReturnsMappedDtoList() {
        List<CourseContent> contents = List.of(
                CourseContent.builder().id(1L).fileName("a.pdf").originalFileName("a.pdf")
                        .fileType("application/pdf").fileSize(100L).fileUrl("/a").uploadDate(LocalDateTime.now()).build(),
                CourseContent.builder().id(2L).fileName("b.mp4").originalFileName("b.mp4")
                        .fileType("video/mp4").fileSize(500L).fileUrl("/b").uploadDate(LocalDateTime.now().minusHours(1)).build()
        );

        when(courseContentRepository.findAllByOrderByUploadDateDesc()).thenReturn(contents);
        when(storageService.buildFileUrl(any())).thenReturn("http://localhost/download/file");

        List<CourseContentDto> results = service.getAllFiles();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getFileType()).isEqualTo("application/pdf");
        assertThat(results.get(1).getFileType()).isEqualTo("video/mp4");
    }

    @Test
    @DisplayName("getAllFiles returns empty list when no files exist")
    void getAllFiles_Empty_ReturnsEmptyList() {
        when(courseContentRepository.findAllByOrderByUploadDateDesc()).thenReturn(List.of());
        List<CourseContentDto> results = service.getAllFiles();
        assertThat(results).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // getFileById
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFileById returns correct DTO for existing ID")
    void getFileById_ExistingId_ReturnsDto() {
        CourseContent content = CourseContent.builder()
                .id(10L).fileName("doc.pdf").originalFileName("doc.pdf")
                .fileType("application/pdf").fileSize(2048L).fileUrl("/doc").build();

        when(courseContentRepository.findById(10L)).thenReturn(Optional.of(content));
        when(storageService.buildFileUrl(any())).thenReturn("http://localhost/download/doc.pdf");

        CourseContentDto result = service.getFileById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getFileSize()).isEqualTo(2048L);
        assertThat(result.getFileSizeFormatted()).isEqualTo("2.0 KB");
    }

    @Test
    @DisplayName("getFileById throws ResourceNotFoundException for missing ID")
    void getFileById_NotFound_ThrowsException() {
        when(courseContentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFileById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    // ─────────────────────────────────────────────────────────────
    // deleteFile
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteFile removes from storage and database")
    void deleteFile_ExistingId_DeletesFromStorageAndDb() {
        CourseContent content = CourseContent.builder()
                .id(1L).fileName("uuid.pdf").originalFileName("lecture.pdf")
                .fileType("application/pdf").fileSize(100L).fileUrl("/").build();

        when(courseContentRepository.findById(1L)).thenReturn(Optional.of(content));

        service.deleteFile(1L);

        verify(storageService, times(1)).delete("uuid.pdf");
        verify(courseContentRepository, times(1)).delete(content);
    }

    @Test
    @DisplayName("deleteFile throws ResourceNotFoundException for missing ID")
    void deleteFile_NotFound_ThrowsException() {
        when(courseContentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteFile(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(storageService, never()).delete(any());
    }
}
