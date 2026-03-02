package com.courseupload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.courseupload.config.JwtAuthenticationFilter;
import com.courseupload.config.JwtUtil;
import com.courseupload.config.SecurityConfig;
import com.courseupload.controller.AuthController;
import com.courseupload.dto.AuthDto;
import com.courseupload.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("AuthController API Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // POST /api/auth/register
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Register with valid data returns 200 with JWT token")
    void register_ValidRequest_Returns200WithToken() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
                "instructor1", "password123", "instructor@test.com");

        AuthDto.AuthResponse response = AuthDto.AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.test.token")
                .username("instructor1")
                .email("instructor@test.com")
                .role("INSTRUCTOR")
                .message("Registration successful")
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("instructor1"))
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));
    }

    @Test
    @DisplayName("Register with blank username returns 400 validation error")
    void register_BlankUsername_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
                "", "password123", "instructor@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.username").isNotEmpty());
    }

    @Test
    @DisplayName("Register with invalid email returns 400 validation error")
    void register_InvalidEmail_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
                "instructor1", "password123", "not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").isNotEmpty());
    }

    @Test
    @DisplayName("Register with short password returns 400 validation error")
    void register_ShortPassword_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
                "instructor1", "abc", "instructor@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").isNotEmpty());
    }

    @Test
    @DisplayName("Register with duplicate username returns 400")
    void register_DuplicateUsername_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
                "existing", "password123", "new@test.com");

        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException("Username already exists: existing"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login with valid credentials returns JWT token")
    void login_ValidCredentials_Returns200WithToken() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("instructor1", "password123");

        AuthDto.AuthResponse response = AuthDto.AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.test.token")
                .username("instructor1")
                .email("instructor@test.com")
                .role("INSTRUCTOR")
                .message("Login successful")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("instructor1"));
    }

    @Test
    @DisplayName("Login with wrong password returns 401")
    void login_WrongPassword_Returns401() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("instructor1", "wrongpassword");

        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid username or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    @DisplayName("Login with blank username returns 400 validation error")
    void login_BlankUsername_Returns400() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.username").isNotEmpty());
    }
}
