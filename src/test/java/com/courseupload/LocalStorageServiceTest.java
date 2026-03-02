package com.courseupload;

import com.courseupload.exception.InvalidFileException;
import com.courseupload.service.LocalStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LocalStorageService Validation Tests")
class LocalStorageServiceTest {

    private LocalStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new LocalStorageService();
        // Inject @Value fields via reflection since we're not spinning up the full context
        setField(service, "maxFileSize", 104857600L); // 100 MB
        setField(service, "uploadDir", System.getProperty("java.io.tmpdir") + "/test-uploads");
        service.init();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Valid file types ───────────────────────────────────────

    @Test
    @DisplayName("Valid PDF passes validation")
    void validate_ValidPdf_Passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture.pdf", "application/pdf", "PDF content".getBytes());
        assertDoesNotThrow(() -> service.validateFile(file));
    }

    @Test
    @DisplayName("Valid MP4 passes validation")
    void validate_ValidMp4_Passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "video bytes".getBytes());
        assertDoesNotThrow(() -> service.validateFile(file));
    }

    @Test
    @DisplayName("Valid JPG passes validation")
    void validate_ValidJpg_Passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "jpg bytes".getBytes());
        assertDoesNotThrow(() -> service.validateFile(file));
    }

    @Test
    @DisplayName("Valid PNG passes validation")
    void validate_ValidPng_Passes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "png bytes".getBytes());
        assertDoesNotThrow(() -> service.validateFile(file));
    }

    // ── Invalid file types ────────────────────────────────────

    @Test
    @DisplayName("TXT file type is rejected with descriptive message")
    void validate_TextFile_ThrowsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "text content".getBytes());
        InvalidFileException ex = assertThrows(InvalidFileException.class,
                () -> service.validateFile(file));
        assertTrue(ex.getMessage().contains("File type not allowed"));
        assertTrue(ex.getMessage().contains("text/plain"));
    }

    @Test
    @DisplayName("DOCX file type is rejected")
    void validate_DocxFile_ThrowsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx bytes".getBytes());
        assertThrows(InvalidFileException.class, () -> service.validateFile(file));
    }

    @Test
    @DisplayName("GIF file extension is rejected even with valid-ish content type")
    void validate_GifExtension_ThrowsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "anim.gif", "image/gif", "gif bytes".getBytes());
        assertThrows(InvalidFileException.class, () -> service.validateFile(file));
    }

    // ── Empty files ────────────────────────────────────────────

    @Test
    @DisplayName("Empty file is rejected with descriptive message")
    void validate_EmptyFile_ThrowsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);
        InvalidFileException ex = assertThrows(InvalidFileException.class,
                () -> service.validateFile(file));
        assertEquals("Cannot upload an empty file.", ex.getMessage());
    }

    // ── File size ──────────────────────────────────────────────

    @Test
    @DisplayName("File exceeding 100MB size limit is rejected")
    void validate_OversizedFile_ThrowsInvalidFileException() throws Exception {
        // Set max size to 10 bytes for this test
        setField(service, "maxFileSize", 10L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", "this is more than ten bytes".getBytes());
        InvalidFileException ex = assertThrows(InvalidFileException.class,
                () -> service.validateFile(file));
        assertTrue(ex.getMessage().contains("exceeds the maximum limit"));
    }

    // ── File extension helper ──────────────────────────────────

    @Test
    @DisplayName("getFileExtension extracts .pdf correctly")
    void getFileExtension_Pdf_ReturnsDotPdf() {
        assertEquals(".pdf", service.getFileExtension("lecture.pdf"));
    }

    @Test
    @DisplayName("getFileExtension extracts .mp4 correctly")
    void getFileExtension_Mp4_ReturnsDotMp4() {
        assertEquals(".mp4", service.getFileExtension("video.mp4"));
    }

    @Test
    @DisplayName("getFileExtension returns empty string for no extension")
    void getFileExtension_NoExtension_ReturnsEmpty() {
        assertEquals("", service.getFileExtension("filename"));
    }
}
