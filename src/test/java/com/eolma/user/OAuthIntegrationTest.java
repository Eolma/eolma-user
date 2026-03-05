package com.eolma.user;

import com.eolma.user.adapter.in.web.dto.*;
import com.eolma.user.adapter.out.oauth.GoogleOAuthAdapter;
import com.eolma.user.adapter.out.oauth.KakaoOAuthAdapter;
import com.eolma.user.application.port.out.dto.OAuthTokenResponse;
import com.eolma.user.application.port.out.dto.OAuthUserInfo;
import com.eolma.user.domain.model.AuthProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

class OAuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private com.eolma.common.kafka.EolmaKafkaProducer eolmaKafkaProducer;

    @MockBean
    private GoogleOAuthAdapter googleOAuthAdapter;

    @MockBean
    private KakaoOAuthAdapter kakaoOAuthAdapter;

    private void mockGoogleProvider(String providerId, String email) {
        given(googleOAuthAdapter.exchangeToken(anyString(), anyString()))
                .willReturn(new OAuthTokenResponse("google-token"));
        given(googleOAuthAdapter.getUserInfo("google-token"))
                .willReturn(new OAuthUserInfo(providerId, email, "Google User", null));
    }

    private void mockKakaoProvider(String providerId, String email) {
        given(kakaoOAuthAdapter.exchangeToken(anyString(), anyString()))
                .willReturn(new OAuthTokenResponse("kakao-token"));
        given(kakaoOAuthAdapter.getUserInfo("kakao-token"))
                .willReturn(new OAuthUserInfo(providerId, email, "Kakao User", null));
    }

    @Nested
    @DisplayName("OAuth Social Login Flow")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class OAuthFlowTest {

        static String accessToken;
        static String refreshToken;

        @Test
        @Order(1)
        @DisplayName("New social login returns tokens with nicknameRequired=true")
        void newSocialLogin() {
            mockGoogleProvider("google-test-001", "social@test.com");

            OAuthLoginRequest request = new OAuthLoginRequest(
                    AuthProvider.GOOGLE, "test-code", "http://localhost:3000/callback");

            ResponseEntity<OAuthLoginResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/oauth/login", request, OAuthLoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().refreshToken()).isNotBlank();
            assertThat(response.getBody().nicknameRequired()).isTrue();

            accessToken = response.getBody().accessToken();
            refreshToken = response.getBody().refreshToken();
        }

        @Test
        @Order(2)
        @DisplayName("Set nickname after social login")
        void setNickname() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            SetNicknameRequest request = new SetNicknameRequest("socialuser");

            ResponseEntity<LoginResponse> response = restTemplate.exchange(
                    "/api/v1/auth/oauth/nickname", HttpMethod.POST,
                    new HttpEntity<>(request, headers), LoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();

            accessToken = response.getBody().accessToken();
        }

        @Test
        @Order(3)
        @DisplayName("Re-login with same social account returns tokens")
        void reLogin() {
            mockGoogleProvider("google-test-001", "social@test.com");

            OAuthLoginRequest request = new OAuthLoginRequest(
                    AuthProvider.GOOGLE, "test-code", "http://localhost:3000/callback");

            ResponseEntity<OAuthLoginResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/oauth/login", request, OAuthLoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().nicknameRequired()).isNull();
            assertThat(response.getBody().linkToken()).isNull();
        }

        @Test
        @Order(4)
        @DisplayName("Get linked accounts returns social accounts")
        void getLinkedAccounts() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<LinkedAccountResponse[]> response = restTemplate.exchange(
                    "/api/v1/auth/oauth/accounts", HttpMethod.GET,
                    new HttpEntity<>(headers), LinkedAccountResponse[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody()[0].provider()).isEqualTo("GOOGLE");
        }
    }

    @Nested
    @DisplayName("Account linking flow")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AccountLinkTest {

        static String linkToken;

        @Test
        @Order(1)
        @DisplayName("Register with email/password first")
        void registerLocal() {
            RegisterRequest request = new RegisterRequest(
                    "link@test.com", "password123", "linkuser");

            ResponseEntity<MemberResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", request, MemberResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @Order(2)
        @DisplayName("OAuth login with same email returns link info")
        void oauthLoginSameEmail() {
            mockKakaoProvider("kakao-test-001", "link@test.com");

            OAuthLoginRequest request = new OAuthLoginRequest(
                    AuthProvider.KAKAO, "test-code", "http://localhost:3000/callback");

            ResponseEntity<OAuthLoginResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/oauth/login", request, OAuthLoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNull();
            assertThat(response.getBody().linkToken()).isNotBlank();
            assertThat(response.getBody().existingProviders()).contains("LOCAL");
            assertThat(response.getBody().email()).isEqualTo("link@test.com");

            linkToken = response.getBody().linkToken();
        }

        @Test
        @Order(3)
        @DisplayName("Link account with password")
        void linkAccount() {
            mockKakaoProvider("kakao-test-001", "link@test.com");

            OAuthLinkRequest request = new OAuthLinkRequest(
                    linkToken, AuthProvider.KAKAO, "test-code",
                    "http://localhost:3000/callback", "password123");

            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/oauth/link", request, LoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
        }
    }
}
