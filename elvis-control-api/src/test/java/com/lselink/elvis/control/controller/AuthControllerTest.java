package com.lselink.elvis.control.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.control.dto.auth.AdminUserDto;
import com.lselink.elvis.control.dto.auth.AuthTokenDto;
import com.lselink.elvis.control.dto.auth.LoginRequest;
import com.lselink.elvis.control.dto.auth.TokenRefreshRequest;
import com.lselink.elvis.control.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S33-API-0070-TE] AuthController 단위 테스트
 */
class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login 호출 시 AuthTokenDto 반환 및 200 OK")
    void testLoginEndpoint() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123!");
        AuthTokenDto mockToken = AuthTokenDto.of("access.mock.token", "refresh.mock.token", 3600, "admin", "ROLE_ADMIN");

        given(authService.login(any(LoginRequest.class))).willReturn(mockToken);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").value("access.mock.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh.mock.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh 호출 시 갱신된 AuthTokenDto 반환 및 200 OK")
    void testRefreshTokenEndpoint() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest("valid.refresh.token");
        AuthTokenDto mockToken = AuthTokenDto.of("new.access.token", "new.refresh.token", 3600, "operator", "ROLE_OPERATOR");

        given(authService.refreshToken(any(TokenRefreshRequest.class))).willReturn(mockToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("operator"))
                .andExpect(jsonPath("$.data.accessToken").value("new.access.token"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me 호출 시 관리자 프로필 정보 반환 및 200 OK")
    void testGetMyProfileEndpoint() throws Exception {
        AdminUserDto adminDto = new AdminUserDto("admin", "최고관리자", "시스템 관리자", true, LocalDateTime.now());

        given(authService.getCurrentUser("admin")).willReturn(adminDto);

        mockMvc.perform(get("/api/v1/auth/me")
                        .principal(() -> "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.name").value("최고관리자"));
    }
}
