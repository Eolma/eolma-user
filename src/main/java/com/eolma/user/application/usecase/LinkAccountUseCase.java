package com.eolma.user.application.usecase;

import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
import com.eolma.user.adapter.in.web.dto.LoginResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.eolma.common.logging.StructuredLogger.kv;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(LinkAccountUseCase.class);

    private final OAuthLinkTokenStore linkTokenStore;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final OAuthProviderRegistry providerRegistry;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public LinkAccountUseCase(OAuthLinkTokenStore linkTokenStore,
                               MemberRepository memberRepository,
                               SocialAccountRepository socialAccountRepository,
                               OAuthProviderRegistry providerRegistry,
                               PasswordEncoder passwordEncoder,
                               TokenProvider tokenProvider,
                               RefreshTokenStore refreshTokenStore) {
        this.linkTokenStore = linkTokenStore;
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.providerRegistry = providerRegistry;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public LoginResponse execute(String linkToken, AuthProvider provider,
                                  String code, String redirectUri, String password) {
        // linkToken 검증 및 소비
        String email = linkTokenStore.consumeToken(linkToken)
                .orElseThrow(() -> new EolmaException(ErrorType.UNAUTHORIZED,
                        "Invalid or expired link token"));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EolmaException(ErrorType.NOT_FOUND,
                        "Member not found"));

        if (!member.isActive()) {
            throw new EolmaException(ErrorType.FORBIDDEN, "Account is not active");
        }

        // LOCAL 계정 연결 시 비밀번호 검증
        if (member.hasPassword()) {
            if (password == null || password.isBlank()) {
                throw new EolmaException(ErrorType.PASSWORD_REQUIRED_FOR_LINKING,
                        "Password is required to link with existing account");
            }
            if (!passwordEncoder.matches(password, member.getPasswordHash())) {
                throw new EolmaException(ErrorType.UNAUTHORIZED,
                        "Invalid password");
            }
        }

        // OAuth provider에서 사용자 정보 조회 후 소셜 계정 연결
        OAuthProviderPort providerPort = providerRegistry.getProvider(provider);
        OAuthTokenResponse tokenResponse = providerPort.exchangeToken(code, redirectUri);
        OAuthUserInfo userInfo = providerPort.getUserInfo(tokenResponse.accessToken());

        SocialAccount socialAccount = new SocialAccount(member, provider, userInfo.providerId());
        socialAccountRepository.save(socialAccount);

        log.info("Account linked: {} {} {}",
                kv("memberId", member.getId()),
                kv("provider", provider),
                kv("email", email));

        // JWT 발급
        String accessToken = tokenProvider.createAccessToken(member);
        String refreshToken = tokenProvider.createRefreshToken(member);
        refreshTokenStore.save(member.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }
}
