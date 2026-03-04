package com.eolma.user.application.usecase;

import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
import com.eolma.user.adapter.in.web.dto.LoginResponse;
import com.eolma.user.adapter.out.redis.RefreshTokenStore;
import com.eolma.user.application.port.out.TokenProvider;
import com.eolma.user.domain.model.Member;
import com.eolma.user.domain.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.eolma.common.logging.StructuredLogger.kv;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginUseCase.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public LoginUseCase(MemberRepository memberRepository,
                        PasswordEncoder passwordEncoder,
                        TokenProvider tokenProvider,
                        RefreshTokenStore refreshTokenStore) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional(readOnly = true)
    public LoginResponse execute(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EolmaException(ErrorType.UNAUTHORIZED,
                        "Invalid email or password"));

        if (!member.isActive()) {
            throw new EolmaException(ErrorType.FORBIDDEN,
                    "Account is not active");
        }

        if (!passwordEncoder.matches(password, member.getPasswordHash())) {
            throw new EolmaException(ErrorType.UNAUTHORIZED,
                    "Invalid email or password");
        }

        String accessToken = tokenProvider.createAccessToken(member);
        String refreshToken = tokenProvider.createRefreshToken(member);

        refreshTokenStore.save(member.getId(), refreshToken);
        log.info("Member logged in: {} {}", kv("memberId", member.getId()), kv("email", member.getEmail()));

        return new LoginResponse(accessToken, refreshToken);
    }
}
