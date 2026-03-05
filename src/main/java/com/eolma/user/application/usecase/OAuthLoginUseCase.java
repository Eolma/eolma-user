package com.eolma.user.application.usecase;

import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.eolma.common.logging.StructuredLogger.kv;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OAuthLoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginUseCase.class);

    private final OAuthProviderRegistry providerRegistry;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final OAuthLinkTokenStore linkTokenStore;

    public OAuthLoginUseCase(OAuthProviderRegistry providerRegistry,
                              MemberRepository memberRepository,
                              SocialAccountRepository socialAccountRepository,
                              TokenProvider tokenProvider,
                              RefreshTokenStore refreshTokenStore,
                              OAuthLinkTokenStore linkTokenStore) {
        this.providerRegistry = providerRegistry;
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.linkTokenStore = linkTokenStore;
    }

    @Transactional
    public OAuthLoginResponse execute(AuthProvider provider, String code, String redirectUri) {
        // OAuth provider에서 사용자 정보 조회
        OAuthProviderPort providerPort = providerRegistry.getProvider(provider);
        OAuthTokenResponse tokenResponse = providerPort.exchangeToken(code, redirectUri);
        OAuthUserInfo userInfo = providerPort.getUserInfo(tokenResponse.accessToken());

        // 이미 연결된 소셜 계정이 있는지 확인
        Optional<SocialAccount> existingSocial = socialAccountRepository
                .findByProviderAndProviderId(provider, userInfo.providerId());

        if (existingSocial.isPresent()) {
            // FR-8: 이미 연결된 소셜로 재로그인
            Member member = existingSocial.get().getMember();
            if (!member.isActive()) {
                throw new EolmaException(ErrorType.FORBIDDEN, "Account is not active");
            }
            return issueTokens(member);
        }

        // 동일 이메일 기존 회원 확인
        Optional<Member> existingMember = memberRepository.findByEmail(userInfo.email());

        if (existingMember.isPresent()) {
            // FR-5: 동일 이메일, 다른 provider -> 계정 연결 요구
            Member member = existingMember.get();
            List<SocialAccount> linkedAccounts = socialAccountRepository.findByMemberId(member.getId());

            List<String> existingProviders = new java.util.ArrayList<>();
            if (member.hasPassword()) {
                existingProviders.add(AuthProvider.LOCAL.name());
            }
            linkedAccounts.forEach(sa -> existingProviders.add(sa.getProvider().name()));

            String linkToken = linkTokenStore.createToken(userInfo.email());

            log.info("Account link required: {} {} {}",
                    kv("email", userInfo.email()),
                    kv("provider", provider),
                    kv("existingProviders", existingProviders));

            return OAuthLoginResponse.linkRequired(linkToken, existingProviders, userInfo.email());
        }

        // FR-3: 신규 소셜 가입
        Member newMember = Member.createSocialMember(userInfo.email());
        Member saved = memberRepository.save(newMember);

        SocialAccount socialAccount = new SocialAccount(saved, provider, userInfo.providerId());
        socialAccountRepository.save(socialAccount);

        log.info("New social member registered: {} {} {}",
                kv("memberId", saved.getId()),
                kv("email", saved.getEmail()),
                kv("provider", provider));

        String accessToken = tokenProvider.createAccessToken(saved);
        String refreshToken = tokenProvider.createRefreshToken(saved);
        refreshTokenStore.save(saved.getId(), refreshToken);

        return OAuthLoginResponse.newMember(accessToken, refreshToken);
    }

    private OAuthLoginResponse issueTokens(Member member) {
        String accessToken = tokenProvider.createAccessToken(member);
        String refreshToken = tokenProvider.createRefreshToken(member);
        refreshTokenStore.save(member.getId(), refreshToken);

        log.info("Social login: {} {}", kv("memberId", member.getId()), kv("email", member.getEmail()));

        return OAuthLoginResponse.existingMember(accessToken, refreshToken);
    }
}
