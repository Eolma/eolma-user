package com.eolma.user;

import com.eolma.user.adapter.in.web.dto.*;
import com.eolma.user.adapter.out.redis.RefreshTokenStore;
import com.eolma.user.application.port.out.EventPublisher;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @MockBean
    private com.eolma.common.kafka.EolmaKafkaProducer eolmaKafkaProducer;

    @Nested
    @DisplayName("Register -> Login -> Authenticated API call")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FullFlowTest {

        static String accessToken;
        static String refreshToken;

        @Test
        @Order(1)
        @DisplayName("Register a new member")
        void register() {
            RegisterRequest request = new RegisterRequest(
                    "test@eolma.com", "password123", "testuser");

            ResponseEntity<MemberResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", request, MemberResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().email()).isEqualTo("test@eolma.com");
            assertThat(response.getBody().nickname()).isEqualTo("testuser");
            assertThat(response.getBody().role()).isEqualTo("USER");
        }

        @Test
        @Order(2)
        @DisplayName("Login and get JWT tokens")
        void login() {
            LoginRequest request = new LoginRequest("test@eolma.com", "password123");

            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/login", request, LoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().refreshToken()).isNotBlank();

            accessToken = response.getBody().accessToken();
            refreshToken = response.getBody().refreshToken();
        }

        @Test
        @Order(3)
        @DisplayName("Access authenticated API with valid token")
        void accessAuthenticatedApi() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<MemberResponse> response = restTemplate.exchange(
                    "/api/v1/members/me", HttpMethod.GET,
                    new HttpEntity<>(headers), MemberResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().email()).isEqualTo("test@eolma.com");
        }

        @Test
        @Order(4)
        @DisplayName("Refresh token to get new access token")
        void refreshTokenTest() {
            RefreshRequest request = new RefreshRequest(refreshToken);

            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/refresh", request, LoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().refreshToken()).isNotBlank();

            // Update tokens for subsequent tests
            accessToken = response.getBody().accessToken();
            refreshToken = response.getBody().refreshToken();
        }

        @Test
        @Order(5)
        @DisplayName("Logout invalidates refresh token")
        void logout() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/auth/logout", HttpMethod.POST,
                    new HttpEntity<>(headers), Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Old refresh token should no longer work
            RefreshRequest refreshRequest = new RefreshRequest(refreshToken);
            ResponseEntity<String> refreshResponse = restTemplate.postForEntity(
                    "/api/v1/auth/refresh", refreshRequest, String.class);

            assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Authentication failure scenarios")
    class AuthFailureTest {

        @Test
        @DisplayName("Invalid JWT returns 401")
        void invalidJwt() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalid.jwt.token");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/members/me", HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("No token returns 401")
        void noToken() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/v1/members/me", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Wrong password returns 401")
        void wrongPassword() {
            // Register first
            RegisterRequest registerRequest = new RegisterRequest(
                    "wrong@eolma.com", "password123", "wronguser");
            restTemplate.postForEntity("/api/v1/auth/register", registerRequest, MemberResponse.class);

            // Login with wrong password
            LoginRequest loginRequest = new LoginRequest("wrong@eolma.com", "wrongpassword");
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/auth/login", loginRequest, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Duplicate email returns 409")
        void duplicateEmail() {
            RegisterRequest request1 = new RegisterRequest(
                    "dup@eolma.com", "password123", "user1");
            restTemplate.postForEntity("/api/v1/auth/register", request1, MemberResponse.class);

            RegisterRequest request2 = new RegisterRequest(
                    "dup@eolma.com", "password123", "user2");
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", request2, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }
}
