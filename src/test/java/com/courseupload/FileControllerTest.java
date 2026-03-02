package com.courseupload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.courseupload.config.JwtUtil;
import com.courseupload.controller.FileController;
import com.courseupload.dto.CourseContentDto;
import com.courseupload.exception.InvalidFileException;
import com.courseupload.exception.ResourceNotFoundException;
import com.courseupload.service.CourseContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@DisplayName("FileController API Tests")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseContentService courseContentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "instructor", roles = "INSTRUCTOR")
    @DisplayName("Upload valid PDF returns 200 with metadata")
    void uploadFile_ValidPdf_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture.pdf", "application/pdf", "PDF bytes".getBytes());

        CourseContentDto dto = CourseContentDto.builder()
                .id(1L).originalFileName("lecture.pdf").fileType("application/pdf")
                .fileSize(9L).fileSizeFormatted("9 B").uploadDate(LocalDateTime.now())
                .downloadUrl("http://localhost/api/files/download/uuid.pdf").build();

        when(courseContentService.uploadFile(any(), any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .param("description", "Week 1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File uploaded successfully."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.originalFileName").value("lecture.pdf"))
                .andExpect(jsonPath("$.data.fileType").value("application/pdf"));
    }

    @Test
    @WithMockUser(username = "instructor", roles = "INSTRUCTOR")
    @DisplayName("Upload invalid file type returns 400")
    void uploadFile_InvalidType_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "some text".getBytes());

        when(courseContentService.uploadFile(any(), any()))
                .thenThrow(new InvalidFileException("File type not allowed. Accepted types: PDF, MP4, JPG, PNG."));

        mockMvc.perform(multipart("/api/files/upload").file(file).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("File type not allowed")));
    }

    @Test
    @WithMockUser(username = "instructor", roles = "INSTRUCTOR")
    @DisplayName("Upload valid MP4 returns 200")
    void uploadFile_ValidMp4_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.mp4", "video/mp4", "video bytes".getBytes());
        CourseContentDto dto = CourseContentDto.builder()
                .id(2L).originalFileName("lesson.mp4").fileType("video/mp4").fileSize(11L).build();
        when(courseContentService.uploadFile(any(), any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/files/upload").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileType").value("video/mp4"));
    }

    @Test
    @DisplayName("Upload without authentication returns 401")
    void uploadFile_Unauthenticated_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());
        mockMvc.perform(multipart("/api/files/upload").file(file).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Get all files returns list")
    void getAllFiles_Returns200WithList() throws Exception {
        List<CourseContentDto> files = List.of(
                CourseContentDto.builder().id(1L).originalFileName("a.pdf").fileType("application/pdf").build(),
                CourseContentDto.builder().id(2L).originalFileName("b.png").fileType("image/png").build()
        );
        when(courseContentService.getAllFiles()).thenReturn(files);

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].originalFileName").value("a.pdf"))
                .andExpect(jsonPath("$.data[1].originalFileName").value("b.png"));
    }

    @Test
    @WithMockUser
    @DisplayName("Get all files when empty returns empty list")
    void getAllFiles_Empty_Returns200() throws Exception {
        when(courseContentService.getAllFiles()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("Get file by valid ID returns metadata")
    void getFileById_ValidId_Returns200() throws Exception {
        CourseContentDto dto = CourseContentDto.builder()
                .id(5L).originalFileName("lecture.pdf").fileType("application/pdf").fileSize(1024L).build();
        when(courseContentService.getFileById(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/files/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.originalFileName").value("lecture.pdf"));
    }

    @Test
    @WithMockUser
    @DisplayName("Get file by non-existent ID returns 404")
    void getFileById_NotFound_Returns404() throws Exception {
        when(courseContentService.getFileById(999L))
                .thenThrow(new ResourceNotFoundException("File not found with id: 999"));

        mockMvc.perform(get("/api/files/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File not found with id: 999"));
    }

    @Test
    @DisplayName("Download file returns file bytes (public endpoint)")
    void downloadFile_ValidFile_Returns200() throws Exception {
        byte[] content = "PDF file content".getBytes();
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override public String getFilename() { return "abc123.pdf"; }
        };
        when(courseContentService.downloadFile("abc123.pdf")).thenReturn(resource);

        mockMvc.perform(get("/api/files/download/abc123.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("abc123.pdf")));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("Delete existing file returns 200")
    void deleteFile_ExistingId_Returns200() throws Exception {
        doNothing().when(courseContentService).deleteFile(1L);

        mockMvc.perform(delete("/api/files/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File deleted successfully."));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("Delete non-existent file returns 404")
    void deleteFile_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("File not found with id: 42"))
                .when(courseContentService).deleteFile(42L);

        mockMvc.perform(delete("/api/files/42").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Delete without authentication returns 401")
    void deleteFile_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(delete("/api/files/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
