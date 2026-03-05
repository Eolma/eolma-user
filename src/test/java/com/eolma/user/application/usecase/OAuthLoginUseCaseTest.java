package com.eolma.user.application.usecase;

import com.eolma.user.adapter.in.web.dto.OAuthLoginResponse;
import com.eolma.user.adapter.out.oauth.OAuthProviderRegistry;
import com.eolma.user.adapter.out.redis.OAuthLinkTokenStore;
import com.eolma.user.adapter.out.redis.RefreshTokenStore;
import com.eolma.user.application.port.out.OAuthProviderPort;
import com.eolma.user.application.port.out.TokenProvider;
import com.eolma.user.application.port.out.dto.OAuthTokenResponse;
import com.eolma.user.application.port.out.dto.OAuthUserInfo;
import com.eolma.user.domain.model.AuthProvider;
import com.eolma.user.domain.model.Member;
import com.eolma.user.domain.model.SocialAccount;
import com.eolma.user.domain.repository.MemberRepository;
import com.eolma.user.domain.repository.SocialAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthLoginUseCaseTest {

    @Mock private OAuthProviderRegistry providerRegistry;
    @Mock private MemberRepository memberRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private TokenProvider tokenProvider;
    @Mock private RefreshTokenStore refreshTokenStore;
    @Mock private OAuthLinkTokenStore linkTokenStore;
    @Mock private OAuthProviderPort oAuthProviderPort;

    @InjectMocks
    private OAuthLoginUseCase oAuthLoginUseCase;

    private static final String CODE = "auth-code";
    private static final String REDIRECT_URI = "http://localhost:3000/callback";

    private void setupOAuthProvider(String providerId, String email) {
        given(providerRegistry.getProvider(any())).willReturn(oAuthProviderPort);
        given(oAuthProviderPort.exchangeToken(CODE, REDIRECT_URI))
                .willReturn(new OAuthTokenResponse("oauth-access-token"));
        given(oAuthProviderPort.getUserInfo("oauth-access-token"))
                .willReturn(new OAuthUserInfo(providerId, email, "Test User", null));
    }

    @Nested
    @DisplayName("FR-8: 이미 연결된 소셜 재로그인")
    class ExistingSocialLoginTest {

        @Test
        @DisplayName("기존 소셜 계정으로 로그인 시 JWT 발급")
        void existingSocialLogin() {
            setupOAuthProvider("google-123", "user@test.com");

            Member member = Member.builder()
                    .email("user@test.com")
                    .passwordHash("hash")
                    .nickname("testuser")
                    .build();
            SocialAccount socialAccount = new SocialAccount(member, AuthProvider.GOOGLE, "google-123");

            given(socialAccountRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123"))
                    .willReturn(Optional.of(socialAccount));
            given(tokenProvider.createAccessToken(member)).willReturn("access-token");
            given(tokenProvider.createRefreshToken(member)).willReturn("refresh-token");

            OAuthLoginResponse result = oAuthLoginUseCase.execute(AuthProvider.GOOGLE, CODE, REDIRECT_URI);

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(result.nicknameRequired()).isNull();
            assertThat(result.linkToken()).isNull();
        }
    }

    @Nested
    @DisplayName("FR-3: 신규 소셜 가입")
    class NewSocialRegisterTest {

        @Test
        @DisplayName("신규 소셜 가입 시 nicknameRequired=true")
        void newSocialRegister() {
            setupOAuthProvider("google-456", "new@test.com");

            given(socialAccountRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-456"))
                    .willReturn(Optional.empty());
            given(memberRepository.findByEmail("new@test.com")).willReturn(Optional.empty());

            Member savedMember = Member.createSocialMember("new@test.com");
            given(memberRepository.save(any(Member.class))).willReturn(savedMember);
            given(tokenProvider.createAccessToken(any())).willReturn("access-token");
            given(tokenProvider.createRefreshToken(any())).willReturn("refresh-token");

            OAuthLoginResponse result = oAuthLoginUseCase.execute(AuthProvider.GOOGLE, CODE, REDIRECT_URI);

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.nicknameRequired()).isTrue();
            assertThat(result.linkToken()).isNull();
            verify(socialAccountRepository).save(any(SocialAccount.class));
        }
    }

    @Nested
    @DisplayName("FR-5: 동일 이메일 다른 provider")
    class AccountLinkRequiredTest {

        @Test
        @DisplayName("동일 이메일 존재 시 linkToken 반환")
        void linkRequired() {
            setupOAuthProvider("kakao-789", "existing@test.com");

            given(socialAccountRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-789"))
                    .willReturn(Optional.empty());

            Member existingMember = Member.builder()
                    .email("existing@test.com")
                    .passwordHash("hash")
                    .nickname("existing")
                    .build();
            given(memberRepository.findByEmail("existing@test.com")).willReturn(Optional.of(existingMember));
            given(socialAccountRepository.findByMemberId(any())).willReturn(List.of());
            given(linkTokenStore.createToken("existing@test.com")).willReturn("link-token-123");

            OAuthLoginResponse result = oAuthLoginUseCase.execute(AuthProvider.KAKAO, CODE, REDIRECT_URI);

            assertThat(result.accessToken()).isNull();
            assertThat(result.linkToken()).isEqualTo("link-token-123");
            assertThat(result.existingProviders()).contains("LOCAL");
            assertThat(result.email()).isEqualTo("existing@test.com");
        }
    }
}
